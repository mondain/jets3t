/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2008-2010 James Murty
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.acl.AccessControlList;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

/**
 * Dialog for choosing the destination bucket for an Object copy operation, and
 * specifying how the copy will be performed. The dialog includes options for
 * renaming object keys during the copy, and for indicating that the copy will
 * actually be a Move operation - in which case the original objects should be
 * deleted after the copy has completed successfully.
 *
 * @author James Murty
 */
public class CopyObjectsDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = -1131752874387139972L;

    private SkinsFactory skinsFactory = null;

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(5, 7, 5, 7);
    private final Insets insetsHorizontalSpace = new Insets(0, 7, 0, 7);

    private S3Object[] destinationObjects = null;
    private String[] sourceObjectKeys = null;
    private S3Bucket[] buckets = null;
    private String sourceBucketName = null;

    private JTextField renamePatternTextField = null;
    private JButton okButton = null;
    private JPanel warningPanel = null;
    private DefaultTableModel previewTableModel = null;
    private JTable previewTable = null;
    private JComboBox destinationBucketComboBox = null;
    private JComboBox destinationAclComboBox = null;
    private JComboBox destinationStorageClassComboBox = null;
    private JCheckBox moveObjectsCheckBox = null;

    private boolean copyActionApproved = false;
    private boolean copyOriginalAccessControlLists = false;

    /**
     * Construct a modal dialog for controlling copy opeations.
     *
     * @param owner
     * the Frame over which the dialog will be displayed and centred.
     * @param title
     * a title for the dialog.
     * @param skinsFactory
     * factory for producing skinned GUI components.
     * @param objects
     * the S3 objects that will be copied if the user confirms the dialog.
     * @param buckets
     * a list of S3 buckets to which the user can copy objects.
     */
    public CopyObjectsDialog(Frame owner, String title,
        SkinsFactory skinsFactory, S3Object[] objects, S3Bucket[] buckets)
    {
        super(owner, title, true);
        this.skinsFactory = skinsFactory;
        this.buckets = buckets;
        // Clone the objects provided.
        this.destinationObjects = new S3Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            this.destinationObjects[i] = (S3Object) objects[i].clone();
        }
        sourceObjectKeys = new String[objects.length];
        for (int i = 0; i < objects.length; i++) {
            sourceObjectKeys[i] = objects[i].getKey();
        }
        sourceBucketName = destinationObjects[0].getBucketName();

        this.initGui();
    }

    /**
     * Initialise the GUI elements to display the dialog.
     */
    private void initGui() {
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel optionsPanel = skinsFactory.createSkinnedJPanel("CopyObjectsDialogOptionsPanel");
        optionsPanel.setLayout(new GridBagLayout());

        // Destination bucket chooser.
        destinationBucketComboBox = skinsFactory.createSkinnedJComboBox("DestinationBucketComboBox");
        for (int i = 0; i < buckets.length; i++) {
            destinationBucketComboBox.addItem(buckets[i].getName());
        }
        destinationBucketComboBox.setSelectedItem(sourceBucketName);
        JLabel destinationBucketLabel = skinsFactory.createSkinnedJHtmlLabel("DestinationBucketLabel");
        destinationBucketLabel.setText("Copy to Bucket:");
        JPanel bucketPanel = skinsFactory.createSkinnedJPanel("CopyObjectsDialogBucketPanel");
        bucketPanel.setLayout(new GridBagLayout());
        bucketPanel.add(destinationBucketLabel, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        bucketPanel.add(destinationBucketComboBox, new GridBagConstraints(1, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        // Destination Access Control List setting.
        destinationAclComboBox = skinsFactory.createSkinnedJComboBox("DestinationAclComboBox");
        destinationAclComboBox.addItem("Unchanged");
        destinationAclComboBox.addItem("Private");
        destinationAclComboBox.addItem("Publically Accessible");
        JLabel destinationAclLabel = skinsFactory.createSkinnedJHtmlLabel("DestinationAclLabel");
        destinationAclLabel.setText("Access permissions for copied objects: ");
        JPanel aclPanel = skinsFactory.createSkinnedJPanel("CopyObjectsDialogAclPanel");
        aclPanel.setLayout(new GridBagLayout());
        aclPanel.add(destinationAclLabel, new GridBagConstraints(0, 1,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        aclPanel.add(destinationAclComboBox, new GridBagConstraints(1, 1,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));

        // Destination Storage Class setting.
        destinationStorageClassComboBox = skinsFactory.createSkinnedJComboBox(
            "DestinationStorageClassComboBox");
        destinationStorageClassComboBox.addItem(S3Object.STORAGE_CLASS_STANDARD);
        destinationStorageClassComboBox.addItem(S3Object.STORAGE_CLASS_REDUCED_REDUNDANCY);
        JLabel destinationStorageClassLabel = skinsFactory.createSkinnedJHtmlLabel(
            "DestinationStorageClassLabel");
        destinationStorageClassLabel.setText("Storage Class for copied objects: ");
        JPanel storageClassPanel = skinsFactory.createSkinnedJPanel(
            "CopyObjectsDialogStorageClassPanel");
        storageClassPanel.setLayout(new GridBagLayout());
        storageClassPanel.add(destinationStorageClassLabel, new GridBagConstraints(0, 1,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        storageClassPanel.add(destinationStorageClassComboBox, new GridBagConstraints(1, 1,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));

        // Move objects checkbox
        moveObjectsCheckBox = skinsFactory.createSkinnedJCheckBox("MoveObjectsCheckBox");
        moveObjectsCheckBox.setSelected(false);
        moveObjectsCheckBox.setText("Move Objects  (Originals will be deleted after copy)");

        // Object renaming options and renaming pattern text field.
        JRadioButton unchangedNamesRadioButton = skinsFactory.createSkinnedJRadioButton("UnchangedObjectNamesRadioButton");
        unchangedNamesRadioButton.setText("Leave object names unchanged");
        unchangedNamesRadioButton.setSelected(true);
        final JRadioButton changedNamesRadioButton = skinsFactory.createSkinnedJRadioButton("ChangedObjectNamesRadioButton");
        changedNamesRadioButton.setText("Rename objects with pattern:");
        renamePatternTextField = skinsFactory.createSkinnedJTextField("RenamePatternTextField");
        renamePatternTextField.setEnabled(false);
        renamePatternTextField.setText(
            (destinationObjects.length == 1 ? destinationObjects[0].getKey() : "{key}"));
        renamePatternTextField.setToolTipText("Use variables to rename your objects: {key}  {path}  {filename}  {basename}  {ext}  {count}");

        renamePatternTextField.getDocument().addDocumentListener(new DocumentListener() {
           public void changedUpdate(DocumentEvent e) {
               refreshNamesPreviewTable();
           }
           public void insertUpdate(DocumentEvent e) {
               refreshNamesPreviewTable();
           }
           public void removeUpdate(DocumentEvent e) {
               refreshNamesPreviewTable();
           }
        });

        changedNamesRadioButton.addChangeListener(new ChangeListener() {
           public void stateChanged(ChangeEvent e) {
               if (changedNamesRadioButton.isSelected()) {
                   renamePatternTextField.setEnabled(true);
                   renamePatternTextField.requestFocus();
               } else {
                   renamePatternTextField.setText(
                       (destinationObjects.length == 1 ? destinationObjects[0].getKey() : "{key}"));
                   renamePatternTextField.setEnabled(false);
               }
            }
        });

        ButtonGroup radioButtonGroup = new ButtonGroup();
        radioButtonGroup.add(unchangedNamesRadioButton);
        radioButtonGroup.add(changedNamesRadioButton);

        JPanel renamePanel = skinsFactory.createSkinnedJPanel("CopyObjectsDialogRenamePanel");
        renamePanel.setLayout(new GridBagLayout());

        renamePanel.add(unchangedNamesRadioButton, new GridBagConstraints(0, 0,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        renamePanel.add(changedNamesRadioButton, new GridBagConstraints(0, 1,
            1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        int textOffset = changedNamesRadioButton.getIconTextGap()
            + (int) changedNamesRadioButton.getPreferredSize().getHeight();
        renamePanel.add(renamePatternTextField, new GridBagConstraints(0, 2,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
            new Insets(0, textOffset, 0, 0), 0, 0));

        // Build preview table for object names after copy.
        previewTableModel = new DefaultTableModel(new Object[] {"Object Key"}, 0) {
            private static final long serialVersionUID = -2859341917353477009L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        TableSorter previewTableSorter = new TableSorter(previewTableModel);
        previewTableSorter.setSortingStatus(0, TableSorter.ASCENDING);
        previewTable = skinsFactory.createSkinnedJTable("MetadataTable");
        previewTable.setModel(previewTableSorter);
        previewTableSorter.setTableHeader(previewTable.getTableHeader());

        JHtmlLabel copyPreviewTableLabel = skinsFactory.createSkinnedJHtmlLabel("CopyObjectsDialogCopyPreviewLabel");
        copyPreviewTableLabel.setText("<html><b>Copy Preview</b></html>");
        copyPreviewTableLabel.setHorizontalAlignment(JLabel.CENTER);

        // OK Button (Accept copy or move operation).
        okButton = skinsFactory.createSkinnedJButton("CopyObjectsDialogOKButton");
        okButton.setText("Copy Object" + (destinationObjects.length > 0 ? "s" : ""));
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);
        okButton.setEnabled(false);

        moveObjectsCheckBox.addChangeListener(new ChangeListener() {
           public void stateChanged(ChangeEvent e) {
               if (moveObjectsCheckBox.isSelected()) {
                   okButton.setText("Move Object" + (destinationObjects.length > 0 ? "s" : ""));
               } else {
                   okButton.setText("Copy Object" + (destinationObjects.length > 0 ? "s" : ""));
               }
            }
        });

        // Cancel Button.
        JButton cancelButton = skinsFactory.createSkinnedJButton("CopyObjectsDialogCancelButton");
        cancelButton.setText("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);

        // Warning message displayed when the user's proposed renaming pattern will cause
        // copied objects to overwrite each other.
        warningPanel = skinsFactory.createSkinnedJPanel("CopyObjectsDialogWarningPanel");
        warningPanel.setLayout(new GridBagLayout());
        JHtmlLabel warningLabel = skinsFactory.createSkinnedJHtmlLabel("CopyObjectsDialogWarningLabel");
        warningLabel.setText("<html><font color=red>ERROR:</font> Object renaming pattern is causing key name clashes.</html>");
        warningLabel.setHorizontalAlignment(JLabel.CENTER);
        warningPanel.add(warningLabel, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        JPanel actionButtonsPanel = skinsFactory.createSkinnedJPanel("CopoyObjectsDialogActionButtonsPanel");
        actionButtonsPanel.setLayout(new GridBagLayout());
        actionButtonsPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 1, 0,
            GridBagConstraints.EAST, GridBagConstraints.NONE, insetsHorizontalSpace, 0, 0));
        actionButtonsPanel.add(okButton, new GridBagConstraints(1, 0, 1, 1, 1, 0,
            GridBagConstraints.WEST, GridBagConstraints.NONE, insetsHorizontalSpace, 0, 0));

        // Put all dialog panels and fragments together.
        int row = 0;
        optionsPanel.add(bucketPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(aclPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(storageClassPanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(moveObjectsCheckBox, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsHorizontalSpace, 0, 0));
        optionsPanel.add(renamePanel, new GridBagConstraints(0, row++,
            1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(copyPreviewTableLabel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(new JScrollPane(previewTable), new GridBagConstraints(0, row++,
            2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsHorizontalSpace, 0, 0));
        optionsPanel.add(warningPanel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));
        optionsPanel.add(actionButtonsPanel, new GridBagConstraints(0, row++,
            2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsDefault, 0, 0));

        // Recognize and handle ENTER and ESCAPE.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = 921962767729511631L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });

        this.getContentPane().add(optionsPanel);
        this.pack();
        this.setSize(450, 400);

        this.setLocationRelativeTo(this.getOwner());

        refreshNamesPreviewTable();
    }

    /**
     * @param objects
     * the objects that will be renamed.
     *
     * @return
     * the renamed keys that will result from the proposed renaming pattern.
     */
    protected Set renameObjectKeys(S3Object[] objects) {
        Set newNames = new HashSet();
        for (int i = 0; i < objects.length; i++) {
            String newName = renameObjectKey(objects[i].getKey(), i);

            newNames.add(newName);
        }
        return newNames;
    }

    /**
     * Return the renamed key for an object based on the current renaming pattern.
     * This method calculates values for the {key}, {count}, {path}, {filename},
     * {basename} and {ext} variables from the original key name, and returns the
     * destination key name when these values are substituted into the current pattern.
     * <p>
     * The substitution variables supported by this method are:
     * <ul>
     * <li>{key} - the original object key</li>
     * <li>{count} - an offset value for this object (one greater than the offset
     * value provided to this method)</li>
     * <li>{path} - the path portion of the key name, up to the last occurence of
     * a slash (/) character. If the key contains no slash characters, this variable
     * will be an empty string.</li>
     * <li>{filename} - the filename portion of the key name, everything after the
     * last slash (/) character. If the key contains no slash characters, this variable
     * will be the original key name.</li>
     * <li>{ext} - the extension portion of a filename, if any.</li>
     * <li>{basename} - the file's base name, excluding the extension.</li>
     * </ul>
     *
     * @param key
     * the original name of an S3 object.
     * @param offset
     * the offset for the current object in a set of objects, eg this is the
     * <b>i</b>th object in the list. This information is necessary to enable the
     * {count}
     *
     * @return
     * the renamed object key generated by the renaming pattern.
     */
    protected String renameObjectKey(String key, int offset) {
        // Generate subsitution variables: key, path, filename, count.
        String count = "" + (offset + 1);
        String filename = key;
        String path = "";

        int lastSlash = key.lastIndexOf('/');
        if (lastSlash >= 0) {
            path = key.substring(0, lastSlash + 1);
            filename = key.substring(lastSlash + 1);
        }

        String basename = filename;
        String ext = "";
        int lastPeriod = filename.lastIndexOf('.');
        if (lastPeriod >= 0) {
            basename = filename.substring(0, lastPeriod);
            ext = filename.substring(lastPeriod + 1);
        }

        // Perform substitutions to generate new names.
        String newName = renamePatternTextField.getText();
        newName = newName.replaceAll("\\{key\\}", Matcher.quoteReplacement(key));
        newName = newName.replaceAll("\\{count\\}", Matcher.quoteReplacement(count));
        newName = newName.replaceAll("\\{path\\}", Matcher.quoteReplacement(path));
        newName = newName.replaceAll("\\{filename\\}", Matcher.quoteReplacement(filename));
        newName = newName.replaceAll("\\{ext\\}", Matcher.quoteReplacement(ext));
        newName = newName.replaceAll("\\{basename\\}", Matcher.quoteReplacement(basename));

        return newName;
    }

    /**
     * Refreshes the preview table to display the target keys that will be
     * generated by the proposed renaming pattern.
     */
    protected void refreshNamesPreviewTable() {
        while (previewTableModel.getRowCount() > 0) {
            previewTableModel.removeRow(0);
        }

        Set renamedKeys = renameObjectKeys(destinationObjects);
        Iterator nameIter = renamedKeys.iterator();
        while (nameIter.hasNext()) {
            previewTableModel.addRow(new Object[] {nameIter.next()});
        }

        if (destinationObjects.length != renamedKeys.size()) {
            okButton.setEnabled(false);
            warningPanel.setVisible(true);
        } else {
            okButton.setEnabled(true);
            warningPanel.setVisible(false);
        }
    }


    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        if ("OK".equals(e.getActionCommand())) {
            copyActionApproved = true;

            for (int i = 0; i < destinationObjects.length; i++) {
                destinationObjects[i].setKey(
                    renameObjectKey(destinationObjects[i].getKey(), i));
                if ("Publically Accessible".equals(destinationAclComboBox.getSelectedItem())) {
                    destinationObjects[i].setAcl(AccessControlList.REST_CANNED_PUBLIC_READ);
                } else if ("Unchanged".equals(destinationAclComboBox.getSelectedItem())) {
                    copyOriginalAccessControlLists = true;
                }
                // Apply storage class
                destinationObjects[i].setStorageClass(
                    (String) destinationStorageClassComboBox.getSelectedItem());
            }

            this.setVisible(false);
        } else if ("Cancel".equals(e.getActionCommand())) {
            copyActionApproved = false;
            this.setVisible(false);
        }
    }

    /**
     * @return
     * true if the user accepted the copy/move operation, false if the user
     * cancelled the dialog.
     */
    public boolean isCopyActionApproved() {
        return copyActionApproved;
    }

    /**
     * @return
     * true if the user selected the Move option to indicate that objects should
     * be moved, rather than merely copied.
     */
    public boolean isMoveOptionSelected() {
        return moveObjectsCheckBox.isSelected();
    }

    /**
     * @return
     * true if the use wishes to have the ACL settings of their source objects
     * retained after the copy.
     */
    public boolean isCopyOriginalAccessControlLists() {
        return copyOriginalAccessControlLists;
    }

    /**
     * @return
     * the original key names of the S3 objects that should be copied or moved
     * when this dialog is accepted.
     */
    public String[] getSourceObjectKeys() {
        if (!isCopyActionApproved()) {
            return null;
        }
        return sourceObjectKeys;
    }

    /**
     * @return
     * the objects that will be created as the destination of a copy or move
     * operation. These objects include the metadata changes and Access Control
     * List setting applied by the user.
     */
    public S3Object[] getDestinationObjects() {
        if (!isCopyActionApproved()) {
            return null;
        }
        return destinationObjects;
    }

    /**
     * @return
     * the name of the bucket to which objects should be copied or moved, as
     * chosen by the user.
     */
    public String getDestinationBucketName() {
        if (!isCopyActionApproved()) {
            return null;
        }
        return (String) destinationBucketComboBox.getSelectedItem();
    }

}
