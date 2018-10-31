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
package org.jets3t.apps.cockpit.gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.utils.ServiceUtils;

/**
 * Dialog box to prompt for the name and location of an S3 bucket. This dialog
 * should be created and displayed with {@link #setVisible(boolean)}, and once
 * control returns the user's responses are available via {@link #getOkClicked()},
 * {@link #getBucketName()} and {@link #getBucketLocation()}.
 * <p>
 * The caller is responsible for disposing of this dialog.
 *
 * @author James Murty
 *
 */
public class CreateBucketDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -8085778146542157010L;

    private static String[] locationNames = new String[] {};
    private static Map locationValueMap = new HashMap();

    private boolean okClicked = false;

    private JTextField bucketNameTextField = null;
    private JLabel bucketNameIsValidDNSResultLabel = null;
    private JComboBox bucketLocationComboBox = null;
    private JButton okButton = null;
    private JButton cancelButton = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);


    static int locOffset = 0;
    static {
        locationNames = new String[] {
            "US Standard",
            "US West (N. California)",
            "US West (Oregon)",
            "EU West (Ireland)",
            "EU Central (Frankfurt)",
            "Asia Pacific (Singapore)",
            "Asia Pacific (Tokyo)",
            "Asia Pacific (Sydney)",
            "South America (Sao Paulo)",
            "GovCloud US West",
            "GovCloud US West (FIPS 140-2)"
        };
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_US);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_US_WEST_NORTHERN_CALIFORNIA);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_US_WEST_OREGON);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_EU_IRELAND);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_EU_FRANKFURT);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_ASIA_PACIFIC_SINGAPORE);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_ASIA_PACIFIC_TOKYO);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_ASIA_PACIFIC_SYDNEY);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_SOUTH_AMERICA_SAO_PAULO);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_GOVCLOUD_US_WEST);
        locationValueMap.put(locationNames[locOffset++], S3Bucket.LOCATION_GOVCLOUD_FIPS_US_WEST);
    }


    public CreateBucketDialog(String suggestedBucketName, Frame ownerFrame,
        HyperlinkActivatedListener hyperlinkListener)
    {
        super(ownerFrame, "Create a new bucket", true);

        boolean disableDnsBuckets =
            Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                .getBoolProperty("s3service.disable-dns-buckets", false);

        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        okButton = new JButton("Create Bucket");
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);

        JHtmlLabel bucketNameLabel = new JHtmlLabel("<html><b>Bucket name</b></html>", hyperlinkListener);
        bucketNameLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel bucketLocationLabel = new JHtmlLabel("<html><b>Bucket location</b></html>", hyperlinkListener);
        bucketLocationLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel bucketNameIsValidDNSLabel = new JHtmlLabel("<html>DNS compatible?</html>", hyperlinkListener);
        bucketLocationLabel.setHorizontalAlignment(JLabel.CENTER);
        bucketNameIsValidDNSResultLabel = new JLabel("No");

        bucketLocationComboBox = new JComboBox(locationNames);
        bucketLocationComboBox.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               okButton.setEnabled(
                   bucketLocationComboBox.getSelectedIndex() == 0
                   || "Yes".equals(bucketNameIsValidDNSResultLabel.getText()));
           }
        });
        bucketLocationComboBox.setToolTipText("The geographical location where the bucket's contents will be stored");

        JHtmlLabel bucketNameIsValidDNSExplanationLabel = null;
        if (disableDnsBuckets) {
            bucketNameIsValidDNSExplanationLabel = new JHtmlLabel(
                "<html><font size=\"-2\">Because the 's3service.disable-dns-buckets' property is set, you<br>" +
                "may only create buckets in the U.S. location.</font></html>", hyperlinkListener);
            bucketLocationComboBox.setEnabled(false);
        } else {
            bucketNameIsValidDNSExplanationLabel = new JHtmlLabel(
                "<html><font size=\"-2\">If your bucket name is DNS-compatible, you can choose a storage location<br>" +
                "other than US Standard and may potentially use the bucket as a virtual host.</font></html>", hyperlinkListener);
            bucketLocationComboBox.setEnabled(true);
        }
        bucketNameIsValidDNSExplanationLabel.setHorizontalAlignment(JLabel.CENTER);

        bucketNameTextField = new JTextField();
        bucketNameTextField.setToolTipText("A unique bucket name in S3");
        bucketNameTextField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                checkBucketName(e.getDocument());
            }
            public void removeUpdate(DocumentEvent e) {
                checkBucketName(e.getDocument());
            }
            public void changedUpdate(DocumentEvent e) {
                checkBucketName(e.getDocument());
            }
            private void checkBucketName(Document doc) {
                String bucketName = "";
                try {
                    bucketName = doc.getText(0, doc.getLength());
                } catch (BadLocationException e) {
                }
                if (ServiceUtils.isBucketNameValidDNSName(bucketName)) {
                    bucketNameIsValidDNSResultLabel.setText("Yes");
                    okButton.setEnabled(true);
                } else {
                    bucketNameIsValidDNSResultLabel.setText("No");
                    // OK button should not be enabled for non-US locations
                    okButton.setEnabled(bucketLocationComboBox.getSelectedIndex() == 0);
                }
            }
        });
        bucketNameTextField.setText(suggestedBucketName);
        bucketNameTextField.setSelectionStart(0);
        bucketNameTextField.setSelectionEnd(suggestedBucketName.length());

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -6225706489569112809L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
                okClicked = false;
            }
        });

        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.add(cancelButton, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));
        buttonsPanel.add(okButton, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        panel.add(bucketNameLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(bucketNameTextField, new GridBagConstraints(1, row,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(bucketNameIsValidDNSExplanationLabel, new GridBagConstraints(0, ++row,
            2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        if (!disableDnsBuckets) {
            panel.add(bucketNameIsValidDNSLabel, new GridBagConstraints(0, ++row,
                1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
            panel.add(bucketNameIsValidDNSResultLabel, new GridBagConstraints(1, row,
                2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        }
        panel.add(bucketLocationLabel, new GridBagConstraints(0, ++row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(bucketLocationComboBox, new GridBagConstraints(1, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(new JHtmlLabel("<html><font size=\"-2\">Choosing a location other than US Standard may incur additional S3 usage fees.</font></html>", hyperlinkListener),
            new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(buttonsPanel, new GridBagConstraints(0, ++row,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(panel, new GridBagConstraints(0, 0,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        this.pack();
        this.setResizable(false);
        this.setLocationRelativeTo(ownerFrame);
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(okButton)) {
            this.setVisible(false);
            okClicked = true;
        } else if (event.getSource().equals(cancelButton)) {
            this.setVisible(false);
            okClicked = false;
        }
    }

    public boolean getOkClicked() {
        return okClicked;
    }

    public String getBucketLocation() {
        Object locationSelected = bucketLocationComboBox.getSelectedItem();
        return (String) locationValueMap.get(locationSelected);
    }

    public String getBucketName() {
        return bucketNameTextField.getText();
    }

}
