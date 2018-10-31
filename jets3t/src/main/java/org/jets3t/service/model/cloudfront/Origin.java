/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2010 James Murty
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
package org.jets3t.service.model.cloudfront;


public abstract class Origin {
    private String id;
    private String domainName = null;

    public Origin(String id, String domainName)
    {
        this.id = id;
        this.domainName = domainName;
    }

    public String getId() {
        return this.id;
    }

    /**
     * @deprecated as of 2012-05-05 API version
     * @return
     * Origin's domain name.
     */
    @Deprecated
    public String getDnsName() {
        return getDomainName();
    }

    public String getDomainName() {
        return domainName;
    }

}
