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
package org.jets3t.service.multithread;

import org.jets3t.service.model.S3Object;

/**
 * Multi-threaded service event fired by {@link S3ServiceMulti#putACLs}.
 * <p>
 * EVENT_IN_PROGRESS events include an array of the {@link S3Object}s that have had their ACLs updated
 * since the last progress event was fired. These objects are available via
 * {@link #getObjectsWithUpdatedACL()}.
 * <p>
 * EVENT_CANCELLED events include an array of the {@link S3Object}s that did not have their ACLs updated
 * before the operation was cancelled. These objects are available via
 * {@link #getCancelledObjects()}.
 *
 * @author James Murty
 */
public class UpdateACLEvent extends ServiceEvent {
    private S3Object[] objects = null;

    private UpdateACLEvent(int eventCode, Object uniqueOperationId) {
        super(eventCode, uniqueOperationId);
    }


    public static UpdateACLEvent newErrorEvent(Throwable t, Object uniqueOperationId) {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_ERROR, uniqueOperationId);
        event.setErrorCause(t);
        return event;
    }

    public static UpdateACLEvent newStartedEvent(ThreadWatcher threadWatcher, Object uniqueOperationId) {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_STARTED, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        return event;
    }

    public static UpdateACLEvent newInProgressEvent(ThreadWatcher threadWatcher,
        S3Object[] completedObjects, Object uniqueOperationId)
    {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_IN_PROGRESS, uniqueOperationId);
        event.setThreadWatcher(threadWatcher);
        event.setObjects(completedObjects);
        return event;
    }

    public static UpdateACLEvent newCompletedEvent(Object uniqueOperationId) {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_COMPLETED, uniqueOperationId);
        return event;
    }

    public static UpdateACLEvent newCancelledEvent(S3Object[] incompletedObjects, Object uniqueOperationId) {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_CANCELLED, uniqueOperationId);
        event.setObjects(incompletedObjects);
        return event;
    }

    public static UpdateACLEvent newIgnoredErrorsEvent(ThreadWatcher threadWatcher,
        Throwable[] ignoredErrors, Object uniqueOperationId)
    {
        UpdateACLEvent event = new UpdateACLEvent(EVENT_IGNORED_ERRORS, uniqueOperationId);
        event.setIgnoredErrors(ignoredErrors);
        return event;
    }


    private void setObjects(S3Object[] objects) {
        this.objects = objects;
    }

    /**
     * @return
     * the S3Objects that have had their ACLs updated since the last progress event was fired.
     * @throws IllegalStateException
     * updated objects are only available from EVENT_IN_PROGRESS events.
     */
    public S3Object[] getObjectsWithUpdatedACL() throws IllegalStateException {
        if (getEventCode() != EVENT_IN_PROGRESS) {
            throw new IllegalStateException("Completed Objects are only available from EVENT_IN_PROGRESS events");
        }
        return objects;
    }

    /**
     * @return
     * the S3Objects that did not have ACLs updated before the operation was cancelled.
     * @throws IllegalStateException
     * cancelled objects are only available from EVENT_CANCELLED events.
     */
    public S3Object[] getCancelledObjects() throws IllegalStateException {
        if (getEventCode() != EVENT_CANCELLED) {
            throw new IllegalStateException("Cancelled Objects are  only available from EVENT_CANCELLED events");
        }
        return objects;
    }

}
