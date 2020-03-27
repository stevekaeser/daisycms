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
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.repository.commonimpl.schema.SchemaStrategy;
import org.outerj.daisy.repository.commonimpl.user.CommonUserManager;
import org.outerj.daisy.repository.commonimpl.user.UserManagementStrategy;
import org.outerj.daisy.repository.commonimpl.user.UserCache;
import org.outerj.daisy.repository.commonimpl.acl.CommonAccessManager;
import org.outerj.daisy.repository.commonimpl.acl.AclStrategy;
import org.outerj.daisy.repository.commonimpl.comment.CommonCommentManager;
import org.outerj.daisy.repository.commonimpl.comment.CommentStrategy;
import org.outerj.daisy.repository.commonimpl.variant.CommonVariantManager;
import org.outerj.daisy.repository.commonimpl.variant.VariantStrategy;
import org.outerj.daisy.repository.commonimpl.variant.VariantCache;
import org.outerj.daisy.repository.commonimpl.namespace.CommonNamespaceManager;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceCache;
import org.outerj.daisy.util.Constants;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.io.InputStream;

public abstract class CommonRepository {
    private RepositoryManager repositoryManager;
    protected RepositoryStrategy repositoryStrategy;
    protected DocumentStrategy documentStrategy;
    private CommonCollectionManager collectionManager;
    private List<RepositoryListener> listeners = new ArrayList<RepositoryListener>();

    private CommonRepositorySchema repositorySchema;
    private CommonAccessManager commonAccessManager;
    private CommonUserManager userManager;
    private CommonCommentManager commentManager;
    private CommonVariantManager variantManager;
    private CommonNamespaceManager namespaceManager;
    private Map<String, ExtensionProvider> extensions;

    public CommonRepository(RepositoryManager repositoryManager, RepositoryStrategy repositoryStrategy,
            DocumentStrategy documentStrategy, SchemaStrategy schemaStrategy, AclStrategy aclStrategy,
            UserManagementStrategy userManagementStrategy, VariantStrategy variantStrategy,
            CollectionStrategy collectionStrategy, CommentStrategy commentStrategy,
            Map<String, ExtensionProvider> extensions, AuthenticatedUser cacheUser) {
        this.repositoryManager = repositoryManager;
        this.repositoryStrategy = repositoryStrategy;
        this.documentStrategy = documentStrategy;
        this.repositorySchema = new CommonRepositorySchema(schemaStrategy, this, cacheUser);

        CollectionCache collectionCache = new CollectionCache(collectionStrategy, cacheUser);
        addListener(collectionCache);
        this.collectionManager = new CommonCollectionManager(collectionStrategy, collectionCache);

        UserCache userCache = new UserCache(userManagementStrategy, cacheUser);
        addListener(userCache);
        this.userManager = new CommonUserManager(userManagementStrategy, userCache);

        VariantCache variantCache = new VariantCache(variantStrategy, cacheUser);
        addListener(variantCache);
        this.variantManager = new CommonVariantManager(variantStrategy, variantCache);        

        this.commonAccessManager = new CommonAccessManager(aclStrategy);
        this.commentManager = new CommonCommentManager(commentStrategy);

        NamespaceCache namespaceCache = new NamespaceCache(repositoryStrategy, cacheUser);
        addListener(namespaceCache);
        this.namespaceManager = new CommonNamespaceManager(namespaceCache, repositoryStrategy);

        this.extensions = extensions;
    }

    public CommonRepositorySchema getRepositorySchema() {
        return repositorySchema;
    }

    public CommonAccessManager getAccessManager() {
        return commonAccessManager;
    }

    public abstract QueryManager getQueryManager(AuthenticatedUser user);

    public CommonCommentManager getCommentManager() {
        return commentManager;
    }

    public CommonVariantManager getVariantManager() {
        return variantManager;
    }

    public DocumentImpl createDocument(String name, long documentTypeId, long branchId, long languageId, AuthenticatedUser user) {
        try {
            // check that the documenttype exists
            repositorySchema.getDocumentTypeById(documentTypeId, false, user);
            // check the branch exists
            variantManager.getBranch(branchId, false, user);
            // check the language exists
            variantManager.getLanguage(languageId, false, user);
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        DocumentImpl newDocument = new DocumentImpl(documentStrategy, this, user, documentTypeId, branchId, languageId);
        newDocument.setName(name);
        return newDocument;
    }

    public DocumentImpl createDocument(String name, String documentTypeName, String branchName, String languageName, AuthenticatedUser user) {
        long documentTypeId;
        long branchId;
        long languageId;
        try {
            documentTypeId = repositorySchema.getDocumentTypeByName(documentTypeName, false, user).getId();
            branchId = variantManager.getBranchByName(branchName, false, user).getId();
            languageId = variantManager.getLanguageByName(languageName, false, user).getId();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        DocumentImpl newDocument = new DocumentImpl(documentStrategy, this, user, documentTypeId, branchId, languageId);
        newDocument.setName(name);
        return newDocument;
    }

    public Document createVariant(DocId docId, long startBranchId, long startLanguageId, long startVersionId, long newBranchId, long newLanguageId, boolean copyContent, AuthenticatedUser user) throws RepositoryException {
        AvailableVariant[] availableVariants = getAvailableVariants(docId, user).getArray();
        for (AvailableVariant availableVariant : availableVariants) {
            if (availableVariant.getBranchId() == newBranchId && availableVariant.getLanguageId() == newLanguageId)
                throw new RepositoryException("Document " + docId.toString() + " already has the variant branch ID " + newBranchId + ", language ID " + newLanguageId);
        }

        // test that the branch and languages exist
        variantManager.getBranch(newBranchId, false, user);
        variantManager.getLanguage(newLanguageId, false, user);

        // check that the user has write access to the start variant (otherwise everyone would be able
        // to add new variants to whatever document, since they can modify the variant data to match the ACL rules)
        AclResultInfo aclResultInfo = getAccessManager().getAclInfoOnLive(user, user.getId(), user.getActiveRoleIds(), docId, startBranchId, startLanguageId);
        if (!aclResultInfo.isAllowed(AclPermission.READ)) {
            throw new AccessException("A new variant can only be added if you have write access to the start variant.");
        }

        if (copyContent) {
            Document document = documentStrategy.createVariant(docId, startBranchId, startLanguageId, startVersionId, newBranchId, newLanguageId, user);
            return document;
        } else {
            Document document = getDocument(docId, startBranchId, startLanguageId, false, user);
            DocumentImpl newDocument = new DocumentImpl(documentStrategy, this, user, document.getDocumentTypeId(), newBranchId, newLanguageId);
            newDocument.getIntimateAccess(documentStrategy).load(docId, document.getLastModified(), document.getLastModifier(), document.getCreated(), document.getOwner(), document.isPrivate(), document.getUpdateCount(), document.getReferenceLanguageId());
            newDocument.setName(document.getName());
            DocumentVariantImpl.IntimateAccess variantInt = newDocument.getIntimateAccess(documentStrategy).getVariant().getIntimateAccess(documentStrategy);
            variantInt.setStartFrom(startBranchId, startLanguageId);
            return newDocument;
        }
    }

    public Document createVariant(DocId docId, String startBranchName, String startLanguageName, long startVersionId, String newBranchName, String newLanguageName, boolean copyContent, AuthenticatedUser user) throws RepositoryException {
        long startBranchId = variantManager.getBranchByName(startBranchName, false, user).getId();
        long startLanguageId = variantManager.getLanguageByName(startLanguageName, false, user).getId();
        long newBranchId = variantManager.getBranchByName(newBranchName, false, user).getId();
        long newLanguageId = variantManager.getLanguageByName(newLanguageName, false, user).getId();
        return createVariant(docId, startBranchId, startLanguageId, startVersionId, newBranchId, newLanguageId, copyContent, user);
    }

    public Document getDocument(String documentId, long branchId, long languageId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, this);
        return getDocument(docId, branchId, languageId, updateable, user);
    }

    /**
     * Note: it is important that all actual document retrievals go through this method, as this method
     * is overrided in the local implementation to get objects from the cache.
     */
    public Document getDocument(DocId docId, long branchId, long languageId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return documentStrategy.load(docId, branchId, languageId, user);
        else {
            Document document = documentStrategy.load(docId, branchId, languageId, user);
            DocumentImpl documentImpl = ((DocumentWrapper)document).getWrappedDocument(documentStrategy);
            documentImpl.makeReadOnly();
            return document;
        }
    }

    public Document getDocument(DocId docId, String branchName, String languageName, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (branchName == null || branchName.length() == 0)
            throw new IllegalArgumentException("Null or empty branch name specified.");
        if (languageName == null || languageName.length() == 0)
            throw new IllegalArgumentException("Null or empty language name specified.");

        long branchId;
        if (Character.isDigit(branchName.charAt(0))) {
            try {
                branchId = Long.parseLong(branchName);
            } catch (NumberFormatException e) {
                throw new RepositoryException("Invalid branch name: " + branchName);
            }
        } else {
            branchId = variantManager.getBranchByName(branchName, false, user).getId();
        }

        long languageId;
        if (Character.isDigit(languageName.charAt(0))) {
            try {
                languageId = Long.parseLong(languageName);
            } catch (NumberFormatException e) {
                throw new RepositoryException("Invalid language name: " + languageName);
            }
        } else {
            languageId = variantManager.getLanguageByName(languageName, false, user).getId();
        }
        
        return getDocument(docId, branchId, languageId, updateable, user);
    }

    /**
     * Note: it is important that all actual retrievals of AvailableVariants go through this method, as this method
     * is overrided in the local implementation to get objects from the cache.
     */
    public AvailableVariants getAvailableVariants(DocId docId, AuthenticatedUser user) throws RepositoryException {
        return new AvailableVariantsImpl(documentStrategy.getAvailableVariants(docId, user));
    }

    public void deleteDocument(DocId docId, AuthenticatedUser user) throws RepositoryException {
        documentStrategy.deleteDocument(docId, user);
    }

    public void deleteVariant(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        documentStrategy.deleteVariant(docId, branchId, languageId, user);
    }

    public InputStream getBlob(DocId docId, long branchId, long languageId, long versionId, long partTypeId, AuthenticatedUser user) throws RepositoryException {
        return documentStrategy.getBlob(docId, branchId, languageId, versionId, partTypeId, user);
    }

    public CommonCollectionManager getCollectionManager() {
         return collectionManager;
    }

    public void removeListener(RepositoryListener listener) {
        listeners.remove(listener);
    }

    public void addListener(RepositoryListener listener) {
        listeners.add(listener);
    }

    public void fireRepositoryEvent(RepositoryEventType type, Object id, long updateCount) {
        for (RepositoryListener listener : listeners) {
            listener.repositoryEvent(type, id, updateCount);
        }
    }

    public ExtensionProvider getExtensionProvider(String extensionName) {
        return extensions.get(extensionName);
    }

    public CommonUserManager getUserManager() {
        return userManager;
    }

    public CommonNamespaceManager getNamespaceManager() {
        return namespaceManager;
    }

    public String getClientVersion(AuthenticatedUser user) {
        return repositoryStrategy.getClientVersion(user);
    }

    public String getServerVersion(AuthenticatedUser user) {
        return repositoryStrategy.getServerVersion(user);
    }

    public String normalizeDocumentId(String documentId) {
        if (documentId == null)
            throw new IllegalArgumentException("documentId argument is not allowed to be null");
        Matcher matcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(documentId);
        if (matcher.matches()) {
            String namespace = matcher.group(2);
            if (namespace == null) {
                String seqId = matcher.group(1);
                namespace = getNamespaceManager().getRepositoryNamespace();
                return seqId + "-" + namespace;
            } else {
                return documentId;
            }
        } else {
            throw new InvalidDocumentIdException("Invalid document ID \"" + documentId + "\".");
        }
    }

    public RepositoryManager getRepositoryManager() {
        return repositoryManager;
    }
}
