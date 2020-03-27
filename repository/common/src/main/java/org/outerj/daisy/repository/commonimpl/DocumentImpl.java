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
import java.util.GregorianCalendar;
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
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Versions;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.DocumentDocument;

/**
 * Implementation of the Document interface.
 *
 * <p>This document implementation depends on a {@link DocumentStrategy} which allows
 * the persistence logic for the document to be pluggable.
 *
 * <p>Please note that all methods in this class that are not present in the Document interface,
 * including public methods, are considered to be <b>*for internal use only*</b> and hence should
 * never be called by "end users".
 */
public class DocumentImpl implements Document, DocumentWrapper {
    private DocumentStrategy documentStrategy;
    private CommonRepository repository;
    private AuthenticatedUser currentUser;
    private DocId docId;
    /** For new documents (not yet saved), this can contain the document ID under which to save the document. */
    private DocId requestedDocId;
    private Date lastModified;
    private long lastModifier = -1;
    private Date created;
    private long owner;
    private boolean _private = false;
    private long referenceLanguageId = -1;
    private boolean readOnly = false;
    /** Tracks if any properties of the document have been changed (not of the document variant) */
    private boolean changes = false;
    private long updateCount = 0;
    private DocumentVariantImpl variant;
    private IntimateAccess intimateAccess = new IntimateAccess();

    public static final String ERROR_ACCESSING_REPOSITORY_SCHEMA = "Error accessing repository schema information.";
    private static final String READ_ONLY_MESSAGE = "This document object is read-only.";

    public DocumentImpl(DocumentStrategy documentStrategy, CommonRepository repository, AuthenticatedUser currentUser, long documentTypeId, long branchId, long languageId) {
        this.documentStrategy = documentStrategy;
        this.repository = repository;
        this.currentUser = currentUser;
        this.variant = new DocumentVariantImpl(this, documentStrategy, repository, currentUser, documentTypeId, branchId, languageId);
        this.owner = currentUser.getId(); // initialiase in case of a new document, otherwise will be overwritten later
    }

    public IntimateAccess getIntimateAccess(DocumentStrategy documentStrategy) {
        if (this.documentStrategy == documentStrategy)
            return intimateAccess;
        else
            return null;
    }

    public DocumentImpl getWrappedDocument(DocumentStrategy strategy) {
        return this;
    }

    public boolean canReadLiveOnly() {
        return false;
    }

    public boolean canReadAllVersions() {
        return true;
    }

    public String getId() {
        if (docId != null)
            return docId.toString();
        else
            return null;
    }

    public long getSeqId() {
        if (docId != null)
            return docId.getSeqId();
        else
            return -1;
    }

    public String getNamespace() {
        if (docId != null)
            return docId.getNamespace();
        else
            return null;
    }

    public boolean isNew() {
        return docId == null;
    }

    public void setRequestedId(String documentId) {
        if (!isNew())
            throw new RepositoryRuntimeException("A document ID can only be requested for non-new documents.");

        if (documentId == null) {
            this.requestedDocId = null;
            return;
        }

        DocId requestedDocId = DocId.parseDocId(documentId, repository);

        try {
            Namespace namespace = repository.getNamespaceManager().getNamespace(requestedDocId.getNamespace());
            if (namespace.isManaged()) {
                    throw new RepositoryRuntimeException("A document ID can only be specified for foreign namespaces, which " + requestedDocId.getNamespace()
                            + " is not.");            
            }
        } catch (NamespaceNotFoundException e) {
            throw new RepositoryRuntimeException("Could not access the namespace " + requestedDocId.getNamespace(), e);  
        }

        this.requestedDocId = requestedDocId;
    }

    public String getRequestedId() {
        if (requestedDocId == null)
            return null;
        else
            return requestedDocId.toString();
    }

    public long getBranchId() {
        return variant.getBranchId();
    }

    public long getLanguageId() {
        return variant.getLanguageId();
    }

    public VariantKey getVariantKey() {
        if (this.docId == null)
            return null;
        return new VariantKey(getId(), getBranchId(), getLanguageId());
    }

    public boolean isVariantNew() {
        return variant.isNew();
    }

    public AvailableVariants getAvailableVariants() throws RepositoryException {
        if (this.docId != null) {
            return repository.getAvailableVariants(docId, currentUser);
        }
        return new AvailableVariantsImpl(new AvailableVariantImpl[0]);
    }

    public long getDocumentTypeId() {
        return variant.getDocumentTypeId();
    }

    public void changeDocumentType(long documentTypeId) throws RepositoryException {
        variant.changeDocumentType(documentTypeId);
    }

    public void changeDocumentType(String documentTypeName) throws RepositoryException {
        variant.changeDocumentType(documentTypeName);
    }

    public long getOwner() {
        return owner;
    }

    public void setOwner(long userId) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (userId == owner)
            return;

        if (!currentUser.isInAdministratorRole() && currentUser.getId() != owner)
            throw new RepositoryRuntimeException("The owner of a document can only be changed by the current owner or users acting in the Administrator role.");

        this.owner = userId;
        this.changes = true;
    }

    public boolean isPrivate() {
        return _private;
    }

    public void setPrivate(boolean _private) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this._private = _private;
        this.changes = true;
    }

    public Field getField(String name) throws FieldNotFoundException {
        return variant.getField(name);
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        return variant.getField(fieldTypeId);
    }

    public boolean hasField(long fieldTypeId) {
        return variant.hasField(fieldTypeId);
    }

    public boolean hasField(String fieldTypeName) {
        return variant.hasField(fieldTypeName);
    }

    public Fields getFields() {
        return variant.getFields();
    }

    public Fields getFieldsInOrder() {
        return variant.getFieldsInOrder();
    }

    public void setField(String name, Object value) throws DocumentTypeInconsistencyException {
        variant.setField(name, value);
    }

    public void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException {
        variant.setField(fieldTypeId, value);
    }

    public void deleteField(String name) {
        variant.deleteField(name);
    }

    public void deleteField(long fieldTypeId) {
        variant.deleteField(fieldTypeId);
    }

    public LockInfo getLockInfo(boolean fresh) throws RepositoryException {
        return variant.getLockInfo(fresh);
    }

    // This method is by purpose not in the Document interface. It is used to clear lock
    // info in cached document objects. If it would be in the interface, then the method
    // getLockInfo should cover the case where lockInfo is null and id is -1.
    public void clearLockInfo() {
        variant.clearLockInfo();
    }

    public boolean lock(long duration, LockType lockType) throws RepositoryException {
        return variant.lock(duration, lockType);
    }

    public boolean releaseLock() throws RepositoryException {
        return variant.releaseLock();
    }

    public DocumentDocument getXml() throws RepositoryException {
        return getXml(null);
    }

    public DocumentDocument getXml(AccessDetails accessDetails) throws RepositoryException {
        DocumentDocument documentDocument = getXmlWithoutVariant();
        DocumentDocument.Document documentXml = documentDocument.getDocument();
        variant.addXml(documentXml, accessDetails);
        return documentDocument;
    }

    public DocumentDocument getXml(long versionId) throws RepositoryException {
        return getXml(versionId, null);
    }

    public DocumentDocument getXml(long versionId, AccessDetails accessDetails) throws RepositoryException {
        DocumentDocument documentDocument = getXmlWithoutVariant();
        DocumentDocument.Document documentXml = documentDocument.getDocument();
        variant.addXml(documentXml, versionId, accessDetails);
        return documentDocument;
    }

    public DocumentDocument getXmlWithoutVersionedData(AccessDetails accessDetails) throws RepositoryException {
        DocumentDocument documentDocument = getXmlWithoutVariant();
        DocumentDocument.Document documentXml = documentDocument.getDocument();
        variant.addNonVersionedDataToXml(documentXml, accessDetails);
        return documentDocument;
    }

    public DocumentDocument getXmlWithoutVersionedData() throws RepositoryException {
        return getXmlWithoutVersionedData(null);
    }

    public DocumentDocument getXmlWithoutVariant() {
        DocumentDocument documentDocument = DocumentDocument.Factory.newInstance();
        DocumentDocument.Document documentXml = documentDocument.addNewDocument();

        if (docId != null) {
            documentXml.setId(docId.toString());
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTime(this.lastModified);
            documentXml.setLastModified(lastModified);
            documentXml.setLastModifier(lastModifier);
            GregorianCalendar created = new GregorianCalendar();
            created.setTime(this.created);
            documentXml.setCreated(created);
        } else if (requestedDocId != null) {
            documentXml.setRequestedId(requestedDocId.toString());
        }

        documentXml.setOwner(owner);
        documentXml.setPrivate(_private);
        documentXml.setUpdateCount(updateCount);
        documentXml.setReferenceLanguageId(getReferenceLanguageId());

        return documentDocument;
    }

    public void setName(String name) {
        variant.setName(name);
    }

    public String getName() {
        return variant.getName();
    }

    public String getDocumentName() {
        return getName();
    }

    public void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        variant.setPart(partTypeName, mimeType, data);
    }

    public void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        variant.setPart(partTypeId, mimeType, data);
    }

    public void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        variant.setPart(partTypeName, mimeType, partDataSource);
    }

    public void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        variant.setPart(partTypeId, mimeType, partDataSource);
    }

    public void setPartFileName(String partTypeName, String fileName) {
        variant.setPartFileName(partTypeName, fileName);
    }

    public void setPartFileName(long partTypeId, String fileName) {
        variant.setPartFileName(partTypeId, fileName);
    }

    public void setPartMimeType(String partTypeName, String mimeType) {
        variant.setPartMimeType(partTypeName, mimeType);
    }

    public void setPartMimeType(long partTypeId, String mimeType) {
        variant.setPartMimeType(partTypeId, mimeType);
    }

    public Parts getParts() {
        return variant.getParts();
    }

    public Parts getPartsInOrder() {
        return variant.getPartsInOrder();
    }

    public void deletePart(long partTypeId) {
        variant.deletePart(partTypeId);
    }

    public void deletePart(String name) {
        variant.deletePart(name);
    }

    public Part getPart(long partTypeId) throws PartNotFoundException {
        return variant.getPart(partTypeId);
    }

    public Part getPart(String name) throws PartNotFoundException {
        return variant.getPart(name);
    }

    public boolean hasPart(long partTypeId) {
        return variant.hasPart(partTypeId);
    }

    public boolean hasPart(String name) {
        return variant.hasPart(name);
    }

    public void setCustomField(String name, String value) {
        variant.setCustomField(name, value);
    }

    public void deleteCustomField(String name) {
        variant.deleteCustomField(name);
    }

    public void clearCustomFields() {
        variant.clearCustomFields();
    }
    
    public void clearCollections() {
        variant.clearCollections();
    }

    public Map<String, String> getCustomFields() {
        return variant.getCustomFields();
    }

    public String getCustomField(String name) {
        return variant.getCustomField(name);
    }

    public boolean hasCustomField(String name) {
        return variant.hasCustomField(name);
    }

    public Links getLinks() {
        return variant.getLinks();
    }

    public void addLink(String title, String target) {
        variant.addLink(title, target);
    }

    public void deleteLink(int index) {
        variant.deleteLink(index);
    }

    public void clearLinks() {
        variant.clearLinks();
    }

    public void save() throws RepositoryException {
        save(true);
    }

    public void save(boolean validate) throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        // first check if the document needs saving at all
        if (!isNew() && !needsSaving() && !variant.isNew() && !variant.needsSaving())
            return;

        if (validate)
            validate();

        variant.setValidateOnSave(validate);
        documentStrategy.store(this);
    }

    public void validate() throws DocumentTypeInconsistencyException {
        variant.validate();
    }

    public void setNewVersionState(VersionState versionState) {
        variant.setNewVersionState(versionState);
    }

    public VersionState getNewVersionState() {
        return variant.getNewVersionState();
    }

    public Version getVersion(long versionId) throws RepositoryException {
        return variant.getVersion(versionId);
    }

    public Version getLastVersion() throws RepositoryException {
        return variant.getLastVersion();
    }

    public Version getLiveVersion() throws RepositoryException {
        return variant.getLiveVersion();
    }

    public long getLiveVersionId() {
        return variant.getLiveVersionId();
    }

    public Versions getVersions() throws RepositoryException {
        return variant.getVersions();
    }

    public long getLastVersionId() {
        return variant.getLastVersionId();
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        return lastModified;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public Date getVariantLastModified() {
        return variant.getLastModified();
    }

    public long getVariantLastModifier() {
        return variant.getLastModifier();
    }

    public Date getCreated() {
        return created;
    }

    public boolean isRetired() {
        return variant.isRetired();
    }

    public void setRetired(boolean retired) {
        variant.setRetired(retired);
    }

    public DocumentCollections getCollections() {
        return variant.getCollections();
    }

    public boolean inCollection(DocumentCollection collection) {
        return variant.inCollection(collection);
    }

    public boolean inCollection(long collectionId) {
        return variant.inCollection(collectionId);
    }

    public void addToCollection(DocumentCollection collection) {
        variant.addToCollection(collection);
    }

    public void removeFromCollection(DocumentCollection collection) {
        variant.removeFromCollection(collection);
    }

    public String getSummary() {
        return variant.getSummary();
    }

    public long getVariantCreatedFromBranchId() {
        return variant.getCreatedFromBranchId();
    }

    public long getVariantCreatedFromLanguageId() {
        return variant.getCreatedFromLanguageId();
    }

    public long getVariantCreatedFromVersionId() {
        return variant.getCreatedFromVersionId();
    }

    public void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled) {
        variant.setDocumentTypeChecksEnabled(documentTypeChecksEnabled);
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public long getVariantUpdateCount() {
        return variant.getUpdateCount();
    }
    
    public long getReferenceLanguageId() {
        return referenceLanguageId;
    }
    
    public void setReferenceLanguage(String referenceLanguageName) throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        long referenceLanguageId = repository.getVariantManager().getLanguage(referenceLanguageName, false, currentUser).getId();
        if (this.referenceLanguageId != referenceLanguageId) {
            this.changes = true;
        }
        this.referenceLanguageId = referenceLanguageId;
    }

    public void setReferenceLanguageId(long referenceLanguageId) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (this.referenceLanguageId != referenceLanguageId) {
            this.changes = true;
        }
        this.referenceLanguageId = referenceLanguageId;
    }

    public long getLastMajorChangeVersionId() {
        return variant.getLastMajorChangeVersionId();
    }

    public long getLiveMajorChangeVersionId() {
        return variant.getLiveMajorChangeVersionId();
    }

    public VersionKey getNewSyncedWith() {
        return variant.getNewSyncedWith();
    }
    
    public void setNewSyncedWith(long languageId, long versionId) throws RepositoryException {
        variant.setNewSyncedWithVersion(languageId, versionId);
    }
    
    public void setNewSyncedWith(String languageName, long versionId) throws RepositoryException {
        if ((languageName == null && versionId != -1) || (languageName != null && versionId == -1))
            throw new IllegalArgumentException("If languageName is null or versionId is -1, both should be set this way.");

        if (languageName == null) {
            variant.setNewSyncedWithVersion(-1, -1);
        } else {
            Language lang = repository.getVariantManager().getLanguageByName(languageName, false, currentUser);
            variant.setNewSyncedWithVersion(lang.getId(), versionId);
        }
    }

    public void setNewSyncedWith(VersionKey syncedWith) throws RepositoryException {
        if (syncedWith == null)
            variant.setNewSyncedWithVersion(-1, -1);
        else
            variant.setNewSyncedWithVersion(syncedWith.getLanguageId(), syncedWith.getVersionId());
    }

    public void setNewChangeType(ChangeType changeType) {
        variant.setNewChangeType(changeType);
    }

    public ChangeType getNewChangeType() {
        return variant.getNewChangeType();
    }

    public void setNewChangeComment(String changeComment) {
        variant.setNewChangeComment(changeComment);
    }

    public String getNewChangeComment() {
        return variant.getNewChangeComment();
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public boolean needsSaving() {
        return changes;
    }
    
    public VariantKey contextualiseVariantKey(VariantKey key) {
        if (key == null) {
            throw new NullPointerException("key should not be null");
        }
        long branchId = key.getBranchId()==-1?getBranchId():key.getBranchId();
        long languageId = key.getLanguageId()==-1?getLanguageId():key.getLanguageId();
        
        return new VariantKey(key.getDocumentId(), branchId, languageId);
    }

    public String toString() {
        return getVariantKey().toString();
    }
    
    public class IntimateAccess {

        private IntimateAccess() {
        }

        public DocId getDocId() {
            return DocumentImpl.this.docId;
        }

        public DocId getRequestedDocId() {
            return DocumentImpl.this.requestedDocId;
        }

        /**
         * Updates the state of this Document object after saving it, also resets
         * all 'dirty' flags.
         *
         * @param created can be null if not modified (only required after first save)
         */
        public void saved(DocId docId, Date lastModified, Date created, long updateCount) {
            DocumentImpl.this.docId = docId;
            DocumentImpl.this.requestedDocId = null;
            DocumentImpl.this.lastModified = lastModified;
            DocumentImpl.this.lastModifier = currentUser.getId();
            DocumentImpl.this.updateCount = updateCount;
            DocumentImpl.this.created = created;
            DocumentImpl.this.changes = false;
        }


        public void setCreated(Date created) {
            DocumentImpl.this.created = created;
        }

        public AuthenticatedUser getCurrentUser() {
            return currentUser;
        }

        public DocumentImpl getDocument() {
            return DocumentImpl.this;
        }

        public DocumentVariantImpl getVariant() {
            return variant;
        }

        /**
         * Intialises the document object as when loading an existing document.
         */
        public void load(DocId docId, Date lastModified, long lastModifier, Date created, long owner, boolean _private, long updateCount, long referenceLanguageId) {
            DocumentImpl.this.docId = docId;
            DocumentImpl.this.lastModified = lastModified;
            DocumentImpl.this.lastModifier = lastModifier;
            DocumentImpl.this.created = created;
            DocumentImpl.this.owner = owner;
            DocumentImpl.this._private = _private;
            DocumentImpl.this.updateCount = updateCount;
            DocumentImpl.this.referenceLanguageId = referenceLanguageId;
        }
    }

    public LiveStrategy getNewLiveStrategy() {
        return variant.getNewLiveStrategy();
    }

    public void setNewLiveStrategy(LiveStrategy liveStrategy) {
        variant.setNewLiveStrategy(liveStrategy);
    }

    public long getRequestedLiveVersionId() {
        return variant.getRequestedLiveVersionId();
    }

    public void setRequestedLiveVersionId(long liveVersionId) {
        variant.setRequestedLiveVersionId(liveVersionId);
    }

    public Timeline getTimeline() {
        return variant.getTimeline();
    }

    public Version getVersion(VersionMode versionMode)
            throws RepositoryException {
        return variant.getVersion(versionMode);
    }

    public long getVersionId(VersionMode versionMode)
            throws RepositoryException {
        return variant.getVersionId(versionMode);
    }
}
