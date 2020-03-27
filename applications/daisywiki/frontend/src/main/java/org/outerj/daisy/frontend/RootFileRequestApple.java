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
package org.outerj.daisy.frontend;

import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.outerj.daisy.frontend.components.siteconf.SitesManager;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;

/**
 * Handles requests for files below the root (where daisy is mounted),
 * for files which are not handled earlier. All such resources either
 * don't exist, or are site names.
 */
public class RootFileRequestApple implements StatelessAppleController, Serviceable {

    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void process(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String path = appleRequest.getSitemapParameter("path");

        SitesManager sitesManager = (SitesManager)serviceManager.lookup(SitesManager.ROLE);
        try {
            SiteConf siteConf = sitesManager.getSiteConfSoftly(path);

            if (siteConf == null) {
                throw new ResourceNotFoundException("Resource not found: " + path);
            } else {
                FrontEndContext frontEndContext = FrontEndContext.get(appleRequest.getCocoonRequest());
                appleResponse.redirectTo(frontEndContext.getMountPoint() + "/" + siteConf.getName() + "/" + siteConf.getHomePage());
            }
        } finally {
            serviceManager.release(sitesManager);
        }
    }
}
