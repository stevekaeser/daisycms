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

import org.outerj.daisy.repository.*;

import java.util.*;

public class CollectionCache implements RepositoryListener {
    private CollectionStrategy collectionStrategy;
    private AuthenticatedUser cacheUser;

    private boolean cacheLoaded = false;
    private Map<Long, DocumentCollectionImpl> collectionsById;
    private Map<String, DocumentCollectionImpl> collectionsByName;
    private Collection<DocumentCollectionImpl> collections;
    private DocumentCollections documentCollections;

    public CollectionCache(CollectionStrategy collectionStrategy, AuthenticatedUser cacheUser) {
        this.collectionStrategy = collectionStrategy;
        this.cacheUser = cacheUser;
    }

    public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
        if (eventType == RepositoryEventType.COLLECTION_CREATED)
            collectionCreated((Long)id);
        else if (eventType == RepositoryEventType.COLLECTION_DELETED)
            collectionDeleted((Long)id);
        else if (eventType == RepositoryEventType.COLLECTION_UPDATED)
            collectionUpdated((Long)id, updateCount);
    }

    public void assureCacheLoaded() throws RepositoryException {
        if (cacheLoaded)
            return;

        synchronized(this) {
            if (cacheLoaded)
                return;

            collections = collectionStrategy.loadCollections(cacheUser);

            Map<Long, DocumentCollectionImpl> newCollectionsById = new HashMap<Long, DocumentCollectionImpl>();
            Map<String, DocumentCollectionImpl> newCollectionsByName = new HashMap<String, DocumentCollectionImpl>();
            for (DocumentCollectionImpl collection : collections) {
                collection.makeReadOnly();
                newCollectionsById.put(new Long(collection.getId()), collection);
                newCollectionsByName.put(collection.getName(), collection);
            }
            collectionsById = newCollectionsById;
            collectionsByName = newCollectionsByName;
            documentCollections = new DocumentCollectionsImpl(collections.toArray(new DocumentCollection[0]));
            this.cacheLoaded = true;
        }
    }

    public DocumentCollectionImpl getCollection(long id) throws RepositoryException {
        assureCacheLoaded();
        DocumentCollectionImpl collection = collectionsById.get(new Long(id));
        if (collection == null)
            throw new CollectionNotFoundException(id);
        else
            return collection;
    }

    public DocumentCollectionImpl getCollectionByName(String name) throws RepositoryException {
        assureCacheLoaded();
        DocumentCollectionImpl collection = collectionsByName.get(name);
        if (collection == null)
            throw new CollectionNotFoundException(name);
        else
            return collection;
    }

    public DocumentCollections getCollections() throws RepositoryException {
        assureCacheLoaded();
        return documentCollections;
    }

    private void collectionCreated(Long id) {
        synchronized(this) {
            if (!cacheLoaded)
                return;

            // check for duplicate event
            if (collectionsById.containsKey(id))
                return;

            cacheLoaded = false;
        }
    }

    private void collectionDeleted(Long id) {
        synchronized(this) {
            if (!cacheLoaded)
                return;

            // check for duplicate event
            if (!collectionsById.containsKey(id))
                return;

            cacheLoaded = false;
        }
    }

    private void collectionUpdated(Long id, long updateCount) {
        synchronized(this) {
            if (!cacheLoaded)
                return;

            // check for duplicate event
            DocumentCollection currentCollection = collectionsById.get(id);
            if (currentCollection != null && currentCollection.getUpdateCount() == updateCount)
                return;

            cacheLoaded = false;
        }
    }
}
