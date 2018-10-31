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
package org.jets3t.apps.cockpit.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jets3t.apps.cockpit.CockpitPreferences;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.security.EncryptionUtil;

/**
 * Dialog box for managing Cockpit Preferences.
 *
 * @author James Murty
 */
public class PreferencesDialog extends JDialog implements ActionListener, ChangeListener {
    private static final long serialVersionUID = 4017680813954709789L;

    private static PreferencesDialog preferencesDialog = null;

    private CockpitPreferences cockpitPreferences = null;

    private Frame ownerFrame = null;
    private HyperlinkActivatedListener hyperlinkListener = null;

    private ButtonGroup aclButtonGroup = null;
    private ButtonGroup compressButtonGroup = null;
    private ButtonGroup encryptButtonGroup = null;
    private JPasswordField encryptPasswordField = null;
    private JPasswordField confirmPasswordField = null;
    private JComboBox encryptAlgorithmComboBox = null;
    private JComboBox storageClassComboBox = null;
    private JButton okButton = null;
    private JButton cancelButton = null;
    private JCheckBox rememberPreferencesCheckBox = null;
    private JTabbedPane tabbedPane = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    /**
     * Creates a modal dialog box with a title.
     *
     * @param owner
     * the frame within which this dialog will be displayed and centred.
     * @param jets3tHomeDirectory
     */
    private PreferencesDialog(CockpitPreferences cockpitPreferences, Frame owner,
        HyperlinkActivatedListener hyperlinkListener)
    {
        super(owner, "Cockpit Preferences", true);
        this.cockpitPreferences = cockpitPreferences;
        this.ownerFrame = owner;
        this.hyperlinkListener = hyperlinkListener;
        this.initGui();
    }

    /**
     * Initialises all GUI elements.
     */
    private void initGui() {
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        String introductionText = "<html><center>Configure Cockpit's preferences</center></html>";
        JHtmlLabel introductionLabel = new JHtmlLabel(introductionText, hyperlinkListener);
        introductionLabel.setHorizontalAlignment(JLabel.CENTER);

        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        okButton = new JButton("Apply preferences");
        okButton.setActionCommand("ApplyPreferences");
        okButton.addActionListener(this);

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 1478626539912658292L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.add(cancelButton, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        buttonsPanel.add(okButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Uploads preferences pane.
        JPanel uploadPrefsPanel = new JPanel(new GridBagLayout());
        int row = 0;

        JHtmlLabel storageClassLabel = new JHtmlLabel(
            "<html>Storage Class<br><font size=\"-2\">Choose a storage class " +
            "to balance cost and redundancy</html>", hyperlinkListener);
        uploadPrefsPanel.add(storageClassLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        String[] storageClasses = new String[] {
            S3Object.STORAGE_CLASS_STANDARD,
            S3Object.STORAGE_CLASS_REDUCED_REDUNDANCY
        };

        storageClassComboBox = new JComboBox(storageClasses);
        storageClassComboBox.addActionListener(this);
        uploadPrefsPanel.add(storageClassComboBox, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        JHtmlLabel aclPrefsLabel = new JHtmlLabel(
            "ACL Permissions", hyperlinkListener);
        uploadPrefsPanel.add(aclPrefsLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        aclButtonGroup = new ButtonGroup();
        JRadioButton aclPrivateButton = new JRadioButton("Private", true);
        aclPrivateButton.setActionCommand(CockpitPreferences.UPLOAD_ACL_PERMISSION_PRIVATE);
        JRadioButton aclPublicReadButton = new JRadioButton("Public read");
        aclPublicReadButton.setActionCommand(CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ);
        JRadioButton aclPublicReadWriteButton = new JRadioButton("Public read and write");
        aclPublicReadWriteButton.setActionCommand(CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ_WRITE);
        aclButtonGroup.add(aclPrivateButton);
        aclButtonGroup.add(aclPublicReadButton);
        aclButtonGroup.add(aclPublicReadWriteButton);
        JPanel aclPrefsRadioPanel = new JPanel(new GridBagLayout());
        aclPrefsRadioPanel.add(aclPrivateButton, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        aclPrefsRadioPanel.add(aclPublicReadButton, new GridBagConstraints(1, 0,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        aclPrefsRadioPanel.add(aclPublicReadWriteButton, new GridBagConstraints(2, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        uploadPrefsPanel.add(aclPrefsRadioPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        JHtmlLabel compressionPrefsLabel = new JHtmlLabel(
            "Compress files with GZip?", hyperlinkListener);
        uploadPrefsPanel.add(compressionPrefsLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        compressButtonGroup = new ButtonGroup();
        JRadioButton compressNoButton = new JRadioButton("Don't compress", true);
        compressNoButton.setActionCommand("INACTIVE");
        JRadioButton compressYesButton = new JRadioButton("Compress");
        compressYesButton.setActionCommand("ACTIVE");
        compressButtonGroup.add(compressNoButton);
        compressButtonGroup.add(compressYesButton);
        JPanel compressPrefsRadioPanel = new JPanel(new GridBagLayout());
        compressPrefsRadioPanel.add(compressNoButton, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        compressPrefsRadioPanel.add(compressYesButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        uploadPrefsPanel.add(compressPrefsRadioPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        JHtmlLabel encryptionPrefsLabel = new JHtmlLabel(
            "<html>Encrypt Uploaded Files?<br><font size=\"-2\">If encryption is turned on you must " +
            "also set the Encryption password</html>", hyperlinkListener);
        uploadPrefsPanel.add(encryptionPrefsLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        encryptButtonGroup = new ButtonGroup();
        JRadioButton encryptNoButton = new JRadioButton("Don't encrypt", true);
        encryptNoButton.setActionCommand("INACTIVE");
        JRadioButton encryptYesButton = new JRadioButton("Encrypt");
        encryptYesButton.setActionCommand("ACTIVE");
        encryptButtonGroup.add(encryptNoButton);
        encryptButtonGroup.add(encryptYesButton);
        encryptPasswordField = new JPasswordField();
        confirmPasswordField = new JPasswordField();
        JPanel encryptPrefsRadioPanel = new JPanel(new GridBagLayout());
        encryptPrefsRadioPanel.add(encryptNoButton, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        encryptPrefsRadioPanel.add(encryptYesButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        uploadPrefsPanel.add(encryptPrefsRadioPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));


        // Determine the default crypto algorithm from jets3t.properties.
        String encryptAlgorithm = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
            .getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
        // Determine the available PBE algorithms.
        String[] algorithms = EncryptionUtil.listAvailablePbeCiphers(true);

        JPanel encryptionPrefsPanel = new JPanel(new GridBagLayout());
        encryptionPrefsPanel.add(new JHtmlLabel("Password", hyperlinkListener), new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        encryptionPrefsPanel.add(encryptPasswordField, new GridBagConstraints(0, 1,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        encryptionPrefsPanel.add(new JHtmlLabel("Confirm Password", hyperlinkListener), new GridBagConstraints(0, 2,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        encryptionPrefsPanel.add(confirmPasswordField, new GridBagConstraints(0, 3,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        encryptionPrefsPanel.add(new JHtmlLabel("Algorithm for Encrypting Uploads", hyperlinkListener), new GridBagConstraints(0, 4,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        encryptAlgorithmComboBox = new JComboBox(algorithms);
        encryptAlgorithmComboBox.addActionListener(this);
        encryptAlgorithmComboBox.setSelectedItem(encryptAlgorithm.toUpperCase());
        encryptionPrefsPanel.add(encryptAlgorithmComboBox, new GridBagConstraints(0, 5,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        String algorithmExplanation =
            "<html>This algorithm need not be set correctly to download<br>" +
                   "encrypted objects, as Cockpit will detect and apply the<br>" +
                   "appropriate algorithm.<br><br>" +
                   "<font size=\"-2\">" +
                   "The algorithm list only includes the Password-Based (PBE) algorithms<br>" +
                   "available to Java programs on your system.</font></html>";
        encryptionPrefsPanel.add(new JHtmlLabel(algorithmExplanation, hyperlinkListener), new GridBagConstraints(0, 6,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        // Padding
        encryptionPrefsPanel.add(new JLabel(), new GridBagConstraints(0, 7,
            1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, insetsDefault, 0, 0));


        // Tabbed Pane.
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(this);
        tabbedPane.add(uploadPrefsPanel, "Uploads");
        tabbedPane.add(encryptionPrefsPanel, "Encryption");

        // Remember preferences option
        rememberPreferencesCheckBox = new JCheckBox("Remember my preferences on this computer?");
        rememberPreferencesCheckBox.setHorizontalAlignment(JCheckBox.CENTER);
        String rememberPreferencesExplanation =
            "<html><font size=\"-2\">" +
            "Your encryption password will <b>never</b> be remembered." +
            "</font></html>";
        JHtmlLabel rememberPreferencesLabel =
            new JHtmlLabel(rememberPreferencesExplanation, hyperlinkListener);
        rememberPreferencesLabel.setHorizontalAlignment(JLabel.CENTER);

        row = 0;
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(introductionLabel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        this.getContentPane().add(tabbedPane, new GridBagConstraints(0, row++,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        this.getContentPane().add(rememberPreferencesCheckBox, new GridBagConstraints(0, row++,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
        this.getContentPane().add(rememberPreferencesLabel, new GridBagConstraints(0, row++,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 10, 0, 10), 0, 0));
        this.getContentPane().add(buttonsPanel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Load preferences from CockpitPreferences object.
        rememberPreferencesCheckBox.setSelected(cockpitPreferences.isRememberPreferences());

        String aclPermission = cockpitPreferences.getUploadACLPermission();
        if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PRIVATE.equals(aclPermission)) {
            aclPrivateButton.setSelected(true);
        } else if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ.equals(aclPermission)) {
            aclPublicReadButton.setSelected(true);
        } else if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ_WRITE.equals(aclPermission)) {
            aclPublicReadWriteButton.setSelected(true);
        }

        if (cockpitPreferences.isUploadCompressionActive()) {
            compressYesButton.setSelected(true);
        } else {
            compressNoButton.setSelected(true);
        }

        if (cockpitPreferences.isUploadEncryptionActive()) {
            encryptYesButton.setSelected(true);
        } else {
            encryptNoButton.setSelected(true);
        }
        encryptAlgorithmComboBox.setSelectedItem(cockpitPreferences.getEncryptionAlgorithm());
        storageClassComboBox.setSelectedItem(cockpitPreferences.getUploadStorageClass());

        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }

    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            if ("ACTIVE".equals(encryptButtonGroup.getSelection().getActionCommand())
                && encryptPasswordField.getPassword().length == 0)
            {
                ErrorDialog.showDialog(ownerFrame, hyperlinkListener,
                    "If encryption is set for Uploads the Encryption password cannot be empty", null);
                return;
            }

            if (encryptPasswordField.getPassword().length > 0 || confirmPasswordField.getPassword().length > 0) {
                String password = new String(encryptPasswordField.getPassword());
                String confirmedPassword = new String(confirmPasswordField.getPassword());

                if (!password.equals(confirmedPassword)) {
                    ErrorDialog.showDialog(ownerFrame, hyperlinkListener,
                        "You entered an encryption password that does not match the password in the Confirm Password field", null);
                    return;
                }
            }

            // Save preferences to CockpitPreferences object.
            cockpitPreferences.setRememberPreferences(
                rememberPreferencesCheckBox.isSelected());
            cockpitPreferences.setUploadACLPermission(
                aclButtonGroup.getSelection().getActionCommand());
            cockpitPreferences.setUploadCompressionActive(
                "ACTIVE".equals(compressButtonGroup.getSelection().getActionCommand()));
            cockpitPreferences.setUploadEncryptionActive(
                "ACTIVE".equals(encryptButtonGroup.getSelection().getActionCommand()));
            cockpitPreferences.setUploadStorageClass(
                (String) storageClassComboBox.getSelectedItem());
            cockpitPreferences.setEncryptionPassword(
                new String(encryptPasswordField.getPassword()));
            cockpitPreferences.setEncryptionAlgorithm(
                (String) encryptAlgorithmComboBox.getSelectedItem());

            this.setVisible(false);
        } else if (e.getSource().equals(cancelButton)) {
            this.setVisible(false);
        }
    }

    public void stateChanged(ChangeEvent e) {
        // Ignore these events.
    }

    /**
     * Displays the Preferences dialog box and waits until the user selects to cancel the dialog or
     * to save the properties.
     *
     * @param cockpitPreferences
     * an object with the current cockpit preferences, which will be updated to reflect any changes
     * the user makes to their preferences.
     * @param owner
     * the frame that will own this dialog.
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     */
    public static void showDialog(CockpitPreferences cockpitPreferences, Frame owner,
        HyperlinkActivatedListener hyperlinkListener)
    {
        if (preferencesDialog == null) {
            preferencesDialog = new PreferencesDialog(cockpitPreferences, owner, hyperlinkListener);
        }
        preferencesDialog.setVisible(true);
    }

}
