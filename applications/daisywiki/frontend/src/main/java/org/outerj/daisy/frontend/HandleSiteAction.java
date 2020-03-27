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

import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.outerj.daisy.frontend.components.siteconf.SitesManager;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;

import java.util.Map;
import java.util.Collections;

public class HandleSiteAction implements ThreadSafe, Action, Serviceable, Disposable {
    private ServiceManager serviceManager;
    private SitesManager sitesManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        this.sitesManager = (SitesManager)serviceManager.lookup(SitesManager.ROLE);
    }

    public void dispose() {
        serviceManager.release(sitesManager);
    }

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws Exception {
        String siteName = parameters.getParameter("siteName");
        SiteConf siteConf = sitesManager.getSiteConf(siteName);

        Request request = ObjectModelHelper.getRequest(objectModel);
        FrontEndContext context = FrontEndContext.get(request);
        context.setSiteConf(siteConf);
        context.setSkin(siteConf.getSkin());        

        return Collections.EMPTY_MAP;
    }
}
