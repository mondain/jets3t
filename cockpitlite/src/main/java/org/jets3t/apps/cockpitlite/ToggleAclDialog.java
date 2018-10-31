/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007 James Murty
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
package org.jets3t.apps.cockpitlite;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.gui.skins.SkinsFactory;

/**
 * Dialog for the user to toggle ACL settings of an object to/from public/private.
 *
 * @author James Murty
 */
public class ToggleAclDialog extends JDialog implements ActionListener {
    private static final Log log = LogFactory.getLog(ToggleAclDialog.class);

    private Properties applicationProperties = null;
    private boolean isPublicObject = false;

    private HyperlinkActivatedListener hyperlinkListener = null;
    private SkinsFactory skinsFactory = null;

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    private JRadioButton privateRadioButton = null;
    private JRadioButton publicRadioButton = null;


    public ToggleAclDialog(Frame ownerFrame, boolean isPublicObject,
        HyperlinkActivatedListener hyperlinkListener, Properties applicationProperties)
    {
        super(ownerFrame, "Toggle privacy settings", true);
        this.hyperlinkListener = hyperlinkListener;
        this.applicationProperties = applicationProperties;
        this.isPublicObject = isPublicObject;
        initGui();
    }

    public ToggleAclDialog(JDialog ownerDialog, boolean isPublicObject,
        HyperlinkActivatedListener hyperlinkListener, Properties applicationProperties)
    {
        super(ownerDialog, "Change privacy", true);
        this.hyperlinkListener = hyperlinkListener;
        this.applicationProperties = applicationProperties;
        this.isPublicObject = isPublicObject;
        initGui();
    }

    public boolean isPublicAclSet() {
        return publicRadioButton.isSelected();
    }

    /**
     * Initialises all GUI elements.
     */
    private void initGui() {
        // Initialise skins factory.
        skinsFactory = SkinsFactory.getInstance(applicationProperties);

        // Set Skinned Look and Feel.
        LookAndFeel lookAndFeel = skinsFactory.createSkinnedMetalTheme("SkinnedLookAndFeel");
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Unable to set skinned LookAndFeel", e);
        }

        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        JHtmlLabel messageLabel = skinsFactory
            .createSkinnedJHtmlLabel("ToggleAclDialogMessage", hyperlinkListener);
        messageLabel.setText("File privacy setting:");
        messageLabel.setHorizontalAlignment(JLabel.CENTER);

        privateRadioButton = skinsFactory.createSkinnedJRadioButton("ToggleAclDialogPrivateRadioButton");
        privateRadioButton.setText("Private file");
        publicRadioButton = skinsFactory.createSkinnedJRadioButton("ToggleAclDialogPublicRadioButton");
        publicRadioButton.setText("Public file");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(privateRadioButton);
        buttonGroup.add(publicRadioButton);

        publicRadioButton.setSelected(isPublicObject);
        privateRadioButton.setSelected(!isPublicObject);

        JButton okButton = skinsFactory.createSkinnedJButton("ToggleAclDialogOkButton");
        okButton.setName("OK");
        okButton.setText("OK");
        okButton.addActionListener(this);
        this.getRootPane().setDefaultButton(okButton);

        JPanel buttonsPanel = skinsFactory.createSkinnedJPanel("ToggleAclDialogButtonsPanel");
        buttonsPanel.setLayout(new GridBagLayout());
        buttonsPanel.add(privateRadioButton, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        buttonsPanel.add(publicRadioButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        buttonsPanel.add(okButton, new GridBagConstraints(0, 1,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsZero, 0, 0));

        int row = 0;
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(messageLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        this.getContentPane().add(buttonsPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }

    public void actionPerformed(ActionEvent e) {
        if ("OK".equals(e.getActionCommand())) {
            this.setVisible(false);
        }
    }

    public static void main(String[] args) {
        JFrame ownerFrame = new JFrame("Test");
        ToggleAclDialog dialog = new ToggleAclDialog(ownerFrame, false, null, new Properties());
        dialog.setVisible(true);

        dialog.dispose();
        ownerFrame.dispose();
    }

}
