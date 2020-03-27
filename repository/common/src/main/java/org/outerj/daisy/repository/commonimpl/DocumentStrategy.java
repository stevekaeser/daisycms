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
package org.outerj.daisy.repository.commonimpl;

import java.io.InputStream;

import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionState;

/**
 * Allows to customise some of the behaviour of the {@link CommonRepository}, and especially
 * {@link DocumentImpl}.
 *
 * <p>The typical use of this is to provide two implementations of the repository API's:
 * one for local (in-server) use, and one for remote (= client) use, by only having to
 * implement twice those aspects which differ in each implementation.
 *
 * <p>The most important difference between the client and server API implementations
 * will be how they load and store entities (such as Document objects): the client API
 * implementation will do this by contacting the server, the server implementation will
 * do this by using its persistence mechanisms (such as an RDBMS).
 *
 * <p>Note that this API is not really meant for public consumption, and the correct
 * workings of its implementations (especially the server-side one) are crucial for
 * the correct operation of the repository. It is important that the strategy implementation
 * correctly initialiases, updates and interprets the internal state of the objects it handles.
 *
 * <p>Certain methonds, like {@link #load(DocId, long, long, AuthenticatedUser)}
 * and {@link #store(org.outerj.daisy.repository.commonimpl.DocumentImpl)} need to check
 * if the user has the rights to perform this operation. This is especially true for the serverside
 * implementation, the client side implementation doesn't need to do this as it will contact the
 * server which will automatically do this checks.
 *
 * <p>Certain methods, like {@link #store(org.outerj.daisy.repository.commonimpl.DocumentImpl)} might
 * also need to send out events to eventlisteners.
 */
public interface DocumentStrategy {
    public Document load(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException;

    /**
     * Stores a document. After successful storage, the document object status should be
     * updates, i.e. the lastModified and lastModifier parts should be updated, and some
     * dirty-indication flags should be reset.
     */
    public void store(DocumentImpl document) throws RepositoryException;

    /**
     *
     * @param startVersionId -1 for last version, -2 for live version
     */
    public Document createVariant(DocId docId, long startBranchId, long startLanguageId, long startVersionId, long newBranchId, long newLanguageId, AuthenticatedUser user) throws RepositoryException;

    public AvailableVariantImpl[] getAvailableVariants(DocId docId, AuthenticatedUser user) throws RepositoryException;

    public void deleteDocument(DocId docId, AuthenticatedUser user) throws RepositoryException;

    public void deleteVariant(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException;

    public VersionImpl loadVersion(DocumentVariantImpl documentVariant, long versionId) throws RepositoryException;

    /**
     * Loads the additional information skipped when the version was loaded via
     * {@link #loadShallowVersions(org.outerj.daisy.repository.commonimpl.DocumentVariantImpl)}.
     */
    public void completeVersion(DocumentVariantImpl variant, VersionImpl version) throws RepositoryException;

    /**
     * Loads all Version objects for this document as shallow Version objects (i.e. without
     * their full content, but only the data necessary to show a version overview table).
     */
    public VersionImpl[] loadShallowVersions(DocumentVariantImpl variant) throws RepositoryException;

    public void storeVersion(DocumentImpl document, VersionImpl version, VersionState versionState, VersionKey syncedWith, ChangeType changeType, String changeComment) throws RepositoryException;

    public InputStream getBlob(DocId docId, long branchId, long languageId, long versionId, long partTypeId, AuthenticatedUser user) throws RepositoryException;

    /**
     * This method does not check access rights (unlike {@link #getBlob(DocId, long, long, long, long, AuthenticatedUser)},
     * because this one is only intended for use by Part objects.
     */ 
    public InputStream getBlob(String blobKey) throws RepositoryException;

    /**
     * Tries to create a lock on the document. If there was alread a pessimitic lock on
     * the document, this method will return a LockInfo object containing information about
     * that lock, so it is important to check the info in the LockInfo object to know if
     * the lock was successful.
     */
    public LockInfoImpl lock(DocumentVariantImpl documentVariant, long duration, LockType lockType) throws RepositoryException;

    public LockInfoImpl getLockInfo(DocumentVariantImpl documentVariant) throws RepositoryException;

    public LockInfoImpl releaseLock(DocumentVariantImpl documentVariant) throws RepositoryException;

    public LiveHistoryEntry[] loadLiveHistory(DocumentVariantImpl documentVariant) throws RepositoryException;
    
    public void storeTimeline(DocumentVariantImpl documentVariant, TimelineImpl timeline) throws RepositoryException;

}
