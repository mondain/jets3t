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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.service.Constants;
import org.jets3t.service.security.ProviderCredentials;

/**
 * A panel for displaying a user's login credentials where those credentials are stored
 * in files available on the user's computer.
 *
 * @author James Murty
 */
public class LoginLocalFolderPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1500994545263522051L;

    private static final Log log = LogFactory.getLog(LoginLocalFolderPanel.class);

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);

    private Frame ownerFrame = null;
    private HyperlinkActivatedListener hyperlinkListener = null;
    private File cockpitHomeFolder = null;

    private JTextField folderPathTextField = null;
    private JTable accountNicknameTable = null;
    private ProviderCredentialsFileTableModel nicknamesTableModel = null;
    private JPasswordField passwordPasswordField = null;

    public LoginLocalFolderPanel(Frame ownerFrame, HyperlinkActivatedListener hyperlinkListener)
    {
        super(new GridBagLayout());
        this.ownerFrame = ownerFrame;
        if (Constants.DEFAULT_PREFERENCES_DIRECTORY.exists()) {
            this.cockpitHomeFolder = Constants.DEFAULT_PREFERENCES_DIRECTORY;
        } else {
            this.cockpitHomeFolder = new File(System.getProperty("user.home"));
        }
        this.hyperlinkListener = hyperlinkListener;

        initGui();
        refreshStoredCredentialsTable();
    }

    private void initGui() {
        // Textual information.
        String descriptionText =
            "<html><center>" +
            "Your credentials are stored in encrypted files in a folder on " +
            "your computer. Each stored login has a nickname." +
            "<br><font size=\"-2\">You need to store your credentials before you can use this login method.</font>" +
            "</center></html>";
        String folderTooltipText =
            "The folder containing your credentials";
        String browseButtonText =
            "Change Folder";
        String accountNicknameText =
            "Stored logins";
        String accountNicknameTooltipText =
            "Nicknames of the login credentials you have stored";
        String passwordLabelText =
            "Password";
        String passwordTooltipText =
            "The password that protects your encrypted file. "
            + "This password may be left empty if you are sure your computer cannot be compromised";

        // Components.
        JHtmlLabel descriptionLabel = new JHtmlLabel(descriptionText, hyperlinkListener);
        descriptionLabel.setHorizontalAlignment(JLabel.CENTER);
        folderPathTextField = new JTextField(this.cockpitHomeFolder.getAbsolutePath());
        folderPathTextField.setEnabled(false);
        folderPathTextField.setToolTipText(folderTooltipText);
        JButton browseButton = new JButton(browseButtonText);
        browseButton.addActionListener(this);
        JHtmlLabel accountNicknamesLabel = new JHtmlLabel(accountNicknameText, hyperlinkListener);
        nicknamesTableModel = new ProviderCredentialsFileTableModel();
        accountNicknameTable = new JTable(nicknamesTableModel);
        accountNicknameTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountNicknameTable.setShowHorizontalLines(true);
        accountNicknameTable.getTableHeader().setVisible(false);
        JScrollPane accountNicknamesScrollPane = new JScrollPane(accountNicknameTable);
        accountNicknamesScrollPane.setToolTipText(accountNicknameTooltipText);
        JHtmlLabel passwordLabel = new JHtmlLabel(passwordLabelText, hyperlinkListener);
        passwordPasswordField = new JPasswordField();
        passwordPasswordField.setToolTipText(passwordTooltipText);

        int row = 0;
        add(descriptionLabel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        add(folderPathTextField, new GridBagConstraints(0, row,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(browseButton, new GridBagConstraints(1, row++,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        add(accountNicknamesLabel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        add(accountNicknamesScrollPane, new GridBagConstraints(0, row++,
            2, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        add(passwordLabel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        add(passwordPasswordField, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
    }

    /**
     * Refreshes the table of stored credentials by finding <tt>*.enc</tt> files in the
     * directory specified as the Cockpit home folder.
     *
     */
    public void refreshStoredCredentialsTable() {
        nicknamesTableModel.removeAll();
        try {
            File[] files = cockpitHomeFolder.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                File candidateFile = files[i];
                if (candidateFile.getName().endsWith(".enc")) {
                    // Load partial details from credentials file.
                    ProviderCredentials credentials = ProviderCredentials.load(null, candidateFile);
                    nicknamesTableModel.addCredentialsFile(
                        credentials, candidateFile);
                }
            }
        } catch (Exception e) {
            String message = "Unable to find credential files in the folder "
                + cockpitHomeFolder.getAbsolutePath();
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, hyperlinkListener, message, e);
        }
    }

    private void chooseFolder() {
        // Prompt user to choose their Cockpit home directory.
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(true);
        fileChooser.setDialogTitle("Choose Cockpit Home Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("Choose Folder");
        fileChooser.setCurrentDirectory(cockpitHomeFolder);

        int returnVal = fileChooser.showOpenDialog(ownerFrame);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
                return;
        } else {
            this.cockpitHomeFolder = fileChooser.getSelectedFile();
            this.folderPathTextField.setText(this.cockpitHomeFolder.getAbsolutePath());
            refreshStoredCredentialsTable();
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        chooseFolder();
    }

    /**
     * @return
     * the folder chosen by the user as their Cockpit home.
     */
    public File getHomeFolder() {
        return this.cockpitHomeFolder;
    }

    /**
     * @return
     * the credentials encrypted file chosen by the user.
     */
    public File getCredentialsFile() {
        int selectedNicknameIndex = accountNicknameTable.getSelectedRow();
        if (selectedNicknameIndex < 0) {
            return null;
        }
        return nicknamesTableModel.getCredentialsFile(selectedNicknameIndex);
    }

    /**
     * @return
     * the password the user provided to unlock their encrypted credentials file.
     */
    public String getPassword() {
        return new String(passwordPasswordField.getPassword());
    }

    /**
     * Clears the user-provided password field.
     */
    public void clearPassword() {
        passwordPasswordField.setText("");
    }

    private class ProviderCredentialsFileTableModel extends DefaultTableModel {
        private static final long serialVersionUID = 8560515388653630790L;

        ArrayList credentialsList = new ArrayList();
        ArrayList credentialFileList = new ArrayList();

        public ProviderCredentialsFileTableModel() {
            super(new String[] {""}, 0);
        }

        public int addCredentialsFile(ProviderCredentials credentials, File credentialsFile) {
            int insertRow =
                Collections.binarySearch(credentialsList, credentials, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String name1 = ((ProviderCredentials)o1).getFriendlyName();
                        String name2 = ((ProviderCredentials)o2).getFriendlyName();
                        int result =  name1.compareToIgnoreCase(name2);
                        return result;
                    }
                });
            if (insertRow >= 0) {
                // We already have an item with this key, replace it.
                credentialsList.remove(insertRow);
                credentialFileList.remove(insertRow);
                this.removeRow(insertRow);
            } else {
                insertRow = (-insertRow) - 1;
            }
            // New object to insert.
            credentialsList.add(insertRow, credentials);
            credentialFileList.add(insertRow, credentialsFile);
            this.insertRow(insertRow, new Object[] {credentials.getFriendlyName()});
            return insertRow;
        }

        public void removeAll() {
            int rowCount = this.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                this.removeRow(0);
            }
            credentialFileList.clear();
            credentialsList.clear();
        }

        public File getCredentialsFile(int row) {
            return (File) credentialFileList.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

}
