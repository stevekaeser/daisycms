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
package org.outerj.daisy.repository.schema;

import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LinkExtractorInfos;

/**
 * Allows querying and manipulation of the Repository Schema.
 *
 * <p>The RepositorySchema can be retrieved via {@link org.outerj.daisy.repository.Repository#getRepositorySchema()}.
 *
 * <p>The Repository Schema defines the types of documents that can be stored
 * in the repository. See {@link DocumentType} for more information about what
 * constitutes a Document Type.
 *
 * <p>The various get methods all take a parameter "updateable". If true,
 * the returned object can be modified and saved, and is caller-specific.
 * If false, the returned object is not updateable (thus immutable), and the same object
 * instance can be returned to different callers (i.e. it is threadsafe). The
 * returned objects can in that case be retrieved from a cache, allowing very fast
 * access to the schema information. So in general, if you don't need to modify
 * the schema information, supply <tt>false</tt> for the updateable parameter.
 */
public interface RepositorySchema {
    /**
     * @deprecated Use createDocumentType instead.
     */
    DocumentType createNewDocumentType(String name);

    /**
     * Creates a new document type with the given name. The document type is
     * not created immediately in the repository, to do this you need to call
     * the save() method on the returned object.
     */
    DocumentType createDocumentType(String name);

    void deleteDocumentType(long documentTypeId) throws RepositoryException;

    /**
     * @deprecated Use createFieldType instead.
     */
    FieldType createNewFieldType(String name, ValueType valueType);

    FieldType createFieldType(String name, ValueType valueType);

    FieldType createFieldType(String name, ValueType valueType, boolean multiValue);

    FieldType createFieldType(String name, ValueType valueType, boolean multiValue, boolean hierarchical);

    void deleteFieldType(long fieldTypeId) throws RepositoryException;

    /**
     * @deprecated Use createPartType instead.
     */
    PartType createNewPartType(String name, String mimeTypes);

    PartType createPartType(String name, String mimeTypes);

    void deletePartType(long partTypeId) throws RepositoryException;

    void addListener(RepositorySchemaListener listener);

    void removeListener(RepositorySchemaListener listener);

    DocumentTypes getAllDocumentTypes(boolean updateable) throws RepositoryException;

    FieldTypes getAllFieldTypes(boolean updateable) throws RepositoryException;

    PartTypes getAllPartTypes(boolean updateable) throws RepositoryException;

    /**
     * @throws PartTypeNotFoundException in case the part type does not exist.
     */
    PartType getPartTypeById(long id, boolean updateable) throws RepositoryException;

    /**
     * @throws PartTypeNotFoundException in case the part type does not exist.
     */
    PartType getPartTypeByName(String name, boolean updateable) throws RepositoryException;

    /**
     * @throws PartTypeNotFoundException in case the part type does not exist.
     */
    PartType getPartType(String nameOrId, boolean updateable) throws RepositoryException;

    /**
     * @throws FieldTypeNotFoundException in case the field type does not exist.
     */
    FieldType getFieldTypeById(long id, boolean updateable) throws RepositoryException;

    /**
     * @throws FieldTypeNotFoundException in case the field type does not exist.
     */
    FieldType getFieldTypeByName(String name, boolean updateable) throws RepositoryException;

    FieldType getFieldType(String nameOrId, boolean updateable) throws RepositoryException;

    /**
     * @throws DocumentTypeNotFoundException in case the document type does not exist.
     */
    DocumentType getDocumentTypeById(long id, boolean updateable) throws RepositoryException;

    /**
     * @throws DocumentTypeNotFoundException in case the document type does not exist.
     */
    DocumentType getDocumentTypeByName(String name, boolean updateable) throws RepositoryException;

    /**
     * @param nameOrId if this starts with a digit, will do getDocumentTypeById, otherwise ByName
     */
    DocumentType getDocumentType(String nameOrId, boolean updateable) throws RepositoryException;

    /**
     * Returns information about the available link extractors.
     */
    LinkExtractorInfos getLinkExtractors() throws RepositoryException;
}
