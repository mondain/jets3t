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

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.text.AttributeSet;
import javax.swing.text.html.HTML;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Enhanced version of {@link JLabel} that changes the mouse curser to indicate when it passes over a
 * clickable HTML HREF link, and triggers a listener to follow the link when it is clicked.
 * <p>
 * This class is a modified version of example code authored by Jeffrey Bush:<br>
 * <a href="http://forum.java.sun.com/thread.jspa?threadID=574895&messageID=2866170"
 *   >http://forum.java.sun.com/thread.jspa?threadID=574895&amp;messageID=2866170</a>.
 */
public class JHtmlLabel extends JLabel implements MouseListener, MouseMotionListener {
    private static final long serialVersionUID = -2146502207121434264L;

    private static final Log log = LogFactory.getLog(JHtmlLabel.class);

    private HyperlinkActivatedListener listener = null;

    /**
     * @param listener
     * a listener responsible for following an href link if it is triggered.
     */
    public JHtmlLabel(HyperlinkActivatedListener listener) {
        this(null, listener);
    }

    /**
     *
     * @param htmlText
     * the html text to display in the label. This text doesn't necessarily have to be HTML,
     * but why would you use this control if it isn't?
     * @param listener
     * a listener responsible for following an href link if it is triggered.
     */
    public JHtmlLabel(String htmlText, HyperlinkActivatedListener listener) {
        super();
        if (htmlText != null) {
            this.setText(htmlText);
        }
        if (listener != null) {
            setHyperlinkeActivatedListener(listener);
        }
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setHyperlinkeActivatedListener(HyperlinkActivatedListener listener) {
        this.listener = listener;
    }

    /**
     * Triggers the listener to follow an HTML href link that has been clicked.
     *
     * @param e event
     */
    public void mouseClicked(MouseEvent e) {
        AccessibleJLabel acc = (AccessibleJLabel) getAccessibleContext();
        int stringIndexAtPoint = acc.getIndexAtPoint(e.getPoint());
        if (stringIndexAtPoint < 0) {
            return;
        }
        AttributeSet attr = (AttributeSet) acc.getCharacterAttribute(
            acc.getIndexAtPoint(e.getPoint())).getAttribute(HTML.Tag.A);
        if (attr != null) {
            String href = (String) attr.getAttribute(HTML.Attribute.HREF);
            String target = (String) attr.getAttribute(HTML.Attribute.TARGET);
            try {
                if (listener == null) {
                    log.warn("No HyperlinkActivatedListener available to follow HTML link for label: "
                        + getText());
                } else {
                    listener.followHyperlink(new URL(href), target);
                }
            } catch (Exception ex) {
                log.error("Unable to load URL: " + href, ex);
            }
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseDragged(MouseEvent e) {
        mouseMoved(e);
    }

    /**
     * Changes the mouse cursor to a hand to indicate when the mouse moves over a clickable
     * HTML link.
     *
     * @param e event
     */
    public void mouseMoved(MouseEvent e) {
        AccessibleJLabel acc = (AccessibleJLabel) getAccessibleContext();
        int stringIndexAtPoint = acc.getIndexAtPoint(e.getPoint());
        if (stringIndexAtPoint < 0) {
            return;
        }
        javax.swing.text.AttributeSet attr = acc.getCharacterAttribute(stringIndexAtPoint);
        if (attr.getAttribute(HTML.Tag.A) == null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

}
