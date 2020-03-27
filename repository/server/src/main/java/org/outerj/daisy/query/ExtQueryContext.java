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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartType;

public class ExtQueryContext implements QueryContext {
    private final Repository repository;

    public ExtQueryContext(Repository repository) {
        this.repository = repository;
    }

    public Document getDocument(VariantKey variantKey) throws RepositoryException {
        return repository.getDocument(variantKey, false);
    }

    public FieldType getFieldTypeByName(String name) throws RepositoryException {
        return repository.getRepositorySchema().getFieldTypeByName(name, false);
    }

    public PartType getPartTypeByName(String name) throws RepositoryException {
        return repository.getRepositorySchema().getPartTypeByName(name, false);
    }

    public DocumentType getDocumentTypeByName(String name) throws RepositoryException {
        return repository.getRepositorySchema().getDocumentTypeByName(name, false);
    }

    public DocumentCollection getCollection(String name) throws RepositoryException {
        return repository.getCollectionManager().getCollection(name, false);
    }

    public DocumentType getDocumentTypeById(long id) throws RepositoryException {
        return repository.getRepositorySchema().getDocumentTypeById(id, false);
    }

    public String getUserDisplayName(long userId) throws RepositoryException {
        return repository.getUserManager().getUserDisplayName(userId);
    }

    public long getUserId(String login) throws RepositoryException {
        return repository.getUserManager().getUserId(login);
    }

    public long getUserId() {
        return repository.getUserId();
    }

    public String getUserLogin(long userId) throws RepositoryException {
        return repository.getUserManager().getUserLogin(userId);
    }

    public Branch getBranchByName(String name) throws RepositoryException {
        return repository.getVariantManager().getBranch(name, false);
    }

    public Language getLanguageByName(String name) throws RepositoryException {
        return repository.getVariantManager().getLanguage(name, false);
    }

    public Branch getBranch(long branchId) throws RepositoryException {
        return repository.getVariantManager().getBranch(branchId, false);
    }

    public Branch getBranch(String branchIdOrName) throws RepositoryException {
        return repository.getVariantManager().getBranch(branchIdOrName, false);
    }

    public Language getLanguage(long languageId) throws RepositoryException {
        return repository.getVariantManager().getLanguage(languageId, false);
    }

    public Language getLanguage(String languageIdOrName) throws RepositoryException {
        return repository.getVariantManager().getLanguage(languageIdOrName, false);
    }

    public DocId parseDocId(String documentId) {
        return DocId.parseDocId(documentId, repository);
    }

    public Namespace getNamespace(String namespaceName) throws NamespaceNotFoundException {
        return repository.getNamespaceManager().getNamespace(namespaceName);
    }

    public String getRepositoryNamespace() {
        return repository.getNamespace();
    }
}
