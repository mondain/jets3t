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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jets3t.gui.GuiUtils;
import org.jets3t.gui.HyperlinkActivatedListener;
import org.jets3t.gui.JHtmlLabel;
import org.jets3t.gui.TableSorter;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.acl.CanonicalGrantee;
import org.jets3t.service.acl.EmailAddressGrantee;
import org.jets3t.service.acl.GrantAndPermission;
import org.jets3t.service.acl.GranteeInterface;
import org.jets3t.service.acl.GroupGrantee;
import org.jets3t.service.acl.Permission;
import org.jets3t.service.model.BaseStorageItem;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;
import org.jets3t.service.model.S3Owner;

/**
 * Dialog for managing S3 access control settings for buckets and objects.
 * <p>
 * All S3 group types are supported:
 * <ul>
 * <li>Canonical Users</li>
 * <li>Groups: All Users, Authenticated Users, and Amazon S3 Log Writers</li>
 * <li>Users identified by Email address</li>
 * </ul>
 * <p>
 * The following access permissions are supported:
 * <ul>
 * <li>READ</li>
 * <li>WRITE</li>
 * <li>READ_ACP</li>
 * <li>WRITE_ACP</li>
 * <li>FULL_CONTROL</li>
 * </ul>
 *
 * @author James Murty
 */
public class AccessControlDialog extends JDialog implements ActionListener {
    private static final long serialVersionUID = -6621927508514378546L;

    private final GuiUtils guiUtils = new GuiUtils();
    private static AccessControlDialog accessControlDialog = null;

    private HyperlinkActivatedListener hyperlinkListener = null;

    private AccessControlList originalAccessControlList = null;
    private AccessControlList updatedAccessControlList = null;

    private JHtmlLabel itemsDescription = null;
    private JTable canonicalGranteeTable = null;
    private GranteeTableModel canonicalGranteeTableModel = null;
    private JTable emailGranteeTable = null;
    private GranteeTableModel emailGranteeTableModel = null;
    private JTable groupGranteeTable = null;
    private GranteeTableModel groupGranteeTableModel = null;

    private static final String[] canonicalUserTableColumnNames = new String[] {
        "Canonical ID", "Display Name", "Permission"
    };

    private static final String[] groupTableColumnNames = new String[] {
        "Group URI", "Permission"
    };

    private static final String[] emailTableColumnNames = new String[] {
        "Email Address", "Permission"
    };

    /**
     * The set of access permission values.
     */
    private final JComboBox permissionComboBox = new JComboBox(new Permission[] {
        Permission.PERMISSION_READ,
        Permission.PERMISSION_WRITE,
        Permission.PERMISSION_FULL_CONTROL,
        Permission.PERMISSION_READ_ACP,
        Permission.PERMISSION_WRITE_ACP
        });

    /**
     * The set of groups.
     */
    private final JComboBox groupGranteeComboBox = new JComboBox(new GroupGrantee[] {
        GroupGrantee.ALL_USERS,
        GroupGrantee.AUTHENTICATED_USERS,
        GroupGrantee.LOG_DELIVERY
        });

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(5, 7, 5, 7);
    private final Insets insetsZeroAtBottom = new Insets(5, 7, 0, 7);
    private final Insets insetsZeroAtTop = new Insets(0, 7, 5, 7);

    /**
     * Creates a modal dialog box with a title.
     *
     * @param owner the frame within which this dialog will be displayed and centred.
     */
    protected AccessControlDialog(Frame owner, HyperlinkActivatedListener hyperlinkListener) {
        super(owner, "Update Access Control List Permissions", true);
        this.hyperlinkListener = hyperlinkListener;
        initGui();
    }

    /**
     * Initialises the dialog with access control information for the given S3 items (bucket or objects)
     *
     * @param s3Items   May be a single <code>S3Bucket</code>, or one or more <code>S3Object</code>s
     * @param accessControlList the initial ACL settings to represent in the dialog.
     */
    protected void initData(BaseStorageItem[] s3Items, AccessControlList accessControlList) {
        this.originalAccessControlList = accessControlList;

        // Item(s) description.
        if (s3Items.length > 1) {
            // Only objects can be updated in multiples, buckets are always single.
            itemsDescription.setText("<html><b>Object count</b>: " + s3Items.length + " objects");
        } else {
            if (s3Items[0] instanceof S3Bucket) {
                itemsDescription.setText("<html><b>Bucket</b><br>" + ((S3Bucket)s3Items[0]).getName());
            } else {
                itemsDescription.setText("<html><b>Object</b><br>" + ((S3Object)s3Items[0]).getKey());
            }
        }

        // Populate grantees tables.
        canonicalGranteeTableModel.removeAllGrantAndPermissions();
        emailGranteeTableModel.removeAllGrantAndPermissions();
        groupGranteeTableModel.removeAllGrantAndPermissions();

        for (GrantAndPermission gap: originalAccessControlList.getGrantAndPermissions()) {
            GranteeInterface grantee = gap.getGrantee();
            Permission permission = gap.getPermission();
            if (grantee instanceof CanonicalGrantee) {
                canonicalGranteeTableModel.addGrantee(grantee, permission);
            } else if (grantee instanceof EmailAddressGrantee) {
                emailGranteeTableModel.addGrantee(grantee, permission);
            } else if (grantee instanceof GroupGrantee) {
                groupGranteeTableModel.addGrantee(grantee, permission);
            }
        }
    }

    /**
     * Initialises all GUI elements.
     */
    protected void initGui() {
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);

        // Canonical Grantee Table and add/remove buttons.
        canonicalGranteeTableModel = new GranteeTableModel(CanonicalGrantee.class);
        canonicalGranteeTable = new GranteeTable(canonicalGranteeTableModel);
        JButton removeCanonical = new JButton();
        removeCanonical.setToolTipText("Remove the selected Canonical User grantee");
        guiUtils.applyIcon(removeCanonical, "/images/nuvola/16x16/actions/viewmag-.png");
        removeCanonical.addActionListener(this);
        removeCanonical.setActionCommand("removeCanonicalGrantee");
        JButton addCanonical = new JButton();
        addCanonical.setToolTipText("Add a new Canonical User grantee");
        guiUtils.applyIcon(addCanonical, "/images/nuvola/16x16/actions/viewmag+.png");
        addCanonical.setActionCommand("addCanonicalGrantee");
        addCanonical.addActionListener(this);

        // Email Address Grantee Table and add/remove buttons.
        emailGranteeTableModel = new GranteeTableModel(EmailAddressGrantee.class);
        emailGranteeTable = new GranteeTable(emailGranteeTableModel);
        JButton removeEmail = new JButton();
        removeEmail.setToolTipText("Remove the selected Email Address grantee");
        guiUtils.applyIcon(removeEmail, "/images/nuvola/16x16/actions/viewmag-.png");
        removeEmail.setActionCommand("removeEmailGrantee");
        removeEmail.addActionListener(this);
        JButton addEmail = new JButton();
        addEmail.setToolTipText("Add a new Email Address grantee");
        guiUtils.applyIcon(addEmail, "/images/nuvola/16x16/actions/viewmag+.png");
        addEmail.setActionCommand("addEmailGrantee");
        addEmail.addActionListener(this);

        // Group grantee table and add/remove buttons.
        groupGranteeTableModel = new GranteeTableModel(GroupGrantee.class);
        groupGranteeTable = new GranteeTable(groupGranteeTableModel);
        JButton removeGroup = new JButton();
        removeGroup.setToolTipText("Remove the selected Group grantee");
        guiUtils.applyIcon(removeGroup, "/images/nuvola/16x16/actions/viewmag-.png");
        removeGroup.setActionCommand("removeGroupGrantee");
        removeGroup.addActionListener(this);
        JButton addGroup = new JButton();
        addGroup.setToolTipText("Add a new Group grantee");
        guiUtils.applyIcon(addGroup, "/images/nuvola/16x16/actions/viewmag+.png");
        addGroup.setActionCommand("addGroupGrantee");
        addGroup.addActionListener(this);

        // Action buttons.
        JPanel buttonsContainer = new JPanel(new GridBagLayout());
        final JButton cancelButton = new JButton("Cancel Permission Changes");
        cancelButton.setDefaultCapable(true);
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand("Cancel");
        JButton okButton = new JButton("Save Permission Changes");
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);

        // Set default ENTER and ESCAPE buttons.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 4173433313456104263L;

            public void actionPerformed(ActionEvent actionEvent) {
                cancelButton.doClick();
            }
        });

        // Overall container.
        JPanel container = new JPanel(new GridBagLayout());
        int row = 0;

        itemsDescription = new JHtmlLabel("", hyperlinkListener);
        container.add(itemsDescription,
            new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        JPanel canonicalAddRemovePanel = new JPanel();
        canonicalAddRemovePanel.add(removeCanonical);
        canonicalAddRemovePanel.add(addCanonical);

        container.add(new JHtmlLabel("<html><b>Canonical User Grantees</b></html>", hyperlinkListener),
            new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, insetsZeroAtBottom, 0, 0));
        container.add(new JScrollPane(canonicalGranteeTable),
            new GridBagConstraints(0, ++row, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZeroAtBottom, 0, 0));

        container.add(new JHtmlLabel("<html><b>Group Grantees</b></html>", hyperlinkListener),
            new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, insetsZeroAtBottom, 0, 0));
        container.add(canonicalAddRemovePanel,
            new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZeroAtTop, 0, 0));

        JPanel groupAddRemovePanel = new JPanel();
        groupAddRemovePanel.add(removeGroup);
        groupAddRemovePanel.add(addGroup);
        container.add(new JScrollPane(groupGranteeTable),
            new GridBagConstraints(0, ++row, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZeroAtBottom, 0, 0));

        container.add(new JHtmlLabel("<html><b>Email Address Grantees</b></html>", hyperlinkListener),
            new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.SOUTHWEST, GridBagConstraints.HORIZONTAL, insetsZeroAtBottom, 0, 0));
        container.add(groupAddRemovePanel,
            new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZeroAtTop, 0, 0));

        JPanel emailAddRemovePanel = new JPanel();
        emailAddRemovePanel.add(removeEmail);
        emailAddRemovePanel.add(addEmail);
        container.add(new JScrollPane(emailGranteeTable),
            new GridBagConstraints(0, ++row, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZeroAtBottom, 0, 0));
        container.add(emailAddRemovePanel,
            new GridBagConstraints(1, ++row, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZeroAtTop, 0, 0));

        buttonsContainer.add(cancelButton,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsZero, 0, 0));
        buttonsContainer.add(okButton,
            new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsZero, 0, 0));

        container.add(buttonsContainer,
            new GridBagConstraints(0, ++row, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        this.getContentPane().add(container);
        this.pack();

        this.setSize(new Dimension(700, 500));
        this.setLocationRelativeTo(this.getOwner());

        // Resize columns.
        canonicalGranteeTable.getColumnModel().getColumn(0).setPreferredWidth((int)
            (canonicalGranteeTable.getParent().getBounds().getWidth() * 0.9));
        emailGranteeTable.getColumnModel().getColumn(0).setPreferredWidth((int)
            (emailGranteeTable.getParent().getBounds().getWidth() * 0.9));
        groupGranteeTable.getColumnModel().getColumn(0).setPreferredWidth((int)
            (groupGranteeTable.getParent().getBounds().getWidth() * 0.9));
    }

    /**
     * @return the ACL settings as set by the user in the dialog.
     */
    public AccessControlList getUpdatedAccessControlList() {
        return updatedAccessControlList;
    }

    /**
     * Populates the local {@link #updatedAccessControlList} variable with ACL
     * details set by the user in the GUI elements.
     */
    private void updateAccessControlList() {
        updatedAccessControlList = new AccessControlList();
        updatedAccessControlList.setOwner(originalAccessControlList.getOwner());

        for (int i = 0; i < canonicalGranteeTable.getRowCount(); i++) {
            GranteeInterface grantee = canonicalGranteeTableModel.getGrantee(i);
            Permission permission = canonicalGranteeTableModel.getPermission(i);
            updatedAccessControlList.grantPermission(grantee, permission);
        }
        for (int i = 0; i < emailGranteeTable.getRowCount(); i++) {
            GranteeInterface grantee = emailGranteeTableModel.getGrantee(i);
            Permission permission = emailGranteeTableModel.getPermission(i);
            updatedAccessControlList.grantPermission(grantee, permission);
        }
        for (int i = 0; i < groupGranteeTable.getRowCount(); i++) {
            GranteeInterface grantee = groupGranteeTableModel.getGrantee(i);
            Permission permission = groupGranteeTableModel.getPermission(i);
            updatedAccessControlList.grantPermission(grantee, permission);
        }
    }

    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        if ("OK".equals(e.getActionCommand())) {
            updateAccessControlList();
            this.setVisible(false);
        } else if ("Cancel".equals(e.getActionCommand())) {
            updatedAccessControlList = null;
            this.setVisible(false);
        } else if ("addCanonicalGrantee".equals(e.getActionCommand())) {
            int rowIndex = canonicalGranteeTableModel.addGrantee(
                new CanonicalGrantee("NewCanonicalId"), Permission.PERMISSION_READ);
            canonicalGranteeTable.setRowSelectionInterval(rowIndex, rowIndex);
        } else if ("removeCanonicalGrantee".equals(e.getActionCommand())) {
            if (canonicalGranteeTable.getSelectedRow() >= 0) {
                canonicalGranteeTableModel.removeGrantAndPermission(canonicalGranteeTable.getSelectedRow());
            }
        } else if ("addEmailGrantee".equals(e.getActionCommand())) {
            int rowIndex = emailGranteeTableModel.addGrantee(
                new EmailAddressGrantee("new.email@address.here"), Permission.PERMISSION_READ);
            emailGranteeTable.setRowSelectionInterval(rowIndex, rowIndex);
        } else if ("removeEmailGrantee".equals(e.getActionCommand())) {
            if (emailGranteeTable.getSelectedRow() >= 0) {
                emailGranteeTableModel.removeGrantAndPermission(emailGranteeTable.getSelectedRow());
            }
        } else if ("addGroupGrantee".equals(e.getActionCommand())) {
            int rowIndex = groupGranteeTableModel.addGrantee(
                GroupGrantee.AUTHENTICATED_USERS, Permission.PERMISSION_READ);
            groupGranteeTable.setRowSelectionInterval(rowIndex, rowIndex);
        } else if ("removeGroupGrantee".equals(e.getActionCommand())) {
            if (groupGranteeTable.getSelectedRow() >= 0) {
                groupGranteeTableModel.removeGrantAndPermission(groupGranteeTable.getSelectedRow());
            }
        } else {
            System.err.println("UNRECOGNISED ACTION COMMAND: " + e.getActionCommand());
        }
    }

    /**
     * Displays the dialog box and waits until the user applies their changes or cancels the dialog.
     * <p>
     * If the user elects to apply their changes, this method returns the updated ACL information.
     * If the user cancels the dialog, this method returns null.
     *
     * @param owner     the Frame within which this dialog will be displayed and centered
     * @param s3Items   an array of {@link S3Bucket} or {@link S3Object}s to which ACL change will be applied
     * @param accessControlList the original ACL settings for the S3Bucket or S3Objects provided
     * @return  the update ACL settings if the user applies changes, null if the dialog is cancelled.
     */
    public static AccessControlList showDialog(Frame owner, BaseStorageItem[] s3Items,
        AccessControlList accessControlList, HyperlinkActivatedListener hyperlinkListener)
    {
        if (accessControlDialog == null) {
            accessControlDialog = new AccessControlDialog(owner, hyperlinkListener);
        }
        accessControlDialog.initData(s3Items, accessControlList);
        accessControlDialog.setVisible(true);
        return accessControlDialog.getUpdatedAccessControlList();
    }

    /**
     * Table to represent ACL grantees.
     *
     * @author James Murty
     */
    private class GranteeTable extends JTable {
        private static final long serialVersionUID = -5339684196750695854L;

        private TableSorter sorter = null;

        public GranteeTable(GranteeTableModel granteeTableModel) {
            super();
            sorter = new TableSorter(granteeTableModel);
            this.setModel(sorter);
            sorter.setTableHeader(this.getTableHeader());

            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            getSelectionModel().addListSelectionListener(this);
            DefaultCellEditor groupCellEditor = new DefaultCellEditor(groupGranteeComboBox);
            groupCellEditor.setClickCountToStart(2);
            setDefaultEditor(GroupGrantee.class, groupCellEditor);
            setDefaultRenderer(GroupGrantee.class, new DefaultTableCellRenderer() {
                private static final long serialVersionUID = 4938391147702620699L;

                @Override
                public Component getTableCellRendererComponent(JTable arg0, Object value, boolean arg2, boolean arg3, int arg4, int arg5) {
                    if (value == null) {
                        return null;
                    }
                    GroupGrantee groupGrantee = (GroupGrantee) value;
                    return super.getTableCellRendererComponent(arg0, groupGrantee.getIdentifier(), arg2, arg3, arg4, arg5);
                }
            });
            DefaultCellEditor permissionCellEditor = new DefaultCellEditor(permissionComboBox);
            permissionCellEditor.setClickCountToStart(2);
            setDefaultEditor(Permission.class, permissionCellEditor);
        }

        @Override
        public int getSelectedRow() {
            int tableIndex = super.getSelectedRow();
            if (tableIndex >= 0) {
                return sorter.modelIndex(tableIndex);
            } else {
                return 0;
            }
        }
    }

    /**
     * Grantee table model that knows what kind of grantees it is displaying and displays
     * them appropriately.
     *
     * @author James Murty
     */
    private class GranteeTableModel extends DefaultTableModel {
        private static final long serialVersionUID = -5533290183089426571L;

        private Class granteeClass = null;
        ArrayList currentGrantees  = new ArrayList();
        int permissionColumn = 0;

        public GranteeTableModel(Class granteeClass) {
            super(
                (CanonicalGrantee.class.equals(granteeClass) ? canonicalUserTableColumnNames :
                    EmailAddressGrantee.class.equals(granteeClass) ? emailTableColumnNames :
                        GroupGrantee.class.equals(granteeClass) ? groupTableColumnNames :
                            new String[] {}
                ), 0);
            this.granteeClass = granteeClass;
            permissionColumn = (CanonicalGrantee.class.equals(granteeClass) ? 2 : 1);
        }

        public int addGrantee(GranteeInterface grantee, Permission permission) {
            GrantAndPermission gap = new GrantAndPermission(grantee, permission);
            int insertRow =
                Collections.binarySearch(currentGrantees, gap, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        GrantAndPermission g1 = (GrantAndPermission) o1;
                        GrantAndPermission g2 = (GrantAndPermission) o2;
                        return g1.getGrantee().getIdentifier().compareToIgnoreCase(
                            g2.getGrantee().getIdentifier());
                    }
                });
            if (insertRow >= 0) {
                // We already have an item with this key, but that's OK.
            } else {
                insertRow = (-insertRow) - 1;
            }
            // New object to insert.
            currentGrantees.add(insertRow, gap);
            if (grantee instanceof GroupGrantee) {
                this.insertRow(insertRow, new Object[] {grantee, permission});
            } else if (grantee instanceof CanonicalGrantee) {
                CanonicalGrantee canonicalGrantee = (CanonicalGrantee) grantee;
                this.insertRow(insertRow, new Object[] {canonicalGrantee.getIdentifier(),
                    canonicalGrantee.getDisplayName(), permission});
            } else {
                this.insertRow(insertRow, new Object[] {grantee.getIdentifier(), permission});
            }
            return insertRow;
        }

        public void removeGrantAndPermission(int index) {
            Object grantee = this.getGrantee(index);
            this.removeRow(index);
            currentGrantees.remove(grantee);
        }

        public void removeAllGrantAndPermissions() {
            int rowCount = this.getRowCount();
            for (int i = 0; i < rowCount; i++) {
                this.removeRow(0);
            }
            currentGrantees.clear();
        }

        public Permission getPermission(int index) {
            return (Permission) this.getValueAt(index, permissionColumn);
        }

        public GranteeInterface getGrantee(int index) {
            GrantAndPermission originalGAP = (GrantAndPermission) currentGrantees.get(index);
            Object updatedGrantee = super.getValueAt(index, 0);
            if (updatedGrantee instanceof GroupGrantee) {
                // We can return this as-is, because GroupGrantees are actually stored in the table.
                return (GroupGrantee) updatedGrantee;
            } else {
                // Non-group Grantees are stored as Strings in the table, so update the original's ID.
                originalGAP.getGrantee().setIdentifier((String) updatedGrantee);
                return originalGAP.getGrantee();
            }
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return (column == 0 || column == permissionColumn);
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                if (GroupGrantee.class.equals(granteeClass)) {
                    return GroupGrantee.class;
                } else {
                    return String.class;
                }
            } else if (columnIndex == permissionColumn) {
                return Permission.class;
            } else {
                return String.class;
            }
        }
    }

    /**
     * Creates stand-alone dialog box for testing only.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        // TEST DATA
        AccessControlList acl = new AccessControlList();
        S3Owner owner = new S3Owner("1234567890", "Some Name");
        acl.setOwner(owner);

        GranteeInterface grantee = new CanonicalGrantee();
        grantee.setIdentifier("zzz");
        acl.grantPermission(grantee, Permission.PERMISSION_WRITE);

        grantee = new CanonicalGrantee();
        grantee.setIdentifier("abc");
        ((CanonicalGrantee)grantee).setDisplayName("jamesmurty");
        acl.grantPermission(grantee, Permission.PERMISSION_FULL_CONTROL);
        grantee = new CanonicalGrantee();
        grantee.setIdentifier("aaa");
        acl.grantPermission(grantee, Permission.PERMISSION_READ);
        grantee = GroupGrantee.ALL_USERS;
        acl.grantPermission(grantee, Permission.PERMISSION_READ);
        grantee = GroupGrantee.AUTHENTICATED_USERS;
        acl.grantPermission(grantee, Permission.PERMISSION_WRITE);
        grantee = new EmailAddressGrantee();
        grantee.setIdentifier("james@test.com");
        acl.grantPermission(grantee, Permission.PERMISSION_READ);
        grantee = new EmailAddressGrantee();
        grantee.setIdentifier("james@test2.com");
        acl.grantPermission(grantee, Permission.PERMISSION_FULL_CONTROL);

        JFrame f = new JFrame("Cockpit");
        S3Bucket bucket = new S3Bucket();
        bucket.setName("SomeReallyLongAndWackyBucketNamePath.HereItIs");

        AccessControlList updatedACL = acl;
        while ((updatedACL = AccessControlDialog.showDialog(f, new S3Bucket[] {bucket}, updatedACL, null)) != null) {
            System.out.println(updatedACL.toXml());
        }

        f.dispose();
    }

}
