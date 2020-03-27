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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;

public class AclInfoHandler extends AbstractWorkflowRequestHandler {
    
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String taskId = HttpUtil.getStringParam(request, "taskId", null);
        String processId = HttpUtil.getStringParam(request, "processId", null);
        String processDefinitionId = HttpUtil.getStringParam(request, "processDefinitionId", null);
        boolean includeGlobalInfo = HttpUtil.getBooleanParam(request, "includeGlobalInfo", false);

        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        WorkflowAclInfoDocument doc = workflowManager.getAclInfo(taskId, processId, processDefinitionId, includeGlobalInfo);
        doc.save(response.getOutputStream());
    }

    public String getPathPattern() {
        return "/aclInfo";
    }
}