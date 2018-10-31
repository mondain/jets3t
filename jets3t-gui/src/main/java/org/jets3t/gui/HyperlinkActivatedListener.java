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
package org.jets3t.gui;

import java.net.URL;


/**
 * Listener responsible for following HTML links that have been activated.
 *
 * @author James Murty
 */
public interface HyperlinkActivatedListener {

    /**
     * This method is triggered when an HTML link is activated, such as by an HTML link in a
     * {@link JHtmlLabel} - any class that implements this listener should do something useful
     * with the triggered hyperlink, preferrably opening it in a web browser.
     *
     * @param url
     * the url contained in the href.
     * @param target
     * the target attribute of the href, may be null if the attribute is not present.
     */
    public void followHyperlink(URL url, String target);

}
