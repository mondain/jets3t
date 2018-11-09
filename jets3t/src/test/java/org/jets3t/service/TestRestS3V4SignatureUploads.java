package org.jets3t.service;

import junit.framework.TestCase;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.MultipartUtils;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestRestS3V4SignatureUploads extends TestCase {
    protected String TEST_PROPERTIES_FILENAME = "test.properties";
    protected Properties testProperties = null;
    protected ProviderCredentials testCredentials = null;

    public TestRestS3V4SignatureUploads() throws Exception {
        InputStream propertiesIS = ClassLoader.getSystemResourceAsStream(TEST_PROPERTIES_FILENAME);
        if (propertiesIS == null) {
            throw new Exception("Unable to load test properties file from classpath: " + TEST_PROPERTIES_FILENAME);
        }
        this.testProperties = new Properties();
        this.testProperties.load(propertiesIS);

        this.testCredentials = new AWSCredentials(
                testProperties.getProperty("aws.accesskey"),
                testProperties.getProperty("aws.secretkey"));

    }

    protected RestS3Service getStorageService(ProviderCredentials credentials) throws ServiceException {
        Jets3tProperties properties = new Jets3tProperties();
        properties.setProperty("s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
        properties.setProperty("storage-service.internal-error-retry-max", "0");
        properties.setProperty("httpclient.retry-max", "0");
        properties.setProperty("storage-service.request-signature-version", "AWS4-HMAC-SHA256");
        return new RestS3Service(credentials, null, null, properties);
    }

    public void testUploadWithMetadata() throws Exception {
        RestS3Service service = getStorageService(testCredentials);
        String bucketName = "test-" + testCredentials.getAccessKey().toLowerCase() + "-metadata";
        String objectData = "This is only a test";
        String objectKey = "object-with-metadata";

        S3Bucket s3Bucket = new S3Bucket(bucketName);
        service.getOrCreateBucket(bucketName, "eu-central-1");

        S3Object object = new S3Object(objectKey, objectData);
        String metaWithWhitespaceValue = "   value with pre- and post-whitespace  ";
        object.addMetadata("meta-with-whitespace", metaWithWhitespaceValue);
        service.putObject(s3Bucket, object);

        S3Object uploaded = service.getObject(bucketName, object.getKey());
        // Confirm whitespace trimmed from metadata value, see #230
        assertEquals(
            metaWithWhitespaceValue.trim(),
            uploaded.getMetadata("meta-with-whitespace"));
        assertEquals(objectData,
            getContentsAsString(uploaded.getDataInputStream()));

        service.deleteObject(bucketName, object.getKey());
        service.deleteBucket(bucketName);
}

    public void testCanUploadAFile() throws Exception {
        RestS3Service service = getStorageService(testCredentials);
        String bucketName = "test-" + testCredentials.getAccessKey().toLowerCase() + "-file";
        String objectData = "This is only a test";

        File fileToUpload = createTestFile("test.txt");
        writeToFile(fileToUpload, objectData);

        service.getOrCreateBucket(bucketName, "eu-central-1");

        S3Bucket s3Bucket = new S3Bucket(bucketName);
        S3Object object = null;
        try {
            object = new S3Object(s3Bucket, fileToUpload);
        } catch (NoSuchAlgorithmException e) {
            rethrow(e);
        } catch (IOException e) {
            rethrow(e);
        }
        service.putObject(bucketName, object);
        S3Object uploaded = service.getObject(bucketName, object.getKey());

        assertEquals(objectData,
            getContentsAsString(uploaded.getDataInputStream()));

        service.deleteObject(bucketName, object.getKey());
        service.deleteBucket(bucketName);
    }

    public void testCanUploadAnInputStream() throws Exception {
        RestS3Service service = getStorageService(testCredentials);
        String bucketName = "test-" + testCredentials.getAccessKey().toLowerCase() + "-istream";
        String objectData = "This is only a test";

        File fileToUpload = createTestFile("testInputStream.txt");
        writeToFile(fileToUpload, objectData);

        service.getOrCreateBucket(bucketName, "eu-central-1");

        FileInputStream fileInputStream = new FileInputStream(fileToUpload);
        S3Object object = null;
        try {
            object = new S3Object(fileToUpload.getName());
            object.setDataInputStream(fileInputStream);
            object.setContentLength(fileInputStream.available());

        } catch (IOException e) {
            rethrow(e);

        }

        service.deleteObject(bucketName, object.getKey());

        service.putObject(bucketName, object);

        S3Object uploaded = service.getObject(bucketName, object.getKey());
        assertEquals(objectData,
            getContentsAsString(uploaded.getDataInputStream()));

        service.deleteObject(bucketName, object.getKey());
        service.deleteBucket(bucketName);
    }

    public void testCanUploadAMultipartStream() throws Exception {
        RestS3Service service = getStorageService(testCredentials);
        String bucketName = "test-" + testCredentials.getAccessKey().toLowerCase() + "-multipart";

        // Create a medium (6 MB) file
        File fileToUpload = TestFileUtils.createTempFileWithSize(
            "multiTest", ".txt", 6 * 1024 * 1024);
        service.getOrCreateBucket(bucketName, "eu-central-1");

        FileInputStream fileInputStream = new FileInputStream(fileToUpload);

        S3Object object = new S3Object(fileToUpload.getName());

        object.setDataInputFile(fileToUpload);

        // NOTE: MultipartUtils#uploadObjects doesn't support objects with input streams
        //object.setDataInputStream(fileInputStream);
        //object.setContentLength(fileInputStream.available());

        service.deleteObject(bucketName, object.getKey());

        long maxSizeForAPartInBytes = 5 * 1024 * 1024;  // Max part size: 5MB
        MultipartUtils mpUtils = new MultipartUtils(maxSizeForAPartInBytes);
        List<StorageObject> objectsForMultipartUpload = new ArrayList<StorageObject>();
        objectsForMultipartUpload.add(object);

        mpUtils.uploadObjects(bucketName, service, objectsForMultipartUpload, null);

        S3Object uploaded = service.getObject(bucketName, object.getKey());
        assertEquals(
            getContentsAsString(new FileInputStream(fileToUpload)),
            getContentsAsString(uploaded.getDataInputStream()));

        service.deleteObject(bucketName, object.getKey());
        service.deleteBucket(bucketName);
    }

    private String getContentsAsString(InputStream is) {
        BufferedReader reader;
        StringBuilder out = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                out.append(line);
            }
            reader.close();
        } catch (IOException e) {
            rethrow(e);
        }
        return out.toString();
    }

    private void rethrow(Exception e) {
        throw new RuntimeException(e);
    }

    private void writeToFile(File f, String s) {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(f));
            writer.write(s);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private File createTestFile(String name) {
        File file = new File(System.getProperty("java.io.tmpdir") + "/" + name);
        try {
            file.createNewFile();
        } catch (IOException e) {
            rethrow(e);
        }
        return file;
    }

}
