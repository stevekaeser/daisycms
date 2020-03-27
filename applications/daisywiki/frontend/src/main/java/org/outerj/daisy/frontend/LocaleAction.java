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

import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.cocoon.acting.Action;
import org.apache.cocoon.environment.Cookie;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Session;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.i18n.I18nUtils;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;

/**
 * Action that determines the locale and associates it with
 * the {@link FrontEndContext}.
 *
 * <p>It determines the locale as follows:
 * <ul>
 *  <li>First look in the session
 *  <li>Then look for a cookie
 *  <li>Then take the one specified in the default param.
 * </ul>
 *
 */
public class LocaleAction implements Action, ThreadSafe, Contextualizable {
    
    private Context context;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }
    
    public Map act(Redirector redirector, SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws Exception {
        Request request = ObjectModelHelper.getRequest(objectModel);
        Session session = request.getSession(false);

        Locale locale = session != null ? (Locale)session.getAttribute("locale") : null;
        String localeParam = request.getParameter("locale");
        if (localeParam != null) {
            // when a locale request parameter is specified, this only changes the language
            // for the current request, thus no session or cookie is updated
            locale = I18nUtils.parseLocale(localeParam);
        } else if (locale == null) {
            Cookie cookie = (Cookie) request.getCookieMap().get("locale");
            if (cookie != null) {
                String value = cookie.getValue();
                if (value != null) {
                    locale = I18nUtils.parseLocale(value);
                }
            }

            // If no cookie was found, use locale found in the context or in System.getProperties();
            if (locale == null) {
                locale = I18nUtils.parseLocale(WikiPropertiesHelper.getDefaultLocale(context));
            }

            // If no locale was set, use the locale from the parameters:
            if (locale == null) {
                locale = I18nUtils.parseLocale(parameters.getParameter("default"));
            }

            // Set locale in session for quick future retrieval
            if (session != null)
                session.setAttribute("locale", locale);
        }

        FrontEndContext.get(request).setLocale(locale);
        return null;
    }

}
