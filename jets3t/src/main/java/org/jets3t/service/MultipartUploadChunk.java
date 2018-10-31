/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2012 by Aspera
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
package org.jets3t.service;

import org.jets3t.service.model.MultipartUpload;


/**
 * Stores a "chunk" of MultipartUpload returned from a 'multipart list uploads'
 * command - this particular chunk may or may not include all the multipart
 * upload started in a bucket.
 *
 * This class contains an array of MultipartUpload and a the last key name
 * returned by a prior call to the method
 * {@link S3Service#listObjectsChunked(String, String, String, long, String)}.
 *
 * <P>
 *
 * @author Gilles Gaillard
 */

public class MultipartUploadChunk {

    private static final MultipartUpload[] EMPTY_UPLOADS = {};
    private static final String[] EMPTY_STRINGS = {};

    protected String prefix = null;
    protected String delimiter = null;
    protected MultipartUpload[] uploads = null;
    protected String[] commonPrefixes = null;
    protected String priorLastKey = null;
    protected String priorLastIdMarker = null;

    public MultipartUploadChunk(String prefix, String delimiter,
        MultipartUpload[] uploads, String[] commonPrefixes, String priorLastKey,
        String priorLastIdMarker)
    {
        this.prefix = prefix;
        this.delimiter = delimiter;
        this.uploads = uploads;
        this.commonPrefixes = commonPrefixes;
        this.priorLastKey = priorLastKey;
        this.priorLastIdMarker = priorLastIdMarker;
    }

    /**
     * @return
     * the uploads in this chunk.
     */
    public MultipartUpload[] getUploads() {
        return uploads == null ? EMPTY_UPLOADS : uploads;
    }

    /**
     * @return
     * the common prefixes in this chunk.
     */
    public String[] getCommonPrefixes() {
        return commonPrefixes == null ? EMPTY_STRINGS : commonPrefixes;
    }

    /**
     * @return
     * the last key returned by the previous chunk if that chunk was incomplete, null otherwise.
     */
    public String getPriorLastKey() {
        return priorLastKey;
    }

    /**
     * @return
     * the last id marker returned by the previous chunk if that chunk was incomplete, null otherwise.
     */
    public String getPriorLastIdMarker() {
        return priorLastIdMarker;
    }

    /**
     * @return
     * the prefix applied when this upload chunk was generated. If no prefix was
     * applied, this method will return null.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * @return
     * the delimiter applied when this upload chunk was generated. If no
     * delimiter was applied, this method will return null.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * A convenience method to check whether a listing of uploads is complete
     * (true) or there are more uploads available (false). Just a synonym for
     * <code>{@link #getPriorLastKey()} == null &amp;&amp; {@link #getPriorLastIdMarker()}==null}</code>.
     *
     * @return
     * true if the listing is complete and there are no more unlisted
     * uploads, false if follow-up requests will return more uploads.
     */
    public boolean isListingComplete() {
        return priorLastKey == null && priorLastIdMarker==null;
    }

} //MultipartUploadChunk