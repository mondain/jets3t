/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2007-2011 James Murty
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
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.utils.ServiceUtils;

/**
 * An Error dialog that displays information about an error that has occurred.
 *
 * @author James Murty
 */
public class ErrorDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -1120587010395375292L;

    private static final Log log = LogFactory.getLog(ErrorDialog.class);

    private final Jets3tProperties jets3tProperties =
        Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);

    private Properties applicationProperties = null;

    private HyperlinkActivatedListener hyperlinkListener = null;
    private SkinsFactory skinsFactory = null;

    private final Insets insetsDefault = new Insets(3, 5, 3, 5);


    private ErrorDialog(Frame ownerFrame, HyperlinkActivatedListener hyperlinkListener,
            Properties applicationProperties)
    {
        super(ownerFrame, "Error Message", true);
        this.hyperlinkListener = hyperlinkListener;
        this.applicationProperties = applicationProperties;
    }

    private ErrorDialog(JDialog ownerDialog, HyperlinkActivatedListener hyperlinkListener,
            Properties applicationProperties)
    {
        super(ownerDialog, "Error Message", true);
        this.hyperlinkListener = hyperlinkListener;
        this.applicationProperties = applicationProperties;
    }

    /**
     * Initialises all GUI elements.
     */
    private void initGui(String message, String details) {
        // Initialise skins factory.
        skinsFactory = SkinsFactory.getInstance(applicationProperties);

        // Set Skinned Look and Feel.
        LookAndFeel lookAndFeel = skinsFactory.createSkinnedMetalTheme("SkinnedLookAndFeel");
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Unable to set skinned LookAndFeel", e);
        }

        this.setResizable(false);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        JHtmlLabel messageLabel = skinsFactory
            .createSkinnedJHtmlLabel("ErrorMessageLabel", hyperlinkListener);
        messageLabel.setText(message);
        messageLabel.setHorizontalAlignment(JLabel.CENTER);
        JHtmlLabel detailsLabel = skinsFactory
            .createSkinnedJHtmlLabel("ErrorDetailsLabel", hyperlinkListener);
        detailsLabel.setText(details);

        JButton okButton = skinsFactory.createSkinnedJButton("ErrorOkButton");
        okButton.setName("OK");
        okButton.setText("OK");
        okButton.addActionListener(this);
        this.getRootPane().setDefaultButton(okButton);

        JPanel dialogPanel = skinsFactory.createSkinnedJPanel("ErrorDialogPanel");

        int row = 0;
        dialogPanel.setLayout(new GridBagLayout());
        dialogPanel.add(messageLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(12, 5, 12, 5), 0, 0));
        dialogPanel.add(detailsLabel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        dialogPanel.add(okButton, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        this.getContentPane().add(dialogPanel);
        this.pack();
        this.setLocationRelativeTo(this.getOwner());
    }

    public void actionPerformed(ActionEvent e) {
        if ("OK".equals(e.getActionCommand())) {
            this.setVisible(false);
        }
    }

    private String buildDetailedText(Throwable throwable) {
        if (!jets3tProperties.getBoolProperty("gui.verboseErrorDialog", true)) {
            return null;
        }

        StringBuffer detailsText = new StringBuffer();
        if (throwable instanceof S3ServiceException) {
            detailsText.append("<table border=\"0\">");

            S3ServiceException s3se = (S3ServiceException) throwable;

            if (s3se.getS3ErrorCode() != null) {
                detailsText.append("<tr><td><b>S3 Error Code</b></td><td>").append(s3se.getS3ErrorCode()).append("</td></tr>");
            } else {
                String msg = throwable.getMessage();
                if (msg.length()>80){
                    ServiceUtils.wrapString(msg, "<br/>", 80);
                }
                detailsText.append("<tr><td><b>Exception message</b></td></tr><tr><td>")
                    .append(msg).append("</td></tr>");
            }

            if (s3se.getS3ErrorMessage() != null) {
                detailsText.append("<tr><td><b>S3 Message</b></td><td>")
                    .append(s3se.getS3ErrorMessage()).append("</td></tr>");
            }

            detailsText.append("<tr><td><b>HTTP Status Code</b></td><td>")
                .append(s3se.getResponseCode())
                .append("</td></tr>");

            if (s3se.getS3ErrorRequestId() != null) {
                detailsText.append("<tr><td><b>S3 Request Id</b></td><td>")
                    .append(s3se.getS3ErrorRequestId()).append("</td></tr>");
            }

            if (s3se.getS3ErrorHostId() != null) {
                detailsText.append("<tr><td><b>S3 Host Id</b></td><td>")
                    .append(s3se.getS3ErrorHostId()).append("</td></tr>");
            }

            boolean firstCause = true;
            Throwable cause = s3se.getCause();
            while (cause != null && cause.getMessage() != null) {
                if (firstCause) {
                    detailsText.append("<tr><td><b>Cause</b></td></tr>");
                }
                detailsText.append("<tr><td>").append(cause.getMessage()).append("</td></tr>");
                firstCause = false;
                cause = cause.getCause();
            }

            detailsText.append("</table>");
        } else {
            boolean firstCause = true;
            Throwable cause = throwable;
            while (cause != null && cause.getMessage() != null) {
                if (firstCause) {
                    detailsText.append("<tr><td><b>Cause</b></td></tr>");
                }
                detailsText.append("<tr><td>").append(cause.getMessage()).append("</td></tr>");
                firstCause = false;
                cause = cause.getCause();
            }
        }

        if (detailsText.length() > 0) {
            detailsText.insert(0, "<html>");
            detailsText.append("</html>");
        }

        return detailsText.toString();
    }

    /**
     * Shows the error dialog and waits for the user to acknowledge the dialog.
     * <p>
     * If the JetS3t property <tt>gui.verboseErrorDialog</tt> is set to true, this dialog will
     * display detailed information about the root cause of the error (the throwable, if provided)
     * </p>
     *
     * @param ownerFrame
     * the frame that will own the dialog
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     * @param applicationProperties
     * property settings for this application
     * @param message
     * a general error message, which should ideally be somewhat user-friendly.
     * @param throwable
     * the underlying exception that caused the error.
     */
    public static void showDialog(Frame ownerFrame, HyperlinkActivatedListener hyperlinkListener,
            Properties applicationProperties, String message, Throwable throwable)
    {
        log.warn("Showing ErrorDialog: message=" + message, throwable);
        ErrorDialog dialog = new ErrorDialog(ownerFrame, hyperlinkListener, applicationProperties);
        dialog.initGui(message, dialog.buildDetailedText(throwable));
        dialog.setVisible(true);
        dialog.dispose();
    }

    /**
     * Shows the error dialog and waits for the user to acknowledge the dialog.
     * <p>
     * If the JetS3t property <tt>gui.verboseErrorDialog</tt> is set to true, this dialog will
     * display detailed information about the root cause of the error (the throwable, if provided)
     * </p>
     *
     * @param ownerDialog
     * the dialog that will own this dialog
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     * @param applicationProperties
     * property settings for this application
     * @param message
     * a general error message, which should ideally be somewhat user-friendly.
     * @param throwable
     * the underlying exception that caused the error.
     */
    public static void showDialog(JDialog ownerDialog, HyperlinkActivatedListener hyperlinkListener,
            Properties applicationProperties, String message, Throwable throwable)
    {
        ErrorDialog dialog = new ErrorDialog(ownerDialog, hyperlinkListener, applicationProperties);
        dialog.initGui(message, dialog.buildDetailedText(throwable));
        dialog.setVisible(true);
        dialog.dispose();
    }

    /**
     * Shows the error dialog and waits for the user to acknowledge the dialog.
     * <p>
     * If the JetS3t property <tt>gui.verboseErrorDialog</tt> is set to true, this dialog will
     * display detailed information about the root cause of the error (the throwable, if provided)
     * </p>
     *
     * @param ownerFrame
     * the frame that will own the dialog
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     * @param message
     * a general error message, which should ideally be somewhat user-friendly.
     * @param throwable
     * the underlying exception that caused the error.
     */
    public static void showDialog(Frame ownerFrame, HyperlinkActivatedListener hyperlinkListener,
            String message, Throwable throwable)
    {
        showDialog(ownerFrame, hyperlinkListener, null, message, throwable);
    }

    /**
     * Shows the error dialog and waits for the user to acknowledge the dialog.
     * <p>
     * If the JetS3t property <tt>gui.verboseErrorDialog</tt> is set to true, this dialog will
     * display detailed information about the root cause of the error (the throwable, if provided)
     * </p>
     *
     * @param ownerDialog
     * the dialog that will own this dialog
     * @param hyperlinkListener
     * the listener that will act on any hyperlink events triggered by the user clicking on HTTP links.
     * @param message
     * a general error message, which should ideally be somewhat user-friendly.
     * @param throwable
     * the underlying exception that caused the error.
     */
    public static void showDialog(JDialog ownerDialog, HyperlinkActivatedListener hyperlinkListener,
            String message, Throwable throwable)
    {
        showDialog(ownerDialog, hyperlinkListener, null, message, throwable);
    }

}
