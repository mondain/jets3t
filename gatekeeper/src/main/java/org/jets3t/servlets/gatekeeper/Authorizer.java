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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;

/**
 * Authorizes or refuses operations on an S3 account - the decision can be based on information
 * in the request message, details about the client, or the signature request itself.
 * <p>
 * An example Authorizer implementation might check that a user has alread been authorized and
 * their Principal details are available from the client information, or it might check that the
 * correct password has been provided in an application property in the request message.
 *
 * @author James Murty
 */
public abstract class Authorizer {

    /**
     * Constructs an Authorizer.
     *
     * @param servletConfig
     * @throws ServletException
     */
    public Authorizer(ServletConfig servletConfig) throws ServletException {
    }

    /**
     * Authorizes an operation represented by a signature request by returning true, or
     * disallows the operation by returned false (and optionally setting a decline reason
     * in the signature request).
     * <p>
     * Authorization decisions can be made based on any of the inputs provided to this method.
     *
     * @param requestMessage
     * the Gatekeeper request message.
     * @param clientInformation
     * information about the client end-point this request was received from.
     * @param signatureRequest
     * a signature request to allow or disallow.
     *
     * @return
     * true if the request is allowed, false otherwise.
     */
    public abstract boolean allowSignatureRequest(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest);

    /**
     * Authorizes a bucket listing operation for a client as represented by a the gatekeeper
     * request message.
     * <p>
     * Authorization decisions can be made based on any of the inputs provided to this method.
     *
     * @param requestMessage
     * the Gatekeeper request message.
     * @param clientInformation
     * information about the client end-point this request was received from.
     *
     * @return
     * true if the request is allowed, false otherwise.
     */
    public abstract boolean allowBucketListingRequest(GatekeeperMessage requestMessage,
            ClientInformation clientInformation);

}
