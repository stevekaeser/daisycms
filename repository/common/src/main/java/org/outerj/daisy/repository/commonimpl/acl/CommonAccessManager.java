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

import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.commonimpl.*;

/**
 * Provides support for classes providing
 * {@link org.outerj.daisy.repository.acl.AccessManager AccessManager} functionality.
 *
 * <p>Most methods in this class correspond to the methods in
 * {@link org.outerj.daisy.repository.acl.AccessManager AccessManager}, but take
 * an additonal User argument. So check the javadoc over there for explanations of
 * these methods.
 */
public class CommonAccessManager {
    private AclStrategy aclStrategy;

    public CommonAccessManager(AclStrategy aclStrategy) {
        this.aclStrategy = aclStrategy;
    }

    public Acl getLiveAcl(AuthenticatedUser user) throws RepositoryException {
        AclImpl acl = aclStrategy.loadAcl(AclStrategy.LIVE_ACL_ID, user);
        acl.makeReadOnly();
        return acl;
    }

    public Acl getStagingAcl(AuthenticatedUser user) throws RepositoryException {
        return aclStrategy.loadAcl(AclStrategy.STAGING_ACL_ID, user);
    }

    public void copyStagingToLive(AuthenticatedUser user) throws RepositoryException {
        aclStrategy.copyStagingToLive(user);
    }

    public void copyLiveToStaging(AuthenticatedUser user) throws RepositoryException {
        aclStrategy.copyLiveToStaging(user);
    }

    public AclResultInfo getAclInfoOnLive(AuthenticatedUser user, long userId, long[] roleIds, DocId docId, long branchId, long languageId) throws RepositoryException {
        return  aclStrategy.getAclInfo(user, AclStrategy.LIVE_ACL_ID, userId, roleIds, docId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnStaging(AuthenticatedUser user, long userId, long[] roleIds, DocId docId, long branchId, long languageId) throws RepositoryException {
        return aclStrategy.getAclInfo(user, AclStrategy.STAGING_ACL_ID, userId, roleIds, docId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnLive(AuthenticatedUser user, long userId, long[] roleIds, Document document) throws RepositoryException {
        return aclStrategy.getAclInfo(user, AclStrategy.LIVE_ACL_ID, userId, roleIds, document);
    }

    public AclResultInfo getAclInfoOnStaging(AuthenticatedUser user, long userId, long[] roleIds, Document document) throws RepositoryException {
        return aclStrategy.getAclInfo(user, AclStrategy.STAGING_ACL_ID, userId, roleIds, document);
    }

    public AclResultInfo getAclInfoOnLiveForConceptualDocument(AuthenticatedUser user, long userId, long[] roleIds,
            long documentTypeId, long branchId, long languageId) throws RepositoryException {
        return aclStrategy.getAclInfoForConceptualDocument(user, AclStrategy.LIVE_ACL_ID, userId, roleIds,
                documentTypeId, branchId, languageId);
    }

    public AclResultInfo getAclInfoOnStagingForConceptualDocument(AuthenticatedUser user, long userId, long[] roleIds,
            long documentTypeId, long branchId, long languageId) throws RepositoryException {
        return aclStrategy.getAclInfoForConceptualDocument(user, AclStrategy.STAGING_ACL_ID, userId, roleIds,
                documentTypeId, branchId, languageId);
    }

    public long[] filterDocumentTypes(AuthenticatedUser user, long[] documentTypeIds, long collectionId, long branchId,
            long languageId) throws RepositoryException {
        return aclStrategy.filterDocumentTypes(user, documentTypeIds, collectionId, branchId, languageId);
    }

    public VariantKey[] filterDocuments(AuthenticatedUser user, VariantKey[] variantKeys, AclPermission permision, boolean nonLive) throws RepositoryException {
        return aclStrategy.filterDocuments(user, variantKeys, permision, nonLive);
    }
}
