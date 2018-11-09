package org.jets3t.service;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.SignatureUtils;
import org.junit.Test;


/**
 * {@link "http://docs.aws.amazon.com/AmazonS3/latest/API/sig-v4-header-based-auth.html"}
 */
public class TestAWSRequestSignatureVersion4 extends TestCase {
    protected String TEST_PROPERTIES_FILENAME = "test.properties";
    protected Properties testProperties = null;
    protected ProviderCredentials testCredentials = null;

    String awsAccessKey = "AKIAIOSFODNN7EXAMPLE";
    String awsSecretAccessKey = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    String timestampISO8601 = "20130524T000000Z";
    String bucketName = "examplebucket";
    String region = "us-east-1";
    String service = "s3";
    String requestSignatureVersion = "AWS4-HMAC-SHA256";

    public TestAWSRequestSignatureVersion4() throws Exception {
        // Load test properties
        InputStream propertiesIS =
            ClassLoader.getSystemResourceAsStream(TEST_PROPERTIES_FILENAME);
        if (propertiesIS == null) {
            throw new Exception(
                "Unable to load test properties file from classpath: "
                + TEST_PROPERTIES_FILENAME);
        }
        this.testProperties = new Properties();
        this.testProperties.load(propertiesIS);

        this.testCredentials = new AWSCredentials(
            testProperties.getProperty("aws.accesskey"),
            testProperties.getProperty("aws.secretkey"));
    }

    @Test
    public void testAwsRegionForRequest() throws URISyntaxException {
        assertEquals(
            null,
            SignatureUtils.awsRegionForRequest(new URI("http://www.amazon.com")));
        assertEquals(
            null,
            SignatureUtils.awsRegionForRequest(new URI("http://s3.amazonaws.com")));
        assertEquals(
            null,
            SignatureUtils.awsRegionForRequest(new URI("http://my.cname.s3.amazonaws.com")));
        assertEquals(
            null,
            SignatureUtils.awsRegionForRequest(new URI("http://s3-external-1.amazonaws.com")));
        assertEquals(
            "us-west-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-us-west-1.amazonaws.com")));
        assertEquals(
            "us-west-2",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-us-west-2.amazonaws.com")));
        assertEquals(
            "eu-west-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-eu-west-1.amazonaws.com")));
        assertEquals(
            "ap-southeast-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-ap-southeast-1.amazonaws.com")));
        assertEquals(
            "ap-southeast-2",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-ap-southeast-2.amazonaws.com")));
        assertEquals(
            "ap-northeast-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-ap-northeast-1.amazonaws.com")));
        assertEquals(
            "sa-east-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-sa-east-1.amazonaws.com")));
        assertEquals(
            "eu-central-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3-eu-central-1.amazonaws.com")));
        // NOTE: Unusual case with "s3." prefix instead of "s3-", likely to become more common
        assertEquals(
            "eu-central-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3.eu-central-1.amazonaws.com")));
        // NOTE: Test handling of upcoming (as yet undocumented) China region with unusual "amazonaws.com.cn" suffix
        assertEquals(
            "cn-north-1",
            SignatureUtils.awsRegionForRequest(new URI("http://s3.cn-north-1.amazonaws.com.cn")));
        // Test strangely capitalized host name
        assertEquals(
            null,
            SignatureUtils.awsRegionForRequest(new URI("http://S3.AMAZONAWS.COM")));
    }

    @Test
    public void testS3ApiReferenceExampleGetObject() {
        HttpGet httpGet = new HttpGet("http://examplebucket.s3.amazonaws.com/test.txt");
        httpGet.setHeader("Host", "examplebucket.s3.amazonaws.com");
        // NOTE: Date header missed in example test case
        httpGet.setHeader("Range", "bytes=0-9");
        httpGet.setHeader("x-amz-content-sha256",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        httpGet.setHeader("x-amz-date", this.timestampISO8601);

        // Default empty payload hash
        String requestPayloadHexSHA256Hash =
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // Canonical request string
        String expected =
            "GET\n" +
            "/test.txt\n" +
            "\n" +
            "host:examplebucket.s3.amazonaws.com\n" +
            "range:bytes=0-9\n" +
            "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
            "x-amz-date:20130524T000000Z\n" +
            "\n" +
            "host;range;x-amz-content-sha256;x-amz-date\n"+
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String canonicalRequestString = SignatureUtils.awsV4BuildCanonicalRequestString(
            httpGet, requestPayloadHexSHA256Hash);
        assertEquals(expected, canonicalRequestString);

        // String to sign
        expected =
            "AWS4-HMAC-SHA256\n" +
            "20130524T000000Z\n" +
            "20130524/us-east-1/s3/aws4_request\n" +
            "7344ae5b7ee6c3e7e6b0fe0640412a37625d1fbfff95c48bbb2dc43964946972";
        String stringToSign = SignatureUtils.awsV4BuildStringToSign(
            requestSignatureVersion, canonicalRequestString,
            this.timestampISO8601, this.region);
        assertEquals(expected, stringToSign);

        // Signature
        byte[] signingKey = SignatureUtils.awsV4BuildSigningKey(
            this.awsSecretAccessKey, this.timestampISO8601,
            this.region);
        String signature = ServiceUtils.toHex(
            ServiceUtils.hmacSHA256(
                signingKey, ServiceUtils.stringToBytes(stringToSign)));
        expected = "f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41";
        assertEquals(expected, signature);

        // Authorization header
        String authorizationHeaderValue =
            SignatureUtils.awsV4BuildAuthorizationHeaderValue(
                this.awsAccessKey, signature, this.requestSignatureVersion,
                canonicalRequestString, this.timestampISO8601, this.region);
        expected = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,SignedHeaders=host;range;x-amz-content-sha256;x-amz-date,Signature=f0e8bdb87c964420e857bd35b5d6ed310bd44f0170aba48dd91039c6036bdb41";
        assertEquals(expected, authorizationHeaderValue);

        // The whole request
        SignatureUtils.awsV4SignRequestAuthorizationHeader(
            this.requestSignatureVersion, httpGet,
            new AWSCredentials(this.awsAccessKey, this.awsSecretAccessKey),
            requestPayloadHexSHA256Hash, this.region);
        assertEquals(expected, httpGet.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void testS3ApiReferenceExamplePutObject() {
        HttpPut httpPut = new HttpPut("http://examplebucket.s3.amazonaws.com/test$file.text");
        httpPut.setHeader("Host", "examplebucket.s3.amazonaws.com");
        httpPut.setHeader("Date", "Fri, 24 May 2013 00:00:00 GMT");
        httpPut.setHeader("x-amz-date", this.timestampISO8601);
        httpPut.setHeader("x-amz-storage-class", "REDUCED_REDUNDANCY");

        String payload = "Welcome to Amazon S3.";
        httpPut.setEntity(new StringEntity(
            payload, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)));

        String requestPayloadHexSHA256Hash = ServiceUtils.toHex(
            ServiceUtils.hash(payload, "SHA-256"));
        httpPut.setHeader("x-amz-content-sha256", requestPayloadHexSHA256Hash);

        // Canonical request string
        String expected =
            "PUT\n" +
            "/test%24file.text\n" +
            "\n" +
            "date:Fri, 24 May 2013 00:00:00 GMT\n" +
            "host:examplebucket.s3.amazonaws.com\n" +
            "x-amz-content-sha256:44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072\n" +
            "x-amz-date:20130524T000000Z\n" +
            "x-amz-storage-class:REDUCED_REDUNDANCY\n" +
            "\n" +
            "date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class\n" +
            "44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072";
        String canonicalRequestString = SignatureUtils.awsV4BuildCanonicalRequestString(
            httpPut, requestPayloadHexSHA256Hash);
        assertEquals(expected, canonicalRequestString);

        // String to sign
        expected =
            "AWS4-HMAC-SHA256\n" +
            "20130524T000000Z\n" +
            "20130524/us-east-1/s3/aws4_request\n" +
            "9e0e90d9c76de8fa5b200d8c849cd5b8dc7a3be3951ddb7f6a76b4158342019d";
        String stringToSign = SignatureUtils.awsV4BuildStringToSign(
            requestSignatureVersion, canonicalRequestString,
            this.timestampISO8601, this.region);
        assertEquals(expected, stringToSign);

        // Signature
        byte[] signingKey = SignatureUtils.awsV4BuildSigningKey(
            this.awsSecretAccessKey, this.timestampISO8601,
            this.region);
        String signature = ServiceUtils.toHex(
            ServiceUtils.hmacSHA256(
                signingKey, ServiceUtils.stringToBytes(stringToSign)));
        expected = "98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
        assertEquals(expected, signature);

        // Authorization header
        String authorizationHeaderValue =
            SignatureUtils.awsV4BuildAuthorizationHeaderValue(
                this.awsAccessKey, signature, this.requestSignatureVersion,
                canonicalRequestString, this.timestampISO8601, this.region);
        expected = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,SignedHeaders=date;host;x-amz-content-sha256;x-amz-date;x-amz-storage-class,Signature=98ad721746da40c64f1a55b78f14c238d841ea1380cd77a1b5971af0ece108bd";
        assertEquals(expected, authorizationHeaderValue);

        // The whole request
        SignatureUtils.awsV4SignRequestAuthorizationHeader(
            this.requestSignatureVersion, httpPut,
            new AWSCredentials(this.awsAccessKey, this.awsSecretAccessKey),
            requestPayloadHexSHA256Hash, this.region);
        assertEquals(expected, httpPut.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void testS3ApiReferenceExampleGetBucketLifecycle() {
        HttpGet httpGet = new HttpGet("http://examplebucket.s3.amazonaws.com?lifecycle");
        httpGet.setHeader("Host", "examplebucket.s3.amazonaws.com");
        // NOTE: Date header missed in example test case
        httpGet.setHeader("x-amz-date", this.timestampISO8601);

        // Empty payload
        String payload = "";
        String requestPayloadHexSHA256Hash = ServiceUtils.toHex(
            ServiceUtils.hash(payload, "SHA-256"));
        httpGet.setHeader("x-amz-content-sha256", requestPayloadHexSHA256Hash);

        // Canonical request string
        String expected =
            "GET\n" +
            "/\n" +
            "lifecycle=\n" +
            "host:examplebucket.s3.amazonaws.com\n" +
            "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
            "x-amz-date:20130524T000000Z\n" +
            "\n" +
            "host;x-amz-content-sha256;x-amz-date\n" +
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String canonicalRequestString = SignatureUtils.awsV4BuildCanonicalRequestString(
            httpGet, requestPayloadHexSHA256Hash);
        assertEquals(expected, canonicalRequestString);

        // String to sign
        expected =
            "AWS4-HMAC-SHA256\n" +
            "20130524T000000Z\n" +
            "20130524/us-east-1/s3/aws4_request\n" +
            "9766c798316ff2757b517bc739a67f6213b4ab36dd5da2f94eaebf79c77395ca";
        String stringToSign = SignatureUtils.awsV4BuildStringToSign(
            requestSignatureVersion, canonicalRequestString,
            this.timestampISO8601, this.region);
        assertEquals(expected, stringToSign);

        // Signature
        byte[] signingKey = SignatureUtils.awsV4BuildSigningKey(
            this.awsSecretAccessKey, this.timestampISO8601,
            this.region);
        String signature = ServiceUtils.toHex(
            ServiceUtils.hmacSHA256(
                signingKey, ServiceUtils.stringToBytes(stringToSign)));
        expected = "fea454ca298b7da1c68078a5d1bdbfbbe0d65c699e0f91ac7a200a0136783543";
        assertEquals(expected, signature);

        // Authorization header
        String authorizationHeaderValue =
            SignatureUtils.awsV4BuildAuthorizationHeaderValue(
                this.awsAccessKey, signature, this.requestSignatureVersion,
                canonicalRequestString, this.timestampISO8601, this.region);
        expected = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,SignedHeaders=host;x-amz-content-sha256;x-amz-date,Signature=fea454ca298b7da1c68078a5d1bdbfbbe0d65c699e0f91ac7a200a0136783543";
        assertEquals(expected, authorizationHeaderValue);

        // The whole request
        SignatureUtils.awsV4SignRequestAuthorizationHeader(
            this.requestSignatureVersion, httpGet,
            new AWSCredentials(this.awsAccessKey, this.awsSecretAccessKey),
            requestPayloadHexSHA256Hash, this.region);
        assertEquals(expected, httpGet.getFirstHeader("Authorization").getValue());
    }

    @Test
    public void testS3ApiReferenceExampleGetBucketListObjects() {
        HttpGet httpGet = new HttpGet("http://examplebucket.s3.amazonaws.com?max-keys=2&prefix=J");
        httpGet.setHeader("Host", "examplebucket.s3.amazonaws.com");
        // NOTE: Date header missed in example test case
        httpGet.setHeader("x-amz-date", this.timestampISO8601);

        // Empty payload
        String payload = "";
        String requestPayloadHexSHA256Hash = ServiceUtils.toHex(
            ServiceUtils.hash(payload, "SHA-256"));
        httpGet.setHeader("x-amz-content-sha256", requestPayloadHexSHA256Hash);

        // Canonical request string
        String expected =
            "GET\n" +
            "/\n" +
            "max-keys=2&prefix=J\n" +
            "host:examplebucket.s3.amazonaws.com\n" +
            "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
            "x-amz-date:20130524T000000Z\n" +
            "\n" +
            "host;x-amz-content-sha256;x-amz-date\n" +
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
        String canonicalRequestString = SignatureUtils.awsV4BuildCanonicalRequestString(
            httpGet, requestPayloadHexSHA256Hash);
        assertEquals(expected, canonicalRequestString);

        // String to sign
        expected =
            "AWS4-HMAC-SHA256\n" +
            "20130524T000000Z\n" +
            "20130524/us-east-1/s3/aws4_request\n" +
            "df57d21db20da04d7fa30298dd4488ba3a2b47ca3a489c74750e0f1e7df1b9b7";
        String stringToSign = SignatureUtils.awsV4BuildStringToSign(
            requestSignatureVersion, canonicalRequestString,
            this.timestampISO8601, this.region);
        assertEquals(expected, stringToSign);

        // Signature
        byte[] signingKey = SignatureUtils.awsV4BuildSigningKey(
            this.awsSecretAccessKey, this.timestampISO8601,
            this.region);
        String signature = ServiceUtils.toHex(
            ServiceUtils.hmacSHA256(
                signingKey, ServiceUtils.stringToBytes(stringToSign)));
        expected = "34b48302e7b5fa45bde8084f4b7868a86f0a534bc59db6670ed5711ef69dc6f7";
        assertEquals(expected, signature);

        // Authorization header
        String authorizationHeaderValue =
            SignatureUtils.awsV4BuildAuthorizationHeaderValue(
                this.awsAccessKey, signature, this.requestSignatureVersion,
                canonicalRequestString, this.timestampISO8601, this.region);
        expected = "AWS4-HMAC-SHA256 Credential=AKIAIOSFODNN7EXAMPLE/20130524/us-east-1/s3/aws4_request,SignedHeaders=host;x-amz-content-sha256;x-amz-date,Signature=34b48302e7b5fa45bde8084f4b7868a86f0a534bc59db6670ed5711ef69dc6f7";
        assertEquals(expected, authorizationHeaderValue);

        // The whole request
        SignatureUtils.awsV4SignRequestAuthorizationHeader(
            this.requestSignatureVersion, httpGet,
            new AWSCredentials(this.awsAccessKey, this.awsSecretAccessKey),
            requestPayloadHexSHA256Hash, this.region);
        assertEquals(expected, httpGet.getFirstHeader("Authorization").getValue());
    }

    public void testFindBucketNameInHostOrPath() throws Exception {
        String defaultS3Endpoint = "s3.amazonaws.com";

        /*
         * Requests to service Host at root level: no bucket name
         */
        assertEquals(
            null,
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3.amazonaws.com/"),
                defaultS3Endpoint)
        );
        assertEquals(
            null,
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3.amazonaws.com/?acl"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            null,
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my.s3.alternate.endpoint/"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with bucket in Path
         */
        assertEquals(
            "my bückét テ",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3.amazonaws.com/my%20bückét%20テ"),
                defaultS3Endpoint)
        );
        assertEquals(
            "my bückét テ",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3.amazonaws.com/my%20bückét%20テ/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "my bückét テ",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my.s3.alternate.endpoint/my%20bückét%20テ/"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with single-term bucket prefix in Host
         */
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://bucketname.s3.amazonaws.com/"),
                defaultS3Endpoint)
        );
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://bucketname.s3.amazonaws.com/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://bucketname.my.s3.alternate.endpoint/"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with multi-term bucket prefix in Host
         */
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://my.bucket.name.s3.amazonaws.com/"),
                defaultS3Endpoint)
        );
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://my.bucket.name.s3.amazonaws.com/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my.bucket.name.my.s3.alternate.endpoint/"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with bucket prefix in Host which has region variations
         */
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://bucketname.s3-external-1.amazonaws.com/"),
                defaultS3Endpoint)
        );
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://my.bucket.name.s3-eu-central-1.amazonaws.com/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Note nasty exceptional case for s3.eu-central-1, version of s3-eu-central-1
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://bucketname.s3.eu-central-1.amazonaws.com/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my.bucket.name.my-other-region.s3.alternate.endpoint/"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with bucket name in Path but using region variation Host names
         */
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3-external-1.amazonaws.com/bucketname"),
                defaultS3Endpoint)
        );
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3-eu-central-1.amazonaws.com/my.bucket.name/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Note nasty exceptional case for s3.eu-central-1, version of s3-eu-central-1
        assertEquals(
            "bucketname",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://s3.eu-central-1.amazonaws.com/bucketname/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my-other-region.s3.alternate.endpoint/my.bucket.name"),
                "my.s3.alternate.endpoint")
        );

        /*
         * Requests with DNS name mapping serving as bucket name
         */
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://my.bucket.name/"),
                defaultS3Endpoint)
        );
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("https://my.bucket.name/some%20object.txt"),
                defaultS3Endpoint)
        );
        // Alternate non-S3 endpoint
        assertEquals(
            "my.bucket.name",
            ServiceUtils.findBucketNameInHostOrPath(
                new URI("http://my.bucket.name/"),
                "my.s3.alternate.endpoint")
        );

    }

    // Very basic test of signed GET request with no payload.
    @Test
    public void testWithServiceListAllBuckets() throws Exception {
        Jets3tProperties properties = new Jets3tProperties();
        properties.setProperty(
            "storage-service.request-signature-version",
            this.requestSignatureVersion);

        RestS3Service service = new RestS3Service(
            this.testCredentials, null, null, properties);

        service.listAllBuckets();
    }

    // Very basic test of signed PUT and DELETE requests with no payload.
    @Test
    public void testWithServiceCreateAndDeleteBucket() throws Exception {
        Jets3tProperties properties = new Jets3tProperties();
        properties.setProperty(
            "storage-service.request-signature-version",
            this.requestSignatureVersion);

        RestS3Service service = new RestS3Service(
            this.testCredentials, null, null, properties);

        String bucketName =
            "test-" + testCredentials.getAccessKey().toLowerCase()
            + "-testwithservicecreatebucket-"
            + System.currentTimeMillis();
        service.createBucket(bucketName);
        service.deleteBucket(bucketName);
    }

    // Test signed PUT requests (with payloads) and DELETE requests for bucket in "eu-central-1"
    // using service that is *not* configured to use AWS4-HMAC-SHA256 signatures by default.
    @Test
    public void testWithServiceCreateAndDeleteBucketAndCreateGetAndDeleteObject() throws Exception {
        RestS3Service service = new RestS3Service(this.testCredentials);

        String bucketName =
            "test-" + testCredentials.getAccessKey().toLowerCase()
            + System.currentTimeMillis();
        String objectData = "Just some simple text data";
        S3Object object = new S3Object(
            "text data object : îüøæç : テストオブジェクト",
            objectData);
        object.addMetadata("my-test-metadata", "my-value");


        service.getOrCreateBucket(bucketName, "eu-central-1");

        service.putObject(bucketName, object);

        // After request targeted at our bucket, we should have a cache entry
        // mapping our bucket name to the correct region.
        assertEquals(
            "eu-central-1",
            service.getRegionEndpointCache().getRegionForBucketName(bucketName));

        // With a cached mapping to the correct region, a HEAD request to
        // non-default region bucket using a service that is not aware of the
        // region will succeed.
        S3Object headObject = (S3Object)service.getObjectDetails(
            bucketName, object.getKey());
        assertEquals("my-value", headObject.getMetadata("my-test-metadata"));

        // The same HEAD request to non-default region bucket using a service
        // that is not aware of the region would fail if it wasn't for the
        // cached mapping from bucket to region.
        service.getRegionEndpointCache().removeRegionForBucketName(bucketName);
        try {
            service.getObjectDetails(bucketName, object.getKey());
            fail("Expected HEAD request to fail with no");
        } catch (ServiceException e) {
        }

        // A GET request to non-default region bucket using a service that is not
        // aware of the region can succeed because we get an error from S3 with
        // the expected region, and can correct the request then retry.
        S3Object retrievedObject = service.getObject(bucketName, object.getKey());
        assertEquals(objectData,
            ServiceUtils.readInputStreamToString(
                retrievedObject.getDataInputStream(),
                Constants.DEFAULT_ENCODING));

        // The above GET request targeted at our bucket could be made to succeed
        // using the error data returned by S3, which also re-populates our
        // bucket name to region cache.
        assertEquals(
            "eu-central-1",
            service.getRegionEndpointCache().getRegionForBucketName(bucketName));

        service.deleteObject(bucketName, object.getKey());

        service.deleteBucket(bucketName);
    }

}
