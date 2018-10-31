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
package org.jets3t.apps.uploader;

import java.io.File;
import javax.swing.filechooser.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Defines which files can be selected within the Uploader's file chooser
 * for upload to S3. Files are filtered based on their filename extension.
 *
 * @author James Murty
 */
public class UploaderFileExtensionFilter extends FileFilter {
    private String description = null;
    private List acceptableFileExtensionsList = null;

    /**
     * Construct an extension-based file filter
     *
     * @param description
     * the name for this filter, such as "Movie files"
     * @param fileExtensionsList
     * a list of file extensions that the filter will accept, eg "avi", "mpg".
     */
    public UploaderFileExtensionFilter(String description, List fileExtensionsList) {
        this.description = description;
        acceptableFileExtensionsList = new ArrayList();
        for (Iterator iter = fileExtensionsList.iterator(); iter.hasNext();) {
            String extension = iter.next().toString();
            // Convert to lower case for case-insensitive matches.
            extension = extension.toLowerCase(Locale.getDefault());
            acceptableFileExtensionsList.add(extension);
        }
    }

    /**
     * @return
     * True if the file is a Directory, or the file has an extension that matches one of the
     * allowed extensions provided to this class's constructor. False otherwise.
     */
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        String fileName = file.getName();
        if (fileName.indexOf(".") >= 0 && fileName.lastIndexOf(".") < fileName.length()) {
            String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1);
            // Conver to lower case for case-insensitive matching.
            fileExt = fileExt.toLowerCase(Locale.getDefault());
            return (acceptableFileExtensionsList.contains(fileExt));
        }
        return false;
    }

    public String getDescription() {
        return description;
    }

}
