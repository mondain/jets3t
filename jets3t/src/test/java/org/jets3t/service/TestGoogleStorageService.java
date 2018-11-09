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

import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageService;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSBucketLoggingStatus;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageBucketLoggingStatus;
import org.jets3t.service.security.GSCredentials;
import org.jets3t.service.security.ProviderCredentials;

/**
 * Test cases specific to general S3 compatibility -- that is, features supported by
 * both S3 and Google Storage.
 *
 * @author James Murty
 */
public class TestGoogleStorageService extends BaseStorageServiceTests {

    public TestGoogleStorageService() throws Exception {
        super();
    }

    @Override
    protected String getTargetService() {
        return TARGET_SERVICE_GS;
    }

    @Override
    protected ProviderCredentials getCredentials() {
        return new GSCredentials(
                testProperties.getProperty("gsservice.accesskey"),
                testProperties.getProperty("gsservice.secretkey"));
    }

    @Override
    protected RestStorageService getStorageService(ProviderCredentials credentials) throws ServiceException {
        return new GoogleStorageService(credentials);
    }

    @Override
    protected AccessControlList buildAccessControlList() {
        return new GSAccessControlList();
    }

    @Override
    protected StorageBucketLoggingStatus getBucketLoggingStatus(
            String targetBucketName, String logfilePrefix) throws Exception {
        return new GSBucketLoggingStatus(targetBucketName, logfilePrefix);
    }

    /**
     * Test creating a bucket with the canned project-private ACL.
     *
     * @throws Exception
     */
    public void testProjectPrivateAcl() throws Exception {
        String bucketName = getBucketNameForTest("testProjectPrivateAcl");
        GoogleStorageService service = (GoogleStorageService) getStorageService(getCredentials());
        try {
            service.createBucket(bucketName, null,
                    GSAccessControlList.REST_CANNED_PROJECT_PRIVATE);
        }
        finally {
            // Clean up
            service.deleteBucket(bucketName);
        }

    }

    public void testCreateBucketInProject() throws Exception {
        String bucketName = getBucketNameForTest("testCreateBucketInProject");
        String projectId = testProperties.getProperty("gsservice.project_id");
        GoogleStorageService service = (GoogleStorageService) getStorageService(getCredentials());
        try {
            service.createBucket(bucketName, null,
                    GSAccessControlList.REST_CANNED_PROJECT_PRIVATE, projectId);
        }
        finally {
            // Clean up
            service.deleteBucket(bucketName);
        }

    }

    public void testListBucketsByProject() throws Exception {

        String projectId = testProperties.getProperty("gsservice.project_id");

        // Ensure newly-created bucket is listed
        String bucketName = getBucketNameForTest("testListBucketsByProject");
        try {
            GoogleStorageService service = (GoogleStorageService)
                    getStorageService(getCredentials());

            service.createBucket(bucketName, null,
                    GSAccessControlList.REST_CANNED_PROJECT_PRIVATE, projectId);
            GSBucket[] buckets = service.listAllBuckets(projectId);
            boolean found = false;
            for(StorageBucket bucket : buckets) {
                found = (bucket.getName().equals(bucketName)) || found;
            }
            assertTrue("Newly-created bucket was not listed", found);
        }
        finally {
            cleanupBucketForTest("testListBucketsByProject");
        }
    }

    protected StorageBucket createBucketForTest(String testName, String location) throws Exception {
        String bucketName = getBucketNameForTest(testName);
        StorageService service = getStorageService(getCredentials());
        String projectId = testProperties.getProperty("gsservice.project_id");
        return ((GoogleStorageService)service).createBucket(bucketName, null, null, projectId);
    }
}
