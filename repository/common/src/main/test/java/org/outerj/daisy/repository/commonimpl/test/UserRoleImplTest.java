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
package org.outerj.daisy.repository.commonimpl.test;

import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.user.*;
import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.*;

/**
 * Test class for the UserImpl and RoleImpl user management objects.
 */
public class UserRoleImplTest extends TestCase {
    private static final String TESTROLE_NAME = "WhiteDwarfCreator";
    private UserManagementStrategy dummyStrategy = new DummyUserManagementStrategy();
    UserImpl user;
    RoleImpl role;
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        user = new UserImpl(dummyStrategy, "blah", new DummyAuthenticatedUser() );
        role = new RoleImpl(dummyStrategy, TESTROLE_NAME, new DummyAuthenticatedUser());
        role.setDescription("ElementaryMyDearWatson");
        //definitely need to save first because we want add this role to a user

        role.save();


        user.setEmail("warshan@tspg.org");
        user.setFirstName("Pelle");
        user.setLastName("Knark");

        user.addToRole(role);
        user.setDefaultRole(role);
        user.save();


    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testDefaultRole() throws Exception {
        //create a role which we want to assign as default
        Role defRole = new RoleImpl(dummyStrategy, TESTROLE_NAME, new DummyAuthenticatedUser());
        defRole.setDescription("ElementaryMyDearWatson");
        //definitely need to save first because we want add this role to a user
        defRole.save();

        user.addToRole(defRole);
        user.setDefaultRole(defRole);
    }

    public void testRolesUtil() {
        Role[] roleArray = new Role[5];
        roleArray[0] = this.role;

        roleArray[1] = new RoleImpl(new DummyUserManagementStrategy(), "qsdfqsdfqdsfsqdqsf", new DummyAuthenticatedUser());
        Role frequentFlyer = new RoleImpl(new DummyUserManagementStrategy(), "TheFrequentFlyer", new DummyAuthenticatedUser());
        roleArray[2] = frequentFlyer;
        roleArray[3] = new RoleImpl(new DummyUserManagementStrategy(), "H�lsristningarna", new DummyAuthenticatedUser());
        Role sfd = new RoleImpl(new DummyUserManagementStrategy(), "SummerFestivalDweller", new DummyAuthenticatedUser());
        roleArray[4] = sfd;



        //test first element
        Roles roles = new RolesImpl(roleArray);
        Role r = RolesUtil.getRole(roles, this.role.getName());
        assertEquals(r, this.role);

        //test random element
        r = RolesUtil.getRole(roles, "TheFrequentFlyer");
        assertEquals(r, frequentFlyer);

        //test last element
        r = RolesUtil.getRole(roles, "SummerFestivalDweller");
        assertEquals(r, sfd);

        roleArray = new Role[0];
        roles = new RolesImpl(roleArray);

        r = RolesUtil.getRole(roles, "mqsdfmdsj");
        assertNull(r);
    }

    public void testGetRoleXml() {
        RoleDocument roleDoc = role.getXml();
        RoleDocument.Role r = roleDoc.getRole();
        assertNotNull(r.getName());
        assertNotNull(r.getDescription());
    }

    public void testUserRoles() {
        Roles r = user.getAllRoles();
        Role[] roleArr = r.getArray();
        assertEquals(roleArr.length, 1);
    }

    public void testGetUserXml() {
        UserDocument userDoc = user.getXml();
        assertNotNull(userDoc);
        UserDocument.User u = userDoc.getUser();
        assertNotNull(u);
        assertNotNull(u.getRole());
        assertNotNull(u.getEmail());
        assertNotNull(u.getFirstName());
        assertNotNull(u.getLastName());

        //ok, and now the role stuff
        RolesDocument.Roles r = u.getRoles();
        List<RoleDocument.Role> roleArr = r.getRoleList();
        assertEquals(roleArr.size(), 1);
        RoleDocument.Role userRole = roleArr.get(0);
        assertEquals(userRole.getName(), TESTROLE_NAME);

        //default role stuff
        RoleDocument.Role defaultRole = u.getRole();
        assertEquals(defaultRole.getId(), role.getId());
        assertEquals(defaultRole.getName(), role.getName());

    }


    private class DummyAuthenticatedUser implements AuthenticatedUser {
        public long getId() {

            return 0;
        }

        public String getLogin() {
            return null;
        }

        public String getPassword() {

            return null;
        }

        public long[] getActiveRoleIds() {

            return new long[0];
        }

        public long[] getAvailableRoleIds() {

            return new long[0];
        }

        public boolean isInAdministratorRole() {
            return false;
        }

        public void setActiveRoleIds(long[] roleIds) {
        }

        public UserInfoDocument getXml() {
            return null;
        }

        public boolean isInRole(long roleId) {
            return false;
        }

    }

    private class DummyUserManagementStrategy implements UserManagementStrategy {
        private long nextUserId=4321;
        private long nextRoleId=1234;

        public Users loadUsers(AuthenticatedUser requestingUser) throws UserManagementException {
            return null;
        }

        public long[] getUserIds(AuthenticatedUser requestingUser) throws UserManagementException {
            return new long[0];
        }

        public Roles loadRoles(AuthenticatedUser requestingUser) throws UserManagementException {
            return null;
        }

        public void deleteUser(long userId, AuthenticatedUser requestingUser) throws UserManagementException {
        }

        public void deleteRole(long roleId, AuthenticatedUser requestingUser) throws UserManagementException {
        }

        public UserImpl getUser(String login, AuthenticatedUser requestingUser) throws UserManagementException {
            return null;
        }

        public RoleImpl getRole(String roleId, AuthenticatedUser requestingUser) throws UserManagementException {
            return null;
        }

        public void store(UserImpl user) throws UserManagementException {
            //let this dummy behave like the real deal
            UserImpl.IntimateAccess userInt = user.getIntimateAccess(DummyUserManagementStrategy.this);
            userInt.saved(nextUserId++, user.getFirstName(), user.getLastName(), user.getEmail(), getNowDate(), 1534, user.getUpdateCount() + 1);
        }

        public void store(RoleImpl role) throws UserManagementException {
            //let this dummy behave like the real deal
            RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(DummyUserManagementStrategy.this);
            roleInt.setLastModified(getNowDate());
            roleInt.setLastModifier(1534);
            roleInt.saved(nextRoleId++, role.getName(), role.getDescription(), role.getUpdateCount() + 1);
        }

        private Date getNowDate() {
            Date d = new Date(System.currentTimeMillis());
            return d;
        }

        public UserImpl getUser(long userId, AuthenticatedUser user) throws UserManagementException {
            return null;
        }

        public RoleImpl getRole(long roleId, AuthenticatedUser user) throws UserManagementException {
            return null;
        }

        public UsersImpl getUsersByEmail(String email, AuthenticatedUser user) throws RepositoryException {
            return null;
        }

        public AuthenticationSchemeInfos getAuthenticationSchemes(AuthenticatedUser requestingUser) throws RepositoryException {
            return null;
        }

        public PublicUserInfo getPublicUserInfo(long userId, AuthenticatedUser user) throws RepositoryException {
            return null;
        }

        public PublicUserInfo getPublicUserInfo(String login, AuthenticatedUser user) throws RepositoryException {
            return null;
        }

        public PublicUserInfos getPublicUserInfos(AuthenticatedUser user) throws RepositoryException {
            return null;
        }
    }
}
