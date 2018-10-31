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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import javax.swing.table.DefaultTableModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jets3t.service.model.S3Object;

/**
 * A table model to store {@link S3Object}s.
 *
 * @author James Murty
 */
public class CLObjectTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 8570725021470237261L;

    private static final Log log = LogFactory.getLog(CLObjectTableModel.class);

    private ArrayList objectList = new ArrayList();
    private String usersPath = "";

    public CLObjectTableModel() {
        super(new String[] {"File","Size","Last Modified","Public?"}, 0);
    }

    public void setUsersPath(String usersPath) {
        this.usersPath = usersPath;
    }

    private void sanitizeObjectKey(S3Object object) {
        if (object.getKey().startsWith(usersPath)) {
            object.setKey(object.getKey().substring(usersPath.length()));
        }
    }

    public int addObject(S3Object object) {
        sanitizeObjectKey(object);

        int insertRow =
            Collections.binarySearch(objectList, object, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((S3Object)o1).getKey().compareToIgnoreCase(((S3Object)o2).getKey());
                }
            });

        String aclStatus = null;
        if (insertRow >= 0) {
            // Retain the object's ACL status if it's available.
            aclStatus = (String) this.getValueAt(insertRow, 3);

            // We already have an item with this key, replace it.
            objectList.remove(insertRow);
            this.removeRow(insertRow);
        } else {
            insertRow = (-insertRow) - 1;
        }

        if (object.getAcl() != null || aclStatus == null) {
            aclStatus = CockpitLite.getAclDescription(object.getAcl());
        }

        // New object to insert.
        objectList.add(insertRow, object);
        this.insertRow(insertRow, new Object[] {object.getKey(),
            new Long(object.getContentLength()), object.getLastModifiedDate(),
            aclStatus});

        return insertRow;
    }

    public int updateObjectAclStatus(S3Object objectWithAcl, String aclStatus) {
        sanitizeObjectKey(objectWithAcl);

        int updateRow =
            Collections.binarySearch(objectList, objectWithAcl, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((S3Object)o1).getKey().compareToIgnoreCase(((S3Object)o2).getKey());
                }
            });
        if (updateRow >= 0) {
            this.setValueAt(aclStatus, updateRow, 3);
        } else {
            // Object isn't in table!
            log.warn("Cannot find object named '" + objectWithAcl.getKey() + "' in objects table");
        }
        return updateRow;
    }

    public String getObjectAclStatus(S3Object objectWithAcl) {
        synchronized (objectList) {
            int updateRow =
                Collections.binarySearch(objectList, objectWithAcl, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        return ((S3Object)o1).getKey().compareToIgnoreCase(((S3Object)o2).getKey());
                    }
                });
            if (updateRow >= 0) {
                return (String) this.getValueAt(updateRow, 3);
            } else {
                return null;
            }
        }
    }

    public void addObjects(S3Object[] objects) {
        for (int i = 0; i < objects.length; i++) {
            addObject(objects[i]);
        }
    }

    public void removeObject(S3Object object) {
        sanitizeObjectKey(object);

        int row =
            Collections.binarySearch(objectList, object, new Comparator() {
                public int compare(Object o1, Object o2) {
                    return ((S3Object)o1).getKey().compareToIgnoreCase(((S3Object)o2).getKey());
                }
            });
        if (row >= 0) {
            this.removeRow(row);
            objectList.remove(row);
        }
    }

    public void removeAllObjects() {
        int rowCount = this.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            this.removeRow(0);
        }
        objectList.clear();
    }

    public S3Object getObject(int row) {
        synchronized (objectList) {
            return (S3Object) objectList.get(row);
        }
    }

    public S3Object[] getObjects() {
        synchronized (objectList) {
            return (S3Object[]) objectList.toArray(new S3Object[objectList.size()]);
        }
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return String.class;
        } else if (columnIndex == 1) {
            return Long.class;
        } else if (columnIndex == 2) {
            return Date.class;
        } else {
            return String.class;
        }
    }

}
