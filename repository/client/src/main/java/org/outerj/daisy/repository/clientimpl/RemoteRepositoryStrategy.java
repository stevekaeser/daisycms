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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.RepositoryStrategy;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceImpl;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.util.VersionHelper;
import org.outerx.daisy.x10.NamespaceDocument;
import org.outerx.daisy.x10.NamespacesDocument;

public class RemoteRepositoryStrategy extends AbstractRemoteStrategy implements RepositoryStrategy {
    private Properties versionProps;

    public RemoteRepositoryStrategy(RemoteRepositoryManager.Context context) {
        super(context);

        try {
            versionProps = VersionHelper.getVersionProperties(getClass().getClassLoader(), "org/outerj/daisy/repository/clientimpl/versioninfo.properties");
        } catch (IOException e) {
            throw new RepositoryRuntimeException("Error getting version information.", e);
        }
    }

    public String getClientVersion(AuthenticatedUser user) {
        if (versionProps == null) {
            throw new RepositoryRuntimeException("Error getting version information.");
        }
        String version = VersionHelper.getVersion(versionProps);
        if (version != null)
            return version;
        else
            throw new RepositoryRuntimeException("Version unknown.");
    }

    public String getServerVersion(AuthenticatedUser user) {
        DaisyHttpClient httpClient = getClient(user);
        // The server reports its version in a HTTP header with each request, the actual URL we request
        // doesn't matter much
        HttpMethod method = new GetMethod("/repository/userinfo");
        try {
            httpClient.executeMethod(method, null, true);
        } catch (RepositoryException e) {
            throw new RepositoryRuntimeException("Error getting version info.", e);
        }
        Header header = method.getResponseHeader("X-Daisy-Version");
        if (header != null && header.getValue() != null && header.getValue().length() > 0) {
            String version = header.getValue();
            int spacePos = version.indexOf(' ');
            if (spacePos > 0)
                return version.substring(0, spacePos);
            else
                return version;
        } else {
            throw new RepositoryRuntimeException("Repository server did not communicate version (missing X-Daisy-Version header).");
        }
    }

    public NamespaceImpl registerNamespace(String namespaceName, String fingerprint, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new PostMethod("/repository/namespace");

        List<NameValuePair> parameters = new ArrayList<NameValuePair>();
        parameters.add(new NameValuePair("name", namespaceName));
        if (fingerprint != null)
            parameters.add(new NameValuePair("fingerprint", fingerprint));

        method.setQueryString(parameters.toArray(new NameValuePair[0]));

        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);

        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_REGISTERED, new Long(namespace.getId()), 0);

        return namespace;
    }

    public NamespaceImpl registerNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        return registerNamespace(namespaceName, null, user);
    }

    public NamespaceImpl unregisterNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String encodedName = encodeNameForUseInPath("namespace", namespaceName);
        HttpMethod method = new DeleteMethod("/repository/namespaceByName/" + encodedName);

        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());

        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_UNREGISTERED, new Long(namespace.getId()), 0);

        return namespace;
    }

    public NamespaceImpl unregisterNamespace(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        HttpMethod method = new DeleteMethod("/repository/namespace/" + id);

        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());

        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_UNREGISTERED, new Long(namespace.getId()), 0);

        return namespace;
    }
    
    public Namespace updateNamespace(Namespace namespace, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        
        PostMethod method = new PostMethod("/repository/namespace/" + namespace.getId());
        ByteArrayOutputStream xmlOS = new ByteArrayOutputStream(5000);
        try {
            namespace.getXml().save(xmlOS);
        } catch (IOException e) {
            throw new RepositoryException("Error serializing document XML.", e);
        }
        method.setRequestEntity(new ByteArrayRequestEntity(xmlOS.toByteArray()));
        
        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl newNs = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        
        RepositoryEventType eventType;
        if (namespace.isManaged())
            eventType = RepositoryEventType.NAMESPACE_MANAGED;
        else
            eventType = RepositoryEventType.NAMESPACE_UNMANAGED;
            
        context.getCommonRepository().fireRepositoryEvent(eventType, namespace.getId(), 0);
        return newNs;
    }

    public NamespaceImpl manageNamespace(long id, long documentCount, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        
        PostMethod method = new PostMethod("/repository/namespace/" + id);
        method.addParameter(new NameValuePair("documentCount", Long.toString(documentCount)));
        
        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_MANAGED, Long.valueOf(namespace.getId()), 0);
        return namespace;
    }

    public NamespaceImpl manageNamespace(String namespaceName, long documentCount, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        
        String encodedName = encodeNameForUseInPath("namespace", namespaceName);
        PostMethod method = new PostMethod("/repository/namespaceByName/" + encodedName);
        method.addParameter(new NameValuePair("documentCount", Long.toString(documentCount)));
        
        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_MANAGED, Long.valueOf(namespace.getId()), 0);
        return namespace;
    }

    public NamespaceImpl unmanageNamespace(long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        
        HttpMethod method = new DeleteMethod("/repository/namespace/managed/" + id);        
        
        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_UNMANAGED, Long.valueOf(namespace.getId()), 0);
        return namespace;
    }

    public NamespaceImpl unmanageNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        
        String encodedName = encodeNameForUseInPath("namespace", namespaceName);
        HttpMethod method = new DeleteMethod("/repository/namespaceByName/managed/" + encodedName);
        
        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        NamespaceImpl namespace = instantiateNamespaceFromXml(namespaceDocument.getNamespace());
        
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.NAMESPACE_UNMANAGED, Long.valueOf(namespace.getId()), 0);
        return namespace;
    }

    public Namespace[] getAllNamespaces(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/namespace");

        NamespacesDocument namespacesDocument = (NamespacesDocument) httpClient.executeMethod(method, NamespacesDocument.class, true);
        List<NamespaceDocument.Namespace> namespacesXml = namespacesDocument.getNamespaces().getNamespaceList();

        NamespaceImpl[] namespaces = new NamespaceImpl[namespacesXml.size()];
        for (int i = 0; i < namespacesXml.size(); i++) {
            namespaces[i] = instantiateNamespaceFromXml(namespacesXml.get(i));
        }
        return namespaces;
    }

    private NamespaceImpl instantiateNamespaceFromXml(NamespaceDocument.Namespace namespaceXml) {
        return new NamespaceImpl(namespaceXml.getId(), namespaceXml.getName(), namespaceXml.getFingerprint(), namespaceXml.getRegisteredBy(), namespaceXml
                .getRegisteredOn().getTime(), namespaceXml.getDocumentCount(), namespaceXml.getIsManaged());
    }

    public Namespace getNamespaceByName(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);

        String encodedName = encodeNameForUseInPath("namespace", namespaceName);
        HttpMethod method = new GetMethod("/repository/namespaceByName/" + encodedName);

        NamespaceDocument namespaceDocument = (NamespaceDocument) httpClient.executeMethod(method, NamespaceDocument.class, true);
        return instantiateNamespaceFromXml(namespaceDocument.getNamespace());
    }

    public String getRepositoryNamespace(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/namespace");

        NamespacesDocument namespacesDocument = (NamespacesDocument) httpClient.executeMethod(method, NamespacesDocument.class, true);
        return namespacesDocument.getNamespaces().getRepositoryNamespace();
    }

    public String[] getRepositoryNamespaces(AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        HttpMethod method = new GetMethod("/repository/namespace");

        NamespacesDocument namespacesDocument = (NamespacesDocument) httpClient.executeMethod(method, NamespacesDocument.class, true);
        List<org.outerx.daisy.x10.NamespaceDocument.Namespace> namespacesXml = namespacesDocument.getNamespaces().getNamespaceList();
        String[] namespaces = new String[namespacesXml.size()];
        for (int i = 0; i < namespacesXml.size(); i++) {
            org.outerx.daisy.x10.NamespaceDocument.Namespace namespaceXml = namespacesXml.get(i);
            if (namespaceXml.getIsManaged())
                namespaces[i] = namespaceXml.getName();
        }

        return namespaces;
    }

    public String getRepositoryNamespace(Document document, AuthenticatedUser user) throws RepositoryException {
        throw new RepositoryException("This method is not supported.");
    }
}
