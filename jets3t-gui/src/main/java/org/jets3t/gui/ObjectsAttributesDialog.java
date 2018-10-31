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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import org.jets3t.gui.skins.SkinsFactory;
import org.jets3t.service.model.S3Object;

/**
 * Dialog to display detailed information about one or more {@link S3Object}s,
 * and optionally to allow the objects metadata attributes to be modified.
 *
 * @author James Murty
 */
public class ObjectsAttributesDialog extends JDialog implements ActionListener {

    private static final long serialVersionUID = 7306510092130196820L;

    private GuiUtils guiUtils = new GuiUtils();
    private SkinsFactory skinsFactory = null;

    private final Insets insetsZero = new Insets(0, 0, 0, 0);
    private final Insets insetsDefault = new Insets(5, 7, 5, 7);
    private final Insets insetsVerticalSpace = new Insets(5, 0, 5, 0);
    private final Insets insetsHorizontalSpace = new Insets(0, 7, 0, 7);

    private S3Object[] destinationObjects = null;
    private S3Object currentObject = null;
    private int currentObjectIndex = 0;

    private JTextField objectKeyTextField = null;
    private JTextField objectContentLengthTextField = null;
    private JTextField objectLastModifiedTextField = null;
    private JTextField objectETagTextField = null;
    private JTextField bucketLocationTextField = null;
    private JTextField storageClassTextField = null;
    private DefaultTableModel objectMetadataTableModel = null;
    private TableSorter metadataTableSorter = null;
    private JLabel ownerNameLabel = null;
    private JLabel ownerIdLabel = null;
    private JLabel currentObjectLabel = null;
    private JTable metadataTable = null;
    private JButton removeMetadataItemButton = null;
    private JButton addMetadataItemButton = null;
    private JButton previousObjectButton = null;
    private JButton nextObjectButton = null;
    private JButton okButton = null;
    private JButton cancelButton = null;
    private JPanel metadataButtonsContainer = null;
    private JPanel destinationPanel = null;
    private JPanel nextPreviousPanel = null;

    private JTextField ownerNameTextField = null;
    private JTextField ownerIdTextField = null;

    private boolean modifyMode = false;
    private boolean modifyActionApproved = false;

    /**
     * Construct a modal dialog to display the attributes for one or more objects.
     *
     * @param owner
     * the Frame over which the dialog will be displayed and centred.
     * @param title
     * a title for the dialog.
     * @param skinsFactory
     * factory for producing skinned GUI components.
     */
    public ObjectsAttributesDialog(Frame owner, String title, SkinsFactory skinsFactory)
    {
        super(owner, title, true);
        this.skinsFactory = skinsFactory;
        this.initGui();
    }

    /**
     * Display the dialog with data - always use this method instead of setVisible.
     *
     * @param objects
     * the S3 objects whose attributes will be displayed, and that may be modified.
     * @param modifyMode
     * if this parameter is true, the user will be able to modify object metadata
     * items. If false, the user will only be able to view object attributes and
     * will not be able to change the metadata.
     */
    public void displayDialog(S3Object[] objects, boolean modifyMode) {
        this.currentObjectIndex = 0;
        this.modifyActionApproved = false;
        // Clone the objects provided.
        this.modifyMode = modifyMode;
        this.destinationObjects = new S3Object[objects.length];
        for (int i = 0; i < objects.length; i++) {
            this.destinationObjects[i] = (S3Object) objects[i].clone();
        }

        if (modifyMode) {
            okButton.setText("Modify Object" + (destinationObjects.length > 0 ? "s" : ""));
            cancelButton.setVisible(true);
            metadataButtonsContainer.setVisible(true);
            destinationPanel.setVisible(true);
        } else {
            okButton.setText("OK");
            cancelButton.setVisible(false);
            metadataButtonsContainer.setVisible(false);
            destinationPanel.setVisible(false);
        }
        if (destinationObjects.length > 1) {
            nextPreviousPanel.setVisible(true);
        } else {
            nextPreviousPanel.setVisible(false);
        }
        displayObjectProperties();

        int height = (isModifyMode() ? 450 : 400);
        if (objects.length > 1) {
            height += 30;
        }
        this.pack();
        this.setSize(new Dimension(450, height));
        this.setLocationRelativeTo(this.getOwner());
        this.setVisible(true);
    }

    /**
     * Initialise the GUI elements to display the given item.
     */
    private void initGui() {
        this.setResizable(true);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel unmodifiableAttributesPanel = skinsFactory.createSkinnedJPanel("ObjectStaticAttributesPanel");
        unmodifiableAttributesPanel.setLayout(new GridBagLayout());
        JPanel metadataContainer = skinsFactory.createSkinnedJPanel("ObjectPropertiesMetadataPanel");
        metadataContainer.setLayout(new GridBagLayout());
        metadataButtonsContainer = skinsFactory.createSkinnedJPanel("ObjectPropertiesMetadataButtonsPanel");
        metadataButtonsContainer.setLayout(new GridBagLayout());

        // Fields to display unmodifiable object details.
        JLabel objectKeyLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectKeyLabel");
        objectKeyLabel.setText("Object key:");
        objectKeyTextField = skinsFactory.createSkinnedJTextField("ObjectKeyTextField");
        objectKeyTextField.setEditable(false);
        JLabel objectContentLengthLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectContentLengthLabel");
        objectContentLengthLabel.setText("Size:");
        objectContentLengthTextField = skinsFactory.createSkinnedJTextField("ObjectContentLengthTextField");
        objectContentLengthTextField.setEditable(false);
        JLabel objectLastModifiedLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectLastModifiedLabel");
        objectLastModifiedLabel.setText("Last modified:");
        objectLastModifiedTextField = skinsFactory.createSkinnedJTextField("ObjectLastModifiedTextField");
        objectLastModifiedTextField.setEditable(false);
        JLabel objectETagLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectETagLabel");
        objectETagLabel.setText("ETag:");
        objectETagTextField = skinsFactory.createSkinnedJTextField("ObjectETagTextField");
        objectETagTextField.setEditable(false);
        JLabel bucketNameLabel = skinsFactory.createSkinnedJHtmlLabel("BucketNameLabel");
        bucketNameLabel.setText("Bucket:");
        bucketLocationTextField = skinsFactory.createSkinnedJTextField("BucketLocationTextField");
        bucketLocationTextField.setEditable(false);
        JLabel storageClassLabel = skinsFactory.createSkinnedJHtmlLabel("StorageClassLabel");
        storageClassLabel.setText("Storage Class:");
        storageClassTextField = skinsFactory.createSkinnedJTextField("StorageClassTextField");
        storageClassTextField.setEditable(false);
        ownerNameLabel = skinsFactory.createSkinnedJHtmlLabel("OwnerNameLabel");
        ownerNameLabel.setText("Owner name:");
        ownerNameTextField = skinsFactory.createSkinnedJTextField("OwnerNameTextField");
        ownerNameTextField.setEditable(false);
        ownerIdLabel = skinsFactory.createSkinnedJHtmlLabel("OwnerIdLabel");
        ownerIdLabel.setText("Owner ID:");
        ownerIdTextField = skinsFactory.createSkinnedJTextField("OwnerIdTextField");
        ownerIdTextField.setEditable(false);

        Insets textFieldInsets = new Insets(2, 4, 2, 4);
        int row = 0;

        unmodifiableAttributesPanel.add(objectKeyLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(objectKeyTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(objectContentLengthLabel, new GridBagConstraints(0, row, 1, 1,
            0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(objectContentLengthTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(objectLastModifiedLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(objectLastModifiedTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(objectETagLabel, new GridBagConstraints(0, row, 1, 1,
            0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(objectETagTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(ownerNameLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(ownerNameTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(ownerIdLabel, new GridBagConstraints(0, row,
            1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0,
            0));
        unmodifiableAttributesPanel.add(ownerIdTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(bucketNameLabel, new GridBagConstraints(0, row, 1, 1,
            0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(bucketLocationTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));
        row++;
        unmodifiableAttributesPanel.add(storageClassLabel, new GridBagConstraints(0, row, 1, 1,
            0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, textFieldInsets, 0, 0));
        unmodifiableAttributesPanel.add(storageClassTextField, new GridBagConstraints(1, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, textFieldInsets, 0, 0));

        // Build metadata table.
        objectMetadataTableModel = new DefaultTableModel(new Object[] {"Name", "Value" }, 0) {
            private static final long serialVersionUID = -3762866886166776851L;

            public boolean isCellEditable(int row, int column) {
                return isModifyMode();
            }
        };

        metadataTableSorter = new TableSorter(objectMetadataTableModel);
        metadataTable = skinsFactory.createSkinnedJTable("MetadataTable");
        metadataTable.setModel(metadataTableSorter);
        metadataTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
           public void valueChanged(ListSelectionEvent e) {
               if (!e.getValueIsAdjusting() && removeMetadataItemButton != null) {
                   int row = metadataTable.getSelectedRow();
                   removeMetadataItemButton.setEnabled(row >= 0);
               }
            }
        });

        metadataTableSorter.setTableHeader(metadataTable.getTableHeader());
        metadataTableSorter.setSortingStatus(0, TableSorter.ASCENDING);
        metadataContainer.add(new JScrollPane(metadataTable), new GridBagConstraints(0, 0,
            1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsHorizontalSpace, 0, 0));

        // Add/remove buttons for metadata table.
        removeMetadataItemButton = skinsFactory.createSkinnedJButton("ObjectPropertiesAddMetadataButton");
        removeMetadataItemButton.setEnabled(false);
        removeMetadataItemButton.setToolTipText("Remove the selected metadata item(s)");
        guiUtils.applyIcon(removeMetadataItemButton, "/images/nuvola/16x16/actions/viewmag-.png");
        removeMetadataItemButton.addActionListener(this);
        removeMetadataItemButton.setActionCommand("removeMetadataItem");
        addMetadataItemButton = skinsFactory.createSkinnedJButton("ObjectPropertiesAddMetadataButton");
        addMetadataItemButton.setToolTipText("Add a new metadata item");
        guiUtils.applyIcon(addMetadataItemButton, "/images/nuvola/16x16/actions/viewmag+.png");
        addMetadataItemButton.setActionCommand("addMetadataItem");
        addMetadataItemButton.addActionListener(this);
        metadataButtonsContainer.add(removeMetadataItemButton, new GridBagConstraints(0, 0,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsZero, 0, 0));
        metadataButtonsContainer.add(addMetadataItemButton, new GridBagConstraints(1, 0,
            1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsZero, 0, 0));
        metadataContainer.add(metadataButtonsContainer, new GridBagConstraints(0, 1,
            1, 1, 1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, insetsHorizontalSpace, 0, 0));
        metadataButtonsContainer.setVisible(false);

        // OK Button.
        okButton = skinsFactory.createSkinnedJButton("ObjectPropertiesOKButton");
        okButton.setText("OK");
        okButton.setActionCommand("OK");
        okButton.addActionListener(this);

        // Cancel Button.
        cancelButton = null;
        cancelButton = skinsFactory.createSkinnedJButton("ObjectPropertiesCancelButton");
        cancelButton.setText("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setVisible(false);

        // Recognize and handle ENTER, ESCAPE, PAGE_UP, and PAGE_DOWN key presses.
        this.getRootPane().setDefaultButton(okButton);
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("ESCAPE"), "ESCAPE");
        this.getRootPane().getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -7768790936535999307L;

            public void actionPerformed(ActionEvent actionEvent) {
                setVisible(false);
            }
        });
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("PAGE_UP"), "PAGE_UP");
        this.getRootPane().getActionMap().put("PAGE_UP", new AbstractAction() {
            private static final long serialVersionUID = -6324229423705756219L;

            public void actionPerformed(ActionEvent actionEvent) {
                previousObjectButton.doClick();
            }
        });
        this.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .put(KeyStroke.getKeyStroke("PAGE_DOWN"), "PAGE_DOWN");
        this.getRootPane().getActionMap().put("PAGE_DOWN", new AbstractAction() {
            private static final long serialVersionUID = -5808972377672449421L;

            public void actionPerformed(ActionEvent actionEvent) {
                nextObjectButton.doClick();
            }
        });

        // Put it all together.
        row = 0;
        JPanel container = skinsFactory.createSkinnedJPanel("ObjectPropertiesPanel");
        container.setLayout(new GridBagLayout());
        container.add(unmodifiableAttributesPanel, new GridBagConstraints(0, row++, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));

        // Object previous and next buttons, if we have multiple objects.
        previousObjectButton = skinsFactory.createSkinnedJButton("ObjectPropertiesPreviousButton");
        guiUtils.applyIcon(previousObjectButton, "/images/nuvola/16x16/actions/1leftarrow.png");
        previousObjectButton.addActionListener(this);
        previousObjectButton.setEnabled(false);
        nextObjectButton = skinsFactory.createSkinnedJButton("ObjectPropertiesNextButton");
        guiUtils.applyIcon(nextObjectButton, "/images/nuvola/16x16/actions/1rightarrow.png");
        nextObjectButton.addActionListener(this);
        nextObjectButton.setEnabled(false);
        currentObjectLabel = skinsFactory.createSkinnedJHtmlLabel("ObjectPropertiesCurrentObjectLabel");
        currentObjectLabel.setHorizontalAlignment(JLabel.CENTER);

        nextPreviousPanel = skinsFactory.createSkinnedJPanel("ObjectPropertiesNextPreviousPanel");
        nextPreviousPanel.setLayout(new GridBagLayout());
        nextPreviousPanel.add(previousObjectButton, new GridBagConstraints(0, 0, 1, 1, 1, 0,
            GridBagConstraints.EAST, GridBagConstraints.NONE, insetsZero, 0, 0));
        nextPreviousPanel.add(currentObjectLabel, new GridBagConstraints(1, 0, 1, 1, 0, 0,
            GridBagConstraints.CENTER, GridBagConstraints.NONE, insetsHorizontalSpace, 0, 0));
        nextPreviousPanel.add(nextObjectButton, new GridBagConstraints(2, 0, 1, 1, 1, 0,
            GridBagConstraints.WEST, GridBagConstraints.NONE, insetsZero, 0, 0));
        container.add(nextPreviousPanel, new GridBagConstraints(0, row, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        nextPreviousPanel.setVisible(false);
        row++;

        JHtmlLabel metadataLabel = skinsFactory.createSkinnedJHtmlLabel("MetadataLabel");
        metadataLabel.setText("<html><b>Metadata Attributes</b></html>");
        metadataLabel.setHorizontalAlignment(JLabel.CENTER);
        container.add(metadataLabel, new GridBagConstraints(0, row++, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsVerticalSpace, 0, 0));
        container.add(metadataContainer, new GridBagConstraints(0, row++, 1, 1, 1, 1,
            GridBagConstraints.CENTER, GridBagConstraints.BOTH, insetsZero, 0, 0));

        // Destination Access Control List setting.
        destinationPanel = skinsFactory.createSkinnedJPanel("DestinationPanel");
        destinationPanel.setLayout(new GridBagLayout());

        JPanel actionButtonsPanel = skinsFactory.createSkinnedJPanel("ObjectPropertiesActionButtonsPanel");
        actionButtonsPanel.setLayout(new GridBagLayout());
        actionButtonsPanel.add(cancelButton, new GridBagConstraints(0, 0, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        actionButtonsPanel.add(okButton, new GridBagConstraints(1, 0, 1, 1, 1, 0,
            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, insetsZero, 0, 0));
        cancelButton.setVisible(false);

        container.add(actionButtonsPanel, new GridBagConstraints(0, row++, 3, 1, 0, 0, GridBagConstraints.CENTER,
            GridBagConstraints.NONE, insetsDefault, 0, 0));
        this.getContentPane().add(container);

        this.pack();
        this.setSize(new Dimension(450, 500));
        this.setLocationRelativeTo(this.getOwner());
    }

    /**
     * Update the dialog to display the attributes of a single object. The user
     * may choose which object to display by iterating forward and back through
     * the set of objects.
     */
    private void displayObjectProperties() {
        currentObject = destinationObjects[currentObjectIndex];

        // Manage previous/next buttons.
        if (destinationObjects.length > 1) {
            currentObjectLabel.setText((currentObjectIndex + 1) + " of " + destinationObjects.length);
            previousObjectButton.setEnabled(currentObjectIndex > 0);
            nextObjectButton.setEnabled(currentObjectIndex < (destinationObjects.length -1));
        }

        // Unmodifiable fields.
        objectKeyTextField.setText(currentObject.getKey());
        objectContentLengthTextField.setText(String.valueOf(currentObject.getContentLength()));
        objectLastModifiedTextField.setText(String.valueOf(currentObject.getLastModifiedDate()));
        objectETagTextField.setText(currentObject.getETag());
        bucketLocationTextField.setText(currentObject.getBucketName());
        String storageClass = (currentObject.getStorageClass() != null
            ? currentObject.getStorageClass()
            : S3Object.STORAGE_CLASS_STANDARD);
        storageClassTextField.setText(storageClass);

        if (currentObject.getOwner() != null) {
            ownerNameLabel.setVisible(true);
            ownerNameTextField.setVisible(true);
            ownerIdLabel.setVisible(true);
            ownerIdTextField.setVisible(true);
            ownerNameTextField.setText(currentObject.getOwner().getDisplayName());
            ownerIdTextField.setText(currentObject.getOwner().getId());
        } else {
            ownerNameLabel.setVisible(false);
            ownerNameTextField.setVisible(false);
            ownerIdLabel.setVisible(false);
            ownerIdTextField.setVisible(false);
        }

        // Clear old table contents
        while (objectMetadataTableModel.getRowCount() > 0) {
            objectMetadataTableModel.removeRow(0);
        }

        // Display remaining metadata items in the table.
        Iterator mdIter = currentObject.getModifiableMetadata().entrySet().iterator();
        while (mdIter.hasNext()) {
            Map.Entry entry = (Map.Entry) mdIter.next();
            Object name = entry.getKey();
            Object value = entry.getValue();
            objectMetadataTableModel.addRow(new Object[] {name, value});
        }
    }


    /**
     * Event handler for this dialog.
     */
    public void actionPerformed(ActionEvent e) {
        // Force table to accept any partial edits.
        if (metadataTable.isEditing()) {
            metadataTable.getCellEditor().stopCellEditing();
        }

        // Apply new/modified attributes to the object.
        Map currentObjectMetadata = currentObject.getModifiableMetadata();
        Set obsoleteMetadataItems = currentObjectMetadata.keySet();
        for (int row = 0; row < metadataTable.getRowCount(); row++) {
            String name = (String) objectMetadataTableModel.getValueAt(row, 0);
            String value = (String) objectMetadataTableModel.getValueAt(row, 1);
            currentObject.addMetadata(name, value);
            obsoleteMetadataItems.remove(name);
        }
        // Remove obsolete attributes.
        Iterator obsoleteNamesIter = obsoleteMetadataItems.iterator();
        while (obsoleteNamesIter.hasNext()) {
            currentObject.removeMetadata((String) obsoleteNamesIter.next());
        }

        if (e.getSource().equals(nextObjectButton)) {
            currentObjectIndex++;
            displayObjectProperties();
        } else if (e.getSource().equals(previousObjectButton)) {
            currentObjectIndex--;
            displayObjectProperties();
        } else if ("OK".equals(e.getActionCommand())) {
            modifyActionApproved = isModifyMode();
            this.setVisible(false);
        } else if ("Cancel".equals(e.getActionCommand())) {
            modifyActionApproved = false;
            this.setVisible(false);
        } else if ("addMetadataItem".equals(e.getActionCommand())) {
            int newRowNumber = metadataTable.getRowCount() + 1;
            objectMetadataTableModel.addRow(
                new String[] {"name-" + newRowNumber, "value-" + newRowNumber});
        } else if ("removeMetadataItem".equals(e.getActionCommand())) {
            int[] rows = metadataTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                int modelIndex = metadataTableSorter.modelIndex(rows[i]);
                objectMetadataTableModel.removeRow(modelIndex);
            }
        }
    }

    /**
     * @return
     * true if the dialog allows the user to modify object metadata.
     */
    public boolean isModifyMode() {
        return modifyMode;
    }

    /**
     * @return
     * true if the user approved the dialog to indicate that objects should be
     * updated, false if the user cancelled the dialog.
     */
    public boolean isModifyActionApproved() {
        return modifyActionApproved;
    }

    /**
     * @return
     * the original key names of the S3 objects that should be modified when
     * this dialog is accepted. Because objects are modified in-place, these
     * source key names will match the key names of the destination objects
     * available from the {@link #getDestinationObjects()} method.
     */
    public String[] getSourceObjectKeys() {
        String[] sourceObjectKeys = new String[destinationObjects.length];
        for (int i = 0; i < destinationObjects.length; i++) {
            sourceObjectKeys[i] = destinationObjects[i].getKey();
        }
        return sourceObjectKeys;
    }

    /**
     * @return
     * objects containing updated metadata and Access Control List settings
     * provided by the user. When this dialog is approved, the S3 objects
     * should be updated in-place by copying over each object with an updated
     * version from this list.
     */
    public S3Object[] getDestinationObjects() {
        if (!isModifyMode()) {
            return null;
        }
        return destinationObjects;
    }

}
