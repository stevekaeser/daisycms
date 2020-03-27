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
package org.outerj.daisy.repository.commonimpl.user;

import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;

public class UserManagerImpl implements UserManager {
    private CommonUserManager delegate;
    /* Mind the fully qualified name of this user!
     * This needs to happen because we clearly need
     * a distinction between an Authenticated User
     * and a User meant for User Management!
     */
    private org.outerj.daisy.repository.commonimpl.AuthenticatedUser user;
    
    public UserManagerImpl(CommonUserManager commonUserManager, AuthenticatedUser user) {
        delegate = commonUserManager;
        this.user = user;
    }

    public Users getUsers() throws RepositoryException {
        return delegate.getUsers(user);
    }

    public long[] getUserIds() throws RepositoryException {
        return delegate.getUserIds(user);
    }

    public PublicUserInfo getPublicUserInfo(long userId) throws RepositoryException {
        return delegate.getPublicUserInfo(userId);
    }

    public PublicUserInfo getPublicUserInfo(String login) throws RepositoryException {
        return delegate.getPublicUserInfo(login);
    }

    public PublicUserInfos getPublicUserInfos() throws RepositoryException {
        return delegate.getPublicUserInfos(user);
    }

    public Roles getRoles() throws RepositoryException {
        return delegate.getRoles(user);
    }

    public User createUser(String login) {
        return delegate.createUser(login, user);
    }

    public void deleteUser(long userId) throws RepositoryException {
        delegate.deleteUser(userId, user);
    }

    public Role createRole(String roleName) {
        return delegate.createRole(roleName, user);
    }

    public User getUser(String login, boolean updateable) throws RepositoryException {
        return delegate.getUser(login, updateable, user);
    }

    public Role getRole(String name, boolean updateable) throws RepositoryException {
        return delegate.getRole(name, updateable, user);
    }

    public void deleteRole(long roleId) throws RepositoryException {
        delegate.deleteRole(roleId, user); 
    }

    public User getUser(long userId, boolean updateable) throws RepositoryException {
        return delegate.getUser(userId, updateable, user);
    }

    public Role getRole(long roleId, boolean updateable) throws RepositoryException {
        return delegate.getRole(roleId, updateable, user);
    }

    public String getUserDisplayName(long userId) throws RepositoryException {
        return delegate.getUserDisplayName(userId);
    }

    public String getUserLogin(long userId) throws RepositoryException {
        return delegate.getUserLogin(userId);
    }

    public long getUserId(String login) throws RepositoryException {
        return delegate.getUserId(login);
    }

    public String getRoleDisplayName(long roleId) throws RepositoryException {
        return delegate.getRoleDisplayName(roleId);
    }

    public Users getUsersByEmail(String email) throws RepositoryException {
        return delegate.getUsersByEmail(email, user);
    }

    public AuthenticationSchemeInfos getAuthenticationSchemes() throws RepositoryException {
        return delegate.getAuthenticationSchemes(user);
    }
}
