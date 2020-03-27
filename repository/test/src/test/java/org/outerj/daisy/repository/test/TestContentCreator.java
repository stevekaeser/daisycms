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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;

import java.util.GregorianCalendar;
import java.math.BigDecimal;

public class TestContentCreator {
    public void run(RepositoryManager repositoryManager) throws Exception {
        Repository testuserRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        testuserRepository.switchRole(Role.ADMINISTRATOR);

        // Create a role and two users
        UserManager userManger = testuserRepository.getUserManager();
        Role role = userManger.getRole("User", false);

        User user1 = userManger.createUser("user1");
        user1.addToRole(role);
        user1.setDefaultRole(role);
        user1.setFirstName("User1First");
        user1.setLastName("User1Last");
        user1.setPassword("user1");
        user1.save();

        User user2 = userManger.createUser("user2");
        user2.addToRole(role);
        user2.setDefaultRole(role);
        user2.setFirstName("User2");
        user2.setLastName("User2Last");
        user2.setPassword("user2");
        user2.save();

        // Create two document types
        RepositorySchema schema = testuserRepository.getRepositorySchema();

        FieldType fieldType1 = schema.createFieldType("field1", ValueType.STRING);
        fieldType1.save();
        FieldType fieldType2 = schema.createFieldType("field2", ValueType.LONG);
        fieldType2.setAclAllowed(true);
        fieldType2.save();

        PartType partType1 = schema.createPartType("part1", "text/xml");
        partType1.setDaisyHtml(true);
        partType1.save();

        DocumentType documentType1 = schema.createDocumentType("doctype1");
        documentType1.addFieldType(fieldType1, false);
        documentType1.addFieldType(fieldType2, false);
        documentType1.addPartType(partType1, false);
        documentType1.save();

        DocumentType documentType2 = schema.createDocumentType("doctype2");
        documentType2.addFieldType(fieldType1, false);
        documentType2.addFieldType(fieldType2, false);
        documentType2.addPartType(partType1, false);
        documentType2.save();

        // Create two collections
        CollectionManager collectionManager = testuserRepository.getCollectionManager();
        DocumentCollection collection1 = collectionManager.createCollection("collection1");
        collection1.save();
        DocumentCollection collection2 = collectionManager.createCollection("collection2");
        collection2.save();

        // Create the ACL

        //  everyone can do everything, except:
        //  documentType = 'doctype2'   -> user1 denied
        //  collection = 'collection2'  -> user2 denied except if fieldType2 = 34

        AccessManager accessManager = testuserRepository.getAccessManager();
        Acl acl = accessManager.getStagingAcl();

        AclObject aclObject;
        AclEntry aclEntry;

        aclObject = acl.createNewObject("true");
        aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.GRANT);
        aclEntry.set(AclPermission.DELETE, AclActionType.GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("documentType = 'doctype2'");
        aclEntry = aclObject.createNewEntry(AclSubjectType.USER, user1.getId());
        aclEntry.set(AclPermission.READ, AclActionType.DENY);
        aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.DENY);
        aclEntry.set(AclPermission.DELETE, AclActionType.DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("InCollection('collection2') and ($field2 != 34 or $field2 is null)");
        aclEntry = aclObject.createNewEntry(AclSubjectType.USER, user2.getId());
        aclEntry.set(AclPermission.READ, AclActionType.DENY);
        aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.DENY);
        aclEntry.set(AclPermission.DELETE, AclActionType.DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        acl.save();
        accessManager.copyStagingToLive();

        // Create some documents as user1
        Repository user1Repository = repositoryManager.getRepository(new Credentials("user1", "user1"));

        Document document1 = user1Repository.createDocument("Document1", documentType1.getId());
        String searchableContent = "<html><body>Effect van de langtermijnrente op het coca cola gebruik bij verzamelaars van postzegels.</body></html>";
        document1.setPart(partType1.getId(), "text/xml", searchableContent.getBytes("UTF-8"));
        document1.addToCollection(collection1);
        document1.save();

        Document document2 = user1Repository.createDocument("Document2", documentType1.getId());
        String contentWithLink = "<html><body><a href='daisy:" + document1.getId() + "'/></body></html>";
        document2.setPart(partType1.getId(), "text/xml", contentWithLink.getBytes("UTF-8"));
        document2.addToCollection(collection1);
        document2.save();

        Document document3 = user1Repository.createDocument("Document3", documentType1.getId());
        document3.setPrivate(true);
        document3.addToCollection(collection1);
        document3.save();

        Document document4 = user1Repository.createDocument("Document4", documentType1.getId());
        document4.setField("field1", "something-X");
        document4.addToCollection(collection2);
        document4.save();

        // Create some documents as user2
        Repository user2Repository = repositoryManager.getRepository(new Credentials("user2", "user2"));

        Document document5 = user2Repository.createDocument("Document5", documentType1.getId());
        document5.setField("field1", "something-X");
        document5.addToCollection(collection1);
        document5.save();

        Document document6 = user2Repository.createDocument("Document6", documentType2.getId());
        document6.setField("field2", new Long(34));
        document6.addToCollection(collection2);
        document6.save();

        // Make a documetn that belongs to two collections
        DocumentCollection collection3 = collectionManager.createCollection("collection3");
        collection3.save();
        DocumentCollection collection4 = collectionManager.createCollection("collection4");
        collection4.save();

        Document document7 = user2Repository.createDocument("Document7", documentType1.getId());
        document7.addToCollection(collection3);
        document7.addToCollection(collection4);
        document7.save();

        //
        // Make some documents / document type that uses the various data types
        //

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
        FieldType linkField1 = schema.createFieldType("LinkField1", ValueType.LINK);
        linkField1.save();
        FieldType linkField2 = schema.createFieldType("LinkField2", ValueType.LINK, true);
        linkField2.save();

        DocumentType documentType3 = schema.createDocumentType("doctype3");
        documentType3.addFieldType(stringField1, false);
        documentType3.addFieldType(dateField1, false);
        documentType3.addFieldType(dateTimeField1, false);
        documentType3.addFieldType(decimalField1, false);
        documentType3.addFieldType(doubleField1, false);
        documentType3.addFieldType(longField1, false);
        documentType3.addFieldType(linkField1, false);
        documentType3.addFieldType(linkField2, false);
        documentType3.save();

        User user3 = userManger.createUser("user3");
        user3.setPassword("user3");
        user3.setFirstName("User3First");
        user3.setLastName("User3Last");
        user3.addToRole(role);
        user3.setDefaultRole(role);
        user3.save();

        DocumentCollection collection5 = collectionManager.createCollection("collection5");
        collection5.save();

        Repository user3Repository = repositoryManager.getRepository(new Credentials("user3", "user3"));

        Document document8 = user3Repository.createDocument("Document8", documentType3.getId());
        document8.setField(stringField1.getId(), "hello");
        GregorianCalendar calendar = new GregorianCalendar(2004, 11, 6, 0, 0, 0); // month is zero-based
        document8.setField(dateField1.getId(), calendar.getTime());
        calendar = new GregorianCalendar(2004, 9, 14, 12, 13, 14);
        document8.setField(dateTimeField1.getId(), calendar.getTime());
        document8.setField(decimalField1.getId(), new BigDecimal(678.94321));
        document8.setField(doubleField1.getId(), new Double(123.456d));
        document8.setField(longField1.getId(), new Long(1978L));
        document8.setField(linkField1.getId(), new VariantKey("2", 1, 1));
        document8.setField(linkField2.getId(), new VariantKey[] {new VariantKey("2", 1, 1), new VariantKey("666", -1, -1)});
        document8.addToCollection(collection5);
        document8.save();

        Document document9 = user3Repository.createDocument("Document9", documentType3.getId());
        calendar = new GregorianCalendar(2004, 11, 10, 0, 0, 0); // month is zero-based
        document9.setField(dateField1.getId(), calendar.getTime());
        document9.addToCollection(collection5);
        document9.save();

        Document document10 = user3Repository.createDocument("Document10", documentType3.getId());
        document10.setField(longField1.getId(), new Long(1985L));
        document10.addToCollection(collection5);
        document10.save();

        Document document11 = user3Repository.createDocument("Document11", documentType3.getId());
        document11.setField(stringField1.getId(), "another hello");
        document11.setField(longField1.getId(), new Long(1990L));
        document11.addToCollection(collection5);
        document11.setCustomField("xyz", "123");
        document11.save();

        Document document12 = user3Repository.createDocument("Document12", documentType3.getId());
        document12.save();
        document12.setNewVersionState(VersionState.DRAFT);
        document12.setField(stringField1.getId(), "boe");
        document12.save();

        Document document13 = user3Repository.createDocument("Document13", documentType3.getId());
        document13.setField(stringField1.getId(), "ba");
        document13.setRetired(true);
        document13.save();

    }

    public void createAdditionalDocumentsForHierarchicalFieldTests(RepositoryManager repositoryManager) throws Exception {
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        RepositorySchema schema = repository.getRepositorySchema();

        FieldType hierField1 = schema.createFieldType("HierField1", ValueType.STRING, false, true);
        hierField1.save();
        FieldType hierField2 = schema.createFieldType("HierField2", ValueType.LINK, true, true);
        hierField2.save();

        DocumentType documentType = schema.createDocumentType("HierTestDocType");
        documentType.addFieldType(hierField1, false);
        documentType.addFieldType(hierField2, false);
        documentType.save();

        {
            Document document = repository.createDocument("Document20", documentType.getId());
            document.setField("HierField1", new HierarchyPath(new Object[] { "Aaa", "Bee", "Cee", "Dee" } ));
            document.save();
        }

        {
            Document document = repository.createDocument("Document21", documentType.getId());
            document.setField("HierField1", new HierarchyPath(new Object[] { "Aaa", "Bee", "Cee", "Dee" } ));
            document.save();
        }

        {
            Document document = repository.createDocument("Document22", documentType.getId());
            document.setField("HierField1", new HierarchyPath(new Object[] { "Aaa", "Bee", "Cee"} ));
            document.save();
        }

        {
            Document document = repository.createDocument("Document23", documentType.getId());
            document.setField("HierField1", new HierarchyPath(new Object[] { "X", "Y"} ));
            document.save();
        }

        {
            Document document = repository.createDocument("Document30", documentType.getId());
            document.setField("HierField2", new Object[] {
                    new HierarchyPath(new Object[] { new VariantKey("1000-DSYTEST", 1, 1), new VariantKey("1001-DSYTEST", 1, 1)} ),
                    new HierarchyPath(new Object[] { new VariantKey("1002-DSYTEST", 1, 1)})});
            document.save();
        }

        {
            Document document = repository.createDocument("Document30", documentType.getId());
            document.setField("HierField2", new Object[] {
                    new HierarchyPath(new Object[] { new VariantKey("1005-DSYTEST", 1, 1), new VariantKey("1006-DSYTEST", 1, 1)} ),
                    new HierarchyPath(new Object[] { new VariantKey("1007-DSYTEST", 1, 1)})
            });
            document.save();
        }

        {
            Document document = repository.createDocument("Document30", documentType.getId());
            document.setField("HierField2", new Object[] {
                    new HierarchyPath(new Object[] { new VariantKey("1005-DSYTEST", 1, 1), new VariantKey("1006-DSYTEST", 1, 1)} ),
                    new HierarchyPath(new Object[] { new VariantKey("1007-DSYTEST", 1, 1)}),
                    new HierarchyPath(new Object[] { new VariantKey("1009-DSYTEST", 1, 1), new VariantKey("1010-DSYTEST", 1, 1), new VariantKey("1009-DSYTEST", 1, 1)})
            });
            document.save();
        }
    }

}
