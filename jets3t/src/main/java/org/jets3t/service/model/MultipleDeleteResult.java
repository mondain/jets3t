/*
 * JetS3t : Java S3 Toolkit
 * Project hosted at http://bitbucket.org/jmurty/jets3t/
 *
 * Copyright 2011 James Murty
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

import java.util.List;

/**
 * Represents the result of multiple object delete requests.
 *
 * @author jmurty
 */
public class MultipleDeleteResult {
    protected List<DeletedObjectResult> deletedObjectResults;
    protected List<ErrorResult> errorResults;

    public MultipleDeleteResult()
    {
    }

    public MultipleDeleteResult(List<DeletedObjectResult> deletedObjectResults,
        List<ErrorResult> errorResults)
    {
        this.deletedObjectResults = deletedObjectResults;
        this.errorResults = errorResults;
    }

    /**
     * @return
     * Information about objects that were successfully deleted.
     * Note that if the multiple delete is performed in "quiet" mode, this list
     * will be empty even if objects were successfully deleted.
     */
    public List<DeletedObjectResult> getDeletedObjectResults() {
        return deletedObjectResults;
    }

    public void setDeletedObjectResults(
        List<DeletedObjectResult> deletedObjectResults) {
        this.deletedObjectResults = deletedObjectResults;
    }

    /**
     * @return
     * Error code and message for objects that could not be deleted.
     */
    public List<ErrorResult> getErrorResults() {
        return errorResults;
    }

    public void setErrorResults(List<ErrorResult> errorResults) {
        this.errorResults = errorResults;
    }

    /**
     * @return
     * True if there is one or more error results, false otherwise.
     */
    public boolean hasErrors() {
        return getErrorResults().size() > 0;
    }

    public class DeletedObjectResult {
        String key, version, deleteMarkerVersion;
        Boolean withDeleteMarker;

        public DeletedObjectResult(String key, String version, Boolean withDeleteMarker,
            String deleteMarkerVersion)
        {
            this.key = key;
            this.version = version;
            this.withDeleteMarker = withDeleteMarker;
            this.deleteMarkerVersion = deleteMarkerVersion;
        }

        public String getKey() {
            return key;
        }

        public String getVersion() {
            return version;
        }

        public String getDeleteMarkerVersion() {
            return deleteMarkerVersion;
        }

        public Boolean getWithDeleteMarker() {
            return withDeleteMarker;
        }
    }

    public class ErrorResult {
        String key, version, errorCode, message;

        public ErrorResult(String key, String version, String errorCode, String message) {
            this.key = key;
            this.version = version;
            this.errorCode = errorCode;
            this.message = message;
        }

        public String getKey() {
            return key;
        }

        public String getVersion() {
            return version;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getMessage() {
            return message;
        }
    }

}
