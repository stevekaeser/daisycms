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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.outerj.daisy.repository.ByteArrayPartDataSource;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentCollections;
import org.outerj.daisy.repository.DocumentTypeInconsistencyException;
import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.FieldNotFoundException;
import org.outerj.daisy.repository.Fields;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Link;
import org.outerj.daisy.repository.Links;
import org.outerj.daisy.repository.LiveHistoryEntry;
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
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionNotFoundException;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Versions;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.repository.commonimpl.variant.CommonVariantManager;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.util.DateUtil;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.LiveHistoryEntryDocument;
import org.outerx.daisy.x10.TimelineDocument;
import org.outerx.daisy.x10.DocumentDocument.Document.NewLiveStrategy;

/**
 * Encapsulates all variant-specific data of a document.
 * An instance of this class is contained by {@link DocumentImpl}.
 */
public class DocumentVariantImpl {
    private DocumentImpl ownerDocument;
    private DocumentStrategy documentStrategy;
    private CommonRepository repository;
    private AuthenticatedUser currentUser;
    /** The parts mapped by PartType ID. */
    private Map<Long, Part> parts = new HashMap<Long, Part>();
    private boolean partChanges = false;
    private long documentTypeId;
    private boolean documentTypeChanged = false;
    private String name;
    private boolean nameUpdated = false;
    private long branchId;
    private long languageId;
    private boolean isNew;
    private Date lastModified;
    private long lastModifier = -1;
    private boolean retired = false;
    private boolean retiredChanged = false;
    private VersionImpl[] versions;
    /** Cached reference to the live version. */
    private Version liveVersion;
    /** Indicates if the variable liveVersion has been initialiased. */
    private boolean liveVersionLoaded = false;
    /** Cached reference to the last version. */
    private Version lastVersion;
    /** Indicates if the variable lastVersion has been initialiased. */
    private boolean lastVersionLoaded = false;
    private long lastVersionId = -1;
    private long liveVersionId = -1;
    /** The fields mapped by FieldType ID. */
    private Map<Long, Field> fields = new HashMap<Long, Field>();
    private boolean fieldChanges = false;
    private List<Link> links = new ArrayList<Link>(3);
    private boolean linkChanges = false;
    private Map<String, String> customFields = new HashMap<String, String>();
    private boolean customFieldChanges = false;
    private Map<Long, DocumentCollection> documentCollections = new HashMap<Long, DocumentCollection>();
    private boolean documentCollectionChanges = false;
    private LockInfoImpl lockInfo = new LockInfoImpl();
    private VersionState versionState = VersionState.PUBLISH;
    private String summary;
    private long updateCount = 0;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private long createdFromBranchId = -1;
    private long createdFromLanguageId = -1;
    private long createdFromVersionId = -1;
    // the variables below are values we need to store to be able to pass them on
    // from the remote implementation
    private boolean validateOnSave = true;
    private boolean documentTypeChecksEnabled = true;
    private long startBranchId = -1;
    private long startLanguageId = -1;
    private VersionKey syncedWith;
    private ChangeType changeType = ChangeType.MAJOR;
    private String changeComment;
    private long lastMajorChangeVersionId = -1;
    private long liveMajorChangeVersionId = -1;
    private LiveStrategy liveStrategy = LiveStrategy.DEFAULT;
    private long requestedLiveVersionId = 0;
    private TimelineImpl timeline;

    public static final String ERROR_ACCESSING_REPOSITORY_SCHEMA = "Error accessing repository schema information.";
    private static final String READ_ONLY_MESSAGE = "This document object is read-only.";

    public DocumentVariantImpl(DocumentImpl ownerDocument, DocumentStrategy documentStrategy, CommonRepository repository, AuthenticatedUser currentUser, long documentTypeId, long branchId, long languageId) {
        this.ownerDocument = ownerDocument;
        this.documentStrategy = documentStrategy;
        this.repository = repository;
        this.currentUser = currentUser;
        this.documentTypeId = documentTypeId;
        this.branchId = branchId;
        this.languageId = languageId;
        this.isNew = true;
        
        this.timeline = new TimelineImpl(this, documentStrategy);
    }

    public DocumentVariantImpl.IntimateAccess getIntimateAccess(DocumentStrategy strategy) {
        if (this.documentStrategy == strategy)
            return intimateAccess;
        return null;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public boolean isNew() {
        return isNew;
    }

    public long getDocumentTypeId() {
        return documentTypeId;
    }

    public String getDocumentId() {
        return ownerDocument.getId();
    }

    public long getDocSeqId() {
        return ownerDocument.getSeqId();
    }

    public String getDocNamespace() {
        return ownerDocument.getNamespace();
    }

    public VariantKey getKey() {
        if (ownerDocument.isNew())
            throw new IllegalStateException("Cannot get variant key: document is new and thus has no ID assigned just yet.");
        return new VariantKey(ownerDocument.getId(), getBranchId(), getLanguageId());
    }

    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;
    }

    public void changeDocumentType(long documentTypeId) throws RepositoryException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (documentTypeId != this.documentTypeId) {
            // fetch the document type to be sure it exists
            // the method below will throw an exception if it doesn't exist
            repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);

            this.documentTypeId = documentTypeId;
            this.documentTypeChanged = true;
        }
    }

    public void changeDocumentType(String documentTypeName) throws RepositoryException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        DocumentType documentType = repository.getRepositorySchema().getDocumentTypeByName(documentTypeName, false, currentUser);
        changeDocumentType(documentType.getId());
    }

    public Field getField(String name) throws FieldNotFoundException {
        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return getField(fieldType.getId());
    }

    public Field getField(long fieldTypeId) throws FieldNotFoundException {
        Field field = fields.get(fieldTypeId);
        if (field == null)
            throw new FieldNotFoundException(fieldTypeId);
        else
            return field;
    }

    public boolean hasField(long fieldTypeId) {
        Field field = fields.get(fieldTypeId);
        return field != null;
    }

    public boolean hasField(String fieldTypeName) {
        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldTypeByName(fieldTypeName, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return hasField(fieldType.getId());
    }

    public Fields getFields() {
        return new FieldsImpl(fields.values().toArray(new Field[0]));
    }

    public Fields getFieldsInOrder() {
        return new FieldsImpl(orderFields(fields.values().toArray(new Field[0])));
    }

    public void setField(String name, Object value) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        setField(fieldType.getId(), value);
    }

    public void setField(long fieldTypeId, Object value) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (value == null)
            throw new NullPointerException("Field value cannot be null.");

        DocumentType documentType;
        try {
            documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldTypeById(fieldTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        if (documentTypeChecksEnabled && !documentType.hasFieldType(fieldTypeId))
            throw new DocumentTypeInconsistencyException("The FieldType \"" + fieldType.getName() + "\" (ID: " + fieldTypeId + ") is not allowed on this document.");

        // For multivalue fields, make sure the array is a clone so that it's unmodifiable from the outside
        if (value instanceof Object[])
            value = ((Object[])value).clone();

        value = checkAndNormalizeFieldValue(fieldType, value);

        FieldImpl field = (FieldImpl)fields.get(new Long(fieldTypeId));

        // if the value didn't change, don't update it
        if (field != null && (fieldType.isMultiValue() ? Arrays.equals((Object[])field.getValue(), (Object[])value) : field.getValue().equals(value)))
            return;

        field = new FieldImpl(intimateAccess, fieldTypeId, value);
        fields.put(new Long(fieldTypeId), field);
        fieldChanges = true;
    }

    private Object checkAndNormalizeFieldValue(FieldType fieldType, Object value) throws DocumentTypeInconsistencyException {
        if (fieldType.isMultiValue()) {
            if (!value.getClass().isArray())
                throw new DocumentTypeInconsistencyException("The value for the multivalue-field \"" + fieldType.getName() + "\" should be an array.");
            Object[] values = (Object[])value;
            if (values.length == 0)
                throw new DocumentTypeInconsistencyException("The value supplied for the multivalue field \"" + fieldType.getName() + "\" is a zero-length array, it should be an array of at least one element.");
            for (int i = 0; i < values.length; i++) {
                values[i] = checkAndNormalizeHierarchyValue(fieldType, values[i], i);
            }
            return values;
        } else {
            return checkAndNormalizeHierarchyValue(fieldType, value, -1);
        }
    }

    private Object checkAndNormalizeHierarchyValue(FieldType fieldType, Object value, int multiValueIndex) throws DocumentTypeInconsistencyException {
        if (fieldType.isHierarchical()) {
            if (!(value instanceof HierarchyPath))
                throw new DocumentTypeInconsistencyException("The supplied value for hierarchical field \"" + fieldType.getName() + "\" should be a HierarchyPath object.");
            HierarchyPath hierarchyPath = (HierarchyPath)value;
            Object[] values = hierarchyPath.getElements();
            if (values.length == 0)
                throw new DocumentTypeInconsistencyException("The supplied HierarchyPath object for hierarchical field \"" + fieldType.getName() + "\" should contain at least one element.");
            for (int i = 0; i < values.length; i++) {
                values[i] = checkAndNormalizePrimitiveValue(fieldType, values[i], multiValueIndex, i);
            }
            return new HierarchyPath(values);
        } else {
            return checkAndNormalizePrimitiveValue(fieldType, value, multiValueIndex, -1);
        }
    }

    private Object checkAndNormalizePrimitiveValue(FieldType fieldType, Object value, int multiValueIndex, int hierarchyPathIndex) throws DocumentTypeInconsistencyException {
        ValueType valueType = fieldType.getValueType();
        if (!valueType.getTypeClass().isAssignableFrom(value.getClass()))
            throw new DocumentTypeInconsistencyException("The supplied value for the field \"" + fieldType.getName() + "\" is not of the correct type. Expected was a " + valueType.toString() + " (" + valueType.getTypeClass().getName() + ") but got a " + value.getClass().getName() + getFieldValuePositionDetails(multiValueIndex, hierarchyPathIndex));

        // perform normalization of values for certain value types
        if (valueType == ValueType.DATE || valueType == ValueType.DATETIME) {
            boolean keepTime = fieldType.getValueType() == ValueType.DATETIME;
            value = DateUtil.getNormalizedDate((Date)value, keepTime);
        } else if (valueType == ValueType.LINK) {
            VariantKey variantKey = (VariantKey)value;
            value = new VariantKey(repository.normalizeDocumentId(variantKey.getDocumentId()), variantKey.getBranchId(), variantKey.getLanguageId());
        }

        return value;
    }

    private String getFieldValuePositionDetails(int multiValueIndex, int hierarchyPathIndex) {
        StringBuilder builder = new StringBuilder();
        if (multiValueIndex != -1 || hierarchyPathIndex != -1)
            builder.append(" (");
        if (multiValueIndex != -1)
            builder.append("multi value index: ").append(multiValueIndex);
        if (hierarchyPathIndex != -1) {
            if (multiValueIndex != -1)
                builder.append(", ");
            builder.append("hierarchy path index: ").append(hierarchyPathIndex);
        }
        if (builder.length() > 0)
            builder.append(")");
        return builder.toString();
    }

    public void deleteField(String name) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        FieldType fieldType;
        try {
            fieldType = repository.getRepositorySchema().getFieldTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        deleteField(fieldType.getId());
    }

    public void deleteField(long fieldTypeId) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        Long key = new Long(fieldTypeId);
        if (fields.containsKey(key)) {
            fields.remove(new Long(fieldTypeId));
            fieldChanges = true;
        }
    }

    public LockInfo getLockInfo(boolean fresh) throws RepositoryException {
        LockInfoImpl lockInfo = this.lockInfo;
        if ((fresh || lockInfo == null) && !isNew) {
            synchronized(this) {
                lockInfo = documentStrategy.getLockInfo(this);
                this.lockInfo = lockInfo;
            }
        }
        return lockInfo;
    }

    // This method is by purpose not in the Document interface. It is used to clear lock
    // info in cached document objects. If it would be in the interface, then the method
    // getLockInfo should cover the case where lockInfo is null and id is -1.
    public void clearLockInfo() {
        this.lockInfo = null;
    }

    public boolean lock(long duration, LockType lockType) throws RepositoryException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (isNew)
            throw new RepositoryException("Can't take a lock on a new, non-saved document variant.");

        lockInfo = documentStrategy.lock(this, duration, lockType);

        if (!lockInfo.hasLock())
            return false;

        return lockInfo.getUserId() == currentUser.getId();
    }

    public boolean releaseLock() throws RepositoryException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (isNew)
            return true;

        lockInfo = documentStrategy.releaseLock(this);
        return !lockInfo.hasLock();
    }
    
    public void addXml(DocumentDocument.Document documentXml, AccessDetails accessDetails) throws RepositoryException {
        addNonVersionedDataToXml(documentXml, accessDetails);

        documentXml.setDataVersionId(-1); // -1 indicates "current data in object, whether it's saved or not"
        documentXml.setName(name);
        documentXml.setFields(VersionedDataAccessWrapper.filterFields(getFieldsInOrder(), accessDetails).getXml().getFields());
        documentXml.setParts(VersionedDataAccessWrapper.filterParts(getPartsInOrder(), accessDetails).getXml().getParts());
        documentXml.setLinks(getLinks().getXml().getLinks());
        documentXml.setValidateOnSave(validateOnSave);
    }

    public void addXml(DocumentDocument.Document documentXml, long versionId, AccessDetails accessDetails) throws RepositoryException {
        Version version = getVersion(versionId);

        addNonVersionedDataToXml(documentXml, accessDetails);
        documentXml.setDataVersionId(versionId);
        documentXml.setName(version.getDocumentName());
        documentXml.setFields(VersionedDataAccessWrapper.filterFields(version.getFieldsInOrder(), accessDetails).getXml().getFields());
        documentXml.setParts(VersionedDataAccessWrapper.filterParts(version.getPartsInOrder(), accessDetails).getXml().getParts());
        documentXml.setLinks(version.getLinks().getXml().getLinks());
    }

    public void addNonVersionedDataToXml(DocumentDocument.Document documentXml, AccessDetails accessDetails) {
        if (!isNew) {
            GregorianCalendar lastModified = new GregorianCalendar();
            lastModified.setTime(this.lastModified);
            documentXml.setVariantLastModified(lastModified);
            documentXml.setVariantLastModifier(lastModifier);
            if (liveVersionId != -1)
                documentXml.setLiveVersionId(liveVersionId);
        }

        documentXml.setBranchId(branchId);
        documentXml.setLanguageId(languageId);
        documentXml.setTypeId(documentTypeId);
        documentXml.setLastVersionId(lastVersionId);
        documentXml.setRetired(retired);
        documentXml.setNewVersionState(DocumentDocument.Document.NewVersionState.Enum.forString(versionState.toString()));
        if (syncedWith != null) {
            documentXml.setNewSyncedWithLanguageId(syncedWith.getLanguageId());
            documentXml.setNewSyncedWithVersionId(syncedWith.getVersionId());
        }
        documentXml.setNewChangeType(DocumentDocument.Document.NewChangeType.Enum.forString(changeType.toString()));
        if  (changeComment != null) {
            documentXml.setNewChangeComment(changeComment);
        }
        documentXml.setNewLiveStrategy(NewLiveStrategy.Enum.forString(liveStrategy.toString()));
        if (requestedLiveVersionId != -1) {
            documentXml.setRequestedLiveVersionId(requestedLiveVersionId);
        }
        documentXml.setVariantUpdateCount(updateCount);
        if ((accessDetails == null || accessDetails.isGranted(AclDetailPermission.SUMMARY)) && summary != null)
            documentXml.setSummary(summary);
        if (accessDetails == null || accessDetails.isGranted(AclDetailPermission.NON_LIVE))
            documentXml.setFullVersionAccess(true);
        documentXml.setCreatedFromBranchId(createdFromBranchId);
        documentXml.setCreatedFromLanguageId(createdFromLanguageId);
        documentXml.setCreatedFromVersionId(createdFromVersionId);
        documentXml.setDocumentTypeChecksEnabled(documentTypeChecksEnabled);
        documentXml.setLastMajorChangeVersionId(lastMajorChangeVersionId);
        documentXml.setLiveMajorChangeVersionId(liveMajorChangeVersionId);

        DocumentDocument.Document.CustomFields customFieldsXml = documentXml.addNewCustomFields();
        for (Map.Entry<String, String> customField : customFields.entrySet()) {
            DocumentDocument.Document.CustomFields.CustomField customFieldXml = customFieldsXml.addNewCustomField();
            customFieldXml.setName(customField.getKey());
            customFieldXml.setValue(customField.getValue());
        }

        LockInfo lockInfo;
        try {
            lockInfo = getLockInfo(false);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        documentXml.setLockInfo(lockInfo.getXml().getLockInfo());

        long[] collectionIds = new long[documentCollections.size()];
        Iterator collectionsIt = documentCollections.values().iterator();
        int i = 0;
        while (collectionsIt.hasNext()) {
            DocumentCollection collection = (DocumentCollection)collectionsIt.next();
            collectionIds[i] = collection.getId();
            i++;
        }
        documentXml.addNewCollectionIds().setCollectionIdArray(collectionIds);
        
        TimelineDocument.Timeline timelineXml = documentXml.addNewTimeline();
        for (LiveHistoryEntry entry: timeline.getLiveHistory()) {
            entry.toXml(timelineXml.addNewLiveHistoryEntry());
        }
    }

    public void setName(String name) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (name == null) {
            throw new NullPointerException("name may not be null.");
        }
        if (name.trim().equals("")) {
        	throw new IllegalArgumentException("name may not be empty.");
        }

        if (!name.equals(this.name)) {
            this.name = name;
            nameUpdated = true;
        }
    }

    public String getName() {
        return name;
    }

    public void setPart(String partTypeName, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (data == null)
            throw new NullPointerException("data argument cannot be null.");

        setPart(partTypeName, mimeType, new ByteArrayPartDataSource(data));

    }

    public void setPart(long partTypeId, String mimeType, byte[] data) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (data == null)
            throw new NullPointerException("data argument cannot be null.");

        setPart(partTypeId, mimeType, new ByteArrayPartDataSource(data));
    }

    public void setPart(String partTypeName, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(partTypeName, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        setPart(partType.getId(), mimeType, partDataSource);
    }

    public void setPart(long partTypeId, String mimeType, PartDataSource partDataSource) throws DocumentTypeInconsistencyException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        // first check with the DocumentType if this part is allowed
        DocumentType documentType;
        try {
            documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeById(partTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        if (mimeType == null || mimeType.length() == 0)
            throw new NullPointerException("mimeType argument cannot be null or an empty string");

        if (partDataSource == null)
            throw new NullPointerException("partDataSource argument cannot be null.");

        if (documentTypeChecksEnabled && !documentType.hasPartType(partTypeId))
            throw new DocumentTypeInconsistencyException("The PartType \"" + partType.getName() + "\" (ID: " + partTypeId + ") is not allowed on this document.");

        if (documentTypeChecksEnabled && !partType.mimeTypeAllowed(mimeType))
            throw new DocumentTypeInconsistencyException("The mime-type \"" + mimeType + "\" isn't part of the allowed mime types (" + partType.getMimeTypes() + ") required by the PartType \"" + partType.getName() + "\" (ID: " + partTypeId + ").");

        PartImpl part = new PartImpl(intimateAccess, partTypeId, lastVersionId, -1);
        PartImpl.IntimateAccess partInt = part.getIntimateAccess(documentStrategy);
        partInt.setMimeType(mimeType);
        partInt.setData(partDataSource);
        partInt.setNewOrUpdated(true, true);

        parts.put(new Long(partTypeId), part);
        partChanges = true;
    }

    public void setPartFileName(String partTypeName, String fileName) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(partTypeName, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        setPartFileName(partType.getId(), fileName);
    }

    public void setPartFileName(long partTypeId, String fileName) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        PartImpl part = (PartImpl)parts.get(new Long(partTypeId));
        if (part == null)
            throw new RepositoryRuntimeException("The document does not have a part for part type ID " + partTypeId);

        if ((part.getFileName() == null && fileName == null) || (part.getFileName() != null && part.getFileName().equals(fileName)))
            return;

        PartImpl.IntimateAccess partInt = part.getIntimateAccess(documentStrategy);
        partInt.setFileName(fileName);
        partInt.setNewOrUpdated(true, partInt.isDataUpdated());
        partChanges = true;
    }

    public void setPartMimeType(String partTypeName, String mimeType) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(partTypeName, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }

        setPartMimeType(partType.getId(), mimeType);
    }

    public void setPartMimeType(long partTypeId, String mimeType) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (mimeType == null)
            throw new IllegalArgumentException("Part mime-type cannot be set to null value.");

        PartImpl part = (PartImpl)parts.get(new Long(partTypeId));
        if (part == null)
            throw new RepositoryRuntimeException("The document does not have a part for part type ID " + partTypeId);

        if (part.getMimeType().equals(mimeType))
            return;

        PartImpl.IntimateAccess partInt = part.getIntimateAccess(documentStrategy);
        partInt.setMimeType(mimeType);
        partInt.setNewOrUpdated(true, partInt.isDataUpdated());
        partChanges = true;
    }

    public Parts getParts() {
        return new PartsImpl(parts.values().toArray(new Part[0]));
    }

    public Parts getPartsInOrder() {
        return new PartsImpl(orderParts(parts.values().toArray(new Part[0])));
    }

    private Part[] orderParts(Part[] parts) {
        Part[] resultList = new Part[parts.length];
        boolean[] handled = new boolean[parts.length];

        DocumentType documentType;
        try {
            documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();

        int resultListPos = -1;
        for (PartTypeUse partTypeUse : partTypeUses) {
            long id = partTypeUse.getPartType().getId();
            for (int k = 0; k < parts.length; k++) {
                if (parts[k].getTypeId() == id) {
                    handled[k] = true;
                    resultListPos++;
                    resultList[resultListPos] = parts[k];
                    break;
                }
            }
        }

        for (int i = 0; i < handled.length; i++) {
            if (!handled[i]) {
                resultListPos++;
                resultList[resultListPos] = parts[i];
            }
        }

        return resultList;
    }

    private Field[] orderFields(Field[] fields) {
        Field[] resultList = new Field[fields.length];
        boolean[] handled = new boolean[fields.length];

        DocumentType documentType;
        try {
            documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();

        int resultListPos = -1;
        for (FieldTypeUse fieldTypeUse : fieldTypeUses) {
            long id = fieldTypeUse.getFieldType().getId();
            for (int k = 0; k < fields.length; k++) {
                if (fields[k].getTypeId() == id) {
                    handled[k] = true;
                    resultListPos++;
                    resultList[resultListPos] = fields[k];
                    break;
                }
            }
        }

        for (int i = 0; i < handled.length; i++) {
            if (!handled[i]) {
                resultListPos++;
                resultList[resultListPos] = fields[i];
            }
        }

        return resultList;
    }

    public void deletePart(long partTypeId) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        Long key = new Long(partTypeId);
        if (parts.containsKey(key)) {
            parts.remove(key);
            partChanges = true;
        }
    }

    public void deletePart(String name) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        deletePart(partType.getId());
    }

    public Part getPart(long partTypeId) throws PartNotFoundException {
        Part part = parts.get(new Long(partTypeId));
        if (part == null)
            throw new PartNotFoundException(partTypeId);
        else
            return part;
    }

    public Part getPart(String name) throws PartNotFoundException {
        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return getPart(partType.getId());
    }

    public boolean hasPart(long partTypeId) {
        Part part = parts.get(new Long(partTypeId));
        return part != null;
    }

    public boolean hasPart(String name) {
        PartType partType;
        try {
            partType = repository.getRepositorySchema().getPartTypeByName(name, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return hasPart(partType.getId());
    }

    public void setCustomField(String name, String value) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (name == null)
            throw new RuntimeException("name argument cannot be null.");
        if (value == null)
            throw new RuntimeException("value argument cannot be null.");

        customFields.put(name, value);
        customFieldChanges = true;
    }

    public void deleteCustomField(String name) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (name == null)
            throw new RuntimeException("name argument cannot be null.");

        customFields.remove(name);
        customFieldChanges = true;
    }

    public void clearCustomFields() {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        customFields.clear();
        customFieldChanges = true;
    }

    public void clearCollections() {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        documentCollections.clear();
        documentCollectionChanges = true;
    }

    public Map<String, String> getCustomFields() {
        return new HashMap<String, String>(customFields);
    }

    public String getCustomField(String name) {
        if (name == null)
            throw new NullPointerException("name argument cannot be null.");

        return customFields.get(name);
    }

    public boolean hasCustomField(String name) {
        if (name == null)
            throw new NullPointerException("name argument cannot be null.");

        return customFields.containsKey(name);
    }

    public Links getLinks() {
        return new LinksImpl(links.toArray(new Link[links.size()]));
    }

    public void addLink(String title, String target) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        links.add(new LinkImpl(title, target));
        linkChanges = true;
    }

    public void deleteLink(int index) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        links.remove(index);
        linkChanges = true;
    }

    public void clearLinks() {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        links.clear();
        linkChanges = true;
    }

    public void validate() throws DocumentTypeInconsistencyException {
        fullConsistencyCheck();
    }

    public void setNewVersionState(VersionState versionState) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (versionState == null)
            throw new NullPointerException("versionState argument cannot be null.");

        this.versionState = versionState;
    }

    public VersionState getNewVersionState() {
        return versionState;
    }

    public synchronized Version getVersion(long versionId) throws RepositoryException {
        if (isNew)
            throw new RepositoryException("A new document variant has no versions.");
        if (versionId < 1 || versionId > lastVersionId)
            throw new VersionNotFoundException(String.valueOf(versionId), ownerDocument.getId(), String.valueOf(branchId), String.valueOf(languageId));

        checkVersionsArray();

        int arrayPos = (int)versionId - 1;
        if (versions[arrayPos] == null) {
            versions[arrayPos] = documentStrategy.loadVersion(this, versionId);
        }

        return versions[arrayPos];
    }

    public Version getLastVersion() throws RepositoryException {
        if (isNew)
            return null;

        if (lastVersionLoaded)
            return lastVersion;

        Version[] versions = getVersions().getArray();
        lastVersion = versions[versions.length - 1];
        lastVersionLoaded = true;
        return lastVersion;
    }

    public Version getLiveVersion() throws RepositoryException {
        if (isNew || liveVersionId == -1)
            return null;

        if (liveVersionLoaded)
            return liveVersion;

        checkVersionsArray();
        int liveVersionPos = (int)liveVersionId - 1; // position in the versions array
        if (versions[(int)liveVersionId - 1] != null) {
            liveVersion = versions[liveVersionPos];
        } else {
            VersionImpl newLiveVersion = documentStrategy.loadVersion(this, liveVersionId);
            versions[liveVersionPos] = newLiveVersion;
            liveVersion = newLiveVersion;
        }
        liveVersionLoaded = true;
        return liveVersion;
    }

    public long getLiveVersionId() {
        return liveVersionId;
    }

    public synchronized Versions getVersions() throws RepositoryException {
        if (isNew)
            return new VersionsImpl(new Version[0]);

        loadVersions();

        return new VersionsImpl(versions.clone());
    }

    private void loadVersions() throws RepositoryException {
        checkVersionsArray();

        VersionImpl[] loadedVersions = null;

        // check if all versions are loaded, if not load them
        // note that we don't simply replace the whole version array because some
        // versions may already be loaded fully, and otherwise that work would be lost.
        for (int i = 0; i < versions.length; i++) {
            if (versions[i] == null) {
                if (loadedVersions == null)
                    loadedVersions = documentStrategy.loadShallowVersions(this);
                versions[i] = loadedVersions[i];
            }
        }
    }

    public long getLastVersionId() {
        return lastVersionId;
    }

    private void checkVersionsArray() {
        if (versions == null) {
            versions = new VersionImpl[(int)lastVersionId];
        } else if (versions.length != lastVersionId) {
            VersionImpl[] oldVersions = versions;
            versions = new VersionImpl[(int)lastVersionId];
            System.arraycopy(oldVersions, 0, versions, 0, oldVersions.length);
        }
    }

    private void fullConsistencyCheck() throws DocumentTypeInconsistencyException {
        DocumentType documentType;
        try {
            documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false, currentUser);
        } catch (RepositoryException e) {
            throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA);
        }

        // check the parts
        PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
        boolean[] hasPartType = new boolean[partTypeUses.length];

        Part[] parts = getParts().getArray();
        for (Part part : parts) {
            long partTypeId = part.getTypeId();
            int partTypeIndex = -1;
            for (int j = 0; j < partTypeUses.length; j++) {
                if (partTypeUses[j].getPartType().getId() == partTypeId) {
                    partTypeIndex = j;
                }
            }
            if (partTypeIndex == -1)
                throw new DocumentTypeInconsistencyException("The document contains a not-allowed part (PartType ID: " + partTypeId + ").");
            hasPartType[partTypeIndex] = true;
            if (!partTypeUses[partTypeIndex].getPartType().mimeTypeAllowed(part.getMimeType()))
                throw new DocumentTypeInconsistencyException("The mime-type for the part \"" + partTypeUses[partTypeIndex].getPartType().getName() + "\" isn't part of the allowed mime types (PartType ID: " + partTypeId + ").");
        }

        for (int i = 0; i < partTypeUses.length; i++) {
            if (partTypeUses[i].isRequired() && !hasPartType[i])
                throw new DocumentTypeInconsistencyException("The document doesn't have the required part \"" + partTypeUses[i].getPartType().getName() + "\" (ID: " + partTypeUses[i].getPartType().getId() + ").");
        }

        // check the fields
        FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
        boolean[] hasFieldType = new boolean[fieldTypeUses.length];

        Field[] fields = getFields().getArray();
        for (Field field : fields) {
            long fieldTypeId = field.getTypeId();
            int fieldTypeIndex = -1;
            for (int j = 0; j < fieldTypeUses.length; j++) {
                if (fieldTypeUses[j].getFieldType().getId() == fieldTypeId) {
                    fieldTypeIndex = j;
                }
            }
            if (fieldTypeIndex == -1)
                throw new DocumentTypeInconsistencyException("The document contains a not-allowed field (FieldType ID: " + fieldTypeId + ").");
            hasFieldType[fieldTypeIndex] = true;
        }

        for (int i = 0; i < fieldTypeUses.length; i++) {
            if (fieldTypeUses[i].isRequired() && !hasFieldType[i])
                throw new DocumentTypeInconsistencyException("The document doesn't have the required field \"" + fieldTypeUses[i].getFieldType().getName() + "\" (ID: " + fieldTypeUses[i].getFieldType().getId() + ").");
        }
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return lastModified;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public boolean isRetired() {
        return retired;
    }

    public void setRetired(boolean retired) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (retired != this.retired) {
            this.retired = retired;
            this.retiredChanged = true;
        }
    }

    public DocumentCollections getCollections() {
        return new DocumentCollectionsImpl(documentCollections.values().toArray(new DocumentCollection[0]));
    }

    public boolean inCollection(DocumentCollection collection) {
        return documentCollections.containsKey(new Long(collection.getId()));
    }

    public boolean inCollection(long collectionId) {
        return documentCollections.containsKey(new Long(collectionId));
    }

    public void addToCollection(DocumentCollection c) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (c.getId() == -1)
            throw new IllegalArgumentException("The specified collection has not yet been saved.");

        this.documentCollectionChanges = true;
        this.documentCollections.put(new Long(c.getId()), c);
    }

    public void removeFromCollection(DocumentCollection c) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.documentCollectionChanges = true;
        documentCollections.remove(new Long(c.getId()));
    }

    public String getSummary() {
        if (isNew) {
            return "";
        } else {
            return summary == null ? "" : summary;
        }
    }

    public long getLastMajorChangeVersionId() {
        return lastMajorChangeVersionId;
    }

    public long getLiveMajorChangeVersionId() {
        return liveMajorChangeVersionId;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public long getCreatedFromBranchId() {
        return createdFromBranchId;
    }

    public long getCreatedFromLanguageId() {
        return createdFromLanguageId;
    }

    public long getCreatedFromVersionId() {
        return createdFromVersionId;
    }

    public void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.documentTypeChecksEnabled = documentTypeChecksEnabled;
    }

    public void setNewSyncedWithVersion(long syncedWithLanguageId, long syncedWithVersionId) throws RepositoryException {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        // NOTE: similar code to VersionImpl.setSyncedWithVersion

        if (syncedWithLanguageId == -1 || syncedWithVersionId == -1) {
            if (syncedWithLanguageId != -1 || syncedWithVersionId != -1)
                throw new IllegalArgumentException("The languageId and versionId arguments should both be -1 or not be -1 at all.");

            syncedWith = null;
            return;
        }

        if (syncedWithLanguageId == this.languageId) {
            throw new IllegalArgumentException("You can not make a document synced with a version in the same language");
        }

        // Check the language exists
        repository.getVariantManager().getLanguage(languageId, false, currentUser);

        syncedWith = new VersionKey(this.getDocumentId(), branchId, syncedWithLanguageId, syncedWithVersionId);
    }

    public VersionKey getNewSyncedWith() {
        return syncedWith;
    }

    public void setNewChangeType(ChangeType changeType) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (changeType == null)
            throw new IllegalArgumentException("Null argument: changeType");

        this.changeType = changeType;
    }

    public ChangeType getNewChangeType() {
        return changeType;
    }

    public void setNewChangeComment(String changeComment) {
        if (ownerDocument.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (changeComment != null) {
            changeComment = changeComment.trim();
            if (changeComment.length() == 0)
                changeComment = null;
        }
        
        this.changeComment = changeComment;
    }

    public String getNewChangeComment() {
        return changeComment;
    }

    /**
     * Checks whether this document needs a new version. This is the case when:
     */
    public boolean needsNewVersion() {
        // a new document always needs an initial version
        if (isNew)
            return true;

        return nameUpdated || fieldChanges || linkChanges || partChanges;
    }

    public boolean needsSaving() {
        boolean nonVersionedChanges = documentCollectionChanges || customFieldChanges || documentTypeChanged || retiredChanged || requestedLiveVersionId != 0 || timeline.getIntimateAccess(documentStrategy).hasChanges();
        if (nonVersionedChanges)
            return true;
        else
            return needsNewVersion();
    }
    
    public class IntimateAccess {

        private IntimateAccess() {
        }

        public void setLockInfo(LockInfoImpl lockInfo) {
            DocumentVariantImpl.this.lockInfo = lockInfo;
        }
        
        public void setLiveHistory(LiveHistoryEntry[] liveHistory) {
            DocumentVariantImpl.this.timeline.getIntimateAccess(documentStrategy).setLiveHistory(liveHistory, updateCount);
        }
        
        public long getLastVersionId() {
            return lastVersionId;
        }

        public long getLiveVersionId() {
            return lastVersionId;
        }

        public boolean hasCustomFieldChanges() {
            return customFieldChanges;
        }

        public void setIsNew(boolean isNew) {
            DocumentVariantImpl.this.isNew = isNew;
        }

        public boolean isNameUpdated() {
            return nameUpdated;
        }

        public boolean hasFieldChanges() {
            return fieldChanges;
        }

        public boolean hasLinkChanges() {
            return linkChanges;
        }

        public boolean hasPartChanges() {
            return partChanges;
        }

        public boolean hasCollectionChanges() {
            return documentCollectionChanges;
        }
        
        public TimelineImpl getTimeline() {
            return timeline;
        }
        
        /**
         * Intialises required fields when loading an existing document variant.
         */
        public void load(long documentTypeId, boolean retired, long lastVersionId, long liveVersionId,
                Date lastModified, long lastModifier, long createdFromBranchId, long createdFromLanguageId,
                long createdFromVersionId, long lastMajorChangeVersionId, long liveMajorChangeVersionId, long updateCount) {
            DocumentVariantImpl.this.documentTypeId = documentTypeId;
            DocumentVariantImpl.this.retired = retired;
            DocumentVariantImpl.this.lastVersionId = lastVersionId;
            DocumentVariantImpl.this.liveVersionId = liveVersionId;
            DocumentVariantImpl.this.lastModified = lastModified;
            DocumentVariantImpl.this.lastModifier = lastModifier;
            DocumentVariantImpl.this.createdFromBranchId = createdFromBranchId;
            DocumentVariantImpl.this.createdFromLanguageId = createdFromLanguageId;
            DocumentVariantImpl.this.createdFromVersionId = createdFromVersionId;
            DocumentVariantImpl.this.lastMajorChangeVersionId = lastMajorChangeVersionId;
            DocumentVariantImpl.this.liveMajorChangeVersionId = liveMajorChangeVersionId;
            DocumentVariantImpl.this.updateCount = updateCount;
            
            isNew = false;
        }

        /**
         * Updates the state of this Document object after saving it, also resets
         * all 'dirty' flags.
         */
        public void saved(long lastVersionId, long liveVersionId, Date lastModified, String summary, long updateCount, LiveHistoryEntry[] liveHistory) {
            DocumentVariantImpl.this.lastVersionId = lastVersionId;
            DocumentVariantImpl.this.liveVersionId = liveVersionId;
            DocumentVariantImpl.this.lastModified = lastModified;
            DocumentVariantImpl.this.lastModifier = currentUser.getId();
            DocumentVariantImpl.this.updateCount = updateCount;
            DocumentVariantImpl.this.summary = summary;
            DocumentVariantImpl.this.timeline.getIntimateAccess(documentStrategy).setLiveHistory(liveHistory, updateCount);

            nameUpdated = false;
            partChanges = false;
            linkChanges = false;
            documentCollectionChanges = false;
            requestedLiveVersionId = 0;

            PartImpl[] parts = getPartImpls();
            for (PartImpl part : parts) {
                PartImpl.IntimateAccess partInt = part.getIntimateAccess(documentStrategy);
                partInt.setVersionId(lastVersionId);
                if (partInt.isDataUpdated())
                    partInt.setDataChangedInVersion(lastVersionId);
                partInt.setNewOrUpdated(false, false);
                partInt.setData(null);
            }
            fieldChanges = false;
            customFieldChanges = false;

            // last/live version might have changed
            liveVersion = null;
            liveVersionLoaded = false;
            lastVersion = null;
            lastVersionLoaded = false;
            isNew = false;
            retiredChanged = false;
            documentTypeChanged = false;
        }

        /**
         * Updates the state of the variant after saving its timeline.
         */
        public void timelineSaved(Date lastModified, long lastModifier,
                long updateCount, long liveVersionId, LiveHistoryEntry[] liveHistory) {
            DocumentVariantImpl.this.lastModified = lastModified;
            DocumentVariantImpl.this.lastModifier = lastModifier;
            DocumentVariantImpl.this.updateCount = updateCount;
            DocumentVariantImpl.this.liveVersionId = liveVersionId;
            DocumentVariantImpl.this.timeline.getIntimateAccess(documentStrategy).setLiveHistory(liveHistory, updateCount);
            liveVersion = null;
            liveVersionLoaded = false;
        }

        public DocumentStrategy getDocumentStrategy() {
            return documentStrategy;
        }

        public AuthenticatedUser getCurrentUser() {
            return currentUser;
        }

        /**
         * Sets a user field without marking the user fields as modified.
         */
        public void setCustomField(String name, String value) {
            customFields.put(name, value);
        }

        public PartImpl[] getPartImpls() {
            return parts.values().toArray(new PartImpl[0]);
        }

        public DocumentCollectionImpl[] getDocumentCollectionImpls() {
            return documentCollections.values().toArray(new DocumentCollectionImpl[0]);
        }


        public void addPart(PartImpl part) {
            parts.put(new Long(part.getTypeId()), part);
        }

        /**
         * Adds a link without marking the links as being modified.
         */
        public void addLink(LinkImpl link) {
            links.add(link);
        }

        /**
         * Sets the name of the document without altering the "name dirty" flag.
         */
        public void setName(String name) {
            if (name == null)
                throw new NullPointerException("name may not be null.");
            DocumentVariantImpl.this.name = name;
        }

        /**
         * Adds the given field. This method will not change the flag indicating
         * whether there were field changes.
         */
        public void addField(FieldImpl field) {
            fields.put(new Long(field.getTypeId()), field);
        }

        public DocumentImpl getDocument() {
            return ownerDocument;
        }

        public DocId getDocId() {
            return ownerDocument.getIntimateAccess(documentStrategy).getDocId();
        }

        public CommonRepositorySchema getRepositorySchema() {
            return repository.getRepositorySchema();
        }

        public CommonVariantManager getVariantManager() {
            return repository.getVariantManager();
        }

        /**
         * Adds the specified collection. This method will not change the flag indicating
         * whether there were collection changes.
         */
        public void addCollection(DocumentCollectionImpl collection) {
            documentCollections.put(new Long(collection.getId()), collection);
        }

        public Part[] orderParts(Part[] parts) {
            return DocumentVariantImpl.this.orderParts(parts);
        }

        public Field[] orderFields(Field[] fields) {
            return DocumentVariantImpl.this.orderFields(fields);
        }

        public void setSummary(String summary) {
            DocumentVariantImpl.this.summary = summary;
        }
        
        public DocumentVariantImpl getVariant() {
            return DocumentVariantImpl.this;
        }

        public void setCreatedFrom(long branchId, long languageId, long versionId) {
            createdFromBranchId = branchId;
            createdFromLanguageId = languageId;
            createdFromVersionId = versionId;
        }

        public void setStartFrom(long branchId, long languageId) {
            startBranchId = branchId;
            startLanguageId = languageId;
        }

        public long getStartBranchId() {
            return startBranchId;
        }

        public long getStartLanguageId() {
            return startLanguageId;
        }

        public void setBranchId(long branchId) {
            DocumentVariantImpl.this.branchId = branchId;
        }

        public void setLanguageId(long languageId) {
            DocumentVariantImpl.this.languageId = languageId;            
        }

        /**
         * Keep only the specified set of fields, without marking this document as modified.
         * Intended to remove non-readable fields after loading the document object.
         */
        public void keepFields(Set<String> fieldNames) {
            Set<Long> fieldIds = new HashSet<Long>();
            try {
                for (String fieldName : fieldNames) {
                    FieldType fieldType = repository.getRepositorySchema().getFieldTypeByName(fieldName, false, currentUser);
                    fieldIds.add(fieldType.getId());
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
            }

            Iterator<Map.Entry<Long, Field>> fieldsIt = fields.entrySet().iterator();
            while (fieldsIt.hasNext()) {
                Map.Entry<Long, Field> fieldEntry = fieldsIt.next();
                if (!fieldIds.contains(fieldEntry.getKey()))
                    fieldsIt.remove();
            }
        }

        /**
         * Keep only the specified set of parts, without marking this document as modified.
         * Intended to remove non-readable fields after loading the document object.
         */
        public void keepParts(Set<String> partNames) {
            Set<Long> partIds = new HashSet<Long>();
            try {
                for (String partName : partNames) {
                    PartType partType = repository.getRepositorySchema().getPartTypeByName(partName, false, currentUser);
                    partIds.add(partType.getId());
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
            }

            Iterator<Map.Entry<Long, Part>> partsIt = parts.entrySet().iterator();
            while (partsIt.hasNext()) {
                Map.Entry<Long, Part> partEntry = partsIt.next();
                if (!partIds.contains(partEntry.getKey()))
                    partsIt.remove();
            }
        }

        public void setLiveVersionId(long liveVersionId) {
            DocumentVariantImpl.this.liveVersionId = liveVersionId;
        }

        public void setUpdateCount(long updateCount) {
            DocumentVariantImpl.this.updateCount = updateCount;
        }

    }

    public LiveStrategy getNewLiveStrategy() {
        return liveStrategy;
    }

    public void setNewLiveStrategy(LiveStrategy liveStrategy) {
        this.liveStrategy = liveStrategy;
    }

    public long getRequestedLiveVersionId() {
        return requestedLiveVersionId;
    }

    public void setRequestedLiveVersionId(long liveVersionId) {
        if (liveVersionId < -2 || (liveVersionId > 0 && liveVersionId > lastVersionId)) {
            throw new IllegalArgumentException("liveVersionId should be between -2 and lastVersionId (inclusive)");
        }
        this.requestedLiveVersionId = liveVersionId;
    }
    
    public Timeline getTimeline() {
        return timeline;
    }

    /**
     * @param versionMode
     * @return the version id corresponding with the given versionMode, or an exception when there is no corresponding version
     */
    public long getVersionId(VersionMode versionMode) throws RepositoryException {
        if (isNew)
            throw new RepositoryException("A new document variant has no versions.");

        if (versionMode.isLive()) {
            return liveVersionId;
        }
        if (versionMode.isLast()) {
            return lastVersionId;
        }
        long result = getTimeline().getVersionId(versionMode.getDate());
        if (result == -1)
            throw new VersionNotFoundException(String.valueOf(versionMode), ownerDocument.getId(), String.valueOf(branchId), String.valueOf(languageId));
        return result;
    }

    /**
     * @param versionMode
     * @return the version id corresponding with the given versionMode, or an exception when there is no corresponding version
     */
    public Version getVersion(VersionMode versionMode) throws RepositoryException {
        return getVersion(getVersionId(versionMode));
    }

}