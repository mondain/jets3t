/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2015 James Murty
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jets3t.service.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.S3Object;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * General utility methods used throughout the jets3t project.
 *
 * @author James Murty
 */
public class ServiceUtils {
    public static String HASH_SHA256 = "SHA-256";

    private static final Log log = LogFactory.getLog(ServiceUtils.class);

    protected static final SimpleDateFormat iso8601DateParser = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    // The Eucalyptus Walrus storage service returns short, non-UTC date time values.
    protected static final SimpleDateFormat iso8601DateParser_Walrus = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss");

    protected static final SimpleDateFormat rfc822DateParser = new SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);

    static {
        iso8601DateParser.setTimeZone(new SimpleTimeZone(0, "GMT"));
        rfc822DateParser.setTimeZone(new SimpleTimeZone(0, "GMT"));
    }

    public static Date parseIso8601Date(String dateString) throws ParseException {
        ParseException exception = null;
        synchronized (iso8601DateParser) {
            try {
                return iso8601DateParser.parse(dateString);
            } catch (ParseException e) {
                exception = e;
            }
        }
        // Work-around to parse datetime value returned by Walrus
        synchronized (iso8601DateParser_Walrus) {
            try {
                return iso8601DateParser_Walrus.parse(dateString);
            } catch (ParseException e) {
                // Ignore work-around exceptions
            }
        }
        // Throw original exception if the Walrus work-around doesn't save us.
        throw exception;
    }

    public static String formatIso8601Date(Date date) {
        synchronized (iso8601DateParser) {
            return iso8601DateParser.format(date);
        }
    }

    public static Date parseRfc822Date(String dateString) throws ParseException {
        synchronized (rfc822DateParser) {
            return rfc822DateParser.parse(dateString);
        }
    }

    public static String formatRfc822Date(Date date) {
        synchronized (rfc822DateParser) {
            return rfc822DateParser.format(date);
        }
    }

    /**
     * Calculate the HMAC/SHA1 on a string.
     *
     * @param awsSecretKey
     * AWS secret key.
     * @param canonicalString
     * canonical string representing the request to sign.
     * @return Signature
     */
    public static String signWithHmacSha1(String awsSecretKey, String canonicalString)
    {
        if (awsSecretKey == null) {
            if (log.isDebugEnabled()) {
                log.debug("Canonical string will not be signed, as no AWS Secret Key was provided");
            }
            return null;
        }

        // The following HMAC/SHA1 code for the signature is taken from the
        // AWS Platform's implementation of RFC2104 (amazon.webservices.common.Signature)
        //
        // Acquire an HMAC/SHA1 from the raw key bytes.
        SecretKeySpec signingKey = null;
        signingKey = new SecretKeySpec(
            stringToBytes(awsSecretKey), Constants.HMAC_SHA1_ALGORITHM);

        // Acquire the MAC instance and initialize with the signing key.
        Mac mac = null;
        try {
            mac = Mac.getInstance(Constants.HMAC_SHA1_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            // should not happen
            throw new RuntimeException("Could not find sha1 algorithm", e);
        }
        try {
            mac.init(signingKey);
        } catch (InvalidKeyException e) {
            // also should not happen
            throw new RuntimeException("Could not initialize the MAC algorithm", e);
        }

        // Compute the HMAC on the digest, and set it.
        byte[] b64;
        try {
            b64 = Base64.encodeBase64(mac.doFinal(stringToBytes(canonicalString)));
            return new String(b64, Constants.DEFAULT_ENCODING);
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * @param str
     * @return String as bytes using default JetS3t encoding (UTF-8)
     */
    public static byte[] stringToBytes(String str) {
        try {
            return str.getBytes(Constants.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(
                "Unsupported encoding \"" + Constants.DEFAULT_ENCODING
                + "\" for: " + str, e);
        }
    }

    /**
     *
     * @param data
     * @param cryptoHash
     * @return lowercase hex-encoded hash value.
     */
    public static byte[] hash(byte[] data, String cryptoHash) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(cryptoHash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                "Could not find hashing algorithm \"" + cryptoHash + "\"", e);
        }
        md.update(data);
        return md.digest();
    }

    /**
    *
    * @param dataIS
    * @param cryptoHash
    * @param resetInsteadOfClose
    * if true, input stream is reset instead of closed after hash is generated.
    * @return lowercase hex-encoded hash value.
    * @throws IOException
    */
   public static byte[] hash(InputStream dataIS, String cryptoHash,
       boolean resetInsteadOfClose) throws IOException
   {
       MessageDigest md = null;
       try {
           md = MessageDigest.getInstance(cryptoHash);
       } catch (NoSuchAlgorithmException e) {
           throw new RuntimeException(
               "Could not find hashing algorithm \"" + cryptoHash + "\"", e);
       }

       BufferedInputStream bis = new BufferedInputStream(dataIS);
       try {
           byte[] buffer = new byte[16384];
           int bytesRead = -1;
           while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
               md.update(buffer, 0, bytesRead);
           }
       } finally {
           if (resetInsteadOfClose) {
               dataIS.reset();
           } else {
               try {
                   bis.close();
               } catch (Exception e) {
               }
           }
       }

       return md.digest();
   }

    /**
     *
     * @param data
     * @param cryptoHash
     * @return lowercase hex-encoded hash value.
     */
    public static byte[] hash(String data, String cryptoHash) {
        return hash(stringToBytes(data), cryptoHash);
    }

    public static byte[] hashSHA256(byte[] data) {
        return hash(data, "SHA-256");
    }

    public static byte[] hashSHA256(
        InputStream dataIS, boolean resetInsteadOfClose) throws IOException
    {
        return hash(dataIS, "SHA-256", resetInsteadOfClose);
    }

    public static byte[] hashSHA256(InputStream dataIS) throws IOException {
        return hashSHA256(dataIS, false);
    }

    /**
     *
     * @param key
     * key for HMAC
     * @param data
     * data to be HMAC'd
     * @param cryptoAlgorithm
     * cryptographic algorithm to use for HMAC, e.g. "SHA-256"
     * @return HMAC hash value with given crypto hashing algorithm.
     */
    public static byte[] hmac(byte[] key, byte[] data, String cryptoAlgorithm) {
        String hmacDefinition = "Hmac" + cryptoAlgorithm;
        try {
            SecretKeySpec signingKey = new SecretKeySpec(key, hmacDefinition);
            Mac mac = Mac.getInstance(hmacDefinition);
            mac.init(signingKey);
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(
                "Could not find hashing algorithm \"" + hmacDefinition + "\"", e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(
                "Could not init hashing algorithm \"" + hmacDefinition + "\"", e);
        }
    }

    /**
     * Return lowercase hex-encoded HMAC message digest of given data using the
     * given key, using a crypto hash like "SHA256".
     *
     * @param key
     * @param data
     * @return HMAC SHA256 hash value.
     */
    public static byte[] hmacSHA256(String key, String data) {
        return hmac(stringToBytes(key), stringToBytes(data), "SHA256");
    }

    /**
     *
     * @param key
     * @param data
     * @return HMAC SHA256 hash value.
     */
    public static byte[] hmacSHA256(byte[] key, byte[] data) {
        return hmac(key, data, "SHA256");
    }

    /**
     * Reads text data from an input stream and returns it as a String.
     *
     * @param is
     * input stream from which text data is read.
     * @param encoding
     * the character encoding of the textual data in the input stream. If this
     * parameter is null, the default system encoding will be used.
     *
     * @return
     * text data read from the input stream.
     *
     * @throws IOException
     */
    public static String readInputStreamToString(InputStream is, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        if (encoding != null) {
            br = new BufferedReader(new InputStreamReader(is, encoding));
        } else {
            br = new BufferedReader(new InputStreamReader(is));
        }
        String line = null;
        try {
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (!firstLine) {
                    sb.append("\n");
                }
                sb.append(line);
                firstLine = false;
            }
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Unable to read String from Input Stream", e);
            }
        }
        return sb.toString();
    }

    /**
     * Reads from an input stream until a newline character or the end of the stream is reached.
     *
     * @param is
     * @return
     * text data read from the input stream, not including the newline character.
     * @throws IOException
     */
    public static String readInputStreamLineToString(InputStream is, String encoding) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b = -1;
        while ((b = is.read()) != -1) {
            if ('\n' == (char) b) {
                break;
            } else {
                baos.write(b);
            }
        }
        return new String(baos.toByteArray(), encoding);
    }

    /**
     * Reads binary data from an input stream and returns it as a byte array.
     *
     * @param is
     * input stream from which data is read.
     *
     * @return
     * byte array containing data read from the input stream.
     *
     * @throws IOException
     */
    public static byte[] readInputStreamToBytes(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int b = -1;
        while ((b = is.read()) != -1) {
            baos.write(b);
        }
        return baos.toByteArray();
    }

    /**
     * Counts the total number of bytes in a set of S3Objects by summing the
     * content length of each.
     *
     * @param objects
     * @return
     * total number of bytes in all S3Objects.
     */
    public static long countBytesInObjects(S3Object[] objects) {
        long byteTotal = 0;
        for (int i = 0; objects != null && i < objects.length; i++) {
            byteTotal += objects[i].getContentLength();
        }
        return byteTotal;
    }

    /**
     * From a map of metadata returned from a REST GET or HEAD request, returns a map
     * of metadata with the HTTP-connection-specific metadata items removed.
     *
     * @param metadata
     * metadata map to be cleaned
     * @param serviceMetadataPrefix
     * prefix denoting service-specific "header" HTTP header values (case insensitive)
     * @param userMetadataPrefix
     * prefix denoting service-specific "user metadata" HTTP header values (case insensitive)
     * @return
     * metadata map with HTTP-connection-specific items removed.
     */
    public static Map<String, Object> cleanRestMetadataMap(
        Map<String, Object> metadata, String serviceMetadataPrefix, String userMetadataPrefix)
    {
        if (log.isDebugEnabled()) {
            log.debug("Processing REST metadata items");
        }

        Map<String, Object> combinedMap = new HashMap<String, Object>();
        Map<String, Object> serviceMetadataMap = new HashMap<String, Object>();
        Map<String, Object> userMetadataMap = new HashMap<String, Object>();
        Map<String, Object> httpMetadataMap = new HashMap<String, Object>();
        Map<String, Object> completeMetadataMap = new HashMap<String, Object>();

        if (metadata != null) {
            for (Map.Entry<String, Object> entry: metadata.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                // Convert connection header string Collections into simple strings (where
                // appropriate)
                if (value instanceof Collection) {
                    Collection<?> coll = (Collection<?>) value;
                    if (coll.size() == 1) {
                        if (log.isDebugEnabled()) {
                            log.debug("Converted metadata single-item Collection "
                                + coll.getClass() + " " + coll + " for key: " + key);
                        }
                        value = coll.iterator().next();
                    } else {
                        if (log.isWarnEnabled()) {
                            log.warn("Collection " + coll
                                + " has too many items to convert to a single string");
                        }
                    }
                }

                // Parse date strings into Date objects, if necessary.
                if ("Date".equals(key) || "Last-Modified".equals(key)) {
                    if (!(value instanceof Date)) {
                        if (log.isDebugEnabled()) {
                            log.debug("Parsing date string '" + value
                            + "' into Date object for key: " + key);
                        }
                        try {
                            value = ServiceUtils.parseRfc822Date(value.toString());
                        } catch (ParseException pe) {
                            // Try ISO-8601 date format, just in case
                            try {
                                value = ServiceUtils.parseIso8601Date(value.toString());
                            } catch (ParseException pe2) {
                                // Log original exception if the work-around fails.
                                if (log.isWarnEnabled()) {
                                    log.warn("Date string is not RFC 822 compliant for metadata field " + key, pe);
                                }
                            }
                        }
                    }
                }

                // Recognize user/headers metadata items
                String keyStr = (key != null ? key.toString() : "");
                completeMetadataMap.put(keyStr, value);

                if (keyStr.toLowerCase().startsWith(userMetadataPrefix)) {
                    key = keyStr.substring(userMetadataPrefix.length(), keyStr.length());
                    userMetadataMap.put(key, value);
                    if (log.isDebugEnabled()) {
                        log.debug("Removed user metadata header prefix "
                            + userMetadataPrefix + " from key: " + keyStr + "=>" + key);
                    }
                } else if (keyStr.toLowerCase().startsWith(serviceMetadataPrefix)) {
                    key = keyStr.substring(serviceMetadataPrefix.length(), keyStr.length());
                    serviceMetadataMap.put(key, value);
                    if (log.isDebugEnabled()) {
                        log.debug("Removed header prefix "
                            + serviceMetadataPrefix + " from key: " + keyStr + "=>" + key);
                    }
                } else if (RestUtils.HTTP_HEADER_METADATA_NAMES.contains(keyStr.toLowerCase(Locale.ENGLISH))) {
                    key = keyStr;
                    httpMetadataMap.put(key, value);
                    if (log.isDebugEnabled()) {
                        log.debug("Leaving HTTP header item unchanged: " + key + "=" + value);
                    }
                } else if ("ETag".equalsIgnoreCase(keyStr)
                    || "Date".equalsIgnoreCase(keyStr)
                    || "Last-Modified".equalsIgnoreCase(keyStr)
                    || "Content-Range".equalsIgnoreCase(keyStr))
                {
                    key = keyStr;
                    httpMetadataMap.put(key, value);
                    if (log.isDebugEnabled()) {
                        log.debug("Leaving header item unchanged: " + key + "=" + value);
                    }
                } else if (keyStr.toLowerCase().startsWith("x-jets3t-")) {
                    // Permit pass-through of internal JetS3t "Header" data
                    key = keyStr;
                    if (log.isDebugEnabled()) {
                        log.debug("Leaving internal JetS3t header item unchanged: "
                            + key + "=" + value);
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignoring metadata item: " + keyStr + "=" + value);
                    }
                    continue;
                }

                combinedMap.put(key, value);
            }
        }

        // Add user and header metadata sub-maps to combined map
        combinedMap.put(Constants.KEY_FOR_SERVICE_METADATA, serviceMetadataMap);
        combinedMap.put(Constants.KEY_FOR_USER_METADATA, userMetadataMap);
        combinedMap.put(Constants.KEY_FOR_HTTP_METADATA, httpMetadataMap);
        combinedMap.put(Constants.KEY_FOR_COMPLETE_METADATA, completeMetadataMap);

        return combinedMap;
    }

    /**
     * Converts byte data to a Hex-encoded string.
     *
     * @param data
     * data to hex encode.
     * @return
     * hex-encoded string.
     */
    public static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            String hex = Integer.toHexString(data[i]);
            if (hex.length() == 1) {
                // Append leading zero.
                sb.append("0");
            } else if (hex.length() == 8) {
                // Remove ff prefix from negative numbers.
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Converts a Hex-encoded data string to the original byte data.
     *
     * @param hexData
     * hex-encoded data to decode.
     * @return
     * decoded data from the hex string.
     */
    public static byte[] fromHex(String hexData) {
        if ((hexData.length() & 1) != 0  ||
            hexData.replaceAll("[a-fA-F0-9]", "").length() > 0) {
            throw new java.lang.IllegalArgumentException("'" + hexData + "' is not a hex string");
        }

        byte[] result = new byte[(hexData.length() + 1) / 2];
        String hexNumber = null;
        int stringOffset = 0;
        int byteOffset = 0;
        while (stringOffset < hexData.length()) {
            hexNumber = hexData.substring(stringOffset, stringOffset + 2);
            stringOffset += 2;
            result[byteOffset++] = (byte) Integer.parseInt(hexNumber, 16);
        }
        return result;
    }

    /**
     * Converts byte data to a Base64-encoded string.
     *
     * @param data
     * data to Base64 encode.
     * @return
     * encoded Base64 string.
     */
    public static String toBase64(byte[] data) {
        byte[] b64 = Base64.encodeBase64(data);
        try {
            return new String(b64, Constants.DEFAULT_ENCODING);
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Joins a list of items into a delimiter-separated string. Each item
     * is converted to a string value with the toString() method before being
     * added to the final delimited list.
     *
     * @param items
     * the items to include in a delimited string
     * @param delimiter
     * the delimiter character or string to insert between each item in the list
     * @return
     * a delimited string
     */
    public static String join(List<?> items, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(items.get(i).toString());
            if (i < items.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Joins a list of items into a delimiter-separated string. Each item
     * is converted to a string value with the toString() method before being
     * added to the final delimited list.
     *
     * @param items
     * the items to include in a delimited string
     * @param delimiter
     * the delimiter character or string to insert between each item in the list
     * @return
     * a delimited string
     */
    public static String join(Object[] items, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            sb.append(items[i]);
            if (i < items.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     * Joins a list of <em>int</em>s into a delimiter-separated string.
     *
     * @param ints
     * the ints to include in a delimited string
     * @param delimiter
     * the delimiter character or string to insert between each item in the list
     * @return
     * a delimited string
     */
    public static String join(int[] ints, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            sb.append(ints[i]);
            if (i < ints.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    /**
     *
     * @param str
     * @param regexp
     * @return
     * string split with regexp using {@link String#split(String)} with empty
     * strings removed.
     */
    public static String[] splitIgnoreEmpty(String str, String regexp) {
        String[] splits = str.split(regexp);
        List<String> results = new ArrayList<String>();
        for (String candidate: splits) {
            if (candidate.length() > 0) {
                results.add(candidate);
            }
        }
        return results.toArray(new String[0]);
    }

    /**
     * Converts a Base64-encoded string to the original byte data.
     *
     * @param b64Data
     * a Base64-encoded string to decode.
     *
     * @return
     * bytes decoded from a Base64 string.
     */
    public static byte[] fromBase64(String b64Data) {
        byte[] decoded = Base64.decodeBase64(stringToBytes(b64Data));
        return decoded;
    }

    /**
     * Computes the MD5 hash of the data in the given input stream and returns it as a hex string.
     * The provided input stream is consumed and closed by this method.
     *
     * @param is
     * @return
     * MD5 hash
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static byte[] computeMD5Hash(InputStream is) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                System.err.println("Unable to close input stream of hash candidate: " + e);
            }
        }
    }

    /**
     * Computes the MD5 hash of the given data and returns it as a hex string.
     *
     * @param data
     * @return
     * MD5 hash.
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static byte[] computeMD5Hash(byte[] data) throws NoSuchAlgorithmException, IOException {
        return computeMD5Hash(new ByteArrayInputStream(data));
    }

    /**
     * Guess whether the given ETag value is also an MD5 hash of an underlying object
     * in a storage service, as opposed to being some other kind of opaque hash.
     * <p>
     * This test was made necessary by Amazon S3's multipart upload feature, where
     * the ETag value returned after a re-assembled multipart upload is completed
     * is no longer the same as an MD5 hash of the assembled data.
     * <p>
     * An ETag is considered also an MD5 when:
     * <ul>
     * <li>The length is exactly 16 characters (excluding surrounding quote characters)</li>
     * <li>All characters in the string are hexadecimal values, i.e. [0-9a-f] when lowercased</li>
     * </ul>
     * <p>
     * These rules are drawn from the post by Carl@AWS on Nov 11, 2010 10:40 AM here:
     * <a href="https://forums.aws.amazon.com/thread.jspa?messageID=222158&tstart=0"
     *   >https://forums.aws.amazon.com/thread.jspa?messageID=222158&amp;tstart=0</a>
     *
     * @return
     * true if the ETag value can be assumed to also be an MD5 hash.
     */
    public static boolean isEtagAlsoAnMD5Hash(String etag) {
        if (etag == null || etag.length() != 32) {
            return false;
        }
        String nonHexChars = etag.toLowerCase().replaceAll("[a-f0-9]", "");
        if (nonHexChars.length() > 0) {
            return false;
        }
        return true;
    }

    /**
     * Identifies the name of a bucket from a given host name, if available.
     * Returns null if the bucket name cannot be identified, as might happen
     * when a bucket name is represented by the path component of a URL instead
     * of the host name component.
     *
     * @param host
     * the host name component of a URL that may include the bucket name,
     * if an alternative host name is in use.
     *
     * @return
     * The S3 bucket name represented by the DNS host name, or null if none.
     */
    public static String findBucketNameInHostname(String host, String s3Endpoint) {
        String bucketName = null;
        // Bucket name is available in URL's host name.
        if (host.endsWith(s3Endpoint)) {
            // Bucket name is available as S3 subdomain
            bucketName = host.substring(0,
                host.length() - s3Endpoint.length() - 1);
        } else {
            // URL refers to a virtual host name
            bucketName = host;
        }
        return bucketName;
    }

    /**
     * Find the name of a bucket referred to by a HTTP request's Host or
     * Path URI components. Returns null if the bucket name cannot be found,
     * which is a legitimate outcome for requests that are not directed at a
     * bucket (e.g. service root-level requests).
     *
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     *
     * @param uri
     * URI containing the request's Host domain name target
     * (via {@link URI#getHost()}) and Path component (via {@link URI#getPath()})
     * @param s3Endpoint
     * Host name of default S3 endpoint, generally "s3.amazonaws.com" but may
     * differ when using alternative services such as Eucalyptus Walrus etc.
     * @return the bucket name target of the request's Host/Path, or null if none.
     */
    public static String findBucketNameInHostOrPath(URI uri, String s3Endpoint)
    {
        String host = uri.getHost();
        String path = uri.getPath();
        String[] pathSplit = ServiceUtils.splitIgnoreEmpty(path, "/");

        // Handle case where Host exactly matches endpoint, so no chance of it
        // being a virtual host or alternate host name
        if (host.equalsIgnoreCase(s3Endpoint)) {
            // If we have multiple path components the first is the bucket name...
            if (pathSplit.length > 0) {
                return pathSplit[0];
            }
            // ...otherwise no bucket name
            return null;
        }

        // Handle case where bucket name is a prefix of the exact Host endpoint,
        // e.g. bucketname.s3.amazonaws.com
        if (host.endsWith("." + s3Endpoint)) {
            return host.substring(0, host.length() - s3Endpoint.length() - 1);
        }

        // Handle complicated case where bucket name is a prefix of some
        // variation of a suffix of the Host endpoint. For example AWS region
        // endpoint variations on s3.amazonaws.com include:
        //
        //   bucketname.s3-external-1.amazonaws.com
        //   bucketname.s3-us-west-1.amazonaws.com
        //   bucketname.s3-ap-northeast-1.amazonaws.com
        //   etc etc
        //   bucketname.s3-eu-central-1.amazonaws.com
        //   bucketname.s3.eu-central-1.amazonaws.com  (Ugh!)
        //
        // We assume region-based Host prefixes will only impact AWS-style
        // endpoints with 3+ components, where only the first endpoint Host
        // component will vary. For example:
        //   bucketname.[s3-external-1].amazonaws.com <=> bucketname.[s3].amazonaws.com
        String[] hostSplit = ServiceUtils.splitIgnoreEmpty(host, "\\.");
        String[] s3EndpointSplit = ServiceUtils.splitIgnoreEmpty(s3Endpoint, "\\.");
        String s3EndpointSuffix = null;
        if (s3EndpointSplit.length >= 3) {
            s3EndpointSuffix = ServiceUtils.join(
                Arrays.copyOfRange(s3EndpointSplit, 1, s3EndpointSplit.length),
                ".");
        }

        if (s3EndpointSuffix != null && host.endsWith("." + s3EndpointSuffix)
            // The request Host must have at least one more name component than
            // the endpoint, or there's definitely no bucketname Host component
            && hostSplit.length > s3EndpointSplit.length)
        {
            // Does this region variation look reasonable? Where reasonable
            // means it starts with a hyphen-delimited version of the endpoint's
            // first component, e.g.
            // [s3-external-1].amazonaws.com <=> [s3].amazonaws.com
            int firstNonEndpointHostComponentOffset = -1;
            String firstS3EndpointComponent = s3EndpointSplit[0];
            String regionVariation = hostSplit[hostSplit.length - s3EndpointSplit.length];
            if (regionVariation.equals(firstS3EndpointComponent)
                || regionVariation.startsWith(firstS3EndpointComponent + "-"))
            {
                firstNonEndpointHostComponentOffset =
                    hostSplit.length - s3EndpointSplit.length;
            }

            // Handle awkward unusual naming convention for AWS Host region
            // variation which may include multiple components, e.g.
            // s3.eu-central-1
            if (firstNonEndpointHostComponentOffset < 0
                && hostSplit.length > s3EndpointSplit.length + 1)
            {
                regionVariation = hostSplit[hostSplit.length - s3EndpointSplit.length - 1];
                if (regionVariation.equals(firstS3EndpointComponent)
                    || regionVariation.startsWith(firstS3EndpointComponent + "-"))
                {
                    firstNonEndpointHostComponentOffset =
                        hostSplit.length - s3EndpointSplit.length - 1;
                }
            }

            if (firstNonEndpointHostComponentOffset > 0) {
                String bucketName = ServiceUtils.join(
                    Arrays.copyOfRange(hostSplit, 0, firstNonEndpointHostComponentOffset),
                    ".");
                return bucketName;
            }
        }

        // Handle case where a DNS virtual host is the bucket name
        // i.e. my.bucket.com => my.bucket.com.s3.amazonaws.com
        // We assume this is the case if the Host doesn't match even the suffix
        // of our endpoint.
        if (s3EndpointSuffix != null && !host.endsWith("." + s3EndpointSuffix)) {
            String bucketName = host;
            return bucketName;
        }

        // If we get this far we haven't detected the bucket name in the Host
        // at all, so we assume the first /-delimited portion of the Path is
        // the bucket name, if any
        if (pathSplit.length > 0) {
            return pathSplit[0];
        }

        // No bucket name found
        return null;
    }

    /**
     * Builds an object based on the bucket name and object key information
     * available in the components of a URL.
     *
     * @param host
     * the host name component of a URL that may include the bucket name,
     * if an alternative host name is in use.
     * @param urlPath
     * the path of a URL that references an S3 object, and which may or may
     * not include the bucket name.
     *
     * @return
     * the object referred to by the URL components.
     */
    public static S3Object buildObjectFromUrl(String host, String urlPath, String s3Endpoint)
        throws UnsupportedEncodingException
    {
        if (urlPath.startsWith("/")) {
            urlPath = urlPath.substring(1); // Ignore first '/' character in url path.
        }

        String bucketName = null;
        String objectKey = null;

        if (!s3Endpoint.equals(host)) {
            bucketName = findBucketNameInHostname(host, s3Endpoint);
        } else {
            // Bucket name must be first component of URL path
            int slashIndex = urlPath.indexOf("/");
            bucketName = URLDecoder.decode(
                urlPath.substring(0, slashIndex), Constants.DEFAULT_ENCODING);

            // Remove the bucket name component of the host name
            urlPath = urlPath.substring(bucketName.length() + 1);
        }

        objectKey = URLDecoder.decode(
            urlPath, Constants.DEFAULT_ENCODING);

        S3Object object = new S3Object(objectKey);
        object.setBucketName(bucketName);
        return object;
    }

    /**
     * Returns true if the given bucket name can be used as a component of a valid
     * DNS name. If so, the bucket can be accessed using requests with the bucket name
     * as part of an S3 sub-domain. If not, the old-style bucket reference URLs must be
     * used, in which case the bucket name must be the first component of the resource
     * path.
     *
     * @param bucketName
     * the name of the bucket to test for DNS compatibility.
     */
    public static boolean isBucketNameValidDNSName(String bucketName) {
        if (bucketName == null || bucketName.length() > 63 || bucketName.length() < 3) {
            return false;
        }

        // Only lower-case letters, numbers, '.' or '-' characters allowed
        if (!Pattern.matches("^[a-z0-9][a-z0-9.-]+$", bucketName)) {
            return false;
        }

        // Cannot be an IP address, i.e. must not contain four '.'-delimited
        // sections with 1 to 3 digits each.
        if (Pattern.matches("([0-9]{1,3}\\.){3}[0-9]{1,3}", bucketName)) {
            return false;
        }

        // Components of name between '.' characters cannot start or end with '-',
        // and cannot be empty
        String[] fragments = bucketName.split("\\.");
        for (int i = 0; i < fragments.length; i++) {
            if (Pattern.matches("^-.*", fragments[i])
                || Pattern.matches(".*-$", fragments[i])
                || Pattern.matches("^$", fragments[i]))
            {
                return false;
            }
        }

        return true;
    }

    public static String generateS3HostnameForBucket(String bucketName,
        boolean isDnsBucketNamingDisabled, String s3Endpoint)
    {
        if (isBucketNameValidDNSName(bucketName) && !isDnsBucketNamingDisabled) {
            return bucketName + "." + s3Endpoint;
        } else {
            return s3Endpoint;
        }
    }

    /**
     * Returns a user agent string describing the jets3t library, and optionally the application
     * using it, to server-side services.
     *
     * @param applicationDescription
     * a description of the application using the jets3t toolkit, included at the end of the
     * user agent string. This value may be null.
     * @return
     * a string built with the following components (some elements may not be available):
     * <tt>JetS3t/</tt><i>{@link Constants#JETS3T_VERSION}</i>
     * (<i>os.name</i>/<i>os.version</i>; <i>os.arch</i>; <i>user.region</i>;
     * <i>user.region</i>; <i>user.language</i>) <i>applicationDescription</i>
     *
     */
    public static String getUserAgentDescription(String applicationDescription) {
        return
            "JetS3t/" + Constants.JETS3T_VERSION + " ("
            + System.getProperty("os.name") + "/"
            + System.getProperty("os.version") + ";"
            + " " + System.getProperty("os.arch")
            + (System.getProperty("user.region") != null
                ? "; " + System.getProperty("user.region")
                : "")
            + (System.getProperty("user.language") != null
                ? "; " + System.getProperty("user.language")
                : "")
            + (System.getProperty("java.version") != null
                ? "; JVM " + System.getProperty("java.version")
                : "")
            + ")"
            + (applicationDescription != null
                ? " " + applicationDescription
                : "");
    }

    /**
     * Find a SAX XMLReader by hook or by crook, with work-arounds for
     * non-standard platforms.
     *
     * @return an initialized XML SAX reader
     */
    public static XMLReader loadXMLReader() throws ServiceException {
        // Try loading the default SAX reader
        try {
            return XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            // Ignore failure
        }

        // No dice using the standard approach, try loading alternatives...
        String[] altXmlReaderClasspaths = new String[] {
            "org.apache.crimson.parser.XMLReaderImpl",  // JDK 1.4
            "org.xmlpull.v1.sax2.Driver",  // Android
        };
        for (int i = 0; i < altXmlReaderClasspaths.length; i++) {
            String xmlReaderClasspath = altXmlReaderClasspaths[i];
            try {
                return XMLReaderFactory.createXMLReader(xmlReaderClasspath);
            } catch (SAXException e) {
                // Ignore failure
            }
        }
        // If we haven't found and returned an XMLReader yet, give up.
        throw new ServiceException("Failed to initialize a SAX XMLReader");
    }

    /**
     * Take the input we're given and wrap at the user-defined intervals
     *
     * @param p_Input The string to be modified by the line wrap.
     * @param p_Prefix a prefix to prebend to the output string
     * @param p_Len The maximum number of characters per line
     * @return The new string that contains the extra new-line escapes.
     */
    public static String wrapString(String p_Input, String p_Prefix, int p_Len) {
      if (p_Input==null){
        return "";
      }
      String in = p_Input.replace('\\', '/');
      boolean replaced = !in.equals(p_Input);
      String output = wrapString( p_Input, p_Prefix, p_Len, " /_");
      return replaced ? output.replace('/', '\\') : output;
    }

    /**
     * Take the input we're given and wrap at the user-defined intervals
     *
     * @param p_Input The string to be modified by the line wrap.
     * @param p_Prefix a prefix to prebend to the output string
     * @param p_Len The maximum number of characters per line
     * @param p_Delims are the characters on which wrapping is allowed
     * @return The new string that contains the extra new-line escapes.
     */
    public static String wrapString(
            String p_Input,
            String p_Prefix,
            int p_Len,
            String p_Delims) {
      if (p_Input==null){
        return "";
      }
      String temp;
        StringBuilder output = new StringBuilder();
      StringBuffer workBuf = new StringBuffer();

      StringTokenizer strTok = new StringTokenizer(p_Input, p_Delims, true);

      while (strTok.hasMoreTokens()) {
        temp = strTok.nextToken();

        if ((workBuf.length() + temp.length()) >= p_Len) {
          if (p_Prefix != null) {
            output.append(p_Prefix);
          }
          output.append(workBuf.toString());
          output.append("\n");
          workBuf = new StringBuffer();

          // Just to make things look a little nicer, we'll see if this
          // element starts with a space and lop it off if so.
          if (temp.startsWith(" ")) {

            int tempLen = temp.length();

            if (tempLen > 1) {
              temp = temp.substring(1, temp.length() - 1);
            } else {
              temp = "";
            }
          }
        }

        workBuf.append(temp);
      }

      // Now catch the last little bit of our work buffer
      if (p_Prefix != null) {
        output.append(p_Prefix);
      }
      output.append(workBuf.toString());
      return output.toString();
    }

}
