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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.workflow.*;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.SortOrder;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.xmlbeans.XmlObject;

import java.util.*;

/**
 * Controller for showing the user his lists of tasks.
 */
public class WfTasksApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        Repository repository = frontEndContext.getRepository();
        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

        if (request.getMethod().equals("GET")) {
            String resource = appleRequest.getSitemapParameter("resource");

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("locale", locale);

            if (resource == null || "".equals(resource)) {
                viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/workflow/wf_tasks.xsl"));
                appleResponse.sendPage("internal/genericPipe", viewData);
            } else if ("mine".equals(resource) || "pooled".equals(resource)) {
                QueryConditions queryConditions = new QueryConditions();
                if (resource.equals("mine")) {
                    queryConditions.addCondition("task.actor", WfValueType.USER, "eq", new WfUserKey(repository.getUserId()));
                } else if (resource.equals("pooled")) {
                    queryConditions.addSpecialCondition("tasksInMyPool", new WfValueType[0], new Object[0]);
                    queryConditions.addCondition("task.actor", WfValueType.USER, "is_null");
                }
                queryConditions.addCondition("task.end", WfValueType.DATETIME, "is_null");

                List<QuerySelectItem> selectItems = new ArrayList<QuerySelectItem>();
                selectItems.add(new QuerySelectItem("task.id", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("daisy_description", QueryValueSource.PROCESS_VARIABLE));
                selectItems.add(new QuerySelectItem("task.create", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.definitionLabel", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.priority", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.actor", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.dueDate", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("process.suspended", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.isOpen", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("task.hasPools", QueryValueSource.PROPERTY));
                selectItems.add(new QuerySelectItem("daisy_owner", QueryValueSource.PROCESS_VARIABLE));
                selectItems.add(new QuerySelectItem("task.end", QueryValueSource.PROPERTY));

                List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
                SearchHelper.OrderByParams orderByParams = SearchHelper.getOrderByParams(request);
                if (orderByParams == null)
                    orderByParams = SearchHelper.getOrderByParams("task.create", QueryValueSource.PROPERTY, SortOrder.DESCENDING);
                orderByItems.add(new QueryOrderByItem(orderByParams.orderBy, orderByParams.orderBySource, orderByParams.orderByDirection));

                SearchHelper.OffsetParams offsetParams = SearchHelper.getOffsetParams(request);

                XmlObject searchResult = workflowManager.searchTasks(selectItems, queryConditions, orderByItems, offsetParams.offset, offsetParams.length, locale);

                viewData.put("wfSearchResult", new XmlObjectXMLizable(searchResult));
                viewData.put("orderBy", orderByParams.orderBy);
                viewData.put("orderBySource", orderByParams.orderBySource.toString());
                viewData.put("orderByDirection", orderByParams.orderByDirection.toString());
                viewData.put("selection", resource);
                viewData.put("actionReturnTo", request.getParameter("actionReturnTo"));
                viewData.put("actionReturnToLabel", request.getParameter("actionReturnToLabel"));

                GenericPipeConfig pipeConfig = new GenericPipeConfig();
                pipeConfig.setTemplate("resources/xml/wf_search_page.xml");
                pipeConfig.setStylesheet("daisyskin:xslt/workflow/wf_tasks_list.xsl");
                pipeConfig.setApplyLayout(false);
                viewData.put("pipeConf", pipeConfig);

                appleResponse.sendPage("internal/genericPipe", viewData);
            } else {
                throw new Exception("Invalid tasks resource: \"" + resource + "\".");
            }
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }
}
