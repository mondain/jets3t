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
package org.jets3t.servlets.gatekeeper.impl;

import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.Constants;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.servlets.gatekeeper.ClientInformation;
import org.jets3t.servlets.gatekeeper.UrlSigner;

/**
 * Default UrlSigner implementation that signs all requests, putting all objects in a specific S3
 * bucket and limiting the signature time to a configurable time period.
 * <p>
 * This implementation also demonstrates how objects may be modified, as it adds a metadata
 * item to each signed object to store the transaction ID in which the object was signed. The
 * transaction id is stored in the metadata name <tt>x-amx-gatekeeper-transaction-id</tt>
 *
 * @author James Murty
 */
public class DefaultUrlSigner extends UrlSigner {
    protected ProviderCredentials credentials = null;
    protected S3Service s3Service = null;
    protected String s3BucketName = null;
    protected int secondsUntilExpiry = 0;

    public static final String TRANSACTION_ID_METADATA_NAME =
        Constants.REST_HEADER_PREFIX + "gatekeeper-transaction-id";

    /**
     * Constructs the UrlSigner with the required parameters.
     * <p>
     * The required parameters that must be available in the servlet configuration are:
     * <ul>
     * <li><tt>AwsAccessKey</tt>: The AWS Access Key for an S3 account</li>
     * <li><tt>AwsSecretKey</tt>: The AWS Secret Key for an S3 account</li>
     * <li><tt>S3BucketName</tt>: The bucket all objects are stored in (regardless of what bucket
     * name the client provided).</li>
     * <li><tt>SecondsToSign</tt>: How many seconds until the signed URLs will expire<br>
     * <b>Note</b>: this setting must allow enough time for the operation to <b>complete</b>
     * before the expiry time is reached. For example, if uploads are expected over slow
     * connections the expiry time must be long enough for the uploads to finish otherwise the
     * uploaded file will be rejected <b>after</b> it has finished uploading.</li>
     * </ul>
     *
     * @param servletConfig
     * @throws ServletException
     */
    public DefaultUrlSigner(ServletConfig servletConfig) throws ServletException {
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
        this.s3Service = new RestS3Service(credentials);

        String secondsToSign = servletConfig.getInitParameter("SecondsToSign");
        if (secondsToSign == null || secondsToSign.length() == 0) {
            throw new ServletException("Missing required servlet init parameters for DefaultUrlSigner: "
                + "SecondsToSign");
        }
        try {
            secondsUntilExpiry = Integer.parseInt(secondsToSign);
        } catch (NumberFormatException e) {
            throw new ServletException("Invalid servlet init param: SecondsToSign", e);
        }

        s3BucketName = servletConfig.getInitParameter("S3BucketName");
        if (s3BucketName == null || s3BucketName.length() == 0) {
            throw new ServletException("Missing required servlet init parameters for DefaultUrlSigner: "
                + "S3BucketName");
        }
    }

    /**
     * Adds a metadata item containing the transaction ID to each object.
     *
     * @param signatureRequest
     * @param messageProperties
     * @throws S3ServiceException
     */
    protected void updateObject(SignatureRequest signatureRequest, Properties messageProperties)
        throws S3ServiceException
    {
        signatureRequest.setBucketName(s3BucketName);

        Map objectMetadata = signatureRequest.getObjectMetadata();
        if (!objectMetadata.containsKey(TRANSACTION_ID_METADATA_NAME)) {
            String transactionId =
                messageProperties.getProperty(TRANSACTION_ID_METADATA_NAME);
            if (transactionId != null) {
                objectMetadata.put(TRANSACTION_ID_METADATA_NAME, transactionId);
            }
        }
    }

    public String signDelete(GatekeeperMessage requestMessage,
        ClientInformation clientInformation,SignatureRequest signatureRequest) throws S3ServiceException
    {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        return s3Service.createSignedDeleteUrl(signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), calculateExpiryTime(secondsUntilExpiry), false);
    }

    public String signGet(GatekeeperMessage requestMessage,
        ClientInformation clientInformation,SignatureRequest signatureRequest) throws S3ServiceException
    {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        return s3Service.createSignedGetUrl(signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), calculateExpiryTime(secondsUntilExpiry), false);
    }

    public String signHead(GatekeeperMessage requestMessage,
        ClientInformation clientInformation,SignatureRequest signatureRequest) throws S3ServiceException
    {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        return s3Service.createSignedHeadUrl(signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), calculateExpiryTime(secondsUntilExpiry), false);
    }

    public String signPut(GatekeeperMessage requestMessage,
        ClientInformation clientInformation,SignatureRequest signatureRequest) throws S3ServiceException
    {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        return s3Service.createSignedPutUrl(signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), signatureRequest.getObjectMetadata(),
            calculateExpiryTime(secondsUntilExpiry), false);
    }

    public String signGetAcl(GatekeeperMessage requestMessage, ClientInformation clientInformation, SignatureRequest signatureRequest) throws S3ServiceException {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        long secondsSinceEpoch = calculateExpiryTime(secondsUntilExpiry).getTime() / 1000;

        return s3Service.createSignedUrl("GET", signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), "acl", null, secondsSinceEpoch, false);
    }

    public String signPutAcl(GatekeeperMessage requestMessage, ClientInformation clientInformation, SignatureRequest signatureRequest) throws S3ServiceException {
        updateObject(signatureRequest, requestMessage.getMessageProperties());
        long secondsSinceEpoch = calculateExpiryTime(secondsUntilExpiry).getTime() / 1000;

        return s3Service.createSignedUrl("PUT", signatureRequest.getBucketName(),
            signatureRequest.getObjectKey(), "acl", signatureRequest.getObjectMetadata(),
            secondsSinceEpoch, false);
    }

}
