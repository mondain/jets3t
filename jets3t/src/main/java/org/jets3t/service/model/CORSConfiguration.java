package org.jets3t.service.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents the CORS configuration of a bucket.
 * 
 * @author Paul Gregoire
 */
public class CORSConfiguration {

    private List<CORSRule> rules = new ArrayList<>();

    public CORSConfiguration() {
    }

    public CORSConfiguration(List<CORSRule> rules) {
        this.rules = rules;
    }

    public List<CORSRule> getCORSRules() {
        return this.rules;
    }

    public void addCORSRule(CORSRule rule) {
        this.rules.add(rule);
    }

    public CORSRule newCORSRule(String allowedOrigin, Set<AllowedMethod> allowedMethods) {
        CORSRule rule = this.new CORSRule(allowedOrigin, allowedMethods);
        this.rules.add(rule);
        return rule;
    }

    public String toXml() throws ParserConfigurationException, FactoryConfigurationError, TransformerException {
        XMLBuilder builder = XMLBuilder.create("CORSConfiguration");
        for (CORSRule rule : this.rules) {
            XMLBuilder b = builder.elem("CORSRule");
            if ((rule.id != null) && (rule.id.length() > 0)) {
                b.elem("ID").t(rule.id);
            }
            b.elem("AllowedOrigin").t(rule.getAllowedOrigin());
            for (AllowedMethod am : rule.getAllowedMethods()) {
                b.elem("AllowedMethod").t(am.name());
            }
            for (String ah : rule.getAllowedHeaders()) {
                b.elem("AllowedHeader").t(ah);
            }
            if (rule.getMaxAgeSeconds() != null) {
                b.elem("MaxAgeSeconds").t(rule.getMaxAgeSeconds().toString());
            }
            for (String xh : rule.getExposeHeaders()) {
                b.elem("ExposeHeader").t(xh);
            }
        }
        return builder.asString();
    }

    public static enum AllowedMethod {
        GET, HEAD, DELETE, POST, PUT;

        private AllowedMethod() {
        }
    }

    public class CORSRule {

        protected String id;

        protected String allowedOrigin;

        protected Set<CORSConfiguration.AllowedMethod> allowedMethods;

        protected Set<String> allowedHeaders = Collections.emptySet();

        protected Integer maxAgeSeconds;

        protected Set<String> exposeHeaders = Collections.emptySet();

        public CORSRule() {
            this.allowedMethods = new HashSet<>();
        }

        public CORSRule(String allowedOrigin, Set<CORSConfiguration.AllowedMethod> allowedMethods) {
            this.allowedOrigin = allowedOrigin;
            this.allowedMethods = allowedMethods;
        }

        public String getAllowedOrigin() {
            return this.allowedOrigin;
        }

        public void setAllowedOrigin(String allowedOrigin) {
            this.allowedOrigin = allowedOrigin;
        }

        public Set<CORSConfiguration.AllowedMethod> getAllowedMethods() {
            return this.allowedMethods;
        }

        public void setAllowedMethods(Set<CORSConfiguration.AllowedMethod> allowedMethods) {
            this.allowedMethods = allowedMethods;
        }

        public void addAllowedMethod(String allowedMethod) {
            this.allowedMethods.add(CORSConfiguration.AllowedMethod.valueOf(allowedMethod));
        }

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Set<String> getAllowedHeaders() {
            return this.allowedHeaders;
        }

        public void setAllowedHeaders(Set<String> allowedHeaders) {
            this.allowedHeaders = allowedHeaders;
        }

        public void addAllowedHeader(String allowedHeader) {
            if (this.allowedHeaders == null) {
                this.allowedHeaders = new HashSet<>();
            }
            this.allowedHeaders.add(allowedHeader);
        }

        public Integer getMaxAgeSeconds() {
            return this.maxAgeSeconds;
        }

        public void setMaxAgeSeconds(Integer maxAgeSeconds) {
            this.maxAgeSeconds = maxAgeSeconds;
        }

        public Set<String> getExposeHeaders() {
            return this.exposeHeaders;
        }

        public void setExposeHeaders(Set<String> exposeHeaders) {
            this.exposeHeaders = exposeHeaders;
        }

        public void addExposeHeader(String exposeHeader) {
            if (this.exposeHeaders == null) {
                this.exposeHeaders = new HashSet<>();
            }
            this.exposeHeaders.add(exposeHeader);
        }

        public int hashCode() {
            int result = 1;
            result = 31 * result + getOuterType().hashCode();
            result = 31 * result + (this.allowedMethods == null ? 0 : this.allowedMethods.hashCode());
            result = 31 * result + (this.allowedOrigin == null ? 0 : this.allowedOrigin.hashCode());
            result = 31 * result + (this.id == null ? 0 : this.id.hashCode());
            return result;
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            CORSRule other = (CORSRule) obj;
            if (!getOuterType().equals(other.getOuterType())) {
                return false;
            }
            if (this.allowedMethods == null) {
                if (other.allowedMethods != null) {
                    return false;
                }
            } else if (!this.allowedMethods.equals(other.allowedMethods)) {
                return false;
            }
            if (this.allowedOrigin == null) {
                if (other.allowedOrigin != null) {
                    return false;
                }
            } else if (!this.allowedOrigin.equals(other.allowedOrigin)) {
                return false;
            }
            if (this.id == null) {
                if (other.id != null) {
                    return false;
                }
            } else if (!this.id.equals(other.id)) {
                return false;
            }
            return true;
        }

        private CORSConfiguration getOuterType() {
            return CORSConfiguration.this;
        }
    }
}
