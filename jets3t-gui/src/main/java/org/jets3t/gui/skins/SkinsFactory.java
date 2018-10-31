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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import java.awt.Component;

/**
 * Manages the creation of skinned GUI elements.
 * Skinned elements are created using the following process:
 * <ol>
 * <li>Instantiate a skin-specific class in the skin's package
 * <code>org.jets3t.gui.skins.<i>&lt;skinName&gt;</i></code></li>
 * <li>If a skin-specific class is not available or cannot be created,
 * instantiate a generic GUI class instead</li>
 * </ol>
 * <p>
 * Skinned classes are specially-named extensions to standard Swing classes, which must have a
 * constructor of the form <br><code>public SkinnedJButton(Properties skinProperties, String itemName)</code>.
 * This constructor allows skinned GUI elements to change their look or behaviour based on any
 * skin-specific properties that are provided, or based on the name of a specific GUI element.
 * <p>
 * The skinned class names supported by this factory include:
 * <table summary="The skinned class names supported by this factory">
 * <tr><th>Class name</th><th>Extends</th></tr>
 * <tr><td>SkinnedJButton</td><td>javax.swing.JButton</td></tr>
 * <tr><td>SkinnedJHtmlLabel</td><td>org.jets3t.gui.JHtmlLabel</td></tr>
 * <tr><td>SkinnedJPanel</td><td>javax.swing.JPanel</td></tr>
 * <tr><td>SkinnedLookAndFeel</td><td>javax.swing.plaf.metal.MetalLookAndFeel</td></tr>
 * </table>
 *
 * @author James Murty
 *
 */
public class SkinsFactory {
    private static final Log log = LogFactory.getLog(SkinsFactory.class);

    public static final String NO_SKIN = "noskin";

    /**
     * The name of the chosen skin.
     */
    private String skinName = null;

    /**
     * Properties that apply specifically to the chosen skin.
     */
    private Properties skinProperties = new Properties();

    /**
     * Track component class names that are not available.
     */
    private static Map unavailableClassMap = new HashMap();

    /**
     * Construct the factory and find skin-specific properties in the provided properties set.
     *
     * @param properties
     * A set of properties that may contain skin-specific properties.
     */
    private SkinsFactory(Properties properties) {
        // Gracefully handle missing properties.
        if (properties == null) {
            properties = new Properties();
        }

        this.skinName = properties.getProperty("skin.name");
        if (this.skinName == null) {
            this.skinName = NO_SKIN;
        }

        // Find skin-specific properties.
        String skinPropertyPrefix = "skin." + this.skinName.toLowerCase(Locale.getDefault()) + ".";
        Iterator iter = properties.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            String propertyName = (String) entry.getKey();
            String propertyValue = (String) entry.getValue();

            if (propertyName.toLowerCase(Locale.getDefault()).startsWith(skinPropertyPrefix)) {
                String skinPropertyName = propertyName.substring(skinPropertyPrefix.length());
                this.skinProperties.put(skinPropertyName, propertyValue);
            }
        }
    }

    /**
     * Provides a skin factory initialised with skin-specific properties from the provided
     * properties set. Skin-specific properties are identified as those properties with the
     * prefix <code>skin.<i>&lt;skinName&gt;</i>.</code>
     *
     * @param properties
     * a set of properties that may contain skin-specific properties.
     *
     * @return
     * the skins factory initialised with skin settings.
     */
    public static SkinsFactory getInstance(Properties properties) {
        return new SkinsFactory(properties);
        }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedLookAndFeel</code> class implementation for the current skin, or the default
     * system LookAndFeel if no skin-specific implementation is available.
     */
    public LookAndFeel createSkinnedMetalTheme(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedLookAndFeel"), itemName);
        if (instance != null) {
            return (LookAndFeel) instance;
        } else {
            return UIManager.getLookAndFeel();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJButton</code> class implementation for the current skin, or a default
     * JButton if no skin-specific implementation is available.
     */
    public JButton createSkinnedJButton(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJButton"), itemName);
        if (instance != null) {
            return (JButton) instance;
        } else {
            return new JButton();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJRadioButton</code> class implementation for the current skin, or a default
     * JRadioButton if no skin-specific implementation is available.
     */
    public JRadioButton createSkinnedJRadioButton(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJRadioButton"), itemName);
        if (instance != null) {
            return (JRadioButton) instance;
        } else {
            return new JRadioButton();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJComboBox</code> class implementation for the current skin, or a default
     * JComboBox if no skin-specific implementation is available.
     */
    public JComboBox createSkinnedJComboBox(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJComboBox"), itemName);
        if (instance != null) {
            return (JComboBox) instance;
        } else {
            return new JComboBox();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJComboBox</code> class implementation for the current skin, or a default
     * JComboBox if no skin-specific implementation is available.
     */
    public JCheckBox createSkinnedJCheckBox(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJCheckBox"), itemName);
        if (instance != null) {
            return (JCheckBox) instance;
        } else {
            return new JCheckBox();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJPanel</code> class implementation for the current skin, or a default
     * JPanel if no skin-specific implementation is available.
     */
    public JPanel createSkinnedJPanel(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJPanel"), itemName);
        if (instance != null) {
            return (JPanel) instance;
        } else {
            return new JPanel();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJTable</code> class implementation for the current skin, or a default
     * JPanel if no skin-specific implementation is available.
     */
    public JTable createSkinnedJTable(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJTable"), itemName);
        if (instance != null) {
            return (JTable) instance;
        } else {
            return new JTable();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @param view
     * the client's viewport view to be used.
     *
     * @return
     * a <code>SkinnedJScrollPane</code> class implementation for the current skin, or a default
     * JScrollPane if no skin-specific implementation is available.
     */
    public JScrollPane createSkinnedJScrollPane(String itemName, Object view) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJScrollPane"), itemName);
        if (instance != null) {
            JScrollPane scrollPane = (JScrollPane) instance;
            scrollPane.setViewportView((Component) view);
            return scrollPane;
        } else {
            return new JScrollPane((Component) view);
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJScrollPane</code> class implementation for the current skin, or a default
     * JScrollPane if no skin-specific implementation is available.
     */
    public JScrollPane createSkinnedJScrollPane(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJScrollPane"), itemName);
        if (instance != null) {
            return (JScrollPane) instance;
        } else {
            return new JScrollPane();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJLabel</code> class implementation for the current skin, or a default
     * JHtmlLabel if no skin-specific implementation is available.
     */
    public JHtmlLabel createSkinnedJHtmlLabel(String itemName, HyperlinkActivatedListener hyperlinkListener) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJHtmlLabel"), itemName);
        if (instance != null) {
            JHtmlLabel label = (JHtmlLabel) instance;
            label.setHyperlinkeActivatedListener(hyperlinkListener);
            return label;
        } else {
            return new JHtmlLabel(hyperlinkListener);
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJLabel</code> class implementation for the current skin, or a default
     * JHtmlLabel if no skin-specific implementation is available.
     */
    public JHtmlLabel createSkinnedJHtmlLabel(String itemName) {
        return createSkinnedJHtmlLabel(itemName, null);
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJPasswordField</code> class implementation for the current skin, or a default
     * JPasswordField if no skin-specific implementation is available.
     */
    public JPasswordField createSkinnedJPasswordField(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJPasswordField"), itemName);
        if (instance != null) {
            return (JPasswordField) instance;
        } else {
            return new JPasswordField();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJTextField</code> class implementation for the current skin, or a default
     * JTextField if no skin-specific implementation is available.
     */
    public JTextField createSkinnedJTextField(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJTextField"), itemName);
        if (instance != null) {
            return (JTextField) instance;
        } else {
            return new JTextField();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJTextArea</code> class implementation for the current skin, or a default
     * JTextArea if no skin-specific implementation is available.
     */
    public JTextArea createSkinnedJTextArea(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJTextArea"), itemName);
        if (instance != null) {
            return (JTextArea) instance;
        } else {
            return new JTextArea();
        }
    }

    public JPopupMenu createSkinnedJPopupMenu(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJPopupMenu"), itemName);
        if (instance != null) {
            return (JPopupMenu) instance;
        } else {
            return new JPopupMenu();
        }
    }

    public JMenuItem createSkinnedJMenuItem(String itemName) {
        Object instance = instantiateClass(buildSkinnedClassName("SkinnedJMenuItem"), itemName);
        if (instance != null) {
            return (JMenuItem) instance;
        } else {
            return new JMenuItem();
        }
    }

    /**
     * @param itemName
     * the name of this specific item in the GUI, which may be used to determine how the skinned
     * item should look or behave.
     *
     * @return
     * a <code>SkinnedJProgressBar</code> class implementation for the current skin, or a default
     * JProgressBar if no skin-specific implementation is available.
     */
    public JProgressBar createSkinnedJProgressBar(String itemName, int min, int max) {
        JProgressBar jProgressBar = (JProgressBar) instantiateClass(
            buildSkinnedClassName("SkinnedJProgressBar"), itemName);
        if (jProgressBar != null) {
            jProgressBar.setMinimum(min);
            jProgressBar.setMaximum(max);
            return jProgressBar;
        } else {
            jProgressBar = new JProgressBar(min, max);
            return jProgressBar;
        }
    }

    private String buildSkinnedClassName(String className) {
        if (NO_SKIN.equals(skinName)) {
            return null;
        } else {
            String skinnedClassName =
                this.getClass().getPackage().getName() + "." + this.skinName + "." + className;
            return skinnedClassName;
        }
    }

    private Object instantiateClass(String className, String itemName) {
        if (className == null) {
            return null;
        }
        if (unavailableClassMap.get(className) != null) {
            // This class name is not available, don't waste time trying to load it.
            return null;
        }

        try {
            Class myClass = Class.forName(className);
            Constructor constructor = myClass.getConstructor(
                new Class[] { Properties.class, String.class });
            Object instance = constructor.newInstance(new Object[] { skinProperties, itemName });
            return instance;
        } catch (ClassNotFoundException e) {
            log.debug("Class does not exist, will use default. Skinned class name: " + className);
        } catch (Exception e) {
            log.warn("Unable to instantiate skinned class '" + className + "'", e);
        }
        unavailableClassMap.put(className, Boolean.TRUE);
        return null;
    }

}
