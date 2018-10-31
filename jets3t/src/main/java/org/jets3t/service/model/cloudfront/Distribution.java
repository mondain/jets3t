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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Distribution {
    private String id = null;
    private String status = null;
    private Date lastModifiedTime = null;
    private Long inProgressInvalidationBatches = 0l;
    private String domainName = null;
    private Map activeTrustedSigners = new HashMap();
    private DistributionConfig config = new DistributionConfig();

    public Distribution(String id, String status, Date lastModifiedDate,
                        Long inProgressInvalidationBatches, String domainName,
                        Map activeTrustedSigners, DistributionConfig config) {
        this.id = id;
        this.status = status;
        this.lastModifiedTime = lastModifiedDate;
        this.inProgressInvalidationBatches = inProgressInvalidationBatches;
        this.domainName = domainName;
        this.config = config;
    }

    /**
     * @deprecated as of 2012-05-05 API version.
     */
    @Deprecated
    public Distribution(String id, String status, Date lastModifiedDate,
                        String domainName, Origin origin, String[] cnames, String comment,
                        boolean enabled) {
        this.id = id;
        this.status = status;
        this.lastModifiedTime = lastModifiedDate;
        this.domainName = domainName;
        this.config.setOrigins(new Origin[]{origin});
        this.config.setCNAMEs(cnames);
        this.config.setComment(comment);
        this.config.setEnabled(enabled);
    }

    /**
     * @deprecated as of 2012-05-05 API version.
     */
    @Deprecated
    public Distribution(String id, String status, Date lastModifiedDate,
                        String domainName, Map activeTrustedSigners, DistributionConfig config) {
        this.id = id;
        this.status = status;
        this.lastModifiedTime = lastModifiedDate;
        this.domainName = domainName;
        this.activeTrustedSigners = activeTrustedSigners;
        this.config = config;
    }

    public boolean isSummary() {
        return getConfig() == null;
    }

    /**
     * @deprecated as of 2012-05-05 API version, use {@link #getConfig()} instead.
     */
    @Deprecated
    public String getComment() {
        return this.config.getComment();
    }

    public Long getInProgressInvalidationBatches() {
        return this.inProgressInvalidationBatches;
    }

    public String getDomainName() {
        return domainName;
    }

    public Map getActiveTrustedSigners() {
        return activeTrustedSigners;
    }

    public String getId() {
        return id;
    }

    public Date getLastModifiedTime() {
        return lastModifiedTime;
    }

    /**
     * @deprecated as of 2012-05-05 API version, use {@link #getConfig()} instead.
     */
    @Deprecated
    public Origin getOrigin() {
        return this.config.getOrigin();
    }

    /**
     * @deprecated as of 2012-05-05 API version, use {@link #getConfig()} instead.
     */
    @Deprecated
    public String[] getCNAMEs() {
        return this.config.getCNAMEs();
    }

    /**
     * @deprecated as of 2012-05-05 API version, use {@link #getConfig()} instead.
     */
    @Deprecated
    public boolean isEnabled() {
        return this.config.isEnabled();
    }

    public String getStatus() {
        return status;
    }

    /**
     * @return true if this distribution's status is "Deployed".
     */
    public boolean isDeployed() {
        return "Deployed".equals(getStatus());
    }

    public DistributionConfig getConfig() {
        return config;
    }

    public boolean isStreamingDistribution() {
        return (this instanceof StreamingDistribution);
    }

    @Override
    public String toString() {
        return
                (isStreamingDistribution()
                        ? "CloudFrontStreamingDistribution"
                        : "CloudFrontDistribution")
                        + ": id=" + id + ", status=" + status
                        + ", domainName=" + domainName
                        + ", activeTrustedSigners=" + activeTrustedSigners
                        + ", lastModifiedTime=" + lastModifiedTime
                        + ", config=" + this.getConfig().toString();
    }

}
