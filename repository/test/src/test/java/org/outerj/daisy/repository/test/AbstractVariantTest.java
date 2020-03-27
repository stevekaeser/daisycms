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
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.variant.*;

/**
 * This testcase tests the basic management of the branch and language
 * entities. For actual testing of the variant functionality of documents,
 * see {@link AbstractDocVariantTest}.
 */
public abstract class AbstractVariantTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    protected void moreTests() throws Exception {
        // allows subclasses to do more tests within the same test run
    }

    public void testVariants() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        Role userRole = repository.getUserManager().getRole("User", false);

        VariantManager variantManager = repository.getVariantManager();

        {
            Branch branch = variantManager.createBranch("branch1");
            branch.save();

            assertEquals(branch.getLastModifier(), repository.getUserId());
            long lastModified = branch.getLastModified().getTime();
            assertTrue(lastModified < System.currentTimeMillis() && lastModified > System.currentTimeMillis() - 3000);
            assertEquals(1, branch.getUpdateCount());

            branch = variantManager.getBranch(branch.getId(), true);
            assertEquals("branch1", branch.getName());

            try {
                variantManager.getBranch(branch.getId() + 1, true);
                fail("Getting a non-existing branch should have failed.");
            } catch (BranchNotFoundException e) {}

            try {
                variantManager.getBranch("xyz", true);
                fail("Getting a non-existing branch should have failed.");
            } catch (BranchNotFoundException e) {}

            Branch cachedBranch = variantManager.getBranch(branch.getId(), false);
            try {
                cachedBranch.setName("hallo");
                fail("Modifying a cached/shared object should throw an exception.");
            } catch (RuntimeException e) {}

            // test duplicate name test
            Branch dupbranch = variantManager.createBranch("branch1");
            try {
                dupbranch.save();
                fail("Saving a branch with the same name as an existing branch should fail.");
            } catch (RepositoryException e) {}

            // test description
            branch = variantManager.getBranch("branch1", true);
            branch.setDescription("beautiful branch");
            branch.save();

            branch = variantManager.getBranch(branch.getId(), true);
            assertEquals("beautiful branch", branch.getDescription());

            // test concurrent modification  check
            Branch altBranch = variantManager.getBranch("branch1", true);
            altBranch.save();

            try {
                branch.save();
                fail("Saving branch should fail due to concurrent modification.");
            } catch (RepositoryException e) {}

            Branch branch2 = variantManager.createBranch("branch2");
            branch2.save();
            Branch branch3 = variantManager.createBranch("branch3");
            branch3.save();

            assertEquals(4, variantManager.getAllBranches(true).size());
            assertEquals(4, variantManager.getAllBranches(false).size());

            // main branch should always exist
            Branch mainBranch = variantManager.getBranch(1, true);
            assertEquals("main", mainBranch.getName());
            try {
                mainBranch.save();
                fail("Saving the branch 'main' should fail.");
            } catch (RepositoryException e) {}

            try {
                variantManager.createBranch("name with spaces");
                fail("Creating a branch with an invalid name should fail");
            } catch (IllegalArgumentException e) {}

            try {
                branch3.setName("123abc");
                fail("Setting invalid branch name should fail");
            } catch (IllegalArgumentException e) {}

            // deletion
            Branch branch4 = variantManager.createBranch("branch4");
            branch4.save();
            // make sure branch is in cache
            variantManager.getBranch(branch4.getId(), false);
            repository.switchRole(userRole.getId());
            try {
                variantManager.deleteBranch(branch4.getId());
                fail("Deletion of branch by non-admin user should have failed.");
            } catch (RepositoryException e) {}
            repository.switchRole(Role.ADMINISTRATOR);
            variantManager.deleteBranch(branch4.getId());

            try {
                variantManager.getBranch(branch4.getId(), false);
                fail("Getting deleted branch should fail.");
            } catch (BranchNotFoundException e) {}

            try {
                variantManager.getBranch(branch4.getId(), true);
                fail("Getting deleted branch should fail.");
            } catch (BranchNotFoundException e) {}

            try {
                variantManager.deleteBranch(Branch.MAIN_BRANCH_ID);
                fail("Deleting branch 'main' should have failed.");
            } catch (RepositoryException e) {}

        }

        //
        // Same tests for languages
        //
        {
            Language language = variantManager.createLanguage("language1");
            language.save();

            assertEquals(language.getLastModifier(), repository.getUserId());
            long lastModified = language.getLastModified().getTime();
            assertTrue(lastModified < System.currentTimeMillis() && lastModified > System.currentTimeMillis() - 3000);
            assertEquals(1, language.getUpdateCount());

            language = variantManager.getLanguage(language.getId(), true);
            assertEquals("language1", language.getName());

            try {
                variantManager.getLanguage(language.getId() + 1, true);
                fail("Getting a non-existing language should have failed.");
            } catch (LanguageNotFoundException e) {}

            try {
                variantManager.getLanguage("xyz", true);
                fail("Getting a non-existing language should have failed.");
            } catch (LanguageNotFoundException e) {}

            Language cachedLanguage = variantManager.getLanguage(language.getId(), false);
            try {
                cachedLanguage.setName("hallo");
                fail("Modifying a cached/shared object should throw an exception.");
            } catch (RuntimeException e) {}

            // test duplicate name test
            Language duplanguage = variantManager.createLanguage("language1");
            try {
                duplanguage.save();
                fail("Saving a language with the same name as an existing language should fail.");
            } catch (RepositoryException e) {}

            // test description
            language = variantManager.getLanguage("language1", true);
            language.setDescription("beautiful language");
            language.save();

            language = variantManager.getLanguage(language.getId(), true);
            assertEquals("beautiful language", language.getDescription());

            // test concurrent modification  check
            Language altLanguage = variantManager.getLanguage("language1", true);
            altLanguage.save();

            try {
                language.save();
                fail("Saving language should fail due to concurrent modification.");
            } catch (RepositoryException e) {}

            Language language2 = variantManager.createLanguage("language2");
            language2.save();
            Language language3 = variantManager.createLanguage("language3");
            language3.save();

            assertEquals(4, variantManager.getAllLanguages(true).size());
            assertEquals(4, variantManager.getAllLanguages(false).size());

            // main language should always exist
            Language defaultLanguage = variantManager.getLanguage(1, true);
            assertEquals("default", defaultLanguage.getName());
            try {
                defaultLanguage.save();
                fail("Saving the language 'default' should fail.");
            } catch (RepositoryException e) {}

            try {
                variantManager.createLanguage("name with spaces");
                fail("Creating a language with an invalid name should fail");
            } catch (IllegalArgumentException e) {}

            try {
                language3.setName("123abc");
                fail("Setting invalid language name should fail");
            } catch (IllegalArgumentException e) {}

            // deletion
            Language language4 = variantManager.createLanguage("language4");
            language4.save();
            // make sure language is in cache
            variantManager.getLanguage(language4.getId(), false);
            repository.switchRole(userRole.getId());
            try {
                variantManager.deleteLanguage(language4.getId());
                fail("Deletion of language by non-admin user should have failed.");
            } catch (RepositoryException e) {}
            repository.switchRole(Role.ADMINISTRATOR);
            variantManager.deleteLanguage(language4.getId());

            try {
                variantManager.getLanguage(language4.getId(), false);
                fail("Getting deleted language should fail.");
            } catch (LanguageNotFoundException e) {}

            try {
                variantManager.getLanguage(language4.getId(), true);
                fail("Getting deleted language should fail.");
            } catch (LanguageNotFoundException e) {}

            try {
                variantManager.deleteLanguage(Language.DEFAULT_LANGUAGE_ID);
                fail("Deleting language 'default' should have failed.");
            } catch (RepositoryException e) {}

        }

        moreTests();
    }
}
