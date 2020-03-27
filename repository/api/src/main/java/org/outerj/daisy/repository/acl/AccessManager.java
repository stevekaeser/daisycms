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
package org.outerj.daisy.repository.acl;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

/**
 * Provides functionality for maintaining the ACL (Access Control List) and
 * checking permissions.
 *
 * <p>The AccessManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getAccessManager()}.
 *
 * <p>See Daisy's documentation for background information on the ACL system.
 *
 * <p>Basically, instead of associating an ACL with each document in the
 * repository, there is one global ACL. Which ACL entries applies to which
 * documents is based on conditions selecting documents based on eg
 * their document type or collection membership. The structure of the
 * ACL is thus as follows:
 *
 * <pre>
 * object expression
 *    acl entry
 *    acl entry
 *    ...
 * object expression
 *    acl entry
 *    acl entry
 *    ...
 * ...
 * </pre>
 *
 * <p>wherin the "object expression" is the expression selecting a set
 * of documents. Each "acl entry" specifies for a certain subject
 * (user, role or everyone) the allowed action (deny/grant) for a
 * certain operation (read/write).
 *
 * <p>Two ACL's are managed: a staging ACL and a live ACL. Only
 * the staging ACL can be directly modified, the live ACL can
 * be updated by replacing it with the staging ACL.
 *
 * <p>About access to these functions: all users can read the ACL,
 * only the Administrator can save (modify) it. All users can retrieve
 * access information (ie using the getAclInfo* methods) for themselves,
 * the Administrator can retrieve this information for whatever user.
 */
public interface AccessManager {
    /**
     * Gets the currently active, live ACL. This ACL is not modifiable.
     * To make modifications to the ACL, first modify the staging ACL, and then
     * put the staging version live by callling {@link #copyLiveToStaging()}.
     */
    Acl getLiveAcl() throws RepositoryException;

    /**
     * Gets the staging ACL.
     */
    Acl getStagingAcl() throws RepositoryException;

    /**
     * Puts the staging ACL live.
     */
    void copyStagingToLive() throws RepositoryException;

    /**
     * Reverts changes to the staging ACL.
     */
    void copyLiveToStaging() throws RepositoryException;

    /**
     * Gets ACL info for the current user, by evaluating the (live) ACL rules
     * on the given document object.
     */
    AclResultInfo getAclInfo(Document document) throws RepositoryException;

    /**
     * Gets ACL info for the specified user acting in the specified role, for the specified
     * document variant, by evaluating the live ACL.
     */
    AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, String documentId, long branchId, long languageId) throws RepositoryException;

    /**
     * Gets ACL info for the specified user acting in the specified role, for the specified
     * document variant, by evaluating the live ACL.
     */
    AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, VariantKey key) throws RepositoryException;

    /**
     * Gets the ACL info for the branch "main" and language "default" of the document. This method
     * is mainly provided for backwards compatibility.
     */
    AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, String documentId) throws RepositoryException;

    /**
     * Gets ACL info for the specified user acting in the specified role, for the specified
     * document variant, by evaluating the staging ACL.
     */
    AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, String documentId, long branchId, long languageId) throws RepositoryException;

    /**
     * Gets ACL info for the specified user acting in the specified role, for the specified
     * document variant, by evaluating the staging ACL.
     */
    AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, VariantKey key) throws RepositoryException;

    /**
     * Gets the ACL info for the branch "main" and language "default" of the document. This method
     * is mainly provided for backwards compatibility.
     */
    AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, String documentId) throws RepositoryException;

    /**
     * Checks the ACL using the supplied document object. The current content of the
     * document is used during ACL evaluation, even if it includes unsaved changes.
     * This allows to check the ACL result before saving the document.
     *
     * <p>This method does not work in the remote API implementation.
     */
    AclResultInfo getAclInfoOnLive(long userId, long[] roleIds, Document document) throws RepositoryException;

    /**
     * Equivalent of {@link #getAclInfoOnLive(long, long[], org.outerj.daisy.repository.Document)}.
     */
    AclResultInfo getAclInfoOnStaging(long userId, long[] roleIds, Document document) throws RepositoryException;

    /**
     * Gets the ACL info for to-be-created documents of a certain type. This determines
     * whether one can create documents, and especially what the write access details for
     * new documents are.
     */
    AclResultInfo getAclInfoOnLiveForConceptualDocument(long userId, long[] roleIds, long documentTypeId,
            long branchId, long languageId) throws RepositoryException;

    AclResultInfo getAclInfoOnStagingForConceptualDocument(long userId, long[] roleIds, long documentTypeId,
            long branchId, long languageId) throws RepositoryException;

    /**
     * Filters the given list of document type ids to the ones for which the user
     * is potentially able to create new documents. This does not guarantee that the
     * user will be able to save a newly created document, as this could depend
     * on the values of document fields or the collections to which the document belongs.
     *
     * <p>The document types are filtered by two checks, corresponding to the two
     * checks that happen when saving a document:
     *
     * <ul>
     * <li>The user should have write access for the "conceptual" document,
     *     which determines the write access details. For this check the
     *     ACL can be evaluated exactly, as all necessary information is
     *     known.
     * <li>The user should have write access for the actual content being
     *     saved. Since we don't yet know what will be saved (and since
     *     the ACL evaluation can depend on document content), we can
     *     only do a best-effort check at this time.
     * </ul>
     *
     * <p>The collectionId parameter is optional (specify -1 to ignore) and allows
     * to specify the collection to which the document will be added, which allows
     * for a better filtered result.
     *
     */
    long[] filterDocumentTypes(long[] documentTypeIds, long collectionId, long branchId, long languageId) throws RepositoryException;

    /**
     * Filters the given list of document variants so that only document variants to which the
     * current user has the given ACL permission remains.
     * Non-existing documents/variants will also be excluded.
     *
     * <p>Especially in the remote API implementation, this is more efficient then
     * retrieving this information for individual documents, since it only requires
     * one backend HTTP call.
     *
     * @param nonLive set to true when read access to all versions of the document is required
     *                    (rather than just the live version).
     */
    VariantKey[] filterDocuments(VariantKey[] variantKeys, AclPermission permission, boolean nonLive) throws RepositoryException;

    /**
     * Filter documents assuming access to non-live versions is not required.
     */
    VariantKey[] filterDocuments(VariantKey[] variantKeys, AclPermission permission) throws RepositoryException;

    /**
     * Filters documents based on 'read' permission and without requiring access to all versions.
     * See also {@link #filterDocuments(org.outerj.daisy.repository.VariantKey[], AclPermission)}.
     */
    VariantKey[] filterDocuments(VariantKey[] variantKeys) throws RepositoryException;
}
