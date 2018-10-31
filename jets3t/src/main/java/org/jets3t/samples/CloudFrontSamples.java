/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008-2012 James Murty
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

import java.io.FileInputStream;
import java.util.List;

import org.jets3t.service.CloudFrontService;
import org.jets3t.service.model.cloudfront.CustomOrigin;
import org.jets3t.service.model.cloudfront.Invalidation;
import org.jets3t.service.model.cloudfront.InvalidationSummary;
import org.jets3t.service.model.cloudfront.OriginAccessIdentity;
import org.jets3t.service.model.cloudfront.Distribution;
import org.jets3t.service.model.cloudfront.DistributionConfig;
import org.jets3t.service.model.cloudfront.LoggingStatus;
import org.jets3t.service.model.cloudfront.OriginAccessIdentityConfig;
import org.jets3t.service.model.cloudfront.S3Origin;
import org.jets3t.service.model.cloudfront.StreamingDistribution;
import org.jets3t.service.model.cloudfront.StreamingDistributionConfig;
import org.jets3t.service.security.EncryptionUtil;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Sample code for performing CloudFront service operations.
 */
public class CloudFrontSamples {

    public static void main(String[] args) throws Exception {
        // Construct a CloudFrontService object to interact with the service.
        CloudFrontService cloudFrontService = new CloudFrontService(
            SamplesUtils.loadAWSCredentials());

        // List non-streaming distributions
        Distribution[] bucketDistributions = cloudFrontService.listDistributions();
        for (int i = 0; i < bucketDistributions.length; i++) {
            System.out.println("Distribution " + (i + 1) + ": " + bucketDistributions[i]);
        }

        // List the distributions applied to a given S3 bucket
        bucketDistributions = cloudFrontService.listDistributions("jets3t");
        for (int i = 0; i < bucketDistributions.length; i++) {
            System.out.println("Bucket distribution " + (i + 1) + ": " + bucketDistributions[i]);
        }

        // Create a new public distribution
        String originBucket = "jets3t.s3.amazonaws.com";
        Distribution newDistribution = cloudFrontService.createDistribution(
            new S3Origin(originBucket),
            "" + System.currentTimeMillis(), // Caller reference - a unique string value
            new String[] {"test1.jamesmurty.com"}, // CNAME aliases for distribution
            "Testing", // Comment
            true,  // Distribution is enabled?
            null  // Logging status of distribution (null means disabled)
            );
        System.out.println("New Distribution: " + newDistribution);

        // The ID of the new distribution we will use for testing
        String testDistributionId = newDistribution.getId();

        // List information about a distribution
        Distribution distribution = cloudFrontService.getDistributionInfo(testDistributionId);
        System.out.println("Distribution: " + distribution);

        // List configuration information about a distribution
        DistributionConfig distributionConfig = cloudFrontService.getDistributionConfig(testDistributionId);
        System.out.println("Distribution Config: " + distributionConfig);

        // Update a distribution's configuration to add an extra CNAME alias and enable logging.
        DistributionConfig updatedDistributionConfig = cloudFrontService.updateDistributionConfig(
            testDistributionId,
            null, // origin -- null for no changes
            new String[] {"test1.jamesmurty.com", "test2.jamesmurty.com"}, // CNAME aliases for distribution
            "Another comment for testing", // Comment
            true, // Distribution enabled?
            new LoggingStatus("log-bucket.s3.amazonaws.com", "log-prefix/")  // Distribution logging
            );
        System.out.println("Updated Distribution Config: " + updatedDistributionConfig);

        // Update a distribution's configuration to require secure HTTPS
        // connections, using the RequiredProtocols feature
        updatedDistributionConfig = cloudFrontService.updateDistributionConfig(
            testDistributionId,
            null, // origin -- null for no changes
            new String[] {"test1.jamesmurty.com", "test2.jamesmurty.com"}, // CNAME aliases for distribution
            "HTTPS Only!", // Comment
            true, // Distribution enabled?
            new LoggingStatus("log-bucket.s3.amazonaws.com", "log-prefix/"),  // Distribution logging
            false, // URLs self-signing disabled
            null,  // No other AWS users can sign URLs
            new String[] {"https"}, // RequiredProtocols with HTTPS protocol
            "index.html" // Default Root Object
        );
        System.out.println("HTTPS only distribution Config: " + updatedDistributionConfig);

        // Update a distribution's configuration to change the time-to-live (MinTTL) setting.
        updatedDistributionConfig.setMinTTL(5L);  // Set TTL to 5 seconds
        updatedDistributionConfig = cloudFrontService.updateDistributionConfig(
            testDistributionId, updatedDistributionConfig);
        System.out.println("Distribution Config with custom MinTTL: " + updatedDistributionConfig);

        // Disable a distribution, e.g. so that it may be deleted.
        // The CloudFront service may take some time to disable and deploy the distribution.
        DistributionConfig disabledDistributionConfig = cloudFrontService.updateDistributionConfig(
            testDistributionId, null, new String[] {}, "Deleting distribution", false, null);
        System.out.println("Disabled Distribution Config: " + disabledDistributionConfig);

        // Check whether a distribution is deployed
        distribution = cloudFrontService.getDistributionInfo(testDistributionId);
        System.out.println("Distribution is deployed? " + distribution.isDeployed());

        // Convenience method to disable a distribution prior to deletion
        cloudFrontService.disableDistributionForDeletion(testDistributionId);

        // Delete a distribution (the distribution must be disabled and deployed first)
        cloudFrontService.deleteDistribution(testDistributionId);

        // -----------------------------------------------------------
        // CloudFront Private Distributions - Origin Access Identities
        // -----------------------------------------------------------

        // Create a new origin access identity
        OriginAccessIdentity originAccessIdentity =
            cloudFrontService.createOriginAccessIdentity(null, "Testing");
        System.out.println(originAccessIdentity.toString());

        // List your origin access identities
        List<OriginAccessIdentity> originAccessIdentityList =
            cloudFrontService.getOriginAccessIdentityList();
        System.out.println(originAccessIdentityList);

        // Obtain an origin access identity ID for future use
        OriginAccessIdentity identity = originAccessIdentityList.get(1);
        String originAccessIdentityId = identity.getId();
        System.out.println("originAccessIdentityId: " + originAccessIdentityId);

        // Lookup information about a specific origin access identity
        originAccessIdentity =
            cloudFrontService.getOriginAccessIdentity(originAccessIdentityId);
        System.out.println(originAccessIdentity);

        // Lookup config details for an origin access identity
        OriginAccessIdentityConfig originAccessIdentityConfig =
            cloudFrontService.getOriginAccessIdentityConfig(originAccessIdentityId);
        System.out.println(originAccessIdentityConfig);

        // Update configuration for an origin access identity
        OriginAccessIdentityConfig updatedConfig =
            cloudFrontService.updateOriginAccessIdentityConfig(
                originAccessIdentityId, "New Comment");
        System.out.println(updatedConfig);

        // Delete an origin access identity
        cloudFrontService.deleteOriginAccessIdentity(originAccessIdentityId);

        // --------------------------------------------------------
        // CloudFront Private Distributions - Private Distributions
        // --------------------------------------------------------

        // Create a new private distribution for which signed URLs are *not* required
        originBucket = "jets3t.s3.amazonaws.com";
        Distribution privateDistribution = cloudFrontService.createDistribution(
            new S3Origin(originBucket, originAccessIdentityId),
            "" + System.currentTimeMillis(), // Caller reference - a unique string value
            new String[] {}, // CNAME aliases for distribution
            "New private distribution -- URL signing not required", // Comment
            true,  // Distribution is enabled?
            null,  // Logging status of distribution (null means disabled)
            false, // URLs self-signing disabled
            null,  // No other AWS users can sign URLs
            null,   // No required protocols
            null // No default root object
        );
        System.out.println("New Private Distribution: " + privateDistribution);

        // Update an existing distribution to make it private and require URL signing
        updatedDistributionConfig = cloudFrontService.updateDistributionConfig(
            testDistributionId,
            new S3Origin(originBucket, originAccessIdentityId),
            new String[] {}, // CNAME aliases for distribution
            "Now a private distribution -- URL Signing required", // Comment
            true, // Distribution enabled?
            null, // No distribution logging
            true, // URLs can be self-signed
            null, // No other AWS users can sign URLs
            null,  // No required protocols
            "index.html" //Default Root Object
        );
        System.out.println("Made distribution private: " + updatedDistributionConfig);


        // List active trusted signers for a private distribution
        distribution = cloudFrontService.getDistributionInfo(testDistributionId);
        System.out.println("Active trusted signers: " + distribution.getActiveTrustedSigners());

        // Obtain one of your own (Self) keypair ids that can sign URLs for the distribution
        List selfKeypairIds = (List) distribution.getActiveTrustedSigners().get("Self");
        String keyPairId = (String) selfKeypairIds.get(0);
        System.out.println("Keypair ID: " + keyPairId);

        // -------------------------------------------------------------------------
        // CloudFront Private Distributions - Signed URLs for a private distribution
        // -------------------------------------------------------------------------

        String distributionDomain = "a1b2c3d4e5f6g7.cloudfront.net";
        String privateKeyFilePath = "/path/to/rsa-private-key.pem";
        String s3ObjectKey = "s3/object/key.txt";
        String policyResourcePath = "http://" + distributionDomain + "/" + s3ObjectKey;

        // Convert an RSA PEM private key file to DER bytes
        byte[] derPrivateKey = EncryptionUtil.convertRsaPemToDer(
            new FileInputStream(privateKeyFilePath));


        // Generate a "canned" signed URL to allow access to a specific distribution and object
        String signedUrlCanned = CloudFrontService.signUrlCanned(
            "http://" + distributionDomain + "/" + s3ObjectKey, // Resource URL or Path
            keyPairId,     // Certificate identifier, an active trusted signer for the distribution
            derPrivateKey, // DER Private key data
            ServiceUtils.parseIso8601Date("2009-11-14T22:20:00.000Z") // DateLessThan
            );
        System.out.println(signedUrlCanned);


        // Build a policy document to define custom restrictions for a signed URL
        String policy = CloudFrontService.buildPolicyForSignedUrl(
            policyResourcePath, // Resource path (optional, may include '*' and '?' wildcards)
            ServiceUtils.parseIso8601Date("2009-11-14T22:20:00.000Z"), // DateLessThan
            "0.0.0.0/0", // CIDR IP address restriction (optional, 0.0.0.0/0 means everyone)
            ServiceUtils.parseIso8601Date("2009-10-16T06:31:56.000Z")  // DateGreaterThan (optional)
            );

        // Generate a signed URL using a custom policy document
        String signedUrl = CloudFrontService.signUrl(
            "http://" + distributionDomain + "/" + s3ObjectKey, // Resource URL or Path
            keyPairId,     // Certificate identifier, an active trusted signer for the distribution
            derPrivateKey, // DER Private key data
            policy // Access control policy
            );
        System.out.println(signedUrl);

        // ------------------------------------------------------------
        // CloudFront Streaming Distributions
        //
        // The methods for interacting with streaming distributions are
        // very similar to those for standard distributions
        // ------------------------------------------------------------

        // List your streaming distributions
        StreamingDistribution[] streamingDistributions =
            cloudFrontService.listStreamingDistributions();
        for (int i = 0; i < streamingDistributions.length; i++) {
            System.out.println("Streaming distribution " + (i + 1) + ": " + streamingDistributions[i]);
        }

        // Create a new streaming distribution
        String streamingBucket = "jets3t-streaming.s3.amazonaws.com";
        StreamingDistribution newStreamingDistribution = cloudFrontService.createStreamingDistribution(
            new S3Origin(streamingBucket),
            "" + System.currentTimeMillis(), // Caller reference - a unique string value
            null, // CNAME aliases for distribution
            "Test streaming distribution", // Comment
            true,  // Distribution is enabled?
            null   // Logging status
            );
        System.out.println("New Streaming Distribution: " + newStreamingDistribution);

        // Streaming distributions can be made private just like standard non-streaming
        // distributions. Create a new private streaming distribution for which signed
        // URLs are *not* required
        StreamingDistribution newPrivateStreamingDistribution =
            cloudFrontService.createStreamingDistribution(
                new S3Origin(streamingBucket, originAccessIdentityId),
                "" + System.currentTimeMillis(), // Caller reference - a unique string value
                new String[] {}, // CNAME aliases for distribution
                "New private streaming distribution -- URL signing not required", // Comment
                true, // Distribution is enabled?
                null, // Logging status
                true, // URLs self-signing enabled
                null // No other AWS users can sign URLs
        );
        System.out.println("New Private Streaming Distribution: " + newPrivateStreamingDistribution);

        // The ID of the streaming distribution we will use for testing
        String testStreamingDistributionId = newStreamingDistribution.getId();

        // List information about a streaming distribution
        StreamingDistribution streamingDistribution =
            cloudFrontService.getStreamingDistributionInfo(testStreamingDistributionId);
        System.out.println("Streaming Distribution: " + streamingDistribution);

        // List configuration information about a streaming distribution
        StreamingDistributionConfig streamingDistributionConfig =
            cloudFrontService.getStreamingDistributionConfig(testStreamingDistributionId);
        System.out.println("Streaming Distribution Config: " + streamingDistributionConfig);

        // Update a streaming distribution's configuration to add an extra CNAME alias
        // and to enable access logging -- logs will be written to '
        StreamingDistributionConfig updatedStreamingDistributionConfig =
            cloudFrontService.updateStreamingDistributionConfig(
                testStreamingDistributionId,
                null, // origin -- null for no changes
                new String[] {"cname.jets3t-streaming.com"}, // CNAME aliases for distribution
                "Updated this streaming distribution", // Comment
                true, // Distribution enabled?
                new LoggingStatus("jets3t-streaming-logs.s3.amazonaws.com", "sdlog-") // Logging
                );
        System.out.println("Updated Streaming Distribution Config: "
            + updatedStreamingDistributionConfig);

        // Disable a streaming distribution, e.g. so that it may be deleted.
        // The CloudFront service may take some time to disable and deploy the distribution.
        StreamingDistributionConfig disabledStreamingDistributionConfig =
            cloudFrontService.updateStreamingDistributionConfig(
                testStreamingDistributionId,
                null, // origin -- null for no changes
                new String[] {}, "Deleting distribution",
                false, // Distribution enabled?
                null   // Logging status
                );
        System.out.println("Disabled Streaming Distribution Config: "
            + disabledStreamingDistributionConfig);

        // Check whether a streaming distribution is deployed
        StreamingDistribution streamingDistributionCheck =
            cloudFrontService.getStreamingDistributionInfo(testStreamingDistributionId);
        System.out.println("Streaming Distribution is deployed? "
            + streamingDistributionCheck.isDeployed());

        // Convenience method to disable a streaming distribution prior to deletion
        cloudFrontService.disableStreamingDistributionForDeletion(testStreamingDistributionId);

        // Delete a streaming distribution (the distribution must be disabled and deployed first)
        cloudFrontService.deleteStreamingDistribution(testStreamingDistributionId);

        // ------------------------------------------------------------
        // Object Invalidation
        // ------------------------------------------------------------

        // Invalidate objects in a distribution to force CloudFront to fetch the
        // latest object data from the S3 origin.
        String[] objectKeys = new String[] {"downloads.html"};

        Invalidation invalidation = cloudFrontService.invalidateObjects(
            testDistributionId,
            objectKeys,
            "" + System.currentTimeMillis() // Caller reference - a unique string value
            );
        System.out.println(invalidation);

        // Retrieve details about a prior invalidation operation
        String invalidationId = invalidation.getId();

        Invalidation priorInvalidation = cloudFrontService.getInvalidation(
            testDistributionId, invalidationId);
        System.out.println(priorInvalidation);

        // List summary information about all invalidations performed
        // on a distribution.
        List<InvalidationSummary> invalidationSummaries =
            cloudFrontService.listInvalidations(testDistributionId);
        System.out.println(invalidationSummaries);

        // ------------------------------------------------------------
        // Non-S3 origin
        // ------------------------------------------------------------

        // Create a new distribution with a non-S3 (custom) origin
        CustomOrigin customOrigin = new CustomOrigin(
            "www.jamesmurty.com", // DNS name
            CustomOrigin.OriginProtocolPolicy.HTTP_ONLY  // Access content over HTTP only
            // To distribute content over HTTPS use:
            // CustomOrigin.OriginProtocolPolicy.MATCH_VIEWER
            );

        Distribution customOriginDistribution = cloudFrontService.createDistribution(
            customOrigin,
            "" + System.currentTimeMillis(), // Caller reference - a unique string value
            null, // CNAME aliases for distribution
            "Distribution with a non-S3 origin", // Comment
            true,  // Distribution is enabled?
            null  // Logging status of distribution (null means disabled)
            );
        System.out.println("Distribution with custom origin: " + customOriginDistribution);
    }

}
