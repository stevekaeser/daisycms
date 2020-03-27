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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.Versions;
import org.outerj.daisy.repository.acl.AccessDetails;

/**
 * This wrapper is intended for updateable documents to which users do not have
 * full read access rights. It is assumed that non-readable data (fields/parts/summary)from
 * the current document are already removed, hence that no fields/parts filtering
 * needs to happen for the current data, only for the versioned data.
 */
public class DocumentWriteAccessWrapper extends AbstractDocumentWrapper {
    private AccessDetails readAccessDetails;
    private CommonRepository repository;
    private AuthenticatedUser currentUser;

    public DocumentWriteAccessWrapper(DocumentImpl delegate, DocumentStrategy documentStrategy,
            AccessDetails readAccessDetails, CommonRepository repository, AuthenticatedUser currentUser) {
        super(delegate, documentStrategy);
        this.readAccessDetails = readAccessDetails;
        this.repository = repository;
        this.currentUser = currentUser;
    }

    public Versions getVersions() throws RepositoryException {
        if (readAccessDetails.isFullAccess())
            return delegate.getVersions();

        Version[] versions = delegate.getVersions().getArray();
        Version[] newVersions = new Version[versions.length];
        for (int i = 0; i < versions.length; i++) {
            newVersions[i] = new VersionAccessWrapper((VersionImpl)versions[i], readAccessDetails, repository, currentUser);
        }

        return new VersionsImpl(newVersions);
    }

    public Version getVersion(long id) throws RepositoryException {
        if (readAccessDetails.isFullAccess())
            return delegate.getVersion(id);

        return new VersionAccessWrapper((VersionImpl)delegate.getVersion(id), readAccessDetails, repository, currentUser);
    }

    @Override
    public Version getLastVersion() throws RepositoryException {
        if (readAccessDetails.isFullAccess())
            return delegate.getLastVersion();

        return new VersionAccessWrapper((VersionImpl)delegate.getLastVersion(), readAccessDetails, repository, currentUser);
    }

    @Override
    public Version getLiveVersion() throws RepositoryException {
        if (readAccessDetails.isFullAccess())
            return delegate.getLiveVersion();

        return new VersionAccessWrapper((VersionImpl)delegate.getLiveVersion(), readAccessDetails, repository, currentUser);
    }

}
