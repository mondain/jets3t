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
package org.jets3t.gui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.multithread.CancelEventTrigger;

/**
 * A modal dialog box to display progress information to the user when a long-lived task
 * is running. If the long-lived task can be cancelled by the user, this dialog will invoke
 * the {@link CancelEventTrigger} when the user clicks the cancel button.
 *
 * @author James Murty
 */
public class ProgressDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -6031102393562980815L;

    private static final Log log = LogFactory.getLog(ProgressDialog.class);

    private final Insets insetsDefault = new Insets(5, 7, 5, 7);

    private Properties applicationProperties = null;
    private SkinsFactory skinsFactory = null;

    private JLabel statusMessageLabel = null;
    private JLabel detailsTextLabel = null;
    private JProgressBar progressBar = null;
    private JButton cancelButton = null;
    private CancelEventTrigger cancelEventTrigger = null;

    /**
     * Constructs the progress dialog box.
     *
     * @param owner         the Frame over which the progress dialog will be displayed and centred
     * @param title         the title for the progress dialog
     */
    public ProgressDialog(Frame owner, String title, Properties applicationProperties)
    {
        super(owner, title, true);
        this.applicationProperties = applicationProperties;
        initGui();
    }

    private void initGui() {
        // Initialise skins factory.
        skinsFactory = SkinsFactory.getInstance(applicationProperties);

        // Set Skinned Look and Feel.
        LookAndFeel lookAndFeel = skinsFactory.createSkinnedMetalTheme("SkinnedLookAndFeel");
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Unable to set skinned LookAndFeel", e);
        }

        this.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        this.setResizable(true);

        JPanel container = new JPanel(new GridBagLayout());

        statusMessageLabel = skinsFactory.createSkinnedJHtmlLabel("ProgressDialogStatusMessageLabel");
        statusMessageLabel.setText(" ");
        statusMessageLabel.setHorizontalAlignment(JLabel.CENTER);
        container.add(statusMessageLabel,
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        progressBar = skinsFactory.createSkinnedJProgressBar("ProgressDialogProgressBar", 0, 100);
        progressBar.setPreferredSize(new Dimension(550, 20));

        container.add(progressBar,
            new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        detailsTextLabel = skinsFactory.createSkinnedJHtmlLabel("ProgressDialogDetailedMessageLabel");
        detailsTextLabel.setText(" ");
        detailsTextLabel.setHorizontalAlignment(JLabel.CENTER);
        container.add(detailsTextLabel,
            new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Display the cancel button if a cancel event listener is available.
        cancelButton = skinsFactory.createSkinnedJButton("ProgressDialogCancelButton");
        cancelButton.setText("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setDefaultCapable(true);

        container.add(cancelButton,
            new GridBagConstraints(0, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Set Cancel as the default operation when ESCAPE is pressed.
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 4397881858674185924L;

            public void actionPerformed(ActionEvent actionEvent) {
                cancelButton.doClick();
            }
        });

        this.getContentPane().add(container);

        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }

    public void actionPerformed(ActionEvent e) {
        if ("Cancel".equals(e.getActionCommand())) {
            cancelButton.setText("Cancelling...");
            cancelButton.setEnabled(false);

            if (cancelEventTrigger != null) {
                (new Thread(new Runnable() {
                    public void run() {
                        cancelEventTrigger.cancelTask(this);
                    }
                })).start();
            }
        }
    }

    public void dispose() {
        // Progress bar must be set to it's maximum value or it won't clean itself up properly.
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());
        super.dispose();
    }

    /**
     * Displays the progress dialog.
     *
     * @param statusMessage
     *        describes the status of a task text meaningful to the user, such as "3 files of 7 uploaded"
     * @param detailsText
     *        describes the status of a task in more detail, such as the current transfer rate and ETA.
     * @param minTaskValue
     *        the minimum progress value for a task, generally 0
     * @param maxTaskValue
     *        the maximum progress value for a task, such as the total number of threads or 100 if
     *        using percentage-complete as a metric.
     * @param cancelEventListener
     *        listener that is responsible for cancelling a long-lived task when the user clicks
     *        the cancel button. If a task cannot be cancelled this must be null.
     * @param cancelButtonText
     *        text displayed in the cancel button if a task can be cancelled
     */
    public void startDialog(String statusMessage, String detailsText,
        int minTaskValue, int maxTaskValue, CancelEventTrigger cancelEventListener,
        String cancelButtonText)
    {
        this.cancelEventTrigger = cancelEventListener;

        if (maxTaskValue > minTaskValue) {
            progressBar.setStringPainted(true);
            progressBar.setMinimum(minTaskValue);
            progressBar.setMaximum(maxTaskValue);
            progressBar.setValue(minTaskValue);
            progressBar.setIndeterminate(false);
        } else {
            progressBar.setStringPainted(false);
            progressBar.setMinimum(0);
            progressBar.setMaximum(0);
            progressBar.setValue(0);
            progressBar.setIndeterminate(true);
        }

        statusMessageLabel.setText(statusMessage);
        if (detailsText == null) {
            detailsTextLabel.setText("");
        } else {
            detailsTextLabel.setText(detailsText);
        }

        if (this.cancelEventTrigger != null) {
            cancelButton.setText(cancelButtonText);
            cancelButton.setEnabled(true);
            cancelButton.setVisible(true);
        } else {
            cancelButton.setVisible(false);
//            cancelButton.setText("Cannot Cancel");
//            cancelButton.setEnabled(false);
        }

        this.setVisible(true);
    }

    public void stopDialog() {
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());
        this.setVisible(false);
    }

    /**
     * Updates the dialog's information.
     *
     * @param statusMessage
     *        text describing the status of a task text meaningful to the user, such as "3 files of 7 uploaded"
     * @param detailsText
     *        detailed description of the task's status, such as the current transfer rate and remaining time.
     * @param progressValue
     *        value representing how far through the task we are, somewhere between
     *        minTaskValue and maxTaskValue as set in the constructor.
     */
    public void updateDialog(String statusMessage, String detailsText, int progressValue) {
        statusMessageLabel.setText(statusMessage);
        detailsTextLabel.setText(detailsText);
        if (!progressBar.isIndeterminate()) {
            progressBar.setValue(progressValue);
        }
    }

}
