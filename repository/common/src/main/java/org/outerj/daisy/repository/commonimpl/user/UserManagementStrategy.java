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
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;

/**
 * <p>Allows to customise the behaviour of the abstract implementation classes of the
 * repository User Management API.
 *
 * <p>For (important) general information about this and other strategy interfaces, see also
 * {@link org.outerj.daisy.repository.commonimpl.DocumentStrategy}.
 */
public interface UserManagementStrategy {

    /**
     * returns all the Users in the system
     */
    Users loadUsers(AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * Returns the IDs of all users in the system.
     */
    long[] getUserIds(AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * returns all the roles in the system
     */
    Roles loadRoles(AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * deletes the user with given userID from the system
     * @param userId the data store id of the User to delete
     */
    void deleteUser(long userId, AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * deletes a role from the system
     * @param roleId the data store id of the role to delete
     */
    void deleteRole(long roleId, AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * @param login login of the user object
     * @param requestingUser the authenticated user that requests this object
     */
    UserImpl getUser(String login, AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * @param roleName the name of the role you want to obtain
     * @param requestingUser the authenticated user that requests this object
     * @return the Role object with the specified roleName
     */
    RoleImpl getRole(String roleName, AuthenticatedUser requestingUser) throws RepositoryException;

    /**
     * stores the specified UserImpl to the data store
     * @param user the UserImpl object to persist
     */
    void store(UserImpl user) throws RepositoryException;

    /**
     * stores the specified RoleImpl to the data store
     * @param role the RoleImpl object to persist
     */
    void store(RoleImpl role) throws RepositoryException;

    /**
     * @param userId the data stoe id of the user object to fetch
     * @param user the authenticated user that requests this object
     */
    UserImpl getUser(long userId, AuthenticatedUser user) throws RepositoryException;

    /**
     * @param roleId the data store id of the role object to fetch
     * @param user the authenticated user that requests this object
     */
    RoleImpl getRole(long roleId, AuthenticatedUser user) throws RepositoryException;

    PublicUserInfo getPublicUserInfo(long userId, AuthenticatedUser user) throws RepositoryException;

    PublicUserInfo getPublicUserInfo(String login, AuthenticatedUser user) throws RepositoryException;

    PublicUserInfos getPublicUserInfos(AuthenticatedUser user) throws RepositoryException;

    UsersImpl getUsersByEmail(String email, AuthenticatedUser requestingUser) throws RepositoryException;

    AuthenticationSchemeInfos getAuthenticationSchemes(AuthenticatedUser requestingUser) throws RepositoryException;
}
