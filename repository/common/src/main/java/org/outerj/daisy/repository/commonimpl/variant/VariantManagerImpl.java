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

public class VariantManagerImpl implements VariantManager {
    private CommonVariantManager delegate;
    private AuthenticatedUser user;

    public VariantManagerImpl(CommonVariantManager delegate, AuthenticatedUser user) {
        this.delegate = delegate;
        this.user = user;
    }

    public Branch createBranch(String name) {
        return delegate.createBranch(name, user);
    }

    public Branch getBranch(long id, boolean updateable) throws RepositoryException {
        return delegate.getBranch(id, updateable, user);
    }

    public Branch getBranch(String branch, boolean updateable) throws RepositoryException {
        return delegate.getBranch(branch, updateable, user);
    }

    public Branch getBranchByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getBranchByName(name, updateable, user);
    }

    public Branches getAllBranches(boolean updateable) throws RepositoryException {
        return delegate.getAllBranches(updateable, user);
    }

    public void deleteBranch(long id) throws RepositoryException {
        delegate.deleteBranch(id, user);
    }

    public Language createLanguage(String name) {
        return delegate.createLanguage(name, user);
    }

    public Language getLanguage(long id, boolean updateable) throws RepositoryException {
        return delegate.getLanguage(id, updateable, user);
    }

    public Language getLanguage(String language, boolean updateable) throws RepositoryException {
        return delegate.getLanguage(language, updateable, user);
    }

    public Language getLanguageByName(String name, boolean updateable) throws RepositoryException {
        return delegate.getLanguageByName(name, updateable, user);
    }

    public Languages getAllLanguages(boolean updateable) throws RepositoryException {
        return delegate.getAllLanguages(updateable, user);
    }

    public void deleteLanguage(long id) throws RepositoryException {
        delegate.deleteLanguage(id, user);
    }
}
