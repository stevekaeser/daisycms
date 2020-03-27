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
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LinkExtractorInfos;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;

public class RepositorySchemaImpl implements RepositorySchema {
    private final CommonRepositorySchema delegate;
    private final AuthenticatedUser user;

    public RepositorySchemaImpl(CommonRepositorySchema delegate, AuthenticatedUser user) {
        this.delegate = delegate;
        this.user = user;
    }

    public DocumentType createNewDocumentType(String name) {
        return createDocumentType(name);
    }

    public DocumentType createDocumentType(String name) {
        return delegate.createDocumentType(name, user);
    }

    public void deleteDocumentType(long documentTypeId) throws RepositoryException {
        delegate.deleteDocumentType(documentTypeId, user);
    }

    public FieldType createNewFieldType(String name, ValueType valueType) {
        return createFieldType(name, valueType);
    }

    public FieldType createFieldType(String name, ValueType valueType) {
        return createFieldType(name, valueType, false);
    }

    public FieldType createFieldType(String name, ValueType valueType, boolean multiValue) {
        return createFieldType(name, valueType, multiValue, false);
    }

    public FieldType createFieldType(String name, ValueType valueType, boolean multiValue, boolean hierarchical) {
        return delegate.createFieldType(name, valueType, multiValue, hierarchical, user);
    }

    public void deleteFieldType(long fieldTypeId) throws RepositoryException {
        delegate.deleteFieldType(fieldTypeId, user);
    }

    public PartType createNewPartType(String name, String mimeTypes) {
        return createPartType(name, mimeTypes);
    }

    public PartType createPartType(String name, String mimeTypes) {
        return delegate.createPartType(name, mimeTypes, user);
    }

    public void deletePartType(long partTypeId) throws RepositoryException {
        delegate.deletePartType(partTypeId, user);
    }

    public DocumentTypes getAllDocumentTypes(boolean updateable) throws RepositoryException {
        return delegate.getAllDocumentTypes(updateable, user);
    }

    public FieldTypes getAllFieldTypes(boolean updateable) throws RepositoryException {
        return delegate.getAllFieldTypes(updateable, user);
    }

    public PartTypes getAllPartTypes(boolean updateable) throws RepositoryException {
        return delegate.getAllPartTypes(updateable, user);
    }

    public PartType getPartTypeById(long id, boolean updateable) throws RepositoryException {
        return delegate.getPartTypeById(id, updateable, user);
    }

    public PartType getPartTypeByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getPartTypeByName(name, updateable, user);
    }

    public PartType getPartType(String nameOrId, boolean updateable) throws RepositoryException {
        return delegate.getPartType(nameOrId, updateable, user);
    }

    public FieldType getFieldTypeById(long id, boolean updateable) throws RepositoryException {
        return delegate.getFieldTypeById(id, updateable, user);
    }

    public FieldType getFieldTypeByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getFieldTypeByName(name, updateable, user);
    }

    public FieldType getFieldType(String nameOrId, boolean updateable) throws RepositoryException {
        return delegate.getFieldType(nameOrId, updateable, user);
    }

    public DocumentType getDocumentTypeById(long id, boolean updateable) throws RepositoryException {
        return delegate.getDocumentTypeById(id, updateable, user);
    }

    public DocumentType getDocumentTypeByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getDocumentTypeByName(name, updateable, user);
    }

    public DocumentType getDocumentType(String nameOrId, boolean updateable) throws RepositoryException {
        return delegate.getDocumentType(nameOrId, updateable, user);
    }

    public void addListener(RepositorySchemaListener listener) {
        delegate.addListener(listener);
    }

    public void removeListener(RepositorySchemaListener listener) {
        delegate.removeListener(listener);
    }

    public LinkExtractorInfos getLinkExtractors() throws RepositoryException {
        return delegate.getLinkExtractors(user);
    }
}
