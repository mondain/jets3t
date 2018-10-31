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
package org.jets3t.samples;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jets3t.service.S3ObjectsChunk;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.multithread.ListObjectsEvent;
import org.jets3t.service.multithread.S3ServiceEventAdaptor;
import org.jets3t.service.multithread.S3ServiceMulti;
import org.jets3t.service.security.AWSCredentials;

/**
 * Demonstrates how to use the {@link S3ServiceMulti#listObjects(String, String[], String, long)}
 * method to list multiple "partitions" of an S3 bucket at once, using multiple
 * threads to list objects matching different prefixes.
 *
 * @author James Murty
 */
public class ThreadedObjectListing {

    public static String TEST_PROPERTIES_FILENAME = "test.properties";

    public static void main(String[] args) throws Exception {
        /*
         * Set these values to test the multi-theaded listing.
         *
         * If you set delimiter to null, only a standard single-threaded listing
         * will be performed. If you set the delimiter to a string that will
         * identify a number of "subdirectory" partitions in your bucket, a
         * threaded listing will be performed for each such partition.
         */
        final String bucketName = "jets3t";
        final String delimiter = "/";

        AWSCredentials awsCredentials = SamplesUtils.loadAWSCredentials();
        S3Service restService = new RestS3Service(awsCredentials);

        final List allObjects = Collections.synchronizedList(new ArrayList());
        final Exception s3ServiceExceptions[] = new Exception[1];

        /*
         * Identify top-level "subdirectory" names in a bucket by performing a
         * standard object listing with a delimiter string.
         */
        long startTime = System.currentTimeMillis();

        // Find all the objects and common prefixes at the top level.
        StorageObjectsChunk initialChunk = restService.listObjectsChunked(
            bucketName, null, delimiter, 1000, null, true);

        long totalElapsedTime = System.currentTimeMillis() - startTime;

        // We will use the common prefix strings, if any, to perform sub-listings
        final String[] commonPrefixes = initialChunk.getCommonPrefixes();

        if (commonPrefixes.length > 0) {
            System.out.println("Performing sub-listings for common prefixes: "
                + Arrays.asList(commonPrefixes));

            /*
             * Create a S3ServiceMulti object with an event listener that responds to
             * ListObjectsEvent notifications and populates a complete object listing.
             */
            final S3ServiceMulti s3Multi = new S3ServiceMulti(restService, new S3ServiceEventAdaptor() {
                @Override
                public void s3ServiceEventPerformed(ListObjectsEvent event) {
                    if (ListObjectsEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                        Iterator chunkIter = event.getChunkList().iterator();
                        while (chunkIter.hasNext()) {
                            StorageObjectsChunk chunk = (StorageObjectsChunk) chunkIter.next();

                            System.out.println("Listed " + chunk.getObjects().length
                                + " objects for sub-listing with prefix: '"
                                + chunk.getPrefix() + "'");

                            allObjects.addAll(Arrays.asList(chunk.getObjects()));
                        }
                    } else if (ListObjectsEvent.EVENT_ERROR == event.getEventCode()) {
                        s3ServiceExceptions[0] = new S3ServiceException(
                            "Failed to list all objects in S3 bucket",
                            event.getErrorCause());
                    }
                }
            });

            startTime = System.currentTimeMillis();

            /*
             * Perform a multi-threaded listing, where each common prefix string
             * will be used as the prefix for a separate listing thread.
             */
            (new Thread() {
                @Override
                public void run() {
                    s3Multi.listObjects(bucketName, commonPrefixes, null, 1000);
                };
            }).run();

            long threadedElapsedTime = System.currentTimeMillis() - startTime;
            totalElapsedTime += threadedElapsedTime;

            System.out.println("\nTime to list " + allObjects.size() + " objects with "
                + commonPrefixes.length + " sub-listings: " + threadedElapsedTime + " ms");
        }

        // Add top-level objects to the complete listing
        allObjects.addAll(Arrays.asList(initialChunk.getObjects()));

        System.out.println("\nTotal time to list " + allObjects.size()
            + " objects: " + totalElapsedTime + " ms");
    }


}
