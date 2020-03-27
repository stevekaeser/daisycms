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
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.comment.CommentManager;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.commonimpl.schema.RepositorySchemaImpl;
import org.outerj.daisy.repository.commonimpl.user.UserManagerImpl;
import org.outerj.daisy.repository.commonimpl.acl.AccessManagerImpl;
import org.outerj.daisy.repository.commonimpl.comment.CommentManagerImpl;
import org.outerj.daisy.repository.commonimpl.variant.VariantManagerImpl;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceManagerImpl;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.user.UserManager;
import org.outerx.daisy.x10.UserInfoDocument;

import java.io.InputStream;

public class RepositoryImpl implements Repository {
    private final CommonRepository delegate;
    protected final AuthenticatedUser user;
    private RepositorySchema repositorySchema = null;
    private AccessManager accessManager = null;
    private QueryManager queryManager = null;
    private CommentManager commentManager = null;
    private VariantManager variantManager = null;
    private NamespaceManager namespaceManager = null;

    public RepositoryImpl(CommonRepository delegate, AuthenticatedUser user) {
        this.delegate = delegate;
        this.user = user;
    }

    public String getNamespace() {
        return getNamespaceManager().getRepositoryNamespace();
    }

    public String getNamespace(Document document) throws RepositoryException {
        return getNamespaceManager().getRepositoryNamespace(document);
    }

    public RepositorySchema getRepositorySchema() {
        if (repositorySchema == null)
            this.repositorySchema = new RepositorySchemaImpl(delegate.getRepositorySchema(), user);
        return repositorySchema;
    }

    public AccessManager getAccessManager() {
        if (accessManager == null)
            this.accessManager = new AccessManagerImpl(delegate.getAccessManager(), delegate, user);
        return accessManager;
    }

    public QueryManager getQueryManager() {
        if (queryManager == null)
            this.queryManager = delegate.getQueryManager(user);
        return queryManager;
    }

    public CommentManager getCommentManager() {
        if (commentManager == null)
            this.commentManager = new CommentManagerImpl(delegate, user);
        return commentManager;
    }

    public VariantManager getVariantManager() {
        if (variantManager == null)
            this.variantManager = new VariantManagerImpl(delegate.getVariantManager(), user);
        return variantManager;
    }

    public NamespaceManager getNamespaceManager() {
        if (namespaceManager == null)
            this.namespaceManager = new NamespaceManagerImpl(delegate.getNamespaceManager(), user);
        return namespaceManager;
    }

    public Document createDocument(String name, long documentTypeId, long branchId, long languageId) {
        return delegate.createDocument(name, documentTypeId, branchId, languageId, user);
    }

    public Document createDocument(String name, String documentTypeName, String branchName, String languageName) {
        return delegate.createDocument(name, documentTypeName, branchName, languageName, user);
    }

    public Document createDocument(String name, long documentTypeId) {
        return createDocument(name, documentTypeId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID);
    }

    public Document createDocument(String name, String documentTypeName) {
        return createDocument(name, documentTypeName, Branch.MAIN_BRANCH_NAME, Language.DEFAULT_LANGUAGE_NAME);
    }

    public Document createVariant(String documentId, long startBranchId, long startLanguageId, long startVersionId, long newBranchId, long newLanguageId, boolean copyContent) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.createVariant(docId, startBranchId, startLanguageId, startVersionId, newBranchId, newLanguageId, copyContent, user);
    }

    public Document createVariant(String documentId, String startBranchName, String startLanguageName, long startVersionId, String newBranchName, String newLanguageName, boolean copyContent) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.createVariant(docId, startBranchName, startLanguageName, startVersionId, newBranchName, newLanguageName, copyContent, user);
    }

    public Document getDocument(String documentId, long branchId, long languageId, boolean updateable) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.getDocument(docId, branchId, languageId, updateable, user);
    }

    public Document getDocument(String documentId, String branchName, String languageName, boolean updateable) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.getDocument(docId, branchName, languageName, updateable, user);
    }

    public Document getDocument(VariantKey key, boolean updateable) throws RepositoryException {
        if (key == null)
            throw new IllegalArgumentException("VariantKey argument cannot be null.");
        return this.getDocument(key.getDocumentId(), key.getBranchId(), key.getLanguageId(), updateable);
    }

    public Document getDocument(long documentId, boolean updateable) throws RepositoryException {
        DocId docId = DocId.getDocId(documentId, delegate.getNamespaceManager().getRepositoryNamespace(), delegate);
        return delegate.getDocument(docId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, updateable, user);
    }

    public Document getDocument(String documentId, boolean updateable) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.getDocument(docId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, updateable, user);
    }

    public AvailableVariants getAvailableVariants(String documentId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.getAvailableVariants(docId, user);
    }

    public void deleteDocument(String documentId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        delegate.deleteDocument(docId, user);
    }

    public void deleteVariant(String documentId, long branchId, long languageId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        delegate.deleteVariant(docId, branchId, languageId, user);
    }

    public void deleteVariant(VariantKey variantKey) throws RepositoryException {
        this.deleteVariant(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId());
    }

    public InputStream getPartData(String documentId, long branchId, long languageId, long versionId, long partTypeId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, delegate);
        return delegate.getBlob(docId, branchId, languageId, versionId, partTypeId, user);
    }

    public InputStream getPartData(String documentId, long versionId, long partTypeId) throws RepositoryException {
        return getPartData(documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, versionId, partTypeId);
    }

    public CollectionManager getCollectionManager() {
        return new CollectionManagerImpl(delegate.getCollectionManager(), user);
    }

    public long getUserId() {
        return user.getId();
    }

    public long[] getActiveRoleIds() {
        return user.getActiveRoleIds();
    }

    public boolean isInRole(long roleId) {
        return user.isInRole(roleId);
    }

    public boolean isInRole(String roleName) {
        long roleId;
        try {
            roleId = getUserManager().getRole(roleName, false).getId();
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        return user.isInRole(roleId);
    }

    public String getUserDisplayName() {
        try {
            return getUserManager().getUserDisplayName(user.getId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserLogin() {
        return user.getLogin();
    }

    public String[] getActiveRolesDisplayNames() {
        try {
            UserManager userManager = getUserManager();
            long[] activeRoleIds = user.getActiveRoleIds();
            String[] roleNames = new String[activeRoleIds.length];
            for (int i = 0; i < activeRoleIds.length; i++)
                roleNames[i] = userManager.getRoleDisplayName(activeRoleIds[i]);
            return roleNames;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long[] getAvailableRoles() {
        return user.getAvailableRoleIds();
    }

    public void switchRole(long roleId) {
        user.setActiveRoleIds(new long[] { roleId });
    }

    public void setActiveRoleIds(long[] roleIds) {
        user.setActiveRoleIds(roleIds);
    }

    public UserInfoDocument getUserInfoAsXml() {
        return user.getXml();
    }

    // To enable subclasses to access the user
    protected AuthenticatedUser getUser() {
        return user;
    }

    protected CommonRepository getCommonRepository() {
        return delegate;
    }

    public UserManager getUserManager() {
        return new UserManagerImpl(delegate.getUserManager(), user);
    }

    public void addListener(RepositoryListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener should not be null.");

        delegate.addListener(listener);
    }

    public void removeListener(RepositoryListener listener) {
        if (listener == null)
            throw new NullPointerException("Listener should not be null.");

        delegate.removeListener(listener);
    }

    public Object getExtension(String name) {
        if (name == null)
            throw new NullPointerException("Extension name should not be null.");

        ExtensionProvider extensionProvider = delegate.getExtensionProvider(name);
        if (extensionProvider == null)
            throw new RuntimeException("Extension named \"" + name + "\" is not available.");

        return extensionProvider.createExtension(this);
    }

    public boolean hasExtension(String name) {
        ExtensionProvider extensionProvider = delegate.getExtensionProvider(name);
        return extensionProvider != null;
    }

    public Object clone() {
        return new RepositoryImpl(delegate, user);
    }

    public String getClientVersion() {
        return delegate.getClientVersion(user);
    }

    public String getServerVersion() {
        return delegate.getServerVersion(user);
    }

    public String normalizeDocumentId(String documentId) {
        return delegate.normalizeDocumentId(documentId);
    }

    public RepositoryManager getRepositoryManager() {
        return delegate.getRepositoryManager();
    }
}
