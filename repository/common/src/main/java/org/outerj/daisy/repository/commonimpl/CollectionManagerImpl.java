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

import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentCollections;
import org.outerj.daisy.repository.RepositoryException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class CollectionManagerImpl implements CollectionManager {
    private CommonCollectionManager delegate;
    private AuthenticatedUser user;
    private static final Pattern ALL_DIGITS = Pattern.compile("^[0-9]+$");

    public CollectionManagerImpl(CommonCollectionManager delegate, AuthenticatedUser user) {
        this.delegate = delegate;
        this.user = user;
    }

    public DocumentCollection createCollection(String name)
        throws RepositoryException {
        return delegate.createCollection(name, user);
    }

    public DocumentCollection getCollection(long collectionId, boolean updateable) throws RepositoryException {
        return delegate.getCollection(collectionId, updateable, user);
    }

    public DocumentCollection getCollection(String collection, boolean updateable) throws RepositoryException {
        if (collection == null)
            throw new IllegalArgumentException("collection param can not be null");
        if (collection.length() == 0)
            throw new IllegalArgumentException("collection param cannot be an empty string");

        Matcher allDigitsMatcher = ALL_DIGITS.matcher(collection);
        if (allDigitsMatcher.matches()) {
            long id = Long.parseLong(collection);
            return getCollection(id, updateable);
        } else {
            return getCollectionByName(collection, updateable);
        }
    }

    public DocumentCollection getCollectionByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getCollectionByName(name, updateable, user);
    }

    public DocumentCollections getCollections(boolean updateable) throws RepositoryException {
        return delegate.getCollections(updateable, user);
    }

    public void deleteCollection(long collectionId) throws RepositoryException {
        delegate.deleteCollection(collectionId, user);
    }

}
