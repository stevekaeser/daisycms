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
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.RoleDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.io.IOException;

public class RoleHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/role/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long roleId = Long.parseLong((String)matchMap.get("1"));
        UserManager userMan = repository.getUserManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            Role role = userMan.getRole(roleId, true);
            role.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            RoleDocument roleDocument = RoleDocument.Factory.parse(request.getInputStream(), xmlOptions);
            RoleDocument.Role roleXml = roleDocument.getRole();

            Role role = userMan.getRole(roleId, true);
            updateRoleFromXml(response, roleXml, role);
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            userMan.deleteRole(roleId);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    private void updateRoleFromXml(HttpServletResponse response, RoleDocument.Role roleXml, Role role) throws RepositoryException, IOException {
        String roleDescription = roleXml.getDescription();
        String roleName = roleXml.getName();

        // check for concurrent modifications
        if (role.getUpdateCount() != roleXml.getUpdateCount())
            throw new ConcurrentUpdateException(Role.class.getName(), String.valueOf(role.getId()));

        // update object
        role.setDescription(roleDescription);
        role.setName(roleName);
        role.save();

        role.getXml().save(response.getOutputStream());
    }
}
