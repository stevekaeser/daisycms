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
package org.outerj.daisy.repository.commonimpl.namespace;

import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.commonimpl.RepositoryStrategy;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;

public class CommonNamespaceManager {
    private NamespaceCache namespaceCache;
    private RepositoryStrategy repositoryStrategy;

    public CommonNamespaceManager(NamespaceCache namespaceCache, RepositoryStrategy repositoryStrategy) {
        this.namespaceCache = namespaceCache;
        this.repositoryStrategy = repositoryStrategy;
    }

    public RepositoryListener getCacheListener() {
        return namespaceCache;
    }

    public String getRepositoryNamespace() {
        return namespaceCache.getRepositoryNamespace();
    }

    public String getRepositoryNamespace(Document document) throws RepositoryException {
        return namespaceCache.getRepositoryNamespace(document);
    }

    public String[] getRepositoryNamespaces() {
        return namespaceCache.getRepositoryNamespaces();
    }

    public Namespace registerNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.registerNamespace(namespaceName, user);
    }

    public Namespace registerNamespace(String namespaceName, String fingerprint, AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.registerNamespace(namespaceName, fingerprint, user);
    }

    public Namespace unregisterNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.unregisterNamespace(namespaceName, user);
    }

    public Namespace unregisterNamespace(long id, AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.unregisterNamespace(id, user);
    }
    
    public Namespace updateNamespace(Namespace namespace, AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.updateNamespace(namespace, user);
    }

    public Namespace[] getAllNamespaces(AuthenticatedUser user) throws RepositoryException {
        return repositoryStrategy.getAllNamespaces(user);
    }

    public Namespace getNamespace(String name) throws NamespaceNotFoundException {
        return namespaceCache.getNamespace(name);
    }

    public Namespace getNamespace(long id) throws NamespaceNotFoundException {
        return namespaceCache.getNamespace(id);
    }
}
