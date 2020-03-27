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
import org.outerj.daisy.httpconnector.spi.UploadItem;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.workflow.WfListHelper;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.util.HttpConstants;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.io.InputStream;

public class ProcessDefinitionsHandler extends AbstractWorkflowRequestHandler {

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        XmlOptions xmlOptions = new XmlOptions().setSaveSuggestedPrefixes(Constants.SUGGESTED_NAMESPACE_PREFIXES);
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = WfHttpUtil.getLocale(request);

        if (request.getMethod().equals(HttpConstants.GET)) {
            boolean latestOnly = "true".equals(request.getParameter("latestOnly"));
            List<WfProcessDefinition> processDefinitions;
            if (latestOnly) {
                processDefinitions = workflowManager.getAllLatestProcessDefinitions(locale);
            } else {
                processDefinitions = workflowManager.getAllProcessDefinitions(locale);
            }
            WfListHelper.getProcessDefinitionsAsXml(processDefinitions).save(response.getOutputStream(), xmlOptions);
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            String action = request.getParameter("action");
            if ("loadSamples".equals(action)) {
                workflowManager.loadSampleWorkflows();
            } else {
                List<UploadItem> uploadedItems = support.parseMultipartRequest(request, response);
                UploadItem processDefItem = getItemByName(uploadedItems, "processdefinition");

                if (processDefItem == null) {
                    HttpUtil.sendCustomError("The required field named \"processdefinition\" is missing.", HttpConstants._400_Bad_Request, response);
                    return;
                }

                InputStream is = null;
                WfProcessDefinition processDef;
                try {
                    is = processDefItem.getInputStream();
                    String mimeType = processDefItem.getContentType();
                    // strip character set from content type
                    int semicolonPos = mimeType.indexOf(';');
                    if (semicolonPos != -1)
                        mimeType = mimeType.substring(0, semicolonPos);

                    processDef = workflowManager.deployProcessDefinition(is, mimeType, locale);
                } finally {
                    if (is != null)
                        is.close();
                }
                processDef.getXml().save(response.getOutputStream(), xmlOptions);
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    protected UploadItem getItemByName(List<UploadItem> items, String name) {
        for (UploadItem item : items) {
            if (item.getFieldName().equals(name))
                return item;
        }
        return null;
    }

    public String getPathPattern() {
        return "/processDefinition";
    }
}
