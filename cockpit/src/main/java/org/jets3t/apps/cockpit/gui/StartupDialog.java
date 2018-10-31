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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.ProgressDialog;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.ServiceException;
import org.jets3t.service.StorageService;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.model.StorageObject;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.AWSDevPayCredentials;
import org.jets3t.service.security.GSCredentials;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ServiceUtils;

import contribs.com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Dialog box for obtaining a user's service credentials, and performing other startup
 * tasks such as loading properties files.
 * <p>
 *
 * @author James Murty
 * @author Nikolas Coukouma
 */
public class StartupDialog extends JDialog implements ActionListener, ChangeListener {
    private static final long serialVersionUID = -2520889480615456474L;

    private static final Log log = LogFactory.getLog(StartupDialog.class);

    public static final String EMPTY_PASSWORD_SURROGATE = "NONE";

    private Frame ownerFrame = null;
    private HyperlinkActivatedListener hyperlinkListener = null;
    private Jets3tProperties myProperties = null;

    private ProviderCredentials credentials = null;

    private JRadioButton targetS3 = null;
    private JRadioButton targetGS = null;
    private JButton okButton = null;
    private JButton cancelButton = null;
    private JButton storeCredentialsButton = null;
    private JTabbedPane tabbedPane = null;
    private LoginPassphrasePanel loginPassphrasePanel = null;
    private LoginLocalFolderPanel loginLocalFolderPanel = null;
    private LoginCredentialsPanel loginCredentialsPanel = null;

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    private static final int LOGIN_MODE_PASSPHRASE = 0;
    private static final int LOGIN_MODE_LOCAL_FOLDER = 1;
    private static final int LOGIN_MODE_DIRECT = 2;
    private int loginMode = LOGIN_MODE_PASSPHRASE;

    /**
     * Creates a modal dialog box with a title.
     *
     * @param owner
     * the frame within which this dialog will be displayed and centred.
     * @param hyperlinkListener
     */
    public StartupDialog(Frame owner, Jets3tProperties properties, HyperlinkActivatedListener hyperlinkListener) {
        super(owner, "Cockpit Login", true);
        this.ownerFrame = owner;
        this.hyperlinkListener = hyperlinkListener;
        this.myProperties = properties;
        this.initGui();
    }

    /**
     * Initialises all GUI elements.
     */
    private void initGui() {
        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        cancelButton = new JButton("Don't log in");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        storeCredentialsButton = new JButton("Store Credentials");
        storeCredentialsButton.setActionCommand("StoreCredentials");
        storeCredentialsButton.addActionListener(this);
        okButton = new JButton("Log in");
        okButton.setActionCommand("LogIn");
        okButton.addActionListener(this);

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -1742280851624947873L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.add(cancelButton, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        buttonsPanel.add(storeCredentialsButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        buttonsPanel.add(okButton, new GridBagConstraints(2, 0,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));

        loginPassphrasePanel = new LoginPassphrasePanel(hyperlinkListener);
        loginLocalFolderPanel = new LoginLocalFolderPanel(ownerFrame, hyperlinkListener);
        loginCredentialsPanel = new LoginCredentialsPanel(false, hyperlinkListener);

        // Target storage service selection
        targetS3 = new JRadioButton("Amazon S3");
        targetS3.setSelected(true);
        targetGS = new JRadioButton("Google Storage");

        ButtonGroup targetButtonGroup = new ButtonGroup();
        targetButtonGroup.add(targetS3);
        targetButtonGroup.add(targetGS);

        JPanel targetServicePanel = new JPanel(new GridBagLayout());
        targetServicePanel.add(targetS3, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        targetServicePanel.add(targetGS, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));

        // Tabbed Pane.
        tabbedPane = new JTabbedPane();
        tabbedPane.addChangeListener(this);
        tabbedPane.add(loginPassphrasePanel, "Online");
        tabbedPane.add(loginLocalFolderPanel, "Local Folder");
        tabbedPane.add(loginCredentialsPanel, "Direct Login");

        int row = 0;
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(targetServicePanel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        this.getContentPane().add(tabbedPane, new GridBagConstraints(0, row++,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));
        this.getContentPane().add(buttonsPanel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        this.pack();
        this.setSize(500, 430);
        this.setLocationRelativeTo(this.getOwner());
    }

    public boolean isTargetS3() {
        return targetS3.isSelected();
    }

    protected StorageService getStorageService()
        throws S3ServiceException
    {
        if (targetS3.isSelected()) {
            return new RestS3Service(credentials);
        } else {
            // Override endpoint property in JetS3t properties
            Jets3tProperties gsProperties = Jets3tProperties.getInstance(
                Constants.JETS3T_PROPERTIES_FILENAME);
            gsProperties.setProperty(
                "s3service.s3-endpoint", Constants.GS_DEFAULT_HOSTNAME);
            return new RestS3Service(credentials, null, null, gsProperties);
        }
    }

    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource().equals(okButton)) {
            if (loginMode == LOGIN_MODE_PASSPHRASE) {
                retrieveCredentialsFromStorageService(
                    loginPassphrasePanel.getPassphrase(), loginPassphrasePanel.getPassword());
            } else if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
                retrieveCredentialsFromDirectory(loginLocalFolderPanel.getHomeFolder(),
                    loginLocalFolderPanel.getCredentialsFile(), loginLocalFolderPanel.getPassword());
            } else if (loginMode == LOGIN_MODE_DIRECT) {
                String[] inputErrors = loginCredentialsPanel.checkForInputErrors();
                if (inputErrors.length > 0) {
                    String errorMessages = "<html>Please correct the following errors:<ul>";
                    for (int i = 0; i < inputErrors.length; i++) {
                        errorMessages += "<li>" + inputErrors[i] + "</li>";
                    }
                    errorMessages += "</ul></html>";

                    ErrorDialog.showDialog(this, null, errorMessages, null);
                } else {
                    if (loginCredentialsPanel.getUsingDevPay()) {
                        this.credentials = new AWSDevPayCredentials(
                            loginCredentialsPanel.getAccessKey(),
                            loginCredentialsPanel.getSecretKey(),
                            loginCredentialsPanel.getAWSUserToken(),
                            loginCredentialsPanel.getAWSProductToken(),
                            loginCredentialsPanel.getFriendlyName());
                    } else {
                        if (targetS3.isSelected()) {
                            this.credentials = new AWSCredentials(
                                loginCredentialsPanel.getAccessKey(),
                                loginCredentialsPanel.getSecretKey(),
                                loginCredentialsPanel.getFriendlyName());
                        } else {
                            this.credentials = new GSCredentials(
                                loginCredentialsPanel.getAccessKey(),
                                loginCredentialsPanel.getSecretKey(),
                                loginCredentialsPanel.getFriendlyName());
                        }
                    }
                    this.setVisible(false);
                }
            }
        } else if (e.getSource().equals(storeCredentialsButton)) {
            if (loginMode == LOGIN_MODE_PASSPHRASE) {
                storeCredentialsInStorageService(
                    loginPassphrasePanel.getPassphrase(), loginPassphrasePanel.getPassword());
            } else if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
                storeCredentialsInDirectory(
                    loginLocalFolderPanel.getHomeFolder(), loginLocalFolderPanel.getPassword());
            } else if (loginMode == LOGIN_MODE_DIRECT) {
                throw new IllegalStateException("Cannot store credentials from Direct Login panel");
            }
        } else if (e.getSource().equals(cancelButton)) {
            this.credentials = null;
            this.setVisible(false);
        }
    }

    public void stateChanged(ChangeEvent e) {
        if (e.getSource().equals(tabbedPane)) {
            loginMode = tabbedPane.getSelectedIndex();
            changedLoginMode();
        }
    }

    private void changedLoginMode() {
        if (loginMode == LOGIN_MODE_PASSPHRASE) {
            storeCredentialsButton.setEnabled(true);
        } else if (loginMode == LOGIN_MODE_LOCAL_FOLDER) {
            storeCredentialsButton.setEnabled(true);
        } else if (loginMode == LOGIN_MODE_DIRECT) {
            storeCredentialsButton.setEnabled(false);
        } else {
            throw new IllegalStateException("Invalid value for loginMode: " + loginMode);
        }
    }

    private String generateBucketNameFromPassphrase(String passphrase) throws Exception {
        return "jets3t-" + ServiceUtils.toHex(
            ServiceUtils.computeMD5Hash(passphrase.getBytes(Constants.DEFAULT_ENCODING)));
    }

    private String generateObjectKeyFromPassphrase(String passphrase, String password) throws Exception {
        String combinedString = passphrase + password;
        return ServiceUtils.toHex(
            ServiceUtils.computeMD5Hash(combinedString.getBytes(Constants.DEFAULT_ENCODING)))
            + "/jets3t.credentials";
    }

    private boolean validPassphraseInputs(String passphrase, String password) {
        String invalidInputsMessage = "";
        if (passphrase.length() < 6) {
            invalidInputsMessage += "Passphrase must be at least 6 characters.";
        }
        if (password.length() < 6) {
            invalidInputsMessage += (invalidInputsMessage.length() > 0
                ? " and password"
                : "Password")
                + " must be at least 6 characters";
        }
        if (invalidInputsMessage.length() > 0) {
            ErrorDialog.showDialog(this, hyperlinkListener, invalidInputsMessage, null);
            return false;
        } else {
            return true;
        }
    }

    private boolean validFolderInputs(boolean isStoreAction, File directory,
        File credentialsFile, String password, boolean allowLegacyPassword)
    {
        if (password.length() < 6) {
            if (allowLegacyPassword) {
                // Legacy password allowed for login, an error will be displayed later if it's incorrect.
            } else if (EMPTY_PASSWORD_SURROGATE.equals(password)) {
                // Surrogate empty password was used, not an error.
            } else {
                ErrorDialog.showDialog(this, hyperlinkListener,
                    "Password must be at least 6 characters. " +
                    "If you do not wish to set a password, use the password " +
                    EMPTY_PASSWORD_SURROGATE + ".",
                    null);
                return false;
            }
        }
        if (!directory.exists() || !directory.canWrite()) {
            String invalidInputsMessage = "Directory '" + directory.getAbsolutePath()
            + "' does not exist or cannot be written to.";
            ErrorDialog.showDialog(this, hyperlinkListener, invalidInputsMessage, null);
            return false;
        }
        if (credentialsFile == null && !isStoreAction) {
            String invalidInputsMessage = "You must choose which stored login to use";
            ErrorDialog.showDialog(this, hyperlinkListener, invalidInputsMessage, null);
            return false;
        }
        return true;
    }

    private void retrieveCredentialsFromStorageService(String passphrase, final String password) {
        if (!validPassphraseInputs(passphrase, password)) {
            return;
        }

        final String[] bucketName = new String[1];
        final String[] credentialObjectKey = new String[1];

        try {
            bucketName[0] = generateBucketNameFromPassphrase(passphrase);
            credentialObjectKey[0] = generateObjectKeyFromPassphrase(passphrase, password);
        } catch (Exception e) {
            String message = "Unable to generate bucket name or object key";
            log.error(message, e);
            ErrorDialog.showDialog(this, hyperlinkListener, message, e);
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(
            ownerFrame, "Retrieving credentials", null);
        final StartupDialog myself = this;

        (new Thread(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressDialog.startDialog("Downloading your credentials", "", 0, 0, null, null);
                    }
                 });

                StorageObject encryptedCredentialsObject = null;

                try {
                    credentials = null;
                    StorageService service = getStorageService();
                    encryptedCredentialsObject = service.getObject(
                        bucketName[0], credentialObjectKey[0]);
                } catch (ServiceException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });

                    String errorMessage = "<html><center>Unable to find your credentials online"
                        + "<br><br>Please check your passphrase and password</center></html>";
                    log.error(errorMessage, e);
                    ErrorDialog.showDialog(myself, hyperlinkListener, errorMessage, null);
                    return;
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressDialog.updateDialog("Decrypting your credentials", null, 0);
                    }
                 });

                try {
                    if (targetS3.isSelected()) {
                        myself.credentials = AWSCredentials.load(password,
                            new BufferedInputStream(encryptedCredentialsObject.getDataInputStream()));
                    } else {
                        myself.credentials = GSCredentials.load(password,
                            new BufferedInputStream(encryptedCredentialsObject.getDataInputStream()));
                    }

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });
                    myself.setVisible(false);
                } catch (ServiceException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });

                    String errorMessage =
                        "<html><center>Unable to load your online credentials"
                        + "<br><br>Please check your password</center></html>";
                    log.error(errorMessage, e);
                    ErrorDialog.showDialog(myself, hyperlinkListener, errorMessage, null);
                }

            }
        })).start();
    }

    private void storeCredentialsInStorageService(String passphrase, String password) {
        if (!validPassphraseInputs(passphrase, password)) {
            return;
        }

        final ProviderCredentials credentials =
            CredentialsDialog.showDialog(ownerFrame,
                (loginMode == LOGIN_MODE_LOCAL_FOLDER),
                this.isTargetS3(),
                myProperties, hyperlinkListener);
        if (credentials == null) {
            return;
        }

        final String[] bucketName = new String[1];
        final String[] credentialObjectKey = new String[1];

        try {
            bucketName[0] = generateBucketNameFromPassphrase(passphrase);
            credentialObjectKey[0] = generateObjectKeyFromPassphrase(passphrase, password);
        } catch (Exception e) {
            String message = "Unable to generate bucket name or object key";
            log.error(message, e);
            ErrorDialog.showDialog(this, hyperlinkListener, message, e);
            return;
        }

        final ByteArrayInputStream[] bais = new ByteArrayInputStream[1];
        try {
            // Convert credentials into a readable input stream.
            String algorithm = myProperties.getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            credentials.save(password, baos, algorithm);
            bais[0] = new ByteArrayInputStream(baos.toByteArray());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unable to encrypt your credentials";
            log.error(message, e);
            ErrorDialog.showDialog(this, hyperlinkListener, message, e);
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(
            ownerFrame, "Storing credentials", null);
        final StartupDialog myself = this;

        (new Thread(new Runnable() {
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressDialog.startDialog("Uploading your credentials", null, 0, 0, null, null);
                    }
                 });

                try {
                    StorageObject encryptedCredentialsObject =
                        new StorageObject(credentialObjectKey[0]);
                    encryptedCredentialsObject.setDataInputStream(bais[0]);
                    encryptedCredentialsObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);

                    // Store credentials
                    StorageService service = getStorageService();
                    service.createBucket(bucketName[0]);
                    service.putObject(bucketName[0], encryptedCredentialsObject);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });

                    JOptionPane.showMessageDialog(ownerFrame,
                        "Your credentials have been stored online"
                        + "\n\nBucket name: " + bucketName[0]
                        + "\nObject key: " + credentialObjectKey[0]);
                } catch (ServiceException e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });

                    String message = "Unable to store your credentials online";
                    log.error(message, e);
                    ErrorDialog.showDialog(myself, hyperlinkListener, message, e);
                }
            }
        })).start();
    }

    private void retrieveCredentialsFromDirectory(File directory, File credentialsFile, String password) {
        if (!validFolderInputs(false, directory, credentialsFile, password, true)) {
            return;
        }

        try {
            this.credentials = ProviderCredentials.load(password, credentialsFile);
            this.setVisible(false);
        } catch (Exception e) {
            String message = "<html><center>Unable to load your credentials from the file: "
                + credentialsFile + "<br><br>Please check your password</center></html>";
            log.error(message, e);
            ErrorDialog.showDialog(this, hyperlinkListener, message, null);
        }
    }

    private void storeCredentialsInDirectory(File directory, String password) {
        if (!validFolderInputs(true, directory, null, password, false)) {
            return;
        }
        if (EMPTY_PASSWORD_SURROGATE.equals(password.trim())) {
            password = "";
        }

        ProviderCredentials myCredentials =
            CredentialsDialog.showDialog(ownerFrame, true, this.isTargetS3(),
                myProperties, hyperlinkListener);
        if (myCredentials == null) {
            return;
        }
        if (myCredentials.getFriendlyName() == null || myCredentials.getFriendlyName().length() == 0) {
            String message = "You must enter a nickname when storing your credentials";
            log.error(message);
            ErrorDialog.showDialog(this, hyperlinkListener, message, null);
            return;
        }

        File credentialsFile = new File(directory, myCredentials.getFriendlyName() + ".enc");

        try {
            String algorithm = myProperties.getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");
            myCredentials.save(password, credentialsFile, algorithm);
            loginLocalFolderPanel.clearPassword();
            loginLocalFolderPanel.refreshStoredCredentialsTable();

            JOptionPane.showMessageDialog(ownerFrame, "Your credentials have been stored in the file:\n" +
                credentialsFile.getAbsolutePath());
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unable to encrypt your credentials to a folder";
            log.error(message, e);
            ErrorDialog.showDialog(this, hyperlinkListener, message, e);
        }
    }

    public ProviderCredentials getProviderCredentials() {
        return this.credentials;
    }

    /**
     * Creates stand-alone dialog box for testing only.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        JFrame f = new JFrame();

        HyperlinkActivatedListener listener = new HyperlinkActivatedListener() {
            private static final long serialVersionUID = -225585129296632961L;

            public void followHyperlink(URL url, String target) {
                BareBonesBrowserLaunch.openURL(url.toString());
            }
        };

        StartupDialog startupDialog = new StartupDialog(f,
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME), listener);
        startupDialog.setVisible(true);
        ProviderCredentials credentials = startupDialog.getProviderCredentials();
        startupDialog.dispose();

        if (credentials != null) {
            System.out.println("Credentials: " + credentials.getLogString());
        } else {
            System.out.println("Credentials: null");
        }

        f.dispose();
    }

}
