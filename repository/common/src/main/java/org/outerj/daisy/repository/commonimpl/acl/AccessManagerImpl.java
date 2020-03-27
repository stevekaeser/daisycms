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
package org.outerj.daisy.repository.commonimpl.acl;

import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;

public class AccessManagerImpl implements AccessManager {
    private CommonAccessManager delegate;
    private CommonRepository repository;
    private AuthenticatedUser user;

    public AccessManagerImpl(CommonAccessManager delegate, CommonRepository repository, AuthenticatedUser user) {
        this.delegate = delegate;
        this.repository = repository;
        this.user = user;
    }

    public Acl getLiveAcl() throws RepositoryException {
        return delegate.getLiveAcl(user);
    }

    public Acl getStagingAcl() throws RepositoryException {
        return delegate.getStagingAcl(user);
    }

    public void copyStagingToLive() throws RepositoryException {
        delegate.copyStagingToLive(user);
    }

    public void copyLiveToStaging() throws RepositoryException {
        delegate.copyLiveToStaging(user);
    }

    public AclResultInfo getAclInfo(Document document) throws RepositoryException {
        return delegate.getAclInfoOnLive(user, user.getId(), user.getActiveRoleIds(), document);
    }

    public AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, String documentId) throws RepositoryException {
        return getAclInfoOnLive(userId, roleIds, documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID);
    }

    public AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, String documentId) throws RepositoryException {
        return getAclInfoOnStaging(userId, roleIds, documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID);
    }

    public AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, String documentId, long branchId, long languageId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, repository);
        return delegate.getAclInfoOnLive(user, userId, roleIds, docId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, VariantKey key) throws RepositoryException {
        return getAclInfoOnLive(userId, roleIds, key.getDocumentId(), key.getBranchId(), key.getLanguageId());
    }

    public AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, String documentId, long branchId, long languageId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, repository);
        return delegate.getAclInfoOnStaging(user, userId, roleIds, docId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, VariantKey key) throws RepositoryException {
        return getAclInfoOnStaging(userId, roleIds, key.getDocumentId(), key.getBranchId(), key.getLanguageId());
    }

    public AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, Document document) throws RepositoryException {
        return delegate.getAclInfoOnLive(user, userId, roleIds, document);
    }

    public AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, Document document) throws RepositoryException {
        return delegate.getAclInfoOnStaging(user, userId, roleIds, document);
    }

    public AclResultInfo getAclInfoOnLiveForConceptualDocument(long userId, long[] roleIds, long documentTypeId,
            long branchId, long languageId) throws RepositoryException {
        return delegate.getAclInfoOnLiveForConceptualDocument(user, userId, roleIds, documentTypeId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnStagingForConceptualDocument(long userId, long[] roleIds, long documentTypeId,
            long branchId, long languageId) throws RepositoryException {
        return delegate.getAclInfoOnStagingForConceptualDocument(user, userId, roleIds, documentTypeId, branchId, languageId);
    }

    public long[] filterDocumentTypes(long[] documentTypeIds, long collectionId, long branchId, long languageId) throws RepositoryException {
        return delegate.filterDocumentTypes(user, documentTypeIds, collectionId, branchId, languageId);
    }

    public VariantKey[] filterDocuments(VariantKey[] variantKeys) throws RepositoryException {
        return delegate.filterDocuments(user, variantKeys, AclPermission.READ, false);
    }

    public VariantKey[] filterDocuments(VariantKey[] variantKeys, AclPermission permission, boolean nonLive) throws RepositoryException {
        return delegate.filterDocuments(user, variantKeys, permission, nonLive);
    }

    public VariantKey[] filterDocuments(VariantKey[] variantKeys, AclPermission permission) throws RepositoryException {
        return delegate.filterDocuments(user, variantKeys, permission, false);
    }
}
