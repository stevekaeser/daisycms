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

import java.io.IOException;
import java.util.Map;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.NetUtils;
import org.apache.excalibur.source.Source;

/**
 * An action for initializing the FrontEndContext and associating it
 * with the request.
 */
public class InitFrontEndContextAction implements Action, ThreadSafe, LogEnabled, Serviceable {
    private Logger logger;
    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String source, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Response response = ObjectModelHelper.getResponse(objectModel);
        FrontEndContext frontEndContext = (FrontEndContext)request.getAttribute(FrontEndContext.REQUEST_ATTR_KEY);
        if (frontEndContext != null) {
            logger.info("FrontEndContext already initialized.");
            return null;
        }

        String[] contextValues = initContext(request, sourceResolver);
        frontEndContext = new FrontEndContext(request, response, serviceManager, logger, contextValues[0], contextValues[1], contextValues[2]);
        request.setAttribute(FrontEndContext.REQUEST_ATTR_KEY, frontEndContext);

        return null;
    }

    private String[] initContext(Request request, SourceResolver sourceResolver) throws IOException {
        String mountPoint = "";
        String requestURI = stripSemicolonArgs(NetUtils.decodePath(request.getRequestURI()));
        String sitemapURI = stripSemicolonArgs(request.getSitemapURI());
        int pos = requestURI.lastIndexOf(sitemapURI);
        if (pos != -1)
            mountPoint = requestURI.substring(0, pos - 1);
        if (getLogger().isDebugEnabled())
            getLogger().debug("mountPoint = " + mountPoint);

        String contextPath = request.getContextPath();
        String daisyCocoonPath;
        if (contextPath.equals("")) {
            daisyCocoonPath = mountPoint;
        } else {
            if (!mountPoint.startsWith(contextPath)) {
                throw new RuntimeException("MountPoint does not start with contextPath.");
            }
            daisyCocoonPath = mountPoint.substring(contextPath.length());
        }


        String daisyContextPath;
        Source contextSource = sourceResolver.resolveURI("");
        try {
            daisyContextPath = contextSource.getURI();
        } finally {
            sourceResolver.release(contextSource);
        }

        return new String[] { mountPoint, daisyCocoonPath, daisyContextPath };
    }

    private String stripSemicolonArgs(String path) {
        int semicolonPos = path.lastIndexOf(';');
        if (semicolonPos != -1)
            path = path.substring(0, semicolonPos);
        return path;
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected Logger getLogger() {
        return this.logger;
    }
}
