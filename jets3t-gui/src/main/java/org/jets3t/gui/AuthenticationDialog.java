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

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;


/**
 * Dialog box for a user to enter authentication information for HTTP communication, such as
 * NT or Basic authentication.
 *
 * @author James Murty
 */
public class AuthenticationDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -8112836668013270984L;

    private final Insets insetsDefault = new Insets(5, 7, 5, 7);
    private JTextField domainField = null;
    private JTextField usernameField = null;
    private JPasswordField passwordField = null;

    private boolean isNtAuthentication = false;

    private String domain = "";
    private String user = "";
    private String password = "";

    /**
     * Construct modal dialog for display over a Frame.
     *
     * @param owner     Frame over which this dialog will be displayed and centred.
     * @param title     the dialog's title text
     * @param question  the question/statement to prompt the user for their password, may be html
     *                  compatible with {@link JHtmlLabel}
     * @param isNtAuthentication   if true a domain name is required in addition to the username and password.
     */
    public AuthenticationDialog(Frame owner, String title, String question, boolean isNtAuthentication) {
        super(owner, title, true);
        this.isNtAuthentication = isNtAuthentication;
        initGui(question);
    }

    /**
     * Construct modal dialog for display over another Dialog.
     *
     * @param owner     Dialog over which this dialog will be displayed and centred.
     * @param title     the dialog's title text
     * @param question  the question/statement to prompt the user for their password
     * @param isNtAuthentication   if true a domain name is required in addition to the username and password.
     */
    public AuthenticationDialog(Dialog owner, String title, String question, boolean isNtAuthentication) {
        super(owner, title, true);
        this.isNtAuthentication = isNtAuthentication;
        initGui(question);
    }

    /**
     * Initialises all GUI elements.
     *
     * @param question  the question/statement to prompt the user for their password
     */
    private void initGui(String question) {
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        int rowIndex = 0;

        JPanel container = new JPanel(new GridBagLayout());
        JHtmlLabel questionLabel = new JHtmlLabel(question, null);
        container.add(questionLabel, new GridBagConstraints(0, rowIndex++, 2, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        domainField = new JTextField();
        usernameField = new JTextField();
        passwordField = new JPasswordField();

        if (isNtAuthentication) {
            container.add(new JLabel("Domain:"), new GridBagConstraints(0, rowIndex, 1, 1, 0, 0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
            container.add(domainField, new GridBagConstraints(1, rowIndex++, 1, 1, 1, 0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        }

        container.add(new JLabel("User:"), new GridBagConstraints(0, rowIndex, 1, 1, 0, 0,
            GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        container.add(usernameField, new GridBagConstraints(1, rowIndex++, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        container.add(new JLabel("Password:"), new GridBagConstraints(0, rowIndex, 1, 1, 0, 0,
            GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        container.add(passwordField, new GridBagConstraints(1, rowIndex++, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        JPanel buttonsContainer = new JPanel(new GridBagLayout());
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        JButton okButton = new JButton("Authenticate me");
        okButton.setActionCommand("OK");
        okButton.setDefaultCapable(true);
        okButton.addActionListener(this);
        buttonsContainer.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));
        buttonsContainer.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        container.add(buttonsContainer, new GridBagConstraints(0, rowIndex++, 2, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 3717631976908670386L;

            public void actionPerformed(ActionEvent actionEvent) {
                cancelButton.doClick();
            }
        });

        this.getContentPane().add(container);
        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }

    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        if ("OK".equals(e.getActionCommand())) {
            if (isNtAuthentication) {
                this.domain = domainField.getText();
            }
            this.user = usernameField.getText();
            this.password = new String(passwordField.getPassword());
        } else if ("Cancel".equals(e.getActionCommand())) {
            this.domain = "";
            this.user = "";
            this.password = "";
        }
        this.setVisible(false);
    }

    /**
     * @return
     * the domain entered by the user, or null if the dialog was cancelled or NT authentication wasn't used.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return
     * the user name entered by the user, or null if the dialog was cancelled.
     */
    public String getUser() {
        return user;
    }

    /**
     * @return
     * the password entered by the user, or null if the dialog was cancelled.
     */
    public String getPassword() {
        return password;
    }

}
