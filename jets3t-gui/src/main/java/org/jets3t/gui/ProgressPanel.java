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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.multithread.CancelEventTrigger;

/**
 * A panel that displays the progress of a task in a progress bar, and allows
 * the task to be cancelled.
 *
 * @author James Murty
 */
public class ProgressPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = 7880101403853417240L;

    private static final Log log = LogFactory.getLog(ProgressPanel.class);

    private final Insets insetsDefault = new Insets(5, 2, 5, 2);
    private final GuiUtils guiUtils = new GuiUtils();

    private Properties applicationProperties = null;
    private SkinsFactory skinsFactory = null;

    private JLabel statusMessageLabel = null;
    private JProgressBar progressBar = null;
    private JButton cancelButton = null;
    private CancelEventTrigger cancelEventTrigger = null;

    public ProgressPanel(Properties applicationProperties, CancelEventTrigger cancelEventTrigger)
    {
        super();
        this.applicationProperties = applicationProperties;
        this.cancelEventTrigger = cancelEventTrigger;
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

        this.setLayout(new GridBagLayout());

        statusMessageLabel = skinsFactory.createSkinnedJHtmlLabel("ProgressPanelStatusMessageLabel");
        statusMessageLabel.setText(" ");
        statusMessageLabel.setHorizontalAlignment(JLabel.CENTER);
        this.add(statusMessageLabel,
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        progressBar = skinsFactory.createSkinnedJProgressBar("ProgressPanelProgressBar", 0, 100);
        this.add(progressBar,
            new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Display the cancel button if a cancel event listener is available.
        cancelButton = skinsFactory.createSkinnedJButton("ProgressPanelCancelButton");
        guiUtils.applyIcon(cancelButton, "/images/nuvola/16x16/actions/cancel.png");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setEnabled(cancelEventTrigger != null);

        this.add(cancelButton,
            new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
    }

    public void actionPerformed(ActionEvent e) {
        if ("Cancel".equals(e.getActionCommand())) {
            cancelButton.setEnabled(false);

            if (cancelEventTrigger != null) {
                cancelEventTrigger.cancelTask(this);
            }
        }
    }

    public void dispose() {
        // Progress bar must be set to it's maximum value or it won't clean itself up properly.
        progressBar.setIndeterminate(false);
        progressBar.setValue(progressBar.getMaximum());
    }

    /**
     * Displays the progress dialog.
     *
     * @param statusMessage
     *        describes the status of a task text meaningful to the user, such as "3 files of 7 uploaded"
     * @param minTaskValue
     *        the minimum progress value for a task, generally 0
     * @param maxTaskValue
     *        the maximum progress value for a task, such as the total number of threads or 100 if
     *        using percentage-complete as a metric.
     */
    public void startProgress(String statusMessage, int minTaskValue, int maxTaskValue)
    {
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
    }

    public void updateProgress(String statusMessage, int progressValue) {
        statusMessageLabel.setText(statusMessage);
        if (!progressBar.isIndeterminate()) {
            progressBar.setValue(progressValue);
        }
    }

}
