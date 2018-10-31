package org.jets3t.service.multi;

import org.jets3t.service.ServiceException;
import org.jets3t.service.model.ThrowableBearingStorageObject;

/**
 * Performs logic to determine whether a given error should be "permitted"
 * by compatible parts of the JetS3t multipart service infrastructure.
 * Permitted errors will result in a {@link ThrowableBearingStorageObject}
 * being generated instead of an operation being cancelled by a raised
 * exception.
 *
 * @author jmurty
 */
public class ErrorPermitter {

    /**
     * Returns true in all cases. Override this class and method to
     * perform sensible logic.
     *
     * @param throwable
     * @return
     * true if error should be permitted
     */
    public boolean isPermitted(Throwable throwable) {
        return true;
    }

    /**
     * Returns true in all cases. Override this class and method to
     * perform sensible logic.
     *
     * @param serviceException
     * @return
     * true if error should be permitted
     */
    public boolean isPermitted(ServiceException serviceException) {
        return true;
    }

}
