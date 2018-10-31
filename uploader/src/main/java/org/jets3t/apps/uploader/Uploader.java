/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2011 James Murty
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
package org.jets3t.apps.uploader;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.swing.BorderFactory;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.httpclient.contrib.proxy.PluginProxyUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;
import org.jets3t.gui.AuthenticationDialog;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.GuiUtils;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.gui.UserInputFields;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.io.ProgressMonitoredInputStream;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.multithread.CancelEventTrigger;
import org.jets3t.service.multithread.CopyObjectsEvent;
import org.jets3t.service.multithread.CreateBucketsEvent;
import org.jets3t.service.multithread.CreateObjectsEvent;
import org.jets3t.service.multithread.DeleteObjectsEvent;
import org.jets3t.service.multithread.DeleteVersionedObjectsEvent;
import org.jets3t.service.multithread.DownloadObjectsEvent;
import org.jets3t.service.multithread.GetObjectHeadsEvent;
import org.jets3t.service.multithread.GetObjectsEvent;
import org.jets3t.service.multithread.ListObjectsEvent;
import org.jets3t.service.multithread.LookupACLEvent;
import org.jets3t.service.multithread.S3ServiceEventListener;
import org.jets3t.service.multithread.S3ServiceMulti;
import org.jets3t.service.multithread.ServiceEvent;
import org.jets3t.service.multithread.ThreadWatcher;
import org.jets3t.service.multithread.UpdateACLEvent;
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.RestUtils;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.TimeFormatter;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.service.utils.signedurl.SignedUrlAndObject;

import contribs.com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Dual application and applet for uploading files and XML metadata information to the Amazon S3
 * service.
 * <p>
 * For more information and help please see the
 * <a href="http://www.jets3t.org/applications/uploader.html">Uploader Guide</a>.
 * </p>
 * <p>
 * The Uploader is highly configurable through properties specified in a file
 * <tt>uploader.properties</tt>. This file <b>must be available</b> at the root of the classpath.
 *
 * @author James Murty
 */
public class Uploader extends JApplet implements S3ServiceEventListener, ActionListener, ListSelectionListener, HyperlinkActivatedListener, CredentialsProvider {
    private static final long serialVersionUID = 2759324769352022783L;

    private static final Log log = LogFactory.getLog(Uploader.class);

    public static final String APPLICATION_DESCRIPTION = "Uploader/" + Constants.JETS3T_VERSION;

    public static final String UPLOADER_PROPERTIES_FILENAME = "uploader.properties";

    private static final String UPLOADER_VERSION_ID = "JetS3t Uploader/" + Constants.JETS3T_VERSION;

    public static final int WIZARD_SCREEN_1 = 1;
    public static final int WIZARD_SCREEN_2 = 2;
    public static final int WIZARD_SCREEN_3 = 3;
    public static final int WIZARD_SCREEN_4 = 4;
    public static final int WIZARD_SCREEN_5 = 5;

    /*
     * Error codes displayed when errors occur. Each possbile error condition has its own code
     * to aid in resolving user's problems.
     */
    public static final String ERROR_CODE__MISSING_REQUIRED_PARAM = "100";
    public static final String ERROR_CODE__S3_UPLOAD_FAILED = "101";
    public static final String ERROR_CODE__UPLOAD_REQUEST_DECLINED = "102";
    public static final String ERROR_CODE__TRANSACTION_ID_REQUIRED_TO_CREATE_XML_SUMMARY = "103";

    /*
     * HTTP connection settings for communication *with Gatekeeper only*, the
     * S3 connection parameters are set in the jets3t.properties file.
     */
    public static final int HTTP_CONNECTION_TIMEOUT = 60000;
    public static final int SOCKET_CONNECTION_TIMEOUT = 60000;
    public static final int MAX_CONNECTION_RETRIES = 5;


    private Frame ownerFrame = null;

    private UserInputFields userInputFields = null;
    private Properties userInputProperties = null;

    private HttpClient httpClientGatekeeper = null;
    private S3ServiceMulti s3ServiceMulti = null;

    /**
     * The files to upload to S3.
     */
    private File[] filesToUpload = null;

    /**
     * The list of file extensions accepted by the Uploader.
     */
    private ArrayList validFileExtensions = new ArrayList();

    /**
     * Uploader's properties.
     */
    private Jets3tProperties uploaderProperties = null;

    /**
     * Properties set in stand-alone application from the command line arguments.
     */
    private Properties standAloneArgumentProperties = null;

    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final TimeFormatter timeFormatter = new TimeFormatter();

    /*
     * Upload file constraints.
     */
    private int fileMaxCount = 0;
    private long fileMaxSizeMB = 0;
    private long fileMinSizeMB = 0;

    /*
     * Insets used throughout the application.
     */
    private final Insets insetsDefault = new Insets(3, 5, 3, 5);
    private final Insets insetsNone = new Insets(0, 0, 0, 0);

    private final GuiUtils guiUtils = new GuiUtils();

    private int currentState = 0;

    private final boolean isRunningAsApplet;

    private HashMap parametersMap = new HashMap();

    private SkinsFactory skinsFactory = null;
    private final GridBagLayout GRID_BAG_LAYOUT = new GridBagLayout();

    /*
     * GUI elements that need to be referenced outside initGui method.
     */
    private JHtmlLabel userGuidanceLabel = null;
    private JPanel appContentPanel = null;
    private JPanel buttonsPanel = null;
    private JPanel primaryPanel = null;
    private CardLayout primaryPanelCardLayout = null;
    private CardLayout buttonsPanelCardLayout = null;
    private JButton backButton = null;
    private JButton nextButton = null;
    private JButton cancelUploadButton = null;
    private JHtmlLabel dragDropTargetLabel = null;
    private JHtmlLabel fileToUploadLabel = null;
    private JHtmlLabel fileInformationLabel = null;
    private JHtmlLabel progressTransferDetailsLabel = null;
    private JProgressBar progressBar = null;
    private JHtmlLabel progressStatusTextLabel = null;
    private JHtmlLabel finalMessageLabel = null;
    private CancelEventTrigger uploadCancelEventTrigger = null;

    /**
     * Set to true when the object/file being uploaded is the final in a set, eg when
     * the XML metadata is being uploaded after a movie file.
     */
    private volatile boolean uploadingFinalObject = false;

    /**
     * If set to true via the "xmlSummary" configuration property, the Uploader will
     * generate an XML summary document describing the files uploaded by the user and
     * will upload this summary document to S3.
     */
    private volatile boolean includeXmlSummaryDoc = false;

    /**
     * Set to true when a file upload has been cancelled, to prevent the Uploader from
     * uploading an XML summary file when the prior file upload was cancelled.
     */
    private volatile boolean uploadCancelled = false;

    /**
     * Set to true if an upload failed due to a key name clash in S3, in which case an error
     * message is displayed in the final 'thankyou' screen.
     */
    private boolean fatalErrorOccurred = false;

    /**
     * Variable to store application exceptions, so that client failure information can be
     * included in the information provided to the Gatekeeper when uploads are retried.
     */
    private Exception priorFailureException = null;


    private final CredentialsProvider mCredentialProvider;


    private Uploader(boolean pIsRunningApplet){
        isRunningAsApplet = pIsRunningApplet;
        mCredentialProvider = new BasicCredentialsProvider();
    }

    /**
     * Constructor to run this application as an Applet.
     */
    public Uploader() {
        this(true);
    }

    /**
     * Constructor to run this application in a stand-alone window.
     *
     * @param ownerFrame the frame the application will be displayed in
     * @throws S3ServiceException
     */
    public Uploader(JFrame ownerFrame, Properties standAloneArgumentProperties) throws S3ServiceException {
        this(false);
        this.ownerFrame = ownerFrame;
        this.standAloneArgumentProperties = standAloneArgumentProperties;

        init();

        ownerFrame.getContentPane().add(this);
        ownerFrame.setBounds(this.getBounds());
        ownerFrame.setVisible(true);
    }

    /**
     * Prepares application to run as a GUI by finding/creating a root owner JFrame, and
     * (if necessary) creating a directory for storing remembered logins.
     */
    @Override
    public void init() {
        super.init();

        boolean isMissingRequiredInitProperty = false;

        // Find or create a Frame to own modal dialog boxes.
        if (this.ownerFrame == null) {
            Component c = this;
            while (!(c instanceof Frame) && c.getParent() != null) {
                c = c.getParent();
            }
            if (!(c instanceof Frame)) {
                this.ownerFrame = new JFrame();
            } else {
                this.ownerFrame = (Frame) c;
            }
        }

        // Read properties from uploader.properties in classpath.
        uploaderProperties = Jets3tProperties.getInstance(UPLOADER_PROPERTIES_FILENAME);

        if (isRunningAsApplet) {
            // Read parameters for Applet, based on names specified in the uploader properties.
            String appletParamNames = uploaderProperties.getStringProperty("applet.params", "");
            StringTokenizer st = new StringTokenizer(appletParamNames, ",");
            while (st.hasMoreTokens()) {
                String paramName = st.nextToken();
                String paramValue = this.getParameter(paramName);

                // Fatal error if a parameter is missing.
                if (null == paramValue) {
                    log.error("Missing required applet parameter: " + paramName);
                    isMissingRequiredInitProperty = true;
                } else {
                    log.debug("Found applet parameter: " + paramName + "='" + paramValue + "'");

                    // Set params as properties in the central properties source for this application.
                    // Note that parameter values will over-write properties with the same name.
                    uploaderProperties.setProperty(paramName, paramValue);

                    // Store params in a separate map, which is used to build XML document.
                    parametersMap.put(paramName, paramValue);
                }
            }
        } else {
            // Add application parameters properties.
            if (standAloneArgumentProperties != null) {
                Enumeration e = standAloneArgumentProperties.keys();
                while (e.hasMoreElements()) {
                    String propName = (String) e.nextElement();
                    String propValue = standAloneArgumentProperties.getProperty(propName);

                    // Fatal error if a parameter is missing.
                    if (null == propValue) {
                        log.error("Missing required command-line property: " + propName);
                        isMissingRequiredInitProperty = true;
                    } else {
                        log.debug("Using command-line property: " + propName + "='" + propValue + "'");

                        // Set arguments as properties in the central properties source for this application.
                        // Note that argument values will over-write properties with the same name.
                        uploaderProperties.setProperty(propName, propValue);

                        // Store arguments in a separate map, which is used to build XML document.
                        parametersMap.put(propName, propValue);
                    }
                }
            }
        }

        // Determine the file constraints.
        fileMaxCount = uploaderProperties.getIntProperty("file.maxCount", 1);
        fileMaxSizeMB = uploaderProperties.getLongProperty("file.maxSizeMB", 200);
        fileMinSizeMB = uploaderProperties.getLongProperty("file.minSizeMB", 0);

        // Initialise the GUI.
        initGui();

        // Jump to error page if there was an exception raised during initialisation.
        if (isMissingRequiredInitProperty) {
            failWithFatalError(ERROR_CODE__MISSING_REQUIRED_PARAM);
            return;
        }

        // Determine valid file extensions.
        String validFileExtensionsStr = uploaderProperties.getStringProperty("file.extensions", "");
        if (validFileExtensionsStr != null) {
            StringTokenizer st = new StringTokenizer(validFileExtensionsStr, ",");
            while (st.hasMoreTokens()) {
                validFileExtensions.add(st.nextToken().toLowerCase(Locale.getDefault()));
            }
        }
    }


    /**
     * Initialises the application's GUI elements.
     */
    private void initGui() {
        // Initialise skins factory.
        skinsFactory = SkinsFactory.getInstance(uploaderProperties.getProperties());

        // Set Skinned Look and Feel.
        LookAndFeel lookAndFeel = skinsFactory.createSkinnedMetalTheme("SkinnedLookAndFeel");
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Unable to set skinned LookAndFeel", e);
        }

        // Apply branding
        String applicationTitle = replaceMessageVariables(
            uploaderProperties.getStringProperty("gui.applicationTitle", null));
        if (applicationTitle != null) {
            ownerFrame.setTitle(applicationTitle);
        }
        String applicationIconPath = uploaderProperties.getStringProperty("gui.applicationIcon", null);
        if (!isRunningAsApplet && applicationIconPath != null) {
            guiUtils.applyIcon(ownerFrame, applicationIconPath);
        }
        String footerHtml = uploaderProperties.getStringProperty("gui.footerHtml", null);
        String footerIconPath = uploaderProperties.getStringProperty("gui.footerIcon", null);

        // Footer for branding
        boolean includeFooter = false;
        JHtmlLabel footerLabel = skinsFactory.createSkinnedJHtmlLabel("FooterLabel");
        footerLabel.setHyperlinkeActivatedListener(this);
        footerLabel.setHorizontalAlignment(JLabel.CENTER);
        if (footerHtml != null) {
            footerLabel.setText(replaceMessageVariables(footerHtml));
            includeFooter = true;
        }
        if (footerIconPath != null) {
            guiUtils.applyIcon(footerLabel, footerIconPath);
        }

        userInputFields = new UserInputFields(insetsDefault, this, skinsFactory);

        // Screeen 1 : User input fields.
        JPanel screen1Panel = skinsFactory.createSkinnedJPanel("Screen1Panel");
        screen1Panel.setLayout(GRID_BAG_LAYOUT);
        userInputFields.buildFieldsPanel(screen1Panel, uploaderProperties);


        // Screen 2 : Drag/drop panel.
        JPanel screen2Panel = skinsFactory.createSkinnedJPanel("Screen2Panel");
        screen2Panel.setLayout(GRID_BAG_LAYOUT);
        dragDropTargetLabel = skinsFactory.createSkinnedJHtmlLabel("DragDropTargetLabel");
        dragDropTargetLabel.setHyperlinkeActivatedListener(this);
        dragDropTargetLabel.setHorizontalAlignment(JLabel.CENTER);
        dragDropTargetLabel.setVerticalAlignment(JLabel.CENTER);

        JButton chooseFileButton = skinsFactory.createSkinnedJButton("ChooseFileButton");
        chooseFileButton.setActionCommand("ChooseFile");
        chooseFileButton.addActionListener(this);
        configureButton(chooseFileButton, "screen.2.browseButton");

        screen2Panel.add(dragDropTargetLabel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        screen2Panel.add(chooseFileButton,
            new GridBagConstraints(0, 1, 1, 1, 1, 1, GridBagConstraints.NORTH, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Screen 3 : Information about the file to be uploaded.
        JPanel screen3Panel = skinsFactory.createSkinnedJPanel("Screen3Panel");
        screen3Panel.setLayout(GRID_BAG_LAYOUT);
        fileToUploadLabel = skinsFactory.createSkinnedJHtmlLabel("FileToUploadLabel");
        fileToUploadLabel.setHyperlinkeActivatedListener(this);
        fileToUploadLabel.setHorizontalAlignment(JLabel.CENTER);
        fileToUploadLabel.setVerticalAlignment(JLabel.CENTER);
        screen3Panel.add(fileToUploadLabel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        // Screen 4 : Upload progress.
        JPanel screen4Panel = skinsFactory.createSkinnedJPanel("Screen4Panel");
        screen4Panel.setLayout(GRID_BAG_LAYOUT);
        fileInformationLabel = skinsFactory.createSkinnedJHtmlLabel("FileInformationLabel");
        fileInformationLabel.setHyperlinkeActivatedListener(this);
        fileInformationLabel.setHorizontalAlignment(JLabel.CENTER);
        progressBar = skinsFactory.createSkinnedJProgressBar("ProgressBar", 0, 100);
        progressBar.setStringPainted(true);
        progressStatusTextLabel = skinsFactory.createSkinnedJHtmlLabel("ProgressStatusTextLabel");
        progressStatusTextLabel.setHyperlinkeActivatedListener(this);
        progressStatusTextLabel.setText(" ");
        progressStatusTextLabel.setHorizontalAlignment(JLabel.CENTER);
        progressTransferDetailsLabel = skinsFactory.createSkinnedJHtmlLabel("ProgressTransferDetailsLabel");
        progressTransferDetailsLabel.setHyperlinkeActivatedListener(this);
        progressTransferDetailsLabel.setText(" ");
        progressTransferDetailsLabel.setHorizontalAlignment(JLabel.CENTER);
        cancelUploadButton = skinsFactory.createSkinnedJButton("CancelUploadButton");
        cancelUploadButton.setActionCommand("CancelUpload");
        cancelUploadButton.addActionListener(this);
        configureButton(cancelUploadButton, "screen.4.cancelButton");

        screen4Panel.add(fileInformationLabel,
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        screen4Panel.add(progressBar,
            new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        screen4Panel.add(progressStatusTextLabel,
            new GridBagConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        screen4Panel.add(progressTransferDetailsLabel,
            new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        screen4Panel.add(cancelUploadButton,
            new GridBagConstraints(0, 4, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Screen 5 : Thankyou message.
        JPanel screen5Panel = skinsFactory.createSkinnedJPanel("Screen5Panel");
        screen5Panel.setLayout(GRID_BAG_LAYOUT);
        finalMessageLabel = skinsFactory.createSkinnedJHtmlLabel("FinalMessageLabel");
        finalMessageLabel.setHyperlinkeActivatedListener(this);
        finalMessageLabel.setHorizontalAlignment(JLabel.CENTER);
        screen5Panel.add(finalMessageLabel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        // Wizard Button panel.
        backButton = skinsFactory.createSkinnedJButton("backButton");
        backButton.setActionCommand("Back");
        backButton.addActionListener(this);
        nextButton = skinsFactory.createSkinnedJButton("nextButton");
        nextButton.setActionCommand("Next");
        nextButton.addActionListener(this);

        buttonsPanel = skinsFactory.createSkinnedJPanel("ButtonsPanel");
        buttonsPanelCardLayout = new CardLayout();
        buttonsPanel.setLayout(buttonsPanelCardLayout);
        JPanel buttonsInvisiblePanel = skinsFactory.createSkinnedJPanel("ButtonsInvisiblePanel");
        JPanel buttonsVisiblePanel = skinsFactory.createSkinnedJPanel("ButtonsVisiblePanel");
        buttonsVisiblePanel.setLayout(GRID_BAG_LAYOUT);
        buttonsVisiblePanel.add(backButton,
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        buttonsVisiblePanel.add(nextButton,
            new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        buttonsPanel.add(buttonsInvisiblePanel, "invisible");
        buttonsPanel.add(buttonsVisiblePanel, "visible");

        // Overall content panel.
        appContentPanel = skinsFactory.createSkinnedJPanel("ApplicationContentPanel");
        appContentPanel.setLayout(GRID_BAG_LAYOUT);
        JPanel userGuidancePanel = skinsFactory.createSkinnedJPanel("UserGuidancePanel");
        userGuidancePanel.setLayout(GRID_BAG_LAYOUT);
        primaryPanel = skinsFactory.createSkinnedJPanel("PrimaryPanel");
        primaryPanelCardLayout = new CardLayout();
        primaryPanel.setLayout(primaryPanelCardLayout);
        JPanel navigationPanel = skinsFactory.createSkinnedJPanel("NavigationPanel");
        navigationPanel.setLayout(GRID_BAG_LAYOUT);

        appContentPanel.add(userGuidancePanel,
            new GridBagConstraints(0, 0, 1, 1, 1, 0.2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        appContentPanel.add(primaryPanel,
            new GridBagConstraints(0, 1, 1, 1, 1, 0.6, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        appContentPanel.add(navigationPanel,
            new GridBagConstraints(0, 2, 1, 1, 1, 0.2, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));
        if (includeFooter) {
            log.debug("Adding footer for branding");
            appContentPanel.add(footerLabel,
                new GridBagConstraints(0, 3, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        }
        this.getContentPane().add(appContentPanel);

        userGuidanceLabel = skinsFactory.createSkinnedJHtmlLabel("UserGuidanceLabel");
        userGuidanceLabel.setHyperlinkeActivatedListener(this);
        userGuidanceLabel.setHorizontalAlignment(JLabel.CENTER);

        userGuidancePanel.add(userGuidanceLabel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsNone, 0, 0));
        navigationPanel.add(buttonsPanel,
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsNone, 0, 0));

        primaryPanel.add(screen1Panel, "screen1");
        primaryPanel.add(screen2Panel, "screen2");
        primaryPanel.add(screen3Panel, "screen3");
        primaryPanel.add(screen4Panel, "screen4");
        primaryPanel.add(screen5Panel, "screen5");

        // Set preferred sizes
        int preferredWidth = uploaderProperties.getIntProperty("gui.minSizeWidth", 400);
        int preferredHeight = uploaderProperties.getIntProperty("gui.minSizeHeight", 500);
        this.setBounds(new Rectangle(new Dimension(preferredWidth, preferredHeight)));

        // Initialize drop target.
        initDropTarget(new Component[] {this} );

        // Revert to default Look and Feel for all future GUI elements (eg Dialog boxes).
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.error("Unable to set default system LookAndFeel", e);
        }

        wizardStepForward();
    }

    /**
     * Initialise the application's File drop targets for drag and drop copying of local files
     * to S3.
     *
     * @param dropTargetComponents
     * the components files can be dropped on to transfer them to S3
     */
    private void initDropTarget(Component[] dropTargetComponents) {
        DropTargetListener dropTargetListener = new DropTargetListener() {

            private Border originalBorder = appContentPanel.getBorder();
            private Border dragOverBorder = BorderFactory.createBevelBorder(1);

            private boolean checkValidDrag(DropTargetDragEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    && (DnDConstants.ACTION_COPY == dtde.getDropAction()
                        || DnDConstants.ACTION_MOVE == dtde.getDropAction()))
                {
                    dtde.acceptDrag(dtde.getDropAction());
                    return true;
                } else {
                    dtde.rejectDrag();
                    return false;
                }
            }

            public void dragEnter(DropTargetDragEvent dtde) {
                if (checkValidDrag(dtde)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            appContentPanel.setBorder(dragOverBorder);
                        };
                    });
                }
            }
            public void dragOver(DropTargetDragEvent dtde) {
                checkValidDrag(dtde);
            }

            public void dropActionChanged(DropTargetDragEvent dtde) {
                checkValidDrag(dtde);
            }

            public void dragExit(DropTargetEvent dte) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        appContentPanel.setBorder(originalBorder);
                    };
                });
            }

            public void drop(DropTargetDropEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    && (DnDConstants.ACTION_COPY == dtde.getDropAction()
                        || DnDConstants.ACTION_MOVE == dtde.getDropAction()))
                {
                    dtde.acceptDrop(dtde.getDropAction());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            appContentPanel.setBorder(originalBorder);
                        };
                    });

                    try {
                        final List fileList = (List) dtde.getTransferable().getTransferData(
                            DataFlavor.javaFileListFlavor);
                        if (fileList != null && fileList.size() > 0) {
                            if (checkProposedUploadFiles(fileList)) {
                                wizardStepForward();
                            }
                        }
                    } catch (Exception e) {
                        String errorMessage = "Unable to accept dropped item";
                        log.error(errorMessage, e);
                        ErrorDialog.showDialog(ownerFrame, null, uploaderProperties.getProperties(),
                            errorMessage, e);
                    }
                } else {
                    dtde.rejectDrop();
                }
            }
        };

        // Attach drop target listener to each target component.
        for (int i = 0; i < dropTargetComponents.length; i++) {
            new DropTarget(dropTargetComponents[i], DnDConstants.ACTION_COPY, dropTargetListener, true);
        }
    }

    /**
     * Checks that all the files in a list are acceptable for uploading.
     * Files are checked against the following criteria:
     * <ul>
     * <li>There are not too many</li>
     * <li>The size is greater than the minimum size, and less that the maximum size.</li>
     * <li>The file has a file extension matching one of those explicitly allowed</li>
     * </ul>
     * Any deviations from the rules result in an error message, and the method returns false.
     * A side-effect of this method is that the wizard is moved forward one screen if the
     * files are all valid.
     *
     * @param fileList
     * A list of {@link File}s to check.
     *
     * @return
     * true if the files in the list are all acceptable, false otherwise.
     */
    private boolean checkProposedUploadFiles(List fileList) {
        // Check number of files for upload is within constraints.
        if (fileMaxCount > 0 && fileList.size() > fileMaxCount) {
            String errorMessage = "You may only upload " + fileMaxCount
                + (fileMaxCount == 1? " file" : " files") + " at a time";
            ErrorDialog.showDialog(ownerFrame, this, uploaderProperties.getProperties(),
                errorMessage, null);
            return false;
        }

        // Check file size within constraints.
        Iterator iter = fileList.iterator();
        while (iter.hasNext()) {
            File file = (File) iter.next();
            long fileSizeMB = file.length() / (1024 * 1024);

            if (fileMinSizeMB > 0 && fileSizeMB < fileMinSizeMB) {
                ErrorDialog.showDialog(ownerFrame, this, uploaderProperties.getProperties(),
                    "File size must be greater than " + fileMinSizeMB + " MB", null);
                return false;
            }
            if (fileMaxSizeMB > 0 && fileSizeMB > fileMaxSizeMB) {
                ErrorDialog.showDialog(ownerFrame, this, uploaderProperties.getProperties(),
                    "File size must be less than " + fileMaxSizeMB + " MB", null);
                return false;
            }
        }

        // Check file extension is acceptable.
        if (validFileExtensions.size() > 0) {
            iter = fileList.iterator();
            while (iter.hasNext()) {
                File file = (File) iter.next();
                String fileName = file.getName();
                String fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (!validFileExtensions.contains(fileExtension.toLowerCase(Locale.getDefault()))) {
                    String extList = validFileExtensions.toString();
                    extList = extList.substring(1, extList.length() -1);
                    extList = extList.replaceAll(",", " ");

                    ErrorDialog.showDialog(ownerFrame, this, uploaderProperties.getProperties(),
                        "<html>File name must end with one of the following extensions:<br>" +
                        extList + "</html>", null);
                    return false;
                }
            }
        }

        filesToUpload = (File[]) fileList.toArray(new File[fileList.size()]);
        return true;
    }

    /**
     * Builds a Gatekeeper response based on AWS credential information available in the Uploader
     * properties. The response signs URLs to be valid for 1 day.
     * <p>
     * The required properties are:
     * <ul>
     * <li>AwsAccessKey</li>
     * <li>AwsSecretKey</li>
     * <li>S3BucketName</li>
     * </ul>
     *
     * @param objects
     * @return
     */
    private GatekeeperMessage buildGatekeeperResponse(S3Object[] objects) throws Exception {

        String awsAccessKey = userInputProperties.getProperty("AwsAccessKey");
        String awsSecretKey = userInputProperties.getProperty("AwsSecretKey");
        String s3BucketName = userInputProperties.getProperty("S3BucketName");

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 1);
        Date expiryDate = cal.getTime();

        AWSCredentials awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey);
        RestS3Service s3Service = new RestS3Service(awsCredentials);

        try {
            /*
             *  Build Gatekeeper request.
             */
            GatekeeperMessage gatekeeperMessage = new GatekeeperMessage();
            gatekeeperMessage.addApplicationProperties(userInputProperties); // Add User inputs as application properties.
            gatekeeperMessage.addApplicationProperties(parametersMap); // Add any Applet/Application parameters as application properties.

            for (int i = 0; i < objects.length; i++) {
                String signedPutUrl = s3Service.createSignedPutUrl(
                    s3BucketName, objects[i].getKey(), objects[i].getMetadataMap(),
                    expiryDate, false);

                SignatureRequest signatureRequest = new SignatureRequest(
                    SignatureRequest.SIGNATURE_TYPE_PUT, objects[i].getKey());
                signatureRequest.setBucketName(s3BucketName);
                signatureRequest.setObjectMetadata(objects[i].getMetadataMap());
                signatureRequest.signRequest(signedPutUrl);

                gatekeeperMessage.addSignatureRequest(signatureRequest);
            }

            return gatekeeperMessage;

        } catch (Exception e) {
            throw new Exception("Unable to generate locally-signed PUT URLs for testing", e);
        }
    }

    /**
     * Retrieves a signed PUT URL from the given URL address.
     * The URL must point at a server-side script or service that accepts POST messages.
     * The POST message will include parameters for all the items in uploaderProperties,
     * that is everything in the file uploader.properties plus all the applet's parameters.
     * Based on this input, the server-side script decides whether to allow access and return
     * a signed PUT URL.
     *
     * @param credsProviderParamName
     * the name of the parameter containing the server URL target for the PUT request.
     * @return
     * the AWS credentials provided by the server-side script if access was allowed, null otherwise.
     *
     * @throws HttpException
     * @throws Exception
     */
    private GatekeeperMessage contactGatewayServer(S3Object[] objects)
        throws Exception
    {
        // Retrieve credentials from URL location value by the property 'credentialsServiceUrl'.
        String gatekeeperUrl = uploaderProperties.getStringProperty(
            "gatekeeperUrl", "Missing required property gatekeeperUrl");

        /*
         *  Build Gatekeeper request.
         */
        GatekeeperMessage gatekeeperMessage = new GatekeeperMessage();
        gatekeeperMessage.addApplicationProperties(userInputProperties); // Add User inputs as application properties.
        gatekeeperMessage.addApplicationProperties(parametersMap); // Add any Applet/Application parameters as application properties.

        // Make the Uploader's identifier available to Gatekeeper for version compatibility checking (if necessary)
        gatekeeperMessage.addApplicationProperty(
            GatekeeperMessage.PROPERTY_CLIENT_VERSION_ID, UPLOADER_VERSION_ID);

        // If a prior failure has occurred, add information about this failure.
        if (priorFailureException != null) {
            gatekeeperMessage.addApplicationProperty(GatekeeperMessage.PROPERTY_PRIOR_FAILURE_MESSAGE,
                priorFailureException.getMessage());
            // Now reset the prior failure variable.
            priorFailureException = null;
        }

        // Add all S3 objects as candiates for PUT signing.
        for (int i = 0; i < objects.length; i++) {
            SignatureRequest signatureRequest = new SignatureRequest(
                SignatureRequest.SIGNATURE_TYPE_PUT, objects[i].getKey());
            signatureRequest.setObjectMetadata(objects[i].getMetadataMap());

            gatekeeperMessage.addSignatureRequest(signatureRequest);
        }


        /*
         *  Build HttpClient POST message.
         */

        // Add all properties/parameters to credentials POST request.
        HttpPost postMethod = new HttpPost(gatekeeperUrl);
        Properties properties = gatekeeperMessage.encodeToProperties();

        Iterator<Map.Entry<Object, Object>> propsIter = properties.entrySet().iterator();
        List<NameValuePair> parameters = new ArrayList<NameValuePair>(properties.size());
        while (propsIter.hasNext()) {
            Map.Entry<Object, Object> entry = propsIter.next();
            String fieldName = (String) entry.getKey();
            String fieldValue = (String) entry.getValue();
            parameters.add(new BasicNameValuePair(fieldName, fieldValue));
        }
        postMethod.setEntity(new UrlEncodedFormEntity(parameters));

        // Create Http Client if necessary, and include User Agent information.
        if (httpClientGatekeeper == null) {
            httpClientGatekeeper = initHttpConnection();
        }

        // Try to detect any necessary proxy configurations.
        try {
            HttpHost proxyHost = PluginProxyUtil.detectProxy(new URL(gatekeeperUrl));
            if (proxyHost != null) {
                httpClientGatekeeper.getParams().setParameter(
                        ConnRoutePNames.DEFAULT_PROXY,
                        proxyHost);
            }
            ((DefaultHttpClient)httpClientGatekeeper).setCredentialsProvider(this);

        } catch (Throwable t) {
            log.debug("No proxy detected");
        }

        // Perform Gateway request.
        log.debug("Contacting Gatekeeper at: " + gatekeeperUrl);
        HttpResponse response = null;
        try {
            response = httpClientGatekeeper.execute(postMethod);
            int responseCode = response.getStatusLine().getStatusCode();
            String contentType = response.getFirstHeader("Content-Type").getValue();
            if (responseCode == 200) {
                InputStream responseInputStream = null;


                Header encodingHeader = response.getFirstHeader("Content-Encoding");
                if (encodingHeader != null && "gzip".equalsIgnoreCase(encodingHeader.getValue())) {
                    log.debug("Inflating gzip-encoded response");
                    responseInputStream = new GZIPInputStream(response.getEntity().getContent());
                } else {
                    responseInputStream = response.getEntity().getContent();
                }

                if (responseInputStream == null) {
                    throw new IOException("No response input stream available from Gatekeeper");
                }

                Properties responseProperties = new Properties();
                try {
                    responseProperties.load(responseInputStream);
                } finally {
                    responseInputStream.close();
                }

                GatekeeperMessage gatekeeperResponseMessage =
                    GatekeeperMessage.decodeFromProperties(responseProperties);

                // Check for Gatekeeper Error Code in response.
                String gatekeeperErrorCode = gatekeeperResponseMessage.getApplicationProperties()
                    .getProperty(GatekeeperMessage.APP_PROPERTY_GATEKEEPER_ERROR_CODE);
                if (gatekeeperErrorCode != null) {
                    log.warn("Received Gatekeeper error code: " + gatekeeperErrorCode);
                    failWithFatalError(gatekeeperErrorCode);
                    return null;
                }

                if (gatekeeperResponseMessage.getSignatureRequests().length != objects.length) {
                    throw new Exception("The Gatekeeper service did not provide the necessary "
                        + objects.length + " response items");
                }

                return gatekeeperResponseMessage;
            } else {
                log.debug("The Gatekeeper did not permit a request. Response code: "
                    + responseCode + ", Response content type: " + contentType);
                throw new Exception("The Gatekeeper did not permit your request");
            }
        } catch (Exception e) {
            throw new Exception("Gatekeeper did not respond", e);
        } finally {
            EntityUtils.consume(response.getEntity());
        }
    }

    private GatekeeperMessage retrieveGatekeeperResponse(S3Object[] objects) throws Exception {
        // Check whether Uploader has all necessary credentials from user inputs.
        boolean s3CredentialsProvided =
            userInputProperties.getProperty("AwsAccessKey") != null
            && userInputProperties.getProperty("AwsSecretKey") != null
            && userInputProperties.getProperty("S3BucketName") != null;

        GatekeeperMessage gatekeeperMessage = null;
        if (s3CredentialsProvided) {
            log.debug("S3 login credentials and bucket name are available, the Uploader "
                + "will generate its own Gatekeeper response");
            gatekeeperMessage = buildGatekeeperResponse(objects);
        } else {
            gatekeeperMessage = contactGatewayServer(objects);
        }
        return gatekeeperMessage;
    }

    /**
     * Uploads to S3 the file referenced by the variable fileToUpload, providing
     * progress feedback to the user all the while.
     *
     */
    private void uploadFilesToS3() {
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Creating file hash message
                    progressStatusTextLabel.setText(replaceMessageVariables(
                        uploaderProperties.getStringProperty("screen.4.hashingMessage",
                        "Missing property 'screen.4.hashingMessage'")));
                };
            });

            // Calculate total files size.
            final long filesSizeTotal[] = new long[1];
            for (int i = 0; i < filesToUpload.length; i++) {
                filesSizeTotal[0] += filesToUpload[i].length();
            }

            // Monitor generation of MD5 hash, and provide feedback via the progress bar.
            BytesProgressWatcher progressWatcher = new BytesProgressWatcher(filesSizeTotal[0]) {
                @Override
                public void updateBytesTransferred(long byteCount) {
                    super.updateBytesTransferred(byteCount);

                    final int percentage =
                        (int)((double)getBytesTransferred() * 100 / getBytesToTransfer());
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            progressBar.setValue(percentage);
                        }
                    });
                }
            };

            // Create objects for upload from file listing.
            S3Object[] objectsForUpload = new S3Object[filesToUpload.length];
            for (int i = 0; i < filesToUpload.length; i++) {
                File file = filesToUpload[i];
                log.debug("Computing MD5 hash for file: " + file);
                byte[] fileHash = ServiceUtils.computeMD5Hash(
                    new ProgressMonitoredInputStream( // Report on MD5 hash progress.
                        new FileInputStream(file), progressWatcher));

                S3Object object = new S3Object(null, file);
                object.setMd5Hash(fileHash);
                objectsForUpload[i] = object;
            }

            // Obtain Gatekeeper response.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressStatusTextLabel.setText(
                        replaceMessageVariables(uploaderProperties.getStringProperty("screen.4.connectingMessage",
                        "Missing property 'screen.4.connectingMessage'")));
                    progressBar.setValue(0);
                };
            });

            GatekeeperMessage gatekeeperMessage = null;

            try {
                gatekeeperMessage = retrieveGatekeeperResponse(objectsForUpload);
            } catch (Exception e) {
                log.info("Upload request was denied", e);
                failWithFatalError(ERROR_CODE__UPLOAD_REQUEST_DECLINED);
                return;
            }
            // If we get a null response, presume the error has already been handled.
            if (gatekeeperMessage == null) {
                return;
            }

            log.debug("Gatekeeper response properties: " + gatekeeperMessage.encodeToProperties());

            XmlGenerator xmlGenerator = new XmlGenerator();
            xmlGenerator.addApplicationProperties(gatekeeperMessage.getApplicationProperties());
            xmlGenerator.addMessageProperties(gatekeeperMessage.getMessageProperties());

            SignedUrlAndObject[] uploadItems = prepareSignedObjects(
                objectsForUpload, gatekeeperMessage.getSignatureRequests(), xmlGenerator);

            if (s3ServiceMulti == null) {
                s3ServiceMulti = new S3ServiceMulti(
                    new RestS3Service(null, APPLICATION_DESCRIPTION, this), this);
            }

            /*
             * Prepare XML Summary document for upload, if the summary option is set.
             */
            includeXmlSummaryDoc = uploaderProperties.getBoolProperty("xmlSummary", false);
            S3Object summaryXmlObject = null;
            if (includeXmlSummaryDoc) {
                String priorTransactionId = gatekeeperMessage.getMessageProperties().getProperty(
                    GatekeeperMessage.PROPERTY_TRANSACTION_ID);
                if (priorTransactionId == null) {
                    failWithFatalError(ERROR_CODE__TRANSACTION_ID_REQUIRED_TO_CREATE_XML_SUMMARY);
                    return;
                }

                summaryXmlObject = new S3Object(
                    null, priorTransactionId + ".xml", xmlGenerator.generateXml());
                summaryXmlObject.setContentType(Mimetypes.MIMETYPE_XML);
                summaryXmlObject.addMetadata(GatekeeperMessage.PROPERTY_TRANSACTION_ID, priorTransactionId);
                summaryXmlObject.addMetadata(GatekeeperMessage.SUMMARY_DOCUMENT_METADATA_FLAG, "true");
            }

            // PUT the user's selected files in S3.
            uploadCancelled = false;
            uploadingFinalObject = (!includeXmlSummaryDoc);
            s3ServiceMulti.putObjects(uploadItems);

            // If an XML summary document is required, PUT this in S3 as well.
            if (includeXmlSummaryDoc && !uploadCancelled && !fatalErrorOccurred) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fileInformationLabel.setText(
                            replaceMessageVariables(uploaderProperties.getStringProperty("screen.4.summaryFileInformation",
                            "Missing property 'screen.4.summaryFileInformation'")));
                        progressStatusTextLabel.setText(
                            replaceMessageVariables(uploaderProperties.getStringProperty("screen.4.connectingMessage",
                            "Missing property 'screen.4.connectingMessage'")));
                    };
                });

                // Retrieve signed URL to PUT the XML summary document.
                gatekeeperMessage = retrieveGatekeeperResponse(new S3Object[] {summaryXmlObject});
                SignedUrlAndObject[] xmlSummaryItem =
                    prepareSignedObjects(new S3Object[] {summaryXmlObject},
                        gatekeeperMessage.getSignatureRequests(), null);

                // PUT the XML summary document.
                uploadingFinalObject = true;
                s3ServiceMulti.putObjects(xmlSummaryItem);
            }
        } catch (final Exception e) {
            priorFailureException = e;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    wizardStepBackward();
                    log.error("File upload failed", e);
                    ErrorDialog.showDialog(ownerFrame, null, uploaderProperties.getProperties(),
                        "File upload failed", e);
                };
            });
        }
    }

    private SignedUrlAndObject[] prepareSignedObjects(S3Object[] objects,
        SignatureRequest[] signatureRequests, XmlGenerator xmlGenerator) throws Exception
    {
        List signedObjects = new ArrayList();
        String firstDeclineReason = null;

        for (int i = 0; i < signatureRequests.length; i++) {
            SignatureRequest request = signatureRequests[i];
            S3Object object = objects[i];

            // Store summary information in XML document generator.
            if (xmlGenerator != null) {
                Map clonedMetadata = new HashMap();
                clonedMetadata.putAll(object.getMetadataMap());
                xmlGenerator.addSignatureRequest(object.getKey(), object.getBucketName(),
                    clonedMetadata, request);
            }

            if (request.isSigned()) {
                // Update object with any changes dictated by Gatekeeper.
                if (request.getObjectKey() != null) {
                    object.setKey(request.getObjectKey());
                }
                if (request.getBucketName() != null) {
                    object.setBucketName(request.getBucketName());
                }
                if (request.getObjectMetadata() != null && request.getObjectMetadata().size() > 0) {
                    object.replaceAllMetadata(request.getObjectMetadata());
                }

                SignedUrlAndObject urlAndObject = new SignedUrlAndObject(request.getSignedUrl(), object);
                signedObjects.add(urlAndObject);
            } else {
                // If ANY requests are declined, we will fail with a fatal error message.
                String declineReason = (request.getDeclineReason() == null
                    ? "Unknown"
                    : request.getDeclineReason());
                log.warn("Upload of '" + objects[i].getKey() + "' was declined for reason: "
                    + declineReason);
                if (firstDeclineReason == null) {
                    firstDeclineReason = declineReason;
                }
            }
        }
        if (firstDeclineReason != null) {
            throw new Exception("Your upload" + (objects.length > 1 ? "s were" : " was")
                + " declined by the Gatekeeper. Reason: " + firstDeclineReason);
        }
        return (SignedUrlAndObject[]) signedObjects.toArray(new SignedUrlAndObject[signedObjects.size()]);
    }

    /**
     * Listener method that responds to events from the jets3t toolkit when objects are
     * created in S3 - ie when files are uploaded.
     */
    public void s3ServiceEventPerformed(final CreateObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    // Cancel button is enabled unless this upload is for the XML summary doc.
                    boolean isXmlSummaryUpload = includeXmlSummaryDoc && uploadingFinalObject;
                    cancelUploadButton.setEnabled(!isXmlSummaryUpload);
                }
            });

            ThreadWatcher watcher = event.getThreadWatcher();
            uploadCancelEventTrigger = watcher.getCancelEventListener();

            // Show percentage of bytes transferred.
            String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
            final String statusText = "Uploaded 0 of " + bytesTotalStr;

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressStatusTextLabel.setText(replaceMessageVariables(statusText));
                    progressBar.setValue(0);
                }
            });
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();

            if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                // Upload is completed, just waiting on resonse from S3.
                String statusText = "Upload completed, awaiting confirmation";

                progressBar.setValue(100);
                progressStatusTextLabel.setText(replaceMessageVariables(statusText));
                progressTransferDetailsLabel.setText("");
            } else {
                String bytesCompletedStr = byteFormatter.formatByteSize(watcher.getBytesTransferred());
                String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
                String statusText = "Uploaded " + bytesCompletedStr + " of " + bytesTotalStr;
                int percentage = (int)
                    (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);

                long bytesPerSecond = watcher.getBytesPerSecond();
                String transferDetailsText = "Speed: " + byteFormatter.formatByteSize(bytesPerSecond) + "/s";

                if (watcher.isTimeRemainingAvailable()) {
                    long secondsRemaining = watcher.getTimeRemaining();
                    if (transferDetailsText.trim().length() > 0) {
                        transferDetailsText += " - ";
                    }
                    transferDetailsText += "Time remaining: " + timeFormatter.formatTime(secondsRemaining);
                }

                progressBar.setValue(percentage);
                progressStatusTextLabel.setText(replaceMessageVariables(statusText));
                progressTransferDetailsLabel.setText(replaceMessageVariables(transferDetailsText));
            }
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            if (uploadingFinalObject) {
                drawWizardScreen(WIZARD_SCREEN_5);
            }
            progressBar.setValue(0);
            progressStatusTextLabel.setText("");
            progressTransferDetailsLabel.setText("");
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cancelUploadButton.setEnabled(false);
                }
            });
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            progressBar.setValue(0);
            progressStatusTextLabel.setText("");
            progressTransferDetailsLabel.setText("");
            uploadCancelled = true;
            drawWizardScreen(WIZARD_SCREEN_3);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cancelUploadButton.setEnabled(false);
                }
            });
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            progressBar.setValue(0);
            progressStatusTextLabel.setText("");
            progressTransferDetailsLabel.setText("");
            failWithFatalError(ERROR_CODE__S3_UPLOAD_FAILED);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    cancelUploadButton.setEnabled(false);
                }
            });
        }
    }

    /**
     * Configures a button's text, tooltip and image using uploader properties prefixed
     * with the given properties prefix.
     *
     * @param button
     * @param propertiesPrefix
     */
    private void configureButton(JButton button, String propertiesPrefix) {
        button.setHorizontalAlignment(JLabel.CENTER);

        String buttonImagePath = uploaderProperties
            .getStringProperty(propertiesPrefix + ".image", null);
        String buttonText = replaceMessageVariables(uploaderProperties
            .getStringProperty(propertiesPrefix + ".text", null));
        String buttonTooltip = replaceMessageVariables(uploaderProperties
            .getStringProperty(propertiesPrefix + ".tooltip", null));

        boolean hasImage = false;
        boolean hasText = false;

        if (buttonImagePath != null && buttonImagePath.length() > 0) {
            if (!guiUtils.applyIcon(button, buttonImagePath)) {
                log.error("Unable to load image URL for a button with property prefix '"
                    + propertiesPrefix + "'. Image path: " + buttonImagePath);
            } else {
                hasImage = true;
            }
        }
        if (buttonText != null && buttonText.length() > 0) {
            String text = replaceMessageVariables(buttonText);
            button.setText(text);
            button.setMnemonic(text.charAt(0));
            hasText = true;
        }
        if (buttonTooltip != null && buttonTooltip.length() > 0) {
            button.setToolTipText(buttonTooltip);
        }

        if (!hasImage && !hasText) {
            button.setVisible(false);
        } else {
            button.setVisible(true);
        }
    }

    /**
     * Draws the wizard screen appropriate to the stage in the wizard process the user has
     * reached.
     *
     * @param nextState
     * an integer detailing the screen the user is moving to.
     */
    private void drawWizardScreen(int nextState) {
        // Configure screen based on properties.
        String title = uploaderProperties.getStringProperty(
            "screen." + nextState + ".title", "");
        userGuidanceLabel.setText(replaceMessageVariables(title));

        configureButton(nextButton, "screen." + nextState + ".nextButton");
        configureButton(backButton, "screen." + nextState + ".backButton");

        this.getDropTarget().setActive(false);

        if (nextState == WIZARD_SCREEN_1) {
            primaryPanelCardLayout.show(primaryPanel, "screen1");
            buttonsPanelCardLayout.show(buttonsPanel, "visible");
        } else if (nextState == WIZARD_SCREEN_2) {
            userInputProperties = userInputFields.getUserInputsAsProperties(false);

            primaryPanelCardLayout.show(primaryPanel, "screen2");
            dragDropTargetLabel.setText(
                replaceMessageVariables(uploaderProperties.getStringProperty("screen.2.dragDropPrompt",
                "Missing property 'screen.2.dragDropPrompt'")));
            this.getDropTarget().setActive(true);
        } else if (nextState == WIZARD_SCREEN_3) {
            primaryPanelCardLayout.show(primaryPanel, "screen3");
            String fileInformation = uploaderProperties.getStringProperty("screen.3.fileInformation",
                "Missing property 'screen.3.fileInformation'");
            fileToUploadLabel.setText(replaceMessageVariables(fileInformation));
        } else if (nextState == WIZARD_SCREEN_4) {
            primaryPanelCardLayout.show(primaryPanel, "screen4");

            String fileInformation = uploaderProperties.getStringProperty("screen.4.fileInformation",
                "Missing property 'screen.4.fileInformation'");
            fileInformationLabel.setText(replaceMessageVariables(fileInformation));

            cancelUploadButton.setEnabled(false);
            new Thread() {
                @Override
                public void run() {
                    uploadFilesToS3();
                }
            }.start();
        } else if (nextState == WIZARD_SCREEN_5) {
            primaryPanelCardLayout.show(primaryPanel, "screen5");

            String finalMessage = null;
            if (fatalErrorOccurred) {
                finalMessage = uploaderProperties.getStringProperty("screen.5.errorMessage",
                    "Missing property 'screen.5.errorMessage'");
            } else {
                finalMessage = uploaderProperties.getStringProperty("screen.5.thankyouMessage",
                    "Missing property 'screen.5.thankyouMessage'");
            }

            finalMessageLabel.setText(replaceMessageVariables(finalMessage));
        } else {
            log.error("Ignoring unexpected wizard screen number: " + nextState);
            return;
        }
        currentState = nextState;
    }

    /**
     * Move the wizard forward one step/screen.
     */
    private void wizardStepForward() {
        drawWizardScreen(currentState + 1);
    }

    /**
     * Move the wizard backward one step/screen.
     */
    private void wizardStepBackward() {
        drawWizardScreen(currentState - 1);
    }

    /**
     * When a fatal error occurs, go straight to last screen to display the error message
     * and make the error code available as a variable (<code>${errorCode}</code>) to be used
     * in the error message displayed to the user.
     * <p>
     * If there is an Uploader property <code>errorCodeMessage.&lt;code&gt;</code> corresponding
     * to this error code, the value of this property is made available as a variable
     * (<code>${errorMessage}</code>). If there is no such property available the
     * <code>${errorMessage}</code> variable will be an empty string.
     *
     *
     * @param errorCode
     * the error code, which may correspond with an error message in uploader.properties.
     */
    private void failWithFatalError(String errorCode) {
        uploaderProperties.setProperty("errorCode", errorCode);

        String errorCodeMessagePropertyName = "errorCodeMessage." + errorCode;
        String errorCodeMessage = uploaderProperties.getStringProperty(errorCodeMessagePropertyName, "");
        uploaderProperties.setProperty("errorMessage", errorCodeMessage);

        fatalErrorOccurred = true;
        drawWizardScreen(WIZARD_SCREEN_5);
    }

    /**
     * Replaces variables of the form ${variableName} in the input string with the value of that
     * variable name in the local uploaderProperties properties object, or with one of the
     * following special variables:
     * <ul>
     * <li>fileName : Name of file being uploaded</li>
     * <li>fileSize : Size of the file being uploaded, eg 1.04 MB</li>
     * <li>filePath : Absolute path of the file being uploaded</li>
     * <li>maxFileSize : The maxiumum allowed file size in MB</li>
     * <li>maxFileCount : The maximum number of files that may be uploaded</li>
     * <li>validFileExtensions : A list of the file extensions allowed</li>
     * </ul>
     * If the variable named in the input string is not available, or has no value, the variable
     * reference is left in the result.
     *
     * @param message
     * string that may have variables to replace
     * @return
     * the input string with any variable referenced replaced with the variable's value.
     */
    private String replaceMessageVariables(String message) {
        if (message == null) {
            return "";
        }

        String result = message;
        // Replace upload file variables, if an upload file has been chosen.
        if (filesToUpload != null) {
            long filesSize = 0;
            StringBuffer fileNameList = new StringBuffer();
            for (int i = 0; i < filesToUpload.length; i++) {
                filesSize += filesToUpload[i].length();
                fileNameList.append(filesToUpload[i].getName()).append(" ");
            }

            result = result.replaceAll("\\$\\{fileNameList\\}", fileNameList.toString());
            result = result.replaceAll("\\$\\{filesSize\\}", byteFormatter.formatByteSize(filesSize));
        }
        result = result.replaceAll("\\$\\{maxFileSize\\}", String.valueOf(fileMaxSizeMB));
        result = result.replaceAll("\\$\\{maxFileCount\\}", String.valueOf(fileMaxCount));

        String extList = validFileExtensions.toString();
        extList = extList.substring(1, extList.length() -1);
        extList = extList.replaceAll(",", " ");
        result = result.replaceAll("\\$\\{validFileExtensions\\}", extList);

        Pattern pattern = Pattern.compile("\\$\\{.+?\\}");
        Matcher matcher = pattern.matcher(result);
        int offset = 0;
        while (matcher.find(offset)) {
            String variable = matcher.group();
            String variableName = variable.substring(2, variable.length() - 1);

            String replacement = null;
            if (userInputProperties != null && userInputProperties.containsKey(variableName)) {
                log.debug("Replacing variable '" + variableName + "' with value from a user input field");
                replacement = userInputProperties.getProperty(variableName, null);
            } else if (parametersMap != null && parametersMap.containsKey(variableName)) {
                log.debug("Replacing variable '" + variableName + "' with value from Uploader's parameters");
                replacement = (String) parametersMap.get(variableName);
            } else if (uploaderProperties != null && uploaderProperties.containsKey(variableName)) {
                log.debug("Replacing variable '" + variableName + "' with value from uploader.properties file");
                replacement = uploaderProperties.getStringProperty(variableName, null);
            }

            if (replacement != null) {
                result = result.substring(0, matcher.start()) + replacement +
                result.substring(matcher.end());
                offset = matcher.start() + 1;
                matcher.reset(result);
            } else {
                offset = matcher.start() + 1;
            }
        }
        if (!result.equals(message)) {
            log.debug("Replaced variables in text: " + message + " => " + result);
        }
        return result;
    }

    /**
     * Handles GUI actions.
     */
    public void actionPerformed(ActionEvent actionEvent) {
        if ("Next".equals(actionEvent.getActionCommand())) {
            wizardStepForward();
        } else if ("Back".equals(actionEvent.getActionCommand())) {
            wizardStepBackward();
        } else if ("ChooseFile".equals(actionEvent.getActionCommand())) {
            JFileChooser fileChooser = new JFileChooser();

            if (validFileExtensions.size() > 0) {
                UploaderFileExtensionFilter filter = new UploaderFileExtensionFilter(
                    "Allowed files", validFileExtensions);
                fileChooser.setFileFilter(filter);
            }

            fileChooser.setMultiSelectionEnabled(fileMaxCount > 1);
            fileChooser.setDialogTitle("Choose file" + (fileMaxCount > 1 ? "s" : "") + " to upload");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setApproveButtonText("Choose file" + (fileMaxCount > 1 ? "s" : ""));

            int returnVal = fileChooser.showOpenDialog(ownerFrame);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
            }

            List fileList = new ArrayList();
            if (fileChooser.getSelectedFiles().length > 0) {
                fileList.addAll(Arrays.asList(fileChooser.getSelectedFiles()));
            } else {
                fileList.add(fileChooser.getSelectedFile());
            }
            if (checkProposedUploadFiles(fileList)) {
                wizardStepForward();
            }
        } else if ("CancelUpload".equals(actionEvent.getActionCommand())) {
            if (uploadCancelEventTrigger != null) {
                uploadCancelEventTrigger.cancelTask(this);
                progressBar.setValue(0);
            } else {
                log.warn("Ignoring attempt to cancel file upload when cancel trigger is not available");
            }
        } else {
            log.warn("Unrecognised action command, ignoring: " + actionEvent.getActionCommand());
        }
    }

    private HttpClient initHttpConnection() {
        // Set client parameters.
        HttpParams params = RestUtils.createDefaultHttpParams();
        HttpProtocolParams.setUserAgent(
                params,
                ServiceUtils.getUserAgentDescription(APPLICATION_DESCRIPTION));

        // Set connection parameters.
        HttpConnectionParams.setConnectionTimeout(
                params,
                HTTP_CONNECTION_TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, SOCKET_CONNECTION_TIMEOUT);
        HttpConnectionParams.setStaleCheckingEnabled(params, false);

        DefaultHttpClient httpClient = new DefaultHttpClient(params);
        // Replace default error retry handler.
        httpClient.setHttpRequestRetryHandler(new RestUtils.JetS3tRetryHandler(
                MAX_CONNECTION_RETRIES,
                null));

        return httpClient;
    }

    /**
     * Follows hyperlinks clicked on by a user. This is achieved differently depending on whether
     * Cockpit is running as an applet or as a stand-alone application:
     * <ul>
     * <li>Application: Detects the default browser application for the user's system (using
     * <tt>BareBonesBrowserLaunch</tt>) and opens the link as a new window in that browser</li>
     * <li>Applet: Opens the link in the current browser using the applet's context</li>
     * </ul>
     *
     * @param url
     * the url to open
     * @param target
     * the target pane to open the url in, eg "_blank". This may be null.
     */
    public void followHyperlink(URL url, String target) {
        if (isRunningAsApplet) {
            if (target == null) {
                getAppletContext().showDocument(url);
            } else {
                getAppletContext().showDocument(url, target);
            }
        } else {
            BareBonesBrowserLaunch.openURL(url.toString());
        }
    }

    public void setCredentials(AuthScope authscope, Credentials credentials) {
        mCredentialProvider.setCredentials(authscope, credentials);
    }

    /**
     * Clear credentials.
     */
    public void clear() {
        mCredentialProvider.clear();
    }

    /**
     * Implementation method for the CredentialsProvider interface.
     * <p>
     * Based on sample code:
     * <a href="http://svn.apache.org/viewvc/jakarta/commons/proper/httpclient/trunk/src/examples/InteractiveAuthenticationExample.java?view=markup">InteractiveAuthenticationExample</a>
     *
     */
    public Credentials getCredentials(AuthScope scope) {
        if (scope == null || scope.getScheme() == null) {
            return null;
        }
        Credentials credentials = mCredentialProvider.getCredentials(scope);
        if (credentials!=null){
            return credentials;
        }

        try {
            if (scope.getScheme().equals("ntlm")) {
                //if (authscheme instanceof NTLMScheme) {
                AuthenticationDialog pwDialog = new AuthenticationDialog(
                    ownerFrame, "Authentication Required",
                    "<html>Host <b>" + scope.getHost() + ":" + scope.getPort() +
                    "</b> requires Windows authentication</html>", true);
                pwDialog.setVisible(true);
                if (pwDialog.getUser().length() > 0) {
                    credentials = new NTCredentials(
                            pwDialog.getUser(),
                            pwDialog.getPassword(),
                            scope.getHost(),
                            pwDialog.getDomain());
                }
                pwDialog.dispose();
            } else if (scope.getScheme().equals("basic")
                    || scope.getScheme().equals("digest")) {
                //if (authscheme instanceof RFC2617Scheme) {
                AuthenticationDialog pwDialog = new AuthenticationDialog(ownerFrame,
                        "Authentication Required",
                        "<html><center>Host <b>"
                                + scope.getHost()
                                + ":"
                                + scope.getPort()
                                + "</b>"
                                + " requires authentication for the realm:<br><b>"
                                + scope.getRealm() + "</b></center></html>",
                        false);
                pwDialog.setVisible(true);
                if (pwDialog.getUser().length() > 0) {
                    credentials = new UsernamePasswordCredentials(pwDialog.getUser(), pwDialog.getPassword());
                }
                pwDialog.dispose();
            } else {
                throw new IllegalArgumentException("Unsupported authentication scheme: "
                        + scope.getScheme());
            }
            if (credentials != null){
                mCredentialProvider.setCredentials(scope, credentials);
            }
            return credentials;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }


    // S3 Service events that are not used in this Uploader application.
    public void s3ServiceEventPerformed(ListObjectsEvent event) {}
    public void s3ServiceEventPerformed(CreateBucketsEvent event) {}
    public void s3ServiceEventPerformed(DeleteObjectsEvent event) {}
    public void s3ServiceEventPerformed(GetObjectsEvent event) {}
    public void s3ServiceEventPerformed(GetObjectHeadsEvent event) {}
    public void s3ServiceEventPerformed(LookupACLEvent event) {}
    public void s3ServiceEventPerformed(UpdateACLEvent event) {}
    public void s3ServiceEventPerformed(DownloadObjectsEvent event) {}
    public void s3ServiceEventPerformed(CopyObjectsEvent event) {}
    public void s3ServiceEventPerformed(DeleteVersionedObjectsEvent event) {}
    public void valueChanged(ListSelectionEvent arg0) {}


    /**
     * Run the Uploader as a stand-alone application.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        JFrame ownerFrame = new JFrame("JetS3t Uploader");
        ownerFrame.addWindowListener(new WindowListener() {
            public void windowOpened(WindowEvent e) {
            }
            public void windowClosing(WindowEvent e) {
                e.getWindow().dispose();
            }
            public void windowClosed(WindowEvent e) {
            }
            public void windowIconified(WindowEvent e) {
            }
            public void windowDeiconified(WindowEvent e) {
            }
            public void windowActivated(WindowEvent e) {
            }
            public void windowDeactivated(WindowEvent e) {
            }
        });

        // Read arguments as properties of the form: <propertyName>'='<propertyValue>
        Properties argumentProperties = new Properties();
        if (args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                int delimIndex = arg.indexOf("=");
                if (delimIndex >= 0) {
                    String name = arg.substring(0, delimIndex);
                    String value = arg.substring(delimIndex + 1);
                    argumentProperties.put(name, value);
                } else {
                    System.out.println("Ignoring property argument with incorrect format: " + arg);
                }
            }
        }

        new Uploader(ownerFrame, argumentProperties);
    }

}
