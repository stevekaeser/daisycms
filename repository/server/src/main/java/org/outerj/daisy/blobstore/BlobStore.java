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
package org.outerj.daisy.blobstore;

import java.io.InputStream;
import java.util.concurrent.locks.Lock;


/**
 * Stores arbitrairy blob ("binary large object") data.
 *
 * <p>The store is a write-once, read-many type of store. An existing
 * blob cannot be updated, rather a new one needs to be written.
 *
 * <p>The user of the BlobStore is himself responsible not to retrieve or delete
 * a blob before it is completely written.
 */
public interface BlobStore {
    /**
     * Returns an auto-generated key by which the blob can later be retrieved.
     */
    String store(byte[] data) throws BlobIOException;

    /**
     * Returns an auto-generated key by which the blob can later be retrieved.
     * The InputStream will be closed for you.
     */
    String store(InputStream is) throws BlobIOException;

    /**
     * The caller is responsible himself that a file is not being read before it is completely written.
     * The caller is also responsible for making sure the stream gets closed, otherwise resource
     * might leak (thus: always in a try-catch block!).
     */
    InputStream retrieve(String name) throws BlobIOException, NonExistingBlobException;

    void delete(String name) throws NonExistingBlobException;

    /**
     * Suspends all write operations to the blob store, after calling this method
     * only read operation will be allowed. This method only returns after all
     * active write operations have been finished.
     *
     * @param msecs max time to wait for active write operations to finish
     */
    boolean suspendWrites(long msecs) throws InterruptedException;

    /**
     * Resumes write operations (after being suspended with {@link #suspendWrites(long)}.
     */
    void resumeWrites();

    /**
     * Returns a lock which can be acquired to avoid that the BlobStore can
     * go into suspension while you have this lock.
     */
    Lock getAvoidSuspendLock();
}
