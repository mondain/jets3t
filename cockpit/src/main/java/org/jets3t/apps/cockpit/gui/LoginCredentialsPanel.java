/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2010 James Murty, 2008 Zmanda Inc.
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;

/**
 * A panel for obtaining a user's credentials. The panel prompts for an Access Key and
 * an Secret Key, and optionally for a Friendly name for an account.
 *
 * @author James Murty
 * @author Nikolas Coukouma
 */
public class LoginCredentialsPanel extends JPanel implements ItemListener {
    private static final long serialVersionUID = 5819631423081597078L;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);
    private final Insets insetsZero = new Insets(0, 0, 0, 0);

    private HyperlinkActivatedListener hyperlinkListener = null;
    private JTextField accessKeyTextField = null;
    private JPasswordField secretKeyPasswordField = null;
    private JCheckBox useDevPayCheckBox = null;
    private JTextField awsUserTokenTextField = null;
    private AWSDevPayProductPanel awsProductPanel = null;
    private JTextField friendlyNameTextField = null;
    private boolean askForFriendlyName = false;

    private Component[] awsDevPayComponents = null;

    public LoginCredentialsPanel(boolean askForFriendlyName, HyperlinkActivatedListener hyperlinkListener) {
        super(new GridBagLayout());
        this.hyperlinkListener = hyperlinkListener;
        this.askForFriendlyName = askForFriendlyName;

        initGui();
    }

    private void initGui() {
        // Textual information.
        String descriptionText =
            "<html><center>View your " +
            "<a href=\"http://aws-portal.amazon.com/gp/aws/developer/account/index.html?ie=UTF8&action=access-key\" " +
            "target=\"_blank\">AWS Access Identifiers</a> on Amazon's web site.<br></center></html>";
        String friendlyNameLabelText =
            "Nickname";
        String friendlyNameTooltipText =
            "A nickname for your stored account";
        String accessKeyLabelText =
            "Access Key";
        String accessKeyTooltipText =
            "Your access key";
        String secretKeyLabelText =
            "Secret Key";
        String secretKeyTooltipText =
            "Your secret key";
        String useDevPayButtonText =
            "Use AWS DevPay";
        String awsUserTokenLabelText =
            "DevPay User Token";
        String awsUserTokenTooltipText =
            "Your DevPay user token";

        // Components.
        JHtmlLabel descriptionLabel = new JHtmlLabel(descriptionText, hyperlinkListener);
        descriptionLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel friendlyNameLabel = new JHtmlLabel(friendlyNameLabelText, hyperlinkListener);
        friendlyNameTextField = new JTextField();
        friendlyNameTextField.setToolTipText(friendlyNameTooltipText);
        JHtmlLabel accessKeyLabel = new JHtmlLabel(accessKeyLabelText, hyperlinkListener);
        accessKeyTextField = new JTextField();
        accessKeyTextField.setToolTipText(accessKeyTooltipText);
        JHtmlLabel secretKeyLabel = new JHtmlLabel(secretKeyLabelText, hyperlinkListener);
        secretKeyPasswordField = new JPasswordField();
        secretKeyPasswordField.setToolTipText(secretKeyTooltipText);

        useDevPayCheckBox = new JCheckBox(useDevPayButtonText);
        useDevPayCheckBox.setSelected(false);
        useDevPayCheckBox.addItemListener(this);
        JHtmlLabel awsUserTokenLabel = new JHtmlLabel(awsUserTokenLabelText, hyperlinkListener);
        awsUserTokenTextField = new JTextField();
        awsUserTokenTextField.setToolTipText(awsUserTokenTooltipText);
        awsProductPanel = new AWSDevPayProductPanel();

        int row = 0;
        add(descriptionLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        if (askForFriendlyName) {
            friendlyNameTextField.setText("My Credentials");

            add(friendlyNameLabel, new GridBagConstraints(0, row++,
                1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
            add(friendlyNameTextField, new GridBagConstraints(0, row++,
                1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        }
        add(accessKeyLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(accessKeyTextField, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(secretKeyLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(secretKeyPasswordField, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(useDevPayCheckBox, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // These items are displayed conditionally when useDevPayCheckBox is checked
        add(awsUserTokenLabel, new GridBagConstraints(0, row++,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(awsUserTokenTextField, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(awsProductPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        // Padder.
        add(new JLabel(), new GridBagConstraints(0, row++,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        awsDevPayComponents = new Component[] {awsUserTokenLabel, awsUserTokenTextField, awsProductPanel};
        // Make DevPay GUI elements inivisible initially.
        for (int i = 0; i < awsDevPayComponents.length; i++) {
            awsDevPayComponents[i].setVisible(useDevPayCheckBox.isSelected());
        }

        this.setPreferredSize(new Dimension(400, 350));
    }

    public void itemStateChanged(ItemEvent e) {
        ItemSelectable source = e.getItemSelectable();
        if (source == useDevPayCheckBox) {
            for (int i = 0; i < awsDevPayComponents.length; i++) {
                awsDevPayComponents[i].setVisible(useDevPayCheckBox.isSelected());
            }
        }
    }

    /**
     * @return
     * the Access Key provided by the user.
     */
    public String getAccessKey() {
        return accessKeyTextField.getText().trim();
    }

    /**
     * @return
     * the Secret Key provided by the user.
     */
    public String getSecretKey() {
        return new String(secretKeyPasswordField.getPassword()).trim();
    }

    /**
     * @return
     * whether or not DevPay authentication should be used
     */
    public boolean getUsingDevPay() {
        return useDevPayCheckBox.isSelected();
    }

    /**
     * @return
     * the user token provided by the user.
     */
    public String getAWSUserToken() {
        return awsUserTokenTextField.getText().trim();
    }

    /**
     * @return
     * the product token provided by the user.
     */
    public String getAWSProductToken() {
        return awsProductPanel.getAWSProductToken();
    }

    /**
     * @return
     * the Friendly Name (nickname) provided by the user, or an empty string if the user was not
     * prompted to provide one (the askForFriendlyName option was false).
     */
    public String getFriendlyName() {
        return friendlyNameTextField.getText();
    }

    /**
     * Verifies that the user has provided the correct inputs, and returns a list
     * of error messages if not.
     *
     * @return
     * an empty array if there a no input errors, otherwise the array will contain
     * a list of error messages.
     */
    public String[] checkForInputErrors() {
        ArrayList errors = new ArrayList();

        if (getAccessKey().trim().length() == 0) {
            errors.add("Access Key must be provided");
        }

        if (getSecretKey().trim().length() == 0) {
            errors.add("Secret Key must be provided");
        }

        if (getUsingDevPay()) {
            if (getAWSUserToken().trim().length() == 0) {
                errors.add("DevPay User Token must be provided");
            }
            if (getAWSProductToken().trim().length() == 0) {
                errors.add("DevPay Product Token must be provided");
            }
        }
        return (String[]) errors.toArray(new String[errors.size()]);
    }

}
