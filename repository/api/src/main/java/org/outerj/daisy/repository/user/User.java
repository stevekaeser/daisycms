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

import java.util.Date;

import org.outerx.daisy.x10.UserDocument;
import org.outerj.daisy.repository.RepositoryException;

/**
 * A repository user.
 */
public interface User {
    /**
     * Sets this User's default role.
     *
     * <p>If the user is not yet associated with this role
     * (via {@link #addToRole(Role)}), then this will be
     * done implicitely.
     *
     * <p>You can specify null to unset the default role.
     *
     * <p>A default role is optional, if a user does not have
     * a default role, then the authentication code will itself
     * decide which role(s) to take as default.
     */
    void setDefaultRole(Role role) throws UserManagementException;

    /**
     * Gets the default role for this user, can return null
     * if there is no default role assigned.
     */
    Role getDefaultRole();
    
    /**
     * Adds the User to the specified Role role. If the user is added
     * to the same role twice, nothing happens (no exception is thrown).
     * In that case, the user will only belong to that role once.
     *
     * @param role the Role to add the user to
     */
    void addToRole(Role role);
    
    /**
     * Remove the User from the specified Role role.
     *
     * @param role the Role to remove the user from
     */
    void removeFromRole(Role role);
    
    /**
     * Returns the Roles to which the user belongs
     */
    Roles getAllRoles();

    /**
     * Returns the ids of the roles to which the user belongs.
     */
    long[] getAllRoleIds();

    /**
     * Sets the password for this user.
     * 
     * <p>For security reasons, <b>getPassword</b>
     * is <b>not</b> provided.
     */
    void setPassword(String password);

    /**
     * Sets this User's e-mail.
     * 
     * @param emailAddress allowed to be null
     */
    void setEmail(String emailAddress);

    /**
     * Gets the email address of this user, or null if not assigned.
     */
    String getEmail();

    /**
     * Returns the user login.
     */
    String getLogin();
    
    /**
     * Sets the user login string.
     */
    void setLogin(String loginName);

    /**
     * Returns the user id.
     */
    long getId();
    
    /**
     * Sets the first name of the user (can be null).
     */
    void setFirstName(String firstName);

    /**
     * Sets the last name of the user (can be null).
     */
    void setLastName(String lastName);

    /**
     * Gets the first name of the user, could be null.
     */
    String getFirstName();

    /**
     * Gets the last name of the user, could be null.
     */
    String getLastName();

    /**
     * Returns either the concatenation of firstName and lastName,
     * if at least one of those has a value, or otherwise the login.
     */
    String getDisplayName();

    /**
     * Persist this User to a data store.
     */
    void save() throws RepositoryException;
    
    Date getLastModified();

    /**
     * Gets the user id of the last modifier.
     */
    long getLastModifier();
    
    //Implement later:
    //Profile getProfile();
    
    /**
     * Gets the XML representation of this object.
     */
    UserDocument getXml();

    /**
     * <b>Clears all roles</b> for this user and <b>sets the default role</b> to <b>null</b>.
     */
    void clearRoles();

    /**
     * Returns true if the user belongs to the specified role.
     */
    boolean hasRole(long roleId);

    /**
     * Returns true if the user to who this user object corresponds
     * can update this object. This allows the user to change its
     * login, email, password, etc. but not the roles he/she has.
     */
    boolean isUpdateableByUser();

    /**
     * @see #isUpdateableByUser()
     */
    void setUpdateableByUser(boolean updateableByUser);

    boolean isConfirmed();

    void setConfirmed(boolean confirmed);

    String getConfirmKey();

    void setConfirmKey(String confirmKey);

    String getAuthenticationScheme();

    void setAuthenticationScheme(String schemeName);

    long getUpdateCount();

    PublicUserInfo getPublicUserInfo();

}
