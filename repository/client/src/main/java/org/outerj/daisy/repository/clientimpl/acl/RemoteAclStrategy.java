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
package org.outerj.daisy.repository.clientimpl.acl;

import org.outerj.daisy.repository.commonimpl.acl.AclStrategy;
import org.outerj.daisy.repository.commonimpl.acl.AclImpl;
import org.outerj.daisy.repository.commonimpl.acl.AclResultInfoImpl;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VariantKeys;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.util.ListUtil;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerx.daisy.x10.*;

import java.util.ArrayList;
import java.util.List;

public class RemoteAclStrategy extends AbstractRemoteStrategy implements AclStrategy {
    public RemoteAclStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public AclImpl loadAcl(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        aclIdCheck(id);
        HttpMethod method = new GetMethod("/repository/acl/" + getAclName(id));

        AclDocument aclDocument = (AclDocument)httpClient.executeMethod(method, AclDocument.class, true);
        AclDocument.Acl aclXml = aclDocument.getAcl();
        AclImpl document = new AclImpl(this, aclXml.getLastModified().getTime(), aclXml.getLastModifier(), aclXml.getId(), user, aclXml.getUpdateCount());
        document.setFromXml(aclXml);
        return document;
    }

    private void aclIdCheck(long id) throws RepositoryException {
        if (id != 1 && id != 2)
            throw new RepositoryException("Invalid ACL ID: " + id);
    }

    private String getAclName(long id) {
        if (id == 1)
            return "live";
        else if (id == 2)
            return "staging";
        else
            throw new RuntimeException("Unsupported ACL ID: " + id);
    }

    public void storeAcl(AclImpl acl) throws RepositoryException {
        AclImpl.IntimateAccess aclInt = acl.getIntimateAccess(this);
        DaisyHttpClient httpClient = getClient(aclInt.getCurrentModifier());
        PostMethod method = new PostMethod("/repository/acl/" + getAclName(aclInt.getId()));

        AclDocument aclDocument = acl.getXml();
        method.setRequestEntity(new InputStreamRequestEntity(aclDocument.newInputStream()));

        aclDocument = (AclDocument)httpClient.executeMethod(method, AclDocument.class, true);
        AclDocument.Acl aclXml = aclDocument.getAcl();
        aclInt.setLastModified(aclXml.getLastModified().getTime());
        aclInt.setLastModifier(aclXml.getLastModifier());
        aclInt.setUpdateCount(aclXml.getUpdateCount());
    }

    public void copyStagingToLive(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        HttpMethod method = new GetMethod("/repository/acl/staging?action=putLive");
        httpClient.executeMethod(method, null, true);
    }

    public void copyLiveToStaging(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        HttpMethod method = new GetMethod("/repository/acl/staging?action=revertChanges");
        httpClient.executeMethod(method, null, true);
    }

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, Document document) throws RepositoryException {
        throw new RuntimeException("This operation is not supported in the remote API implementation.");
    }

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, DocId docId, long branchId, long languageId) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        aclIdCheck(id);
        HttpMethod method = new GetMethod("/repository/acl/" + getAclName(id));
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("action", "evaluate"));
        parameters.add(new NameValuePair("documentId", docId.toString()));
        parameters.add(new NameValuePair("branch", String.valueOf(branchId)));
        parameters.add(new NameValuePair("language", String.valueOf(languageId)));
        parameters.add(new NameValuePair("user", String.valueOf(userId)));
        for (long roleId : roleIds)
            parameters.add(new NameValuePair("role", String.valueOf(roleId)));
        method.setQueryString(parameters.toArray(new NameValuePair[parameters.size()]));

        AclResultDocument aclResultDocument = (AclResultDocument)httpClient.executeMethod(method, AclResultDocument.class, true);
        AclResultDocument.AclResult aclResultXml = aclResultDocument.getAclResult();
        AclResultInfo aclResultInfo = new AclResultInfoImpl(aclResultXml.getUser().getId(), ListUtil.toArray(aclResultXml.getUser().getRoles().getRoleIdList()), aclResultXml.getDocumentId(), aclResultXml.getBranchId(), aclResultXml.getLanguageId());
        aclResultInfo.setFromXml(aclResultXml);
        return aclResultInfo;
    }

    public AclResultInfo getAclInfoForConceptualDocument(AuthenticatedUser user, long id, long userId, long[] roleIds,
            long documentTypeId, long branchId, long languageId) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        aclIdCheck(id);
        HttpMethod method = new GetMethod("/repository/acl/" + getAclName(id));
        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("action", "evaluateConceptual"));
        parameters.add(new NameValuePair("documentType", String.valueOf(documentTypeId)));
        parameters.add(new NameValuePair("branch", String.valueOf(branchId)));
        parameters.add(new NameValuePair("language", String.valueOf(languageId)));
        parameters.add(new NameValuePair("user", String.valueOf(userId)));
        for (long roleId : roleIds)
            parameters.add(new NameValuePair("role", String.valueOf(roleId)));
        method.setQueryString(parameters.toArray(new NameValuePair[parameters.size()]));

        AclResultDocument aclResultDocument = (AclResultDocument)httpClient.executeMethod(method, AclResultDocument.class, true);
        AclResultDocument.AclResult aclResultXml = aclResultDocument.getAclResult();
        AclResultInfo aclResultInfo = new AclResultInfoImpl(aclResultXml.getUser().getId(), ListUtil.toArray(aclResultXml.getUser().getRoles().getRoleIdList()), aclResultXml.getDocumentId(), aclResultXml.getBranchId(), aclResultXml.getLanguageId());
        aclResultInfo.setFromXml(aclResultXml);
        return aclResultInfo;
    }

    public long[] filterDocumentTypes(AuthenticatedUser user, long[] documentTypeIds, long collectionId, long branchId,
            long languageId) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/filterDocumentTypes");

        NameValuePair[] parameters = new NameValuePair[3];
        parameters[0] = new NameValuePair("collectionId", String.valueOf(collectionId));
        parameters[1] = new NameValuePair("branch", String.valueOf(branchId));
        parameters[2] = new NameValuePair("language", String.valueOf(languageId));
        method.setQueryString(parameters);

        IdsDocument idsDocument = IdsDocument.Factory.newInstance();
        idsDocument.addNewIds().setIdArray(documentTypeIds);
        method.setRequestEntity(new InputStreamRequestEntity(idsDocument.newInputStream()));

        idsDocument = (IdsDocument)httpClient.executeMethod(method, IdsDocument.class, true);
        return ListUtil.toArray(idsDocument.getIds().getIdList());
    }

    public VariantKey[] filterDocuments(AuthenticatedUser user, VariantKey[] variantKeys, AclPermission permission, boolean nonLive) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/filterDocuments");
        method.setQueryString(new NameValuePair[] {
                new NameValuePair("permission", permission.toString()),
                new NameValuePair("nonLive", String.valueOf(nonLive))
        });
        method.setRequestEntity(new InputStreamRequestEntity(new VariantKeys(variantKeys).getXml().newInputStream()));
        VariantKeysDocument variantKeysDocument = (VariantKeysDocument)httpClient.executeMethod(method, VariantKeysDocument.class, true);
        return VariantKeys.fromXml(variantKeysDocument).getArray();
    }
}
