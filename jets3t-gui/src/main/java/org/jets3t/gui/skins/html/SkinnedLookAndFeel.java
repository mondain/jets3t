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
package org.jets3t.gui.skins.html;

import java.awt.Color;
import java.awt.Font;
import java.util.Properties;

import javax.swing.JLabel;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A specialisation of the default Metal theme that allows specific colours and
 * fonts to be used instead of those in the Metal theme.
 * <p>
 * This class controls:
 * <ul>
 * <li>the colour used for Window and Control backgrounds</li>
 * <li>the colour used for System, Control and User text</li>
 * <li>the font used for System, Control and User text</li>
 * </ul>
 *
 * @author James Murty
 */
public class SkinnedLookAndFeel extends MetalLookAndFeel {
    private static final long serialVersionUID = 4391824305469950437L;

    private static final Log log = LogFactory.getLog(SkinnedLookAndFeel.class);

    public SkinnedLookAndFeel(Properties skinProperties, String itemName) {
        super();

        // Determine system defaults.
        JLabel defaultLabel = new JLabel();
        Color backgroundColor = defaultLabel.getBackground();
        Color textColor = defaultLabel.getForeground();
        Font font = defaultLabel.getFont();

        // Find skinning configurations.
        String backgroundColorValue = skinProperties.getProperty("backgroundColor", null);
        String textColorValue = skinProperties.getProperty("textColor", null);
        String fontValue = skinProperties.getProperty("font", null);

        // Apply skinning configurations.
        if (backgroundColorValue != null) {
            Color color = Color.decode(backgroundColorValue);
            if (color == null) {
                log.error("Unable to set background color with value: " + backgroundColorValue);
            } else {
                backgroundColor = color;
            }
        }
        if (textColorValue != null) {
            Color color = Color.decode(textColorValue);
            if (color == null) {
                log.error("Unable to set text color with value: " + textColorValue);
            } else {
                textColor = color;
            }
        }
        if (fontValue != null) {
            Font myFont = Font.decode(fontValue);
            if (myFont == null) {
                log.error("Unable to set font with value: " + fontValue);
            } else {
                font = myFont;
            }
        }

        // Update metal theme with configured display properties.
        SkinnedMetalTheme skinnedTheme = new SkinnedMetalTheme(new ColorUIResource(backgroundColor),
            new ColorUIResource(textColor), new FontUIResource(font));
        MetalLookAndFeel.setCurrentTheme(skinnedTheme);
    }

    private class SkinnedMetalTheme extends DefaultMetalTheme {
        private ColorUIResource backgroundColorUIResource = null;
        private ColorUIResource textColorUIResource = null;
        private FontUIResource fontUIResource = null;

        public SkinnedMetalTheme(ColorUIResource backgroundColorUIResource, ColorUIResource
            textColorUIResource, FontUIResource fontUIResource)
        {
            this.backgroundColorUIResource = backgroundColorUIResource;
            this.textColorUIResource = textColorUIResource;
            this.fontUIResource = fontUIResource;
        }

        public String getName() {
            return "Uploader HTML skinnable theme";
        }

        public FontUIResource getSystemTextFont() {
            return fontUIResource;
        }

        public FontUIResource getControlTextFont() {
            return fontUIResource;
        }

        public FontUIResource getUserTextFont() {
            return fontUIResource;
        }

        public ColorUIResource getSystemTextColor() {
            return textColorUIResource;
        }

        public ColorUIResource getControlTextColor() {
            return textColorUIResource;
        }

        public ColorUIResource getUserTextColor() {
            return textColorUIResource;
        }

        public ColorUIResource getWindowBackground() {
            return backgroundColorUIResource;
        }

        public ColorUIResource getControl() {
            return backgroundColorUIResource;
        }
    }

}
