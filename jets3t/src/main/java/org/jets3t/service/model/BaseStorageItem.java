/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2006-2015 James Murty
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
package org.jets3t.service.model;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jets3t.service.Constants;

/**
 * Base class to represent storage items that can contain metadata: both objects and buckets.
 *
 * @author James Murty
 */
public abstract class BaseStorageItem {
    /*
     * Standard HTTP metadata/header names.
     */
    public static final String METADATA_HEADER_CREATION_DATE = "Date";
    public static final String METADATA_HEADER_LAST_MODIFIED_DATE = "Last-Modified";
    public static final String METADATA_HEADER_DATE = "Date";
    public static final String METADATA_HEADER_CONTENT_MD5 = "Content-MD5";
    public static final String METADATA_HEADER_CONTENT_LENGTH = "Content-Length";
    public static final String METADATA_HEADER_CONTENT_TYPE = "Content-Type";
    public static final String METADATA_HEADER_CONTENT_ENCODING = "Content-Encoding";
    public static final String METADATA_HEADER_CONTENT_DISPOSITION = "Content-Disposition";
    public static final String METADATA_HEADER_CONTENT_LANGUAGE = "Content-Language";
    public static final String METADATA_HEADER_ETAG = "ETag";

    /*
     * Metadata names common to S3 and Google Storage.
     */

    private String name = null;
    private StorageOwner owner = null;

    /**
     *  Map to metadata associated with this object.
     */
    private final Map<String, Object> metadata = new HashMap<String, Object>();


    protected BaseStorageItem(String name) {
        this.name = name;
    }

    protected BaseStorageItem() {
    }

    /**
     * @return
     * the name of the bucket.
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of the bucket.
     * @param name the name for the bucket
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return
     * an <b>immutable</b> map containing the basic metadata associated with this object,
     * with case-sensitive name strings as keys.
     */
    public Map<String, Object> getMetadataMap() {
        return Collections.unmodifiableMap(this.metadata);
    }

    protected Map<String, Object> lookupMetadataSubsetMap(String keyname) {
        Map<String, Object> map = (Map<String, Object>) this.metadata.get(keyname);
        if (map == null) {
            map = new HashMap<String, Object>();
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * @return
     * an <b>immutable</b> map containing the user metadata associated with this object,
     * with case-sensitive name strings as keys.
     * Note: this map will not be populated in all cases where basic metadata is available
     * from {@link #getMetadata(String)}.
     */
    public Map<String, Object> getUserMetadataMap() {
        return lookupMetadataSubsetMap(Constants.KEY_FOR_USER_METADATA);
    }

    /**
     * @return
     * an <b>immutable</b> map containing the service metadata associated with this object,
     * with case-sensitive name strings as keys.
     * Note: this map will not be populated in all cases where basic metadata is available
     * from {@link #getMetadata(String)}.
     */
    public Map<String, Object> getServiceMetadataMap() {
        return lookupMetadataSubsetMap(Constants.KEY_FOR_SERVICE_METADATA);
    }

    /**
     * @return
     * an <b>immutable</b> map containing the HTTP metadata associated with this object,
     * with case-sensitive name strings as keys.
     * Note: this map will not be populated in all cases where basic metadata is available
     * from {@link #getMetadata(String)}.
     */
    public Map<String, Object> getHttpMetadataMap() {
        return lookupMetadataSubsetMap(Constants.KEY_FOR_HTTP_METADATA);
    }

    /**
     * @return
     * an <b>immutable</b> map containing the complete metadata associated with this object,
     * with case-sensitive name strings as keys.
     * Note: this map will not be populated in all cases where basic metadata is available
     * from {@link #getMetadata(String)}.
     */
    public Map<String, Object> getCompleteMetadataMap() {
        return lookupMetadataSubsetMap(Constants.KEY_FOR_COMPLETE_METADATA);
    }

    /**
     * @param name1
     * @param name2
     * @return
     * Return true if the given string Metadata item names are equivalent, i.e.
     * either both are null, or both are a case-insensitive match.
     */
    protected boolean isMatchingMetadataName(String name1, String name2) {
       if (name1 == null && name2 == null) {
           return true;
       }
       // No match if one or other is null, but both are not
       if (name1 == null || name2 == null) {
           return false;
       }
       // Match if lower-cased names are equivalent
       return name1.toLowerCase().equals(name2.toLowerCase());
    }

    /**
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * the value of the first item found with the given case-insensitive name
     * in the map, or null if no such name exists in the map
     */
    protected Object getMetadataCaseInsensitiveFromMap(
        String name, Map<String, Object> map)
    {
        for (Entry<String, Object> entry: map.entrySet()) {
            if (isMatchingMetadataName(entry.getKey(), name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * the value of the metadata with the given case-insensitive name, or
     * null if no such metadata item exists.
     */
    public Object getMetadata(String name) {
        // Prefer metadata values set from HTTP response headers if one is
        // available, to solve problems with user-set metadata clobbering
        // the definitive values received from HTTP, see #213
        Object httpMetadataValue = getHttpMetadata(name);
        if (httpMetadataValue != null) {
            return httpMetadataValue;
        }
        return getMetadataCaseInsensitiveFromMap(name, this.metadata);
    }

    /**
     * Return true if a metdata data item with the given name (case-insensitive)
     * is present.
     *
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * true if this object contains a metadata item with the given name, false otherwise.
     */
    public boolean containsMetadata(String name) {
        for (Entry<String, Object> entry: this.metadata.entrySet()) {
            if (isMatchingMetadataName(entry.getKey(), name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * the value of the metadata with the given case-insensitive name within
     * the *service-supplied* metadata items -- that is, from HTTP headers not
     * user-set metadata values -- or null if no such metadata item exists.
     */
    public Object getServiceMetadata(String name) {
        return getMetadataCaseInsensitiveFromMap(name, getServiceMetadataMap());
    }

    /**
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * the value of the metadata with the given case-insensitive name within
     * the *user-set* metadata items -- that is, not from HTTP headers returned
     * by the service -- or null if no such metadata item exists.
     */
    public Object getUserMetadata(String name) {
        return getMetadataCaseInsensitiveFromMap(name, getUserMetadataMap());
    }

    /**
     * @param name
     * the metadata item name, case-insensitive.
     *
     * @return
     * the value of the metadata with the given case-insensitive name within
     * the *user-set* metadata items -- that is, not from HTTP headers returned
     * by the service -- or null if no such metadata item exists.
     */
    public Object getHttpMetadata(String name) {
        return getMetadataCaseInsensitiveFromMap(name, getHttpMetadataMap());
    }

    /**
     * Add a metadata entry with the given name.
     *
     * Metadata item names are treated as case-insensitive when you set/get values.
     * If a name values being set matches an existing metdata item (even if
     * case is different) the original value will be replaced with the new one.
     *
     * The case of metadata item names is preserved when items are stored so the
     * original names are accessible via {@link #getMetadataMap()}, but case is
     * otherwise ignored.
     *
     * In other words, if you set two metadata items with the names "ETag" and "Etag"
     * only one value will be stored, whichever was set most recently.
     *
     * @param name
     * the metadata item name, case-insensitive.
     * @param value
     * the metadata item value.
     */
    protected void addMetadata(String name, Object value) {
        this.removeMetadata(name);
        this.metadata.put(name, value);
    }

    /**
     * Adds a String metadata item to the object.
     *
     * @param name
     * the metadata item name, case-insensitive.
     * @param value
     * the metadata item's date value.
     */
    public void addMetadata(String name, String value) {
        this.addMetadata(name, (Object) value);
    }

    /**
     * Adds a Date metadata item to the object.
     *
     * @param name
     * the metadata item name, case-insensitive.
     * @param value
     * the metadata item's date value.
     */
    public void addMetadata(String name, Date value) {
        this.addMetadata(name, (Object) value);
    }

    /**
     * Adds an owner metadata item to the object.
     *
     * @param name
     * the metadata item name, case-insensitive.
     * @param value
     * the metadata item's owner value.
     */
    public void addMetadata(String name, StorageOwner value) {
        this.addMetadata(name, (Object) value);
    }

    /**
     * Adds all the items in the provided map to this object's metadata.
     *
     * @param metadataToAdd
     * metadata items to add, names are case-insensitive.
     */
    public void addAllMetadata(Map<String, Object> metadataToAdd) {
        for (Entry<String, Object> entry: metadataToAdd.entrySet()) {
            this.addMetadata(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes a metadata item from the object.
     *
     * @param name
     * the name of the metadata item to remove, case-insensitive.
     */
    public void removeMetadata(String name) {
        String existingItemKey = null;
        for (Entry<String, Object> entry: this.metadata.entrySet()) {
            if (isMatchingMetadataName(entry.getKey(), name)) {
                existingItemKey = entry.getKey();
            }
        }
        // Remove existing matching entry, if present.
        if (existingItemKey != null || name == null) {
            this.metadata.remove(existingItemKey);
        }
    }

    /**
     * Removes all the metadata items associated with this object, then adds all the items
     * in the provided map. After performing this operation, the metadata list will contain
     * only those items in the provided map.
     *
     * @param metadata
     * metadata items to add.
     */
    public void replaceAllMetadata(Map<String, Object> metadata) {
        this.metadata.clear();
        this.addAllMetadata(metadata);
    }

    /**
     * @return
     * this object's owner, or null if the owner is not available.
     */
    public StorageOwner getOwner() {
        return this.owner;
    }

    /**
     * Set this object's owner object based on information returned from the service.
     * This method should only by used by code that reads service responses.
     *
     * @param owner
     */
    public void setOwner(StorageOwner owner) {
        this.owner = owner;
    }

}
