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
package org.outerj.daisy.repository.namespace;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;

/**
 * Manages the namespaces the repository knows about.
 *
 * <p>The NamespaceManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getNamespaceManager()}.
 *
 * <p>The purpose of namespaces is to be able to avoid name collisions
 * (or in case of documents, ID-collisions) between repository servers
 * when exchanging entities (e.g. through export/import) between them.
 *
 * <p>At the time of this writing, namespaces are only used for documents.
 *
 * <p>As long as each repository between which items are exchanged has
 * its own namespace, collisions will be avoided. So it comes down
 * to properly managing the namespaces. Making the namespace more unique
 * (e.g. long random string) will of course lower the possibility
 * that two repositories use the same namespace. However, such namespaces
 * are not so friendly to work with.
 *
 * <p>A namespace has a fingerprint which is used as additional verification
 * that two namespaces are really the same. This is useful since the
 * namespace name will often be a short string (e.g. XYZ), so there is a
 * realistic chance that another repository uses the same namespace name.
 * The fingerprint then allows to verify the namespaces really correspond.
 *
 * <p>Note that in contrast with other entities in Daisy, which are
 * primarily identified by ID and can be renamed (e.g. document types, users),
 * a namespace cannot be renamed. This is because the namespace name
 * serves as the primary identification. [The current implementation also
 * relies on this, i.e. it assumes the mapping internal ID - namespace name
 * never changes.]
 *
 * @since Daisy 2.0
 */
public interface NamespaceManager {
    /**
     * Returns the namespace for the current repository. All entities
     * (documents) created in this repository are by default created in
     * this namespace. This method returns exactly the same as
     * {@link org.outerj.daisy.repository.Repository#getNamespace()}.
     */
    String getRepositoryNamespace();
    
    /**
     * Returns the namespace for the current repository. All entities
     * (documents) created in this repository are by default created in
     * this namespace. This method returns exactly the same as
     * {@link org.outerj.daisy.repository.Repository#getNamespace()}.
     */
    String getRepositoryNamespace(Document document) throws RepositoryException;
    
    /**
     * Returns a list of namespaces that are managed by the repository
     * @return
     */
    String[] getRepositoryNamespaces();

    /**
     * @throws NamespaceNotFoundException if the namespace does not exist.
     */
    Namespace getNamespace(String name) throws NamespaceNotFoundException;

    /**
     * Gets a namespace by internal namespace ID. Usually you should not pay
     * any value to the internal ID and get the namespace by name instead.
     *
     * @throws NamespaceNotFoundException if the namespace does not exist.
     */
    Namespace getNamespace(long id) throws NamespaceNotFoundException;

    /**
     * @param namespaceName the name should be unique, and conform to the regexp <tt>[a-zA-Z0-9_]+</tt>.
     *                      Namespace names are case-sensitive, though two namespaces that are equal-ignoring-case are not allowed.
     */
    Namespace registerNamespace(String namespaceName) throws RepositoryException;

    Namespace registerNamespace(String namespaceName, String fingerprint) throws RepositoryException;

    /**
     * Unregisters (deletes) a namespace.
     *
     * <p>Unregistering a namespace is only possible if it is not in use anymore.
     */
    Namespace unregisterNamespace(String namespaceName) throws RepositoryException;

    /**
     * Unregisters (deletes) a namespace.
     *
     * <p>Unregistering a namespace is only possible if it is not in use anymore.
     */
    Namespace unregisterNamespace(long id) throws RepositoryException;
    
    /**
     * Updates a namespace. Only the isManaged & document count attributes will be updated
     * @param namespace
     * @return
     * @throws RepositoryException
     */
    Namespace updateNamespace(Namespace namespace) throws RepositoryException;
    
    /**
     * Gets a list of all namespaces known to the repository
     * @return
     * @throws RepositoryException
     */
    Namespaces getAllNamespaces() throws RepositoryException;
}
