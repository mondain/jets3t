/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008-2010 James Murty
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
package org.jets3t.service.impl.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.model.cloudfront.*;
import org.jets3t.service.model.cloudfront.CacheBehavior.ViewerProtocolPolicy;
import org.jets3t.service.utils.ServiceUtils;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * XML Sax parser to read XML documents returned by the CloudFront service via
 * the REST interface, and convert these documents into JetS3t objects.
 *
 * @author James Murty
 */
public class CloudFrontXmlResponsesSaxParser {
    private static final Log log = LogFactory.getLog(CloudFrontXmlResponsesSaxParser.class);

    private XMLReader xr = null;
    private Jets3tProperties properties = null;

    /**
     * Constructs the XML SAX parser.
     *
     * @param properties the JetS3t properties that will be applied when parsing XML documents.
     * @throws S3ServiceException
     */
    public CloudFrontXmlResponsesSaxParser(Jets3tProperties properties) throws ServiceException {
        this.properties = properties;
        this.xr = ServiceUtils.loadXMLReader();
    }

    public Jets3tProperties getProperties() {
        return this.properties;
    }

    /**
     * Parses an XML document from an input stream using a document handler.
     *
     * @param handler     The handler for the XML document
     * @param inputStream An input stream containing the XML document to parse
     * @throws CloudFrontServiceException Any parsing, IO or other exceptions are wrapped in an S3ServiceException.
     */
    protected void parseXmlInputStream(DefaultHandler handler, InputStream inputStream)
            throws CloudFrontServiceException {
        try {
            if(log.isDebugEnabled()) {
                log.debug("Parsing XML response document with handler: " + handler.getClass());
            }
            BufferedReader breader = new BufferedReader(new InputStreamReader(inputStream,
                    Constants.DEFAULT_ENCODING));
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.parse(new InputSource(breader));
        }
        catch(Exception t) {
            try {
                inputStream.close();
            }
            catch(IOException e) {
                if(log.isErrorEnabled()) {
                    log.error("Unable to close response InputStream up after XML parse failure", e);
                }
            }
            throw new CloudFrontServiceException("Failed to parse XML document with handler "
                    + handler.getClass(), t);
        }
    }

    /**
     * Parses a ListBucket response XML document from an input stream.
     *
     * @param inputStream XML data input stream.
     * @return the XML handler object populated with data parsed from the XML stream.
     * @throws CloudFrontServiceException
     */
    public DistributionListHandler parseDistributionListResponse(InputStream inputStream)
            throws CloudFrontServiceException {
        DistributionListHandler handler = new DistributionListHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public DistributionHandler parseDistributionResponse(InputStream inputStream)
            throws CloudFrontServiceException {
        DistributionHandler handler = new DistributionHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public DistributionConfigHandler parseDistributionConfigResponse(InputStream inputStream)
            throws CloudFrontServiceException {
        DistributionConfigHandler handler = new DistributionConfigHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public OriginAccessIdentityHandler parseOriginAccessIdentity(
            InputStream inputStream) throws CloudFrontServiceException {
        OriginAccessIdentityHandler handler = new OriginAccessIdentityHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public OriginAccessIdentityConfigHandler parseOriginAccessIdentityConfig(
            InputStream inputStream) throws CloudFrontServiceException {
        OriginAccessIdentityConfigHandler handler = new OriginAccessIdentityConfigHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public OriginAccessIdentityListHandler parseOriginAccessIdentityListResponse(
            InputStream inputStream) throws CloudFrontServiceException {
        OriginAccessIdentityListHandler handler = new OriginAccessIdentityListHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public InvalidationHandler parseInvalidationResponse(
            InputStream inputStream) throws CloudFrontServiceException {
        InvalidationHandler handler = new InvalidationHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public InvalidationListHandler parseInvalidationListResponse(
            InputStream inputStream) throws CloudFrontServiceException {
        InvalidationListHandler handler = new InvalidationListHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    public ErrorHandler parseErrorResponse(InputStream inputStream)
            throws CloudFrontServiceException {
        ErrorHandler handler = new ErrorHandler(xr);
        parseXmlInputStream(handler, inputStream);
        return handler;
    }

    // ////////////
    // Handlers //
    // ////////////

    public class DistributionHandler extends SimpleHandler {
        private Distribution distribution = null;

        private String id = null;
        private String status = null;
        private Date lastModifiedTime = null;
        private String domainName = null;
        private final Map<String, List<String>> activeTrustedSigners =
                new HashMap<String, List<String>>();

        private boolean inSignerElement;
        private String lastSignerIdentifier = null;

        public DistributionHandler(XMLReader xr) {
            super(xr);
        }

        public Distribution getDistribution() {
            return distribution;
        }

        public void endId(String text) {
            this.id = text;
        }

        public void endStatus(String text) {
            this.status = text;
        }

        public void endLastModifiedTime(String text) throws ParseException {
            this.lastModifiedTime = ServiceUtils.parseIso8601Date(text);
        }

        public void endDomainName(String text) {
            this.domainName = text;
        }

        // Handle ActiveTrustedSigner elements //
        public void startSigner() {
            inSignerElement = true;
        }

        public void endSigner(String text) {
            inSignerElement = false;
            lastSignerIdentifier = null;
        }

        public void endSelf(String text) {
            if(inSignerElement) {
                lastSignerIdentifier = "Self";
            }
        }

        public void endAwsAccountNumber(String text) {
            if(inSignerElement) {
                lastSignerIdentifier = text;
            }
        }

        public void endKeyPairId(String text) {
            if(inSignerElement) {
                List<String> keypairIdList = activeTrustedSigners.get(lastSignerIdentifier);
                if(keypairIdList == null) {
                    keypairIdList = new ArrayList<String>();
                    activeTrustedSigners.put(lastSignerIdentifier, keypairIdList);
                }
                keypairIdList.add(text);
            }
        }
        // End handle ActiveTrustedSigner elements //

        public void startDistributionConfig() {
            transferControlToHandler(new DistributionConfigHandler(xr));
        }

        public void startStreamingDistributionConfig() {
            transferControlToHandler(new DistributionConfigHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            DistributionConfig config =
                    ((DistributionConfigHandler) childHandler).getDistributionConfig();
            if(config instanceof StreamingDistributionConfig) {
                this.distribution = new StreamingDistribution(id, status,
                        lastModifiedTime, domainName, activeTrustedSigners, config);
            }
            else {
                this.distribution = new Distribution(id, status,
                        lastModifiedTime, 0L, domainName, activeTrustedSigners, config);
            }
        }

        // End of a normal Distribution
        public void endDistribution(String text) {
            returnControlToParentHandler();
        }

        // End of a StreamingDistribution
        public void endStreamingDistribution(String text) {
            returnControlToParentHandler();
        }
    }

    public class LoggingStatusHandler extends SimpleHandler {
        private LoggingStatus loggingStatus = new LoggingStatus(false, null, null);

        public LoggingStatusHandler(XMLReader xr) {
            super(xr);
        }

        public LoggingStatus getLoggingStatus() {
            return loggingStatus;
        }

        public void endEnabled(String text) {
            loggingStatus.setEnabled(String.valueOf(true).equals(text));
        }

        public void endBucket(String text) {
            loggingStatus.setBucket(text);
        }

        public void endPrefix(String text) {
            loggingStatus.setPrefix(text);
        }

        public void endLogging(String text) {
            returnControlToParentHandler();
        }
    }

    public class TrustedSignersHandler extends SimpleHandler {
        private List<String> trustedSigners = new ArrayList<String>();

        public TrustedSignersHandler(XMLReader xr) {
            super(xr);
        }

        public String[] getTrustedSigners() {
            return trustedSigners.toArray(new String[trustedSigners.size()]);
        }

        public void endAwsAccountNumber(String text) {
            trustedSigners.add(text);
        }

        public void endTrustedSigners(String text) {
            returnControlToParentHandler();
        }
    }

    public class CacheBehaviorHandler extends SimpleHandler {
        private CacheBehavior cacheBehavior = new CacheBehavior();

        public CacheBehaviorHandler(XMLReader xr) {
            super(xr);
        }

        public CacheBehavior getCacheBehavior() {
            return cacheBehavior;
        }

        public void endPathPattern(String text) {
            cacheBehavior.setPathPattern(text);
        }

        public void endTargetOriginId(String text) {
            cacheBehavior.setTargetOriginId(text);
        }

        public void endQueryString(String text) {
            cacheBehavior.setIsForwardQueryString(String.valueOf(true).equals(text));
        }

        public void endViewerProtocolPolicy(String text) {
            cacheBehavior.setViewerProtocolPolicy(ViewerProtocolPolicy.fromText(text));
        }

        public void endMinTTL(String text) {
            cacheBehavior.setMinTTL(new Long(text));
        }

        public void startTrustedSigners() {
            transferControlToHandler(new TrustedSignersHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            cacheBehavior.setTrustedSignerAwsAccountNumbers(
                    ((TrustedSignersHandler) childHandler).getTrustedSigners());
        }

        public void endDefaultCacheBehavior(String text) {
            returnControlToParentHandler();
        }

        public void endCacheBehavior(String text) {
            returnControlToParentHandler();
        }
    }

    public class DistributionConfigHandler extends SimpleHandler {
        private DistributionConfig distributionConfig = null;

        private String callerReference = null;
        private List<Origin> origins = new ArrayList<Origin>();
        private final List<String> cnamesList = new ArrayList<String>();
        private String comment = null;
        private boolean enabled = false;
        private LoggingStatus loggingStatus = null;
        private String defaultRootObject = null;
        private CacheBehavior defaultCacheBehavior;
        private List<CacheBehavior> cacheBehaviors = new ArrayList<CacheBehavior>();
        List<String> trustedSignerAwsAccountNumberList = new ArrayList<String>();

        private boolean inDefaultCacheBehavior = false;

        public DistributionConfigHandler(XMLReader xr) {
            super(xr);
        }

        public DistributionConfig getDistributionConfig() {
            return distributionConfig;
        }

        public void endCallerReference(String text) {
            this.callerReference = text;
        }

        public void endCNAME(String text) {
            this.cnamesList.add(text);
        }

        public void endDefaultRootObject(String text) {
            this.defaultRootObject = text;
        }

        public void endComment(String text) {
            this.comment = text;
        }

        public void endEnabled(String text) {
            this.enabled = String.valueOf(true).equalsIgnoreCase(text);
        }

        public void startOrigin() {
            // For DistributionConfig
            transferControlToHandler(new OriginHandler(xr));
        }

        public void startS3Origin() {
            // For StreamingDistributionConfig
            transferControlToHandler(new OriginHandler(xr));
        }

        public void startDefaultCacheBehavior() {
            inDefaultCacheBehavior = true;
            transferControlToHandler(new CacheBehaviorHandler(xr));
        }

        public void startCacheBehavior() {
            inDefaultCacheBehavior = false;
            transferControlToHandler(new CacheBehaviorHandler(xr));
        }

        public void startLogging() {
            transferControlToHandler(new LoggingStatusHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            if(childHandler instanceof OriginHandler) {
                this.origins.add(((OriginHandler) childHandler).origin);
            }
            else if(childHandler instanceof CacheBehaviorHandler) {
                if(inDefaultCacheBehavior) {
                    this.defaultCacheBehavior = ((CacheBehaviorHandler) childHandler).cacheBehavior;
                }
                else {
                    this.cacheBehaviors.add(((CacheBehaviorHandler) childHandler).cacheBehavior);
                }
            }
            else if(childHandler instanceof LoggingStatusHandler) {
                this.loggingStatus = ((LoggingStatusHandler) childHandler).loggingStatus;
            }
        }

        public void endDistributionConfig(String text) {
            this.distributionConfig = new DistributionConfig(
                    origins.toArray(new Origin[origins.size()]),
                    callerReference,
                    cnamesList.toArray(new String[cnamesList.size()]),
                    comment, enabled, loggingStatus, defaultRootObject,
                    defaultCacheBehavior,
                    cacheBehaviors.toArray(new CacheBehavior[cacheBehaviors.size()]));
            returnControlToParentHandler();
        }

        public void endStreamingDistributionConfig(String text) {
            this.distributionConfig = new StreamingDistributionConfig(
                    origins.toArray(new Origin[origins.size()]),
                    callerReference,
                    cnamesList.toArray(new String[cnamesList.size()]), comment,
                    enabled, loggingStatus,
                    trustedSignerAwsAccountNumberList.toArray(
                            new String[trustedSignerAwsAccountNumberList.size()]));
            returnControlToParentHandler();
        }
    }

    public class DistributionSummaryHandler extends SimpleHandler {
        private Distribution distribution = null;

        private String id = null;
        private String status = null;
        private Date lastModifiedTime = null;
        private Long inProgressInvalidationBatches = 0l;
        private String domainName = null;
        private List<Origin> origins = new ArrayList<Origin>();
        private final List<String> cnamesList = new ArrayList<String>();
        private String comment = null;
        private boolean enabled = false;
        private CacheBehavior defaultCacheBehavior = null;
        private List<CacheBehavior> cacheBehaviors = new ArrayList<CacheBehavior>();

        private boolean inDefaultCacheBehavior = false;

        public DistributionSummaryHandler(XMLReader xr) {
            super(xr);
        }

        public Distribution getDistribution() {
            return distribution;
        }

        public void endId(String text) {
            this.id = text;
        }

        public void endStatus(String text) {
            this.status = text;
        }

        public void endLastModifiedTime(String text) throws ParseException {
            this.lastModifiedTime = ServiceUtils.parseIso8601Date(text);
        }

        public void endInProgressInvalidationBatches(String text) {
            this.inProgressInvalidationBatches = Long.parseLong(text);
        }

        public void endDomainName(String text) {
            this.domainName = text;
        }

        public void endCNAME(String text) {
            this.cnamesList.add(text);
        }

        public void startOrigin() {
            // For download distribution origins
            transferControlToHandler(new OriginHandler(xr));
        }

        public void startS3Origin() {
            // For streaming distribution origins
            transferControlToHandler(new OriginHandler(xr));
        }

        public void startDefaultCacheBehavior() {
            inDefaultCacheBehavior = true;
            transferControlToHandler(new CacheBehaviorHandler(xr));
        }

        public void startCacheBehavior() {
            inDefaultCacheBehavior = false;
            transferControlToHandler(new CacheBehaviorHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            if(childHandler instanceof OriginHandler) {
                this.origins.add(((OriginHandler) childHandler).origin);
            }
            else if(childHandler instanceof CacheBehaviorHandler) {
                if(inDefaultCacheBehavior) {
                    this.defaultCacheBehavior = ((CacheBehaviorHandler) childHandler).cacheBehavior;
                }
                else {
                    this.cacheBehaviors.add(((CacheBehaviorHandler) childHandler).cacheBehavior);
                }
            }
        }

        public void endComment(String text) {
            this.comment = text;
        }

        public void endEnabled(String text) {
            this.enabled = String.valueOf(true).equalsIgnoreCase(text);
        }

        public void endDistributionSummary(String text) {
            DistributionConfig config = new DistributionConfig(
                    this.origins.toArray(new Origin[origins.size()]),
                    null,  // callerReference
                    this.cnamesList.toArray(new String[cnamesList.size()]),
                    this.comment,
                    this.enabled,
                    null,  // loggingStatus
                    null,
                    this.defaultCacheBehavior,
                    this.cacheBehaviors.toArray(new CacheBehavior[cacheBehaviors.size()])
            );
            this.distribution = new Distribution(id, status,
                    lastModifiedTime, inProgressInvalidationBatches, domainName, null, config);
            returnControlToParentHandler();
        }

        public void endStreamingDistributionSummary(String text) {
            StreamingDistributionConfig config = new StreamingDistributionConfig(
                    this.origins.toArray(new Origin[origins.size()]),
                    null,  // callerReference
                    this.cnamesList.toArray(new String[cnamesList.size()]),
                    this.comment,
                    this.enabled,
                    null,  // loggingStatus
                    null  // trustedSignerAwsAccountNumbers
            );
            this.distribution = new StreamingDistribution(id, status,
                    lastModifiedTime, domainName, null, config);
            returnControlToParentHandler();
        }
    }

    public class DistributionListHandler extends SimpleHandler {
        private final List<Distribution> distributions = new ArrayList<Distribution>();
        private String marker = null;
        private String nextMarker = null;
        private int maxItems = 100;
        private boolean isTruncated = false;

        public DistributionListHandler(XMLReader xr) {
            super(xr);
        }

        public List<Distribution> getDistributions() {
            return distributions;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public String getMarker() {
            return marker;
        }

        public String getNextMarker() {
            return nextMarker;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void startDistributionSummary() {
            transferControlToHandler(new DistributionSummaryHandler(xr));
        }

        public void startStreamingDistributionSummary() {
            transferControlToHandler(new DistributionSummaryHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            distributions.add(
                    ((DistributionSummaryHandler) childHandler).getDistribution());
        }

        public void endMarker(String text) {
            this.marker = text;
        }

        public void endNextMarker(String text) {
            this.nextMarker = text;
        }

        public void endMaxItems(String text) {
            this.maxItems = Integer.parseInt(text);
        }

        public void endIsTruncated(String text) {
            this.isTruncated = String.valueOf(true).equalsIgnoreCase(text);
        }
    }

    public class OriginHandler extends SimpleHandler {
        protected Origin origin = null;
        private String id;
        private String domainName = null;
        private String originAccessIdentity = null; // S3Origin
        private String httpPort = null; // CustomOrigin
        private String httpsPort = null; // CustomOrigin
        private String originProtocolPolicy = null; // CustomOrigin

        public OriginHandler(XMLReader xr) {
            super(xr);
        }

        public void endId(String text) {
            this.id = text;
        }

        public void endDomainName(String text) {
            this.domainName = text;
        }

        public void endOriginAccessIdentity(String text) {
            this.originAccessIdentity = text;
        }

        public void endHTTPPort(String text) {
            this.httpPort = text;
        }

        public void endHTTPSPort(String text) {
            this.httpsPort = text;
        }

        public void endOriginProtocolPolicy(String text) {
            this.originProtocolPolicy = text;
        }

        public void endS3OriginConfig(String text) {
            // For download distribution origins
            if(this.originAccessIdentity != null && this.originAccessIdentity.length() == 0) {
                this.originAccessIdentity = null;
            }
            this.origin = new S3Origin(this.id, this.domainName, this.originAccessIdentity);
            returnControlToParentHandler();
        }

        public void endS3Origin(String text) {
            // For streaming distribution origins
            if(this.originAccessIdentity != null && this.originAccessIdentity.length() == 0) {
                this.originAccessIdentity = null;
            }
            this.origin = new S3Origin(this.id, this.domainName, this.originAccessIdentity);
            returnControlToParentHandler();
        }

        public void endCustomOriginConfig(String text) {
            // For download distribution origins
            this.origin = new CustomOrigin(this.id, this.domainName,
                    CustomOrigin.OriginProtocolPolicy.fromText(this.originProtocolPolicy),
                    Integer.valueOf(this.httpPort), Integer.valueOf(this.httpsPort));
            returnControlToParentHandler();
        }
    }

    public class OriginAccessIdentityHandler extends SimpleHandler {
        private String id = null;
        private String s3CanonicalUserId = null;
        private String comment = null;
        private OriginAccessIdentity originAccessIdentity = null;
        private OriginAccessIdentityConfig originAccessIdentityConfig = null;

        public OriginAccessIdentityHandler(XMLReader xr) {
            super(xr);
        }

        public OriginAccessIdentity getOriginAccessIdentity() {
            return this.originAccessIdentity;
        }

        public void endId(String text) {
            this.id = text;
        }

        public void endS3CanonicalUserId(String text) {
            this.s3CanonicalUserId = text;
        }

        public void endComment(String text) {
            this.comment = text;
        }

        public void startCloudFrontOriginAccessIdentityConfig() {
            transferControlToHandler(new OriginAccessIdentityConfigHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            this.originAccessIdentityConfig =
                    ((OriginAccessIdentityConfigHandler) childHandler).getOriginAccessIdentityConfig();
        }

        public void endCloudFrontOriginAccessIdentity(String text) {
            this.originAccessIdentity = new OriginAccessIdentity(
                    this.id, this.s3CanonicalUserId, this.originAccessIdentityConfig);
        }

        public void endCloudFrontOriginAccessIdentitySummary(String text) {
            this.originAccessIdentity = new OriginAccessIdentity(
                    this.id, this.s3CanonicalUserId, this.comment);
            returnControlToParentHandler();
        }
    }

    public class OriginAccessIdentityConfigHandler extends SimpleHandler {
        private String callerReference = null;
        private String comment = null;
        private OriginAccessIdentityConfig config = null;

        public OriginAccessIdentityConfigHandler(XMLReader xr) {
            super(xr);
        }

        public OriginAccessIdentityConfig getOriginAccessIdentityConfig() {
            return this.config;
        }

        public void endCallerReference(String text) {
            this.callerReference = text;
        }

        public void endComment(String text) {
            this.comment = text;
        }

        public void endCloudFrontOriginAccessIdentityConfig(String text) {
            this.config = new OriginAccessIdentityConfig(this.callerReference, this.comment);
            returnControlToParentHandler();
        }
    }

    public class OriginAccessIdentityListHandler extends SimpleHandler {
        private final List<OriginAccessIdentity> originAccessIdentityList =
                new ArrayList<OriginAccessIdentity>();
        private String marker = null;
        private String nextMarker = null;
        private int maxItems = 100;
        private boolean isTruncated = false;

        public OriginAccessIdentityListHandler(XMLReader xr) {
            super(xr);
        }

        public List<OriginAccessIdentity> getOriginAccessIdentityList() {
            return this.originAccessIdentityList;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public String getMarker() {
            return marker;
        }

        public String getNextMarker() {
            return nextMarker;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void startCloudFrontOriginAccessIdentitySummary() {
            transferControlToHandler(new OriginAccessIdentityHandler(xr));
        }

        @Override
        public void controlReturned(SimpleHandler childHandler) {
            originAccessIdentityList.add(
                    ((OriginAccessIdentityHandler) childHandler).getOriginAccessIdentity());
        }

        public void endMarker(String text) {
            this.marker = text;
        }

        public void endNextMarker(String text) {
            this.nextMarker = text;
        }

        public void endMaxItems(String text) {
            this.maxItems = Integer.parseInt(text);
        }

        public void endIsTruncated(String text) {
            this.isTruncated = String.valueOf(true).equalsIgnoreCase(text);
        }
    }

    public class InvalidationListHandler extends SimpleHandler {
        private String marker = null;
        private String nextMarker = null;
        private int maxItems = 100;
        private boolean isTruncated = false;
        private String invalidationSummaryId = null;
        private String invalidationSummaryStatus = null;
        private List<InvalidationSummary> invalidationSummaries =
                new ArrayList<InvalidationSummary>();
        private InvalidationList invalidationList = null;

        public InvalidationListHandler(XMLReader xr) {
            super(xr);
        }

        public InvalidationList getInvalidationList() {
            return invalidationList;
        }

        public boolean isTruncated() {
            return isTruncated;
        }

        public String getMarker() {
            return marker;
        }

        public String getNextMarker() {
            return nextMarker;
        }

        public int getMaxItems() {
            return maxItems;
        }

        public void endMarker(String text) {
            this.marker = text;
        }

        public void endNextMarker(String text) {
            this.nextMarker = text;
        }

        public void endMaxItems(String text) {
            this.maxItems = Integer.parseInt(text);
        }

        public void endIsTruncated(String text) {
            this.isTruncated = String.valueOf(true).equalsIgnoreCase(text);
        }

        // Inside InvalidationSummary
        public void endId(String text) {
            this.invalidationSummaryId = text;
        }

        // Inside InvalidationSummary
        public void endStatus(String text) {
            this.invalidationSummaryStatus = text;
            this.invalidationSummaries.add(new InvalidationSummary(
                    this.invalidationSummaryId, this.invalidationSummaryStatus));
        }

        public void endInvalidationList(String ignore) {
            this.invalidationList = new InvalidationList(
                    this.marker, this.nextMarker, this.maxItems, this.isTruncated,
                    this.invalidationSummaries);
        }
    }

    public class InvalidationHandler extends SimpleHandler {
        private Invalidation invalidation = new Invalidation();

        public InvalidationHandler(XMLReader xr) {
            super(xr);
        }

        public Invalidation getInvalidation() {
            return this.invalidation;
        }

        public void endId(String text) {
            this.invalidation.setId(text);
        }

        public void endStatus(String text) {
            this.invalidation.setStatus(text);
        }

        public void endCreateTime(String text) throws ParseException {
            this.invalidation.setCreateTime(
                    ServiceUtils.parseIso8601Date(text));
        }

        public void endPath(String text) {
            this.invalidation.getObjectKeys().add(text.substring(1));
        }

        public void endCallerReference(String text) {
            this.invalidation.setCallerReference(text);
        }
    }

    public class ErrorHandler extends SimpleHandler {
        private String type = null;
        private String code = null;
        private String message = null;
        private String detail = null;
        private String requestId = null;

        public ErrorHandler(XMLReader xr) {
            super(xr);
        }

        public String getCode() {
            return code;
        }

        public String getDetail() {
            return detail;
        }

        public String getMessage() {
            return message;
        }

        public String getRequestId() {
            return requestId;
        }

        public String getType() {
            return type;
        }

        public void endType(String text) {
            this.type = text;
        }

        public void endCode(String text) {
            this.code = text;
        }

        public void endMessage(String text) {
            this.message = text;
        }

        public void endDetail(String text) {
            this.detail = text;
        }

        public void endRequestId(String text) {
            this.requestId = text;
        }

        // Handle annoying case where request id is in
        // the element "RequestID", not "RequestId"
        public void endRequestID(String text) {
            this.requestId = text;
        }
    }
}
