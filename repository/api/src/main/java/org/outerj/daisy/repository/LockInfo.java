/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository;

import org.outerx.daisy.x10.LockInfoDocument;

import java.util.Date;

/**
 * Provides information about a lock on a document variant.
 *
 * <p>If the method {@link #hasLock()} returns false, there is no lock
 * on the document and the output of the other methods is irrelevant and
 * unspecified.
 *
 * <p>Note that so called "optimistic locking" always happens automatically,
 * next to this explicit locking facility.
 */
public interface LockInfo {
    boolean hasLock();

    /**
     * Get the id of the user holding the lock.
     */
    long getUserId();

    /**
     * Get the time when the lock was acquired.
     */
    Date getTimeAcquired();

    /**
     * Gets the duration of the lock, in milliseconds. This is the
     * total duration, to be counted from the time this lock was
     * acquired, NOT the remaining duration. This can be -1, meaning
     * the lock does never expire.
     */
    long getDuration();

    /**
     * Get the type of lock. See {@link Document#lock(long, LockType)} for
     * more info on the type of locks.
     */
    LockType getType();

    /**
     * Get an XML document describing this lock.
     */
    LockInfoDocument getXml();
}
