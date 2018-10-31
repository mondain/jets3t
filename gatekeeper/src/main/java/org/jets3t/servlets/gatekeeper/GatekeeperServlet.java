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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.servlets.gatekeeper.impl.DefaultAuthorizer;
import org.jets3t.servlets.gatekeeper.impl.DefaultBucketLister;
import org.jets3t.servlets.gatekeeper.impl.DefaultTransactionIdProvider;
import org.jets3t.servlets.gatekeeper.impl.DefaultUrlSigner;

/**
 * A servlet implementation of an S3 Gatekeeper, as described in the document
 * <a href="http://www.jets3t.org/applications/gatekeeper-concepts.html">
 * Gatekeeper Concepts</a>.
 * <p>
 * This servlet offers an easily configurable and extensible approach, where key
 * steps in the authorization and signature generation process are performed by pluggable
 * interfaces:
 * <ul>
 * <li>{@link TransactionIdProvider}: Generate a transaction ID to uniquely identify a
 * request/response transaction</li>
 * <li>{@link Authorizer}: Allow or deny specific requested operations</li>
 * <li>{@link UrlSigner}: Generate signed URLs for each operation that has been allowed by the
 * Authorizer</li>
 * </ul>
 * <p>
 * These pluggable interfaces are configured in the servlet's configuration file, or if left
 * unconfigured the default JetS3t implementations are used.
 * <p>
 * For more information about this servlet please refer to:
 * <a href="http://www.jets3t.org/applications/gatekeeper.html">
 * JetS3t Gatekeeper</a>
 *
 * @author James Murty
 */
public class GatekeeperServlet extends HttpServlet {
    private static final long serialVersionUID = 2054765427620529238L;

    private static final Log log = LogFactory.getLog(GatekeeperServlet.class);

    private ServletConfig servletConfig = null;

    private TransactionIdProvider transactionIdProvider = null;
    private UrlSigner urlSigner = null;
    private Authorizer authorizer = null;
    private BucketLister bucketLister = null;

    private boolean isInitCompleted = false;


    /**
     * Instantiates a class by locating and invoking the appropriate constructor.
     *
     * @param className
     * @param constructorParamClasses
     * @param constructorParams
     * @return
     */
    private Object instantiateClass(String className, Class[] constructorParamClasses,
        Object[] constructorParams) throws ServletException
    {
        try {
            Class myClass = Class.forName(className);
            Constructor constructor = myClass.getConstructor(constructorParamClasses);
            Object instance = constructor.newInstance(constructorParams);
            return instance;
        } catch (ClassNotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Class does not exist for name: " + className);
            }
        } catch (Exception e) {
            throw new ServletException("Unable to instantiate class '" + className + "'", e);
        }
        return null;
    }

    /**
     * Initialises the pluggable implementation classes for {@link Authorizer},
     * {@link TransactionIdProvider}, and {@link UrlSigner}
     */
    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        if (log.isInfoEnabled()) {
            log.info("Initialising GatekeeperServlet");
        }
        this.servletConfig = servletConfig;

        // Initialise required classes.
        transactionIdProvider = initTransactionIdProvider();
        authorizer = initAuthorizer();
        urlSigner = initUrlSigner();
        bucketLister = initBucketLister();
        isInitCompleted = true;
    }

    /**
     * Initialises the Authorizer implementation that will be used by the servlet.
     *
     * @return
     * @throws ServletException
     */
    private Authorizer initAuthorizer() throws ServletException {
        String authorizerClass = servletConfig.getInitParameter("AuthorizerClass");
        if (log.isDebugEnabled()) {
            log.debug("AuthorizerClass: " + authorizerClass);
        }
        if (authorizerClass != null) {
            if (log.isInfoEnabled()) {
                log.info("Loading Authorizer implementation class: " + authorizerClass);
            }
            return (Authorizer) instantiateClass(authorizerClass,
                new Class[] {ServletConfig.class}, new Object[] {servletConfig});
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded default Authorizer implementation class: "
                + DefaultAuthorizer.class.getName());
        }
        return new DefaultAuthorizer(servletConfig);
    }

    /**
     * Initialises the UrlSigner implementation that will be used by the servlet.
     *
     * @return
     * @throws ServletException
     */
    private UrlSigner initUrlSigner() throws ServletException {
        String urlSignerClass = servletConfig.getInitParameter("UrlSignerClass");
        if (log.isDebugEnabled()) {
            log.debug("UrlSignerClass: " + urlSignerClass);
        }
        if (urlSignerClass != null) {
            if (log.isInfoEnabled()) {
                log.info("Loading UrlSigner implementation class: " + urlSignerClass);
            }
            return (UrlSigner) instantiateClass(urlSignerClass,
                new Class[] {ServletConfig.class}, new Object[] {servletConfig});
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded default UrlSigner implementation class: "
                + DefaultUrlSigner.class.getName());
        }
        return new DefaultUrlSigner(servletConfig);
    }

    /**
     * Initialises the TransactionIdProvider implementation that will be used by the servlet.
     *
     * @return
     * @throws ServletException
     */
    private TransactionIdProvider initTransactionIdProvider() throws ServletException {
        String transactionIdProviderClass = servletConfig.getInitParameter("TransactionIdProviderClass");
        if (log.isDebugEnabled()) {
            log.debug("TransactionIdProviderClass: " + transactionIdProviderClass);
        }
        if (transactionIdProviderClass != null) {
            if (log.isInfoEnabled()) {
                log.info("Loading TransactionIdProvider implementation class: " + transactionIdProviderClass);
            }
            return (TransactionIdProvider) instantiateClass(transactionIdProviderClass,
                new Class[] {ServletConfig.class}, new Object[] {servletConfig});
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded default TransactionIdProvider implementation class: "
                + TransactionIdProvider.class.getName());
        }
        return new DefaultTransactionIdProvider(servletConfig);
    }

    /**
     * Initialises the BucketLister implementation that will be used by the servlet.
     *
     * @return
     * @throws ServletException
     */
    private BucketLister initBucketLister() throws ServletException {
        String bucketListerClass = servletConfig.getInitParameter("BucketListerClass");
        if (log.isDebugEnabled()) {
            log.debug("BucketListerClass: " + bucketListerClass);
        }
        if (bucketListerClass != null) {
            if (log.isInfoEnabled()) {
                log.info("Loading BucketLister implementation class: " + bucketListerClass);
            }
            return (BucketLister) instantiateClass(bucketListerClass,
                new Class[] {ServletConfig.class}, new Object[] {servletConfig});
        }
        if (log.isInfoEnabled()) {
            log.info("Loaded default BucketLister implementation class: "
                + TransactionIdProvider.class.getName());
        }
        return new DefaultBucketLister(servletConfig);
    }

    /**
     * Sends a simple HTML page in response to GET requests, indicating that the servlet is running.
     */
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Handling GET request");
        }
        response.setStatus(200);
        response.setContentType("text/html");
        response.getWriter().println("<html><head><title>JetS3t Gatekeeper</title><body>");
        response.getWriter().println("<p>JetS3t Gatekeeper is running " +
            (isInitCompleted? "and initialized successfully" : "but <b>initialization failed</b>")
            + "</p></body></html>");
    }

    /**
     * Handles POST requests that contain Gatekeeper messages encoded as POST form properties, and
     * sends a plain text response document containing the Gatekeeper response message encoded as
     * a properties file.
     */
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (log.isDebugEnabled()) {
            log.debug("Handling POST request");
        }
        try {
            // Build Gatekeeper request from POST form parameters.
            GatekeeperMessage gatekeeperMessage =
                GatekeeperMessage.decodeFromProperties(request.getParameterMap());

            // Obtain client information
            ClientInformation clientInformation = new ClientInformation(
                request.getRemoteAddr(), request.getRemoteHost(), request.getRemoteUser(),
                request.getRemotePort(), request.getSession(false), request.getUserPrincipal(),
                request.getHeader("User-Agent"), request);

            // Generate Transaction ID, and store it in the message.
            String transactionId = transactionIdProvider.getTransactionId(gatekeeperMessage, clientInformation);
            if (transactionId != null) {
                gatekeeperMessage.addMessageProperty(GatekeeperMessage.PROPERTY_TRANSACTION_ID, transactionId);
            }

            if (!isInitCompleted)
            {
                if (log.isWarnEnabled()) {
                    log.warn("Cannot process POST request as Gatekeeper servlet did not initialize correctly");
                }
                gatekeeperMessage.addApplicationProperty(
                        GatekeeperMessage.APP_PROPERTY_GATEKEEPER_ERROR_CODE, "GatekeeperInitializationError");
            } else if (gatekeeperMessage.getApplicationProperties().containsKey(
                    GatekeeperMessage.LIST_OBJECTS_IN_BUCKET_FLAG))
            {
                // Handle "limited listing" requests.
                if (log.isDebugEnabled()) {
                    log.debug("Listing objects");
                }
                boolean allowed = authorizer.allowBucketListingRequest(gatekeeperMessage, clientInformation);
                if (allowed) {
                    bucketLister.listObjects(gatekeeperMessage, clientInformation);
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Processing " + gatekeeperMessage.getSignatureRequests().length
                            + " object signature requests");
                }
                // Process each signature request.
                for (int i = 0; i < gatekeeperMessage.getSignatureRequests().length; i++) {
                    SignatureRequest signatureRequest = gatekeeperMessage.getSignatureRequests()[i];

                    // Determine whether the request will be allowed. If the request is not allowed, the
                    // reason will be made available in the signature request object (with signatureRequest.declineRequest())
                    boolean allowed = authorizer.allowSignatureRequest(gatekeeperMessage, clientInformation, signatureRequest);

                    // Sign requests when they are allowed. When a request is signed, the signed URL is made available
                    // in the SignatureRequest object.
                    if (allowed) {
                        String signedUrl = null;
                        if (SignatureRequest.SIGNATURE_TYPE_GET.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signGet(gatekeeperMessage, clientInformation, signatureRequest);
                        } else if (SignatureRequest.SIGNATURE_TYPE_HEAD.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signHead(gatekeeperMessage, clientInformation, signatureRequest);
                        } else if (SignatureRequest.SIGNATURE_TYPE_PUT.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signPut(gatekeeperMessage, clientInformation, signatureRequest);
                        } else if (SignatureRequest.SIGNATURE_TYPE_DELETE.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signDelete(gatekeeperMessage, clientInformation, signatureRequest);
                        } else if (SignatureRequest.SIGNATURE_TYPE_ACL_LOOKUP.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signGetAcl(gatekeeperMessage, clientInformation, signatureRequest);
                        } else if (SignatureRequest.SIGNATURE_TYPE_ACL_UPDATE.equals(signatureRequest.getSignatureType())) {
                            signedUrl = urlSigner.signPutAcl(gatekeeperMessage, clientInformation, signatureRequest);
                        }
                        signatureRequest.signRequest(signedUrl);
                    }
                }
            }

            // Build response as a set of properties, and return this document.
            Properties responseProperties = gatekeeperMessage.encodeToProperties();
            if (log.isDebugEnabled()) {
                log.debug("Sending response message as properties: " + responseProperties);
            }

            // Serialize properties to bytes.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            responseProperties.store(baos, "");

            // Send successful response.
            response.setStatus(200);
            response.setContentType("text/plain");
            response.getOutputStream().write(baos.toByteArray());
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error("Gatekeeper failed to send valid response", e);
            }
            response.setStatus(500);
            response.setContentType("text/plain");
            response.getWriter().println(e.toString());
        }
    }

}
