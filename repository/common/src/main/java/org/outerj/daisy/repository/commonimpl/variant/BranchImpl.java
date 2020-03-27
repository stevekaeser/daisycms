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

import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.Util;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.BranchDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class BranchImpl implements Branch {
    private long id = -1;
    private String name;
    private String description;
    private boolean readOnly = false;
    private long lastModifier;
    private Date lastModified;
    private long updateCount = 0;
    private AuthenticatedUser currentUser;
    private VariantStrategy strategy;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private static final String READ_ONLY_MESSAGE = "This Branch object is read-only.";

    public BranchImpl(VariantStrategy strategy, String name, AuthenticatedUser currentUser) {
        Util.checkName(name);
        this.strategy = strategy;
        this.name = name;
        this.currentUser = currentUser;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        Util.checkName(name);
        this.name = name;
    }

    public void setDescription(String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.description = description;
    }

    public void save() throws RepositoryException {
        strategy.storeBranch(this);
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public BranchDocument getXml() {
        BranchDocument branchDocument = BranchDocument.Factory.newInstance();
        BranchDocument.Branch branch = branchDocument.addNewBranch();

        branch.setName(name);
        if (description != null)
            branch.setDescription(description);

        if (id != -1) {
            branch.setId(id);

            GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
            lastModifiedCalendar.setTime(lastModified);
            branch.setLastModified(lastModifiedCalendar);

            branch.setLastModifier(lastModifier);
            branch.setUpdateCount(updateCount);
        }

        return branchDocument;
    }

    public void setAllFromXml(BranchDocument.Branch branchXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        setName(branchXml.getName());
        setDescription(branchXml.getDescription());
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public IntimateAccess getIntimateAccess(VariantStrategy strategy) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (this.strategy == strategy)
            return intimateAccess;
        else
            return null;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public AuthenticatedUser getCurrentUser() {
            return currentUser;
        }

        public void setId(long id) {
            BranchImpl.this.id = id;
        }

        public void setLastModified(Date lastModified) {
            BranchImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            BranchImpl.this.lastModifier = lastModifier;
        }

        public void setUpdateCount(long updateCount) {
            BranchImpl.this.updateCount = updateCount;
        }
    }
}
