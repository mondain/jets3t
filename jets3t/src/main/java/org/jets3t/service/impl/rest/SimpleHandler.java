package org.jets3t.service.impl.rest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import java.lang.reflect.Method;

public class SimpleHandler extends DefaultHandler {
    private static final Log log = LogFactory.getLog(SimpleHandler.class);

    private XMLReader xr = null;
    private StringBuffer textContent = null;
    protected SimpleHandler currentHandler = null;
    protected SimpleHandler parentHandler = null;

    public SimpleHandler(XMLReader xr) {
        this.xr = xr;
        this.textContent = new StringBuffer();
        currentHandler = this;
    }

    public void transferControlToHandler(SimpleHandler toHandler) {
        currentHandler = toHandler;
        toHandler.parentHandler = this;
        xr.setContentHandler(currentHandler);
        xr.setErrorHandler(currentHandler);
        log.debug("Transferred control to handler " + toHandler.getClass().getSimpleName());
    }

    public void returnControlToParentHandler() {
        if(isChildHandler()) {
            parentHandler.currentHandler = parentHandler;
            parentHandler.controlReturned(this);
            currentHandler = parentHandler;
            xr.setContentHandler(currentHandler);
            xr.setErrorHandler(currentHandler);
            log.debug("Returned control from handler " + this.getClass().getSimpleName());
        }
        else {
            log.debug("Ignoring call to return control to parent handler, as this class has no parent: " +
                    this.getClass().getSimpleName());
        }
    }

    public boolean isChildHandler() {
        return parentHandler != null;
    }

    public void controlReturned(SimpleHandler childHandler) {
    }

    @Override
    public void startElement(String uri, String name, String qName, Attributes attrs) {
        try {
            Method method = currentHandler.getClass().getMethod("start" + name, new Class[]{});
            method.invoke(currentHandler);
            log.debug("Processed " + this.getClass().getSimpleName() + " startElement method for '" + name + "'");
        }
        catch(NoSuchMethodException e) {
            log.debug("Skipped non-existent " + this.getClass().getSimpleName() + " startElement method for '" + name + "'");
        }
        catch(Exception t) {
            log.error("Unable to invoke " + this.getClass().getSimpleName() + " startElement method for '" + name + "'", t);
        }
    }

    @Override
    public void endElement(String uri, String name, String qName) {
        String elementText = this.textContent.toString().trim();
        try {
            Method method = currentHandler.getClass().getMethod("end" + name, new Class[]{String.class});
            method.invoke(currentHandler, elementText);
            log.debug("Processed " + this.getClass().getSimpleName() + " endElement method for '" + name + "'");
        }
        catch(NoSuchMethodException e) {
            log.debug("Skipped non-existent " + this.getClass().getSimpleName() + " endElement method for '" + name + "'");
        }
        catch(Exception t) {
            log.error("Unable to invoke " + this.getClass().getSimpleName() + " endElement method for '" + name + "'", t);
        }
        this.textContent = new StringBuffer();
    }

    @Override
    public void characters(char ch[], int start, int length) {
        this.textContent.append(ch, start, length);
    }

}
