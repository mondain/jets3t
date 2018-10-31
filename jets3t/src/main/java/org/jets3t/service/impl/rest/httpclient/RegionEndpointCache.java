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
package org.jets3t.service.impl.rest.httpclient;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache to store mappings from a bucket name to a region, used to help with
 * request signing for AWS version 4 requests where you need to know a bucket's
 * region before you can correctly sign requests that operate on that bucket.
 *
 * @author jmurty
 */
public class RegionEndpointCache {
    private Map<String, String> bucketNameToRegionMap = new HashMap<String, String>();

    public String getRegionForBucketName(String bucketName) {
        if (bucketNameToRegionMap.containsKey(bucketName)) {
            return bucketNameToRegionMap.get(bucketName);
        } else {
            return null;
        }
    }

    public String putRegionForBucketName(String bucketName, String region) {
        if (bucketName != null && region != null) {
            return bucketNameToRegionMap.put(bucketName, region);
        } else {
            return null;
        }
    }

    public boolean containsRegionForBucketName(String bucketName) {
        return bucketNameToRegionMap.containsKey(bucketName);
    }

    public boolean containsRegionForAnyBucketName(String region) {
        return bucketNameToRegionMap.containsValue(region);
    }

    public String removeRegionForBucketName(String bucketName) {
        return bucketNameToRegionMap.remove(bucketName);
    }

    public void clear() {
        bucketNameToRegionMap.clear();
    }

}
