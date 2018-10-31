/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010-2014 James Murty
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
package org.jets3t.service.impl.rest.httpclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.StorageService;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.HttpException;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.CopyObjectResultHandler;
import org.jets3t.service.impl.rest.XmlResponsesSaxParser.ListBucketHandler;
import org.jets3t.service.model.CreateBucketConfiguration;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.StorageBucket;
import org.jets3t.service.model.StorageBucketLoggingStatus;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.model.StorageOwner;
import org.jets3t.service.model.WebsiteConfig;
import org.jets3t.service.mx.MxDelegate;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.SignatureUtils;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Abstract REST/HTTP implementation of an S3Service based on the
 * <a href="http://jakarta.apache.org/commons/httpclient/">HttpClient</a> library.
 * <p>
 * This class uses properties obtained through {@link org.jets3t.service.Jets3tProperties}. For more information on
 * these properties please refer to
 * <a href="http://www.jets3t.org/toolkit/configuration.html">JetS3t Configuration</a>
 * </p>
 *
 * @author James Murty, Google Developers
 */
public abstract class RestStorageService extends StorageService implements JetS3tRequestAuthorizer {
    private static final Log log = LogFactory.getLog(RestStorageService.class);

    protected static enum HTTP_METHOD {PUT, POST, HEAD, GET, DELETE}

    protected HttpClient httpClient;
    protected CredentialsProvider credentialsProvider;

    protected RegionEndpointCache regionEndpointCache = null;

    protected String defaultStorageClass;
    protected String defaultServerSideEncryptionAlgorithm;

    protected volatile boolean shuttingDown;

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials the user credentials to use when communicating with S3, may be null in which case the
     *                    communication is done as an anonymous user.
     */
    public RestStorageService(ProviderCredentials credentials) {
        this(credentials, null, null);
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials                    the S3 user credentials to use when communicating with S3, may be null in which case the
     *                                       communication is done as an anonymous user.
     * @param invokingApplicationDescription a short description of the application using the service, suitable for inclusion in a
     *                                       user agent string for REST/HTTP requests. Ideally this would include the application's
     *                                       version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider            an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     *                                       prompting for credentials when necessary.
     */
    public RestStorageService(ProviderCredentials credentials, String invokingApplicationDescription,
                              CredentialsProvider credentialsProvider) {
        this(credentials, invokingApplicationDescription, credentialsProvider,
                Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME));
    }

    /**
     * Constructs the service and initialises the properties.
     *
     * @param credentials                    the S3 user credentials to use when communicating with S3, may be null in which case the
     *                                       communication is done as an anonymous user.
     * @param invokingApplicationDescription a short description of the application using the service, suitable for inclusion in a
     *                                       user agent string for REST/HTTP requests. Ideally this would include the application's
     *                                       version number, for example: <code>Cockpit/0.7.3</code> or <code>My App Name/1.0</code>
     * @param credentialsProvider            an implementation of the HttpClient CredentialsProvider interface, to provide a means for
     *                                       prompting for credentials when necessary.
     * @param jets3tProperties               JetS3t properties that will be applied within this service.
     */
    public RestStorageService(ProviderCredentials credentials, String invokingApplicationDescription,
                              CredentialsProvider credentialsProvider, Jets3tProperties jets3tProperties) {
        super(credentials, invokingApplicationDescription, jets3tProperties);
        this.credentialsProvider = credentialsProvider;

        this.defaultStorageClass = getJetS3tProperties().getStringProperty(
                "s3service.default-storage-class", null);
        this.defaultServerSideEncryptionAlgorithm = getJetS3tProperties().getStringProperty(
                "s3service.server-side-encryption", null);
        this.regionEndpointCache = new RegionEndpointCache();
    }

    @Override
    protected void initializeDefaults() {
        super.initializeDefaults();
        this.httpClient = initHttpConnection();
        initializeProxy();
    }

    protected void initializeProxy() {
        // Retrieve Proxy settings.
        if(getJetS3tProperties().getBoolProperty("httpclient.proxy-autodetect", true)) {
            RestUtils.initHttpProxy(httpClient, getJetS3tProperties(), this.getEndpoint());
        }
        else {
            String proxyHostAddress = getJetS3tProperties().getStringProperty("httpclient.proxy-host", null);
            int proxyPort = getJetS3tProperties().getIntProperty("httpclient.proxy-port", -1);
            String proxyUser = getJetS3tProperties().getStringProperty("httpclient.proxy-user", null);
            String proxyPassword = getJetS3tProperties().getStringProperty("httpclient.proxy-password", null);
            String proxyDomain = getJetS3tProperties().getStringProperty("httpclient.proxy-domain", null);
            RestUtils.initHttpProxy(httpClient, getJetS3tProperties(), false,
                    proxyHostAddress, proxyPort, proxyUser, proxyPassword, proxyDomain, this.getEndpoint());
        }
    }

    protected abstract boolean isTargettingGoogleStorageService();

    /**
     * Shut down all connections managed by the underlying HttpConnectionManager.
     */
    @Override
    protected void shutdownImpl() throws ServiceException {
        shuttingDown = true;
        ClientConnectionManager manager = this.getHttpConnectionManager();
        manager.shutdown();
    }

    /**
     * Initialise HttpClient and HttpConnectionManager objects with the configuration settings
     * appropriate for communicating with S3. By default, this method simply delegates the
     * configuration task to {@link org.jets3t.service.utils.RestUtils#initHttpConnection(JetS3tRequestAuthorizer, org.jets3t.service.Jets3tProperties, String, org.apache.http.client.CredentialsProvider)}.
     * <p>
     * To alter the low-level behaviour of the HttpClient library, override this method in
     * a subclass and apply your own settings before returning the objects.
     *
     * @return configured HttpClient library client and connection manager objects.
     */
    protected HttpClient initHttpConnection() {
        return RestUtils.initHttpConnection(
                this,
                getJetS3tProperties(),
                getInvokingApplicationDescription(),
                getCredentialsProvider());
    }

    /**
     * @return the manager of HTTP connections for this service.
     */
    public ClientConnectionManager getHttpConnectionManager() {
        return this.httpClient.getConnectionManager();
    }

    /**
     * @return the HTTP client for this service.
     */
    public HttpClient getHttpClient() {
        return this.httpClient;
    }

    /**
     * Replaces the service's default HTTP client.
     * This method should only be used by advanced users.
     *
     * @param httpClient the client that will replace the default client created by
     *                   the class constructor.
     */
    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @return the credentials provider this service will use to authenticate itself, or null
     *         if no provider is set.
     */
    public CredentialsProvider getCredentialsProvider() {
        return this.credentialsProvider;
    }

    /**
     * Sets the credentials provider this service will use to authenticate itself.
     * Changing the credentials provider with this method will have no effect until
     * the {@link #initHttpConnection()} method is called.
     *
     * @param credentialsProvider Credentials
     */
    public void setCredentialsProvider(CredentialsProvider credentialsProvider) {
        this.credentialsProvider = credentialsProvider;
    }

    public RegionEndpointCache getRegionEndpointCache() {
        return this.regionEndpointCache;
    }

    public void setRegionEndpointCache(RegionEndpointCache rec) {
        this.regionEndpointCache = rec;
    }

    /**
     * @param contentType HTTP Header
     * @return true if the given Content-Type string represents an XML document.
     */
    protected boolean isXmlContentType(String contentType) {
        return contentType != null
                && contentType.toLowerCase().startsWith(Mimetypes.MIMETYPE_XML.toLowerCase());
    }

    protected HttpResponse performRequest(HttpUriRequest httpMethod, int[] expectedResponseCodes)
            throws ServiceException {
        return performRequest(httpMethod, expectedResponseCodes, null);
    }

    /**
     * Performs an HTTP/S request by invoking the provided HttpMethod object. If the HTTP
     * response code doesn't match the expected value, an exception is thrown.
     *
     * @param httpMethod            the object containing a request target and all other information necessary to perform the
     *                              request
     * @param expectedResponseCodes the HTTP response code(s) that indicates a successful request. If the response code received
     *                              does not match this value an error must have occurred, so an exception is thrown.
     * @param context               An HttpContext to facilitate information sharing in the HTTP chain
     * @throws ServiceException all exceptions are wrapped in an ServiceException. Depending on the kind of error that
     *                          occurred, this exception may contain additional error information available from an XML
     *                          error response document.
     */
    protected HttpResponse performRequest(
            HttpUriRequest httpMethod,
            int[] expectedResponseCodes,
            HttpContext context) throws ServiceException
    {
        HttpResponse response = null;
        try {
            if(log.isDebugEnabled()) {
                log.debug("Performing " + httpMethod.getMethod()
                        + " request for '" + httpMethod.getURI().toString()
                        + "', expecting response codes: " +
                        "[" + ServiceUtils.join(expectedResponseCodes, ",") + "]");
                log.debug("Headers: " + Arrays.asList(httpMethod.getAllHeaders()));
            }

            // Track count of different response "error" types
            int internalErrorCount = 0;
            int requestTimeoutErrorCount = 0;
            int redirectCount = 0;
            int authFailureCount = 0;
            int requestTimeTooSkewedErrorCount = 0;

            // Use retry count limit for all error types
            int retryMaxCount = getJetS3tProperties().getIntProperty("httpclient.retry-max", 5);

            String forceRequestSignatureVersion = null;

            // Perform the request, and retry on potentially recoverable failures.
            // This eternal loop is broken by an explicit `break` command or by throwing an exception
            while(true) {
                // Build the authorization string for the method
                authorizeHttpRequest(httpMethod, context, forceRequestSignatureVersion);

                response = httpClient.execute(httpMethod, context);
                int responseCode = response.getStatusLine().getStatusCode();

                String contentType = "";
                if(response.getFirstHeader("Content-Type") != null) {
                    contentType = response.getFirstHeader("Content-Type").getValue();
                }

                if(log.isDebugEnabled()) {
                    log.debug("Response for '" + httpMethod.getMethod()
                            + "'. Content-Type: " + contentType
                            + ", Headers: " + Arrays.asList(response.getAllHeaders()));
                    log.debug("Response entity: " + response.getEntity());
                    if(response.getEntity() != null) {
                        log.debug("Entity length: " + response.getEntity().getContentLength());
                    }
                }

                // Check whether we got the expected response code?
                boolean didReceiveExpectedResponseCode = false;
                for (int i = 0; i < expectedResponseCodes.length; i++) {
                    if (responseCode == expectedResponseCodes[i]) {
                        didReceiveExpectedResponseCode = true;
                        break;
                    }
                }

                if(log.isDebugEnabled()) {
                    log.debug(
                        "Received response code " + responseCode + "; matches one of expected set ("
                        + Arrays.toString(expectedResponseCodes) + ")? "
                        + didReceiveExpectedResponseCode);
                }

                /*
                 * IMPORTANT!
                 * Break out of while(true) loop if we received an expected response code, the
                 * remainder of code in this while(true) loop handles errors and retries.
                 */
                if (didReceiveExpectedResponseCode) {
                    break;
                }

                if(log.isDebugEnabled()) {
                    log.debug("Error response xml: " + isXmlContentType(contentType));
                    log.debug("Error response entity: " + response.getEntity());
                    log.debug("Error response entity length: " +
                        (response.getEntity() == null ? "??" : "" +
                            response.getEntity().getContentLength()));
                }

                // Prepare exception with information about error response, whether or not it
                // contains XML content, in case we decide not to retry.
                ServiceException exception = null;

                if (isXmlContentType(contentType)
                    && response.getEntity() != null
                    && response.getEntity().getContentLength() != 0)
                {
                    // Prepare exception for XML-bearing error response
                    if(log.isDebugEnabled()) {
                        log.debug("Response '" + httpMethod.getURI().getRawPath()
                                + "' - Received error response with XML message");
                    }

                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = null;
                    try {
                        reader = new BufferedReader(new InputStreamReader(
                            new HttpMethodReleaseInputStream(response)));
                        String line;
                        while((line = reader.readLine()) != null) {
                            sb.append(line).append("\n");
                        }
                    }
                    finally {
                        if(reader != null) {
                            reader.close();
                        }
                    }
                    // Prepare exception containing the XML message document.
                    exception = new ServiceException("Service Error Message.", sb.toString());
                } else {
                    if(log.isDebugEnabled()) {
                        log.debug("Response '" + httpMethod.getURI().getRawPath()
                                + "' - Received error response without XML content");
                    }
                    String responseText = null;
                    byte[] responseBody = null;
                    if(response.getEntity() != null) {
                        responseBody = EntityUtils.toByteArray(response.getEntity());
                    }
                    if(responseBody != null && responseBody.length > 0) {
                        responseText = new String(responseBody);
                    }
                    // Prepare exception containing the HTTP error fields.
                    HttpException httpException = new HttpException(
                        responseCode, response.getStatusLine().getReasonPhrase());
                    exception = new ServiceException(
                        "Request Error" + (responseText != null ? " [" + responseText + "]." : "."),
                        httpException);
                }
                exception.setResponseCode(responseCode);
                exception.setResponseHeaders(RestUtils.convertHeadersToMap(response.getAllHeaders()));
                // Consume and release connection.
                EntityUtils.consume(response.getEntity());

                /*
                 * For cases where the request may succeed if retried, count the number of attempts
                 * we have made to ensure we don't exceeded the max retry limit.
                 */
                // Sleep then retry on 5xx Internal Server errors.
                if (responseCode >= 500) {
                    // Throws provided exception if we have exceeded the retry count
                    sleepOnInternalError(++internalErrorCount, exception);
                }
                // Retry after Temporary Redirect 307
                else if(responseCode == 307) {
                    // Retry up to our limit; but at least once to support S3 bucket locations
                    if (redirectCount >= retryMaxCount
                        && redirectCount > 0)  // Ensure we have retried at least once...
                    {
                        throw exception;
                    }

                    // Set new URI from Location header
                    Header locationHeader = response.getFirstHeader("location");
                    URI newLocation = new URI(locationHeader.getValue());
                    // deal with implementations of HttpUriRequest
                    if(httpMethod instanceof HttpRequestBase) {
                        ((HttpRequestBase) httpMethod).setURI(newLocation);
                    }
                    else if(httpMethod instanceof RequestWrapper) {
                        ((RequestWrapper) httpMethod).setURI(newLocation);
                    }
                    httpMethod.setHeader("Host", newLocation.getHost());

                    redirectCount++;
                    if(log.isDebugEnabled()) {
                        log.debug(
                            "Following Temporary Redirect (" + redirectCount + ") to: "
                            + httpMethod.getURI().toString());
                    }
                }
                else if("RequestTimeout".equals(exception.getErrorCode())) {
                    if(requestTimeoutErrorCount >= retryMaxCount) {
                        throw exception;
                    }
                    requestTimeoutErrorCount++;
                    if(log.isWarnEnabled()) {
                        log.warn(
                            "Retrying connection that failed with RequestTimeout error ("
                            + requestTimeoutErrorCount + ")");
                    }
                }
                else if("RequestTimeTooSkewed".equals(exception.getErrorCode())) {
                    if(requestTimeTooSkewedErrorCount >= retryMaxCount) {
                        throw exception;
                    }
                    requestTimeTooSkewedErrorCount++;
                    this.timeOffset = RestUtils.calculateTimeAdjustmentOffset(response);
                    if(log.isWarnEnabled()) {
                        log.warn("Adjusted time offset in response to RequestTimeTooSkewed error"
                                + "(" + requestTimeTooSkewedErrorCount + ")."
                                + "Local machine and service disagree on the time by approximately "
                                + (this.timeOffset / 1000) + " seconds, please fix your system's time."
                                + " Retrying connection.");
                    }
                }
                // Special handling for S3 object PUT failures causing NoSuchKey errors - Issue #85, #175
                // Treat this as a special kind of internal server error.
                else if(responseCode == 404
                        && "PUT".equalsIgnoreCase(httpMethod.getMethod())
                        && "NoSuchKey".equals(exception.getErrorCode())
                        // If PUT operation is trying to copy an existing source object, don't ignore 404
                        && httpMethod.getFirstHeader(getRestHeaderPrefix() + "copy-source") == null) {
                    // Throws provided exception if we have exceeded the retry count
                    sleepOnInternalError(++internalErrorCount, exception);
                    if(log.isDebugEnabled()) {
                        log.debug("Ignoring NoSuchKey/404 error on PUT to: " + httpMethod.getURI().toString());
                    }
                }
                else if((responseCode == 403 || responseCode == 401) && this.isRecoverable403(httpMethod, exception)) {
                    // Retry up to our limit; but at least once
                    if (authFailureCount >= retryMaxCount
                        && authFailureCount > 0)  // Ensure we have retried at least once...
                    {
                        throw exception;
                    }
                    authFailureCount++;
                    if(log.isDebugEnabled()) {
                        log.debug("Retrying after 403 Forbidden");
                    }
                }
                // Special handling for requests to a region that requires AWS
                // request signature version 4 when service isn't configured to
                // use AWS4-HMAC-SHA256 signatures by default.
                else if ("InvalidRequest".equals(exception.getErrorCode())
                         && exception.getErrorMessage().contains("Please use AWS4-HMAC-SHA256"))
                {
                    forceRequestSignatureVersion = "AWS4-HMAC-SHA256";
                    // Retry up to our limit; but at least once
                    if (authFailureCount >= retryMaxCount && authFailureCount > 0) {
                        throw exception;
                    }
                    authFailureCount++;
                    if (log.isWarnEnabled()) {
                        log.warn(
                            "Retrying request with \"AWS4-HMAC-SHA256\" signing"
                            + " mechanism: " + httpMethod);
                    }
                }
                // Special handling for requests signed using AWS request signature version
                // 4 but sent to the wrong region due to an incorrect Host endpoint.
                else if("AuthorizationHeaderMalformed".equals(exception.getErrorCode())) {
                    String expectedRegion = null;
                    try {
                        expectedRegion = exception.getXmlMessageAsBuilder()
                            .xpathFind("/Error/Region").getElement().getTextContent();
                    } catch(Exception ignored) {
                        // Throw original exception if we cannot parse expected
                        // Region out of error message, in which case this error
                        // was caused by something other than just wrong region.
                        throw exception;
                    }

                    // Cache correct region for this request's bucket name
                    URI originalURI = httpMethod.getURI();
                    String bucketName = ServiceUtils.findBucketNameInHostOrPath(
                        originalURI, this.getEndpoint());
                    this.regionEndpointCache.putRegionForBucketName(
                        bucketName, expectedRegion);

                    ((HttpRequestBase) httpMethod).setURI(
                        SignatureUtils.awsV4CorrectHostnameForRegion(
                            httpMethod.getURI(), expectedRegion));
                    authFailureCount++;

                    if(log.isWarnEnabled()) {
                        log.warn("Retrying request after automatic adjustment of"
                            + " Host endpoint from " + "\"" + originalURI.getHost()
                            + "\" to \"" + httpMethod.getURI().getHost()
                            + "\" following request signing error"
                            + " using AWS request signing version 4: " + httpMethod);
                    }
                }
                // If we haven't explicitly detected an retry-able error response type above,
                // just throw the exception to abort the request.
                else {
                    throw exception;
                }

                // Print warning message if a non-fatal error occurred (we only reach this
                // point in the code if an exception isn't thrown above)
                if(log.isWarnEnabled()) {
                    String requestDescription =
                        httpMethod.getMethod()
                            + " '" + httpMethod.getURI().getPath()
                            + (httpMethod.getURI().getQuery() != null
                            && httpMethod.getURI().getQuery().length() > 0
                            ? "?" + httpMethod.getURI().getQuery() : "")
                            + "'"
                            + " -- ResponseCode: " + responseCode
                            + ", ResponseStatus: " + response.getStatusLine().getReasonPhrase()
                            + ", Request Headers: [" + ServiceUtils.join(httpMethod.getAllHeaders(), ", ") + "]"
                            + ", Response Headers: [" + ServiceUtils.join(response.getAllHeaders(), ", ") + "]";
                    requestDescription = requestDescription.replaceAll("[\\n\\r\\f]", "");  // Remove any newlines.
                    log.warn("Retrying request following error response: " + requestDescription);
                }
            }
        }
        // Top-level exception handler to deal with otherwise unhandled exceptions, or to augment
        // a ServiceException thrown above with additional information.
        catch(Exception t) {
            if(log.isDebugEnabled()) {
                String msg = "Rethrowing as a ServiceException error in performRequest: " + t;
                if(t.getCause() != null) {
                    msg += ", with cause: " + t.getCause();
                }
                if(log.isTraceEnabled()) {
                    log.trace(msg, t);
                }
                else {
                    log.debug(msg);
                }
            }
            if(log.isDebugEnabled() && !shuttingDown) {
                log.debug("Releasing HttpClient connection after error: " + t.getMessage());
            }
            httpMethod.abort();

            ServiceException serviceException;
            if(t instanceof ServiceException) {
                serviceException = (ServiceException) t;
            }
            else {
                MxDelegate.getInstance().registerS3ServiceExceptionEvent();
                serviceException = new ServiceException("Request Error: " + t.getMessage(), t);
            }

            // Add S3 request and host IDs from HTTP headers to exception, if they are available
            // and have not already been populated by parsing an XML error response.
            if(!serviceException.isParsedFromXmlMessage()
                    && response != null
                    && response.getFirstHeader(Constants.AMZ_REQUEST_ID_1) != null
                    && response.getFirstHeader(Constants.AMZ_REQUEST_ID_2) != null) {
                serviceException.setRequestAndHostIds(
                        response.getFirstHeader(Constants.AMZ_REQUEST_ID_1).getValue(),
                        response.getFirstHeader(Constants.AMZ_REQUEST_ID_2).getValue());
                serviceException.setResponseHeaders(RestUtils.convertHeadersToMap(
                        response.getAllHeaders()));
            }
            if(response != null && response.getStatusLine() != null) {
                // If no network connection is available, status info is not available
                serviceException.setResponseCode(response.getStatusLine().getStatusCode());
                serviceException.setResponseStatus(response.getStatusLine().getReasonPhrase());
            }
            if(httpMethod.getFirstHeader("Host") != null) {
                serviceException.setRequestHost(
                        httpMethod.getFirstHeader("Host").getValue());
            }
            if(response != null && response.getFirstHeader("Date") != null) {
                serviceException.setResponseDate(
                        response.getFirstHeader("Date").getValue());
            }
            throw serviceException;
        }
        return response;
    }

    /**
     * Determine whether a given 403 Forbidden HTTP error response is recoverable and should
     * be retried. Normally 403s should only be retried if we can take some action as a side
     * effect which makes the subsequent request likely to succeed.
     * <p>
     * Generally, such errors should not be retried since a user's access permissions
     * for an item are unlikely to change, but if a service is using expiring authorization tokens
     * (e.g. OAuth) it may be worthwhile retrying after refreshing those tokens.
     *
     * @param httpRequest Request
     * @param exception   Server response
     * @return true if the request should be retried, otherwise false.
     */
    protected boolean isRecoverable403(HttpUriRequest httpRequest, Exception exception) {
        return false;
    }

    /**
     * Authorizes an HTTP/S request by signing it with an HMAC signature compatible with
     * the S3 service and Google Storage (legacy) authorization techniques.
     * <p>
     * The signature is added to the request as an Authorization header.
     *
     * @param httpMethod the request object
     * @param context
     * @param forceRequestSignatureVersion
     * request signature/signing version to use for requests, may be null in
     * which case the default signature version is applied.
     * @throws ServiceException
     */
    public void authorizeHttpRequest(
        HttpUriRequest httpMethod, HttpContext context,
        String forceRequestSignatureVersion) throws ServiceException
    {
        if(getProviderCredentials() != null) {
            if(log.isDebugEnabled()) {
                log.debug("Adding authorization for Access Key '"
                        + getProviderCredentials().getAccessKey() + "'.");
            }
        }
        else {
            if(log.isDebugEnabled()) {
                log.debug("Service has no Credential and is un-authenticated, skipping authorization");
            }
            return;
        }

        // Clear any existing Authorization headers
        httpMethod.removeHeaders("Authorization");

        // Set/update the date timestamp to the current time
        // Note that this will be over-ridden if an "x-amz-date" or
        // "x-goog-date" header is present.
        httpMethod.setHeader("Date",
                ServiceUtils.formatRfc822Date(getCurrentTimeWithOffset()));

        final URI requestURI;
        if(httpMethod.getURI().isAbsolute()) {
            requestURI = httpMethod.getURI();
        }
        else {
            // Handle strange edge-case that can occur when retrying requests in
            // which the URI object has a null host value, re #205
            try {
                // Re-create request's URI to populate its internal host value
                requestURI = new URI(String.format("%s%s",
                        context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST).toString(), httpMethod.getURI()));
            } catch (URISyntaxException e) {
                throw new ServiceException(
                    "Failed to re-create URI for request containing a URI"
                    + " object with an invalid null Host value", e);
            }
        }

        String requestBucketName = ServiceUtils.findBucketNameInHostOrPath(
            requestURI, this.getEndpoint());
        String requestSignatureVersion = this.getJetS3tProperties()
            .getStringProperty(
                "storage-service.request-signature-version", "AWS2")
            .toUpperCase();

        if ("AWS4-HMAC-SHA256".equalsIgnoreCase(forceRequestSignatureVersion)
            || "AWS4-HMAC-SHA256".equalsIgnoreCase(requestSignatureVersion)
            // If we have a cached region for request's bucket target, we know
            // we have used "AWS4-HMAC-SHA256" for this bucket in the past.
            || (this.regionEndpointCache != null
                && this.regionEndpointCache.containsRegionForBucketName(
                    requestBucketName)))
        {
            requestSignatureVersion = "AWS4-HMAC-SHA256";
            // Look up AWS region appropriate for the request's Host endpoint
            // from the request's Host if a definite mapping is available...
            String region = SignatureUtils.awsRegionForRequest(requestURI);
            if (region != null) {
                // Try caching the definitive region in case this request is
                // directed at a bucket. If it's not a bucket-related request
                // this is a no-op.
                this.regionEndpointCache.putRegionForBucketName(
                    requestBucketName, region);
            }
            // ...otherwise from the region cache if available...
            if (region == null && this.regionEndpointCache != null) {
                region = this.regionEndpointCache.getRegionForBucketName(
                    requestBucketName);

                // We cached a bucket-to-region mapping previously but this
                // request doesn't use the correct Host name for the region,
                // so fix that now to avoid failure or excess retries.
                if (region != null) {
                    ((HttpRequestBase) httpMethod).setURI(
                        SignatureUtils.awsV4CorrectHostnameForRegion(
                            requestURI, region));
                }
            }
            // ...finally fall back to the default region and hope for the best.
            if (region == null) {
                region = "us-east-1";
            }

            String requestPayloadHexSHA256Hash =
                SignatureUtils.awsV4GetOrCalculatePayloadHash(httpMethod);
            httpMethod.setHeader(
                "x-amz-content-sha256", requestPayloadHexSHA256Hash);

            SignatureUtils.awsV4SignRequestAuthorizationHeader(
                requestSignatureVersion, httpMethod,
                this.getProviderCredentials(), requestPayloadHexSHA256Hash,
                region);
        } else if ("AWS2".equalsIgnoreCase(forceRequestSignatureVersion)
                   || "AWS2".equalsIgnoreCase(requestSignatureVersion))
        {
            /*
             * Determine the complete URL for the S3 resource, including any S3-specific parameters.
             */
            // Use raw-path, otherwise escaped characters are unescaped and a wrong
            // signature is produced
            String fullUrl = requestURI.getRawPath();

            // If bucket name is not already part of the full path, add it.
            // This can be the case if the Host name has a bucket-name prefix,
            // or if the Host name constitutes the bucket name for DNS-redirects.
            String bucketName = ServiceUtils.findBucketNameInHostOrPath(
                requestURI, getEndpoint());
            if (bucketName != null && requestURI.getHost().startsWith(bucketName)) {
                fullUrl = "/" + bucketName + fullUrl;
            }

            String queryString = requestURI.getRawQuery();
            if(queryString != null && queryString.length() > 0) {
                fullUrl += "?" + queryString;
            }

            // Generate a canonical string representing the operation.
            String canonicalString = RestUtils.makeServiceCanonicalString(
                    httpMethod.getMethod(),
                    fullUrl,
                    convertHeadersToMap(httpMethod.getAllHeaders()),
                    null,
                    getRestHeaderPrefix(),
                    getResourceParameterNames());
            if(log.isDebugEnabled()) {
                log.debug("Canonical string ('|' is a newline): " + canonicalString.replace('\n', '|'));
            }

            // Sign the canonical string.
            String signedCanonical = ServiceUtils.signWithHmacSha1(
                    getProviderCredentials().getSecretKey(), canonicalString);

            // Add encoded authorization to connection as HTTP Authorization header.
            String authorizationString = getSignatureIdentifier() + " "
                    + getProviderCredentials().getAccessKey() + ":" + signedCanonical;
            httpMethod.setHeader("Authorization", authorizationString);
        } else {
            throw new ServiceException("Unsupported property setting for "
                + "storage-service.request-signature-version \""
                + requestSignatureVersion + "\", must be one of: "
                + "\"AWS2\" (legacy), \"AWS4-HMAC-SHA256\"");
        }
    }

    /**
     * Adds all the provided request parameters to a URL in GET request format.
     *
     * @param urlPath           the target URL
     * @param requestParameters the parameters to add to the URL as GET request params.
     * @return the target URL including the parameters.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected String addRequestParametersToUrlPath(String urlPath,
                                                   Map<String, String> requestParameters) throws ServiceException {
        if(requestParameters != null) {
            for(Map.Entry<String, String> entry : requestParameters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                urlPath += (!urlPath.contains("?") ? "?" : "&")
                        + RestUtils.encodeUrlString(key);
                if(value != null && value.length() > 0) {
                    urlPath += "=" + RestUtils.encodeUrlString(value);
                    if(log.isDebugEnabled()) {
                        log.debug("Added request parameter: " + key + "=" + value);
                    }
                }
                else {
                    if(log.isDebugEnabled()) {
                        log.debug("Added request parameter without value: " + key);
                    }
                }
            }
        }
        return urlPath;
    }

    /**
     * Adds the provided request headers to the connection.
     *
     * @param httpMethod     the connection object
     * @param requestHeaders the request headers to add as name/value pairs.
     */
    protected void addRequestHeadersToConnection(
            HttpUriRequest httpMethod,
            Map<String, Object> requestHeaders) {
        if(requestHeaders != null) {
            for(Map.Entry<String, Object> entry : requestHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue().toString();

                httpMethod.setHeader(key, value);
                if(log.isDebugEnabled()) {
                    log.debug("Added request header to connection: " + key + "=" + value);
                }
            }
        }
    }

    /**
     * Converts an array of Header objects to a map of name/value pairs.
     *
     * @param headers Request or response HTTP headers
     * @return Map by header name
     */
    private Map<String, Object> convertHeadersToMap(Header[] headers) {
        Map<String, Object> map = new HashMap<String, Object>();
        for(int i = 0; headers != null && i < headers.length; i++) {
            map.put(headers[i].getName(), headers[i].getValue());
        }
        return map;
    }

    /**
     * Adds all valid metadata name and value pairs as HTTP headers to the given HTTP method.
     * Null metadata names are ignored, as are metadata values that are not of type string.
     * <p>
     * The metadata values are verified to ensure that keys contain only ASCII characters,
     * and that items are not accidentally duplicated due to use of different capitalization.
     * If either of these verification tests fails, an {@link org.jets3t.service.ServiceException} is thrown.
     *
     * @param httpMethod Request
     * @param metadata   Metadata request headers
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected void addMetadataToHeaders(
            HttpUriRequest httpMethod,
            Map<String, Object> metadata)
            throws ServiceException {
        Map<String, Object> headersAlreadySeenMap = new HashMap<String, Object>(metadata.size());

        for(Map.Entry<String, Object> entry : metadata.entrySet()) {
            String key = entry.getKey();
            Object objValue = entry.getValue();

            if(key == null) {
                // Ignore invalid metadata.
                continue;
            }

            String value = objValue.toString();

            // Ensure user-supplied metadata values are compatible with the REST interface.
            // Key must be ASCII text, non-ASCII characters are not allowed in HTTP header names.
            boolean validAscii = false;
            UnsupportedEncodingException encodingException = null;
            try {
                byte[] asciiBytes = key.getBytes("ASCII");
                byte[] utf8Bytes = key.getBytes("UTF-8");
                validAscii = Arrays.equals(asciiBytes, utf8Bytes);
            }
            catch(UnsupportedEncodingException e) {
                // Shouldn't ever happen
                encodingException = e;
            }
            if(!validAscii) {
                String message =
                        "User metadata name is incompatible with the S3 REST interface, " +
                                "only ASCII characters are allowed in HTTP headers: " + key;
                if(encodingException == null) {
                    throw new ServiceException(message);
                }
                else {
                    throw new ServiceException(message, encodingException);
                }
            }

            // Fail early if user-supplied metadata cannot be represented as valid HTTP headers,
            // rather than waiting for a SignatureDoesNotMatch error.
            // NOTE: These checks are very much incomplete.
            if(value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
                throw new ServiceException("The value of metadata item " + key
                        + " cannot be represented as an HTTP header for the REST S3 interface: "
                        + value);
            }

            // Ensure each AMZ header is uniquely identified according to the lowercase name.
            String duplicateValue = (String) headersAlreadySeenMap.get(key.toLowerCase(Locale.ENGLISH));
            if(duplicateValue != null && !duplicateValue.equals(value)) {
                throw new ServiceException(
                        "HTTP header name occurs multiple times in request with different values, " +
                                "probably due to mismatched capitalization when setting metadata names. " +
                                "Duplicate metadata name: '" + key + "', All metadata: " + metadata);
            }

            // PUT: don't set the 'Content-Length' header or http-client-4 will
            // raise an exception 'already set'.
            if(!httpMethod.getMethod().equalsIgnoreCase("PUT")
                    || !S3Object.METADATA_HEADER_CONTENT_LENGTH.equalsIgnoreCase(key)) {
                httpMethod.setHeader(key, value);
            }
            headersAlreadySeenMap.put(key.toLowerCase(Locale.ENGLISH), value);
        }
    }

    /**
     * Compares the expected and actual ETag value for an uploaded object, and throws an
     * ServiceException if these values do not match.
     *
     * @param expectedETag   Expected checksum
     * @param uploadedObject Uploaded key
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected void verifyExpectedAndActualETagValues(String expectedETag, StorageObject uploadedObject)
            throws ServiceException {
        // Special handling for S3 MultiPart Part uploads, for which the response's ETag value is
        // an opaque value and is not a hex-encoded MD5 hash value of the uploaded data like all
        // other S3 ETag response values (Issue #141).
        // See https://forums.aws.amazon.com/thread.jspa?messageID=203436&#203436
        if(expectedETag.length() != 32) {
            log.warn("The ETag header value '" + expectedETag + "' returned for "
                    + uploadedObject + " is not a valid hex-encoded MD5 hash value;"
                    + " cannot verify the correctness of the uploaded data");
            return;
        }

        // Compare our locally-calculated hash with the ETag returned by S3.
        if(!expectedETag.equals(uploadedObject.getETag())) {
            throw new ServiceException("Mismatch between MD5 hash of uploaded data ("
                    + expectedETag + ") and ETag returned by S3 ("
                    + uploadedObject.getETag() + ") for object key: "
                    + uploadedObject.getKey());
        }
        else {
            if(log.isDebugEnabled()) {
                log.debug("Object upload was automatically verified, the calculated MD5 hash " +
                        "value matched the ETag returned by S3: " + uploadedObject.getKey());
            }
        }
    }

    /**
     * Performs an HTTP HEAD request using the {@link #performRequest} method.
     *
     * @param bucketName        the bucket's name
     * @param objectKey         the object's key name, may be null if the operation is on a bucket only.
     * @param requestParameters parameters to add to the request URL as GET params
     * @param requestHeaders    headers to add to the request
     * @return the HTTP method object used to perform the request
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponse performRestHead(String bucketName, String objectKey,
                                           Map<String, String> requestParameters, Map<String, Object> requestHeaders)
            throws ServiceException {

        HttpUriRequest httpMethod = setupConnection(
                HTTP_METHOD.HEAD,
                bucketName,
                objectKey,
                requestParameters);

        // Add all request headers.
        addRequestHeadersToConnection(httpMethod, requestHeaders);

        return performRequest(httpMethod, new int[]{200});
    }

    /**
     * Performs an HTTP GET request using the {@link #performRequest} method.
     *
     * @param bucketName        the bucket's name
     * @param objectKey         the object's key name, may be null if the operation is on a bucket only.
     * @param requestParameters parameters to add to the request URL as GET params
     * @param requestHeaders    headers to add to the request
     * @return The HTTP method object used to perform the request.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponse performRestGet(String bucketName, String objectKey,
        Map<String, String> requestParameters, Map<String, Object> requestHeaders)
        throws ServiceException
    {
        int[] expectedStatusCodes = {200}; // 200 is normally the expected response code
        if (requestHeaders != null && requestHeaders.containsKey("Range")) {
            // Partial data responses have a status code of 206, or sometimes 200
            // for complete responses (issue #80)
            expectedStatusCodes = new int[]{206, 200};
        }
        return performRestGet(bucketName, objectKey, requestParameters, requestHeaders, expectedStatusCodes);
    }

    /**
     * Performs an HTTP GET request using the {@link #performRequest} method.
     *
     * @param bucketName        the bucket's name
     * @param objectKey         the object's key name, may be null if the operation is on a bucket only.
     * @param requestParameters parameters to add to the request URL as GET params
     * @param requestHeaders    headers to add to the request
     * @param expectedStatusCodes HTTP status response codes expected, anything else causes an exception.
     * @return The HTTP method object used to perform the request.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponse performRestGet(String bucketName, String objectKey,
        Map<String, String> requestParameters, Map<String, Object> requestHeaders,
        int[] expectedStatusCodes)
        throws ServiceException
    {

        HttpUriRequest httpMethod = setupConnection(
                HTTP_METHOD.GET,
                bucketName,
                objectKey,
                requestParameters);
        // Add all request headers.
        addRequestHeadersToConnection(httpMethod, requestHeaders);
        return performRequest(httpMethod, expectedStatusCodes);
    }

    /**
     * Performs an HTTP PUT request using the {@link #performRequest} method.
     *
     * @param bucketName        the name of the bucket the object will be stored in.
     * @param objectKey         the key (name) of the object to be stored.
     * @param metadata          map of name/value pairs to add as metadata to any S3 objects created.
     * @param requestParameters parameters to add to the request URL as GET params
     * @param requestEntity     an HttpClient object that encapsulates the object and data contents that will be
     *                          uploaded. This object supports the resending of object data, when possible.
     * @param autoRelease       if true, the HTTP Method object will be released after the request has
     *                          completed and the connection will be closed. If false, the object will
     *                          not be released and the caller must take responsibility for doing this.
     * @return a package including the HTTP method object used to perform the request, and the
     *         content length (in bytes) of the object that was PUT to S3.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponseAndByteCount performRestPut(String bucketName, String objectKey,
                                                      Map<String, Object> metadata, Map<String, String> requestParameters,
                                                      HttpEntity requestEntity, boolean autoRelease)
            throws ServiceException {
        // Add any request parameters.
        HttpUriRequest httpMethod = setupConnection(
                HTTP_METHOD.PUT,
                bucketName,
                objectKey,
                requestParameters);

        Map<String, Object> renamedMetadata = renameMetadataKeys(metadata);
        addMetadataToHeaders(httpMethod, renamedMetadata);

        long contentLength = 0;

        if(log.isTraceEnabled()) {
            log.trace("Put request with entity: " + requestEntity);
        }
        if(requestEntity != null) {
            ((HttpPut) httpMethod).setEntity(requestEntity);

            /* Explicitly apply any latent Content-Type header from the request entity to the
             * httpMethod to ensure it is included in the request signature, since it will be
             * included in the wire request by HttpClient. But only apply the latent mimetype
             * if an explicit Content-Type is not already set. See issue #109
             */
            if(requestEntity.getContentType() != null
                    && httpMethod.getFirstHeader("Content-Type") == null) {
                httpMethod.setHeader(requestEntity.getContentType());
            }
        }

        HttpResponse result = performRequest(httpMethod, new int[]{200, 204});

        if(requestEntity != null) {
            // Respond with the actual guaranteed content length of the uploaded data.
            contentLength = ((HttpPut) httpMethod).getEntity().getContentLength();
        }

        if(autoRelease) {
            releaseConnection(result);
        }

        return new HttpResponseAndByteCount(result, contentLength);
    }

    /**
     * Performs an HTTP POST request using the {@link #performRequest} method.
     *
     * @param bucketName        the name of the bucket the object will be stored in.
     * @param objectKey         the key (name) of the object to be stored.
     * @param metadata          map of name/value pairs to add as metadata to any S3 objects created.
     * @param requestParameters parameters to add to the request URL as GET params
     * @param requestEntity     an HttpClient object that encapsulates the object and data contents that will be
     *                          uploaded. This object supports the re-sending of object data, when possible.
     * @param autoRelease       if true, the HTTP Method object will be released after the request has
     *                          completed and the connection will be closed. If false, the object will
     *                          not be released and the caller must take responsibility for doing this.
     * @return a package including the HTTP method object used to perform the request, and the
     *         content length (in bytes) of the object that was POSTed to S3.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponse performRestPost(String bucketName, String objectKey,
                                           Map<String, Object> metadata, Map<String, String> requestParameters,
                                           HttpEntity requestEntity, boolean autoRelease)
            throws ServiceException {
        // Add any request parameters.
        HttpUriRequest postMethod = setupConnection(
                HTTP_METHOD.POST,
                bucketName,
                objectKey,
                requestParameters);

        Map<String, Object> renamedMetadata = renameMetadataKeys(metadata);
        addMetadataToHeaders(postMethod, renamedMetadata);

        if(requestEntity != null) {
            ((HttpPost) postMethod).setEntity(requestEntity);
        }

        HttpResponse result = performRequest(postMethod, new int[]{200});

        if(autoRelease) {
            releaseConnection(result);
        }

        return result;
    }

    /**
     * Performs an HTTP DELETE request using the {@link #performRequest} method.
     *
     * @param bucketName the bucket's name
     * @param objectKey  the object's key name, may be null if the operation is on a bucket only.
     * @return The HTTP method object used to perform the request.
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpResponse performRestDelete(String bucketName, String objectKey,
                                             Map<String, String> requestParameters, String multiFactorSerialNumber,
                                             String multiFactorAuthCode) throws ServiceException {

        HttpUriRequest httpMethod = setupConnection(
                HTTP_METHOD.DELETE,
                bucketName,
                objectKey,
                requestParameters);

        // Set Multi-Factor Serial Number and Authentication code if provided.
        if(multiFactorSerialNumber != null || multiFactorAuthCode != null) {
            httpMethod.setHeader(Constants.AMZ_MULTI_FACTOR_AUTH_CODE,
                    multiFactorSerialNumber + " " + multiFactorAuthCode);
        }

        HttpResponse result = performRequest(httpMethod, new int[]{204, 200});

        // Release connection after DELETE (there's no response content)
        if(log.isDebugEnabled()) {
            log.debug("Releasing HttpMethod after delete");
        }
        releaseConnection(result);

        return result;
    }

    protected HttpResponseAndByteCount performRestPutWithXmlBuilder(String bucketName,
                                                                    String objectKey, Map<String, Object> metadata, Map<String, String> requestParameters,
                                                                    XMLBuilder builder) throws ServiceException {
        try {
            if(metadata == null) {
                metadata = new HashMap<String, Object>();
            }
            if(!metadata.containsKey("content-type")) {
                metadata.put("Content-Type", "text/plain");
            }
            String xml = builder.asString(null);
            return performRestPut(bucketName, objectKey, metadata, requestParameters,
                    new StringEntity(xml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)), true);
        }
        catch(TransformerException e) {
            throw new ServiceException("Failed to PUT request containing an XML document", e);
        }
    }

    protected HttpResponse performRestPostWithXmlBuilder(String bucketName,
                                                         String objectKey, Map<String, Object> metadata, Map<String, String> requestParameters,
                                                         XMLBuilder builder) throws ServiceException {
        try {
            if(metadata == null) {
                metadata = new HashMap<String, Object>();
            }
            if(!metadata.containsKey("content-type")) {
                metadata.put("Content-Type", "text/plain");
            }
            String xml = builder.asString(null);
            return performRestPost(bucketName, objectKey, metadata, requestParameters,
                    new StringEntity(xml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)), false);
        }
        catch(Exception e) {
            if(e instanceof ServiceException) {
                throw (ServiceException) e;
            }
            else {
                throw new ServiceException("Failed to POST request containing an XML document", e);
            }
        }
    }

    /**
     * Creates an {@link org.apache.http.HttpRequest} object to handle a particular connection method.
     *
     * @param method     the HTTP method/connection-type to use, must be one of: PUT, HEAD, GET, DELETE
     * @param bucketName the bucket's name
     * @param objectKey  the object's key name, may be null if the operation is on a bucket only.
     * @return the HTTP method object used to perform the request
     * @throws org.jets3t.service.ServiceException
     *
     */
    protected HttpUriRequest setupConnection(HTTP_METHOD method, String bucketName,
                                             String objectKey, Map<String, String> requestParameters) throws ServiceException {
        if(bucketName == null) {
            throw new ServiceException("Cannot connect to S3 Service with a null path");
        }

        boolean disableDnsBuckets = this.getDisableDnsBuckets();
        String endPoint = this.getEndpoint();
        String hostname = ServiceUtils.generateS3HostnameForBucket(
                bucketName, disableDnsBuckets, endPoint);

        // Allow for non-standard virtual directory paths on the server-side
        String virtualPath = this.getVirtualPath();

        // Determine the resource string (ie the item's path in S3, including the bucket name)
        String resourceString = "/";
        if(hostname.equals(endPoint) && bucketName.length() > 0) {
            resourceString += bucketName + "/";
        }
        resourceString += (objectKey != null ? RestUtils.encodeUrlPath(objectKey, "/") : "");

        // Construct a URL representing a connection for the S3 resource.
        String url;
        if(isHttpsOnly()) {
            int securePort = this.getHttpsPort();
            url = "https://" + hostname + ":" + securePort + virtualPath + resourceString;
        }
        else {
            int insecurePort = this.getHttpPort();
            url = "http://" + hostname + ":" + insecurePort + virtualPath + resourceString;
        }
        if(log.isDebugEnabled()) {
            log.debug("S3 URL: " + url);
        }

        // Add additional request parameters to the URL for special cases (eg ACL operations)
        url = addRequestParametersToUrlPath(url, requestParameters);

        HttpUriRequest httpMethod;
        if(HTTP_METHOD.PUT.equals(method)) {
            httpMethod = new HttpPut(url);
        }
        else if(HTTP_METHOD.POST.equals(method)) {
            httpMethod = new HttpPost(url);
        }
        else if(HTTP_METHOD.HEAD.equals(method)) {
            httpMethod = new HttpHead(url);
        }
        else if(HTTP_METHOD.GET.equals(method)) {
            httpMethod = new HttpGet(url);
        }
        else if(HTTP_METHOD.DELETE.equals(method)) {
            httpMethod = new HttpDelete(url);
        }
        else {
            throw new IllegalArgumentException("Unrecognised HTTP method name: " + method);
        }

        // Set mandatory Request headers.
        if(httpMethod.getFirstHeader("Date") == null) {
            httpMethod.setHeader("Date",
                    ServiceUtils.formatRfc822Date(getCurrentTimeWithOffset()));
        }

        return httpMethod;
    }

    private void releaseConnection(HttpResponse pResponse) {
        if(pResponse == null) {
            return;
        }
        try {
            EntityUtils.consume(pResponse.getEntity());
        }
        catch(Exception e) {
            log.warn("Unable to consume response entity " + pResponse, e);
        }
    }

    /////////////////////////////////////////////////////////////////////
    // Methods below this point implement StorageService abstract methods
    /////////////////////////////////////////////////////////////////////

    @Override
    public boolean isBucketAccessible(String bucketName) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Checking existence of bucket: " + bucketName);
        }

        HttpResponse httpResponse = null;

        try {
            // Ensure bucket exists and is accessible by performing a HEAD request
            httpResponse = performRestHead(bucketName, null, null, null);

            EntityUtils.consume(httpResponse.getEntity());
        }
        catch(ServiceException e) {
            if(log.isDebugEnabled()) {
                log.debug("Bucket does not exist: " + bucketName, e);
            }
            return false;
        }
        catch(IOException e) {
            if(log.isWarnEnabled()) {
                log.warn("Unable to close response body input stream", e);
            }
        }
        finally {
            if(log.isDebugEnabled()) {
                log.debug("Releasing un-wanted bucket HEAD response");
            }
            releaseConnection(httpResponse);
        }

        // If we get this far, the bucket exists.
        return true;
    }

    @Override
    public int checkBucketStatus(String bucketName) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Checking availability of bucket name: " + bucketName);
        }

        HttpResponse httpResponse = null;

        // This request may return an XML document that we're not interested in. Clean this up.
        try {
            // Test bucket's status by performing a HEAD request against it.
            Map<String, String> params = new HashMap<String, String>();
            params.put("max-keys", "0");
            httpResponse = performRestHead(bucketName, null, params, null);

            EntityUtils.consume(httpResponse.getEntity());
        }
        catch(ServiceException e) {
            if(e.getResponseCode() == 403) {
                if(log.isDebugEnabled()) {
                    log.debug("Bucket named '" + bucketName + "' exists but is inaccessible, "
                            + "probably belongs to another user");
                }
                return BUCKET_STATUS__ALREADY_CLAIMED;
            }
            else if(e.getResponseCode() == 404) {
                if(log.isDebugEnabled()) {
                    log.debug("Bucket does not exist: " + bucketName, e);
                }
                return BUCKET_STATUS__DOES_NOT_EXIST;
            }
            else {
                throw e;
            }
        }
        catch(IOException e) {
            if(log.isWarnEnabled()) {
                log.warn("Unable to close response body input stream", e);
            }
        }
        finally {
            if(log.isDebugEnabled()) {
                log.debug("Releasing un-wanted bucket HEAD response");
            }
            releaseConnection(httpResponse);
        }

        // If we get this far, the bucket exists and you own it.
        return BUCKET_STATUS__MY_BUCKET;
    }

    @Override
    protected StorageBucket[] listAllBucketsImpl(Map<String, Object> headers) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Listing all buckets for user: "
                    + getProviderCredentials().getAccessKey());
        }

        String bucketName = ""; // Root path of S3 service lists the user's buckets.
        HttpResponse httpResponse = performRestGet(bucketName, null, null, headers);
        String contentType = httpResponse.getFirstHeader("Content-Type").getValue();

        if(!isXmlContentType(contentType)) {
            throw new ServiceException("Expected XML document response from S3 but received content type " +
                    contentType);
        }

        return getXmlResponseSaxParser()
                .parseListMyBucketsResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getBuckets();
    }

    @Override
    protected StorageOwner getAccountOwnerImpl() throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Looking up owner of S3 account via the ListAllBuckets response: "
                    + getProviderCredentials().getAccessKey());
        }

        String bucketName = ""; // Root path of S3 service lists the user's buckets.
        HttpResponse httpResponse = performRestGet(bucketName, null, null, null);
        String contentType = httpResponse.getFirstHeader("Content-Type").getValue();

        if(!isXmlContentType(contentType)) {
            throw new ServiceException("Expected XML document response from S3 but received content type " +
                    contentType);
        }

        return getXmlResponseSaxParser()
                .parseListMyBucketsResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getOwner();
    }


    @Override
    protected StorageObject[] listObjectsImpl(String bucketName, String prefix, String delimiter,
                                              long maxListingLength) throws ServiceException {
        return listObjectsInternal(bucketName, prefix, delimiter,
                maxListingLength, true, null).getObjects();
    }

    @Override
    protected StorageObjectsChunk listObjectsChunkedImpl(String bucketName, String prefix, String delimiter,
                                                         long maxListingLength, String priorLastKey, boolean completeListing) throws ServiceException {
        return listObjectsInternal(bucketName, prefix, delimiter,
                maxListingLength, completeListing, priorLastKey);
    }

    protected StorageObjectsChunk listObjectsInternal(
            String bucketName, String prefix, String delimiter, long maxListingLength,
            boolean automaticallyMergeChunks, String priorLastKey)
            throws ServiceException {
        Map<String, String> parameters = new HashMap<String, String>();
        if(prefix != null) {
            parameters.put("prefix", prefix);
        }
        if(delimiter != null) {
            parameters.put("delimiter", delimiter);
        }
        if(maxListingLength > 0) {
            parameters.put("max-keys", String.valueOf(maxListingLength));
        }

        List<StorageObject> objects = new ArrayList<StorageObject>();
        List<String> commonPrefixes = new ArrayList<String>();

        boolean incompleteListing = true;
        int ioErrorRetryCount = 0;

        while(incompleteListing) {
            if(priorLastKey != null) {
                parameters.put("marker", priorLastKey);
            }
            else {
                parameters.remove("marker");
            }

            HttpResponse httpResponse = performRestGet(bucketName, null, parameters, null);
            ListBucketHandler listBucketHandler;

            try {
                listBucketHandler = getXmlResponseSaxParser()
                        .parseListBucketResponse(
                                new HttpMethodReleaseInputStream(httpResponse));
                ioErrorRetryCount = 0;
            }
            catch(ServiceException e) {
                if(e.getCause() instanceof IOException && ioErrorRetryCount < 5) {
                    ioErrorRetryCount++;
                    if(log.isWarnEnabled()) {
                        log.warn("Retrying bucket listing failure due to IO error", e);
                    }
                    continue;
                }
                else {
                    throw e;
                }
            }

            StorageObject[] partialObjects = listBucketHandler.getObjects();
            if(log.isDebugEnabled()) {
                log.debug("Found " + partialObjects.length + " objects in one batch");
            }
            objects.addAll(Arrays.asList(partialObjects));

            String[] partialCommonPrefixes = listBucketHandler.getCommonPrefixes();
            if(log.isDebugEnabled()) {
                log.debug("Found " + partialCommonPrefixes.length + " common prefixes in one batch");
            }
            commonPrefixes.addAll(Arrays.asList(partialCommonPrefixes));

            incompleteListing = listBucketHandler.isListingTruncated();
            if(incompleteListing) {
                priorLastKey = listBucketHandler.getMarkerForNextListing();
                if(log.isDebugEnabled()) {
                    log.debug("Yet to receive complete listing of bucket contents, "
                            + "last key for prior chunk: " + priorLastKey);
                }
            }
            else {
                priorLastKey = null;
            }

            if(!automaticallyMergeChunks) {
                break;
            }
        }
        if(automaticallyMergeChunks) {
            if(log.isDebugEnabled()) {
                log.debug("Found " + objects.size() + " objects in total");
            }
            return new StorageObjectsChunk(
                    prefix, delimiter,
                    objects.toArray(new StorageObject[objects.size()]),
                    commonPrefixes.toArray(new String[commonPrefixes.size()]),
                    null);
        }
        else {
            return new StorageObjectsChunk(
                    prefix, delimiter,
                    objects.toArray(new StorageObject[objects.size()]),
                    commonPrefixes.toArray(new String[commonPrefixes.size()]),
                    priorLastKey);
        }
    }

    @Override
    protected void deleteObjectImpl(String bucketName, String objectKey,
                                    String versionId, String multiFactorSerialNumber, String multiFactorAuthCode)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<String, String>();
        if(versionId != null) {
            requestParameters.put("versionId", versionId);
        }
        performRestDelete(bucketName, objectKey, requestParameters,
                multiFactorSerialNumber, multiFactorAuthCode);
    }

    protected AccessControlList getObjectAclImpl(String bucketName, String objectKey)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving Access Control List for bucketName="
                    + bucketName + ", objectKey=" + objectKey);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("acl", "");

        HttpResponse httpResponse = performRestGet(bucketName, objectKey, requestParameters, null);
        return getXmlResponseSaxParser()
                .parseAccessControlListResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getAccessControlList();
    }

    @Override
    protected AccessControlList getObjectAclImpl(String bucketName, String objectKey,
                                                 String versionId) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving versioned Access Control List for bucketName="
                    + bucketName + ", objectKey=" + objectKey);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("acl", "");
        if(versionId != null) {
            requestParameters.put("versionId", versionId);
        }

        HttpResponse httpResponse = performRestGet(bucketName, objectKey, requestParameters, null);
        return getXmlResponseSaxParser()
                .parseAccessControlListResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getAccessControlList();
    }

    @Override
    protected AccessControlList getBucketAclImpl(String bucketName) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving Access Control List for Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("acl", "");

        HttpResponse httpResponse = performRestGet(bucketName, null, requestParameters, null);
        return getXmlResponseSaxParser()
                .parseAccessControlListResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getAccessControlList();
    }

    @Override
    protected void putObjectAclImpl(String bucketName, String objectKey, AccessControlList acl,
                                    String versionId) throws ServiceException {
        putAclImpl(bucketName, objectKey, acl, versionId);
    }

    @Override
    protected void putBucketAclImpl(String bucketName, AccessControlList acl)
            throws ServiceException {
        putAclImpl(bucketName, null, acl, null);
    }

    protected void putAclImpl(String bucketName, String objectKey, AccessControlList acl,
                              String versionId) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Setting Access Control List for bucketName=" + bucketName + ", objectKey=" + objectKey);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("acl", "");
        if(versionId != null) {
            requestParameters.put("versionId", versionId);
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Content-Type", "text/plain");

        String aclAsXml = acl.toXml();
        metadata.put("Content-Length", String.valueOf(aclAsXml.length()));
        performRestPut(bucketName, objectKey, metadata, requestParameters,
                new StringEntity(aclAsXml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)),
                true);
    }

    @Override
    protected StorageBucket createBucketImpl(String bucketName, String location,
                                             AccessControlList acl, Map<String, Object> headers)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Creating bucket with name: " + bucketName);
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.putAll(headers);
        HttpEntity requestEntity = null;

        if(location != null && !"US".equalsIgnoreCase(location)) {
            metadata.put("Content-Type", "text/xml");
            try {
                CreateBucketConfiguration config = new CreateBucketConfiguration(location);
                String configXml = config.toXml();
                metadata.put("Content-Length", String.valueOf(configXml.length()));
                requestEntity = new StringEntity(configXml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING));
            }
            catch(ParserConfigurationException e) {
                throw new ServiceException("Unable to encode CreateBucketConfiguration XML document", e);
            }
            catch(TransformerException e) {
                throw new ServiceException("Unable to encode CreateBucketConfiguration XML document", e);
            }
        }

        Map<String, Object> map = createObjectImpl(bucketName, null, null,
                requestEntity, metadata, null, acl, null, null);

        StorageBucket bucket = newBucket();
        bucket.setName(bucketName);
        bucket.setLocation(location);
        bucket.setAcl(acl);
        bucket.replaceAllMetadata(map);
        return bucket;
    }

    @Override
    protected void deleteBucketImpl(String bucketName) throws ServiceException {
        performRestDelete(bucketName, null, null, null, null);
    }

    protected boolean isLiveMD5HashingRequired(StorageObject object) {
        // We do not need to calculate the data MD5 hash during upload if the
        // expected hash value was provided as the object's Content-MD5 header.
        if(object.getMetadata(StorageObject.METADATA_HEADER_CONTENT_MD5) != null) {
            return false;
        }
        boolean disableLiveMd5 = getJetS3tProperties().getBoolProperty(
                "storage-service.disable-live-md5", false);
        return !disableLiveMd5;
    }

    protected String getBucketLocationImpl(String bucketName)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving location of Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("location", "");

        HttpResponse httpResponse = performRestGet(bucketName, null, requestParameters, null);
        return getXmlResponseSaxParser()
                .parseBucketLocationResponse(
                        new HttpMethodReleaseInputStream(httpResponse));
    }

    protected StorageBucketLoggingStatus getBucketLoggingStatusImpl(String bucketName)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving Logging Status for Bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("logging", "");

        HttpResponse httpResponse = performRestGet(bucketName, null, requestParameters, null);
        return getXmlResponseSaxParser()
                .parseLoggingStatusResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getBucketLoggingStatus();
    }

    protected void setBucketLoggingStatusImpl(String bucketName, StorageBucketLoggingStatus status)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Setting Logging Status for bucket: " + bucketName);
        }

        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put("logging", "");

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("Content-Type", "text/plain");

        String statusAsXml;
        try {
            statusAsXml = status.toXml();
        }
        catch(Exception e) {
            throw new ServiceException("Unable to generate LoggingStatus XML document", e);
        }
        try {
            performRestPut(bucketName, null, metadata, requestParameters,
                    new StringEntity(statusAsXml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)),
                    true);
        }
        catch(ServiceException se) {
            throw new ServiceException(se);
        }
    }

    /**
     * Beware of high memory requirements when creating large S3 objects when the Content-Length
     * is not set in the object.
     */
    @Override
    protected StorageObject putObjectImpl(String bucketName, StorageObject object) throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Creating Object with key " + object.getKey() + " in bucket " + bucketName);
        }

        HttpEntity requestEntity = null;

        if(object.getDataInputStream() != null) {
            if(object.containsMetadata(StorageObject.METADATA_HEADER_CONTENT_LENGTH)) {
                if(log.isDebugEnabled()) {
                    log.debug("Uploading object data with Content-Length: " + object.getContentLength());
                }
                requestEntity = new RepeatableRequestEntity(
                        object.getKey(),
                        object.getDataInputStream(),
                        object.getContentType(),
                        object.getContentLength(),
                        getJetS3tProperties(),
                        isLiveMD5HashingRequired(object));
            }
            else {
                // Use a BufferedHttpEntity for objects with an unknown content length, as the
                // entity will cache the results and doesn't need to know the data length in advance.
                if(log.isWarnEnabled()) {
                    log.warn("Content-Length of data stream not set, will automatically determine data length in memory");
                }
                BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
                basicHttpEntity.setContent(object.getDataInputStream());
                try {
                    requestEntity = new BufferedHttpEntity(basicHttpEntity);
                }
                catch(IOException ioe) {
                    throw new ServiceException("Unable to read data stream of unknown length", ioe);
                }
            }
        }
        putObjectWithRequestEntityImpl(bucketName, object, requestEntity, null);

        return object;
    }

    protected void putObjectWithRequestEntityImpl(String bucketName, StorageObject object,
                                                  HttpEntity requestEntity, Map<String, String> requestParams) throws ServiceException {

        Map<String, Object> map = createObjectImpl(bucketName, object.getKey(),
                object.getContentType(), requestEntity, object.getMetadataMap(),
                requestParams, object.getAcl(), object.getStorageClass(),
                object.getServerSideEncryptionAlgorithm());

        try {
            object.closeDataInputStream();
        }
        catch(IOException e) {
            if(log.isWarnEnabled()) {
                log.warn("Unable to close data input stream for object '" + object.getKey() + "'", e);
            }
        }

        // Populate object with result metadata.
        object.replaceAllMetadata(map);

        // Confirm that the data was not corrupted in transit by checking S3's calculated
        // hash value with the locally computed value. This is only necessary if the user
        // did not provide a Content-MD5 header with the original object.
        // Note that we can only confirm the data if we used a RepeatableRequestEntity to
        // upload it, if the user did not provide a content length with the original
        // object we are SOL.
        boolean md5Verify = isLiveMD5HashingRequired(object)
                && requestEntity instanceof RepeatableRequestEntity;
        if(log.isTraceEnabled()) {
            log.trace("Will " + (md5Verify ? "" : "NOT ")
                    + "verify expected and actual e-tag values.");
        }
        if(md5Verify) {
            // Obtain locally-calculated MD5 hash from request entity.
            String hexMD5OfUploadedData = ServiceUtils.toHex(
                    ((RepeatableRequestEntity) requestEntity).getMD5DigestOfData());
            verifyExpectedAndActualETagValues(hexMD5OfUploadedData, object);
        }
    }

    protected Map<String, Object> createObjectImpl(String bucketName, String objectKey, String contentType,
                                                   HttpEntity requestEntity, Map<String, Object> metadata,
                                                   Map<String, String> requestParams, AccessControlList acl,
                                                   String storageClass, String serverSideEncryptionAlgorithm)
            throws ServiceException {
        if(metadata == null) {
            metadata = new HashMap<String, Object>();
        }
        else {
            // Use a new map object in case the one we were provided is immutable.
            metadata = new HashMap<String, Object>(metadata);
        }
        if(contentType != null) {
            metadata.put("Content-Type", contentType);
        }
        else {
            metadata.put("Content-Type", Mimetypes.MIMETYPE_OCTET_STREAM);
        }

        // Apply per-object or default options when uploading object
        if(objectKey != null) {
            prepareStorageClass(metadata, storageClass, true, objectKey);

            // do not set server-side encryption flag for part-objects
            if (requestParams == null || !requestParams.containsKey("partNumber")) {
                prepareServerSideEncryption(metadata, serverSideEncryptionAlgorithm, objectKey);
            }
        }

        boolean isExtraAclPutRequired = !prepareRESTHeaderAcl(metadata, acl);

        if(log.isDebugEnabled()) {
            log.debug("Creating object bucketName=" + bucketName +
                    ", objectKey=" + objectKey +
                    ", storageClass=" + storageClass + "." +
                    " Content-Type=" + metadata.get("Content-Type") +
                    " Including data? " + (requestEntity != null) +
                    " Metadata: " + metadata +
                    " ACL: " + acl
            );
        }

        HttpResponseAndByteCount methodAndByteCount = performRestPut(
                bucketName, objectKey, metadata, requestParams, requestEntity, true);

        // Consume response content.
        HttpResponse httpResponse = methodAndByteCount.getHttpResponse();

        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(metadata); // Keep existing metadata.
        map.putAll(convertHeadersToMap(httpResponse.getAllHeaders()));
        map.put(StorageObject.METADATA_HEADER_CONTENT_LENGTH, String.valueOf(methodAndByteCount.getByteCount()));
        map = ServiceUtils.cleanRestMetadataMap(
                map, this.getRestHeaderPrefix(), this.getRestMetadataPrefix());

        if(isExtraAclPutRequired) {
            if(log.isDebugEnabled()) {
                log.debug("Creating object with a non-canned ACL using REST, so an extra ACL Put is required");
            }
            putAclImpl(bucketName, objectKey, acl, null);
        }

        return map;
    }

    /**
     * Prepares the metadata with the given ACL
     * an ACL is provided and can be represented with a HTTP header.
     *
     * @param metadata Metadata request headers
     * @param acl      Access control list
     * @return true if no acl or standard, false otherwise
     *         false if an ACL was provided but it could not be applied as a canned ACL.
     */
    protected boolean prepareRESTHeaderAcl(Map<String, Object> metadata, AccessControlList acl) {
        if(metadata == null) {
            throw new IllegalArgumentException("Null metadata not allowed.");
        }
        if(acl != null) {
            String restHeaderAclValue = acl.getValueForRESTHeaderACL();
            if(restHeaderAclValue != null) {
                metadata.put(this.getRestHeaderPrefix() + "acl", restHeaderAclValue);
            }
            else {
                return false;
            }
        }
        return true;
    }

    protected void prepareStorageClass(Map<String, Object> metadata, String storageClass,
                                       boolean useDefaultStorageClass, String objectKey) {
        if(metadata == null) {
            throw new IllegalArgumentException("Null metadata not allowed.");
        }
        if(getEnableStorageClasses()) {
            if(storageClass == null
                    && useDefaultStorageClass
                    && this.defaultStorageClass != null) {
                // Apply default storage class
                storageClass = this.defaultStorageClass;
                log.debug("Applied default storage class '" + storageClass
                        + "' to object '" + objectKey + "'");
            }
            if(storageClass != null
                    && !storageClass.equals(""))  // Hack to avoid applying empty storage class (Issue #121)
            {
                metadata.put(this.getRestHeaderPrefix() + "storage-class", storageClass);
            }
        }
    }

    protected void prepareServerSideEncryption(Map<String, Object> metadata,
                                               String serverSideEncryptionAlgorithm, String objectKey) {
        if(metadata == null) {
            throw new IllegalArgumentException("Null metadata not allowed.");
        }
        if(!getEnableServerSideEncryption()) {
            // Feature disabled
            return;
        }
        if(serverSideEncryptionAlgorithm == null
                && this.defaultServerSideEncryptionAlgorithm != null) {
            // Apply default server side encryption algorithm
            serverSideEncryptionAlgorithm = this.defaultServerSideEncryptionAlgorithm;
            log.debug("Applied default server-side encryption algorithm '"
                    + serverSideEncryptionAlgorithm
                    + "' to object '" + objectKey + "'");
        }
        if(serverSideEncryptionAlgorithm != null) {
            metadata.put(this.getRestHeaderPrefix() + "server-side-encryption",
                    serverSideEncryptionAlgorithm);
        }
    }

    @Override
    protected Map<String, Object> copyObjectImpl(String sourceBucketName, String sourceObjectKey,
                                                 String destinationBucketName, String destinationObjectKey,
                                                 AccessControlList acl, Map<String, Object> destinationMetadata, Calendar ifModifiedSince,
                                                 Calendar ifUnmodifiedSince, String[] ifMatchTags, String[] ifNoneMatchTags,
                                                 String versionId, String destinationObjectStorageClass,
                                                 String destinationObjectServerSideEncryptionAlgorithm)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Copying Object from " + sourceBucketName + ":" + sourceObjectKey
                    + " to " + destinationBucketName + ":" + destinationObjectKey);
        }

        Map<String, Object> metadata = new HashMap<String, Object>();

        String sourceKey = RestUtils.encodeUrlPath("/" + sourceBucketName + "/" + sourceObjectKey, "/");

        if(versionId != null) {
            sourceKey += "?versionId=" + versionId;
        }

        metadata.put(this.getRestHeaderPrefix() + "copy-source", sourceKey);

        prepareStorageClass(metadata, destinationObjectStorageClass, false, destinationObjectKey);
        prepareServerSideEncryption(metadata, destinationObjectServerSideEncryptionAlgorithm, destinationObjectKey);

        if(destinationMetadata != null) {
            metadata.put(this.getRestHeaderPrefix() + "metadata-directive", "REPLACE");
            // Include any metadata provided with S3 object.
            metadata.putAll(destinationMetadata);
            // Set default content type.
            if(!metadata.containsKey("Content-Type")) {
                metadata.put("Content-Type", Mimetypes.MIMETYPE_OCTET_STREAM);
            }
        }
        else {
            metadata.put(this.getRestHeaderPrefix() + "metadata-directive", "COPY");
        }

        boolean isExtraAclPutRequired = !prepareRESTHeaderAcl(metadata, acl);

        if(ifModifiedSince != null) {
            metadata.put(this.getRestHeaderPrefix() + "copy-source-if-modified-since",
                    ServiceUtils.formatRfc822Date(ifModifiedSince.getTime()));
            if(log.isDebugEnabled()) {
                log.debug("Only copy object if-modified-since:" + ifModifiedSince);
            }
        }
        if(ifUnmodifiedSince != null) {
            metadata.put(this.getRestHeaderPrefix() + "copy-source-if-unmodified-since",
                    ServiceUtils.formatRfc822Date(ifUnmodifiedSince.getTime()));
            if(log.isDebugEnabled()) {
                log.debug("Only copy object if-unmodified-since:" + ifUnmodifiedSince);
            }
        }
        if(ifMatchTags != null) {
            String tags = ServiceUtils.join(ifMatchTags, ",");
            metadata.put(this.getRestHeaderPrefix() + "copy-source-if-match", tags);
            if(log.isDebugEnabled()) {
                log.debug("Only copy object based on hash comparison if-match:" + tags);
            }
        }
        if(ifNoneMatchTags != null) {
            String tags = ServiceUtils.join(ifNoneMatchTags, ",");
            metadata.put(this.getRestHeaderPrefix() + "copy-source-if-none-match", tags);
            if(log.isDebugEnabled()) {
                log.debug("Only copy object based on hash comparison if-none-match:" + tags);
            }
        }

        HttpResponseAndByteCount methodAndByteCount = performRestPut(
                destinationBucketName, destinationObjectKey, metadata, null, null, false);

        CopyObjectResultHandler handler = getXmlResponseSaxParser()
                .parseCopyObjectResponse(
                        new HttpMethodReleaseInputStream(methodAndByteCount.getHttpResponse()));

        // Release HTTP connection manually. This should already have been done by the
        // HttpMethodReleaseInputStream class, but you can never be too sure...
        releaseConnection(methodAndByteCount.getHttpResponse());

        if(handler.isErrorResponse()) {
            throw new ServiceException(
                    "Copy failed: Code=" + handler.getErrorCode() +
                            ", Message=" + handler.getErrorMessage() +
                            ", RequestId=" + handler.getErrorRequestId() +
                            ", HostId=" + handler.getErrorHostId());
        }

        Map<String, Object> map = new HashMap<String, Object>();

        // Result fields returned when copy is successful.
        map.put("Last-Modified", handler.getLastModified());
        map.put("ETag", handler.getETag());

        // Include information about copy operation in result map, issue #192
        map.put("X-JetS3t-SourceBucketName", sourceBucketName);
        map.put("X-JetS3t-SourceObjectKey", sourceObjectKey);
        map.put("X-JetS3t-DestinationBucketName", destinationBucketName);
        map.put("X-JetS3t-DestinationObjectKey", destinationObjectKey);
        if (versionId != null) {
            map.put("X-JetS3t-VersionId", sourceBucketName);
        }
        if (destinationObjectStorageClass != null) {
            map.put("X-JetS3t-DestinationObjectStorageClass",
                destinationObjectStorageClass);
        }
        if (destinationObjectServerSideEncryptionAlgorithm != null) {
            map.put("X-JetS3t-DestinationObjectServerSideEncryptionAlgorithm",
                destinationObjectServerSideEncryptionAlgorithm);
        }

        // Include response headers in result map.
        map.putAll(convertHeadersToMap(methodAndByteCount.getHttpResponse().getAllHeaders()));
        map = ServiceUtils.cleanRestMetadataMap(
                map, this.getRestHeaderPrefix(), this.getRestMetadataPrefix());

        if(isExtraAclPutRequired) {
            if(log.isDebugEnabled()) {
                log.debug("Creating object with a non-canned ACL using REST, so an extra ACL Put is required");
            }
            putAclImpl(destinationBucketName, destinationObjectKey, acl, null);
        }

        return map;
    }

    @Override
    protected StorageObject getObjectDetailsImpl(String bucketName, String objectKey,
                                                 Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
                                                 String[] ifMatchTags, String[] ifNoneMatchTags, String versionId)
            throws ServiceException {
        return getObjectImpl(true, bucketName, objectKey,
                ifModifiedSince, ifUnmodifiedSince, ifMatchTags, ifNoneMatchTags, null, null,
                versionId);
    }

    @Override
    protected StorageObject getObjectImpl(String bucketName, String objectKey,
                                          Calendar ifModifiedSince, Calendar ifUnmodifiedSince,
                                          String[] ifMatchTags, String[] ifNoneMatchTags,
                                          Long byteRangeStart, Long byteRangeEnd, String versionId)
            throws ServiceException {
        return getObjectImpl(false, bucketName, objectKey, ifModifiedSince, ifUnmodifiedSince,
                ifMatchTags, ifNoneMatchTags, byteRangeStart, byteRangeEnd, versionId);
    }

    private StorageObject getObjectImpl(boolean headOnly, String bucketName, String objectKey,
                                        Calendar ifModifiedSince, Calendar ifUnmodifiedSince, String[] ifMatchTags,
                                        String[] ifNoneMatchTags, Long byteRangeStart, Long byteRangeEnd, String versionId)
            throws ServiceException {
        if(log.isDebugEnabled()) {
            log.debug("Retrieving " + (headOnly ? "Head" : "All")
                    + " information for bucket " + bucketName + " and object " + objectKey);
        }

        Map<String, Object> requestHeaders = new HashMap<String, Object>();
        Map<String, String> requestParameters = new HashMap<String, String>();

        if(ifModifiedSince != null) {
            requestHeaders.put("If-Modified-Since",
                    ServiceUtils.formatRfc822Date(ifModifiedSince.getTime()));
            if(log.isDebugEnabled()) {
                log.debug("Only retrieve object if-modified-since:" + ifModifiedSince);
            }
        }
        if(ifUnmodifiedSince != null) {
            requestHeaders.put("If-Unmodified-Since",
                    ServiceUtils.formatRfc822Date(ifUnmodifiedSince.getTime()));
            if(log.isDebugEnabled()) {
                log.debug("Only retrieve object if-unmodified-since:" + ifUnmodifiedSince);
            }
        }
        if(ifMatchTags != null) {
            String tags = ServiceUtils.join(ifMatchTags, ",");
            requestHeaders.put("If-Match", tags);
            if(log.isDebugEnabled()) {
                log.debug("Only retrieve object based on hash comparison if-match:" + tags);
            }
        }
        if(ifNoneMatchTags != null) {
            String tags = ServiceUtils.join(ifNoneMatchTags, ",");
            requestHeaders.put("If-None-Match", tags);
            if(log.isDebugEnabled()) {
                log.debug("Only retrieve object based on hash comparison if-none-match:" + tags);
            }
        }
        if(byteRangeStart != null || byteRangeEnd != null) {
            String range = "bytes="
                    + (byteRangeStart != null ? byteRangeStart.toString() : "")
                    + "-"
                    + (byteRangeEnd != null ? byteRangeEnd.toString() : "");
            requestHeaders.put("Range", range);
            if(log.isDebugEnabled()) {
                log.debug("Only retrieve object if it is within range:" + range);
            }
        }
        if(versionId != null) {
            requestParameters.put("versionId", versionId);
        }

        HttpResponse httpResponse;
        if(headOnly) {
            httpResponse = performRestHead(bucketName, objectKey, requestParameters, requestHeaders);
        }
        else {
            httpResponse = performRestGet(bucketName, objectKey, requestParameters, requestHeaders);
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(convertHeadersToMap(httpResponse.getAllHeaders()));

        StorageObject responseObject = newObject();
        responseObject.setKey(objectKey);
        if(!headOnly) {
            responseObject.setContentLength(httpResponse.getEntity().getContentLength());
        }
        responseObject.setBucketName(bucketName);
        responseObject.replaceAllMetadata(ServiceUtils.cleanRestMetadataMap(
                map, this.getRestHeaderPrefix(), this.getRestMetadataPrefix()));
        responseObject.setMetadataComplete(true); // Flag this object as having the complete metadata set.
        if(!headOnly) {
            HttpMethodReleaseInputStream releaseIS = new HttpMethodReleaseInputStream(httpResponse);
            responseObject.setDataInputStream(releaseIS);
        }
        else {
            // Release connection after HEAD (there's no response content)
            if(log.isDebugEnabled()) {
                log.debug("Releasing HttpMethod after HEAD");
            }
            releaseConnection(httpResponse);
        }

        return responseObject;
    }

    /**
     * Puts an object using a pre-signed PUT URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     * <p>
     * This operation does not required any S3 functionality as it merely
     * uploads the object by performing a standard HTTP PUT using the signed URL.
     *
     * @param signedPutUrl a signed PUT URL generated with
     *                     {@link org.jets3t.service.S3Service#createSignedPutUrl(String, String, java.util.Map, org.jets3t.service.security.ProviderCredentials, java.util.Date)}.
     * @param object       the object to upload, which must correspond to the object for which the URL was signed.
     *                     The object <b>must</b> have the correct content length set, and to apply a non-standard
     *                     ACL policy only the REST canned ACLs can be used
     *                     (eg {@link org.jets3t.service.acl.AccessControlList#REST_CANNED_PUBLIC_READ_WRITE}).
     * @return the S3Object put to S3. The S3Object returned will represent the object created in S3.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public S3Object putObjectWithSignedUrl(String signedPutUrl, S3Object object)
            throws ServiceException {
        HttpPut putMethod = new HttpPut(signedPutUrl);

        Map<String, Object> renamedMetadata = renameMetadataKeys(object.getMetadataMap());
        addMetadataToHeaders(putMethod, renamedMetadata);

        if(!object.containsMetadata("Content-Length")) {
            throw new IllegalStateException("Content-Length must be specified for objects put using signed PUT URLs");
        }

        RepeatableRequestEntity repeatableRequestEntity = null;

        // We do not need to calculate the data MD5 hash during upload if the
        // expected hash value was provided as the object's Content-MD5 header.
        boolean isLiveMD5HashingRequired = isLiveMD5HashingRequired(object);

        String s3Endpoint = this.getEndpoint();

        if(object.getDataInputStream() != null) {
            repeatableRequestEntity = new RepeatableRequestEntity(object.getKey(),
                    object.getDataInputStream(), object.getContentType(), object.getContentLength(),
                    getJetS3tProperties(), isLiveMD5HashingRequired);

            putMethod.setEntity(repeatableRequestEntity);
        }

        HttpResponse httpResponse = performRequest(putMethod, new int[]{200});

        // Consume response data and release connection.
        releaseConnection(httpResponse);
        try {
            object.closeDataInputStream();
        }
        catch(IOException e) {
            if(log.isWarnEnabled()) {
                log.warn("Unable to close data input stream for object '" + object.getKey() + "'", e);
            }
        }

        try {
            StorageObject uploadedObject = ServiceUtils.buildObjectFromUrl(
                    putMethod.getURI().getHost(),
                    putMethod.getURI().getRawPath(),
                    s3Endpoint);
            uploadedObject.setBucketName(uploadedObject.getBucketName());

            // Add all metadata returned by S3 to uploaded object.
            Map<String, Object> map = new HashMap<String, Object>();
            map.putAll(convertHeadersToMap(httpResponse.getAllHeaders()));
            uploadedObject.replaceAllMetadata(ServiceUtils.cleanRestMetadataMap(
                    map, this.getRestHeaderPrefix(), this.getRestMetadataPrefix()));

            // Confirm that the data was not corrupted in transit by checking S3's calculated
            // hash value with the locally computed value. This is only necessary if the user
            // did not provide a Content-MD5 header with the original object.
            // Note that we can only confirm the data if we used a RepeatableRequestEntity to
            // upload it, if the user did not provide a content length with the original
            // object we are SOL.
            if(repeatableRequestEntity != null && isLiveMD5HashingRequired) {
                // Obtain locally-calculated MD5 hash from request entity.
                String hexMD5OfUploadedData = ServiceUtils.toHex(
                        repeatableRequestEntity.getMD5DigestOfData());
                verifyExpectedAndActualETagValues(hexMD5OfUploadedData, uploadedObject);
            }

            return (S3Object) uploadedObject;
        }
        catch(UnsupportedEncodingException e) {
            throw new ServiceException("Unable to determine name of object created with signed PUT", e);
        }
    }

    /**
     * Deletes an object using a pre-signed DELETE URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     * <p>
     * This operation does not required any S3 functionality as it merely
     * deletes the object by performing a standard HTTP DELETE using the signed URL.
     *
     * @param signedDeleteUrl a signed DELETE URL generated with {@link org.jets3t.service.S3Service#createSignedDeleteUrl}.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public void deleteObjectWithSignedUrl(String signedDeleteUrl) throws ServiceException {
        HttpDelete deleteMethod = new HttpDelete(signedDeleteUrl);
        HttpResponse response = performRequest(deleteMethod, new int[]{204, 200});
        releaseConnection(response);
    }

    /**
     * Gets an object using a pre-signed GET URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     * <p>
     * This operation does not required any S3 functionality as it merely
     * uploads the object by performing a standard HTTP GET using the signed URL.
     *
     * @param signedGetUrl a signed GET URL generated with
     *                     {@link org.jets3t.service.S3Service#createSignedGetUrl(String, String, org.jets3t.service.security.ProviderCredentials, java.util.Date)}.
     * @return the S3Object in S3 including all metadata and the object's data input stream.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public S3Object getObjectWithSignedUrl(String signedGetUrl) throws ServiceException {
        return getObjectWithSignedUrlImpl(signedGetUrl, false);
    }

    /**
     * Gets an object's details using a pre-signed HEAD URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     * <p>
     * This operation does not required any S3 functionality as it merely
     * uploads the object by performing a standard HTTP HEAD using the signed URL.
     *
     * @param signedHeadUrl a signed HEAD URL generated with
     *                      {@link org.jets3t.service.S3Service#createSignedHeadUrl(String, String, org.jets3t.service.security.ProviderCredentials, java.util.Date)}.
     * @return the S3Object in S3 including all metadata, but without the object's data input stream.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public S3Object getObjectDetailsWithSignedUrl(String signedHeadUrl) throws ServiceException {
        return getObjectWithSignedUrlImpl(signedHeadUrl, true);
    }

    /**
     * Gets an object's ACL details using a pre-signed GET URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     *
     * @param signedAclUrl a signed URL generated with {@link org.jets3t.service.S3Service#createSignedUrl(String, String, String, String, java.util.Map, org.jets3t.service.security.ProviderCredentials, long, boolean)}.
     * @return the AccessControlList settings of the object in S3.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public AccessControlList getObjectAclWithSignedUrl(String signedAclUrl)
            throws ServiceException {
        HttpGet httpMethod = new HttpGet(signedAclUrl);

        HttpResponse httpResponse = performRequest(httpMethod, new int[]{200});
        return getXmlResponseSaxParser()
                .parseAccessControlListResponse(
                        new HttpMethodReleaseInputStream(httpResponse)).getAccessControlList();
    }

    /**
     * Sets an object's ACL details using a pre-signed PUT URL generated for that object.
     * This method is an implementation of the interface {@link org.jets3t.service.utils.signedurl.SignedUrlHandler}.
     *
     * @param signedAclUrl a signed URL generated with {@link org.jets3t.service.S3Service#createSignedUrl(String, String, String, String, java.util.Map, org.jets3t.service.security.ProviderCredentials, long, boolean)}.
     * @param acl          the ACL settings to apply to the object represented by the signed URL.
     * @throws org.jets3t.service.ServiceException
     *
     */
    public void putObjectAclWithSignedUrl(String signedAclUrl, AccessControlList acl) throws ServiceException {
        HttpPut putMethod = new HttpPut(signedAclUrl);

        if(acl != null) {
            String restHeaderAclValue = acl.getValueForRESTHeaderACL();
            if(restHeaderAclValue != null) {
                putMethod.addHeader(this.getRestHeaderPrefix() + "acl", restHeaderAclValue);
            }
            else {
                String aclAsXml = acl.toXml();
                putMethod.setEntity(new StringEntity(
                        aclAsXml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)));
            }
        }

        HttpResponse httpResponse = performRequest(putMethod, new int[]{200});

        // Consume response data and release connection.
        releaseConnection(httpResponse);
    }

    private S3Object getObjectWithSignedUrlImpl(String signedGetOrHeadUrl, boolean headOnly)
            throws ServiceException {
        String s3Endpoint = this.getEndpoint();

        HttpRequestBase httpMethod;
        if(headOnly) {
            httpMethod = new HttpHead(signedGetOrHeadUrl);
        }
        else {
            httpMethod = new HttpGet(signedGetOrHeadUrl);
        }

        HttpResponse httpResponse = performRequest(httpMethod, new int[]{200});

        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(convertHeadersToMap(httpResponse.getAllHeaders()));

        S3Object responseObject;
        try {
            responseObject = ServiceUtils.buildObjectFromUrl(
                    httpMethod.getURI().getHost(),
                    httpMethod.getURI().getRawPath().substring(1),
                    s3Endpoint);
        }
        catch(UnsupportedEncodingException e) {
            throw new ServiceException("Unable to determine name of object created with signed PUT", e);
        }

        responseObject.replaceAllMetadata(ServiceUtils.cleanRestMetadataMap(
                map, this.getRestHeaderPrefix(), this.getRestMetadataPrefix()));
        responseObject.setMetadataComplete(true); // Flag this object as having the complete metadata set.
        if(!headOnly) {
            HttpMethodReleaseInputStream releaseIS = new HttpMethodReleaseInputStream(httpResponse);
            responseObject.setDataInputStream(releaseIS);
        }
        else {
            // Release connection after HEAD (there's no response content)
            if(log.isDebugEnabled()) {
                log.debug("Releasing HttpMethod after HEAD");
            }
            releaseConnection(httpResponse);
        }

        return responseObject;
    }

    protected WebsiteConfig getWebsiteConfigImpl(String bucketName) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(this.isTargettingGoogleStorageService() ? "websiteConfig" : "website", "");

        HttpResponse getMethod = performRestGet(bucketName, null, requestParameters, null);
        return getXmlResponseSaxParser().parseWebsiteConfigurationResponse(
                new HttpMethodReleaseInputStream(getMethod));
    }

    protected void setWebsiteConfigImpl(String bucketName, WebsiteConfig config)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(this.isTargettingGoogleStorageService() ? "websiteConfig" : "website", "");

        Map<String, Object> metadata = new HashMap<String, Object>();

        String xml;
        try {
            xml = config.toXml();
        }
        catch(Exception e) {
            throw new ServiceException("Unable to build WebsiteConfig XML document", e);
        }

        performRestPut(bucketName, null, metadata, requestParameters,
                new StringEntity(xml, ContentType.create("text/plain", Constants.DEFAULT_ENCODING)),
                true);
    }

    protected void deleteWebsiteConfigImpl(String bucketName)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<String, String>();
        requestParameters.put(this.isTargettingGoogleStorageService() ? "websiteConfig" : "website", "");
        performRestDelete(bucketName, null, requestParameters, null, null);
    }

}