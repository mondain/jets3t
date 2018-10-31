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
package org.jets3t.apps.cockpit.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableModel;

import org.jets3t.gui.GuiUtils;
import org.jets3t.service.model.S3Bucket;

/**
 * A table model to store {@link S3Bucket}s.
 *
 * @author James Murty
 */
public class BucketTableModel extends DefaultTableModel {
    private static final long serialVersionUID = 7957867859342194534L;

    private GuiUtils guiUtils = new GuiUtils();
    private ArrayList bucketList = new ArrayList();

    private boolean includeDistributions = false;
    private Icon distributionActiveIcon = null;

    public BucketTableModel(boolean includeDistributions) {
        super(includeDistributions
            ? new String[] {"Bucket Name", ""}
            : new String[] {"Bucket Name"},
            0);

        this.includeDistributions = includeDistributions;

        JLabel dummyLabel = new JLabel();
        if (guiUtils.applyIcon(dummyLabel, "/images/nuvola/16x16/actions/irkick.png"))
        {
            distributionActiveIcon = dummyLabel.getIcon();
        }
    }

    protected int findBucketsIndex(S3Bucket bucket) {
        return Collections.binarySearch(
            bucketList, new S3BucketAndDistributionFlag(bucket, false), new Comparator() {
                public int compare(Object o1, Object o2) {
                    String b1Name = ((S3BucketAndDistributionFlag)o1).getS3Bucket().getName();
                    String b2Name = ((S3BucketAndDistributionFlag)o2).getS3Bucket().getName();
                    int result =  b1Name.compareTo(b2Name);
                    return result;
                }
            }
        );
    }

    public int addBucket(S3Bucket bucket, boolean hasDistributions) {
        int insertRow = findBucketsIndex(bucket);
        if (insertRow >= 0) {
            // We already have an item with this key, replace it.
            bucketList.remove(insertRow);
            this.removeRow(insertRow);
        } else {
            insertRow = (-insertRow) - 1;
        }
        // New object to insert.
        bucketList.add(insertRow, new S3BucketAndDistributionFlag(bucket, hasDistributions));
        if (this.includeDistributions) {
            Boolean flag = hasDistributions ? Boolean.TRUE : Boolean.FALSE;
            this.insertRow(insertRow, new Object[] {bucket.getName(), flag});
        } else {
            this.insertRow(insertRow, new Object[] {bucket.getName()});
        }
        return insertRow;
    }

    public void removeBucket(S3Bucket bucket) {
        int index = findBucketsIndex(bucket);
        this.removeRow(index);
        bucketList.remove(index);
    }

    public void removeAllBuckets() {
        int rowCount = this.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            this.removeRow(0);
        }
        bucketList.clear();
    }

    public S3Bucket getBucket(int row) {
        return ((S3BucketAndDistributionFlag)bucketList.get(row)).getS3Bucket();
    }

    public S3Bucket[] getBuckets() {
        S3Bucket[] buckets = new S3Bucket[bucketList.size()];
        for (int i = 0; i < bucketList.size(); i++) {
            buckets[i] = getBucket(i);
        }
        return buckets;
    }

    public int getBucketIndexByName(String name) {
        synchronized (bucketList) {
            for (int index=0; index < bucketList.size(); index++) {
                S3Bucket bucket = getBucket(index);
                if (bucket.getName().equals(name)) {
                    return index;
                }
            }
            return -1;
        }
    }

    /**
     * @return
     * true if the distributions flag is true for at least one bucket.
     */
    public boolean hasDistributions() {
        for (int i = 0; i < bucketList.size(); i++) {
            if ( ((S3BucketAndDistributionFlag)bucketList.get(i)).distributionFlag ) {
                return true;
            }
        }
        return false;
    }

    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public Class getColumnClass(int columnIndex) {
        if (columnIndex == 1) {
            if (distributionActiveIcon != null) {
                return ImageIcon.class;
            } else {
                return Boolean.class;
            }
        } else {
            return String.class;
        }
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 1 && distributionActiveIcon != null) {
            if (((S3BucketAndDistributionFlag)bucketList.get(rowIndex)).hasDistribution()) {
                return distributionActiveIcon;
            }
        }
        return super.getValueAt(rowIndex, columnIndex);
    }


    private class S3BucketAndDistributionFlag {
        private S3Bucket bucket = null;
        private boolean distributionFlag = false;

        public S3BucketAndDistributionFlag(S3Bucket bucket, boolean distributionFlag) {
            this.bucket = bucket;
            this.distributionFlag = distributionFlag;
        }

        public S3Bucket getS3Bucket() { return bucket; }
        public boolean hasDistribution() { return distributionFlag; }
    }

}
