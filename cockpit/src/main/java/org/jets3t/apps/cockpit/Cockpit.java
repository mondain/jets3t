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
package org.jets3t.apps.cockpit;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import org.jets3t.apps.cockpit.gui.AccessControlDialog;
import org.jets3t.apps.cockpit.gui.BucketLoggingDialog;
import org.jets3t.apps.cockpit.gui.BucketTableModel;
import org.jets3t.apps.cockpit.gui.CreateBucketDialog;
import org.jets3t.apps.cockpit.gui.ObjectTableModel;
import org.jets3t.apps.cockpit.gui.PreferencesDialog;
import org.jets3t.apps.cockpit.gui.RequesterPaysDialog;
import org.jets3t.apps.cockpit.gui.SignedGetUrlDialog;
import org.jets3t.apps.cockpit.gui.StartupDialog;
import org.jets3t.gui.AuthenticationDialog;
import org.jets3t.gui.CopyObjectsDialog;
import org.jets3t.gui.ErrorDialog;
import org.jets3t.gui.GuiUtils;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.ItemPropertiesDialog;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.gui.ManageDistributionsDialog;
import org.jets3t.gui.ObjectsAttributesDialog;
import org.jets3t.gui.ProgressDialog;
import org.jets3t.gui.TableSorter;
import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.CloudFrontService;
import org.jets3t.service.CloudFrontServiceException;
import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.S3ServiceException;
import org.jets3t.service.StorageObjectsChunk;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.impl.rest.httpclient.RestS3Service;
import org.jets3t.service.io.BytesProgressWatcher;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.cloudfront.Distribution;
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
import org.jets3t.service.security.AWSCredentials;
import org.jets3t.service.security.EncryptionUtil;
import org.jets3t.service.security.ProviderCredentials;
import org.jets3t.service.utils.ByteFormatter;
import org.jets3t.service.utils.FileComparer;
import org.jets3t.service.utils.FileComparerResults;
import org.jets3t.service.utils.Mimetypes;
import org.jets3t.service.utils.ObjectUtils;
import org.jets3t.service.utils.TimeFormatter;

import contribs.com.centerkey.utils.BareBonesBrowserLaunch;

/**
 * Cockpit is a graphical Java application for viewing and managing the contents of an Amazon S3 account.
 * For more information and help please see the
 * <a href="http://www.jets3t.org/applications/cockpit.html">Cockpit Guide</a>.
 * <p>
 * This is the Cockpit application class; it may be run as a stand-alone application or as an Applet.
 *
 * @author jmurty
 */
public class Cockpit extends JApplet implements S3ServiceEventListener, ActionListener,
    ListSelectionListener, HyperlinkActivatedListener, CredentialsProvider
{
    private static final long serialVersionUID = -3193198830234682011L;

    private static final Log log = LogFactory.getLog(Cockpit.class);

    public static final String JETS3T_COCKPIT_HELP_PAGE = "http://www.jets3t.org/applications/cockpit.html";
    public static final String AMAZON_S3_PAGE = "http://www.amazon.com/s3";
    public static final String OS_NAME_MAC = "Mac OS X";

    public static final String APPLICATION_DESCRIPTION = "Cockpit/" + Constants.JETS3T_VERSION;

    public static final String APPLICATION_TITLE = "JetS3t Cockpit";
    private static final int BUCKET_LIST_CHUNKING_SIZE = 1000;

    private final File cockpitHomeDirectory = Constants.DEFAULT_PREFERENCES_DIRECTORY;
    private CockpitPreferences cockpitPreferences = null;

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(5, 7, 5, 7);

    private final ByteFormatter byteFormatter = new ByteFormatter();
    private final TimeFormatter timeFormatter = new TimeFormatter();
    private final SimpleDateFormat yearAndTimeSDF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final SimpleDateFormat timeSDF = new SimpleDateFormat("HH:mm:ss");

    private final GuiUtils guiUtils = new GuiUtils();

    /**
     * Multi-threaded S3 service used by the application.
     */
    private boolean isTargetS3 = true;
    private String originalTargetEndpoint = null;
    private S3ServiceMulti s3ServiceMulti = null;
    private CloudFrontService cloudFrontService = null;
    private boolean cloudFrontMembershipChecked = false;

    private JFrame ownerFrame = null;
    private boolean isStandAloneApplication = false;

    // Service main menu items
    private JMenuItem loginMenuItem = null;
    private JMenuItem logoutMenuItem = null;
    private JMenu loginSwitchMenu = null;

    // Bucket main menu items
    private JPopupMenu bucketActionMenu = null;
    private JMenuItem viewBucketPropertiesMenuItem = null;
    private JMenuItem refreshBucketMenuItem = null;
    private JMenuItem createBucketMenuItem = null;
    private JMenuItem manageDistributionsMenuItem = null;
    private JMenuItem updateBucketACLMenuItem = null;
    private JMenuItem updateBucketRequesterPaysStatusMenuItem = null;
    private JMenuItem deleteBucketMenuItem = null;

    // Object main menu items
    private JPopupMenu objectActionMenu = null;
    private JMenuItem refreshObjectMenuItem = null;
    private JMenuItem viewOrModifyObjectAttributesMenuItem = null;
    private JMenuItem copyObjectsMenuItem = null;
    private JMenuItem updateObjectACLMenuItem = null;
    private JMenuItem downloadObjectMenuItem = null;
    private JMenuItem uploadFilesMenuItem = null;
    private JMenuItem generatePublicGetUrls = null;
    private JMenuItem generateTorrentUrl = null;
    private JMenuItem deleteObjectMenuItem = null;

    // Tools menu items.
    private JMenuItem bucketLoggingMenuItem = null;

    // Preference menu items.
    private JMenuItem preferencesDialogMenuItem = null;

    // Help menu items.
    private JMenuItem cockpitHelpMenuItem = null;
    private JMenuItem amazonS3HelpMenuItem = null;

    // Tables
    private JTable bucketsTable = null;
    private JTable objectsTable = null;
    private JScrollPane objectsTableSP = null;
    private BucketTableModel bucketTableModel =  null;
    private TableSorter bucketTableModelSorter = null;
    private ObjectTableModel objectTableModel =  null;
    private TableSorter objectTableModelSorter = null;

    private JLabel objectsSummaryLabel = null;

    private final HashMap cachedBuckets = new HashMap();
    private ProgressDialog progressDialog = null;
    private ObjectsAttributesDialog objectsAttributesDialog = null;

    private File downloadDirectory = null;
    private File fileChoosersLastUploadDirectory = null;

    private JPanel filterObjectsPanel = null;
    private JCheckBox filterObjectsCheckBox = null;
    private JTextField filterObjectsPrefix = null;
    private JComboBox filterObjectsDelimiter = null;

    // File comparison options
    private static final String UPLOAD_NEW_FILES_ONLY = "Only upload new files";
    private static final String UPLOAD_NEW_AND_CHANGED_FILES = "Upload new and changed files";
    private static final String UPLOAD_ALL_FILES = "Upload all files";
    private static final String DOWNLOAD_NEW_FILES_ONLY = "Only download new files";
    private static final String DOWNLOAD_NEW_AND_CHANGED_FILES = "Download new and changed files";
    private static final String DOWNLOAD_ALL_FILES = "Download all files";

    private EncryptionUtil encryptionUtil = null;
    private Jets3tProperties cockpitProperties = null;
    private SkinsFactory skinsFactory = null;

    private S3Bucket currentSelectedBucket = null;

    private final HashMap<String, ProviderCredentials> loginAwsCredentialsMap = new HashMap<String, ProviderCredentials>();
    private final CredentialsProvider mCredentialProvider;

    /**
     * Constructor to run this application as an Applet.
     */
    public Cockpit() {
        mCredentialProvider = new BasicCredentialsProvider();
    }

    /**
     * Constructor to run this application in a stand-alone window.
     *
     * @param ownerFrame the frame the application will be displayed in
     * @throws S3ServiceException
     */
    public Cockpit(JFrame ownerFrame) throws S3ServiceException {
        this();
        this.ownerFrame = ownerFrame;
        isStandAloneApplication = true;
        init();

        ownerFrame.getContentPane().add(this);
        ownerFrame.setBounds(this.getBounds());
        ownerFrame.setVisible(true);
    }

    protected RestS3Service getRestS3Service(ProviderCredentials credentials)
        throws S3ServiceException
    {
        if (!this.isTargetS3) {
            // Override endpoint property in JetS3t properties
            originalTargetEndpoint =  cockpitProperties.getStringProperty(
                    "s3service.s3-endpoint", Constants.S3_DEFAULT_HOSTNAME);
            cockpitProperties.setProperty(
                "s3service.s3-endpoint", Constants.GS_DEFAULT_HOSTNAME);
        } else if (originalTargetEndpoint != null) {
            cockpitProperties.setProperty(
                "s3service.s3-endpoint", originalTargetEndpoint);
        }
        return new RestS3Service(credentials, APPLICATION_DESCRIPTION,
            this, cockpitProperties);
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
            if (!(c instanceof JFrame)) {
                this.ownerFrame = new JFrame();
            } else {
                this.ownerFrame = (JFrame) c;
            }
        }

        // Initialise the GUI.
        initGui();

        // Load Cockpit configuration files from cockpit's home directory.
        File mimeTypesFile = new File(cockpitHomeDirectory, "mime.types");
        if (mimeTypesFile.exists()) {
            try {
                Mimetypes.getInstance().loadAndReplaceMimetypes(
                    new FileInputStream(mimeTypesFile));
            } catch (IOException e) {
                String message = "Unable to load mime.types file: " + mimeTypesFile;
                log.error(message, e);
                ErrorDialog.showDialog(ownerFrame, this, message, e);
            }
        }
        File jets3tPropertiesFile = new File(cockpitHomeDirectory, "jets3t.properties");
        if (jets3tPropertiesFile.exists()) {
            try {
                Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                    .loadAndReplaceProperties(new FileInputStream(jets3tPropertiesFile),
                        "jets3t.properties in Cockpit's home folder " + cockpitHomeDirectory);
            } catch (IOException e) {
                String message = "Unable to load jets3t.properties file: " + jets3tPropertiesFile;
                log.error(message, e);
                ErrorDialog.showDialog(ownerFrame, this, message, e);
            }
        }
        // Initialise the user's preferences.
        this.cockpitPreferences = new CockpitPreferences();
        File cockpitPreferencesPropertiesFile = new File(cockpitHomeDirectory, Constants.COCKPIT_PROPERTIES_FILENAME);
        if (cockpitPreferencesPropertiesFile.exists()) {
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(cockpitPreferencesPropertiesFile));
                this.cockpitPreferences.fromProperties(properties);
            } catch (IOException e) {
                String message = "Unable to load your preferences";
                log.error(message, e);
                ErrorDialog.showDialog(ownerFrame, this, message, e);
            }
        }

        cockpitProperties = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME);
        skinsFactory = SkinsFactory.getInstance(cockpitProperties.getProperties());

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                loginEvent(null);
            }
        });
    }

    /**
     * Initialises the application's GUI elements.
     */
    private void initGui() {
        initMenus();

        JPanel appContent = new JPanel(new GridBagLayout());
        this.getContentPane().add(appContent);

        // Buckets panel.
        JPanel bucketsPanel = new JPanel(new GridBagLayout());

        JButton bucketActionButton = new JButton();
        bucketActionButton.setToolTipText("Bucket actions menu");
        guiUtils.applyIcon(bucketActionButton, "/images/nuvola/16x16/actions/misc.png");
        bucketActionButton.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
                JButton sourceButton = (JButton) e.getSource();
                bucketActionMenu.show(sourceButton, 0, sourceButton.getHeight());
           }
        });
        bucketsPanel.add(new JHtmlLabel("<html><b>Buckets</b></html>", this),
            new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        bucketsPanel.add(bucketActionButton,
            new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        bucketTableModel = new BucketTableModel(false);
        bucketTableModelSorter = new TableSorter(bucketTableModel);
        bucketsTable = new JTable(bucketTableModelSorter);
        bucketTableModelSorter.setTableHeader(bucketsTable.getTableHeader());
        bucketsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bucketsTable.getSelectionModel().addListSelectionListener(this);
        bucketsTable.setShowHorizontalLines(true);
        bucketsTable.setShowVerticalLines(false);
        bucketsTable.addMouseListener(new ContextMenuListener());
        bucketsPanel.add(new JScrollPane(bucketsTable),
            new GridBagConstraints(0, 1, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));
        bucketsPanel.add(new JLabel(" "),
            new GridBagConstraints(0, 2, 2, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));

        // Filter panel.
        filterObjectsPanel = new JPanel(new GridBagLayout());
        filterObjectsPrefix = new JTextField();
        filterObjectsPrefix.setToolTipText("Only show objects with this prefix");
        filterObjectsPrefix.addActionListener(this);
        filterObjectsPrefix.setActionCommand("RefreshObjects");
        filterObjectsDelimiter = new JComboBox(new String[] {"", "/", "?", "\\"});
        filterObjectsDelimiter.setEditable(true);
        filterObjectsDelimiter.setToolTipText("Object name delimiter");
        filterObjectsDelimiter.addActionListener(this);
        filterObjectsDelimiter.setActionCommand("RefreshObjects");
        filterObjectsPanel.add(new JHtmlLabel("Prefix:", this),
            new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        filterObjectsPanel.add(filterObjectsPrefix,
            new GridBagConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        filterObjectsPanel.add(new JHtmlLabel("Delimiter:", this),
            new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsDefault, 0, 0));
        filterObjectsPanel.add(filterObjectsDelimiter,
            new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        filterObjectsPanel.setVisible(false);

        // Objects panel.
        JPanel objectsPanel = new JPanel(new GridBagLayout());
        int row = 0;
        filterObjectsCheckBox = new JCheckBox("Filter objects");
        filterObjectsCheckBox.addActionListener(this);
        filterObjectsCheckBox.setToolTipText("Check this option to filter the objects listed");
        objectsPanel.add(new JHtmlLabel("<html><b>Objects</b></html>", this),
            new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        objectsPanel.add(filterObjectsCheckBox,
            new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        JButton objectActionButton = new JButton();
        objectActionButton.setToolTipText("Object actions menu");
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

        objectsTable = new JTable();
        objectTableModel = new ObjectTableModel();
        objectTableModelSorter = new TableSorter(objectTableModel);
        objectTableModelSorter.setTableHeader(objectsTable.getTableHeader());
        objectsTable.setModel(objectTableModelSorter);
        objectsTable.setDefaultRenderer(Long.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 301092191828910402L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String formattedSize = byteFormatter.formatByteSize(((Long)value).longValue());
                return super.getTableCellRendererComponent(table, formattedSize, isSelected, hasFocus, row, column);
            }
        });
        objectsTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = 7285511556343895652L;

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
        objectsTableSP = new JScrollPane(objectsTable);
        objectsPanel.add(objectsTableSP,
                new GridBagConstraints(0, ++row, 3, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));
        objectsSummaryLabel = new JHtmlLabel("Please select a bucket", this);
        objectsSummaryLabel.setHorizontalAlignment(JLabel.CENTER);
        objectsSummaryLabel.setFocusable(false);
        objectsPanel.add(objectsSummaryLabel,
                new GridBagConstraints(0, ++row, 3, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Combine sections.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                bucketsPanel, objectsPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);

        appContent.add(splitPane,
                new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsDefault, 0, 0));

        // Set preferred sizes
        int preferredWidth = 800;
        int preferredHeight = 600;
        this.setBounds(new Rectangle(new Dimension(preferredWidth, preferredHeight)));

        splitPane.setResizeWeight(0.30);

        // Initialize drop target.
        initDropTarget(new JComponent[] {objectsTableSP, objectsTable} );
        objectsTable.getDropTarget().setActive(false);
        objectsTableSP.getDropTarget().setActive(false);
    }

    /**
     * Initialise the application's menu bar.
     */
    private void initMenus() {
        JMenuBar appMenuBar = new JMenuBar();
        if (this.isStandAloneApplication && OS_NAME_MAC.equals(System.getProperty("os.name"))) {
            /*
             * We need to check we're running on a Mac before adding the menu to the
             * owner frame, otherwise the menus will be displayed *behind* the main
             * GUI panel on Windows Vista and later [sigh]
             */
            ownerFrame.setJMenuBar(appMenuBar);
        } else {
            this.setJMenuBar(appMenuBar);
        }

        // Service menu
        JMenu serviceMenu = new JMenu("Service");

        loginMenuItem = new JMenuItem("Log in...");
        loginMenuItem.setActionCommand("LoginEvent");
        loginMenuItem.addActionListener(this);
        guiUtils.applyIcon(loginMenuItem, "/images/nuvola/16x16/actions/connect_creating.png");
        serviceMenu.add(loginMenuItem);

        logoutMenuItem = new JMenuItem("Log out");
        logoutMenuItem.setActionCommand("LogoutEvent");
        logoutMenuItem.addActionListener(this);
        guiUtils.applyIcon(logoutMenuItem, "/images/nuvola/16x16/actions/connect_no.png");
        serviceMenu.add(logoutMenuItem);

        loginSwitchMenu = new JMenu("Switch login");
        loginSwitchMenu.addActionListener(this);
        serviceMenu.add(new JSeparator());
        guiUtils.applyIcon(loginSwitchMenu, "/images/nuvola/16x16/actions/connect_established.png");
        serviceMenu.add(loginSwitchMenu);
        loginSwitchMenu.setEnabled(false);

        if (isStandAloneApplication) {
            serviceMenu.add(new JSeparator());

            JMenuItem quitMenuItem = new JMenuItem("Quit");
            quitMenuItem.setActionCommand("QuitEvent");
            quitMenuItem.addActionListener(this);
            guiUtils.applyIcon(quitMenuItem, "/images/nuvola/16x16/actions/exit.png");
            serviceMenu.add(quitMenuItem);
        }

        logoutMenuItem.setEnabled(false);

        // Bucket action menu.
        bucketActionMenu = new JPopupMenu();

        refreshBucketMenuItem = new JMenuItem("Refresh bucket listing");
        refreshBucketMenuItem.setActionCommand("RefreshBuckets");
        refreshBucketMenuItem.addActionListener(this);
        guiUtils.applyIcon(refreshBucketMenuItem, "/images/nuvola/16x16/actions/reload.png");
        bucketActionMenu.add(refreshBucketMenuItem);

        viewBucketPropertiesMenuItem = new JMenuItem("View bucket properties...");
        viewBucketPropertiesMenuItem.setActionCommand("ViewBucketProperties");
        viewBucketPropertiesMenuItem.addActionListener(this);
        guiUtils.applyIcon(viewBucketPropertiesMenuItem, "/images/nuvola/16x16/actions/viewmag.png");
        bucketActionMenu.add(viewBucketPropertiesMenuItem);

        updateBucketACLMenuItem = new JMenuItem("Update bucket's Access Control List...");
        updateBucketACLMenuItem.setActionCommand("UpdateBucketACL");
        updateBucketACLMenuItem.addActionListener(this);
        guiUtils.applyIcon(updateBucketACLMenuItem, "/images/nuvola/16x16/actions/encrypted.png");
        bucketActionMenu.add(updateBucketACLMenuItem);

        updateBucketRequesterPaysStatusMenuItem = new JMenuItem("Update bucket's Requester Pays status...");
        updateBucketRequesterPaysStatusMenuItem.setActionCommand("UpdateBucketRequesterPaysStatus");
        updateBucketRequesterPaysStatusMenuItem.addActionListener(this);
        guiUtils.applyIcon(updateBucketRequesterPaysStatusMenuItem, "/images/nuvola/16x16/actions/identity.png");
        bucketActionMenu.add(updateBucketRequesterPaysStatusMenuItem);

        bucketActionMenu.add(new JSeparator());

        createBucketMenuItem = new JMenuItem("Create new bucket...");
        createBucketMenuItem.setActionCommand("CreateBucket");
        createBucketMenuItem.addActionListener(this);
        guiUtils.applyIcon(createBucketMenuItem, "/images/nuvola/16x16/actions/viewmag+.png");
        bucketActionMenu.add(createBucketMenuItem);

        JMenuItem thirdPartyBucketMenuItem = new JMenuItem("Add third-party bucket...");
        thirdPartyBucketMenuItem.setActionCommand("AddThirdPartyBucket");
        thirdPartyBucketMenuItem.addActionListener(this);
        guiUtils.applyIcon(thirdPartyBucketMenuItem, "/images/nuvola/16x16/actions/viewmagfit.png");
        bucketActionMenu.add(thirdPartyBucketMenuItem);

        bucketActionMenu.add(new JSeparator());

        deleteBucketMenuItem = new JMenuItem("Delete bucket...");
        deleteBucketMenuItem.setActionCommand("DeleteBucket");
        deleteBucketMenuItem.addActionListener(this);
        guiUtils.applyIcon(deleteBucketMenuItem, "/images/nuvola/16x16/actions/cancel.png");
        bucketActionMenu.add(deleteBucketMenuItem);

        viewBucketPropertiesMenuItem.setEnabled(false);
        refreshBucketMenuItem.setEnabled(false);
        createBucketMenuItem.setEnabled(false);
        updateBucketACLMenuItem.setEnabled(false);
        updateBucketRequesterPaysStatusMenuItem.setEnabled(false);
        deleteBucketMenuItem.setEnabled(false);

        // Object action menu.
        objectActionMenu = new JPopupMenu();

        refreshObjectMenuItem = new JMenuItem("Refresh object listing");
        refreshObjectMenuItem.setActionCommand("RefreshObjects");
        refreshObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(refreshObjectMenuItem, "/images/nuvola/16x16/actions/reload.png");
        objectActionMenu.add(refreshObjectMenuItem);

        viewOrModifyObjectAttributesMenuItem = new JMenuItem("View or Modify object attributes...");
        viewOrModifyObjectAttributesMenuItem.setActionCommand("ViewOrModifyObjectAttributes");
        viewOrModifyObjectAttributesMenuItem.addActionListener(this);
        guiUtils.applyIcon(viewOrModifyObjectAttributesMenuItem, "/images/nuvola/16x16/actions/viewmag.png");
        objectActionMenu.add(viewOrModifyObjectAttributesMenuItem);

        copyObjectsMenuItem = new JMenuItem("Copy or Move objects...");
        copyObjectsMenuItem.setActionCommand("CopyObjects");
        copyObjectsMenuItem.addActionListener(this);
        guiUtils.applyIcon(copyObjectsMenuItem, "/images/nuvola/16x16/actions/filenew.png");
        objectActionMenu.add(copyObjectsMenuItem);

        updateObjectACLMenuItem = new JMenuItem("View or Modify Access Control Lists...");
        updateObjectACLMenuItem.setActionCommand("UpdateObjectACL");
        updateObjectACLMenuItem.addActionListener(this);
        guiUtils.applyIcon(updateObjectACLMenuItem, "/images/nuvola/16x16/actions/encrypted.png");
        objectActionMenu.add(updateObjectACLMenuItem);

        downloadObjectMenuItem = new JMenuItem("Download objects...");
        downloadObjectMenuItem.setActionCommand("DownloadObjects");
        downloadObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(downloadObjectMenuItem, "/images/nuvola/16x16/actions/1downarrow.png");
        objectActionMenu.add(downloadObjectMenuItem);

        uploadFilesMenuItem = new JMenuItem("Upload files...");
        uploadFilesMenuItem.setActionCommand("UploadFiles");
        uploadFilesMenuItem.addActionListener(this);
        guiUtils.applyIcon(uploadFilesMenuItem, "/images/nuvola/16x16/actions/1uparrow.png");
        objectActionMenu.add(uploadFilesMenuItem);

        objectActionMenu.add(new JSeparator());

        generatePublicGetUrls = new JMenuItem("Generate Public GET URLs...");
        generatePublicGetUrls.setActionCommand("GeneratePublicGetURLs");
        generatePublicGetUrls.addActionListener(this);
        guiUtils.applyIcon(generatePublicGetUrls, "/images/nuvola/16x16/actions/wizard.png");
        objectActionMenu.add(generatePublicGetUrls);

        generateTorrentUrl = new JMenuItem("Generate Torrent URL...");
        generateTorrentUrl.setActionCommand("GenerateTorrentURL");
        generateTorrentUrl.addActionListener(this);
        guiUtils.applyIcon(generateTorrentUrl, "/images/nuvola/16x16/actions/wizard.png");
        objectActionMenu.add(generateTorrentUrl);

        objectActionMenu.add(new JSeparator());

        deleteObjectMenuItem = new JMenuItem("Delete objects...");
        deleteObjectMenuItem.setActionCommand("DeleteObjects");
        deleteObjectMenuItem.addActionListener(this);
        guiUtils.applyIcon(deleteObjectMenuItem, "/images/nuvola/16x16/actions/cancel.png");
        objectActionMenu.add(deleteObjectMenuItem);

        viewOrModifyObjectAttributesMenuItem.setEnabled(false);
        copyObjectsMenuItem.setEnabled(false);
        refreshObjectMenuItem.setEnabled(false);
        updateObjectACLMenuItem.setEnabled(false);
        downloadObjectMenuItem.setEnabled(false);
        uploadFilesMenuItem.setEnabled(false);
        generatePublicGetUrls.setEnabled(false);
        generateTorrentUrl.setEnabled(false);
        deleteObjectMenuItem.setEnabled(false);

        // Tools menu.
        JMenu toolsMenu = new JMenu("Tools");

        bucketLoggingMenuItem = new JMenuItem("Configure Bucket logging...");
        bucketLoggingMenuItem.setActionCommand("BucketLogging");
        bucketLoggingMenuItem.addActionListener(this);
        bucketLoggingMenuItem.setEnabled(false);
        guiUtils.applyIcon(bucketLoggingMenuItem, "/images/nuvola/16x16/actions/toggle_log.png");
        toolsMenu.add(bucketLoggingMenuItem);

        manageDistributionsMenuItem = new JMenuItem("CloudFront Distributions...");
        manageDistributionsMenuItem.setActionCommand("ManageDistributions");
        manageDistributionsMenuItem.addActionListener(this);
        guiUtils.applyIcon(manageDistributionsMenuItem, "/images/nuvola/16x16/actions/irkick.png");
        manageDistributionsMenuItem.setEnabled(false);
        toolsMenu.add(manageDistributionsMenuItem);

        toolsMenu.add(new JSeparator());

        preferencesDialogMenuItem = new JMenuItem("Preferences...");
        preferencesDialogMenuItem.setActionCommand("PreferencesDialog");
        preferencesDialogMenuItem.addActionListener(this);
        guiUtils.applyIcon(preferencesDialogMenuItem, "/images/nuvola/16x16/actions/configure.png");
        toolsMenu.add(preferencesDialogMenuItem);

        // Help menu.
        JMenu helpMenu = new JMenu("Help");
        cockpitHelpMenuItem = new JMenuItem("Cockpit Guide");
        guiUtils.applyIcon(cockpitHelpMenuItem, "/images/nuvola/16x16/actions/help.png");
        cockpitHelpMenuItem.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                   followHyperlink(new URL(JETS3T_COCKPIT_HELP_PAGE), "_blank");
               } catch (MalformedURLException ex) {
                   throw new IllegalStateException("Invalid URL embedded in program: "
                       + JETS3T_COCKPIT_HELP_PAGE);
               }
           }
        });
        helpMenu.add(cockpitHelpMenuItem);
        amazonS3HelpMenuItem = new JMenuItem("Amazon S3");
        guiUtils.applyIcon(amazonS3HelpMenuItem, "/images/nuvola/16x16/actions/gohome.png");
        amazonS3HelpMenuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    followHyperlink(new URL(AMAZON_S3_PAGE), "_blank");
                } catch (MalformedURLException ex) {
                    throw new IllegalStateException("Invalid URL embedded in program: "
                        + AMAZON_S3_PAGE);
                }
            }
         });
        helpMenu.add(amazonS3HelpMenuItem);


        // Build application menu bar.
        appMenuBar.add(serviceMenu);
        appMenuBar.add(toolsMenu);
        appMenuBar.add(helpMenu);
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
                            uploadFiles((File[]) fileList.toArray(new File[fileList.size()]));
                        }
                    } catch (Exception e) {
                        String message = "Unable to start accept dropped items";
                        log.error(message, e);
                        ErrorDialog.showDialog(ownerFrame, null, message, e);
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
     * Run the provided Runnable object in a background thread. This method will
     * return as soon as the background thread is started, it does not wait for
     * the thread to complete.
     */
    private synchronized void runInBackgroundThread(Runnable runnable) {
        Thread t = new Thread(runnable);
        t.start();
    }

    /**
     * Run the provided runnable in the application's event dispatcher thread,
     * and wait for the action to complete before returning.
     *
     * @param runnable
     * @return
     */
    private synchronized boolean runInDispatcherThreadImmediately(Runnable runnable) {
        try {
            SwingUtilities.invokeAndWait(runnable);
            return true;
        } catch (Exception e) {
            log.error("Error displaying graphical elements", e);
            return false;
        }
    }


    /**
     * Starts a progress display dialog that cannot be cancelled. While the dialog is running the user
     * cannot interact with the application.
     *
     * @param statusText
     *        describes the status of a task in text meaningful to the user
     */
    private void startProgressDialog(String statusText) {
        startProgressDialog(statusText, null, 0, 0, null, null);
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
            this.progressDialog = new ProgressDialog(this.ownerFrame, "Please wait...", null);
        }

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
        runInDispatcherThreadImmediately(new Runnable() {
            public void run() {
                progressDialog.stopDialog();
            }
         });
    }

    /**
     * Event handler for this application, handles all menu items.
     */
    public void actionPerformed(ActionEvent event) {
        // Service Menu Events
        if ("LoginEvent".equals(event.getActionCommand())) {
            loginEvent(null);
        } else if ("LogoutEvent".equals(event.getActionCommand())) {
            logoutEvent();
        } else if (event.getActionCommand() != null && event.getActionCommand().startsWith("LoginSwitch")) {
            String loginName = event.getActionCommand().substring("LoginSwitch:".length());
            ProviderCredentials credentials = loginAwsCredentialsMap.get(loginName);
            loginEvent(credentials);
        } else if ("QuitEvent".equals(event.getActionCommand())) {
            System.exit(0);
        }

        // Bucket Events.
        else if ("ViewBucketProperties".equals(event.getActionCommand())) {
            listBucketProperties();
        } else if ("RefreshBuckets".equals(event.getActionCommand())) {
            listAllBuckets();
        } else if ("CreateBucket".equals(event.getActionCommand())) {
            createBucketAction();
        } else if ("DeleteBucket".equals(event.getActionCommand())) {
            deleteSelectedBucket();
        } else if ("ManageDistributions".equals(event.getActionCommand())) {
            S3Bucket[] buckets = bucketTableModel.getBuckets();
            String[] bucketNames = new String[buckets.length];
            for (int i = 0; i < buckets.length; i++) {
                bucketNames[i] = buckets[i].getName();
            }
            ManageDistributionsDialog.showDialog(ownerFrame, cloudFrontService, bucketNames, this);
        } else if ("AddThirdPartyBucket".equals(event.getActionCommand())) {
            addThirdPartyBucket();
        } else if ("UpdateBucketACL".equals(event.getActionCommand())) {
            updateBucketAccessControlList();
        } else if ("UpdateBucketRequesterPaysStatus".equals(event.getActionCommand())) {
            updateBucketRequesterPaysSetting();
        }

        // Object Events
        else if ("ViewOrModifyObjectAttributes".equals(event.getActionCommand())) {
            displayObjectsAttributesDialog();
        } else if ("CopyObjects".equals(event.getActionCommand())) {
            copyObjects();
        } else if ("RefreshObjects".equals(event.getActionCommand())) {
            listObjects();
        } else if ("UpdateObjectACL".equals(event.getActionCommand())) {
            displayAclModificationDialog();
        } else if ("GeneratePublicGetURLs".equals(event.getActionCommand())) {
            generatePublicGetUrls();
        } else if ("GenerateTorrentURL".equals(event.getActionCommand())) {
            generateTorrentUrl();
        } else if ("DeleteObjects".equals(event.getActionCommand())) {
            deleteSelectedObjects();
        } else if ("DownloadObjects".equals(event.getActionCommand())) {
            downloadSelectedObjects();
        } else if ("UploadFiles".equals(event.getActionCommand())) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setDialogTitle("Choose files to upload");
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

            uploadFiles(uploadFiles);
        } else if (event.getSource().equals(filterObjectsCheckBox)) {
            if (filterObjectsCheckBox.isSelected()) {
                filterObjectsPanel.setVisible(true);
            } else {
                filterObjectsPanel.setVisible(false);
                filterObjectsPrefix.setText("");
                if (filterObjectsDelimiter.getSelectedIndex() != 0) {
                    filterObjectsDelimiter.setSelectedIndex(0);
                }
            }
        }

        // Tools events
        else if ("BucketLogging".equals(event.getActionCommand())) {
            S3Bucket[] buckets = bucketTableModel.getBuckets();
            BucketLoggingDialog.showDialog(ownerFrame, s3ServiceMulti.getS3Service(), buckets, this);
        }

        // Preference Events
        else if ("PreferencesDialog".equals(event.getActionCommand())) {
            PreferencesDialog.showDialog(cockpitPreferences, ownerFrame, this);

            // Save a user's preferences if requested, otherwise wipe any existing preferences file.
            File cockpitPreferencesPropertiesFile = new File(cockpitHomeDirectory, Constants.COCKPIT_PROPERTIES_FILENAME);
            if (cockpitPreferences.isRememberPreferences()) {
                try {
                    Properties properties = cockpitPreferences.toProperties();
                    if (!cockpitHomeDirectory.exists()) {
                        cockpitHomeDirectory.mkdir();
                    }
                    properties.list(new PrintStream(
                        new FileOutputStream(cockpitPreferencesPropertiesFile)));
                } catch (IOException e) {
                    String message = "Unable to save your preferences";
                    log.error(message, e);
                    ErrorDialog.showDialog(ownerFrame, this, message, e);
                }
            } else if (cockpitPreferencesPropertiesFile.exists()) {
                // User elected not to store preferences, delete the existing preferences file.
                cockpitPreferencesPropertiesFile.delete();
            }

            if (cockpitPreferences.isEncryptionPasswordSet()) {
                try {
                    encryptionUtil = new EncryptionUtil(
                        cockpitPreferences.getEncryptionPassword(),
                        cockpitPreferences.getEncryptionAlgorithm(),
                        EncryptionUtil.DEFAULT_VERSION);
                } catch (Exception e) {
                    String message = "Unable to start encryption utility";
                    log.error(message, e);
                    ErrorDialog.showDialog(ownerFrame, this, message, e);
                }
            } else {
                encryptionUtil = null;
            }
        }

        // Ooops...
        else {
            log.debug("Unrecognised ActionEvent command '" + event.getActionCommand() + "' in " + event);
        }
    }

    /**
     * Handles list selection events for this application.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }

        if (e.getSource().equals(bucketsTable.getSelectionModel())) {
            bucketSelectedAction();
        } else if (e.getSource().equals(objectsTable.getSelectionModel())) {
            objectSelectedAction();
        }
    }

    /**
     * Displays the {@link StartupDialog} dialog and, if the user provides login credentials,
     * logs into the S3 service using those credentials.
     *
     * This method should always be run within the event dispatcher thread.
     */
    private void loginEvent(ProviderCredentials credentials) {
        try {
            if (credentials == null) {
                StartupDialog startupDialog = new StartupDialog(ownerFrame, cockpitProperties, this);
                startupDialog.setVisible(true);
                credentials = startupDialog.getProviderCredentials();
                this.isTargetS3 = startupDialog.isTargetS3();
                startupDialog.dispose();

                if (credentials == null) {
                    log.debug("Log in cancelled by user");
                    return;
                }
            }

            s3ServiceMulti = new S3ServiceMulti(
                getRestS3Service(credentials), this);

            cloudFrontMembershipChecked = false;
            listAllBuckets();

            objectsSummaryLabel.setText(" ");

            logoutMenuItem.setEnabled(true);

            refreshBucketMenuItem.setEnabled(true);
            createBucketMenuItem.setEnabled(true);
            bucketLoggingMenuItem.setEnabled(true);

            String loginName = (credentials.hasFriendlyName()
                ? credentials.getFriendlyName()
                : credentials.getAccessKey());
            if (!loginAwsCredentialsMap.containsKey(loginName)) {
                loginAwsCredentialsMap.put(loginName, credentials);
                JMenuItem menuItem = new JMenuItem(loginName);
                menuItem.setActionCommand("LoginSwitch:" + loginName);
                menuItem.addActionListener(this);
                loginSwitchMenu.add(menuItem);
                loginSwitchMenu.setEnabled(true);
            }
        } catch (Exception e) {
            String message = "Unable to log in to storage service";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);

            logoutEvent();
        }
    }

    /**
     * Logs out of the S3 service by clearing all listed objects and buckets and resetting
     * the s3ServiceMulti member variable.
     *
     * This method should always be invoked within the event dispatching thread.
     */
    private void logoutEvent() {
        log.debug("Logging out");
        try {
            ProviderCredentials credentials = s3ServiceMulti.getAWSCredentials();
            String loginName = (credentials.hasFriendlyName()
                ? credentials.getFriendlyName()
                : credentials.getAccessKey());
            if (loginAwsCredentialsMap.containsKey(loginName)) {
                Component[] components = loginSwitchMenu.getMenuComponents();
                for (int i = 0; i < components.length; i++) {
                    JMenuItem menuItem = (JMenuItem)components[i];
                    if (loginName.equals(menuItem.getText())) {
                        loginSwitchMenu.remove(components[i]);
                        break;
                    }
                }
                loginAwsCredentialsMap.remove(loginName);
                loginSwitchMenu.setEnabled(loginAwsCredentialsMap.size() > 0);
            }

            // Revert to anonymous service.
            s3ServiceMulti = new S3ServiceMulti(
                getRestS3Service(null), this);
            cloudFrontService = null;

            bucketsTable.clearSelection();
            bucketTableModel.removeAllBuckets();
            objectTableModel.removeAllObjects();

            objectsSummaryLabel.setText(" ");

            ownerFrame.setTitle(APPLICATION_TITLE);
            logoutMenuItem.setEnabled(false);

            refreshBucketMenuItem.setEnabled(false);
            createBucketMenuItem.setEnabled(false);
            bucketLoggingMenuItem.setEnabled(false);

            manageDistributionsMenuItem.setEnabled(false);
        } catch (Exception e) {
            String message = "Unable to log out from S3";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    /**
     * Displays the currently selected bucket's properties in the dialog {@link ItemPropertiesDialog}.
     */
    private void listBucketProperties() {
        final S3Bucket selectedBucket = currentSelectedBucket;

        if (selectedBucket.getAcl() == null || !selectedBucket.isLocationKnown()) {
            // Retrieve all a bucket's details before displaying the summary.
            runInBackgroundThread(new Runnable() {
                public void run() {
                    startProgressDialog("Retrieving details for bucket " + selectedBucket.getName());
                    try {
                        try {
                            if (selectedBucket.getAcl() == null) {
                                selectedBucket.setAcl(
                                    s3ServiceMulti.getS3Service().getBucketAcl(
                                        selectedBucket));
                            }
                            if (!selectedBucket.isLocationKnown()) {
                                selectedBucket.setLocation(
                                    s3ServiceMulti.getS3Service().getBucketLocation(
                                        selectedBucket.getName()));
                            }
                            if (!selectedBucket.isRequesterPaysKnown()) {
                                selectedBucket.setRequesterPays(
                                    s3ServiceMulti.getS3Service().isRequesterPaysBucket(
                                        selectedBucket.getName()));
                            }
                        } catch (S3ServiceException e) {
                            // Retrieving details for a third-party bucket will
                            // often fail when ACL or Location is retrieved,
                            // ignore these failures.
                        }

                        stopProgressDialog();
                        runInDispatcherThreadImmediately(new Runnable() {
                            public void run() {
                                ItemPropertiesDialog.showDialog(ownerFrame, selectedBucket, null);
                            }
                        });
                    } catch (final Exception e) {
                        stopProgressDialog();
                        String message = "Unable to retrieve details for bucket";
                        log.error(message, e);
                        ErrorDialog.showDialog(ownerFrame, null, message, e);
                    }
                };
            });
        } else {
            ItemPropertiesDialog.showDialog(ownerFrame, selectedBucket, null);
        }
    }

    /**
     * Displays the currently selected object's properties in the dialog {@link ObjectsAttributesDialog}.
     * <p>
     * As detailed information about the object may not yet be available, this method works
     * indirectly via the {@link #retrieveObjectsDetails} method. The <code>retrieveObjectsDetails</code>
     * method retrieves all the details for the currently selected objects, and once they are available
     * knows to display the dialog as the {@link #isViewingOrModifyingObjectProperties} flag is set.
     */
    private void displayObjectsAttributesDialog() {
        final S3Bucket selectedBucket = currentSelectedBucket;

        runInBackgroundThread(new Runnable() {
            public void run() {
                if (!retrieveObjectsDetails(getSelectedObjects())) {
                    return;
                }

                if (objectsAttributesDialog == null) {
                    objectsAttributesDialog = new ObjectsAttributesDialog(
                        ownerFrame, "Object Attributes", skinsFactory);
                }

                final S3Object[] sourceObjects = getSelectedObjects();

                boolean ok = runInDispatcherThreadImmediately(new Runnable() {
                    public void run() {
                        objectsAttributesDialog.displayDialog(sourceObjects, true);
                    }
                });
                if (!ok) {
                    return;
                }

                final String[] sourceObjectKeys = objectsAttributesDialog.getSourceObjectKeys();
                final S3Object[] destinationObjects = objectsAttributesDialog.getDestinationObjects();

                if (!objectsAttributesDialog.isModifyActionApproved()) {
                    // Do nothing.
                    return;
                }

                // Retain ACL settings from original objects.
                if (!s3ServiceMulti.getObjectACLs(selectedBucket, sourceObjects)) {
                    return;
                }
                for (int i = 0; i < sourceObjects.length; i++) {
                    destinationObjects[i].setAcl(
                        sourceObjects[i].getAcl());
                }

                // Copy objects in-place, to REPLACE their metadata attributes.
                ok = s3ServiceMulti.copyObjects(
                    selectedBucket.getName(), selectedBucket.getName(),
                    sourceObjectKeys, destinationObjects, true);

                // Refresh details for modified objects
                if (ok) {
                    s3ServiceMulti.getObjectsHeads(
                        selectedBucket, destinationObjects);
                }
            }
        });
    }

    /**
     * Lists the buckets in the user's S3 account and refreshes the GUI to display
     * these buckets. Any buckets or objects already listed in the GUI are cleared first.
     */
    private void listAllBuckets() {
        // Remove current bucket and object data from models.
        cachedBuckets.clear();
        bucketsTable.clearSelection();
        bucketTableModel.removeAllBuckets();
        objectTableModel.removeAllObjects();
        final Cockpit myself = this;

        // This is all very convoluted. This was necessary so we can display the status dialog box.
        runInBackgroundThread(new Runnable() {
            public void run() {
                if (!cloudFrontMembershipChecked) {
                    // Check whether the user is signed-up for CloudFront.
                    startProgressDialog("Checking for CloudFront account membership");
                    try {
                        cloudFrontService = new CloudFrontService(
                            s3ServiceMulti.getAWSCredentials(), APPLICATION_DESCRIPTION, myself, null);
                        cloudFrontService.listDistributions();
                    } catch (CloudFrontServiceException e) {
                        stopProgressDialog();

                        if ("OptInRequired".equals(e.getErrorCode())) {
                            log.debug("Your AWS account is not subscribed to the Amazon CloudFront service, "
                                + "you will not be able to manage distributions");
                        }
                        cloudFrontService = null;
                    } finally {
                        stopProgressDialog();
                        cloudFrontMembershipChecked = true;

                        // Update the bucket table to show, or not show, distributions
                        bucketTableModel = new BucketTableModel(cloudFrontService != null);
                        bucketTableModelSorter = new TableSorter(bucketTableModel);
                        bucketsTable.setModel(bucketTableModelSorter);
                        bucketTableModelSorter.setTableHeader(bucketsTable.getTableHeader());

                        if (cloudFrontService != null) {
                            // Set column width for Cloud Front distributions indicator.
                            TableColumn distributionFlagColumn = bucketsTable.getColumnModel().getColumn(1);
                            int distributionFlagColumnWidth = 18;
                            distributionFlagColumn.setPreferredWidth(distributionFlagColumnWidth);
                            distributionFlagColumn.setMaxWidth(distributionFlagColumnWidth);
                            distributionFlagColumn.setMinWidth(0);
                        }

                        manageDistributionsMenuItem.setEnabled(cloudFrontService != null);
                    }
                }

                startProgressDialog("Listing buckets for " + s3ServiceMulti.getAWSCredentials().getAccessKey());
                try {
                    final S3Bucket[] buckets = s3ServiceMulti.getS3Service().listAllBuckets();

                    // Lookup user's CloudFront distributions.
                    Distribution[] distributions = new Distribution[] {};
                    if (cloudFrontService != null) {
                        updateProgressDialog("Listing distributions for " + cloudFrontService.getAWSCredentials().getAccessKey(), "", 0);
                        distributions = cloudFrontService.listDistributions();
                    }
                    final Distribution[] finalDistributions = distributions;

                    runInDispatcherThreadImmediately(new Runnable() {
                        public void run() {
                            for (int i = 0; i < buckets.length; i++) {
                                // Determine whether each bucket has one or more CloudFront distributions.
                                boolean bucketHasDistribution = false;
                                for (int j = 0; j < finalDistributions.length; j++) {
                                    if (finalDistributions[j].getOrigin().equals(buckets[i].getName() + ".s3.amazonaws.com")) {
                                        bucketHasDistribution = true;
                                    }
                                }

                                bucketTableModel.addBucket(buckets[i], bucketHasDistribution);

                                if (i == 0) {
                                    ownerFrame.setTitle(APPLICATION_TITLE + " : " +
                                        buckets[i].getOwner().getDisplayName());
                                }
                            }
                        }
                    });
                } catch (final Exception e) {
                    stopProgressDialog();

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            logoutEvent();

                            String message = "Unable to list your buckets in S3, please log in again";
                            log.error(message, e);
                            ErrorDialog.showDialog(ownerFrame, null, message, e);

                            loginEvent(null);
                        }
                    });
                } finally {
                    stopProgressDialog();
                }
            };
        });
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
     * Actions performed when a bucket is selected in the bucket list table.
     */
    private void bucketSelectedAction() {
        this.currentSelectedBucket = null;

        // Find the selected bucket in the buckets table, if any.
        if (bucketsTable.getSelectedRows().length != 0) {
            this.currentSelectedBucket = bucketTableModel.getBucket(
                bucketTableModelSorter.modelIndex(
                    bucketsTable.getSelectedRows()[0]));
        }

        if (currentSelectedBucket == null) {
            viewBucketPropertiesMenuItem.setEnabled(false);
            refreshBucketMenuItem.setEnabled(true);
            updateBucketACLMenuItem.setEnabled(false);
            updateBucketRequesterPaysStatusMenuItem.setEnabled(false);
            deleteBucketMenuItem.setEnabled(false);

            refreshObjectMenuItem.setEnabled(false);
            uploadFilesMenuItem.setEnabled(false);

            objectTableModel.removeAllObjects();

            objectsTable.getDropTarget().setActive(false);
            objectsTableSP.getDropTarget().setActive(false);
        } else {
            viewBucketPropertiesMenuItem.setEnabled(true);
            refreshBucketMenuItem.setEnabled(true);
            updateBucketACLMenuItem.setEnabled(true);
            updateBucketRequesterPaysStatusMenuItem.setEnabled(true);
            deleteBucketMenuItem.setEnabled(true);

            refreshObjectMenuItem.setEnabled(true);
            uploadFilesMenuItem.setEnabled(true);

            objectsTable.getDropTarget().setActive(true);
            objectsTableSP.getDropTarget().setActive(true);

            if (cachedBuckets.containsKey(currentSelectedBucket.getName())) {
                S3Object[] objects = (S3Object[]) cachedBuckets.get(currentSelectedBucket.getName());

                objectTableModel.removeAllObjects();
                objectTableModel.addObjects(objects);
                updateObjectsSummary(false);
            } else {
                listObjects();
            }
        }
    }

    /**
     * Actions performed when an object is selected in the objects list table.
     */
    private void objectSelectedAction() {
        int count = getSelectedObjects().length;

        updateObjectACLMenuItem.setEnabled(count > 0);
        downloadObjectMenuItem.setEnabled(count > 0);
        deleteObjectMenuItem.setEnabled(count > 0);
        viewOrModifyObjectAttributesMenuItem.setEnabled(count > 0);
        copyObjectsMenuItem.setEnabled(count > 0);
        generatePublicGetUrls.setEnabled(count >= 1 && s3ServiceMulti.getAWSCredentials() != null);
        generateTorrentUrl.setEnabled(count == 1);
    }

    /**
     * Starts a thread to run {@link S3ServiceMulti#listObjects}.
     */
    private void listObjects() {
        if (currentSelectedBucket == null) {
            // Oops, better do nothing.
            return;
        }

        final boolean listingCancelled[] = new boolean[1]; // Default to false.
        final CancelEventTrigger cancelListener = new CancelEventTrigger() {
            private static final long serialVersionUID = 6939193243303189876L;

            public void cancelTask(Object eventSource) {
                listingCancelled[0] = true;
            }
        };

        objectTableModel.removeAllObjects();
        objectsSummaryLabel.setText(" ");

        // This is all very convoluted, it was done this way to ensure we can display the dialog box.
        runInBackgroundThread(new Runnable() {
            public void run() {
                try {
                    startProgressDialog(
                        "Listing objects in " + currentSelectedBucket.getName(),
                        "", 0, 0, "Cancel bucket listing", cancelListener);

                    final String prefix = filterObjectsPrefix.getText();
                    final String delimiter = (String) filterObjectsDelimiter.getSelectedItem();

                    final ArrayList allObjects = new ArrayList();
                    String priorLastKey = null;
                    do {
                        StorageObjectsChunk chunk = s3ServiceMulti.getS3Service().listObjectsChunked(
                            currentSelectedBucket.getName(), prefix, delimiter,
                            BUCKET_LIST_CHUNKING_SIZE, priorLastKey);

                        final S3Object[] objects = S3Object.cast(chunk.getObjects());
                        for (int i = 0; i < objects.length; i++) {
                            objects[i].setOwner(currentSelectedBucket.getOwner());
                        }

                        priorLastKey = chunk.getPriorLastKey();
                        allObjects.addAll(Arrays.asList(objects));

                        updateProgressDialog(
                            "Listed " + allObjects.size() + " objects in "
                            + currentSelectedBucket.getName(), "", 0);

                        runInDispatcherThreadImmediately(new Runnable() {
                            public void run() {
                                objectTableModel.addObjects(objects);
                                updateObjectsSummary(true);
                            }
                        });
                    } while (!listingCancelled[0] && priorLastKey != null);

                    runInDispatcherThreadImmediately(new Runnable() {
                        public void run() {
                            updateObjectsSummary(listingCancelled[0]);
                            S3Object[] allObjects = objectTableModel.getObjects();
                            cachedBuckets.put(currentSelectedBucket.getName(), allObjects);
                        }
                    });

                } catch (final Exception e) {
                    stopProgressDialog();

                    String message = "Unable to list objects";
                    log.error(message, e);
                    ErrorDialog.showDialog(ownerFrame, null, message, e);
                } finally {
                    stopProgressDialog();
                }
            };
        });
    }

    /**
     * Updates the summary text shown below the listing of objects, which details the
     * number and total size of the objects.
     *
     */
    private void updateObjectsSummary(boolean isIncompleteListing) {
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
                    summary += " - <font color=\"blue\">Filtered</font>";
                }
                if (isIncompleteListing) {
                    summary += " - <font color=\"red\">Incomplete</font>";
                }
                summary += "</html>";
            }

            objectsSummaryLabel.setText(summary);
        } catch (Throwable t) {
            String message = "Unable to update object list summary";
            log.error(message, t);
            ErrorDialog.showDialog(ownerFrame, this, message, t);
        }
    }

    /**
     * Displays bucket-specific actions in a popup menu.
     * @param invoker the component near which the popup menu will be displayed
     * @param xPos the mouse's horizontal co-ordinate when the popup menu was invoked
     * @param yPos the mouse's vertical co-ordinate when the popup menu was invoked
     */
    private void showBucketPopupMenu(JComponent invoker, int xPos, int yPos) {
        if (s3ServiceMulti == null) {
            return;
        }
        bucketActionMenu.show(invoker, xPos, yPos);
    }

    /**
     * Displays object-specific actions in a popup menu.
     * @param invoker the component near which the popup menu will be displayed
     * @param xPos the mouse's horizontal co-ordinate when the popup menu was invoked
     * @param yPos the mouse's vertical co-ordinate when the popup menu was invoked
     */
    private void showObjectPopupMenu(JComponent invoker, int xPos, int yPos) {
        if (currentSelectedBucket == null || getSelectedObjects().length == 0) {
            return;
        }
        objectActionMenu.show(invoker, xPos, yPos);
    }

    /**
     * Action to create a new bucket in S3 after prompting the user for a bucket name.
     *
     */
    private void createBucketAction() {
        String proposedNewName =
            s3ServiceMulti.getAWSCredentials().getAccessKey().toLowerCase()
            + "." + "bucket-name";

        CreateBucketDialog dialog = new CreateBucketDialog(proposedNewName, ownerFrame, this);
        dialog.setVisible(true);

        if (!dialog.getOkClicked()) {
            return;
        }

        final S3Bucket newBucket = new S3Bucket(dialog.getBucketName(), dialog.getBucketLocation());
        dialog.dispose();

        runInBackgroundThread(new Runnable() {
           public void run() {
               if (s3ServiceMulti.createBuckets(new S3Bucket[] { newBucket })) {
                   int modelIndex = bucketTableModel.getBucketIndexByName(newBucket.getName());
                   int viewIndex = bucketTableModelSorter.viewIndex(modelIndex);
                   bucketsTable.setRowSelectionInterval(viewIndex, viewIndex);
               }
           }
        });
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>CreateBucketsEvent</code>.
     * <p>
     * When a bucket is successfully created it is added to the listing of buckets.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final CreateBucketsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            startProgressDialog(
                "Creating " + event.getThreadWatcher().getThreadCount() + " buckets",
                "", 0, (int) event.getThreadWatcher().getThreadCount(),
                "Cancel bucket creation", event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            runInDispatcherThreadImmediately(new Runnable() {
                public void run() {
                    for (int i = 0; i < event.getCreatedBuckets().length; i++) {
                        bucketTableModel.addBucket(event.getCreatedBuckets()[i], false);
                    }
                }
            });

            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Created " + progressStatus.getCompletedThreads() + " buckets of " + progressStatus.getThreadCount();
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to create a bucket";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    /**
     * Deletes the bucket currently selected in the gui.
     *
     */
    private void deleteSelectedBucket() {
        if (currentSelectedBucket == null) {
            log.warn("Ignoring delete bucket command, no currently selected bucket");
            return;
        }

        int response = JOptionPane.showConfirmDialog(ownerFrame,
            "Are you sure you want to delete '" + currentSelectedBucket.getName() + "'?",
            "Delete Bucket?", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.NO_OPTION) {
            return;
        }

        try {
            s3ServiceMulti.getS3Service().deleteBucket(currentSelectedBucket.getName());
            bucketTableModel.removeBucket(currentSelectedBucket);
            currentSelectedBucket = null;
        } catch (Exception e) {
            String message = "Unable to delete bucket";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    /**
     * Adds a bucket not owned by the current S3 user to the bucket listing, after
     * prompting the user for the name of the bucket to add.
     * To be added in this way, the third-party bucket must be publicly available.
     *
     */
    private void addThirdPartyBucket() {
        try {
            String bucketName = JOptionPane.showInputDialog(ownerFrame,
                "Name for third-party bucket:",
                "Add a third-party bucket", JOptionPane.QUESTION_MESSAGE);

            if (bucketName != null) {
                if (s3ServiceMulti.getS3Service().isBucketAccessible(bucketName)) {
                    S3Bucket thirdPartyBucket = new S3Bucket(bucketName);
                    bucketTableModel.addBucket(thirdPartyBucket, false);
                } else {
                    String message = "Unable to access third-party bucket: " + bucketName;
                    log.error(message);
                    ErrorDialog.showDialog(ownerFrame, this, message, null);
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            String message = "Unable to access third-party bucket";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    /**
     * Updates the ACL settings for the currently selected bucket.
     */
    private void updateBucketAccessControlList() {
        try {
            AccessControlList bucketACL = s3ServiceMulti.getS3Service().getBucketAcl(currentSelectedBucket);

            AccessControlList updatedBucketACL = AccessControlDialog.showDialog(
                ownerFrame, new S3Bucket[] {currentSelectedBucket}, bucketACL, this);
            if (updatedBucketACL != null) {
                currentSelectedBucket.setAcl(updatedBucketACL);
                s3ServiceMulti.getS3Service().putBucketAcl(currentSelectedBucket);
            }
        } catch (Exception e) {
            String message = "Unable to update bucket's Access Control List";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    /**
     * Updates the ACL settings for the currently selected bucket.
     */
    private void updateBucketRequesterPaysSetting() {
        try {
            final S3Bucket selectedBucket = currentSelectedBucket;

            if (!selectedBucket.isRequesterPaysKnown()) {
                selectedBucket.setRequesterPays(
                    s3ServiceMulti.getS3Service().isRequesterPaysBucket(
                        selectedBucket.getName()));
            }

            boolean originalRequesterPaysFlag = selectedBucket.isRequesterPays();

            RequesterPaysDialog dialog = new RequesterPaysDialog(selectedBucket, ownerFrame, this);
            dialog.setVisible(true);

            if (!dialog.getOkClicked()) {
                return;
            }

            final boolean newRequesterPaysFlag = dialog.isRequesterPaysSelected();
            dialog.dispose();

            if (newRequesterPaysFlag != originalRequesterPaysFlag) {
                runInBackgroundThread(new Runnable() {
                    public void run() {
                        try {
                            s3ServiceMulti.getS3Service().setRequesterPaysBucket(
                                selectedBucket.getName(), newRequesterPaysFlag);
                            selectedBucket.setRequesterPays(newRequesterPaysFlag);
                        } catch (final Exception e) {
                            String message = "Unable to update Requester Pays status";
                            log.error(message, e);
                            ErrorDialog.showDialog(ownerFrame, null, message, e);
                        }
                    }
                 });
            }
        } catch (Exception e) {
            String message = "Unable to update bucket's Access Control List";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
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

    private void displayAclModificationDialog() {
        final HyperlinkActivatedListener hyperlinkListener = this;
        final S3Bucket selectedBucket = currentSelectedBucket;

        runInBackgroundThread(new Runnable() {
            public void run() {
                final S3Object[] selectedObjects = getSelectedObjects();

                boolean aclLookupSucceeded = s3ServiceMulti.getObjectACLs(
                    selectedBucket, selectedObjects);

                if (!aclLookupSucceeded) {
                    return;
                }

                final AccessControlList[] updatedObjectACL = new AccessControlList[] {null};

                runInDispatcherThreadImmediately(new Runnable() {
                    public void run() {
                        // Build merged ACL containing ALL relevant permissions
                        AccessControlList mergedACL = new AccessControlList();
                        for (int i = 0; i < selectedObjects.length; i++) {
                            AccessControlList objectACL = selectedObjects[i].getAcl();
                            mergedACL.grantAllPermissions(objectACL.getGrantAndPermissions());

                            // BEWARE! Here we assume that all the objects have the same owner...
                            if (mergedACL.getOwner() == null) {
                                mergedACL.setOwner(objectACL.getOwner());
                            }
                        }

                        // Show ACL dialog box for user to change ACL settings for all objects.
                        updatedObjectACL[0] = AccessControlDialog.showDialog(
                            ownerFrame, selectedObjects, mergedACL, hyperlinkListener);
                    }
                });

                if (updatedObjectACL[0] != null) {
                    // Update ACLs for each object.
                    for (int i = 0; i < selectedObjects.length; i++) {
                        selectedObjects[i].setAcl(updatedObjectACL[0]);
                    }
                    // Perform ACL updates.
                    s3ServiceMulti.putACLs(selectedBucket, selectedObjects);
                }
            }
        });
    }


    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>LookupACLEvent</code>.
     * <p>
     * The ACL details are retrieved for the currently selected objects in the gui, then the
     * {@link AccessControlDialog} is displayed to allow the user to update the ACL settings
     * for these objects.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(LookupACLEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            startProgressDialog(
                "Retrieved 0 of " + event.getThreadWatcher().getThreadCount() + " ACLs",
                "", 0, (int) event.getThreadWatcher().getThreadCount(), "Cancel Lookup",
                event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Retrieved " + progressStatus.getCompletedThreads() + " of " + progressStatus.getThreadCount() + " ACLs";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to lookup Access Control list for objects";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>UpdateACLEvent</code>.
     * <p>
     * This method merely updates the progress dialog as ACLs are updated.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(UpdateACLEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            startProgressDialog(
                "Updated 0 of " + event.getThreadWatcher().getThreadCount() + " ACLs",
                "", 0, (int) event.getThreadWatcher().getThreadCount(), "Cancel Update",
                event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Updated " + progressStatus.getCompletedThreads() + " of " + progressStatus.getThreadCount() + " ACLs";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to update Access Control Lists";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    /**
     * Downloads the objects currently selected in the objects table. The user is
     * prompted
     * Prepares to perform a download of objects from S3 by prompting the user for a directory
     * to store the files in, then performing the download.
     *
     * @throws IOException
     */
    private void downloadSelectedObjects() {
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

        boolean storeEmptyDirectories = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
            .getBoolProperty("uploads.storeEmptyDirectories", true);
        final Map<String, String> objectKeyToFilepathMap = FileComparer.getInstance()
            .buildObjectKeyToFilepathMap(downloadDirectory.listFiles(), "", storeEmptyDirectories);

        // Build map of S3 Objects being downloaded.
        final Map s3DownloadObjectsMap = FileComparer.getInstance()
            .populateObjectMap("", getSelectedObjects());

        final HyperlinkActivatedListener hyperlinkListener = this;

        runInBackgroundThread(new Runnable() {
            public void run() {
                // Retrieve details of objects for download
                if (!retrieveObjectsDetails(getSelectedObjects())) {
                    return;
                }

                try {
                    final FileComparerResults comparisonResults = compareRemoteAndLocalFiles(
                        objectKeyToFilepathMap, s3DownloadObjectsMap);

                    DownloadPackage[] downloadPackages =
                        buildDownloadPackageList(comparisonResults, s3DownloadObjectsMap);
                    if (downloadPackages == null) {
                        return;
                    }

                    s3ServiceMulti.downloadObjects(currentSelectedBucket, downloadPackages);

                } catch (final Exception e) {
                    runInDispatcherThreadImmediately(new Runnable() {
                        public void run() {
                            String message = "Unable to download objects";
                            log.error(message, e);
                            ErrorDialog.showDialog(ownerFrame,
                                hyperlinkListener, message, e);
                        }
                    });
                }
            }
        });
    }

    private void uploadFiles(File[] uploadFiles) {
        // Fail if encryption is turned on but no password is available.
        if (cockpitPreferences.isUploadEncryptionActive()
            && !cockpitPreferences.isEncryptionPasswordSet())
        {
            ErrorDialog.showDialog(ownerFrame, this,
                "Upload encryption is enabled but you have not yet set a password in the Encryption Preferences.",
                null);
            return;

        }

        try {
            // Build map of files proposed for upload.
            boolean storeEmptyDirectories = Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
                .getBoolProperty("uploads.storeEmptyDirectories", true);
            final Map<String, String> objectKeyToFilepathMap = FileComparer.getInstance()
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

            final Map s3ExistingObjectsMap = FileComparer.getInstance()
                .populateObjectMap("", existingObjects);

            final HyperlinkActivatedListener hyperlinkListener = this;
            final S3Object[] clashingObjects = existingObjects;

            runInBackgroundThread(new Runnable() {
               public void run() {
                   if (clashingObjects.length > 0) {
                       // Retrieve details of potential clashes
                       if (!retrieveObjectsDetails(clashingObjects)) {
                           return;
                       }
                   }

                   try {
                       FileComparerResults comparisonResults =
                           compareRemoteAndLocalFiles(objectKeyToFilepathMap, s3ExistingObjectsMap);

                        S3Object[] uploadObjects = buildUploadObjectsList(
                            comparisonResults, objectKeyToFilepathMap);

                        if (uploadObjects == null) {
                            return;
                        }

                        // Upload the files.
                        s3ServiceMulti.putObjects(currentSelectedBucket, uploadObjects);

                   } catch (final Exception e) {
                       runInDispatcherThreadImmediately(new Runnable() {
                           public void run() {
                               String message = "Unable to upload objects";
                               log.error(message, e);
                               ErrorDialog.showDialog(ownerFrame, hyperlinkListener, message, e);
                           }
                       });
                   }
               }
            });

        } catch (Exception e) {
            String message = "Unable to upload objects";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    private FileComparerResults compareRemoteAndLocalFiles(
        final Map<String, String> objectKeyToFilepathMap, final Map s3ObjectsMap)
        throws Exception
    {
        try {
            // Compare objects being downloaded and existing local files.
            final String statusText =
                "Comparing " + s3ObjectsMap.size() + " object" + (s3ObjectsMap.size() > 1 ? "s" : "") +
                " in S3 with " + objectKeyToFilepathMap.size()
                + " local file" + (objectKeyToFilepathMap.size() > 1 ? "s" : "");
            startProgressDialog(statusText, "", 0, 100, null, null);

            // Calculate total files size.
            final long filesSizeTotal[] = new long[1];
            for (String filepath: objectKeyToFilepathMap.values()) {
                filesSizeTotal[0] += (new File(filepath)).length();
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

            return comparisonResults;
        } finally {
            stopProgressDialog();
        }
    }

    /**
     * Performs the real work of downloading files by comparing the download candidates against
     * existing files, prompting the user whether to overwrite any pre-existing file versions,
     * and starting {@link S3ServiceMulti#downloadObjects} where the real work is done.
     *
     */
    private DownloadPackage[] buildDownloadPackageList(FileComparerResults comparisonResults,
        Map s3DownloadObjectsMap) throws Exception
    {
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
                + " objects being downloaded:\n\n";

            if (newFiles > 0) {
                message += newFiles + " files are new.\n\n";
                options.add(DOWNLOAD_NEW_FILES_ONLY);
            }
            if (changedFiles > 0) {
                message += changedFiles + " files have changed.\n\n";
                options.add(DOWNLOAD_NEW_AND_CHANGED_FILES);
            }
            if (unchangedFiles > 0) {
                message += unchangedFiles + " files already exist and are unchanged.\n\n";
                options.add(DOWNLOAD_ALL_FILES);
            }
            message += "Please choose which files you wish to download:";

            Object response = JOptionPane.showInputDialog(
                ownerFrame, message, "Replace files?", JOptionPane.QUESTION_MESSAGE,
                null, options.toArray(), DOWNLOAD_NEW_AND_CHANGED_FILES);

            if (response == null) {
                return null;
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
                return null;
            }
        }

        log.debug("Downloading " + objectKeysForDownload.size() + " objects");
        if (objectKeysForDownload.size() == 0) {
            return null;
        }

        // Create array of objects for download.
        S3Object[] objects = new S3Object[objectKeysForDownload.size()];
        int objectIndex = 0;
        for (Iterator iter = objectKeysForDownload.iterator(); iter.hasNext();) {
            objects[objectIndex++] = (S3Object) s3DownloadObjectsMap.get(iter.next());
        }

        Map downloadObjectsToFileMap = new HashMap();
        ArrayList downloadPackageList = new ArrayList();

        // Setup files to write to, creating parent directories when necessary.
        for (int i = 0; i < objects.length; i++) {
            File file = new File(downloadDirectory, objects[i].getKey());

            // Encryption password must be null if no password is set.
            String encryptionPassword = null;
            if (cockpitPreferences.isEncryptionPasswordSet()) {
                encryptionPassword = cockpitPreferences.getEncryptionPassword();
            }

            // Create local directories corresponding to objects flagged as dirs.
            if (objects[i].isDirectoryPlaceholder()) {
                file = new File(downloadDirectory,
                    ObjectUtils.convertDirPlaceholderKeyNameToDirName(
                        objects[i].getKey()));
                file.mkdirs();
            }

            DownloadPackage downloadPackage = ObjectUtils
                .createPackageForDownload(objects[i], file, true, true, encryptionPassword);

            if (downloadPackage != null) {
                downloadObjectsToFileMap.put(objects[i].getKey(), file);
                downloadPackageList.add(downloadPackage);
            }

        }

        return (DownloadPackage[]) downloadPackageList
            .toArray(new DownloadPackage[downloadPackageList.size()]);
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
                startProgressDialog("Downloaded " +
                    watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                    byteFormatter.formatByteSize(watcher.getBytesTransferred())
                    + " of " + byteFormatter.formatByteSize(watcher.getBytesTotal()),
                    "", 0, 100, "Cancel Download",
                    watcher.getCancelEventListener());
            // ... otherwise just show the number of completed threads.
            } else {
                startProgressDialog("Downloaded " + watcher.getCompletedThreads()
                    + " of " + watcher.getThreadCount() + " objects",
                    "", 0, (int) watcher.getThreadCount(),  "Cancel Download",
                    watcher.getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                String bytesCompletedStr = byteFormatter.formatByteSize(watcher.getBytesTransferred());
                String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
                String statusText = "Downloaded " +
                    watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                    bytesCompletedStr + " of " + bytesTotalStr;

                String detailsText = formatTransferDetails(watcher);

                int percentage = (int)
                    (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);
                updateProgressDialog(statusText, detailsText, percentage);
            }
            // ... otherwise just show the number of completed threads.
            else {
                ThreadWatcher progressStatus = event.getThreadWatcher();
                String statusText = "Downloaded " + progressStatus.getCompletedThreads()
                    + " of " + progressStatus.getThreadCount() + " objects";
                updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
            }
        } else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to download objects";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }


    private S3Object[] buildUploadObjectsList(FileComparerResults comparisonResults,
        Map<String, String> objectKeyToFilepathMap) throws Exception
    {
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
                + " files being uploaded:\n\n";

            if (newFiles > 0) {
                message += newFiles + " files are new.\n\n";
                options.add(UPLOAD_NEW_FILES_ONLY);
            }
            if (changedFiles > 0) {
                message += changedFiles + " files have changed.\n\n";
                options.add(UPLOAD_NEW_AND_CHANGED_FILES);
            }
            if (unchangedFiles > 0) {
                message += unchangedFiles + " files already exist and are unchanged.\n\n";
                options.add(UPLOAD_ALL_FILES);
            }
            message += "Please choose which files you wish to upload:";

            Object response = JOptionPane.showInputDialog(
                ownerFrame, message, "Replace files?", JOptionPane.QUESTION_MESSAGE,
                null, options.toArray(), UPLOAD_NEW_AND_CHANGED_FILES);

            if (response == null) {
                return null;
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
                return null;
            }
        }

        if (fileKeysForUpload.size() == 0) {
            return null;
        }

        final String[] statusText = new String[1];
        statusText[0] = "Prepared 0 of " + fileKeysForUpload.size() + " files for upload";
        startProgressDialog(statusText[0], "", 0, 100, null, null);

        long bytesToProcess = 0;
        for (Iterator iter = fileKeysForUpload.iterator(); iter.hasNext();) {
            File file = new File(objectKeyToFilepathMap.get(iter.next().toString()));
            bytesToProcess += file.length() *
                (cockpitPreferences.isUploadEncryptionActive() || cockpitPreferences.isUploadCompressionActive() ? 3 : 1);
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
                .createObjectForUpload(fileKey, file,
                    (cockpitPreferences.isUploadEncryptionActive() ? encryptionUtil : null),
                    cockpitPreferences.isUploadCompressionActive(), progressWatcher);

            String aclPreferenceString = cockpitPreferences.getUploadACLPermission();
            if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PRIVATE.equals(aclPreferenceString)) {
                // Objects are private by default, nothing more to do.
            } else if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ.equals(aclPreferenceString)) {
                newObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
            } else if (CockpitPreferences.UPLOAD_ACL_PERMISSION_PUBLIC_READ_WRITE.equals(aclPreferenceString)) {
                newObject.setAcl(AccessControlList.REST_CANNED_PUBLIC_READ_WRITE);
            } else {
                log.warn("Ignoring unrecognised upload ACL permission setting: " + aclPreferenceString);
            }

            newObject.setStorageClass(cockpitPreferences.getUploadStorageClass());

            statusText[0] = "Prepared " + (objectIndex + 1)
                + " of " + fileKeysForUpload.size() + " files for upload";

            objects[objectIndex++] = newObject;
        }

        stopProgressDialog();

        return objects;
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
                String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
                String statusText = "Uploaded " +
                    watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                    "0 of " + bytesTotalStr;
                startProgressDialog(statusText, " ", 0, 100, "Cancel Upload",
                    event.getThreadWatcher().getCancelEventListener());
            }
            // ... otherwise show the number of completed threads.
            else {
                startProgressDialog(
                    "Uploaded 0 of " + watcher.getThreadCount() + " objects",
                    "", (int) watcher.getCompletedThreads(), (int) watcher.getThreadCount(),
                    "Cancel upload", event.getThreadWatcher().getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    for (int i = 0; i < event.getCreatedObjects().length; i++) {
                        S3Object object = event.getCreatedObjects()[i];
                        object.setBucketName(currentSelectedBucket.getName());
                        objectTableModel.addObject(object);
                    }
                    if (event.getCreatedObjects().length > 0) {
                        updateObjectsSummary(true);
                    }
                }
            });

            ThreadWatcher watcher = event.getThreadWatcher();

            // Show percentage of bytes transferred, if this info is available.
            if (watcher.isBytesTransferredInfoAvailable()) {
                if (watcher.getBytesTransferred() >= watcher.getBytesTotal()) {
                    // Upload is completed, just waiting on resonse from S3.
                    String statusText = "Upload completed, awaiting confirmation";
                    updateProgressDialog(statusText, "", 100);
                } else {
                    String bytesCompletedStr = byteFormatter.formatByteSize(watcher.getBytesTransferred());
                    String bytesTotalStr = byteFormatter.formatByteSize(watcher.getBytesTotal());
                    String statusText = "Uploaded " +
                        watcher.getCompletedThreads() + "/" + watcher.getThreadCount() + " - " +
                        bytesCompletedStr + " of " + bytesTotalStr;
                    int percentage = (int)
                        (((double)watcher.getBytesTransferred() / watcher.getBytesTotal()) * 100);

                    String detailsText = formatTransferDetails(watcher);

                    updateProgressDialog(statusText, detailsText, percentage);
                }
            }
            // ... otherwise show the number of completed threads.
            else {
                ThreadWatcher progressStatus = event.getThreadWatcher();
                String statusText = "Uploaded " + progressStatus.getCompletedThreads() +
                    " of " + progressStatus.getThreadCount() + " objects";
                updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
            }
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();

            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateObjectsSummary(false);
                    S3Object[] allObjects = objectTableModel.getObjects();
                    cachedBuckets.put(currentSelectedBucket.getName(), allObjects);
                }
            });
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateObjectsSummary(false);
                }
            });

            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to upload objects";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    private void copyObjects() {
        try {
            final S3Object[] sourceObjects = getSelectedObjects();

            CopyObjectsDialog dialog = new CopyObjectsDialog(ownerFrame,
                "Copy or Move Objects", skinsFactory,
                sourceObjects, bucketTableModel.getBuckets());

            dialog.setVisible(true);
            if (dialog.isCopyActionApproved()) {
                final String currentBucketName = currentSelectedBucket.getName();
                final String destinationBucketName = dialog.getDestinationBucketName();
                final String[] sourceObjectKeys = dialog.getSourceObjectKeys();
                final S3Object[] destinationObjects = dialog.getDestinationObjects();
                final boolean isDeleteAfterCopy = dialog.isMoveOptionSelected();
                final boolean retainAcls = dialog.isCopyOriginalAccessControlLists();
                dialog.dispose();

                if (!destinationBucketName.equals(currentBucketName)) {
                    cachedBuckets.remove(destinationBucketName);
                }

                runInBackgroundThread(new Runnable() {
                    public void run() {
                        if (retainAcls) {
                            // Retain ACL settings from original objects.
                            if (!s3ServiceMulti.getObjectACLs(
                                currentSelectedBucket, sourceObjects))
                            {
                                return;
                            }
                            for (int i = 0; i < sourceObjects.length; i++) {
                                destinationObjects[i].setAcl(
                                    sourceObjects[i].getAcl());
                            }
                        }

                        // Copy objects. Metadata is retained, not replaced.
                        s3ServiceMulti.copyObjects(currentBucketName, destinationBucketName,
                            sourceObjectKeys, destinationObjects, false);

                        if (isDeleteAfterCopy) {
                            final S3Object[] sourceObjects = new S3Object[sourceObjectKeys.length];
                            for (int i = 0; i < sourceObjectKeys.length; i++) {
                                sourceObjects[i] = new S3Object(sourceObjectKeys[i]);
                            }

                            s3ServiceMulti.deleteObjects(currentSelectedBucket, sourceObjects);
                        }
                        if (destinationBucketName.equals(currentBucketName) || isDeleteAfterCopy) {
                            // Refesh object listing for current bucket if the bucket's contents
                            // have changed.
                            listObjects();
                        }
                    }
                });

            } else {
                dialog.dispose();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            stopProgressDialog();
            String message = "Unable to modify objects";
            log.error(message, e);
            ErrorDialog.showDialog(ownerFrame, this, message, e);
        }
    }

    /**
     * This method is an {@link S3ServiceEventListener} action method that is invoked when this
     * application's <code>S3ServiceMulti</code> triggers a <code>CopyObjectsEvent</code>.
     * <p>
     * This method merely updates the progress dialog as objects are copied.
     *
     * @param event
     */
    public void s3ServiceEventPerformed(final CopyObjectsEvent event) {
        if (ServiceEvent.EVENT_STARTED == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();
            startProgressDialog("Copied 0 of " + watcher.getThreadCount() + " objects",
                "", 0, (int) watcher.getThreadCount(),
                "Cancel Copy", event.getThreadWatcher().getCancelEventListener());
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            ThreadWatcher watcher = event.getThreadWatcher();
            String statusText = "Copied " + watcher.getCompletedThreads()
                + " of " + watcher.getThreadCount() + " objects";
            updateProgressDialog(statusText, "", (int) watcher.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()
            || ServiceEvent.EVENT_CANCELLED == event.getEventCode())
        {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to copy objects";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    private void generatePublicGetUrls() {
        final S3Object[] objects = getSelectedObjects();

        if (objects.length < 1) {
            log.warn("Ignoring Generate Public URLs object command because no objects are selected");
            return;
        }

        SignedGetUrlDialog dialog = new SignedGetUrlDialog(ownerFrame, this,
            s3ServiceMulti.getS3Service(), objects);
        dialog.setVisible(true);
    }

    private void generateTorrentUrl() {
        final S3Object[] objects = getSelectedObjects();

        if (objects.length != 1) {
            log.warn("Ignoring Generate Public URL object command, can only operate on a single object");
            return;
        }
        S3Object currentObject = objects[0];

        // Generate URL
        String torrentUrl = s3ServiceMulti.getS3Service().createTorrentUrl(
            currentSelectedBucket.getName(), currentObject.getKey());

        // Display signed URL
        JOptionPane.showInputDialog(ownerFrame,
            "Torrent URL for '" + currentObject.getKey() + "'.",
            "Torrent URL", JOptionPane.INFORMATION_MESSAGE, null, null, torrentUrl);
    }

    private void deleteSelectedObjects() {
        final S3Object[] objects = getSelectedObjects();

        if (objects.length == 0) {
            log.warn("Ignoring delete objects command, no currently selected objects");
            return;
        }

        int response = JOptionPane.showConfirmDialog(ownerFrame,
            (objects.length == 1 ?
                "Are you sure you want to delete '" + objects[0].getKey() + "'?" :
                "Are you sure you want to delete " + objects.length + " objects"
            ),
            "Delete Objects?", JOptionPane.YES_NO_OPTION);

        if (response == JOptionPane.NO_OPTION) {
            return;
        }

        runInBackgroundThread(new Runnable() {
           public void run() {
               s3ServiceMulti.deleteObjects(currentSelectedBucket, objects);

               runInDispatcherThreadImmediately(new Runnable() {
                   public void run() {
                       updateObjectsSummary(false);
                       S3Object[] allObjects = objectTableModel.getObjects();
                       cachedBuckets.put(currentSelectedBucket.getName(), allObjects);
                   }
               });
           }
        });
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
                "Deleted 0 of " + event.getThreadWatcher().getThreadCount() + " objects",
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
                    if (event.getDeletedObjects().length > 0) {
                        updateObjectsSummary(true);
                    }
                }
            });

            ThreadWatcher progressStatus = event.getThreadWatcher();
            String statusText = "Deleted " + progressStatus.getCompletedThreads()
                + " of " + progressStatus.getThreadCount() + " objects";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to delete objects";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    /**
     * Retrieves details about objects including metadata etc by invoking the method
     * {@link S3ServiceMulti#getObjectsHeads}.
     *
     * This is generally done as a prelude to some further action, such as
     * displaying the objects' details or downloading the objects.
     * The real action occurs in the method <code>s3ServiceEventPerformed</code> for handling
     * <code>GetObjectHeadsEvent</code> events.
     *
     * @param candidateObjects
     *
     * @return
     * true if objects details were successfully retrieved.
     */
    private boolean retrieveObjectsDetails(final S3Object[] candidateObjects) {
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

        return s3ServiceMulti.getObjectsHeads(currentSelectedBucket, incompleteObjects);
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
                    + event.getThreadWatcher().getThreadCount() + " objects",
                    "", 0, (int) event.getThreadWatcher().getThreadCount(), "Cancel Retrieval",
                    event.getThreadWatcher().getCancelEventListener());
            }
        }
        else if (ServiceEvent.EVENT_IN_PROGRESS == event.getEventCode()) {
            final ThreadWatcher progressStatus = event.getThreadWatcher();

            // Store detail-complete objects in table.
            runInDispatcherThreadImmediately(new Runnable() {
                public void run() {
                    // Update object in table with the retrieved details.
                    for (int i = 0; i < event.getCompletedObjects().length; i++) {
                        S3Object objectWithDetails = event.getCompletedObjects()[i];
                        S3Object originalObject = objectTableModel.getObjectByKey(
                            objectWithDetails.getKey());

                        originalObject.replaceAllMetadata(objectWithDetails.getMetadataMap());
                        originalObject.setMetadataComplete(true);
                        log.debug("Updated table with " + originalObject.getKey()
                            + ", content-type=" + originalObject.getContentType());
                    }
                }
            });

            // Update progress of GetObject requests.
            String statusText = "Retrieved details for " + progressStatus.getCompletedThreads()
                + " of " + progressStatus.getThreadCount() + " objects";
            updateProgressDialog(statusText, "", (int) progressStatus.getCompletedThreads());
        }
        else if (ServiceEvent.EVENT_COMPLETED == event.getEventCode()) {
            // Stop GetObjectHead progress display.
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_CANCELLED == event.getEventCode()) {
            stopProgressDialog();
        }
        else if (ServiceEvent.EVENT_ERROR == event.getEventCode()) {
            stopProgressDialog();

            String message = "Unable to retrieve objects details";
            log.error(message, event.getErrorCause());
            ErrorDialog.showDialog(ownerFrame, this, message, event.getErrorCause());
        }
    }

    private String formatTransferDetails(ThreadWatcher watcher) {
        long bytesPerSecond = watcher.getBytesPerSecond();
        String detailsText = byteFormatter.formatByteSize(bytesPerSecond) + "/s";

        if (watcher.isTimeRemainingAvailable()) {
            long secondsRemaining = watcher.getTimeRemaining();
            detailsText += " - Time remaining: " + timeFormatter.formatTime(secondsRemaining);
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
            timeFormatter.formatTime(secondsRemaining);
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
                //if (authscheme instanceof NTLMScheme) {
                AuthenticationDialog pwDialog = new AuthenticationDialog(ownerFrame,
                        "Authentication Required",
                        "<html>Host <b>" + scope.getHost() + ":"
                                + scope.getPort()
                                + "</b> requires Windows authentication</html>",
                        true);
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
                    credentials = new UsernamePasswordCredentials(pwDialog.getUser(),
                            pwDialog.getPassword());
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

    private boolean isObjectFilteringActive() {
        if (!filterObjectsCheckBox.isSelected()) {
            return false;
        } else {
            String delimiter = (String) filterObjectsDelimiter.getSelectedItem();
            if (filterObjectsPrefix.getText().length() > 0
                || delimiter.length() > 0)
            {
                return true;
            } else {
                return false;
            }
        }
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
                if (e.getSource().equals(bucketsTable)) {
                    showBucketPopupMenu((JComponent)e.getSource(), e.getX(), e.getY());
                } else if (e.getSource().equals(objectsTable)) {
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
        // When running on OS X, display app menu in the right place (i.e. not the app window)
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        JFrame ownerFrame = new JFrame("JetS3t Cockpit");
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

        new Cockpit(ownerFrame);
    }

}
