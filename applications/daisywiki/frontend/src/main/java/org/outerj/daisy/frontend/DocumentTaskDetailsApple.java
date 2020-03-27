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
package org.outerj.daisy.frontend;

import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.MultiXMLizable;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Repository;
import org.outerx.daisy.x10Doctaskrunner.TaskDocDetailsDocument;

public class DocumentTaskDetailsApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1)
            throw new Exception("Document Task functionality not available for guest users.");

        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");

        String idParam = appleRequest.getSitemapParameter("id");
        long id;
        try {
            id = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            throw new Exception("Invalid task ID: " + idParam);
        }
        TaskDocDetails taskDocDetails = taskManager.getTaskDocDetails(id);
        TaskDocDetailsDocument taskDocDetailsDocument = taskDocDetails.getAnnotatedXml(repository);
        Task task = taskManager.getTask(id);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", new MultiXMLizable(new XmlObjectXMLizable(taskDocDetailsDocument), new XmlObjectXMLizable(task.getXml())));
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/documenttaskdetails.xsl"));

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

}
