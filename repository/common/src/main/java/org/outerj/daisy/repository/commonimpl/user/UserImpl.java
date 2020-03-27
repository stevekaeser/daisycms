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

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.Roles;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.PublicUserInfo;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.RoleDocument;
import org.outerx.daisy.x10.RolesDocument;
import org.outerx.daisy.x10.UserDocument;

public class UserImpl implements User {
    private UserManagementStrategy userManagementStrategy;
    /** 
     * the userId as it is known in the <b>data store</b>.
     * 
     * <p>Not to be confused with <b>login</b>!
     * 
     * <p>Initialize on -1 until the save method is called.
     * As soon as the User is persisted, this id
     * can be updated with the actual value. This allows
     * a user of this class to distinguish between
     * persisted and not yet persisted User objects. 
     *
     * <p>After the UserManagementStrategy has been invoked
     * in order to persist the User, this 
     * UserManagementStrategy itself calls a method of the
     * Object that was used to invoke the UserManagementStrategy
     * in the first place.
     * 
     */
    private long userId=-1;
    private String login;
    private RoleImpl defaultRole;
    private String email;
    private String firstName;
    private String lastName;
    private String passWord;
    private boolean updateableByUser = false;
    private Date lastModified;
    private long lastModifier;
    private AuthenticatedUser requestingUser;
    private Map roles = new HashMap();
    private long updateCount = 0;
    private boolean confirmed = true;
    private String confirmKey;
    private boolean roleChanges=false;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private boolean readOnly = false;
    private String authenticationScheme = "daisy";
    private static final String READ_ONLY_MESSAGE = "This User object is read-only.";
    private PublicUserInfo publicUserInfo;

    /**
     * creates a new User
     * @param userManagementStrategy the storage manipulation strategy to use
     * @param login the login name of the new User
     * @param requestingUser the authenticated, administrative user that requested this UserImpl object
     */
    public UserImpl(UserManagementStrategy userManagementStrategy, String login, AuthenticatedUser requestingUser) {
        this.userManagementStrategy = userManagementStrategy;
        this.login = login;
        this.requestingUser = requestingUser;
    }

    public void setDefaultRole(Role role) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (role == null) {
            this.defaultRole = null;
        } else {
            preRoleAddChecks(role);
            addToRole(role);
            this.defaultRole = (RoleImpl)role;
        }
    }

    private void preRoleAddChecks(Role role) {
        if (role == null)
            throw new IllegalArgumentException("Role object cannot be null.");

        if (!(role instanceof RoleImpl))
            throw new IllegalArgumentException("Unsupported Role object supplied.");

        if (((RoleImpl)role).getIntimateAccess(userManagementStrategy) == null)
            throw new IllegalArgumentException("Role object is not loaded from the same Repository as this User object.");

        if (role.getId() == -1)
            throw new IllegalArgumentException("Only roles which have already been created in the repository can be added to a User.");
    }

    public Role getDefaultRole() {
        return defaultRole;
    }

    public Roles getAllRoles() {
        return getRolesFromMap();
    }

    public long[] getAllRoleIds() {
        long[] ids = new long[roles.size()];
        Iterator rolesIt = roles.values().iterator();
        int i = 0;
        while (rolesIt.hasNext()) {
            ids[i++] = ((Role)rolesIt.next()).getId();
        }
        return ids;
    }

    public void setPassword(String s) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        passWord = s;
    }

    public void setEmail(String emailAddress) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        email = emailAddress;
    }

    public String getEmail() {
        return email;
    }

    public long getId() {
        return userId;
    }

    /**
     * persists the state of this object to the data store
     */

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        userManagementStrategy.store(this);
    }

    public UserDocument getXml() {        
        UserDocument userDocument = UserDocument.Factory.newInstance();
        UserDocument.User userXml = userDocument.addNewUser();

        if (defaultRole != null) {
            RoleDocument.Role defaultRoleXml = defaultRole.getXml().getRole();
            userXml.setRole(defaultRoleXml);
        }

        if (email != null)
            userXml.setEmail(email);
        if (firstName != null)
            userXml.setFirstName(firstName);
        if (lastName != null)
            userXml.setLastName(lastName);
        userXml.setLogin(login);
        if (passWord != null)
            userXml.setPassword(passWord);
        userXml.setUpdateCount(updateCount);
        userXml.setUpdateableByUser(updateableByUser);
        userXml.setConfirmed(confirmed);
        if (confirmKey != null)
            userXml.setConfirmKey(confirmKey);
        userXml.setAuthenticationScheme(authenticationScheme);

        Roles r = getRolesFromMap();
        RolesDocument.Roles rolesXml = r.getXml().getRoles();
        userXml.setRoles(rolesXml);
        
        if (userId!=-1) {
            GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
            lastModifiedCalendar.setTime(lastModified);
            
            userXml.setLastModified(lastModifiedCalendar);
            userXml.setLastModifier(lastModifier);
            
            userXml.setId(userId);
        }
        return userDocument;
    }

    private Roles getRolesFromMap() {
        Role[] rolesArray = (Role[])roles.values().toArray(new Role[roles.size()]);
        Roles r = new RolesImpl(rolesArray);
        return r;
    }

    public void addToRole(Role role) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        preRoleAddChecks(role);

        if (!roles.containsKey(new Long(role.getId()))) {
            roleChanges = true;
            roles.put(new Long(role.getId()), role);
        }
    }

    public void removeFromRole(Role role) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        roleChanges = true;
        roles.remove(new Long(role.getId()));
    }

    public String getLogin() {
        return login;
    }

    public void setFirstName(String firstName) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getDisplayName() {
        return getDisplayName(firstName, lastName, login);
    }

    public static String getDisplayName(String firstName, String lastName, String login) {
        String name;
        if (firstName == null && lastName == null) {
            name = login;
        } else if (firstName != null && lastName != null) {
            name = firstName + " " + lastName;
        } else if (lastName != null) {
            name = lastName;
        } else {
            name = firstName;
        }
        return name;
    }

    /**
     * Disables all operations that can modify the state of this object.
     */
    public void makeReadOnly() {
        this.readOnly = true;

        // also make associated roles read-only
        if (defaultRole != null)
            defaultRole.makeReadOnly();
        Iterator roleIt = roles.values().iterator();
        while (roleIt.hasNext()) {
            ((RoleImpl)roleIt.next()).makeReadOnly();
        }

        publicUserInfo = new PublicUserInfoImpl(getId(), getLogin(), getDisplayName());
    }
    
    public IntimateAccess getIntimateAccess(UserManagementStrategy strategy, boolean checkReadOnly) {
        if (checkReadOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (this.userManagementStrategy == strategy)
            return intimateAccess;
        else
            return null;
    }
    
    public IntimateAccess getIntimateAccess(UserManagementStrategy strategy) {
        return this.getIntimateAccess(strategy, readOnly);
    }
    
    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return lastModified;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    /**
     * <p>Checks if a supplied password is valid.
     * 
     * <p>Currently the rules for validity are:
     * <ol>
     *  <li>Must be different from null
     *  <li>Must have a length of at least 1 character
     * </ol>
     * @param password the password to check for validity
     * @return true if valid, false if not valid
     */
    public static boolean isValidPassword(String password) {
        boolean isValid = false;
        if (password!=null){
            if (password.length()>=1) isValid=true;
        }
        return isValid;
    }

    public void clearRoles() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.roles.clear();
        this.roleChanges = true;
        this.defaultRole = null;
    }

    public void setLogin(String loginName) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.login = loginName;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public boolean hasRole(long roleId) {
        return roles.containsKey(new Long(roleId));
    }

    public boolean isUpdateableByUser() {
        return updateableByUser;
    }

    public void setUpdateableByUser(boolean updateableByUser) {
        this.updateableByUser = updateableByUser;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public String getConfirmKey() {
        return confirmKey;
    }

    public void setConfirmKey(String confirmKey) {
        this.confirmKey = confirmKey;
    }

    public String getAuthenticationScheme() {
        return authenticationScheme;
    }

    public void setAuthenticationScheme(String schemeName) {
        if (schemeName == null)
            throw new IllegalArgumentException("schemeName cannot be null.");
        if (schemeName.length() < 1)
            throw new IllegalArgumentException("schemeName cannot be an empty string.");

        this.authenticationScheme = schemeName;
    }

    public PublicUserInfo getPublicUserInfo() {
        if (publicUserInfo != null)
            return publicUserInfo;

        return new PublicUserInfoImpl(getId(), getLogin(), getDisplayName());
    }

    /**
     * provides intimate access to the UserImpl.
     */
    public class IntimateAccess {
        private IntimateAccess() {
        }

        public void setLastModified(Date lastModDate) {
            lastModified = lastModDate;
        }
        public void setLastModifier(long lastMod) {
            lastModifier = lastMod;
        }

        public AuthenticatedUser getCurrentUser() {
            return requestingUser;
        }

        public void saved(long id, String firstName, String lastName, String email, Date lastModified, long lastModifier, long updateCount) {
            userId = id;
            UserImpl.this.firstName = firstName;
            UserImpl.this.lastName = lastName;
            UserImpl.this.email = email;
            UserImpl.this.lastModified = lastModified;
            UserImpl.this.lastModifier = lastModifier;
            UserImpl.this.updateCount = updateCount;
            roleChanges=false;
        }

        /**
         * We <b>only</b> allow access to the supplied password value through Intimate Access!
         * @return the password supplied by the user, meant to be stored in the data store immediately
         */
        public String getPassword() {
            return passWord;
        }

        public void addToRole(Role r) {
            roles.put(new Long(r.getId()), r);
        }

        /**
         * marks if this user has role changes or not
         */
        public boolean hasRoleChanges() {
            return roleChanges;
        }
    }
}
