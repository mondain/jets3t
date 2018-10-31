package org.jets3t.service.model;

import java.io.InputStream;

public class MissingStorageObject extends StorageObject {
	
	// mth 2014-01-26
	// unclear to me how many of the inherited StorageObject methods
	// should be overridden so that they return null. 
	// or if they should throw an exception
	
	@Override
	public long getContentLength() {
		return -1;
	}

     @Override
    public String toString() {
        return "MissingStorageObject [bucket=" + getBucketName() + " key=" + getKey() + "]";
    }

}
