/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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
 * Provides a transaction ID that uniquely identifies a Gatekeeper transaction - that is, a request
 * and response interaction.
 * <p>
 * A transaction ID could be based on a user's session ID (available in the client information),
 * come from a database sequence, or any other mechanism that is likely to generate unique IDs.
 *
 * @author James Murty
 */
public abstract class TransactionIdProvider {

    /**
     * Constructs a TransactionIdProvider.
     *
     * @param servletConfig
     * @throws ServletException
     */
    public TransactionIdProvider(ServletConfig servletConfig) throws ServletException {
    }

    /**
     * Returns a transaction ID to uniquely identify the Gatekeeper transaction - if transaction
     * tracking is not required this method can return an empty string.
     *
     * @param requestMessage
     * @param clientInformation
     *
     * @return
     * an ID unique to this transaction.
     */
    public abstract String getTransactionId(GatekeeperMessage requestMessage,
        ClientInformation clientInformation);

}
