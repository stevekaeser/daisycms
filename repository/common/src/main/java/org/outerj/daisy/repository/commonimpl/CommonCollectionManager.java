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

import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentCollections;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;

/**
 * This class contains the same methods as the specific
 * CollectionManager implementations, but has extra
 * User arguments. 
 */
public class CommonCollectionManager {
    private CollectionStrategy collectionStrategy;
    private CollectionCache cache;

    /**
     * create a CommonCollectionManager with a 
     * specified collectionStrategy.
     */
    public CommonCollectionManager(CollectionStrategy collectionStrategy, CollectionCache cache) {
        this.collectionStrategy = collectionStrategy;
        this.cache = cache;
    }

    public RepositoryListener getCacheListener() {
        return cache;
    }

    public CollectionCache getCache() {
        return cache;
    }

    public DocumentCollection createCollection(String name, AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("The current user is not allowed to create collections.");
        return new DocumentCollectionImpl(collectionStrategy, name, user);
    }

    public DocumentCollectionImpl getCollection(long collectionId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return collectionStrategy.loadCollection(collectionId, user);
        else
            return cache.getCollection(collectionId);
    }

    public DocumentCollectionImpl getCollectionByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return collectionStrategy.loadCollectionByName(name, user);
        else
            return cache.getCollectionByName(name);
    }

    public DocumentCollections getCollections(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable)
            return new DocumentCollectionsImpl(collectionStrategy.loadCollections(user).toArray(new DocumentCollection[0]));
        else
            return cache.getCollections();
    }

    public void deleteCollection(long collectionId, AuthenticatedUser user) throws RepositoryException{
        collectionStrategy.deleteCollection(collectionId, user);
    }

}
