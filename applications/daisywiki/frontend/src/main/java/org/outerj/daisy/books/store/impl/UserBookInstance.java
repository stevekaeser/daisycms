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
package org.outerj.daisy.books.store.impl;

import org.outerj.daisy.books.store.*;
import org.outerj.daisy.repository.Repository;
import org.apache.cocoon.util.WildcardMatcherHelper;
import org.outerx.daisy.x10Bookstoremeta.ResourcePropertiesDocument;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;

public class UserBookInstance implements BookInstance {
    private CommonBookInstance delegate;
    private Repository repository;
    private boolean locked = false;

    /**
     * @param locked normally always false, except when a new (and locked) book instance was created
     */
    public UserBookInstance(CommonBookInstance delegate, Repository repository, boolean locked) {
        this.delegate = delegate;
        this.repository = repository;
        this.locked = locked;
    }

    File getDirectory() {
        return delegate.getDirectory();
    }

    private boolean isLockedBySomeoneElse() {
        if (!locked) {
            return delegate.isLocked();
        } else {
            return false;
        }
    }

    private boolean isLockedByMe() {
        return locked;
    }

    private AclResult checkCanRead() {
        if (isLockedByMe())
            return new AclResult(true, true);
        if (isLockedBySomeoneElse())
            throw new BookStoreException("This book instance is current locked for updating. Try again later.");
        AclResult aclResult = BookAclEvaluator.evaluate(delegate.getAcl(), repository.getUserId(), repository.getActiveRoleIds());
        if (!aclResult.canRead())
            throw new BookStoreAccessDeniedException();
        return aclResult;
    }

    private void checkCanRead(String path) {
        AclResult result = checkCanRead();
        if (!result.canManage()) {
            // If someone only has read access but not write access, we do not allow access
            // to all resources in the book instance.
            if (path.charAt(0) != '/')
                path = "/" + path;
            boolean ok = false;
            for (int i = 0; i < PUBLIC_RESOURCES_PATTERNS.length; i++) {
                if (WildcardMatcherHelper.match(PUBLIC_RESOURCES_PATTERNS[i], path)!= null) {
                    ok = true;
                    break;
                }
            }
            if (!ok)
                throw new BookStoreAccessDeniedException("Access denied to " + path);
        }
    }

    private void checkCanWrite() {
        if (!isLockedByMe())
            throw new BookStoreException("A lock is required to update a book instance.");
        AclResult aclResult = BookAclEvaluator.evaluate(delegate.getAcl(), repository.getUserId(), repository.getActiveRoleIds());
        if (!aclResult.canRead())
            throw new BookStoreAccessDeniedException();
    }

    public void lock() {
        if (isLockedByMe() || isLockedBySomeoneElse()) {
            throw new BookStoreException("This book instance is already locked.");
        }

        AclResult aclResult = BookAclEvaluator.evaluate(delegate.getAcl(), repository.getUserId(), repository.getActiveRoleIds());
        if (!aclResult.canManage())
            throw new BookStoreAccessDeniedException("You are not allowed to take a lock on the book instance \"" + getName() + "\".");

        File lockFile = new File(delegate.getDirectory(), CommonBookInstance.LOCK_FILE_NAME);
        try {
            if (lockFile.createNewFile()) {
                locked = true;
            } else {
                throw new BookStoreException("This book instance is already locked.");
            }
        } catch (IOException e) {
            throw new BookStoreException("IO Exception while trying to take a lock on a book instance.", e);
        }
    }

    public void unlock() {
        if (!isLockedByMe()) {
            throw new BookStoreException("You do not have a lock on this book instance.");
        }

        if (locked) {
            File lockFile = new File(delegate.getDirectory(), CommonBookInstance.LOCK_FILE_NAME);
            boolean success = lockFile.delete();
            if (!success)
                throw new BookStoreException("Could not remove lock on book instance.");
            locked = false;
        }
    }

    public String getName() {
        return delegate.getName();
    }

    private static final String[] PUBLIC_RESOURCES_PATTERNS = new String[] {
            "/data/resources/**",
            "/publications/*/output/**"
    };

    public InputStream getResource(String path) {
        if (path == null || path.length() == 0)
            throw new IllegalArgumentException("path argument can not be null or empty");
        checkCanRead(path);
        return delegate.getResource(path);
    }

    public ResourcePropertiesDocument getResourceProperties(String path) {
        if (path == null || path.length() == 0)
            throw new IllegalArgumentException("path argument can not be null or empty");
        checkCanRead(path);
        return delegate.getResourceProperties(path);
    }

    public void storeResource(String path, InputStream is) {
        checkCanWrite();
        delegate.storeResource(path, is);
    }

    public void storeResourceProperties(String path, ResourcePropertiesDocument resourcePropertiesDocument) {
        checkCanWrite();
        delegate.storeResourceProperties(path, resourcePropertiesDocument);
    }

    public OutputStream getResourceOutputStream(String path) throws IOException {
        checkCanWrite();
        return delegate.getResourceOutputStream(path);
    }

    public boolean rename(String path, String newName) {
        checkCanWrite();
        return delegate.rename(path, newName);
    }

    public boolean exists(String path) {
        checkCanRead(path);
        return delegate.exists(path);
    }

    public long getLastModified(String path) {
        checkCanRead(path);
        return delegate.getLastModified(path);
    }

    public long getContentLength(String path) {
        checkCanRead(path);
        return delegate.getContentLength(path);
    }

    public BookAcl getAcl() {
        checkCanRead();
        return delegate.getAcl();
    }

    public void setAcl(BookAcl bookAcl) {
        checkCanWrite();
        delegate.setAcl(bookAcl);
    }

    public boolean canManage() {
        AclResult aclResult = BookAclEvaluator.evaluate(delegate.getAcl(), repository.getUserId(), repository.getActiveRoleIds());
        return aclResult.canManage();
    }

    public URI getResourceURI(String path) {
        checkCanRead(path);
        return delegate.getResourceURI(path);
    }

    public PublicationsInfo getPublicationsInfo() {
        checkCanRead();
        return delegate.getPublicationsInfo();
    }

    public void addPublication(PublicationInfo publicationInfo) {
        checkCanWrite();
        delegate.addPublication(publicationInfo);
    }

    public void setPublications(PublicationsInfo publicationsInfo) {
        checkCanWrite();
        delegate.setPublications(publicationsInfo);
    }

    public BookInstanceMetaData getMetaData() {
        checkCanRead();
        return delegate.getMetaData();
    }

    public void setMetaData(BookInstanceMetaData metaData) {
        checkCanWrite();
        delegate.setMetaData(metaData);
    }

    public String[] getDescendantPaths(String path) {
        checkCanRead();
        return delegate.getDescendantPaths(path);
    }
}
