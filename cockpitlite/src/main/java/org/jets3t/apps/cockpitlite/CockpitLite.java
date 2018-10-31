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
package org.jets3t.apps.cockpitlite;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Cursor;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.jets3t.gui.AuthenticationDialog;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.GuiUtils;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.ItemPropertiesDialog;
import org.jets3t.gui.ProgressDialog;
import org.jets3t.gui.ProgressPanel;
import org.jets3t.gui.TableSorter;
import org.jets3t.gui.UserInputFields;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.multithread.CancelEventTrigger;
import org.jets3t.service.multithread.CopyObjectsEvent;
import org.jets3t.service.multithread.CreateBucketsEvent;
import org.jets3t.service.multithread.CreateObjectsEvent;
import org.jets3t.service.multithread.DeleteObjectsEvent;
import org.jets3t.service.multithread.DeleteVersionedObjectsEvent;
import org.jets3t.service.multithread.DownloadObjectsEvent;
import org.jets3t.service.multithread.DownloadPackage;
import org.jets3t.service.multithread.GetObjectHeadsEvent;
import org.jets3t.service.multithread.GetObjectsEvent;
import org.jets3t.service.multithread.ListObjectsEvent;
import org.jets3t.service.multithread.LookupACLEvent;
import org.jets3t.service.multithread.S3ServiceEventListener;
import org.jets3t.service.multithread.S3ServiceMulti;
import org.jets3t.service.multithread.ServiceEvent;
import org.jets3t.service.multithread.ThreadWatcher;
import org.jets3t.service.multithread.UpdateACLEvent;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.FileComparer;
import org.jets3t.service.utils.FileComparerResults;
import org.jets3t.service.utils.ObjectUtils;
import org.jets3t.service.utils.ServiceUtils;
import org.jets3t.service.utils.TimeFormatter;
import org.jets3t.service.utils.gatekeeper.GatekeeperMessage;
import org.jets3t.service.utils.gatekeeper.SignatureRequest;
import org.jets3t.service.utils.signedurl.GatekeeperClientUtils;
import org.jets3t.service.utils.signedurl.SignedUrlAndObject;

import contribs.com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * CockpitLite is a graphical Java application for viewing and managing the
 * contents of an Amazon S3 account, where the S3 account is not owned by the
 * application's user directly but is made available by a service provider.
 * The service provider uses the Gatekeeper application to mediate the user's
 * access to the S3 account, authorizing each of the user's interactions before
 * it can be executed.
 * <p>
 * <a href="http://www.jets3t.org/applications/cockpitlite.html">CockpitLite Guide</a>.
 * <p>
 * This is the CockpitLite application class; it may be run as a stand-alone
 * application or as an Applet.
 *
 * @author jmurty
 */
public class CockpitLite extends JApplet implements S3ServiceEventListener, ActionListener,
    ListSelectionListener, HyperlinkActivatedListener, CredentialsProvider {

    private static final long serialVersionUID = 4969295009540293079L;

    private static final Log log = LogFactory.getLog(CockpitLite.class);

    private static final String PROPERTIES_FILENAME = "cockpitlite.properties";

    public static final String APPLICATION_DESCRIPTION = "Cockpit Lite/" + Constants.JETS3T_VERSION;

    public static final String APPLICATION_TITLE = "JetS3t Cockpit Lite";

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(5, 7, 5, 7);

    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final ByteFormatter byteFormatterTerse = new ByteFormatter("G","M","K","B",1);
    private final TimeFormatter timeFormatterTerse = new TimeFormatter("h","h","m","m","s","s");
    private final SimpleDateFormat yearAndTimeSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat timeSDF = new SimpleDateFormat("HH:mm:ss");

    private final GuiUtils guiUtils = new GuiUtils();

    private Jets3tProperties cockpitLiteProperties = null;

    private static final String ACL_PRIVATE_DESCRIPTION = "Private";
    private static final String ACL_PUBLIC_DESCRIPTION = "Public";
    private static final String ACL_UNKNOWN_DESCRIPTION = "?";

    /**
     * Properties set in stand-alone application from the command line arguments.
     */
    private Properties standAloneArgumentProperties = null;

    /**
     * Stores the active ProgressPanel objects that track event progress.
     */
    private final Map progressPanelMap = new HashMap();

    private final Object lock = new Object();

    /**
     * Multi-threaded S3 service used by the application.
     */
    private S3ServiceMulti s3ServiceMulti = null;

    private GatekeeperClientUtils gkClient = null;

    private final CredentialsProvider mCredentialProvider;

    private String userBucketName = null;
    private String userVanityHost = null;
    private String userPath = "";
    private boolean userCanUpload = false;
    private boolean userCanDownload = false;
    private boolean userCanDelete = false;
    private boolean userCanACL = false;

    private boolean isRunningAsApplet = false;

    private Frame ownerFrame = null;
    private boolean isStandAloneApplication = false;

    private SkinsFactory skinsFactory = null;

    /*
     * HTTP connection settings for communication *with Gatekeeper only*, the
     * S3 connection parameters are set in the jets3t.properties file.
     */
    public static final int HTTP_CONNECTION_TIMEOUT = 60000;
    public static final int SOCKET_CONNECTION_TIMEOUT = 60000;
    public static final int MAX_CONNECTION_RETRIES = 5;

    private JPanel stackPanel = null;
    private CardLayout stackPanelCardLayout = null;

    // Object main menu items
    private JPopupMenu objectActionMenu = null;
    private JMenuItem viewObjectPropertiesMenuItem = null;
    private JMenuItem refreshObjectMenuItem = null;
    private JMenuItem togglePublicMenuItem = null;
    private JMenuItem downloadObjectMenuItem = null;
    private JMenuItem uploadFilesMenuItem = null;
    private JMenuItem generatePublicGetUrl = null;
    private JMenuItem deleteObjectMenuItem = null;

    // Login panel items
    private JPanel loginPanel = null;
    private JButton loginButton = null;

    // Objects table
    private JLabel objectsHeadingLabel = null;
    private JTable objectsTable = null;
    private JScrollPane objectsTableSP = null;
    private CLObjectTableModel objectTableModel =  null;
    private TableSorter objectTableModelSorter = null;

    // Progress notification aea
    private JPanel progressNotificationPanel = null;

    private JLabel objectsSummaryLabel = null;

    private ProgressDialog progressDialog = null;

    private UserInputFields userInputFields = null;

    // Class variables used for uploading or downloading files.
    private File downloadDirectory = null;
    private Map downloadObjectsToFileMap = null;
    private boolean isDownloadingObjects = false;
    private boolean isUploadingFiles = false;
    private Map filesAlreadyInDownloadDirectoryMap = null;
    private Map s3DownloadObjectsMap = null;
    private Map<String, String> objectKeyToFilepathMap = null;
    private Map s3ExistingObjectsMap = null;

    private File fileChoosersLastUploadDirectory = null;

    private JPanel filterObjectsPanel = null;
    private JCheckBox filterObjectsCheckBox = null;
    private JTextField filterObjectsPrefix = null;

    // File comparison options
    private static final String UPLOAD_NEW_FILES_ONLY = "Only upload new file(s)";
    private static final String UPLOAD_NEW_AND_CHANGED_FILES = "Upload new and changed file(s)";
    private static final String UPLOAD_ALL_FILES = "Upload all files";
    private static final String DOWNLOAD_NEW_FILES_ONLY = "Only download new file(s)";
    private static final String DOWNLOAD_NEW_AND_CHANGED_FILES = "Download new and changed file(s)";
    private static final String DOWNLOAD_ALL_FILES = "Download all files";

    /**
     * Flag used to indicate the "viewing objects" application state.
     */
    private boolean isViewingObjectProperties = false;

    /**
     * Constructor to run this application as an Applet.
     */
    public CockpitLite() {
        mCredentialProvider = new BasicCredentialsProvider();
        isRunningAsApplet = true;
    }

    /**
     * Constructor to run this application in a stand-alone window.
     *
     * @param ownerFrame the frame the application will be displayed in
     * @throws S3ServiceException
     */
    public CockpitLite(JFrame ownerFrame, Properties standAloneArgumentProperties) throws S3ServiceException {
        mCredentialProvider = new BasicCredentialsProvider();
        this.ownerFrame = ownerFrame;
        this.standAloneArgumentProperties = standAloneArgumentProperties;
        isStandAloneApplication = true;
        init();

        ownerFrame.getContentPane().add(this);
        ownerFrame.setBounds(this.getBounds());
        ownerFrame.setVisible(true);
    }

    /**
     * Prepares application to run as a GUI by finding/creating a root owner JFrame, creating an
     * un-authenticated {@link RestS3Service} and loading properties files.
     */
    @Override
    public void init() {
        super.init();

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

        cockpitLiteProperties = Jets3tProperties.getInstance(PROPERTIES_FILENAME);

        boolean isMissingRequiredInitProperty = false;

        if (isRunningAsApplet) {
            // Read parameters for Applet, based on names specified in the uploader properties.
            String appletParamNames = cockpitLiteProperties.getStringProperty("applet.params", "");
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
                    cockpitLiteProperties.setProperty(paramName, paramValue);
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
                        cockpitLiteProperties.setProperty(propName, propValue);
                    }
                }
            }
        }

        // Initialise the GUI.
        initGui();

        if (isMissingRequiredInitProperty) {
            String message = "Missing one or more required application properties";
            log.error(message);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, null);
            System.exit(1);
        }

        String gatekeeperUrl = cockpitLiteProperties.getStringProperty("gatekeeperUrl", null);
        if (gatekeeperUrl == null) {
            String message = "Application properties file '" + PROPERTIES_FILENAME + "' is not available";
            log.error(message);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, null);
            System.exit(1);
        }

        gkClient = new GatekeeperClientUtils(
                gatekeeperUrl,
                APPLICATION_DESCRIPTION, MAX_CONNECTION_RETRIES, HTTP_CONNECTION_TIMEOUT,
                this);

        // Initialise a non-authenticated service.
        // Revert to anonymous service.
        s3ServiceMulti = new S3ServiceMulti(
                new RestS3Service(null, APPLICATION_DESCRIPTION, this), this);
    }

    /**
     * Initialises the application's GUI elements.
     */
    private void initGui() {
        // Initialise skins factory.
        skinsFactory = SkinsFactory.getInstance(cockpitLiteProperties.getProperties());

        // Set Skinned Look and Feel.
        LookAndFeel lookAndFeel = skinsFactory.createSkinnedMetalTheme("SkinnedLookAndFeel");
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (UnsupportedLookAndFeelException e) {
            log.error("Unable to set skinned LookAndFeel", e);
        }

        // Primary panel that contains all other items.
        JPanel primaryPanel = skinsFactory.createSkinnedJPanel("PrimaryPanel");
        primaryPanel.setLayout(new GridBagLayout());
        this.getContentPane().add(primaryPanel);

        // Setup the stack panel, which contains all other panels as a stack.
        stackPanel = skinsFactory.createSkinnedJPanel("StackPanel");
        stackPanelCardLayout = new CardLayout();
        stackPanel.setLayout(stackPanelCardLayout);
        primaryPanel.add(stackPanel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));

        // Progress notification panel
        progressNotificationPanel = skinsFactory.createSkinnedJPanel("ProgressNotificationPanel");
        progressNotificationPanel.setLayout(new GridBagLayout());
        primaryPanel.add(progressNotificationPanel,
            new GridBagConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(5, 0, 5, 0), 0, 0));

        int row = 0;

        // Login panel.
        row = 0;
        loginPanel = skinsFactory.createSkinnedJPanel("LoginPanel");
        loginPanel.setLayout(new GridBagLayout());

        userInputFields = new UserInputFields(insetsDefault, null, skinsFactory);
        userInputFields.buildFieldsPanel(loginPanel, cockpitLiteProperties);

        loginButton = skinsFactory.createSkinnedJButton("LoginButton");
        loginButton.setText("Log me in");
        loginButton.addActionListener(this);
        loginPanel.add(loginButton,
            new GridBagConstraints(0, loginPanel.getComponentCount(), 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Filter panel.
        filterObjectsPanel = skinsFactory.createSkinnedJPanel("FilterPanel");
        filterObjectsPanel.setLayout(new GridBagLayout());
        filterObjectsPrefix = skinsFactory.createSkinnedJTextField("FilterPrefix");
        filterObjectsPrefix.setToolTipText("Only show files starting with this string");
        filterObjectsPrefix.addActionListener(this);
        filterObjectsPrefix.setActionCommand("RefreshObjects");
        JLabel filterPrefixLabel = skinsFactory.createSkinnedJHtmlLabel("FilterPrefixLable", this);
        filterPrefixLabel.setText("File name starts with: ");
        filterObjectsPanel.add(filterPrefixLabel,
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        filterObjectsPanel.add(filterObjectsPrefix,
            new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        filterObjectsPanel.setVisible(false);

        // Objects panel.
        row = 0;
        JPanel objectsPanel = skinsFactory.createSkinnedJPanel("ObjectsPanel");
        objectsPanel.setLayout(new GridBagLayout());
        filterObjectsCheckBox = skinsFactory.createSkinnedJCheckBox("FilterCheckbox");
        filterObjectsCheckBox.setText("Search files");
        filterObjectsCheckBox.setEnabled(true);
        filterObjectsCheckBox.addActionListener(this);
        filterObjectsCheckBox.setToolTipText("Check this option to search your files");
        objectsHeadingLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectsHeadingLabel", this);
        objectsHeadingLabel.setText("Not logged in");
        objectsPanel.add(objectsHeadingLabel,
            new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        objectsPanel.add(filterObjectsCheckBox,
            new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        JButton objectActionButton = skinsFactory.createSkinnedJButton("ObjectMenuButton");
        objectActionButton.setToolTipText("File actions menu");
        guiUtils.applyIcon(objectActionButton, "/images/nuvola/16x16/actions/misc.png");
        objectActionButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
                JButton sourceButton = (JButton) e.getSource();
                objectActionMenu.show(sourceButton, 0, sourceButton.getHeight());
           }
        });
        objectsPanel.add(objectActionButton,
            new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        objectsPanel.add(filterObjectsPanel,
            new GridBagConstraints(0, ++row, 3, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        objectsTable = skinsFactory.createSkinnedJTable("ObjectsTable");
        objectTableModel = new CLObjectTableModel();
        objectTableModelSorter = new TableSorter(objectTableModel);
        objectTableModelSorter.setTableHeader(objectsTable.getTableHeader());
        objectsTable.setModel(objectTableModelSorter);
        objectsTable.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 7229656175879985698L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String formattedSize = byteFormatter.formatByteSize(((Long)value).longValue());
                return super.getTableCellRendererComponent(table, formattedSize, isSelected, hasFocus, row, column);
            }
        });
        objectsTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = -4983176028291916397L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Date date = (Date) value;
                return super.getTableCellRendererComponent(table, yearAndTimeSDF.format(date), isSelected, hasFocus, row, column);
            }
        });
        objectsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        objectsTable.getSelectionModel().addListSelectionListener(this);
        objectsTable.setShowHorizontalLines(true);
        objectsTable.setShowVerticalLines(true);
        objectsTable.addMouseListener(new ContextMenuListener());
        objectsTableSP = skinsFactory.createSkinnedJScrollPane("ObjectsTableSP", objectsTable);
        objectsPanel.add(objectsTableSP,
                new GridBagConstraints(0, ++row, 3, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));
        objectsSummaryLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectsSummary", this);
        objectsSummaryLabel.setHorizontalAlignment(JLabel.CENTER);
        objectsSummaryLabel.setFocusable(false);
        objectsPanel.add(objectsSummaryLabel,
                new GridBagConstraints(0, ++row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Object action menu.
        objectActionMenu = skinsFactory.createSkinnedJPopupMenu("ObjectPopupMenu");

        refreshObjectMenuItem = skinsFactory.createSkinnedJMenuItem("RefreshMenuItem");
        refreshObjectMenuItem.setText("Refresh file listing");
        refreshObjectMenuItem.setActionCommand("RefreshObjects");
        refreshObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(refreshObjectMenuItem, "/images/nuvola/16x16/actions/reload.png");
        objectActionMenu.add(refreshObjectMenuItem);

        viewObjectPropertiesMenuItem = skinsFactory.createSkinnedJMenuItem("PropertiesMenuItem");
        viewObjectPropertiesMenuItem.setText("View file properties...");
        viewObjectPropertiesMenuItem.setActionCommand("ViewObjectProperties");
        viewObjectPropertiesMenuItem.addActionListener(this);
        guiUtils.applyIcon(viewObjectPropertiesMenuItem, "/images/nuvola/16x16/actions/viewmag.png");
        objectActionMenu.add(viewObjectPropertiesMenuItem);

        downloadObjectMenuItem = skinsFactory.createSkinnedJMenuItem("DownloadMenuItem");
        downloadObjectMenuItem.setText("Download file(s)...");
        downloadObjectMenuItem.setActionCommand("DownloadObjects");
        downloadObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(downloadObjectMenuItem, "/images/nuvola/16x16/actions/1downarrow.png");
        objectActionMenu.add(downloadObjectMenuItem);

        uploadFilesMenuItem = skinsFactory.createSkinnedJMenuItem("UploadMenuItem");
        uploadFilesMenuItem.setText("Upload file(s)...");
        uploadFilesMenuItem.setActionCommand("UploadFiles");
        uploadFilesMenuItem.addActionListener(this);
        guiUtils.applyIcon(uploadFilesMenuItem, "/images/nuvola/16x16/actions/1uparrow.png");
        objectActionMenu.add(uploadFilesMenuItem);

        objectActionMenu.add(new JSeparator());

        togglePublicMenuItem = skinsFactory.createSkinnedJMenuItem("AclToggleMenuItem");
        togglePublicMenuItem.setText("Change privacy setting...");
        togglePublicMenuItem.setActionCommand("TogglePublicPrivate");
        togglePublicMenuItem.addActionListener(this);
        guiUtils.applyIcon(togglePublicMenuItem, "/images/nuvola/16x16/actions/encrypted.png");
        objectActionMenu.add(togglePublicMenuItem);

        generatePublicGetUrl = skinsFactory.createSkinnedJMenuItem("PublicUrlMenuItem");
        generatePublicGetUrl.setText("Public web link...");
        generatePublicGetUrl.setActionCommand("GeneratePublicGetURL");
        generatePublicGetUrl.addActionListener(this);
        guiUtils.applyIcon(generatePublicGetUrl, "/images/nuvola/16x16/actions/wizard.png");
        objectActionMenu.add(generatePublicGetUrl);

        objectActionMenu.add(new JSeparator());

        deleteObjectMenuItem = skinsFactory.createSkinnedJMenuItem("DeleteMenuItem");
        deleteObjectMenuItem.setText("Delete file(s)...");
        deleteObjectMenuItem.setActionCommand("DeleteObjects");
        deleteObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(deleteObjectMenuItem, "/images/nuvola/16x16/actions/cancel.png");
        objectActionMenu.add(deleteObjectMenuItem);

        viewObjectPropertiesMenuItem.setEnabled(false);
        refreshObjectMenuItem.setEnabled(false);
        togglePublicMenuItem.setEnabled(false);
        downloadObjectMenuItem.setEnabled(false);
        generatePublicGetUrl.setEnabled(false);
        deleteObjectMenuItem.setEnabled(false);

        // Card layout in stack panel
        stackPanel.add(loginPanel, "LoginPanel");
        stackPanel.add(objectsPanel, "ObjectsPanel");

        // Set preferred sizes
        int preferredWidth = 800;
        int preferredHeight = 600;
        this.setBounds(new Rectangle(new Dimension(preferredWidth, preferredHeight)));

        // Initialize drop target.
        initDropTarget(new JComponent[] {objectsPanel} );
        objectsPanel.getDropTarget().setActive(true);
    }

    /**
     * Initialise the application's File drop targets for drag and drop copying of local files
     * to S3.
     *
     * @param dropTargetComponents
     * the components files can be dropped on to transfer them to S3
     */
    private void initDropTarget(JComponent[] dropTargetComponents) {
        DropTargetListener dropTargetListener = new DropTargetListener() {

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
                            objectsTable.requestFocusInWindow();
                        };
                    });
                }
            }
            public void dragOver(DropTargetDragEvent dtde) {
                checkValidDrag(dtde);
            }
            public void dropActionChanged(DropTargetDragEvent dtde) {
                if (checkValidDrag(dtde)) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            objectsTable.requestFocusInWindow();
                        };
                    });
                } else {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ownerFrame.requestFocusInWindow();
                        };
                    });
                }
            }
            public void dragExit(DropTargetEvent dte) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        ownerFrame.requestFocusInWindow();
                    };
                });
            }

            public void drop(DropTargetDropEvent dtde) {
                if (dtde.isDataFlavorSupported(DataFlavor.javaFileListFlavor)
                    && (DnDConstants.ACTION_COPY == dtde.getDropAction()
                        || DnDConstants.ACTION_MOVE == dtde.getDropAction()))
                {
                    dtde.acceptDrop(dtde.getDropAction());
                    try {
                        final List fileList = (List) dtde.getTransferable().getTransferData(
                            DataFlavor.javaFileListFlavor);
                        if (fileList != null && fileList.size() > 0) {
                            new Thread() {
                                @Override
                                public void run() {
                                    prepareForFilesUpload((File[]) fileList.toArray(new File[fileList.size()]));
                                }
                            }.start();
                        }
                    } catch (Exception e) {
                        String message = "Unable to start accept dropped item(s)";
                        log.error(message, e);
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(), message, e);
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
     * Starts a progress display dialog. While the dialog is running the user cannot interact
     * with the application, except to cancel the task.
     *
     * @param statusMessage
     *        describes the status of a task text meaningful to the user, such as "3 files of 7 uploaded"
     * @param detailsText
     *        describes the status of a task in more detail, such as the current transfer rate and Time remaining.
     * @param minTaskValue  the minimum progress value for a task, generally 0
     * @param maxTaskValue
     *        the maximum progress value for a task, such as the total number of threads or 100 if
     *        using percentage-complete as a metric.
     * @param cancelEventListener
     *        listener that is responsible for cancelling a long-lived task when the user clicks
     *        the cancel button. If a task cannot be cancelled this must be null.
     * @param cancelButtonText
     *        text displayed in the cancel button if a task can be cancelled. This is only used if
     *        a cancel event listener is provided.
     */
    private void startProgressDialog(final String statusMessage, final String detailsText,
        final int minTaskValue, final int maxTaskValue, final String cancelButtonText,
        final CancelEventTrigger cancelEventListener)
    {
        if (this.progressDialog == null) {
            this.progressDialog = new ProgressDialog(
                this.ownerFrame, "Please wait...", cockpitLiteProperties.getProperties());
        }

        this.getContentPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressDialog.startDialog(statusMessage, detailsText, minTaskValue, maxTaskValue,
                    cancelEventListener, cancelButtonText);
            }
         });
    }

    /**
     * Updates the status text and value of the progress display dialog.
     * @param statusMessage
     *        describes the status of a task text meaningful to the user, such as "3 files of 7 uploaded"
     * @param detailsText
     *        describes the status of a task in more detail, such as the current transfer rate and time remaining.
     * @param progressValue
     *        value representing how far through the task we are (relative to min and max values)
     */
    private void updateProgressDialog(final String statusMessage, final String detailsText, final int progressValue) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressDialog.updateDialog(statusMessage, detailsText, progressValue);
            }
         });
    }

    /**
     * Stops/halts the progress display dialog and allows the user to interact with the application.
     */
    private void stopProgressDialog() {
        this.getContentPane().setCursor(null);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressDialog.stopDialog();
            }
         });
    }


    protected void startProgressPanel(Object operationId, String statusMessage,
        int maxCount, CancelEventTrigger cancelEventTrigger)
    {
        // Create new progress panel.
        final ProgressPanel progressPanel = new ProgressPanel(
            cockpitLiteProperties.getProperties(), cancelEventTrigger);
        progressPanel.startProgress(statusMessage, 0, maxCount);

        // Store this panel against the operation ID it tracks.
        progressPanelMap.put(operationId, progressPanel);

        // Display panel in progress notification area.
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                progressNotificationPanel.add(progressPanel,
                    new GridBagConstraints(0, progressNotificationPanel.getComponents().length,
                        1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
                progressNotificationPanel.revalidate();
            }
         });
    }

    protected void updateProgressPanel(Object operationId, final String statusMessage,
        final int currentCount)
    {
        // Retrieve progress panel.
        final ProgressPanel progressPanel = (ProgressPanel) progressPanelMap.get(operationId);

        if (progressPanel != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressPanel.updateProgress(statusMessage, currentCount);
                }
             });
        }
    }

    protected void stopProgressPanel(Object operationId) {
        // Retrieve progress panel.
        final ProgressPanel progressPanel = (ProgressPanel) progressPanelMap.get(operationId);

        if (progressPanel != null) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressNotificationPanel.remove(progressPanel);
                    progressNotificationPanel.revalidate();

                    progressPanelMap.remove(progressPanel);
                    progressPanel.dispose();
                }
             });
        }
    }


    /**
     * Event handler for this application, handles all menu items.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getSource().equals(loginButton)) {
            new Thread() {
                @Override
                public void run() {
                    listObjects();
                }
            }.start();
        }

        // Object Events
        else if ("ViewObjectProperties".equals(event.getActionCommand())) {
            listObjectProperties();
        } else if ("RefreshObjects".equals(event.getActionCommand())) {
            new Thread() {
                @Override
                public void run() {
                    listObjects();
                }
            }.start();
        } else if ("TogglePublicPrivate".equals(event.getActionCommand())) {
            new Thread() {
                @Override
                public void run() {
                    S3Object object = getSelectedObjects()[0];
                    String aclStatus = objectTableModel.getObjectAclStatus(object);
                    boolean originalAclWasPublic = ACL_PUBLIC_DESCRIPTION.equals(aclStatus);

                    ToggleAclDialog dialog = new ToggleAclDialog(ownerFrame,
                        originalAclWasPublic, null, cockpitLiteProperties.getProperties());
                    dialog.setVisible(true);

                    // Update ACL setting.
                    S3Object minimalObject = new S3Object(object.getKey());

                    AccessControlList newAcl = (dialog.isPublicAclSet()
                        ? AccessControlList.REST_CANNED_PUBLIC_READ
                        : AccessControlList.REST_CANNED_PRIVATE);

                    if (newAcl != null) {
                        if (AccessControlList.REST_CANNED_PRIVATE.equals(newAcl)) {
                            minimalObject.addMetadata(Constants.REST_HEADER_PREFIX + "acl", "private");
                        } else if (AccessControlList.REST_CANNED_PUBLIC_READ.equals(newAcl)) {
                            minimalObject.addMetadata(Constants.REST_HEADER_PREFIX + "acl", "public-read");
                        }
                    }

                    updateObjectsAccessControlLists(
                        new S3Object[] {minimalObject}, newAcl);

                    dialog.dispose();
                }
            }.start();
        } else if ("GeneratePublicGetURL".equals(event.getActionCommand())) {
            generatePublicGetUrl();
        } else if ("DeleteObjects".equals(event.getActionCommand())) {
            deleteSelectedObjects();
        } else if ("DownloadObjects".equals(event.getActionCommand())) {
            try {
                downloadSelectedObjects();
            } catch (Exception ex) {
                String message = "Unable to download objects from S3";
                log.error(message, ex);
                ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, ex);
            }
        } else if ("UploadFiles".equals(event.getActionCommand())) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setDialogTitle("Choose file(s) to upload");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fileChooser.setApproveButtonText("Upload files");
            fileChooser.setCurrentDirectory(fileChoosersLastUploadDirectory);

            int returnVal = fileChooser.showOpenDialog(ownerFrame);
            if (returnVal != JFileChooser.APPROVE_OPTION) {
                    return;
            }

            final File[] uploadFiles = fileChooser.getSelectedFiles();
            if (uploadFiles.length == 0) {
                return;
            }

            // Save the chosen directory location for next time.
            fileChoosersLastUploadDirectory = uploadFiles[0].getParentFile();

            new Thread() {
                @Override
                public void run() {
                    prepareForFilesUpload(uploadFiles);
                }
            }.start();
        } else if (event.getSource().equals(filterObjectsCheckBox)) {
            if (filterObjectsCheckBox.isSelected()) {
                filterObjectsPanel.setVisible(true);
            } else {
                filterObjectsPanel.setVisible(false);
                filterObjectsPrefix.setText("");
            }
        }

        // Ooops...
        else {
            log.warn("Unrecognised ActionEvent command '" + event.getActionCommand() + "' in " + event);
        }
    }

    /**
     * Handles list selection events for this application.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (e.getSource().equals(objectsTable.getSelectionModel())) {
            objectSelectedAction();
        }
    }

    private void listObjects() {
        try {
            // Obtain login details from application's login screen and store them in
            // the application properties so the details will be forwarded to the Gatekeeper
            // with each request.
            Properties loginProperties = userInputFields.getUserInputsAsProperties(true);
            Iterator iter = loginProperties.keySet().iterator();
            while (iter.hasNext()) {
                String propertyName = (String) iter.next();
                String propertyValue = loginProperties.getProperty(propertyName);
                cockpitLiteProperties.setProperty(propertyName, propertyValue);
            }

            startProgressPanel(this, "Finding files", 0, null);

            // Perform object listing operation via Gatekeeper.
            Map requestProperties = new HashMap();
            requestProperties.put(GatekeeperMessage.LIST_OBJECTS_IN_BUCKET_FLAG, "");
            requestProperties.putAll(cockpitLiteProperties.getProperties());
            if (filterObjectsCheckBox.isSelected() && filterObjectsPrefix.getText().length() > 0) {
                requestProperties.put("Prefix", filterObjectsPrefix.getText());
            }

            GatekeeperMessage responseMessage =
                gkClient.requestActionThroughGatekeeper(
                        null, null, new S3Object[] {}, requestProperties);

            stopProgressPanel(this);

            String gatekeeperErrorCode = responseMessage.getApplicationProperties()
                .getProperty(GatekeeperMessage.APP_PROPERTY_GATEKEEPER_ERROR_CODE);

            if (gatekeeperErrorCode == null) {
                // Listing succeeded
                final S3Object[] objects = gkClient.buildS3ObjectsFromSignatureRequests(
                    responseMessage.getSignatureRequests());

                // User account description provided by Gatekeeper
                final String accountDescription =
                    responseMessage.getApplicationProperties().getProperty("AccountDescription");

                // User's settings
                userCanUpload = "true".equalsIgnoreCase(
                    responseMessage.getApplicationProperties().getProperty("UserCanUpload"));
                userCanDownload = "true".equalsIgnoreCase(
                    responseMessage.getApplicationProperties().getProperty("UserCanDownload"));
                userCanDelete = "true".equalsIgnoreCase(
                    responseMessage.getApplicationProperties().getProperty("UserCanDelete"));
                userCanACL = "true".equalsIgnoreCase(
                    responseMessage.getApplicationProperties().getProperty("UserCanACL"));

                userBucketName = responseMessage.getApplicationProperties().getProperty("S3BucketName");
                userPath = responseMessage.getApplicationProperties().getProperty("UserPath", "");
                userVanityHost = responseMessage.getApplicationProperties().getProperty("UserVanityHost");

                objectTableModel.setUsersPath(userPath);
                uploadFilesMenuItem.setEnabled(userCanUpload);

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        objectsHeadingLabel.setText(
                            (accountDescription != null ? accountDescription : "Logged in"));

                        objectTableModel.removeAllObjects();
                        objectTableModel.addObjects(objects);
                        updateObjectsSummary();
                        refreshObjectMenuItem.setEnabled(true);

                        lookupObjectsAccessControlLists(objects);
                    }
                });

                stackPanelCardLayout.show(stackPanel, "ObjectsPanel");
            } else {
                // Listing failed
                ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(),
                    "Your log-in information was not correct, please try again", null);
            }
        } catch (Exception e) {
            stopProgressPanel(this);
            log.error("Gatekeeper login failed for URL: " + gkClient.getGatekeeperUrl(), e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(),
                    "Log-in failed, please try again", e);
        }
    }

    /**
     * Displays the currently selected object's properties in the dialog {@link ItemPropertiesDialog}.
     * <p>
     * As detailed information about the object may not yet be available, this method works
     * indirectly via the {@link #retrieveObjectsDetails} method. The <code>retrieveObjectsDetails</code>
     * method retrieves all the details for the currently selected objects, and once they are available
     * knows to display the <code>PropertiesDialog</code> as the {@link #isViewingObjectProperties} flag
     * is set.
     */
    private void listObjectProperties() {
        isViewingObjectProperties = true;
        retrieveObjectsDetails(getSelectedObjects());
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>GetObjectsEvent</code>.
     * <p>
     * This never happens in this application as downloads are performed by
     * {@link S3ServiceMulti#downloadObjects(S3Bucket, DownloadPackage[])} instead.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(GetObjectsEvent event) {
        // Not used.
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>ListObjectsEvent</code>.
     * <p>
     * This never happens in this application as it does not perform multi-threaded object
     * listings.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(ListObjectsEvent event) {
        // Not used.
    }

    public void s3ServiceEventPerformed(DeleteVersionedObjectsEvent event) {
        // Not used.
    }

    /**
     * Actions performed when an object is selected in the objects list table.
     */
    private void objectSelectedAction() {
        S3Object[] selectedObjects = getSelectedObjects();
        int count = selectedObjects.length;

        togglePublicMenuItem.setEnabled(
            userCanACL && count == 1);
        downloadObjectMenuItem.setEnabled(
            userCanDownload && count > 0);
        deleteObjectMenuItem.setEnabled(
            userCanDelete && count > 0);
        viewObjectPropertiesMenuItem.setEnabled(count > 0);
        generatePublicGetUrl.setEnabled(
            count == 1
            && ACL_PUBLIC_DESCRIPTION.equals(
                objectTableModel.getObjectAclStatus(selectedObjects[0])));
    }

    /**
     * Updates the summary text shown below the listing of objects, which details the
     * number and total size of the objects.
     *
     */
    private void updateObjectsSummary() {
        S3Object[] objects = objectTableModel.getObjects();

        try {
            String summary = "Please select a bucket";
            long totalBytes = 0;
            if (objects != null) {
                summary = "<html>" + objects.length + " item" + (objects.length != 1? "s" : "");

                for (int i = 0; i < objects.length; i++) {
                    totalBytes += objects[i].getContentLength();
                }
                if (totalBytes > 0) {
                    summary += ", " + byteFormatter.formatByteSize(totalBytes);
                }
                summary += " @ " + timeSDF.format(new Date());

                if (isObjectFilteringActive()) {
                    summary += " - <i>Search results</i>";
                }
                summary += "</html>";
            }

            objectsSummaryLabel.setText(summary);
        } catch (Throwable t) {
            String message = "Unable to update object list summary";
            log.error(message, t);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, t);
        }
    }

    /**
     * Displays object-specific actions in a popup menu.
     * @param invoker the component near which the popup menu will be displayed
     * @param xPos the mouse's horizontal co-ordinate when the popup menu was invoked
     * @param yPos the mouse's vertical co-ordinate when the popup menu was invoked
     */
    private void showObjectPopupMenu(JComponent invoker, int xPos, int yPos) {
        if (getSelectedObjects().length == 0) {
            return;
        }
        objectActionMenu.show(invoker, xPos, yPos);
    }

    /**
     * @return the set of objects currently selected in the gui, or an empty array if none are selected.
     */
    private S3Object[] getSelectedObjects() {
        int viewRows[] = objectsTable.getSelectedRows();
        if (viewRows.length == 0) {
            return new S3Object[] {};
        } else {
            S3Object objects[] = new S3Object[viewRows.length];
            for (int i = 0; i < viewRows.length; i++) {
                int modelRow = objectTableModelSorter.modelIndex(viewRows[i]);
                objects[i] = objectTableModel.getObject(modelRow);
            }
            return objects;
        }
    }

    /**
     * Retrieves ACL settings for the currently selected objects. The actual action is performed
     * in the <code>s3ServiceEventPerformed</code> method specific to <code>LookupACLEvent</code>s.
     *
     */
    private void lookupObjectsAccessControlLists(final S3Object[] objects) {
        (new Thread() {
            @Override
            public void run() {
                try {
                    SignatureRequest[] signatureRequests = requestSignedRequests(
                            SignatureRequest.SIGNATURE_TYPE_ACL_LOOKUP, objects);

                    if (signatureRequests != null) {
                        String[] signedRequests = new String[signatureRequests.length];
                        for (int i = 0; i < signedRequests.length; i++) {
                            signedRequests[i] = signatureRequests[i].getSignedUrl();
                        }

                        s3ServiceMulti.getObjectsACLs(signedRequests);
                    } else {
                        // Signature request failed
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Sorry, you do not have the permission to view object privacy settings", null);
                    }
                } catch (Exception e) {
                    log.error("Gatekeeper permissions check failed", e);
                    ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Permissions check failed, please try again", e);
                }
            }
        }).start();
    }


    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>LookupACLEvent</code>.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final LookupACLEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            int threadCount = (int) event.getThreadWatcher().getThreadCount();
            startProgressPanel(event.getUniqueOperationId(), "Privacy lookup 0/" + threadCount,
                threadCount, event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            int threadCount = (int) event.getThreadWatcher().getThreadCount();
            int threadsCompleted = (int) event.getThreadWatcher().getCompletedThreads();
            updateProgressPanel(event.getUniqueOperationId(),
                "Privacy lookup " + threadsCompleted + "/" + threadCount, threadsCompleted);

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    S3Object[] objectsWithAcl = event.getObjectsWithACL();
                    for (int i = 0; i < objectsWithAcl.length; i++) {
                        String aclStatus = getAclDescription(objectsWithAcl[i].getAcl());
                        objectTableModel.updateObjectAclStatus(objectsWithAcl[i], aclStatus);
                    }
                }
            });

        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());

            String message = "Unable to lookup Access Control list for object(s)";
            log.error(message, event.getErrorCause());
        }
    }

    /**
     * Updates ACL settings for the currently selected objects. The actual action is performed
     * in the <code>s3ServiceEventPerformed</code> method specific to <code>UpdateACLEvent</code>s.
     *
     */
    private void updateObjectsAccessControlLists(final S3Object[] objectsToUpdate, final AccessControlList acl) {
        (new Thread() {
            @Override
            public void run() {
                try {
                    SignatureRequest[] signatureRequests = requestSignedRequests(
                            SignatureRequest.SIGNATURE_TYPE_ACL_UPDATE, objectsToUpdate);
                    if (signatureRequests != null) {
                        String[] signedRequests = new String[signatureRequests.length];
                        for (int i = 0; i < signedRequests.length; i++) {
                            signedRequests[i] = signatureRequests[i].getSignedUrl();
                        }

                        s3ServiceMulti.putObjectsACLs(signedRequests, acl);
                    } else {
                        // Listing failed
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Sorry, you do not have the permission to change object privacy settings", null);
                    }
                } catch (Exception e) {
                    log.error("Gatekeeper permissions check failed", e);
                    ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Permissions check failed, please try again", e);
                }
            }
        }).start();
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>UpdateACLEvent</code>.
     * <p>
     * This method merely updates the progress dialog as ACLs are updated.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final UpdateACLEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            startProgressPanel(event.getUniqueOperationId(),
                "Privacy update 0/" + event.getThreadWatcher().getThreadCount(),
                (int) event.getThreadWatcher().getThreadCount(),
                event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Privacy update " + progressStatus.getCompletedThreads()
                + "/" + progressStatus.getThreadCount();
            updateProgressPanel(event.getUniqueOperationId(), statusText,
                (int) progressStatus.getCompletedThreads());

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    S3Object[] objects = event.getObjectsWithUpdatedACL();
                    for (int i = 0; i < objects.length; i++) {
                        String aclStatus = getAclDescription(objects[i].getAcl());
                        objectTableModel.updateObjectAclStatus(objects[i], aclStatus);
                        objectSelectedAction();
                    }
                }
            });

        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());

            String message = "Unable to update Access Control List(s)";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, event.getErrorCause());
        }
    }

    /**
     * Prepares to perform a download of objects from S3 by prompting the user for a directory
     * to store the files in, then performing the download.
     *
     * @throws IOException
     */
    private void downloadSelectedObjects() throws IOException {
        // Prompt user to choose directory location for downloaded files (or cancel download altogether)
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose directory to save S3 files in");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setSelectedFile(downloadDirectory);

        int returnVal = fileChooser.showDialog(ownerFrame, "Choose Directory");
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }

        downloadDirectory = fileChooser.getSelectedFile();

        prepareForObjectsDownload();
    }

    private void prepareForObjectsDownload() {
        // Build map of existing local files.
        Map<String, String> objectKeyToFilepathMap = null;
        try {
            boolean storeEmptyDirectories = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                .getBoolProperty("uploads.storeEmptyDirectories", true);
            objectKeyToFilepathMap = FileComparer.getInstance()
                .buildObjectKeyToFilepathMap(downloadDirectory.listFiles(), "", storeEmptyDirectories);
        } catch (Exception e) {
            String message = "Unable to review files in targetted download directory";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);

            return;
        }

        filesAlreadyInDownloadDirectoryMap = new HashMap();

        // Build map of S3 Objects being downloaded.
        s3DownloadObjectsMap = FileComparer.getInstance()
            .populateObjectMap("", getSelectedObjects());

        // Identify objects that may clash with existing files, or may be directories,
        // and retrieve details for these.
        ArrayList potentialClashingObjects = new ArrayList();
        Set existingFilesObjectKeys = objectKeyToFilepathMap.keySet();
        Iterator objectsIter = s3DownloadObjectsMap.entrySet().iterator();
        while (objectsIter.hasNext()) {
            Map.Entry entry = (Map.Entry) objectsIter.next();
            String objectKey = (String) entry.getKey();
            S3Object object = (S3Object) entry.getValue();

            if (object.getContentLength() == 0 || existingFilesObjectKeys.contains(objectKey)) {
                potentialClashingObjects.add(object);
            }
            if (existingFilesObjectKeys.contains(objectKey)) {
                filesAlreadyInDownloadDirectoryMap.put(
                    objectKey, objectKeyToFilepathMap.get(objectKey));
            }
        }

        if (potentialClashingObjects.size() > 0) {
            // Retrieve details of potential clashes.
            final S3Object[] clashingObjects = (S3Object[])
                potentialClashingObjects.toArray(new S3Object[potentialClashingObjects.size()]);
            (new Thread() {
                @Override
                public void run() {
                    isDownloadingObjects = true;
                    retrieveObjectsDetails(clashingObjects);
                }
            }).start();
        } else {
            compareRemoteAndLocalFiles(filesAlreadyInDownloadDirectoryMap, s3DownloadObjectsMap, false);
        }
    }

    private void prepareForFilesUpload(File[] uploadFiles) {
        try {
            // Build map of files proposed for upload.
            boolean storeEmptyDirectories = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                .getBoolProperty("uploads.storeEmptyDirectories", true);
            objectKeyToFilepathMap = FileComparer.getInstance()
                .buildObjectKeyToFilepathMap(uploadFiles, "", storeEmptyDirectories);

            // Build map of objects already existing in target S3 bucket with keys
            // matching the proposed upload keys.
            List objectsWithExistingKeys = new ArrayList();
            S3Object[] existingObjects = objectTableModel.getObjects();
            for (int i = 0; i < existingObjects.length; i++) {
                if (objectKeyToFilepathMap.containsKey(existingObjects[i].getKey()))
                {
                    objectsWithExistingKeys.add(existingObjects[i]);
                }
            }
            existingObjects = (S3Object[]) objectsWithExistingKeys
                .toArray(new S3Object[objectsWithExistingKeys.size()]);

            s3ExistingObjectsMap = FileComparer.getInstance()
                .populateObjectMap("", existingObjects);

            if (existingObjects.length > 0) {
                // Retrieve details of potential clashes.
                final S3Object[] clashingObjects = existingObjects;
                (new Thread() {
                    @Override
                    public void run() {
                        isUploadingFiles = true;
                        retrieveObjectsDetails(clashingObjects);
                    }
                }).start();
            } else {
                compareRemoteAndLocalFiles(objectKeyToFilepathMap, s3ExistingObjectsMap, true);
            }
        } catch (Exception e) {
            String message = "Unable to upload objects";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);
        }
    }

    private void compareRemoteAndLocalFiles(
        final Map<String, String> objectKeyToFilepathMap, final Map s3ObjectsMap, final boolean upload)
    {
        final HyperlinkActivatedListener hyperlinkListener = this;
        (new Thread(new Runnable() {
            public void run() {
                try {
                    // Compare objects being downloaded and existing local files.
                    final String statusText =
                        "Comparing " + s3ObjectsMap.size() + " object" + (s3ObjectsMap.size() > 1 ? "s" : "") +
                        " in S3 with " + objectKeyToFilepathMap.size()
                        + " local file" + (objectKeyToFilepathMap.size() > 1 ? "s" : "");
                    startProgressDialog(statusText, "", 0, 100, null, null);

                    // Calculate total files size.
                    File[] files = objectKeyToFilepathMap.values().toArray(new File[objectKeyToFilepathMap.size()]);
                    final long filesSizeTotal[] = new long[1];
                    for (int i = 0; i < files.length; i++) {
                        filesSizeTotal[0] += files[i].length();
                    }

                    // Monitor generation of MD5 hash, and provide feedback via the progress bar.
                    BytesProgressWatcher progressWatcher = new BytesProgressWatcher(filesSizeTotal[0]) {
                        @Override
                        public void updateBytesTransferred(long byteCount) {
                            super.updateBytesTransferred(byteCount);

                            String detailsText = formatBytesProgressWatcherDetails(this, true);
                            int progressValue = (int)((double)getBytesTransferred() * 100 / getBytesToTransfer());
                            updateProgressDialog(statusText, detailsText, progressValue);
                        }
                    };

                    FileComparerResults comparisonResults = FileComparer.getInstance()
                        .buildDiscrepancyLists(objectKeyToFilepathMap, s3ObjectsMap, progressWatcher);

                    stopProgressDialog();

                    if (upload) {
                        performFilesUpload(comparisonResults, objectKeyToFilepathMap);
                    } else {
                        performObjectsDownload(comparisonResults, s3ObjectsMap);
                    }
                } catch (RuntimeException e) {
                    stopProgressDialog();
                    throw e;
                } catch (Exception e) {
                    stopProgressDialog();
                    String message = "Unable to " + (upload? "upload" : "download") + " objects";
                    log.error(message, e);
                    ErrorDialog.showDialog(ownerFrame, hyperlinkListener, cockpitLiteProperties.getProperties(), message, e);
                }
            }
        })).start();
    }

    /**
     * Retrieves details about objects including metadata etc by invoking the method
     * {@link S3ServiceMulti#getObjectsHeads}.
     *
     * This is generally done as a prelude
     * to some further action, such as displaying the objects' details or downloading the objects.
     * The real action occurs in the method <code>s3ServiceEventPerformed</code> for handling
     * <code>GetObjectHeadsEvent</code> events.
     * @param candidateObjects
     */
    private void retrieveObjectsDetails(final S3Object[] candidateObjects) {
        // Identify which of the candidate objects have incomplete metadata.
        ArrayList s3ObjectsIncompleteList = new ArrayList();
        for (int i = 0; i < candidateObjects.length; i++) {
            if (!candidateObjects[i].isMetadataComplete()) {
                s3ObjectsIncompleteList.add(candidateObjects[i]);
            }
        }

        log.debug("Of " + candidateObjects.length + " object candidates for HEAD requests "
            + s3ObjectsIncompleteList.size() + " are incomplete, performing requests for these only");

        final S3Object[] incompleteObjects = (S3Object[]) s3ObjectsIncompleteList
            .toArray(new S3Object[s3ObjectsIncompleteList.size()]);
        (new Thread() {
            @Override
            public void run() {
                try {
                    SignatureRequest[] signatureRequests = requestSignedRequests(
                            SignatureRequest.SIGNATURE_TYPE_HEAD, incompleteObjects);

                    if (signatureRequests != null) {
                        String[] signedRequests = new String[signatureRequests.length];
                        for (int i = 0; i < signedRequests.length; i++) {
                            signedRequests[i] = signatureRequests[i].getSignedUrl();
                        }

                        s3ServiceMulti.getObjectsHeads(signedRequests);
                    } else {
                        // Listing failed
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Sorry, you do not have the permission to view object details", null);
                    }
                } catch (Exception e) {
                    stopProgressDialog();
                    log.error("Gatekeeper permissions check failed", e);
                    ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Permissions check failed, please try again", e);
                }
            };
        }).start();
    }

    /**
     * Performs the real work of downloading files by comparing the download candidates against
     * existing files, prompting the user whether to overwrite any pre-existing file versions,
     * and starting {@link S3ServiceMulti#downloadObjects} where the real work is done.
     *
     */
    private void performObjectsDownload(FileComparerResults comparisonResults, Map s3DownloadObjectsMap) {
        try {

            // Determine which files to download, prompting user whether to over-write existing files
            List objectKeysForDownload = new ArrayList();
            objectKeysForDownload.addAll(comparisonResults.onlyOnServerKeys);

            int newFiles = comparisonResults.onlyOnServerKeys.size();
            int unchangedFiles = comparisonResults.alreadySynchronisedKeys.size();
            int changedFiles = comparisonResults.updatedOnClientKeys.size()
                + comparisonResults.updatedOnServerKeys.size();

            if (unchangedFiles > 0 || changedFiles > 0) {
                // Ask user whether to replace existing unchanged and/or existing changed files.
                log.debug("Files for download clash with existing local files, prompting user to choose which files to replace");
                List options = new ArrayList();
                String message = "Of the " + (newFiles + unchangedFiles + changedFiles)
                    + " object(s) being downloaded:\n\n";

                if (newFiles > 0) {
                    message += newFiles + " file(s) are new.\n\n";
                    options.add(DOWNLOAD_NEW_FILES_ONLY);
                }
                if (changedFiles > 0) {
                    message += changedFiles + " file(s) have changed.\n\n";
                    options.add(DOWNLOAD_NEW_AND_CHANGED_FILES);
                }
                if (unchangedFiles > 0) {
                    message += unchangedFiles + " file(s) already exist and are unchanged.\n\n";
                    options.add(DOWNLOAD_ALL_FILES);
                }
                message += "Please choose which file(s) you wish to download:";

                Object response = JOptionPane.showInputDialog(
                    ownerFrame, message, "Replace file(s)?", JOptionPane.QUESTION_MESSAGE,
                    null, options.toArray(), DOWNLOAD_NEW_AND_CHANGED_FILES);

                if (response == null) {
                    return;
                }

                if (DOWNLOAD_NEW_FILES_ONLY.equals(response)) {
                    // No change required to default objectKeysForDownload list.
                } else if (DOWNLOAD_ALL_FILES.equals(response)) {
                    objectKeysForDownload.addAll(comparisonResults.updatedOnClientKeys);
                    objectKeysForDownload.addAll(comparisonResults.updatedOnServerKeys);
                    objectKeysForDownload.addAll(comparisonResults.alreadySynchronisedKeys);
                } else if (DOWNLOAD_NEW_AND_CHANGED_FILES.equals(response)) {
                    objectKeysForDownload.addAll(comparisonResults.updatedOnClientKeys);
                    objectKeysForDownload.addAll(comparisonResults.updatedOnServerKeys);
                } else {
                    // Download cancelled.
                    return;
                }
            }

            log.debug("Downloading " + objectKeysForDownload.size() + " objects");
            if (objectKeysForDownload.size() == 0) {
                return;
            }

            // Create array of objects for download.
            final S3Object[] objects = new S3Object[objectKeysForDownload.size()];
            int objectIndex = 0;
            for (Iterator iter = objectKeysForDownload.iterator(); iter.hasNext();) {
                objects[objectIndex++] = (S3Object) s3DownloadObjectsMap.get(iter.next());
            }

            (new Thread() {
                @Override
                public void run() {
                    try {
                        SignatureRequest[] signedRequests = requestSignedRequests(
                            SignatureRequest.SIGNATURE_TYPE_GET, objects);

                        if (signedRequests != null) {
                            // Setup files to write to, creating parent directories when necessary.
                            downloadObjectsToFileMap = new HashMap();
                            ArrayList downloadPackageList = new ArrayList();
                            for (int i = 0; i < signedRequests.length; i++) {
                                S3Object object = signedRequests[i].buildObject();

                                File file = new File(downloadDirectory, object.getKey());

                                // Create local directories corresponding to objects flagged as dirs.
                                if (object.isDirectoryPlaceholder()) {
                                    file = new File(downloadDirectory,
                                        ObjectUtils.convertDirPlaceholderKeyNameToDirName(
                                            objects[i].getKey()));
                                    file.mkdirs();
                                }

                                DownloadPackage downloadPackage = ObjectUtils.createPackageForDownload(
                                        object, file, true, false, null);
                                if (downloadPackage == null) {
                                    continue;
                                }
                                downloadPackage.setSignedUrl(signedRequests[i].getSignedUrl());

                                downloadObjectsToFileMap.put(object.getKey(), file);
                                downloadPackageList.add(downloadPackage);
                            }
                            DownloadPackage[] downloadPackagesArray = (DownloadPackage[])
                                downloadPackageList.toArray(new DownloadPackage[downloadPackageList.size()]);

                            // Perform downloads.
                            s3ServiceMulti.downloadObjects(downloadPackagesArray);
                        }
                    } catch (Exception e) {
                        log.error("Download failed", e);
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                                "Download failed, please try again", e);
                    }
                }
            }).start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unable to download objects";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);
        }
    }

    private SignatureRequest[] requestSignedRequests(String operationType, S3Object[] objects) {
        try {
            startProgressPanel(this, "Checking permissions", 0, null);

            GatekeeperMessage responseMessage = gkClient.requestActionThroughGatekeeper(
                operationType, userBucketName, objects, cockpitLiteProperties.getProperties());

            stopProgressPanel(this);

            String gatekeeperErrorCode = responseMessage.getApplicationProperties()
                .getProperty(GatekeeperMessage.APP_PROPERTY_GATEKEEPER_ERROR_CODE);

            if (gatekeeperErrorCode == null) {
                // Confirm that all the signatures requested were approved
                for (int i = 0; i < responseMessage.getSignatureRequests().length; i++) {
                    if (responseMessage.getSignatureRequests()[i].getSignedUrl() == null) {
                        // Some permissions missing.
                        return null;
                    }
                }

                return responseMessage.getSignatureRequests();
            } else {
                // No permissions
                return null;
//                ErrorDialog.showDialog(ownerFrame, null, appProperties.getProperties(),
//                    "Sorry, you do not have the necessary permissions", null);
            }
        } catch (Exception e) {
            stopProgressPanel(this);
            log.error("Gatekeeper permissions check failed", e);
            ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                    "Permissions check failed, please try again", e);
        }
        return null;
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>DownloadObjectsEvent</code>.
     * <p>
     * This method merely updates the progress dialog as objects  are downloaded.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(DownloadObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                startProgressPanel(event.getUniqueOperationId(),
                    "Download " +
                    byteFormatterTerse.formatByteSize(watcher.getBytesTransferred())
                    + "/" + byteFormatterTerse.formatByteSize(watcher.getBytesTotal()),
                    100, event.getThreadWatcher().getCancelEventListener());
            // ... otherwise just show the number of completed threads.
            } else {
                startProgressPanel(event.getUniqueOperationId(),
                    "Download " + event.getThreadWatcher().getCompletedThreads()
                    + "/" + event.getThreadWatcher().getThreadCount(),
                    (int) event.getThreadWatcher().getThreadCount(),
                    event.getThreadWatcher().getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                int percentage = (int)
                    (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);
                updateProgressPanel(event.getUniqueOperationId(),
                    "Download " +
                    byteFormatterTerse.formatByteSize(watcher.getBytesTransferred())
                    + "/" + byteFormatterTerse.formatByteSize(watcher.getBytesTotal())
                    + " (" +
                    byteFormatterTerse.formatByteSize(watcher.getBytesPerSecond()) + "/s, "
                    + timeFormatterTerse.formatTime(watcher.getTimeRemaining())
                    + ")",
                    percentage);
            }
            // ... otherwise just show the number of completed threads.
            else {
                ThreadWatcher progressStatus = event.getThreadWatcher();
                String statusText = "Download " + progressStatus.getCompletedThreads()
                    + " of " + progressStatus.getThreadCount() + " objects";
                updateProgressPanel(event.getUniqueOperationId(), statusText, (int) progressStatus.getCompletedThreads());
            }
        } else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());

            String message = "Unable to download object(s)";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, event.getErrorCause());
        }
    }


    private void performFilesUpload(FileComparerResults comparisonResults,
        Map<String, String> objectKeyToFilepathMap)
    {
        try {
            // Determine which files to upload, prompting user whether to over-write existing files
            List fileKeysForUpload = new ArrayList();
            fileKeysForUpload.addAll(comparisonResults.onlyOnClientKeys);

            int newFiles = comparisonResults.onlyOnClientKeys.size();
            int unchangedFiles = comparisonResults.alreadySynchronisedKeys.size();
            int changedFiles = comparisonResults.updatedOnClientKeys.size()
                + comparisonResults.updatedOnServerKeys.size();

            if (unchangedFiles > 0 || changedFiles > 0) {
                // Ask user whether to replace existing unchanged and/or existing changed files.
                log.debug("Files for upload clash with existing S3 objects, prompting user to choose which files to replace");
                List options = new ArrayList();
                String message = "Of the " + objectKeyToFilepathMap.size()
                    + " file(s) being uploaded:\n\n";

                if (newFiles > 0) {
                    message += newFiles + " file(s) are new.\n\n";
                    options.add(UPLOAD_NEW_FILES_ONLY);
                }
                if (changedFiles > 0) {
                    message += changedFiles + " file(s) have changed.\n\n";
                    options.add(UPLOAD_NEW_AND_CHANGED_FILES);
                }
                if (unchangedFiles > 0) {
                    message += unchangedFiles + " file(s) already exist and are unchanged.\n\n";
                    options.add(UPLOAD_ALL_FILES);
                }
                message += "Please choose which file(s) you wish to upload:";

                Object response = JOptionPane.showInputDialog(
                    ownerFrame, message, "Replace file(s)?", JOptionPane.QUESTION_MESSAGE,
                    null, options.toArray(), UPLOAD_NEW_AND_CHANGED_FILES);

                if (response == null) {
                    return;
                }

                if (UPLOAD_NEW_FILES_ONLY.equals(response)) {
                    // No change required to default fileKeysForUpload list.
                } else if (UPLOAD_ALL_FILES.equals(response)) {
                    fileKeysForUpload.addAll(comparisonResults.updatedOnClientKeys);
                    fileKeysForUpload.addAll(comparisonResults.updatedOnServerKeys);
                    fileKeysForUpload.addAll(comparisonResults.alreadySynchronisedKeys);
                } else if (UPLOAD_NEW_AND_CHANGED_FILES.equals(response)) {
                    fileKeysForUpload.addAll(comparisonResults.updatedOnClientKeys);
                    fileKeysForUpload.addAll(comparisonResults.updatedOnServerKeys);
                } else {
                    // Upload cancelled.
                    stopProgressDialog();
                    return;
                }
            }

            if (fileKeysForUpload.size() == 0) {
                return;
            }

            final String[] statusText = new String[1];
            statusText[0] = "Prepared 0 of " + fileKeysForUpload.size() + " file(s) for upload";
            startProgressDialog(statusText[0], "", 0, 100, null, null);

            long bytesToProcess = 0;
            for (Iterator iter = fileKeysForUpload.iterator(); iter.hasNext();) {
                File file = new File(objectKeyToFilepathMap.get(iter.next().toString()));
                bytesToProcess += file.length();
            }

            BytesProgressWatcher progressWatcher = new BytesProgressWatcher(bytesToProcess) {
                @Override
                public void updateBytesTransferred(long byteCount) {
                    super.updateBytesTransferred(byteCount);

                    String detailsText = formatBytesProgressWatcherDetails(this, false);
                    int progressValue = (int)((double)getBytesTransferred() * 100 / getBytesToTransfer());
                    updateProgressDialog(statusText[0], detailsText, progressValue);
                }
            };

            // Populate S3Objects representing upload files with metadata etc.
            final S3Object[] objects = new S3Object[fileKeysForUpload.size()];
            int objectIndex = 0;
            for (Iterator iter = fileKeysForUpload.iterator(); iter.hasNext();) {
                String fileKey = iter.next().toString();
                File file = new File(objectKeyToFilepathMap.get(fileKey));

                S3Object newObject = ObjectUtils
                    .createObjectForUpload(fileKey, file, null, false, progressWatcher);

                statusText[0] = "Prepared " + (objectIndex + 1)
                    + " of " + fileKeysForUpload.size() + " file(s) for upload";

                objects[objectIndex++] = newObject;
            }

            stopProgressDialog();

            // Confirm we have permission to do this.
            SignatureRequest[] signedRequests = requestSignedRequests(
                    SignatureRequest.SIGNATURE_TYPE_PUT, objects);

            if (signedRequests != null) {
                // Upload the files.
                SignedUrlAndObject[] urlAndObjs = new SignedUrlAndObject[signedRequests.length];
                for (int i = 0; i < signedRequests.length; i++) {
                    urlAndObjs[i] = new SignedUrlAndObject(
                        signedRequests[i].getSignedUrl(), objects[i]);
                }

                s3ServiceMulti.putObjects(urlAndObjs);
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            stopProgressDialog();
            String message = "Unable to upload object(s)";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);
        }
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>CreateObjectsEvent</code>.
     * <p>
     * This method merely updates the progress dialog as files are uploaded.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final CreateObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                startProgressPanel(event.getUniqueOperationId(),
                    "Upload " +
                    byteFormatterTerse.formatByteSize(watcher.getBytesTransferred())
                    + "/" + byteFormatterTerse.formatByteSize(watcher.getBytesTotal()),
                    100, event.getThreadWatcher().getCancelEventListener());
            }
            // ... otherwise show the number of completed threads.
            else {
                startProgressPanel(event.getUniqueOperationId(),
                    "Upload 0/" + watcher.getThreadCount(),
                    (int) watcher.getThreadCount(),
                    event.getThreadWatcher().getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (int i = 0; i < event.getCreatedObjects().length; i++) {
                        objectTableModel.addObject(event.getCreatedObjects()[i]);
                    }
                }
            });

            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                    // Upload is completed, just waiting on resonse from S3.
                    updateProgressPanel(event.getUniqueOperationId(), "Confirming", 100);
                } else {
                    int percentage = (int)
                        (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);

                    updateProgressPanel(event.getUniqueOperationId(),
                        "Upload " +
                        byteFormatterTerse.formatByteSize(watcher.getBytesTransferred())
                        + "/" + byteFormatterTerse.formatByteSize(watcher.getBytesTotal())
                        + " (" +
                        byteFormatterTerse.formatByteSize(watcher.getBytesPerSecond()) + "/s, "
                        + timeFormatterTerse.formatTime(watcher.getTimeRemaining())
                        + ")", percentage);
                }
            }
            // ... otherwise show the number of completed threads.
            else {
                ThreadWatcher progressStatus = event.getThreadWatcher();
                updateProgressPanel(event.getUniqueOperationId(),
                    "Upload " + progressStatus.getCompletedThreads()
                    + "/" + progressStatus.getThreadCount(),
                    (int) progressStatus.getCompletedThreads());
            }
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateObjectsSummary();
                }
            });

            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateObjectsSummary();
                }
            });

            stopProgressPanel(event.getUniqueOperationId());
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressPanel(event.getUniqueOperationId());

            String message = "Unable to upload object(s)";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, event.getErrorCause());
        }
    }

    private void generatePublicGetUrl() {
        final S3Object[] objects = getSelectedObjects();

        if (objects.length != 1) {
            log.warn("Ignoring Generate Public URL object command, can only operate on a single object");
            return;
        }
        S3Object currentObject = objects[0];

        try {
            String hostAndBucket = null;
            if (userVanityHost != null) {
                hostAndBucket = userVanityHost;
            } else {
                boolean disableDnsBuckets = false;

                String s3Endpoint = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                    .getStringProperty("s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
                hostAndBucket = ServiceUtils.generateS3HostnameForBucket(
                    userBucketName, disableDnsBuckets, s3Endpoint);

                if (!ServiceUtils.isBucketNameValidDNSName(userBucketName)) {
                    // If bucket name isn't DNS compatible, we must include the bucket
                    // name as a URL path item.
                    hostAndBucket += "/" + userBucketName;
                }
            }

            String url = "http://" + hostAndBucket + "/" + userPath + currentObject.getKey();

            // Display signed URL
            String dialogText = "Public URL for '" + currentObject.getKey() + "'.";
            // Ensure dialog text is at least 150 characters (to force dialog to be wider)
            if (dialogText.length() < 150) {
                int charsShort = 150 - dialogText.length();
                StringBuffer padding = new StringBuffer();
                for (int i = 0; i < charsShort / 2; i++) {
                    padding.append(" ");
                }
                dialogText = padding.toString() + dialogText + padding.toString();
            }

            JOptionPane.showInputDialog(ownerFrame, dialogText,
                "URL", JOptionPane.INFORMATION_MESSAGE, null, null, url);

        } catch (NumberFormatException e) {
            String message = "Hours must be a valid decimal value; eg 3, 0.1";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);
        } catch (Exception e) {
            String message = "Unable to generate public GET URL";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, e);
        }
    }

    private void deleteSelectedObjects() {
        final S3Object[] objects = getSelectedObjects();

        if (objects.length == 0) {
            log.warn("Ignoring delete object(s) command, no currently selected objects");
            return;
        }

        int response = JOptionPane.showConfirmDialog(ownerFrame,
            (objects.length == 1 ?
                "Are you sure you want to delete '" + objects[0].getKey() + "'?" :
                "Are you sure you want to delete " + objects.length + " object(s)"
            ),
            "Delete Object(s)?", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.NO_OPTION) {
            return;
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    SignatureRequest[] signatureRequests = requestSignedRequests(
                            SignatureRequest.SIGNATURE_TYPE_DELETE, objects);

                    if (signatureRequests != null) {
                        String[] signedRequests = new String[signatureRequests.length];
                        for (int i = 0; i < signedRequests.length; i++) {
                            signedRequests[i] = signatureRequests[i].getSignedUrl();
                        }

                        s3ServiceMulti.deleteObjects(signedRequests);
                    } else {
                        ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Sorry, you do not have the permission to delete files", null);
                    }
                } catch (Exception e) {
                    stopProgressDialog();
                    log.error("Gatekeeper permissions check failed", e);
                    ErrorDialog.showDialog(ownerFrame, null, cockpitLiteProperties.getProperties(),
                            "Permissions check failed, please try again", e);
                }
            }
        }.start();
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>DeleteObjectsEvent</code>.
     * <p>
     * This method merely updates the progress dialog as objects are deleted.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final DeleteObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            startProgressDialog(
                "Deleted 0 of " + event.getThreadWatcher().getThreadCount() + " object(s)",
                "", 0, (int) event.getThreadWatcher().getThreadCount(), "Cancel Delete Objects",
                event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (int i = 0; i < event.getDeletedObjects().length; i++) {
                        objectTableModel.removeObject(
                            event.getDeletedObjects()[i]);
                    }
                }
            });

            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Deleted " + progressStatus.getCompletedThreads()
                + " of " + progressStatus.getThreadCount() + " object(s)";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateObjectsSummary();
                }
            });

            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to delete object(s)";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, event.getErrorCause());
        }
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>GetObjectHeadsEvent</code>.
     * <p>
     * This method merely updates the progress dialog as object details (heads) are retrieved.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final GetObjectHeadsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            if (event.getThreadWatcher().getThreadCount() > 0) {
                startProgressDialog("Retrieved details for 0 of "
                    + event.getThreadWatcher().getThreadCount() + " object(s)",
                    "", 0, (int) event.getThreadWatcher().getThreadCount(), "Cancel Retrieval",
                    event.getThreadWatcher().getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            final ThreadWatcher progressStatus = event.getThreadWatcher();

            // Store detail-complete objects in table.
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    synchronized (lock) {
                        // Retain selected status of objects for downloads or properties
                        for (int i = 0; i < event.getCompletedObjects().length; i++) {
                            S3Object object = event.getCompletedObjects()[i];
                            int modelIndex = objectTableModel.addObject(object);
                            log.debug("Updated table with " + object.getKey() + ", content-type=" + object.getContentType());

                            if (isDownloadingObjects) {
                                s3DownloadObjectsMap.put(object.getKey(), object);
                                log.debug("Updated object download list with " + object.getKey()
                                    + ", content-type=" + object.getContentType());
                            } else if (isUploadingFiles) {
                                s3ExistingObjectsMap.put(object.getKey(), object);
                                log.debug("Updated object upload list with " + object.getKey()
                                    + ", content-type=" + object.getContentType());
                            }

                            int viewIndex = objectTableModelSorter.viewIndex(modelIndex);
                            if (isDownloadingObjects || isViewingObjectProperties) {
                                objectsTable.addRowSelectionInterval(viewIndex, viewIndex);
                            }
                        }
                    }
                }
            });

            // Update progress of GetObject requests.
            String statusText = "Retrieved details for " + progressStatus.getCompletedThreads()
                + " of " + progressStatus.getThreadCount() + " object(s)";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            // Stop GetObjectHead progress display.
            stopProgressDialog();

            synchronized (lock) {
                if (isDownloadingObjects) {
                    compareRemoteAndLocalFiles(filesAlreadyInDownloadDirectoryMap, s3DownloadObjectsMap, false);
                    isDownloadingObjects = false;
                } else if (isUploadingFiles) {
                    compareRemoteAndLocalFiles(objectKeyToFilepathMap, s3ExistingObjectsMap, true);
                    isUploadingFiles = false;
                } else if (isViewingObjectProperties) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            ItemPropertiesDialog.showDialog(ownerFrame, getSelectedObjects(),
                                cockpitLiteProperties.getProperties(),
                                // Only admin users with all rights can view metadata
                                (userCanUpload && userCanDownload && userCanACL && userCanDelete));
                            isViewingObjectProperties = false;
                        }
                    });
                }
            }
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to retrieve object(s) details";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, cockpitLiteProperties.getProperties(), message, event.getErrorCause());
        }
    }

    private String formatTransferDetails(ThreadWatcher watcher) {
        long bytesPerSecond = watcher.getBytesPerSecond();
        String detailsText = byteFormatter.formatByteSize(bytesPerSecond) + "/s";

        if (watcher.isTimeRemainingAvailable()) {
            long secondsRemaining = watcher.getTimeRemaining();
            detailsText += " - Time remaining: " + timeFormatterTerse.formatTime(secondsRemaining);
        }
        return detailsText;
    }

    private String formatBytesProgressWatcherDetails(BytesProgressWatcher watcher, boolean includeBytes) {
        long secondsRemaining = watcher.getRemainingTime();

        String detailsText =
            (includeBytes
                ? byteFormatter.formatByteSize(watcher.getBytesTransferred())
                  + " of " + byteFormatter.formatByteSize(watcher.getBytesToTransfer())
                  + " - "
                : "")
            + "Time remaining: " +
            timeFormatterTerse.formatTime(secondsRemaining);
        return detailsText;
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
        if (!isStandAloneApplication) {
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
                AuthenticationDialog pwDialog = new AuthenticationDialog(
                    ownerFrame, "Authentication Required",
                    "<html>Host <b>" + scope.getHost() + ":" + scope.getPort()
                    + "</b> requires Windows authentication</html>", true);
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
                //authscheme instanceof RFC2617Scheme
                AuthenticationDialog pwDialog = new AuthenticationDialog(
                    ownerFrame, "Authentication Required",
                    "<html><center>Host <b>" + scope.getHost() + ":" + scope.getPort() + "</b>"
                    + " requires authentication for the realm:<br><b>" + scope.getRealm()
                    + "</b></center></html>", false);
                pwDialog.setVisible(true);
                if (pwDialog.getUser().length() > 0) {
                    credentials = new UsernamePasswordCredentials(pwDialog.getUser(), pwDialog.getPassword());
                }
                pwDialog.dispose();
            } else {
                throw new InvalidCredentialsException(
                        "Unsupported authentication scheme: " + scope.getScheme());
            }
            if (credentials != null){
                mCredentialProvider.setCredentials(scope, credentials);
            }
            return credentials;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    private boolean isObjectFilteringActive() {
        if (!filterObjectsCheckBox.isSelected()) {
            return false;
        } else {
            if (filterObjectsPrefix.getText().length() > 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    public void s3ServiceEventPerformed(CreateBucketsEvent event) {
        // Not applicable in this app.
    }

    public void s3ServiceEventPerformed(CopyObjectsEvent event) {
        // Not applicable in this app.
    }


    public static String getAclDescription(AccessControlList acl) {
        if (acl == null) {
            return ACL_UNKNOWN_DESCRIPTION;
        }

        for (GrantAndPermission gap: acl.getGrantAndPermissions()) {
            if (GroupGrantee.ALL_USERS.equals(gap.getGrantee())
                && Permission.PERMISSION_READ.equals(gap.getPermission()))
            {
                return ACL_PUBLIC_DESCRIPTION;
            }
        }
        if (AccessControlList.REST_CANNED_PUBLIC_READ.equals(acl)) {
            return ACL_PUBLIC_DESCRIPTION;
        }
        return ACL_PRIVATE_DESCRIPTION;
    }

    private class ContextMenuListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            showContextMenu(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            showContextMenu(e);
        }

        private void showContextMenu(MouseEvent e) {
            if (e.isPopupTrigger()) {
                // Select item under context-click.
                if (e.getSource() instanceof JList) {
                    JList jList = (JList) e.getSource();
                    int locIndex = jList.locationToIndex(e.getPoint());
                    if (locIndex >= 0) {
                        jList.setSelectedIndex(locIndex);
                    }
                } else if (e.getSource() instanceof JTable) {
                    JTable jTable = (JTable) e.getSource();
                    int rowIndex = jTable.rowAtPoint(e.getPoint());
                    if (rowIndex >= 0) {
                        jTable.addRowSelectionInterval(rowIndex, rowIndex);
                    }
                }

                // Show context popup menu.
                if (e.getSource().equals(objectsTable)) {
                    showObjectPopupMenu((JComponent)e.getSource(), e.getX(), e.getY());
                }
            }
        }
    }

    /**
     * Runs Cockpit as a stand-alone application.
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        JFrame ownerFrame = new JFrame("JetS3t Cockpit-Lite");
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

        new CockpitLite(ownerFrame, argumentProperties);
    }

}
