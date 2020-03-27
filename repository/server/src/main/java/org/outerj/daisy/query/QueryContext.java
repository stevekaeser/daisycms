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
package org.outerj.daisy.query;

import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;

/**
 * Query context offers functionality to the query execution engine.
 * It doesn't offer access to the full repository as queries shouldn't
 * be allowed to have side-effects.
 */
public interface QueryContext {
    Document getDocument(VariantKey variantKey) throws RepositoryException;

    FieldType getFieldTypeByName(String name) throws RepositoryException;
    PartType getPartTypeByName(String name) throws RepositoryException;
    DocumentType getDocumentTypeById(long id) throws RepositoryException;
    DocumentType getDocumentTypeByName(String name) throws RepositoryException;
    DocumentCollection getCollection(String name) throws RepositoryException;
    String getUserDisplayName(long userId) throws RepositoryException;
    long getUserId(String login) throws RepositoryException;
    long getUserId();
    String getUserLogin(long userId) throws RepositoryException;

    Language getLanguageByName(String name) throws RepositoryException;
    Language getLanguage(long languageId) throws RepositoryException;
    Language getLanguage(String languageIdOrName) throws RepositoryException;

    Branch getBranchByName(String name) throws RepositoryException;
    Branch getBranch(long branchId) throws RepositoryException;
    Branch getBranch(String branchIdOrName) throws RepositoryException;

    DocId parseDocId(String documentId);
    Namespace getNamespace(String namespaceName) throws NamespaceNotFoundException;
    String getRepositoryNamespace();
}