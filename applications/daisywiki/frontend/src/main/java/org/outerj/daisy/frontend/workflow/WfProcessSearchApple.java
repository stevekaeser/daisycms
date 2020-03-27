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
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.workflow.*;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.xmlbeans.XmlObject;

import java.util.*;

public class WfProcessSearchApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;
    private Repository repository;
    private WorkflowManager workflowManager;
    private Locale locale;
    private SiteConf siteConf;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        repository = frontEndContext.getRepository();
        workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        locale = frontEndContext.getLocale();
        siteConf = frontEndContext.getSiteConf();

        String resource = appleRequest.getSitemapParameter("resource");
        if ("data".equals(resource)) {
            getData(appleRequest, appleResponse);
            return;
        }

        Form form = FormHelper.createForm(serviceManager, "resources/form/wfprocesssearch_definition.xml");

        boolean endProcessing = false;
        if (request.getParameter("state") != null) { // state field is used here to check if the form is on the request
            endProcessing = form.process(new FormContext(request, locale));
        } else {
            form.getChild("state").setValue("open");
            form.getChild("owner").setValue(repository.getUserLogin());
        }

        Map<String, Object> viewData = new HashMap<String, Object>();

        if (endProcessing) {
            Map<String, String> params = new HashMap<String, String>();

            // State
            String state = (String)form.getChild("state").getValue();
            if (state != null)
                params.put("state", state);

            // Description
            String description = (String)form.getChild("description").getValue();
            if (description != null)
                params.put("description", description);

            // Owner
            String owner = (String)form.getChild("owner").getValue();
            if (owner != null)
                params.put("owner", owner);

            // Process definition
            String processDefinition = (String)form.getChild("processDefinition").getValue();
            if (processDefinition != null)
                params.put("processDefinition", processDefinition);

            // Document
            String documentLink = (String)form.getChild("document").getValue();
            if (documentLink != null)
                params.put("documentLink", documentLink);

            String dataUrl = SearchHelper.buildUrl("processSearch/data", params);
            viewData.put("wfSearchResultDataUrl", dataUrl);
        }

        viewData.put("locale", locale);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("CocoonFormsInstance", form);
        viewData.put("processDefinitions", workflowManager.getAllLatestProcessDefinitions(locale)); // TODO retrieving this each time remotely is expensive

        appleResponse.sendPage("Form-wfprocesssearch-Pipe", viewData);
    }

    private void getData(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        QueryConditions queryConditions = new QueryConditions();
        Request request = appleRequest.getCocoonRequest();
        FrontEndContext frontEndContext = FrontEndContext.get(request);

        // State
        String state = request.getParameter("state");
        if ("open".equals(state)) {
            queryConditions.addCondition("process.end", WfValueType.DATETIME, "is_null");
        } else if ("finished".equals(state)) {
            queryConditions.addCondition("process.end", WfValueType.DATETIME, "is_not_null");
        }

        // Description
        String description = request.getParameter("description");
        if (description != null) {
            queryConditions.addProcessVariableCondition("daisy_description", WfValueType.STRING, "like", description);
        }

        // Owner
        String owner = request.getParameter("owner");
        if (owner != null) {
            long ownerId = repository.getUserManager().getPublicUserInfo(owner).getId();
            queryConditions.addProcessVariableCondition("daisy_owner", WfValueType.USER, "eq", new WfUserKey(ownerId));
        }

        // Process definition
        String processDefinition = request.getParameter("processDefinition");
        if (processDefinition != null) {
            queryConditions.addCondition("process.definitionName", WfValueType.STRING, "eq", processDefinition);
        }

        // Document
        String documentLink = request.getParameter("documentLink");
        if (documentLink != null) {
            WfVersionKey versionKey = WfVersionKeyUtil.parseWfVersionKey(documentLink, repository, siteConf);
            queryConditions.addSpecialCondition("relatedToDocument",
                    new WfValueType[] { WfValueType.DAISY_LINK },
                    new Object[] { versionKey });
        }

        List<QuerySelectItem> selectItems = new ArrayList<QuerySelectItem>();
        selectItems.add(new QuerySelectItem("process.id", QueryValueSource.PROPERTY));
        selectItems.add(new QuerySelectItem("daisy_description", QueryValueSource.PROCESS_VARIABLE));
        selectItems.add(new QuerySelectItem("process.start", QueryValueSource.PROPERTY));
        selectItems.add(new QuerySelectItem("process.end", QueryValueSource.PROPERTY));
        selectItems.add(new QuerySelectItem("daisy_owner", QueryValueSource.PROCESS_VARIABLE));
        selectItems.add(new QuerySelectItem("process.suspended", QueryValueSource.PROPERTY));

        // order by
        List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
        SearchHelper.OrderByParams orderByParams = SearchHelper.getOrderByParams(request);
        if (orderByParams == null)
            orderByParams = SearchHelper.getOrderByParams("process.start", QueryValueSource.PROPERTY, SortOrder.DESCENDING);
        orderByItems.add(new QueryOrderByItem(orderByParams.orderBy, orderByParams.orderBySource, orderByParams.orderByDirection));

        SearchHelper.OffsetParams offsetParams = SearchHelper.getOffsetParams(request);

        XmlObject result = workflowManager.searchProcesses(selectItems, queryConditions, orderByItems, offsetParams.offset, offsetParams.length, locale);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("wfSearchResult", new XmlObjectXMLizable(result));
        viewData.put("locale", locale);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("orderBy", orderByParams.orderBy);
        viewData.put("orderBySource", orderByParams.orderBySource.toString());
        viewData.put("orderByDirection", orderByParams.orderByDirection.toString());
        viewData.put("actionReturnTo", request.getParameter("actionReturnTo"));
        viewData.put("actionReturnToLabel", request.getParameter("actionReturnToLabel"));

        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setTemplate("resources/xml/wf_search_page.xml");
        pipeConfig.setStylesheet("daisyskin:xslt/workflow/wf_process_searchresult.xsl");
        pipeConfig.setApplyLayout(false);
        viewData.put("pipeConf", pipeConfig);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
