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

import java.awt.Frame;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.utils.RestUtils;

/**
 * Utility methods for GUI-related tasks.
 *
 * @author James Murty
 */
public class GuiUtils {
    private static final Log log = LogFactory.getLog(GuiUtils.class);

    private Map cachedImageIcons = new HashMap();

    /**
     * Loads an icon image from the classpath and sets the icon of the component.
     *
     * @param component
     * the component to apply the icon to, supported components are: JMenuItem, JButton, JLabel, Frame.
     * @param iconResourcePath
     * the path to an icon image in the classpath.
     *
     * @return
     * true if the icon was found and applied to the component
     */
    public boolean applyIcon(Object component, String iconResourcePath) {
        ImageIcon icon = (ImageIcon) cachedImageIcons.get(iconResourcePath);

        if (icon == null) {
            // Icon is not yet cached, load it as a resource.
            URL iconUrl = this.getClass().getResource(iconResourcePath);
            if (iconUrl == null) {
                // Try loading icon using an encoded URL, which can help if the path uses characters
                // that should be URL-encoded (eg '+')
                try {
                    int firstSlashIndex = iconResourcePath.indexOf('/', 1);
                    String firstPathComponent = iconResourcePath.substring(0, firstSlashIndex);
                    String pathRemainder = iconResourcePath.substring(firstSlashIndex);
                    URL baseUrl = this.getClass().getResource(firstPathComponent);
                    iconUrl = new URL(baseUrl.toString() + RestUtils.encodeUrlPath(pathRemainder, "/"));
                    iconUrl.getContent(); // Check whether there is data availabel at the built path.
                } catch (Exception e) {
                    log.warn("Unable to load icon with resource path: " + iconResourcePath);
                    return false;
                }
            }
            if (iconUrl != null) {
                icon = new ImageIcon(iconUrl);
                cachedImageIcons.put(iconResourcePath, icon);
            }
        }

        if (icon != null) {
            if (component instanceof JMenuItem) {
                ((JMenuItem)component).setIcon(icon);
            } else if (component instanceof JButton) {
                ((JButton)component).setIcon(icon);
            } else if (component instanceof JLabel) {
                ((JLabel)component).setIcon(icon);
            } else if (component instanceof Frame) {
                ((Frame)component).setIconImage(icon.getImage());
            } else {
                log.warn("Cannot set icon for unexpected JComponent object: "
                    + component.getClass().getName());
                return false;
            }
            return true;
        } else {
            log.warn("Unable to load icon with resource path: " + iconResourcePath);
            return false;
        }
    }

}
