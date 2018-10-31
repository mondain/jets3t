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
package org.jets3t.service.utils.oauth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.security.OAuth2Tokens;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.oauth.OAuthConstants.GSOAuth2_10;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilties for obtaining OAuth authentication tokens.
 *
 * Implementation is currently specific to the Google Storage OAuth 2.0 implementation,
 * though hopefully generic enough it may be extensible in the future.
 *
 * @author jmurty
 * @see <a href="http://code.google.com/apis/storage/docs/authentication.html#oauth">Google Storage: OAuth 2.0 Authentication</a>
 * @see <a href="http://code.google.com/apis/accounts/docs/OAuth2.html">Using OAuth 2.0 to Access Google APIs</a>
 */
public class OAuthUtils {
    private static final Log log = LogFactory.getLog(OAuthUtils.class);

    protected static final String HTTP_USER_AGENT = "OAuthUtils/" + Constants.JETS3T_VERSION;

    /**
     * Which OAuth implementation to target.
     */
    public enum OAuthImplementation {
        /**
         * Google Storage OAuth 2.0 (release 10)
         */
        GOOGLE_STORAGE_OAUTH2_10
    }

    protected HttpClient httpClient = null;
    protected ObjectMapper jsonMapper = new ObjectMapper();

    protected OAuthImplementation implementation = null;
    protected String clientId = null;
    protected String clientSecret = null;

    /**
     * Create utility class for a given OAuth implementation that will use the given
     * client ID and Secret. Values in the given {@link Jets3tProperties} object are
     * used to configure HTTP/S connections that may be performed by this class.
     *
     * @param implementation   OAuth implementation version
     * @param clientId         Client ID for installed application
     * @param clientSecret     Client secret for installed applications
     * @param jets3tProperties Properties to configure HTTP/S connections
     */
    public OAuthUtils(OAuthImplementation implementation, String clientId, String clientSecret,
                      Jets3tProperties jets3tProperties) {
        this(RestUtils.initHttpConnection(
                null, // requestAuthorizer
                jets3tProperties,
                HTTP_USER_AGENT,
                null), implementation, clientId, clientSecret);
    }

    /**
     * Create utility class for a given OAuth implementation that will use the given
     * client ID and Secret. Values in the given {@link Jets3tProperties} object are
     * used to configure HTTP/S connections that may be performed by this class.
     *
     * @param httpClient     HTTP Client
     * @param implementation OAuth implementation version
     * @param clientId       Client ID for installed application
     * @param clientSecret   Client secret for installed applications
     */
    public OAuthUtils(HttpClient httpClient, OAuthImplementation implementation, String clientId, String clientSecret) {
        this.implementation = implementation;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = httpClient;

        if(this.implementation == null
                || this.clientId == null
                || this.clientSecret == null
                || this.httpClient == null) {
            throw new IllegalArgumentException(
                    "Null arguments not permitted when constructing " + this.getClass().getName());
        }
    }

    /**
     * Create utility class for a given OAuth implementation that will use the given
     * client ID and Secret. Values in the default system {@link Jets3tProperties} object
     * are used to configure HTTP/S connections that may be performed by this class.
     *
     * @param implementation OAuth implementation version
     * @param clientId       Client ID for installed application
     * @param clientSecret   Client secret for installed applications
     */
    public OAuthUtils(OAuthImplementation implementation, String clientId, String clientSecret) {
        this(implementation, clientId, clientSecret,
                Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * Generate the URL for an OAuth authorization end-point that a person can visit in a
     * web browser to authorize access to a storage resource, where access is limited to
     * the given scope. The URL will contain the Client ID stored in this class, along with
     * other information that may be specific to the OAuth implementation.
     *
     * @param scope URI representing the access scope a user will be prompted to authorize, for example
     *              example <pre>OAuthConstants.GSOAuth2_10.Scopes.ReadOnly</pre>
     * @return URL to an OAuth authorization end-point.
     * @see <a href="http://code.google.com/apis/accounts/docs/OAuth2.html#IA">OAuth 2.0 for native applications</a>
     */
    public String generateBrowserUrlToAuthorizeNativeApplication(OAuthScope scope) {
        if(this.implementation == OAuthImplementation.GOOGLE_STORAGE_OAUTH2_10) {
            String url = GSOAuth2_10.Endpoints.Authorization
                    + "?response_type=" + GSOAuth2_10.ResponseTypes.Code
                    + "&redirect_uri=" + GSOAuth2_10.NATIVE_APPLICATION_REDIRECT_URI
                    + "&client_id=" + this.clientId
                    + "&scope=" + scope;
            log.debug("Generated authorization URL for OAuth implementation "
                    + this.implementation + ": " + url);
            return url;
        }
        else {
            throw new IllegalStateException("Unsupported implementation: " + this.implementation);
        }
    }

    /**
     * Swap the given authorization token for access/refresh tokens (and optional expiry time)
     * from an OAuth token endpoint.
     *
     * @param authorizationCode token representing a pre-approved authorization (e.g. as might be generated by a user who
     *                          visits the {@link #generateBrowserUrlToAuthorizeNativeApplication(OAuthScope)} URL)
     * @return object representing OAuth token and expiry data.
     * @throws IOException Error receiving tokens
     */
    @SuppressWarnings("serial")
    public OAuth2Tokens retrieveOAuth2TokensFromAuthorization(
            final String authorizationCode) throws IOException {
        log.debug("Retrieving OAuth2 tokens using implementation " + implementation
                + " with authorization code: " + authorizationCode);
        Map<String, Object> responseData;

        if(this.implementation == OAuthImplementation.GOOGLE_STORAGE_OAUTH2_10) {
            responseData = this.performPostRequestAndParseJSONResponse(
                    GSOAuth2_10.Endpoints.Token,
                    new ArrayList<NameValuePair>() {{
                        add(new BasicNameValuePair("client_id", clientId));
                        add(new BasicNameValuePair("client_secret", clientSecret));
                        add(new BasicNameValuePair("code", authorizationCode));
                        add(new BasicNameValuePair("grant_type", GSOAuth2_10.GrantTypes.Authorization));
                        add(new BasicNameValuePair("redirect_uri", GSOAuth2_10.NATIVE_APPLICATION_REDIRECT_URI));
                    }});
            log.debug("Retrieved authorization data from OAuth2 token endpoint "
                    + GSOAuth2_10.Endpoints.Token + ": " + responseData);

            // Pass on error message in response data
            String error = (String) responseData.get("error");
            if(error != null) {
                throw new IOException("OAuth2 authentication-to-tokens error: " + error);
            }

            // Retrieve tokens and expiry data from response
            String accessToken = (String) responseData.get("access_token");
            String refreshToken = (String) responseData.get("refresh_token");
            Number expiresIn = (Number) responseData.get("expires_in");
            String tokenType = (String) responseData.get("token_type");

            // Sanity-check response data
            if(!"Bearer".equals(tokenType)) {
                throw new IOException("OAuth2 authentication-to-tokens error, invalid token type in data: "
                        + responseData);
            }
            if(accessToken == null || refreshToken == null) {
                throw new IOException("OAuth2 authentication-to-tokens error, missing token(s) in data: "
                        + responseData);
            }

            return new OAuth2Tokens(
                    accessToken, refreshToken,
                    OAuth2Tokens.calculateExpiry(expiresIn));
        }
        else {
            throw new IllegalStateException("Unsupported implementation: " + this.implementation);
        }
    }

    /**
     * Retrieve and return a refreshed access token from an OAuth2 token end-point using the
     * refresh token in the provided tokens object.
     *
     * @param tokens OAuth token data that must include a valid refresh token.
     * @return a new object containing the refreshed access token, an updated expiry timestamp
     *         (if applicable) and the original refresh token.
     * @throws IOException Invalid response data
     */
    @SuppressWarnings("serial")
    public OAuth2Tokens refreshOAuth2AccessToken(final OAuth2Tokens tokens) throws IOException {
        log.debug("Refreshing OAuth2 access token using implementation " + implementation
                + " with refresh token: " + tokens.getRefreshToken());
        Map<String, Object> responseData = null;

        if(this.implementation == OAuthImplementation.GOOGLE_STORAGE_OAUTH2_10) {
            responseData = this.performPostRequestAndParseJSONResponse(
                    GSOAuth2_10.Endpoints.Token,
                    new ArrayList<NameValuePair>() {{
                        add(new BasicNameValuePair("client_id", clientId));
                        add(new BasicNameValuePair("client_secret", clientSecret));
                        add(new BasicNameValuePair("refresh_token", tokens.getRefreshToken()));
                        add(new BasicNameValuePair("grant_type", GSOAuth2_10.GrantTypes.RefreshToken));
                    }});
            log.debug("Retrieved access token refresh data from OAuth2 token endpoint "
                    + GSOAuth2_10.Endpoints.Token + ": " + responseData);

            // Pass on error message in response data
            String error = (String) responseData.get("error");
            if(error != null) {
                throw new IOException("OAuth2 error refreshing access token: " + error);
            }

            // Retrieve tokens and expiry data from response
            String accessToken = (String) responseData.get("access_token");
            Number expiresIn = (Number) responseData.get("expires_in");
            String tokenType = (String) responseData.get("token_type");

            // Sanity-check response data
            if(!"Bearer".equals(tokenType)) {
                throw new IOException("OAuth2 error refreshing access token, invalid token type in data: "
                        + responseData);
            }
            if(accessToken == null) {
                throw new IOException("OAuth2 error refreshing access token, missing token in data: "
                        + responseData);
            }

            return new OAuth2Tokens(
                    accessToken, tokens.getRefreshToken(),
                    OAuth2Tokens.calculateExpiry(expiresIn));
        }
        else {
            throw new IllegalStateException("Unsupported implementation: " + this.implementation);
        }
    }

    /**
     * Performs an HTTP/S POST request to a given URL with the given POST parameters
     * and parses the response document, which must be JSON, into a Map of name/value objects.
     *
     * @param endpointUri Authorization or token endpoint
     * @param postParams  Name value pairs
     * @return JSON mapped response
     * @throws ClientProtocolException No HTTP 200 response
     * @throws IOException
     */
    protected Map<String, Object> performPostRequestAndParseJSONResponse(
            String endpointUri, List<NameValuePair> postParams)
            throws IOException {
        log.debug("Performing POST request to " + endpointUri
                + " and expecting JSON response. POST parameters: " + postParams);

        HttpPost post = new HttpPost(endpointUri);
        post.setEntity(new UrlEncodedFormEntity(postParams, "UTF-8"));

        String responseDataString = httpClient.execute(post, new ResponseHandler<String>() {
            public String handleResponse(HttpResponse response)
                    throws IOException {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if(statusCode == HttpStatus.SC_OK) {
                    HttpEntity entity = response.getEntity();
                    if(entity != null) {
                        return EntityUtils.toString(entity);
                    }
                    else {
                        return null;
                    }
                }
                throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
            }
        });
        return jsonMapper.readValue(responseDataString, Map.class);
    }
}
