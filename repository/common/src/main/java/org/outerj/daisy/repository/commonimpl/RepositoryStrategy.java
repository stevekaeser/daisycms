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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.namespace.NamespaceImpl;
import org.outerj.daisy.repository.namespace.Namespace;

public interface RepositoryStrategy {
    String getClientVersion(AuthenticatedUser user);

    String getServerVersion(AuthenticatedUser user);

    NamespaceImpl registerNamespace(String namespaceName, String fingerprint, AuthenticatedUser user) throws RepositoryException;

    NamespaceImpl registerNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException;

    NamespaceImpl unregisterNamespace(String namespaceName, AuthenticatedUser user) throws RepositoryException;

    NamespaceImpl unregisterNamespace(long id, AuthenticatedUser user) throws RepositoryException;
    
    Namespace updateNamespace(Namespace namespace, AuthenticatedUser user) throws RepositoryException;

    Namespace[] getAllNamespaces(AuthenticatedUser user) throws RepositoryException;

    Namespace getNamespaceByName(String namespaceName, AuthenticatedUser user) throws RepositoryException;

    String getRepositoryNamespace(AuthenticatedUser user) throws RepositoryException;
    
    String getRepositoryNamespace(Document document, AuthenticatedUser user) throws RepositoryException;
    
    String[] getRepositoryNamespaces(AuthenticatedUser user) throws RepositoryException;
}
