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

import java.util.List;
import java.util.Locale;

import org.apache.xmlbeans.XmlObject;
import org.jaxen.dom.DOMXPath;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclEntry;
import org.outerj.daisy.repository.acl.AclObject;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclSubjectType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.testsupport.DOMBuilder;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.w3c.dom.Element;

/**
 * Tests the NavigationManager. Of this test there is no
 * distinction between Remote and Local, since there is no
 * remote API implementation of this component (the remote
 * usage is directly using the HTTP interface).
 */
public abstract class AbstractNavigationTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testNavigation() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");

        RepositorySchema schema = repository.getRepositorySchema();

        // Create types required by navigation stuff
        PartType navigationDescriptionType = schema.createPartType("NavigationDescription", "text/xml");
        navigationDescriptionType.save();

        DocumentType navigationType = schema.createDocumentType("Navigation");
        navigationType.addPartType(navigationDescriptionType, true);
        navigationType.save();

        // Create types for other documents
        FieldType stringField = schema.createFieldType("StringField", ValueType.STRING);
        stringField.save();

        DocumentType documentType = schema.createDocumentType("doctype");
        documentType.addFieldType(stringField, false);
        documentType.save();

        // Create a user
        UserManager userManager = repository.getUserManager();
        Role userRole = userManager.getRole("User", false);

        User user1 = userManager.createUser("user1");
        user1.setPassword("user1");
        user1.addToRole(userRole);
        user1.setDefaultRole(userRole);
        user1.save();
        Repository user1Repository = repositoryManager.getRepository(new Credentials("user1", "user1"));
        NavigationManager user1NavigationManager = (NavigationManager)user1Repository.getExtension("NavigationManager");

        // Create an ACL
        AccessManager accessManager = repository.getAccessManager();
        Acl acl = accessManager.getStagingAcl();

        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.GRANT);
        aclEntry.set(AclPermission.DELETE, AclActionType.GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("documentType = 'Navigation'");
        aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        AccessDetails accessDetails = aclEntry.createNewDetails();
        accessDetails.set(AclDetailPermission.NON_LIVE, AclActionType.DENY);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT, accessDetails);
        aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.DENY);
        aclEntry.set(AclPermission.DELETE, AclActionType.DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        acl.save();
        accessManager.copyStagingToLive();

        // Create a collection
        CollectionManager collectionManager = repository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("collection1");
        collection1.save();

        // Create some documents
        Document document1 = repository.createDocument("Document 1", documentType.getId());
        document1.setField(stringField.getId(), "X");
        document1.setPrivate(true);
        document1.addToCollection(collection1);
        document1.save();

        Document document2 = user1Repository.createDocument("Document 2", documentType.getId());
        document2.setField(stringField.getId(), "X");
        document2.addToCollection(collection1);
        document2.save();

        Document document3 = user1Repository.createDocument("Document 3", documentType.getId());
        document3.setField(stringField.getId(), "Y");
        document3.save();

        DOMBuilder domBuilder;
        DOMXPath xpath;
        org.w3c.dom.Document navResult;

        // Create navigation documents
        Document navdoc2 = repository.createDocument("navdoc2", navigationType.getId());
        StringBuilder content2 = new StringBuilder();
        content2.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
        content2.append("  <d:doc id='" + document3.getId() + "'/>");
        content2.append("  <d:query q=\"select $StringField where documentType != 'Navigation' order by $StringField\"/>");
        content2.append("</d:navigationTree>");
        navdoc2.setPart("NavigationDescription", "text/xml", content2.toString().getBytes("UTF-8"));
        navdoc2.save();

        Document navdoc1 = repository.createDocument("navdoc1", navigationType.getId());
        StringBuilder content1 = new StringBuilder();
        content1.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
        content1.append("  <d:collections>");
        content1.append("    <d:collection name='collection1'/>");
        content1.append("  </d:collections>");
        content1.append("  <d:doc id='" + document3.getId() + "'>");
        content1.append("    <d:doc id='" + document3.getId() + "'/>");
        content1.append("    <d:doc id='" + document1.getId() + "'/>");
        content1.append("  </d:doc>");
        content1.append("  <d:group label='My Group' id='mygroup'>");
        content1.append("    <d:group label='My Second Group'>");
        content1.append("      <d:doc id='").append(document3.getId()).append("'/>");
        content1.append("      <d:group label='empty group' id='emptygroup'/>");
        content1.append("    </d:group>");
        content1.append("    <d:group label='My Third Group'>");
        content1.append("      <d:import docId='" + navdoc2.getId() + "'/>");
        content1.append("    </d:group>");
        content1.append("    <d:query q='select name where true'/>");
        content1.append("  </d:group>");
        content1.append("  <d:link url='http://www.google.com' label='Google'>");
        content1.append("    <d:link url='http://www.apache.org' label='Apache'/>");
        content1.append("  </d:link>");
        content1.append("</d:navigationTree>");
        navdoc1.setPart("NavigationDescription", "text/xml", content1.toString().getBytes("UTF-8"));
        navdoc1.save();

        domBuilder = new DOMBuilder();
        user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc1.getVariantKey(), null, false), null, false, false);
        navResult = domBuilder.getDocument();
        xpath = createXPath("/n:navigationTree/n:doc[1]/@id");
        assertEquals(String.valueOf(document3.getId()), xpath.stringValueOf(navResult));
        xpath = createXPath("/n:navigationTree/n:doc[1]/n:doc[1]/@id");
        assertEquals(String.valueOf(document3.getId()), xpath.stringValueOf(navResult));
        xpath = createXPath("/n:navigationTree/n:doc[1]/n:doc[2]/@id");
        assertEquals("", xpath.stringValueOf(navResult));
        xpath = createXPath("/n:navigationTree/n:group[1]/@label");
        assertEquals("My Group", xpath.stringValueOf(navResult));
        xpath = createXPath("count(/n:navigationTree/n:group[1]/n:group[2]/n:doc)");
        assertEquals(3, xpath.numberValueOf(navResult).intValue());
        xpath = createXPath("/n:navigationTree/n:group[1]/n:group[2]/n:doc[2]/@label");
        assertEquals("X", xpath.stringValueOf(navResult));
        xpath = createXPath("count(/n:navigationTree/n:group[1]/n:doc)");
        assertEquals(1, xpath.numberValueOf(navResult).intValue());

        //
        // Check behaviour with recursive imports
        //
        {
            Document navdoc3 = repository.createDocument("navdoc3", navigationType.getId());
            navdoc3.setPart("NavigationDescription", "text/xml", new byte[0]);
            navdoc3.save();

            Document navdoc4 = repository.createDocument("navdoc4", navigationType.getId());
            navdoc4.setPart("NavigationDescription", "text/xml", new byte[0]);
            navdoc4.save();

            Document navdoc5 = repository.createDocument("navdoc5", navigationType.getId());
            navdoc5.setPart("NavigationDescription", "text/xml", new byte[0]);
            navdoc5.save();

        // first test 3 -> 4 -> 4
            StringBuilder content3 = new StringBuilder();
            content3.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content3.append("  <d:import docId='" + navdoc4.getId() + "'/>");
            content3.append("</d:navigationTree>");
            navdoc3.setPart("NavigationDescription", "text/xml", content3.toString().getBytes("UTF-8"));
            navdoc3.save();

            StringBuilder content4 = new StringBuilder();
            content4.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content4.append("  <d:import docId='" + navdoc4.getId() + "'/>");
            content4.append("</d:navigationTree>");
            navdoc4.setPart("NavigationDescription", "text/xml", content4.toString().getBytes("UTF-8"));
            navdoc4.save();

            System.out.println("!!WARNING!! If the test will now hang it is because the detecting of recursive imports does not work.");
            domBuilder = new DOMBuilder();
            user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc3.getVariantKey(), null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(//n:error)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

        // test 3 -> 4 -> 5 -> 3
            content4 = new StringBuilder();
            content4.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content4.append("  <d:import docId='" + navdoc5.getId() + "'/>");
            content4.append("</d:navigationTree>");
            navdoc4.setPart("NavigationDescription", "text/xml", content4.toString().getBytes("UTF-8"));
            navdoc4.save();

            StringBuilder content5 = new StringBuilder();
            content5.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content5.append("  <d:import docId='" + navdoc3.getId() + "'/>");
            content5.append("</d:navigationTree>");
            navdoc5.setPart("NavigationDescription", "text/xml", content5.toString().getBytes("UTF-8"));
            navdoc5.save();

            System.out.println("!!WARNING!! If the test will now hang it is because the detecting of recursive imports does not work.");
            domBuilder = new DOMBuilder();
            user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc3.getVariantKey(), null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(//n:error)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }

        // test 6 -> 6
        {
            Document navdoc6 = repository.createDocument("navdoc6", navigationType.getId());
            navdoc6.setPart("NavigationDescription", "text/xml", new byte[0]);
            navdoc6.save();

            StringBuilder content6 = new StringBuilder();
            content6.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content6.append("  <d:import docId='" + navdoc6.getId() + "'/>");
            content6.append("</d:navigationTree>");
            navdoc6.setPart("NavigationDescription", "text/xml", content6.toString().getBytes("UTF-8"));
            navdoc6.save();

            System.out.println("!!WARNING!! If the test will now hang it is because the detecting of recursive imports does not work.");
            domBuilder = new DOMBuilder();
            user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc6.getVariantKey(), null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(//n:error)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }

        //
        // Test access to navigation tree should fail when in 'last' versionmode and user has
        // no read access to the navigation tree
        //
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.setNewVersionState(VersionState.DRAFT);
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LAST, null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(/n:navigationTree/*)");
            assertEquals(0, xpath.numberValueOf(navResult).intValue());

            domBuilder = new DOMBuilder();
            user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LAST, null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(/n:navigationTree/n:error)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }

        // Dito when the tree one has no access to is imported
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.setNewVersionState(VersionState.DRAFT);
            navdoc.save();

            Document navdocB = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder contentB = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:import docId='").append(navdoc.getId()).append("'/>");
            content.append("</d:navigationTree>");
            navdocB.setPart("NavigationDescription", "text/xml", contentB.toString().getBytes("UTF-8"));
            navdocB.save();

            domBuilder = new DOMBuilder();
            user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdocB.getVariantKey(), VersionMode.LAST, null, false), null, false, false);
            navResult = domBuilder.getDocument();
            xpath = createXPath("count(/n:navigationTree/n:error)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

        }


        //
        // Test cache invalidation on updates
        //

        // update to a document
        document3.setField(stringField.getId(), "Z");
        document3.save();

        domBuilder = new DOMBuilder();
        user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc1.getVariantKey(), null, false), null, false, false);
        navResult = domBuilder.getDocument();

        xpath = createXPath("/n:navigationTree/n:group[1]/n:group[2]/n:doc[3]/@label");
        assertEquals("Z", xpath.stringValueOf(navResult));

        // update to a navigation tree itself

        // with current implementation it doesn't make a difference so skipped this for now

        //
        // Test navigationManager.lookup method
        //

        LookupAlternative[] lookupAlternatives = new LookupAlternative[] { new LookupAlternative("x", collection1.getId(), navdoc1.getVariantKey()) };

        // a request for a group should give a redirect to its first document child
        NavigationLookupResult lookupResult = user1NavigationManager.lookup("/mygroup", -1, -1, lookupAlternatives, false);
        assertTrue(lookupResult.isRedirect());
        assertEquals("/mygroup/g1/3-DSYTEST", lookupResult.getNavigationPath());

        // a request for a non-existing path should give a not found response
        lookupResult = user1NavigationManager.lookup("/abc", -1, -1, lookupAlternatives, false);
        assertTrue(lookupResult.isNotFound());

        // a request for a document ID (does not matter whether the document really exists) that does not occur in the tree
        lookupResult = user1NavigationManager.lookup("/2323", -1, -1, lookupAlternatives, false);
        assertEquals("2323-DSYTEST", lookupResult.getVariantKey().getDocumentId());
        assertEquals("", lookupResult.getNavigationPath());

        // a request for a document ID that does not occur at the given path but does occur
        // at another location in the tree
        lookupResult = user1NavigationManager.lookup("/1", -1, -1, lookupAlternatives, false);
        assertTrue(lookupResult.isRedirect());
        assertEquals("/3-DSYTEST/1-DSYTEST", lookupResult.getNavigationPath());

        // a request for a group node which does not have any document node child
        lookupResult = user1NavigationManager.lookup("/mygroup/g1/emptygroup", -1, -1, lookupAlternatives, false);
        assertTrue(lookupResult.isNotFound());

        // a request for a link node
        lookupResult = user1NavigationManager.lookup("/l1", -1, -1, lookupAlternatives, false);
        assertTrue(lookupResult.isNotFound());

        // a request for a document node that exists at its given path (the most 'normal' situation)
        lookupResult = user1NavigationManager.lookup("/3-DSYTEST/3-DSYTEST", -1, -1, lookupAlternatives, false);
        assertEquals("3-DSYTEST", lookupResult.getVariantKey().getDocumentId());
        assertEquals("/3-DSYTEST/3-DSYTEST", lookupResult.getNavigationPath());

        //
        // Test contextualized trees
        //

        // Only activeDoc, not activePath specified
        domBuilder = new DOMBuilder();
        user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc1.getVariantKey(), null, false), document3.getVariantKey(), false, false);
        navResult = domBuilder.getDocument();

        xpath = createXPath("/n:navigationTree/@selectedPath");
        assertEquals("/" + document3.getId(), xpath.stringValueOf(navResult));

        xpath = createXPath("/n:navigationTree/n:doc[1]/@selected");
        assertEquals("true", xpath.stringValueOf(navResult));

        xpath = createXPath("count(//*[@selected='true'])");
        assertEquals(1, xpath.numberValueOf(navResult).intValue());

        // activePath specified
        String activePath = "/" + document3.getId() + "/" + document3.getId();
        domBuilder = new DOMBuilder();
        user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc1.getVariantKey(), activePath, false), document3.getVariantKey(), false, false);
        navResult = domBuilder.getDocument();

        xpath = createXPath("/n:navigationTree/@selectedPath");
        assertEquals(activePath, xpath.stringValueOf(navResult));

        xpath = createXPath("/n:navigationTree/n:doc[1]/n:doc[1]/@selected");
        assertEquals("true", xpath.stringValueOf(navResult));

        xpath = createXPath("count(//*[@selected='true'])");

        // activePath specified, with a group in the activePath
        assertEquals(2, xpath.numberValueOf(navResult).intValue());
        activePath = "/mygroup/g2/" + document3.getId();
        domBuilder = new DOMBuilder();
        user1NavigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc1.getVariantKey(), activePath, false), document3.getVariantKey(), false, false);
        navResult = domBuilder.getDocument();

        xpath = createXPath("/n:navigationTree/@selectedPath");
        assertEquals(activePath, xpath.stringValueOf(navResult));

        xpath = createXPath("/n:navigationTree/n:group[1]/n:group[2]/@selected");
        assertEquals("true", xpath.stringValueOf(navResult));

        xpath = createXPath("count(//*[@selected='true'])");
        assertEquals(3, xpath.numberValueOf(navResult).intValue());

        //
        // Simple test of nested nodes in query, with useSelectValues=0
        // Includes some testing of the context value resolver (escape syntax)
        //
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:query q='select name where id=&apos;" + document1.getId() + "&apos; or id=&apos;" + document2.getId() + "&apos; order by name' useSelectValues='0'>");
            content.append("    <d:doc id='${documentId}' branch='${branchId}' language='${languageId}' label='${1} boe'/>");
            content.append("    <d:link url='http://www.som$$\\${\\$ewhere?param=${1}${xyz}' label='${1}'/>");
            content.append("  </d:query>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(/n:navigationTree/n:doc)");
            assertEquals(2, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("/n:navigationTree/n:link[1]/@label");
            assertEquals("Document 1", xpath.stringValueOf(navResult));

            xpath = createXPath("/n:navigationTree/n:link[1]/@url");
            assertEquals("http://www.som$$${\\$ewhere?param=Document+1${xyz}", xpath.stringValueOf(navResult));
        }

        // Test query which doesn't return anything
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:query q='select name, ownerLogin where 1=2 order by name'>");
            content.append("    <d:column sortOrder='ascending'/>");
            content.append("    <d:column sortOrder='ascending'/>");
            content.append("    <d:link url='boe' label='boe'/>");
            content.append("  </d:query>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(/n:navigationTree/*)");
            assertEquals(0, xpath.numberValueOf(navResult).intValue());
        }

        //
        // Test nested queries
        //
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:query q='select name, variantLastModified where true order by name' useSelectValues='0'>");
            content.append("    <d:doc id='${documentId}' branch='${branchId}' language='${languageId}' label='${1} ${2}'>");
            content.append("      <d:query q='select name, variantLastModified where variantLastModified=${2} order by name' useSelectValues='0'>");
            content.append("        <d:doc id='${documentId}' branch='${branchId}' language='${languageId}' label='${../1} ${2}'/>");
            content.append("      </d:query>");
            content.append("    </d:doc>");
            content.append("  </d:query>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(/n:navigationTree/n:doc)");
            assertTrue(xpath.numberValueOf(navResult).intValue() > 1);

            // The children of each document node should have the same label as its parent node
            xpath = createXPath("/n:navigationTree/n:doc");
            List docs = xpath.selectNodes(navResult);
            for (int i = 0; i < docs.size(); i++) {
                Element el = (Element)docs.get(i);
                String label = el.getAttribute("label");
                xpath = createXPath("n:doc");
                List subdocs = xpath.selectNodes(el);
                assertTrue(subdocs.size() > 0);
                for (int k = 0; k < subdocs.size(); k++) {
                    assertEquals(label, ((Element)subdocs.get(k)).getAttribute("label"));
                }
            }
        }

        // Test useSelectValues != 0
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:query q='select name, variantLastModified, ownerLogin where id=&apos;" + document1.getId() + "&apos; order by name' useSelectValues='2'/>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(//n:doc)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("count(//n:group)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }

        //
        // Test tree with link fields, hierarchical fields, ...
        //
        {
            FieldType linkField = schema.createFieldType("LinkField", ValueType.LINK);
            linkField.save();

            FieldType hierMvField = schema.createFieldType("HierMvField", ValueType.STRING, true, true);
            hierMvField.save();

            DocumentType documentType2 = schema.createDocumentType("doctype2");
            documentType2.addFieldType(linkField, false);
            documentType2.addFieldType(hierMvField, false);
            documentType2.save();

            Document document10 = repository.createDocument("Document 10", documentType2.getId());
            document10.setField("LinkField", document1.getVariantKey());
            document10.setField("HierMvField", new Object[] {new HierarchyPath(new Object[] {"A", "B"})});
            document10.save();

            Document document11 = repository.createDocument("Document 11", documentType2.getId());
            document11.setField("LinkField", document1.getVariantKey());
            document11.setField("HierMvField", new Object[] {new HierarchyPath(new Object[] {"A", "B"})});
            document11.save();

            Document document12 = repository.createDocument("Document 12", documentType2.getId());
            document12.setField("LinkField", document1.getVariantKey());
            document12.setField("HierMvField", new Object[] {new HierarchyPath(new Object[] {"A"})});
            document12.save();

            Document document13 = repository.createDocument("Document 13", documentType2.getId());
            document13.setField("LinkField", document2.getVariantKey());
            document13.setField("HierMvField", new Object[] {new HierarchyPath(new Object[] {"B"})});
            document13.save();

            // Test building navtree with link field in select clause
            {
                Document navdoc = repository.createDocument("navdoc", navigationType.getId());
                StringBuilder content = new StringBuilder();
                content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
                content.append("  <d:query q='select $LinkField where documentType=&apos;doctype2&apos; order by name'/>");
                content.append("</d:navigationTree>");
                navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
                navdoc.save();

                domBuilder = new DOMBuilder();
                navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
                navResult = domBuilder.getDocument();

                // There's only 2 different values in the link fields, hence just 2 nodes
                xpath = createXPath("count(/n:navigationTree/n:doc)");
                assertEquals(2, xpath.numberValueOf(navResult).intValue());
            }

            {
                Document navdoc = repository.createDocument("navdoc", navigationType.getId());
                StringBuilder content = new StringBuilder();
                content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
                content.append("  <d:query q='select $LinkField, name where documentType=&apos;doctype2&apos; order by name desc' useSelectValues='1'>");
                // more columns then needed, to test that (1) this doesn't confuse the implementation
                // and (2) that the nodes created by the child d:link element are NOT sorted according
                // to this but keep there original insertion order
                content.append("    <d:column sortOrder='ascending'/>");
                content.append("    <d:column sortOrder='ascending'/>");
                content.append("    <d:column sortOrder='ascending'/>");
                content.append("    <d:column sortOrder='ascending'/>");
                content.append("    <d:column sortOrder='ascending'/>");
                content.append("    <d:link url='boe' label='${2}'/>");
                content.append("  </d:query>");
                content.append("</d:navigationTree>");
                navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
                navdoc.save();

                domBuilder = new DOMBuilder();
                navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
                navResult = domBuilder.getDocument();

                // The children of the query element are executed for each query resultset row, thus
                // even if there's only two distinct nodes created by the link field, there's 4 nested link nodes in total
                xpath = createXPath("count(/n:navigationTree/n:doc/n:link)");
                assertEquals(4, xpath.numberValueOf(navResult).intValue());

                // Test sort order is maintained
                xpath = createXPath("/n:navigationTree/n:doc[1]/n:link[1]/@label");
                assertEquals("Document 12", xpath.stringValueOf(navResult));

                xpath = createXPath("/n:navigationTree/n:doc[1]/n:link[3]/@label");
                assertEquals("Document 10", xpath.stringValueOf(navResult));
            }

            {
                Document impnavdoc = repository.createDocument("navdoc", navigationType.getId());
                StringBuilder impcontent = new StringBuilder();
                impcontent.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
                impcontent.append("  <d:link url='http://outerthought.org' label='Outerthought'/>");
                impcontent.append("  <d:link url='http://daisycms.org' label='Daisy CMS'/>");
                impcontent.append("</d:navigationTree>");
                impnavdoc.setPart("NavigationDescription", "text/xml", impcontent.toString().getBytes("UTF-8"));
                impnavdoc.save();

                Document navdoc = repository.createDocument("navdoc", navigationType.getId());
                StringBuilder content = new StringBuilder();
                content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
                content.append("  <d:query q='select $LinkField, $HierMvField where documentType=&apos;doctype2&apos; order by name'>");
                content.append("    <d:import docId='" + impnavdoc.getId() + "'/>");
                content.append("  </d:query>");
                content.append("</d:navigationTree>");
                navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
                navdoc.save();

                domBuilder = new DOMBuilder();
                navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
                navResult = domBuilder.getDocument();

                xpath = createXPath("/n:navigationTree/n:doc[1]/n:group[1]/n:doc[1]/@documentId");
                assertEquals(document10.getId(), xpath.stringValueOf(navResult));

                xpath = createXPath("/n:navigationTree/n:doc[1]/n:group[1]/n:doc[1]/n:link[1]/@label");
                assertEquals("Outerthought", xpath.stringValueOf(navResult));

                xpath = createXPath("/n:navigationTree/n:doc[1]/n:group[1]/n:doc[1]/n:link[2]/@label");
                assertEquals("Daisy CMS", xpath.stringValueOf(navResult));

                xpath = createXPath("/n:navigationTree/n:doc[1]/n:doc[1]/@documentId");
                assertEquals(document12.getId(), xpath.stringValueOf(navResult));

                xpath = createXPath("/n:navigationTree/n:doc[2]/n:doc[1]/@documentId");
                assertEquals(document13.getId(), xpath.stringValueOf(navResult));
            }
        }

        //
        // Test separator node
        //  especially removal of meaningless/disturbing separator nodes
        //
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("  <d:doc id='" + document1.getId() + "'/>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("  <d:doc id='999999999999'/>"); // a non-existing document
            content.append("  <d:separator/>");
            content.append("  <d:doc id='" + document2.getId() + "'/>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("  <d:group label='boe'>"); // a group without visible children
            content.append("    <d:doc id='999999999999'/>"); // a non-existing document
            content.append("  </d:group>");
            content.append("  <d:separator/>");
            content.append("  <d:separator/>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, null, false), null, false, false);
            navResult = domBuilder.getDocument();

            // There should only be one separator between the two document nodes
            xpath = createXPath("count(/n:navigationTree/n:separator)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }

        //
        // Test custom navigation depth
        //
        {
            Document navdoc = repository.createDocument("navdoc", navigationType.getId());
            StringBuilder content = new StringBuilder();
            content.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>");
            content.append("  <d:group id='group1' label='Group 1'>");
            content.append("    <d:doc id='" + document1.getId() + "'>");
            content.append("      <d:doc id='" + document1.getId() + "'>");
            content.append("      </d:doc>");
            content.append("    </d:doc>");
            content.append("  </d:group>");
            content.append("  <d:group id='group2' label='Group 2'>");
            content.append("    <d:doc id='" + document1.getId() + "'>");
            content.append("      <d:doc id='" + document1.getId() + "'>");
            content.append("      </d:doc>");
            content.append("    </d:doc>");
            content.append("  </d:group>");
            content.append("  <d:group id='group3' label='Group 3'>");
            content.append("    <d:doc id='" + document1.getId() + "'>");
            content.append("      <d:doc id='" + document1.getId() + "'>");
            content.append("      </d:doc>");
            content.append("    </d:doc>");
            content.append("  </d:group>");
            content.append("</d:navigationTree>");
            navdoc.setPart("NavigationDescription", "text/xml", content.toString().getBytes("UTF-8"));
            navdoc.save();

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, "/group1/" + document1.getId() + "/" + document1.getId(), true, 2, Locale.US), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group1']//n:doc)");
            assertEquals(2, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group2']//n:doc)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group3']//n:doc)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

            domBuilder = new DOMBuilder();
            navigationManager.generateNavigationTree(domBuilder, new NavigationParams(navdoc.getVariantKey(), VersionMode.LIVE, "/group1/" + document1.getId() + "/" + document1.getId(), false, 2, Locale.US), null, false, false);
            navResult = domBuilder.getDocument();

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group1']//n:doc)");
            assertEquals(2, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group2']//n:doc)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());

            xpath = createXPath("count(/n:navigationTree/n:group[@id='group3']//n:doc)");
            assertEquals(1, xpath.numberValueOf(navResult).intValue());
        }
    }

    private DOMXPath createXPath(String expr) throws Exception {
        DOMXPath xpath = new DOMXPath(expr);
        xpath.addNamespace("n", "http://outerx.org/daisy/1.0#navigation");
        return xpath;
    }

    private void dumpDOM(org.w3c.dom.Document document) throws Exception {
        // small dirty trick for pretty printing
        System.out.println(XmlObject.Factory.parse(document).toString());
    }
}
