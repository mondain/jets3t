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
package org.jets3t.apps.uploader;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Generates XML documents containing metadata about files uploaded to Amazon S3
 * by the Uploader. The XML document includes metadata for user inputs,
 * inputs sourced from applet parameter tags, and additional information
 * available from the uploader such as filenames and generated UUID.
 *
 * @author James Murty
 */
public class XmlGenerator {
    public static final String xmlVersionNumber = "1.0";

    private List objectRequestList = new ArrayList();
    private Map applicationProperties = new HashMap();
    private Map messageProperties = new HashMap();

    /**
     * Add a signature request item to the XML document to store the request, and details about
     * the object the request was related to.
     *
     * @param key
     * the key name of the object the signature request applies to.
     * @param bucketName
     * the bucket containing the object.
     * @param metadata
     * the object's metadata
     * @param signatureRequest
     * the signature request for the object.
     */
    public void addSignatureRequest(String key, String bucketName, Map metadata,
        SignatureRequest signatureRequest)
    {
        objectRequestList.add(new ObjectAndSignatureRequestDetails(key, bucketName, metadata, signatureRequest));
    }

    /**
     * Add application-specific properties to the XML document.
     *
     * @param properties
     */
    public void addApplicationProperties(Map properties) {
        applicationProperties.putAll(properties);
    }

    /**
     * Add message-specific properties to the XML document.
     *
     * @param properties
     */
    public void addMessageProperties(Map properties) {
        messageProperties.putAll(properties);
    }

    private class ObjectAndSignatureRequestDetails {
        public String key = null;
        public String bucketName = null;
        public Map metadata = null;
        public SignatureRequest signatureRequest = null;

        public ObjectAndSignatureRequestDetails(String key, String bucketName, Map metadata,
            SignatureRequest signatureRequest)
        {
            this.key = key;
            this.bucketName = bucketName;
            this.metadata = metadata;
            this.signatureRequest = signatureRequest;
        }
    }

    /**
     * Generates an XML document containing metadata information as Property elements.
     * The root of the document is the element Uploader.
     *
     * @return
     * an XML document string containing Property elements.
     *
     * @throws Exception
     */
    public String generateXml() throws Exception
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

        Document document = builder.newDocument();
        Element rootElem = document.createElement("Uploader");
        document.appendChild(rootElem);
        rootElem.setAttribute("version", xmlVersionNumber);
        rootElem.setAttribute("uploadDate",
            ServiceUtils.formatIso8601Date(new Date()));

        // Add application properties (user inputs and application parameters) to XML document.
        for (Iterator iter = applicationProperties.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String propertyName = (String) entry.getKey();
            String propertyValue = (String) entry.getValue();
            rootElem.appendChild(createPropertyElement(document, propertyName, propertyValue, "ApplicationProperty"));
        }

        // Add message properties (user inputs and application parameters) to XML document.
        for (Iterator iter = messageProperties.entrySet().iterator(); iter.hasNext();) {
            Map.Entry entry = (Map.Entry) iter.next();
            String propertyName = (String) entry.getKey();
            String propertyValue = (String) entry.getValue();
            rootElem.appendChild(createPropertyElement(document, propertyName, propertyValue, "MessageProperty"));
        }

        // Add Object request details to XML document.
        ObjectAndSignatureRequestDetails[] details = (ObjectAndSignatureRequestDetails[]) objectRequestList
            .toArray(new ObjectAndSignatureRequestDetails[objectRequestList.size()]);
        for (int i = 0; i < details.length; i++) {
            ObjectAndSignatureRequestDetails objectDetails = details[i];
            rootElem.appendChild(createSignatureRequestElement(document, objectDetails));
        }

        // Serialize XML document to String.
        StringWriter writer = new StringWriter();
        StreamResult streamResult = new StreamResult(writer);

        DOMSource domSource = new DOMSource(document);
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer serializer = tf.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.transform(domSource, streamResult);
        return writer.toString();
    }

    /**
     * Creates a Property XML element for a document.
     *
     * @param document
     * the document the property is being created for.
     * @param propertyName
     * the property's name, becomes a <b>name</b> attribute of the element.
     * @param propertyValue
     * the property's value, becomes the CDATA text value of the element. If this value
     * is null, the Property element is empty.
     * @param source
     * text to describe the source of the information, such as userinput or parameter.
     * Becomes a <b>source</b> attribute of the element.
     *
     * @return
     * a Property element.
     */
    private Element createPropertyElement(
        Document document, String propertyName, String propertyValue, String source)
    {
        Element propertyElem = document.createElement(source);
        if (propertyName != null) {
            propertyElem.setAttribute("name", propertyName);
        }
        if (propertyValue != null) {
            CDATASection cdataSection = document.createCDATASection(propertyValue);
            propertyElem.appendChild(cdataSection);
        }
        return propertyElem;
    }

    /**
     * Creates a SignatureRequest document element.
     *
     * @param document
     * @param details
     * @return
     */
    private Element createSignatureRequestElement(Document document, ObjectAndSignatureRequestDetails details) {
        SignatureRequest request = details.signatureRequest;

        Element requestElem = document.createElement("SignatureRequest");
        requestElem.setAttribute("type", request.getSignatureType());
        requestElem.setAttribute("signed", String.valueOf(request.isSigned()));
        requestElem.appendChild(
            createObjectElement(document, details.key, details.bucketName, details.metadata, "RequestObject"));

        if (request.isSigned()) {
            requestElem.appendChild(
                createObjectElement(document, request.getObjectKey(), request.getBucketName(),
                    request.getObjectMetadata(), "SignedObject"));
            requestElem.appendChild(
                createPropertyElement(document, null, request.getSignedUrl(), "SignedURL"));
        } else if (request.getDeclineReason() != null) {
            requestElem.appendChild(
                createPropertyElement(document, null, request.getDeclineReason(), "DeclineReason"));
        }
        return requestElem;
    }

    /**
     * Creates and element to contain information about an object.
     *
     * @param document
     * @param key
     * @param bucketName
     * @param metadata
     * @param elementName
     * @return
     */
    private Element createObjectElement(Document document, String key, String bucketName, Map metadata, String elementName) {
        if (key == null) { key = ""; }
        if (bucketName == null) { bucketName = ""; }

        Element objectElement = document.createElement(elementName);
        objectElement.setAttribute("key", key);
        objectElement.setAttribute("bucketName", bucketName);
        Iterator iter = metadata.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String metadataName = (String) entry.getKey();
            String metadataValue = (String) entry.getValue();

            if (metadataValue == null) { metadataValue = ""; }
            objectElement.appendChild(
                createPropertyElement(document, metadataName, metadataValue, "Metadata"));
        }
        return objectElement;
    }

}
