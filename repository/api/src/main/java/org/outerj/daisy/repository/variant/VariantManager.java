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
package org.outerj.daisy.repository.variant;

import org.outerj.daisy.repository.RepositoryException;

/**
 * Allows to manage the branch and language definitions.
 *
 * <p>The VariantManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getVariantManager()}.
 *
 * <p>Note that this is only about defining branches and languages, the actual creation
 * of documents on these branches and languages is done through the
 * {@link org.outerj.daisy.repository.Repository Repository} API.
 */
public interface VariantManager {
    /**
     * Creates a new branch definition. The branch is not immediately created
     * in the repository, you need to call the save() method of the returned
     * object to do this.
     *
     * @param name a unique name satisfying the regexp "[a-zA-Z][a-zA-Z\-_0-9]*"
     */
    Branch createBranch(String name);

    /**
     * Retrieves a branch by ID.
     */
    Branch getBranch(long id, boolean updateable) throws RepositoryException;

    /**
     * Retrieves a branch by ID or by name depending on whether the branch parameter
     * starts with a digit.
     */
    Branch getBranch(String branch, boolean updateable) throws RepositoryException;

    /**
     * Retrieves a branch by name.
     */
    Branch getBranchByName(String name, boolean updateable) throws RepositoryException;

    Branches getAllBranches(boolean updateable) throws RepositoryException;

    /**
     * Deletes a branch. A branch can only be deleted if no document exists on the branch.
     * Thus before deleting a branch, all document variants on this branch must be deleted.
     * This can be easily done by performing a query that searches all documents on
     * the branch -- see QueryManager -- and then deletes them one by one in a loop.
     */
    void deleteBranch(long id) throws RepositoryException;

    Language createLanguage(String name);

    Language getLanguage(long id, boolean updateable) throws RepositoryException;

    Language getLanguage(String language, boolean updateable) throws RepositoryException;

    Language getLanguageByName(String name, boolean updateable) throws RepositoryException;

    Languages getAllLanguages(boolean updateable) throws RepositoryException;

    void deleteLanguage(long id) throws RepositoryException;
}
