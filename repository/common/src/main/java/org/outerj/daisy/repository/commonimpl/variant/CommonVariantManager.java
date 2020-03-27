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

import org.outerj.daisy.repository.variant.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;

public class CommonVariantManager {
    private VariantStrategy strategy;
    private VariantCache cache;

    public CommonVariantManager(VariantStrategy strategy, VariantCache cache) {
        this.strategy = strategy;
        this.cache = cache;
    }

    public RepositoryListener getCacheListener() {
        return cache;
    }

    public Branch createBranch(String name, AuthenticatedUser user) {
        return new BranchImpl(strategy, name, user);
    }

    public Branch getBranch(long id, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getBranch(id);
        return strategy.getBranch(id, user);
    }

    public Branch getBranchByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getBranchByName(name);
        return strategy.getBranchByName(name, user);
    }

    public Branch getBranch(String branch, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (branch == null || branch.length() == 0)
            throw new IllegalArgumentException("name: null or empty");

        if (Character.isDigit(branch.charAt(0))) {
            try {
                long id = Long.parseLong(branch);
                return getBranch(id, updateable, user);
            } catch (NumberFormatException e) {
                throw new BranchNotFoundException(branch);
            }
        }
        return getBranchByName(branch, updateable, user);
    }

    public Branches getAllBranches(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getBranches();
        return new BranchesImpl(strategy.getAllBranches(user));
    }

    public void deleteBranch(long id, AuthenticatedUser user) throws RepositoryException {
        strategy.deleteBranch(id, user);
    }

    public Language createLanguage(String name, AuthenticatedUser user) {
        return new LanguageImpl(strategy, name, user);
    }

    public Language getLanguage(long id, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getLanguage(id);
        return strategy.getLanguage(id, user);
    }

    public Language getLanguageByName(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getLanguageByName(name);
        return strategy.getLanguageByName(name, user);
    }

    public Language getLanguage(String language, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (language == null || language.length() == 0)
            throw new IllegalArgumentException("name: null or empty");

        if (Character.isDigit(language.charAt(0))) {
            try {
                long id = Long.parseLong(language);
                return getLanguage(id, updateable, user);
            } catch (NumberFormatException e) {
                throw new LanguageNotFoundException(language);
            }
        }
        return getLanguageByName(language, updateable, user);
    }

    public Languages getAllLanguages(boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (!updateable)
            return cache.getLanguages();
        return new LanguagesImpl(strategy.getAllLanguages(user));
    }

    public void deleteLanguage(long id, AuthenticatedUser user) throws RepositoryException {
        strategy.deleteLanguage(id, user);
    }
}
