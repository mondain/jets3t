/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2012 James Murty
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
package org.jets3t.service.security;

/**
 * Class to contain the temporary (session-based) Amazon Web Services (AWS) credentials of a user.
 *
 * @author James Murty
 */
public class AWSSessionCredentials extends AWSCredentials {

    protected String sessionToken = null;

    /**
     * Construct credentials, and associate them with a human-friendly name.
     *
     * @param awsAccessKey
     * AWS access key for an Amazon S3 account.
     * @param awsSecretAccessKey
     * AWS secret key for an Amazon S3 account.
     * @param sessionToken
     * AWS session token for temporary/session-based account credentials.
     * @param friendlyName
     * a name identifying the owner of the credentials, such as 'James'.
     */
    public AWSSessionCredentials(
        String awsAccessKey, String awsSecretAccessKey, String sessionToken, String friendlyName)
    {
        super(awsAccessKey, awsSecretAccessKey, friendlyName);
        this.sessionToken = sessionToken;
    }

    /**
     * Construct credentials, without a human-friendly name.
     *
     * @param awsAccessKey
     * AWS access key for an Amazon S3 account.
     * @param awsSecretAccessKey
     * AWS secret key for an Amazon S3 account.
     * @param sessionToken
     * AWS session token for temporary/session-based account credentials.
     */
    public AWSSessionCredentials(
        String awsAccessKey, String awsSecretAccessKey, String sessionToken)
    {
        this(awsAccessKey, awsSecretAccessKey, sessionToken, null);
    }

    /**
     * @return
     * The AWS session token for temporary/session-based account credentials.
     */
    public String getSessionToken() {
        return this.sessionToken;
    }

    @Override
    protected String getTypeName() {
        return "session";
    }

}
