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
package org.jets3t.servlets.gatekeeper;

import java.util.Calendar;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.S3ServiceException;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;

/**
 * Provides signed URLs that will allow a client to perform the operation requested on a specific
 * object in S3.
 * <p>
 * This <tt>sign</tt> methods in this class are not called for a signature request unless that
 * request has already been allowed by the {@link Authorizer}.
 * <p>
 * Implementations of this class need only generate the appropriate signed URL. However, more
 * advanced implementations may do other work such as renaming objects to comply with naming
 * rules for an S3 account.
 *
 * @author James Murty
 */
public abstract class UrlSigner {

    /**
     * Constructs a UrlSigner with the following required properties from the servlet configuration:
     *
     * @param servletConfig
     * @throws ServletException
     */
    public UrlSigner(ServletConfig servletConfig) throws ServletException {
    }

    /**
     * Generate a signed GET URL for the signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signGet(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;

    /**
     * Generate a signed HEAD URL for the signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signHead(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;

    /**
     * Generate a signed PUT URL for the signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signPut(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;

    /**
     * Generate a signed DELETE URL for the signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signDelete(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;

    /**
     * Generate a signed GET URL for an ACL-based signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signGetAcl(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;

    /**
     * Generate a signed PUT URL for an ACL-based signature request.
     *
     * @param requestMessage
     * the request message received from the client.
     * @param clientInformation
     * information about the client's end-point, and any Session or Principal associated with the client.
     * @param signatureRequest
     * a pre-approved signature request.
     *
     * @return
     * a signed URL string that will allow the operation specified in the signature request
     * on the object specified in the signature request.
     *
     * @throws S3ServiceException
     */
    public abstract String signPutAcl(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
        throws S3ServiceException;


    /**
     * @return
     * the date and time when signed URLs should expire, calculated by adding the number of seconds
     * until expiry to the current time.
     */
    protected Date calculateExpiryTime(int secondsUntilExpiry) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.SECOND, secondsUntilExpiry);
        return cal.getTime();
    }

}
