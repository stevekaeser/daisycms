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
package org.outerj.daisy.repository.commonimpl.variant;

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;

public interface VariantStrategy {
    BranchImpl getBranch(long id, AuthenticatedUser user) throws RepositoryException;

    BranchImpl getBranchByName(String name, AuthenticatedUser user) throws RepositoryException;

    BranchImpl[] getAllBranches(AuthenticatedUser user) throws RepositoryException;

    void storeBranch(BranchImpl branch) throws RepositoryException;

    void deleteBranch(long id, AuthenticatedUser user) throws RepositoryException;

    LanguageImpl getLanguage(long id, AuthenticatedUser user) throws RepositoryException;

    LanguageImpl getLanguageByName(String name, AuthenticatedUser user) throws RepositoryException;

    LanguageImpl[] getAllLanguages(AuthenticatedUser user) throws RepositoryException;

    void storeLanguage(LanguageImpl language) throws RepositoryException;

    void deleteLanguage(long id, AuthenticatedUser user) throws RepositoryException;
}
