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

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;

/**
 * Allows to customise the behaviour of the abstract implementation classes of the
 * repository ACL API.
 *
 * <p>For (important) general information about this and other strategy interfaces, see also
 * {@link org.outerj.daisy.repository.commonimpl.DocumentStrategy}.
 */
public interface AclStrategy {
    public static final long LIVE_ACL_ID = 1;
    public static final long STAGING_ACL_ID = 2;

    public AclImpl  loadAcl(long id, AuthenticatedUser user) throws RepositoryException;

    public void storeAcl(AclImpl acl) throws RepositoryException;

    public void copyStagingToLive(AuthenticatedUser user) throws RepositoryException;

    public void copyLiveToStaging(AuthenticatedUser user) throws RepositoryException;

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, Document document)
            throws RepositoryException;

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, DocId docId,
            long branchId, long languageId) throws RepositoryException;

    public AclResultInfo getAclInfoForConceptualDocument(AuthenticatedUser user, long id, long userId, long[] roleIds,
            long documentTypeId, long branchId, long languageId) throws RepositoryException;

    public long[] filterDocumentTypes(AuthenticatedUser user, long[] documentTypeIds, long collectionId,
            long branchId, long languageId) throws RepositoryException;

    public VariantKey[] filterDocuments(AuthenticatedUser user, VariantKey[] variantKeys, AclPermission permission,
            boolean nonLive) throws RepositoryException;
}
