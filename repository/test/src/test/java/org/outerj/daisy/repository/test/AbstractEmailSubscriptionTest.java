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
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.emailnotifier.Subscription;
import org.outerj.daisy.emailnotifier.CollectionSubscriptionKey;

import java.util.HashSet;
import java.util.Arrays;

public abstract class AbstractEmailSubscriptionTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testSubscriptions() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        // create some content
        CollectionManager collectionManager = repository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("collection1");
        collection1.save();
        DocumentCollection collection2 = collectionManager.createCollection("collection2");
        collection2.save();

        DocumentType documentType = repository.getRepositorySchema().createDocumentType("testdoctype");
        documentType.save();

        Document document1 = repository.createDocument("doc1", "testdoctype");
        document1.addToCollection(collection1);
        document1.addToCollection(collection2);
        document1.save();

        Document document2 = repository.createDocument("doc2", "testdoctype");
        document2.addToCollection(collection1);
        document2.addToCollection(collection2);
        document2.save();

        EmailSubscriptionManager emailSubscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");

        Subscription subscription = emailSubscriptionManager.getSubscription();
        subscription.setReceiveDocumentEvents(true);
        subscription.save();

        assertEquals(1, emailSubscriptionManager.getSubscriptions().getArray().length);

        // Create a non-admin role and user
        Role userRole = repository.getUserManager().getRole("User", false);

        User jules = repository.getUserManager().createUser("jules");
        jules.addToRole(userRole);
        jules.setDefaultRole(userRole);
        jules.setPassword("jules");
        jules.save();

        Repository julesRepository = repositoryManager.getRepository(new Credentials("jules", "jules"));
        EmailSubscriptionManager julesSM = (EmailSubscriptionManager)julesRepository.getExtension("EmailSubscriptionManager");
        Subscription julesSubscription = julesSM.getSubscription();
        julesSubscription.setReceiveDocumentEvents(true);
        julesSubscription.setReceiveSchemaEvents(true);
        julesSubscription.setReceiveCommentEvents(true);
        julesSubscription.save();

        julesSubscription = julesSM.getSubscription();
        assertEquals(julesSubscription.getUserId(), jules.getId());
        assertEquals(julesSubscription.getReceiveDocumentEvents(), true);
        assertEquals(julesSubscription.getReceiveSchemaEvents(), true);
        assertEquals(julesSubscription.getReceiveUserEvents(), false);
        assertEquals(julesSubscription.getReceiveCollectionEvents(), false);
        assertEquals(julesSubscription.getReceiveAclEvents(), false);
        assertEquals(julesSubscription.getReceiveCommentEvents(), true);

        try {
            julesSM.getSubscription(repository.getUserId());
            fail("User jules shouldn't be able to retrieve subscription of other user.");
        } catch (Exception e) {
        }

        try {
            julesSM.deleteSubscription(repository.getUserId());
            fail("User jules shouldn't be able to delete subscription of other user.");
        } catch (Exception e) {
        }

        try {
            julesSM.getSubscriptions();
            fail("User jules shouldn't be able to get list of all subscriptions.");
        } catch (Exception e) {
        }

        try {
            julesSM.deleteAllSubscriptionsForDocument("1");
            fail("User jules shouldn't be able to delete all subscriptions of a document.");
        } catch (Exception e) {
        }

        try {
            julesSM.deleteAllSubscriptionsForCollection(1);
            fail("User jules shouldn't be able to delete all subscriptions of a collection.");
        } catch (Exception e) {
        }

        assertEquals(0, emailSubscriptionManager.getAllCollectionEventSubscribers().getArray().length);
        assertEquals(1, emailSubscriptionManager.getAllSchemaEventSubscribers().getArray().length);
        assertEquals(0, emailSubscriptionManager.getAllUserEventSubscribers().getArray().length);

        julesSubscription.setSubscribedVariantKeys(new VariantKey[] { new VariantKey(document1.getId(), -1, -1) });
        julesSubscription.save();

        julesSM.addDocumentSubscription(new VariantKey(document2.getId(), -1, -1));

        julesSubscription = julesSM.getSubscription();
        assertEquals(2, julesSubscription.getSubscribedVariantKeys().length);

        assertEquals(1, emailSubscriptionManager.getAllDocumentEventSubscribers(document1.getId(), -1, -1, new long[] {}).getArray().length);
        assertEquals(1, emailSubscriptionManager.getAllCommentEventSubscribers(document1.getId(), -1, -1, new long[] {}).getArray().length);

        emailSubscriptionManager.addDocumentSubscription(new VariantKey(document1.getId(), -1, -1));
        assertEquals(2, emailSubscriptionManager.getAllDocumentEventSubscribers(document1.getId(), -1, -1, new long[] {}).getArray().length);

        julesSubscription.setSubscribedVariantKeys(new VariantKey[] {});
        julesSubscription.setSubscribedCollectionKeys(new CollectionSubscriptionKey[] { new CollectionSubscriptionKey(collection1.getId(), -1, -1), new CollectionSubscriptionKey(collection2.getId(), -1, -1)});
        julesSubscription.save();

        assertEquals(2, emailSubscriptionManager.getAllDocumentEventSubscribers(document1.getId(), -1, -1, new long[] {collection1.getId(), collection2.getId()}).getArray().length);

        julesSM.addDocumentSubscription(new VariantKey(document1.getId(), -1, -1));
        julesSM.deleteDocumentSubscription(new VariantKey(document1.getId(), -1, -1));

        julesSubscription = julesSM.getSubscription();
        assertEquals(0, julesSubscription.getSubscribedVariantKeys().length);

        emailSubscriptionManager.addDocumentSubscription(jules.getId(), new VariantKey(document1.getId(), -1, -1));
        emailSubscriptionManager.addDocumentSubscription(jules.getId(), new VariantKey(document2.getId(), -1, -1));
        julesSubscription = julesSM.getSubscription();
        assertEquals(2, julesSubscription.getSubscribedVariantKeys().length);

        emailSubscriptionManager.deleteAllSubscriptionsForDocument(document1.getId());
        julesSubscription = julesSM.getSubscription();
        assertEquals(1, julesSubscription.getSubscribedVariantKeys().length);

        assertTrue(julesSM.isSubsribed(new VariantKey(document2.getId(), -1, -1)));

        julesSM.deleteSubscription();

        // user with admin privileges should be able to manipulate other users' subscriptions
        julesSubscription = emailSubscriptionManager.getSubscription(jules.getId());
        julesSubscription.setReceiveSchemaEvents(false);
        julesSubscription.save();

        // try subscription deletion
        julesSM.deleteSubscription();


        // try implicit subscription creation when calling addDocumentSubscription
        User jef = repository.getUserManager().createUser("jef");
        jef.addToRole(userRole);
        jef.setDefaultRole(userRole);
        jef.setPassword("jef");
        jef.save();

        Repository jefRepository = repositoryManager.getRepository(new Credentials("jef", "jef"));
        EmailSubscriptionManager jefSM = (EmailSubscriptionManager)jefRepository.getExtension("EmailSubscriptionManager");
        jefSM.addDocumentSubscription(new VariantKey(document1.getId(), -1, -1));

        Subscription jefSubscription = jefSM.getSubscription();
        assertEquals(1, jefSubscription.getSubscribedVariantKeys().length);

        jefSM.deleteSubscription();
        emailSubscriptionManager.deleteSubscription();


        //
        // Test variant-specific subscription things
        //

        // Note: for the subscription manager it doesn't matter whether branches & languages
        // actually exist (nor documents for that matter), so we just use fake numbers here

        // first reset the subscription
        julesSubscription = julesSM.getSubscription();
        julesSubscription.setSubscribedVariantKeys(new VariantKey[] {});
        julesSubscription.setSubscribedCollectionKeys(new CollectionSubscriptionKey[] {});
        julesSubscription.save();

        // add subscription for different languages & branches
        julesSM.addDocumentSubscription(new VariantKey(document1.getId(), 1, 1));
        julesSM.addDocumentSubscription(new VariantKey(document1.getId(), 2, 2));
        julesSM.addDocumentSubscription(new VariantKey(document2.getId(), 1, -1));

        julesSubscription = julesSM.getSubscription();
        HashSet keys = new HashSet(Arrays.asList(julesSubscription.getSubscribedVariantKeys()));
        assertEquals(3, keys.size());
        assertTrue(keys.contains(new VariantKey(document1.getId(), 1, 1)));
        assertTrue(keys.contains(new VariantKey(document1.getId(), 2, 2)));
        assertTrue(keys.contains(new VariantKey(document2.getId(), 1, -1)));

        assertTrue(julesSM.isSubsribed(new VariantKey(document2.getId(), 1, -1)));
        assertTrue(julesSM.isSubsribed(new VariantKey(document1.getId(), 1, 1)));
        assertFalse(julesSM.isSubsribed(new VariantKey(document1.getId(), 1, 2)));

        julesSM.deleteDocumentSubscription(new VariantKey(document1.getId(), 1, 1));
        julesSM.deleteDocumentSubscription(new VariantKey(document1.getId(), 2, 2));
        julesSM.deleteDocumentSubscription(new VariantKey(document2.getId(), 1, -1));

        assertEquals(0, julesSM.getSubscription().getSubscribedVariantKeys().length);

        julesSubscription = julesSM.getSubscription();
        julesSubscription.setReceiveDocumentEvents(true);
        julesSubscription.setSubscribedCollectionKeys(new CollectionSubscriptionKey[] {
            new CollectionSubscriptionKey(collection1.getId(), 1, -1)
        });
        julesSubscription.save();

        assertEquals(1, emailSubscriptionManager.getAllDocumentEventSubscribers(document1.getId(), 1, 123, new long[] {collection1.getId()}).getArray().length);
        assertEquals(0, emailSubscriptionManager.getAllDocumentEventSubscribers(document1.getId(), 2, 1, new long[] {collection1.getId()}).getArray().length);
    }
}
