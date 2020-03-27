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
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.CommentManager;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.schema.*;

import java.util.*;
import java.math.BigDecimal;

public abstract class AbstractDocumentTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testDocument() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository adminRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        adminRepository.switchRole(Role.ADMINISTRATOR);

        // create a user (without admin rights) which will do all operations
        UserManager userManager = adminRepository.getUserManager();
        Role role = userManager.getRole("User", false);
        User user = userManager.createUser("ordinaryUser");
        user.addToRole(role);
        user.setDefaultRole(role);
        user.setPassword("secret");
        user.save();
        Repository repository = repositoryManager.getRepository(new Credentials("ordinaryUser", "secret"));

        User user2 = userManager.createUser("ordinaryUser2");
        user2.addToRole(role);
        user2.setDefaultRole(role);
        user2.setPassword("secret");
        user2.save();
        Repository user2Repository = repositoryManager.getRepository(new Credentials("ordinaryUser2", "secret"));

        RepositorySchema schema = adminRepository.getRepositorySchema();

        // Create some field types
        FieldType stringField1 = schema.createFieldType("StringField1", ValueType.STRING);
        stringField1.save();
        FieldType dateField1 = schema.createFieldType("DateField1", ValueType.DATE);
        dateField1.save();
        FieldType dateTimeField1 = schema.createFieldType("DateTimeField1", ValueType.DATETIME);
        dateTimeField1.save();
        FieldType decimalField1 = schema.createFieldType("DecimalField1", ValueType.DECIMAL);
        decimalField1.save();
        FieldType doubleField1 = schema.createFieldType("DoubleField1", ValueType.DOUBLE);
        doubleField1.save();
        FieldType longField1 = schema.createFieldType("LongField1", ValueType.LONG);
        longField1.save();
        FieldType booleanField1 = schema.createFieldType("BoolenField1", ValueType.BOOLEAN);
        booleanField1.save();
        FieldType linkField1 = schema.createFieldType("LinkField1", ValueType.LINK);
        linkField1.save();

        // Create some part types
        PartType partType1 = schema.createPartType("PartType1", "text/xml");
        partType1.save();
        PartType partType2 = schema.createPartType("PartType2", "");
        partType2.save();
        PartType partType3 = schema.createPartType("PartType3", "image/gif,image/jpeg");
        partType3.save();

        // Create a documenttype
        DocumentType documentType1 = schema.createDocumentType("DocumentType1");
        documentType1.addFieldType(stringField1, true);
        documentType1.addFieldType(dateField1, true);
        documentType1.addFieldType(dateTimeField1, false);
        documentType1.addFieldType(decimalField1, false);
        documentType1.addFieldType(doubleField1, false);
        documentType1.addFieldType(longField1, false);
        documentType1.addFieldType(booleanField1, false);
        documentType1.addFieldType(linkField1, false);
        documentType1.addPartType(partType1, false);
        documentType1.addPartType(partType2, true);
        documentType1.addPartType(partType3, false);
        documentType1.save();

        // Create a document type without any required fields or parts
        DocumentType documentType2 = schema.createDocumentType("DocumentType2");
        documentType2.addFieldType(stringField1, false);
        documentType2.addFieldType(dateField1, false);
        documentType2.addFieldType(dateTimeField1, false);
        documentType2.addFieldType(decimalField1, false);
        documentType2.addFieldType(doubleField1, false);
        documentType2.addFieldType(longField1, false);
        documentType2.addFieldType(booleanField1, false);
        documentType2.addFieldType(linkField1, false);
        documentType2.addPartType(partType1, false);
        documentType2.addPartType(partType2, false);
        documentType2.addPartType(partType3, false);
        documentType2.save();

        // Create an ACL
        // Note: testing more meaningful ACL's is done by the separate ACL testcase.
        AccessManager accessManager = adminRepository.getAccessManager();
        Acl acl = accessManager.getStagingAcl();
        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.GRANT);
        aclEntry.set(AclPermission.DELETE, AclActionType.GRANT);
        aclObject.add(aclEntry);
        aclEntry = aclObject.createNewEntry(AclSubjectType.USER, user2.getId());
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.DENY);
        aclEntry.set(AclPermission.DELETE, AclActionType.DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);
        acl.save();
        accessManager.copyStagingToLive();

        //
        // Basic document creation
        //
        Document document1 = repository.createDocument("Document 1", documentType1.getId());

        // set a field using, once using id, once using name
        document1.setField(stringField1.getId(), "hello");
        document1.setField("DateField1", new Date());

        // set a part, once using id, once using name
        byte[] part2Data = "meaningful data".getBytes();
        byte[] part3Data = "myimage".getBytes();
        document1.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document1.setPart("PartType3", "image/gif", part3Data);

        document1.save();

        //
        // Test data is correctly saved and loaded
        //
        Document document1Reloaded = repository.getDocument(document1.getId(), true);
        assertEquals("hello", (String)document1Reloaded.getField("StringField1").getValue());
        System.out.println(((Date)document1.getField(dateField1.getId()).getValue()).getTime() + " - " + ((Date)document1Reloaded.getField(dateField1.getId()).getValue()).getTime());
        assertEquals(document1.getField(dateField1.getId()).getValue(), document1Reloaded.getField(dateField1.getId()).getValue());
        assertEquals(part2Data, document1Reloaded.getPart("PartType2").getData());
        assertEquals(part3Data, document1Reloaded.getPart(partType3.getId()).getData());

        //
        // Calling save now on document should not create new version
        //
        document1.save();
        assertEquals(1, document1.getLastVersionId());
        assertEquals(1, document1.getLastVersion().getId());

        //
        // Create document with missing field
        //
        Document document2 = repository.createDocument("Document 2", documentType1.getId());
        document2.setField(stringField1.getId(), "yo");
        document2.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        try {
            document2.save();
            fail("Saving document should have failed due to missing field.");
        } catch (Exception e) {}
        // try saving with disabling validation
        document2.save(false);
        // now set missing field -- should now save successfully
        document2.setField(dateField1.getId(), new Date());
        document2.save(true);
        // meanwhile test that the part isn't changed
        assertEquals(1, document2.getPart(partType2.getId()).getDataChangedInVersion());
        // also not when document is completely reloaded
        assertEquals(1, repository.getDocument(document2.getId(), true).getLastVersion().getPart(partType2.getId()).getDataChangedInVersion());

        document2.deletePart(partType2.getId());
        try {
            document2.save();
            fail("Saving document should have failed due to missing part.");
        } catch (Exception e) {}
        // set missing part -- should now save succesfully
        document2.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document2.save();

        //
        // Test setting new data for a part
        //
        byte[] newPart2Data = "Apples and Oranges".getBytes();
        document2.setPart(partType2.getId(), "application/x-meaningful", newPart2Data);
        document2.save();

        Document document2Reloaded = repository.getDocument(document2.getId(), true);
        assertEquals(newPart2Data, document2Reloaded.getPart(partType2.getId()).getData());
        assertEquals(document2.getLastVersionId(), document2Reloaded.getPart(partType2.getId()).getDataChangedInVersion());

        //
        // Test setting data with incorrect mimetype
        //
        try {
            document2.setPart(partType1.getId(), "image/zarba", part3Data);
            fail("Setting data with incorrect mimetype should fail.");
        } catch (Exception e) {}

        //
        // Test out all types of fields
        //
        Document document3 = repository.createDocument("Document 3", documentType1.getId());
        document3.setField(stringField1.getId(), "hello");
        Date currentDate = getDate(new Date());
        document3.setField(dateField1.getId(), currentDate);
        Date currentDateTime = getDateTime(new Date());
        document3.setField(dateTimeField1.getId(), currentDateTime);
        document3.setField(decimalField1.getId(), new BigDecimal("33.43539"));
        document3.setField(doubleField1.getId(), new Double(343.232d));
        document3.setField(longField1.getId(), new Long(23234235));
        document3.setField(booleanField1.getId(), Boolean.FALSE);
        document3.setField(linkField1.getId(), new VariantKey("23-DSYTEST", 2, 8));
        document3.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document3.save();

        // test field values are same after reloading document
        document3 = repository.getDocument(document3.getId(), true);
        assertEquals(currentDate, document3.getField(dateField1.getId()).getValue());
        assertEquals(currentDateTime, document3.getField(dateTimeField1.getId()).getValue());
        assertEquals(new BigDecimal("33.43539"), document3.getField(decimalField1.getId()).getValue());
        assertEquals(new Double(343.232d), document3.getField(doubleField1.getId()).getValue());
        assertEquals(new Long(23234235), document3.getField(longField1.getId()).getValue());
        assertEquals(Boolean.FALSE, document3.getField(booleanField1.getId()).getValue());
        assertEquals(new VariantKey("23-DSYTEST", 2, 8), document3.getField(linkField1.getId()).getValue());

        //
        // Test setting wrong type of value on a field
        //
        Document document12 = repository.createDocument("Document 12", documentType1.getId());
        try {
            document12.setField(doubleField1.getId(), new Long(123));
            fail("Setting wrong object as field value should give an exception.");
        } catch (Exception e) {}

        //
        // Test retired and private flags
        //
        Document document4 = repository.createDocument("Document 4", documentType1.getId());
        document4.setField(stringField1.getId(), "hello");
        document4.setField("DateField1", new Date());
        document4.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document4.save();
        assertEquals(false, document4.isRetired());
        assertEquals(false, document4.isPrivate());
        document4.setRetired(true);
        document4.setPrivate(true);
        document4.save();
        document4 = repository.getDocument(document4.getId(), false);
        assertEquals(true, document4.isRetired());
        assertEquals(true, document4.isPrivate());

        //
        // Test document cache
        //
        Document document5 = repository.createDocument("Document 5", documentType1.getId());
        document5.setField(stringField1.getId(), "hello");
        document5.setField("DateField1", new Date());
        document5.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document5.save();
        Document document5ReadOnly = repository.getDocument(document5.getId(), false);
        try {
            document5ReadOnly.save();
            fail("Saving document should have failed because it is not modifiable.");
        } catch (Exception e) {}
        // Do a document update and verify that cache has been refreshed
        document5.setField(stringField1.getId(), "hello!");
        document5.save();
        document5ReadOnly = repository.getDocument(document5.getId(), false);
        assertEquals("hello!", (String)document5ReadOnly.getField(stringField1.getId()).getValue());

        //
        // Test comments
        //
        CommentManager commentManager = repository.getCommentManager();
        Document document6 = repository.createDocument("Document 6", documentType2.getId());
        try {
            commentManager.addComment(document6.getId(), CommentVisibility.PUBLIC, "Hi, I have something to say.");
            fail("Adding comment to a non-saved document should fail.");
        } catch (Exception e) {}
        document6.save();
        String comment1Text = "Hi, I have something to say.";
        commentManager.addComment(document6.getId(), CommentVisibility.PUBLIC, comment1Text);
        commentManager.addComment(document6.getId(), CommentVisibility.PUBLIC, "Hi, I have another thing to say.");
        commentManager.addComment(document6.getId(), CommentVisibility.PRIVATE, "This document sucks.");
        commentManager.addComment(document6.getId(), CommentVisibility.EDITORS, "Oh yes.");
        Comment[] comments = commentManager.getComments(document6.getId()).getArray();
        assertEquals(4, comments.length);
        assertEquals(comments[0].getCreatedBy(), user.getId());
        assertEquals(comments[0].getText(), comment1Text);

        assertEquals(4, commentManager.getComments().getArray().length);
        assertEquals(1, commentManager.getComments(CommentVisibility.PRIVATE).getArray().length);
        assertEquals(2, commentManager.getComments(CommentVisibility.PUBLIC).getArray().length);
        assertEquals(1, commentManager.getComments(CommentVisibility.EDITORS).getArray().length);

        // another user should not see the private or editors-only comments
        assertEquals(2, user2Repository.getCommentManager().getComments(document6.getId()).getArray().length);

        // admin user should see all comments except private comments
        assertEquals(3, adminRepository.getCommentManager().getComments(document6.getId()).getArray().length);

        commentManager.addComment(document6.getId(), CommentVisibility.PUBLIC, "This document is o so nice.");
        // first user should see new comment without re-fetching document
        assertEquals(5, commentManager.getComments(document6.getId()).getArray().length);

        // test deleting comments
        commentManager.deleteComment(document6.getId(), comments[0].getId());
        assertEquals(4, commentManager.getComments(document6.getId()).getArray().length);

        try {
            commentManager.deleteComment(document6.getId(), 10);
            fail("Deleting non-existing comment should give exception.");
        } catch (Exception e) {}

        // user without write rights should be able to make a private comment and delete it again
        user2Repository.getCommentManager().addComment(document6.getId(), CommentVisibility.PRIVATE, "This is a comment.");
        Comment[] user2Comments = user2Repository.getCommentManager().getComments(document6.getId()).getArray();
        user2Repository.getCommentManager().deleteComment(document6.getId(), user2Comments[user2Comments.length - 1].getId());

        // ... but should not be able to delete other comments
        try {
            user2Repository.getCommentManager().deleteComment(document6.getId(), user2Comments[user2Comments.length - 2].getId());
            fail("User without write rights should not be able to delete non-private comments.");
        } catch (RepositoryException e) {}

        commentManager.addComment(document6.getId(), CommentVisibility.PRIVATE, "Private comment");
        Comment[] comments2 = commentManager.getComments(document6.getId()).getArray();
        try {
            user2Repository.getCommentManager().deleteComment(document6.getId(), comments2[comments2.length - 1].getId());
            fail("Non-admin user should not be able to delete other users' private comments.");
        } catch (RepositoryException e) {}

        // admin user should be able to delete otehr users' private comments
        adminRepository.getCommentManager().deleteComment(document6.getId(), comments2[comments2.length - 1].getId());

        //
        // Test customfields
        //
        Document document7 = repository.createDocument("Document 7", documentType2.getId());
        document7.save();
        document7.setCustomField("some field", "some value");
        document7.setCustomField("some field 2", "123");
        document7.save();
        document7 = repository.getDocument(document7.getId(), true);
        assertEquals("123", document7.getCustomField("some field 2"));
        document7.deleteCustomField("some field 2");
        assertNull(document7.getCustomField("some field 2"));
        document7.save();



        //
        // Test locks
        //
        Document document8 = repository.createDocument("Document 8", documentType2.getId());
        document8.save();
        LockInfo lockInfo = document8.getLockInfo(false);
        assertEquals(false, lockInfo.hasLock());
        document8.lock(1000000, LockType.PESSIMISTIC);
        // we have a lock, so should be able to save the document
        // TODO this doesn't make sense anymore since saving is never done if document is not modified
        document8.save();
        // another user should not be able to save it...
        Document document8OtherUser = adminRepository.getDocument(document8.getId(), true);
        try {
            document8OtherUser.setCustomField("a", "b");
            document8OtherUser.save();
            fail("Saving document should have failed because lock belongs to another user.");
        } catch (Exception e) {}
        // .. but if we remove the lock, it should work
        document8.releaseLock();
        document8OtherUser.save();
        // test lock expires
        document8.lock(1, LockType.PESSIMISTIC);
        Thread.sleep(1200); // lock creation time is stored only with one-second precision, so take that and some margin
        lockInfo = document8OtherUser.getLockInfo(true);
        assertEquals(false, lockInfo.hasLock());

        // Try to create a never-expiring lock
        document8.lock(-1, LockType.PESSIMISTIC);
        assertEquals(-1, document8.getLockInfo(true).getDuration());

        //
        // Test versions
        //
        Document document9 = repository.createDocument("Document 9", documentType1.getId());
        document9.setField(stringField1.getId(), "hello");
        document9.setField("DateField1", new Date());
        document9.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document9.save();

        Version version = document9.getVersion(1);
        assertEquals(part2Data, version.getPart(partType2.getId()).getData());

        //
        // Test collections
        //
        CollectionManager collectionManager = adminRepository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("Collection1");
        collection1.save();
        DocumentCollection collection2 = collectionManager.createCollection("Collection2");
        collection2.save();
        DocumentCollection collection3 = collectionManager.createCollection("Collection3");
        collection3.save();

        Document document10 = repository.createDocument("Document 10", documentType2.getId());
        document10.addToCollection(collection1);
        document10.save();
        assertEquals(1, document10.getCollections().getArray().length);
        document10 = repository.getDocument(document10.getId(), true);
        assertEquals(1, document10.getCollections().getArray().length);

        document10.addToCollection(collection2);
        document10.addToCollection(collection3);
        document10.save();

        document10 = repository.getDocument(document10.getId(), true);
        DocumentCollection[] collections = document10.getCollections().getArray();
        HashSet names = new HashSet();
        for (int i = 0; i < collections.length; i++) {
            names.add(collections[i].getName());
        }
        assertEquals(true, names.contains("Collection1"));
        assertEquals(true, names.contains("Collection2"));
        assertEquals(true, names.contains("Collection3"));

        // delete a collection still belonging to the document, check it is removed after doc loading
        collectionManager.deleteCollection(collection3.getId());
        document10 = repository.getDocument(document10.getId(), true);
        assertEquals(2, document10.getCollections().getArray().length);

        // remove a collection from the document
        document10.removeFromCollection(collection2);
        document10.save();
        document10 = repository.getDocument(document10.getId(), true);
        assertEquals(1, document10.getCollections().getArray().length);

        // delete a collection still belonging to the document, and then save the document
        //   (the document object has still the collection in it)
        collectionManager.deleteCollection(collection1.getId());
        document10.save();
        document10 = repository.getDocument(document10.getId(), false);
        assertEquals(0, document10.getCollections().getArray().length);

        // All these collection operations shouldn't have created any new versions
        assertEquals(1, document10.getVersions().getArray().length);



        //
        // Test links
        //
        Document document11 = repository.createDocument("Document 11", documentType2.getId());
        document11.addLink("Google", "http://www.google.be");
        document11.save();
        document11 = repository.getDocument(document11.getId(), true);
        assertEquals(1, document11.getLinks().getArray().length);
        document11.clearLinks();
        document11.save();
        document11 = repository.getDocument(document11.getId(), true);
        assertEquals(0, document11.getLinks().getArray().length);


        //
        // Test concurrent modification detection
        //
        Document document20 = repository.createDocument("Document 20", documentType2.getId());
        document20.save();
        Document document20Parallel = repository.getDocument(document20.getId(), true);
        document20Parallel.setCustomField("p", "t");
        document20Parallel.save();
        try {
            document20.setCustomField("r", "s");
            document20.save();
            fail("Saving document should have failed because of concurrent modifications.");
        } catch (Exception e) {};
        
        //
        // Test changing the document type
        //
        {
            Document document30 = repository.createDocument("Document 30", documentType1.getId());
            document30.setField(stringField1.getId(), "value30");
            document30.setField(dateField1.getId(), new Date());
            document30.setPart(partType2.getId(), "application/x-meaningful", part2Data);
            document30.save();
            assertEquals("document type check", documentType1.getId(), document30.getDocumentTypeId());

            FieldType fieldType = schema.createFieldType("MyString", ValueType.STRING);
            fieldType.save();

            // Create a document type with a field which was not present in the previous doc type, and vice versa
            // This in order to check that validation happens against the new document type
            DocumentType newDocType = schema.createDocumentType("Foo");
            newDocType.addFieldType(fieldType, false);
            newDocType.addFieldType(dateField1, false);
            newDocType.save();

            document30.changeDocumentType(newDocType.getId());
            document30.deleteField(stringField1.getId()); // delete field since it is not allowed anymore
            document30.deletePart(partType2.getId()); // dito
            document30.setField("MyString", "hallo hallo");
            document30.save();

            document30 = repository.getDocument(document30.getId(), true);
            assertEquals("document type check", newDocType.getId(), document30.getDocumentTypeId());
        }

        //
        // Test deleting documents
        //
        Document document40 = repository.createDocument("Document 40", documentType1.getId());
        document40.setField(stringField1.getId(), "value40");
        document40.setField(dateField1.getId(), new Date());
        document40.setPart(partType2.getId(), "application/x-meaningful", part2Data);
        document40.save();
        repository.deleteDocument(document40.getId());

        //
        // Test changing owner
        //
        DocumentType simpleDocType = adminRepository.getRepositorySchema().createDocumentType("SimpleDocType");
        simpleDocType.save();
        Document document50 = repository.createDocument("Document 50", "SimpleDocType");
        document50.save();

        // test current owner is able to change role
        document50.setOwner(user2.getId());
        document50.save();

        document50 = repository.getDocument(document50.getId(), false);
        assertEquals(user2.getId(), document50.getOwner());

        // changing role on non-modifiable document object should not be possible
        try {
            document50.setOwner(adminRepository.getUserId());
            fail("Changing role on non-modifiable document object should not be possible.");
        } catch (RuntimeException e) {}

        // test non-admin non-owner is not able to change role
        document50 = repository.getDocument(document50.getId(), true);
        try {
            document50.setOwner(adminRepository.getUserId());
            fail("Ordinary user should not be able to change owner.");
        } catch (RepositoryRuntimeException e) {}

        // test admin is able to change role
        document50 = adminRepository.getDocument(document50.getId(), true);
        document50.setOwner(user.getId());

        try {
            repository.deleteDocument(document40.getId());
            fail("Expected a DocumentNotFoundException.");
        } catch (DocumentNotFoundException e) {}

        //
        // Test multivalue fields
        //
        FieldType mvType1 = schema.createFieldType("mv1", ValueType.STRING, true);
        mvType1.save();
        FieldType mvType2 = schema.createFieldType("mv2", ValueType.LONG, true);
        mvType2.save();
        DocumentType mvDoctype = schema.createDocumentType("mvdoctype1");
        mvDoctype.addFieldType(mvType1, true);
        mvDoctype.addFieldType(mvType2, true);
        mvDoctype.save();

        Document mvDoc = repository.createDocument("multivalue field test doc", mvDoctype.getId());
        mvDoc.setField(mvType1.getId(), new String[] {"value1", "value2"});
        mvDoc.setField("mv2", new Object[] { new Long(12) });
        mvDoc.save();

        mvDoc = repository.getDocument(mvDoc.getId(), true);
        Object[] mvValues = (Object[])mvDoc.getField(mvType1.getId()).getValue();
        assertEquals(2, mvValues.length);
        assertEquals("value1", mvValues[0]);
        assertEquals("value2", mvValues[1]);
        mvValues = (Object[])mvDoc.getField(mvType2.getId()).getValue();
        assertEquals(1, mvValues.length);
        assertEquals(new Long(12), mvValues[0]);

        try {
            mvDoc.setField("mv2", new Object[0]);
            fail("Zero-length array should not be allowed for the value of a multivalue field.");
        } catch (DocumentTypeInconsistencyException e) {}


        //
        // Test hierarchical fields
        //
        FieldType hfType1 = schema.createFieldType("hf1", ValueType.STRING, false, true);
        hfType1.save();
        FieldType hfType2 = schema.createFieldType("hf2", ValueType.STRING, true, true);
        hfType2.save();

        DocumentType hfDoctype = schema.createDocumentType("hfdoctype1");
        hfDoctype.addFieldType(hfType1, true);
        hfDoctype.addFieldType(hfType2, true);
        hfDoctype.save();

        Document hfDoc = repository.createDocument("hierarchical field test doc", hfDoctype.getId());
        HierarchyPath path1 = new HierarchyPath(new String[] {"path1", "path2"});
        hfDoc.setField(hfType1.getId(), path1);
        HierarchyPath path2 = new HierarchyPath(new String[] {"A"});
        HierarchyPath path3 = new HierarchyPath(new String[] {"A", "B"});
        HierarchyPath path4 = new HierarchyPath(new String[] {"A", "B", "C"});
        Object[] multiValueHierarchicalValue = new Object[] {path2, path3, path4};
        hfDoc.setField("hf2", multiValueHierarchicalValue);
        hfDoc.save();

        hfDoc = repository.getDocument(hfDoc.getId(), true);
        assertEquals(path1, hfDoc.getField(hfType1.getId()).getValue());
        assertTrue(Arrays.equals(multiValueHierarchicalValue, (Object[])hfDoc.getField(hfType2.getId()).getValue()));

        // test toString implementation
        assertEquals("/path1/path2", hfDoc.getField(hfType1.getId()).getValue().toString());

        //
        // Test part file name and mime type update
        //
        Document document60 = repository.createDocument("doc60", documentType2.getId());
        document60.setPart(partType2.getId(), "application/x-ooktettenstroom", new byte[] {1, 2, 3});
        document60.setPartFileName(partType2.getId(), "myfilename.data");
        assertEquals("myfilename.data", document60.getPart(partType2.getId()).getFileName());
        document60.save();
        document60 = repository.getDocument(document60.getId(), true);
        assertEquals("myfilename.data", document60.getPart(partType2.getId()).getFileName());

        document60.setPartFileName(partType2.getId(), "myfilenamenew.data");
        document60.save();
        document60 = repository.getDocument(document60.getId(), true);
        assertEquals("myfilenamenew.data", document60.getPart(partType2.getId()).getFileName());
        assertEquals(2, document60.getLastVersionId());

        // setting part file name to same value should not cause any update
        document60.setPartFileName(partType2.getId(), "myfilenamenew.data");
        document60.save();
        document60 = repository.getDocument(document60.getId(), true);
        assertEquals(2, document60.getLastVersionId());

        document60.setPartFileName(partType2.getName(), null);
        document60.save();
        document60 = repository.getDocument(document60.getId(), true);
        assertNull(document60.getPart(partType2.getId()).getFileName());
        assertEquals(3, document60.getLastVersionId());

        document60.setPartMimeType(partType2.getName(), "application/octet-stream");
        document60.save();
        document60 = repository.getDocument(document60.getId(), true);
        assertEquals("application/octet-stream", document60.getPart(partType2.getId()).getMimeType());
        assertEquals(4, document60.getLastVersionId());

        //
        // Test link query selection list
        //   tested here instead of in the SchemaTest since it is more meaningful if documents exist
        //
        FieldType myLinkField = schema.createFieldType("myLinkField", ValueType.LINK);
        myLinkField.createLinkQuerySelectionList("true", false);
        List<? extends ListItem> linkFieldListItems = myLinkField.getSelectionList().getItems();
        assertTrue(linkFieldListItems.size() > 0);
        assertNotNull(linkFieldListItems.get(0).getValue());
        assertNotNull(linkFieldListItems.get(0).getLabel(Locale.US));

        // Test query selection list
        stringField1 = schema.getFieldTypeByName("StringField1", true);
        stringField1.createQuerySelectionList("select $StringField1 where $StringField1 is not null", false, SortOrder.ASCENDING);
        stringField1.save();
        List<? extends ListItem> queryListItems = stringField1.getSelectionList().getItems();
        assertTrue(queryListItems.size() > 0);

        //
        // Test creation of documents in foreign namespace
        //
        adminRepository.getNamespaceManager().registerNamespace("FOO");

        Document nsDoc = repository.createDocument("Foreign namespace test", documentType2.getId());
        nsDoc.setRequestedId("1-FOO");
        nsDoc.save();
        assertEquals("1-FOO", nsDoc.getId());
        assertNull(nsDoc.getRequestedId());

        nsDoc = repository.createDocument("Foreign namespace test", documentType2.getId());
        try {
            nsDoc.setRequestedId("1-XYZ");
            fail("Setting a document ID with a non-existing namespace should fail.");
        } catch (InvalidDocumentIdException e) {}
        try {
            nsDoc.setRequestedId("1-DSYTEST");
            fail("Setting a document ID within the current repository's namespace should fail.");
        } catch (RepositoryRuntimeException e) {}
        nsDoc.setRequestedId("2-FOO");
        nsDoc.setRequestedId(null);
        assertNull(nsDoc.getRequestedId());

        nsDoc.setRequestedId("2-FOO");
        nsDoc.save();
        try {
            nsDoc.setRequestedId("3-FOO");
            fail("Setting a document ID on an already-saved document should fail.");
        } catch (RepositoryRuntimeException e) {}

        nsDoc = repository.createDocument("Foreign namespace test", documentType2.getId());
        nsDoc.setRequestedId("1-FOO");
        try {
            nsDoc.save();
            fail("Saving a document twice with the same ID should fail.");
        } catch (RepositoryException e) {}


        //
        // Test the version methods. Not really document related but making a different testcase for this
        // seems like overkill.
        //
        assertNotNull(repository.getClientVersion());
        assertNotNull(repository.getServerVersion());
    }

    public void assertEquals(byte[] data1, byte[] data2) {
        if (data1.length != data2.length)
            fail("byte arrays not of equals length");

        for (int i = 0; i < data1.length; i++) {
            if (data1[i] != data2[i])
                fail("byte arrays not equal, detected difference at byte " + i);
        }
    }

    private Date getDate(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getDateTime(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }
}
