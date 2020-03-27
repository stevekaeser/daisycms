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
import org.outerj.daisy.repository.*;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class VariantCache implements RepositoryListener {
    private VariantStrategy variantStrategy;
    private AuthenticatedUser cacheUser;

    private Lock branchesLock = new ReentrantLock();
    private boolean branchesLoaded = false;
    private Map branchesById;
    private Map branchesByName;
    private Branches branches;

    private Lock languagesLock = new ReentrantLock();
    private boolean languagesLoaded = false;
    private Map languagesById;
    private Map languagesByName;
    private Languages languages;

    public VariantCache(VariantStrategy variantStrategy, AuthenticatedUser cacheUser) {
        this.variantStrategy = variantStrategy;
        this.cacheUser = cacheUser;
    }

    private void assureBranchesLoaded() throws RepositoryException {
        if (branchesLoaded)
            return;

        try {
            branchesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (branchesLoaded)
                return;

            Map branchesById = new HashMap();
            Map branchesByName = new HashMap();
            BranchImpl[] branches = variantStrategy.getAllBranches(cacheUser);
            for (int i = 0; i < branches.length; i++) {
                BranchImpl branch = branches[i];
                branch.makeReadOnly();
                branchesById.put(new Long(branch.getId()), branch);
                branchesByName.put(branch.getName(), branch);
            }
            this.branchesById = branchesById;
            this.branchesByName = branchesByName;
            this.branches = new BranchesImpl(branches);
            this.branchesLoaded = true;
        } finally {
            branchesLock.unlock();
        }
    }

    private void assureLanguagesLoaded() throws RepositoryException {
        if (languagesLoaded)
            return;

        try {
            languagesLock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            if (languagesLoaded)
                return;

            Map languagesById = new HashMap();
            Map languagesByName = new HashMap();
            LanguageImpl[] languages = variantStrategy.getAllLanguages(cacheUser);
            for (int i = 0; i < languages.length; i++) {
                LanguageImpl language = languages[i];
                language.makeReadOnly();
                languagesById.put(new Long(language.getId()), language);
                languagesByName.put(language.getName(), language);
            }
            this.languagesById = languagesById;
            this.languagesByName = languagesByName;
            this.languages = new LanguagesImpl(languages);
            this.languagesLoaded = true;
        } finally {
            languagesLock.unlock();
        }
    }

    public Branch getBranch(long id) throws RepositoryException {
        assureBranchesLoaded();
        Branch branch = (Branch)branchesById.get(new Long(id));
        if (branch == null)
            throw new BranchNotFoundException(id);
        return branch;
    }

    public Branch getBranchByName(String name) throws RepositoryException {
        assureBranchesLoaded();
        Branch branch = (Branch)branchesByName.get(name);
        if (branch == null)
            throw new BranchNotFoundException(name);
        return branch;
    }

    public Branches getBranches() throws RepositoryException {
        assureBranchesLoaded();
        return branches;
    }

    public Language getLanguage(long id) throws RepositoryException {
        assureLanguagesLoaded();
        Language language = (Language)languagesById.get(new Long(id));
        if (language == null)
            throw new LanguageNotFoundException(id);
        return language;
    }

    public Language getLanguageByName(String name) throws RepositoryException {
        assureLanguagesLoaded();
        Language language = (Language)languagesByName.get(name);
        if (language == null)
            throw new LanguageNotFoundException(name);
        return language;
    }

    public Languages getLanguages() throws RepositoryException {
        assureLanguagesLoaded();
        return languages;
    }

    public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
        if (eventType.isBranchEvent()) {
            branchesLoaded = false;
        } else if (eventType.isLanguageEvent()) {
            languagesLoaded = false;
        }
    }

}
