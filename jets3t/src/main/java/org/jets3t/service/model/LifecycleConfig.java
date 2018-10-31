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
package org.jets3t.service.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.jets3t.service.utils.ServiceUtils;

import com.jamesmurty.utils.XMLBuilder;

/**
 * Represents the lifecycle configuraton of a bucket.
 *
 * @author James Murty
 */
public class LifecycleConfig {
    public static final String STORAGE_CLASS_GLACIER = "GLACIER";

    private List<Rule> rules = new ArrayList<Rule>();

    public LifecycleConfig(List<Rule> rules) {
        this.rules = rules;
    }

    public LifecycleConfig() {
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void addRule(Rule rule) {
        this.rules.add(rule);
    }

    public Rule newRule(String id, String prefix, Boolean enabled) {
        Rule rule = this.new Rule(id, prefix, enabled);
        this.rules.add(rule);
        return rule;
    }

    /**
     *
     * @return
     * An XML representation of the object suitable for use as an input to the REST/HTTP interface.
     *
     * @throws FactoryConfigurationError
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    public String toXml()
        throws ParserConfigurationException, FactoryConfigurationError, TransformerException
    {
        XMLBuilder builder = XMLBuilder.create("LifecycleConfiguration");
        for (Rule rule: this.getRules()) {
            XMLBuilder b = builder.elem("Rule");
            if (rule.id != null && rule.id.length() > 0) {
                b.elem("ID").t(rule.id);
            }
            b.elem("Prefix").t(rule.prefix).up()
             .elem("Status").t(rule.enabled ? "Enabled" : "Disabled").up();

            if (rule.transition != null) {
                XMLBuilder tBuilder = b.elem("Transition");
                if (rule.transition.date != null) {
                    tBuilder.elem("Date").t(ServiceUtils.formatIso8601Date(rule.transition.date));
                }
                if (rule.transition.days != null) {
                    tBuilder.elem("Days").t(Integer.toString(rule.transition.days));
                }
                tBuilder.elem("StorageClass").t(rule.transition.storageClass);
            }
            if (rule.expiration != null) {
                XMLBuilder eBuilder = b.elem("Expiration");
                if (rule.expiration.date != null) {
                    eBuilder.elem("Date").t(ServiceUtils.formatIso8601Date(rule.expiration.date));
                }
                if (rule.expiration.days != null) {
                    eBuilder.elem("Days").t(Integer.toString(rule.expiration.days));
                }
            }
        }
        return builder.asString();
    }

    public abstract class TimeEvent {
        protected Integer days;
        protected Date date;

        public TimeEvent() {
        }

        public TimeEvent(Integer days) {
            this.days = days;
        }

        public TimeEvent(Date date) {
            this.date = date;
        }

        public Integer getDays() {
            return days;
        }

        public void setDays(Integer days) {
            this.days = days;
            this.date = null;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
            this.days = null;
        }

        @Override
        public boolean equals(final Object o) {
            if(this == o) {
                return true;
            }
            if(!(o instanceof TimeEvent)) {
                return false;
            }
            final TimeEvent timeEvent = (TimeEvent) o;
            if(date != null ? !date.equals(timeEvent.date) : timeEvent.date != null) {
                return false;
            }
            if(days != null ? !days.equals(timeEvent.days) : timeEvent.days != null) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = days != null ? days.hashCode() : 0;
            result = 31 * result + (date != null ? date.hashCode() : 0);
            return result;
        }
    }

    public class Expiration extends TimeEvent {

        public Expiration() {
        }

        public Expiration(Date date) {
            super(date);
        }

        public Expiration(Integer days) {
            super(days);
        }
    }

    public class Transition extends TimeEvent {
        protected String storageClass = STORAGE_CLASS_GLACIER;

        public Transition() {
            super();
        }

        public Transition(Date date, String storageClass) {
            super(date);
            this.storageClass = storageClass;
        }

        public Transition(Integer days, String storageClass) {
            super(days);
            this.storageClass = storageClass;
        }

        public String getStorageClass() {
            return storageClass;
        }

        public void setStorageClass(String storageClass) {
            this.storageClass = storageClass;
        }
    }

    public class Rule {
        protected String id;
        protected String prefix;
        protected Boolean enabled;
        protected Transition transition;
        protected Expiration expiration;

        public Rule() {
        }

        public Rule(String id, String prefix, Boolean enabled) {
            this.id = id;
            this.prefix = prefix;
            this.enabled = enabled;
        }

        public Expiration newExpiration() {
            this.expiration = new Expiration();
            return this.expiration;
        }

        public Transition newTransition() {
            this.transition = new Transition();
            return this.transition;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPrefix() {
            return prefix;
        }

        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public Transition getTransition() {
            return transition;
        }

        public void setTransition(Transition transition) {
            this.transition = transition;
        }

        public Expiration getExpiration() {
            return expiration;
        }

        public void setExpiration(Expiration expiration) {
            this.expiration = expiration;
        }

        @Override
        public boolean equals(final Object o) {
            if(this == o) {
                return true;
            }
            if(o == null || getClass() != o.getClass()) {
                return false;
            }
            final Rule rule = (Rule) o;
            if(expiration != null ? !expiration.equals(rule.expiration) : rule.expiration != null) {
                return false;
            }
            if(transition != null ? !transition.equals(rule.transition) : rule.transition != null) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = transition != null ? transition.hashCode() : 0;
            result = 31 * result + (expiration != null ? expiration.hashCode() : 0);
            return result;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if(this == o) {
            return true;
        }
        if(o == null || getClass() != o.getClass()) {
            return false;
        }
        final LifecycleConfig that = (LifecycleConfig) o;
        if(rules != null ? !rules.equals(that.rules) : that.rules != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return rules != null ? rules.hashCode() : 0;
    }
}
