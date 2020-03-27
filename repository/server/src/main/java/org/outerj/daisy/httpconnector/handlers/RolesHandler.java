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
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Roles;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.RoleDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class RolesHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/role";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        UserManager userMan = repository.getUserManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            Roles roles = userMan.getRoles();
            roles.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            // on the other hand, POST creates a new Role
            // and returns the XML representation of the newly created Role
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            RoleDocument roleDoc = RoleDocument.Factory.parse(request.getInputStream(), xmlOptions);
            RoleDocument.Role roleDocXml = roleDoc.getRole();
            String roleName = roleDocXml.getName();
            Role role = userMan.createRole(roleName);
            String description = roleDocXml.getDescription();
            role.setDescription(description);
            role.save();

            role.getXml().save(response.getOutputStream());
        }  else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
