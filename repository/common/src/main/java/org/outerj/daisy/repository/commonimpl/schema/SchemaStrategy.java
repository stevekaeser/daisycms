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

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LinkExtractorInfos;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerx.daisy.x10.ExpSelectionListDocument;

import java.util.Collection;
import java.util.Locale;

/**
 * Allows to customise the behaviour of the abstract implementation classes of the
 * repository schema API.
 *
 * <p>For (important) general information about this and other strategy interfaces, see also
 * {@link org.outerj.daisy.repository.commonimpl.DocumentStrategy}.
 */
public interface SchemaStrategy {
    public void store(FieldTypeImpl fieldType) throws RepositoryException;

    public void deleteFieldType(long fieldTypeId, AuthenticatedUser user) throws RepositoryException;

    public void store(DocumentTypeImpl documentType) throws RepositoryException;

    public void deleteDocumentType(long documentTypeId, AuthenticatedUser user) throws RepositoryException;

    public void store(PartTypeImpl partType) throws RepositoryException;

    public void deletePartType(long partTypeId, AuthenticatedUser user) throws RepositoryException;

    public Collection<DocumentTypeImpl> getAllDocumentTypes(AuthenticatedUser user) throws RepositoryException;

    public Collection<FieldTypeImpl> getAllFieldTypes(AuthenticatedUser user) throws RepositoryException;

    public Collection<PartTypeImpl> getAllPartTypes(AuthenticatedUser user) throws RepositoryException;

    public PartTypeImpl getPartTypeById(long id, AuthenticatedUser user) throws RepositoryException;

    public PartTypeImpl getPartTypeByName(String name, AuthenticatedUser user) throws RepositoryException;

    public FieldTypeImpl getFieldTypeById(long id, AuthenticatedUser user) throws RepositoryException;

    public FieldTypeImpl getFieldTypeByName(String name, AuthenticatedUser user) throws RepositoryException;

    public DocumentTypeImpl getDocumentTypeById(long id, AuthenticatedUser user) throws RepositoryException;

    public DocumentTypeImpl getDocumentTypeByName(String name, AuthenticatedUser user) throws RepositoryException;

    public LinkExtractorInfos getLinkExtractors(AuthenticatedUser user) throws RepositoryException;

    /**
     * Optional, can return null.
     */
    public ExpSelectionListDocument getExpandedSelectionListData(long fieldTypeId, long branchId, long languageId, Locale locale, AuthenticatedUser user) throws RepositoryException;
}
