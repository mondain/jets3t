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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.StorageService;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.acl.gs.AllUsersGrantee;
import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSBucketLoggingStatus;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3BucketLoggingStatus;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageBucketLoggingStatus;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.model.ThrowableBearingStorageObject;
import org.jets3t.service.multi.ErrorPermitter;
import org.jets3t.service.multi.SimpleThreadedStorageService;
import org.jets3t.service.multi.StorageServiceEventAdaptor;
import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.event.CreateObjectsEvent;
import org.jets3t.service.multi.event.DeleteObjectsEvent;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.GetObjectsEvent;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.FileComparer;
import org.jets3t.service.utils.FileComparerResults;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.ObjectUtils;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Runs generic functional tests that any storage service implementation should be
 * able to perform.
 * <p>
 * Any test cases for specific StorageService implementations should extend this class as a
 * starting point, then add test cases specific to that particular implementation.
 *
 * @author James Murty
 */
public abstract class BaseStorageServiceTests extends TestCase {
    protected static final String TARGET_SERVICE_S3 = "AmazonS3";
    protected static final String TARGET_SERVICE_GS = "GoogleStorage";

    protected String TEST_PROPERTIES_FILENAME = "test.properties";
    protected Properties testProperties = null;

    public BaseStorageServiceTests() throws Exception {
        // Load test properties
        InputStream propertiesIS =
            ClassLoader.getSystemResourceAsStream(TEST_PROPERTIES_FILENAME);
        if (propertiesIS == null) {
            throw new Exception("Unable to load test properties file from classpath: "
                + TEST_PROPERTIES_FILENAME);
        }
        this.testProperties = new Properties();
        this.testProperties.load(propertiesIS);
    }

    protected abstract ProviderCredentials getCredentials() throws Exception;

    protected abstract RestStorageService getStorageService(ProviderCredentials credentials) throws Exception;

    protected abstract String getTargetService();

    protected abstract AccessControlList buildAccessControlList();

    protected abstract StorageBucketLoggingStatus getBucketLoggingStatus(
        String targetBucketName, String logfilePrefix) throws Exception;

    /**
     * @param testName
     * @return unique per-account and per-test bucket name
     */
    protected String getBucketNameForTest(String testName) throws Exception {
        return
            "test-"
            + getCredentials().getAccessKey().toLowerCase()
            + "-"
            + testName.toLowerCase();
    }

    protected StorageBucket createBucketForTest(String testName) throws Exception {
        return this.createBucketForTest(testName, null);
    }

    protected StorageBucket createBucketForTest(String testName, String location) throws Exception {
        String bucketName = getBucketNameForTest(testName);
        StorageService service = getStorageService(getCredentials());
        if (service instanceof S3Service) {
            return ((S3Service)service).getOrCreateBucket(bucketName, location);
        } else {
            GSBucket bucket = new GSBucket(bucketName, location);
            return ((GoogleStorageService)service).createBucket(bucket);
        }
    }

    protected void deleteAllObjectsInBucket(String bucketName) {
        try {
            RestStorageService service = getStorageService(getCredentials());
            for (StorageObject o: service.listObjects(bucketName)) {
                service.deleteObject(bucketName, o.getKey());
            }
        } catch (Exception e) {
            // This shouldn't happen, but if it does don't ruin the test
            e.printStackTrace();
        }
    }

    protected void cleanupBucketForTest(String testName, boolean deleteAllObjects) {
        try {
            RestStorageService service = getStorageService(getCredentials());
            String bucketName = getBucketNameForTest(testName);

            if (deleteAllObjects) {
                deleteAllObjectsInBucket(bucketName);
            }

            service.deleteBucket(bucketName);
        } catch (Exception e) {
            // This shouldn't happen, but if it does don't ruin the test
            e.printStackTrace();
        }
    }

    protected void cleanupBucketForTest(String testName) {
        this.cleanupBucketForTest(testName, true);
    }

    /////////////////////////////
    // Actual tests start here //
    /////////////////////////////

    public void testListBuckets() throws Exception {
        // List without credentials
        try {
            getStorageService(null).listAllBuckets();
            fail("Bucket listing should fail without authentication");
        } catch (ServiceException e) {
        }

        // List with credentials
        getStorageService(getCredentials()).listAllBuckets();

        // Ensure newly-created bucket is listed
        String bucketName = createBucketForTest("testListBuckets").getName();
        try {
            StorageBucket[] buckets = getStorageService(getCredentials()).listAllBuckets();
            boolean found = false;
            for (StorageBucket bucket: buckets) {
                found = (bucket.getName().equals(bucketName)) || found;
            }
            assertTrue("Newly-created bucket was not listed", found);
        } finally {
            cleanupBucketForTest("testListBuckets");
        }
    }

    public void testBucketManagement() throws Exception {
        RestStorageService service = getStorageService(getCredentials());

        try {
            service.createBucket("");
            fail("Cannot create a bucket with empty name");
        } catch (ServiceException e) {
        }

        try {
            service.createBucket("test");
            fail("Cannot create a bucket with non-unique name");
        } catch (ServiceException e) {
        }

        String bucketName = createBucketForTest("testBucketManagement").getName();

        boolean bucketExists = service.isBucketAccessible(bucketName);
        assertTrue("Bucket should exist", bucketExists);

        try {
            service.deleteBucket((String) null);
            fail("Cannot delete a bucket with null name");
        } catch (ServiceException e) {
        }

        try {
            service.deleteBucket("");
            fail("Cannot delete a bucket with empty name");
        } catch (ServiceException e) {
        }

        try {
            service.deleteBucket("test");
            fail("Cannot delete a bucket you don't own");
        } catch (ServiceException e) {
        }

        // Ensure we can delete our bucket
        service.deleteBucket(bucketName);
    }

    public void testBucketStatusLookup() throws Exception {
        String bucketName = getBucketNameForTest("testBucketStatusLookup");
        RestStorageService service = getStorageService(getCredentials());

        try {
            // Non-existent bucket
            int status = service.checkBucketStatus(bucketName);
            assertEquals(S3Service.BUCKET_STATUS__DOES_NOT_EXIST, status);

            // Bucket is owned by someone else
            // NOTE: This test will fail if you actually own the "testing" bucket,
            // or if it is owned by someone else but has been made publicly readable.
            status = service.checkBucketStatus("testing");
            assertEquals(S3Service.BUCKET_STATUS__ALREADY_CLAIMED, status);

            service.createBucket(bucketName);
            // Bucket now exists and is owned by me.
            status = service.checkBucketStatus(bucketName);
            assertEquals(S3Service.BUCKET_STATUS__MY_BUCKET, status);
        } finally {
            // Clean up
            service.deleteBucket(bucketName);
        }
    }

    public void testBucketLocations() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        // Create bucket in default (null) location
        String bucketName = getBucketNameForTest("testBucketLocations-test1");
        try {
            service.createBucket(bucketName);
            assertEquals(StorageService.BUCKET_STATUS__MY_BUCKET, service.checkBucketStatus(bucketName));
            String bucketLocation = (service instanceof S3Service
                ? ((S3Service) service).getBucketLocation(bucketName)
                : ((GoogleStorageService) service).getBucketLocation(bucketName) );
            if (TARGET_SERVICE_S3.equals(getTargetService())) {
                assertEquals(null, bucketLocation); // For S3, the default/null location is literally null
            } else {
                assertEquals("US", bucketLocation); // For GS, "US" is default/null location
            }
        } finally {
            service.deleteBucket(bucketName);
        }
        // Create bucket in alternate location
        bucketName = getBucketNameForTest("testBucketLocations-test2");
        try {
            StorageBucket newBucket = null;
            String targetLocation = null;
            if (TARGET_SERVICE_S3.equals(getTargetService())) {
                targetLocation = S3Bucket.LOCATION_US_WEST;
                newBucket = new S3Bucket(bucketName, targetLocation);
            } else {
                targetLocation = GSBucket.LOCATION_EUROPE;
                newBucket = new GSBucket(bucketName, targetLocation);
            }
            service.createBucket(newBucket);
            assertEquals(StorageService.BUCKET_STATUS__MY_BUCKET, service.checkBucketStatus(bucketName));
            String bucketLocation = (service instanceof S3Service
                ? ((S3Service) service).getBucketLocation(bucketName)
                : ((GoogleStorageService) service).getBucketLocation(bucketName) );
            assertEquals(targetLocation, bucketLocation);
        } finally {
            service.deleteBucket(bucketName);
        }
    }

    public void testObjectManagement() throws Exception {
        String bucketName = createBucketForTest("testObjectManagement").getName();
        RestStorageService service = getStorageService(getCredentials());

        try {
            StorageObject object = new StorageObject("TestObject");

            try {
                service.putObject((String) null, null);
                fail("Cannot create an object without a valid bucket");
            } catch (ServiceException e) {
            }

            try {
                service.putObject((String) null, object);
                fail("Cannot create an object without a valid bucket");
            } catch (ServiceException e) {
            }

            try {
                service.putObject(bucketName, new StorageObject());
                fail("Cannot create an object without a valid object");
            } catch (ServiceException e) {
            }

            // Create basic object with no content type (use the default) and no data.
            StorageObject basicObject = service.putObject(bucketName, object);

            // Ensure Content-Type is set to binary by default
            // TODO: Google Storage bug: Content type returned on initial PUT is always "text/html"
            if (!TARGET_SERVICE_GS.equals(getTargetService())) {
                assertTrue("Unexpected default content type",
                    Mimetypes.MIMETYPE_OCTET_STREAM.equals(basicObject.getContentType()));
            }

            // Re-retrieve object to ensure it was correctly created.
            basicObject = service.getObject(bucketName, object.getKey());
            assertEquals("Unexpected content type",
                Mimetypes.MIMETYPE_OCTET_STREAM, basicObject.getContentType());
            assertEquals("Unexpected size for 'empty' object", 0, basicObject.getContentLength());
            basicObject.closeDataInputStream();

            // Make sure bucket cannot be removed while it has contents.
            try {
                service.deleteBucket(bucketName);
                fail("Should not be able to delete a bucket containing objects");
            } catch (ServiceException e) {
            }

            // Update/overwrite object with real data content and some metadata.
            String contentType = "text/plain";
            String objectData = "Just some rubbish text to include as data";
            String dataMd5HashAsHex = ServiceUtils.toHex(
                ServiceUtils.computeMD5Hash(objectData.getBytes()));
            HashMap<String, Object> metadata = new HashMap<String, Object>();
            metadata.put("creator", "testObjectManagement");
            metadata.put("purpose", "For testing purposes");
            object.replaceAllMetadata(metadata);
            object.setContentType(contentType);
            object.setDataInputStream(new ByteArrayInputStream(objectData.getBytes()));
            StorageObject dataObject = service.putObject(bucketName, object);
            // TODO: Google Storage bug: Content type returned on initial PUT is always "text/html"
            if (TARGET_SERVICE_GS.equals(getTargetService())) {
                dataObject = service.getObject(bucketName, object.getKey());
            }
            assertEquals("Unexpected content type", contentType, dataObject.getContentType());
            assertEquals("Mismatching MD5 hex hash", dataMd5HashAsHex, dataObject.getETag());

            // Retrieve data object to ensure it was correctly created, the server-side hash matches
            // what we expect, and we get our metadata back.
            dataObject = service.getObject(bucketName, object.getKey());
            assertEquals("Unexpected default content type", "text/plain", dataObject.getContentType());
            // TODO: Google Storage doesn't reliably return Content-Length in a GET
            if (!TARGET_SERVICE_GS.equals(getTargetService())) {
                assertEquals("Unexpected content-length for object",
                    objectData.length(), dataObject.getContentLength());
            }
            assertEquals("Mismatching hash", dataMd5HashAsHex, dataObject.getETag());

            // Check user's data are available in basic metadata map
            assertEquals("Missing creator metadata", "testObjectManagement",
                dataObject.getMetadata("creator"));
            assertEquals("Missing purpose metadata", "For testing purposes",
                dataObject.getMetadata("purpose"));

            // Check data are available in user metadata map
            assertEquals("Missing creator user metadata",
                "testObjectManagement", dataObject.getUserMetadataMap().get("creator"));
            assertEquals("Missing purpose user metadata",
                "For testing purposes", dataObject.getUserMetadataMap().get("purpose"));
            assertNotNull("Expected data input stream to be available", dataObject.getDataInputStream());

            // Check data are available in service metadata map
            assertNotNull(dataObject.getServiceMetadataMap().get("request-id"));

            // Ensure we can get the data from S3.
            StringBuffer sb = new StringBuffer();
            int b = -1;
            while ((b = dataObject.getDataInputStream().read()) != -1) {
                sb.append((char) b);
            }
            dataObject.closeDataInputStream();
            assertEquals("Mismatching data", objectData, sb.toString());

            // Retrieve only HEAD of data object (all metadata is available, but not the object content
            // data input stream)
            dataObject = service.getObjectDetails(bucketName, object.getKey());
            assertEquals("Unexpected default content type", "text/plain", dataObject.getContentType());
            assertEquals("Mismatching hash", dataMd5HashAsHex, dataObject.getETag());
            assertEquals("Missing creator metadata", "testObjectManagement",
                dataObject.getMetadata("creator"));
            assertEquals("Missing purpose metadata", "For testing purposes",
                dataObject.getMetadata("purpose"));
            assertNull("Expected data input stream to be unavailable", dataObject.getDataInputStream());
            assertEquals("Unexpected size for object", objectData.length(), dataObject.getContentLength());

            // Test object GET constraints.
            Calendar objectCreationTimeCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.US);
            objectCreationTimeCal.setTime(dataObject.getLastModifiedDate());

            Calendar yesterday = (Calendar) objectCreationTimeCal.clone();
            yesterday.add(Calendar.DAY_OF_YEAR, -1);
            Calendar tomorrow = (Calendar) objectCreationTimeCal.clone();
            tomorrow.add(Calendar.DAY_OF_YEAR, +2);

            // Precondition: Modified since yesterday
            service.getObjectDetails(bucketName, object.getKey(), yesterday, null, null, null);
            // Precondition: Mot modified since after creation date.
            try {
                service.getObjectDetails(bucketName, object.getKey(), objectCreationTimeCal, null, null, null);
                fail("Cannot have been modified since object was created");
            } catch (ServiceException e) { }
            // Precondition: Not modified since yesterday
            try {
                service.getObjectDetails(bucketName, object.getKey(), null, yesterday, null, null);
                fail("Cannot be unmodified since yesterday");
            } catch (ServiceException e) { }
            // Precondition: Not modified since tomorrow
            service.getObjectDetails(bucketName, object.getKey(), null, tomorrow, null, null);
            // Precondition: matches correct hash
            service.getObjectDetails(bucketName, object.getKey(), null, null, new String[] {dataMd5HashAsHex}, null);
            // Precondition: doesn't match incorrect hash
            try {
                service.getObjectDetails(bucketName, object.getKey(), null, null,
                    new String[] {"__" + dataMd5HashAsHex.substring(2)}, null);
                fail("Hash values should not match");
            } catch (ServiceException e) {
            }
            // Precondition: doesn't match correct hash
            try {
                service.getObjectDetails(bucketName, object.getKey(), null, null, null, new String[] {dataMd5HashAsHex});
                fail("Hash values should mis-match");
            } catch (ServiceException e) {
            }
            // Precondition: doesn't match incorrect hash
            service.getObjectDetails(bucketName, object.getKey(), null, null, null,
                new String[] {"__" + dataMd5HashAsHex.substring(2)});

            // Retrieve only a limited byte-range of the data, with a start and end.
            Long byteRangeStart = new Long(3);
            Long byteRangeEnd = new Long(12);
            dataObject = service.getObject(bucketName, object.getKey(), null, null, null, null, byteRangeStart, byteRangeEnd);
            String dataReceived = ServiceUtils.readInputStreamToString(
                dataObject.getDataInputStream(), Constants.DEFAULT_ENCODING);
            String dataExpected = objectData.substring(byteRangeStart.intValue(), byteRangeEnd.intValue() + 1);
            assertEquals("Mismatching data from range precondition", dataExpected, dataReceived);

            // Retrieve only a limited byte-range of the data, with a start range only.
            byteRangeStart = new Long(7);
            byteRangeEnd = null;
            dataObject = service.getObject(bucketName, object.getKey(), null, null, null, null, byteRangeStart, byteRangeEnd);
            dataReceived = ServiceUtils.readInputStreamToString(
                dataObject.getDataInputStream(), Constants.DEFAULT_ENCODING);
            dataExpected = objectData.substring(byteRangeStart.intValue());
            assertEquals("Mismatching data from range precondition", dataExpected, dataReceived);

            // Retrieve only a limited byte-range of the data, with an end range only.
            byteRangeStart = null;
            byteRangeEnd = new Long(13);
            dataObject = service.getObject(bucketName, object.getKey(), null, null, null, null, byteRangeStart, byteRangeEnd);
            dataReceived = ServiceUtils.readInputStreamToString(
                dataObject.getDataInputStream(), Constants.DEFAULT_ENCODING);
            dataExpected = objectData.substring(objectData.length() - byteRangeEnd.intValue());
            assertEquals("Mismatching data from range precondition", dataExpected, dataReceived);

            // Clean-up.
            service.deleteObject(bucketName, object.getKey());

            // Create object with tricky key.
            String trickyKey = "testing:example.site.com/some/path/document name.html?param1=a@b#c$d&param2=(089)";
            StorageObject trickyObject = service.putObject(bucketName,
                new StorageObject(trickyKey, "Some test data"));
            assertEquals("Tricky key name mistmatch", trickyKey, trickyObject.getKey());

            // Make sure the tricky named object really exists with its full name.
            StorageObject[] objects = service.listObjects(bucketName);
            boolean trickyNamedObjectExists = false;
            for (int i = 0; !trickyNamedObjectExists && i < objects.length; i++) {
                if (trickyKey.equals(objects[i].getKey())) {
                    trickyNamedObjectExists = true;
                }
            }
            assertTrue("Tricky key name object does not exist with its full name", trickyNamedObjectExists);

            // Delete object with tricky key.
            service.deleteObject(bucketName, trickyObject.getKey());

        } finally {
            cleanupBucketForTest("testObjectManagement");
        }
    }

    public void testDirectoryPlaceholderObjects() throws Exception {
        String bucketName = createBucketForTest("testDirectoryPlaceholderObjects").getName();
        RestStorageService service = getStorageService(getCredentials());

        try {
            // Create new-style place-holder object (compatible with Amazon's AWS Console
            // and Panic's Transmit) -- note trailing slash
            StorageObject requestObject = new StorageObject("DirPlaceholderObject/");
            requestObject.setContentLength(0);
            requestObject.setContentType(Mimetypes.MIMETYPE_BINARY_OCTET_STREAM);
            assertTrue(requestObject.isDirectoryPlaceholder());

            service.putObject(bucketName, requestObject);
            StorageObject resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertTrue(resultObject.isDirectoryPlaceholder());

            // Create legacy-style place-holder object (compatible with objects stored using
            // JetS3t applications prior to version 0.8.0) -- note content type
            requestObject = new StorageObject("LegacyDirPlaceholderObject");
            requestObject.setContentLength(0);
            requestObject.setContentType(Mimetypes.MIMETYPE_JETS3T_DIRECTORY);
            assertTrue(requestObject.isDirectoryPlaceholder());

            service.putObject(bucketName, requestObject);
            resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertTrue(resultObject.isDirectoryPlaceholder());

            // Create place-holder object compatible with the S3 Organizer Firefox extension
            // -- note object name suffix.
            requestObject = new StorageObject("S3OrganizerDirPlaceholderObject_$folder$");
            requestObject.setContentLength(0);
            assertTrue(requestObject.isDirectoryPlaceholder());

            service.putObject(bucketName, requestObject);
            resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertTrue(resultObject.isDirectoryPlaceholder());
        } finally {
            cleanupBucketForTest("testDirectoryPlaceholderObjects");
        }
    }

    public void testCopyObjects() throws Exception {
        String sourceBucketName = createBucketForTest("testCopyObjects-source").getName();
        String targetBucketName = createBucketForTest("testCopyObjects-target").getName();
        RestStorageService service = getStorageService(getCredentials());
        try {
            // Create source objects to copy, with potentially troublesome names
            String[] objectNames = new String[] {
                "testing.txt",
                "test me.txt",
                "virtual-path/testing.txt",
                "vîrtüál-πå†h/tés†ing.txt"
            };

            for (int i = 0; i < objectNames.length; i++) {
                StorageObject object = new StorageObject(
                    objectNames[i], "A little data");
                object.addMetadata("object-offset", "" + i);
                service.putObject(sourceBucketName, object);
            }

            // Copy objects within bucket, retaining metadata
            String targetPath = "copies/";
            for (String objectName: objectNames) {
                StorageObject targetObject = new StorageObject(
                    targetPath + objectName);
                service.copyObject(sourceBucketName, objectName,
                    sourceBucketName, targetObject,
                    false // replaceMetadata
                    );
            }
            // Ensure objects are in target location and have the same metadata
            for (int i = 0; i < objectNames.length; i++) {
                StorageObject object = service.getObjectDetails(
                    sourceBucketName, targetPath + objectNames[i]);
                assertEquals("" + i, object.getMetadata("object-offset"));
            }

            // Copy object within bucket, replacing metadata
            StorageObject targetObject = new StorageObject(
                targetPath + objectNames[0]);
            targetObject.addMetadata("replaced-metadata", "booyah!");
            service.copyObject(sourceBucketName, objectNames[0],
                sourceBucketName, targetObject,
                true // replaceMetadata
                );
            StorageObject copiedObject = service.getObjectDetails(
                sourceBucketName, targetObject.getName());
            assertNull(copiedObject.getMetadata("object-offset"));
            assertEquals("booyah!", copiedObject.getMetadata("replaced-metadata"));

            // Copy objects between buckets
            List<Map<String, Object>> copyResults =
                new ArrayList<Map<String, Object>>();
            for (String objectName: objectNames) {
                targetObject = new StorageObject(objectName);
                Map<String, Object> copyResult = service.copyObject(
                    sourceBucketName, objectName,
                    targetBucketName, targetObject,
                    false // replaceMetadata
                    );
                copyResults.add(copyResult);
                copiedObject = service.getObjectDetails(
                    targetBucketName, targetObject.getName());
            }
            assertEquals(4, service.listObjects(targetBucketName).length);
            // Check results map from copy operation
            Map<String, Object> firstCopyResult = copyResults.get(0);
            assertEquals("\"5d11eb8a313fc3e205fef245b7be06c7\"",
                firstCopyResult.get("ETag"));
            assertEquals(sourceBucketName,
                firstCopyResult.get("X-JetS3t-SourceBucketName"));
            assertEquals(objectNames[0],
                firstCopyResult.get("X-JetS3t-SourceObjectKey"));
            assertEquals(targetBucketName,
                firstCopyResult.get("X-JetS3t-DestinationBucketName"));
            assertEquals(objectNames[0], // Note same source and target key
                firstCopyResult.get("X-JetS3t-DestinationObjectKey"));
            assertNull(
                firstCopyResult.get("X-JetS3t-VersionId"));
            assertNull(
                firstCopyResult.get("X-JetS3t-DestinationObjectStorageClass"));
            assertNull(
                firstCopyResult.get("X-JetS3t-DestinationObjectServerSideEncryptionAlgorithm"));

            // Rename convenience method
            int objectOffset = 3;
            targetObject = new StorageObject("my-new-name");
            service.renameObject(
                sourceBucketName, objectNames[objectOffset], targetObject);
            copiedObject = service.getObjectDetails(
                sourceBucketName, targetObject.getName());
            // Ensure we have a new object with the same metadata
            assertEquals("my-new-name", copiedObject.getKey());
            assertEquals("" + objectOffset, copiedObject.getMetadata("object-offset"));

            // Update metadata convenience method
            objectOffset = 2;
            targetObject = new StorageObject(objectNames[objectOffset]);
            targetObject.addMetadata("object-offset", "" + objectOffset); // Unchanged
            targetObject.addMetadata("was-i-updated", "yes!");
            service.updateObjectMetadata(sourceBucketName, targetObject);
            copiedObject = service.getObjectDetails(
                sourceBucketName, targetObject.getName());
            // Ensure we have the same object with updated metadata
            assertEquals("yes!", copiedObject.getUserMetadataMap().get("was-i-updated"));

            // Move object convenience method - retain metadata
            objectOffset = 0;
            targetObject = new StorageObject(objectNames[objectOffset]);
            service.moveObject(sourceBucketName, objectNames[objectOffset],
                targetBucketName, targetObject, false);
            try {
                service.getObjectDetails(
                    sourceBucketName, objectNames[objectOffset]);
                fail("Source object should be moved");
            } catch (ServiceException e) {
                // Expected
            }
            copiedObject = service.getObjectDetails(
                targetBucketName, targetObject.getName());
            assertEquals("" + objectOffset, copiedObject.getUserMetadataMap().get("object-offset"));

            // Move object convenience method - replace metadata
            objectOffset = 1;
            targetObject = new StorageObject(objectNames[objectOffset]);
            targetObject.addMetadata("was-i-moved-with-new-metadata", "yes!");
            service.moveObject(sourceBucketName, objectNames[objectOffset],
                targetBucketName, targetObject, true);
            try {
                service.getObjectDetails(
                    sourceBucketName, objectNames[objectOffset]);
                fail("Source object should be moved");
            } catch (ServiceException e) {
                // Expected
            }
            copiedObject = service.getObjectDetails(
                targetBucketName, targetObject.getName());
            assertNull(copiedObject.getMetadata("object-offset"));
            assertEquals("yes!", copiedObject.getMetadata("was-i-moved-with-new-metadata"));
        } finally {
            cleanupBucketForTest("testCopyObjects-source");
            cleanupBucketForTest("testCopyObjects-target");
        }
    }

    public void testUnicodeData() throws Exception {
        String bucketName = createBucketForTest("testUnicodeData").getName();
        RestStorageService service = getStorageService(getCredentials());

        try {
            // Unicode object name
            String unicodeText = "テストオブジェクト";
            StorageObject requestObject = new StorageObject("1." + unicodeText);
            service.putObject(bucketName, requestObject);
            StorageObject resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertEquals("1." + unicodeText, resultObject.getKey());

            // Unicode data content
            requestObject = new StorageObject("2." + unicodeText, unicodeText);
            service.putObject(bucketName, requestObject);
            resultObject = service.getObject(bucketName, requestObject.getKey());
            String data = ServiceUtils.readInputStreamToString(
                resultObject.getDataInputStream(), "UTF-8");
            assertEquals(unicodeText, data);

            // Unicode metadata values are not supported
            requestObject = new StorageObject("3." + unicodeText);
            requestObject.addMetadata("testing", unicodeText);
            try {
                service.putObject(bucketName, requestObject);
            } catch (ServiceException e) {
            }

            // Unicode metadata values can be encoded
            requestObject = new StorageObject("4." + unicodeText);
            requestObject.addMetadata("testing", URLEncoder.encode(unicodeText, "UTF-8"));
            service.putObject(bucketName, requestObject);
            resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertEquals(unicodeText, URLDecoder.decode(
                (String) resultObject.getMetadata("testing"), "UTF-8"));

            // Unicode metadata names are not possible with HTTP
            requestObject = new StorageObject("5." + unicodeText);
            requestObject.addMetadata(unicodeText, "value");
            try {
                service.putObject(bucketName, requestObject);
                fail("Illegal to use non-ASCII characters in HTTP headers");
            } catch (ServiceException e) {
            }

            // Unicode HTTP headers (via RFC 5987 encoding) -- not working...
            /*
            requestObject = new StorageObject("6." + unicodeText);
            requestObject.setContentDisposition(
                "attachment; filename*=UTF-8''" + RestUtils.encodeUrlString(unicodeText + ".txt"));
            service.putObject(bucketName, requestObject);
            resultObject = service.getObjectDetails(bucketName, requestObject.getKey());
            assertEquals(
                "attachment; filename=" + unicodeText + "", resultObject.getContentDisposition());
            */
        } finally {
            cleanupBucketForTest("testUnicodeData");
        }
    }

    public void testACLManagement() throws Exception {
        // Access public-readable third-party bucket: jets3t
        RestStorageService anonymousS3Service = getStorageService(null);
        boolean jets3tBucketAvailable = anonymousS3Service.isBucketAccessible("jets3t");
        assertTrue("Cannot find public jets3t bucket", jets3tBucketAvailable);

        RestStorageService service = getStorageService(getCredentials());

        // Use Google- or S3-specific URL endpoint to lookup objects, depending on the target service
        String linkUrlPrefix = null;
        if (TARGET_SERVICE_GS.equals(getTargetService())) {
            linkUrlPrefix = "https://commondatastorage.googleapis.com";
        } else {
            linkUrlPrefix = "https://s3.amazonaws.com";
        }
        // Use Google- or S3-specific ACL elements depending on which service class we're using
        GranteeInterface allUsersGrantee = null;
        if (service instanceof GoogleStorageService) {
            allUsersGrantee = new AllUsersGrantee();
        } else {
            allUsersGrantee = GroupGrantee.ALL_USERS;
        }

        StorageBucket bucket = createBucketForTest("testACLManagement");
        String bucketName = bucket.getName();
        StorageObject object = null;

        try {
            // Create private object (default permissions).
            String privateKey = "Private Object - " + System.currentTimeMillis();
            object = new StorageObject(privateKey, "Private object sample text");
            service.putObject(bucketName, object);
            URL url = new URL(linkUrlPrefix + "/" + bucketName + "/" + RestUtils.encodeUrlString(privateKey));
            assertEquals("Expected denied access (403) error", 403, ((HttpURLConnection) url
                .openConnection()).getResponseCode());

            // Get ACL details for private object so we can determine the account owner ID.
            AccessControlList objectACL = service.getObjectAcl(bucketName, privateKey);
            StorageOwner accountOwner = objectACL.getOwner();

            // Create a public object.
            String publicKey = "Public Object - " + System.currentTimeMillis();
            object = new StorageObject(publicKey, "Public object sample text");
            AccessControlList acl = buildAccessControlList();
            acl.setOwner(accountOwner);
            acl.grantPermission(allUsersGrantee, Permission.PERMISSION_READ);
            object.setAcl(acl);
            service.putObject(bucketName, object);
            url = new URL(linkUrlPrefix + "/" + bucketName + "/" + RestUtils.encodeUrlString(publicKey));
            assertEquals("Expected access (200)",
                    200, ((HttpURLConnection)url.openConnection()).getResponseCode());

            // Update ACL to make private object public.
            AccessControlList privateToPublicACL = service.getObjectAcl(bucketName, privateKey);
            privateToPublicACL.grantPermission(allUsersGrantee, Permission.PERMISSION_READ);
            object.setKey(privateKey);
            object.setAcl(privateToPublicACL);
            service.putObjectAcl(bucketName, object);
            url = new URL(linkUrlPrefix + "/" + bucketName + "/" + RestUtils.encodeUrlString(privateKey));
            assertEquals("Expected access (200)", 200, ((HttpURLConnection) url.openConnection())
                .getResponseCode());

            // Create a non-standard uncanned public object.
            String publicKey2 = "Public Object - " + System.currentTimeMillis();
            object = new StorageObject(publicKey2);
            object.setAcl(privateToPublicACL); // This ACL has ALL_USERS READ permission set above.
            service.putObject(bucketName, object);
            url = new URL(linkUrlPrefix + "/" + bucketName + "/" + RestUtils.encodeUrlString(publicKey2));
            assertEquals("Expected access (200)", 200, ((HttpURLConnection) url.openConnection())
                .getResponseCode());

            // Update ACL to make public object private.
            AccessControlList publicToPrivateACL = service.getObjectAcl(bucketName, publicKey);
            publicToPrivateACL.revokeAllPermissions(allUsersGrantee);
            object.setKey(publicKey);
            object.setAcl(publicToPrivateACL);
            service.putObjectAcl(bucketName, object);
            // TODO: Google Storage quirk: It may take some time for public object to become private again
            if (TARGET_SERVICE_GS.equals(getTargetService())) {
                // Confirm changes were applied on object's ACL, because we don't know
                // how long to wait until the object will really become private again.
                AccessControlList updatedAcl = service.getObjectAcl(bucketName, object.getKey());
                assertFalse(updatedAcl.hasGranteeAndPermission(
                    allUsersGrantee, Permission.PERMISSION_READ));
                assertEquals(0, updatedAcl.getPermissionsForGrantee(allUsersGrantee).size());
            } else {
                // In S3, objects are made private immediately.
                url = new URL(linkUrlPrefix + "/" + bucketName + "/" + RestUtils.encodeUrlString(publicKey));
                assertEquals("Expected denied access (403) error", 403, ((HttpURLConnection) url
                    .openConnection()).getResponseCode());
            }

            // Clean-up.
            service.deleteObject(bucketName, privateKey);
            service.deleteObject(bucketName, publicKey);
            service.deleteObject(bucketName, publicKey2);
        } finally {
            cleanupBucketForTest("testACLManagement");
        }
    }

    public void testACLManagementViaRestHeaders() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testACLManagementViaRestHeaders");

        AccessControlList publicHeaderAcl = null;
        if (service instanceof GoogleStorageService) {
            publicHeaderAcl = GSAccessControlList.REST_CANNED_PUBLIC_READ;
        } else {
            publicHeaderAcl = AccessControlList.REST_CANNED_PUBLIC_READ;
        }

        try {
            // Try to create public object using HTTP header ACL settings.
            String publicKey = "PublicObject";
            StorageObject object = new StorageObject(publicKey);
            object.setAcl(publicHeaderAcl);
            object.setOwner(bucket.getOwner());

            try {
                service.putObject(bucket.getName(), object);
                URL url = new URL("https://" + service.getEndpoint()
                    + "/" + bucket.getName() + "/" + publicKey);
                assertEquals("Expected public access (200)",
                        200, ((HttpURLConnection)url.openConnection()).getResponseCode());
            } finally {
                service.deleteObject(bucket.getName(), object.getKey());
            }
        } finally {
            cleanupBucketForTest("testACLManagementViaRestHeaders");
        }
    }

    public void testObjectListing() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testObjectListing");
        String bucketName = bucket.getName();

        try {
            // Represent a directory structure in S3.
            List<StorageObject> objectsList = new ArrayList<StorageObject>();
            objectsList.add(new StorageObject("dir1"));
            objectsList.add(new StorageObject("dir1/doc1Level1"));
            objectsList.add(new StorageObject("dir1/doc2level1"));
            objectsList.add(new StorageObject("dir1/dir1Level1"));
            objectsList.add(new StorageObject("dir1/dir1Level1/doc1Level2"));
            objectsList.add(new StorageObject("dir1/dir1Level1/dir1Level2"));
            objectsList.add(new StorageObject("dir1/dir1Level1/dir1Level2/doc1Level3"));

            // Create objects
            for (StorageObject object: objectsList) {
                service.putObject(bucketName, object);
            }

            StorageObject[] objects = null;

            // List all items in directory.
            objects = service.listObjects(bucketName);
            assertEquals("Incorrect number of objects in directory structure",
                objectsList.size(), objects.length);

            // Check all objects have bucket name property set
            for (StorageObject object: objects) {
                assertEquals(bucketName, object.getBucketName());
            }

            // List items in chunks of size 2, ensure we get a total of seven.
            int chunkedObjectsCount = 0;
            int chunkedIterationsCount = 0;
            String priorLastKey = null;
            do {
                StorageObjectsChunk chunk = service.listObjectsChunked(
                    bucketName, null, null, 2, priorLastKey);
                priorLastKey = chunk.getPriorLastKey();
                chunkedObjectsCount += chunk.getObjects().length;
                chunkedIterationsCount++;
            } while (priorLastKey != null);
            assertEquals("Chunked bucket listing retreived incorrect number of objects",
                objectsList.size(), chunkedObjectsCount);
            assertEquals("Chunked bucket listing ran for an unexpected number of iterations",
                (objectsList.size() + 1) / 2, chunkedIterationsCount);

            // List objects with a prefix and delimiter to check common prefixes.
            StorageObjectsChunk chunk = service.listObjectsChunked(
                bucketName, "dir1/", "/", 100, null);
            assertEquals("Chunked bucket listing with prefix and delimiter retreived incorrect number of objects",
                3, chunk.getObjects().length);
            assertEquals("Chunked bucket listing with prefix and delimiter retreived incorrect number of common prefixes",
                1, chunk.getCommonPrefixes().length);

            // List the same items with a prefix.
            objects = service.listObjects(bucketName, "dir1", null);
            assertEquals("Incorrect number of objects matching prefix", 7, objects.length);

            // List items up one directory with a prefix (will include dir1Level1)
            objects = service.listObjects(bucketName, "dir1/dir1Level1", null);
            assertEquals("Incorrect number of objects matching prefix", 4, objects.length);

            // List items up one directory with a prefix (will not include dir1Level1)
            objects = service.listObjects(bucketName, "dir1/dir1Level1/", null);
            assertEquals("Incorrect number of objects matching prefix", 3, objects.length);

            // Try a prefix matching no object keys.
            objects = service.listObjects(bucketName, "dir1-NonExistent", null);
            assertEquals("Expected no results", 0, objects.length);

            // Use delimiter with an partial prefix.
            objects = service.listObjects(bucketName, "dir", "/");
            assertEquals("Expected no results", 1, objects.length);

            // Use delimiter to find item dir1 only.
            objects = service.listObjects(bucketName, "dir1", "/");
            assertEquals("Incorrect number of objects matching prefix and delimiter", 1, objects.length);

            // Use delimiter to find items within dir1 only.
            objects = service.listObjects(bucketName, "dir1/", "/");
            assertEquals("Incorrect number of objects matching prefix and delimiter", 3, objects.length);

            // List items up one directory with prefix and delimiter (will include only dir1Level1)
            objects = service.listObjects(bucketName, "dir1/dir1Level1", "/");
            assertEquals("Incorrect number of objects matching prefix", 1, objects.length);

            // List items up one directory with prefix and delimiter (will include only contents of dir1Level1)
            objects = service.listObjects(bucketName, "dir1/dir1Level1/", "/");
            assertEquals("Incorrect number of objects matching prefix", 2, objects.length);

            // Clean up.
            for (StorageObject object: objectsList) {
                service.deleteObject(bucketName, object.getKey());
            }
        } finally {
            cleanupBucketForTest("testObjectListing");
        }
    }

    public void testHashVerifiedUploads() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testHashVerifiedUploads");
        String bucketName = bucket.getName();

        try {
            // Create test object with an MD5 hash of the data.
            String dataString = "Text for MD5 hashing...";
            StorageObject object = new StorageObject("Testing MD5 Hashing", dataString);
            object.setContentType("text/plain");

            // Calculate hash data for object.
            byte[] md5Hash = ServiceUtils.computeMD5Hash(dataString.getBytes());

            // Ensure that using an invalid hash value fails.
            try {
                object.addMetadata("Content-MD5", "123");
                service.putObject(bucketName, object);
                fail("Should have failed due to invalid hash value");
            } catch (ServiceException e) {
                assertTrue("Expected error code indicating invalid md5 hash",
                    "InvalidDigest".equals(e.getErrorCode())  // S3 error code
                    || "BadDigest".equals(e.getErrorCode())   // GS error code
                    );
            }
            object = new StorageObject("Testing MD5 Hashing", dataString);

            // Ensure that using the wrong hash value fails.
            try {
                byte[] incorrectHash = new byte[md5Hash.length];
                System.arraycopy(md5Hash, 0, incorrectHash, 0, incorrectHash.length);
                incorrectHash[0] = incorrectHash[1];
                object.setMd5Hash(incorrectHash);
                service.putObject(bucketName, object);
                fail("Should have failed due to incorrect hash value");
            } catch (ServiceException e) {
                assertEquals("Expected error code indicating invalid md5 hash", "BadDigest", e.getErrorCode());
            }
            object = new StorageObject("Testing MD5 Hashing", dataString);

            // Ensure that correct hash value succeeds.
            object.setMd5Hash(md5Hash);
            StorageObject resultObject = service.putObject(bucketName, object);

            // Ensure the ETag result matches the hex-encoded MD5 hash.
            assertEquals("Hex-encoded MD5 hash should match ETag", resultObject.getETag(),
                ServiceUtils.toHex(md5Hash));

            // Ensure we can convert the hex-encoded ETag to Base64 that matches the Base64 md5 hash.
            String md5HashBase64 = ServiceUtils.toBase64(md5Hash);
            String eTagBase64 = ServiceUtils.toBase64(ServiceUtils.fromHex(resultObject.getETag()));
            assertEquals("Could not convert ETag and MD5 hash to matching Base64-encoded strings",
                md5HashBase64, eTagBase64);

            // Clean up.
            service.deleteObject(bucketName, object.getKey());
        } finally {
            cleanupBucketForTest("testHashVerifiedUploads");
        }
    }

    public void testIsObjectInBucket() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testIsObjecInBucket");
        String bucketName = bucket.getName();

        try {
            service.putObject(bucketName, new StorageObject("does-exist"));

            assertTrue(service.isObjectInBucket(bucketName, "does-exist"));

            assertFalse(service.isObjectInBucket(bucketName, "does-not-exist"));
        } finally {
            cleanupBucketForTest("testIsObjecInBucket");
        }
    }

    public void testThreadedStorageService() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testThreadedStorageService");
        String bucketName = bucket.getName();

        try {
            final int[] createObjectsEventCount = new int[] {0};
            final int[] getObjectHeadsEventCount = new int[] {0};
            final List<StorageObject> getObjectsList = new ArrayList<StorageObject>();
            final int[] deleteObjectsEventCount = new int[] {0};

            // Multi-threaded service with adaptor to count event occurrences.
            ThreadedStorageService threadedService = new ThreadedStorageService(
                service,
                new StorageServiceEventAdaptor() {
                    @Override
                    public void event(CreateObjectsEvent event) {
                        if (CreateObjectsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                            createObjectsEventCount[0] += event.getCreatedObjects().length;
                        }
                    }

                    @Override
                    public void event(GetObjectHeadsEvent event) {
                        if (GetObjectHeadsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                            getObjectHeadsEventCount[0] += event.getCompletedObjects().length;
                        }
                    }

                    @Override
                    public void event(GetObjectsEvent event) {
                        if (GetObjectsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                            for (StorageObject object: event.getCompletedObjects()) {
                                getObjectsList.add(object);
                            }
                        }
                    }

                    @Override
                    public void event(DeleteObjectsEvent event) {
                        if (DeleteObjectsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                            deleteObjectsEventCount[0] += event.getDeletedObjects().length;
                        }
                    }
            });

            assertEquals(0, createObjectsEventCount[0]);
            assertEquals(0, getObjectHeadsEventCount[0]);
            assertEquals(0, getObjectsList.size());
            assertEquals(0, deleteObjectsEventCount[0]);

            StorageObject[] objects = new StorageObject[] {
                new StorageObject("one.txt", "Some data"),
                new StorageObject("twö.txt", "Some data"),
                new StorageObject("thréè.txt", "Some data"),
                new StorageObject("fôür.txt", "Some data"),
                new StorageObject("fîvæ∫.txt", "Some data")
            };

            // Upload multiple objects
            boolean success = threadedService.putObjects(bucketName, objects);
            assertTrue(success);
            assertEquals(objects.length, createObjectsEventCount[0]);
            assertEquals(0, getObjectHeadsEventCount[0]);
            assertEquals(0, getObjectsList.size());
            assertEquals(0, deleteObjectsEventCount[0]);

            // Retrieve details for multiple objects
            success = threadedService.getObjectsHeads(bucketName, objects);
            assertTrue(success);
            assertEquals(objects.length, createObjectsEventCount[0]);
            assertEquals(objects.length, getObjectHeadsEventCount[0]);
            assertEquals(0, getObjectsList.size());
            assertEquals(0, deleteObjectsEventCount[0]);

            // Retrieve data for multiple objects
            success = threadedService.getObjects(bucketName, objects);
            assertTrue(success);
            assertEquals(objects.length, createObjectsEventCount[0]);
            assertEquals(objects.length, getObjectHeadsEventCount[0]);
            assertEquals(objects.length, getObjectsList.size());
            assertEquals(0, deleteObjectsEventCount[0]);
            // Check all objects retrieved have expected data content.
            for (StorageObject getObject: getObjectsList) {
                // TODO: Google Storage doesn't reliably return Content-Length in a GET
                if (!TARGET_SERVICE_GS.equals(getTargetService())) {
                    assertEquals("Some data".length(), getObject.getContentLength());
                }
                String objectData = ServiceUtils.readInputStreamToString(
                    getObject.getDataInputStream(), Constants.DEFAULT_ENCODING);
                assertEquals("Some data", objectData);
            }

            // Delete multiple objects
            success = threadedService.deleteObjects(bucketName, objects);
            assertTrue(success);
            assertEquals(objects.length, createObjectsEventCount[0]);
            assertEquals(objects.length, getObjectHeadsEventCount[0]);
            assertEquals(objects.length, getObjectsList.size());
            assertEquals(objects.length, deleteObjectsEventCount[0]);

            StorageObject[] listedObjects = service.listObjects(bucketName);
            assertEquals(0, listedObjects.length);
        } finally {
            cleanupBucketForTest("testThreadedStorageService");
        }
    }

    public void testSimpleThreadedStorageService() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testSimpleThreadedStorageService");
        String bucketName = bucket.getName();

        try {
            SimpleThreadedStorageService simpleThreadedService =
                new SimpleThreadedStorageService(service);

            StorageObject[] objects = new StorageObject[] {
                new StorageObject("1-one.txt", "Some data"),
                new StorageObject("2-twö.txt", "Some data"),
                new StorageObject("3-thréè.txt", "Some data"),
                new StorageObject("4-fôür.txt", "Some data"),
                new StorageObject("5-fîvæ∫.txt", "Some data")
            };

            // Upload multiple objects
            StorageObject[] putObjects =
                simpleThreadedService.putObjects(bucketName, objects);
            StorageObject[] listedObjects = service.listObjects(bucketName);
            assertEquals(objects.length, listedObjects.length);

            // Retrieve details for multiple objects
            StorageObject[] headObjects = simpleThreadedService.getObjectsHeads(bucketName, objects);
            assertEquals(objects.length, headObjects.length);
            Arrays.sort(headObjects, new Comparator<StorageObject>() {
                public int compare(StorageObject o1, StorageObject o2) {
                    return o1.getKey().compareTo(o2.getKey());
                }
            });
            for (int i = 0; i < objects.length; i++) {
                assertEquals(objects[i].getKey(), headObjects[i].getKey());
                assertEquals("Some data".length(), headObjects[i].getContentLength());
            }

            // Retrieve details for objects, some of which are missing but which we don't
            // want to cause an error.
            class Http404ErrorPermitter extends ErrorPermitter {
                @Override
                public boolean isPermitted(ServiceException ex) {
                    return ex.getResponseCode() == 404;
                }
            }
            // Prepare object keys combining existing and non-existent key names
            String objectKeys[] = new String[objects.length + 2];
            for (int i = 0; i < objects.length; i++) {
                objectKeys[i] = objects[i].getKey();
            }
            objectKeys[objects.length] = "missing-object-key-1";
            objectKeys[objects.length + 1] = "missing-object-key-2";
            StorageObject[] headObjectsWithMissing = simpleThreadedService.getObjectsHeads(
                bucketName, objectKeys, new Http404ErrorPermitter());
            assertEquals(objects.length + 2, headObjectsWithMissing.length);
            for (int i = 0; i < objects.length; i++) {
                assertEquals(objects[i].getKey(), headObjectsWithMissing[i].getKey());
                assertEquals("Some data".length(), headObjectsWithMissing[i].getContentLength());
            }
            // Ensure we got ThrowableBearingStorageObject results with relevant info
            assertEquals("missing-object-key-1", headObjectsWithMissing[objects.length].getKey());
            assertEquals("missing-object-key-2", headObjectsWithMissing[objects.length + 1].getKey());
            for (int i = objects.length; i < objects.length + 2; i++) {
                assertEquals(
                    ThrowableBearingStorageObject.class, headObjectsWithMissing[i].getClass());
                assertEquals(404,
                    ((ServiceException)
                        ((ThrowableBearingStorageObject)headObjectsWithMissing[i])
                        .getThrowable()).getResponseCode());
            }

            // Retrieve data for multiple objects
            StorageObject[] getObjects = simpleThreadedService.getObjects(bucketName, objects);
            assertEquals(objects.length, getObjects.length);
            for (int i = 0; i < objects.length; i++) {
                // TODO: Google Storage doesn't reliably return Content-Length in a GET
                if (!TARGET_SERVICE_GS.equals(getTargetService())) {
                    assertEquals("Some data".length(), getObjects[i].getContentLength());
                }
                // Check all objects retrieved have expected data content.
                assertEquals("Some data", ServiceUtils.readInputStreamToString(
                    getObjects[i].getDataInputStream(), Constants.DEFAULT_ENCODING));
            }

            // Delete multiple objects
            simpleThreadedService.deleteObjects(bucketName, objects);
            listedObjects = service.listObjects(bucketName);
            assertEquals(0, listedObjects.length);
        } finally {
            cleanupBucketForTest("testSimpleThreadedStorageService");
        }
    }

    public void testRecognizeDnsFriendlyBucketNames() {
        // Valid DNS bucket names
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test-name"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test.domain.name"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("test-domain.name"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName(
            "this-bucket-name-is-not-too-long-with-63-chars--less-than-limit"));

        // IP-like, but not actual IP address numbers allowed
        assertTrue(ServiceUtils.isBucketNameValidDNSName("1234"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("123.4.5.6789"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("123.456"));
        assertTrue(ServiceUtils.isBucketNameValidDNSName("123.456.789"));

        // Invalid DNS bucket names
        assertFalse(ServiceUtils.isBucketNameValidDNSName(null));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("Capitalized"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("ab")); // Too short
        assertFalse(ServiceUtils.isBucketNameValidDNSName(
            "this-bucket-name-is-too-long-with-64-chars--more-than-allowed-63"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("bad-ch@racter"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("empty..segment"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("dash.-starts.segment"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("dash.ends-.segment"));
        // IP address numbers not allowed
        assertFalse(ServiceUtils.isBucketNameValidDNSName("192.12.4.1"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("127.0.0.1"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("123.456.789.012"));
        assertFalse(ServiceUtils.isBucketNameValidDNSName("10.0.0.1"));
    }


    public void testFileComparer() throws Exception {
        RestStorageService service = getStorageService(getCredentials());
        StorageBucket bucket = createBucketForTest("testFileComparer");
        String bucketName = bucket.getName();
        try {
            // Create temporary file and directory structure
            File dummy = File.createTempFile("dummy-", ".txt");
            File rootDir = new File(dummy.getParentFile(), "jets3t-test-" + dummy.getName());
            File parentDir1 = new File(rootDir, "dir1");
            File parentDir2 = new File(rootDir, "dir2");
            parentDir1.mkdirs();
            parentDir2.mkdirs();
            File local1 = File.createTempFile("one", ".txt", parentDir1);
            File local2 = File.createTempFile("two", ".txt", parentDir1);
            File local3 = File.createTempFile("three", " ثلاثة.txt", parentDir2);
            String local1Path = parentDir1.getName() + File.separator + local1.getName();
            String local2Path = parentDir1.getName() + File.separator + local2.getName();
            String local3Path = parentDir2.getName() + File.separator + local3.getName();

            FileComparer comparer = new FileComparer(new Jets3tProperties());
            // Build a file map of local files
            Map<String, String> objectKeyToFilepathMap = comparer.buildObjectKeyToFilepathMap(
                new File[] {parentDir1, parentDir2}, "", true);
            assertEquals(5, objectKeyToFilepathMap.size());
            assertTrue(objectKeyToFilepathMap.keySet().contains(local1Path));

            // Upload local directories and files to storage service
            service.putObject(bucketName, ObjectUtils.createObjectForUpload(
                parentDir1.getName() + "/", parentDir1, null, false));
            service.putObject(bucketName, ObjectUtils.createObjectForUpload(
                parentDir2.getName() + "/", parentDir2, null, false));
            service.putObject(bucketName, ObjectUtils.createObjectForUpload(
                local1Path, local1, null, false));
            service.putObject(bucketName, ObjectUtils.createObjectForUpload(
                local2Path, local2, null, false));
            service.putObject(bucketName, ObjectUtils.createObjectForUpload(
                local3Path, local3, null, false));

            // Build a map of objects in storage service
            Map<String, StorageObject> objectMap = comparer.buildObjectMap(
                service, bucket.getName(), "", objectKeyToFilepathMap, false, false, null, null);
            assertEquals(5, objectMap.size());
            assertTrue(objectMap.keySet().contains(local3Path));

            // Compare local and remote objects -- should be identical
            FileComparerResults comparerResults =
                comparer.buildDiscrepancyLists(objectKeyToFilepathMap, objectMap);
            assertEquals(5, comparerResults.alreadySynchronisedKeys.size());
            assertEquals(0, comparerResults.onlyOnClientKeys.size());
            assertEquals(0, comparerResults.onlyOnServerKeys.size());
            assertEquals(0, comparerResults.updatedOnClientKeys.size());
            assertEquals(0, comparerResults.updatedOnServerKeys.size());

            // Update 1 local and 1 remote file, then confirm discrepancies
            byte[] data = "Updated local file".getBytes("UTF-8");
            FileOutputStream local1FOS = new FileOutputStream(local1);
            local1FOS.write(data);
            local1FOS.close();
            // Ensure local file's timestamp differs by at least 1 sec
            local1.setLastModified(local1.lastModified() + 1000);

            data = "Updated remote file".getBytes("UTF-8");
            StorageObject remoteObject = new StorageObject(local3Path);
            // Ensure remote file's JetS3t timestamp differs from local file by at least 1 sec
            remoteObject.addMetadata(Constants.METADATA_JETS3T_LOCAL_FILE_DATE,
                ServiceUtils.formatIso8601Date(new Date(local3.lastModified() + 1000)));
            remoteObject.setDataInputStream(new ByteArrayInputStream(data));
            remoteObject.setContentLength(data.length);
            service.putObject(bucketName, remoteObject);

            objectMap = comparer.buildObjectMap(
                service, bucket.getName(), "", objectKeyToFilepathMap, false, false, null, null);

            comparerResults =
                comparer.buildDiscrepancyLists(objectKeyToFilepathMap, objectMap);
            assertEquals(3, comparerResults.alreadySynchronisedKeys.size());
            assertEquals(0, comparerResults.onlyOnClientKeys.size());
            assertEquals(0, comparerResults.onlyOnServerKeys.size());
            assertEquals(1, comparerResults.updatedOnClientKeys.size());
            assertTrue(comparerResults.updatedOnClientKeys.contains(local1Path));
            assertEquals(1, comparerResults.updatedOnServerKeys.size());
            assertTrue(comparerResults.updatedOnServerKeys.contains(local3Path));

            // Create new local and remote objects, then confirm discrepancies
            File local4 = File.createTempFile("four", ".txt", parentDir2);
            String local4Path = parentDir2.getName() + File.separator + local4.getName();
            remoteObject = new StorageObject("new-on-service.txt");
            service.putObject(bucketName, remoteObject);

            objectKeyToFilepathMap = comparer.buildObjectKeyToFilepathMap(
                new File[] {parentDir1, parentDir2}, "", true);
            objectMap = comparer.buildObjectMap(
                service, bucket.getName(), "", objectKeyToFilepathMap, false, false, null, null);

            comparerResults = comparer.buildDiscrepancyLists(objectKeyToFilepathMap, objectMap);
            assertEquals(3, comparerResults.alreadySynchronisedKeys.size());
            assertTrue(comparerResults.alreadySynchronisedKeys.contains(local2Path));
            assertEquals(1, comparerResults.onlyOnClientKeys.size());
            assertTrue(comparerResults.onlyOnClientKeys.contains(local4Path));
            assertEquals(1, comparerResults.onlyOnServerKeys.size());
            assertTrue(comparerResults.onlyOnServerKeys.contains("new-on-service.txt"));
            assertEquals(1, comparerResults.updatedOnClientKeys.size());
            assertTrue(comparerResults.updatedOnClientKeys.contains(local1Path));
            assertEquals(1, comparerResults.updatedOnServerKeys.size());
            assertTrue(comparerResults.updatedOnServerKeys.contains(local3Path));

            // Clean up after prior test
            local4.delete();
            service.deleteObject(bucketName, "new-on-service.txt");

            // Remove local file and remote object, then confirm discrepancies
            local3.delete();
            service.deleteObject(bucketName, local1Path);

            objectKeyToFilepathMap = comparer.buildObjectKeyToFilepathMap(
                new File[] {parentDir1, parentDir2}, "", true);
            objectMap = comparer.buildObjectMap(
                service, bucket.getName(), "", objectKeyToFilepathMap, false, false, null, null);

            comparerResults = comparer.buildDiscrepancyLists(objectKeyToFilepathMap, objectMap);
            assertEquals(1, comparerResults.onlyOnClientKeys.size());
            assertTrue(comparerResults.onlyOnClientKeys.contains(local1Path));
            assertEquals(1, comparerResults.onlyOnServerKeys.size());
            assertTrue(comparerResults.onlyOnServerKeys.contains(local3Path));
        } finally {
            cleanupBucketForTest("testFileComparer");
        }
    }

    public void testBucketLogging() throws Exception {
        StorageService service = getStorageService(getCredentials());

        // TODO Test case doesn't work when accessing GS via S3 service (perhaps impossible
        // due to Google Storage API?)
        if (TARGET_SERVICE_GS.equals(getTargetService()) && !(service instanceof GoogleStorageService)) {
            return;
        }

        try {
            StorageBucket bucket = createBucketForTest("testBucketLogging");
            String bucketName = bucket.getName();

            // Check logging status is false
            StorageBucketLoggingStatus loggingStatus = null;
            if (service instanceof GoogleStorageService) {
                loggingStatus = ((GoogleStorageService)service)
                    .getBucketLoggingStatus(bucket.getName());
            } else {
                loggingStatus = ((S3Service)service)
                    .getBucketLoggingStatus(bucket.getName());
            }
            assertFalse("Expected logging to be disabled for bucket " + bucketName,
                loggingStatus.isLoggingEnabled());

            // Enable logging (non-existent target bucket)
            // NOTE: throws Exception with S3, but not with GS!
            try {
                StorageBucketLoggingStatus newLoggingStatus = getBucketLoggingStatus(
                    getCredentials().getAccessKey() + ".NonExistentBucketName", "access-log-");
                if (service instanceof GoogleStorageService) {
                    ((GoogleStorageService)service)
                        .setBucketLoggingStatus(bucket.getName(), (GSBucketLoggingStatus)newLoggingStatus);
                } else {
                    ((S3Service)service)
                        .setBucketLoggingStatus(bucket.getName(), (S3BucketLoggingStatus)newLoggingStatus, true);
                    fail("Using non-existent target bucket should have caused an exception");
                }
            } catch (Exception e) {
            }

            // Enable logging (in same bucket)
            StorageBucketLoggingStatus newLoggingStatus = getBucketLoggingStatus(bucketName, "access-log-");
            if (service instanceof GoogleStorageService) {
                ((GoogleStorageService)service)
                    .setBucketLoggingStatus(bucket.getName(), (GSBucketLoggingStatus)newLoggingStatus);
            } else {
                ((S3Service)service)
                    .setBucketLoggingStatus(bucket.getName(), (S3BucketLoggingStatus)newLoggingStatus, true);
            }
            if (service instanceof GoogleStorageService) {
                loggingStatus = ((GoogleStorageService)service)
                    .getBucketLoggingStatus(bucket.getName());
            } else {
                loggingStatus = ((S3Service)service)
                    .getBucketLoggingStatus(bucket.getName());
            }
            assertTrue("Expected logging to be enabled for bucket " + bucketName,
                loggingStatus.isLoggingEnabled());
            assertEquals("Target bucket", bucketName, loggingStatus.getTargetBucketName());
            assertEquals("Log file prefix", "access-log-", loggingStatus.getLogfilePrefix());

            // Add TargetGrants ACLs for log files (S3 only)
            if (!(service instanceof GoogleStorageService)) {
                ((S3BucketLoggingStatus)newLoggingStatus).addTargetGrant(new GrantAndPermission(
                    GroupGrantee.ALL_USERS, Permission.PERMISSION_READ));
                ((S3BucketLoggingStatus)newLoggingStatus).addTargetGrant(new GrantAndPermission(
                    GroupGrantee.AUTHENTICATED_USERS, Permission.PERMISSION_READ_ACP));
                ((S3Service)service)
                    .setBucketLoggingStatus(bucket.getName(), (S3BucketLoggingStatus)newLoggingStatus, true);
                loggingStatus = ((S3Service)service)
                    .getBucketLoggingStatus(bucket.getName());
                assertEquals(2, ((S3BucketLoggingStatus)loggingStatus).getTargetGrants().length);
                GrantAndPermission gap = ((S3BucketLoggingStatus)loggingStatus).getTargetGrants()[0];
                assertEquals(gap.getGrantee().getIdentifier(), GroupGrantee.ALL_USERS.getIdentifier());
                assertEquals(gap.getPermission(), Permission.PERMISSION_READ);
                gap = ((S3BucketLoggingStatus)loggingStatus).getTargetGrants()[1];
                assertEquals(gap.getGrantee().getIdentifier(), GroupGrantee.AUTHENTICATED_USERS.getIdentifier());
                assertEquals(gap.getPermission(), Permission.PERMISSION_READ_ACP);
            }

            // Disable logging
            newLoggingStatus = getBucketLoggingStatus(null, null);
            if (service instanceof GoogleStorageService) {
                ((GoogleStorageService)service)
                    .setBucketLoggingStatus(bucket.getName(), (GSBucketLoggingStatus)newLoggingStatus);
            } else {
                ((S3Service)service)
                    .setBucketLoggingStatus(bucket.getName(), (S3BucketLoggingStatus)newLoggingStatus, true);
            }
            if (service instanceof GoogleStorageService) {
                loggingStatus = ((GoogleStorageService)service)
                    .getBucketLoggingStatus(bucket.getName());
            } else {
                loggingStatus = ((S3Service)service)
                    .getBucketLoggingStatus(bucket.getName());
            }
            assertFalse("Expected logging to be disabled for bucket " + bucketName,
                loggingStatus.isLoggingEnabled());
        } finally {
            cleanupBucketForTest("testBucketLogging");
        }
    }


}
