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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;

/**
 * UrlSigner implementation that extends the DefaultUrlSigner class to perform some additional
 * work - speficically this class renames the S3 objects keys based on the transaction ID.
 *
 * @author James Murty
 */
public class RenameToUuidUrlSigner extends DefaultUrlSigner {
    private final static Log log = LogFactory.getLog(RenameToUuidUrlSigner.class);

    private String lastTransactionId = "";
    private int countOfRenamedObjects = 0;

    /**
     * Constructs the UrlSigner with the required parameters.
     * <p>
     * The required parameters that must be available in the servlet configuration are:
     * <ul>
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
    public RenameToUuidUrlSigner(ServletConfig servletConfig) throws ServletException {
        super(servletConfig);
    }

    /**
     * Overrides the implementation in DefaultUrlProvider to do everything that class does, but
     * also to rename objects based on the transaction ID.
     * <p>
     * Each object is renamed to the following format:<br>
     * <tt><i>transactionId</i>.<i>objectCount</i>.<i>objectExtension</i></tt>
     * <p>
     * Objects that arrive with the metadata property
     * {@link GatekeeperMessage#SUMMARY_DOCUMENT_METADATA_FLAG} as treated as special cases, as this
     * flag indicates that the object is an XML summary document provided by the JetS3t
     * {@link org.jets3t.apps.uploader.Uploader} application. In this case, the object should not
     * be renamed as it is already named according to the last transaction ID.
     * </p>
     *
     * @param signatureRequest
     * @param messageProperties
     * @throws S3ServiceException
     */
    @Override
    protected void updateObject(SignatureRequest signatureRequest, Properties messageProperties)
        throws S3ServiceException
    {
        super.updateObject(signatureRequest, messageProperties);

        String transactionId = messageProperties.getProperty(GatekeeperMessage.PROPERTY_TRANSACTION_ID);

        Map objectMetadata = signatureRequest.getObjectMetadata();

        if (transactionId != null) {
            if (lastTransactionId.equals(transactionId)) {
                // No need to reset the object count, we are still working on the same transaction ID.
            } else {
                // This is a new transaction ID, reset the object count.
                countOfRenamedObjects = 0;
                lastTransactionId = transactionId;
            }

            String originalKey = signatureRequest.getObjectKey();

            if (objectMetadata.containsKey(GatekeeperMessage.SUMMARY_DOCUMENT_METADATA_FLAG)) {
                if (log.isDebugEnabled()) {
                    log.debug("Object with key '" + originalKey + "' is flagged as a Summary Document"
                        + ", and will not be renamed");
                }
            } else {
                String extension = null;
                int lastDotIndex = originalKey.lastIndexOf(".");
                if (lastDotIndex >= 0) {
                    extension = originalKey.substring(lastDotIndex + 1);
                }

                String newKey = transactionId + "." + (++countOfRenamedObjects)
                    + (extension != null? "." + extension : "");
                if (log.isDebugEnabled()) {
                    log.debug("Renamed object key '" + originalKey + "' to '" + newKey + "'");
                }
                signatureRequest.setObjectKey(newKey);
            }
        }
    }

}
