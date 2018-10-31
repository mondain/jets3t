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
package org.jets3t.service.model;

import org.jets3t.service.Constants;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents the website configuration of a bucket
 *
 * @author James Murty
 */
public class GSWebsiteConfig extends WebsiteConfig {

    public GSWebsiteConfig(String indexDocumentSuffix, String errorDocumentKey) {
        super(indexDocumentSuffix, errorDocumentKey);
    }

    public GSWebsiteConfig(String indexDocumentSuffix) {
        this(indexDocumentSuffix, null);
    }

    public GSWebsiteConfig() {
        this(null, null);
    }

    /**
     *
     * @return
     * An XML representation of the object suitable for use as an input to the REST/HTTP interface.
     *
     * @throws javax.xml.parsers.FactoryConfigurationError
     * @throws javax.xml.parsers.ParserConfigurationException
     * @throws javax.xml.transform.TransformerException
     */
    public String toXml() throws ParserConfigurationException,
        FactoryConfigurationError, TransformerException
    {
        XMLBuilder builder = XMLBuilder.create("WebsiteConfiguration")
            .attr("xmlns", Constants.XML_NAMESPACE)
            .up();
        if (this.getIndexDocumentSuffix() != null && this.getIndexDocumentSuffix().length() > 0) {
            builder.elem("MainPageSuffix").text(this.getIndexDocumentSuffix());
        }
        if (this.getErrorDocumentKey() != null && this.getErrorDocumentKey().length() > 0) {
            builder.elem("NotFoundPage").text(this.getErrorDocumentKey());
        }
        return builder.asString();
    }

}
