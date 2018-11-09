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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.model.GSWebsiteConfig;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.security.OAuth2Credentials;
import org.jets3t.service.security.ProviderCredentials;

import java.util.Arrays;

/**
 * Test Google Storage OAuth Access.
 */
public class TestGoogleStorageServiceOAuth extends TestGoogleStorageService {

    private static OAuth2Credentials savedCredentials;

    public TestGoogleStorageServiceOAuth() throws Exception {
        super();
    }

    @Override
    protected String getTargetService() {
        return TARGET_SERVICE_GS;
    }

    @Override
    protected String getBucketNameForTest(String testName) throws Exception {
        return "test-"
                + getCredentials().getAccessKey().toLowerCase().substring(0, 7)
                + "-"
                + testName.toLowerCase();
    }

    @Override
    protected ProviderCredentials getCredentials() {
        //I've made the credentials a singleton object because otherwise
        //JUnit tries to get a bunch of access tokens, which I suspect is being
        //flagged as a DoS attempt, and hence starts failing  after the first
        //few token fetches.
        synchronized(getClass()) {
            if(savedCredentials == null) {
                savedCredentials = new OAuth2Credentials(
                        testProperties.getProperty("gsservice.client_id"),
                        testProperties.getProperty("gsservice.client_secret"),
                        null,
                        testProperties.getProperty("gsservice.refresh_token"));
            }
        }
        return savedCredentials;
    }

    public void testGSWebsiteConfig() throws Exception {
        // Testing takes place in the us-west-1 location
        GoogleStorageService service = (GoogleStorageService) getStorageService(getCredentials());
        // After setting a website configuration default index document, the DNS bucket
        // endpoint returns the index document
        service.getJetS3tProperties().setProperty("gsservice.disable-dns-buckets", String.valueOf(true));
        StorageBucket bucket = createBucketForTest(
            "testGSWebsiteConfig");
        assertTrue(Arrays.asList(service.listObjects(bucket.getName())).isEmpty());
        String bucketName = bucket.getName();

        String websiteURL = "http://" + bucketName + "."
            // Website location must correspond to bucket location, in this case
            // the US Standard. For website endpoints see:
            // docs.amazonwebservices.com/AmazonS3/latest/dev/WebsiteEndpoints.html
            + "commondatastorage.googleapis.com";

        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet getMethod;

            // Check no existing website config
            assertNull(service.getWebsiteConfig(bucketName).getErrorDocumentKey());
            assertNull(service.getWebsiteConfig(bucketName).getIndexDocumentSuffix());

            // Set index document
            service.setWebsiteConfig(bucketName,
                    new GSWebsiteConfig("index.html"));

            // Confirm index document set
            GSWebsiteConfig config = service.getWebsiteConfig(bucketName);
            assertTrue(config.isWebsiteConfigActive());
            assertEquals("index.html", config.getIndexDocumentSuffix());
            assertNull(config.getErrorDocumentKey());

            // Upload public index document
            S3Object indexObject = new S3Object("index.html", "index.html contents");
            indexObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            service.putObject(bucketName, indexObject);

            // Confirm index document is served at explicit path
            getMethod = new HttpGet(websiteURL + "/index.html");
            HttpResponse response = httpClient.execute(getMethod);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("index.html contents", EntityUtils.toString(response.getEntity()));

            // Confirm index document is served at root path
            // (i.e. website config is effective)
            getMethod = new HttpGet(websiteURL + "/");
            response = httpClient.execute(getMethod);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("index.html contents", EntityUtils.toString(response.getEntity()));

            // Set index document and error document
            service.setWebsiteConfig(bucketName,
                    new GSWebsiteConfig("index.html", "error.html"));

            // Confirm index document and error document set
            config = service.getWebsiteConfig(bucketName);
            assertTrue(config.isWebsiteConfigActive());
            assertEquals("index.html", config.getIndexDocumentSuffix());
            assertEquals("error.html", config.getErrorDocumentKey());

            // Upload public error document
            S3Object errorObject = new S3Object("error.html", "error.html contents");
            errorObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            service.putObject(bucketName, errorObject);

            // Confirm error document served at explicit path
            getMethod = new HttpGet(websiteURL + "/error.html");
            response = httpClient.execute(getMethod);
            assertEquals(200, response.getStatusLine().getStatusCode());
            assertEquals("error.html contents", EntityUtils.toString(response.getEntity()));

            // Confirm error document served instead of 404 Not Found
            getMethod = new HttpGet(websiteURL + "/does-not-exist");
            response = httpClient.execute(getMethod);
            assertEquals(404, response.getStatusLine().getStatusCode());
            assertEquals("error.html contents", EntityUtils.toString(response.getEntity()));

            // Upload private document
            S3Object privateObject = new S3Object("private.html", "private.html contents");
            service.putObject(bucketName, privateObject);

            // Confirm error document served instead for 403 Forbidden
            getMethod = new HttpGet(websiteURL + "/private.html");
            response = httpClient.execute(getMethod);
            assertEquals(403, response.getStatusLine().getStatusCode());

            // Delete website config
            service.deleteWebsiteConfig(bucketName);
            // Confirm website config deleted
            assertNull(service.getWebsiteConfig(bucketName).getErrorDocumentKey());
            assertNull(service.getWebsiteConfig(bucketName).getIndexDocumentSuffix());
        } finally {
            cleanupBucketForTest("testGSWebsiteConfig", true);
        }
    }
}
