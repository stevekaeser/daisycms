/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow.clientimpl;

import org.outerj.daisy.workflow.commonimpl.WfPoolStrategy;
import org.outerj.daisy.workflow.commonimpl.WfPoolImpl;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WfListHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.NameValuePair;
import org.outerx.daisy.x10Workflow.PoolDocument;
import org.outerx.daisy.x10Workflow.PoolsDocument;
import org.outerx.daisy.x10Workflow.UsersDocument;

import java.util.List;
import java.util.ArrayList;

public class RemoteWfPoolStrategy implements WfPoolStrategy {
    public void store(WfPoolImpl pool, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        String url = "/workflow/pool";
        if (pool.getId() != -1)
            url = url + "/" + pool.getId();
        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new InputStreamRequestEntity(pool.getXml().newInputStream()));

        PoolDocument responseDocument = (PoolDocument)httpClient.executeMethod(method, PoolDocument.class, true);

        PoolDocument.Pool poolXml = responseDocument.getPool();
        WfPoolImpl.IntimateAccess poolInt = pool.getIntimateAccess(this);
        poolInt.setId(poolXml.getId());
        poolInt.setUpdateCount(poolXml.getUpdateCount());
        poolInt.setLastModifier(poolXml.getLastModifier());
        poolInt.setLastModified(poolXml.getLastModified().getTime());
    }

    public WfPool getPool(long id, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();
        GetMethod method = new GetMethod("/workflow/pool/" + id);

        PoolDocument responseDoc = (PoolDocument)httpClient.executeMethod(method, PoolDocument.class, true);
        return instantiatePool(responseDoc.getPool(), repository);
    }

    public WfPool getPoolByName(String name, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        String encodedName = RemoteWorkflowManager.encodeStringForUseInPath("pool name", name);
        GetMethod method = new GetMethod("/workflow/poolByName/" + encodedName);

        PoolDocument responseDoc = (PoolDocument)httpClient.executeMethod(method, PoolDocument.class, true);
        return instantiatePool(responseDoc.getPool(), repository);
    }

    public List<WfPool> getPools(Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        GetMethod method = new GetMethod("/workflow/pool");

        PoolsDocument responseDoc = (PoolsDocument)httpClient.executeMethod(method, PoolsDocument.class, true);
        return instantiatePools(responseDoc.getPools(), repository);
    }

    public List<WfPool> getPoolsForUser(long userId, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        GetMethod method = new GetMethod("/workflow/pool");
        method.setQueryString(new NameValuePair[] {new NameValuePair("limitToUser", String.valueOf(userId))});

        PoolsDocument responseDoc = (PoolsDocument)httpClient.executeMethod(method, PoolsDocument.class, true);
        return instantiatePools(responseDoc.getPools(), repository);
    }

    public void addUsersToPool(long poolId, List<Long> userIds, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        PostMethod method = new PostMethod("/workflow/pool/" + poolId + "/membership");
        method.setQueryString(new NameValuePair[] {new NameValuePair("action", "add")});
        UsersDocument doc = WfListHelper.getUserIdsAsXml(userIds);
        method.setRequestEntity(new InputStreamRequestEntity(doc.newInputStream()));

        httpClient.executeMethod(method, null, true);
    }

    public void removeUsersFromPool(long poolId, List<Long> userIds, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        PostMethod method = new PostMethod("/workflow/pool/" + poolId + "/membership");
        method.setQueryString(new NameValuePair[] {new NameValuePair("action", "remove")});
        UsersDocument doc = WfListHelper.getUserIdsAsXml(userIds);
        method.setRequestEntity(new InputStreamRequestEntity(doc.newInputStream()));

        httpClient.executeMethod(method, null, true);
    }

    public void clearPool(long poolId, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        PostMethod method = new PostMethod("/workflow/pool/" + poolId + "/membership");
        method.setQueryString(new NameValuePair[] {new NameValuePair("action", "clear")});

        httpClient.executeMethod(method, null, true);
    }

    public void deletePool(long poolId, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();
        DeleteMethod method = new DeleteMethod("/workflow/pool/" + poolId);
        httpClient.executeMethod(method, null, true);
    }

    public List<Long> getUsersForPool(long poolId, Repository repository) throws RepositoryException {
        DaisyHttpClient httpClient = ((RemoteRepositoryImpl)repository).getHttpClient();

        GetMethod method = new GetMethod("/workflow/pool/" + poolId + "/membership");

        UsersDocument responseDoc = (UsersDocument)httpClient.executeMethod(method, UsersDocument.class, true);
        List<Long> userIds = responseDoc.getUsers().getIdList();
        List<Long> userIdsList = new ArrayList<Long>(userIds.size());
        for (long userId : userIds)
            userIdsList.add(userId);
        return userIdsList;
    }

    private WfPoolImpl instantiatePool(PoolDocument.Pool xml, Repository repository) {
        WfPoolImpl pool = new WfPoolImpl(xml.getName(), this, repository);
        pool.setDescription(xml.getDescription());
        WfPoolImpl.IntimateAccess poolInt = pool.getIntimateAccess(this);
        poolInt.setId(xml.getId());
        poolInt.setLastModified(xml.getLastModified().getTime());
        poolInt.setLastModifier(xml.getLastModifier());
        poolInt.setUpdateCount(xml.getUpdateCount());
        return pool;
    }

    private List<WfPool> instantiatePools(PoolsDocument.Pools xml, Repository repository) {
        List<PoolDocument.Pool> poolsXml = xml.getPoolList();
        List<WfPool> pools = new ArrayList<WfPool>(poolsXml.size());
        for (PoolDocument.Pool poolXml : poolsXml) {
            pools.add(instantiatePool(poolXml, repository));
        }
        return pools;
    }
}
