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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.servlets.gatekeeper.Authorizer;
import org.jets3t.servlets.gatekeeper.ClientInformation;

/**
 * Default Authorizer implementation that allows all signature requests.
 *
 * @author James Murty
 */
public class DefaultAuthorizer extends Authorizer {

    /**
     * Constructs the Authorizer - no configuration parameters are required.
     *
     * @param servletConfig
     * @throws ServletException
     */
    public DefaultAuthorizer(ServletConfig servletConfig) throws ServletException {
        super(servletConfig);
    }

    /**
     * Returns true in all cases.
     */
    public boolean allowSignatureRequest(GatekeeperMessage requestMessage,
        ClientInformation clientInformation, SignatureRequest signatureRequest)
    {
        return true;
    }

    /**
     * Returns true in all cases.
     */
    public boolean allowBucketListingRequest(
            GatekeeperMessage requestMessage, ClientInformation clientInformation)
    {
        return true;
    }

}
