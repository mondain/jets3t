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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;

/**
 * A panel for prompting a user to provide a passphrase and password used to store
 * or access their credentials in a storage service.
 *
 * @author James Murty
 */
public class LoginPassphrasePanel extends JPanel {
    private static final long serialVersionUID = -5554177389537270280L;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    private HyperlinkActivatedListener hyperlinkListener = null;
    private JTextField passphraseTextField = null;
    private JPasswordField passwordPasswordField = null;

    public LoginPassphrasePanel(HyperlinkActivatedListener hyperlinkListener) {
        super(new GridBagLayout());
        this.hyperlinkListener = hyperlinkListener;

        initGui();
    }

    private void initGui() {
        // Textual information.
        String descriptionText =
            "<html><center>" +
            "Your credentials are stored in an encrypted object in your online storage account. " +
            "To access your credentials you must provide your unique passphrase and password." +
            "<br><br>" +
            "<font size=\"-2\">You need to store your credentials before you can use this login method.</font>" +
            "</center></html>";

        String passphraseLabelText =
            "Passphrase";
        String passphraseTooltipText =
            "An easy to remember phrase of 6 characters or more that is unlikely to be used by anyone else";
        String passwordLabelText =
            "Password";
        String passwordTooltipText =
            "A password of at least 6 characters";

        // Components.
        JHtmlLabel descriptionLabel = new JHtmlLabel(descriptionText, hyperlinkListener);
        descriptionLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel passphraseLabel = new JHtmlLabel(passphraseLabelText, hyperlinkListener);
        passphraseTextField = new JTextField();
        passphraseTextField.setName("LoginPassphrasePanel.Passphrase");
        passphraseTextField.setToolTipText(passphraseTooltipText);
        JHtmlLabel passwordLabel = new JHtmlLabel(passwordLabelText, hyperlinkListener);
        passwordPasswordField = new JPasswordField();
        passwordPasswordField.setName("LoginPassphrasePanel.Password");
        passwordPasswordField.setToolTipText(passwordTooltipText);

        int row = 0;
        add(descriptionLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        add(passphraseLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(passphraseTextField, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(passwordLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(passwordPasswordField, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Padder.
        add(new JLabel(), new GridBagConstraints(0, row++,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
    }

    /**
     * @return
     * the passphrase provided by the user (may be an empty string)
     */
    public String getPassphrase() {
        return passphraseTextField.getText();
    }

    /**
     * @return
     * the password provided by the user (may be an empty string)
     */
    public String getPassword() {
        return new String(passwordPasswordField.getPassword());
    }

}
