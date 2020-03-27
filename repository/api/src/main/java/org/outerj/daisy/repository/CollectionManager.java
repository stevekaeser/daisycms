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
package org.outerj.daisy.repository;

/**
 * The CollectionManager is responsible for all tasks related
 * to collection management (creation, deletion, listing).
 *
 * <p>The CollectionManager can be retrieved via {@link Repository#getCollectionManager()}.
 *
 * <p>Collections are named sets of documents. The same document can
 * be part of multiple collections, thus collections can overlap.
 *
 * <p>Assigning documents to collections is done through the
 * {@link Document} API.
 * 
 */
public interface CollectionManager {
    /**
     * Creates a new collection. This does not immediately create the
     * collection in the repository, you need to call the save() method
     * on the returned DocumentCollection object to do this.
     *
     * @param name the name of the new collection
     */
    DocumentCollection createCollection(String name) throws RepositoryException;
    
    /**
     * Retrieves a collection by its ID.
     * 
     */
    DocumentCollection getCollection(long collectionId, boolean updateable) throws RepositoryException;

    /**
     * Retrieves a collection by ID or by name. If the collection parameter consists of digits only (0-9), it
     * will be considered to be an ID (even though entirely numeric collection names are allowed, which
     * for this reason are not recommended).
     */
    DocumentCollection getCollection(String collection, boolean updateable) throws RepositoryException;

    /**
     * Retrieves a collection by its name.
     */
    DocumentCollection getCollectionByName(String name, boolean updateable) throws RepositoryException;

    /**
     * Deletes a collection from the repository.
     *
     * <p>If any documents were associated with (contained by) this collection,
     * these associations will be removed, but the documents themselves are not
     * removed. Note that this can be done even if there are locks on documents
     * contained by this collection, and that the last modified timestamp of the
     * documents will remain untouched.
     *
     * @param collectionId the ID of the collection to remove
     */
    void deleteCollection(long collectionId) throws RepositoryException;
    
    /**
     * Gets all collections, in no specific order.
     */
    DocumentCollections getCollections(boolean updateable) throws RepositoryException;
}
