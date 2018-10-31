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

import org.jets3t.service.Constants;
import org.jets3t.service.ServiceException;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.acl.gs.AllUsersGrantee;
import org.jets3t.service.acl.gs.GSAccessControlList;
import org.jets3t.service.acl.gs.GroupByDomainGrantee;
import org.jets3t.service.acl.gs.UserByEmailAddressGrantee;
import org.jets3t.service.acl.gs.UserByIdGrantee;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.model.GSBucket;
import org.jets3t.service.model.GSObject;
import org.jets3t.service.security.GSCredentials;
import org.jets3t.service.utils.ServiceUtils;

/**
 * This class includes all the code samples as listed in the Google Storage
 * <a href="http://code.google.com/apis/storage/docs/developer-guide.html">Developer's Guide</a>.
 * <p>
 * This code is provided as a convenience for those who are reading through the guide and don't want
 * to type out the examples themselves.
 * </p>
 *
 * @author Google Developers
 */
public class GSCodeSamples {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_OBJECT_NAME = "helloworld.txt";

    public static void main(String[] args) throws Exception {
        /* ************
         * Code Samples
         * ************
         */

        /*
         * Connecting to Google Storage
         */

        // Your Google Storage (GS) login credentials are required to manage GS accounts.
        // These credentials are stored in an GSCredentials object:

        GSCredentials gsCredentials = SamplesUtils.loadGSCredentials();

        // To communicate with Google Storage use the GoogleStorageService.
        GoogleStorageService gsService = new GoogleStorageService(gsCredentials);

        // A good test to see if your GoogleStorageService can connect to GS is to list all the buckets you own.
        // If a bucket listing produces no exceptions, all is well.

        GSBucket[] myBuckets = gsService.listAllBuckets();
        System.out.println("How many buckets to I have in GS? " + myBuckets.length);

        /*
         * Create a bucket
         */

        // To store data in GS you must first create a bucket, a container for objects.

        GSBucket testBucket = gsService.createBucket(BUCKET_NAME);
        System.out.println("Created test bucket: " + testBucket.getName());

        // If you try using a common name, you will probably not be able to create the
        // bucket as someone else will already have a bucket of that name.

        /*
         * Uploading data objects
         */

        // We use GSObject classes to represent data objects in Google Storage. To store some
        // information in our new test bucket, we must first create an object with a key/name then
        // tell our GoogleStorageService to upload it to GS.

        // In the example below, we print out information about the GSObject before and after
        // uploading it to GS. These print-outs demonstrate that the GSObject returned by the
        // putObject method contains extra information provided by GS, such as the date the
        // object was last modified on a GS server.

        // Create an empty object with a key/name, and print the object's details.
        GSObject object = new GSObject("object");
        System.out.println("GSObject before upload: " + object);

        // Upload the object to our test bucket in GS.
        object = gsService.putObject(BUCKET_NAME, object);

        // Print the details about the uploaded object, which contains more information.
        System.out.println("GSObject after upload: " + object);

        // The example above will create an empty object in GS, which isn't very useful.
        // To include data in the object you must provide some data for the object.
        // If you know the Content/Mime type of the data (e.g. text/plain) you should set this too.

        // GSObject's can contain any data available from an input stream, but JetS3t provides two
        // convenient object types to hold File or String data. These convenient constructors
        // automatically set the Content-Type and Content-Length of the object.

        // Create an GSObject based on a string, with Content-Length set automatically and
        // Content-Type set to "text/plain"
        String stringData = "Hello World!";
        GSObject stringObject = new GSObject(TEST_OBJECT_NAME, stringData);

        // Create an GSObject based on a file, with Content-Length set automatically and
        // Content-Type set based on the file's extension (using the Mimetypes utility class)
        File fileData = new File("src/org/jets3t/samples/GSCodeSamples.java");
        GSObject fileObject = new GSObject(fileData);

        // If your data isn't a File or String you can use any input stream as a data source,
        // but you must manually set the Content-Length.

        // Create an object containing a greeting string as input stream data.
        String greeting = "Hello World!";
        GSObject helloWorldObject = new GSObject("HelloWorld2.txt");
        ByteArrayInputStream greetingIS = new ByteArrayInputStream(
            greeting.getBytes(Constants.DEFAULT_ENCODING));
        helloWorldObject.setDataInputStream(greetingIS);
        helloWorldObject.setContentLength(
            greeting.getBytes(Constants.DEFAULT_ENCODING).length);
        helloWorldObject.setContentType("text/plain");

        // Upload the data objects.
        gsService.putObject(BUCKET_NAME, stringObject);
        gsService.putObject(BUCKET_NAME, fileObject);
        gsService.putObject(BUCKET_NAME, helloWorldObject);

        // Print details about the uploaded object.
        System.out.println("GSObject with data: " + helloWorldObject);

        /*
         * Verifying Uploads
         */

        // To be 100% sure that data you have uploaded to GS has not been
        // corrupted in transit, you can verify that the hash value of the data
        // GS received matches the hash value of your original data.

        // The easiest way to do this is to specify your data's hash value
        // in the Content-MD5 header before you upload the object. JetS3t will
        // do this for you automatically when you use the File- or String-based
        // GSObject constructors:

        GSObject objectWithHash = new GSObject(TEST_OBJECT_NAME, stringData);
        System.out.println("Hash value: " + objectWithHash.getMd5HashAsHex());

        // If you do not use these constructors, you should *always* set the
        // Content-MD5 header value yourself before you upload an object.
        // JetS3t provides the ServiceUtils#computeMD5Hash method to calculate
        // the hash value of an input stream or byte array.

        ByteArrayInputStream dataIS = new ByteArrayInputStream(
            "Here is my data".getBytes(Constants.DEFAULT_ENCODING));
        byte[] md5Hash = ServiceUtils.computeMD5Hash(dataIS);
        dataIS.reset();

        GSObject hashObject = new GSObject("MyData");
        hashObject.setDataInputStream(dataIS);
        hashObject.setMd5Hash(md5Hash);

        /*
         * Downloading data objects
         */

        // To download data from GS you retrieve an GSObject through the GSService.
        // You may retrieve an object in one of two ways, with the data contents or without.

        // If you just want to know some details about an object and you don't need its contents,
        // it's faster to use the getObjectDetails method. This returns only the object's details,
        // also known as its 'HEAD'. Head information includes the object's size, date, and other
        // metadata associated with it such as the Content Type.

        // Retrieve the HEAD of the data object we created previously.
        GSObject objectDetailsOnly = gsService.getObjectDetails(BUCKET_NAME, TEST_OBJECT_NAME);
        System.out.println("GSObject, details only: " + objectDetailsOnly);

        // If you need the data contents of the object, the getObject method will return all the
        // object's details and will also set the object's DataInputStream variable from which
        // the object's data can be read.

        // Retrieve the whole data object we created previously
        GSObject objectComplete = gsService.getObject(BUCKET_NAME, TEST_OBJECT_NAME);
        System.out.println("GSObject, complete: " + objectComplete);

        // Read the data from the object's DataInputStream using a loop, and print it out.
        System.out.println("Greeting:");
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(objectComplete.getDataInputStream()));
        String data;
        while ((data = reader.readLine()) != null) {
            System.out.println(data);
        }

        /*
         * Verifying Downloads
         */

        // To be 100% sure that data you have downloaded from GS has not been
        // corrupted in transit, you can verify the data by calculating its hash
        // value and comparing this against the hash value returned by GS.

        // JetS3t provides convenient methods for verifying data that has been
        // downloaded to a File, byte array or InputStream.

        GSObject downloadedObject = gsService.getObject(BUCKET_NAME, TEST_OBJECT_NAME);
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
        GSBucket[] buckets = gsService.listAllBuckets();

        // List the object contents of each bucket.
        for (int b = 0; b < buckets.length; b++) {
            System.out.println("Bucket '" + buckets[b].getName() + "' contains:");

            // List the objects in this bucket.
            GSObject[] objects = gsService.listObjects(buckets[b].getName());

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
        String delimiter = null; // Refer to the service guide for more information on delimiters
        GSObject[] filteredObjects = gsService.listObjects(BUCKET_NAME, prefix, delimiter);

        /*
         * Copying objects
         */

        // Objects can be copied within the same bucket and between buckets.

        // Create a target GSObject
        GSObject targetObject = new GSObject("target-object-with-sources-metadata");

        // Copy an existing source object to the target GSObject
        // This will copy the source's object data and metadata to the target object.
        boolean replaceMetadata = false;
        gsService.copyObject(BUCKET_NAME, TEST_OBJECT_NAME, "target-bucket", targetObject, replaceMetadata);

        // You can also copy an object and update its metadata at the same time. Perform a
        // copy-in-place  (with the same bucket and object names for source and destination)
        // to update an object's metadata while leaving the object's data unchanged.
        targetObject = new GSObject(TEST_OBJECT_NAME);
        targetObject.addMetadata(GSObject.METADATA_HEADER_CONTENT_TYPE, "text/html");
        replaceMetadata = true;
        gsService.copyObject(BUCKET_NAME, TEST_OBJECT_NAME, BUCKET_NAME, targetObject, replaceMetadata);

        /*
         * Moving and Renaming objects
         */

        // Objects can be moved within a bucket (to a different name) or to another bucket.
        // A move operation is composed of a copy then a delete operation behind the scenes.
        // If the initial copy operation fails, the object is not deleted. If the final delete
        // operation fails, the object will exist in both the source and destination locations.

        // Here is a command that moves an object from one bucket to another.
        gsService.moveObject(BUCKET_NAME, TEST_OBJECT_NAME, "target-bucket", targetObject, false);

        // You can move an object to a new name in the same bucket. This is essentially a rename operation.
        gsService.moveObject(BUCKET_NAME, TEST_OBJECT_NAME, BUCKET_NAME, new GSObject("newname.txt"), false);

        // To make renaming easier, JetS3t has a shortcut method especially for this purpose.
        gsService.renameObject(BUCKET_NAME, TEST_OBJECT_NAME, targetObject);

        /*
         * Deleting objects and buckets
         */

        // Objects can be easily deleted. When they are gone they are gone for good so be careful.

        // Buckets may only be deleted when they are empty.

        // If you try to delete your bucket before it is empty, it will fail.
        try {
            // This will fail if the bucket isn't empty.
            gsService.deleteBucket(BUCKET_NAME);
        } catch (ServiceException e) {
            e.printStackTrace();
        }

        // Delete all the objects in the bucket
        gsService.deleteObject(BUCKET_NAME, object.getKey());
        gsService.deleteObject(BUCKET_NAME, helloWorldObject.getKey());
        gsService.deleteObject(BUCKET_NAME, stringObject.getKey());
        gsService.deleteObject(BUCKET_NAME, fileObject.getKey());

        // Now that the bucket is empty, you can delete it.
        gsService.deleteBucket(BUCKET_NAME);
        System.out.println("Deleted bucket " + BUCKET_NAME);

        /*
         * Manage Access Control Lists
         */

        // GS uses Access Control Lists to control who has access to buckets and objects in GS.
        // By default, any bucket or object you create will belong to you and will not be accessible
        // to anyone else. You can use JetS3t's support for access control lists to make buckets or
        // objects publicly accessible, or to allow other GS members to access or manage your objects.

        // The ACL capabilities of GS are quite involved, so to understand this subject fully please
        // consult Google's documentation. The code examples below show how to put your understanding
        // of the GS ACL mechanism into practice.

        // ACL settings may be provided with a bucket or object when it is created, or the ACL of
        // existing items may be updated. Let's start by creating a bucket with default (i.e. private)
        // access settings, then making it public.

        // Create a bucket.
        String publicBucketName = BUCKET_NAME + "-public";
        GSBucket publicBucket = new GSBucket(publicBucketName);
        gsService.createBucket(publicBucketName);

        // Retrieve the bucket's ACL and modify it to grant public access,
        // ie READ access to the ALL_USERS group.
        GSAccessControlList bucketAcl = gsService.getBucketAcl(publicBucketName);
        bucketAcl.grantPermission(new AllUsersGrantee(), Permission.PERMISSION_READ);

        // Update the bucket's ACL. Now anyone can view the list of objects in this bucket.
        publicBucket.setAcl(bucketAcl);
        gsService.putBucketAcl(publicBucket);

        // Now let's create an object that is public from scratch. Note that we will use the bucket's
        // public ACL object created above, this works fine. Although it is possible to create an
        // AccessControlList object from scratch, this is more involved as you need to set the
        // ACL's Owner information which is only readily available from an existing ACL.

        // Create a public object in GS. Anyone can download this object.
        GSObject publicObject = new GSObject("publicObject.txt", "This object is public");
        publicObject.setAcl(bucketAcl);
        gsService.putObject(publicBucketName, publicObject);

        // The ALL_USERS Group is particularly useful, but there are also other grantee types
        // that can be used with AccessControlList. Please see Google Storage technical documentation
        // for a fuller discussion of these settings.

        GSAccessControlList acl = new GSAccessControlList();

        // Grant access by email address. Note that this only works email address of GS members.
        acl.grantPermission(new UserByEmailAddressGrantee("someone@somewhere.com"),
            Permission.PERMISSION_FULL_CONTROL);

        // Grant Read access by Goodle ID.
        acl.grantPermission(new UserByIdGrantee("Google member's ID"),
            Permission.PERMISSION_READ);

        // Grant Write access to a group by domain.
        acl.grantPermission(new GroupByDomainGrantee("yourdomain.com"),
            Permission.PERMISSION_WRITE);
    }

}
