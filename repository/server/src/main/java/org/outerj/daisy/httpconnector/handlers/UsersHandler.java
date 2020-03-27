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
import org.outerj.daisy.repository.commonimpl.user.RolesUtil;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Users;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.UserDocument;
import org.outerx.daisy.x10.RoleDocument;
import org.outerx.daisy.x10.RolesDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;

public class UsersHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/user";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        UserManager userMan = repository.getUserManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            Users users = userMan.getUsers();
            users.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            // on the other hand, POST creates a new User
            // and returns the XML representation of the newly created User
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            UserDocument userDocument = UserDocument.Factory.parse(request.getInputStream(), xmlOptions);
            UserDocument.User userXml = userDocument.getUser();

            User user = userMan.createUser(userXml.getLogin());

            user.setEmail(userXml.getEmail());
            user.setFirstName(userXml.getFirstName());
            user.setLastName(userXml.getLastName());
            user.setPassword(userXml.getPassword());
            user.setUpdateableByUser(userXml.getUpdateableByUser());

            RolesDocument.Roles roles = userXml.getRoles();
            List<RoleDocument.Role> rolesArr = roles.getRoleList();

            for (RoleDocument.Role role : rolesArr) {
                Role r = userMan.getRole(role.getId(), false);
                user.addToRole(r);
            }

            RoleDocument.Role defaultRoleXml = userXml.getRole();
            if (defaultRoleXml != null) {
                Role defaultRole = RolesUtil.getRole(user.getAllRoles(), defaultRoleXml.getName());
                user.setDefaultRole(defaultRole);
            } else {
                user.setDefaultRole(null);
            }
            user.setConfirmed(userXml.getConfirmed());
            user.setConfirmKey(userXml.getConfirmKey());
            user.setAuthenticationScheme(userXml.getAuthenticationScheme());

            user.save();

            user.getXml().save(response.getOutputStream());
        }  else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
