/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007 James Murty
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
package org.jets3t.servlets.gatekeeper.impl;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.servlets.gatekeeper.BucketLister;
import org.jets3t.servlets.gatekeeper.ClientInformation;

/**
 * Default BucketLister implementation that lists all objects in the configured bucket.
 *
 * @author James Murty
 */
public class DefaultBucketLister extends BucketLister {

    protected ProviderCredentials credentials = null;
    private String s3BucketName = null;

    /**
     * Constructs the Bucket lister with the required parameters.
     * <p>
     * The required parameters that must be available in the servlet configuration are:
     * <ul>
     * <li><tt>AwsAccessKey</tt>: The AWS Access Key for an S3 account</li>
     * <li><tt>AwsSecretKey</tt>: The AWS Secret Key for an S3 account</li>
     * <li><tt>S3BucketName</tt>: The bucket all objects are stored in (regardless of what bucket
     * name the client provided).</li>
     * </ul>
     *
     * @param servletConfig
     * @throws ServletException
     */
    public DefaultBucketLister(ServletConfig servletConfig) throws ServletException {
        super(servletConfig);

        String awsAccessKey = servletConfig.getInitParameter("AwsAccessKey");
        String awsSecretKey = servletConfig.getInitParameter("AwsSecretKey");

        // Fail with an exception if required init params are missing.
        boolean missingInitParam = false;
        String errorMessage = "Missing required servlet init parameters for UrlSigner: ";
        if (awsAccessKey == null || awsAccessKey.length() == 0) {
            errorMessage += "AwsAccessKey ";
            missingInitParam = true;
        }
        if (awsSecretKey == null || awsSecretKey.length() == 0) {
            errorMessage += "AwsSecretKey ";
            missingInitParam = true;
        }
        if (missingInitParam) {
            throw new ServletException(errorMessage);
        }

        this.credentials = new AWSCredentials(awsAccessKey, awsSecretKey);

        s3BucketName = servletConfig.getInitParameter("S3BucketName");
        if (s3BucketName == null || s3BucketName.length() == 0) {
            throw new ServletException("Missing required servlet init parameters for DefaultBucketLister: "
                + "S3BucketName");
        }
    }

    public void listObjects(GatekeeperMessage gatekeeperMessage,
            ClientInformation clientInformation) throws S3ServiceException
    {
        // Build prefix based on user's path and any additional prefix provided.
        String prefix = null;
        if (gatekeeperMessage.getApplicationProperties().containsKey("Prefix")) {
            prefix = gatekeeperMessage.getApplicationProperties().getProperty("Prefix");
        }

        // Construct an authorized service.
        RestS3Service service = new RestS3Service(credentials);

        // List objects in the configured bucket.
        S3Object[] objects = service.listObjects(s3BucketName, prefix, null, 1000);

        // Package object information in SignatureRequest objects. This data will be
        // automatically encoded and sent across the wire back to the client.
        for (int i = 0; i < objects.length; i++) {
            SignatureRequest sr = new SignatureRequest();
            sr.setObjectMetadata(objects[i].getMetadataMap());
            sr.addObjectMetadata(S3Object.METADATA_HEADER_LAST_MODIFIED_DATE,
                ServiceUtils.formatIso8601Date(objects[i].getLastModifiedDate()));
            sr.setObjectKey(objects[i].getKey());
            gatekeeperMessage.addSignatureRequest(sr);
        }

        gatekeeperMessage.addApplicationProperty("AccountDescription",
            "<html>Bucket: <b>" + s3BucketName + "</b></html>");

        // Include an application property to inform Cockpit Lite of the user's bucket
        gatekeeperMessage.addApplicationProperty("S3BucketName", s3BucketName);

        gatekeeperMessage.addApplicationProperty("UserCanUpload", "true");
        gatekeeperMessage.addApplicationProperty("UserCanDownload", "true");
        gatekeeperMessage.addApplicationProperty("UserCanDelete", "true");
        gatekeeperMessage.addApplicationProperty("UserCanACL", "true");
    }

}
