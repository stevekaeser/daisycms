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
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.Namespaces;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;

public abstract class AbstractNamespacesTest  extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testNamespaces() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));

        UserManager userManager = repository.getUserManager();
        User testuser = userManager.getUser("testuser", false);

        NamespaceManager namespaceManager = repository.getNamespaceManager();

        // While in non-admin role, registering or unregistering namespaces should not work
        try {
            namespaceManager.registerNamespace("PIEF");
            fail("Expected an exception trying to register a namespace as non-admin.");
        } catch (RepositoryException e) {
        }

        try {
            namespaceManager.registerNamespace("PIEF", "PAF");
            fail("Expected an exception trying to register a namespace as non-admin.");
        } catch (RepositoryException e) {
        }

        try {
            namespaceManager.unregisterNamespace("DSYTEST");
            fail("Expected an exception trying to unregister a namespace as non-admin.");
        } catch (RepositoryException e) {
        }

        repository.switchRole(Role.ADMINISTRATOR);
        // Test getting list of namespaces
        Namespaces namespaces = namespaceManager.getAllNamespaces();
        assertEquals(1, namespaces.getArray().length);

        // Test getting repository's own namespace
        assertEquals("DSYTEST", namespaceManager.getRepositoryNamespace());

        // Test getting specific namespace
        Namespace namespace = namespaceManager.getNamespace("DSYTEST");
        assertEquals(1, namespace.getId());
        assertEquals("DSYTEST", namespace.getName());
        assertNotNull(namespace.getRegisteredOn());

        // Test registering a new namespace
        Namespace namespace1 = namespaceManager.registerNamespace("FOO");
        assertEquals("FOO", namespace1.getName());
        assertNotNull(namespace1.getFingerprint());
        assertTrue(!namespace.getFingerprint().equals(namespace1.getFingerprint())); // could be (and is allowed to be) the same, but should be very unlikely in case of auto-generated fingerprints
        assertEquals(testuser.getId(), namespace1.getRegisteredBy());
        assertTrue(System.currentTimeMillis() - namespace1.getRegisteredOn().getTime() < 5000);

        // Test registering a new namespace with a specifc fingerprint
        Namespace namespace2 = namespaceManager.registerNamespace("BAR", "finger");
        assertEquals("finger", namespace2.getFingerprint());

        // Test getting new namespace
        namespace2 = namespaceManager.getNamespace("BAR");
        assertNotNull(namespace2);

        // Test all namespaces still OK
        assertEquals(3, namespaceManager.getAllNamespaces().getArray().length);

        // Test unregistering a namespace
        namespaceManager.unregisterNamespace("BAR");
        try {
            namespaceManager.getNamespace("BAR");
            fail("Should have gotten an exception when getting non-existing namespace.");
        } catch (NamespaceNotFoundException e) {
        }

        assertEquals(2, namespaceManager.getAllNamespaces().getArray().length);

        // Test registering same namespace twice
        try {
            namespaceManager.registerNamespace("FOO");
            fail("Registering namespace with same name twice should give an exception.");
        } catch (RepositoryException e) {
        }

        // Test registering same namespace twice
        try {
            namespaceManager.registerNamespace("Longggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg");
            fail("Registering namespace with a name over 200 characters should give an exception.");
        } catch (RepositoryException e) {
        }

    }
}
