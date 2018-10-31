package org.jets3t.service.model;

/**
 * A perverted StorageObject subclass intended to error information along
 * with basic object data through the existing JetS3t "plumbing", such as
 * for cases where we need to return error information from low level
 * methods without failing an operation by throwing an exception.
 *
 * WARNING: The only data likely to be available from instances of this
 * class are the object key name via {@link #getName()} and the throwable
 * that caused its generation via {@link #getThrowable()}.
 *
 * @author jmurty michaelthoward
 */
public class ThrowableBearingStorageObject extends StorageObject {

    protected Throwable throwable = null;

	// mth 2014-01-26
	// unclear to me how many of the inherited StorageObject methods
	// should be overridden so that they return null.
	// or if they should throw an exception

    public ThrowableBearingStorageObject(String key, Throwable throwable) {
        super(key);
        this.throwable = throwable;
    }

    public Throwable getThrowable() {
        return throwable;
    }

	@Override
	public long getContentLength() {
		return -1;
	}

     @Override
    public String toString() {
        return "ErrorBearingStorageObject [key=" + getKey() + ", throwable=" + throwable + "]";
    }

}
