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
import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.WfVersionKey;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.util.HttpConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;

public class InitialVariablesHandler extends AbstractWorkflowRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String id = (String)matchMap.get("1");
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

        if (request.getMethod().equals(HttpConstants.GET)) {
            String docId = request.getParameter("contextDocId");
            long branchId = HttpUtil.getBranchId(request, repository, "contextDocBranch");
            long languageId = HttpUtil.getLanguageId(request, repository, "contextDocLanguage");
            String version = request.getParameter("contextDocVersion");

            WfVersionKey contextDoc = null;
            if (docId != null) {
                contextDoc = new WfVersionKey(docId, branchId, languageId, version);
            }

            List<WfVariable> variables = workflowManager.getInitialVariables(id, contextDoc);
            WfXmlHelper.getVariablesAsXml(variables).save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/processDefinition/*/initialVariables";
    }
}