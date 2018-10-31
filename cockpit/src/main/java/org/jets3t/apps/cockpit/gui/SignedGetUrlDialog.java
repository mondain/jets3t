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
import java.util.Calendar;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.service.Constants;
import org.jets3t.service.S3Service;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.model.S3Object;

/**
 * Dialog box to query to generate Signed URLs for a given set of objects, based
 * on URL signing configuration options selected by the user. This dialog does
 * all the work, prompting for user inputs then generating and displaying the
 * resultant Signed URLs.
 * <p>
 * The caller is responsible for disposing of this dialog.
 *
 * @author James Murty
 *
 */
public class SignedGetUrlDialog extends JDialog implements ActionListener, DocumentListener {
    private static final long serialVersionUID = -3243824805519630114L;

    private static final Log log = LogFactory.getLog(SignedGetUrlDialog.class);

    private Frame ownerFrame = null;
    private HyperlinkActivatedListener hyperlinkListener = null;
    private S3Service s3Service = null;
    private S3Object[] objects = null;

    private JCheckBox virtualHostCheckBox = null;
    private JCheckBox requesterPaysCheckBox = null;
    private JCheckBox httpsUrlsCheckBox = null;
    private JTextField expiryTimeTextField = null;
    private JTextArea signedUrlsTextArea = null;
    private JButton finishedButton = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);


    public SignedGetUrlDialog(Frame ownerFrame,
        HyperlinkActivatedListener hyperlinkListener,
        S3Service s3Service, S3Object[] objects)
    {
        super(ownerFrame, "Generate Signed GET URLs", true);
        this.ownerFrame = ownerFrame;
        this.hyperlinkListener = hyperlinkListener;
        this.s3Service = s3Service;
        this.objects = objects;

        String introductionText = "<html><center>Generate signed GET URLs that you can provide to anyone<br>"
            + "who needs to access objects in your bucket for a limited time.</center></html>";
        JHtmlLabel introductionLabel = new JHtmlLabel(introductionText, hyperlinkListener);
        introductionLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel expiryTimeLabel = new JHtmlLabel("<html><b>Expiry Time</b> (Hours)</html>", hyperlinkListener);
        expiryTimeLabel.setHorizontalAlignment(JLabel.RIGHT);
        JHtmlLabel httpsUrlsLabel = new JHtmlLabel("<html><b>Secure HTTPS URLs?</b></html>", hyperlinkListener);
        httpsUrlsLabel.setHorizontalAlignment(JLabel.RIGHT);
        JHtmlLabel virtualHostLabel = new JHtmlLabel("<html><b>Bucket is a Virtual Host?</b></html>", hyperlinkListener);
        virtualHostLabel.setHorizontalAlignment(JLabel.RIGHT);
        JHtmlLabel requesterPaysLabel = new JHtmlLabel("<html><b>Bucket is Requester Pays?</b></html>", hyperlinkListener);
        requesterPaysLabel.setHorizontalAlignment(JLabel.RIGHT);

        expiryTimeTextField = new JTextField("1.0");
        expiryTimeTextField.setToolTipText("How long in hours until the URL will expire");
        expiryTimeTextField.getDocument().addDocumentListener(this);

        httpsUrlsCheckBox = new JCheckBox();
        httpsUrlsCheckBox.setSelected(false);
        httpsUrlsCheckBox.setToolTipText("Check this box to generate secure HTTPS URLs.");
        httpsUrlsCheckBox.addActionListener(this);

        virtualHostCheckBox = new JCheckBox();
        virtualHostCheckBox.setSelected(false);
        virtualHostCheckBox.setToolTipText("Check this box if your bucket is configured as a virtual host.");
        virtualHostCheckBox.addActionListener(this);

        requesterPaysCheckBox = new JCheckBox();
        requesterPaysCheckBox.setSelected(false);
        requesterPaysCheckBox.setToolTipText("Check this box if the bucket has Requester Pays enabled.");
        requesterPaysCheckBox.addActionListener(this);

        finishedButton = new JButton("Finished");
        finishedButton.setActionCommand("Finished");
        finishedButton.addActionListener(this);

        signedUrlsTextArea = new JTextArea();
        signedUrlsTextArea.setEditable(false);

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

        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        panel.add(introductionLabel, new GridBagConstraints(0, row,
            6, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(expiryTimeLabel, new GridBagConstraints(0, ++row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(expiryTimeTextField, new GridBagConstraints(1, row,
            5, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(httpsUrlsLabel, new GridBagConstraints(0, ++row,
            1, 1, 0.3, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(httpsUrlsCheckBox, new GridBagConstraints(1, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(virtualHostLabel, new GridBagConstraints(2, row,
            1, 1, 0.3, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(virtualHostCheckBox, new GridBagConstraints(3, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(requesterPaysLabel, new GridBagConstraints(4, row,
            1, 1, 0.3, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        panel.add(requesterPaysCheckBox, new GridBagConstraints(5, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(new JScrollPane(signedUrlsTextArea), new GridBagConstraints(0, ++row,
            6, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        panel.add(finishedButton, new GridBagConstraints(0, ++row,
            6, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().add(panel, new GridBagConstraints(0, 0,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        this.setSize(700, 450);
        this.setResizable(true);
        this.setLocationRelativeTo(ownerFrame);

        generateSignedUrls();
    }

    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(finishedButton)) {
            this.setVisible(false);
        } else {
            generateSignedUrls();
        }
    }

    public void changedUpdate(DocumentEvent e) {
        generateSignedUrls();
    }

    public void insertUpdate(DocumentEvent e) {
        generateSignedUrls();
    }

    public void removeUpdate(DocumentEvent e) {
        generateSignedUrls();
    }

    protected void generateSignedUrls() {
        try {
            signedUrlsTextArea.setText("");

            if (expiryTimeTextField.getText().length() == 0) {
                return;
            }

            // Determine expiry time for URL
            double hoursFromNow = Double.parseDouble(expiryTimeTextField.getText());
            int secondsFromNow = (int) (hoursFromNow * 60 * 60);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, secondsFromNow);
            long secondsSinceEpoch = cal.getTimeInMillis() / 1000;

            // Include Requester Pays flag if our service has these buckets enabled.
            String specialParamName = null;
            if (requesterPaysCheckBox.isSelected()) {
                specialParamName = Constants.REQUESTER_PAYS_BUCKET_FLAG;
            }

            boolean disableDnsBuckets = s3Service.getJetS3tProperties()
                .getBoolProperty("s3service.disable-dns-buckets", false);

            // Generate URLs
            StringBuffer signedUrlsBuffer = new StringBuffer();
            for (int i = 0; i < objects.length; i++) {
                S3Object currentObject = objects[i];

                String signedUrl = this.s3Service.createSignedUrl("GET",
                    currentObject.getBucketName(), currentObject.getKey(), specialParamName,
                    null, secondsSinceEpoch, virtualHostCheckBox.isSelected(),
                    httpsUrlsCheckBox.isSelected(), disableDnsBuckets);

                signedUrlsBuffer.append(signedUrl + "\n");
            }

            signedUrlsTextArea.setText(signedUrlsBuffer.toString());

        } catch (NumberFormatException e) {
            String message = "Hours must be a valid decimal value; eg 3, 0.1";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, hyperlinkListener, message, e);
        }
    }

}
