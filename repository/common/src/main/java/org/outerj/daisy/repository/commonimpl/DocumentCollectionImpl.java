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
import java.util.Date;
import java.util.GregorianCalendar;

import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.CollectionDocument;

/**
 * A <b>user aware</b> Collection implementation.
 * In order to use this awareness, a client needs of course
 * to be aware of the fact that the Collections being used
 * are in fact CollectionImpl object.
 * 
 * The extra methods in this interface that are not present
 * in the Collection interface are meant for internal use
 * and <b>not for client use</b>! 
 */
public class DocumentCollectionImpl implements DocumentCollection, Comparable {
    private final AuthenticatedUser requestingUser;
    private String name;
    private Date lastModified;
    private long lastModifier=-1;
    private boolean readOnly = false;
    private long updateCount = 0;
    private static final String READ_ONLY_MESSAGE = "This collection is read-only.";

    /* Initialize on -1 until the save method is called.
     * As soon as the Collection is persisted, this id
     * can be updated with the actual value. This allows
     * a user of this class to distinguish between
     * persisted and not yet persisted Collection objects. 
     *
     * After the CollectionStrategy has been invoked
     * in order to persist the Collection, this 
     * CollectionStrategy itself calls a method of the
     * Object that was used to invoke the CollectionStrategy
     * in the first place. Although this concept isn't
     * formalized yet, this is the general idea.
     * Also the 'user awareness' will probably only
     * be used by this CollectionStrategy.
     * 
     */
    private long id = -1;
    private CollectionStrategy collectionStrategy;
    private IntimateAccess intimateAccess = new IntimateAccess();
    
    /**
     * creates a new CollectionImpl object which is aware
     * of the user that requested the collection.
     * 
     * @param collectionStrategy the strategy used to load and store data
     * @param name the name of the Collection
     * @param requestingUser the User requesting the collection
     */
    public DocumentCollectionImpl(CollectionStrategy collectionStrategy, String name, AuthenticatedUser requestingUser) {
        this.requestingUser = requestingUser;
        this.setName(name);
        this.collectionStrategy = collectionStrategy;
    }
    
    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String collectionName) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (collectionName == null || collectionName.length() == 0)
            throw new IllegalArgumentException("Collection name should not be null or empty string.");
        
        name = collectionName;
    }

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        collectionStrategy.store(this);
    }

    /* (non-Javadoc)
     * @see org.outerj.daisy.repository.Collection#getXml()
     */
    public CollectionDocument getXml() {
        CollectionDocument collectionDocument = CollectionDocument.Factory.newInstance();
        CollectionDocument.Collection collectionXml = collectionDocument.addNewCollection();
        collectionXml.setName(name);
        collectionXml.setUpdatecount(updateCount);

        if (id != -1) {
            collectionXml.setId(id);
            GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
            lastModifiedCalendar.setTime(lastModified);
            collectionXml.setLastModified(lastModifiedCalendar);
            collectionXml.setLastModifier(lastModifier);
        }
        
        return collectionDocument;
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return null;
    }

    public long getLastModifier() {
        return lastModifier;
    }
    
    /**
     * @param strategy the CollectionStrategy requesting intimate access
     * @return an IntimateAccess object if the CollectionStrategy is the same as the
     * one supplied when creating the DocumentCollectionImpl object, null if it is
     * another one.
     */
    public IntimateAccess getIntimateAccess(CollectionStrategy strategy) {
        if (this.collectionStrategy == strategy) {
            return intimateAccess;
        }
        else
            return null;        
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public int compareTo(Object o) {
        DocumentCollection otherCollection = (DocumentCollection)o;
        return getName().compareTo(otherCollection.getName());
    }

    /**
     * a class that provides intimate access to the DocumentCollectionImpl.
     * 
     * <p>The purpose is to grant setter access to all fields for certain
     * classes, whilst other (all) classes only have setter access to a limited amount
     * of fields.
     */
    public class IntimateAccess {
        private IntimateAccess() {}

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentCollectionImpl.this.id=id;
        }

        public void setLastModified(Date d) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentCollectionImpl.this.lastModified=d;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentCollectionImpl.this.lastModifier=lastModifier;
        }

        public AuthenticatedUser getCurrentUser() {
            return DocumentCollectionImpl.this.requestingUser;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);
            DocumentCollectionImpl.this.updateCount = updateCount;
        }

        /**
         * updates the state of the current object after it has been persisted.
         *
         * <p>The CollectionStrategy uses this method to update the state of the
         * current object after it has been persisted.
         */
        public void saved (long id, String name, Date lastModified, long lastModifier, long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            DocumentCollectionImpl.this.id = id;
            DocumentCollectionImpl.this.name = name;
            DocumentCollectionImpl.this.lastModified = lastModified;
            DocumentCollectionImpl.this.lastModifier = lastModifier;
            DocumentCollectionImpl.this.updateCount = updateCount;
        }
    }
}
