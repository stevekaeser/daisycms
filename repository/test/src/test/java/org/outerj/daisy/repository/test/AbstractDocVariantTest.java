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
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerx.daisy.x10.SearchResultDocument;

import java.util.Date;
import java.util.Locale;

/**
 * This testcase tests all functionality related to document variants.
 */
public abstract class AbstractDocVariantTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testVariants() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        //
        // Create some variants
        //
        VariantManager variantManager = repository.getVariantManager();
        Branch branch1 = variantManager.createBranch("branch1");
        branch1.save();
        Branch branch2 = variantManager.createBranch("branch2");
        branch2.save();
        Branch branch3 = variantManager.createBranch("branch3");
        branch3.save();
        Language language1 = variantManager.createLanguage("lang1");
        language1.save();
        Language language2 = variantManager.createLanguage("lang2");
        language2.save();
        Language language3 = variantManager.createLanguage("lang3");
        language3.save();

        //
        // Create some schema types
        //
        RepositorySchema repositorySchema = repository.getRepositorySchema();
        FieldType fieldType1 = repositorySchema.createFieldType("fieldType1", ValueType.STRING);
        fieldType1.setAclAllowed(true);
        fieldType1.save();
        FieldType fieldType2 = repositorySchema.createFieldType("fieldType2", ValueType.STRING);
        fieldType2.save();
        PartType partType1 = repositorySchema.createPartType("partType1", "");
        partType1.setDaisyHtml(true);
        partType1.save();
        PartType partType2 = repositorySchema.createPartType("partType2", "");
        partType2.save();
        DocumentType documentType1 = repositorySchema.createDocumentType("doctype1");
        documentType1.addFieldType(fieldType1, false);
        documentType1.addFieldType(fieldType2, false);
        documentType1.addPartType(partType1, false);
        documentType1.addPartType(partType2, false);
        documentType1.save();

        // Test some basic branching stuff
        {
            Document document = repository.createDocument("document 1", "doctype1");
            document.setField("fieldType1", "abc");
            document.setField("fieldType2", "def");
            document.save();
            document.setField("fieldType2", "def2");
            document.save();

            Document variant1 = repository.createVariant(document.getId(), "main", "default", -1, "branch1", "lang1", true);
            // new variant should be initialised with existing data
            assertEquals("abc", variant1.getField("fieldType1").getValue());
            assertEquals("def2", variant1.getField("fieldType2").getValue());
            assertTrue(variant1.getCreated() != document.getCreated());
            assertEquals(1, variant1.getVersions().getArray().length);
            assertEquals(2, document.getVersions().getArray().length);

            assertEquals(1, variant1.getVariantCreatedFromBranchId());
            assertEquals(1, variant1.getVariantCreatedFromLanguageId());
            assertEquals(2, variant1.getVariantCreatedFromVersionId());

            variant1.setField("fieldType1", "ghi");
            variant1.save();

            document = repository.getDocument(document.getId(), true);
            variant1 = repository.getDocument(document.getId(), "branch1", "lang1", true);

            assertEquals("abc", document.getField("fieldType1").getValue());
            assertEquals("def2", document.getField("fieldType2").getValue());
            assertEquals("ghi", variant1.getField("fieldType1").getValue());
            assertEquals("def2", variant1.getField("fieldType2").getValue());

            if (repositoryManager.getClass().getName().endsWith("LocalRepositoryManager")) {
                Document docFromCache1 = repository.getDocument(document.getId(), false);
                Document docFromCache2 = repository.getDocument(document.getId(), false);
                assertTrue(docFromCache1 == docFromCache2);

                Document variant1FromCache1 = repository.getDocument(document.getId(), "branch1", "lang1", false);
                Document variant1FromCache2 = repository.getDocument(document.getId(), "branch1", "lang1", false);
                assertTrue(variant1FromCache1 == variant1FromCache2);
                assertTrue(docFromCache1 != variant1FromCache1);

                // update doc and see that cache was invalidated
                variant1.setField("fieldType1", "zoo");
                variant1.save();

                Document variant1FromCache = repository.getDocument(document.getId(), "branch1", "lang1", false);
                assertEquals("zoo", variant1FromCache.getField("fieldType1").getValue());

                // ... but original variant should not have been cache-invalidated
                assertTrue(docFromCache1 == repository.getDocument(document.getId(), false));

                // change a version state
                Version lastVersion = variant1.getLastVersion();
                lastVersion.setState(VersionState.DRAFT);
                lastVersion.save();
                // and check again that cache was invalidated
                assertTrue(variant1FromCache != repository.getDocument(document.getId(), "branch1", "lang1", false));
                // ... but original variant should not have been cache-invalidated
                assertTrue(docFromCache1 == repository.getDocument(document.getId(), false));


                // update a shared doc property and check that all variants have been invalidated from the cache
                Date variant1VariantLastModified = variant1.getVariantLastModified();
                variant1.setPrivate(true);
                variant1.save();
                // and meanwhile check that only changing a shared property doesn't save the variant data
                assertEquals(variant1VariantLastModified.getTime(), variant1.getVariantLastModified().getTime());
                assertTrue(variant1FromCache != repository.getDocument(document.getId(), "branch1", "lang1", false));
                assertTrue(docFromCache1 != repository.getDocument(document.getId(), false));
            }

            // check that saving an unchanged document doesn't save anything
            document = repository.getDocument(document.getId(), true);
            Date lastModified = document.getLastModified();
            Date variantLastModified = document.getVariantLastModified();
            document.save();
            assertEquals(lastModified.getTime(), document.getLastModified().getTime());
            assertEquals(variantLastModified.getTime(), document.getVariantLastModified().getTime());
            document = repository.getDocument(document.getId(), true);
            assertEquals(lastModified.getTime(), document.getLastModified().getTime());
            assertEquals(variantLastModified.getTime(), document.getVariantLastModified().getTime());

            // check availableVariants
            assertEquals(2, repository.getAvailableVariants(document.getId()).size());
        }

        // test that shared blobkey is not removed when one document variant is removed
        {
            Document variant1 = repository.createDocument("shared blobkey test", documentType1.getId(), branch1.getId(), language1.getId());
            String data = "<html><body><p>kabouter plop</p></body></html>";
            variant1.setPart(partType1.getId(), "text/xml", data.getBytes("UTF-8"));
            variant1.save();

            Document variant2 = repository.createVariant(variant1.getId(), branch1.getId(), language1.getId(), 1, branch2.getId(), language2.getId(), true);

            repository.deleteVariant(variant1.getId(), branch1.getId(), language1.getId());

            // version 1 of variant 2 was created based on variant1 and internally this
            // will reuse the same blob key. Here we verify that after deleting variant1,
            // the blob is still available to variant2
            String retrievedData = new String(variant2.getPart(partType1.getId()).getData(), "UTF-8");
            assertEquals(data, retrievedData);


            // check deleted document variant is no longer in the cache
            try {
                repository.getDocument(variant1.getId(), branch1.getId(), language1.getId(), false);
                fail("Expected a DocumentVariantNotFoundException.");
            } catch (DocumentVariantNotFoundException e) {}
            // ... nor in the repository
            try {
                repository.getDocument(variant1.getId(), branch1.getId(), language1.getId(), true);
                fail("Expected a DocumentVariantNotFoundException.");
            } catch (DocumentVariantNotFoundException e) {}
        }

        // More extensive testing of various document properties
        {
            Document document = repository.createDocument("extensive document", documentType1.getId());

            CollectionManager collectionManager = repository.getCollectionManager();
            DocumentCollection collection1 = collectionManager.createCollection("collection 1");
            collection1.save();
            DocumentCollection collection2 = collectionManager.createCollection("collection 2");
            collection2.save();

            document.addToCollection(collection1);
            document.addToCollection(collection2);

            document.setCustomField("field1", "value1");
            document.setCustomField("field2", "value2");

            document.addLink("Google", "http://www.google.be");
            document.addLink("Apache", "http://www.apache.org");

            document.setField(fieldType1.getId(), "field1 value");
            document.setField(fieldType2.getId(), "field2 value");

            String part1Data = "<html><body><p>Hello this is the content of part1</p></body></html>";
            String part2Data = "<html><body><p>Hello this is the content of part2</p></body></html>";

            document.setPart(partType1.getId(), "text/xml", part1Data.getBytes("UTF-8"));
            document.setPart(partType2.getId(), "text/xml", part2Data.getBytes("UTF-8"));

            document.save();

            document.removeFromCollection(collection2);
            document.deleteField(fieldType1.getId());
            document.setPrivate(true);
            document.save();

            // both document and variant were modified, and saved at the same time, so they should have the
            // same last modified stamp
            assertEquals(document.getLastModified().getTime(), document.getVariantLastModified().getTime());
            assertEquals(2, document.getUpdateCount());
            assertEquals(2, document.getVariantUpdateCount());

            Document variant = repository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, 1, branch1.getId(), language2.getId(), true);

            // creating a new branch shouldn't have updated the common document part
            assertEquals(variant.getUpdateCount(), document.getUpdateCount());

            assertEquals(true, variant.isPrivate());
            assertEquals(branch1.getId(), variant.getBranchId());
            assertEquals(language2.getId(), variant.getLanguageId());
            assertEquals(document.getId(), variant.getId());

            assertTrue(variant.inCollection(collection1));
            assertFalse(variant.inCollection(collection2));

            assertTrue(variant.hasField(fieldType1.getId()));
            assertTrue(variant.hasField(fieldType2.getId()));

            assertEquals("field1 value", variant.getField(fieldType1.getId()).getValue());
            assertEquals("field2 value", variant.getField(fieldType2.getId()).getValue());

            assertEquals(part1Data, new String(variant.getPart(partType1.getId()).getData(), "UTF-8"));
            assertEquals(part2Data, new String(variant.getPart(partType2.getId()).getData(), "UTF-8"));

            Link[] links = variant.getLinks().getArray();
            assertEquals("Google", links[0].getTitle());
            assertEquals("http://www.google.be", links[0].getTarget());
            assertEquals("Apache", links[1].getTitle());
            assertEquals("http://www.apache.org", links[1].getTarget());

            assertEquals("value1", variant.getCustomField("field1"));
            assertEquals("value2", variant.getCustomField("field2"));

            String newPart1Data = "<html><body><p>Updated content</p></body></html>";
            variant.setPart(partType1.getId(), "text/xml", newPart1Data.getBytes("UTF-8"));
            variant.save();

            assertEquals("Updated content", variant.getSummary());
            assertEquals("Hello this is the content of part1", repository.getDocument(document.getId(), true).getSummary());
            assertEquals("Updated content", repository.getDocument(document.getId(), branch1.getId(), language2.getId(), true).getSummary());
            assertEquals("Hello this is the content of part1", repository.getDocument(document.getId(), false).getSummary());
            assertEquals("Updated content", repository.getDocument(document.getId(), branch1.getId(), language2.getId(), false).getSummary());
        }

        // test create branch without copying data
        {
            Document document = repository.createDocument("my document", documentType1.getId());
            document.setCustomField("field", "value");
            document.save();

            Document variant = repository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, document.getLiveVersion().getId(), branch2.getId(), language1.getId(), false);

            try {
                repository.getDocument(document.getId(), branch2.getId(), language1.getId(), true);
                fail("Variant created without copying data should not yet be persisted.");
            } catch (DocumentVariantNotFoundException e) {}

            variant.setCustomField("anotherfield", "anothervalue");
            variant.save();
            variant = repository.getDocument(document.getId(), branch2.getId(), language1.getId(), true);

            // check that shared part was not saved
            assertEquals(document.getUpdateCount(), variant.getUpdateCount());
        }

        // check that changing a shared doc property doesn't create a new version of the variant
        {
            Document document = repository.createDocument("document x", documentType1.getId());
            document.addLink("Daisy", "http://daisycms.org/daisy");
            document.save();

            Document cachedDocument = repository.getDocument(document.getId(), false);

            User jan = repository.getUserManager().createUser("jan");
            Role adminRole = repository.getUserManager().getRole(Role.ADMINISTRATOR, false);
            jan.addToRole(adminRole);
            jan.setDefaultRole(adminRole);
            jan.setPassword("jan");
            jan.save();

            document.setOwner(jan.getId());
            document.save();

            assertEquals(1, document.getLastVersionId());
            document = repository.getDocument(document.getId(), true);
            assertEquals(1, document.getLastVersionId());

            // test cache was invalidated after changing shared property
            assertEquals(jan.getId(), repository.getDocument(document.getId(), false).getOwner());
        }

        // test the getAvailableVariants methods
        {
            Document document = repository.createDocument("document y", documentType1.getId());
            assertEquals(0, document.getAvailableVariants().size());
            document.save();

            assertEquals(1, document.getAvailableVariants().size());

            repository.createVariant(document.getId(), document.getBranchId(), document.getLanguageId(), document.getLastVersionId(), branch2.getId(), language1.getId(), true);
            repository.createVariant(document.getId(), document.getBranchId(), document.getLanguageId(), document.getLastVersionId(), branch1.getId(), language2.getId(), true);
            repository.createVariant(document.getId(), document.getBranchId(), document.getLanguageId(), document.getLastVersionId(), branch2.getId(), language2.getId(), true);

            assertEquals(4, document.getAvailableVariants().size());
            assertEquals(4, repository.getAvailableVariants(document.getId()).size());

            if (repositoryManager.getClass().getName().endsWith("LocalRepositoryManager")) {
                // test this stuff is cached
                assertTrue(document.getAvailableVariants() == document.getAvailableVariants());
                assertTrue(document.getAvailableVariants() == repository.getAvailableVariants(document.getId()));
            }
        }

        // test only valid branch and language id's can be used
        {
            try {
                repository.createDocument("document x", documentType1.getId(), 5000, 6000);
                fail("Using non-existing lang or branch id's should have failed.");
            } catch (Throwable e) {}

            Document document = repository.createDocument("document z", documentType1.getId());
            document.save();

            try {
                repository.createVariant(document.getId(), document.getBranchId(), document.getLanguageId(), -1, 5000, 6000, true);
                fail("Using non-existing lang or branch id's should have failed.");
            } catch (Throwable e) {}
        }

        // test creation of new variant requires (only) read access to the start variant
        {
            UserManager userManager = repository.getUserManager();
            Role userRole = userManager.getRole("User", false);
            User tomUser = userManager.createUser("Tom");
            tomUser.addToRole(userRole);
            tomUser.setDefaultRole(userRole);
            tomUser.setPassword("tom");
            tomUser.save();

            AccessManager accessManager = repository.getAccessManager();
            Acl acl = accessManager.getStagingAcl();

            AclObject aclObject = acl.createNewObject("true");
            AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.ROLE, userRole.getId());
            aclEntry.set(AclPermission.READ, AclActionType.GRANT);
            aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
            aclObject.add(aclEntry);
            acl.add(aclObject);

            aclObject = acl.createNewObject("branch = 'main' and language = 'default'");
            aclEntry = aclObject.createNewEntry(AclSubjectType.ROLE, userRole.getId());
            aclEntry.set(AclPermission.READ, AclActionType.GRANT);
            aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
            aclObject.add(aclEntry);
            acl.add(aclObject);

            acl.save();
            accessManager.copyStagingToLive();

            Document document = repository.createDocument("testdocument", documentType1.getId());
            document.save();

            Repository tomRepository = repositoryManager.getRepository(new Credentials("Tom", "tom"));
            // check user has read access
            tomRepository.getDocument(document.getId(), true);

            // User has read access, creating the variant should work
            tomRepository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, -1,
                    branch1.getId(), language1.getId(), true);

            tomRepository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, -1,
                    branch2.getId(), language2.getId(), false);

            // remove the read access
            acl.get(1).get(0).set(AclPermission.READ, AclActionType.DENY);
            acl.save();
            accessManager.copyStagingToLive();

            // now create variant should not work
            try {
                tomRepository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, -1,
                        branch3.getId(), language3.getId(), true);
                fail("Creating branch should have failed because of access rights.");
            } catch (AccessException e) {}

            try {
                Document variantDoc = tomRepository.createVariant(document.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, -1,
                        branch3.getId(), language3.getId(), false);
                variantDoc.setField("fieldType1", "notsecret");
                fail("Creating branch should have failed because of access rights.");
            } catch (AccessException e) {}
        }

        // test index and query features
        {
            Document document1 = repository.createDocument("q doc 1", documentType1.getId());
            document1.setPart(partType1.getId(), "text/xml", "<html><body><p>gent</p></body></html>".getBytes("UTF-8"));
            document1.setPart(partType2.getId(), "text/xml", "<html><body><p>leuven</p></body></html>".getBytes("UTF-8"));
            document1.save();

            Document variant1 = repository.createVariant(document1.getId(), document1.getBranchId(), document1.getLanguageId(), -1, branch1.getId(), language1.getId(), false);
            variant1.setPart(partType1.getId(), "text/xml", "<html><body><p>kortrijk</p></body></html>".getBytes("UTF-8"));
            variant1.setPart(partType2.getId(), "text/xml", "<html><body><p>brugge</p></body></html>".getBytes("UTF-8"));
            variant1.save();

            Document variant2 = repository.createVariant(document1.getId(), variant1.getBranchId(), variant1.getLanguageId(), -1, branch2.getId(), language1.getId(), true);
            variant2.setPart(partType1.getId(), "text/xml", "<html><body><p>antwerpen</p></body></html>".getBytes("UTF-8"));
            variant2.setPart(partType2.getId(), "text/xml", "<html><body><p>zelzate</p></body></html>".getBytes("UTF-8"));
            variant2.save();

            System.out.println("Sleeping a bit to let fulltext indexer do its work");
            Thread.sleep(10000);

            QueryManager queryManager = repository.getQueryManager();
            SearchResultDocument result = queryManager.performQuery("select id where FullText('gent')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            SearchResultDocument.SearchResult.Rows.Row row = result.getSearchResult().getRows().getRowArray(0);
            assertEquals(document1.getId(), row.getDocumentId());
            assertEquals(document1.getBranchId(), row.getBranchId());
            assertEquals(document1.getLanguageId(), row.getLanguageId());

            result = queryManager.performQuery("select id where FullText('gent', 1, 1, 1, 2, 2)", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = queryManager.performQuery("select id where FullText('brugge')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            row = result.getSearchResult().getRows().getRowArray(0);
            assertEquals(variant1.getId(), row.getDocumentId());
            assertEquals(variant1.getBranchId(), row.getBranchId());
            assertEquals(variant1.getLanguageId(), row.getLanguageId());

            result = queryManager.performQuery("select id where FullText('antwerpen and zelzate')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            row = result.getSearchResult().getRows().getRowArray(0);
            assertEquals(variant2.getId(), row.getDocumentId());
            assertEquals(variant2.getBranchId(), row.getBranchId());
            assertEquals(variant2.getLanguageId(), row.getLanguageId());

            result = queryManager.performQuery("select id, branchId, languageId, branch, language where id = '" + document1.getId() + "' and languageId = " + language1.getId() + " order by branchId", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("branch1", result.getSearchResult().getRows().getRowArray(0).getValueArray(3));
            assertEquals("branch2", result.getSearchResult().getRows().getRowArray(1).getValueArray(3));
        }

        // test DoesNotHaveVariant and variants identifier
        {
            Document document1 = repository.createDocument("ooo1", documentType1.getId());
            document1.setField(fieldType1.getId(), "ooo");
            document1.save();

            Document document2 = repository.createDocument("ooo2", documentType1.getId());
            document2.setField(fieldType1.getId(), "ooo");
            document2.save();

            Document document3 = repository.createDocument("ooo3", documentType1.getId());
            document3.setField(fieldType1.getId(), "ooo");
            document3.save();

            repository.createVariant(document2.getId(), Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, -1, branch1.getId(), language1.getId(), true);

            QueryManager queryManager = repository.getQueryManager();

            SearchResultDocument result = queryManager.performQuery("select id where DoesNotHaveVariant('branch1', 'lang1') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());
            assertEquals(document3.getId(), result.getSearchResult().getRows().getRowArray(1).getDocumentId());

            result = queryManager.performQuery("select id where DoesNotHaveVariant(" + branch1.getId() + ", " + language1.getId() + ") and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());

            //
            // test variants and variants.valueCount identifiers
            //

            // test variants has any
            result = queryManager.performQuery("select id where language = 'default' and variants has any ('branch1:lang1', 'main:default') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(3, result.getSearchResult().getRows().sizeOfRowArray());

            result = queryManager.performQuery("select id where language = 'default' and variants has any ('branch1:lang1', 'branch2:lang2') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = queryManager.performQuery("select id where language = 'default' and variants has any ('branch2:lang2') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // test variants has all
            result = queryManager.performQuery("select id where language = 'default' and variants has all ('branch1:lang1', 'branch2:lang2') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = queryManager.performQuery("select id where language = 'default' and variants has all ('branch1:lang1', 'main:default') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            // test variants has exactly
            result = queryManager.performQuery("select id where language = 'default' and variants has exactly ('branch1:lang1', 'main:default') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = queryManager.performQuery("select id where language = 'default' and variants has exactly ('branch1:lang1') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // test variants has none
            result = queryManager.performQuery("select id where language = 'default' and variants has none ('branch1:lang1') and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());

            // test variants.valueCount
            result = queryManager.performQuery("select id where language = 'default' and variants.valueCount = 1 and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());

            // test these things on the select-side
            result = queryManager.performQuery("select id, variants, variants.valueCount where language = 'default' and variants.valueCount = 2 and $fieldType1 = 'ooo' order by id", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            // the following is just to make sure it gets properly serialized as a multi-value link-type value
            assertEquals(document2.getId(), result.getSearchResult().getRows().getRowArray(0).getMultiValueArray(0).getLinkValueArray(0).getDocumentId());
            assertEquals("2", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

        }

        // test deletion of in-use variants should fail
        {
            Branch branchX = variantManager.createBranch("branchX");
            branchX.save();

            Document document1 = repository.createDocument("haha", documentType1.getId(), branchX.getId(), Language.DEFAULT_LANGUAGE_ID);
            document1.setField(fieldType1.getId(), "haha");
            document1.save();

            try {
                variantManager.deleteBranch(branchX.getId());
                fail("Branch deletion should have failed because it is still in use.");
            } catch (RepositoryException e) {}

            repository.deleteDocument(document1.getId());
            variantManager.deleteBranch(branchX.getId());
        }

        // test updating of different variants in presence of locks by different users
        // ..

    }
}
