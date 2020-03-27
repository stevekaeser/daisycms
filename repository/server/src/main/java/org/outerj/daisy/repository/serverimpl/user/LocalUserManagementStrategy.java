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
package org.outerj.daisy.repository.serverimpl.user;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.security.MessageDigest;

import org.apache.xmlbeans.XmlObject;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.logging.Log;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.user.*;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.repository.serverimpl.EventHelper;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager.Context;
import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerx.daisy.x10.*;

public class LocalUserManagementStrategy implements UserManagementStrategy {
    private static final String SELECT_ALL_FROM_USERS = "select id, login, default_role, first_name, last_name, email, updateable_by_user, confirmed, confirmkey, auth_scheme, last_modified, last_modifier,updatecount from users";
    private static final String SELECT_USER_WHERE_LOGIN = SELECT_ALL_FROM_USERS+" where login=?";
    private static final String SELECT_USER_WHERE_ID = SELECT_ALL_FROM_USERS+" where id=?";
    private static final String SELECT_USERS_WHERE_EMAIL = SELECT_ALL_FROM_USERS+" where email=?";
    private static final String SELECT_ALL_FROM_ROLES = "select id, name, description, last_modified, last_modifier, updatecount from roles";
    private static final String SELECT_ROLE_WHERE_NAME = SELECT_ALL_FROM_ROLES+" where name=?";
    private static final String SELECT_ROLE_WHERE_ID = SELECT_ALL_FROM_ROLES+" where id=?";
    private static final String SELECT_ROLES_WHERE_USERID = "select role_id from user_roles where user_id=?";
    private static final String INSERT_ROLES = "insert into user_roles(role_id, user_id) values (?,?)";
    private static final String DELETE_ROLES = "delete from user_roles where user_id=?";
    private Log logger;
    private Context context;
    private JdbcHelper jdbcHelper;
    private EventHelper eventHelper;
    private AuthenticatedUser systemUser;

    /**
     * @param context
     */
    public LocalUserManagementStrategy(Context context, JdbcHelper jdbcHelper, AuthenticatedUser systemUser) {
        this.context = context;
        this.logger = context.getLogger();
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);
        this.systemUser = systemUser;
    }
    
    public Users loadUsers(AuthenticatedUser requestingUser) throws UserManagementException {
        if (!requestingUser.isInAdministratorRole())
            throw new UserManagementException("Non-Administrator users cannot retrieve other users then themselve.");

        Connection conn = null;
        PreparedStatement stmt = null;
        List<User> users = new ArrayList<User>();
        
        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement(SELECT_ALL_FROM_USERS);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UserImpl user = getUserFromResultSet(rs, requestingUser);
                users.add(user);
            }
        } catch (Exception e) {
            throw new UserManagementException("Error while loading user list.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        
        return new UsersImpl(users.toArray(new User[users.size()]));
    }

    private UserImpl getUserFromResultSet(ResultSet rs, AuthenticatedUser requestingUser) throws SQLException, UserManagementException {
        long id = rs.getLong(1);
        String login = rs.getString(2);
        long defaultRoleId = rs.getLong(3);
        String firstName = rs.getString(4);
        String lastName = rs.getString(5);
        String email = rs.getString(6);
        boolean updateableByUser = rs.getBoolean(7);
        boolean confirmed = rs.getBoolean(8);
        String confirmKey = rs.getString(9);
        String authenticationScheme = rs.getString(10);
        Date lastModified = rs.getTimestamp(11);
        long lastModifier = rs.getLong(12);
        long updateCount = rs.getLong(13);

        UserImpl user = new UserImpl(this, login, requestingUser);
        UserImpl.IntimateAccess userInt = user.getIntimateAccess(this);
        // TODO using the saved() method here to set the object state is conceptually wrong,
        //      the purpose of that method is to update the object state after saving it
        userInt.saved(id, firstName, lastName, email, lastModified, lastModifier, updateCount);
        user.setUpdateableByUser(updateableByUser);
        user.setConfirmed(confirmed);
        user.setConfirmKey(confirmKey);
        user.setAuthenticationScheme(authenticationScheme);

        Role[] userRoles = getUserRoles(id, requestingUser);
        for (Role role : userRoles) {
            userInt.addToRole(role);
        }

        if (defaultRoleId == -1) {
            user.setDefaultRole(null);
        } else {
            Role defaultRole = RolesUtil.getRole(user.getAllRoles(), defaultRoleId);
            user.setDefaultRole(defaultRole);
        }

        return user;
    }

    public long[] getUserIds(AuthenticatedUser requestingUser) throws UserManagementException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select id from users");
            LongList userIds = new ArrayLongList();
            while (rs.next())
                userIds.add(rs.getLong(1));
            return userIds.toArray();
        } catch (Exception e) {
            throw new UserManagementException("Error while loading user list.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Roles loadRoles(AuthenticatedUser requestingUser) throws UserManagementException {
        Connection conn = null;
        PreparedStatement stmt = null;
        List<Role> roles = new ArrayList<Role>();
        
        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement(SELECT_ALL_FROM_ROLES);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String description = rs.getString(3);
                Date lastModified = rs.getTimestamp(4);
                long lastModifier = rs.getLong(5);
                RoleImpl role = new RoleImpl(this, name, requestingUser);
                role.setDescription(description);
                RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
                roleInt.setId(id);
                roleInt.setLastModified(lastModified);
                roleInt.setLastModifier(lastModifier);
                roles.add(role);
            }
            
            rs.close();
        } catch (Exception e) {
            throw new UserManagementException("Error while loading role list.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        
        return new RolesImpl(roles.toArray(new Role[roles.size()]));
    }
    
    public void deleteUser(long userId, AuthenticatedUser requestingUser) throws UserManagementException {
        if (logger.isDebugEnabled())
            logger.debug("start user deletion for user with login "+userId);

        if (!requestingUser.isInAdministratorRole())
            throw new UserManagementException("Only Administrators can delete users.");

        if (userId == 1)
            throw new UserManagementException("The system user (id 1) cannot be deleted.");
        
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            //yes...transaction necessary.
            jdbcHelper.startTransaction(conn);

            UserImpl user = getUser(userId, requestingUser);

            logger.debug("first we need to remove child records from user_roles");

            stmt = conn.prepareStatement("delete from user_roles where user_id=?");
            stmt.setLong(1, userId);
            
            stmt.executeUpdate();
            stmt.close();
            logger.debug("user_roles should be clear for this user. User can be deleted now");

            // Before deleting the user, set its last_modifier to 1 (system user ID), because
            // it could be that the user last saved itself in which case deleting will
            // fail because of the foreign key constraint that protects users from being
            // deleted if they are still referenced as last_modifier of users.
            stmt = conn.prepareStatement("update users set last_modifier = 1 where id = ?");
            stmt.setLong(1, userId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from users where id=?");
            stmt.setLong(1, userId);
            
            int modifiedRecords = stmt.executeUpdate();
            if (modifiedRecords!=1) {
                throw new UserManagementException("Something went wrong " +
                        "when trying to delete user with login "+userId+". " +
                        "No records were affected.");
            }

            XmlObject eventDescription = createUserDeletedEvent(user, requestingUser.getId());
            eventHelper.createEvent(eventDescription, "UserDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new UserManagementException("Error deleting the user with ID " + userId
                    + ". This might be because the user is still in use (referenced by other objects such as documents), in which case it is impossible to delete the user.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            logger.debug("end user deletion");
        }

        // fire synchronous events
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_DELETED, new Long(userId), -1);
    }

    private UserDeletedDocument createUserDeletedEvent(UserImpl user, long deleterId) {
        UserDeletedDocument userDeletedDocument = UserDeletedDocument.Factory.newInstance();
        UserDeletedDocument.UserDeleted userDeleted = userDeletedDocument.addNewUserDeleted();
        userDeleted.setDeleterId(deleterId);
        userDeleted.setDeletedTime(new GregorianCalendar());
        UserDocument.User userXml = user.getXml().getUser();
        if (userXml.isSetPassword())
            userXml.unsetPassword();
        userDeleted.addNewDeletedUser().setUser(userXml);
        return userDeletedDocument;
    }

    public void deleteRole(long roleId, AuthenticatedUser requestingUser) throws UserManagementException {
        if (!requestingUser.isInAdministratorRole())
            throw new UserManagementException("Only Administrators can delete roles.");

        if (roleId == 1)
            throw new UserManagementException("The Administrator role (id 1) cannot be deleted.");

        /* TRICKY SITUATION:
         * 
         * suppose a role is deleted. Then we first remove all related records
         * from the user_roles table. We then check all the users who have
         * their default role pointing towards the role-to-be deleted.
         * 
         * Suppose we find out that some users still have this role as their
         * default role AND IT IS THEIR ONLY ROLE! This is tricky. What to do?
         * 
         * Do we:
         * 1) either: delete these users
         * 2) or: throw an exception (preferred!) - we can provide an extra api call
         *   that enables an admin to switch a specified default role to another
         *   one. Also relieves us from the tedious task to switch default role
         *   for users who DO have other roles to another role. Which role to 
         *   choose there? Therefore: just throw an exception in that case
         *   and let the role switching task be the responsability of an admin!
         * 
         */
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement("select count(*) from users where default_role=?");
            stmt.setLong(1, roleId);
            
            ResultSet rs = stmt.executeQuery();
            
            rs.next();
            // Let's see if the admin needs to do some work :)
            long userCount = rs.getInt(1);
            if (userCount>0) {
                throw new UserManagementException("Cannot delete the role with id "+roleId+" because " +
                        "there are still "+userCount+" users who have this role as their default. " +
                        "Change their default roles before deleting this role.");
            }
            
        } catch (Exception e) {
            throw new UserManagementException("Error while deleting role.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        
        // no users exist with their default role the role-to-be-deleted. We can continue.
        
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            RoleImpl role = getRole(roleId, requestingUser);

            stmt = conn.prepareStatement("delete from user_roles where role_id=?");
            stmt.setLong(1, roleId);
            
            stmt.executeUpdate();
            stmt.close();
            //went ok? allright. Now we can delete role itself.
            
            stmt = conn.prepareStatement("delete from roles where id=?");
            stmt.setLong(1, roleId);
                
            int modifiedRecords = stmt.executeUpdate();
            if (modifiedRecords!=1) {
                throw new UserManagementException("Something went wrong " +
                        "when trying to delete role with id "+roleId +". " +
                        "Most likely the role didn't exist." +
                        modifiedRecords +" records modified.");
            }
            stmt.close();

            eventHelper.createEvent(createRoleDeletedEvent(role), "RoleDeleted", conn);

            conn.commit();
            
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new UserManagementException("Error deleting the role with ID " + roleId + ".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }        
        
        // fire synchronous events
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_DELETED, new Long(roleId), -1);
    }

    private RoleDeletedDocument createRoleDeletedEvent(RoleImpl role) {
        RoleDeletedDocument roleDeletedDocument = RoleDeletedDocument.Factory.newInstance();
        RoleDeletedDocument.RoleDeleted roleDeleted = roleDeletedDocument.addNewRoleDeleted();
        roleDeleted.addNewDeletedRole().setRole(role.getXml().getRole());
        roleDeleted.setDeletedTime(new GregorianCalendar());
        roleDeleted.setDeleterId(role.getIntimateAccess(this).getCurrentUser().getId());
        return roleDeletedDocument;
    }

    public UserImpl getUser(String userLogin, AuthenticatedUser requestingUser) throws UserManagementException {
        Connection conn = null;
        PreparedStatement stmt = null;
        UserImpl user = null;

        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_USER_WHERE_LOGIN);
            stmt.setString(1, userLogin);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                user = getUserFromResultSet(rs, requestingUser);

                if (user.getId() != requestingUser.getId() && !requestingUser.isInAdministratorRole())
                    throw new UserManagementException("Non-Administrator users can only access their own user record.");

            } else {
                throw new UserNotFoundException(userLogin);
            }
            
            rs.close();
        } catch (Exception e) {
            if (e instanceof UserManagementException)
                throw (UserManagementException)e;
            else
                throw new UserManagementException("Error while loading user with login "+userLogin+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        return user;
    }

    /**
     * returns array of Role objects to which the user with userid id belongs
     */
    private Role[] getUserRoles(long id, AuthenticatedUser requestingUser) throws UserManagementException {
        if (logger.isDebugEnabled())
            logger.debug("start loading roles for user with id "+id);
        Connection conn = null;
        PreparedStatement stmt = null;
        List<Long> ids = new ArrayList<Long>();
        Role[] roles;
        
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_ROLES_WHERE_USERID);
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                long roleId = rs.getLong(1);
                if (logger.isDebugEnabled())
                    logger.debug("role found with id "+roleId);
                ids.add(new Long(roleId));
            } 
            rs.close();
        } catch (Exception e) {
            throw new UserManagementException("Error while loading role list for user with id "+id+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }     
        
        Iterator iter = ids.iterator();
        roles = new Role[ids.size()];
        for (int i=0; i<roles.length; i++) {
            long roleId = ((Long)iter.next()).longValue();
            roles[i] = getRoleWhereId(roleId, requestingUser);
        }
        
        return roles;
        
    }

    public RoleImpl getRole(String roleName, AuthenticatedUser requestingUser) throws UserManagementException{
        Connection conn = null;
        PreparedStatement stmt = null;
        RoleImpl role = null;
        
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_ROLE_WHERE_NAME);
            stmt.setString(1, roleName);
            ResultSet rs = stmt.executeQuery();
                        
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String description = rs.getString(3);
                Date lastModified = rs.getTimestamp(4);
                long lastModifier = rs.getLong(5);
                long updateCount = rs.getLong(6);

                role = new RoleImpl(this, name, requestingUser);
                RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
                roleInt.saved(id, name, description, updateCount);
                roleInt.setLastModified(lastModified);
                roleInt.setLastModifier(lastModifier);
                
            } else {
                throw new RoleNotFoundException(roleName);
            }
            rs.close();
        } catch (Exception e) {
            if (e instanceof UserManagementException)
                throw (UserManagementException)e;
            else
                throw new UserManagementException("Error while loading role with name "+roleName+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }        
        
        return role;
    }

    private Role getRoleWhereId(long roleId, AuthenticatedUser requestingUser) throws UserManagementException{
        Connection conn = null;
        PreparedStatement stmt = null;
        RoleImpl role = null;
        
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_ROLE_WHERE_ID);
            stmt.setLong(1, roleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String description = rs.getString(3);
                Date lastModified = rs.getTimestamp(4);
                long lastModifier = rs.getLong(5);
                long updateCount = rs.getLong(6);

                role = new RoleImpl(this, name, requestingUser);
                RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
                roleInt.saved(id, name, description, updateCount);
                roleInt.setLastModified(lastModified);
                roleInt.setLastModifier(lastModifier);
                
            } else {
                throw new UserManagementException("No role found with id "+roleId);
            } 
            rs.close();
        } catch (Exception e) {
            throw new UserManagementException("Error while loading role with id "+roleId+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }        
        
        return role;
    }
    
    public void store(UserImpl user) throws UserManagementException {
        logger.debug("starting storage of user");

        if (user.getId() == 1) {
            throw new UserManagementException("User $system is read-only.");
        }

        UserImpl.IntimateAccess userInt = user.getIntimateAccess(this);

        // New users can only be created by administrators, for restrictions on updating of users, see further on
        if (user.getId() == -1 && !userInt.getCurrentUser().isInAdministratorRole())
            throw new UserManagementException("Only Administrators can update user records.");


        if (user.getId() == -1 && user.getAuthenticationScheme().equals("daisy") && userInt.getPassword() == null) {
            throw new UserManagementException("Password is required if the authentication scheme is daisy.");
        }

        if (userInt.getPassword() != null && !UserImpl.isValidPassword(userInt.getPassword())) {
            throw new UserManagementException("Password does not adhere to password rules.");
        }

        if (user.getDefaultRole() != null) {
            if (!user.hasRole(user.getDefaultRole().getId())) {
                throw new UserManagementException("The user has a default role that does not belong to its assigned roles.");
            }
        }

        if (user.getAllRoles().getArray().length < 1) {
            throw new UserManagementException("A user must have at least one role.");
        }

        long id = user.getId();
        boolean isNew = (id == -1);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            //we need transaction for this one...
            jdbcHelper.startTransaction(conn);

            String login = user.getLogin();
            java.util.Date lastModified = new Date();
            logger.debug("the last_modified date that will be stored is "+lastModified);

            /* The (authenticated)user that requested the administrative user object is now persisting it,
             * therefore he will be the last modifier
             */
            long lastModifier = userInt.getCurrentUser().getId();
            XmlObject eventDescription;

            if (id == -1) {
                // first test if the login already exists so that we can throw a nice exception in that case
                stmt = conn.prepareStatement("select id from users where login = ?");
                stmt.setString(1, login);
                ResultSet rs = stmt.executeQuery();
                if (rs.next())
                    throw new DuplicateLoginException(login);
                stmt.close();

                //a new user must be stored in the data store
                stmt = conn.prepareStatement("insert into users(id, login, password, default_role, first_name, last_name, email, updateable_by_user, confirmed, confirmkey, auth_scheme, last_modified, last_modifier, updatecount) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
                id = context.getNextUserId();
                stmt.setLong(1, id);
                stmt.setString(2, login);
                stmt.setString(3, hashPassword(userInt.getPassword()));
                stmt.setLong(4, user.getDefaultRole() == null ? -1 : user.getDefaultRole().getId());
                stmt.setString(5, user.getFirstName());
                stmt.setString(6, user.getLastName());
                stmt.setString(7, user.getEmail());
                stmt.setBoolean(8, user.isUpdateableByUser());
                stmt.setBoolean(9, user.isConfirmed());
                stmt.setString(10, user.getConfirmKey());
                stmt.setString(11, user.getAuthenticationScheme());
                stmt.setTimestamp(12, new Timestamp(lastModified.getTime()));
                stmt.setLong(13, lastModifier);
                stmt.setLong(14, 1L);
                stmt.executeUpdate();

                storeRolesForUser(user, id, conn);

                eventDescription = createUserCreatedEvent(user, id, lastModified);
            } else {
                //we have to update an existing user
                UserImpl oldUser = getUser(user.getId(), userInt.getCurrentUser());

                if (!userInt.getCurrentUser().isInAdministratorRole()) {
                    if (!oldUser.isUpdateableByUser() || (user.getId() != userInt.getCurrentUser().getId())) {
                        throw new UserManagementException("Updating the user " + user.getId() + " is not allowed for user " + userInt.getCurrentUser().getId());
                    }

                    if (!oldUser.isConfirmed()) {
                        throw new UserManagementException("A non-confimred user cannot by changed by a non-Administrator.");
                    }

                    if (userInt.hasRoleChanges() || (user.getDefaultRole() == null && oldUser.getDefaultRole() != null) || (user.getDefaultRole() != null && user.getDefaultRole().getId() != oldUser.getDefaultRole().getId())
                        || !user.getLogin().equals(oldUser.getLogin())) {
                        throw new UserManagementException("Changes to the roles or login of a user can only be done by an Administrator.");
                    }
                }

                String oldPassword;
                // check if nobody else changed it in the meantime, and load previous password
                stmt = conn.prepareStatement("select updatecount, password from users where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new UserManagementException("Unexpected error: the User with id " + id + " does not exist in the database.");
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != user.getUpdateCount()) {
                        throw new ConcurrentUpdateException(User.class.getName(), String.valueOf(user.getId()));
                    }
                    oldPassword = rs.getString(2);
                }
                stmt.close();

                if (!oldUser.getLogin().equals(user.getLogin())) {
                    // login changed, test if there is already a user with the new login so that we can
                    // throw a nice exception in that case
                    stmt = conn.prepareStatement("select id from users where login = ?");
                    stmt.setString(1, login);
                    ResultSet rs2 = stmt.executeQuery();
                    if (rs2.next() && rs2.getLong(1) != user.getId())
                        throw new DuplicateLoginException(login);
                    stmt.close();
                }

                //first see if there were role changes
                if (userInt.hasRoleChanges()) {
                    stmt = conn.prepareStatement("delete from user_roles where user_id=?");
                    stmt.setLong(1, user.getId());
                    
                    stmt.executeUpdate();
                    stmt.close();
                }

                long newUpdateCount = user.getUpdateCount() + 1;

                String password = null;
                if (user.getAuthenticationScheme().equals("daisy")) {
                    password = userInt.getPassword() == null ? oldPassword: hashPassword(userInt.getPassword());
                    if (password == null)
                        throw new UserManagementException("If the authentication scheme is daisy, then the password is required.");
                }

                stmt = conn.prepareStatement("update users set login=?, password=?, default_role=?, first_name=?, last_name=?, email=?, updateable_by_user=?, confirmed=?, confirmkey=?, auth_scheme=?, last_modified=?, last_modifier=?, updatecount=? where id=?");
                stmt.setString(1, login);
                stmt.setString(2, password);
                stmt.setLong(3, user.getDefaultRole() == null ? -1 : user.getDefaultRole().getId());
                stmt.setString(4, user.getFirstName());
                stmt.setString(5, user.getLastName());
                stmt.setString(6, user.getEmail());
                stmt.setBoolean(7, user.isUpdateableByUser());
                stmt.setBoolean(8, user.isConfirmed());
                stmt.setString(9, user.getConfirmKey());
                stmt.setString(10, user.getAuthenticationScheme());
                stmt.setTimestamp(11, new Timestamp(lastModified.getTime()));
                stmt.setLong(12, lastModifier);
                stmt.setLong(13, newUpdateCount);
                stmt.setLong(14, user.getId());
                
                stmt.executeUpdate();
                storeRolesForUser(user, user.getId(), conn);

                eventDescription = createUserUpdatedEvent(oldUser, user, lastModified, newUpdateCount);
            }

            eventHelper.createEvent(eventDescription, isNew ? "UserCreated" : "UserUpdated", conn);

            //everything went ok, so we can actively update the user OBJECT as well.
            conn.commit();
            userInt.saved(id, user.getFirstName(), user.getLastName(), user.getEmail(), lastModified, lastModifier, user.getUpdateCount() + 1);
            
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof DuplicateLoginException)
                throw (DuplicateLoginException)e;
            else
                throw new UserManagementException("Error storing user.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // fire synchronous events
        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_CREATED, new Long(id), user.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_UPDATED, new Long(id), user.getUpdateCount());
    }

    public static String hashPassword(String password) {
        if (password == null)
            return null;
        try {
            byte[] data = password.getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(data);
            byte[] result = digest.digest();
            return toHexString(result);
        } catch (Exception e) {
            throw new RuntimeException("Problem calculating password hash.", e);
        }
    }

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private UserCreatedDocument createUserCreatedEvent(UserImpl user, long id, Date lastModified) {
        UserDocument.User userXml = user.getXml().getUser();
        userXml.setId(id);
        userXml.setLastModified(getCalendar(lastModified));
        userXml.setLastModifier(user.getIntimateAccess(this).getCurrentUser().getId());
        userXml.setUpdateCount(1);
        if (userXml.isSetPassword())
            userXml.unsetPassword();

        UserCreatedDocument userCreatedDocument = UserCreatedDocument.Factory.newInstance();
        userCreatedDocument.addNewUserCreated().addNewNewUser().setUser(userXml);

        return userCreatedDocument;
    }

    private UserUpdatedDocument createUserUpdatedEvent(UserImpl oldUser, UserImpl newUser, Date lastModified, long newUpdateCount) {
        UserUpdatedDocument userUpdatedDocument = UserUpdatedDocument.Factory.newInstance();
        UserUpdatedDocument.UserUpdated userUpdated = userUpdatedDocument.addNewUserUpdated();

        UserDocument.User oldUserXml = oldUser.getXml().getUser();
        if (oldUserXml.isSetPassword())
            oldUserXml.unsetPassword();
        userUpdated.addNewOldUser().setUser(oldUserXml);

        UserDocument.User newUserXml = newUser.getXml().getUser();
        newUserXml.setLastModified(getCalendar(lastModified));
        newUserXml.setLastModifier(newUser.getIntimateAccess(this).getCurrentUser().getId());
        newUserXml.setUpdateCount(newUpdateCount);
        if (newUserXml.isSetPassword())
            newUserXml.unsetPassword();
        userUpdated.addNewNewUser().setUser(newUserXml);

        return userUpdatedDocument;
    }

    private Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    /**
     * @param user the user to store the roles for
     * @param conn the connection to the data store containing users and roles
     */
    private void storeRolesForUser(UserImpl user, long userId, Connection conn) throws SQLException {
        logger.debug("ARRAY SIZE: "+user.getAllRoles().getArray().length);
        UserImpl.IntimateAccess userInt = user.getIntimateAccess(this);
        
        //can't do this because we haven't called the saved() method yet on the user!!!
        //long userId = user.getId();
        
        if (userInt.hasRoleChanges()) {
            logger.debug("there are role changes");
            PreparedStatement delRoles = null;
            PreparedStatement insertRoles = null;
            try {
                //first we need to delete the roles the user currently belongs to
                delRoles = conn.prepareStatement(DELETE_ROLES);
                delRoles.setLong(1, userId);
                delRoles.executeUpdate();

                //roles deleted, now we can store the current state
                insertRoles = conn.prepareStatement(INSERT_ROLES);
                Role[] userRoles = user.getAllRoles().getArray();
                for (Role role : userRoles) {
                    insertRoles.setLong(1, role.getId());
                    insertRoles.setLong(2, userId);
                    insertRoles.executeUpdate();
                }
            } finally {
                jdbcHelper.closeStatement(delRoles);
                jdbcHelper.closeStatement(insertRoles);
            }
        }
    }

    public void store(RoleImpl role) throws UserManagementException {
        
        RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);

        if (!roleInt.getCurrentUser().isInAdministratorRole())
            throw new UserManagementException("Only Administrators can create or update roles.");

        long id = role.getId();
        boolean isNew = id == -1;

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            String roleName = role.getName();
            java.util.Date lastModified = new Date();

            /* The user that requested the role is now persisting it,
             * therefore he will be the last modifier
             */
            long lastModifier = roleInt.getCurrentUser().getId();
            XmlObject eventDescription;

            if (id == -1) {
                //a new role must be stored in the data store
                stmt = conn.prepareStatement("insert into roles(id, name, description, last_modified, last_modifier, updatecount) values (?,?,?,?,?,?)");
                id = context.getNextRoleId();
                stmt.setLong(1, id);
                stmt.setString(2, roleName);
                stmt.setString(3, role.getDescription());
                stmt.setTimestamp(4, new Timestamp(lastModified.getTime()));
                stmt.setLong(5, lastModifier);
                stmt.setLong(6, 1);
                stmt.executeUpdate();

                eventDescription = createRoleCreatedEvent(role, id, lastModified);
            } else {
                //we have to update an existing role

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from roles where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new UserManagementException("Unexpected error: the Role with id " + id + " does not exist in the database.");
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != role.getUpdateCount()) {
                        throw new ConcurrentUpdateException(Role.class.getName(), String.valueOf(role.getId()));
                    }
                }
                stmt.close();

                RoleImpl oldRole = getRole(role.getId(), roleInt.getCurrentUser());
                long newUpdateCount = role.getUpdateCount() + 1;

                stmt = conn.prepareStatement("update roles set name=?, description=?, last_modified=?, last_modifier=?, updatecount=? where id=?");
                stmt.setString(1, roleName);
                stmt.setString(2, role.getDescription());
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, newUpdateCount);
                stmt.setLong(6, id);
                stmt.executeUpdate();

                eventDescription = createRoleUpdatedEvent(oldRole, role, lastModified, newUpdateCount);
            }

            eventHelper.createEvent(eventDescription, isNew ? "RoleCreated" : "RoleUpdated", conn);

            conn.commit();

            //everything went ok, so we can actively update the role OBJECT as well.
            roleInt.saved(id, roleName, role.getDescription(), role.getUpdateCount() + 1);
            roleInt.setLastModified(lastModified);
            roleInt.setLastModifier(lastModifier);
            
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new UserManagementException("Error storing role.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        // fire synchronous events
        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_CREATED, new Long(id), role.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_UPDATED, new Long(id), role.getUpdateCount());
   }

    private RoleCreatedDocument createRoleCreatedEvent(RoleImpl role, long id, Date lastModified) {
        RoleCreatedDocument roleCreatedDocument = RoleCreatedDocument.Factory.newInstance();

        RoleDocument.Role roleXml = role.getXml().getRole();
        roleXml.setId(id);
        roleXml.setLastModified(getCalendar(lastModified));
        roleXml.setLastModifier(role.getIntimateAccess(this).getCurrentUser().getId());
        roleXml.setUpdateCount(1);

        roleCreatedDocument.addNewRoleCreated().addNewNewRole().setRole(roleXml);

        return roleCreatedDocument;
    }

    private RoleUpdatedDocument createRoleUpdatedEvent(RoleImpl oldRole, RoleImpl newRole, Date lastModified, long newUpdateCount) {
        RoleUpdatedDocument roleUpdatedDocument = RoleUpdatedDocument.Factory.newInstance();
        RoleUpdatedDocument.RoleUpdated roleUpdated = roleUpdatedDocument.addNewRoleUpdated();
        roleUpdated.addNewOldRole().setRole(oldRole.getXml().getRole());

        RoleDocument.Role newRoleXml = newRole.getXml().getRole();
        newRoleXml.setLastModified(getCalendar(lastModified));
        newRoleXml.setLastModifier(newRole.getIntimateAccess(this).getCurrentUser().getId());
        newRoleXml.setUpdateCount(newUpdateCount);

        roleUpdated.addNewNewRole().setRole(newRoleXml);

        return roleUpdatedDocument;
    }

    public UserImpl getUser(long userId, AuthenticatedUser requestingUser) throws UserManagementException {
        if (userId != requestingUser.getId() && !requestingUser.isInAdministratorRole())
            throw new UserManagementException("Non-Administrator users can only access their own user record.");

        Connection conn = null;
        PreparedStatement stmt = null;
        UserImpl user = null;

        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_USER_WHERE_ID);
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                user = getUserFromResultSet(rs, requestingUser);
            } else {
                throw new UserNotFoundException(userId);
            }
            
            rs.close();
        } catch (Exception e) {
            if (e instanceof UserManagementException)
                throw (UserManagementException)e;
            else
                throw new UserManagementException("Error while loading user with id "+userId+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
        return user;
    }

    public RoleImpl getRole(long roleId, AuthenticatedUser requestingUser) throws UserManagementException {
        Connection conn = null;
        PreparedStatement stmt = null;
        RoleImpl role = null;
        
        try {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(SELECT_ROLE_WHERE_ID);
            stmt.setLong(1, roleId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                long id = rs.getLong(1);
                String name = rs.getString(2);
                String description = rs.getString(3);
                Date lastModified = rs.getTimestamp(4);
                long lastModifier = rs.getLong(5);
                long updateCount = rs.getLong(6);

                role = new RoleImpl(this, name, requestingUser);
                RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
                roleInt.saved(id, name, description, updateCount);
                roleInt.setLastModified(lastModified);
                roleInt.setLastModifier(lastModifier);
                
            } else {
                throw new RoleNotFoundException(roleId);
            }
            rs.close();
        } catch (Exception e) {
            if (e instanceof UserManagementException)
                throw (UserManagementException)e;
            else
                throw new UserManagementException("Error while loading role with id "+roleId+".", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }        
        
        return role;
    }

    public UsersImpl getUsersByEmail(String email, AuthenticatedUser requestingUser) throws RepositoryException {
        if (!requestingUser.isInAdministratorRole())
            throw new UserManagementException("Non-Administrator users cannot retrieve other users then themselve.");

        Connection conn = null;
        PreparedStatement stmt = null;
        List<User> users = new ArrayList<User>();

        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement(SELECT_USERS_WHERE_EMAIL);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                UserImpl user = getUserFromResultSet(rs, requestingUser);
                users.add(user);
            }
        } catch (Exception e) {
            throw new UserManagementException("Error while loading list of users by email.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        return new UsersImpl(users.toArray(new User[users.size()]));
    }

    public PublicUserInfo getPublicUserInfo(long userId, AuthenticatedUser user) throws RepositoryException {
        return getUser(userId, systemUser).getPublicUserInfo();
    }

    public PublicUserInfo getPublicUserInfo(String login, AuthenticatedUser user) throws RepositoryException {
        return getUser(login, systemUser).getPublicUserInfo();
    }

    public PublicUserInfos getPublicUserInfos(AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        List<PublicUserInfo> users = new ArrayList<PublicUserInfo>();

        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement("select id, login, first_name, last_name from users");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String login = rs.getString(2);
                String firstName = rs.getString(3);
                String lastName = rs.getString(4);
                String displayName = UserImpl.getDisplayName(firstName, lastName, login);
                users.add(new PublicUserInfoImpl(rs.getLong(1), login, displayName));
            }
        } catch (Exception e) {
            throw new UserManagementException("Error while loading public info of all users.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }

        return new PublicUserInfosImpl(users.toArray(new PublicUserInfo[users.size()]));
    }

    public AuthenticationSchemeInfos getAuthenticationSchemes(AuthenticatedUser requestingUser) {
        return context.getUserAuthenticator().getAuthenticationSchemes();
    }
}
