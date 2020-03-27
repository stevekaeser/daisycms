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

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.commonimpl.WfXmlHelper;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10Workflow.StartProcessDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Locale;

public class ProcessInstancesHandler extends AbstractWorkflowRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

        if (request.getMethod().equals(HttpConstants.POST)) {
            Locale locale = WfHttpUtil.getLocale(request);
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            StartProcessDocument document = StartProcessDocument.Factory.parse(request.getInputStream(), xmlOptions);
            StartProcessDocument.StartProcess xml = document.getStartProcess();

            TaskUpdateData taskUpdateData = null;
            if (xml.isSetTaskUpdateData()) {
                taskUpdateData = WfXmlHelper.instantiateTaskUpdateData(xml.getTaskUpdateData());
            }

            WfProcessInstance process = workflowManager.startProcess(xml.getProcessDefinitionId(), taskUpdateData,
                    xml.getInitialTransition(), locale);
            process.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/process";
    }
}
