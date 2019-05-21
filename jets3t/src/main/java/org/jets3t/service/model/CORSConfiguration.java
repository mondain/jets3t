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

    // default to AWS style
    private Format format = Format.AWS;
    
    // XML formatting style
    public enum Format {
        AWS, GoogleStorage;
    }

    public enum AllowedMethod {
        GET, HEAD, DELETE, POST, PUT;
    }
    
    public CORSConfiguration() {
    }

    public CORSConfiguration(List<CORSRule> rules) {
        this.rules = rules;
    }

    public void setFormat(Format format) {
        this.format = format;
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

    public CORSRule newCORSRule(String allowedOrigin, Set<AllowedMethod> allowedMethods, Set<String> allowedHeaders) {
        CORSRule rule = this.new CORSRule(allowedOrigin, allowedMethods, allowedHeaders);
        this.rules.add(rule);
        return rule;
    }

    public String toXml() throws ParserConfigurationException, FactoryConfigurationError, TransformerException {
        XMLBuilder builder = null;
        if (format == Format.AWS) {
            builder = XMLBuilder.create("CORSConfiguration");
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
        } else if (format == Format.GoogleStorage) {
            /* https://cloud.google.com/storage/docs/configuring-cors
            <?xml version="1.0" encoding="UTF-8"?>
               <CorsConfig>
                 <Cors>
                   <Origins>
                     <Origin>http://example.appspot.com</Origin>
                   </Origins>
                   <Methods>
                     <Method>GET</Method>
                     <Method>HEAD</Method>
                     <Method>DELETE</Method>
                   </Methods>
                   <ResponseHeaders>
                     <ResponseHeader>Content-Type</ResponseHeader>
                   </ResponseHeaders>
                   <MaxAgeSec>3600</MaxAgeSec>
                 </Cors>
               </CorsConfig>                 
            */
            builder = XMLBuilder.create("CorsConfig");
            for (CORSRule rule : this.rules) {
                XMLBuilder b = builder.elem("Cors");
                XMLBuilder origins = b.elem("Origins");
                origins.elem("Origin").t(rule.getAllowedOrigin());
                if (rule.allowedHeaders != null) {
                    XMLBuilder methods = b.elem("Methods");
                    for (AllowedMethod am : rule.getAllowedMethods()) {
                        methods.elem("Method").t(am.name());
                    }
                }
                if (rule.allowedHeaders != null) {
                    XMLBuilder respHeaders = b.elem("ResponseHeaders");
                    for (String ah : rule.getAllowedHeaders()) {
                        respHeaders.elem("ResponseHeader").t(ah);
                    }
                }
                if (rule.getMaxAgeSeconds() != null) {
                    b.elem("MaxAgeSec").t(rule.getMaxAgeSeconds().toString());
                }
            }
        }
        return builder != null ? builder.asString() : null;
    }

    public class CORSRule {

        protected String id;

        protected String allowedOrigin;

        protected Set<CORSConfiguration.AllowedMethod> allowedMethods;

        protected Set<String> allowedHeaders = Collections.emptySet();

        protected Integer maxAgeSeconds = 3600;

        protected Set<String> exposeHeaders = Collections.emptySet();

        public CORSRule() {
            this.allowedMethods = new HashSet<>();
        }

        public CORSRule(String allowedOrigin, Set<CORSConfiguration.AllowedMethod> allowedMethods) {
            this.allowedOrigin = allowedOrigin;
            this.allowedMethods = allowedMethods;
        }

        public CORSRule(String allowedOrigin, Set<AllowedMethod> allowedMethods, Set<String> allowedHeaders) {
            this.allowedOrigin = allowedOrigin;
            this.allowedMethods = allowedMethods;
            this.allowedHeaders = allowedHeaders;
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
