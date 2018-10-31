JetS3t Release Notes
====================

-------------
Version 0.9.4
-------------

### TOOLKIT

S3 Service:

 * Fix bugs in Amazon signature version 4 implementation (#204)
   NOTE: Anyone who has implemented their own JetS3t service implemented the
   `JetS3tRequestAuthorizer` will need to adjust their code due to API changes.
 * Fix for request URI not escaped when using AWS4 signing (#212)
 * Potential fix for null pointer in exceptions in
   `ServiceUtils#findBucketNameInHostOrPath` (#205)
 * Fix for InputStream uploads using AWSv4 signature, and fast-fail in
   multipart upload utils which do not properly support input streams (#206)
 * Fix to ensure explicitly correct URL structure for object copy operations
   via `x-amz-copy-source` (#210)
 * Fix for edge-case bug parsing `LastModifiedDate` from S3 response header
   when there is a clash with user metadata (#213)
 * Improve `HttpException` error message to include response code & description
   (#214)
 * Avoid race conditions initialising default JetS3tProperties from file (#216)
   
Credential Management:

 * Add support to load credentials from AWS IAM Instance roles on EC2 with
   new `AWSEC2IAMSessionCredentials` credentials class for fetching and using
   credentials provided by IAM instance roles via EC2 instance data. (#163)
   
General:

 * Updated required libraries to latest versions: Bouncy Castle 1.52;
   Commons Codec 1.9; Commons Logging 1.2; HttpClient 4.5; HttpCore 4.4.1;
   Jackson Core ASL 1.9.13; Java XML Builder 1.1; Log4J 1.2.17 
 * Fix BouncyCastle crypto jar references in shell scripts (#208)
   
### Maven

 * Make mx4j and mail library dependencies optional
 * Allow for point release updates to commons-codec

### KUDOS TO

 * David Kocher (dkocher) for numerous fixes and improvements, as usual. Cheers!
 * gillouxg for detailed and helpful bug reports.
 * Adrian Kelly (adrianbk) for bug report and patch to help fix #206.
 * Felix Natter (fnatter) for improvements to the new 
   `AWSEC2IAMSessionCredentials` (#163) 
 * The Alchemist (The_Alchemist) and Stephen Beitzel for improvements to the
   Maven POM. 
 * Sky Nss (skynss) for a detailed bug report and follow-up to identify the
   latent bug causing issue #213
 * Gian Merlino (gianmerlino) for suggested improvement to `HttpException`
   error messages (#214)


-------------
Version 0.9.3
-------------

### TOOLKIT

S3 Service - Amazon signature version 4 (AWS4-HMAC-SHA256):

 * Support Amazon signature version 4 (AWS4-HMAC-SHA256) when signing requests,
   in particular to permit access to version-4-only region eu-central-1 (#183)
 * Add some smarts, voodoo, magic, and fairy dust to JetS3t to make version 4
   (AWS4-HMAC-SHA256) signatures work in *most* cases without requiring the
   user to always know a bucket's S3 region in advance. See in particular the
   bucket-to-region cache `RegionEndpointCache` in `RestStorageService` (#183)
 * Automatically switch to version 4 signatures when accessing buckets in a
   region that doesn't support legacy version 2 signatures, namely
   "eu-central-1" (#183)
 * Support generating pre-signed URLs using AWS signature version 4 with
   the new `S3Service.createSignedUrlUsingSignatureVersion` method (#202)
 * Fix bug preventing JetS3t applications Sycnronize and Cockpit etc from
   working with AWSv4 signatures (#203) 

General:

 * Updated BouncyCastle crypto library requirements to latest versions:
   bcprov-jdk15on-1.51 (#196)
 * Updated HttpComponents library requirements to latest versions:
   httpclient 4.3.6; httpcore 4.3.3 (#197)
 * `MultipartUtils.uploadObject` now closes file streams on upload failure (#198)
 * Fix null pointer exception when building XML to update object ACL settings
   based on an ACL with missing DisplayName (#201)
   
### MAVEN

 * Remove problematic config files that were being included in Maven
   distribution, especially a default logging config, that could break things
   for some users (#199)


-------------
Version 0.9.2
-------------

### MAVEN CENTRAL

 * Fix Maven dependency on third-party library unavailable in Maven Central 
   by in-lining BareBonesBrowserLauncher utility code into the JetSet codebase.
 * Fix broken build for Java 8 caused by new aggresive Javadoc syntax checking.
 * Avoid GPG signing step of build-and-release process unless it is explicitly
   enabled, as this is pointless overhead for everyone but me. 

### TOOLKIT

General:

 * Add `.hgignore` file to project so Mercurial SCM will ignore common project
   and build artefacts.
 * Updated library requirements to latest versions:
   java-xmlbuilder 1.0
 * CopyObject operations now return details useful for tracking in result 
   headers map (#192)


-------------
Version 0.9.1
-------------

### TOOLKIT

General:

 * Updated library requirements to latest versions:
   commons-logging 1.1.3; httpclient 4.3.2; httpcore 4.3.1; commons-codec 1.8;
   java-xmlbuilder 0.6
 * Storage objects now make fine-grained metadata maps available when possible
   including: user-specified metadata via #getUserMetadataMap; service-specified
   metadata via #getServiceMetadataMap; a complete and unfiltered set of metadata
   via #getCompleteMetadataMap (#171) 
 * Fixed `httpclient.read-throttle` upload bandwidth throttling implementation 
   which wasn't working for values under 128KB. It should now throttle correctly
   down to single-digit KB/s values.
 * Fixed bug in object/file comparison logic that prevented object metadata from 
   being downloaded when a service-side target path was used (e.g. using a bucket
   path like *target-bucket/some/path* in Synchronize)
 * Improved automatic time adjustment feature to use the 'Date'
   timestamp in a RequestTimeTooSkewed error response to calculate
   a time offset, instead of performing an extra GET request (#173).
 * Remove dependency on safehaus UUI library, as UUID is now included in Java 6.
 * Fix file comparison bugs with metadata lookup and local MD5 hash caching
   (#159, #161)  

S3Service:

 * Added support to apply *canned* ACL settings when starting a multipart upload,
   and to apply arbitrary ACL settings (canned or otherwise) to a multipart
   object uploaded with the `putObjectMaybeAsMultipart` convenience method (#184)
 * Close INPUT form tags in S3 POST upload generated form (#148) 
 * Fix bugs when using AWS server-side encryption
 * Add API-level support for S3 bucket lifecycle configuration (#158)
 * Add STORAGE_CLASS_GLACIER storage class. 
 * Apply ACL settings to multipart uploads: canned ACL settings are applied in
   all cases, non-canned ACLs are applied by #putObjectMaybeAsMultipart (#184)
 * Fix bug that caused failure of multi-part uploads with service-side encryption.

CloudFrontService:

 * Added support for setting time-to-live (TTL) values as low as zero
   seconds.
 * Support for API version 2012-05-05 (#150)
   
SimpleThreadedStorageService / ThreadedStorageService:

 * Added mechanism to selectively permit individual object-level failures
   within a multi-threaded service operation without causing the whole 
   operation to abort. This is particularly useful for handling missing objects
   in service during multi-threaded operations without failing the whole
   operation.
   
   Error conditions are permitted by providing an ErrorPermitter callback
   handler class, while object-level failures result in generation of a 
   ThrowableBearningStorageObject being generated instead of a normal
   StorageObject. (#181)

GoogleStorageService:

 * Google Storage website configuration and other improvements (by dkocher)

### SYNCHRONIZE

 * Fix bug that prevented Synchronize from deleting service-side objects when
   files within a pseudo-directory were downloaded to the local system with
   the --move option.

### COCKPIT

 * Distribution management dialog now supports viewing and setting the
   MinTTL value for non-streaming distributions.
   
### UTILITIES

 * New console utility to delete outdated incomplete Multipart Upload parts:
   DeleteMultipartUploads

### KUDOS TO

 * Chris Baker for catching the `httpclient.read-throttle` and object/file
   comparison issues, investigating the bugs and testing the fixes.
 * David Kocher for a **multitude** of fixes and improvements, see his pull
   requests and contributions at https://bitbucket.org/dkocher/jets3t/
 * Michael Howard (michaelthoward) for impetus and code contributions leading
   to improved handling of missing objects in multi-threaded operations.
 * Matteo Bertozzi (matteobertozzi) for fixing bug preventing use of
   service-side encryption for multipart objects.  
    

-------------
Version 0.9.0
-------------

### TOOLKIT

General:

 * Major update to use HttpClient library version 4.x instead of
   version 3.x
 * All storage objects (e.g. StorageObject/StorageBucket and subclasses)
   now ignore case in metadata item names. The case of item names is
   retained and available if necessary, but metadata add/get methods
   no longer pay attention to case.
 * Specify a default mimetype to use for uploaded files with an
   unrecognized or missing extension by defining a '*' extension
   in the mime.types config file.

GoogleStorageService:

 * Support for OAuth2 authentication mechanism, with automatic
   access token refresh.

RestS3Service:

 * Support for multiple object deletes in a single request
 * Explicit support for new S3 locations: Oregon (us-west-2),
   South America (sa-east-1), GovCloud US West (s3-us-gov-west-1),
   GovCloud US West FIPS 140-2 (s3-fips-us-gov-west-1)
 * Support for server-side encryption, with per-object setting of algorithm
   and default algorithm configuration 's3service.server-side-encryption'
 * Support for Multipart Upload Part - Copy operation, to add data from
   existing S3 objects to multipart uploads.
 * Support for signing S3 requests with response-altering request 
   parameters like "response-content-type", "response-content-disposition"
 * Support for temporary/session-based AWS account credentials, via the new
   AWSSessionCredentials class.

### SYNCHRONIZE

 * Fixed bug in that prevented objects encrypted during upload from being
   automatically decrypted during download (Issue 98)

### KUDOS TO

 * David Kocher for many improvements to HTTP handling and OAuth support
   for Google Storage.
 * davidphillips on BitBucket for initial patch that led to implementation
   of Multipart Upload Part - Copy operation.
 * vaporwarecorp on BitBucket for patch to improve generality of 
   StorageService.
 * Gilles Gaillard (gillouxg) for a patch to add support for prefix and
   delimiter constraints when listing multipart uploads.
 * Michael Howard (michaelthoward) for the impetus to better-support fine-
   grained failure modes in the multipart services, and for code
   contributions.


------------------------
Version 0.8.1 - Apr 2011
------------------------

### TOOLKIT

General:

 * StorageObject and its subclasses (e.g. S3Object) now include a
   convenience constructor that accepts byte array data.
 * Fixed incorrect file path for XMLBuilder library in shell scripts.
 * Fixed issue in FileComparer utility that prevented locally-deleted
   files within directories from being noticed and removed from the
   remote service. Also affected JetS3t's Synchronize and Cockpit apps.
   See https://bitbucket.org/jmurty/jets3t/issue/69
 * Greatly reduced memory usage in FileComparer file comparison
   utilities when working with large numbers of files.
 * Service credentials classes now always return ProviderCredentials
   from #load methods, where the actual credentials instance is
   AWSCredentials by default or GSCredentials if explicitly loaded.
 * FileComparer utility now skips/ignores 'special' files on the local
   file system, to avoid problems synchronizing system directories
   that contain files that are not really data and cannot be
   synchronized to a storage service.

S3Service:

 * Added support for the Multipart Uploads feature, including the
   utility class MultipartUtils to make it easier to use.
 * Added support for the Website Configuration feature, which allows
   static S3 bucket to behave more like a dynamic web site with
   custom index and error documents.
 * Added support for configuring buckets to send Simple Notification
   Service (SNS) messages.

ThreadedS3Service:

 * Added threaded service subclass of ThreadedStorageService for
   features specific to Amazon's S3, namely multipart upload.

CloudFrontService:

 * Added support for custom (non-S3) distribution origins (API 2010-11-01).
   This improvement involved changes to the CloudFrontService API which
   are not backwards-compatible, so code that relies on the prior version
   will need to be updated.

### SYNCHRONIZE

 * Improved batching algorithm to reduce memory usage when working with
   large numbers of files.
 * Added support for the Amazon S3 service's Multipart Upload feature,
   which allows large files to be uploaded in smaller parts for improved
   transfer performance or to upload files larger than 5 GB.
 * Added new property `upload.max-part-size` to synchronize.properties
   file, to allow the maximum upload part size to be configured when
   using the Amazon S3 service. The maximum part size defaults to 5 GB.
 * Improved file comparison logic so object metadata is only retrieved
   from a service when it is required.
 * Removed support for `--skipmetadata` option. This option is no longer
   necessary since metadata retrieval is more intelligent, and is not
   desirable because metadata retrieval is no longer optional when
   comparing objects uploaded using the Multipart Upload mechanism.

### COCKPIT

 * Fixed nasty bug that prevented directories from being included when
   uploading files.

### KUDOS TO

 * gillouxg on BitBucket for patches for Multipart functionality.
 * Tim Sylvester for patches related to Multipart Uploads.


------------------------
Version 0.8.0 - Oct 2010
------------------------

### TOOLKIT

General:

 * New generic bucket, object, and multi-threaded service classes for 
   interacting with either of the S3 or Google Storage services.
 * No long compatible with JDK 1.4. JDK 6+ is now required to run JetS3t.
 * Removed antiquated and unsupported SOAP S3 service implementation
   from toolkit.
 * FileComparer and related tools are now compatible with both S3 and
   Google Storage services.
 * FileComparer tool, and therefore all JetS3t apps, now uses a trailing
   slash to denote directory placeholder objects in S3 instead of
   the custom `application/x-directory` content-type. This should
   improve compatibility with other storage management tools, especially
   Amazon's AWS Console.
  
JetS3t Property File changes:

 * Renamed `s3service.internal-error-retry-max`  
   to `storage-service.internal-error-retry-max`
 * Renamed `s3service.stream-retry-buffer-size`
   to `uploads.stream-retry-buffer-size`
 * Renamed `s3service.defaultStorageClass` 
   to `s3service.default-storage-class`
 * Added properties for new ThreadedStorageService:
   `threaded-service.max-thread-count`
   `threaded-service.admin-max-thread-count`
   `threaded-service.ignore-exceptions-in-multi`
  
ThreadedStorageService / SimpleThreadedStorageService:

 * New multi-threaded service classes that allow operations to be 
   performed in bulk on either the RestS3Service or GoogleStorageService
   service implementations.

GoogleStorageService:

 * Google Storage (GS) service class added to provide specific support
   for Google's storage provider.

S3Service:

 * Added support for bucket policy documents: set, get, and delete.
 * Methods for setting and using AWS DevPay credentials moved from
   S3Service to RestS3Service.

CloudFront Service:

 * Added support for setting Default Root Object for CloudFront distributions.
 * Added support for API version 2010-08-01 and invalidation of objects.
 * Modified CloudFrontService#signUrl and #signUrlCanned to accept raw resource
   URL or path strings, instead of expressing all resources as HTTP URLs. This
   allows for URL signing of HTTPS and RTMP streaming resources.  

SOAP implementation:

 * **REMOVED** from project.

### COCKPIT

 * Added explicit support for logging in to either the Google Storage or
   Amazon S3 service in the startup/login dialog.
 * A CloudFront distribution's Default Root Object setting can be configured
   in the "CloudFront Distributions" dialog.

### SYNCHRONIZE

 * Directly supports new GoogleStorageService, allowing synchronization with
   Google Storage accounts through the native API.
 * Choose target service end-point with the new `--provider` command argument.

### KUDOS TO

 * Claudio Cherubino of Google Inc., for significant refactoring work
   and the addition of full support for the Google Storage service.
 * David Kocher for a patch to implement Default Root Object support
   in the CloundFrontService, and for his invaluable testing and feedback. 


------------------------
Version 0.7.4 - Jul 2010
------------------------

### TOOLKIT

General:

 * Added support for the new Reduced Redundancy Storage (RRS) class
   for objects. This discounted storage class can be applied when an
   object is created, overwritten or copied. The storage class can be
   set per-object, or a default value can be set with the property
   's3service.defaultStorageClass'. Storage classes can be disabled
   altogether for services that don't support them using the
   's3service.enable-storage-classes' property.
 * Added support for buckets located in the Asia Pacific
   (Singapore) location `ap-southeast-1`.
 * Eucalyptus-friendly generated URLs (#14). JetS3t configuration
   properties 's3service.s3-endpoint-virtual-path',
   's3service.s3-endpoint-http-port' and
   's3service.s3-endpoint-https-port' are no longer ignored when
   generating signed or torrent URLs.
 * Bash shell scripts now support cygwin by automatically
   converting classpaths to Windows format.
 * Improved configurability and flexibility by removing dependence
   on the VM-wide static Constants#S3_HOSTNAME constant value
   (see API Changes below)

CloudFront Service:

 * NOTE: These changes break backward-compatibility for some of the
   CloudFrontService and related class interfaces.
 * Updated service to support API version 2010-06-01 which adds
   support for HTTPS-only distributions (a.k.a. RequiredProtocols)
 * Updated service to support API version 2010-05-01 which adds
   logging support for streaming distributions.

API Changes:

 * Removed support for methods and metadata attributes that have been
   obsolete (and deprecated) since version 0.6.0.
 * Deprecated static methods in S3Service for generating signed URLs.
   The new non-static method equivalents should be used from now on to
   avoid dependency on a VM-wide S3 endpoint constant.

SOAP implementation:

 * **Deprecated** in preparation for imminent removal from project.

### COCKPIT

 * Added support for the REDUCED_REDUNDANCY storage class to the
   Preferences dialog (set default storage class for uploads),
   the Copy or Move dialog (set the storage class for destination
   objects), and the Object Attributes dialog (see the current
   storage class property of objects)
 * Added support for buckets located in the Asia Pacific
   (Singapore) location.
 * Added support for creating HTTPS-only CloudFront distributions.
 * Removed overly fussy string-length checks in credentials dialog.

### SYNCHRONIZE

 * Allow synchronization with third-party buckets that are not owned by
   the user, provided the user has the necessary access to that bucket.

### KUDOS TO

 * Alexey I. Froloff for Eucalyptus-friendly generated URLs patch.
 * David Kocher for patches to allow advanced users to provide their own
   RequestEntity classes when uploading objects, and to improve HTTP
   retry handling.


------------------------
Version 0.7.3 - Mar 2010
------------------------

### TOOLKIT

General:

 * Added support for the new S3 Bucket Versioning feature, and
   for Multi-Factor authenticated deletes.
 * S3ServiceMulti#downloadObjects method now automatically verifies
   that the data received matches the ETag MD5 hash value provided by
   S3. JetS3t's applications, and any third-party clients that use
   this method, will therefore get automatic data verificaiton of
   downloads similar to that provided (in most cases) for uploads.
 * Removed methods incompatible with JDK version 1.4.

CloudFront Service:

 * Updated service to support API version 2010-03-01 which allows
   streaming distributions to be made private.
 * Fixed bug parsing that prevented AWS Account Numbers from
   being parsed from XML responses into a DistributionConfig object.

### COCKPIT

 * Fixed bug that caused Cockpit to create buckets in the US Standard
   location regardless of the location selected by the user.

### SYNCHRONIZE

 * When an encryption password or credentials file password must be
   entered by a user at the command prompt the characters are no
   longer echoed to standard output when run on JDK 1.6 or greater.

### KUDOS TO

 * Bennett Hiles for echo-less password input for Synchronize and
   removal of methods incompatible with JDK 1.4.


------------------------
Version 0.7.2 - Dec 2009
------------------------

### TOOLKIT

General:

 * Added support for private and streaming CloudFront distributions (see
   the "CloudFront Service" section below)
 * Changed default thread and connection count settings to improve
   reliability when tranferring many large files and performance when
   performing object administration tasks. Default property settings for
   maximum simultaneous admin threads and HTTP connections are increased
   from 10 to 20, while the default number of non-admin threads is reduced
   from 4 to 2.
 * Improved compatibility with other S3 tools by setting the following
   properties to true by default:
   `filecomparer.ignore-panic-dir-placeholders`
   `filecomparer.ignore-s3organizer-dir-placeholders`
 * Added new property `filecomparer.ignore-s3organizer-dir-placeholders`
   which allows JetS3t to ignore directory place-holder objects created
   by the S3 Organizer Firefox extension when uploading or downloading
   files.
 * Added a new property `filecomparer.md5-files-root-dir` which allows MD5
   files generated for file comparison purposes to be stored in a custom
   path, instead of within the same directory structure as data files.
 * Added support for the property `filecomparer.skip-symlinks` which causes
   symlink/alias files to be ignored by JetS3t applications when deciding
   which files to send to S3. Although "soft" links can be detected and
   ignored, hard links cannot be detected and will be treated as ordinary
   files.
 * Added support for the TargetGrants setting of logged S3 buckets. This
   allows you to set default ACL access permissions for S3 bucket logs
   generated by the service.
 * JetS3t's JMX instrumentation delegate is now re-initialized each time a
   new S3Service is created, allowing users to enable/disable JMX logging
   by adjusting System properties after a JVM has started. Note that it is
   still not possible to enable/disable Bucket- or Object-specific event
   logging after the JVM has started.
 * JMX logging for JetS3t can be enabled on Java 1.6 systems by including
   the "jets3t.mx" system property, since the "com.sun.management.jmxremote"
   property is no longer required to turn on JMX support in the JVM.
 * Improved the Windows batch scripts to correctly detect the JETS3T_HOME
   location regardless of the current directory when the JetS3t script is run.

CloudFront Service:

 * API support for creating and managing private distributions:
   - create and manage Origin Access Identifiers as required for private
     distributions.
   - generate canned and custom-policy signed URLs for private distributions
     protected with mandatory request signing.
 * API support for creating and managing Streaming Distributions.

REST Implementation:

 * Added sanity-checking of HTTP header values as obtained from an S3Object's
   metadata map. Non-ASCII header names and header duplicates caused by
   capitalization mismatches now fail early with descriptive S3ServiceException
   errors.
 * Fixed bug in signed-url based uploading that caused automatic ETag
   verification to fail with a NullPointerException if an ETag value wasn't set
   prior to upload.
 * Fixed NullPointerException in error reporting when REST service is run
   without a network available.
 * Added property setting `httpclient.connection-manager-timeout` which allows
   users to specify a timeout when an S3 operation is waiting for a connection
   to become available in the HttpClient connection pool. The default setting
   of 0 means wait indefinitely.

### SYNCHRONIZE

 * You can now set custom metadata information when uploading objects to S3
   using Synchronize property names prefixed with `upload.metadata.`, for
   example:
   upload.metadata.Cache-Control=max-age=300
   upload.metadata.Expires=Thu, 01 Dec 1994 16:00:00 GMT
   upload.metadata.my-metadata-item=This is the value for my metadata item
 * Synchronize can be told to continue even if local files or folders included
   in the UPLOAD command are missing or unreadable, by setting the
   `upload.ignoreMissingPaths` property to true. Normally Synchronize would
   consider this an error and halt. WARNING: This property could be dangerous
   if used unwisely because it could cause legitimate objects in S3 to be
   deleted due to problems on the local file system. Beware!

### KUDOS TO

 * Christian Mallwitz for informing us of the %~dp0 script variable that greatly
   improves the Windows batch scripts.
 * Jawahar Lal Nayak for:
   - a patch that contributed to the new TargetGrants support for logged
     S3 buckets.
   - bug report about the NPE error condition when the REST implementation is
     run on a machine without a network connection available.
 * Benjamin Schmaus for patches to configure the REST connection pool timeout,
   and to make HTTP response headers available for failed REST connections via
   S3ServiceExceptions.
 * Justin C for a patch that inspired Synchonize's ability to upload files with
   custom metadata using `upload.metadata.` properties.


------------------------
Version 0.7.1 - May 2009
------------------------

### COCKPIT

 * Fixed a menu display bug that caused the bucket and object action menus to
   appear behind other GUI elements on some Windows systems.
 * The Logging Status settings of CloudFront distributions can now be managed
   within the CloudFront Distributions dialog box (see the Tools menu).
 * New "Switch login" Service menu item allows uses with multiple S3 accounts
   to quickly switch between them, once the initial log in has been performed.

### TOOLKIT

General:

 * Added support for JMX instrumentation of S3Service functions. To enable
   JMX set the system property `com.sun.management.jmxremote`. Instrumentation
   for S3Bucket and S3Object MBeans is disabled by default, to enable this
   reporting set the following system properties to `true`:
   "jets3t.bucket.mx", "jets3t.object.mx"
 * Added simplified constructors for S3Object, so the object's bucket need
   not be specified in advance. This is most useful when uploading data.
 * Added method getAccountOwner() to S3Service which allows you to look up the
   ID and display name of an S3 account's owner, even if the account contains
   no buckets.
 * Tweaks to improve support for using JetS3t with the open source Eucalyptus
   cloud computing service.

REST Implementation:

 * Fixed a configuration error that caused the REST implementation to be limited
   to 20 simultaneous connections, regardless of the `httpclient.max-connections`
   property setting. Now the JetS3t property `httpclient.max-connections` sets
   the global connection limit, while the optional property
   `httpclient.max-connections-per-host` sets the per-host connection limit.

CloudFront Service Implementation:

 * Added support for the new Logging Status feature that allows log files for
   activity in your CloudFront distributions to be sent to an S3 bucket.

### KUDOS TO

 * Laxmilal Menaria of Chambal.com Inc. for a patch that contributed to the new
   S3Service#getAccountOwner feature.
 * Doug MacEachern of Hyperic.com for contributing the bulk of a JMX instrumentation
   implementation.
 * peter_griess, askwar and leebutts (java.net users) for identifying bugs and
   providing patches or solutions.


------------------------
Version 0.7.0 - Jan 2009
------------------------

### COCKPIT

 * Added ability to view and manage Amazon CloudFront distributions using
   a dialog box.
 * Added support for configuring Requester Pays buckets.
 * Improved support for DevPay credentials, which can now be set and saved
   directly in the login dialog box instead of only in a properties file.
 * User preferences can now be remembered on your computer, so they do not need
   to be re-set every time you start Cockpit.
 * Improved dialog box for generating Signed URLs. URLs for multiple objects
   can now be generated at once.

### SYNCHRONIZE

 * AWS credentials and the cryptographic password can now be provided via prompts
   on the command-line, rather than merely through a properties file.
 * Added the --credentials option, which allows AWS credentials to be loaded from
   an encrypted file rather than an insecure properties file. The encrypted file
   can be created with the AWSCredentials API or the Cockpit application.
 * Synchronize will act as an anonymous client if empty values are provided
   as the AWS access and secret keys. When the S3 connection is anonymous, only
   public buckets will be accessible.
 * Fixed a bug that prevented Synchronize from recognizing duplicate file names
   when uploading to an S3 subdirectory path when using the --batch option.
 * Synchronize will now prompt for HTTP Proxy login credentials if required. If
   proxy credentials are pre-specified in the jets3t.properties file, you should
   not be prompted.
 * Improved handling of uploads of many files where the files must first be
   transformed (compressed or encrypted). In this case, files are now transformed
   and uploaded in batches, where the batch size is set by the
   synchronize.properties setting 'upload.transformed-files-batch-size'

### TOOLKIT

General:

 * Added initial implementation of CloudFront service.
 * Added property `cloudfront-service.internal-error-retry-max` for defining the
   retry limit for CloudFront internal service errors.
 * Added support for configuring and accessing Requester Pays buckets.
 * Added property `httpclient.requester-pays-buckets-enabled` for defining whether
   the RestS3Service supports Requester Pays buckets by default.
 * Improved support for accessing S3 using DevPay credentials with a new
   credentials class for defining and storing these credentials:
   AWSDevPayCredentials
 * Added a class (AWSDevPayProduct) to represent a DevPay product, and to load
   information about pre-defined products from the properties file
   devpay_products.properties.
 * Improved the interpretation of .jets3t-ignore files so that child paths or
   arbitrary paths can be specified and ignored. For example, to ignore all CVS
   directories you can now add a single ignore path at the top level: **/CVS

REST Implementation:

 * Added support for proxy authentication settings in jets3t.properties:
   "proxy-user", "proxy-password", and "proxy-domain"
 * The service's HTTP proxy settings can be updated an reapplied on-demand using
   a range of #initHttpProxy methods.
 * Added property settings to allow the default HTTP and HTTPS ports to be
   changed, which can be handy when running the service through a proxy for
   testing: s3service.s3-endpoint-http-port, s3service.s3-endpoint-https-port
 * The HttpConnectionManager and HttpClient objects used by the REST service can
   now be reinitialised on-demand using the #initHttpConnection method.
 * The underlying HttpClient and HttpConnectionManager objects can be accessed
   to provide greater control and flexibility after a service is created.
 * The automatic time adjustment performed in response to RequestTimeTooSkewed
   errors will now work through proxies.

### KUDOS TO

 * David Kavanagh for sample code that helped improve HTTP proxy support and
   configurability.
 * Nikolas Coukouma of Zmanda Inc. for patches to significantly improve support
   for Amazon DevPay in the library and Cockpit applications.
 * Allan Frank for a patch that helped improved the Synchronize application's
   handling of uploads.


------------------------
Version 0.6.1 - Aug 2008
------------------------

### COCKPIT

 * Added support for copying or moving objects within and between buckets.
 * Added dialog to assist in moving/renaming multiple objects within a bucket.
 * Added support for updating objects with new metadata values.
 * Fixed issue where downloading items to a directory containing many files and
   directories (eg 250K+) would cause Cockpit to fail with OutOfMemory errors.
 * Added a Confirm Password field to the encryption settings, to help prevent
   problems where a password is mistyped.
 * Cockpit can now generate valid signed GET URLs for DevPay S3 accounts.

### SYNCHRONIZE

 * Synchonize now recognizes when you have specified a partial S3 path when
   synchronizing DOWN. A partial path is one that does not exactly match a
   directory path. Partial paths can now act as a prefix test that identifies
   objects in an S3 path, where only the objects in S3 that match the prefix are
   downloaded. For example, in a bucket that has a Docs subdirectory containing
   objects named with a timestamp prefix, the S3 path "my-bucket/Docs/2008" will
   identify the objects in the Docs subdirectory with names beginning with 2008.
 * Added --batch option that causes Synchronize to compare and download/upload
   files in batches of 1000, rather than all at once. This option will reduce the
   memory required to synchronize buckets with many objects, and will allow file
   transfers to commence as soon as possible rather than after the slow
   comparison process.
 * Added --skipmetadata option that causes Synchronize to skip the retrieval of
   object metadata information from S3. This makes synchs much faster for large
   buckets, but leave the app with less info to make decisions.
 * Added --reportlevel option that allows the user to control how much report
   detail is printed, from 0 (no reporting) to 3 (all reporting).
 * Added --move option that deletes local files after they have been uploaded to
   S3, or deletes objects from S3 after they have been downloaded.
 * Property settings in the synchronize.properties file, or in the file referred
   to by the --properties option, will override properties of the same name in
   jets3t.properties. This makes it easy to create task-specific properties
   files.

### TOOLKIT

General:

 * Added support for the new Copy Object functionality that allows you to copy,
   move, rename and update objects in your S3 account. These operations are
   available through the S3Service as the methods copyObject, moveObject,
   renameObject and updateObjectMetadata.
 * Added support for conditional copying of objects, based on ETag and
   Modified Date comparisons.
 * Made it easier to verify that your data is correctly stored in S3 without
   corruption during transit, by calculating Content-MD5 hash values by default
   in commonly-used S3Object constructors and providing S3Object#verifyData
   methods that make it easy to verify data downloaded from S3.
 * Added basic support for accessing DevPay S3 accounts. DevPay user and product
   tokens can be provided to the S3Service class directly using the
   #setDevPayUserToken and #setDevPayProductToken methods, and default token
   values can be specified in the jets3t.properties file using the settings
   "devpay.user-token" and "devpay.product-token".
 * Modified Bucket Logging Status changing behaviour to update ACL settings of
   the target bucket before (re)directing log files to the bucket.
 * Fixed bug in RestS3Service that caused failures in some circumstances when
   buckets were created with the `US` location.
 * S3Service instances can now be configured individually by providing a
   JetS3tProperties object when constructing the service. The property values
   in this object can also be updated programmatically after the object has
   been constructed.
 * FileComparer now supports an optional setting that makes it assume local files
   are the latest version when there is a clash between the modification dates
   and hash values of a local file and an object stored in S3. This option should
   only be used as a work-around for users who synchronize Microsoft Excel files
   to S3, as these are the only documents that exhibit the mismatch. To enable
   the option, set the jets3t.properties item
   `filecomparer.assume-local-latest-in-mismatch` to true.
 * Added support to FileComparer for listing objects based on prefix "partitions"
   that allow a bucket to be listed by multiple simultaneous threads. This can
   speed up listings for large buckets containing many virtual subdirectories.
   To use multiple partitions when building S3 object maps, configure the
   jets3t.properties item `filecomparer.bucket-listing.<bucketname>` to specify
   a delimiter string and a traversal depth value separated by a comma, eg:
   filecomparer.bucket-listing.my-bucket=/,2
   to partition the bucket my-bucket based on the '/' delimiter to a depth of
   2 virtual subdirectories.

REST Implementation:

 * Added support for the new Copy Object functionality, and conditional copying.
 * The XML parsing code now copes better when object names contain a carriage
   return character. A work-around is applied to prevent the Java XML parser
   from misinterpreting these characters, a fault which could cause some objects
   to become un-deletable. This new feature is enabled by default, but can be
   disabled with the jets3t.properties setting `xmlparser.sanitize-listings`.
 * Changed default AuthenticationPreemptive setting for HttpClient connections
   from true to false, to improve compatibility with NTLM proxies. Preemptive
   auth can be turned on by setting `httpclient.authentication-preemptive` to
   true in jets3t.properties.
 * Refactored repeatable input streams implementations to recognize and support
   standard InputStream objects that can be reset. Thanks to Keith Bonawitz for
   the idea and patch for this.

Multi-threaded Service:

 * Added a new listObjects method that performs a multi-threaded listing of a
   bucket's contents. You provide an array of prefix strings which serve to
   divide your objects into a number of "partitions", and the service performs
   these prefix-based listings in parallel. See
   src/org/jets3t/samples/ThreadedObjectListing.java for example usage.
 * Added a new notification event ServiceEvent#EVENT_IGNORED_ERRORS that provides
   information about exceptions that were ignored during a multi-threaded S3
   operation. Exceptions are only ignored if the JetS3t property
   `s3service.ignore-exceptions-in-multi` is set to true.

### KUDOS TO

 * Shlomo Swidler for numerous improvements to the project's technical
  documentation and code.
 * Keith Bonawitz for a patch to improve the handling of data re-transmissions
  using reset-able input streams.


------------------------
Version 0.6.0 - Feb 2008
------------------------

### COCKPIT

 * Added support for buckets located in Europe (EU location)
 * Generate signed GET URLs based on virtual host names.
 * Encryption algorithm can be changed in the preferences dialog.
 * Progress dialogs modified to fix display problems with JDK version 6.
 * Fixed bug causing encrypted files not to be automatically recognized
   and decrypted in some cases

### SYNCHRONIZE

 * Added --properties option that allows an explicit properties filename to
   be specified on the command line.
 * Added --nodelete option that keeps items on the destination that have been
   removed from the source. This is similar to --keepfiles except that files
   may still be reverted with the --nodelete option.
 * Added --noprogress option that prevents progress messages from being
   printed to the console, for cases where these messages pollute log files.
   This option is similar to --quiet, except the action report is printed.
 * Added the --acl option that allows Access Control List settings to be applied
   on the command line. Value must be one of: PRIVATE, PUBLIC_READ,
   PUBLIC_READ_WRITE
 * Added property setting to ignore missing source directories when uploading.
 * Progress status messages shortened to fit into 80 character width consoles.

### UPLOADER

 * Improved skinning capabilities.

### COCKPIT LITE

 * New Gatekeeper-compatible client application that allows almost all S3
   operations to be performed by a client via Gatekeeper-provided
   signed URLs.

### GATEKEEPER

 * Extended interfaces and default implementations to provide URL signing
   for all operations performed by the new Cockpit Lite application.
 * Improved support for web proxies.

### TOOLKIT

General:

 * S3Service class can now generate POST forms to allow web browsers to upload
   files or data directly to S3.
 * Added support for buckets located in the EU. Due to this change, the
   "s3service.end-point-host" property is now obsolete.
 * Added a default bucket location configuration setting in
   jets3t.properties: "s3service.default-bucket-location". If you do not
   use an explicit bucket location when you use the bucket creation API
   method, the bucket will be created in this default location.
 * Fixed bugs that caused uploads greater than 2GB to fail
   (Expect: 100-Continue is now supported)
 * Added the Bouncy Castle security provider to the suite, increasing the number
   of cipher algorithms available for encrypting files.
 * The method S3Service.listObjectsChunked now includes Common Prefixes in the
   result object returned.
 * Corrected the metadata name for the original date object metadata item, which
   was originally mis-typed as "jets3t-original-file-date-iso860". This name is
   now "jets3t-original-file-date-iso8601" (added "1" to the end). JetS3t
   tools remain compatible with the original metadata name.
 * The FileComparer class used by JetS3t applications can now be configured to
   use pre-generated <filename>.md5 files as a source for MD5 hash values,
   removing the need for hash values to be calculated for every synchronization
   operation - and potentially saving a great deal of time when you are
   synchronizing large files whose content changes rarely. This feature is
   controlled by a group of propery settings in jets3t.properties:
   filecomparer.use-md5-files, filecomparer.generate-md5-files,
   filecomparer.skip-upload-of-md5-files.
 * File listings as built using the FileComparer.buildFileMap() methods can be
   set not to include place-holder objects to represent empty directories
   (mimetype "application/x-directory") with the new configuration setting
   uploads.storeEmptyDirectories.

REST S3 Service:

 * Added support for configuring TCP window sizes. In jets3t.properties, the
   settings httpclient.socket-receive-buffer and httpclient.socket-send-buffer
   are applied to the Socket send and receive buffers used by the underlying
   HttpClient library.
 * The REST implementation can now automatically cope with RequestTimeTooSkewed
   errors caused by the client computer's clock disagreeing with S3 about the
   current time. If this happens, JetS3t will look up the time according to S3
   and compensate for the difference between the S3 clock and the client's clock.
 * Rudimentary upload bandwidth throttling using the jets3t.properties setting
   httpclient.read-throttle, which is specified in KB/s.
 * Proxy settings are now configurable via the jets3t.properties settings:
   httpclient.proxy-autodetect, httpclient.proxy-host, httpclient.proxy-port
 * Upgraded HTTPClient library to version 3.1

Multi-threaded Service:

 * Administration actions can have more threads assigned than upload/download
   actions with the new configuration setting s3service.admin-max-thread-count.
   These connections run much faster with more threads and as they are light
   weight they less likely to fail with higher thread counts.
 * Fixed S3ServiceSimpleMulti class to work with download sets larger than the
   number of available HTTP connections. Objects' data is now cached in temp
   files automatically.
 * The S3ServiceMulti@downloadObjects methods will restore the original last
   modified date of downloaded objects if the downloads.restoreLastModifiedDate
   configuration property is set to true. As these methods are used by the JetS3t
   applications, setting this property to true will allow file dates to be
   retained across synchronizations performed by Cockpit and Synchronize.
   The original last modified date must be available in the object's metadata
   item named "jets3t-original-file-date-iso8601".

### KUDOS TO

Alexis Agahi for a fix to BytesProgressWatcher.

Pradyumna Lenka for an example update to support the EU bucket location.

Andrea Barbieri of Moving Image Research for suggestions, feedback and quality
control.

### SPONSORS:

The JetS3t project has been generously supported with sponsorship from
Moving Image Research : http://www.movingimageresearch.com/


------------------------
Version 0.5.0 - Jan 2007
------------------------

### COCKPIT

 * Login credentials can now be stored in S3.
 * Added a dialog for configuring Bucket Logging.
 * Objects can be filtered by prefix and/or delimiter strings.
 * Third-party buckets can be added without first logging in.
 * User can cancel the listing of objects in large buckets.
 * Files being sent to s3 are only opened when necessary, removing the potential
   for too many files to be open at once (exceeding an Operating System
   imposed limit).
 * When uploading files, specific file/directory paths can be ignored
   using .jets3t-ignore settings files.
 * Access Control List permissions of uploaded files can be set to PRIVATE,
   PUBLIC_READ or PUBLIC_READ_WRITE

### SYNCHRONIZE

 * Progress status messages are displayed for long-running processes.
 * Files being sent to s3 are only opened when necessary, removing the potential
   for too many files to be open at once (exceeding an Operating System
   imposed limit).
 * When uploading files, specific file/directory paths can be ignored
   using .jets3t-ignore settings files.
 * Access Control List permissions of uploaded files can be set to PRIVATE,
   PUBLIC_READ or PUBLIC_READ_WRITE

### UPLOADER

A new applet/application to allow third parties without AWS accounts or
credentials to upload files to an S3 account. The Uploader provides a simple
wizard-based GUI allowing end-users to provide information, choose the
file(s) they will upload, and see upload progress.

The Uploader is highly configurable via the uploader.properties file, with
settings to configure: user input fields, explanatory text messages,
names/images/tooltips for buttons at each stage in the wizard, basic skinning
of the Uploader (an example HTML-like skin is included), and branding.

The Uploader is designed to work closely with a Gatekeeper server, which
provides the Uploader with signed URLs to allow it to perform uploads.

### GATEKEEPER

A new servlet that acts as a Gatekeeper server for S3 operations. The servlet
receives requests for S3 operations (GET, HEAD, PUT, DELETE) and responds to
these requests with either a signed URL allowing the operation, or a message
stating that the operation will not be allowed.

Basic decision-making functionality is included with the Gatekeeper Servlet,
however it is straight-forward to obtain more advanced control over the
Gatekeeper's behaviour by implementing the relevant Java interfaces. There
are specific interfaces for: allowing/denying requests, signing URLs, and
assigning unique transaction IDs for a request.

The Gatekeeper is designed to work closely with the Uploader application,
providing it with signed URLs so the Uploader can add items to an S3 account
without the end-user having any access to the AWS account credentials.

### TOOLKIT

General:

 * Properties for many aspects of jets3t behaviour can be set by the
   user in a properties file.
 * Support for getting/setting Server Access Logging for buckets.
 * Improved encryption mechanism, which now uses PBE-based encryption and
   allows users to set their preferred algorithm.
   NOTE: All changes *should* be backwards compatible, but due to these
   changes there may be an increased risk of data loss for encrypted items.
 * New methods to chunk bucket listings, allowing for better handling of
   buckets with many objects.
 * A limit to the maximum number of simultaneous communication threads
   (ie those interacting with S3) is now imposed. This limit can be set in
   the properties file, and defaults to 50.
 * Signed URLs can be generated for GET, HEAD, PUT and DELETE requests
 * Fixed bug where object keys/names with special characters were not
   correctly encoded.
 * DNS caching is limited to 300 seconds.
 * When an object's data comes from a file, the file can be opened only when
   necessary rather than being opened as soon as the object is created.
 * Added documentation for advanced options settings and logging

REST/HTTP implementation:

 * Requests that fail due to S3 Internal Server error are retried a
   configurable number of times, with an increasing delay between each
   retry attempt.
 * The REST/HTTP implementation is now less fussy about object key names,
   and will allow unusual names such as full URL strings etc.
 * Can detect (in some circumstances) a browser's proxy settings when run
   inside an applet context, and allows for callbacks to a credentials
   provider object when authentication is required (eg for proxies
   requiring username/password)
 * Signed URLs can be used to perform GET, HEAD, PUT and DELETE operations
   without the need for knowledge of AWS credentials.
 * Added a utility method putObjectWithSignedUrl to upload objects to S3
   using only a signed PUT URL (ie no AWS credentials are required).
 * Configurable user agent string.
 * Sends an upload object's MD5 data hash to S3 in the header Content-MD5,
   to confirm no data corruption has taken place on the wire.

SOAP implementation:

 * Tests for data corruption on the wire by matching ETag returned by S3 with
   expected MD5 hash value.

Multi-threaded Service:

 * Signed URLs can be used to perform GET, HEAD, PUT and DELETE operations of
   multiple items at a time, without the need for knowledge of AWS credentials.

### KNOWN ISSUES

General:

 * Uploading or downloading multiple large files can result in connection
   errors if the network connection is flooded. The chances of such errors
   can be reduced by using low values (eg 2) for for the jets3t.properties
   settings s3service.max-thread-count and httpclient.max-connections.

Cockpit:

 * Copy & paste and Drag & drop doesn't work on some versions of Linux,
   making it difficult to enter AWS credentials.

### SPONSORS

The JetS3t project has been generously supported with sponsorship from the
following organisations.

* Moving Image Research : http://www.movingimageresearch.com/

   Moving Image Research (MIR) is a technology company with deep roots in
   media science offering software, network services, and consulting.

### CONTRIBUTORS

Thankyou to the following contributors, who helped make this release possible:

 * Moving Image Research, Andrea Barbieri (http://www.movingimageresearch.com/)
 * Angel Vera (gunfus)

-------------------------
Version 0.4.0 - Sept 2006
-------------------------
Initial public release.
