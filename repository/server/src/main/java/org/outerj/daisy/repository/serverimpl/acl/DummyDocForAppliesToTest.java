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
package org.outerj.daisy.repository.serverimpl.acl;

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
import org.outerj.daisy.repository.PartNotFoundException;
import org.outerj.daisy.repository.Parts;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Versions;
import org.outerx.daisy.x10.CollectionDocument;
import org.outerx.daisy.x10.CollectionsDocument;
import org.outerx.daisy.x10.DocumentDocument;

public class DummyDocForAppliesToTest implements Document {
    private final static String FAIL_MSG = "Tried to access document information which is not available while testing 'applies to'. This is a bug in Daisy, please report it.";
    private final long documentTypeId;
    private final DocumentCollections documentCollections;
    private final long branchId;
    private final long languageId;

    public DummyDocForAppliesToTest(long documentTypeId, long collectionId, long branchId, long languageId) {
        this.documentTypeId = documentTypeId;
        DocumentCollection[] collections = new DocumentCollection[1];
        collections[0] = new DummyDocumentCollection(collectionId);
        documentCollections = new DummyDocumentCollections(collections);
        this.branchId = branchId;
        this.languageId = languageId;
    }

    static final class DummyDocumentCollection implements DocumentCollection {
        private final long id;

        public DummyDocumentCollection(long id) {
            this.id = id;
        }

        public long getId() {
            return id;
        }

        public String getName() {
            throw new RuntimeException(FAIL_MSG);
        }

        public void setName(String name) {
            throw new RuntimeException(FAIL_MSG);
        }

        public void save() {
            throw new RuntimeException(FAIL_MSG);
        }

        public CollectionDocument getXml() {
            throw new RuntimeException(FAIL_MSG);
        }

        public Date getLastModified() {
            throw new RuntimeException(FAIL_MSG);
        }

        public long getLastModifier() {
            throw new RuntimeException(FAIL_MSG);
        }

        public long getUpdateCount() {
            throw new RuntimeException(FAIL_MSG);
        }
    }

    static final class DummyDocumentCollections implements DocumentCollections {
        private final DocumentCollection[] collections;

        public DummyDocumentCollections(DocumentCollection[] collections) {
            this.collections = collections;
        }

        public DocumentCollection[] getArray() {
            return collections;
        }

        public CollectionsDocument getXml() {
            throw new RuntimeException(FAIL_MSG);
        }
    }

    public String getId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean isNew() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getSeqId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getNamespace() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public void setRequestedId(String documentId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getRequestedId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public VariantKey getVariantKey() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean isVariantNew() {
        throw new RuntimeException(FAIL_MSG);
    }

    public AvailableVariants getAvailableVariants() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getDocumentTypeId() {
        return documentTypeId;
    }

    public void changeDocumentType(long documentTypeId) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void changeDocumentType(String documentTypeName) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getName() {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getDocumentName() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setName(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getOwner() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setOwner(long userId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean isPrivate() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPrivate(boolean _private) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Date getCreated() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean isRetired() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setRetired(boolean retired) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Date getLastModified() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getLastModifier() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Date getVariantLastModified() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVariantLastModifier() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Versions getVersions() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public Version getVersion(long id) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getLastVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Version getLastVersion() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public Version getLiveVersion() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getLiveVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean canReadLiveOnly() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean canReadAllVersions() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setField(String name, Object value) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deleteField(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deleteField(long fieldTypeId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Field getField(String fieldTypeName) throws FieldNotFoundException {
        throw new RuntimeException(FAIL_MSG);
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean hasField(long fieldTypeId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean hasField(String fieldTypeName) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Fields getFields() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Fields getFieldsInOrder() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void save() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void save(boolean validate) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void validate() throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewVersionState(VersionState versionState) {
        throw new RuntimeException(FAIL_MSG);
    }

    public VersionState getNewVersionState() {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean lock(long duration, LockType lockType) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean releaseLock() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public LockInfo getLockInfo(boolean fresh) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setCustomField(String name, String value) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deleteCustomField(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getCustomField(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean hasCustomField(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void clearCustomFields() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Map<String, String> getCustomFields() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPartFileName(String partTypeName, String fileName) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPartFileName(long partTypeId, String fileName) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPartMimeType(String partTypeName, String mimeType) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setPartMimeType(long partTypeId, String mimeType) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deletePart(long partTypeId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deletePart(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Part getPart(long partTypeId) throws PartNotFoundException {
        throw new RuntimeException(FAIL_MSG);
    }

    public Part getPart(String name) throws PartNotFoundException {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean hasPart(long partTypeId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean hasPart(String name) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Parts getParts() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Parts getPartsInOrder() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void addLink(String title, String target) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void deleteLink(int index) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void clearLinks() {
        throw new RuntimeException(FAIL_MSG);
    }

    public Links getLinks() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void addToCollection(DocumentCollection collection) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void removeFromCollection(DocumentCollection collection) {
        throw new RuntimeException(FAIL_MSG);
    }

    public DocumentCollections getCollections() {
        return documentCollections;
    }

    public boolean inCollection(DocumentCollection collection) {
        throw new RuntimeException(FAIL_MSG);
    }

    public boolean inCollection(long collectionId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public DocumentDocument getXml() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public DocumentDocument getXmlWithoutVariant() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public DocumentDocument getXmlWithoutVersionedData() throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public DocumentDocument getXml(long versionId) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void clearCollections() {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getSummary() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVariantCreatedFromBranchId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVariantCreatedFromLanguageId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVariantCreatedFromVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled) {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getUpdateCount() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVariantUpdateCount() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getReferenceLanguageId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setReferenceLanguageId(long referenceLanguageId) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setReferenceLanguage(String referenceLanguageName) {
        throw new RuntimeException(FAIL_MSG);
    }

    public String getNewChangeComment() {
        throw new RuntimeException(FAIL_MSG);
    }

    public ChangeType getNewChangeType() {
        throw new RuntimeException(FAIL_MSG);
    }

    public VersionKey getNewSyncedWith() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewChangeComment(String changeComment) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewChangeType(ChangeType changeType) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewSyncedWith(long languageId, long versionId)
            throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewSyncedWith(String languageName, long versionId)
            throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewSyncedWith(VersionKey syncedWith) throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getLastMajorChangeVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getLiveMajorChangeVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public VariantKey contextualiseVariantKey(VariantKey variantKey) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Timeline getTimeline() {
        throw new RuntimeException(FAIL_MSG);
    }

    public LiveStrategy getNewLiveStrategy() {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getRequestedLiveVersionId() {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setNewLiveStrategy(LiveStrategy arg0) {
        throw new RuntimeException(FAIL_MSG);
    }

    public void setRequestedLiveVersionId(long arg0) {
        throw new RuntimeException(FAIL_MSG);
    }

    public Version getVersion(VersionMode versionMode)
            throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }

    public long getVersionId(VersionMode versionMode)
            throws RepositoryException {
        throw new RuntimeException(FAIL_MSG);
    }
    
}
