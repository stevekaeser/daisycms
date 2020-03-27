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
package org.outerj.daisy.repository.clientimpl.infrastructure;

import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.util.URIUtil;

public class AbstractRemoteStrategy {
    protected RemoteRepositoryManager.Context context;

    public AbstractRemoteStrategy(RemoteRepositoryManager.Context context) {
        this.context = context;
    }

    protected DaisyHttpClient getClient(AuthenticatedUser user) {
        HttpState httpState = DaisyHttpClient.buildHttpState(user.getLogin(), user.getPassword(), user.getActiveRoleIds());
        DaisyHttpClient httpClient = new DaisyHttpClient(context.getSharedHttpClient(), context.getSharedHostConfiguration(), httpState, user.getLogin());
        return httpClient;
    }

    protected static String encodeNameForUseInPath(String name, String value) throws RepositoryException {
        try {
            return URIUtil.encodeWithinPath(value);
        } catch (URIException e) {
            throw new RepositoryException("Error encoding " + name + " string", e);
        }
    }

    protected NameValuePair[] getBranchLangParams(long branchId, long languageId) {
        NameValuePair[] queryString = new NameValuePair[2];
        queryString[0] = new NameValuePair("branch", String.valueOf(branchId));
        queryString[1] = new NameValuePair("language", String.valueOf(languageId));
        return queryString;
    }
}
