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
package org.jets3t.apps.synchronize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3Service;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageService;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.GoogleStorageService;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.multi.DownloadPackage;
import org.jets3t.service.multi.ThreadWatcher;
import org.jets3t.service.multi.ThreadedStorageService;
import org.jets3t.service.multi.event.CreateObjectsEvent;
import org.jets3t.service.multi.event.DeleteObjectsEvent;
import org.jets3t.service.multi.event.DownloadObjectsEvent;
import org.jets3t.service.multi.event.GetObjectHeadsEvent;
import org.jets3t.service.multi.event.ServiceEvent;
import org.jets3t.service.multi.s3.MultipartCompletesEvent;
import org.jets3t.service.multi.s3.MultipartStartsEvent;
import org.jets3t.service.multi.s3.MultipartUploadsEvent;
import org.jets3t.service.multi.s3.S3ServiceEventAdaptor;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.EncryptionUtil;
import org.jets3t.service.security.GSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.FileComparer;
import org.jets3t.service.utils.FileComparerResults;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.MultipartUtils;
import org.jets3t.service.utils.ObjectUtils;
import org.jets3t.service.utils.TimeFormatter;
import org.jets3t.service.utils.FileComparer.PartialObjectListing;

/**
 * Console application to synchronize the local file system with a storage service.
 * For more information and help please see the
 * <a href="http://www.jets3t.org/applications/synchronize.html">Synchronize Guide</a>.
 *
 * @author James Murty
 */
public class Synchronize {
    /**
     * String provided to service as User-Agent description.
     */
    public static final String APPLICATION_DESCRIPTION = "Synchronize/" + Constants.JETS3T_VERSION;

    protected static final int REPORT_LEVEL_NONE = 0;
    protected static final int REPORT_LEVEL_ACTIONS = 1;
    protected static final int REPORT_LEVEL_DIFFERENCES = 2;
    protected static final int REPORT_LEVEL_ALL = 3;

    private StorageService storageService = null;

    private boolean doAction = false; // Files will only be transferred if true.
    private boolean isQuiet = false; // Report will only include summary of actions if true.
    private boolean isNoProgress = false; // Progress messages are not displayed if true.
    private boolean isForce = false; // Files will be overwritten when unchanged if true.
    private boolean isKeepFiles = false; // Files will not be replaced/deleted if true.
    private boolean isNoDelete = false; // Files will not be deleted if true, but may be replaced.
    private boolean isGzipEnabled = false; // Files will be gzipped prior to upload if true.
    private boolean isEncryptionEnabled = false; // Files will be encrypted prior to upload if true.
    private boolean isMoveEnabled = false;
    private boolean isBatchMode = false;
    private int reportLevel = REPORT_LEVEL_ALL;
    private String cryptoPassword = null;
    private Jets3tProperties properties = null;

    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final TimeFormatter timeFormatter = new TimeFormatter();
    private FileComparer fileComparer = null;
    private int maxTemporaryStringLength = 0;
    private final Map<String, Object> customMetadata = new HashMap<String, Object>();

    /**
     * Constructs the application with a pre-initialised service and the user-specified options.
     *
     * @param service
     * a pre-initialised service (including Provider credentials)
     * @param doAction
     * Files will only be transferred if true.
     * @param isQuiet
     * Report will only include summary of actions if true.
     * @param isNoProgress
     * Upload/download progress updates will not be printed.
     * @param isForce
     * Files will be overwritten when unchanged if true.
     * @param isKeepFiles
     * Files will not be replaced/deleted if true.
     * @param isMoveEnabled
     * If true, items will be moved rather than just copied. Files will be
     * deleted after they have been uploaded, and objects will be deleted
     * after they have been downloaded.
     * @param isBatchMode
     * If true, uploads or downloads will proceed in batches rather than all at
     * once. This mode is useful for large buckets where listing all the
     * objects and their details at once may consume a large amount of time
     * and memory.
     * @param isNoDelete
     * Files will not be deleted if true, but may be replaced.
     * @param isGzipEnabled
     * Files will be gzipped prior to upload if true.
     * @param isEncryptionEnabled
     * Files will be encrypted prior to upload if true.
     * @param reportLevel
     * The level or amount of reporting to perform. The default value is
     * {@link #REPORT_LEVEL_ALL}.
     * @param properties
     * The configuration properties that will be used by this instance.
     */
    public Synchronize(StorageService service, boolean doAction, boolean isQuiet,
        boolean isNoProgress, boolean isForce, boolean isKeepFiles,
        boolean isNoDelete, boolean isMoveEnabled, boolean isBatchMode,
        boolean isGzipEnabled, boolean isEncryptionEnabled,
        int reportLevel, Jets3tProperties properties)
    {
        this.storageService = service;
        this.doAction = doAction;
        this.isQuiet = isQuiet;
        this.isNoProgress = isNoProgress;
        this.isForce = isForce;
        this.isKeepFiles = isKeepFiles;
        this.isNoDelete = isNoDelete;
        this.isMoveEnabled = isMoveEnabled;
        this.isBatchMode = isBatchMode;
        this.isGzipEnabled = isGzipEnabled;
        this.isEncryptionEnabled = isEncryptionEnabled;
        this.reportLevel = reportLevel;
        this.properties = properties;
        this.fileComparer = FileComparer.getInstance(properties);

        // Find any custom metadata items specified in property files
        Iterator<Entry<Object, Object>> myPropertiesIter =
            this.properties.getProperties().entrySet().iterator();
        while (myPropertiesIter.hasNext()) {
            Entry<Object, Object> entry = myPropertiesIter.next();
            String keyName = entry.getKey().toString().toLowerCase();
            if (entry.getKey() != null
                && keyName.startsWith("upload.metadata."))
            {
                String metadataName = entry.getKey().toString()
                    .substring("upload.metadata.".length());
                String metadataValue = entry.getValue().toString();
                this.customMetadata.put(metadataName, metadataValue);
            }
        }
    }


    /**
     * Prepares a file to be uploaded to the service, creating an object with the
     * appropriate key and with some jets3t-specific metadata items set.
     */
    class LazyPreparedUploadObject {
        private final String targetKey;
        private final File file;
        private byte[] md5HashOfFile;
        private final String aclString;
        private final EncryptionUtil encryptionUtil;

        /**
         * @param targetKey
         * the key name for the object
         * @param file
         * the file to upload
         * @param md5HashOfFile
         * MD5 hash value of file to upload, may be null in which case this will be auto-generated.
         * @param aclString
         * the ACL to apply to the uploaded object
         * @param encryptionUtil
         * the object to apply encryption, or null if no encryption is required
         */
        public LazyPreparedUploadObject(String targetKey, File file, byte[] md5HashOfFile,
                String aclString, EncryptionUtil encryptionUtil)
        {
            this.targetKey = targetKey;
            this.file = file;
            this.md5HashOfFile = md5HashOfFile;
            this.aclString = aclString;
            this.encryptionUtil = encryptionUtil;
        }

        public StorageObject prepareUploadObject() throws Exception {
            StorageObject newObject = ObjectUtils.createObjectForUpload(
                targetKey, file, md5HashOfFile, encryptionUtil, isGzipEnabled, null);

            if ("PUBLIC_READ".equalsIgnoreCase(aclString)) {
                newObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            } else if ("PUBLIC_READ_WRITE".equalsIgnoreCase(aclString)) {
                newObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
            } else if ("PRIVATE".equalsIgnoreCase(aclString)) {
                // Private is the default, no need to add an ACL
            } else {
                throw new Exception("Invalid value for ACL string: " + aclString);
            }

            // Apply custom metadata items to upload object.
            newObject.addAllMetadata(customMetadata);

            return newObject;
        }

        public File getFile() {
            return file;
        }
    }


    private String formatTransferDetails(ThreadWatcher watcher) {
        String detailsText = "";
        long bytesPerSecond = watcher.getBytesPerSecond();
        detailsText = byteFormatter.formatByteSize(bytesPerSecond) + "/s";

        if (watcher.isTimeRemainingAvailable()) {
            if (detailsText.trim().length() > 0) {
                detailsText += " - ";
            }
            long secondsRemaining = watcher.getTimeRemaining();
            detailsText += "ETA: " + timeFormatter.formatTime(secondsRemaining, false);
        }
        return detailsText;
    }

    private void printOutputLine(String line, int level) {
        if ((isQuiet && level > REPORT_LEVEL_NONE) || reportLevel < level) {
            return;
        }

        String blanks = "";
        for (int i = line.length(); i < maxTemporaryStringLength; i++) {
            blanks += " ";
        }
        System.out.println(line + blanks);
        maxTemporaryStringLength = 0;
    }

    /**
     * Prints text to StdOut provided the isQuiet and isNoProgress flags are not set.
     *
     * @param line the text to print
     */
    private void printProgressLine(String line) {
        if (isQuiet || isNoProgress) {
            return;
        }

        String temporaryLine = "  " + line;
        if (temporaryLine.length() > maxTemporaryStringLength) {
            maxTemporaryStringLength = temporaryLine.length();
        }
        String blanks = "";
        for (int i = temporaryLine.length(); i < maxTemporaryStringLength; i++) {
            blanks += " ";
        }
        System.out.print(temporaryLine + blanks + "\r");
    }

    protected ComparisonResult compareLocalAndRemoteFiles(
        FileComparerResults mergedDiscrepancyResults,
        String bucketName, String rootObjectPath,
        String priorLastKey, Map<String, String> objectKeyToFilepathMap,
        BytesProgressWatcher md5GenerationProgressWatcher)
        throws ServiceException, NoSuchAlgorithmException, FileNotFoundException,
        IOException, ParseException
    {
        // List objects in service. Listing may be complete, or partial.
        printProgressLine("Listing objects in service"
            + (isBatchMode && mergedDiscrepancyResults.getCountOfItemsCompared() > 0
                ? " (Continuing in batches, objects listed previously: "
                    + mergedDiscrepancyResults.getCountOfItemsCompared() + ")"
                : ""));

        boolean forceMetadataDownload = isEncryptionEnabled || isGzipEnabled;

        PartialObjectListing partialListing = fileComparer.buildObjectMapPartial(
            storageService, bucketName, rootObjectPath, priorLastKey,
            objectKeyToFilepathMap, !isBatchMode, forceMetadataDownload, isForce,
            md5GenerationProgressWatcher, serviceEventAdaptor);
        if (serviceEventAdaptor.wasErrorThrown()) {
            throw new ServiceException("Unable to build map of objects",
                serviceEventAdaptor.getErrorThrown());
        }
        md5GenerationProgressWatcher.resetWatcher();

        // Retrieve details from listing.
        priorLastKey = partialListing.getPriorLastKey();
        Map<String, StorageObject> objectsMap = partialListing.getObjectsMap();

        // Compare the listed objects with the local system.
        printProgressLine("Comparing service contents with local system");
        FileComparerResults discrepancyResults = fileComparer.buildDiscrepancyLists(
            objectKeyToFilepathMap, objectsMap, md5GenerationProgressWatcher, isForce);

        // Merge objects and discrepancies to track overall changes.
        mergedDiscrepancyResults.merge(discrepancyResults);

        ComparisonResult result = new ComparisonResult();
        result.priorLastKey = priorLastKey;
        result.objectsMap = objectsMap;
        result.discrepancyResults = discrepancyResults;
        return result;
    }

    /**
     * Copies the contents of a local directory to a service, storing them in the given root path.
     * <p>
     * A set of comparisons is used to determine exactly how the local files differ from the
     * contents of the service location, and files are transferred based on these comparisons and
     * options set by the user.
     * <p>
     * The following object properties are set when a file is uploaded:
     * <ul>
     * <li>The object's key name</li>
     * <li>Content-Length: The size of the uploaded file. This will be 0 for directories, and will
     *     differ from the original file if gzip or encryption options are set.</li>
     * <li>Content-Type: {@link Mimetypes#MIMETYPE_BINARY_OCTET_STREAM} for directories, otherwise a
     *     mimetype determined by {@link Mimetypes#getMimetype} <b>unless</b> the gzip option is
     *     set, in which case the Content-Type is set to application/x-gzip.
     * </ul>
     * <p>
     * The following jets3t-specific metadata items are also set:
     * <ul>
     * <li>The local file's last-modified date, as {@link Constants#METADATA_JETS3T_LOCAL_FILE_DATE}</li>
     * <li>An MD5 hash of file data, as {@link StorageObject#METADATA_HEADER_HASH_MD5}</li>
     * </ul>
     *
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param bucket
     * the bucket to put the objects in (will be created if necessary)
     * @param rootObjectPath
     * the root path where objects are put (will be created if necessary)
     * @param aclString
     * the ACL to apply to the uploaded object
     * @param md5GenerationProgressWatcher
     * a class that reports on the progress of this method
     *
     * @throws Exception
     */
    public void uploadLocalDirectory(Map<String, String> objectKeyToFilepathMap,
        StorageBucket bucket, String rootObjectPath, String aclString,
        BytesProgressWatcher md5GenerationProgressWatcher) throws Exception
    {
        FileComparerResults mergedDiscrepancyResults = new FileComparerResults();
        String priorLastKey = null;
        String lastFileKeypathChecked = "";

        boolean skipMissingFiles =
            this.properties.getBoolProperty("upload.ignoreMissingPaths", false);

        EncryptionUtil encryptionUtil = null;
        if (isEncryptionEnabled) {
            String algorithm = properties
                .getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            encryptionUtil = new EncryptionUtil(cryptoPassword, algorithm, EncryptionUtil.DEFAULT_VERSION);
        }

        // Support for multipart uploads -- currently available for Amazon S3 only
        MultipartUtils multipartUtils = null;
        if (storageService instanceof S3Service) {
            long maxUploadPartSize = properties.getLongProperty(
                "upload.max-part-size", MultipartUtils.MAX_OBJECT_SIZE);
            multipartUtils = new MultipartUtils(maxUploadPartSize);
        }

        // Repeat list and upload actions until all objects in bucket have been listed.
        do {
            ComparisonResult result =
                compareLocalAndRemoteFiles(mergedDiscrepancyResults, bucket.getName(), rootObjectPath,
                    priorLastKey, objectKeyToFilepathMap, md5GenerationProgressWatcher);
            priorLastKey = result.priorLastKey;
            FileComparerResults discrepancyResults = result.discrepancyResults;

            // Repeat upload actions until all local files have been uploaded (or we repeat listing loop)
            Iterator<String> objectKeyIter = objectKeyToFilepathMap.keySet().iterator();
            do {
                List<LazyPreparedUploadObject> objectsToUpload = new ArrayList<LazyPreparedUploadObject>();

                // Iterate through local files and perform the necessary action to synchronize them.
                while (objectKeyIter.hasNext()) {
                    String relativeKeyPath = objectKeyIter.next();

                    String targetKey = relativeKeyPath;
                    if (rootObjectPath.length() > 0) {
                        if (rootObjectPath.endsWith(Constants.FILE_PATH_DELIM)) {
                            targetKey = rootObjectPath + targetKey;
                        } else {
                            targetKey = rootObjectPath + Constants.FILE_PATH_DELIM + targetKey;
                        }
                    }

                    if (isBatchMode) {
                        if (priorLastKey != null && targetKey.compareTo(priorLastKey) > 0) {
                            // We do not yet have the object listing to compare this file.
                            continue;
                        }

                        if (targetKey.compareTo(lastFileKeypathChecked) <= 0) {
                            // We have already handled this file in a prior batch.
                            continue;
                        } else {
                            lastFileKeypathChecked = targetKey;
                        }
                    }

                    File file = new File(objectKeyToFilepathMap.get(relativeKeyPath));

                    // Lookup and/or generate cached MD5 hash file for data file, if enabled
                    byte[] md5HashOfFile = null;
                    if (!file.isDirectory()) {
                        if (fileComparer.isGenerateMd5Files()) {
                            md5HashOfFile = fileComparer.generateFileMD5Hash(file, targetKey, null);
                        } else if (fileComparer.isUseMd5Files()) {
                            md5HashOfFile = fileComparer.lookupFileMD5Hash(file, targetKey);
                        }
                    }

                    if (discrepancyResults.onlyOnClientKeys.contains(relativeKeyPath)) {
                        printOutputLine("N " + targetKey, REPORT_LEVEL_ACTIONS);
                        objectsToUpload.add(new LazyPreparedUploadObject(
                            targetKey, file, md5HashOfFile, aclString, encryptionUtil));
                    } else if (discrepancyResults.updatedOnClientKeys.contains(relativeKeyPath)) {
                        printOutputLine("U " + targetKey, REPORT_LEVEL_ACTIONS);
                        objectsToUpload.add(new LazyPreparedUploadObject(
                            targetKey, file, md5HashOfFile, aclString, encryptionUtil));
                    } else if (discrepancyResults.alreadySynchronisedKeys.contains(relativeKeyPath)
                               || discrepancyResults.alreadySynchronisedLocalPaths.contains(relativeKeyPath))
                    {
                        if (isForce) {
                            printOutputLine("F " + targetKey, REPORT_LEVEL_ACTIONS);
                            objectsToUpload.add(new LazyPreparedUploadObject(
                                targetKey, file, md5HashOfFile, aclString, encryptionUtil));
                        } else {
                            printOutputLine("- " + targetKey, REPORT_LEVEL_ALL);
                        }
                    } else if (discrepancyResults.updatedOnServerKeys.contains(relativeKeyPath)) {
                        // This file has been updated on the server-side.
                        if (isKeepFiles) {
                            printOutputLine("r " + targetKey, REPORT_LEVEL_DIFFERENCES);
                        } else {
                            printOutputLine("R " + targetKey, REPORT_LEVEL_ACTIONS);
                            objectsToUpload.add(new LazyPreparedUploadObject(
                                targetKey, file, md5HashOfFile, aclString, encryptionUtil));
                        }
                    } else {
                        // Uh oh, program error here. The safest thing to do is abort!
                        throw new SynchronizeException("Invalid discrepancy comparison details for file "
                            + file.getPath()
                            + ". Sorry, this is a program error - aborting to keep your data safe");
                    }

                    // If we're batching, break out of upload preparation loop and
                    // actually upload files once we have our quota.
                    if (isBatchMode
                        && objectsToUpload.size() >= Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE)
                    {
                        printOutputLine(
                            "Uploading batch of " + objectsToUpload.size() + " files",
                            REPORT_LEVEL_ACTIONS);
                        break;
                    }
                }

                // Break uploads into (smaller) batches if we are transforming files during upload
                int uploadBatchSize = objectsToUpload.size();
                if ((isEncryptionEnabled || isGzipEnabled)
                    && properties.containsKey("upload.transformed-files-batch-size"))
                {
                    // Limit uploads to small batches in batch mode -- based on the
                    // number of upload threads that are available.
                    uploadBatchSize = properties.getIntProperty("upload.transformed-files-batch-size", 1000);
                }

                // Upload New/Updated/Forced/Replaced objects.
                while (doAction && objectsToUpload.size() > 0) {
                    List<StorageObject> objectsForStandardPut = new ArrayList<StorageObject>();
                    List<StorageObject> objectsForMultipartUpload = new ArrayList<StorageObject>();

                    // Invoke lazy upload object creator.
                    int maxBatchSize = Math.min(uploadBatchSize, objectsToUpload.size());
                    for (int i = 0; i < maxBatchSize; i++) {
                        LazyPreparedUploadObject lazyObj = objectsToUpload.remove(0);
                        StorageObject object = null;

                        try {
                            object = lazyObj.prepareUploadObject();
                        } catch (FileNotFoundException e) {
                            if (skipMissingFiles) {
                                printOutputLine(
                                    "WARNING: Skipping unreadable file: "
                                    + lazyObj.getFile().getAbsolutePath(),
                                    REPORT_LEVEL_NONE);
                                continue;
                            } else {
                                throw e;
                            }
                        }

                        if (multipartUtils != null
                            && multipartUtils.isFileLargerThanMaxPartSize(lazyObj.getFile()))
                        {
                            objectsForMultipartUpload.add(object);
                        } else {
                            objectsForStandardPut.add(object);
                        }
                    }

                    // Perform standard object uploads
                    if (objectsForStandardPut.size() > 0) {
                        (new ThreadedStorageService(storageService, serviceEventAdaptor)).putObjects(
                            bucket.getName(), objectsForStandardPut.toArray(new StorageObject[] {}));
                        serviceEventAdaptor.throwErrorIfPresent();
                    }

                    // Perform multipart uploads
                    if (objectsForMultipartUpload.size() > 0) {
                        multipartUtils.uploadObjects(
                            bucket.getName(), (S3Service)storageService,
                            objectsForMultipartUpload, serviceEventAdaptor);
                    }
                }
            } while (objectKeyIter.hasNext()); // End of upload loop

        } while (priorLastKey != null); // End of list and upload loop

        // Delete objects that don't correspond with local files.
        List<StorageObject> objectsToDelete = new ArrayList<StorageObject>();
        Iterator<String> serverOnlyIter = mergedDiscrepancyResults.onlyOnServerKeys.iterator();
        while (serverOnlyIter.hasNext()) {
            // Relative key
            String relativeKeyPath = serverOnlyIter.next();

            // Build absolute key path for object.
            String targetKey = relativeKeyPath;
            if (rootObjectPath.length() > 0) {
                if (rootObjectPath.endsWith(Constants.FILE_PATH_DELIM)) {
                    targetKey = rootObjectPath + targetKey;
                } else {
                    targetKey = rootObjectPath + Constants.FILE_PATH_DELIM + targetKey;
                }
            }
            StorageObject object = new StorageObject(targetKey);

            if (isKeepFiles || isNoDelete) {
                printOutputLine("d " + relativeKeyPath, REPORT_LEVEL_DIFFERENCES);
            } else {
                printOutputLine("D " + relativeKeyPath, REPORT_LEVEL_ACTIONS);
                if (doAction) {
                    objectsToDelete.add(object);
                }
            }
        }
        if (objectsToDelete.size() > 0) {
            StorageObject[] objects = objectsToDelete.toArray(new StorageObject[objectsToDelete.size()]);
            (new ThreadedStorageService(storageService, serviceEventAdaptor)).deleteObjects(bucket.getName(), objects);
            serviceEventAdaptor.throwErrorIfPresent();
        }

        // Delete local files that have been moved to service.
        List<String> filesMoved = new ArrayList<String>();
        if (isMoveEnabled) {
            filesMoved.addAll(mergedDiscrepancyResults.onlyOnClientKeys);
            filesMoved.addAll(mergedDiscrepancyResults.updatedOnClientKeys);
            filesMoved.addAll(mergedDiscrepancyResults.updatedOnServerKeys);
            filesMoved.addAll(mergedDiscrepancyResults.alreadySynchronisedKeys);

            List<File> dirsToDelete = new ArrayList<File>();
            Iterator<String> filesMovedIter = filesMoved.iterator();
            while (filesMovedIter.hasNext()) {
                String keyPath = filesMovedIter.next();
                File file = new File(objectKeyToFilepathMap.get(keyPath));

                printOutputLine("M " + keyPath, REPORT_LEVEL_ACTIONS);
                if (doAction) {
                    if (file.isDirectory()) {
                        // Delete directories later, as they may still contain
                        // files until this loop completes.
                        dirsToDelete.add(file);
                    } else {
                        file.delete();
                    }
                }
            }
            Iterator<File> dirIter = dirsToDelete.iterator();
            while (dirIter.hasNext()) {
                File dir = dirIter.next();
                dir.delete();
            }
        }

        printOutputLine(
            (doAction ? "" : "[No Action] ") +
            "New files: " + mergedDiscrepancyResults.onlyOnClientKeys.size() +
            ", Updated: " + mergedDiscrepancyResults.updatedOnClientKeys.size() +
            (isKeepFiles?
                ", Kept: " +
                (mergedDiscrepancyResults.updatedOnServerKeys.size())
                :
                ", Reverted: " + mergedDiscrepancyResults.updatedOnServerKeys.size()
                ) +
            (isNoDelete || isKeepFiles?
                ", Not Deleted: " + mergedDiscrepancyResults.onlyOnServerKeys.size()
                :
                ", Deleted: " + mergedDiscrepancyResults.onlyOnServerKeys.size()
                ) +
            (isForce ?
                ", Forced updates: " + mergedDiscrepancyResults.alreadySynchronisedKeys.size() :
                ", Unchanged: " + mergedDiscrepancyResults.alreadySynchronisedKeys.size()
                ) +
            (isMoveEnabled ?
                ", Moved: " + filesMoved.size()
                : ""
                ), REPORT_LEVEL_NONE
            );
    }

    /**
     * Copies the contents of a root path in service to the local file system.
     * <p>
     * A set of comparisons is used to determine exactly how the service objects differ from the
     * local target, and files are transferred based on these comparisons and options set by the user.
     * <p>
     * If an object is gzipped (according to its Content-Type) and the gzip option is set, the object
     * is inflated. If an object is encrypted (according to the metadata item
     * {@link Constants#METADATA_JETS3T_CRYPTO_ALGORITHM}) and the crypt option is set, the object
     * is decrypted. If encrypted and/or gzipped objects are restored without the corresponding option
     * being set, the user will be responsible for inflating or decrypting the data.
     * <p>
     * <b>Note</b>: If a file was backed-up with both encryption and gzip options it cannot be
     * restored with only the gzip option set, as files are gzipped prior to being encrypted and cannot
     * be inflated without first being decrypted.
     *
     * @param objectKeyToFilepathMap
     * map of '/'-delimited object key names to local file absolute paths
     * @param rootObjectPath
     * the root path in service where backed-up objects were stored
     * @param localDirectory the directory to which the objects will be restored
     * @param bucket
     * the bucket into which files were backed up
     * @param md5GenerationProgressWatcher
     * a class that reports on the progress of this method
     *
     * @throws Exception
     */
    public void restoreToLocalDirectory(Map<String, String> objectKeyToFilepathMap,
        String rootObjectPath, File localDirectory, StorageBucket bucket,
        BytesProgressWatcher md5GenerationProgressWatcher) throws Exception
    {
        FileComparerResults mergedDiscrepancyResults = new FileComparerResults();

        String priorLastKey = null;

        // Store local path mapped to storage object for moved objects.
        Map<String, StorageObject> objectsMoved = new HashMap<String, StorageObject>();

        // Repeat download actions until all objects in bucket have been listed.
        do {
            ComparisonResult result =
                compareLocalAndRemoteFiles(mergedDiscrepancyResults, bucket.getName(),
                    rootObjectPath, priorLastKey, objectKeyToFilepathMap,
                    md5GenerationProgressWatcher);
            priorLastKey = result.priorLastKey;
            FileComparerResults discrepancyResults = result.discrepancyResults;
            Map<String, StorageObject> objectsMap = result.objectsMap;

            // Download objects to local files/directories.
            Iterator<String> objectKeyIter = objectsMap.keySet().iterator();

            // Optionally download objects in batches to minimize memory use
            do {
                List<DownloadPackage> downloadPackagesList = new ArrayList<DownloadPackage>();
                while (objectKeyIter.hasNext()) {
                    String keyPath = objectKeyIter.next();
                    StorageObject object = objectsMap.get(keyPath);
                    String localPath = keyPath;

                    // If object metadata is not available, skip zero-byte objects that
                    // are not definitively directory place-holders, since we can't tell
                    // whether they are directory place-holders or normal empty files.
                    if (!object.isMetadataComplete()
                        && object.getContentLength() == 0
                        && !object.isDirectoryPlaceholder())
                    {
                        continue;
                    }

                    File fileTarget = new File(localDirectory, keyPath);
                    // Create local directories corresponding to objects flagged as dirs.
                    if (object.isDirectoryPlaceholder()) {
                        localPath = ObjectUtils.convertDirPlaceholderKeyNameToDirName(keyPath);
                        fileTarget = new File(localDirectory, localPath);
                        if (doAction) {
                            fileTarget.mkdirs();
                        }
                    }

                    if (discrepancyResults.onlyOnServerKeys.contains(keyPath)) {
                        printOutputLine("N " + localPath, REPORT_LEVEL_ACTIONS);
                        DownloadPackage downloadPackage = ObjectUtils.createPackageForDownload(
                            object, fileTarget, isGzipEnabled, isEncryptionEnabled, cryptoPassword);
                        if (downloadPackage != null) {
                            downloadPackagesList.add(downloadPackage);
                        }
                    } else if (discrepancyResults.updatedOnServerKeys.contains(keyPath)) {
                        printOutputLine("U " + localPath, REPORT_LEVEL_ACTIONS);
                        DownloadPackage downloadPackage = ObjectUtils.createPackageForDownload(
                            object, fileTarget, isGzipEnabled, isEncryptionEnabled, cryptoPassword);
                        if (downloadPackage != null) {
                            downloadPackagesList.add(downloadPackage);
                        }
                    } else if (discrepancyResults.alreadySynchronisedKeys.contains(keyPath)) {
                        if (isForce) {
                            printOutputLine("F " + localPath, REPORT_LEVEL_ACTIONS);
                            DownloadPackage downloadPackage = ObjectUtils.createPackageForDownload(
                                object, fileTarget, isGzipEnabled, isEncryptionEnabled, cryptoPassword);
                            if (downloadPackage != null) {
                                downloadPackagesList.add(downloadPackage);
                            }
                        } else {
                            printOutputLine("- " + localPath, REPORT_LEVEL_ALL);
                        }
                    } else if (discrepancyResults.updatedOnClientKeys.contains(keyPath)) {
                        // This file has been updated on the client-side.
                        if (isKeepFiles) {
                            printOutputLine("r " + localPath, REPORT_LEVEL_DIFFERENCES);
                        } else {
                            printOutputLine("R " + localPath, REPORT_LEVEL_ACTIONS);
                            DownloadPackage downloadPackage = ObjectUtils.createPackageForDownload(
                                object, fileTarget, isGzipEnabled, isEncryptionEnabled, cryptoPassword);
                            if (downloadPackage != null) {
                                downloadPackagesList.add(downloadPackage);
                            }
                        }
                    } else {
                        // Uh oh, program error here. The safest thing to do is abort!
                        throw new SynchronizeException("Invalid discrepancy comparison details for object "
                            + localPath
                            + ". Sorry, this is a program error - aborting to keep your data safe");
                    }

                    if (isMoveEnabled) {
                        objectsMoved.put(localPath, object);
                    }

                    // Optionally break up download sets into batches
                    if (isBatchMode
                        && downloadPackagesList.size() >= Constants.DEFAULT_OBJECT_LIST_CHUNK_SIZE)
                    {
                        printOutputLine(
                            "Downloading batch of " + downloadPackagesList.size() + " objects",
                            REPORT_LEVEL_ACTIONS);
                        break;
                    }
                }

                // Download New/Updated/Forced/Replaced objects from service.
                if (doAction && downloadPackagesList.size() > 0) {
                    DownloadPackage[] downloadPackages = downloadPackagesList.toArray(
                        new DownloadPackage[downloadPackagesList.size()]);
                    (new ThreadedStorageService(storageService, serviceEventAdaptor)).downloadObjects(
                        bucket.getName(), downloadPackages);
                    serviceEventAdaptor.throwErrorIfPresent();
                }
            } while (objectKeyIter.hasNext());

        } while (priorLastKey != null);

        // Delete local files that don't correspond with service objects.
        List<File> dirsToDelete = new ArrayList<File>();
        Iterator<String> clientOnlyIter = mergedDiscrepancyResults.onlyOnClientKeys.iterator();
        while (clientOnlyIter.hasNext()) {
            String keyPath = clientOnlyIter.next();
            File file = new File(objectKeyToFilepathMap.get(keyPath));

            if (isKeepFiles || isNoDelete) {
                printOutputLine("d " + keyPath, REPORT_LEVEL_DIFFERENCES);
            } else {
                printOutputLine("D " + keyPath, REPORT_LEVEL_ACTIONS);
                if (doAction) {
                    if (file.isDirectory()) {
                        // Delete directories later, as they may still have files
                        // inside until this loop completes.
                        dirsToDelete.add(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        Iterator<File> dirIter = dirsToDelete.iterator();
        while (dirIter.hasNext()) {
            File dir = dirIter.next();
            dir.delete();
        }

        // Delete objects in service that have been moved to the local computer.
        if (isMoveEnabled) {
            List<String> objectsMovedLocalPaths = new ArrayList<String>(objectsMoved.size());
            objectsMovedLocalPaths.addAll(objectsMoved.keySet());
            Collections.sort(objectsMovedLocalPaths);

            for (String movedLocalPath: objectsMovedLocalPaths) {
                if (doAction) {
                    printOutputLine("M " + movedLocalPath, REPORT_LEVEL_ACTIONS);
                } else {
                    printOutputLine("m " + movedLocalPath, REPORT_LEVEL_ACTIONS);
                }
            }

            if (objectsMoved.size() > 0 && doAction) {
                StorageObject[] objects = objectsMoved.values().toArray(
                    new StorageObject[objectsMoved.size()]);
                (new ThreadedStorageService(storageService, serviceEventAdaptor)).deleteObjects(
                    bucket.getName(), objects);
                serviceEventAdaptor.throwErrorIfPresent();
            }
        }

        printOutputLine(
            (doAction ? "" : "[No Action] ") +
            "New files: " + mergedDiscrepancyResults.onlyOnServerKeys.size() +
            ", Updated: " + mergedDiscrepancyResults.updatedOnServerKeys.size() +
            (isKeepFiles?
                ", Kept: " +
                (mergedDiscrepancyResults.updatedOnClientKeys.size())
                :
                ", Reverted: " + mergedDiscrepancyResults.updatedOnClientKeys.size()
                ) +
            (isNoDelete || isKeepFiles?
                ", Not Deleted: " + mergedDiscrepancyResults.onlyOnClientKeys.size()
                :
                ", Deleted: " + mergedDiscrepancyResults.onlyOnClientKeys.size()
                ) +
            (isForce ?
                ", Forced updates: " + mergedDiscrepancyResults.alreadySynchronisedKeys.size() :
                ", Unchanged: " + mergedDiscrepancyResults.alreadySynchronisedKeys.size()
                ) +
            (isMoveEnabled ?
                ", Moved: " + objectsMoved.size() :
                ""
                ), REPORT_LEVEL_NONE
            );
    }

    /**
     * Runs the application, performing the action specified on the given service and local directory paths.
     *
     * @param servicePath
     * the path in service (including the bucket name) to which files are backed-up, or from which files are restored.
     * @param files
     * an array of one or more File objects for Uploads, or a single target directory for Downloads.
     * @param actionCommand
     * the action to perform, UP(load) or DOWN(load)
     * @param cryptoPassword
     * if non-null, an {@link EncryptionUtil} object is created with the provided password to encrypt or decrypt files.
     * @param aclString
     * the ACL to apply to the uploaded object
     * @param providerId
     * service provider name: "S3" or "GS"
     *
     * @throws Exception
     */
    public void run(String servicePath, File[] files, String actionCommand, String cryptoPassword,
        String aclString, String providerId) throws Exception
    {
        String bucketName = null;
        String objectPath = "";
        int slashIndex = servicePath.indexOf(Constants.FILE_PATH_DELIM);
        if (slashIndex >= 0) {
            // We have a bucket name and an object path.
            bucketName = servicePath.substring(0, slashIndex);
            objectPath = servicePath.substring(slashIndex + 1, servicePath.length());
        } else {
            // We only have a bucket name.
            bucketName = servicePath;
        }

        // Describe the action that will be performed.
        if ("UP".equals(actionCommand)) {
            String uploadPathSummary = null;

            if (files.length > 1) {
                int dirsCount = 0;
                int filesCount = 0;

                for (File file: files) {
                    if (file.isDirectory()) {
                        dirsCount++;
                    } else {
                        filesCount++;
                    }
                }
                uploadPathSummary =
                    "["
                    + dirsCount + (dirsCount == 1 ? " directory" : " directories")
                    + ", " + filesCount + (filesCount == 1 ? " file" : " files") + "]";
            } else {
                uploadPathSummary = Arrays.toString(files);
            }

            printOutputLine("UP "
                + (doAction ? "" : "[No Action] ")
                + "Local " + uploadPathSummary + " => " + providerId + "[" + servicePath + "]",
                REPORT_LEVEL_NONE);
        } else if ("DOWN".equals(actionCommand)) {
            if (files.length != 1) {
                throw new SynchronizeException("Only one target directory is allowed for downloads");
            }
            printOutputLine("DOWN "
                + (doAction ? "" : "[No Action] ")
                + providerId + "[" + servicePath + "] => Local " + files[0], REPORT_LEVEL_NONE);
        } else {
            throw new SynchronizeException("Action string must be 'UP' or 'DOWN'");
        }

        this.cryptoPassword = cryptoPassword;

        StorageBucket bucket = null;
        if (storageService.getProviderCredentials() == null) {
            // Using an anonymous connection, don't check bucket ownership or attempt to create it.
            bucket = new StorageBucket(bucketName);
        } else {
            // Using an authentication connection, so check for bucket ownership and create one if necessary.
            try {
                bucket = storageService.getBucket(bucketName);
            } catch (ServiceException e) {
                // Don't give up if we cannot find our bucket in an account listing via ListAllBuckets,
                // since the whole account may not be accessible but the bucket itself may be.
            }

            if (bucket == null) {
                // Bucket does not exist in this user's account or is inaccessible, try creating it.
                try {
                    bucket = storageService.createBucket(new StorageBucket(bucketName));
                } catch (ServiceException e) {
                    // Bucket could not be created, either someone else already owns it
                    // or we don't have create permissions.
                    try {
                        // Let's see if we can at least access the bucket...
                        storageService.listObjectsChunked(bucketName, null, null, 1, null, false);
                        // ... if we get this far we're dealing with a
                        // bucket we can read. That's fine, let's proceed.
                        bucket = new StorageBucket(bucketName);
                    } catch (ServiceException e2) {
                        // We can't create or access this bucket, time to give up.
                        throw new SynchronizeException(
                            "Unable to create or access bucket: " + bucketName, e);
                    }
                }
            }
        }

        boolean storeEmptyDirectories = properties
            .getBoolProperty("uploads.storeEmptyDirectories", true);

        // Generate of object key names to absolute paths of local files
        printProgressLine("Listing files in local file system");
        Map<String, String> objectKeyToFilepathMap = null;
        if ("UP".equals(actionCommand)) {
            // Ensure all files/directories chosen for upload exist and are accessible
            for (File file: files) {
                if (!file.exists()) {
                    throw new IOException("File '" + file.getPath() + "' does not exist");
                }
            }
            objectKeyToFilepathMap = fileComparer.buildObjectKeyToFilepathMap(
                files, "", storeEmptyDirectories);
        } else if ("DOWN".equals(actionCommand)) {
            File[] filesInTargetDir = files[0].listFiles();
            if (filesInTargetDir == null) {
                throw new IOException("Unable to list files in download target directory: "
                    + files[0].getAbsolutePath());
            }
            objectKeyToFilepathMap = fileComparer.buildObjectKeyToFilepathMap(
                filesInTargetDir, "", storeEmptyDirectories);
        }

        // Watcher to provide feedback during generation of MD5 hash values
        final long filesSizeTotal[] = new long[] { 0 }; // Don't know how much comparison req'd
        BytesProgressWatcher md5GenerationProgressWatcher =
            new BytesProgressWatcher(filesSizeTotal[0]) {
                @Override
                public void updateBytesTransferred(long byteCount) {
                    super.updateBytesTransferred(byteCount);
                    printProgressLine("Comparing files: " +
                        byteFormatter.formatByteSize(super.getBytesTransferred()));
                }
            };

        if ("UP".equals(actionCommand)) {
            uploadLocalDirectory(objectKeyToFilepathMap, bucket, objectPath,
                aclString, md5GenerationProgressWatcher);
        } else if ("DOWN".equals(actionCommand)) {
            restoreToLocalDirectory(objectKeyToFilepathMap, objectPath,
                files[0], bucket, md5GenerationProgressWatcher);
        }
    }

    /**
     * Runs the application, performing the action specified on the given service and local directory paths.
     *
     * @param servicePath
     * the path in service (including the bucket name) to which files are backed-up, or from which files are restored.
     * @param files
     * a set of one or more of File objects for Uploads, or a single target directory for Downloads.
     * @param actionCommand
     * the action to perform, UP(load) or DOWN(load)
     * @param cryptoPassword
     * if non-null, an {@link EncryptionUtil} object is created with the provided password to encrypt or decrypt files.
     * @param aclString
     * the ACL to apply to the uploaded object
     * @param providerId
     * service provider name: "S3" or "GS"
     *
     * @throws Exception
     */
    public void run(String servicePath, List<File> files, String actionCommand, String cryptoPassword,
        String aclString, String providerId) throws Exception
    {
        File[] filesArray = files.toArray(new File[files.size()]);
        this.run(servicePath, filesArray, actionCommand, cryptoPassword, aclString, providerId);
    }

    private void displayProgressStatus(String prefix, ThreadWatcher watcher) {
        String progressMessage = prefix + watcher.getCompletedThreads() + "/" + watcher.getThreadCount();

        // Show percentage of bytes transferred, if this info is available.
        if (watcher.isBytesTransferredInfoAvailable()) {
            String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
            long percentage = (int)
                (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);

            String detailsText = formatTransferDetails(watcher);

            progressMessage += " - " + percentage + "% of " + bytesTotalStr
                + (detailsText.length() > 0 ? " (" + detailsText + ")" : "");
        } else {
            long percentage = (int)
                (((double)watcher.getCompletedThreads() / watcher.getThreadCount()) * 100);

            progressMessage += " - " + percentage + "%";
        }
        printProgressLine(progressMessage);
    }

    S3ServiceEventAdaptor serviceEventAdaptor = new S3ServiceEventAdaptor() {
        private void displayIgnoredErrors(ServiceEvent event) {
            if (ServiceEvent.EVENT_IGNORED_ERRORS == event.getEventCode()) {
                Throwable[] throwables = event.getIgnoredErrors();
                for (int i = 0; i < throwables.length; i++) {
                    printOutputLine("Ignoring error: " + throwables[i].getMessage(), REPORT_LEVEL_ALL);
                }
            }
        }

        @Override
        public void event(CreateObjectsEvent event) {
            super.event(event);
            displayIgnoredErrors(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Upload: ", event.getThreadWatcher());
            }
        }

        @Override
        public void event(MultipartStartsEvent event) {
            super.event(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Starting large file uploads: ",
                    event.getThreadWatcher());
            }
        }

        @Override
        public void event(MultipartCompletesEvent event) {
            super.event(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Completing large file uploads: ",
                    event.getThreadWatcher());
            }
        }

        @Override
        public void event(MultipartUploadsEvent event) {
            super.event(event);
            displayIgnoredErrors(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Large upload parts: ", event.getThreadWatcher());
            }
        }

        @Override
        public void event(DownloadObjectsEvent event) {
            super.event(event);
            displayIgnoredErrors(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Download: ", event.getThreadWatcher());
            }
        }

        @Override
        public void event(GetObjectHeadsEvent event) {
            super.event(event);
            displayIgnoredErrors(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Retrieving object details from service: ", event.getThreadWatcher());
            }
        }

        @Override
        public void event(DeleteObjectsEvent event) {
            super.event(event);
            displayIgnoredErrors(event);
            if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
                displayProgressStatus("Deleting objects in service: ", event.getThreadWatcher());
            }
        }
    };

    private class ComparisonResult {
        public String priorLastKey;
        public FileComparerResults discrepancyResults;
        public Map<String, StorageObject> objectsMap;
    }

    /**
     * Prints usage/help information and forces the application to exit with errorcode 1.
     */
    private static void printHelpAndExit(boolean fullHelp) {
        System.out.println();
        System.out.println("Usage: Synchronize [options] UP <Path> <File/Directory> (<File/Directory>...)");
        System.out.println("   or: Synchronize [options] DOWN <Path> <DownloadDirectory>");
        System.out.println("");
        System.out.println("UP      : Synchronize the contents of the Local Directory with a service.");
        System.out.println("DOWN    : Synchronize the contents of a service with the Local Directory");
        System.out.println("Path    : A path to the resource. This must include at least the");
        System.out.println("          bucket name, but may also specify a path inside the bucket.");
        System.out.println("          E.g. <bucketName>/Backups/Documents/20060623");
        System.out.println("File/Directory : A file or directory on your computer to upload");
        System.out.println("DownloadDirectory : A directory on your computer where downloaded files");
        System.out.println("          will be stored");
        System.out.println();
        System.out.println("Required properties can be provided via: a file named 'synchronize.properties'");
        System.out.println("in the classpath, a file specified with the --properties option, or by typing");
        System.out.println("them in when prompted on the command line. Required properties are:");
        System.out.println("          accesskey : Your Access Key (Required)");
        System.out.println("          secretkey : Your Secret Key (Required)");
        System.out.println("          password  : Encryption password (only required when using crypto)");
        System.out.println("Properties specified in this file will override those in jets3t.properties.");
        if (!fullHelp) {
            System.out.println("");
            System.out.println("For more help : Synchronize --help");
            System.exit(1);
        }

        System.out.println("");
        System.out.println("Options");
        System.out.println("-------");
        System.out.println("-h | --help");
        System.out.println("   Displays this help message.");
        System.out.println("");
        System.out.println("--provider <provider id>");
        System.out.println("   Service provider, either 'S3' for Amazon S3 or 'GS' for Google Storage");
        System.out.println("");
        System.out.println("-n | --noaction");
        System.out.println("   No action taken. No files will be changed locally or on service, instead");
        System.out.println("   a report will be generating showing what will happen if the command");
        System.out.println("   is run without the -n option.");
        System.out.println("");
        System.out.println("-q | --quiet");
        System.out.println("   Runs quietly, without reporting on each action performed or displaying");
        System.out.println("   progress messages. The summary is still displayed.");
        System.out.println("");
        System.out.println("-p | --noprogress");
        System.out.println("   Runs somewhat quietly, without displaying progress messages.");
        System.out.println("   The action report and overall summary are still displayed.");
        System.out.println("");
        System.out.println("-f | --force");
        System.out.println("   Force tool to perform synchronization even when files are up-to-date.");
        System.out.println("   This may be useful if you need to update metadata or timestamps online.");
        System.out.println("");
        System.out.println("-k | --keepfiles");
        System.out.println("   Keep outdated files on destination instead of reverting/removing them.");
        System.out.println("   This option cannot be used with --nodelete.");
        System.out.println("");
        System.out.println("-d | --nodelete");
        System.out.println("   Keep files on destination that have been removed from the source. This");
        System.out.println("   option is similar to --keepfiles except that files may be reverted.");
        System.out.println("   This option cannot be used with --keepfiles.");
        System.out.println("");
        System.out.println("-m | --move");
        System.out.println("   Move items rather than merely copying them. Files on the local computer will");
        System.out.println("   be deleted after they have been uploaded to service, or objects will be deleted");
        System.out.println("   from service after they have been downloaded. Be *very* careful with this option.");
        System.out.println("   This option cannot be used with --keepfiles.");
        System.out.println("");
        System.out.println("-b | --batch");
        System.out.println("   Download or upload files in batches, rather than all at once. Enabling this");
        System.out.println("   option will reduce the memory required to synchronize large buckets, and will");
        System.out.println("   ensure file transfers commence as soon as possible. When this option is");
        System.out.println("   enabled, the progress status lines refer only to the progress of a single batch.");
        System.out.println("");
        System.out.println("-g | --gzip");
        System.out.println("   Compress (GZip) files when backing up and Decompress gzipped files");
        System.out.println("   when restoring.");
        System.out.println("");
        System.out.println("-c | --crypto");
        System.out.println("   Encrypt files when backing up and decrypt encrypted files when restoring. If");
        System.out.println("   this option is specified the properties must contain a password.");
        System.out.println("");
        System.out.println("--properties <filename>");
        System.out.println("   Load the synchronizer app properties from the given file rather than from");
        System.out.println("   a synchronizer.properties file in the classpath.");
        System.out.println("");
        System.out.println("--credentials <filename>");
        System.out.println("   Load your service credentials from an encrypted file, rather than from the");
        System.out.println("   synchronizer.properties file. This encrypted file can be created using");
        System.out.println("   the Cockpit application, or the JetS3t API library.");
        System.out.println("");
        System.out.println("--acl <ACL string>");
        System.out.println("   Specifies the Access Control List setting to apply. This value must be one");
        System.out.println("   of: PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE. This setting will override any");
        System.out.println("   acl property specified in the synchronize.properties file");
        System.out.println("");
        System.out.println("--reportlevel <Level>");
        System.out.println("   A number that specifies how much report information will be printed:");
        System.out.println("   0 - no report items will be printed (the summary will still be printed)");
        System.out.println("   1 - only actions are reported          [Prefixes N, U, D, R, F, M]");
        System.out.println("   2 - differences & actions are reported [Prefixes N, U, D, R, F, M, d, r]");
        System.out.println("   3 - DEFAULT: all items are reported    [Prefixes N, U, D, R, F, M, d, r, -]");
        System.out.println("");
        System.out.println("Report");
        System.out.println("------");
        System.out.println("Report items are printed on a single line with an action flag followed by");
        System.out.println("the relative path of the file or object. The report legend follows:");
        System.out.println("");
        System.out.println("N: A new file/object will be created");
        System.out.println("U: An existing file/object has changed and will be updated");
        System.out.println("D: A file/object existing on the target does not exist on the source and");
        System.out.println("   will be deleted.");
        System.out.println("d: A file/object existing on the target does not exist on the source but");
        System.out.println("   because the --keepfiles or --nodelete option was set it was not deleted.");
        System.out.println("R: An existing file/object has changed more recently on the target than on the");
        System.out.println("   source. The target version will be reverted to the older source version");
        System.out.println("r: An existing file/object has changed more recently on the target than on the");
        System.out.println("   source but because the --keepfiles option was set it was not reverted.");
        System.out.println("-: A file is identical between the local system and service, no action is necessary.");
        System.out.println("F: A file identical locally and in service was updated due to the Force option.");
        System.out.println("M: The file/object will be moved (deleted after it has been copied to/from service).");
        System.out.println();
        System.exit(1);
    }

    /**
     * Runs this application from the console, accepts and checks command-line parameters and runs an
     * upload or download operation when all the necessary parameters are provided.
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        // Load default JetS3t properties
        Jets3tProperties myProperties =
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
        String propertiesFileName = "synchronize.properties";

        // Read the Synchronize properties file from the classpath
        Jets3tProperties synchronizeProperties =
            Jets3tProperties.getInstance(propertiesFileName);
        if (synchronizeProperties.isLoaded()) {
            myProperties.loadAndReplaceProperties(synchronizeProperties,
                propertiesFileName + " in classpath");
        }

        // Required arguments
        String actionCommand = null;
        String servicePath = null;
        int reqArgCount = 0;
        Set<File> fileSet = new HashSet<File>();

        // Options
        boolean doAction = true;
        boolean isQuiet = false;
        boolean isNoProgress = false;
        boolean isForce = false;
        boolean isKeepFiles = false;
        boolean isNoDelete = false;
        boolean isGzipEnabled = false;
        boolean isEncryptionEnabled = false;
        boolean isMoveEnabled = false;
        boolean isBatchMode = false;
        String aclString = null;
        int reportLevel = REPORT_LEVEL_ALL;
        ProviderCredentials providerCredentials = null;
        String providerId = "S3";

        // Parse arguments.
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                // Argument is an option.
                if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("--help")) {
                    printHelpAndExit(true);
                } else if (arg.equalsIgnoreCase("-n") || arg.equalsIgnoreCase("--noaction")) {
                    doAction = false;
                } else if (arg.equalsIgnoreCase("-q") || arg.equalsIgnoreCase("--quiet")) {
                    isQuiet = true;
                } else if (arg.equalsIgnoreCase("-p") || arg.equalsIgnoreCase("--noprogress")) {
                    isNoProgress = true;
                } else if (arg.equalsIgnoreCase("-f") || arg.equalsIgnoreCase("--force")) {
                    isForce = true;
                } else if (arg.equalsIgnoreCase("-k") || arg.equalsIgnoreCase("--keepfiles")) {
                    isKeepFiles = true;
                } else if (arg.equalsIgnoreCase("-d") || arg.equalsIgnoreCase("--nodelete")) {
                    isNoDelete = true;
                } else if (arg.equalsIgnoreCase("-g") || arg.equalsIgnoreCase("--gzip")) {
                    isGzipEnabled = true;
                } else if (arg.equalsIgnoreCase("-c") || arg.equalsIgnoreCase("--crypto")) {
                    isEncryptionEnabled = true;
                } else if (arg.equalsIgnoreCase("-m") || arg.equalsIgnoreCase("--move")) {
                    isMoveEnabled = true;
                } else if (arg.equalsIgnoreCase("-s") || arg.equalsIgnoreCase("--skipmetadata")) {
                    System.err.println("WARNING: --skipmetadata is obsolete since JetS3t 0.8.1, it has no effect");
                } else if (arg.equalsIgnoreCase("-b") || arg.equalsIgnoreCase("--batch")) {
                    isBatchMode = true;
                } else if (arg.equalsIgnoreCase("--provider")) {
                    if (i + 1 < args.length) {
                        // Read custom Synchronize properties file from the specified file
                        i++;
                        providerId = args[i];
                        if (!"S3".equalsIgnoreCase(providerId)
                            && !"GS".equalsIgnoreCase(providerId))
                        {
                            System.err.println("ERROR: --provider option must be one of 'S3' or 'GS'");
                            printHelpAndExit(false);
                        }
                    } else {
                        System.err.println("ERROR: --provider option must be followed by a provider ID 'S3' or 'GS'");
                        printHelpAndExit(false);
                    }
                } else if (arg.equalsIgnoreCase("--properties")) {
                    if (i + 1 < args.length) {
                        // Read custom Synchronize properties file from the specified file
                        i++;
                        propertiesFileName = args[i];
                        File propertiesFile = new File(propertiesFileName);
                        if (!propertiesFile.canRead()) {
                            System.err.println("ERROR: The properties file " + propertiesFileName + " could not be found");
                            System.exit(2);
                        }
                        myProperties.loadAndReplaceProperties(
                            new FileInputStream(propertiesFileName), propertiesFile.getName());
                    } else {
                        System.err.println("ERROR: --properties option must be followed by a file path");
                        printHelpAndExit(false);
                    }
                } else if (arg.equalsIgnoreCase("--acl")) {
                    if (i + 1 < args.length) {
                        // Read the acl setting string
                        i++;
                        aclString = args[i];

                        if (!"PUBLIC_READ".equalsIgnoreCase(aclString)
                            && !"PUBLIC_READ_WRITE".equalsIgnoreCase(aclString)
                            && !"PRIVATE".equalsIgnoreCase(aclString))
                        {
                            System.err.println("ERROR: Acess Control List setting \"acl\" must have one of the values "
                                + "PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE");
                            printHelpAndExit(false);
                        }
                    } else {
                        System.err.println("ERROR: --acl option must be followed by an ACL string");
                        printHelpAndExit(false);
                    }
                } else if (arg.equalsIgnoreCase("--reportlevel")) {
                    if (i + 1 < args.length) {
                        // Read the report level integer
                        i++;
                        try {
                            reportLevel = Integer.parseInt(args[i]);

                            if (reportLevel < 0 || reportLevel > 3) {
                                System.err.println("ERROR: Report Level setting \"reportlevel\" must have one of the values "
                                    + "0 (no reporting), 1 (actions only), 2 (differences only), 3 (DEFAULT - all reporting)");
                                printHelpAndExit(false);
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR: --reportlevel option must be followed by 0, 1, 2 or 3");
                            printHelpAndExit(false);
                        }
                    } else {
                        System.err.println("ERROR: --reportlevel option must be followed by 0, 1, 2 or 3");
                        printHelpAndExit(false);
                    }
                } else if (arg.equalsIgnoreCase("--credentials")) {
                    if (i + 1 < args.length) {
                        // Read the credentials file location
                        i++;
                        File credentialsFile = new File(args[i]);
                        if (!credentialsFile.canRead()) {
                            System.err.println("ERROR: Cannot read credentials file '" + credentialsFile + "'");
                            printHelpAndExit(false);
                        }
                        while (providerCredentials == null) {
                            String credentialsPassword = PasswordInput.getPassword(
                                "Password for credentials file '" + credentialsFile + "'");
                            try {
                                providerCredentials = ProviderCredentials.load(credentialsPassword, credentialsFile);
                                // Set dummy accesskey and secretkey property values, to avoid prompting for these values later on.
                                myProperties.setProperty("accesskey", "");
                                myProperties.setProperty("secretkey", "");
                            } catch (ServiceException e) {
                                System.out.println("Failed to read credentials from the file '" + credentialsFile + "'");
                            }
                        }
                    } else {
                        System.err.println("ERROR: --credentials option must be followed by a file path");
                        printHelpAndExit(false);
                    }
                } else {
                    System.err.println("ERROR: Invalid option: " + arg);
                    printHelpAndExit(false);
                }
            } else {
                // Argument is one of the required parameters.
                if (reqArgCount == 0) {
                    actionCommand = arg.toUpperCase(Locale.getDefault());
                    if (!"UP".equals(actionCommand) && !"DOWN".equals(actionCommand)) {
                        System.err.println("ERROR: Invalid action command " + actionCommand
                            + ". Valid values are 'UP' or 'DOWN'");
                        printHelpAndExit(false);
                    }
                } else if (reqArgCount == 1) {
                    servicePath = arg;
                } else if (reqArgCount > 1) {
                    File file = new File(arg);

                    if ("DOWN".equals(actionCommand)) {
                        if (reqArgCount > 2) {
                            System.err.println("ERROR: Only one target directory may be specified"
                                + " for " + actionCommand);
                            printHelpAndExit(false);
                        }
                        if (!file.exists()) {
                            // Create missing target directory
                            if (!file.mkdirs()) {
                                System.err.println(
                                    "ERROR: Target download directory does not exist and could not be created: " + file);
                                System.exit(1);
                            }
                        }
                        if (file.exists() && !file.isDirectory()) {
                            System.err.println(
                                "ERROR: Target download location already exists but is not a directory: " + file);
                            System.exit(1);
                        }
                        if (!file.canWrite() || !file.canWrite()) {
                            System.err.println(
                                "ERROR: Invalid permissions on target download location, cannot "
                                + (!file.canRead()
                                    ? "read from" + (!file.canWrite()? " or " : "")
                                    : "")
                                + (!file.canWrite() ? "write to" : "")
                                + " directory: " + file.getAbsolutePath());
                            System.exit(1);
                        }
                        if (!file.canWrite()) {
                            System.err.println(
                                "ERROR: Cannot write to target download location: " + file);
                            System.exit(1);
                        }
                    } else {
                        if (!file.canRead()) {
                            if (myProperties != null && myProperties.getBoolProperty("upload.ignoreMissingPaths", false)) {
                                System.err.println("WARN: Ignoring missing upload path: " + file);
                                continue;
                            } else {
                                System.err.println(
                                    "ERROR: Cannot read upload file/directory: " + file + "\n" +
                                    "       To ignore missing paths set the property upload.ignoreMissingPaths");
                                printHelpAndExit(false);
                            }
                        }
                    }
                    fileSet.add(file);
                }
                reqArgCount++;
            }
        }

        if (fileSet.size() < 1
            && !myProperties.getBoolProperty("upload.ignoreMissingPaths", false))
        {
            // Missing one or more required parameters.
            System.err.println("ERROR: Missing required file path(s)");
            printHelpAndExit(false);
        }

        if (isKeepFiles && isNoDelete) {
            // Incompatible options.
            System.err.println("ERROR: Options --keepfiles and --nodelete cannot be used at the same time");
            printHelpAndExit(false);
        }

        if (isKeepFiles && isMoveEnabled) {
            // Incompatible options.
            System.err.println("ERROR: Options --keepfiles and --move cannot be used at the same time");
            printHelpAndExit(false);
        }

        // Ensure the Synchronize properties file contains everything we need, and prompt
        // for any required information that is missing.
        if (!myProperties.containsKey("accesskey")
            || !myProperties.containsKey("secretkey")
            || (isEncryptionEnabled && !myProperties.containsKey("password")))
        {
            System.out.println("Please enter the required properties that have not been provided in a properties file:");
            BufferedReader inputReader = new BufferedReader(new InputStreamReader(System.in));
            if (!myProperties.containsKey("accesskey")) {
                System.out.print("Acccess Key: ");
                myProperties.setProperty("accesskey", inputReader.readLine());
            }
            if (!myProperties.containsKey("secretkey")) {
                System.out.print("Secret Key: ");
                myProperties.setProperty("secretkey", inputReader.readLine());
            }
            if (isEncryptionEnabled && !myProperties.containsKey("password")) {
                String password1 = "password1";
                String password2 = "password2";
                while (!password1.equals(password2)) {
                    password1 = PasswordInput.getPassword("Encryption password");
                    password2 = PasswordInput.getPassword("Confirm password");
                    if (!password1.equals(password2)) {
                        System.out.println("The original and confirmation passwords do not match, try again.");
                    }
                }
                myProperties.setProperty("password", password1);
            }
        }

        // Use property values for the credentials, if we haven't already been
        // given the credentials through the --credentials argument.
        if (providerCredentials == null) {
            if ("S3".equalsIgnoreCase(providerId)) {
                providerCredentials = new AWSCredentials(
                    myProperties.getStringProperty("accesskey", null),
                    myProperties.getStringProperty("secretkey", null));
            } else if ("GS".equalsIgnoreCase(providerId)) {
                providerCredentials = new GSCredentials(
                    myProperties.getStringProperty("accesskey", null),
                    myProperties.getStringProperty("secretkey", null));
            }
        }

        // Sanity-check credentials -- if both are null or empty strings,
        // then nullify the credentials object to get an anonymous connection.
        if (providerCredentials.getAccessKey() == null || providerCredentials.getAccessKey().length() == 0
            || providerCredentials.getSecretKey() == null || providerCredentials.getSecretKey().length() == 0)
        {
            providerCredentials = null;
        }

        if (aclString == null) {
            aclString = myProperties.getStringProperty("acl", "PRIVATE");
        }
        if (!"PUBLIC_READ".equalsIgnoreCase(aclString)
            && !"PUBLIC_READ_WRITE".equalsIgnoreCase(aclString)
            && !"PRIVATE".equalsIgnoreCase(aclString))
        {
            System.err.println("ERROR: Acess Control List setting \"acl\" must have one of the values "
                + "PRIVATE, PUBLIC_READ, PUBLIC_READ_WRITE");
            System.exit(2);
        }

        StorageService service = null;
        if ("S3".equalsIgnoreCase(providerId)) {
            service = new RestS3Service(
                providerCredentials, APPLICATION_DESCRIPTION,
                new CommandLineCredentialsProvider(), myProperties);
        } else if ("GS".equalsIgnoreCase(providerId)) {
            service = new GoogleStorageService(
                providerCredentials, APPLICATION_DESCRIPTION,
                new CommandLineCredentialsProvider(), myProperties);
        }

        // Perform the UPload/DOWNload.
        Synchronize client = new Synchronize(
            service, doAction, isQuiet, isNoProgress, isForce, isKeepFiles, isNoDelete,
            isMoveEnabled, isBatchMode, isGzipEnabled, isEncryptionEnabled,
            reportLevel, myProperties);
        client.run(servicePath,
            fileSet.toArray(new File[fileSet.size()]),
            actionCommand,
            myProperties.getStringProperty("password", null), aclString,
            providerId.toUpperCase());
    }

}