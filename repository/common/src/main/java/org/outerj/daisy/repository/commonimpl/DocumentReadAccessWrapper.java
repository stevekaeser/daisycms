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

import java.util.Date;
import java.util.Map;

import org.outerj.daisy.repository.AvailableVariants;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentCollections;
import org.outerj.daisy.repository.DocumentTypeInconsistencyException;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.LiveStrategy;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Versions;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerx.daisy.x10.DocumentDocument;

/**
 * A wrapper put around a document in case access to it needs to be
 * limited to what is allowed by {@link AccessDetails}.
 */
public class DocumentReadAccessWrapper extends VersionedDataAccessWrapper implements Document, DocumentWrapper {
    private final DocumentImpl delegate;
    private final DocumentStrategy documentStrategy;
    private final long liveVersionId;
    private static final String UNMODIFIABLE_MESSAGE = "You are not allowed to update this document.";
    private static final String NONLIVE_ACCESS = "You are not allowed to access non-live information of this document.";
    public static final String ERROR_ACCESSING_REPOSITORY_SCHEMA = "Error accessing repository schema information.";

    public DocumentReadAccessWrapper(DocumentImpl delegate, AccessDetails accessDetails, CommonRepository repository, AuthenticatedUser currentUser, DocumentStrategy documentStrategy) {
        super(delegate, accessDetails, repository, currentUser);
        this.delegate = delegate;
        this.documentStrategy = documentStrategy;
        this.liveVersionId = delegate.getLiveVersionId();
        if (accessDetails.liveOnly() && liveVersionId == -1)
            throw new RuntimeException("Document has no live version.");
    }

    protected void checkLiveAccess() {
        if (!accessDetails.isGranted(AclDetailPermission.NON_LIVE))
            throw new RuntimeException(NONLIVE_ACCESS);
    }
    
    public DocumentImpl getWrappedDocument(DocumentStrategy strategy) {
         // protection to avoid everyone can call this and bypass the access wrapper
        if (strategy == this.documentStrategy)
            return delegate;
        return null;
    }

    public String getId() {
        return delegate.getId();
    }

    public long getSeqId() {
        return delegate.getSeqId();
    }

    public String getNamespace() {
        return delegate.getNamespace();
    }

    public long getBranchId() {
        return delegate.getBranchId();
    }

    public void setRequestedId(String documentId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public String getRequestedId() {
        return delegate.getRequestedId();
    }

    public boolean isNew() {
        return delegate.isNew();
    }

    public long getLanguageId() {
        return delegate.getLanguageId();
    }

    public VariantKey getVariantKey() {
        return delegate.getVariantKey();
    }

    public boolean isVariantNew() {
        return delegate.isVariantNew();
    }

    public AvailableVariants getAvailableVariants() throws RepositoryException {
        return delegate.getAvailableVariants();
    }

    public long getDocumentTypeId() {
        return delegate.getDocumentTypeId();
    }

    public void changeDocumentType(long documentTypeId) throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void changeDocumentType(String documentTypeName) throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public String getName() {
        throw new RuntimeException(NONLIVE_ACCESS);
    }

    public String getDocumentName() {
        throw new RuntimeException(NONLIVE_ACCESS);
    }

    public String getSummary() {
        throw new RuntimeException(NONLIVE_ACCESS);
    }

    public void setName(String name) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public long getOwner() {
        return delegate.getOwner();
    }

    public void setOwner(long userId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public boolean isPrivate() {
        return delegate.isPrivate();
    }

    public void setPrivate(boolean _private) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public Date getCreated() {
        return delegate.getCreated();
    }

    public boolean isRetired() {
        return delegate.isRetired();
    }

    public void setRetired(boolean retired) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public Date getLastModified() {
        return delegate.getLastModified();
    }

    public long getLastModifier() {
        return delegate.getLastModifier();
    }

    public Date getVariantLastModified() {
        return delegate.getVariantLastModified();
    }

    public long getVariantLastModifier() {
        return delegate.getVariantLastModifier();
    }

    public Versions getVersions() throws RepositoryException {
        if (accessDetails.liveOnly())
            throw new RuntimeException(NONLIVE_ACCESS);

        Version[] versions = delegate.getVersions().getArray();
        Version[] newVersions = new Version[versions.length];
        for (int i = 0; i < versions.length; i++) {
            if (!delegate.getTimeline().hasLiveHistoryEntry(versions[i].getId()) && !accessDetails.isGranted(AclDetailPermission.NON_LIVE))
                continue;

            newVersions[i] = versions[i];
        }

        return new VersionsImpl(newVersions);
    }

    public Version getVersion(long id) throws RepositoryException {
        checkVersionAccess(id);
        return new VersionAccessWrapper((VersionImpl)delegate.getVersion(id), accessDetails, repository, currentUser);
    }

    private void checkVersionAccess(long id) {
        if (id == liveVersionId) {
            // ok
        } else if (delegate.getTimeline().hasLiveHistoryEntry(id)) {
            if (!accessDetails.liveHistoryAccess()) {
                throw new RuntimeException(NONLIVE_ACCESS);
            }
        } else {
            if (!accessDetails.isGranted(AclDetailPermission.NON_LIVE))
                throw new RuntimeException(NONLIVE_ACCESS);
        }
    }

    public long getLiveVersionId() {
        return liveVersionId;
    }

    public boolean canReadLiveOnly() {
        return accessDetails.liveOnly();
    }
    
    public boolean canReadAllVersions() {
        return accessDetails.isGranted(AclDetailPermission.NON_LIVE);
    }

    public long getLastVersionId() {
        return delegate.getLastVersionId();
    }

    public Version getLastVersion() throws RepositoryException {
        return getVersion(delegate.getLastVersionId());
    }

    public Version getLiveVersion() throws RepositoryException {
        return getVersion(delegate.getLiveVersionId());
    }

    public void setField(String name, Object value) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deleteField(String name) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deleteField(long fieldTypeId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void save() throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void save(boolean validate) throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void validate() throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setNewVersionState(VersionState versionState) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public VersionState getNewVersionState() {
        return delegate.getNewVersionState();
    }

    public boolean lock(long duration, LockType lockType) throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public boolean releaseLock() throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public LockInfo getLockInfo(boolean fresh) throws RepositoryException {
        return delegate.getLockInfo(fresh);
    }

    public void setCustomField(String name, String value) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deleteCustomField(String name) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public String getCustomField(String name) {
        return delegate.getCustomField(name);
    }

    public boolean hasCustomField(String name) {
        return delegate.hasCustomField(name);
    }

    public void clearCustomFields() {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public Map<String, String> getCustomFields() {
        return delegate.getCustomFields();
    }

    public void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPartFileName(String partTypeName, String fileName) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPartFileName(long partTypeId, String fileName) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPartMimeType(String partTypeName, String mimeType) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setPartMimeType(long partTypeId, String mimeType) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deletePart(long partTypeId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deletePart(String name) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void addLink(String title, String target) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deleteLink(int index) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void clearLinks() {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void addToCollection(DocumentCollection collection) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void removeFromCollection(DocumentCollection collection) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public DocumentCollections getCollections() {
        return delegate.getCollections();
    }

    public boolean inCollection(DocumentCollection collection) {
        return delegate.inCollection(collection);
    }

    public boolean inCollection(long collectionId) {
        return delegate.inCollection(collectionId);
    }

    public DocumentDocument getXml() throws RepositoryException {
        if (accessDetails.liveOnly())
            throw new RuntimeException(NONLIVE_ACCESS);

        return delegate.getXml(accessDetails);
    }

    public DocumentDocument getXmlWithoutVariant() throws RepositoryException {
        // Not influenced by AccessDetails
        return delegate.getXmlWithoutVariant();
    }

    public DocumentDocument getXmlWithoutVersionedData() throws RepositoryException {
        return delegate.getXmlWithoutVersionedData(accessDetails);
    }

    public DocumentDocument getXml(long versionId) throws RepositoryException {
        checkVersionAccess(versionId);        
        return delegate.getXml(versionId, accessDetails);
    }

    public void clearCollections() {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public long getVariantCreatedFromBranchId() {
        return delegate.getVariantCreatedFromBranchId();
    }

    public long getVariantCreatedFromLanguageId() {
        return delegate.getVariantCreatedFromLanguageId();
    }

    public long getVariantCreatedFromVersionId() {
        return delegate.getVariantCreatedFromVersionId();
    }

    public void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public long getUpdateCount() {
        return delegate.getUpdateCount();
    }

    public long getVariantUpdateCount() {
        return delegate.getVariantUpdateCount();
    }

    public long getReferenceLanguageId() {
        return delegate.getReferenceLanguageId();
    }

    public void setReferenceLanguageId(long referenceanguageId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setReferenceLanguage(String referenceLanguageName) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setNewSyncedWith(long languageId, long versionId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setNewSyncedWith(VersionKey syncedWith) throws RepositoryException {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setNewSyncedWith(String languageName, long versionId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public VersionKey getNewSyncedWith() {
        return delegate.getNewSyncedWith();
    }

    public void setNewChangeType(ChangeType changeType) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public ChangeType getNewChangeType() {
        return delegate.getNewChangeType();
    }

    public void setNewChangeComment(String changeComment) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public String getNewChangeComment() {
        return delegate.getNewChangeComment();
    }


    public long getLastMajorChangeVersionId() {
        return delegate.getLastMajorChangeVersionId();
    }

    public long getLiveMajorChangeVersionId() {
        return delegate.getLiveMajorChangeVersionId();
    }

    public VariantKey contextualiseVariantKey(VariantKey key) {
        return delegate.contextualiseVariantKey(key);
    }

    public LiveHistoryEntry addLiveHistoryEntry(Date beginDate, Date endDate, long versionId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void deleteLiveHistoryEntry(LiveHistoryEntry liveHistoryEntry) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public Timeline getTimeline() {
        return delegate.getTimeline();
    }

    public LiveStrategy getNewLiveStrategy() {
        return delegate.getNewLiveStrategy();
    }

    public long getRequestedLiveVersionId() {
        return delegate.getRequestedLiveVersionId();
    }

    public void setNewLiveStrategy(LiveStrategy liveStrategy) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public void setRequestedLiveVersionId(long liveVersionId) {
        throw new RuntimeException(UNMODIFIABLE_MESSAGE);
    }

    public Version getVersion(VersionMode versionMode)
            throws RepositoryException {
        if (versionMode.equals(VersionMode.LAST)) {
            return getLastVersion();
        } else if (versionMode.equals(VersionMode.LIVE)) {
            return getLiveVersion();
        } // live history access is tested in getVersion(id)
        
        return getVersion(delegate.getVersionId(versionMode));
    }

    public long getVersionId(VersionMode versionMode)
            throws RepositoryException {
        return delegate.getVersionId(versionMode);
    }
}
