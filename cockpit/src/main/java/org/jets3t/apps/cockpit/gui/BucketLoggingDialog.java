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
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.gui.ProgressDialog;
import org.jets3t.service.S3Service;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3BucketLoggingStatus;

/**
 * Dialog box for displaying and modifying the logging status of buckets.
 * <p>
 * The first time a bucket is selected its logging status is retrieved from S3 and the details are
 * displayed, as well as being cached so further lookups aren't necessary. The logging status is
 * modified by choosing/changing the target log bucket.
 *
 * @author James Murty
 *
 */
public class BucketLoggingDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 7251165117085406917L;

    private Frame ownerFrame = null;
    private S3Service s3Service = null;
    private HashMap loggingStatusMap = new HashMap();

    private JComboBox loggedBucketComboBox = null;
    private JComboBox loggedToBucketComboBox = null;
    private JTextField prefixTextField = null;
    private JButton finishedButton = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);


    public BucketLoggingDialog(Frame ownerFrame, S3Service s3Service, String[] bucketNames,
        HyperlinkActivatedListener hyperlinkListener)
    {
        super(ownerFrame, "Bucket Logging Status", true);
        this.ownerFrame = ownerFrame;
        this.s3Service = s3Service;

        String introductionText = "<html><center>View and modify your bucket logging settings<br>"
            + "Select a bucket in the <b>Log to</b> list to apply changes<br><b>Note</b>: The target "
            + "bucket's ACL permissions are updated if necessary to allow logging<p>&nbsp;</center></html>";
        JHtmlLabel introductionLabel = new JHtmlLabel(introductionText, hyperlinkListener);
        introductionLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel loggingStatusLabel = new JHtmlLabel("<html><b>Logging status</b></html>", hyperlinkListener);
        loggingStatusLabel.setHorizontalAlignment(JLabel.CENTER);

        JHtmlLabel loggedBucketLabel = new JHtmlLabel("Bucket:", hyperlinkListener);
        loggedBucketComboBox = new JComboBox(bucketNames);
        loggedBucketComboBox.insertItemAt("-- Choose a bucket --", 0);
        loggedBucketComboBox.addActionListener(this);
        JHtmlLabel prefixLabel = new JHtmlLabel("Log file prefix:", hyperlinkListener);
        prefixTextField = new JTextField();
        prefixTextField.setToolTipText("Log files for the bucket start with this prefix. The prefix cannot be empty");
        JHtmlLabel loggedToBucketLabel = new JHtmlLabel("Log to:", hyperlinkListener);
        loggedToBucketComboBox = new JComboBox(bucketNames);
        loggedToBucketComboBox.setToolTipText("Where the bucket's log files will be stored");
        loggedToBucketComboBox.insertItemAt("-- Not Logged --", 0);
        loggedToBucketComboBox.addActionListener(this);

        finishedButton = new JButton("Finished");
        finishedButton.setActionCommand("Finished");
        finishedButton.addActionListener(this);

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(finishedButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -6225706489569112809L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        loggedBucketComboBox.setSelectedIndex(0);
        loggedToBucketComboBox.setSelectedIndex(0);

        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        panel.add(introductionLabel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(loggedBucketLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(loggedBucketComboBox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(loggingStatusLabel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(prefixLabel, new GridBagConstraints(0, row,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(prefixTextField, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(loggedToBucketLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(loggedToBucketComboBox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(finishedButton, new GridBagConstraints(0, row,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(panel, new GridBagConstraints(0, 0,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(ownerFrame);
    }

    private int findBucketIndexByName(String bucketName) {
        for (int i = 0; i < loggedToBucketComboBox.getItemCount(); i++) {
            String testBucketName = (String) loggedToBucketComboBox.getItemAt(i);
            if (testBucketName.equals(bucketName)) {
                return i;
            }
        }
        return 0;
    }

    private void displayBucketLoggingStatus(S3BucketLoggingStatus loggingStatus) {
        if (loggingStatus.isLoggingEnabled()) {
            loggedToBucketComboBox.setSelectedIndex(
                findBucketIndexByName(loggingStatus.getTargetBucketName()));
            prefixTextField.setText(loggingStatus.getLogfilePrefix());
        } else {
            loggedToBucketComboBox.setSelectedIndex(0);
            if (loggedBucketComboBox.getSelectedIndex() != 0) {
                prefixTextField.setText(loggedBucketComboBox.getSelectedItem() + ".");
            } else {
                prefixTextField.setText("");
            }
        }
        prefixTextField.setEnabled(true);
        loggedToBucketComboBox.setEnabled(true);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(finishedButton)) {
            // Hack to update the logging status of a bucket before exiting the dialog
            // if the prefix string has changed
            String bucketName = (String) loggedBucketComboBox.getSelectedItem();
            if (loggingStatusMap.containsKey(bucketName)) {
                S3BucketLoggingStatus loggingStatus =
                    (S3BucketLoggingStatus) loggingStatusMap.get(bucketName);
                if (!prefixTextField.getText().equals(loggingStatus.getLogfilePrefix())
                    && loggedToBucketComboBox.getSelectedIndex() != 0)
                {
                    loggedToBucketComboBox.setSelectedIndex(loggedToBucketComboBox.getSelectedIndex());
                }
            }

            this.setVisible(false);
        } else if (event.getSource().equals(loggedBucketComboBox)) {
            prefixTextField.setEnabled(false);
            loggedToBucketComboBox.setEnabled(false);

            if (loggedBucketComboBox.getSelectedIndex() == 0) {
                prefixTextField.setText("");
                loggedToBucketComboBox.setSelectedIndex(0);
            } else {

                final String bucketName = (String) loggedBucketComboBox.getSelectedItem();

                if (loggingStatusMap.containsKey(bucketName)) {
                    S3BucketLoggingStatus loggingStatus =
                        (S3BucketLoggingStatus) loggingStatusMap.get(bucketName);
                    displayBucketLoggingStatus(loggingStatus);
                } else {
                    (new Thread() {
                        public void run() {
                            final ProgressDialog progressDialog =
                                new ProgressDialog(ownerFrame, "Bucket Logging", null);

                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressDialog.startDialog("Retrieving bucket logging status",
                                        null, 0, 0, null, null);
                                }
                             });

                            try {
                                S3BucketLoggingStatus loggingStatus =
                                    s3Service.getBucketLoggingStatus(bucketName);
                                loggingStatusMap.put(bucketName, loggingStatus);
                                displayBucketLoggingStatus(loggingStatus);
                            } catch (Exception e) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    public void run() {
                                        progressDialog.stopDialog();
                                    }
                                 });

                                ErrorDialog.showDialog(ownerFrame, null,
                                    "Unable to retrieve bucket logging status for " + bucketName, e);
                            }
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressDialog.stopDialog();
                                }
                             });
                        }
                    }).start();
                }
            }
        } else if (event.getSource().equals(loggedToBucketComboBox)) {
            if (!loggedToBucketComboBox.isEnabled()) {
                // Ignore this event, it is internally generated.
                return;
            }

            final String loggedBucketName = (String) loggedBucketComboBox.getSelectedItem();
            final String[] loggedToBucketName = new String[1];
            final String[] loggingFilePrefix = new String[1];

            if (loggedToBucketComboBox.getSelectedIndex() == 0) {
                // Logging is being disabled, leave values as null.
            } else {
                if (prefixTextField.getText().length() == 0) {
                    ErrorDialog.showDialog(ownerFrame, null,
                        "A log file name prefix must be provided to log buckets", null);
                    loggedToBucketComboBox.setSelectedIndex(0);
                    return;
                }

                loggedToBucketName[0] = (String) loggedToBucketComboBox.getSelectedItem();
                loggingFilePrefix[0] = prefixTextField.getText();
            }

            (new Thread(new Runnable() {
                public void run() {
                    final ProgressDialog progressDialog =
                        new ProgressDialog(ownerFrame, "Bucket Logging", null);
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.startDialog("Setting bucket logging status",
                                null, 0, 0, null, null);
                        }
                     });

                    try {
                        S3BucketLoggingStatus loggingStatus =
                            new S3BucketLoggingStatus(loggedToBucketName[0], loggingFilePrefix[0]);
                        s3Service.setBucketLoggingStatus(loggedBucketName, loggingStatus, true);

                        loggingStatusMap.put(loggedBucketName, loggingStatus);
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressDialog.stopDialog();
                            }
                         });
                        ErrorDialog.showDialog(ownerFrame, null,
                            "Unable to set bucket logging status for " + loggedBucketName, e);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });
                };
            })).start();
        }
    }

    /**
     * Dialog box for displaying and modifying the logging status of buckets.
     *
     * @param ownerFrame
     * the frame that will own the dialog.
     * @param s3Service
     * an S3 Service that will be used to query and update the logging status of buckets. This
     * service must be initialised with the necessary AWS credentials to perform the logging status
     * change operations.
     * @param buckets
     * the buckets in the user's S3 account.
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     */
    public static void showDialog(Frame ownerFrame, S3Service s3Service, S3Bucket[] buckets, HyperlinkActivatedListener hyperlinkListener) {
        String[] bucketNames = new String[buckets.length];
        for (int i = 0; i < buckets.length; i++) {
            bucketNames[i] = buckets[i].getName();
        }
        showDialog(ownerFrame, s3Service, bucketNames, hyperlinkListener);
    }

    /**
     * Dialog box for displaying and modifying the logging status of buckets.
     *
     * @param ownerFrame
     * the frame that will own the dialog.
     * @param s3Service
     * an S3 Service that will be used to query and update the logging status of buckets. This
     * service must be initialised with the necessary AWS credentials to perform the logging status
     * change operations.
     * @param bucketNames
     * the names of buckets in the user's S3 account.
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     */
    public static void showDialog(Frame ownerFrame, S3Service s3Service, String[] bucketNames, HyperlinkActivatedListener hyperlinkListener) {
        BucketLoggingDialog dialog = new BucketLoggingDialog(
            ownerFrame, s3Service, bucketNames, hyperlinkListener);
        dialog.setVisible(true);
        dialog.dispose();
    }

}
