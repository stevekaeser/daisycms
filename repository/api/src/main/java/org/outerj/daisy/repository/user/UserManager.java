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
package org.outerj.daisy.repository.user;

import org.outerj.daisy.repository.RepositoryException;

/**
 * Manager for all things user related.
 *
 * <p>The UserManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getUserManager()}.
 *
 */
public interface UserManager {
    /**
     * Returns all available users. Only Administrators can do this.
     */
    Users getUsers() throws RepositoryException;

    long[] getUserIds() throws RepositoryException;

    /**
     * Returns the publicly available information for a user.
     */
    PublicUserInfo getPublicUserInfo(long userId) throws RepositoryException;

    /**
     * Returns the publicly available information for a user.
     */
    PublicUserInfo getPublicUserInfo(String login) throws RepositoryException;

    /**
     * Returns the public information of all users. Contrary to
     * {@link #getUsers()}, this method can be called by any user.
     */
    PublicUserInfos getPublicUserInfos() throws RepositoryException;

    /**
     * Returns all available roles.
     */
    Roles getRoles() throws RepositoryException;
    
    /**
     * Creates a new User.
     *
     * <p>The persistency of this object towards the data store
     * is the responsibility of the client using the User
     * object itself by calling the {@link User#save()} method.

     * @param login the user login used when authenticating
     * @return a User object which isn't persistent yet.
     */
    User createUser(String login);

    /**
     * Deletes the User with data store id userId
     * @param userId data store id of the User to delete
     */
    void deleteUser(long userId) throws RepositoryException;
    
    /**
     * Return the User object which is identified by data store id userId.
     *
     * <p>Only administrators can retrieve the User object for users that are
     * not themselve.
     *
     * @param userId the data store id of the desired User object
     * @return the User object corresponding to data store id userId
     */
    User getUser(long userId, boolean updateable) throws RepositoryException;

    /**
     * Return the Role object which is identified by data store id roleId
     * @param roleId the data store id of the desired Role object
     * @return the Role object corresponding to data store id roleId
     */
    Role getRole(long roleId, boolean updateable) throws RepositoryException;
    
    /**
     * Return the User object which is identified by the specified userLogin
     * @param userLogin the login by which the desired User object is identified
     * @return the User object for the user with login userLogin
     */
    User getUser(String userLogin, boolean updateable) throws RepositoryException;

    /**
     * Return the Role object which is identified by the specified roleName
     * @param roleName the name by which the desired Role object is identified
     * @return the Role object for the role with name roleName
     */
    Role getRole(String roleName, boolean updateable) throws RepositoryException;
    
    /**
     * Creates a new Role.
     *
     * @param roleName
     * @return a Role object
     */
    Role createRole(String roleName);

    /**
     * Deletes the Role with data store id roleId
     * @param roleId data store id of the Role to delete
     */
    void deleteRole(long roleId) throws RepositoryException;

    /**
     * Retrieves the display name of a user, using the user cache for quick access.
     * Only administrators are allowed to access the full user object, so this method
     * enables 'normal' users to resolve user ids to names.
     *
     * <p>This is the same as otherwise retrieved from {@link User#getDisplayName()}.
     *
     * @throws UserNotFoundException if the user doesn't exist
     */
    String getUserDisplayName(long userId) throws RepositoryException;

    /**
     * Retrieves the login of a user.
     * Only administrators are allowed to access the full user object, so this method
     * enables 'normal' users to resolve user ids to logins.
     *
     * <p>This is the same as otherwise retrieved from {@link User#getLogin()}.
     *
     * @throws UserNotFoundException if the user doesn't exist
     */
    String getUserLogin(long userId) throws RepositoryException;

    /**
     * Retrieves the id of a user based on its login. This method can be used
     * instead of getUser(login).getId() for non-administrator users.
     *
     * @throws UserNotFoundException if the user doesn't exist
     */
    long getUserId(String login) throws RepositoryException;

    /**
     * Retrieves the name of a role, using the user cache for quick access.
     *
     * <p>This is the same as otherwise retrieved from {@link Role#getName()}.
     *
     * @throws RoleNotFoundException if the role doesn't exist
     */
    String getRoleDisplayName(long roleId) throws RepositoryException;

    Users getUsersByEmail(String email) throws RepositoryException;

    AuthenticationSchemeInfos getAuthenticationSchemes() throws RepositoryException;
}
