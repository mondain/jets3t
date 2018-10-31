/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2014 James Murty
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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.management.RuntimeErrorException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.jets3t.service.impl.rest.httpclient.RepeatableRequestEntity;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.security.ProviderCredentials;

/**
 * Utility methods for signing HTTP requests, mainly for newer signature
 * versions as legacy implementations remain in {@link RestUtils}.
 *
 * @author jmurty
 */
public class SignatureUtils {

    protected static final SimpleDateFormat awsFlavouredISO8601DateParser =
        new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    static {
        awsFlavouredISO8601DateParser.setTimeZone(new SimpleTimeZone(0, "GMT"));
    }

    /**
     * @param date
     * @return date formatted as AWS-flavoured ISO8601
     */
    public static String formatAwsFlavouredISO8601Date(Date date) {
        synchronized (awsFlavouredISO8601DateParser) {
            return awsFlavouredISO8601DateParser.format(date);
        }
    }

    /**
     *
     * @param dateString
     * date string representation that is hopefully AWS-flavoured ISO8601
     * @return date parsed from AWS-flavoured ISO8601
     * @throws ParseException
     */
    public static Date parseAwsFlavouredISO8601Date(String dateString)
        throws ParseException
    {
        synchronized (awsFlavouredISO8601DateParser) {
            return awsFlavouredISO8601DateParser.parse(dateString);
        }
    }

    /**
     * Determine the AWS Region to which a request will be sent based on the
     * request's Host endpoint. See
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     *
     * @param requestURI
     * @return AWS region name corresponding to the request's Host endpoint.
     */
    public static String awsRegionForRequest(URI requestURI) {
        String host = requestURI.getHost().toLowerCase();
        // Recognise default/legacy endpoints where the Host does not
        // correspond to the region name.
        if (host.endsWith("s3.amazonaws.com")
            || host.endsWith("s3-external-1.amazonaws.com"))
        {
            return null;
        }
        // Host names of the following forms include the region name as a
        // component of the Host name:
        //   s3-<regionName>.amazonaws.com
        //   s3.<regionName>.amazonaws.com
        //   s3.<regionName>.amazonaws.com.cn
        else if (host.endsWith(".amazonaws.com")
                 || host.contains(".amazonaws.com."))
        {
            String[] hostSplit = host.split("\\.");
            // Work forwards from start of host name to find the portion
            // immediately preceding the "amazonaws" part, which will either be
            // a region name or can be converted into a region name by removing
            // a "s3-" prefix.
            String regionNameCandidate = null;
            boolean wasS3PrefixFound = false;
            for (String portion: hostSplit) {
                if (portion.equals("amazonaws")) {
                    break;
                } else if (portion.equals("s3")) {
                    wasS3PrefixFound = true;
                }
                regionNameCandidate = portion;
            }
            if (null == regionNameCandidate) {
                return null;
            }
            if (regionNameCandidate.startsWith("s3-")) {
                return regionNameCandidate.substring("s3-".length());
            } else if (wasS3PrefixFound) {
                return regionNameCandidate;
            }
        }
        // No specific Host-to-region mappings available
        return null;
    }

    /**
     * Calculate AWS Version 4 signature for a HTTP request and apply the
     * appropriate "Authorization" header value to authorize it.
     *
     * @param httpMethod
     * the request's HTTP method just prior to sending
     * @param requestSignatureVersion
     * request signature version string, e.g. "AWS4-HMAC-SHA256"
     * @param providerCredentials
     * account holder's access and secret key credentials
     * @param requestPayloadHexSha256Hash
     * hex-encoded SHA256 hash of request's payload.
     * @param region
     * region to which the request will be sent
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     */
    public static void awsV4SignRequestAuthorizationHeader(
        String requestSignatureVersion, HttpUriRequest httpMethod,
        ProviderCredentials providerCredentials,
        String requestPayloadHexSha256Hash, String region)
    {
        // Ensure the required Host header is set prior to signing.
        if (httpMethod.getFirstHeader("Host") == null) {
            httpMethod.setHeader("Host", httpMethod.getURI().getHost());
        }

        // Generate AWS-flavoured ISO8601 timestamp string
        String timestampISO8601 = SignatureUtils.awsV4ParseAndFormatDate(
            httpMethod);

        // Apply AWS-flavoured ISO8601 timestamp string to "x-aws-date"
        // metadata, otherwise if only the Date header is present and it is
        // RFC 822 formatted S3 expects that date to be part of the string
        // to sign, not the AWS-flavoured ISO8601 timestamp as claimed by the
        // documentation.
        // TODO This shouldn't be necessary, confirm it really is...
        if (httpMethod.getFirstHeader("x-amz-date") == null) {
            httpMethod.setHeader("x-amz-date", timestampISO8601);
        }

        // Canonical request string
        String canonicalRequestString =
            SignatureUtils.awsV4BuildCanonicalRequestString(
                httpMethod, requestPayloadHexSha256Hash);

        // String to sign
        String stringToSign = SignatureUtils.awsV4BuildStringToSign(
            requestSignatureVersion, canonicalRequestString,
            timestampISO8601, region);

        // Signing key
        byte[] signingKey = SignatureUtils.awsV4BuildSigningKey(
            providerCredentials.getSecretKey(), timestampISO8601,
            region);

        // Request signature
        String signature = ServiceUtils.toHex(ServiceUtils.hmacSHA256(
            signingKey, ServiceUtils.stringToBytes(stringToSign)));

        // Authorization header value
        String authorizationHeaderValue =
            SignatureUtils.awsV4BuildAuthorizationHeaderValue(
                providerCredentials.getAccessKey(), signature,
                requestSignatureVersion, canonicalRequestString,
                timestampISO8601, region);

        httpMethod.setHeader("Authorization", authorizationHeaderValue);
    }

    /**
     * Return SHA256 payload hash value already set on HTTP request, or if none
     * is yet set calculate this value if possible.
     *
     * @param httpMethod
     * the request's HTTP method just prior to sending
     * @return hex-encoded SHA256 hash of payload data.
     */
    public static String awsV4GetOrCalculatePayloadHash(HttpUriRequest httpMethod) {
        // Lookup and return request payload SHA256 hash if present
        String requestPayloadHexSHA256Hash = null;
        Header sha256Header = httpMethod.getFirstHeader("x-amz-content-sha256");
        if (sha256Header != null) {
            return sha256Header.getValue();
        }

        // If request payload SHA256 isn't available, check for a payload
        if (httpMethod instanceof HttpEntityEnclosingRequest)
        {
            HttpEntity entity =
                ((HttpEntityEnclosingRequest)httpMethod).getEntity();
            // We will automatically generate the SHA256 hash for a limited
            // set of payload entities, and bail out early for the
            // unsupported ones.
            if (entity instanceof StringEntity
                || entity instanceof ByteArrayEntity
                || entity instanceof RepeatableRequestEntity)
            {
                try {
                    // Hack to get to underlying input stream if this has been
                    // wrapped by JetS3t's ProgressMonitoredInputStream, since
                    // the caller didn't intend to monitor the progress of this
                    // last-ditch effort to calculate a SHA256 hash.
                    InputStream requestIS = entity.getContent();
                    while (requestIS instanceof ProgressMonitoredInputStream) {
                        requestIS = ((ProgressMonitoredInputStream)requestIS)
                            .getWrappedInputStream();
                    }

                    requestPayloadHexSHA256Hash = ServiceUtils.toHex(
                        ServiceUtils.hashSHA256(
                            requestIS,
                            true  // resetInsteadOfClose - reset don't close
                        )
                    );

                    requestIS.reset();
                } catch (IOException e) {
                    throw new RuntimeException(
                        "Failed to automatically set required header"
                        + " \"x-amz-content-sha256\" for request with"
                        + " entity " + entity, e);
                }
            }
            // For unsupported payload entities bail out with a (hopefully)
            // useful error message.
            // We don't want to do too much automatically because it could
            // kill performance, without the reason being clear to users.
            else if (entity != null){
                throw new RuntimeException(
                    "Header \"x-amz-content-sha256\" set to the hex-encoded"
                    + " SHA256 hash of the request payload is required for"
                    + " AWS Version 4 request signing, please set this on: "
                    + httpMethod);
            }
        }

        if (requestPayloadHexSHA256Hash == null) {
            // If no payload, we set the SHA256 hash of an empty string.
            requestPayloadHexSHA256Hash =
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        }
        return requestPayloadHexSHA256Hash;
    }

    /**
     * Extract the request timestamp from the given HTTP request, from either
     * the "x-amz-date" metadata header or the Date header, and convert it
     * into an AWS-flavoured ISO8601 string format suitable for us in
     * request authorization for AWS version 4 signatures.
     *
     * @param httpMethod
     * request containing at least one of the "x-amz-date" or Date headers with
     * a timestamp value in one of the supported formats: RFC 822, ISO 8601,
     * AWS-flavoured ISO 8601.
     * @return timestamp formatted as AWS-flavoured ISO8601: "YYYYMMDDTHHmmssZ"
     */
    public static String awsV4ParseAndFormatDate(HttpUriRequest httpMethod) {
        // Retrieve request's date header, from locations in order of
        // preference: explicit metadata date, request Date header
        Header dateHeader = httpMethod.getFirstHeader("x-amz-date");
        if (dateHeader == null) {
            dateHeader = httpMethod.getFirstHeader("Date");
        }
        if (dateHeader == null) {
            throw new RuntimeException(
                "Request must have a date timestamp applied before it can be"
                + " signed with AWS Version 4, but no date value found in"
                + " \"x-amz-date\" or \"Date\" headers");
        }

        // Parse provided Date object or string into ISO8601 format timestamp
        String dateValue = dateHeader.getValue();
        if (dateValue.endsWith("Z")) {
            // ISO8601-like date, does it need to be converted to AWS flavour?
            try {
                parseAwsFlavouredISO8601Date(dateValue);
                // Parse succeeded, no more work necessary
                return dateValue;
            } catch (ParseException e) {
                // Parse failed, try parsing normal ISO8601 format
                try {
                    Date date = ServiceUtils.parseIso8601Date(dateValue);
                    return formatAwsFlavouredISO8601Date(date);
                } catch (ParseException e2) {
                    throw new RuntimeException(
                        "Invalid date value in request: " + dateValue, e2);
                }
            }
        } else {
            try {
                Date date = ServiceUtils.parseRfc822Date(dateValue);
                return formatAwsFlavouredISO8601Date(date);
            } catch (ParseException e) {
                throw new RuntimeException(
                    "Invalid date value in request: " + dateValue, e);
            }
        }
    }

    /**
     * Build the canonical request string for a REST/HTTP request to a storage
     * service for the AWS Request Signature version 4.
     *
     * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
     *
     * @param httpMethod
     * the request's HTTP method just prior to sending
     * @param requestPayloadHexSha256Hash
     * hex-encoded SHA256 hash of request's payload.
     * May be null or "" in which case the default SHA256 hash of an empty string is used.
     * May also be "UNSIGNED-PAYLOAD" for generating pre-signed request signatures.
     * @return canonical request string according to AWS Request Signature version 4
     */
    public static String awsV4BuildCanonicalRequestString(
        HttpUriRequest httpMethod, String requestPayloadHexSha256Hash)
    {
        URI uri = httpMethod.getURI();
        String httpRequestMethod = httpMethod.getMethod();

        Map<String, String> headersMap = new HashMap<String, String>();
        Header[] headers = httpMethod.getAllHeaders();
        for (Header header: headers) {
            // Trim whitespace and make lower-case for header names
            String name = header.getName().trim().toLowerCase();
            // Trim whitespace for header values
            String value = header.getValue().trim();
            headersMap.put(name, value);
        }

        return awsV4BuildCanonicalRequestString(
            uri, httpRequestMethod, headersMap, requestPayloadHexSha256Hash);
    }

    /**
     * Build the canonical request string for a REST/HTTP request to a storage
     * service for the AWS Request Signature version 4.
     *
     * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
     *
     * @param uri
     * @param httpMethod
     * the request's HTTP method just prior to sending
     * @param headersMap
     * @param requestPayloadHexSha256Hash
     * hex-encoded SHA256 hash of request's payload. May be null or "" in
     * which case the default SHA256 hash of an empty string is used.
     * @return canonical request string according to AWS Request Signature version 4
     */
    public static String awsV4BuildCanonicalRequestString(
        URI uri, String httpMethod, Map<String, String> headersMap,
        String requestPayloadHexSha256Hash)
    {
        StringBuilder canonicalStringBuf = new StringBuilder();

        // HTTP Request method: GET, POST etc
        canonicalStringBuf
            .append(httpMethod)
            .append("\n");

        // Canonical URI: URI-encoded version of the absolute path
        String absolutePath = uri.getPath();
        if (absolutePath.length() == 0) {
            canonicalStringBuf.append("/");
        } else {
            canonicalStringBuf.append(
                SignatureUtils.awsV4EncodeURI(absolutePath, false));
        }
        canonicalStringBuf.append("\n");

        // Canonical query string
        String query = uri.getQuery();
        if (query == null || query.length() == 0) {
            canonicalStringBuf.append("\n");
        } else {
            // Parse and sort query parameters and values from query string
            SortedMap<String, String> sortedQueryParameters =
                new TreeMap<String, String>();
            for (String paramPair: query.split("&")) {
                String[] paramNameValue = paramPair.split("=");
                String name = paramNameValue[0];
                String value = "";
                if (paramNameValue.length > 1) {
                    value = paramNameValue[1];
                }
                // Add parameters to sorting map, URI-encoded appropriately
                sortedQueryParameters.put(
                    SignatureUtils.awsV4EncodeURI(name, true),
                    SignatureUtils.awsV4EncodeURI(value, true));
            }
            // Add query parameters to canonical string
            boolean isPriorParam = false;
            for (Map.Entry<String, String> entry: sortedQueryParameters.entrySet()) {
                if (isPriorParam) {
                    canonicalStringBuf.append("&");
                }
                canonicalStringBuf
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
                isPriorParam = true;
            }
            canonicalStringBuf.append("\n");
        }

        // Canonical Headers
        SortedMap<String, String> sortedHeaders = new TreeMap<String, String>();
        sortedHeaders.putAll(headersMap);
        for (Map.Entry<String, String> entry: sortedHeaders.entrySet()) {
            canonicalStringBuf
                .append(entry.getKey())
                .append(":")
                .append(entry.getValue())
                .append("\n");
        }
        canonicalStringBuf.append("\n");

        // Signed headers
        boolean isPriorSignedHeader = false;
        for (Map.Entry<String, String> entry: sortedHeaders.entrySet()) {
            if (isPriorSignedHeader) {
                canonicalStringBuf.append(";");
            }
            canonicalStringBuf.append(entry.getKey());
            isPriorSignedHeader = true;
        }
        canonicalStringBuf.append("\n");

        // Hashed Payload.
        canonicalStringBuf
            .append(requestPayloadHexSha256Hash);

        return canonicalStringBuf.toString();
    }

    /**
     * Build the string to sign for a REST/HTTP request to a storage
     * service for the AWS Request Signature version 4.
     *
     * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
     *
     * @param requestSignatureVersion
     * request signature version string, e.g. "AWS4-HMAC-SHA256"
     * @param canonicalRequestString
     * canonical request string as generated by {@link #awsV4BuildCanonicalRequestString(HttpUriRequest, String)}
     * @param timestampISO8601
     * timestamp of request creation in ISO8601 format
     * @param region
     * region to which the request will be sent
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     * @return string to sign according to AWS Request Signature version 4
     */
    public static String awsV4BuildStringToSign(
            String requestSignatureVersion, String canonicalRequestString,
            String timestampISO8601, String region)
    {
        String service = "s3";
        String datestampISO8601 = timestampISO8601.substring(0, 8); // TODO
        String credentialScope =
            datestampISO8601 + "/" + region + "/" + service + "/aws4_request";
        String hashedCanonicalString = ServiceUtils.toHex(
            ServiceUtils.hash(canonicalRequestString, "SHA-256"));

        return requestSignatureVersion + "\n"
        + timestampISO8601 + "\n"
        + credentialScope + "\n"
        + hashedCanonicalString;
    }

    /**
     * Build the signing key for a REST/HTTP request to a storage
     * service for the AWS Request Signature version 4.
     *
     * @param secretAccessKey
     * account holder's secret access key
     * @param timestampISO8601
     * timestamp of request creation in ISO8601 format
     * @param region
     * region to which the request will be sent
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     * @return signing key according to AWS Request Signature version 4
     */
    public static byte[] awsV4BuildSigningKey(
            String secretAccessKey, String timestampISO8601, String region)
    {
        String service = "s3";
        String datestampISO8601 = timestampISO8601.substring(0, 8);
        byte[] kDate = ServiceUtils.hmacSHA256(
            "AWS4" + secretAccessKey, datestampISO8601);
        byte[] kRegion = ServiceUtils.hmacSHA256(
            kDate, ServiceUtils.stringToBytes(region));
        byte[] kService = ServiceUtils.hmacSHA256(
            kRegion, ServiceUtils.stringToBytes(service));
        byte[] kSigning = ServiceUtils.hmacSHA256(
            kService, ServiceUtils.stringToBytes("aws4_request"));
        return kSigning;
    }

    /**
     * Build the Authorization header value for a REST/HTTP request to a storage
     * service for the AWS Request Signature version 4.
     *
     * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
     *
     * @param accessKey
     * account holder's access key
     * @param requestSignature
     * request signature as generated signing the string to sign from
     * {@link #awsV4BuildStringToSign(String, String, String, String)}
     * with the key from
     * {@link #awsV4BuildSigningKey(String, String, String)}
     * @param requestSignatureVersion
     * request signature version string, e.g. "AWS4-HMAC-SHA256"
     * @param canonicalRequestString
     * canonical request string as generated by
     * {@link #awsV4BuildCanonicalRequestString(HttpUriRequest, String)}
     * @param timestampISO8601
     * timestamp of request creation in ISO8601 format
     * @param region
     * region to which request will be sent, see
     * {@link "http://docs.aws.amazon.com/general/latest/gr/rande.html#s3_region"}
     * @return string to sign according to AWS Request Signature version 4
     */
    public static String awsV4BuildAuthorizationHeaderValue(
            String accessKey, String requestSignature,
            String requestSignatureVersion, String canonicalRequestString,
            String timestampISO8601, String region)
    {
        String service = "s3";
        String datestampISO8601 = timestampISO8601.substring(0, 8); // TODO
        // Parse signed headers back out of canonical request string
        String[] canonicalStringComponents = canonicalRequestString.split("\n");
        String signedHeaders = canonicalStringComponents[canonicalStringComponents.length - 2];

        String credentialScope =
            datestampISO8601 + "/" + region + "/" + service + "/aws4_request";

        return requestSignatureVersion + " "
        + "Credential=" + accessKey
        + "/" + credentialScope
        + ",SignedHeaders=" + signedHeaders
        + ",Signature=" + requestSignature;
    }

    /**
     * Replace the hostname of the given URI endpoint to match the given region.
     *
     * @param uri
     * @param region
     *
     * @return
     * URI with hostname that may or may not have been changed to be appropriate
     * for the given region. For example, the hostname "s3.amazonaws.com" is
     * unchanged for the "us-east-1" region but for the "eu-central-1" region
     * becomes "s3-eu-central-1.amazonaws.com".
     */
    public static URI awsV4CorrectHostnameForRegion(URI uri, String region) {
        String[] hostSplit = uri.getHost().split("\\.");
        if (region.equals("us-east-1")) {
            hostSplit[hostSplit.length - 3] = "s3";
        } else {
            hostSplit[hostSplit.length - 3] = "s3-" + region;
        }
        String newHost = ServiceUtils.join(hostSplit, ".");
        try {
            String rawPathAndQuery = uri.getRawPath();
            if (uri.getRawQuery() != null) {
                rawPathAndQuery += "?" + uri.getRawQuery();
            }
            return new URL(uri.getScheme(), newHost, uri.getPort(), rawPathAndQuery).toURI();
        } catch(URISyntaxException e) {
            throw new RuntimeException(e);
        } catch(MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Slightly modified version of "uri-encode" from:
     * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
     *
     * @param input
     * URI or URI-fragment string to encode.
     * @param encodeSlash
     * true if slash (/) character should be encoded.
     * @return URI string encoded per recommendations from AWS.
     */
    public static String awsV4EncodeURI(CharSequence input, boolean encodeSlash) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if ((ch >= 'A' && ch <= 'Z')
                || (ch >= 'a' && ch <= 'z')
                || (ch >= '0' && ch <= '9')
                || ch == '_'
                || ch == '-'
                || ch == '~'
                || ch == '.')
            {
                result.append(ch);
            } else if (ch == '/') {
                result.append(encodeSlash ? "%2F" : ch);
            } else {
                String hex = RestUtils.encodeUrlString(String.valueOf(ch));
                result.append(hex);
            }
        }
        return result.toString();
    }

}
