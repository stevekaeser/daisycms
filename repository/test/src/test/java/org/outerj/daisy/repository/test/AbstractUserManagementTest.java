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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.Roles;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManagementException;
import org.outerj.daisy.repository.user.UserManager;

/**
 * Tests for user management.
 */
public abstract class AbstractUserManagementTest extends AbstractDaisyTestCase {

    protected boolean resetDataStores() {
        /* we want to use a clean data store
         * for this testcase, hence return
         * true in this method
         */
        return true;
    }
    
    protected abstract RepositoryManager getRepositoryManager() throws Exception;
    
    /**
     * setup this test, assumes that the repository has been freshly created and the password
     * of the system user didn't change yet - which shouldn't be a problem since the testcases
     * are running against a separate testdatabase (under normal circumstances)
     */
    private void setUpBasicUsersAndRoles() throws Exception {
        Repository repository = getRepositoryManager().getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        UserManager userMan = repository.getUserManager();
        
        setupRole(userMan, "guest");
        
        Role adminRole = userMan.getRole(1, true);
        Role userRole = userMan.getRole("User", true);
        Role guestRole = userMan.getRole("guest", true);
        
        
        User testAdminUser = userMan.createUser("testadmin");
        testAdminUser.addToRole(adminRole);
        testAdminUser.addToRole(userRole);
        testAdminUser.setDefaultRole(userRole);
        testAdminUser.setPassword("secret");
        testAdminUser.save();
        
        User user = userMan.createUser("testnonadmin");
        user.addToRole(userRole);
        user.setDefaultRole(userRole);
        user.setPassword("spa");
        user.save();
                        
    }

    private void setupRole(UserManager userMan, String roleName) throws Exception{
        try {
            userMan.getRole(roleName, true);
        } catch (UserManagementException e) {
            Role userRole = userMan.createRole(roleName);
            userRole.save();
        }
    }
    
    
    public void testUserManagement() throws Exception {
        setUpBasicUsersAndRoles();
        UserManager userManager = null;
        Repository repository = null;
        
        System.out.println("starting user management tests");
        
        //2. get the usermanager for an administrative user
        RepositoryManager repositoryManager = getRepositoryManager();
        repository = repositoryManager.getRepository(new Credentials("testadmin", "secret"));
        repository.switchRole(1);
        long[] availableRoles = repository.getAvailableRoles();

        userManager = repository.getUserManager();
        assertNotNull(userManager);

        String tmpRoleName = "Confessor"+System.currentTimeMillis();
        Role confessorRole = userManager.createRole(tmpRoleName);

        confessorRole.setDescription("if you're touched by confessor magic - you're in quite some trouble :)");

        confessorRole.save();
        //id should at this point have been updated from the data store
        assertTrue(confessorRole.getId()!=-1);

        //let's doublecheck through other method as well
        Role cfRole = userManager.getRole(tmpRoleName, true);
        assertTrue(cfRole.getId()!=-1);

        System.out.println("About to delete role with id "+confessorRole.getId());
        userManager.deleteRole(confessorRole.getId());

        try {
            userManager.getRole(tmpRoleName, true);
            assertTrue("role deletion didn't succeed", false);
        } catch (UserManagementException e6) {}

        tmpRoleName+="-partDeux";
        Role anotherConfessorRole = userManager.createRole(tmpRoleName);

        tmpRoleName+="x";
        Role yetAnotherConfessorRole = userManager.createRole(tmpRoleName);
        //this should work..
        yetAnotherConfessorRole.setName(tmpRoleName);
        yetAnotherConfessorRole.save();

        User kahlanUser = userManager.createUser("KahlanAmnell"+System.currentTimeMillis());

        kahlanUser.setPassword("ConDar");

        try {
            kahlanUser.addToRole(anotherConfessorRole);


            assertTrue("adding user to role that wasn't saved yet didn't throw an exception!", false);
        } catch (Exception e1) {}

        anotherConfessorRole.save();
        //ok, we just tried to add, role is saved now, so now we can add the user to this new role
        kahlanUser.addToRole(anotherConfessorRole);
        kahlanUser.setDefaultRole(anotherConfessorRole);
        kahlanUser.save();

        // now let's load this testuser from the manager again
        kahlanUser = userManager.getUser(kahlanUser.getLogin(), true);
        Roles wuRoles = kahlanUser.getAllRoles();
        // this user should belong to 1 role (see before)
        Role[] wuRolesArr = wuRoles.getArray();
        System.out.println("this user belongs to "+wuRolesArr.length+" roles");
        System.out.println("just checking: this user's login was "+kahlanUser.getLogin());

        assertEquals(wuRolesArr.length, 1);
        //we can safely just get the first index from the array and run our test on it
        assertEquals(wuRolesArr[0].getName(), anotherConfessorRole.getName());

        //this user has only one role, we now try to delete that role itself

        try {
            userManager.deleteRole(anotherConfessorRole.getId());
            assertTrue("deleting role when some users only have it as their " +
                    "default role didn't throw an exception", false);

        } catch (UserManagementException e5) {}

        String userToBeDeletedLogin = kahlanUser.getLogin();
        //this should still work....
        userManager.getUser(userToBeDeletedLogin, true);
        userManager.deleteUser(kahlanUser.getId());

        try {
            userManager.getUser(userToBeDeletedLogin, true);
            assertTrue("user deletion didn't succeed", false);
        } catch (UserManagementException e4) {}

        //just create another user to do further tests

        // create a user with no password... and try to save
        User warshanUser = userManager.createUser("Warshan"+System.currentTimeMillis());
        warshanUser.addToRole(anotherConfessorRole);
        warshanUser.addToRole(anotherConfessorRole);
        warshanUser.addToRole(anotherConfessorRole);
        //added three times to same role - should result in just 1 time addition
        //and should not have thrown an exception
        warshanUser.addToRole(yetAnotherConfessorRole);

        warshanUser.setDefaultRole(anotherConfessorRole);

        try {
            warshanUser.save();
            assertTrue("saving user with empty password succeeded! Houston, we have a problem!", false);
        } catch (UserManagementException e2) {}

        warshanUser.setPassword("Tyskie");

        // now let's see what happens if we save after correction...
        warshanUser.save();


        //let's go for a user update test now
        User userToUpdate = userManager.getUser(warshanUser.getLogin(), true);
        //this user should belong to two roles at this moment...
        assertEquals(userToUpdate.getAllRoles().getArray().length, 2);

        userToUpdate.setEmail("speedy@gonzales.org");
        userToUpdate.setFirstName("speedy");
        userToUpdate.setLastName("gonzales");
        userToUpdate.setPassword("chachacha");
        userToUpdate.setAuthenticationScheme("abc");

        userToUpdate.removeFromRole(anotherConfessorRole);
        userToUpdate.setDefaultRole(yetAnotherConfessorRole);

        userToUpdate.save();
        User updatedUser = userManager.getUser(warshanUser.getLogin(), true);
        assertEquals(userToUpdate.getAllRoles().getArray().length, 1);
        assertEquals(updatedUser.getEmail(), "speedy@gonzales.org");
        assertEquals(updatedUser.getFirstName(), "speedy");
        assertEquals(updatedUser.getLastName(), "gonzales");
        assertNotNull(updatedUser.getDefaultRole());
        assertEquals(updatedUser.getDefaultRole().getName(), yetAnotherConfessorRole.getName());
        assertEquals("abc", updatedUser.getAuthenticationScheme());
        //end user update test

        //role update & concurrency test
        //remark: normally we would have to actually test concurrency with another user,
        //but we can test behaviour without having to be obliged to do this.

        Role roleToUpdate = userManager.getRole(yetAnotherConfessorRole.getName(), true);
        Role roleToUpdate2 = userManager.getRole(yetAnotherConfessorRole.getName(), true);
        roleToUpdate.setDescription("bugsbunnyimitator");
        roleToUpdate.save();

        Role updatedRole = userManager.getRole(yetAnotherConfessorRole.getName(), true);
        assertEquals(updatedRole.getDescription(), "bugsbunnyimitator");
        //modify something (but we know someone else already modified at same time)
        roleToUpdate2.setDescription("TomskiAndJerrySki");
        try {
            roleToUpdate2.save();
            assertTrue("concurrency problem not detected!", false);
        } catch (RepositoryException e7) {}

        //end role update test


        // check some stuff on the test users which we put in the database ourselves.

        Role r = userManager.getRole("Administrator", true);
        assertEquals(r.getName(), "Administrator");

        // check to see if the list of all roles behaves ok
        Role[] allRoles = userManager.getRoles().getArray();
        
        // at this point there should definitely be more than 1 role in the role list.
        // theoretically we could even test to see if the net number of roles we created
        // in this test is correct in the list of all roles. 
        assertTrue(allRoles.length>1);
        for (int i = 0; i < allRoles.length; i++) {
            Role role = allRoles[i];
            assertNotNull(role);
        }
        
        // some tests on the nonadministrative testuser we put in the database ourselves
        User simpleNonAdminUser = userManager.getUser("testnonadmin", true);
        assertNotNull(simpleNonAdminUser);
        assertNotNull(simpleNonAdminUser.getDefaultRole());
        Roles simpleNonAdminRoles = simpleNonAdminUser.getAllRoles();
        
        // testing the roles of testnonadmin
        for (int i = 0; i < simpleNonAdminRoles.getArray().length; i++) {
            Role role = simpleNonAdminRoles.getArray()[i];
            assertNotNull(role);
        }

        // test direct retrieval of display names
        assertEquals(simpleNonAdminUser.getDisplayName(), userManager.getUserDisplayName(simpleNonAdminUser.getId()));

        // test caching of user objects (assuming caching is enabled)
        User nonUpdateableUser1 = userManager.getUser(simpleNonAdminUser.getId(), false);
        User nonUpdateableUser2 = userManager.getUser(simpleNonAdminUser.getId(), false);
        assertTrue("retrieving cached user two times should give same object instance", nonUpdateableUser1 == nonUpdateableUser2);

        simpleNonAdminUser.setFirstName("jules");
        simpleNonAdminUser.save();
        assertEquals("jules", userManager.getUserDisplayName(simpleNonAdminUser.getId()));

        //
        // test the "updateableByUser" flag
        //
        Role justAUser = userManager.createRole("just_a_user");
        justAUser.save();
        Role justAUser2 = userManager.createRole("just_a_user2");
        justAUser2.save();
        User herman = userManager.createUser("herman");
        herman.setPassword("herman");
        herman.addToRole(justAUser);
        herman.addToRole(justAUser2);
        herman.setDefaultRole(justAUser);
        herman.setUpdateableByUser(true);
        herman.save();

        Repository hermanRepository = repositoryManager.getRepository(new Credentials("herman", "herman"));
        herman = hermanRepository.getUserManager().getUser(herman.getId(), true);
        herman.setEmail("herman@highanddry");
        herman.save();

        Role adminRole = hermanRepository.getUserManager().getRole(Role.ADMINISTRATOR, false);
        herman.addToRole(adminRole);
        try {
            herman.save();
            fail("User should not be able to change his/her own roles.");
        } catch (Exception e) {
        }

        herman = hermanRepository.getUserManager().getUser(herman.getId(), true);
        herman.setDefaultRole(justAUser2);
        try {
            herman.save();
            fail("User should not be able to change his/her own default role.");
        } catch (Exception e) {
        }

        herman = hermanRepository.getUserManager().getUser(herman.getId(), true);
        herman.setLogin("otherHerman");
        try {
            herman.save();
            fail("User should not be able to change his/her own login.");
        } catch (Exception e) {
        }

        //
        // Test that user $system cannot login and cannot be updated
        //
        try {
            repositoryManager.getRepository(new Credentials("$system", "does_not_matter"));
            fail("Logging in as user $system should not have succeeded.");
        } catch (Exception e) {
            // no good way to check the kind of exception
        }

        try {
            User systemUser = repository.getUserManager().getUser(1, true);
            systemUser.save();
            fail("Saving the user $system should have failed.");
        } catch (Exception e) {
            // no good way to check the kind of exception
        }

        try {
            User systemUser = repository.getUserManager().getUser("$system", true);
            systemUser.save();
            fail("Saving the user $system should have failed.");
        } catch (Exception e) {
            // no good way to check the kind of exception
        }

        //
        // Test the confirmation options
        //
        Role testRole = userManager.createRole("test");
        testRole.save();
        User confirmUser = userManager.createUser("confirm");
        confirmUser.setPassword("confirm");
        confirmUser.addToRole(testRole);
        confirmUser.setDefaultRole(testRole);
        assertTrue(confirmUser.isConfirmed());
        confirmUser.save();
        assertTrue(confirmUser.isConfirmed());
        confirmUser = userManager.getUser(confirmUser.getId(), true);
        assertTrue(confirmUser.isConfirmed());
        confirmUser.setConfirmed(false);
        confirmUser.setConfirmKey("abc");
        confirmUser.save();
        confirmUser = userManager.getUser(confirmUser.getId(), true);
        assertFalse(confirmUser.isConfirmed());
        assertEquals("abc", confirmUser.getConfirmKey());

        // unconfirmed user should not be able to log in
        try {
            repositoryManager.getRepository(new Credentials("confirm", "confirm"));
            fail("Logging in with unconfirmed user should fail.");
        } catch (AuthenticationFailedException e) {
        }

        //
        // Test that a user which was last updated by himself can be deleted
        //  (was a problem before because of a foreign key constraint on the DB to itself)
        //
        User user = userManager.createUser("deleteLastUpdateTest");
        Role guestRole = userManager.getRole("guest", false);
        user.addToRole(guestRole);
        user.setDefaultRole(guestRole);
        user.setPassword("dummy");
        user.setUpdateableByUser(true);
        user.save();

        User myself = repositoryManager.getRepository(new Credentials("deleteLastUpdateTest", "dummy")).getUserManager().getUser("deleteLastUpdateTest", true);
        myself.save();

        userManager.deleteUser(user.getId());
    }
}
