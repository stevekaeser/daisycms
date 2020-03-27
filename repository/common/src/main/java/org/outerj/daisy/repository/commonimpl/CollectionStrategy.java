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

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.DocumentCollection;

import java.util.Collection;

public interface CollectionStrategy {
    /**
     * Stores a collection.
     * @param collection the collection to store
     */
    public void store(DocumentCollectionImpl collection) throws RepositoryException;

    /**
     * Loads a DocumentCollection for a specified documentcollectionid. A RepositoryException
     * is thrown if no DocumentCollection could be found for the specified id.
     * @param collectionId
     * @param user
     * @return the DocumentCollection for the specified id, if found
     */
    public DocumentCollectionImpl loadCollection(long collectionId, AuthenticatedUser user) throws RepositoryException;

    public DocumentCollectionImpl loadCollectionByName(String name, AuthenticatedUser user) throws RepositoryException;

    /**
     * Loads all the available collections in the repository.
     * @param user the user requesting the DocumentCollections
     * @return the available collections in the repository, null if no collections can be found.
     */
    public Collection<DocumentCollectionImpl> loadCollections(AuthenticatedUser user) throws RepositoryException;

    /**
     * Removes the collection, identified by the specified identifier, from the Repository.
     *
     * @param collectionId the collection id of the collection to remove
     * @param user the user who wants to delete the collection
     */
    public void deleteCollection(long collectionId, AuthenticatedUser user) throws RepositoryException;
}
