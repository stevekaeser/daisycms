/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow.serverimpl.httpconnector.handlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfListHelper;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.apache.xmlbeans.XmlOptions;
import org.outerx.daisy.x10Workflow.UsersDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;

public class PoolMembershipHandler extends AbstractWorkflowRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long id = HttpUtil.parseId("pool", (String)matchMap.get("1"));

        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        WfPoolManager poolManager = workflowManager.getPoolManager();

        if (request.getMethod().equals(HttpConstants.GET)) {
            List<Long> userIds = poolManager.getUsersForPool(id);
            WfListHelper.getUserIdsAsXml(userIds).save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            String action = HttpUtil.getStringParam(request, "action");
            if (action.equals("add") || action.equals("remove")) {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                UsersDocument usersDocument = UsersDocument.Factory.parse(request.getInputStream(), xmlOptions);

                if (action.equals("add")) {
                    poolManager.addUsersToPool(id, usersDocument.getUsers().getIdList());
                } else {
                    poolManager.removeUsersFromPool(id, usersDocument.getUsers().getIdList());
                }
            } else if (action.equals("clear")) {
                poolManager.clearPool(id);
            } else {
                HttpUtil.sendCustomError("Invalid value for action parameter: " + action, HttpConstants._400_Bad_Request, response);
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/pool/*/membership";
    }
}
