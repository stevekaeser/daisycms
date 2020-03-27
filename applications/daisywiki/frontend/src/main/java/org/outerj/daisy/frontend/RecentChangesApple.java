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

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.query.QueryHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.outerx.daisy.x10.SearchResultDocument;

import java.util.Locale;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class RecentChangesApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private static final int DEFAULT_PERIOD = 7; // TODO get these values from the siteconf
    private static final int DEFAULT_LIMIT = 50;
    private ServiceManager serviceManager;
    private SiteConf siteConf;
    private Repository repository;
    private Form form;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        siteConf = frontEndContext.getSiteConf();
        repository = frontEndContext.getRepository();

        form = FormHelper.createForm(serviceManager, "resources/form/recentchanges_definition.xml");
        boolean endProcessing = true;
        if (request.getParameter("period") != null) { // check if form submission by testing presence of one of the fields
            endProcessing = form.process(new FormContext(request, locale));
        } else {
            // set default values
            form.getChild("period").setValue(new Integer(DEFAULT_PERIOD));
            form.getChild("limit").setValue(new Integer(DEFAULT_LIMIT));
            form.getChild("scope").setValue("currentSiteCollection");
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", form);
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);

        if (endProcessing) {
            String query = getQuery();
            SearchResultDocument searchResultDocument = repository.getQueryManager().performQuery(query, locale);
            viewData.put("pageXml", new XmlObjectXMLizable(searchResultDocument));
        }

        appleResponse.sendPage("Form-recentchanges-Pipe", viewData);
    }

    private String getQuery() throws Exception {
        int period = ((Integer)form.getChild("period").getValue()).intValue();
        int limit = ((Integer)form.getChild("limit").getValue()).intValue();
        String scope = (String)form.getChild("scope").getValue();

        Date fromDate = new Date(System.currentTimeMillis() - (((long)period) * 24L * 60L * 60L * 1000L));

        StringBuilder query = new StringBuilder("select id, branch, language, name, versionCreationTime, versionCreatorName, versionState where versionCreationTime > ");
        query.append(QueryHelper.formatDateTime(fromDate));

        if (scope.equals("currentSiteCollection")) {
            query.append(" and InCollection(").append(QueryHelper.formatString(getCollectionName())).append(")");
        }

        query.append(" order by versionCreationTime desc limit ");
        query.append(String.valueOf(limit));
        query.append(" option point_in_time='last'");

        return query.toString();
    }

    private String getCollectionName() throws RepositoryException {
        return repository.getCollectionManager().getCollection(siteConf.getCollectionId(), false).getName();
    }
}
