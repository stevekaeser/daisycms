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

import java.util.Date;

import org.outerx.daisy.x10.CollectionDocument;

/**
 * A collection of documents in the repository. This interface is called
 * DocumentCollection to avoid a name collision with the often-used Collection
 * interface from the Java API.
 *
 * <p>Using {@link Document#getCollections()}, you can retrieve the collections
 * to which a document belongs. To get a list of all documents belonging to
 * a collection, peform a query using the {@link org.outerj.daisy.repository.query.QueryManager}.
 *
 * <p>Creating and deleting collection is done using the
 * {@link CollectionManager} which can be obtained using
 * {@link Repository#getCollectionManager()}.
 */
public interface DocumentCollection {
    /**
     * Returns the ID of this collection. For newly created collections, this method
     * returns -1 until {@link #save()} is called.
     */
    long getId();

    /**
     * Returns the name of this collection.
     */
    String getName();

    /**
     * Sets the name of this collection.
     */
    void setName(String name);

    /**
     * Stores the modified collection.
     */
    void save() throws RepositoryException;

    /**
     * Get an XML document describing this collection.
     */
    CollectionDocument getXml();
    
    /**
     * Gets the date when this collection was last saved. This
     * does NOT include the adding or removing of documents to
     * the collection.
     */
    Date getLastModified();
    
    /**
     * Get the id of the user that last modified this collection.
     */
    long getLastModifier();

    long getUpdateCount();
}
