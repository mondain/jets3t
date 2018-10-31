/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008-2010 James Murty
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

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;

import org.jets3t.service.CloudFrontService;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.model.cloudfront.Distribution;
import org.jets3t.service.model.cloudfront.DistributionConfig;
import org.jets3t.service.model.cloudfront.LoggingStatus;
import org.jets3t.service.model.cloudfront.S3Origin;

/**
 * Dialog box for displaying and modifying CloudFront distributions.
 * <p>
 * The first time a bucket is selected its logging status is retrieved from S3 and the details are
 * displayed, as well as being cached so further lookups aren't necessary. The logging status is
 * modified by choosing/changing the target log bucket.
 *
 * @author James Murty
 */
public class ManageDistributionsDialog extends JDialog
    implements ActionListener, ListSelectionListener, HyperlinkActivatedListener
{
    private static final long serialVersionUID = 4375982994004017092L;

    private CloudFrontService cloudFrontService = null;

    private final GuiUtils guiUtils = new GuiUtils();

    private Frame ownerFrame = null;
    private JTable distributionListTable = null;
    private DistributionListTableModel distributionListTableModel = null;
    private TableSorter distributionListTableModelSorter = null;
    private JComboBox bucketComboBox = null;
    private JCheckBox enabledCheckbox = null;
    private JCheckBox httpsOnlyCheckbox = null;
    private JComboBox loggingBucketComboBox = null;
    private JTextField defaultRootObjectTextField = null;
    private JTextField loggingPrefixTextField = null;
    private JTable cnamesTable = null;
    private CNAMETableModel cnamesTableModel = null;
    private JButton addCname = null;
    private JButton removeCname = null;
    private JTextArea commentTextArea = null;
    private JTextField minTTLTextField = null;
    private JButton actionButton = null;
    private JButton deleteButton = null;
    private JButton finishedButton = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);
    private final Insets insetsHorizontalOnly = new Insets(0, 5, 0, 5);


    public ManageDistributionsDialog(Frame ownerFrame, CloudFrontService cloudFrontService,
        String bucketNames[], HyperlinkActivatedListener hyperlinkListener)
    {
        super(ownerFrame, "CloudFront Distributions", true);
        this.ownerFrame = ownerFrame;
        this.cloudFrontService = cloudFrontService;

        distributionListTableModel = new DistributionListTableModel();
        distributionListTableModelSorter = new TableSorter(distributionListTableModel);
        distributionListTable = new JTable(distributionListTableModelSorter);
        distributionListTableModelSorter.setTableHeader(distributionListTable.getTableHeader());
        distributionListTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        distributionListTable.getSelectionModel().addListSelectionListener(this);

        JLabel bucketLabel = new JLabel("Bucket");
        bucketComboBox = new JComboBox(bucketNames);
        JLabel enabledLabel = new JLabel("Enabled");
        enabledCheckbox = new JCheckBox();
        JLabel httpsOnlyLabel = new JLabel("HTTPS Only");
        httpsOnlyCheckbox = new JCheckBox();
        JLabel defaultRootObjectLabel = new JLabel("Default Root Object");
        defaultRootObjectTextField = new JTextField();
        JLabel minTTLLabel = new JLabel("Minimum TTL");
        minTTLTextField = new JTextField("0");

        JLabel loggingBucketLabel = new JLabel("Logging bucket");
        loggingBucketComboBox = new JComboBox(bucketNames);
        loggingBucketComboBox.insertItemAt("-- Logging Disabled --", 0);
        loggingBucketComboBox.setSelectedIndex(0);
        loggingBucketComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loggingPrefixTextField.setEnabled(
                    loggingBucketComboBox.getSelectedIndex() > 0);
            }
        });
        JLabel loggingPrefixLabel = new JLabel("Logging prefix");
        loggingPrefixTextField = new JTextField();
        loggingPrefixTextField.setEnabled(false);

        cnamesTableModel = new CNAMETableModel();
        TableSorter cnamesTableModelSorter = new TableSorter(cnamesTableModel);
        cnamesTable = new JTable(cnamesTableModelSorter);
        cnamesTableModelSorter.setTableHeader(cnamesTable.getTableHeader());
        cnamesTable.getSelectionModel().addListSelectionListener(this);

        removeCname = new JButton();
        removeCname.setToolTipText("Remove the selected CNAME");
        guiUtils.applyIcon(removeCname, "/images/nuvola/16x16/actions/viewmag-.png");
        removeCname.addActionListener(this);
        removeCname.setActionCommand("RemoveCname");
        removeCname.setEnabled(false);
        addCname = new JButton();
        addCname.setToolTipText("Add a new CNAME");
        guiUtils.applyIcon(addCname, "/images/nuvola/16x16/actions/viewmag+.png");
        addCname.setActionCommand("AddCname");
        addCname.addActionListener(this);

        JLabel commentLabel = new JLabel("Comment");
        commentTextArea = new JTextArea();
        commentTextArea.setLineWrap(true);

        JButton refreshButton = new JButton("Refresh Distributions");
        refreshButton.setActionCommand("RefreshDistributions");
        refreshButton.addActionListener(this);
        guiUtils.applyIcon(refreshButton, "/images/nuvola/16x16/actions/reload.png");

        deleteButton = new JButton("Delete Distribution");
        deleteButton.setActionCommand("DeleteDistribution");
        deleteButton.addActionListener(this);
        deleteButton.setEnabled(false);
        deleteButton.setToolTipText("To delete a distribution it must first be disabled and deployed");
        guiUtils.applyIcon(deleteButton, "/images/nuvola/16x16/actions/cancel.png");

        actionButton = new JButton("New Distribution");
        actionButton.setActionCommand("NewDistribution");
        actionButton.addActionListener(this);

        finishedButton = new JButton("Finished");
        finishedButton.setActionCommand("Finished");
        finishedButton.addActionListener(this);

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(finishedButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -7782288899638043533L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        JPanel distributionsButtonsPanel = new JPanel(new GridBagLayout());
        distributionsButtonsPanel.add(refreshButton, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));
        distributionsButtonsPanel.add(deleteButton, new GridBagConstraints(1, 0,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));

        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.add(new JScrollPane(distributionListTable), new GridBagConstraints(0, 0,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(3, 5, 0, 3), 0, 0));
        tablePanel.add(distributionsButtonsPanel, new GridBagConstraints(0, 1,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));

        JPanel cnameAddRemoveButtonsPanel = new JPanel(new GridBagLayout());
        cnameAddRemoveButtonsPanel.add(removeCname, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));
        cnameAddRemoveButtonsPanel.add(addCname, new GridBagConstraints(1, 0,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));

        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Distribution Details"));
        int row = 0;
        detailPanel.add(bucketLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(bucketComboBox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        detailPanel.add(enabledLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(enabledCheckbox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        detailPanel.add(httpsOnlyLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(httpsOnlyCheckbox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        detailPanel.add(defaultRootObjectLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(defaultRootObjectTextField, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        detailPanel.add(minTTLLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(minTTLTextField, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        detailPanel.add(loggingBucketLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(loggingBucketComboBox, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        detailPanel.add(loggingPrefixLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        detailPanel.add(loggingPrefixTextField, new GridBagConstraints(1, row++,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        detailPanel.add(new JScrollPane(cnamesTable), new GridBagConstraints(0, row++,
            2, 1, 1, 2, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(3, 5, 0, 5), 0, 0));
        detailPanel.add(cnameAddRemoveButtonsPanel, new GridBagConstraints(0, row++,
            2, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));
        detailPanel.add(commentLabel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));
        detailPanel.add(new JScrollPane(commentTextArea), new GridBagConstraints(0, row++,
            2, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        detailPanel.add(actionButton, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));

        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(tablePanel, new GridBagConstraints(0, 0,
            1, 1, 1, 2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        this.getContentPane().add(detailPanel, new GridBagConstraints(0, 1,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsHorizontalOnly, 0, 0));
        this.getContentPane().add(finishedButton, new GridBagConstraints(0, 2,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalOnly, 0, 0));

        // Size dialog
        this.pack();
        this.setSize(690, 600);

        // Micro-managed column widths, because Java table columns are stupid... yuk.
        TableColumnModel columnModel = distributionListTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);  // Status
        columnModel.getColumn(1).setPreferredWidth(50);  // Enabled?
        columnModel.getColumn(2).setPreferredWidth(170); // Bucket
        columnModel.getColumn(3).setPreferredWidth(210); // Domain name
        columnModel.getColumn(4).setPreferredWidth(220); // Last Modified
        this.setLocationRelativeTo(ownerFrame);

        refreshDistributions();
    }

    protected void refreshDistributions() {
        (new Thread() {
            @Override
            public void run() {
                final ProgressDialog progressDialog =
                    new ProgressDialog(ownerFrame, "Listing Distributions", null);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressDialog.startDialog("Listing Distributions", null, 0, 0, null, null);
                    }
                 });

                try {
                    final Distribution[] distributions = cloudFrontService.listDistributions();
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            int priorSelection = distributionListTable.getSelectedRow();

                            distributionListTableModel.removeAll();
                            for (int i = 0; i < distributions.length; i++) {
                                distributionListTableModel.addDistribution(distributions[i]);
                            }

                            if (priorSelection >= 0 && priorSelection < distributionListTable.getRowCount()) {
                                distributionListTable.setRowSelectionInterval(priorSelection, priorSelection);
                            } else if (distributionListTable.getRowCount() > 0) {
                                distributionListTable.setRowSelectionInterval(0, 0);
                            }
                        }
                     });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });

                    String errorMessage = "Failed to list distributions";
                    if (e instanceof CloudFrontServiceException) {
                        errorMessage = ((CloudFrontServiceException)e).getErrorMessage();
                    }
                    ErrorDialog.showDialog(ownerFrame, null, errorMessage, e);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        progressDialog.stopDialog();
                    }
                 });
            }
        }).start();
    }

    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (e.getSource().equals(distributionListTable.getSelectionModel())) {
            int tableIndex = distributionListTable.getSelectedRow();
            if (tableIndex < 0) {
                return;
            }
            Distribution distribution = distributionListTableModel.getDistributionAtRow(
                distributionListTableModelSorter.modelIndex(tableIndex));

            if (distribution == null) {
                // Special case, allow the creation of a new distribution.
                deleteButton.setEnabled(false);

                bucketComboBox.setEnabled(true);
                enabledCheckbox.setSelected(false);
                httpsOnlyCheckbox.setEnabled(false);
                defaultRootObjectTextField.setText("");
                minTTLTextField.setText("");

                loggingBucketComboBox.setSelectedIndex(0);
                loggingPrefixTextField.setText("");

                cnamesTableModel.removeAll();
                commentTextArea.setText("");

                actionButton.setText("New Distribution");
                actionButton.setActionCommand("NewDistribution");
                actionButton.setEnabled(true);
            } else {
                // Retrieve distribution's configuration to access current logging status settings.
                DistributionConfig distributionConfig = null;
                try {
                    distributionConfig = cloudFrontService.getDistributionConfig(distribution.getId());
                } catch (CloudFrontServiceException ex) {
                    String message = "Unable to retrieve configuration information "
                        + "for distribution: " + distribution.getId();
                    ErrorDialog.showDialog(ownerFrame, this, message, ex);
                    return;
                }

                if (distribution.getOrigin() instanceof S3Origin) {
                    bucketComboBox.setSelectedItem(
                        ((S3Origin)distribution.getOrigin()).getOriginAsBucketName());
                }
                bucketComboBox.setEnabled(false);
                enabledCheckbox.setSelected(distribution.isEnabled());
                httpsOnlyCheckbox.setSelected(distributionConfig.isHttpsProtocolRequired());
                defaultRootObjectTextField.setText(distributionConfig.getDefaultRootObject());
                if (distributionConfig.isStreamingDistributionConfig()) {
                    minTTLTextField.setEnabled(false);
                    minTTLTextField.setText("N/A");
                } else {
                    minTTLTextField.setEnabled(true);
                    Long minTTL = distributionConfig.getMinTTL();
                    if (minTTL == null) {
                        minTTLTextField.setText("0");
                    } else {
                        minTTLTextField.setText(minTTL.toString());
                    }
                }

                if (distributionConfig.getLoggingStatus() != null) {
                    loggingBucketComboBox.setSelectedItem(
                        distributionConfig.getLoggingStatus().getShortBucketName());
                    loggingPrefixTextField.setText(
                        distributionConfig.getLoggingStatus().getPrefix());
                } else {
                    loggingBucketComboBox.setSelectedIndex(0);
                    loggingPrefixTextField.setText("");
                }

                cnamesTableModel.removeAll();
                String[] cnames = distribution.getCNAMEs();
                for (int i = 0; i < cnames.length; i++) {
                    cnamesTableModel.addCname(cnames[i]);
                }
                commentTextArea.setText(distribution.getComment());

                // Update possible actions
                deleteButton.setEnabled(!distribution.isEnabled() && distribution.isDeployed());

                actionButton.setText("Update Distribution");
                actionButton.setActionCommand("UpdateDistribution");
                actionButton.setEnabled(distribution.isDeployed());
            }
        } else if (e.getSource().equals(cnamesTable.getSelectionModel())) {
            removeCname.setEnabled(cnamesTable.getSelectedRowCount() != 0);
        }
    }


    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(finishedButton)) {
            this.setVisible(false);
        } else if (event.getActionCommand().equals("AddCname")) {
            cnamesTableModel.addCname("New CNAME");
            int newRowIndex = cnamesTable.getRowCount() - 1;
            cnamesTable.setRowSelectionInterval(newRowIndex, newRowIndex);
            cnamesTable.editCellAt(newRowIndex, 0);
        } else if (event.getActionCommand().equals("RemoveCname")) {
            cnamesTableModel.removeRow(cnamesTable.getSelectedRow());
        } else if (event.getActionCommand().equals("RefreshDistributions")) {
            refreshDistributions();
        } else if (event.getActionCommand().equals("DeleteDistribution")) {
            final Distribution distribution = distributionListTableModel.getDistributionAtRow(
                distributionListTableModelSorter.modelIndex(
                    distributionListTable.getSelectedRow()));

            int answer = JOptionPane.showConfirmDialog(this,
                "Delete the distribution " + distribution.getDomainName() + "?",
                "Delete Distribution", JOptionPane.YES_NO_OPTION);
            if (answer == 1) { // NO
                return;
            } else {
                (new Thread() {
                    @Override
                    public void run() {
                        final ProgressDialog progressDialog =
                            new ProgressDialog(ownerFrame, "Deleting Distribution", null);

                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressDialog.startDialog("Deleting distribution: " + distribution.getDomainName(),
                                    null, 0, 0, null, null);
                            }
                         });

                        try {
                            cloudFrontService.deleteDistribution(distribution.getId());
                            refreshDistributions();
                        } catch (Exception e) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressDialog.stopDialog();
                                }
                             });

                            String errorMessage = "Failed to delete distribution";
                            if (e instanceof CloudFrontServiceException) {
                                errorMessage = ((CloudFrontServiceException)e).getErrorMessage();
                            }
                            ErrorDialog.showDialog(ownerFrame, null, errorMessage, e);
                        }
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressDialog.stopDialog();
                            }
                         });
                    }
                }).start();
            }
        } else if (event.getActionCommand().equals("NewDistribution")) {
            final String bucketName = bucketComboBox.getSelectedItem() + ".s3.amazonaws.com";

            (new Thread() {
                @Override
                public void run() {
                    final ProgressDialog progressDialog =
                        new ProgressDialog(ownerFrame, "Creating Distribution", null);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.startDialog("Creating distribution for: " + bucketComboBox.getSelectedItem(),
                                null, 0, 0, null, null);
                        }
                     });

                    try {
                        LoggingStatus loggingStatus = null;
                        if (loggingBucketComboBox.getSelectedIndex() > 0) {
                            String loggingBucket = (String) loggingBucketComboBox.getSelectedItem()
                                + CloudFrontService.DEFAULT_BUCKET_SUFFIX;
                            loggingStatus = new LoggingStatus(
                                    loggingBucket,
                                    loggingPrefixTextField.getText());
                        }

                        String[] requiredProtocols = null;
                        if (httpsOnlyCheckbox.isSelected()) {
                            requiredProtocols = new String[] {"https"};
                        } else {
                            requiredProtocols = new String[0];
                        }

                        String defaultRootObject = defaultRootObjectTextField.getText();
                        if (defaultRootObject.length() == 0) {
                            defaultRootObject = null;
                        }

                        Long minTTL = null;
                        if (minTTLTextField.isEnabled()) {
                            try {
                                minTTL = new Long(minTTLTextField.getText());
                            } catch (NumberFormatException e) {}
                            if (minTTL == null || minTTL < 0) {
                                throw new Exception("Minimum TTL must be an integer value 0 or greater");
                            }
                        }

                        cloudFrontService.createDistribution(
                            new S3Origin(bucketName), null,
                            cnamesTableModel.getCnames(), commentTextArea.getText(),
                            enabledCheckbox.isSelected(), loggingStatus,
                            false, // trustedSignerSelf
                            null,  // trustedSignerAwsAccountNumbers
                            requiredProtocols,
                            defaultRootObject,
                            minTTL);
                        refreshDistributions();
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressDialog.stopDialog();
                            }
                         });

                        String errorMessage = "Failed to create distribution";
                        if (e instanceof CloudFrontServiceException) {
                            errorMessage = ((CloudFrontServiceException)e).getErrorMessage();
                        }
                        ErrorDialog.showDialog(ownerFrame, null, errorMessage, e);
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.stopDialog();
                        }
                     });
                }
            }).start();
        } else if (event.getActionCommand().equals("UpdateDistribution")) {
            final Distribution distribution = distributionListTableModel.getDistributionAtRow(
                distributionListTableModelSorter.modelIndex(
                    distributionListTable.getSelectedRow()));

            (new Thread() {
                @Override
                public void run() {
                    final ProgressDialog progressDialog =
                        new ProgressDialog(ownerFrame, "Updating Distribution", null);

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressDialog.startDialog("Updating distribution: " + distribution.getDomainName(),
                                null, 0, 0, null, null);
                        }
                     });

                    try {
                        LoggingStatus loggingStatus = null;
                        if (loggingBucketComboBox.getSelectedIndex() > 0) {
                            String loggingBucket = (String) loggingBucketComboBox.getSelectedItem()
                                + CloudFrontService.DEFAULT_BUCKET_SUFFIX;
                            loggingStatus = new LoggingStatus(
                                    loggingBucket,
                                    loggingPrefixTextField.getText());
                        }

                        String[] requiredProtocols = null;
                        if (httpsOnlyCheckbox.isSelected()) {
                            requiredProtocols = new String[] {"https"};
                        } else {
                            requiredProtocols = new String[0];
                        }

                        String defaultRootObject = defaultRootObjectTextField.getText();
                        if (defaultRootObject.length() == 0) {
                            defaultRootObject = null;
                        }

                        Long minTTL = null;
                        if (minTTLTextField.isEnabled()) {
                            try {
                                minTTL = new Long(minTTLTextField.getText());
                            } catch (NumberFormatException e) {}
                            if (minTTL == null || minTTL < 0) {
                                throw new Exception("Minimum TTL must be an integer value 0 or greater");
                            }
                        }

                        cloudFrontService.updateDistributionConfig(distribution.getId(),
                            null, // origin
                            cnamesTableModel.getCnames(), commentTextArea.getText(),
                            enabledCheckbox.isSelected(), loggingStatus,
                            false, // trustedSignerSelf
                            null,  // trustedSignerAwsAccountNumbers
                            requiredProtocols,
                            defaultRootObject,
                            minTTL);
                        refreshDistributions();
                    } catch (Exception e) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                progressDialog.stopDialog();
                            }
                         });

                        String errorMessage = "Failed to update distribution";
                        if (e instanceof CloudFrontServiceException) {
                            errorMessage = ((CloudFrontServiceException)e).getErrorMessage();
                        }
                        ErrorDialog.showDialog(ownerFrame, null, errorMessage, e);
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

    /**
     * Dialog box for displaying and modifying CloudFront distributions.
     *
     * @param ownerFrame
     * the frame that will own the dialog.
     * @param cloudFrontService
     * a CloudFrontService that will be used to query and update distributions. This
     * service must be initialised with the necessary AWS credentials to perform the
     * API operations.
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     */
    public static void showDialog(Frame ownerFrame, CloudFrontService cloudFrontService,
        String[] bucketNames, HyperlinkActivatedListener hyperlinkListener)
    {
        ManageDistributionsDialog dialog = new ManageDistributionsDialog(
            ownerFrame, cloudFrontService, bucketNames, hyperlinkListener);
        dialog.setVisible(true);
        dialog.dispose();
    }

    private class DistributionListTableModel extends DefaultTableModel {
        private static final long serialVersionUID = -8315527286580422385L;

        private final ArrayList distributionList = new ArrayList();

        private Icon inProgressIcon = null;
        private Icon deployedIcon = null;

        public DistributionListTableModel() {
            super(new String[] {"Status", "Enabled", "Bucket", "Domain Name", "Last Modified"}, 0);
            this.addRow(new Object[] {null, null, "Add New Distribution", null, null});

            JLabel dummyLabel = new JLabel();
            if (guiUtils.applyIcon(dummyLabel, "/images/nuvola/16x16/actions/noatunloopsong.png"))
            {
                inProgressIcon = dummyLabel.getIcon();
            }
            if (guiUtils.applyIcon(dummyLabel, "/images/nuvola/16x16/actions/ok.png"))
            {
                deployedIcon = dummyLabel.getIcon();
            }
        }

        protected int findDistributionIndex(String bucketName) {
            return Collections.binarySearch(
                distributionList, bucketName, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        String b1Name = ((Distribution)o1).getOrigin().getDnsName();
                        String b2Name = (String) o2;
                        int result =  b1Name.compareTo(b2Name);
                        return result;
                    }
                }
            );
        }

        public void addDistribution(Distribution distribution) {
            distributionList.add(distribution);
            this.insertRow(distributionList.size() - 1, new Object[] {
                (distribution.isDeployed() ? deployedIcon : inProgressIcon),
                Boolean.valueOf(distribution.isEnabled()),
                distribution.getOrigin().getDnsName(), distribution.getDomainName(),
                distribution.getLastModifiedTime()
            });
        }

        public Distribution getDistributionAtRow(int rowIndex) {
            if (rowIndex < distributionList.size()) {
                return (Distribution) distributionList.get(rowIndex);
            } else {
                return null;
            }
        }

        public void removeDistribution(String bucketName) {
            int index = findDistributionIndex(bucketName);
            this.removeRow(index);
            distributionList.remove(index);
        }

        public void removeAll() {
            int rowCount = distributionList.size();
            for (int i = 0; i < rowCount; i++) {
                this.removeRow(0);
            }
            distributionList.clear();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return (column == 3 && row < distributionList.size());
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0: return Icon.class;
            case 1: return Boolean.class;
            default:return String.class;
            }
        }
    }

    private class CNAMETableModel extends DefaultTableModel {
        private static final long serialVersionUID = 7975150589167172506L;

        public CNAMETableModel() {
            super(new String[] {"CNAME Aliases"}, 0);
        }

        public void addCname(String cname) {
            this.addRow(new Object[] {cname});
        }

        public String[] getCnames() {
            String[] cnames = new String[this.getRowCount()];
            for (int i = 0; i < this.getRowCount(); i++) {
                cnames[i] = (String) this.getValueAt(i, 0);
            }
            return cnames;
        }

        public void removeAll() {
            int rowCount = this.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                this.removeRow(0);
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    }

    public void followHyperlink(URL url, String target) {
    }

}
