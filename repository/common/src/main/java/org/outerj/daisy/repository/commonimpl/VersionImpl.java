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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.util.ObjectUtils;
import org.outerx.daisy.x10.VersionDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class VersionImpl implements Version {
    private long id;
    private Date created;
    private long creator = -1;
    private String documentName;
    private PartImpl[] parts;
    private FieldImpl[] fields;
    private LinkImpl[] links;
    private VersionState state;
    private long totalSizeOfParts;
    private long lastModifier = -1;
    private Date lastModified;
    private VersionKey syncedWith;
    private ChangeType changeType;
    private String changeComment;
    private String summary;
    private DocumentVariantImpl.IntimateAccess ownerVariantInt;
    private IntimateAccess intimateAccess = new IntimateAccess();
    
    private boolean stateChanged = false;
    private boolean syncedWithChanged = false;
    private boolean changeTypeChanged = false;
    private boolean changeCommentChanged = false;

    private static String READ_ONLY_MESSAGE = "This Version object is read-only.";

    public VersionImpl(DocumentVariantImpl.IntimateAccess ownerVariantInt, long id, String documentName, Date created,
            long creator, VersionState versionState, VersionKey syncedWith, ChangeType changeType, String changeComment,
            Date lastModified, long lastModifier, long totalSizeOfParts, String summary) {
        this.ownerVariantInt = ownerVariantInt;
        this.id = id;
        this.documentName = documentName;
        this.created = created;
        this.creator = creator;
        this.state = versionState;
        this.syncedWith = syncedWith;
        this.changeType = changeType;
        this.changeComment = changeComment;
        this.totalSizeOfParts = totalSizeOfParts;
        this.lastModified = lastModified;
        this.lastModifier = lastModifier;
        this.summary = summary;
    }

    public VersionImpl(DocumentVariantImpl.IntimateAccess ownerVariantInt, long id, Date created, long creator,
            String documentName, PartImpl[] parts, FieldImpl[] fields, LinkImpl[] links, VersionState versionState,
            VersionKey syncedWith, ChangeType changeType, String changeComment, Date lastModified,
            long lastModifier, long totalSizeOfParts) {
        this.ownerVariantInt = ownerVariantInt;
        this.id = id;
        this.documentName = documentName;
        this.parts = parts;
        this.fields = fields;
        this.links = links;
        this.created = created;
        this.creator = creator;
        this.state = versionState;
        this.syncedWith = syncedWith;
        this.changeType = changeType;
        this.changeComment = changeComment;
        this.lastModified = lastModified;
        this.lastModifier = lastModifier;
        this.totalSizeOfParts = totalSizeOfParts;
    }

    public VersionImpl.IntimateAccess getIntimateAccess(DocumentStrategy documentStrategy) {
        if (this.ownerVariantInt.getDocumentStrategy() == documentStrategy)
            return intimateAccess;
        else
            return null;
    }

    public long getId() {
        return id;
    }

    public String getDocumentName() {
        return documentName;
    }

    public Date getCreated() {
        return (Date)created.clone();
    }

    public long getCreator() {
        return creator;
    }

    public Parts getParts() {
        lazyLoadCheck();
        return new PartsImpl(parts);
    }

    public Parts getPartsInOrder() {
        lazyLoadCheck();
        return new PartsImpl(ownerVariantInt.orderParts(parts));
    }

    public Part getPart(long typeId) {
        lazyLoadCheck();
        for (PartImpl part : parts) {
            if (part.getTypeId() == typeId)
                return part;
        }
        throw new PartNotFoundException(typeId);
    }

    public Part getPart(String typeName) {
        PartType partType;
        try {
            partType = ownerVariantInt.getRepositorySchema().getPartTypeByName(typeName, false, ownerVariantInt.getCurrentUser());
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return getPart(partType.getId());
    }

    public boolean hasPart(long typeId) {
        lazyLoadCheck();
        for (PartImpl part : parts) {
            if (part.getTypeId() == typeId)
                return true;
        }
        return false;        
    }

    public boolean hasPart(String typeName) {
        lazyLoadCheck();
        long partTypeId;
        try {
            partTypeId = ownerVariantInt.getRepositorySchema().getPartTypeByName(typeName, false, ownerVariantInt.getCurrentUser()).getId();
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return hasPart(partTypeId);
    }

    public Fields getFields() {
        lazyLoadCheck();
        return new FieldsImpl(fields);
    }

    public Fields getFieldsInOrder() {
        lazyLoadCheck();
        return new FieldsImpl(ownerVariantInt.orderFields(fields));
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        lazyLoadCheck();
        for (FieldImpl field : fields) {
            if (field.getTypeId() == fieldTypeId)
                return field;
        }
        throw new FieldNotFoundException(fieldTypeId);
    }

    public Field getField(String fieldTypeName) throws FieldNotFoundException {
        FieldType fieldType;
        try {
            fieldType = ownerVariantInt.getRepositorySchema().getFieldTypeByName(fieldTypeName, false, ownerVariantInt.getCurrentUser());
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return getField(fieldType.getId());
    }

    public boolean hasField(long fieldTypeId) {
        lazyLoadCheck();
        for (FieldImpl field : fields) {
            if (field.getTypeId() == fieldTypeId)
                return true;
        }
        return false;
    }

    public boolean hasField(String fieldTypeName) {
        long fieldTypeId;
        try {
            fieldTypeId = ownerVariantInt.getRepositorySchema().getFieldTypeByName(fieldTypeName, false, ownerVariantInt.getCurrentUser()).getId();
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return hasField(fieldTypeId);
    }

    public Links getLinks() {
        lazyLoadCheck();
        return new LinksImpl(links);
    }

    private synchronized void lazyLoadCheck() {
        if (parts != null)
            return;

        try {
            ownerVariantInt.getDocumentStrategy().completeVersion(ownerVariantInt.getVariant(), this);
        } catch (Exception e) {
            throw new RuntimeException("Error lazy-loading version data for version " + id + " of document " + ownerVariantInt.getDocument().getId() + ".");
        }
    }

    public VersionDocument getShallowXml() {
        VersionDocument versionDocument = VersionDocument.Factory.newInstance();
        VersionDocument.Version versionXml = versionDocument.addNewVersion();
        versionXml.setId(id);
        versionXml.setDocumentName(documentName);
        GregorianCalendar createdCalendar = new GregorianCalendar();
        createdCalendar.setTime(created);
        versionXml.setCreated(createdCalendar);
        versionXml.setCreator(creator);
        versionXml.setState(state.toString());
        if (syncedWith != null) {
            versionXml.setSyncedWithLanguageId(syncedWith.getLanguageId());
            versionXml.setSyncedWithVersionId(syncedWith.getVersionId());
        }
        versionXml.setChangeType(changeType.toString());
        versionXml.setChangeComment(changeComment);
        GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
        lastModifiedCalendar.setTime(lastModified);
        versionXml.setLastModified(lastModifiedCalendar);
        versionXml.setLastModifier(lastModifier);
        versionXml.setTotalSizeOfParts(totalSizeOfParts);
        return versionDocument;
    }

    public VersionDocument getXml() throws RepositoryException {
        return getXml(null);
    }

    public VersionDocument getXml(AccessDetails accessDetails) throws RepositoryException {
        lazyLoadCheck();

        VersionDocument versionDocument = getShallowXml();
        VersionDocument.Version versionXml = versionDocument.getVersion();
        versionXml.setFields(VersionedDataAccessWrapper.filterFields(getFieldsInOrder(), accessDetails).getXml().getFields());
        versionXml.setParts(VersionedDataAccessWrapper.filterParts(getPartsInOrder(), accessDetails).getXml().getParts());
        versionXml.setLinks(getLinks().getXml().getLinks());
        if (changeComment != null) {
            versionXml.setChangeComment(changeComment);
        }
        versionXml.setChangeType(changeType.toString());
        if (summary != null) {
            versionXml.setSummary(summary);
        }

        return versionDocument;
    }

    public void setState(VersionState state) {
        if (ownerVariantInt.getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);
        
        if (state == null)
            throw new NullPointerException("versionState argument cannot be null.");

        if (this.state == state)
            return;
        
        this.state = state;
        stateChanged = true;
    }

    public VersionState getState() {
        return state;
    }
    
    public void setSyncedWith(long languageId, long versionId) throws RepositoryException {
        if (ownerVariantInt.getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);
        
        DocumentImpl.IntimateAccess documentInt = ownerVariantInt.getDocument().getIntimateAccess(ownerVariantInt.getDocumentStrategy());
        if (languageId == documentInt.getDocument().getLanguageId()) {
            throw new IllegalArgumentException("You can not make a document synced with a version in the same language");
        }

        if (languageId == -1 || versionId == -1) {
            if (languageId != -1 || versionId != -1)
                throw new IllegalArgumentException("The languageId and versionId arguments should both be -1 or not be -1 at all.");
            if (syncedWith != null) {
                syncedWith = null;
                syncedWithChanged = true;
            }
            return;
        }

        // Check the language exists
        ownerVariantInt.getVariantManager().getLanguage(languageId, false, ownerVariantInt.getCurrentUser());

        VersionKey newSyncedWith = new VersionKey(documentInt.getDocId().toString(), documentInt.getVariant().getBranchId(), languageId, versionId);
        if (newSyncedWith.equals(syncedWith))
            return;
        
        this.syncedWith = newSyncedWith;
        syncedWithChanged = true;
    }

    public void setSyncedWith(VersionKey syncedWith) throws RepositoryException {
        if (syncedWith == null)
            setSyncedWith(-1, -1);
        else
            setSyncedWith(syncedWith.getLanguageId(), syncedWith.getVersionId());
    }

    public VersionKey getSyncedWith() {
        return syncedWith;       
    }

    public void setChangeComment(String changeComment) throws RepositoryException {
        if (ownerVariantInt.getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (changeComment != null) {
            changeComment = changeComment.trim();
            if (changeComment.length() == 0) {
                changeComment = null;
            }
        }
        
        if (ObjectUtils.safeEquals(changeComment, this.changeComment))
            return;
        
        this.changeComment = changeComment;
        changeCommentChanged = true;
    }

    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeType(ChangeType changeType) throws RepositoryException {
        if (ownerVariantInt.getDocument().isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);
        
        if (changeType == null)
            throw new NullPointerException("changeType argument cannot be null.");

        if (changeType == this.changeType)
            return;
            
        this.changeType = changeType;
        changeTypeChanged = true;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void save() throws RepositoryException {
        if (!needsSaving())
            return;
        
        ownerVariantInt.getDocumentStrategy().storeVersion(ownerVariantInt.getDocument(), this, state, syncedWith, changeType, changeComment);
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public Date getLastModified() {
        return (Date)lastModified.clone();
    }

    public long getTotalSizeOfParts() {
        return totalSizeOfParts;
    }
    
    private boolean needsSaving() {
        return stateChanged || syncedWithChanged || changeTypeChanged || changeCommentChanged;
    }
    
    private void resetChangeFlags() {
        VersionImpl.this.stateChanged = false;
        VersionImpl.this.syncedWithChanged = false;
        VersionImpl.this.changeTypeChanged = false;
        VersionImpl.this.changeCommentChanged = false;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public void stateChanged(VersionState versionState, VersionKey syncedWith, ChangeType changeType, String changeComment, Date lastModfied, long lastModifier) {
            VersionImpl.this.state = versionState;
            VersionImpl.this.syncedWith = syncedWith;
            VersionImpl.this.changeType = changeType;
            VersionImpl.this.changeComment = changeComment;
            VersionImpl.this.lastModified = lastModfied;
            VersionImpl.this.lastModifier = lastModifier;
            
            VersionImpl.this.resetChangeFlags();
        }

        public void setParts(PartImpl[] parts) {
            VersionImpl.this.parts = parts;
        }

        public void setFields(FieldImpl[] fields) {
            VersionImpl.this.fields = fields;
        }

        public void setLinks(LinkImpl[] links) {
            VersionImpl.this.links = links;
        }
        
        public void setSummary(String summary) {
            VersionImpl.this.summary = summary;
        }

        public PartImpl[] getPartImpls() {
            lazyLoadCheck();
            return parts;
        }
    }

    public String getSummary() {
        return summary == null ? "" : summary;
    }

}
