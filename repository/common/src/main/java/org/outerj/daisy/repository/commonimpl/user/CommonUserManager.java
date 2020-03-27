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

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryListener;

public class CommonUserManager {        
    protected final UserManagementStrategy userManagementStrategy;
    private UserCache userCache;

    public CommonUserManager(UserManagementStrategy userManagementStrategy, UserCache userCache) {
        this.userManagementStrategy = userManagementStrategy;
        this.userCache = userCache;
    }

    public RepositoryListener getCacheListener() {
        return userCache;
    }

    public Users getUsers(AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.loadUsers(user);
    }

    public long[] getUserIds(AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.getUserIds(user);
    }

    public PublicUserInfo getPublicUserInfo(long userId) throws RepositoryException {
        return userCache.getPublicUserInfo(userId);
    }

    public PublicUserInfo getPublicUserInfo(String login) throws RepositoryException {
        return userCache.getPublicUserInfo(login);
    }

    public PublicUserInfos getPublicUserInfos(AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.getPublicUserInfos(user);
    }

    public Roles getRoles(AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.loadRoles(user);
    }

    public User createUser(String login, AuthenticatedUser user) {
        return new UserImpl(userManagementStrategy, login, user);
    }

    public void deleteUser(long userId, AuthenticatedUser user) throws RepositoryException {
        userManagementStrategy.deleteUser(userId, user);
    }
    
    public Role createRole(String roleName, AuthenticatedUser user) {
        return new RoleImpl(userManagementStrategy, roleName, user);        
    }

    public void deleteRole(long roleId, AuthenticatedUser user) throws RepositoryException {
        userManagementStrategy.deleteRole(roleId, user);        
    }
    
    public void checkUser(User user) throws RepositoryException {
        if (!(user instanceof UserImpl)) {
            throw new RepositoryException("user is not an instance of UserImpl");
        }
        UserImpl userImpl = (UserImpl)user;
        if (userImpl.getIntimateAccess(userManagementStrategy, false) == null) {
            throw new RepositoryException("User was not created by this repository");
        }
    }

    public User getUser(String login, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable) {
            return userManagementStrategy.getUser(login, user);
        } else {
            User theUser = userCache.getUser(login, user);
            if (theUser.getId() != user.getId() && !user.isInAdministratorRole())
                throw new UserManagementException("Only administrators can access user records of other users.");
            return theUser;
        }
    }

    public Role getRole(String name, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable) {
            return userManagementStrategy.getRole(name, user);
        } else {
            return userCache.getRole(name);
        }
    }

    public User getUser(long userId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable) {
            return userManagementStrategy.getUser(userId, user);
        } else {
            if (userId != user.getId() && !user.isInAdministratorRole())
                throw new UserManagementException("Only administrators can access user records of other users.");
            return userCache.getUser(userId, user);
        }
    }

    public Role getRole(long roleId, boolean updateable, AuthenticatedUser user) throws RepositoryException {
        if (updateable) {
            return userManagementStrategy.getRole(roleId, user);
        } else {
            return userCache.getRole(roleId);
        }
    }

    public String getUserDisplayName(long userId) throws RepositoryException {
        return userCache.getPublicUserInfo(userId).getDisplayName();
    }

    public String getUserLogin(long userId) throws RepositoryException {
        return userCache.getPublicUserInfo(userId).getLogin();
    }

    public long getUserId(String login) throws RepositoryException {
        return userCache.getPublicUserInfo(login).getId();
    }

    public String getRoleDisplayName(long roleId) throws RepositoryException {
        return userCache.getRole(roleId).getName();
    }

    public Users getUsersByEmail(String email, AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.getUsersByEmail(email, user);
    }

    public AuthenticationSchemeInfos getAuthenticationSchemes(AuthenticatedUser user) throws RepositoryException {
        return userManagementStrategy.getAuthenticationSchemes(user);
    }
}
