/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008 James Murty
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.service.model.S3Bucket;

/**
 * Dialog box show the Request Payment Configuration setting for a bucket,
 * and to allow this setting to be changed.
 *
 * @author James Murty
 */
public class RequesterPaysDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = 2406896456675486928L;

    private boolean okClicked = false;

    private JTextField bucketNameTextField = null;
    private JCheckBox requesterPaysCheckBox = null;
    private JButton okButton = null;
    private JButton cancelButton = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);


    public RequesterPaysDialog(S3Bucket bucket, Frame ownerFrame,
        HyperlinkActivatedListener hyperlinkListener)
    {
        super(ownerFrame, "Requester Pays", true);

        cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        okButton = new JButton("Update Status");
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);

        JHtmlLabel bucketNameLabel = new JHtmlLabel("<html><b>Bucket name</b></html>", hyperlinkListener);
        bucketNameLabel.setHorizontalAlignment(JLabel.CENTER);

        bucketNameTextField = new JTextField(bucket.getName());
        bucketNameTextField.setEditable(false);

        JHtmlLabel requesterPaysLabel = new JHtmlLabel("<html><b>Requester Pays?</b></html>", hyperlinkListener);
        requesterPaysCheckBox = new JCheckBox();
        requesterPaysCheckBox.setSelected(bucket.isRequesterPays());

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
        panel.add(requesterPaysLabel, new GridBagConstraints(0, ++row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        panel.add(requesterPaysCheckBox, new GridBagConstraints(1, row,
            1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
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

    public boolean isRequesterPaysSelected() {
        return requesterPaysCheckBox.isSelected();
    }

}
