/*
; * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.FieldNotFoundException;
import org.outerj.daisy.repository.Fields;
import org.outerj.daisy.repository.Links;
import org.outerj.daisy.repository.LiveStrategy;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.Parts;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Versions;
import org.outerx.daisy.x10.DocumentDocument;

public class AbstractDocumentWrapper implements Document, DocumentWrapper {
    protected final DocumentImpl delegate;
    private final DocumentStrategy documentStrategy;

    public AbstractDocumentWrapper(DocumentImpl delegate, DocumentStrategy documentStrategy) {
        this.delegate = delegate;
        this.documentStrategy = documentStrategy;
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

    public boolean isNew() {
        return delegate.isNew();
    }

    public long getBranchId() {
        return delegate.getBranchId();
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

    public void setRequestedId(String documentId) {
        delegate.setRequestedId(documentId);
    }

    public String getRequestedId() {
        return delegate.getRequestedId();
    }

    public AvailableVariants getAvailableVariants() throws RepositoryException {
        return delegate.getAvailableVariants();
    }

    public long getDocumentTypeId() {
        return delegate.getDocumentTypeId();
    }

    public void changeDocumentType(long documentTypeId) throws RepositoryException {
        delegate.changeDocumentType(documentTypeId);
    }

    public void changeDocumentType(String documentTypeName) throws RepositoryException {
        delegate.changeDocumentType(documentTypeName);
    }

    public String getName() {
        return delegate.getName();
    }

    public void setName(String name) {
        delegate.setName(name);
    }

    public long getOwner() {
        return delegate.getOwner();
    }

    public void setOwner(long userId) {
        delegate.setOwner(userId);
    }

    public boolean isPrivate() {
        return delegate.isPrivate();
    }

    public void setPrivate(boolean _private) {
        delegate.setPrivate(_private);
    }

    public Date getCreated() {
        return delegate.getCreated();
    }

    public boolean isRetired() {
        return delegate.isRetired();
    }

    public void setRetired(boolean retired) {
        delegate.setRetired(retired);
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
        return delegate.getVersions();
    }

    public Version getVersion(long id) throws RepositoryException {
        return delegate.getVersion(id);
    }

    public long getLastVersionId() {
        return delegate.getLastVersionId();
    }

    public Version getLastVersion() throws RepositoryException {
        return delegate.getLastVersion();
    }

    public Version getLiveVersion() throws RepositoryException {
        return delegate.getLiveVersion();
    }

    public long getLiveVersionId() {
        return delegate.getLiveVersionId();
    }

    public boolean canReadLiveOnly() {
        return delegate.canReadLiveOnly();
    }

    public boolean canReadAllVersions() {
        return delegate.canReadAllVersions();
    }

    public void setField(String name, Object value) throws DocumentTypeInconsistencyException {
        delegate.setField(name, value);
    }

    public void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException {
        delegate.setField(fieldTypeId, value);
    }

    public void deleteField(String name) {
        delegate.deleteField(name);
    }

    public void deleteField(long fieldTypeId) {
        delegate.deleteField(fieldTypeId);
    }

    public void save() throws RepositoryException {
        delegate.save();
    }

    public void save(boolean validate) throws RepositoryException {
        delegate.save(validate);
    }

    public void validate() throws DocumentTypeInconsistencyException {
        delegate.validate();
    }

    public void setNewVersionState(VersionState versionState) {
        delegate.setNewVersionState(versionState);
    }

    public VersionState getNewVersionState() {
        return delegate.getNewVersionState();
    }

    public boolean lock(long duration, LockType lockType) throws RepositoryException {
        return delegate.lock(duration, lockType);
    }

    public boolean releaseLock() throws RepositoryException {
        return delegate.releaseLock();
    }

    public LockInfo getLockInfo(boolean fresh) throws RepositoryException {
        return delegate.getLockInfo(fresh);
    }

    public void setCustomField(String name, String value) {
        delegate.setCustomField(name, value);
    }

    public void deleteCustomField(String name) {
        delegate.deleteCustomField(name);
    }

    public String getCustomField(String name) {
        return delegate.getCustomField(name);
    }

    public boolean hasCustomField(String name) {
        return delegate.hasCustomField(name);
    }

    public void clearCustomFields() {
        delegate.clearCustomFields();
    }

    public Map<String, String> getCustomFields() {
        return delegate.getCustomFields();
    }

    public void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        delegate.setPart(partTypeName, mimeType, data);
    }

    public void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        delegate.setPart(partTypeId, mimeType, data);
    }

    public void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        delegate.setPart(partTypeName, mimeType, partDataSource);
    }

    public void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        delegate.setPart(partTypeId, mimeType, partDataSource);
    }

    public void setPartFileName(String partTypeName, String fileName) {
        delegate.setPartFileName(partTypeName, fileName);
    }

    public void setPartFileName(long partTypeId, String fileName) {
        delegate.setPartFileName(partTypeId, fileName);
    }

    public void setPartMimeType(String partTypeName, String mimeType) {
        delegate.setPartMimeType(partTypeName, mimeType);
    }

    public void setPartMimeType(long partTypeId, String mimeType) {
        delegate.setPartMimeType(partTypeId, mimeType);
    }

    public void deletePart(long partTypeId) {
        delegate.deletePart(partTypeId);
    }

    public void deletePart(String name) {
        delegate.deletePart(name);
    }

    public void addLink(String title, String target) {
        delegate.addLink(title, target);
    }

    public void deleteLink(int index) {
        delegate.deleteLink(index);
    }

    public void clearLinks() {
        delegate.clearLinks();
    }

    public void addToCollection(DocumentCollection collection) {
        delegate.addToCollection(collection);
    }

    public void removeFromCollection(DocumentCollection collection) {
        delegate.removeFromCollection(collection);
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
        return delegate.getXml();
    }

    public DocumentDocument getXmlWithoutVariant() throws RepositoryException {
        return delegate.getXmlWithoutVariant();
    }

    public DocumentDocument getXmlWithoutVersionedData() throws RepositoryException {
        return delegate.getXmlWithoutVersionedData();
    }

    public DocumentDocument getXml(long versionId) throws RepositoryException {
        return delegate.getXml(versionId);
    }

    public void clearCollections() {
        delegate.clearCollections();
    }

    public String getSummary() {
        return delegate.getSummary();
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
        delegate.setDocumentTypeChecksEnabled(documentTypeChecksEnabled);
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

    public void setReferenceLanguageId(long referenceLanguageId) {
        delegate.setReferenceLanguageId(referenceLanguageId);
    }

    public void setReferenceLanguage(String referenceLanguageName) throws RepositoryException {
        delegate.setReferenceLanguage(referenceLanguageName);
    }

    public long getLastMajorChangeVersionId() {
        return delegate.getLastMajorChangeVersionId();
    }

    public long getLiveMajorChangeVersionId() {
        return delegate.getLiveMajorChangeVersionId();
    }

    public VersionKey getNewSyncedWith() {
        return delegate.getNewSyncedWith();
    }

    public void setNewSyncedWith(long languageId, long versionId) throws RepositoryException {
        delegate.setNewSyncedWith(languageId, versionId);
    }

    public void setNewSyncedWith(String languageName, long versionId) throws RepositoryException {
        delegate.setNewSyncedWith(languageName, versionId);
    }

    public void setNewSyncedWith(VersionKey syncedWith) throws RepositoryException {
        delegate.setNewSyncedWith(syncedWith);
    }

    public void setNewChangeType(ChangeType changeType) {
        delegate.setNewChangeType(changeType);
    }

    public ChangeType getNewChangeType() {
        return delegate.getNewChangeType();
    }

    public void setNewChangeComment(String changeComment) {
        delegate.setNewChangeComment(changeComment);
    }

    public String getNewChangeComment() {
        return delegate.getNewChangeComment();
    }

    public String getDocumentName() {
        return delegate.getDocumentName();
    }

    public Parts getParts() {
        return delegate.getParts();
    }

    public Parts getPartsInOrder() {
        return delegate.getPartsInOrder();
    }

    public Part getPart(long typeId) {
        return delegate.getPart(typeId);
    }

    public boolean hasPart(long typeId) {
        return delegate.hasPart(typeId);
    }

    public boolean hasPart(String typeName) {
        return delegate.hasPart(typeName);
    }

    public Part getPart(String typeName) {
        return delegate.getPart(typeName);
    }

    public Fields getFields() {
        return delegate.getFields();
    }

    public Fields getFieldsInOrder() {
        return delegate.getFieldsInOrder();
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        return delegate.getField(fieldTypeId);
    }

    public Field getField(String fieldTypeName) throws FieldNotFoundException {
        return delegate.getField(fieldTypeName);
    }

    public boolean hasField(long fieldTypeId) {
        return delegate.hasField(fieldTypeId);
    }

    public boolean hasField(String fieldTypeName) {
        return delegate.hasField(fieldTypeName);
    }

    public Links getLinks() {
        return delegate.getLinks();
    }

    public VariantKey contextualiseVariantKey(VariantKey key) {
        return delegate.contextualiseVariantKey(key);
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
        delegate.setNewLiveStrategy(liveStrategy);
    }

    public void setRequestedLiveVersionId(long liveVersionId) {
        delegate.setRequestedLiveVersionId(liveVersionId);
    }

    public Version getVersion(VersionMode versionMode)
            throws RepositoryException {
        return delegate.getVersion(versionMode);
    }

    public long getVersionId(VersionMode versionMode)
            throws RepositoryException {
        return delegate.getVersionId(versionMode);
    }
    
}
