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
package org.jets3t.apps.cockpit;

import java.io.Serializable;
import java.util.Properties;

import org.jets3t.service.Constants;
import org.jets3t.service.Jets3tProperties;
import org.jets3t.service.model.S3Object;

/**
 * <p>
 * Stores Cockpit's preferences as set by the user via the
 * {@link org.jets3t.apps.cockpit.gui.PreferencesDialog}.
 * </p>
 *
 * @author James Murty
 */
public class CockpitPreferences implements Serializable {
    private static final long serialVersionUID = 6072192057121567975L;

    /**
     * Represents ACL permissions to make objects private.
     */
    public static final String UPLOAD_ACL_PERMISSION_PRIVATE = "PRIVATE";

    /**
     * Represents ACL permissions to make objects readable by anyone.
     */
    public static final String UPLOAD_ACL_PERMISSION_PUBLIC_READ = "PUBLIC_READ";

    /**
     * Represents ACL permissions to make objects readable and writable by anyone.
     */
    public static final String UPLOAD_ACL_PERMISSION_PUBLIC_READ_WRITE = "PUBLIC_READ_WRITE";

    private boolean rememberPreferences = true;
    private String uploadACLPermission = UPLOAD_ACL_PERMISSION_PRIVATE;
    private boolean uploadCompressionActive = false;
    private boolean uploadEncryptionActive = false;
    private String uploadStorageClass =
        Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
            .getStringProperty("s3service.default-storage-class",
                S3Object.STORAGE_CLASS_STANDARD);
    private String encryptionPassword = null;
    private String encryptionAlgorithm =
        Jets3tProperties.getInstance(Constants.JETS3T_PROPERTIES_FILENAME)
            .getStringProperty("crypto.algorithm", "PBEWithMD5AndDES");

    public String getEncryptionPassword() {
        return encryptionPassword;
    }

    public void setEncryptionPassword(String encryptionPasswrod) {
        this.encryptionPassword = encryptionPasswrod;
    }

    public String getEncryptionAlgorithm() {
        return encryptionAlgorithm;
    }

    public void setEncryptionAlgorithm(String encryptionAlgorithm) {
        this.encryptionAlgorithm = encryptionAlgorithm;
    }

    public boolean isEncryptionPasswordSet() {
        return
            this.encryptionPassword != null
            && this.encryptionPassword.length() > 0;
    }

    /**
     * @return
     * the ACL permission setting, which will match one of the <tt>UPLOAD_ACL_PERMISSION_xyz</tt>
     * constants contained in this class.
     */
    public String getUploadACLPermission() {
        return uploadACLPermission;
    }

    /**
     * Set the ACL permissions string setting.
     *
     * @param uploadACLPermission
     * the ACL permission setting, which must match one of the <tt>UPLOAD_ACL_PERMISSION_xyz</tt>
     * constants contained in this class.
     */
    public void setUploadACLPermission(String uploadACLPermission) {
        if (!UPLOAD_ACL_PERMISSION_PRIVATE.equals(uploadACLPermission)
            && !UPLOAD_ACL_PERMISSION_PUBLIC_READ.equals(uploadACLPermission)
            && !UPLOAD_ACL_PERMISSION_PUBLIC_READ_WRITE.equals(uploadACLPermission))
        {
            throw new IllegalArgumentException("ACL Permission string is not a legal value: "
                + uploadACLPermission);
        }
        this.uploadACLPermission = uploadACLPermission;
    }

    public boolean isUploadCompressionActive() {
        return uploadCompressionActive;
    }

    public void setUploadCompressionActive(boolean uploadCompressionActive) {
        this.uploadCompressionActive = uploadCompressionActive;
    }

    public boolean isUploadEncryptionActive() {
        return uploadEncryptionActive;
    }

    public void setUploadEncryptionActive(boolean uploadEncryptionActive) {
        this.uploadEncryptionActive = uploadEncryptionActive;
    }

    public String getUploadStorageClass() {
        return this.uploadStorageClass;
    }

    public void setUploadStorageClass(String storageClass) {
        this.uploadStorageClass = storageClass;
    }

    public void setRememberPreferences(boolean rememberPreferences) {
        this.rememberPreferences = rememberPreferences;
    }

    public boolean isRememberPreferences() {
        return rememberPreferences;
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.setProperty("upload-acl-permission", getUploadACLPermission());
        properties.setProperty("upload-compression-active",
            isUploadCompressionActive() ? "true" : "false");
        properties.setProperty("upload-encryption-active",
            isUploadEncryptionActive() ? "true" : "false");
        properties.setProperty("upload-storage-class", getUploadStorageClass());
        properties.setProperty("upload-encryption-algorithm", getEncryptionAlgorithm());
        return properties;
    }

    public void fromProperties(Properties properties) {
        setRememberPreferences(true);
        setUploadACLPermission(
            properties.getProperty("upload-acl-permission", getUploadACLPermission()));
        setUploadCompressionActive(
            "true".equalsIgnoreCase(properties.getProperty("upload-compression-active")));
        setUploadEncryptionActive(
            "true".equalsIgnoreCase(properties.getProperty("upload-encryption-active")));
        setUploadStorageClass(
            properties.getProperty("upload-storage-class", getUploadStorageClass()));
        setEncryptionAlgorithm(
            properties.getProperty("upload-encryption-algorithm", getEncryptionAlgorithm()));
    }

}
