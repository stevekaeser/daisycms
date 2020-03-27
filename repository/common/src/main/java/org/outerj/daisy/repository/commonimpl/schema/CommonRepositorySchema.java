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
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LinkExtractorInfos;
import org.outerj.daisy.repository.Repository;

import java.util.List;
import java.util.ArrayList;

public class CommonRepositorySchema {
    private CommonRepository repository;
    private SchemaStrategy schemaStrategy;
    private List<RepositorySchemaListener> changeListeners = new ArrayList<RepositorySchemaListener>();
    private RepositorySchemaCache cache;

    public CommonRepositorySchema(SchemaStrategy schemaStrategy, CommonRepository repository, AuthenticatedUser systemUser) {
        this.schemaStrategy = schemaStrategy;
        this.cache = new RepositorySchemaCache(schemaStrategy, systemUser);
        this.repository = repository;
        addListener(cache);
    }

    public RepositorySchemaListener getCacheListener() {
        return cache;
    }

    public RepositorySchemaCache getCache() {
        return cache;
    }

    public DocumentType createDocumentType(String name, AuthenticatedUser user) {
        return new DocumentTypeImpl(name, schemaStrategy, repository, user);
    }

    public void deleteDocumentType(long documentTypeId, AuthenticatedUser user) throws RepositoryException {
        schemaStrategy.deleteDocumentType(documentTypeId, user);
    }

    public FieldType createFieldType(String name, ValueType valueType, boolean multiValue, boolean hierarchical, AuthenticatedUser user) {
        return wrap(new FieldTypeImpl(name, valueType, multiValue, hierarchical, schemaStrategy, user), user, new RepositoryImpl(this.repository, user));
    }

    public void deleteFieldType(long fieldTypeId, AuthenticatedUser user) throws RepositoryException {
        schemaStrategy.deleteFieldType(fieldTypeId, user);
    }

    public PartType createPartType(String name, String mimeTypes, AuthenticatedUser user) {
        return new PartTypeImpl(name, mimeTypes, schemaStrategy, user);
    }

    public void deletePartType(long partTypeId, AuthenticatedUser user) throws RepositoryException {
        schemaStrategy.deletePartType(partTypeId, user);
    }

    public DocumentTypes getAllDocumentTypes(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        DocumentTypeImpl[] documentTypes;
        if (updateable)
            documentTypes = schemaStrategy.getAllDocumentTypes(user).toArray(new DocumentTypeImpl[0]);
        else
            documentTypes = cache.getAllDocumentTypes(user);

        DocumentType[] wrappedDocumentTypes = new DocumentType[documentTypes.length];
        for (int i = 0 ; i < documentTypes.length; i++) {
            wrappedDocumentTypes[i] = wrap(documentTypes[i], user);
        }
        return new DocumentTypesImpl(wrappedDocumentTypes);
    }

    public FieldTypes getAllFieldTypes(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        FieldTypeImpl[] fieldTypes;
        if (updateable)
            fieldTypes = schemaStrategy.getAllFieldTypes(user).toArray(new FieldTypeImpl[0]);
        else
            fieldTypes = cache.getAllFieldTypes(user);

        FieldType[] wrappedFieldTypes = new FieldType[fieldTypes.length];
        Repository repository = new RepositoryImpl(this.repository, user);
        for (int i = 0 ; i < fieldTypes.length; i++) {
            wrappedFieldTypes[i] = wrap(fieldTypes[i], user, repository);
        }
        return new FieldTypesImpl(wrappedFieldTypes);
    }

    public PartTypes getAllPartTypes(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return new PartTypesImpl(schemaStrategy.getAllPartTypes(user).toArray(new PartTypeImpl[0]));
        else
            return cache.getAllPartTypes();
    }

    public PartType getPartTypeById(long id, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return schemaStrategy.getPartTypeById(id, user);
        else
            return cache.getPartTypeById(id);
    }

    public PartType getPartTypeByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name: null or empty");

        if (updateable)
            return schemaStrategy.getPartTypeByName(name, user);
        else
            return cache.getPartTypeByName(name);
    }

    public PartType getPartType(String nameOrId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (nameOrId == null || nameOrId.length() == 0)
            throw new IllegalArgumentException("nameOrId: null or empty");

        if (Character.isDigit(nameOrId.charAt(0))) {
            try {
                long id = Long.parseLong(nameOrId);
                return getPartTypeById(id, updateable, user);
            } catch (NumberFormatException e) {
                throw new RepositoryException("Invalid part type name or ID: " + nameOrId);
            }
        }
        return getPartTypeByName(nameOrId, updateable, user);
    }

    public FieldType getFieldTypeById(long id, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        FieldTypeImpl fieldTypeImpl;
        if (updateable)
            fieldTypeImpl = schemaStrategy.getFieldTypeById(id, user);
        else
            fieldTypeImpl = cache.getFieldTypeById(id, user);
        return wrap(fieldTypeImpl, user, new RepositoryImpl(repository, user));
    }

    private FieldType wrap(FieldTypeImpl fieldTypeImpl, AuthenticatedUser user, Repository repository) {
        return new FieldTypeWrapper(fieldTypeImpl, user, schemaStrategy, repository);
    }

    public FieldType getFieldTypeByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name: null or empty");

        FieldTypeImpl fieldTypeImpl;
        if (updateable)
            fieldTypeImpl = schemaStrategy.getFieldTypeByName(name, user);
        else
            fieldTypeImpl = cache.getFieldTypeByName(name, user);

        return wrap(fieldTypeImpl, user, new RepositoryImpl(repository, user));
    }

    public FieldType getFieldType(String nameOrId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (nameOrId == null || nameOrId.length() == 0)
            throw new IllegalArgumentException("nameOrId: null or empty");

        if (Character.isDigit(nameOrId.charAt(0))) {
            try {
                long id = Long.parseLong(nameOrId);
                return getFieldTypeById(id, updateable, user);
            } catch (NumberFormatException e) {
                throw new RepositoryException("Invalid field type name or ID: " + nameOrId);
            }
        }
        return getFieldTypeByName(nameOrId, updateable, user);
    }

    public DocumentType getDocumentTypeById(long id, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        DocumentTypeImpl documentType;
        if (updateable)
            documentType = schemaStrategy.getDocumentTypeById(id, user);
        else
            documentType = cache.getDocumentTypeById(id, user);

        return wrap(documentType, user);
    }

    public DocumentType getDocumentTypeByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name: null or empty");

        DocumentTypeImpl documentType;
        if (updateable)
            documentType = schemaStrategy.getDocumentTypeByName(name, user);
        else
            documentType = cache.getDocumentTypeByName(name, user);

        return documentType.isReadOnly() ? wrap(documentType, user) : documentType;
    }

    public DocumentType getDocumentType(String nameOrId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (nameOrId == null || nameOrId.length() == 0)
            throw new IllegalArgumentException("nameOrId: null or empty");

        if (Character.isDigit(nameOrId.charAt(0))) {
            try {
                long id = Long.parseLong(nameOrId);
                return getDocumentTypeById(id, updateable, user);
            } catch (NumberFormatException e) {
                throw new RepositoryException("Invalid document type name or ID: " + nameOrId);
            }
        }
        return getDocumentTypeByName(nameOrId, updateable, user);
    }

    private DocumentTypeImpl wrap(DocumentTypeImpl documentTypeImpl, AuthenticatedUser user) {
        return documentTypeImpl.isReadOnly() ? (DocumentTypeImpl)documentTypeImpl.clone(user) : documentTypeImpl;
    }

    public LinkExtractorInfos getLinkExtractors(AuthenticatedUser user) throws RepositoryException {
        return schemaStrategy.getLinkExtractors(user);
    }

    public void removeListener(RepositorySchemaListener listener) {
        changeListeners.remove(listener);
    }

    public void addListener(RepositorySchemaListener listener) {
        changeListeners.add(listener);
    }

    public void fireSchemaEvent(RepositorySchemaEventType type, long id, long updateCount) {
        for (RepositorySchemaListener changeListener : changeListeners) {
            changeListener.modelChange(type, id, updateCount);
        }
    }

    protected SchemaStrategy getSchemaStrategy() {
        return schemaStrategy;
    }
}
