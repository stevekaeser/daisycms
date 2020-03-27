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

import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.Namespaces;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;

public class NamespaceManagerImpl implements NamespaceManager {
    private CommonNamespaceManager commonNamespaceManager;
    private AuthenticatedUser user;

    public NamespaceManagerImpl(CommonNamespaceManager commonNamespaceManager, AuthenticatedUser user) {
        this.commonNamespaceManager = commonNamespaceManager;
        this.user = user;
    }

    public String getRepositoryNamespace() {
        return commonNamespaceManager.getRepositoryNamespace();
    }

    public String getRepositoryNamespace(Document document) throws RepositoryException {
        return commonNamespaceManager.getRepositoryNamespace(document);
    }

    public String[] getRepositoryNamespaces() {
        return commonNamespaceManager.getRepositoryNamespaces();
    }

    public Namespace registerNamespace(String namespaceName) throws RepositoryException {
        return commonNamespaceManager.registerNamespace(namespaceName, user);
    }

    public Namespace registerNamespace(String namespaceName, String fingerprint) throws RepositoryException {
        return commonNamespaceManager.registerNamespace(namespaceName, fingerprint, user);
    }

    public Namespace unregisterNamespace(String namespaceName) throws RepositoryException {
        return commonNamespaceManager.unregisterNamespace(namespaceName, user);
    }

    public Namespace unregisterNamespace(long id) throws RepositoryException {
        return commonNamespaceManager.unregisterNamespace(id, user);
    }

    public Namespace getNamespace(String name) throws NamespaceNotFoundException {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        return commonNamespaceManager.getNamespace(name);
    }

    public Namespace getNamespace(long id) throws NamespaceNotFoundException {
        return commonNamespaceManager.getNamespace(id);
    }

    public Namespace updateNamespace(Namespace namespace) throws RepositoryException {
        return commonNamespaceManager.updateNamespace(namespace, user);
    }

    public Namespaces getAllNamespaces() throws RepositoryException {
        return new NamespacesImpl(commonNamespaceManager.getAllNamespaces(user), commonNamespaceManager.getRepositoryNamespace());
    }
}
