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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


// NOTE: the implementation of this class should not make any assumptions on the
// order in which the events arrive, since in case of remote cache invalidation
// we can't make any assumptions about this.
public class RepositorySchemaCache implements RepositorySchemaListener {
    private SchemaStrategy schemaStrategy;
    private AuthenticatedUser systemUser;

    private Lock partTypesLock = new ReentrantLock();
    private boolean partTypesLoaded = false;
    private Map<Long, PartType> partTypesById;
    private Map<String, PartType> partTypesByName;
    private PartTypes partTypes;

    private Lock fieldTypesLock = new ReentrantLock();
    private boolean fieldTypesLoaded = false;
    private Map<Long, FieldType> fieldTypesById;
    private Map<String, FieldType> fieldTypesByName;
    private FieldTypeImpl[] fieldTypes;

    private Lock documentTypesLock = new ReentrantLock();
    private boolean documentTypesLoaded = false;
    private Map<Long, DocumentType> documentTypesById;
    private Map<String, DocumentType> documentTypesByName;
    private DocumentTypeImpl[] documentTypes;

    public RepositorySchemaCache(SchemaStrategy schemaStrategy, AuthenticatedUser systemUser) {
        this.schemaStrategy = schemaStrategy;
        this.systemUser = systemUser;
    }

    private void assurePartTypesLoaded() throws RepositoryException {
        if (partTypesLoaded)
            return;

        try {
            partTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (partTypesLoaded)
                return;

            // load, cache, index the PartTypes
            Map<Long, PartType> partTypesById = new HashMap<Long, PartType>();
            Map<String, PartType> partTypesByName = new HashMap<String, PartType>();
            Collection<PartTypeImpl> partTypes = schemaStrategy.getAllPartTypes(systemUser);
            for (PartTypeImpl partType : partTypes) {
                partType.makeReadOnly();
                partTypesById.put(new Long(partType.getId()), partType);
                partTypesByName.put(partType.getName(), partType);
            }
            this.partTypesById = partTypesById;
            this.partTypesByName = partTypesByName;
            this.partTypes = new PartTypesImpl(partTypes.toArray(new PartType[0]));
            this.partTypesLoaded = true;
        } finally {
            partTypesLock.unlock();
        }
    }

    private void assureFieldTypesLoaded() throws RepositoryException {
        if (fieldTypesLoaded)
            return;

        try {
            fieldTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (fieldTypesLoaded)
                return;

            // load, cache, index the FieldTypes
            Map<Long, FieldType> fieldTypesById = new HashMap<Long, FieldType>();
            Map<String, FieldType> fieldTypesByName = new HashMap<String, FieldType>();
            Collection<FieldTypeImpl> fieldTypes = schemaStrategy.getAllFieldTypes(systemUser);
            for (FieldTypeImpl fieldType : fieldTypes) {
                fieldType.makeReadOnly();
                fieldTypesById.put(new Long(fieldType.getId()), fieldType);
                fieldTypesByName.put(fieldType.getName(), fieldType);
            }
            this.fieldTypesById = fieldTypesById;
            this.fieldTypesByName = fieldTypesByName;
            this.fieldTypes = fieldTypes.toArray(new FieldTypeImpl[0]);
            this.fieldTypesLoaded = true;
        } finally {
            fieldTypesLock.unlock();
        }
    }

    private void assureDocumentTypesLoaded() throws RepositoryException {
        if (documentTypesLoaded)
            return;

        assurePartTypesLoaded();
        assureFieldTypesLoaded();

        try {
            documentTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (documentTypesLoaded)
                return;

            // load, cache, index the DocumentTypes
            Map<Long, DocumentType> documentTypesById = new HashMap<Long, DocumentType>();
            Map<String, DocumentType> documentTypesByName = new HashMap<String, DocumentType>();
            Collection<DocumentTypeImpl> documentTypes = schemaStrategy.getAllDocumentTypes(systemUser);
            for (DocumentTypeImpl documentType : documentTypes) {
                documentType.makeReadOnly();
                documentTypesById.put(new Long(documentType.getId()), documentType);
                documentTypesByName.put(documentType.getName(), documentType);
            }
            this.documentTypesById = documentTypesById;
            this.documentTypesByName = documentTypesByName;
            this.documentTypes = documentTypes.toArray(new DocumentTypeImpl[0]);
            this.documentTypesLoaded = true;
        } finally {
            documentTypesLock.unlock();
        }
    }

    public FieldTypeImpl getFieldTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        assureFieldTypesLoaded();
        FieldTypeImpl fieldType = (FieldTypeImpl)fieldTypesById.get(new Long(id));
        if (fieldType == null)
            throw new FieldTypeNotFoundException(id);
        return fieldType;
    }

    public DocumentTypeImpl getDocumentTypeById(long id, AuthenticatedUser user) throws RepositoryException {
        assureDocumentTypesLoaded();
        DocumentTypeImpl documentType = (DocumentTypeImpl)documentTypesById.get(new Long(id));
        if (documentType == null)
            throw new DocumentTypeNotFoundException(id);
        return documentType;
    }

    public FieldTypeImpl getFieldTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        assureFieldTypesLoaded();
        FieldTypeImpl fieldType = (FieldTypeImpl)fieldTypesByName.get(name);
        if (fieldType == null)
            throw new FieldTypeNotFoundException(name);
        return fieldType;
    }

    public PartType getPartTypeById(long id) throws RepositoryException {
        assurePartTypesLoaded();
        PartType partType = partTypesById.get(new Long(id));
        if (partType == null)
            throw new PartTypeNotFoundException(id);
        return partType;
    }

    public PartTypes getAllPartTypes() throws RepositoryException {
        assurePartTypesLoaded();
        return partTypes;
    }

    public PartType getPartTypeByName(String name) throws RepositoryException {
        assurePartTypesLoaded();
        PartType partType = partTypesByName.get(name);
        if (partType == null)
            throw new PartTypeNotFoundException(name);
        return partType;
    }

    public FieldTypeImpl[] getAllFieldTypes(AuthenticatedUser user) throws RepositoryException {
        assureFieldTypesLoaded();
        return this.fieldTypes;
    }

    public DocumentTypeImpl[] getAllDocumentTypes(AuthenticatedUser user) throws RepositoryException {
        assureDocumentTypesLoaded();
        return this.documentTypes;
    }

    public DocumentTypeImpl getDocumentTypeByName(String name, AuthenticatedUser user) throws RepositoryException {
        assureDocumentTypesLoaded();
        DocumentTypeImpl documentType = (DocumentTypeImpl)documentTypesByName.get(name);
        if (documentType == null)
            throw new DocumentTypeNotFoundException(name);
        return documentType;
    }

    public void modelChange(RepositorySchemaEventType type, long id, long updateCount) {
        if (type == RepositorySchemaEventType.DOCUMENT_TYPE_CREATED)
            documentTypeCreated(id);
        else if (type == RepositorySchemaEventType.DOCUMENT_TYPE_UPDATED)
            documentTypeUpdated(id, updateCount);
        else if (type == RepositorySchemaEventType.DOCUMENT_TYPE_DELETED)
            documentTypeDeleted(id);
        else if (type == RepositorySchemaEventType.FIELD_TYPE_CREATED)
            fieldTypeCreated(id);
        else if (type == RepositorySchemaEventType.FIELD_TYPE_UPDATED)
            fieldTypeUpdated(id, updateCount);
        else if (type == RepositorySchemaEventType.FIELD_TYPE_DELETED)
            fieldTypeDeleted(id);
        else if (type == RepositorySchemaEventType.PART_TYPE_CREATED)
            partTypeCreated(id);
        else if (type == RepositorySchemaEventType.PART_TYPE_UPDATED)
            partTypeUpdated(id, updateCount);
        else if (type == RepositorySchemaEventType.PART_TYPE_DELETED)
            partTypeDeleted(id);
        else
            throw new RuntimeException("Unsupported ChangEventType: " + type);
    }

    public void partTypeUpdated(long id, long updateCount) {
        if (!partTypesLoaded)
            return;

        try {
            partTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            PartType currentPartType = partTypesById.get(new Long(id));
            if (currentPartType != null && currentPartType.getUpdateCount() == updateCount)
                return;

            this.partTypesLoaded = false;
            this.documentTypesLoaded = false;
        } finally {
            partTypesLock.unlock();
        }
    }

    private void partTypeDeleted(long id) {
        if (!partTypesLoaded)
            return;

        try {
            partTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            PartType currentPartType = partTypesById.get(new Long(id));
            if (currentPartType == null)
                return;

            this.partTypesLoaded = false;
        } finally {
            partTypesLock.unlock();
        }
    }

    public void partTypeCreated(long id) {
        if (!partTypesLoaded)
            return;

        try {
            partTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (partTypesById.containsKey(new Long(id)))
                return;

            this.partTypesLoaded = false;

            // the newly created PartType can't possibly be in use by a DocumentType yet, so
            // no need to refresh those.
        } finally {
            partTypesLock.unlock();
        }
    }

    public void documentTypeUpdated(long id, long updateCount) {
        if (!documentTypesLoaded)
            return;

        try {
            documentTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            // check for duplicate event
            DocumentType currentDocumentType = documentTypesById.get(new Long(id));
            if (currentDocumentType != null && currentDocumentType.getUpdateCount() == updateCount)
                return;

            this.documentTypesLoaded = false;
        } finally {
            documentTypesLock.unlock();
        }
    }

    private void documentTypeDeleted(long id) {
        if (!documentTypesLoaded)
            return;

        try {
            documentTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            DocumentType currentDocumentType = documentTypesById.get(new Long(id));
            if (currentDocumentType == null)
                return;

            this.documentTypesLoaded = false;
        } finally {
            documentTypesLock.unlock();
        }
    }

    public void documentTypeCreated(long id) {
        if (!documentTypesLoaded)
            return;

        try {
            documentTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (documentTypesById.containsKey(new Long(id)))
                return;

            this.documentTypesLoaded = false;
        } finally {
            documentTypesLock.unlock();
        }
    }

    public void fieldTypeUpdated(long id, long updateCount) {
        if (!fieldTypesLoaded)
            return;

        try {
            fieldTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            FieldType currentFieldType = fieldTypesById.get(new Long(id));
            if (currentFieldType != null && currentFieldType.getUpdateCount() == updateCount)
                return;

            this.fieldTypesLoaded = false;
            this.documentTypesLoaded = false;
        } finally {
            fieldTypesLock.unlock();
        }
    }

    private void fieldTypeDeleted(long id) {
        if (!fieldTypesLoaded)
            return;

        try {
            fieldTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            FieldType currentFieldType = fieldTypesById.get(new Long(id));
            if (currentFieldType == null)
                return;

            this.fieldTypesLoaded = false;
        } finally {
            fieldTypesLock.unlock();
        }
    }

    public void fieldTypeCreated(long id) {
        if (!fieldTypesLoaded)
            return;

        try {
            fieldTypesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (fieldTypesById.containsKey(new Long(id)))
                return;

            this.fieldTypesLoaded = false;

            // the newly created FieldType can't possibly be in use by a DocumentType yet, so
            // no need to refresh those.
        } finally {
            fieldTypesLock.unlock();
        }
    }
}
