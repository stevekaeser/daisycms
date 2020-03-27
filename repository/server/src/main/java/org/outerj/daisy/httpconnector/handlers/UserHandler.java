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
package org.outerj.daisy.httpconnector.handlers;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.UserDocument;
import org.outerx.daisy.x10.RoleDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;
import java.io.IOException;

public class UserHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/user/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long userId = Long.parseLong((String)matchMap.get("1"));
        UserManager userMan = repository.getUserManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            User user = userMan.getUser(userId, true);
            user.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            UserDocument userDocument = UserDocument.Factory.parse(request.getInputStream(), xmlOptions);
            UserDocument.User userXml = userDocument.getUser();
            User user = userMan.getUser(userId, true);
            updateUserFromXml(response, userMan, userXml, user);
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            userMan.deleteUser(userId);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    private void updateUserFromXml(HttpServletResponse response, UserManager userMan, UserDocument.User userXml, User user) throws RepositoryException, IOException {
        // check for concurrent modifications
        if (user.getUpdateCount() != userXml.getUpdateCount())
            throw new ConcurrentUpdateException(User.class.getName(), String.valueOf(user.getId()));

        // update object
        user.setEmail(userXml.getEmail());
        user.setFirstName(userXml.getFirstName());
        user.setLastName(userXml.getLastName());
        if (userXml.getPassword() != null)
            user.setPassword(userXml.getPassword());
        user.setUpdateableByUser(userXml.getUpdateableByUser());
        user.setConfirmed(userXml.getConfirmed());
        user.setConfirmKey(userXml.getConfirmKey());
        user.setLogin(userXml.getLogin());
        user.setAuthenticationScheme(userXml.getAuthenticationScheme());

        // update roles: first check there are any changes to the roles
        List<RoleDocument.Role> rolesXml = userXml.getRoles().getRoleList();
        boolean roleChanges = false;
        Role[] currentRoles = user.getAllRoles().getArray();
        if (rolesXml.size() != currentRoles.length) {
            roleChanges = true;
        } else {
            rolesXmlLoop: for (RoleDocument.Role roleXml : rolesXml) {
                for (Role currentRole : currentRoles) {
                    if (currentRole.getId() == roleXml.getId())
                        continue rolesXmlLoop;
                }
                // role was not found -- thus there were role changes
                roleChanges = true;
                break;
            }
        }

        // update roles if necessary
        if (roleChanges) {
            user.clearRoles();
            for (RoleDocument.Role role : rolesXml) {
                Role r = userMan.getRole(role.getId(), false);
                user.addToRole(r);
            }
        }

        RoleDocument.Role defaultRoleXml = userXml.getRole();
        if (defaultRoleXml != null) {
            Role defaultRole = userMan.getRole(defaultRoleXml.getId(), false);
            user.setDefaultRole(defaultRole);
        } else {
            user.setDefaultRole(null);
        }
        user.save();

        user.getXml().save(response.getOutputStream());
    }
}
