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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.components.siteconf.SitesManager;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.acl.AclPermission;

public class IndexPageApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable, Disposable {
    private ServiceManager serviceManager;
    private SitesManager sitesManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        this.sitesManager = (SitesManager)serviceManager.lookup(SitesManager.ROLE);
    }

    public void dispose() {
        if (sitesManager != null)
            serviceManager.release(sitesManager);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();

        // create array containg home page document IDs
        Collection<SiteConf> siteConfs = sitesManager.getSiteConfs();
        List<VariantKey> homePageDocKeys = new ArrayList<VariantKey>(siteConfs.size());
        for (SiteConf siteConf : siteConfs) {
            if (siteConf.getHomePageDocId() != null)
                homePageDocKeys.add(new VariantKey(siteConf.getHomePageDocId(), siteConf.getBranchId(), siteConf.getLanguageId()));
        }

        // filter them
        VariantKey[] filteredHomePageDocKeys = repository.getAccessManager().filterDocuments(homePageDocKeys.toArray(new VariantKey[homePageDocKeys.size()]), AclPermission.READ, frontEndContext.getVersionMode().isLast());
        Arrays.sort(filteredHomePageDocKeys);

        // create filtered site confs collection
        ArrayList<SiteConf> filteredSiteConfs = new ArrayList<SiteConf>(siteConfs.size());
        for (SiteConf siteConf : siteConfs) {
            if (siteConf.getHomePageDocId() == null
                    || (Arrays.binarySearch(filteredHomePageDocKeys, new VariantKey(siteConf.getHomePageDocId(), siteConf.getBranchId(), siteConf.getLanguageId())) >= 0)) {
                filteredSiteConfs.add(siteConf);
            }
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("sites", filteredSiteConfs);
        viewData.put("pageContext", frontEndContext.getPageContext());

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/indexpage.xml");
        pipeConf.setStylesheet("daisyskin:xslt/indexpage.xsl");
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
