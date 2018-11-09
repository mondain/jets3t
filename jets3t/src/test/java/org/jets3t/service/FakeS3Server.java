/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty
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
package org.jets3t.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Locale;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.apache.commons.codec.binary.Base64;
import org.jets3t.service.security.AWSCredentials;

/**
 * Very basic implementation of an S3 server-side stub that can fake certain S3 interactions
 * including:
 * <ul>
 * <li>Logging in using S3-stored credentials (passphrase/password=please/please)</li>
 * <li>Listing buckets</li>
 * <li>Listing the contents of an empty bucket</li>
 * <li>Allowing for PUT uploads, with generation and comparison of an MD5 digest for data received</li>
 * </ul>
 *
 * @author James Murty
 *
 */
public class FakeS3Server {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        AWSCredentials fakeAwsCredentials = new AWSCredentials("fake-aws-access-key", "fake-aws-secret-key");

        int port = 443;
        ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
        ServerSocket ssocket = ssocketFactory.createServerSocket(port);
        System.out.println("Accepting connections on port 443");

        while (port == 443) {
            // Listen for connections
            Socket socket = ssocket.accept();
            System.out.println("Opened connection");

            // Create streams to securely send and receive data to the client
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            byte[] buffer = new byte[1024];
            int read;

            while ((read = in.read(buffer)) != -1) {
                String receivedDataStr = new String(buffer, 0, read);
                String requestActionAndHeaders =
                    receivedDataStr.substring(0, receivedDataStr.indexOf("\r\n\r\n") + 4);

                System.out.println(requestActionAndHeaders);

                if (requestActionAndHeaders.startsWith("GET")) {
                    String path = requestActionAndHeaders.substring(4,
                        requestActionAndHeaders.indexOf(' ', 4));

                    if (path.startsWith("/jets3t-")) {
                        // Return fake AWS credentials.
                        String headers =
                            "HTTP/1.1 200 OK\r\n" +
                            "x-amz-id-2: FakeAWSCredentials\r\n" +
                            "x-amz-request-id: FakeAWSCredentials\r\n" +
                            "Date: Thu, 24 May 2007 13:39:21 GMT\r\n" +
                            "Cache-Control: max-age=259200\r\n" +
                            "Last-Modified: Wed, 27 Dec 2006 02:37:58 GMT\r\n" +
                            "ETag: \"fa5d6b0ea9716cf692b286b6aa187f3d\"\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "Content-Length: 139\r\n" +
                            "Server: AmazonS3\r\n\r\n";

                        out.write(headers.getBytes("UTF-8"));
                        fakeAwsCredentials.save("please", out);
                    }

                    else if (path.equals("/")) {
                        // Return fake bucket listing.
                        String headers =
                            "HTTP/1.1 200 OK\r\n" +
                            "x-amz-id-2: FakeBucketListing\r\n" +
                            "x-amz-request-id: FakeBucketListing\r\n" +
                            "Date: Thu, 24 May 2007 13:39:23 GMT\r\n" +
                            "Content-Type: application/xml\r\n" +
                            "Transfer-Encoding: chunked\r\n" +
                            "Server: AmazonS3\r\n\r\n";

                        String bucketListing =
                            "17b\r\n" +
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<ListAllMyBucketsResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                            "<Owner><ID>1a405254c932b52e5b5caaa88186bc431a1bacb9ece631f835daddaf0c47677c</ID>" +
                            "<DisplayName>jamesmurty</DisplayName></Owner>"+
                            "<Buckets><Bucket><Name>TestUploadBucket</Name>" +
                            "<CreationDate>2006-12-13T21:21:14.000Z</CreationDate>" +
                            "</Bucket></Buckets></ListAllMyBucketsResult>" +
                            "\r\n0\r\n\r\n";

                        out.write(headers.getBytes("UTF-8"));
                        out.write(bucketListing.getBytes("UTF-8"));
                    }

                    else if (path.startsWith("/TestUploadBucket")) {
                        // Return empty bucket contents

                        String headers =
                            "HTTP/1.1 200 OK\r\n" +
                            "x-amz-id-2: FakeBucketContents\r\n" +
                            "x-amz-request-id: FakeBucketContents\r\n" +
                            "Date: Thu, 24 May 2007 13:39:23 GMT\r\n" +
                            "Content-Type: application/xml\r\n" +
                            "Transfer-Encoding: chunked\r\n" +
                            "Server: AmazonS3\r\n\r\n";

                        String bucketContents =
                            "f2\r\n" +
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<ListBucketResult xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">" +
                            "<Name>TestUploadBucket</Name><Prefix></Prefix><Marker></Marker>" +
                            "<MaxKeys>1000</MaxKeys><IsTruncated>false</IsTruncated>" +
                            "</ListBucketResult>" +
                            "\r\n0\r\n\r\n";

                        out.write(headers.getBytes("UTF-8"));
                        out.write(bucketContents.getBytes("UTF-8"));
                    }

                    else {
                        System.out.println("ERROR: Unrecognised GET request");
                    }

                } else if (requestActionAndHeaders.startsWith("PUT")) {
                    long contentLength = 0;
                    String clientProvidedHash = "NONE";

                    // Determine content length.
                    int searchIndex = requestActionAndHeaders.indexOf("Content-Length: ") + "Content-Length: ".length();
                    contentLength = (new Long(requestActionAndHeaders.substring(searchIndex, requestActionAndHeaders.indexOf('\r', searchIndex)))).longValue();

                    // Determine content MD5 (hex encoded).
                    searchIndex = requestActionAndHeaders.indexOf("Content-MD5: ") + "Content-MD5: ".length();
                    if (searchIndex >= -1) {
                        clientProvidedHash = requestActionAndHeaders.substring(
                            searchIndex, requestActionAndHeaders.indexOf('\r', searchIndex));
                    }

                    // Read all PUT data provided by client, generating an MD5 hash as we go.
                    System.out.println("Receiving " + contentLength + " bytes from client");
                    MessageDigest digest = MessageDigest.getInstance("MD5");

                    long putdataAlreadyRead = read - requestActionAndHeaders.length(); // read - (requestActionAndHeaders.lastIndexOf("\r\n") + 2);
                    digest.update(buffer, (int)(read - putdataAlreadyRead), (int)putdataAlreadyRead);

                    byte[] putdata = new byte[8192];
                    int putdataRead = 0;
                    while ((putdataRead = in.read(putdata)) != -1) {
                        digest.update(putdata, 0, putdataRead);
                        putdataAlreadyRead += putdataRead;

                        if (putdataAlreadyRead == contentLength) {
                            System.out.println("PUT object upload is complete");
                            break;
                        }
                    }

                    if (putdataAlreadyRead != contentLength) {
                        System.err.println("ERROR: Expected " + contentLength + " bytes but received " + putdataAlreadyRead);
                        continue;
                    }

                    String receivedDataHashAsHex = new String(Base64.encodeBase64(digest.digest()), "UTF-8");

                    // Generate the headers appropriate for the PUT object.
                    String headers =
                        "HTTP/1.1 200 OK\r\n" +
                        "x-amz-id-2: FakePUT\r\n" +
                        "x-amz-request-id: FakePUT\r\n" +
                        "Date: Thu, 24 May 2007 15:12:30 GMT\r\n" +
                        "ETag: \"" + receivedDataHashAsHex + "\"\r\n" +
                        "Content-Length: 0\r\n" +
                        "Server: AmazonS3\r\n\r\n";
                    out.write(headers.getBytes("UTF-8"));
                    out.flush();

                    // Compare expected hash (supplied by client) verses actual hash (for retrieved data)
                    if (!receivedDataHashAsHex.equals(clientProvidedHash)) {
                        System.err.println("ERROR: Client-side hash " + clientProvidedHash
                            + " does not match hash of received data " + receivedDataHashAsHex);
                    } else {
                        System.out.println("SUCCESS: Client-side hash matches hash of received data: " + receivedDataHashAsHex);
                    }

                } else {
                    System.out.println("ERROR: Unrecognised input");
                }
            }
            // Close the socket
            System.out.println("Closing connection");
            in.close();
            out.close();
        }
    }

    public static void writeFileToOutputStream(File file, OutputStream out) throws Exception {
        byte[] buffer = new byte[1024];
        int read;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    public static String toHex(byte[] data) {
        StringBuffer sb = new StringBuffer(data.length * 2);
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
        return sb.toString().toLowerCase(Locale.getDefault());
    }

}
