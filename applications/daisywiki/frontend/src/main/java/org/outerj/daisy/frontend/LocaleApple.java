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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.Session;
import org.apache.cocoon.i18n.I18nUtils;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.ResponseUtil;

public class LocaleApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable, LogEnabled {
    private ServiceManager serviceManager;
    private Logger logger;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String returnTo = request.getParameter("returnTo");
        String localeParam = request.getParameter("locale");

        if (localeParam == null) {
            // show locale selection page
            String submitPath = getMountPoint() + "/locale?";
            if (returnTo != null)
                submitPath += "returnTo=" + URLEncoder.encode(returnTo, "UTF-8");

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("submitPath", submitPath);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("locales", new AvailableLocales(serviceManager, logger).getLocales());
            viewData.put("locale", frontEndContext.getLocale());

            GenericPipeConfig pipeConf = new GenericPipeConfig();
            pipeConf.setTemplate("resources/xml/select_locale.xml");
            pipeConf.setStylesheet("daisyskin:xslt/select_locale.xsl");
            viewData.put("pipeConf", pipeConf);

            appleResponse.sendPage("internal/genericPipe", viewData);
        } else {
            Locale locale = I18nUtils.parseLocale(localeParam);
            Response response = appleResponse.getCocoonResponse();
            FrontEndContext.setLocaleCookie(localeParam, response);

            // Set locale in session for quick future retrieval
            Session session = request.getSession(false);
            if (session != null)
                session.setAttribute("locale", locale);

            FrontEndContext.get(request).setLocale(locale);

            if (returnTo == null || returnTo.equals(""))
                ResponseUtil.safeRedirect(appleRequest, appleResponse, getMountPoint() + "/");
            else
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
        }
    }
}
