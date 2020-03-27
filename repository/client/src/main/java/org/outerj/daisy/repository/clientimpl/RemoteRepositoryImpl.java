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
package org.outerj.daisy.repository.clientimpl;

import org.outerj.daisy.repository.commonimpl.RepositoryImpl;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.NameValuePair;

import java.util.Map;

public class RemoteRepositoryImpl extends RepositoryImpl {
    private RemoteClient remoteClient;
    private RemoteRepositoryManager.Context context;

    public RemoteRepositoryImpl(CommonRepository delegate, AuthenticatedUser user, RemoteRepositoryManager.Context context) {
        super(delegate, user);
        this.context = context;
    }

    /**
     * Communicate to the daisy server using HTTP. Advantage of using this method
     * (instead of doing it just yourself) is that the authentication information
     * will be automatically passed on, and if the result is an error appropriate
     * exceptions will be thrown.
     *
     * <p>The performed request is a GET request.
     *
     */
    public GetMethod getResource(String path, Map<String, String> parameters) throws RepositoryException {
        if (remoteClient == null)
            remoteClient = new RemoteClient(context);

        GetMethod getMethod = new GetMethod(path);
        NameValuePair[] queryString = new NameValuePair[parameters.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            queryString[i] = new NameValuePair(key, value);
            i++;
        }
        getMethod.setQueryString(queryString);

        DaisyHttpClient httpClient = remoteClient.getHttpClientForCurrentUser();
        httpClient.executeMethod(getMethod, null, false);

        return getMethod;
    }

    public DaisyHttpClient getHttpClient() {
        if (remoteClient == null)
            remoteClient = new RemoteClient(context);

        DaisyHttpClient httpClient = remoteClient.getHttpClientForCurrentUser();
        return httpClient;
    }

    public String getBaseURL() {
        return context.getBaseURL();
    }

    private class RemoteClient extends AbstractRemoteStrategy {
        public RemoteClient(RemoteRepositoryManager.Context context) {
            super(context);
        }

        protected DaisyHttpClient getHttpClientForCurrentUser() {
            return getClient(user);
        }
    }

    public Object clone() {
        return new RemoteRepositoryImpl(getCommonRepository(), getUser(), context);
    }
}
