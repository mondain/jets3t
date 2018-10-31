/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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


import org.jets3t.service.utils.oauth.OAuthScope;
import org.jets3t.service.utils.oauth.OAuthUtils;

import java.io.IOException;

/**
 * Class to contain OAuth2 client credentials for authenticating against an
 * OAuth end-point, as opposed to authenticating directly with a storage service.
 * <p>
 * Instead of the typical user access and client keys stored within a {@link ProviderCredentials}
 * class, this class stores an OAuth2 Client ID (as the access key) and Client Secret (as the
 * secret key).
 *
 * @author jmurty
 */
public class OAuth2Credentials extends ProviderCredentials {

    private OAuth2Tokens oauth2Tokens;
    private OAuthUtils oauthUtils;

    /**
     * Construct credentials.
     *
     * @param clientId     Client ID to identify the application to an OAuth2 end-point.
     * @param clientSecret Client Secret for the application to authenticate against an OAuth2 end-point.
     */
    public OAuth2Credentials(String clientId, String clientSecret) {
        this(clientId, clientSecret, null);
    }

    /**
     * Construct credentials, and associate them with a human-friendly name.
     *
     * @param clientId     Client ID to identify the application to an OAuth2 end-point.
     * @param clientSecret Client Secret for the application to authenticate against an OAuth2 end-point.
     * @param friendlyName a name identifying the owner of the credentials, such as 'James'.
     */
    public OAuth2Credentials(String clientId, String clientSecret, String friendlyName) {
        this(clientId, clientSecret, friendlyName, null);
    }

    /**
     * Construct credentials, and associate them with a human-friendly name. Start with
     * a valid refresh token
     *
     * @param clientId     Client ID to identify the application to an OAuth2 end-point.
     * @param clientSecret Client Secret for the application to authenticate against an OAuth2 end-point.
     * @param friendlyName a name identifying the owner of the credentials, such as 'James'.
     * @param refreshToken a refresh token that was obtained previously.
     */
    public OAuth2Credentials(String clientId, String clientSecret, String friendlyName, String refreshToken) {
        this(new OAuthUtils(OAuthUtils.OAuthImplementation.GOOGLE_STORAGE_OAUTH2_10, clientId, clientSecret),
                friendlyName, refreshToken);
    }

    /**
     * @param oauth Implementation
     * @param friendlyName a name identifying the owner of the credentials, such as 'James'.
     */
    public OAuth2Credentials(OAuthUtils oauth, String friendlyName) {
        this(oauth, friendlyName, null);
    }

    /**
     * @param oauth Implementation
     * @param friendlyName a name identifying the owner of the credentials, such as 'James'.
     * @param refreshToken a refresh token that was obtained previously.
     */
    public OAuth2Credentials(OAuthUtils oauth, String friendlyName, String refreshToken) {
        super(oauth.getClientId(), oauth.getClientSecret(), friendlyName);
        // If service initialized with OAuth2 credentials, init utility class for handling OAuth
        this.oauthUtils = oauth;
        if (refreshToken == null) {
            this.oauth2Tokens = null;
        }
        else {
            this.oauth2Tokens = new OAuth2Tokens(null, refreshToken);
        }
    }

    public void setOAuth2Tokens(OAuth2Tokens tokens) {
        this.oauth2Tokens = tokens;
    }

    public OAuth2Tokens getOAuth2Tokens() throws IOException {
        if(null == this.oauth2Tokens) {
            return null;
        }
        if (this.oauth2Tokens.isAccessTokenExpired()) {
            this.oauth2Tokens = this.refreshOAuth2Tokens();
        }
        return this.oauth2Tokens;
    }

    private OAuth2Tokens refreshOAuth2Tokens() throws IOException {
        log.debug("Refreshing OAuth2 access token using refresh token: "
                + this.oauth2Tokens.getRefreshToken());

        OAuth2Tokens newTokens = this.oauthUtils.refreshOAuth2AccessToken(this.oauth2Tokens);
        this.setOAuth2Tokens(newTokens);
        log.debug("Refreshed OAuth2 access token to " + newTokens.getAccessToken()
                + " with expiry at " + newTokens.getExpiry());
        return newTokens;
    }

    public String generateBrowserUrlToAuthorizeNativeApplication(OAuthScope scope) {
        return this.oauthUtils.generateBrowserUrlToAuthorizeNativeApplication(scope);
    }

    public void retrieveOAuth2TokensFromAuthorization(final String authorizationCode)
            throws IOException
    {
        this.oauth2Tokens = this.oauthUtils.retrieveOAuth2TokensFromAuthorization(authorizationCode);
    }

    /**
     * @return the OAuth2 Client ID (stored as access key)
     */
    public String getClientId() {
        return this.getAccessKey();
    }

    /**
     * @return the OAuth2 Client Secret (stored as secret key)
     */
    public String getClientSecret() {
        return this.getSecretKey();
    }

    /**
     * @return string representing this credential type's name (for serialization)
     */
    @Override
    protected String getTypeName() {
        return "OAuth2Client";
    }

    @Override
    public String getVersionPrefix() {
        return "jets3t OAuth2 Client Credentials, version: ";
    }
}
