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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jets3t.service.Constants;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.EmailAddressGrantee;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.BaseVersionOrDeleteMarker;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3BucketVersioningStatus;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multithread.DownloadPackage;
import org.jets3t.service.multithread.S3ServiceSimpleMulti;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.AWSDevPayCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.MultipartUtils;
import org.jets3t.service.utils.ServiceUtils;

/**
 * This class includes all the code samples as listed in the JetS3t
 * <a href="http://www.jets3t.org/toolkit/guide.html">Programmer Guide</a>.
 * <p>
 * This code is provided as a convenience for those who are reading through the guide and don't want
 * to type out the examples themselves.
 * </p>
 *
 * @author James Murty
 */
public class CodeSamples {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_OBJECT_NAME = "helloworld.txt";

    public static void main(String[] args) throws Exception {
        /* ************
         * Code Samples
         * ************
         */

        /*
         * Connecting to S3
         */

        // Your Amazon Web Services (AWS) login credentials are required to manage S3 accounts.
        // These credentials are stored in an AWSCredentials object:

        AWSCredentials awsCredentials = SamplesUtils.loadAWSCredentials();

        // To communicate with S3 use the RestS3Service.
        RestS3Service s3Service = new RestS3Service(awsCredentials);

        // A good test to see if your S3Service can connect to S3 is to list all the buckets you own.
        // If a bucket listing produces no exceptions, all is well.

        S3Bucket[] myBuckets = s3Service.listAllBuckets();
        System.out.println("How many buckets do I have in S3? " + myBuckets.length);

        /*
         * Create a bucket
         */

        // To store data in S3 you must first create a bucket, a container for objects.

        S3Bucket testBucket = s3Service.createBucket(BUCKET_NAME);
        System.out.println("Created test bucket: " + testBucket.getName());

        // If you try using a common name, you will probably not be able to create the
        // bucket as someone else will already have a bucket of that name.

        // To create a bucket in an S3 data center located somewhere other than
        // the United States, you can specify a location for your bucket as a
        // second parameter to the createBucket() method. Currently, the alternative
        // S3 locations are Europe (EU), US West - Northern California (us-west-1),
        // and Asia Pacific (Singapore)

        S3Bucket euBucket = s3Service.createBucket("eu-bucket", S3Bucket.LOCATION_EUROPE);
        S3Bucket usWestBucket = s3Service.createBucket("us-west-bucket", S3Bucket.LOCATION_US_WEST);
        S3Bucket asiaPacificBucket = s3Service.createBucket(
            "asia-pacific-bucket", S3Bucket.LOCATION_ASIA_PACIFIC);


        /*
         * Uploading data objects
         */

        // We use S3Object classes to represent data objects in S3. To store some information in our
        // new test bucket, we must first create an object with a key/name then tell our
        // S3Service to upload it to S3.

        // In the example below, we print out information about the S3Object before and after
        // uploading it to S3. These print-outs demonstrate that the S3Object returned by the
        // putObject method contains extra information provided by S3, such as the date the
        // object was last modified on an S3 server.

        // Create an empty object with a key/name, and print the object's details.
        S3Object object = new S3Object("object");
        System.out.println("S3Object before upload: " + object);

        // Upload the object to our test bucket in S3.
        object = s3Service.putObject(testBucket, object);

        // Print the details about the uploaded object, which contains more information.
        System.out.println("S3Object after upload: " + object);

        // The example above will create an empty object in S3, which isn't very useful.
        // To include data in the object you must provide some data for the object.
        // If you know the Content/Mime type of the data (e.g. text/plain) you should set this too.

        // S3Object's can contain any data available from an input stream, but JetS3t provides two
        // convenient object types to hold File or String data. These convenient constructors
        // automatically set the Content-Type and Content-Length of the object.

        // Create an S3Object based on a string, with Content-Length set automatically and
        // Content-Type set to "text/plain"
        String stringData = "Hello World!";
        S3Object stringObject = new S3Object(TEST_OBJECT_NAME, stringData);

        // Create an S3Object based on a file, with Content-Length set automatically and
        // Content-Type set based on the file's extension (using the Mimetypes utility class)
        File fileData = new File("src/org/jets3t/samples/CodeSamples.java");
        S3Object fileObject = new S3Object(fileData);

        // If your data isn't a File or String you can use any input stream as a data source,
        // but you must manually set the Content-Length.

        // Create an object containing a greeting string as input stream data.
        String greeting = "Hello World!";
        S3Object helloWorldObject = new S3Object("HelloWorld2.txt");
        ByteArrayInputStream greetingIS = new ByteArrayInputStream(
            greeting.getBytes(Constants.DEFAULT_ENCODING));
        helloWorldObject.setDataInputStream(greetingIS);
        helloWorldObject.setContentLength(
            greeting.getBytes(Constants.DEFAULT_ENCODING).length);
        helloWorldObject.setContentType("text/plain");

        // Upload the data objects.
        s3Service.putObject(testBucket, stringObject);
        s3Service.putObject(testBucket, fileObject);
        s3Service.putObject(testBucket, helloWorldObject);

        // Print details about the uploaded object.
        System.out.println("S3Object with data: " + helloWorldObject);

        // You may want to store your objects using a non-standard
        // "storage class" in some cases, such as if you are prepared to
        // accept a reduced level of redundancy in exchange for cheaper
        // storage. Here is how you store an object using the
        // Reduced Redundancy Storage (RRS) feature.
        S3Object rrsObject = new S3Object("reduced-redundancy-object");
        // Apply the RRS storage class instead of the default STANDARD one.
        rrsObject.setStorageClass(S3Object.STORAGE_CLASS_REDUCED_REDUNDANCY);
        // Upload the object as usual.
        s3Service.putObject(testBucket, rrsObject);

        /*
         * Verifying Uploads
         */

        // To be 100% sure that data you have uploaded to S3 has not been
        // corrupted in transit, you can verify that the hash value of the data
        // S3 received matches the hash value of your original data.

        // The easiest way to do this is to specify your data's hash value
        // in the Content-MD5 header before you upload the object. JetS3t will
        // do this for you automatically when you use the File- or String-based
        // S3Object constructors:

        S3Object objectWithHash = new S3Object(TEST_OBJECT_NAME, stringData);
        System.out.println("Hash value: " + objectWithHash.getMd5HashAsHex());

        // If you do not use these constructors, you should *always* set the
        // Content-MD5 header value yourself before you upload an object.
        // JetS3t provides the ServiceUtils#computeMD5Hash method to calculate
        // the hash value of an input stream or byte array.

        ByteArrayInputStream dataIS = new ByteArrayInputStream(
            "Here is my data".getBytes(Constants.DEFAULT_ENCODING));
        byte[] md5Hash = ServiceUtils.computeMD5Hash(dataIS);
        dataIS.reset();

        stringObject = new S3Object("MyData");
        stringObject.setDataInputStream(dataIS);
        stringObject.setMd5Hash(md5Hash);

        /*
         * Downloading data objects
         */

        // To download data from S3 you retrieve an S3Object through the S3Service.
        // You may retrieve an object in one of two ways, with the data contents or without.

        // If you just want to know some details about an object and you don't need its contents,
        // it's faster to use the getObjectDetails method. This returns only the object's details,
        // also known as its 'HEAD'. Head information includes the object's size, date, and other
        // metadata associated with it such as the Content Type.

        // Retrieve the HEAD of the data object we created previously.
        S3Object objectDetailsOnly = s3Service.getObjectDetails(testBucket, TEST_OBJECT_NAME);
        System.out.println("S3Object, details only: " + objectDetailsOnly);

        // If you need the data contents of the object, the getObject method will return all the
        // object's details and will also set the object's DataInputStream variable from which
        // the object's data can be read.

        // Retrieve the whole data object we created previously
        S3Object objectComplete = s3Service.getObject(testBucket, TEST_OBJECT_NAME);
        System.out.println("S3Object, complete: " + objectComplete);

        // Read the data from the object's DataInputStream using a loop, and print it out.
        System.out.println("Greeting:");
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(objectComplete.getDataInputStream()));
        String data = null;
        while ((data = reader.readLine()) != null) {
            System.out.println(data);
        }

        /*
         * Verifying Downloads
         */

        // To be 100% sure that data you have downloaded from S3 has not been
        // corrupted in transit, you can verify the data by calculating its hash
        // value and comparing this against the hash value returned by S3.

        // JetS3t provides convenient methods for verifying data that has been
        // downloaded to a File, byte array or InputStream.

        S3Object downloadedObject = s3Service.getObject(testBucket, TEST_OBJECT_NAME);
        String textData = ServiceUtils.readInputStreamToString(
            downloadedObject.getDataInputStream(), "UTF-8");
        boolean valid = downloadedObject.verifyData(textData.getBytes("UTF-8"));
        System.out.println("Object verified? " + valid);

        /*
         * List your buckets and objects
         */

        // Now that you have a bucket and some objects, it's worth listing them. Note that when
        // you list objects, the objects returned will not include much information compared to
        // what you get from the getObject and getObjectDetails methods. However, they will
        // include the size of each object

        // List all your buckets.
        S3Bucket[] buckets = s3Service.listAllBuckets();

        // List the object contents of each bucket.
        for (int b = 0; b < buckets.length; b++) {
            System.out.println("Bucket '" + buckets[b].getName() + "' contains:");

            // List the objects in this bucket.
            S3Object[] objects = s3Service.listObjects(buckets[b]);

            // Print out each object's key and size.
            for (int o = 0; o < objects.length; o++) {
                System.out.println(" " + objects[o].getKey() + " (" + objects[o].getContentLength() + " bytes)");
            }
        }

        // When listing the objects in a bucket you can filter which objects to return based on
        // the names of those objects. This is useful when you are only interested in some
        // specific objects in a bucket and you don't need to list all the bucket's contents.

        // List only objects whose keys match a prefix.
        String prefix = "Reports";
        String delimiter = null; // Refer to the S3 guide for more information on delimiters
        S3Object[] filteredObjects = s3Service.listObjects(testBucket, prefix, delimiter);

        /*
         * Copying objects
         */

        // Objects can be copied within the same bucket and between buckets.

        // Create a target S3Object
        S3Object targetObject = new S3Object("targetObjectWithSourcesMetadata");

        // Copy an existing source object to the target S3Object
        // This will copy the source's object data and metadata to the target object.
        boolean replaceMetadata = false;
        s3Service.copyObject(BUCKET_NAME, TEST_OBJECT_NAME, "destination-bucket", targetObject, replaceMetadata);

        // You can also copy an object and update its metadata at the same time. Perform a
        // copy-in-place  (with the same bucket and object names for source and destination)
        // to update an object's metadata while leaving the object's data unchanged.
        targetObject = new S3Object(TEST_OBJECT_NAME);
        targetObject.addMetadata(S3Object.METADATA_HEADER_CONTENT_TYPE, "text/html");
        replaceMetadata = true;
        s3Service.copyObject(BUCKET_NAME, TEST_OBJECT_NAME, BUCKET_NAME, targetObject, replaceMetadata);

        /*
         * Moving and Renaming objects
         */

        // Objects can be moved within a bucket (to a different name) or to another S3
        // bucket in the same region (eg US or EU).
        // A move operation is composed of a copy then a delete operation behind the scenes.
        // If the initial copy operation fails, the object is not deleted. If the final delete
        // operation fails, the object will exist in both the source and destination locations.

        // Here is a command that moves an object from one bucket to another.
        s3Service.moveObject(BUCKET_NAME, TEST_OBJECT_NAME, "destination-bucket", targetObject, false);

        // You can move an object to a new name in the same bucket. This is essentially a rename operation.
        s3Service.moveObject(BUCKET_NAME, TEST_OBJECT_NAME, BUCKET_NAME, new S3Object("NewName.txt"), false);

        // To make renaming easier, JetS3t has a shortcut method especially for this purpose.
        s3Service.renameObject(BUCKET_NAME, TEST_OBJECT_NAME, targetObject);

        /*
         * Deleting objects and buckets
         */

        // Objects can be easily deleted. When they are gone they are gone for good so be careful.

        // Buckets may only be deleted when they are empty.

        // If you try to delete your bucket before it is empty, it will fail.
        try {
            // This will fail if the bucket isn't empty.
            s3Service.deleteBucket(testBucket.getName());
        } catch (S3ServiceException e) {
            e.printStackTrace();
        }

        // Delete all the objects in the bucket
        s3Service.deleteObject(testBucket, object.getKey());
        s3Service.deleteObject(testBucket, helloWorldObject.getKey());

        // Now that the bucket is empty, you can delete it.
        s3Service.deleteBucket(testBucket.getName());
        System.out.println("Deleted bucket " + testBucket.getName());


        /* ***********************
         * Multi-threaded Examples
         * ***********************
         */

        // The JetS3t Toolkit includes utility services, S3ServiceMulti and S3ServiceSimpleMulti, that
        // can perform an S3 operation on many objects at a time. These services allow you to use more
        // of your available bandwidth and perform S3 operations much faster. They work with any
        // thread-safe S3Service implementation, such as the HTTP/REST implementation provided with
        // JetS3t.

        // The S3ServiceMulti service is intended for advanced developers. It is designed for use in
        // graphical applications and uses an event-notification approach to communicate its results
        // rather than standard method calls. This means the service can provide progress reports to
        // an application during long-running operations. However, this approach makes the service
        // complicated to use. See the code for the Cockpit application to see how this service is used
        // to display progress updates.

        // The S3ServiceSimpleMulti is a service that wraps around S3ServiceMulti and provides a
        // simplified interface, so developers can take advantage of multi-threading without any extra work.

        // The examples below demonstrate how to use some of the multi-threaded operations provided by
        // S3ServiceSimpleMulti.

        /*
         * Construct an S3ServiceSimpleMulti service
         */

        // To use the S3ServiceSimpleMulti service you construct it by providing an existing
        // S3Service object.

        // Create a simple multi-threading service based on our existing S3Service
        S3ServiceSimpleMulti simpleMulti = new S3ServiceSimpleMulti(s3Service);

        /*
         * Upload multiple objects at once
         */

        // To demonstrate multiple uploads, let's create some small text-data objects and a bucket to put them in.

        // First, create a bucket.
        S3Bucket bucket = new S3Bucket(awsCredentials.getAccessKey() + ".TestMulti");
        bucket = s3Service.createBucket(bucket);

        // Create an array of data objects to upload.
        S3Object[] objects = new S3Object[5];
        objects[0] = new S3Object("object1.txt", "Hello from object 1");
        objects[1] = new S3Object("object2.txt", "Hello from object 2");
        objects[2] = new S3Object("object3.txt", "Hello from object 3");
        objects[3] = new S3Object("object4.txt", "Hello from object 4");
        objects[4] = new S3Object("object5.txt", "Hello from object 5");

        // Now we have some sample objects, we can upload them.

        // Upload multiple objects.
        S3Object[] createdObjects = simpleMulti.putObjects(bucket, objects);
        System.out.println("Uploaded " + createdObjects.length + " objects");

        /*
         * Retrieve the HEAD information of multiple objects
         */

        // Perform a Details/HEAD query for multiple objects.
        S3Object[] objectsWithHeadDetails = simpleMulti.getObjectsHeads(bucket, objects);

        // Print out details about all the objects.
        System.out.println("Objects with HEAD Details...");
        for (int i = 0; i < objectsWithHeadDetails.length; i++) {
            System.out.println(objectsWithHeadDetails[i]);
        }

        /*
         * Download objects to local files
         */

        // The multi-threading services provide a method to download multiple objects at a time, but
        // to use this you must first prepare somewhere to put the data associated with each object.
        // The most obvious place to put this data is into a file, so let's go through an example of
        // downloading object data into files.

        // To download our objects into files we first must create a DownloadPackage class for
        // each object. This class is a simple container which merely associates an object with a
        // file, to which the object's data will be written.

        // Create a DownloadPackage for each object, to associate the object with an output file.
        DownloadPackage[] downloadPackages = new DownloadPackage[5];
        downloadPackages[0] = new DownloadPackage(objects[0],
            new File(objects[0].getKey()));
        downloadPackages[1] = new DownloadPackage(objects[1],
            new File(objects[1].getKey()));
        downloadPackages[2] = new DownloadPackage(objects[2],
            new File(objects[2].getKey()));
        downloadPackages[3] = new DownloadPackage(objects[3],
            new File(objects[3].getKey()));
        downloadPackages[4] = new DownloadPackage(objects[4],
            new File(objects[4].getKey()));

        // Download the objects.
        simpleMulti.downloadObjects(bucket, downloadPackages);
        System.out.println("Downloaded objects to current working directory");

        /*
         * Delete multiple objects
         */

        // It's time to clean up, so let's get rid of our multiple objects and test bucket.

        // Delete multiple objects, then the bucket too.
        simpleMulti.deleteObjects(bucket, objects);
        s3Service.deleteBucket(bucket);
        System.out.println("Deleted bucket: " + bucket);

        /* *****************
         * Bucket Versioning
         * *****************
         * S3 Buckets have a versioning feature which allows you to keep prior versions of
         * your objects when they are updated or deleted. This feature means you can be much
         * more confident that vital data will not be lost even if it is accidentally
         * overwritten or deleted.
         *
         * Versioning is not enabled for a bucket by default, you must explicitly enable
         * it. Once it is enabled you access and mange object versions using unique version
         * identifiers.
         */
        // Create a bucket to test versioning
        S3Bucket versioningBucket = s3Service.getOrCreateBucket(
            "test-versioning");
        String vBucketName = versioningBucket.getName();

        // Check bucket versioning status for the bucket
        S3BucketVersioningStatus versioningStatus =
            s3Service.getBucketVersioningStatus(vBucketName);
        System.out.println("Versioning enabled ? "
            + versioningStatus.isVersioningEnabled());

        // Suspend (disable) versioning for a bucket -- will have no
        // effect if bucket versioning is not yet enabled.
        // This will not delete any existing object versions.
        s3Service.suspendBucketVersioning(vBucketName);

        // Enable versioning for a bucket.
        s3Service.enableBucketVersioning(vBucketName);

        // Once versioning is enabled you can GET, PUT, copy and
        // delete objects as normal. Every change to an object will
        // cause a new version to be created.

        // Store and update and delete an object in the versioning bucket
        S3Object versionedObject = new S3Object("versioned-object", "Initial version");
        s3Service.putObject(vBucketName, versionedObject);
        versionedObject = new S3Object("versioned-object", "Second version");
        s3Service.putObject(vBucketName, versionedObject);
        versionedObject = new S3Object("versioned-object", "Final version");
        s3Service.putObject(vBucketName, versionedObject);

        // If you retrieve an object with the standard method you will
        // get the latest version, and if the object is in a versioned
        // bucket its Version ID will be available
        versionedObject = s3Service.getObject(vBucketName, "versioned-object");
        String finalVersionId = versionedObject.getVersionId();
        System.out.println("Version ID: " + finalVersionId);

        // If you delete a versioned object it is no longer available using
        // standard methods...
        s3Service.deleteObject(vBucketName, "versioned-object");
        try {
            s3Service.getObject(vBucketName, "versioned-object");
        } catch (S3ServiceException e) {
            if (e.getResponseCode() == 404) {
                System.out.println("Is deleted object versioned? "
                    + e.getResponseHeaders().get(Constants.AMZ_DELETE_MARKER));
                System.out.println("Delete marker version ID: "
                    + e.getResponseHeaders().get(Constants.AMZ_VERSION_ID));
            }
        }
        // ... but you can use a versioning-aware method to retrieve any of
        // the prior versions by Version ID.
        versionedObject = s3Service.getVersionedObject(finalVersionId,
            vBucketName, "versioned-object");
        String versionedData = ServiceUtils.readInputStreamToString(
            versionedObject.getDataInputStream(), "UTF-8");
        System.out.println("Data from prior version of deleted document: "
            + versionedData);

        // List all the object versions in the bucket, with no prefix
        // or delimiter restrictions. Each result object will be one of
        // S3Version or S3DeleteMarker.
        BaseVersionOrDeleteMarker[] versions =
            s3Service.listVersionedObjects(vBucketName, null, null);
        for (int i = 0; i < versions.length; i++) {
            System.out.println(versions[i]);
        }

        // List versions of objects that match a prefix.
        String versionPrefix = "versioned-object";
        versions = s3Service.listVersionedObjects(vBucketName, versionPrefix, null);

        // JetS3t includes a convenience method to list only the versions
        // for a specific object, even if it shares a prefix with other objects.
        versions = s3Service.getObjectVersions(vBucketName, "versioned-object");

        // There are versioning-aware methods corresponding to all S3 operations
        versionedObject = s3Service.getVersionedObjectDetails(
            finalVersionId, vBucketName, "versioned-object");
        // Confirm that S3 returned the versioned object you requested
        if (!finalVersionId.equals(versionedObject.getVersionId())) {
            throw new Exception("Incorrect version!");
        }

        s3Service.copyVersionedObject(finalVersionId,
            vBucketName, "versioned-object",
            "destination-bucket", new S3Object("copied-from-version"),
            false, null, null, null, null);

        AccessControlList versionedObjectAcl =
            s3Service.getVersionedObjectAcl(finalVersionId,
                vBucketName, "versioned-object");

        s3Service.putVersionedObjectAcl(finalVersionId,
            vBucketName, "versioned-object", versionedObjectAcl);

        // To delete an object version once-and-for-all you must use the
        // versioning-specific delete operation, and you can only do so
        // if you are the owner of the bucket containing the version.
        s3Service.deleteVersionedObject(finalVersionId,
            vBucketName, "versioned-object");

        // You can easily delete all the versions of an object using
        // one of JetS3t's multi-threaded services.
        versions = s3Service.getObjectVersions(vBucketName, "versioned-object");
        // Convert version and delete marker objects into versionId strings.
        String[] versionIds = BaseVersionOrDeleteMarker.toVersionIds(versions);
        (new S3ServiceSimpleMulti(s3Service)).deleteVersionsOfObject(
            versionIds, vBucketName, "versioned-object");


        //////////////////////////////////////////////////////////////
        // For additional data protection you can require multi-factor
        // authentication (MFA) to delete object versions.
        //////////////////////////////////////////////////////////////

        // Require multi-factor authentication to delete versions.
        s3Service.enableBucketVersioningAndMFA(vBucketName);

        // Check MFA status for the bucket
        versioningStatus = s3Service.getBucketVersioningStatus(vBucketName);
        System.out.println("Multi-factor auth required to delete versions ? "
            + versioningStatus.isMultiFactorAuthDeleteRequired());

        // If MFA is enabled for a bucket you must provide the serial number
        // for your multi-factor authentication device and a recent code to
        // delete object versions.
        String multiFactorSerialNumber = "#111222333";
        String multiFactorAuthCode = "12345678";

        s3Service.deleteVersionedObjectWithMFA(finalVersionId,
            multiFactorSerialNumber, multiFactorAuthCode, vBucketName, "versioned-object");

        // With MFA enabled, you must provide your multi-factor auth credentials
        // to disable MFA.
        s3Service.disableMFAForVersionedBucket(vBucketName,
            multiFactorSerialNumber, multiFactorAuthCode);

        // With MFA enabled, you must provide your multi-factor auth credentials
        // to suspend S3 versioning altogether. However, the credentials will not
        // be needed if you have already disabled MFA.
        s3Service.suspendBucketVersioningWithMFA(vBucketName,
            multiFactorSerialNumber, multiFactorAuthCode);

        /* *****************
         * Advanced Examples
         * *****************
         */

        /*
         * Managing Metadata
         */

        // S3Objects can contain metadata stored as name/value pairs. This metadata is stored in
        // S3 and can be accessed when an object is retrieved from S3 using getObject
        // or getObjectDetails methods. To store metadata with an object, add your metadata to
        // the object prior to uploading it to S3.

        // Note that metadata cannot be updated in S3 without replacing the existing object,
        // and that metadata names must be strings without spaces.

        S3Object objectWithMetadata = new S3Object("metadataObject");
        objectWithMetadata.addMetadata("favourite-colour", "blue");
        objectWithMetadata.addMetadata("document-version", "0.3");


        /*
         * Save and load encrypted AWS Credentials
         */

        // AWS credentials are your means to login to and manage your S3 account, and should be
        // kept secure. The JetS3t toolkit stores these credentials in AWSCredentials objects.
        // The AWSCredentials class provides utility methods to allow credentials to be saved to
        // an encrypted file and loaded from a previously saved file with the right password.

        // Save credentials to an encrypted file protected with a password.
        File credFile = new File("awscredentials.enc");
        awsCredentials.save("password", credFile);

        // Load encrypted credentials from a file.
        ProviderCredentials loadedCredentials = AWSCredentials.load("password", credFile);
        System.out.println("AWS Key loaded from file: " + loadedCredentials.getAccessKey());

        // You won't get far if you use the wrong password...
        try {
            loadedCredentials = AWSCredentials.load("wrongPassword", credFile);
        } catch (S3ServiceException e) {
            System.err.println("Cannot load credentials from file with the wrong password!");
        }

        /*
         * Manage Access Control Lists
         */

        // S3 uses Access Control Lists to control who has access to buckets and objects in S3.
        // By default, any bucket or object you create will belong to you and will not be accessible
        // to anyone else. You can use JetS3t's support for access control lists to make buckets or
        // objects publicly accessible, or to allow other S3 members to access or manage your objects.

        // The ACL capabilities of S3 are quite involved, so to understand this subject fully please
        // consult Amazon's documentation. The code examples below show how to put your understanding
        // of the S3 ACL mechanism into practice.

        // ACL settings may be provided with a bucket or object when it is created, or the ACL of
        // existing items may be updated. Let's start by creating a bucket with default (i.e. private)
        // access settings, then making it public.

        // Create a bucket in S3.
        S3Bucket publicBucket = new S3Bucket(awsCredentials.getAccessKey() + ".publicBucket");
        s3Service.createBucket(publicBucket);

        // Retrieve the bucket's ACL and modify it to grant public access,
        // ie READ access to the ALL_USERS group.
        AccessControlList bucketAcl = s3Service.getBucketAcl(publicBucket);
        bucketAcl.grantPermission(GroupGrantee.ALL_USERS, Permission.PERMISSION_READ);

        // Update the bucket's ACL. Now anyone can view the list of objects in this bucket.
        publicBucket.setAcl(bucketAcl);
        s3Service.putBucketAcl(publicBucket);
        System.out.println("View bucket's object listing here: http://s3.amazonaws.com/"
            + publicBucket.getName());

        // Now let's create an object that is public from scratch. Note that we will use the bucket's
        // public ACL object created above, this works fine. Although it is possible to create an
        // AccessControlList object from scratch, this is more involved as you need to set the
        // ACL's Owner information which is only readily available from an existing ACL.

        // Create a public object in S3. Anyone can download this object.
        S3Object publicObject = new S3Object(
            "publicObject.txt", "This object is public");
        publicObject.setAcl(bucketAcl);
        s3Service.putObject(publicBucket, publicObject);
        System.out.println("View public object contents here: http://s3.amazonaws.com/"
            + publicBucket.getName() + "/" + publicObject.getKey());

        // The ALL_USERS Group is particularly useful, but there are also other grantee types
        // that can be used with AccessControlList. Please see Amazon's S3 technical documentation
        // for a fuller discussion of these settings.

        AccessControlList acl = new AccessControlList();

        // Grant access by email address. Note that this only works email address of AWS S3 members.
        acl.grantPermission(new EmailAddressGrantee("someone@somewhere.com"),
            Permission.PERMISSION_FULL_CONTROL);

        // Grant control of ACL settings to a known AWS S3 member.
        acl.grantPermission(new CanonicalGrantee("AWS member's ID"),
            Permission.PERMISSION_READ_ACP);
        acl.grantPermission(new CanonicalGrantee("AWS member's ID"),
            Permission.PERMISSION_WRITE_ACP);

        /*
         * Bucket Policies -- offer a greater degree of access control for a bucket.
         */

        // Set a bucket policy that allows public read access to all objects under
        // the virtual path "/public"

        String bucketNameForPolicy = publicBucket.getName();
        String policyJSON =
            "{"
            + "\"Version\":\"2008-10-17\""
            + ",\"Id\":\"EXAMPLE\""
            + ",\"Statement\": [{"
                + "\"Effect\":\"Allow\""
                + ",\"Action\":[\"s3:GetObject*\"]"
                + ",\"Principal\":{\"AWS\": [\"*\"]}"
                + ",\"Resource\":\"arn:aws:s3:::" + bucketNameForPolicy + "/public/*\""
            + "}]}";
        s3Service.setBucketPolicy(bucketNameForPolicy, policyJSON);

        // Retrieve the policy document applied to a bucket
        String policyDocument = s3Service.getBucketPolicy(bucketNameForPolicy);
        System.out.println(policyDocument);

        // Delete the policy document applied to a bucket
        s3Service.deleteBucketPolicy(bucketNameForPolicy);


        /*
         * Temporarily make an Object available to anyone
         */

        // A private object stored in S3 can be made publicly available for a limited time using a
        // signed URL. The signed URL can be used by anyone to download the object, yet it includes
        // a date and time after which the URL will no longer work.

        // Create a private object in S3.
        S3Bucket privateBucket = new S3Bucket(awsCredentials.getAccessKey() + ".privateBucket");
        S3Object privateObject = new S3Object(
            "privateObject.txt", "This object is private");
        s3Service.createBucket(privateBucket);
        s3Service.putObject(privateBucket, privateObject);

        // Determine what the time will be in 5 minutes.
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 5);
        Date expiryDate = cal.getTime();

        // Create a signed HTTP GET URL valid for 5 minutes.
        // If you use the generated URL in a web browser within 5 minutes, you will be able to view
        // the object's contents. After 5 minutes, the URL will no longer work and you will only
        // see an Access Denied message.
        String signedUrl = s3Service.createSignedGetUrl(
            privateBucket.getName(), privateObject.getKey(), expiryDate, false);
        System.out.println("Signed URL: " + signedUrl);

        /*
         * Multipart uploads
         */

        // Amazon S3 offers an alternative method for uploading objects for
        // users with advanced requirements, called Multipart Uploads. This
        // mechanism involves uploading an object's data in parts instead of
        // all at once, which can give the following advantages:
        //  * large files can be uploaded in smaller pieces to reduce the
        //    impact of transient uploading/networking errors
        //  * objects larger than 5 GB can be stored
        //  * objects can be constructed from data that is uploaded over a
        //    period of time, when it may not all be available in advance.

        // JetS3t's MultipartUtils class makes it easy to perform mutipart
        // uploads of your files. To upload a file in 20MB parts:

        S3Object largeFileObject = new S3Object(new File("/path/to/large/file"));

        List<StorageObject> objectsToUploadAsMultipart = new ArrayList<StorageObject>();
        objectsToUploadAsMultipart.add(largeFileObject);

        long maxSizeForAPartInBytes = 20 * 1024 * 1024;
        MultipartUtils mpUtils = new MultipartUtils(maxSizeForAPartInBytes);

        mpUtils.uploadObjects(BUCKET_NAME, s3Service, objectsToUploadAsMultipart,
            null // eventListener : Provide one to monitor the upload progress
            );

        // The S3Service API also provides the underlying low-level multipart operations
        // if you need more control over the process. See the method names that
        // start with "multipart", and the example code in
        // TestRestS3Service#testMultipartUploads

        // IMPORTANT: The objects in S3 created by a multipart upload process do not
        // have ETag header values that can be used to perform MD5 hash verification
        // of the object data. See https://forums.aws.amazon.com/thread.jspa?messageID=234579

        /*
         * Create an S3 POST form
         */

        // When you create and S3 POST form, anyone who accesses that form in
        // a web browser will be able to upload files to S3 directly from the
        // browser, without needing S3-compatible client software.
        // Refer to the S3 documentation for more information:
        // http://docs.amazonwebservices.com/AmazonS3/2006-03-01/UsingHTTPPOST.html

        // We will start by creating a POST form with no policy document,
        // meaning that the form will have no expiration date or usage
        // conditions. This form will only work if the target bucket has
        // public write access enabled.

        String unrestrictedForm =
            S3Service.buildPostForm("public-bucket", "${filename}");

        // To use this form, save it in a UTF-8 encoded HTML page (ie with
        // the meta tag
        // <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />)
        // and load the page in a web browser.


        // We will now create a POST form with a range of policy conditions,
        // that will allow users to upload image files to a protected bucket.
        String key = "uploads/images/pic.jpg";

        // Specify input fields to set the access permissions and content type
        // of the object created by the form. We will also redirect the user to
        // another web site after they have successfully uploaded a file.
        String[] inputFields = new String[] {
            "<input type=\"hidden\" name=\"acl\" value=\"public-read\">",
            "<input type=\"hidden\" name=\"Content-Type\" value=\"image/jpeg\">",
            "<input type=\"hidden\" name=\"success_action_redirect\" value=\"http://localhost/post_upload\">"
        };

        // We then specify policy conditions for at least the mandatory
        // 'bucket' and 'key' fields that will be included in the POST request.
        // In addition to the mandatory fields, we will add a condition to
        // control the size of the file the user can upload.
        // Note that our list of conditions must include a condition
        // corresponding to each of the additional input fields we specified above.
        String[] conditions = {
            S3Service.generatePostPolicyCondition_Equality("bucket", BUCKET_NAME),
            S3Service.generatePostPolicyCondition_Equality("key", key),
            S3Service.generatePostPolicyCondition_Range(10240, 204800),
            // Conditions to allow the additional fields specified above
            S3Service.generatePostPolicyCondition_Equality("acl", "public-read"),
            S3Service.generatePostPolicyCondition_Equality("Content-Type", "image/jpeg"),
            S3Service.generatePostPolicyCondition_Equality("success_action_redirect", "http://localhost/post_upload")
        };

        // Form will expire in 24 hours
        cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, 24);
        Date expiration = cal.getTime();

        // Generate the form.
        String restrictedForm = S3Service.buildPostForm(
            BUCKET_NAME, key, awsCredentials, expiration, conditions,
            inputFields, null, true);

        /*
         * Activate Requester Pays for a bucket.
         */

        // A bucket in S3 is normally configured such that the bucket's owner
        // pays all the service fees for accessing, sharing and storing objects.
        // The Requester Pays feature of S3 allows a bucket to be configured
        // such that the individual who sends requests to a bucket is charged
        // the S3 request and data transfer fees, instead of the bucket's owner.

        // Set a bucket to be Requester Pays
        s3Service.setRequesterPaysBucket(BUCKET_NAME, true);

        // Set a bucket to be Owner pays (the default value for S3 buckets)
        s3Service.setRequesterPaysBucket(BUCKET_NAME, false);

        // Find out whether a bucket is configured as Requester pays
        s3Service.isRequesterPaysBucket(BUCKET_NAME);

        /*
         * Access a Requester Pays bucket when you are not the bucket's owner
         */

        // When a bucket is configured as Requester Pays, other AWS users can
        // upload objects to the bucket or retrieve them provided the user:
        // - has the necessary Access Control List permissions, and
        // - indicates that he/she is willing to pay the Requester Pays fees,
        //   by including a special flag in the request.

        // Indicate that you will accept any Requester Pays fees by setting
        // the RequesterPaysEnabled flag to true in your RestS3Service class.
        // You can then use the service to list, upload, or download objects as
        // normal.
        // Support for Requester Pays buckets is disabled by default in JetS3t
        // with the jets3t.properties setting
        // 'httpclient.requester-pays-buckets-enabled=false'
        s3Service.setRequesterPaysEnabled(true);

        /*
         * Generate a Signed URL for a Requester Pays bucket
         */

        // Third party users of a Requester Pays bucket can generate Signed
        // URLs that permit public access to objects. To generate such a URL,
        // these users call the S3Service#createSignedUrl method with a flag to
        // indicate that the he/she is willing to pay the Requester Pays fees
        // incurred by the use of the signed URL.

        // Generate a signed GET URL for
        Map httpHeaders = null;
        long expirySecsAfterEpoch = System.currentTimeMillis() / 1000 + 300;
        boolean isVirtualHost = false;
        boolean isHttpsUrl = false;
        boolean isDnsBucketNamingDisabled = false;

        String requesterPaysSignedGetUrl =
            s3Service.createSignedUrl("GET", BUCKET_NAME, "object-name",
                Constants.REQUESTER_PAYS_BUCKET_FLAG, // Include Requester Pays flag
                httpHeaders, expirySecsAfterEpoch,
                isVirtualHost, isHttpsUrl,
                isDnsBucketNamingDisabled);

        /*
         * Accessing Amazon DevPay S3 accounts
         */

        // Amazon's DevPay service allows vendors to sell user-pays S3 accounts.
        // To access the S3 portions of a DevPay product, JetS3t needs
        // additional credentials that include the DevPay User Token, and the
        // DevPay Product Token.

        AWSDevPayCredentials devPayCredentials = new AWSDevPayCredentials(
            "YOUR_AWS_ACCESSS_KEY", "YOUR_AWS_SECRET_KEY",
            "DEVPAY_USER_TOKEN", "DEVPAY_PRODUCT_TOKEN");

        // Once you have defined your DevPay S3 credentials, you can create an
        // S3Service class based on these and access the DevPay account as usual.
        S3Service devPayService = new RestS3Service(devPayCredentials);
        devPayService.listAllBuckets();

        // You can also generate signed URLs for DevPay S3 accounts. Here is the
        // code to generate a link that makes an object in a DevPay account
        // temporary available for public download.

        cal = Calendar.getInstance();
        cal.add(Calendar.MINUTE, 5);

        String signedDevPayUrl = devPayService.createSignedGetUrl(
            "devpay-bucket-name", "devpay-object-name", cal.getTime());

    }

}
