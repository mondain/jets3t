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
package org.jets3t.servlets.gatekeeper;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;

/**
 * Provides a listing of objects in an S3 account to a client application that cannot query
 * the account for itself.
 * <p>
 * The object listing may contain all the objects in the bucket, or any subset as is
 * appropriate for the client application or user in question.
 *
 * @author James Murty
 */
public abstract class BucketLister {

    public BucketLister(ServletConfig servletConfig) throws ServletException {
    }

    /**
     * Create in the GatekeeperMessage object a list of SignatureRequest objects capturing
     * details about the S3 objects contained in a bucket. The client application will
     * interpret the SignatureRequest object details to reconstruct the objects.
     *
     * @param gatekeeperMessage
     * the message object that was received, and in which the object listing to be returned
     * is stored.
     * @param clientInformation
     * information about the client end-point this request was received from.
     *
     * @throws Exception
     */
    public abstract void listObjects(GatekeeperMessage gatekeeperMessage,
            ClientInformation clientInformation) throws Exception;

}
