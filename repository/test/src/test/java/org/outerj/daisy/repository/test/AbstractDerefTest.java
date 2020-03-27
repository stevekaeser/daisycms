/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10.SearchResultDocument;

import java.util.Locale;

/**
 * Tests for the query language dereference operator.
 */
public abstract class AbstractDerefTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testDeref() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();

        Repository testuserRepository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        testuserRepository.switchRole(Role.ADMINISTRATOR);

        RepositorySchema schema = testuserRepository.getRepositorySchema();
        QueryManager testuserQueryManager = testuserRepository.getQueryManager();

        FieldType linkField = schema.createFieldType("Link", ValueType.LINK);
        linkField.save();

        SearchResultDocument result;

        //
        // Some basic tests
        //
        {
            DocumentType documentType = schema.createDocumentType("Doctype1");
            documentType.addFieldType(linkField, false);
            documentType.save();

            Document document3 = testuserRepository.createDocument("Doc3", documentType.getId());
            document3.save();

            Document document2 = testuserRepository.createDocument("Doc2", documentType.getId());
            document2.setField("Link", new VariantKey(document3.getId(), -1, -1));
            document2.save();

            Document document1 = testuserRepository.createDocument("Doc1", documentType.getId());
            document1.setField("Link", document2.getVariantKey());
            document1.save();

            Document document5 = testuserRepository.createDocument("Doc5", documentType.getId());
            document5.save();

            Document document4 = testuserRepository.createDocument("Doc4", documentType.getId());
            document4.setField("Link", document5.getVariantKey());
            document4.save();

            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and $Link=>name = 'Doc2'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and $Link=>name = 'Doc3'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document2.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            // multiple levels of dereference in where clause
            result = testuserQueryManager.performQuery("select name, $Link=>name, $Link=>$Link=>name where documentType='Doctype1' and $Link=>$Link=>name = 'Doc3'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            // Some tests with multiple deref expressions combined with and / or
            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and $Link=>name = 'Doc2' and $Link=>name = 'Doc3'", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and ($Link=>name = 'Doc2' or $Link=>name = 'DocNotExisting')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and ($Link=>name = 'Doc2' or $Link=>name = 'Doc2')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and $Link=>name = 'Doc2' and $Link=>name = 'Doc2' and $Link=>name = 'Doc2'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());

            // deref in order by clause
            result = testuserQueryManager.performQuery("select name, $Link=>name where documentType='Doctype1' and ($Link=>name = 'Doc2' or $Link=>name = 'Doc5') order by $Link=>name DESC", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals("Doc5", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));
            assertEquals("Doc2", result.getSearchResult().getRows().getRowArray(1).getValueArray(1));

            // deref expressions as arguments in functions
            result = testuserQueryManager.performQuery("select name, Left($Link=>name, 1), $Link=>$Link=>name where documentType='Doctype1' and Right($Link=>$Link=>name, 1) = '3'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
            assertEquals(document1.getId(), result.getSearchResult().getRows().getRowArray(0).getDocumentId());
            assertEquals("D", result.getSearchResult().getRows().getRowArray(0).getValueArray(1));

            // Make one of the documents retired
            document2.setRetired(true);
            document2.save();
            // now we should not get a result anymore
            result = testuserQueryManager.performQuery("select name, $Link=>name, $Link=>$Link=>name where documentType='Doctype1' and $Link=>$Link=>name = 'Doc3'", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());
            // unless we specify the option to include retired docs
            result = testuserQueryManager.performQuery("select name, $Link=>name, $Link=>$Link=>name where documentType='Doctype1' and $Link=>$Link=>name = 'Doc3' option include_retired = 'true'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            // Refering to ourselves using the 'link' identifier, should give all docs except for the one which is retired
            result = testuserQueryManager.performQuery("select name where documentType='Doctype1' and link=>name = name", Locale.US);
            assertEquals(4, result.getSearchResult().getRows().sizeOfRowArray());
            // how much fun...
            result = testuserQueryManager.performQuery("select name where documentType='Doctype1' and link=>name = name and link=>link=>link=>link=>link=>name = name", Locale.US);
            assertEquals(4, result.getSearchResult().getRows().sizeOfRowArray());
        }

        //
        // variant related tests
        //
        {
            VariantManager variantManager = testuserRepository.getVariantManager();
            Branch branch = variantManager.createBranch("branch");
            branch.save();

            DocumentType documentType = schema.createDocumentType("Doctype2");
            documentType.addFieldType(linkField, false);
            documentType.save();

            Document document2 = testuserRepository.createDocument("Doc2", documentType.getId());
            document2.save();

            Document document1 = testuserRepository.createDocument("Doc1", documentType.getId());
            document1.setField("Link", new VariantKey(document2.getId(), -1, 1));
            document1.save();

            Document document2b = testuserRepository.createVariant(document2.getId(), 1, 1, -1, branch.getId(), 1, true);
            document2b.save();

            Document document1b = testuserRepository.createVariant(document1.getId(), 1, 1, -1, branch.getId(), 1, true);
            document1b.save();

            // query without filtering on branch
            result = testuserQueryManager.performQuery("select name where documentType='Doctype2' and $Link=>name = 'Doc2'", Locale.US);
            assertEquals(2, result.getSearchResult().getRows().sizeOfRowArray());

            // with filtering on branch
            result = testuserQueryManager.performQuery("select name where documentType='Doctype2' and $Link=>name = 'Doc2' and branchId = 1", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            // with filtering on branch of the link target doc
            result = testuserQueryManager.performQuery("select name where documentType='Doctype2' and $Link=>name = 'Doc2' and link=>branchId = 1", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
        }

        //
        // access control tests
        //
        {
            // create another user
            UserManager userManager = testuserRepository.getUserManager();
            User user2 = userManager.createUser("user2");
            user2.setPassword("user2");
            user2.addToRole(userManager.getRole("User", false));
            user2.save();

            // create an ACL (default is empty so no access for non-admins/non-owners)
            AccessManager accessManager = testuserRepository.getAccessManager();
            Acl acl = accessManager.getStagingAcl();
            AclObject aclObject = acl.createNewObject("true");
            AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
            for (AclPermission perm : AclPermission.values())
                aclEntry.set(perm, AclActionType.GRANT);
            aclObject.add(aclEntry);
            acl.add(aclObject);
            acl.save();
            accessManager.copyStagingToLive();


            DocumentType documentType = schema.createDocumentType("Doctype3");
            documentType.addFieldType(linkField, false);
            documentType.save();

            Document document2 = testuserRepository.createDocument("Doc2", documentType.getId());
            document2.save();

            Document document1 = testuserRepository.createDocument("Doc1", documentType.getId());
            document1.setField("Link", new VariantKey(document2.getId(), -1, 1));
            document1.save();

            Repository user2Repository = repositoryManager.getRepository(new Credentials("user2", "user2"));
            QueryManager user2QueryManager = user2Repository.getQueryManager();

            // user2 should still be able to access the documents
            result = user2QueryManager.performQuery("select name where documentType='Doctype3' and $Link=>name = 'Doc2'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            // make doc private so user2 doesn't have access to it
            document2.setPrivate(true);
            document2.save();

            // In the where-part we traverse the link "$Link" which points to a document to which the user doesn't
            // have access anymore, hence it's impossible to know whether it satisfies the demanded conditions,
            // and the result is excluded from the result set.
            result = user2QueryManager.performQuery("select name where documentType='Doctype3' and $Link=>name = 'Doc2'", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            // add some more silly conditions
            result = user2QueryManager.performQuery("select name where documentType='Doctype3' and ($Link=>name = 'Doc2' or name='Doc1' or $Link=>name='Doc2')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());
        }

        //
        // Test all sorts of operators, many of these had to be adjusted for the deref operator to work
        // so it's important we test them all in derefereced context
        //
        {
            FieldType mvField = schema.createFieldType("Multi", ValueType.STRING, true);
            mvField.save();

            FieldType hierField = schema.createFieldType("Hier", ValueType.STRING, false, true);
            hierField.save();

            FieldType mvHierField = schema.createFieldType("MultiHier", ValueType.STRING, true, true);
            mvHierField.save();

            FieldType stringField = schema.createFieldType("String", ValueType.STRING);
            stringField.save();

            DocumentType documentType = schema.createDocumentType("Doctype4");
            documentType.addFieldType(linkField, false);
            documentType.addFieldType(mvField, false);
            documentType.addFieldType(hierField, false);
            documentType.addFieldType(mvHierField, false);
            documentType.addFieldType(stringField, false);
            documentType.save();

            DocumentType linkedDocumentType = schema.createDocumentType("DoctypeLinkedByDoctype4");
            linkedDocumentType.addFieldType(linkField, false);
            linkedDocumentType.addFieldType(mvField, false);
            linkedDocumentType.addFieldType(hierField, false);
            linkedDocumentType.addFieldType(mvHierField, false);
            linkedDocumentType.addFieldType(stringField, false);
            linkedDocumentType.save();

            Document document2 = testuserRepository.createDocument("Doc2", linkedDocumentType.getId());
            document2.setField("String", "foo");
            document2.setField("Hier", new HierarchyPath(new String[] {"r", "s", "t"}));
            document2.setField("MultiHier", new Object[] {new HierarchyPath(new String[] {"a1", "a2", "a3"}),
                    new HierarchyPath(new String[] {"b1", "b2"}), new HierarchyPath(new String[] {"c1"})});
            document2.setField("Multi", new Object[] { "val1", "val2", "val3"});
            document2.save();

            Document document1 = testuserRepository.createDocument("Doc1", documentType.getId());
            document1.setField("Link", new VariantKey(document2.getId(), -1, 1));
            document1.save();


            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String = 'foo'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String = $Link=>$String and link=>link=>link=>$Link=>$String = $Link=>link=>link=>$String", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String = 'bar'", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String IN ('foo', 'bar')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String NOT IN ('foo', 'bar')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String IN ('bar')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String IS NOT NULL", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link is not null and $Link=>$String IS NULL", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String between 'a' and 'z'", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$String between 'a' and 'b'", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Multi has exactly ('val1', 'val2', 'val3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Multi has exactly ('val1', 'val2')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Multi has all ('val1', 'val2', 'val3')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Multi has all ('val1', 'val2', 'valOther')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link is not null and $Link=>$Multi has none ('x', 'y')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link is not null and $Link=>$Multi has none ('x', 'y', 'val1')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Hier matchesPath('r/s/t')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Hier matchesPath('r/*/*')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Hier matchesPath('r/**')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$Hier matchesPath('p/**')", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has all(Path('a1/a2/a3'), Path('c1'))", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has all(Path('a1/a2/a3'), Path('d1'))", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has all(Path('a1/a2/a3'), Path('b1/b2'), Path('c1'))", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has exactly (Path('a1/a2/a3'), Path('b1/b2'), Path('c1'))", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has none (Path('a1/a2/a3'), Path('b1/b2'), Path('c1'))", Locale.US);
            assertEquals(0, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has none (Path('x1/x2/x3'), Path('y1/b2'))", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());

            result = testuserQueryManager.performQuery("select name where documentType='Doctype4' and $Link=>$MultiHier has none (Path('x1/x2/x3'), Path('y1/b2')) and $Link=>$MultiHier has exactly (Path('a1/a2/a3'), Path('b1/b2'), Path('c1')) and $Link=>$Hier matchesPath('r/s/t')", Locale.US);
            assertEquals(1, result.getSearchResult().getRows().sizeOfRowArray());
        }

        //
        // Test things which shouldn't be possible
        //
        {
            FieldType mvLinkField = schema.createFieldType("MvLink", ValueType.LINK, true);
            mvLinkField.save();

            FieldType hierLinkField = schema.createFieldType("HierLink", ValueType.LINK, false, true);
            hierLinkField.save();

            DocumentType documentType = schema.createDocumentType("Doctype5");
            documentType.addFieldType(mvLinkField, false);
            documentType.addFieldType(hierLinkField, false);
            documentType.save();

            Document document2 = testuserRepository.createDocument("Doc2", documentType.getId());
            document2.save();

            Document document1 = testuserRepository.createDocument("Doc1", documentType.getId());
            document1.setField("MvLink", new Object[] {new VariantKey(document2.getId(), -1, 1)});
            document1.setField("HierLink", new HierarchyPath(new Object[] {new VariantKey(document2.getId(), -1, 1)}));
            document1.save();

            try {
                testuserQueryManager.performQuery("select name where documentType='Doctype5' and $MvLink=>name = 'Doc2'", Locale.US);
                fail("Expected a query exception trying to dereference a multivalue link field");
            } catch (QueryException e) {
                // ok
            }

            try {
                testuserQueryManager.performQuery("select name where documentType='Doctype5' and $HierLink=>name = 'Doc2'", Locale.US);
                fail("Expected a query exception trying to dereference a hierarchical link field");
            } catch (QueryException e) {
                // ok
            }
        }

    }
}
