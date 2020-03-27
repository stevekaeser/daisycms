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
package org.outerj.daisy.repository.clientimpl.user;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryEventType;
import org.outerj.daisy.repository.commonimpl.user.*;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager.Context;
import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.util.ListUtil;
import org.outerx.daisy.x10.*;

/**
 * User management strategy that connects to the repository server, communicating through
 * HTTP/XML.
 */
public class RemoteUserManagementStrategy
    extends AbstractRemoteStrategy
    implements UserManagementStrategy {

    public RemoteUserManagementStrategy(Context context) {
        super(context);
    }

    public Users loadUsers(AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/user");

        UsersDocument usersDocument = (UsersDocument)httpClient.executeMethod(method, UsersDocument.class, true);
        return instantiateUsersFromXml(usersDocument.getUsers(), requestingUser);
    }

    public long[] getUserIds(AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/userIds");

        IdsDocument idsDocument = (IdsDocument)httpClient.executeMethod(method, IdsDocument.class, true);
        return ListUtil.toArray(idsDocument.getIds().getIdList());
    }

    private UserImpl instantiateUserFromXml(UserDocument.User userXml, AuthenticatedUser requestingUser) {
        String login = userXml.getLogin();
        
        UserImpl user = new UserImpl(this, login, requestingUser);
        UserImpl.IntimateAccess userInt = user.getIntimateAccess(this);
        
        RolesDocument.Roles roles = userXml.getRoles();
        List<RoleDocument.Role> roleArr = roles.getRoleList();
        for (RoleDocument.Role role : roleArr) {
            userInt.addToRole(instantiateRoleFromXml(role, requestingUser));
        }
        
        userInt.saved(userXml.getId(), userXml.getFirstName(), userXml.getLastName(), userXml.getEmail(),
                userXml.getLastModified().getTime(), userXml.getLastModifier(), userXml.getUpdateCount());
        if (userXml.getRole() != null)
            user.setDefaultRole(instantiateRoleFromXml(userXml.getRole(), requestingUser));
        else
            user.setDefaultRole(null);
        user.setUpdateableByUser(userXml.getUpdateableByUser());
        user.setConfirmed(userXml.getConfirmed());
        if (userXml.isSetConfirmKey())
            user.setConfirmKey(userXml.getConfirmKey());
        user.setAuthenticationScheme(userXml.getAuthenticationScheme());

        return user;
    }

    public Roles loadRoles(AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/role");

        RolesDocument rolesDocument = (RolesDocument)httpClient.executeMethod(method, RolesDocument.class, true);
        RolesDocument.Roles roles = rolesDocument.getRoles();
        List<RoleDocument.Role> rolesXml = roles.getRoleList();
        List<RoleImpl> rolesList = new ArrayList<RoleImpl>(rolesXml.size());
        for (RoleDocument.Role roleXml : rolesXml) {
            RoleImpl role = instantiateRoleFromXml(roleXml, requestingUser);
            rolesList.add(role);
        }
        Role[] roleArr = rolesList.toArray(new Role[rolesList.size()]);
        return new RolesImpl(roleArr);
    }

    private RoleImpl instantiateRoleFromXml(RoleDocument.Role roleXml, AuthenticatedUser requestingUser) {
        if (roleXml == null)
            throw new RuntimeException("roleXml was null!");
        
        String roleName = roleXml.getName();
        
        RoleImpl role = new RoleImpl(this, roleName, requestingUser);
        RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
        roleInt.setLastModified(roleXml.getLastModified().getTime());
        roleInt.setLastModifier(roleXml.getLastModifier());
        roleInt.saved(roleXml.getId(), roleName, roleXml.getDescription(), roleXml.getUpdateCount());
        
        return role;
    }

    public void deleteUser(long userId, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        DeleteMethod method = new DeleteMethod("/repository/user/" + userId);

        httpClient.executeMethod(method, null, true);
        // fire synchronous events
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_DELETED, new Long(userId), -1);
    }

    public void deleteRole(long roleId, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        DeleteMethod method = new DeleteMethod("/repository/role/" + roleId);

        httpClient.executeMethod(method, null, true);
        // fire synchronous events
        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_DELETED, new Long(roleId), -1);
    }

    public UserImpl getUser(String login, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        String encodedLogin = encodeNameForUseInPath("login", login);
        HttpMethod method = new GetMethod("/repository/userByLogin/" + encodedLogin);
        return getUserXmlFromServer(requestingUser, httpClient, method);
    }

    public RoleImpl getRole(String name, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        String encodedName = encodeNameForUseInPath("role", name);
        HttpMethod method = new GetMethod("/repository/roleByName/" + encodedName);
        return getRoleXmlFromServer(requestingUser, httpClient, method);
    }

    public void store(UserImpl user) throws RepositoryException {
        if (user == null)
            throw new RuntimeException("UserImpl expected - instead received null!");

        UserImpl.IntimateAccess collInt = user.getIntimateAccess(this);

        DaisyHttpClient httpClient = getClient(collInt.getCurrentUser());

        String url = "/repository";
        boolean isNew = user.getId() == -1;
        if (isNew)
            url += "/user";
        else
            url += "/user/" + user.getId();

        PostMethod method = new PostMethod(url);

        UserDocument userDocument = user.getXml();

        method.setRequestEntity(new InputStreamRequestEntity(userDocument.newInputStream()));

        UserDocument responseUserDocument = (UserDocument)httpClient.executeMethod(method, UserDocument.class, true);
        UserDocument.User userXml = responseUserDocument.getUser();
        UserImpl.IntimateAccess userInt = user.getIntimateAccess(this);
        userInt.saved(userXml.getId(), userXml.getFirstName(), userXml.getLastName(), userXml.getEmail(),
            userXml.getLastModified().getTime(), userXml.getLastModifier(), userXml.getUpdateCount());

        // fire synchronous events
        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_CREATED, new Long(user.getId()), user.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.USER_UPDATED, new Long(user.getId()), user.getUpdateCount());
    }

    public void store(RoleImpl role) throws RepositoryException {
        if (role == null)
            throw new RuntimeException("RoleImpl expected - instead received null!");

        RoleImpl.IntimateAccess collInt = role.getIntimateAccess(this);

        DaisyHttpClient httpClient = getClient(collInt.getCurrentUser());

        String url = "/repository";
        boolean isNew = role.getId() == -1;
        if (isNew)
            url += "/role";
        else
            url += "/role/" + role.getId();

        PostMethod method = new PostMethod(url);

        RoleDocument roleDocument = role.getXml();
        method.setRequestEntity(new InputStreamRequestEntity(roleDocument.newInputStream()));

        RoleDocument responseRoleDocument = (RoleDocument)httpClient.executeMethod(method, RoleDocument.class, true);
        RoleDocument.Role roleXml = responseRoleDocument.getRole();
        RoleImpl.IntimateAccess roleInt = role.getIntimateAccess(this);
        roleInt.saved(
            roleXml.getId(),
            roleXml.getName(),
            roleXml.getDescription(),
            roleXml.getUpdateCount());
        roleInt.setLastModified(roleXml.getLastModified().getTime());
        roleInt.setLastModifier(roleXml.getLastModifier());

        // fire synchronous events
        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_CREATED, new Long(role.getId()), role.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ROLE_UPDATED, new Long(role.getId()), role.getUpdateCount());
    }

 
    public UserImpl getUser(long userId, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/user/" + userId);
        return getUserXmlFromServer(requestingUser, httpClient, method);
    }



    public RoleImpl getRole(long roleId, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/role/" + roleId);
        return getRoleXmlFromServer(requestingUser, httpClient, method);
    }

    private UserImpl getUserXmlFromServer(AuthenticatedUser requestingUser, DaisyHttpClient httpClient, HttpMethod method) throws RepositoryException {
        UserDocument userDocument = (UserDocument)httpClient.executeMethod(method, UserDocument.class, true);
        UserDocument.User userXml = userDocument.getUser();
        UserImpl document = instantiateUserFromXml(userXml, requestingUser);
        return document;
    }
    
    private RoleImpl getRoleXmlFromServer(AuthenticatedUser requestingUser, DaisyHttpClient httpClient, HttpMethod method) throws RepositoryException {
        RoleDocument roleDocument = (RoleDocument)httpClient.executeMethod(method, RoleDocument.class, true);
        RoleDocument.Role roleXml = roleDocument.getRole();
        RoleImpl document = instantiateRoleFromXml(roleXml, requestingUser);
        return document;
    }

    public UsersImpl getUsersByEmail(String email, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        String encodedEmail = encodeNameForUseInPath("email", email);
        HttpMethod method = new GetMethod("/repository/usersByEmail/" + encodedEmail);

        UsersDocument usersDocument = (UsersDocument)httpClient.executeMethod(method, UsersDocument.class, true);
        return instantiateUsersFromXml(usersDocument.getUsers(), requestingUser);
    }

    private UsersImpl instantiateUsersFromXml(UsersDocument.Users users, AuthenticatedUser requestingUser) {
        List<UserDocument.User> usersXml = users.getUserList();
        List<User> usersList = new ArrayList<User>(usersXml.size());
        for (UserDocument.User userXml : usersXml) {
            UserImpl user = instantiateUserFromXml(userXml, requestingUser);
            usersList.add(user);
        }
        User[] userArr = usersList.toArray(new User[usersList.size()]);
        return new UsersImpl(userArr);
    }

    public PublicUserInfo getPublicUserInfo(long userId, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/publicUserInfo/" + userId);

        PublicUserInfoDocument pubUserDoc = (PublicUserInfoDocument)httpClient.executeMethod(method, PublicUserInfoDocument.class, true);
        return instantiatePublicUserInfoFromXml(pubUserDoc.getPublicUserInfo());
    }

    public PublicUserInfo getPublicUserInfo(String login, AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        String encodedLogin = encodeNameForUseInPath("login", login);
        HttpMethod method = new GetMethod("/repository/publicUserInfoByLogin/" + encodedLogin);

        PublicUserInfoDocument pubUserDoc = (PublicUserInfoDocument)httpClient.executeMethod(method, PublicUserInfoDocument.class, true);
        return instantiatePublicUserInfoFromXml(pubUserDoc.getPublicUserInfo());
    }

    public PublicUserInfos getPublicUserInfos(AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/publicUserInfo");

        PublicUserInfosDocument pubUsersDoc = (PublicUserInfosDocument)httpClient.executeMethod(method, PublicUserInfosDocument.class, true);
        List<PublicUserInfoDocument.PublicUserInfo> pubUserInfosXml = pubUsersDoc.getPublicUserInfos().getPublicUserInfoList();
        PublicUserInfo[] pubUserInfos = new PublicUserInfo[pubUserInfosXml.size()];
        for (int i = 0; i < pubUserInfosXml.size(); i++) {
            pubUserInfos[i] = instantiatePublicUserInfoFromXml(pubUserInfosXml.get(i));
        }
        return new PublicUserInfosImpl(pubUserInfos);
    }

    private PublicUserInfo instantiatePublicUserInfoFromXml(PublicUserInfoDocument.PublicUserInfo userXml) {
        return new PublicUserInfoImpl(userXml.getId(), userXml.getLogin(), userXml.getDisplayName());
    }

    public AuthenticationSchemeInfos getAuthenticationSchemes(AuthenticatedUser requestingUser) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(requestingUser);
        HttpMethod method = new GetMethod("/repository/authenticationSchemes");

        AuthenticationSchemesDocument schemesDocument = (AuthenticationSchemesDocument)httpClient.executeMethod(method, AuthenticationSchemesDocument.class, true);

        List<AuthenticationSchemeDocument.AuthenticationScheme> schemesXml = schemesDocument.getAuthenticationSchemes().getAuthenticationSchemeList();
        AuthenticationSchemeInfo[] schemes = new AuthenticationSchemeInfo[schemesXml.size()];
        for (int i = 0; i < schemesXml.size(); i++) {
            schemes[i] = new AuthenticationSchemeInfoImpl(schemesXml.get(i).getName(), schemesXml.get(i).getDescription());
        }

        return new AuthenticationSchemeInfosImpl(schemes);
    }
}
