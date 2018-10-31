/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2014 James Murty
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
package org.jets3t.service.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.contrib.proxy.PluginProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.tsccm.AbstractConnPool;
import org.apache.http.impl.conn.tsccm.ConnPoolByRoute;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.impl.rest.httpclient.JetS3tRequestAuthorizer;
import org.jets3t.service.io.UnrecoverableIOException;

/**
 * Utilities useful for REST/HTTP S3Service implementations.
 *
 * @author James Murty
 */
public class RestUtils {

    private static final Log log = LogFactory.getLog(RestUtils.class);

    /**
     * A list of HTTP-specific header names, that may be present in S3Objects as metadata but
     * which should be treated as plain HTTP headers during transmission (ie not converted into
     * S3 Object metadata items). All items in this list are in lower case.
     * <p>
     * This list includes the items:
     * <table summary="Headers names treated as plain HTTP headers">
     * <tr><th>Unchanged metadata names</th></tr>
     * <tr><td>content-type</td></tr>
     * <tr><td>content-md5</td></tr>
     * <tr><td>content-length</td></tr>
     * <tr><td>content-language</td></tr>
     * <tr><td>expires</td></tr>
     * <tr><td>cache-control</td></tr>
     * <tr><td>content-disposition</td></tr>
     * <tr><td>content-encoding</td></tr>
     * </table>
     */
    public static final List<String> HTTP_HEADER_METADATA_NAMES = Arrays.asList(
            "content-type",
            "content-md5",
            "content-length",
            "content-language",
            "expires",
            "cache-control",
            "content-disposition",
            "content-encoding");

    /**
     * Encodes a URL string, and ensures that spaces are encoded as "%20" instead of "+" to keep
     * fussy web browsers happier.
     *
     * @param path
     * @return
     * encoded URL.
     */
    public static String encodeUrlString(String path) {
        String encodedPath = null;
        try {
            encodedPath = URLEncoder.encode(path, Constants.DEFAULT_ENCODING);
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        // Web browsers do not always handle '+' characters well, use the well-supported '%20' instead.
        encodedPath = encodedPath.replaceAll("\\+", "%20");
        // '@' character need not be URL encoded and Google Chrome balks on signed URLs if it is.
        encodedPath = encodedPath.replaceAll("%40", "@");
        return encodedPath;
    }

    /**
     * Encodes a URL string but leaves a delimiter string unencoded.
     * Spaces are encoded as "%20" instead of "+".
     *
     * @param path
     * @param delimiter
     * @return
     * encoded URL string.
     */
    public static String encodeUrlPath(String path, String delimiter) {
        final StringBuilder result = new StringBuilder();
        final StringTokenizer t = new StringTokenizer(path, delimiter);
        if(!t.hasMoreTokens()) {
            return path;
        }
        if(path.startsWith(delimiter)) {
            result.append(delimiter);
        }
        while(t.hasMoreTokens()) {
            result.append(encodeUrlString(t.nextToken()));
            if(t.hasMoreTokens()) {
                result.append(delimiter);
            }
        }
        if(path.endsWith(delimiter)) {
            result.append(delimiter);
        }
        return result.toString();
    }

    /**
     * Calculate the canonical string for a REST/HTTP request to a storage service.
     *
     * When expires is non-null, it will be used instead of the Date header.
     */
    public static String makeServiceCanonicalString(String method, String resource,
        Map<String, Object> headersMap, String expires, String headerPrefix,
        List<String> serviceResourceParameterNames)
    {
        StringBuilder canonicalStringBuf = new StringBuilder();
        canonicalStringBuf.append(method).append("\n");

        // Add all interesting headers to a list, then sort them.  "Interesting"
        // is defined as Content-MD5, Content-Type, Date, and x-amz-
        SortedMap<String, Object> interestingHeaders = new TreeMap<String, Object>();
        if (headersMap != null && headersMap.size() > 0) {
            for (Map.Entry<String, Object> entry: headersMap.entrySet()) {
                Object key = entry.getKey();
                Object value = entry.getValue();

                if (key == null) {
                    continue;
                }
                String lk = key.toString().toLowerCase(Locale.ENGLISH);

                // Ignore any headers that are not particularly interesting.
                if (lk.equals("content-type") || lk.equals("content-md5") || lk.equals("date") ||
                    lk.startsWith(headerPrefix))
                {
                    interestingHeaders.put(lk, value);
                }
            }
        }

        // Remove default date timestamp if "x-amz-date" or "x-goog-date" is set.
        if (interestingHeaders.containsKey(Constants.REST_METADATA_ALTERNATE_DATE_AMZ)
            || interestingHeaders.containsKey(Constants.REST_METADATA_ALTERNATE_DATE_GOOG)) {
          interestingHeaders.put("date", "");
        }

        // Use the expires value as the timestamp if it is available. This trumps both the default
        // "date" timestamp, and the "x-amz-date" header.
        if (expires != null) {
            interestingHeaders.put("date", expires);
        }

        // these headers require that we still put a new line in after them,
        // even if they don't exist.
        if (! interestingHeaders.containsKey("content-type")) {
            interestingHeaders.put("content-type", "");
        }
        if (! interestingHeaders.containsKey("content-md5")) {
            interestingHeaders.put("content-md5", "");
        }

        // Finally, add all the interesting headers (i.e.: all that start with x-amz- ;-))
        for (Map.Entry<String, Object> entry: interestingHeaders.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith(headerPrefix)) {
                canonicalStringBuf.append(key).append(':').append(value);
            } else {
                canonicalStringBuf.append(value);
            }
            canonicalStringBuf.append("\n");
        }

        // don't include the query parameters...
        int queryIndex = resource.indexOf('?');
        if (queryIndex == -1) {
            canonicalStringBuf.append(resource);
        } else {
            canonicalStringBuf.append(resource.substring(0, queryIndex));
        }

        // ...unless the parameter(s) are in the set of special params
        // that actually identify a service resource.
        if (queryIndex >= 0) {
            SortedMap<String, String> sortedResourceParams = new TreeMap<String, String>();

            // Parse parameters from resource string
            String query = resource.substring(queryIndex + 1);
            for (String paramPair: query.split("&")) {
                String[] paramNameValue = paramPair.split("=");
                try {
                    String name = URLDecoder.decode(paramNameValue[0], "UTF-8");
                    String value = null;
                    if (paramNameValue.length > 1) {
                        value = URLDecoder.decode(paramNameValue[1], "UTF-8");
                    }
                    // Only include parameter (and its value if present) in canonical
                    // string if it is a resource-identifying parameter
                    if (serviceResourceParameterNames.contains(name)) {
                        sortedResourceParams.put(name, value);
                    }
                }
                catch(UnsupportedEncodingException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }

            // Add resource parameters
            if (sortedResourceParams.size() > 0) {
                canonicalStringBuf.append("?");
            }
            boolean addedParam = false;
            for (Map.Entry<String, String> entry: sortedResourceParams.entrySet()) {
                if (addedParam) {
                    canonicalStringBuf.append("&");
                }
                canonicalStringBuf.append(entry.getKey());
                if (entry.getValue() != null) {
                    canonicalStringBuf.append("=").append(entry.getValue());
                }
                addedParam = true;
            }
        }

        return canonicalStringBuf.toString();
    }

    /**
     * Initialises, or re-initialises, the underlying HttpConnectionManager and
     * HttpClient objects a service will use to communicate with an AWS service.
     * If proxy settings are specified in this service's {@link Jets3tProperties} object,
     * these settings will also be passed on to the underlying objects.
     */
    public static HttpClient initHttpConnection(
            final JetS3tRequestAuthorizer requestAuthorizer,
            Jets3tProperties jets3tProperties,
            String userAgentDescription,
            CredentialsProvider credentialsProvider) {
        // Configure HttpClient properties based on Jets3t Properties.
        HttpParams params = createDefaultHttpParams();
        params.setParameter(Jets3tProperties.JETS3T_PROPERTIES_ID, jets3tProperties);

        params.setParameter(
            ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME,
            jets3tProperties.getStringProperty(
                ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME,
                ConnManagerFactory.class.getName()));

        HttpConnectionParams.setConnectionTimeout(params,
            jets3tProperties.getIntProperty("httpclient.connection-timeout-ms", 60000));
        HttpConnectionParams.setSoTimeout(params,
            jets3tProperties.getIntProperty("httpclient.socket-timeout-ms", 60000));
        HttpConnectionParams.setStaleCheckingEnabled(params,
            jets3tProperties.getBoolProperty("httpclient.stale-checking-enabled", true));

        // Connection properties to take advantage of S3 window scaling.
        if (jets3tProperties.containsKey("httpclient.socket-receive-buffer")) {
            HttpConnectionParams.setSocketBufferSize(params,
                jets3tProperties.getIntProperty("httpclient.socket-receive-buffer", 0));
        }

        HttpConnectionParams.setTcpNoDelay(params, true);

        // Set user agent string.
        String userAgent = jets3tProperties.getStringProperty("httpclient.useragent", null);
        if (userAgent == null) {
            userAgent = ServiceUtils.getUserAgentDescription(userAgentDescription);
        }
        if (log.isDebugEnabled()) {
            log.debug("Setting user agent string: " + userAgent);
        }
        HttpProtocolParams.setUserAgent(params, userAgent);

        boolean expectContinue
                = jets3tProperties.getBoolProperty("http.protocol.expect-continue", true);
        HttpProtocolParams.setUseExpectContinue(params, expectContinue);

        long connectionManagerTimeout
                = jets3tProperties.getLongProperty("httpclient.connection-manager-timeout", 0);
        ConnManagerParams.setTimeout(params, connectionManagerTimeout);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        httpClient.setHttpRequestRetryHandler(
            new JetS3tRetryHandler(
                jets3tProperties.getIntProperty("httpclient.retry-max", 5), requestAuthorizer));

        if (credentialsProvider != null) {
            if (log.isDebugEnabled()) {
                log.debug("Using credentials provider class: "
                        + credentialsProvider.getClass().getName());
            }
            httpClient.setCredentialsProvider(credentialsProvider);
            if (jets3tProperties.getBoolProperty(
                    "httpclient.authentication-preemptive",
                    false)) {
                // Add as the very first interceptor in the protocol chain
                httpClient.addRequestInterceptor(new PreemptiveInterceptor(), 0);
            }
        }

        return httpClient;
    }

    /**
     * Initialises this service's HTTP proxy by auto-detecting the proxy settings.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties) {
        initHttpProxy(httpClient, jets3tProperties, true, null, -1, null, null, null);
    }

    /**
     * Initialises this service's HTTP proxy by auto-detecting the proxy settings using the given endpoint.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties,
        String endpoint) {
        initHttpProxy(httpClient, jets3tProperties, true, null, -1, null, null, null, endpoint);
    }

    /**
     * Initialises this service's HTTP proxy with the given proxy settings.
     *
     * @param proxyHostAddress
     * @param proxyPort
     */
    public static void initHttpProxy(HttpClient httpClient, String proxyHostAddress,
        int proxyPort, Jets3tProperties jets3tProperties) {
        initHttpProxy(httpClient, jets3tProperties, false,
            proxyHostAddress, proxyPort, null, null, null);
    }

    /**
     * Initialises this service's HTTP proxy for authentication using the given
     * proxy settings.
     *
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     * if a proxy domain is provided, an {@link NTCredentials} credential provider
     * will be used. If the proxy domain is null, a
     * {@link UsernamePasswordCredentials} credentials provider will be used.
     */
    public static void initHttpProxy(HttpClient httpClient, Jets3tProperties jets3tProperties,
        String proxyHostAddress, int proxyPort, String proxyUser,
        String proxyPassword, String proxyDomain)
    {
        initHttpProxy(httpClient, jets3tProperties, false,
            proxyHostAddress, proxyPort, proxyUser, proxyPassword, proxyDomain);
    }

    /**
     * @param httpClient
     * @param proxyAutodetect
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     */
    public static void initHttpProxy(HttpClient httpClient,
        Jets3tProperties jets3tProperties, boolean proxyAutodetect,
        String proxyHostAddress, int proxyPort, String proxyUser,
        String proxyPassword, String proxyDomain)
    {
        String s3Endpoint = jets3tProperties.getStringProperty(
                "s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
        initHttpProxy(httpClient, jets3tProperties, proxyAutodetect, proxyHostAddress, proxyPort,
            proxyUser, proxyPassword, proxyDomain, s3Endpoint);
    }

    /**
     * @param httpClient
     * @param proxyAutodetect
     * @param proxyHostAddress
     * @param proxyPort
     * @param proxyUser
     * @param proxyPassword
     * @param proxyDomain
     * @param endpoint
     */
    public static void initHttpProxy(
            HttpClient httpClient,
            Jets3tProperties jets3tProperties,
            boolean proxyAutodetect,
            String proxyHostAddress,
            int proxyPort,
            String proxyUser,
            String proxyPassword,
            String proxyDomain,
            String endpoint) {

        // Use explicit proxy settings, if available.
        if (proxyHostAddress != null && proxyPort != -1) {
            if (log.isInfoEnabled()) {
                log.info("Using Proxy: " + proxyHostAddress + ":" + proxyPort);
            }

            HttpHost proxy = new HttpHost(proxyHostAddress, proxyPort);
            httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                    proxy);
            /*
             * TODO: Use alternative method?
             * Alternate method based on JRE standard
             *
            ProxySelectorRoutePlanner routePlanner = new ProxySelectorRoutePlanner(
                    httpClient.getConnectionManager().getSchemeRegistry(),
                    ProxySelector.getDefault());
            ((DefaultHttpClient)httpClient).setRoutePlanner(routePlanner);
            */

            if (proxyUser != null && !proxyUser.trim().equals("")
                    && httpClient instanceof AbstractHttpClient) {
                if (proxyDomain != null) {
                    ((AbstractHttpClient) httpClient).getCredentialsProvider()
                            .setCredentials(new AuthScope(
                                    proxyHostAddress,
                                    proxyPort),
                                    new NTCredentials(
                                            proxyUser,
                                            proxyPassword,
                                            proxyHostAddress,
                                            proxyDomain));
                } else {
                    ((AbstractHttpClient) httpClient).getCredentialsProvider()
                            .setCredentials(new AuthScope(
                                    proxyHostAddress,
                                    proxyPort),
                                    new UsernamePasswordCredentials(proxyUser,
                                            proxyPassword));
                }
            }
        }
        // If no explicit settings are available, try autodetecting proxies (unless autodetect is disabled)
        else if (proxyAutodetect) {
            // Try to detect any proxy settings from applet.
            HttpHost proxyHost = null;
            try {
                proxyHost = PluginProxyUtil.detectProxy(
                        new URL("http://" + endpoint));
                if (proxyHost != null) {
                    if (log.isInfoEnabled()) {
                        log.info("Using Proxy: " + proxyHost.getHostName()
                                + ":" + proxyHost.getPort());
                    }
                    httpClient.getParams()
                            .setParameter(ConnRoutePNames.DEFAULT_PROXY,
                                    proxyHost);
                }
            } catch (Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Unable to set proxy configuration", t);
                }
            }
        }
    }

    /**
     * Calculates and returns a time offset value to reflect the time difference
     * between your computer's clock and the current time according to the 'Date'
     * header in the given HTTP response, likely provided by a service endpoint
     * whose time you wish to treat as authoritative.
     *
     * Ideally you should not rely on this method to overcome clock-related
     * disagreements between your computer and a service endpoint.
     * If you computer is set to update its clock periodically and has the
     * correct timezone setting you should never have to resort to this work-around.
     *
     * @throws ParseException
     */
    public static long calculateTimeAdjustmentOffset(HttpResponse response)
        throws ParseException
    {
        Header[] dateHeaders = response.getHeaders("Date");
        if (dateHeaders.length > 0) {
            // Retrieve the service time according to response Date header
            String dateHeader = dateHeaders[0].getValue();
            Date awsTime = ServiceUtils.parseRfc822Date(dateHeader);
            // Calculate the difference between the current time according to AWS,
            // and the current time according to your computer's clock.
            Date localTime = new Date();
            long timeOffset = awsTime.getTime() - localTime.getTime();

            if (log.isDebugEnabled()) {
                log.debug("Calculated time offset value of " + timeOffset
                    + " milliseconds between the local machine and the response: "
                    + response);
            }
            return timeOffset;
        } else {
            if (log.isWarnEnabled()) {
                log.warn("Unable to calculate value of time offset between the "
                    + "local machine and the response: " + response);
            }
            return 0l;
        }
    }

    public static Map<String, String> convertHeadersToMap(Header[] headers) {
        Map<String, String> s3Headers = new HashMap<String, String>();
        for (Header header: headers) {
            s3Headers.put(header.getName(), header.getValue());
        }
        return s3Headers;
    }

    /**
     * Default Http parameters got from the DefaultHttpClient implementation.
     *
     * @return
     * Default HTTP connection parameters
     */
    public static HttpParams createDefaultHttpParams() {
        HttpParams params = new SyncBasicHttpParams();
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(params,
                HTTP.DEFAULT_CONTENT_CHARSET);
        HttpConnectionParams.setTcpNoDelay(params, true);
        HttpConnectionParams.setSocketBufferSize(params, 8192);
        return params;
    }

    /**
     * A ClientConnectionManagerFactory that creates ThreadSafeClientConnManager
     */
    public static class ConnManagerFactory implements
            ClientConnectionManagerFactory {
        /*
         * @see ClientConnectionManagerFactory#newInstance(HttpParams, SchemeRegistry)
         */
        public ClientConnectionManager newInstance(HttpParams params,
                SchemeRegistry schemeRegistry) {
            return new ThreadSafeConnManager(params, schemeRegistry);
        }

    } //ConnManagerFactory

    /**
     * ThreadSafeConnManager is a ThreadSafeClientConnManager configured via
     * jets3tProperties.
     *
     * @see Jets3tProperties#JETS3T_PROPERTIES_ID
     */
    public static class ThreadSafeConnManager extends
            ThreadSafeClientConnManager {
        public ThreadSafeConnManager(final HttpParams params,
                final SchemeRegistry schreg) {
            super(params, schreg);
        }

        @Override
        protected AbstractConnPool createConnectionPool(final HttpParams params) {
            // Set the maximum connections per host for the HTTP connection manager,
            // *and* also set the maximum number of total connections (new in 0.7.1).
            // The max connections per host setting is made the same value as the max
            // global connections if there is no per-host property.
            Jets3tProperties props = (Jets3tProperties) params.getParameter(
                    Jets3tProperties.JETS3T_PROPERTIES_ID);
            int maxConn = 20;
            int maxConnectionsPerHost = 0;
            if (props != null) {
                maxConn = props.getIntProperty("httpclient.max-connections", 20);
                maxConnectionsPerHost = props.getIntProperty(
                        "httpclient.max-connections-per-host",
                        0);
            }
            if (maxConnectionsPerHost == 0) {
                maxConnectionsPerHost = maxConn;
            }
            connPerRoute.setDefaultMaxPerRoute(maxConnectionsPerHost);
            return new ConnPoolByRoute(connOperator, connPerRoute, maxConn,props.getLongProperty("httpclient.connection.ttl", -1L), TimeUnit.MILLISECONDS);
        }
    } //ThreadSafeConnManager

    public static class JetS3tRetryHandler extends DefaultHttpRequestRetryHandler {
        private final JetS3tRequestAuthorizer requestAuthorizer;

        public JetS3tRetryHandler(int pRetryMaxCount, JetS3tRequestAuthorizer requestAuthorizer) {
            super(pRetryMaxCount, false);
            this.requestAuthorizer = requestAuthorizer;
        }

        @Override
        public boolean retryRequest(IOException exception,
                int executionCount,
                HttpContext context) {
            if (super.retryRequest(exception, executionCount, context)){

                if (exception instanceof UnrecoverableIOException) {
                    if (log.isDebugEnabled()) {
                        log.debug("Deliberate interruption, will not retry");
                    }
                    return false;
                }
                HttpRequest request = (HttpRequest) context.getAttribute(
                        ExecutionContext.HTTP_REQUEST);

                // Convert RequestWrapper to original HttpBaseRequest (issue #127)
                if (request instanceof RequestWrapper) {
                    request = ((RequestWrapper)request).getOriginal();
                }

                if (!(request instanceof HttpRequestBase)) {
                    return false;
                }
                HttpRequestBase method = (HttpRequestBase) request;

                // Release underlying connection so we will get a new one (hopefully) when we retry.
                HttpConnection conn = (HttpConnection) context.getAttribute(
                        ExecutionContext.HTTP_CONNECTION);
                try {
                    conn.close();
                } catch (Exception e) {
                    //ignore
                }

                if (log.isDebugEnabled()) {
                    log.debug("Retrying " + method.getMethod()
                            + " request with path '" + method.getURI()
                            + "' - attempt " + executionCount + " of "
                            + getRetryCount());
                }

                // Build the authorization string for the method.
                try {
                    if (requestAuthorizer != null){
                        requestAuthorizer.authorizeHttpRequest(method, context, null);
                    }
                    return true; // request OK'd for retry by base handler and myself
                } catch (Exception e) {
                    if (log.isWarnEnabled()) {
                        log.warn("Unable to generate updated authorization string for retried request",
                                e);
                    }
                }
            }

            return false;
        }
    } //AWSRetryHandler

    /**
     * PreemptiveInterceptor
     */
    // A preemptive interceptor (copied from doc).
    private static class PreemptiveInterceptor implements
            HttpRequestInterceptor {

        public void process(final HttpRequest request, final HttpContext context) {
            AuthState authState = (AuthState) context.getAttribute(
                    ClientContext.TARGET_AUTH_STATE);
            CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(
                    ClientContext.CREDS_PROVIDER);
            HttpHost targetHost = (HttpHost) context.getAttribute(
                    ExecutionContext.HTTP_TARGET_HOST);
            // If not auth scheme has been initialized yet
            if (authState.getAuthScheme() == null) {
                AuthScope authScope = new AuthScope(targetHost.getHostName(),
                        targetHost.getPort());
                // Obtain credentials matching the target host
                Credentials creds = credsProvider.getCredentials(authScope);
                // If found, generate BasicScheme preemptively
                if (creds != null) {
                    authState.setAuthScheme(new BasicScheme());
                    authState.setCredentials(creds);
                }
            }
        }
    } //PreemptiveInterceptor

    public static String httpGetUrlAsString(String uri)
            throws ClientProtocolException, IOException
    {
        HttpUriRequest getMethod = new HttpGet(uri);
        HttpClient client = new DefaultHttpClient();
        HttpEntity entity = client.execute(getMethod).getEntity();

        String contentEncoding = "UTF-8";  // Default
        if (entity.getContentEncoding() != null
            && entity.getContentEncoding().getValue() != null)
        {
            contentEncoding = entity.getContentEncoding().getValue();
        }

        String dataString = ServiceUtils.readInputStreamToString(
            entity.getContent(), contentEncoding);

        EntityUtils.consume(entity);

        return dataString;
    }

}
