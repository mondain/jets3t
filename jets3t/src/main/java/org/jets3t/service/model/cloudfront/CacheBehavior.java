/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2012 James Murty
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

import java.util.Arrays;

import com.jamesmurty.utils.XMLBuilder;


public class CacheBehavior {

    public enum ViewerProtocolPolicy {
        HTTPS_ONLY ("https-only"),
        ALLOW_ALL ("allow-all"),
        REDIRECT_TO_HTTPS ("redirect-to-https");

        private final String textValue;

        ViewerProtocolPolicy(String textValue) {
            this.textValue = textValue;
        }

        public String toText() {
            return textValue;
        }

        public static ViewerProtocolPolicy fromText(String text) {
            for (ViewerProtocolPolicy e: ViewerProtocolPolicy.values()) {
                if (e.toText().equalsIgnoreCase(text)) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Invalid ViewerProtocolPolicy: " + text);
        }
    }

    private String pathPattern;
    private String targetOriginId;
    private boolean isForwardQueryString = false;
    private String[] trustedSignerAwsAccountNumbers;
    private ViewerProtocolPolicy viewerProtocolPolicy = ViewerProtocolPolicy.ALLOW_ALL;
    private Long minTTL = 0l;

    public CacheBehavior(String pathPattern, String targetOriginId,
        boolean isForwardQueryString, String[] trustedSignerAwsAccountNumbers,
        ViewerProtocolPolicy viewerProtocolPolicy, Long minTTL)
    {
        this.pathPattern = pathPattern;
        this.targetOriginId = targetOriginId;
        this.isForwardQueryString = isForwardQueryString;
        this.trustedSignerAwsAccountNumbers = trustedSignerAwsAccountNumbers;
        this.viewerProtocolPolicy = viewerProtocolPolicy;
        this.minTTL = minTTL;
    }

    public CacheBehavior(String targetOriginId,
        boolean isForwardQueryString, String[] trustedSignerAwsAccountNumbers,
        ViewerProtocolPolicy viewerProtocolPolicy, Long minTTL)
    {
        this(null, targetOriginId, isForwardQueryString, trustedSignerAwsAccountNumbers,
            viewerProtocolPolicy, minTTL);
    }

    public CacheBehavior() {
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public void setPathPattern(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    public String getTargetOriginId() {
        return targetOriginId;
    }

    public void setTargetOriginId(String targetOriginId) {
        this.targetOriginId = targetOriginId;
    }

    public boolean isForwardQueryString() {
        return isForwardQueryString;
    }

    public void setIsForwardQueryString(boolean isForwardQueryString) {
        this.isForwardQueryString = isForwardQueryString;
    }

    public String[] getTrustedSignerAwsAccountNumbers() {
        return trustedSignerAwsAccountNumbers;
    }

    public void setTrustedSignerAwsAccountNumbers(
        String[] trustedSignerAwsAccountNumbers) {
        this.trustedSignerAwsAccountNumbers = trustedSignerAwsAccountNumbers;
    }

    public ViewerProtocolPolicy getViewerProtocolPolicy() {
        return viewerProtocolPolicy;
    }

    public void setViewerProtocolPolicy(ViewerProtocolPolicy viewerProtocolPolicy) {
        this.viewerProtocolPolicy = viewerProtocolPolicy;
    }

    public Long getMinTTL() {
        return minTTL;
    }

    public void setMinTTL(Long minTTL) {
        this.minTTL = minTTL;
    }

    public boolean hasTrustedSignerAwsAccountNumbers() {
        return getTrustedSignerAwsAccountNumbers() != null
            && getTrustedSignerAwsAccountNumbers().length > 0;
    }

    public boolean hasMinTTL() {
        return this.minTTL != null;
    }

    public boolean isTrustedSignerSelf() {
        for (String trustedSigner: getTrustedSignerAwsAccountNumbers()) {
            if ("self".equals(trustedSigner)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "CacheBehavior"
            + ": pathPattern=" + getPathPattern()
            + ", targetOriginId=" + getTargetOriginId()
            + ", isForwardQueryString=" + isForwardQueryString()
            + ", trustedSignerAwsAccountNumbers=" + Arrays.asList(getTrustedSignerAwsAccountNumbers())
            + ", viewerProtocolPolicy=" + getViewerProtocolPolicy()
            + ", minTTL=" + getMinTTL()
            ;
    }

}
