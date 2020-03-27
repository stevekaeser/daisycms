/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerx.daisy.x10.VersionDocument;

import java.util.Date;

public class VersionAccessWrapper extends VersionedDataAccessWrapper implements Version {
    protected VersionImpl delegate;

    public VersionAccessWrapper(VersionImpl delegate, AccessDetails accessDetails, CommonRepository repository, AuthenticatedUser currentUser) {
        super(delegate, accessDetails, repository, currentUser);
        this.delegate = delegate;
    }

    protected void checkLiveAccess() {
        return;
    }
    
    public long getId() {
        return delegate.getId();
    }

    public Date getCreated() {
        return delegate.getCreated();
    }

    public long getCreator() {
        return delegate.getCreator();
    }

    public VersionDocument getShallowXml() {
        // shallow XML doesn't contain anything limited by access details
        return delegate.getShallowXml();
    }

    public VersionDocument getXml() throws RepositoryException {
        return delegate.getXml(accessDetails);
    }

    public void setState(VersionState state) {
        delegate.setState(state);
    }

    public VersionState getState() {
        return delegate.getState();
    }

    public long getLastModifier() {
        return delegate.getLastModifier();
    }

    public Date getLastModified() {
        return delegate.getLastModified();
    }

    public long getTotalSizeOfParts() {
        return delegate.getTotalSizeOfParts();
    }

    public void setSyncedWith(long languageId, long versionId) throws RepositoryException {
        delegate.setSyncedWith(languageId, versionId);
    }

    public void setSyncedWith(VersionKey syncedWith) throws RepositoryException {
        delegate.setSyncedWith(syncedWith);
    }

    public VersionKey getSyncedWith() {
        return delegate.getSyncedWith();
    }

    public void setChangeType(ChangeType changeType) throws RepositoryException {
        delegate.setChangeType(changeType);
    }

    public ChangeType getChangeType() {
        return delegate.getChangeType();
    }

    public void setChangeComment(String changeComment) throws RepositoryException {
        delegate.setChangeComment(changeComment);
    }

    public String getChangeComment() {
        return delegate.getChangeComment();
    }

    public void save() throws RepositoryException {
        delegate.save();
    }

}
