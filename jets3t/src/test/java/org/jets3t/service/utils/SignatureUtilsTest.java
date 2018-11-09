package org.jets3t.service.utils;

import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SignatureUtilsTest {

    @Test
    public void testAwsV4BuildCanonicalRequestString() throws Exception {
        final URI uri = new URI("" +
                "https://test-eu-central-1-mountainduck.s3-eu-central-1.amazonaws.com:443/?max-keys=1000&prefix=%26%2F&delimiter=%2F");
        final String method = "GET";
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("date", "Mon, 18 Jan 2016 15:41:49 GMT");
        headers.put("x-amz-content-sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        headers.put("x-amz-request-payer", "requester");
        headers.put("x-amz-date", "20160118T154149Z");
        headers.put("host", "test-eu-central-1-mountainduck.s3.amazonaws.com");

        final String signature = SignatureUtils.awsV4BuildCanonicalRequestString(uri, method, headers,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        assertEquals("GET\n" +
                        "/\n" +
                        "delimiter=%2F&max-keys=1000&prefix=%26%2F\n" +
                        "date:Mon, 18 Jan 2016 15:41:49 GMT\n" +
                        "host:test-eu-central-1-mountainduck.s3.amazonaws.com\n" +
                        "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                        "x-amz-date:20160118T154149Z\n" +
                        "x-amz-request-payer:requester\n" +
                        "\n" +
                        "date;host;x-amz-content-sha256;x-amz-date;x-amz-request-payer\n" +
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                signature);
    }

    @Test
    public void testBucketListRequestEmptyPrefix() throws Exception {
        final URI uri = new URI("" +
                "https://test-eu-central-1-mountainduck.s3.amazonaws.com:443/?max-keys=1000&prefix&delimiter=%2F");
        final String method = "GET";
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("date", "Mon, 18 Jan 2016 15:28:08 GMT");
        headers.put("x-amz-content-sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        headers.put("x-amz-request-payer", "requester");
        headers.put("x-amz-date", "20160118T152808Z");
        headers.put("host", "test-eu-central-1-mountainduck.s3.amazonaws.com");

        final String signature = SignatureUtils.awsV4BuildCanonicalRequestString(uri, method, headers,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        assertEquals("GET\n" +
                        "/\n" +
                        "delimiter=%2F&max-keys=1000&prefix=\n" +
                        "date:Mon, 18 Jan 2016 15:28:08 GMT\n" +
                        "host:test-eu-central-1-mountainduck.s3.amazonaws.com\n" +
                        "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                        "x-amz-date:20160118T152808Z\n" +
                        "x-amz-request-payer:requester\n" +
                        "\n" +
                        "date;host;x-amz-content-sha256;x-amz-date;x-amz-request-payer\n" +
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                signature);
    }

    @Test
    public void testBucketListRequestWithPrefix() throws Exception {
        final URI uri = new URI("" +
                "https://test-eu-central-1-mountainduck.s3-eu-central-1.amazonaws.com:443/?max-keys=1000&prefix=a%2F&delimiter=%2F");
        final String method = "GET";
        final Map<String, String> headers = new HashMap<String, String>();
        headers.put("date", "Mon, 18 Jan 2016 15:49:40 GMT");
        headers.put("x-amz-content-sha256", "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        headers.put("x-amz-request-payer", "requester");
        headers.put("x-amz-date", "20160118T154940Z");
        headers.put("host", "test-eu-central-1-mountainduck.s3-eu-central-1.amazonaws.com");

        final String signature = SignatureUtils.awsV4BuildCanonicalRequestString(uri, method, headers,
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        assertEquals("GET\n" +
                        "/\n" +
                        "delimiter=%2F&max-keys=1000&prefix=a%2F\n" +
                        "date:Mon, 18 Jan 2016 15:49:40 GMT\n" +
                        "host:test-eu-central-1-mountainduck.s3-eu-central-1.amazonaws.com\n" +
                        "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                        "x-amz-date:20160118T154940Z\n" +
                        "x-amz-request-payer:requester\n" +
                        "\n" +
                        "date;host;x-amz-content-sha256;x-amz-date;x-amz-request-payer\n" +
                        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                signature);
    }
}