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

import static org.outerj.daisy.repository.acl.AclActionType.DENY;
import static org.outerj.daisy.repository.acl.AclActionType.DO_NOTHING;
import static org.outerj.daisy.repository.acl.AclActionType.GRANT;
import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_FIELDS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.ALL_PARTS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.DOCUMENT_NAME;
import static org.outerj.daisy.repository.acl.AclDetailPermission.DOCUMENT_TYPE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.FULLTEXT_FRAGMENTS;
import static org.outerj.daisy.repository.acl.AclDetailPermission.FULLTEXT_INDEX;
import static org.outerj.daisy.repository.acl.AclDetailPermission.NON_LIVE;
import static org.outerj.daisy.repository.acl.AclDetailPermission.LIVE_HISTORY;
import static org.outerj.daisy.repository.acl.AclDetailPermission.SUMMARY;
import static org.outerj.daisy.repository.acl.AclDetailPermission.VERSION_META;
import static org.outerj.daisy.repository.acl.AclPermission.DELETE;
import static org.outerj.daisy.repository.acl.AclPermission.PUBLISH;
import static org.outerj.daisy.repository.acl.AclPermission.READ;
import static org.outerj.daisy.repository.acl.AclPermission.WRITE;
import static org.outerj.daisy.repository.acl.AclSubjectType.EVERYONE;
import static org.outerj.daisy.repository.acl.AclSubjectType.OWNER;
import static org.outerj.daisy.repository.acl.AclSubjectType.ROLE;
import static org.outerj.daisy.repository.acl.AclSubjectType.USER;

import java.util.Collections;
import java.util.Locale;

import org.apache.xmlbeans.QNameSet;
import org.outerj.daisy.repository.AccessDetailViolationException;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentReadDeniedException;
import org.outerj.daisy.repository.DocumentWriteDeniedException;
import org.outerj.daisy.repository.LiveHistoryEntry;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.Timeline;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclEntry;
import org.outerj.daisy.repository.acl.AclObject;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerx.daisy.x10.SearchResultDocument;

/**
 * Tests for the AccessManager.
 */
public abstract class AbstractAclTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    private RepositoryManager repositoryManager;
    private Repository testuserRepository;
    private Acl acl;
    private AccessManager accessManager;
    private Role role1;
    private Role role2;
    private User user1;
    private User user2;
    private User user3;
    private Repository user1Repository;
    private Repository user2Repository;
    private Repository user3Repository;

    private Document document1;
    private Role userRole;
    private FieldType stringField;
    private FieldType longField;
    private PartType partType1;
    private PartType partType2;
    private DocumentType documentType;
    private DocumentType documentType2;
    private RepositorySchema schema;
    private Language enLanguage;
    private Branch branch1;
    private DocumentCollection collection1;

    public void testAcl() throws Exception {
        doInit();

        doTestBasicAclManipulation();

        doTestAclEvaluation();

        doTestFilterDocumentTypes();

        doTestReadAccessDetails();

        doTestOnlyAllowSingleAdd();

        doTestWriteAccessDetailsEvaluation();

        doTestConceptualEvaluation();

        doTestWriteAccessDetailsEnforcement();
        
        doTestConcurrentUpdateWithPartialWriteAccess();

        doTestAccessDetailsOnVariantCreation();

        doTestVersionMetaDataUpdate();

        doTestNothingLostOnPartialAccess();

        doTestAccessDetailViolationExceptions();

        // Note: the QueryTest implicitely also contains some ACL testing due
        // to the ACL-based filtering of resultsets, which is tested over there.
    }
    
    private void doInit() throws Exception {
        repositoryManager = getRepositoryManager();

        testuserRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        testuserRepository.switchRole(Role.ADMINISTRATOR);

        // Create a document type
        schema = testuserRepository.getRepositorySchema();

        stringField = schema.createFieldType("StringField", ValueType.STRING);
        stringField.setAclAllowed(true);
        stringField.save();
        longField = schema.createFieldType("LongField", ValueType.LONG);
        longField.save();

        partType1 = schema.createPartType("TestPart1", "");
        partType1.setDaisyHtml(true);
        partType1.save();

        partType2 = schema.createPartType("TestPart2", "");
        partType2.setDaisyHtml(true);
        partType2.save();

        documentType = schema.createDocumentType("acltesttype");
        documentType.addFieldType(stringField, false);
        documentType.addFieldType(longField, false);
        documentType.addPartType(partType1, false);
        documentType.addPartType(partType2, false);
        documentType.save();

        documentType2 = schema.createDocumentType("acltesttype2");
        documentType2.addFieldType(stringField, false);
        documentType2.addFieldType(longField, false);
        documentType2.addPartType(partType1, false);
        documentType2.addPartType(partType2, false);
        documentType2.save();

        // Create users
        UserManager userManager = testuserRepository.getUserManager();
        userRole = userManager.getRole("User", false);

        role1 = userManager.createRole("role1");
        role1.save();

        role2 = userManager.createRole("role2");
        role2.save();

        user1 = userManager.createUser("user1");
        user1.setPassword("user1");
        user1.addToRole(userRole);
        user1.addToRole(role1);
        user1.addToRole(role2);
        user1.setDefaultRole(userRole);
        user1.save();
        user1Repository = repositoryManager.getRepository(new Credentials("user1", "user1"));

        user2 = userManager.createUser("user2");
        user2.setPassword("user2");
        user2.addToRole(userRole);
        user2.setDefaultRole(userRole);
        user2.save();
        user2Repository = repositoryManager.getRepository(new Credentials("user2", "user2"));

        user3 = userManager.createUser("user3");
        user3.setPassword("user3");
        user3.addToRole(userRole);
        user3.setDefaultRole(userRole);
        user3.save();
        user3Repository = repositoryManager.getRepository(new Credentials("user3", "user3"));


        // Create documents
        document1 = testuserRepository.createDocument("Document1", documentType.getId());
        document1.setField(stringField.getId(), "hello");
        document1.setField(longField.getId(), new Long(55));
        document1.setPart(partType1.getId(), "text/xml", "<html><body><p>Once upon a time.</p></body></html>".getBytes());
        document1.save();

        // Create a branch and language
        branch1 = testuserRepository.getVariantManager().createBranch("work");
        branch1.save();

        enLanguage = testuserRepository.getVariantManager().createLanguage("en");
        enLanguage.save();

        // Create a collection
        collection1 = testuserRepository.getCollectionManager().createCollection("collection1");
        collection1.save();
    }

    private void doTestBasicAclManipulation() throws Exception {
        //
        // First do some tests regarding ACL manipulation
        //

        accessManager = testuserRepository.getAccessManager();
        acl = accessManager.getStagingAcl();
        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        acl.save();

        Acl aclParallel = accessManager.getStagingAcl();
        aclParallel.save();
        try {
            acl.save();
            fail("Saving ACL should have given concurrent modification exception.");
        } catch (Exception e) { /* ignore */ }

        accessManager.copyStagingToLive();
        Acl liveAcl = accessManager.getLiveAcl();

        try {
            liveAcl.save();
            fail("Saving the live ACL should not be possible.");
        } catch (Exception e) { /* ignore */ }

        assertEquals(1, liveAcl.size());

        acl = accessManager.getStagingAcl();
        aclObject = acl.get(0);

        try {
            aclObject.createNewEntry(EVERYONE, 5);
            fail("For subject type EVERYONE, -1 is required as subject value.");
        } catch (Exception e) { /* ignore */ }

        try {
            aclObject.createNewEntry(USER, -1);
            fail("For subject type USER, -1 is not allowed as subject value.");
        } catch (Exception e) { /* ignore */ }

        try {
            aclObject.createNewEntry(ROLE, -1);
            fail("For subject type USER, -1 is not allowed as subject value.");
        } catch (Exception e) { /* ignore */ }

        try {
            AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
            aclEntry.setSubjectValue(5);
            fail("For subject type EVERYONE, 5 is not allowed as subject value.");
        } catch (Exception e) { /* ignore */ }

        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, GRANT);
        assertEquals(DO_NOTHING, aclEntry.get(WRITE));
        assertEquals(DO_NOTHING, aclEntry.get(PUBLISH));
        aclEntry.set(PUBLISH, GRANT);
        assertEquals(GRANT, aclEntry.get(PUBLISH));
        aclEntry.set(DELETE, GRANT);
        assertEquals(GRANT, aclEntry.get(DELETE));
        aclObject.add(aclEntry);

        acl.save();

        // Check that saving staging ACL didn't influence live ACL
        liveAcl = accessManager.getLiveAcl();
        assertEquals(0, liveAcl.get(0).size());

        // check that copying live back to staging works
        accessManager.copyLiveToStaging();
        acl = accessManager.getStagingAcl();
        assertEquals(0, acl.get(0).size());

        // Test object expressions don't work on fields that are not ACL allowed
        aclObject = acl.createNewObject("$LongField = 55");
        acl.add(aclObject);
        acl.save();
        try {
            accessManager.getAclInfoOnStaging(-1, new long[] {-1}, document1.getId());
            fail("Evaluating an ACL containg a check on a non-ACL allowed field should have failed.");
        } catch (RepositoryException e) { /* ignore */ }
    }

    private void doTestAclEvaluation() throws Exception {
        // Overview of the ACL created below:
        //                                  READ   WRITE PUBLISH DELETE
        // true
        //   role=user                       G      G       G      G
        // documentType='acltesttype' and $StringField='ciao'
        //   user=user1                      G      D       D      D
        // documentType='acltesttype' and $StringField='hello'
        //   everyone                        G      D       D      D
        // documentType='acltesttype'
        //   user=user2                      D      G       G      G
        // $StringField='ta'
        //   user=user2                      G      G       D      D
        //   user=user1                      G      G       G      G
        // $StringField='tata'
        //   user=user2                      D      D       D      D
        //   user=user1                      G      G       G      G
        // conceptual='true'
        //   everyone                        G      G

        acl = accessManager.getStagingAcl();
        acl.clear();

        AclObject object = acl.createNewObject("true");
        acl.add(object);
        AclEntry entry = object.createNewEntry(ROLE, userRole.getId());
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, GRANT);
        entry.set(PUBLISH, GRANT);
        entry.set(DELETE, GRANT);

        object = acl.createNewObject("documentType = 'acltesttype' and $StringField='ciao'");
        acl.add(object);
        entry = object.createNewEntry(USER, user1.getId());
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, DENY);
        entry.set(PUBLISH, DENY);
        entry.set(DELETE, DENY);

        object = acl.createNewObject("documentType = 'acltesttype' and $StringField='hello'");
        acl.add(object);
        entry = object.createNewEntry(EVERYONE, -1);
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, DENY);
        entry.set(PUBLISH, DENY);
        entry.set(DELETE, DENY);

        object = acl.createNewObject("documentType = 'acltesttype'");
        acl.add(object);
        entry = object.createNewEntry(USER, user2.getId());
        object.add(entry);
        entry.set(READ, DENY);
        entry.set(WRITE, GRANT);
        entry.set(PUBLISH, DENY);
        entry.set(DELETE, DENY);

        object = acl.createNewObject("$StringField = 'ta'");
        acl.add(object);
        entry = object.createNewEntry(USER, user2.getId());
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, GRANT);
        entry.set(PUBLISH, DENY);
        entry.set(DELETE, DENY);
        entry = object.createNewEntry(USER, user1.getId());
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, GRANT);
        entry.set(PUBLISH, GRANT);
        entry.set(DELETE, GRANT);

        object = acl.createNewObject("$StringField = 'tata'");
        acl.add(object);
        entry = object.createNewEntry(USER, user2.getId());
        object.add(entry);
        AccessDetails accessDetails = entry.createNewDetails();
        accessDetails.set(NON_LIVE, DENY);
        accessDetails.set(LIVE_HISTORY, DENY);
        entry.set(READ, GRANT, accessDetails);
        entry.set(WRITE, DENY);
        entry.set(PUBLISH, DENY);
        entry.set(DELETE, DENY);
        entry = object.createNewEntry(USER, user1.getId());
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, GRANT);
        entry.set(PUBLISH, GRANT);
        entry.set(DELETE, GRANT);

        object = acl.createNewObject("conceptual = 'true'");
        acl.add(object);
        entry = object.createNewEntry(EVERYONE, -1);
        object.add(entry);
        entry.set(READ, GRANT); // can't have write without read permissions
        entry.set(WRITE, GRANT);

        object = acl.createNewObject("true");
        acl.add(object);
        entry = object.createNewEntry(OWNER, -1);
        object.add(entry);
        entry.set(READ, GRANT);
        entry.set(WRITE, GRANT);
        entry.set(DELETE, GRANT);

        acl.save();
        accessManager.copyStagingToLive();

        // owner of the document should have all permissions
        AclResultInfo result = accessManager.getAclInfoOnLive(testuserRepository.getUserId(), new long[] {-1}, document1.getId());
        assertEquals("owner should have read access", result.isAllowed(READ), true);
        assertEquals("owner should have write access", result.isAllowed(WRITE), true);

        // everyone should have read access on the document, but not write access
        result = accessManager.getAclInfoOnLive(-1, new long[] {-1}, document1.getId());
        assertEquals(result.isAllowed(READ), true);
        assertEquals(result.isAllowed(WRITE), false);

        // everyone should have read access on the document, but not write access
        result = accessManager.getAclInfoOnLive(user2.getId(), new long[] {userRole.getId()}, document1.getId());
        assertEquals(result.isAllowed(READ), false);
        assertEquals(result.isAllowed(WRITE), false); // Note that even though write access is granted
                                                                    // in the ACL, write rights can never be more
                                                                    // liberal then read rights.

        // Document owner should be able to retrieve the document
        testuserRepository.getDocument(document1.getId(), true);

        // Test that user2 should not have read access
        try {
            user2Repository.getDocument(document1.getId(), true);
            fail("user 'user2' should not be able to access Document1");
        } catch (RepositoryException e) { /* ignore */ }

        // and verify this is also the case if user2 tries to directly access part data
        try {
            user2Repository.getPartData(document1.getId(), 1, partType1.getId());
        } catch (RepositoryException e) { /* ignore */ }

        Document document2 = user1Repository.createDocument("Document2", documentType.getId());
        document2.setField(stringField.getId(), "hi");
        document2.setField(longField.getId(), new Long(55));
        document2.save();

        // User 'user1' should be able to save a document having $StringField='ciao' IF HE IS THE OWNER
        document2 = user1Repository.getDocument(document2.getId(), true);
        document2.setField("StringField", "ciao");
        document2.save();

        // User 'user1' should not be able to save a document having $StringField='ciao'
        Document document3 = testuserRepository.createDocument("Document3", documentType.getId());
        document3.save();
        document3 = user1Repository.getDocument(document3.getId(), true);
        document3.save(); // verifies that saving under normal conditions would work
        try {
            document3.setField("StringField", "ciao");
            document3.save();
            fail("Saving document3 by user 'user1' should have failed.");
        } catch (RepositoryException e) { /* ignore */ }

        // test that a new document cannot be created if the user himself would not have
        // access to it (even if he is the owner, since the owner-rules only apply
        // for existing documents)
        Document document4 = user1Repository.createDocument("Document4", documentType.getId());
        try {
            document4.setField("StringField", "ciao");
            document4.save();
            fail("Saving a new document (document4) to which the user would not have access should fail.");
        } catch (RepositoryException e) { /* ignore */ }


        // Adding comments only works if you have read access
        try {
            user2Repository.getCommentManager().addComment(document1.getId(), CommentVisibility.PUBLIC, "hello");
            fail("Adding a comment should have failed because user2 does not have read access.");
        } catch (Exception e) { /* ignore */ }

        user1Repository.getCommentManager().addComment(document1.getId(), CommentVisibility.PUBLIC, "hello");


        // Test private flag
        Document document5 = user1Repository.createDocument("Document5", documentType.getId());
        document5.setPrivate(true);
        document5.save();

        // owner should be able to access private document
        user1Repository.getDocument(document5.getId(), true);
        // admin should be able to access private document
        testuserRepository.getDocument(document5.getId(), true);

        Document document6 = testuserRepository.createDocument("Document6", documentType.getId());
        document6.setPrivate(true);
        document6.save();
        try {
            user1Repository.getDocument(document6.getId(), true);
            fail("user1 should not have access to a private document of someone else.");
        } catch (Exception e) { /* ignore */ }

        Document document7 = user2Repository.createDocument("Document7", documentType.getId());
        document7.setOwner(user1.getId()); // avoid the owner rules apply
        document7.setField("StringField", "ta");
        document7.setNewVersionState(VersionState.PUBLISH);
        document7.save();
        document7 = user2Repository.getDocument(document7.getId(), true);
        assertTrue("user2 can't put publish document", document7.getLastVersion().getState() == VersionState.DRAFT);

        testuserRepository.getAccessManager().getAclInfoOnLive(user2Repository.getUserId(), user2Repository.getActiveRoleIds(), document7.getId());

        try {
            Version doc7Last = document7.getLastVersion();
            doc7Last.setState(VersionState.PUBLISH);
            doc7Last.save();
            fail("Setting state to publish should have failed.");
        } catch (Exception e) { /* ignore */ }

        Document document7user1 = user1Repository.getDocument(document7.getId(), true);
        try {
            Version doc7last = document7user1.getLastVersion();
            doc7last.setState(VersionState.PUBLISH);
            doc7last.save();
        } catch (Exception e) { /* ignore */ }

        assertTrue(user2Repository.getAccessManager().getAclInfoOnLive(user2.getId(), user2Repository.getActiveRoleIds(), document7.getId()).getActionType(DELETE) == DENY);
        try {
            user2Repository.deleteDocument(document7.getId());
            fail("user2 shouldn't be able to delete document.");
        } catch (AccessException e) { /* ignore */ }

        assertTrue(user1Repository.getAccessManager().getAclInfoOnLive(user1.getId(), user1Repository.getActiveRoleIds(), document7.getId()).getActionType(DELETE) == GRANT);
        user1Repository.deleteDocument(document7.getId());

        // test read live access
        // document 8 v1 (historic live), v2 (never live), v3 (current live)
        Document document8 = user1Repository.createDocument("Document8", documentType.getId());
        document8.setField("StringField", "tata");
        document8.save();
        document8.setName("Document8 - 2");
        document8.setNewVersionState(VersionState.DRAFT);
        // make sure that there is at least one second between versions so live history is complete
        Thread.sleep(1000);
        document8.save();

        document8 = user2Repository.getDocument(document8.getId(), false);
        callDisallowedVersionMethods(document8);
        document8 = user2Repository.getDocument(document8.getId(), true);
        callDisallowedVersionMethods(document8);

        document8 = user1Repository.getDocument(document8.getId(), true);
        document8.setField("LongField", new Long(5));
        document8.setNewVersionState(VersionState.PUBLISH);
        // make sure that there is at least one second between versions so live history is complete
        Thread.sleep(1000);
        document8.save();

        document8 = user2Repository.getDocument(document8.getId(), true);

        // Getting data from the live version should succeed.
        document8.getLiveVersion().getField("StringField");
        // Last version is live version, so this should work too
        //  However, this currently does not work in the remote implementation, since the implementation depends on loading the full version list
        // document8.getLastVersion().getField("StringField");
        
        // As of daisy 2.4 'read live' applies to every version available in the live version history, not just to the 'most recent published version'
        try {
            assertEquals("an exception should be thrown", document8.getVersion(1));
            fail("getVersion(<historic-live-version>) should fail since the user doesn't have access to LIVE_HISTORY");
        } catch (Exception e) {
            // ok
        }
        try {
            assertEquals("an exception should be thrown", document8.getVersion(2));
            fail("getVersion(<never-live-version>) should fail since the user doesn't have access to NON_LIVE");
        } catch (Exception e) {
            // ok
        }
        
        // ok, access to the live version is granted to user2
        assertEquals("tata", document8.getVersion(3).getField("StringField").getValue());

        // Completely remove the first version from the live history:
        document8 = testuserRepository.getDocument(document8.getId(), true);
        Timeline doc8Timeline = document8.getTimeline();
        for (LiveHistoryEntry lhe: doc8Timeline.getLiveHistory()) {
            if (lhe.getVersionId() == 1) {
                doc8Timeline.deleteLiveHistoryEntry(lhe);
            }
        }
        doc8Timeline.save();
        assertEquals(1, doc8Timeline.getLiveHistory().length);
        assertEquals(1, user2Repository.getDocument(document8.getId(), true).getTimeline().getLiveHistory().length);

        try {
            // Now the 1st version is non-live, so getting it should fail:
            document8 = user2Repository.getDocument(document8.getId(), true);
            document8.getVersion(1).getField("StringField");
            fail("Getting data from non-live version should fail.");
        } catch (RuntimeException e) {
            // ok
        }

        document8 = user1Repository.getDocument(document8.getId(), true);
        document8.setRetired(true);
        document8.save();

        try {
            user2Repository.getDocument(document8.getId(), false);
            fail("Getting live version of retired document should fail if user has only read live permission");
        } catch (DocumentReadDeniedException e) { /* ignore */ }

        try {
            user2Repository.getDocument(document8.getId(), true);
            fail("Getting live version of retired document should fail if user has only read live permission");
        } catch (DocumentReadDeniedException e) { /* ignore */ }
    }

    private void doTestFilterDocumentTypes() throws Exception {
        //
        // Test filtering of document types
        //

        // start by creating some document types
        DocumentType documentTypeA = schema.createDocumentType("A");
        documentTypeA.save();
        DocumentType documentTypeB = schema.createDocumentType("B");
        documentTypeB.save();
        DocumentType documentTypeC = schema.createDocumentType("C");
        documentTypeC.save();

        // and collection
        DocumentCollection boeCollection = testuserRepository.getCollectionManager().createCollection("boe");
        boeCollection.save();
        DocumentCollection baaCollection = testuserRepository.getCollectionManager().createCollection("baa");
        baaCollection.save();

        // Create an acl

        // Overview of the ACL created below:
        //                                  READ   WRITE
        // documentType = 'A'
        //   everyone                       G      G
        //
        // documentType = 'B' and ($StringField = 'hello' or conceptual='true')
        //   everyone                       G      G
        //   user1                          D      D
        //
        // documentType = 'C' and branch = 'main'
        //   everyone                       D      D

        acl = accessManager.getStagingAcl();
        acl.clear();

        AclObject aclObject = acl.createNewObject("documentType = 'A'");
        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("documentType = 'B' and ($StringField = 'hello' or conceptual='true')");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, DENY);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("documentType = 'C' and branch = 'main'");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, DENY);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        acl.save();
        accessManager.copyStagingToLive();

        long[] filteredDocTypes = user1Repository.getAccessManager().filterDocumentTypes(
                new long[] {documentTypeA.getId(), documentTypeB.getId(), documentTypeC.getId()}, -1, 1, 1);
        assertEquals(1, filteredDocTypes.length);
        assertEquals(documentTypeA.getId(), filteredDocTypes[0]);

        filteredDocTypes = user2Repository.getAccessManager().filterDocumentTypes(
                new long[] {documentTypeA.getId(), documentTypeB.getId(), documentTypeC.getId()}, -1, 1, 1);
        assertEquals(2, filteredDocTypes.length);
        assertEquals(documentTypeA.getId(), filteredDocTypes[0]);
        assertEquals(documentTypeB.getId(), filteredDocTypes[1]);

        // Test filtering with InCollection expressions
        // define a new ACL first

        // Overview of the ACL created below:
        //                                  READ   WRITE
        // InCollection('boe') or conceptual='true'
        //   everyone                       D      D
        //   role=user                      G      G
        //
        // InCollection('baa')
        //   everyone                       D      D

        acl.clear();

        aclObject = acl.createNewObject("InCollection('boe') or conceptual='true'");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, DENY);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);
        aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("InCollection('baa')");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, DENY);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        acl.save();
        accessManager.copyStagingToLive();

        filteredDocTypes = user1Repository.getAccessManager().filterDocumentTypes(
                new long[] {documentTypeA.getId()}, boeCollection.getId(), 1, 1);
        assertEquals(1, filteredDocTypes.length);
        assertEquals(documentTypeA.getId(), filteredDocTypes[0]);

        filteredDocTypes = user1Repository.getAccessManager().filterDocumentTypes(
                new long[] {documentTypeA.getId()}, baaCollection.getId(), 1, 1);
        assertEquals(0, filteredDocTypes.length);
    }

    private void doTestReadAccessDetails() throws Exception {
        //
        // Test fine-grained read permissions (AccessDetails)
        //
        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        AccessDetails accessDetails = aclEntry.createNewDetails();
        try {
            accessDetails.addAccessibleField("foo");
            fail("Should not be able to add an accessible field when all fields permission is not denied");
        } catch (Exception e) { /* ignore */ }
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("foo");
        try {
            accessDetails.addAccessiblePart("bar");
            fail("Should not be able to add an accessible part when all parts permission is not denied");
        } catch (Exception e) { /* ignore */ }
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("bar");
        accessDetails.set(NON_LIVE, DENY);
        accessDetails.set(LIVE_HISTORY, DENY);
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        acl.save();

        // reload ACL to check everything was saved correctly
        acl = accessManager.getStagingAcl();
        accessDetails = acl.get(0).get(0).getDetails(READ);
        assertTrue(accessDetails.getAccessibleFields().equals(Collections.singleton("foo")));
        assertTrue(accessDetails.getAccessibleParts().equals(Collections.singleton("bar")));
        assertTrue(accessDetails.liveOnly());
        assertEquals(DENY, accessDetails.get(NON_LIVE));
        assertEquals(DENY, accessDetails.get(LIVE_HISTORY));

        AclResultInfo info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());
        accessDetails = info.getAccessDetails(READ);
        assertTrue(accessDetails.getAccessibleFields().equals(Collections.singleton("foo")));
        assertTrue(accessDetails.getAccessibleParts().equals(Collections.singleton("bar")));
        assertEquals(DENY, accessDetails.get(NON_LIVE));

        // Test that older AccessDetails are combined/overwritten by newer ones
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails();
        aclEntry.set(READ, GRANT, accessDetails);
        accessDetails.set(NON_LIVE, DENY);
        accessDetails.set(LIVE_HISTORY, DENY);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessibleField("foo");
        accessDetails.addAccessiblePart("bar");
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(NON_LIVE, DENY);
        accessDetails.set(LIVE_HISTORY, DENY);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessibleField("foo2");
        accessDetails.addAccessiblePart("bar2");
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        acl.save();

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());
        accessDetails = info.getAccessDetails(READ);
        assertTrue(accessDetails.getAccessibleFields().contains("foo"));
        assertTrue(accessDetails.getAccessibleFields().contains("foo2"));
        assertTrue(accessDetails.getAccessibleParts().contains("bar"));
        assertTrue(accessDetails.getAccessibleParts().contains("bar2"));
        assertEquals(DENY, accessDetails.get(NON_LIVE));
        assertEquals(DENY, accessDetails.get(LIVE_HISTORY));

        // Test giving back full access
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessibleField("foo");
        accessDetails.addAccessiblePart("bar");
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, GRANT); // granting without details == all details granted
        aclObject.add(aclEntry);

        acl.save();

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());
        accessDetails = info.getAccessDetails(READ);
        assertTrue(accessDetails.isFullAccess());

        // test combining (merging) AccessDetails granted by different roles
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(ROLE, role1.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("foo");
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(ROLE, role2.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("bar");
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        acl.save();

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), new long[] { role1.getId() }, document1.getVariantKey());
        assertTrue(info.getAccessDetails(READ).canAccessField("foo"));
        assertFalse(info.getAccessDetails(READ).canAccessField("bar"));

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), new long[] { role2.getId() }, document1.getVariantKey());
        assertNotNull(info.getAccessDetails(READ));
        assertFalse(info.getAccessDetails(READ).canAccessField("foo"));
        assertTrue(info.getAccessDetails(READ).canAccessField("bar"));

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), new long[] { role1.getId(), role2.getId() }, document1.getVariantKey());
        assertTrue(info.getAccessDetails(READ).canAccessField("foo"));
        assertTrue(info.getAccessDetails(READ).canAccessField("bar"));

        // Verify that 'grant' is taken as default (= initial situation) for the detail permissions.
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclEntry.set(READ, GRANT);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(ALL_PARTS, DENY);
        aclEntry.set(READ, GRANT, accessDetails);
        aclObject.add(aclEntry);

        acl.save();
        accessManager.copyStagingToLive();

        AclResultInfo result = accessManager.getAclInfoOnLive(user1.getId(), new long[] { role1.getId() }, document1.getVariantKey());
        // since we didn't say anything about the non-live, it should be granted.
        assertEquals(GRANT, result.getAccessDetails(READ).get(NON_LIVE));
        // since we didn't say anything about the live history, it should be granted.
        assertEquals(GRANT, result.getAccessDetails(READ).get(LIVE_HISTORY));
        assertEquals(DENY, result.getAccessDetails(READ).get(ALL_PARTS));

        // Test actual protection
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.set(ALL_PARTS, DENY);
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);

        acl.save();

        accessManager.copyStagingToLive();

        Document document9 = testuserRepository.createDocument("Document9", documentType.getId());
        document9.setField("StringField", "live field");
        document9.setPart(partType1.getId(), "text/xml", "<html><body>live part</body></html>".getBytes("UTF-8"));
        document9.save();
        Thread.sleep(1000);
        document9.setField("StringField", "last field");
        document9.setPart(partType1.getId(), "text/xml", "<html><body>last part</body></html>".getBytes("UTF-8"));
        document9.setNewVersionState(VersionState.DRAFT);
        document9.save();

        assertTrue(document9.hasField("StringField"));
        assertTrue(document9.getLastVersion().hasField("StringField"));
        assertEquals(1, document9.getXml().getDocument().getFields().getFieldList().size());

        info = user2Repository.getAccessManager().getAclInfoOnLive(user2.getId(), user2Repository.getActiveRoleIds(), document9.getVariantKey());
        assertEquals(GRANT, info.getActionType(READ));
        assertNotNull(info.getAccessDetails(READ));
        document9 = user2Repository.getDocument(document9.getVariantKey(), true);
        assertFalse(document9.hasField("StringField"));
        assertFalse(document9.getLastVersion().hasField("StringField"));
        assertEquals(0, document9.getXml().getDocument().getFields().getFieldList().size());

        // Test values are missing in query results too
        SearchResultDocument searchResult = user2Repository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "'", Locale.US);
        assertEquals("", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1));
        assertTrue(searchResult.getSearchResult().getRows().getRowList().get(0).getAccess().contains("restrictedRead"));

        searchResult = testuserRepository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "'", Locale.US);
        assertEquals("live field", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1));
        searchResult = testuserRepository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "' option point_in_time='last'", Locale.US);
        assertEquals("last field", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1));

        searchResult = user2Repository.getQueryManager().performQuery("select name, %TestPart1.mimeType, %TestPart1.size, %TestPart1.content where id = '" + document9.getId() + "'", Locale.US);
        assertEquals("", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1));
        assertEquals("", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(2));
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().get(0).getXmlValueArray(0).selectChildren(QNameSet.ALL).length);

        // When the where clause includes a test on a non-readable field, the result row should be excluded
        searchResult = user2Repository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "' and $StringField = 'live field'", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        searchResult = user2Repository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "' and %TestPart1.size > 0", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        searchResult = user2Repository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "' and %TestPart1.mimeType = 'text/xml'", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        // Same when the field is part of a dereference
        searchResult = user2Repository.getQueryManager().performQuery("select name, $StringField where id = '" + document9.getId() + "' and link=>link=>$StringField = 'live field'", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        // test deny summary and fulltext access

        System.err.println("Sleeping a little while to give fulltextinder time to do its job.");
        Thread.sleep(10000);

        // first test they are accessible right now
        searchResult = user2Repository.getQueryManager().performQuery("select name where FullText('live part') and id= '" + document9.getId() + "'", Locale.US);
        assertEquals(1, searchResult.getSearchResult().getRows().getRowList().size());
        searchResult = user2Repository.getQueryManager().performQuery("select name where FullText('+last +part') and id= '" + document9.getId() + "' option point_in_time='last'", Locale.US);
        // 0 because non-live versions are not indexed 
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        searchResult = user2Repository.getQueryManager().performQuery("select name, summary where id = '" + document9.getId() + "'", Locale.US);
        assertTrue(searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1).length() > 0);

        // update ACL to disallow fulltext and summary access
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(NON_LIVE, GRANT);
        accessDetails.set(LIVE_HISTORY, GRANT);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.set(FULLTEXT_INDEX, DENY);
        accessDetails.set(SUMMARY, DENY);
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        acl.save();
        accessManager.copyStagingToLive();

        // now the summary should be empty
        searchResult = user2Repository.getQueryManager().performQuery("select name, summary where id = '" + document9.getId() + "'", Locale.US);
        assertEquals("", searchResult.getSearchResult().getRows().getRowList().get(0).getValueArray(1));

        // and the fulltext should not return a result
        searchResult = user2Repository.getQueryManager().performQuery("select name where FullText('live part') and id= '" + document9.getId() + "'", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());
        searchResult = user2Repository.getQueryManager().performQuery("select name where FullText('last part') and id= '" + document9.getId() + "' option point_in_time='last'", Locale.US);
        assertEquals(0, searchResult.getSearchResult().getRows().getRowList().size());

        // Document.getSummary() should return "" without read_live permission
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(NON_LIVE, GRANT);
        accessDetails.set(LIVE_HISTORY, GRANT);
        accessDetails.set(SUMMARY, GRANT);
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        acl.save();
        accessManager.copyStagingToLive();

        document9 = user2Repository.getDocument(document9.getVariantKey(), true);
        assertEquals("last part", document9.getSummary());
        
        acl.clear();
        aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        accessDetails = aclEntry.createNewDetails();
        accessDetails.set(NON_LIVE, DENY);
        accessDetails.set(LIVE_HISTORY, DENY);
        accessDetails.set(ALL_FIELDS, GRANT);
        accessDetails.set(SUMMARY, GRANT);
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);
        acl.save();
        accessManager.copyStagingToLive();

        document9 = user2Repository.getDocument(document9.getVariantKey(), true);
        try {
            assertEquals("an exception should be thrown", document9.getSummary());
            fail("getSummary should throw exceptions if there is not read live access, but it did not");
        } catch (RuntimeException re) {
            // ok, expected
        }
        try {
            assertEquals("an exception should be thrown", document9.getField("StringField").getValue());
            fail("getField should throw exceptions if there is no read non-live access, but it did not");
        } catch (RuntimeException re) {
            // ok, expected
        }

    }

    private void doTestOnlyAllowSingleAdd() throws Exception {
        //
        // Test that the same object/entry/details object can only be used once in the ACL object model
        //
        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(ROLE, userRole.getId());
        aclEntry.set(READ, GRANT);
        aclObject.add(aclEntry);

        try {
            aclObject.add(aclEntry);
            fail("Adding the same entry twice should give an error.");
        } catch (RuntimeException e) { /* ignore */ }

        // removing and re-adding should work
        aclObject.remove(0);
        aclObject.add(aclEntry);

        try {
            acl.add(aclObject);
            fail("Adding the same object twice should give an error.");
        } catch (RuntimeException e) { /* ignore */ }

        AccessDetails accessDetails = aclEntry.createNewDetails();
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(READ, GRANT, accessDetails); // should work

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        try {
            aclEntry.set(READ, GRANT, accessDetails);
            fail("Adding the same details twice should give an error.");
        } catch (RuntimeException e) { /* ignore */ }

        acl.save();
    }

    private void doTestWriteAccessDetailsEvaluation() throws Exception {
        //
        // Test fine-grained (partial) write permissions
        //
        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        AccessDetails accessDetails = aclEntry.createNewDetails(WRITE);
        try {
            accessDetails.set(FULLTEXT_FRAGMENTS, DENY);
            fail("Should not be able to set detail permissions which do not apply to the write permission");
        } catch (Exception e) { /* ignore */ }
        accessDetails.set(DOCUMENT_NAME, DENY);
        try {
            AccessDetails accessDetails2 = aclEntry.createNewDetails(READ);
            aclEntry.set(WRITE, GRANT, accessDetails2);
            fail("Should not be able to set read access details for the write permission");
        } catch (Exception e) { /* ignore */ }
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclEntry.set(READ, GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        acl.save();

        // reload ACL to check everything was saved correctly
        acl = accessManager.getStagingAcl();
        accessDetails = acl.get(0).get(0).getDetails(WRITE);
        assertEquals(DENY, accessDetails.get(DOCUMENT_NAME));
        assertEquals(DO_NOTHING, accessDetails.get(DOCUMENT_TYPE));
        assertNull(acl.get(0).get(0).getDetails(READ));

        AclResultInfo info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());
        accessDetails = info.getAccessDetails(WRITE);
        assertEquals(DENY, accessDetails.get(DOCUMENT_NAME));
        assertEquals(GRANT, accessDetails.get(DOCUMENT_TYPE));
        accessDetails = info.getAccessDetails(READ);
        assertTrue(accessDetails.isFullAccess());

        // Test that write access details are limited to what's allowed by the read access details
        acl.clear();
        aclObject = acl.createNewObject("true");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails(READ);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("foo");
        accessDetails.addAccessibleField("bar");
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        acl.save();

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());

        accessDetails = info.getAccessDetails(WRITE);
        assertFalse(accessDetails.isFullAccess());
        assertEquals(DENY, accessDetails.get(ALL_FIELDS));
        assertEquals(GRANT, accessDetails.get(ALL_PARTS));
        assertTrue(accessDetails.getAccessibleFields().contains("foo"));
        assertTrue(accessDetails.getAccessibleFields().contains("bar"));
        assertEquals(2, accessDetails.getAccessibleFields().size());

        accessDetails = info.getAccessDetails(READ);
        assertFalse(accessDetails.isFullAccess());

        // Another test that write access details are limited to what's allowed by the read access details
        acl.clear();
        aclObject = acl.createNewObject("true");
        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        accessDetails = aclEntry.createNewDetails(READ);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("foo");
        accessDetails.addAccessibleField("foo2");
        aclEntry.set(READ, GRANT, accessDetails);
        accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("foo");
        accessDetails.addAccessibleField("bar");
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        acl.save();

        info = user1Repository.getAccessManager().getAclInfoOnStaging(user1.getId(), user1Repository.getActiveRoleIds(), document1.getVariantKey());

        accessDetails = info.getAccessDetails(WRITE);
        assertFalse(accessDetails.isFullAccess());
        assertEquals(DENY, accessDetails.get(ALL_FIELDS));
        assertTrue(accessDetails.getAccessibleFields().contains("foo"));
        assertEquals(1, accessDetails.getAccessibleFields().size());
    }

    private void doTestConceptualEvaluation() throws Exception {
        acl.clear();
        acl.save();
        accessManager.copyStagingToLive();

        AclResultInfo aclInfo = accessManager.getAclInfoOnLiveForConceptualDocument(user1.getId(), user1.getAllRoleIds(), documentType.getId(), 1, 1);
        assertFalse(aclInfo.isAllowed(WRITE));

        AclObject aclObject = acl.createNewObject("conceptual='true' and documentType = '" + documentType.getName() + "'");
        acl.add(aclObject);
        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        AccessDetails accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(DOCUMENT_NAME, DENY);
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclEntry.set(READ, GRANT);
        aclObject.add(aclEntry);
        
        acl.save();
        accessManager.copyStagingToLive();

        aclInfo = accessManager.getAclInfoOnLiveForConceptualDocument(user1.getId(), user1.getAllRoleIds(), documentType.getId(), 1, 1);
        assertTrue(aclInfo.isAllowed(WRITE));
        assertFalse(aclInfo.isFullyAllowed(WRITE));
        assertFalse(aclInfo.getAccessDetails(WRITE).isGranted(DOCUMENT_NAME));
        assertTrue(aclInfo.getAccessDetails(WRITE).isGranted(ALL_FIELDS));
    }

    /**
     * Concurrent updates were not throwing exceptions in case the user has only partial write access.
     * This test exposes that problem.
     * @throws Exception
     */
    public void doTestConcurrentUpdateWithPartialWriteAccess() throws Exception {
        testuserRepository.switchRole(Role.ADMINISTRATOR);
        accessManager = testuserRepository.getAccessManager();
        acl = accessManager.getStagingAcl();
        acl.clear();
        
        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, AclActionType.GRANT);
        AccessDetails writeDetails = aclEntry.createNewDetails(AclPermission.WRITE);
        writeDetails.set(AclDetailPermission.CHANGE_COMMENT, AclActionType.DENY); // this takes away 'full write access' -> a DocumentWriteAccessWrapper is used.
        aclEntry.set(WRITE, AclActionType.GRANT, writeDetails);
        aclEntry.set(PUBLISH, AclActionType.GRANT);
        aclEntry.set(DELETE, AclActionType.GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        
        acl.save();
        accessManager.copyStagingToLive();
        
        String name = "partial write concurrent update doc";
        Document adminDoc = testuserRepository.createDocument(name, documentType.getId());
        adminDoc.setCustomField("info", "version one");
        adminDoc.save(); // version 1;
        
        Document userDoc = user1Repository.getDocument(adminDoc.getVariantKey(), true);

        adminDoc.setCustomField("info", "version two");
        adminDoc.save();
        
        userDoc.setCustomField("info", "concurrent update - version two bis");
        try {
            userDoc.save();
            fail("Expected a concurrent update exception, but got none");
        } catch (Exception e) {
           // ok
        }
    }
    
    public void doTestWriteAccessDetailsEnforcement() throws Exception {
        // Overview of the ACL created below:
        //                                  READ                 WRITE
        // true and conceptual='false'
        //   user1                          G completely         G completely
        //   user2                          G completely         G only StringField
        //   user3                          G only stringfield   G completely
        //
        // $StringField='boe'
        //   user1                          G                    G only TestPart
        //
        // conceptual='true'
        //   user1                          G                    G only TestPart
        //   user2                          G                    D
        //

        acl.clear();

        AclObject aclObject = acl.createNewObject("true and conceptual='false'");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(USER, user2.getId());
        aclEntry.set(READ, GRANT);
        AccessDetails accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("StringField");
        accessDetails.set(ALL_PARTS, DENY);
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(USER, user3.getId());
        accessDetails = aclEntry.createNewDetails(READ);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("StringField");
        accessDetails.set(ALL_PARTS, DENY);
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);

        aclObject = acl.createNewObject("$StringField = 'boe'");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, GRANT);
        accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("TestPart1");
        accessDetails.set(ALL_FIELDS, DENY);
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclObject = acl.createNewObject("conceptual = 'true'");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, GRANT);
        accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("TestPart1");
        accessDetails.set(ALL_FIELDS, DENY);
        aclEntry.set(WRITE, GRANT, accessDetails);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(USER, user2.getId());
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, DENY);
        aclObject.add(aclEntry);

        acl.save();
        accessManager.copyStagingToLive();

        //
        // Creation of a new document
        //

        // We are allowed to set TestPart during creation
        Document doc = user1Repository.createDocument("doc1", documentType.getId());
        doc.setPart("TestPart1", "text/html", "<html></html>".getBytes("UTF-8"));
        doc.save();

        // We are not allowed to set StringField during creation
        doc = user1Repository.createDocument("doc", documentType.getId());
        doc.setField("StringField", "foo");
        try {
            doc.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { /* ignore */ }

        // after removal of the field, it should work
        doc.deleteField("StringField");
        doc.save();

        // User 2 should not be able to create a document, since the conceptual
        // doc doesn't allow it
        doc = user2Repository.createDocument("doc", documentType.getId());
        try {
            doc.save();
        } catch (DocumentWriteDeniedException e) { /* ignore */ }

        // User 3 should not be able to create a document either, since there
        // is no conceptual document rule for it
        doc = user3Repository.createDocument("doc", documentType.getId());
        try {
            doc.save();
        } catch (DocumentWriteDeniedException e) { /* ignore */ }

        //
        // Document updates
        //

        doc = user1Repository.createDocument("doc", documentType.getId());
        doc.setPart("TestPart1", "text/html", "<html></html>".getBytes("UTF-8"));
        doc.save();

        doc = user1Repository.getDocument(doc.getId(), true);

        // At first, user1 should be able to update anything
        doc.setName("Hi");
        doc.setField("StringField", "boe");
        doc.setField("LongField", new Long(55));
        doc.setPart("TestPart1", "text/html", "<html>hi</html>".getBytes("UTF-8"));
        doc.save();

        // Since $StringField is now 'boe', should only be able to update TestPart
        try {
            doc.setField("StringField", "bah");
            doc.save();
            fail("exception expected");
        } catch (AccessDetailViolationException e) { /* ignore */ }

        doc = user1Repository.getDocument(doc.getId(), true);
        try {
            doc.setField("LongField", new Long(333));
            doc.save();
            fail("exception expected");
        } catch (AccessDetailViolationException e) { /* ignore */ }

        doc = user1Repository.getDocument(doc.getId(), true);
        doc.setPart("TestPart1", "text/html", "<html>hi there</html>".getBytes("UTF-8"));
        doc.save();

        // User 3 should only be able to read or update $StringField
        doc = user3Repository.getDocument(doc.getId(), true);
        assertFalse(doc.hasPart("TestPart1"));
        assertEquals(0, doc.getPartsInOrder().getArray().length);
        assertFalse(doc.hasField("LongField"));
        assertTrue(doc.hasField("StringField"));
        assertEquals(1, doc.getFieldsInOrder().getArray().length);

        try {
            doc.setField("LongField", new Long(2323));
            doc.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { /* ignore */ }

        doc = user3Repository.getDocument(doc.getId(), true);
        try {
            doc.setPart("TestPart1", "text/html", "<html>hi there again</html>".getBytes("UTF-8"));
            doc.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { /* ignore */ }

        doc = user3Repository.getDocument(doc.getId(), true);
        doc.setField("StringField", "bah");
        doc.save();

        // check document object was updated after save
        assertEquals(user3.getId(), doc.getVariantLastModifier());

        // Assure that after update by someone with partial read (and write) access,
        // the document still contains the non-readable content
        doc = user1Repository.getDocument(doc.getId(), true);
        assertTrue(doc.hasField("StringField"));
        assertTrue(doc.hasField("LongField"));
        assertTrue(doc.hasPart("TestPart1"));
    }

    /**
     * Tests that when creating a variant, only those things to which one has
     * read access on the source variant, and write access on the target variant,
     * are part of the new variant.
     */
    public void doTestAccessDetailsOnVariantCreation() throws Exception {
        // Overview of the ACL created below:
        //                                  READ                 WRITE
        // true
        //   user1                          G completely         G completely
        //   user2                          G only StringField   G only LongField
        //                                     and TestPart         and TestPart

        acl.clear();

        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(USER, user2.getId());
        aclObject.add(aclEntry);

        AccessDetails accessDetails = aclEntry.createNewDetails(READ);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("StringField");
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("TestPart1");
        aclEntry.set(READ, GRANT, accessDetails);

        accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("LongField");
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("TestPart1");
        aclEntry.set(WRITE, GRANT, accessDetails);

        acl.save();
        accessManager.copyStagingToLive();

        Document doc = user1Repository.createDocument("doc", documentType.getId());
        doc.setField("StringField", "boe");
        doc.setField("LongField", new Long(11));
        doc.setPart("TestPart1", "text/html", "<html>qwerty</html>".getBytes("UTF-8"));
        doc.save();

        Document doc2 = user2Repository.createVariant(doc.getId(), 1, 1, 1, branch1.getId(), enLanguage.getId(), true);
        assertFalse(doc2.hasField("StringField"));
        assertFalse(doc2.hasField("LongField"));
        assertTrue(doc2.hasPart("TestPart1"));
    }

    public void doTestVersionMetaDataUpdate() throws Exception {
        acl.clear();

        //
        // First case: both metadata update and publish allowed
        //
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);
        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclEntry.set(PUBLISH, GRANT);

        acl.save();
        accessManager.copyStagingToLive();

        Document document = user1Repository.createDocument("doc", documentType.getId());
        document.save();

        Version version = document.getVersion(1);

        assertNull(version.getChangeComment());
        assertEquals(VersionState.PUBLISH, version.getState());

        version.setChangeComment("comment1");
        version.setState(VersionState.DRAFT);
        version.save();

        //
        // Second case: metadata update not allowed, publish allowed
        //
        AccessDetails accessDetails = aclEntry.createNewDetails(WRITE);
        accessDetails.set(VERSION_META, DENY);
        aclEntry.set(WRITE, GRANT, accessDetails);
        acl.save();
        accessManager.copyStagingToLive();

        version = document.getVersion(1);

        version.setChangeComment("comment2");
        try {
            version.save();
            fail("expected exception");
        } catch (AccessException e) { /* ignore */}

        version.setChangeComment("comment1");
        version.setState(VersionState.PUBLISH);
        version.save();

        //
        // Third case: metadata update allowed, publish not
        //
        accessDetails.set(VERSION_META, GRANT);
        aclEntry.set(PUBLISH, DENY);
        acl.save();
        accessManager.copyStagingToLive();

        version = document.getVersion(1);

        version.setChangeComment("comment3");
        version.save();

        version.setState(VersionState.DRAFT);
        try {
            version.save();
            fail("expected exception");
        } catch (AccessException e) { /* ignore */ }
    }

    /**
     * When one has only partial read access to a document, the repository
     * will give you a document object with only readable things in it,
     * and when saving the document, will internally copy this data to
     * a complete document object. This method verifies that all properties
     * are correctly copied over.
     */
    public void doTestNothingLostOnPartialAccess() throws Exception {
        acl.clear();

        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclObject.add(aclEntry);
        AccessDetails accessDetails = aclEntry.createNewDetails(READ);
        accessDetails.set(ALL_FIELDS, DENY);
        accessDetails.addAccessibleField("StringField");
        accessDetails.set(ALL_PARTS, DENY);
        accessDetails.addAccessiblePart("TestPart1");
        aclEntry.set(READ, GRANT, accessDetails);
        aclEntry.set(WRITE, GRANT);

        aclEntry = aclObject.createNewEntry(USER, user2.getId());
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);

        acl.save();
        accessManager.copyStagingToLive();

        //
        // Create a document
        //
        Document document = user2Repository.createDocument("doc1", documentType.getId());
        document.setField(stringField.getId(), "value1");
        document.setField(longField.getId(), new Long(1));
        document.setPart(partType1.getId(), "text/html", "<html>1.1</html>".getBytes("UTF-8"));
        document.setPart(partType2.getId(), "text/html", "<html>2.1</html>".getBytes("UTF-8"));
        document.setOwner(user1.getId());
        document.save();

        // create a language variant so that we can let the synced-with pointer point to it
        user2Repository.createVariant(document.getId(), 1, 1, 1, 1, enLanguage.getId(), true);

        //
        // Update the document using a user who has only partial read access
        //
        document = user1Repository.getDocument(document.getId(), true);
        assertFalse(document.hasField(longField.getId()));
        assertFalse(document.hasPart(partType2.getId()));

        // versioned content
        document.setField(stringField.getId(), "value2");
        document.setPart(partType1.getId(), "text/html", "<html>1.2</html>".getBytes("UTF-8"));
        document.setName("doc2");
        document.addLink("link 1", "http://link1");

        // non-versioned content
        document.setCustomField("custom field 1", "value1");
        document.changeDocumentType(documentType2.getId());
        document.setRetired(true);

        document.addToCollection(collection1);

        // document-level properties (variant-shared properties)
        document.setOwner(user2.getId());
        document.setReferenceLanguage(enLanguage.getName());

        // new version properties
        document.setNewChangeComment("comment1");
        document.setNewChangeType(ChangeType.MINOR); // major is default
        document.setNewVersionState(VersionState.DRAFT); // publish is default
        document.setNewSyncedWith(enLanguage.getId(), 1);

        document.save();

        //
        // Load document again, check everything is there
        //
        document = user2Repository.getDocument(document.getId(), true);

        // versioned content
        assertEquals("value2", document.getField(stringField.getId()).getValue());
        assertEquals(new Long(1), document.getField(longField.getId()).getValue());
        assertEquals("<html>1.2</html>", new String(document.getPart(partType1.getId()).getData(), "UTF-8"));
        assertEquals("<html>2.1</html>", new String(document.getPart(partType2.getId()).getData(), "UTF-8"));

        assertEquals("doc2", document.getName());
        assertEquals("link 1", document.getLinks().getArray()[0].getTitle());
        assertEquals("http://link1", document.getLinks().getArray()[0].getTarget());

        // non-versioned content
        assertEquals("value1", document.getCustomField("custom field 1"));
        assertEquals(documentType2.getId(), document.getDocumentTypeId());
        assertEquals("collection1", document.getCollections().getArray()[0].getName());
        assertEquals(true, document.isRetired());

        // document-level properties
        assertEquals(user2.getId(), document.getOwner());
        assertEquals(enLanguage.getId(), document.getReferenceLanguageId());

        // version properties
        Version version = document.getLastVersion();
        assertEquals("comment1", version.getChangeComment());
        assertEquals(ChangeType.MINOR, version.getChangeType());
        assertEquals(VersionState.DRAFT, version.getState());
        assertEquals(enLanguage.getId(), version.getSyncedWith().getLanguageId());
        assertEquals(1, version.getSyncedWith().getVersionId());
    }

    public void doTestAccessDetailViolationExceptions() throws Exception {
        acl.clear();

        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(USER, user1.getId());
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        AccessDetails accessDetails = aclEntry.createNewDetails(WRITE);
        for (AclDetailPermission perm : AclDetailPermission.values()) {
            if (perm.appliesTo(WRITE))
                accessDetails.set(perm, DENY);
        }
        aclEntry.set(WRITE, GRANT, accessDetails);

        aclEntry = aclObject.createNewEntry(USER, user2.getId());
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclEntry.set(PUBLISH, GRANT);

        acl.save();
        accessManager.copyStagingToLive();

        Document document = user2Repository.createDocument("doc", documentType.getId());
        document.save();

        user2Repository.createVariant(document.getId(), 1, 1, 1, 1, enLanguage.getId(), true);

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setName("doc2"); // need to change something to cause a new version to be created
            document.setNewChangeComment("foobar");
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setName("doc2"); // need to change something to cause a new version to be created
            document.setNewChangeType(ChangeType.MINOR);
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setName("doc2"); // need to change something to cause a new version to be created
            document.setNewSyncedWith(enLanguage.getId(), 1);
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setName("doc2");
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setField("StringField", "value");
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setPart("TestPart1", "text/html", "<html></html>".getBytes("UTF-8"));
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.addLink("foo", "bar");
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setCustomField("foo", "bar");
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.addToCollection(collection1);
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.changeDocumentType(documentType2.getId());
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setRetired(true);
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }

        try {
            document = user1Repository.getDocument(document.getId(), true);
            document.setReferenceLanguage(enLanguage.getName());
            document.save();
            fail("expected exception");
        } catch (AccessDetailViolationException e) { e.getMessage(); }
    }


    private void callDisallowedVersionMethods(Document document) throws Exception {
        document.getLiveVersion();

        try {
            document.getVersions();
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }

        try {
            document.getLastVersion();
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }

        try {
            document.getXml();
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }

        try {
            document.getXml(2);
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }

        try {
            document.getName();
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }

        try {
            document.getField("whatever");
            fail("Getting non-live version data should fail.");
        } catch (RuntimeException e) { /* ignore */ }
    }
}
