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
package org.jets3t.servlets.gatekeeper.impl;

import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.servlets.gatekeeper.ClientInformation;
import org.jets3t.servlets.gatekeeper.TransactionIdProvider;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.util.UUID;

/**
 * Default TransactionIdProvider implementation that generated random-based UUIDs using the
 * java.util.UUID Generator.
 *
 * @author James Murty
 */
public class DefaultTransactionIdProvider extends TransactionIdProvider {

    /**
     * Constructs the TransactionIdProvider - no configuration parameters are required.
     *
     * @param servletConfig
     * @throws ServletException
     */
    public DefaultTransactionIdProvider(ServletConfig servletConfig) throws ServletException {
        super(servletConfig);
    }

    /**
     * Returns a random-based UUID.
     */
    @Override
    public String getTransactionId(GatekeeperMessage requestMessage, ClientInformation clientInformation) {
        // Generate a UUID based on a random generation.
        return UUID.randomUUID().toString();
    }

}
