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

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VersionMode;

/**
 * Use {@link FrontEndContext} instead of this class for new development.
 */
public class WikiHelper {
    public static String getMountPoint(Request request) {
        return FrontEndContext.get(request).getMountPoint();
    }

    public static String getDaisyContextPath(Request request) {
        return FrontEndContext.get(request).getDaisyContextPath();
    }

    public static String getDaisyCocoonPath(Request request) {
        return FrontEndContext.get(request).getDaisyCocoonPath();
    }

    public static SiteConf getSiteConf(Request request) {
        return FrontEndContext.get(request).getSiteConf();
    }

    public static boolean inSite(Request request) {
        return FrontEndContext.get(request).inSite();
    }

    public static Locale getLocale(Request request) {
        return FrontEndContext.get(request).getLocale();
    }

    public static String getLocaleAsString(Request request) {
        return FrontEndContext.get(request).getLocaleAsString();
    }

    public static String getSkin(Request request) {
        return FrontEndContext.get(request).getSkin();
    }

    public static Repository getRepository(Request request, ServiceManager serviceManager) throws Exception {
        return FrontEndContext.get(request).getRepository();
    }

    public static Repository getGuestRepository(ServiceManager serviceManager) throws Exception {
        GuestRepositoryProvider guestRepositoryProvider = null;
        try {
            guestRepositoryProvider = (GuestRepositoryProvider)serviceManager.lookup(GuestRepositoryProvider.ROLE);
            Repository repository = guestRepositoryProvider.getGuestRepository();
            return repository;
        } finally {
            if (guestRepositoryProvider != null)
                serviceManager.release(guestRepositoryProvider);
        }
    }

    public static Repository login(String login, String password, Request request, ServiceManager serviceManager) throws Exception {
        return FrontEndContext.get(request).login(login, password);
    }

    public static Repository getRepository(String login, String password, ServiceManager serviceManager) throws Exception {
        RepositoryManager repositoryManager = (RepositoryManager)serviceManager.lookup("daisy-repository-manager");
        try {
            Repository repository = repositoryManager.getRepository(new Credentials(login, password));
            return repository;
        } finally {
            serviceManager.release(repositoryManager);
        }
    }

    public static String getLayoutType(Request request) {
        return FrontEndContext.get(request).getLayoutType();
    }

    public static void changeLocale(Locale locale, Request request) {
        FrontEndContext.get(request).setLocale(locale);
    }

    public static void setLocaleCookie(String locale, Response response) {
        FrontEndContext.setLocaleCookie(locale, response);
    }

    public static VersionMode getVersionMode(Request request) {
        return FrontEndContext.get(request).getVersionMode();
    }

    public static void setVersionMode(Request request, VersionMode versionMode) {
        FrontEndContext.get(request).setVersionMode(versionMode);
    }
}
