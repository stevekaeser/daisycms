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

import java.util.HashMap;
import java.util.Map;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.RepositoryStrategy;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;

public class NamespaceCache implements RepositoryListener {
    private final RepositoryStrategy repositoryStrategy;
    private final AuthenticatedUser cacheUser;
    private boolean cacheLoaded = false;
    private Map<String, Namespace> namespacesByName = new HashMap<String, Namespace>();
    private Map<Long, Namespace> namespacesById = new HashMap<Long, Namespace>();

    private String[] repositoryNamespaces;

    public NamespaceCache(RepositoryStrategy repositoryStrategy, AuthenticatedUser cacheUser) {
        this.repositoryStrategy = repositoryStrategy;
        this.cacheUser = cacheUser;
    }

    public String getRepositoryNamespace() {
        assureCacheLoaded();
        return repositoryNamespaces[0];
    }

    public String getRepositoryNamespace(Document document) throws RepositoryException {
        assureCacheLoaded();
        return repositoryStrategy.getRepositoryNamespace(document, cacheUser);
    }

    public String[] getRepositoryNamespaces() {
        assureCacheLoaded();
        return repositoryNamespaces;
    }

    public Namespace getNamespace(String namespaceName) throws NamespaceNotFoundException {
        assureCacheLoaded();

        Namespace namespace = namespacesByName.get(namespaceName);
        if (namespace == null)
            throw new NamespaceNotFoundException(namespaceName);
        else
            return namespace;
    }

    public Namespace getNamespace(long id) throws NamespaceNotFoundException {
        assureCacheLoaded();

        Namespace namespace = namespacesById.get(new Long(id));
        if (namespace == null)
            throw new NamespaceNotFoundException(id);
        else
            return namespace;
    }

    private void assureCacheLoaded() {
        if (cacheLoaded)
            return;

        synchronized(this) {
            if (cacheLoaded)
                return;

            try {
                Map<String, Namespace> newNamespacesByName = new HashMap<String, Namespace>();
                Map<Long, Namespace> newNamespacesById = new HashMap<Long, Namespace>();
                Namespace[] namespaces = repositoryStrategy.getAllNamespaces(cacheUser);
                for (int i = 0; i < namespaces.length; i++) {
                    newNamespacesByName.put(namespaces[i].getName(), namespaces[i]);
                    newNamespacesById.put(new Long(namespaces[i].getId()), namespaces[i]);
                }
                this.namespacesByName = newNamespacesByName;
                this.namespacesById = newNamespacesById;

                repositoryNamespaces = repositoryStrategy.getRepositoryNamespaces(cacheUser);
            } catch (RepositoryException e) {
                throw new RepositoryRuntimeException("Error loading namespace cache.", e);
            }

            cacheLoaded = true;
        }
    }

    public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
        if (eventType.isNamespaceEvent()) {
            synchronized(this) {
                cacheLoaded = false;
            }
        }
    }

}
