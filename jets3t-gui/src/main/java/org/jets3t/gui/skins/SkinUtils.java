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
package org.jets3t.gui.skins;

import java.awt.Color;
import java.net.URL;
import java.util.Properties;

import javax.swing.ImageIcon;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility methods for loading skin resources from settings in skin properties.
 *
 * @author James Murty
 */
public class SkinUtils {
    private static final Log log = LogFactory.getLog(SkinUtils.class);

    /**
     * Loads a skin property setting for a color.
     *
     * @param skinProperties
     * contains skin property settings.
     * @param colorPropertyName
     * the name of the property expected to contain a color value.
     *
     * @return
     * the parsed color value if the given property is available and valid, null otherwise.
     */
    public Color loadColor(Properties skinProperties, String colorPropertyName) {
        Color color = null;

        String colorValue = skinProperties.getProperty(colorPropertyName, null);
        log.debug("Loading skin color with property '" + colorPropertyName + "', value: " + colorValue);
        if (colorValue != null) {
            color = Color.decode(colorValue);
        } else {
            log.warn("Color is not available for property '" + colorPropertyName + "'");
        }
        return color;
    }

    /**
     * Loads a skin property setting for an icon image.
     *
     * @param skinProperties
     * contains skin property settings.
     * @param iconPathPropertyName
     * the name of the property expected to contain the path to an icon image resource.
     *
     * @return
     * an icon image resource when the path property is available and it points to a valid
     * image resource, null otherwise.
     */
    public ImageIcon loadIcon(Properties skinProperties, String iconPathPropertyName) {
        ImageIcon imageIcon = null;

        String imageIconPath = skinProperties.getProperty(iconPathPropertyName, null);
        log.debug("Loading image icon with property '" + iconPathPropertyName + "', value: " + imageIconPath);
        if (imageIconPath != null && imageIconPath.length() > 0) {
            URL iconURL = this.getClass().getResource(imageIconPath);
            if (iconURL != null) {
                imageIcon = new ImageIcon(iconURL);
            } else {
                log.warn("Image icon resources is not available in classpath for path '" + imageIconPath + "'");
            }
        } else {
            log.warn("Image icon path is not available for property '" + iconPathPropertyName + "'");
        }
        return imageIcon;
    }

}
