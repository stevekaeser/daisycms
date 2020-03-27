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

import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.PublicUserInfo;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

// Implementation note:
//  The user cache can work in two modes depending on whether the cacheUser is
//  in the Administrator role or not.
//  If the cacheUser in in the Administrator role, the cache user can retrieve
//  the full user objects. If the cacheUser is not in the Administrator role,
//  the full user objects can still be retrieved by using the user that
//  requested the user object. However, if a non-Admin user accesses the
//  public user info, and the cache user is also a non-Admin user, then
//  we cannot retrieve the full user object and therefore use a cache of
//  only public user infos (for performance reasons, it is important that
//  this info is cached).
//  Supporting non-Administrator role cache users is interesting for the remote
//  Java API implementation, which would otherwise only be usable with
//  an Administrator user.
public class UserCache implements RepositoryListener {
    private Map users = Collections.synchronizedMap(new HashMap());
    private Map usersByLogin = Collections.synchronizedMap(new HashMap());

    private Map usersPublicInfo = Collections.synchronizedMap(new HashMap());
    private Map usersPublicInfoByLogin = Collections.synchronizedMap(new HashMap());

    private Map roles = Collections.synchronizedMap(new HashMap());
    private Map rolesByName = Collections.synchronizedMap(new HashMap());
    private org.outerj.daisy.repository.commonimpl.AuthenticatedUser cacheUser;
    private UserManagementStrategy userManagementStrategy;
    private boolean adminMode;
    private final static PublicUserInfo NONEXISTING_USER = new PublicUserInfoImpl(-1, "Non-existing user", "A non-existing user");

    public UserCache(UserManagementStrategy userManagementStrategy, org.outerj.daisy.repository.commonimpl.AuthenticatedUser cacheUser) {
        this.cacheUser = cacheUser;
        this.userManagementStrategy = userManagementStrategy;
        adminMode = cacheUser.isInAdministratorRole();
    }

    public UserImpl getUser(long userId) throws RepositoryException {
        return getUser(userId, cacheUser);
    }

    public UserImpl getUser(long userId, AuthenticatedUser requestingUser) throws RepositoryException {
        Long key = new Long(userId);
        UserImpl user = (UserImpl)users.get(key);
        if (user == null) {
            user = userManagementStrategy.getUser(userId, requestingUser);
            user.makeReadOnly();
            synchronized (this) {
                users.put(key, user);
                usersByLogin.put(user.getLogin(), user);
            }
        }
        return user;
    }

    public UserImpl getUser(String login) throws RepositoryException {
        return getUser(login, cacheUser);
    }

    public UserImpl getUser(String login, AuthenticatedUser requestingUser) throws RepositoryException {
        UserImpl user = (UserImpl)usersByLogin.get(login);
        if (user == null) {
            user = userManagementStrategy.getUser(login, requestingUser);
            user.makeReadOnly();
            synchronized (this) {
                users.put(new Long(user.getId()), user);
                usersByLogin.put(user.getLogin(), user);
            }
        }
        return user;
    }

    public PublicUserInfo getPublicUserInfo(long userId) throws RepositoryException {
        if (adminMode) {
            return getUser(userId).getPublicUserInfo();
        } else {
            PublicUserInfo publicUserInfo = (PublicUserInfo)usersPublicInfo.get(new Long(userId));
            if (publicUserInfo == null) {
                try {
                    publicUserInfo = userManagementStrategy.getPublicUserInfo(userId, cacheUser);
                } catch (UserNotFoundException e) {
                    publicUserInfo = NONEXISTING_USER;
                }
                synchronized(this) {
                    usersPublicInfo.put(new Long(userId), publicUserInfo);
                    usersPublicInfoByLogin.put(publicUserInfo.getLogin(), publicUserInfo);
                }
            }
            if (publicUserInfo == NONEXISTING_USER)
                throw new UserNotFoundException(userId);
            return publicUserInfo;
        }
    }

    public PublicUserInfo getPublicUserInfo(String login) throws RepositoryException {
        if (adminMode) {
            return getUser(login).getPublicUserInfo();
        } else {
            PublicUserInfo publicUserInfo = (PublicUserInfo)usersPublicInfoByLogin.get(login);
            if (publicUserInfo == null) {
                publicUserInfo = userManagementStrategy.getPublicUserInfo(login, cacheUser);
                synchronized(this) {
                    usersPublicInfo.put(new Long(publicUserInfo.getId()), publicUserInfo);
                    usersPublicInfoByLogin.put(publicUserInfo.getLogin(), publicUserInfo);
                }
            }
            return publicUserInfo;
        }
    }

    public RoleImpl getRole(long roleId) throws RepositoryException {
        Long key = new Long(roleId);
        RoleImpl role = (RoleImpl)roles.get(key);
        if (role == null) {
            role = userManagementStrategy.getRole(roleId, cacheUser);
            role.makeReadOnly();
            synchronized (this) {
                roles.put(key, role);
                rolesByName.put(role.getName(), role);
            }
        }
        return role;
    }

    public RoleImpl getRole(String roleName) throws RepositoryException {
        RoleImpl role = (RoleImpl)rolesByName.get(roleName);
        if (role == null) {
            role = userManagementStrategy.getRole(roleName, cacheUser);
            role.makeReadOnly();
            synchronized (this) {
                roles.put(new Long(role.getId()), role);
                rolesByName.put(roleName, role);
            }
        }
        return role;
    }

    public void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount) {
        if (eventType == RepositoryEventType.USER_UPDATED || eventType == RepositoryEventType.USER_DELETED) {
            synchronized (this) {
                User user = (User)users.get(id);
                if (user != null && user.getUpdateCount() == updateCount)
                    return;
                if (user != null) {
                    users.remove(id);
                    usersByLogin.remove(user.getLogin());
                }

                PublicUserInfo publicUserInfo = (PublicUserInfo)usersPublicInfo.get(id);
                if (publicUserInfo != null) {
                    usersPublicInfo.remove(id);
                    usersPublicInfo.remove(publicUserInfo.getLogin());
                }
            }
        } else if (eventType == RepositoryEventType.ROLE_UPDATED || eventType == RepositoryEventType.ROLE_DELETED) {
            synchronized (this) {
                RoleImpl role = (RoleImpl)roles.get(id);
                if (role != null && role.getUpdateCount() == updateCount)
                    return;

                // roles can belong to various users, just clear entire user cache
                users.clear();
                usersByLogin.clear();
                if (role != null) {
                    roles.remove(new Long(role.getId()));
                    rolesByName.remove(role.getName());
                }

                // note: public user info does not contain role information, so its cache can remain
            }
        }
        // Note: upon role or user creation, we don't need to do anything.
    }

}
