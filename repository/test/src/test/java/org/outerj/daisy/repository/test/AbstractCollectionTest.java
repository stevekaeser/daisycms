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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.user.Role;

public abstract class AbstractCollectionTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testCollections() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        CollectionManager collectionManager = repository.getCollectionManager();

        DocumentCollection collection1 = collectionManager.createCollection("collection1");
        collection1.save();

        DocumentCollection fetchedCollection1 = collectionManager.getCollection(collection1.getId(), true);
        assertEquals("collection name is stored correctly", collection1.getName(), fetchedCollection1.getName());

        //some extra tests on the object we just fetched
        assertTrue("data store id of stored collection is -1 or less", fetchedCollection1.getId() > -1);
        assertNotNull("last modified date of stored collection is null", fetchedCollection1.getLastModified());
        assertTrue("last modifier of stored collection is invalid", fetchedCollection1.getLastModifier() >= -1);
        assertNotNull("xml object returned by stored collection is null", fetchedCollection1.getXml());

        // try to modify the collection
        collection1.setName(collection1.getName() + "x");
        collection1.save();
        assertEquals("collection name change correctly saved", collection1.getName(),
                collectionManager.getCollection(collection1.getId(), true).getName());

        // try to retrieve the collection by name
        assertEquals("retrieving collection by name should give collection with correct id",
                collection1.getId(), collectionManager.getCollectionByName(collection1.getName(), true).getId());

        /* at this point there should be at least one collection in the list of all collections,
         * so the collection list can easily be loaded here.
         */
        DocumentCollections collections = collectionManager.getCollections(true);
        assertNotNull(collections);
        DocumentCollection[] collArray = collections.getArray();
        assertNotNull(collArray);

        for (int i = 0; i < collArray.length; i++) {
            DocumentCollection collection = collArray[i];
            assertNotNull(collection);
        }

        //
        // test proper invalidation of collection caches (if any)
        //
        DocumentCollection collection2 = collectionManager.createCollection("collection2");
        collection2.save();

        // line below should NOT throw an CollectionNotFoundException
        collectionManager.getCollection(collection2.getId(), false);

        collection2.setName("collection2-modified");
        collection2.save();

        DocumentCollection collection2Refetched = collectionManager.getCollection(collection2.getId(), false);
        assertEquals(collection2.getName(), collection2Refetched.getName());

        collectionManager.deleteCollection(collection2.getId());
        try {
            collectionManager.getCollection(collection2.getId(), false);
            fail("Getting a deleted exception should throw an exception.");
        } catch (CollectionNotFoundException e) {}

        DocumentCollection collection3 = collectionManager.createCollection("a b");
        collection3.save();
        assertEquals(collection3.getId(), collectionManager.getCollectionByName("a b", true).getId());
        assertEquals(collection3.getId(), collectionManager.getCollectionByName("a b", false).getId());

        // NOTE: test of assigning collections to documents are part of the AbstractDocumentTest
    }
}
