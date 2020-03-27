/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend;

import java.util.Locale;

import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.environment.Cookie;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.Session;
import org.apache.cocoon.environment.http.HttpCookie;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.frontend.components.config.ConfigurationManager;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.components.siteconf.SitesManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VersionMode;

/**
 * Provides access to front end (= "Daisy Wiki") context.
 *
 * <p>Since FrontEndContext is specific for a request, it should not be stored
 * in places that exist longer than the duration of the request (e.g. session,
 * stateful flow variables, ...)
 */
public class FrontEndContext {
    /** Reference to the request to which this FrontEndContext is associated */
    private Request request;
    private Response response;
    private ServiceManager serviceManager;
    private String mountPoint;
    private String daisyContextPath;
    private String daisyCocoonPath;
    private SiteConf siteConf;
    private String skin;
    private Locale locale;
    private String localeAsString;
    private Logger logger;

    public static final String REQUEST_ATTR_KEY = "daisyFrontEndContext";

    protected FrontEndContext(Request request, Response response, ServiceManager serviceManager, Logger logger,
            String mountPoint, String daisyCocoonPath, String daisyContextPath) {
        this.request = request;
        this.response = response;
        this.serviceManager = serviceManager;
        this.logger = logger;
        setMountPoint(mountPoint);
        setDaisyCocoonPath(daisyCocoonPath);
        setDaisyContextPath(daisyContextPath);
    }

    /**
     * Gets the front end context associated with the given request object.
     * Throws an exception if it is missing.
     */
    public static FrontEndContext get(Request request) {
        FrontEndContext frontEndContext = (FrontEndContext)request.getAttribute(REQUEST_ATTR_KEY);
        if (frontEndContext == null)
            throw new RuntimeException("No FrontEndContext available.");
        return frontEndContext;
    }

    /**
     * The place where the frontend is mounted in the URL space.
     * Could e.g. by "/daisy" or an empty string. Useful for building
     * up absolute URLs.
     */
    public String getMountPoint() {
        return mountPoint;
    }

    private void setMountPoint(String mountPoint) {
        this.mountPoint = mountPoint;
        // for backwards compatibility, set it also as an attribute on the request
        request.setAttribute("mountPoint", mountPoint);
    }

    /**
     * Path on disk to the location of the Daisy front end application
     * (= subdir within the Cocoon webapp).
     */
    public String getDaisyContextPath() {
        return daisyContextPath;
    }

    private void setDaisyContextPath(String daisyContextPath) {
        this.daisyContextPath = daisyContextPath;
        // for backwards compatibility, set it also as an attribute on the request
        request.setAttribute("daisyContextPath", this.daisyContextPath);
    }

    /**
     * Place where Daisy is mounted in the Cocoon URL space. In other
     * words, what you need to put after "cocoon://" to reach the root
     * Daisy sitemap.
     *
     * <p>This string should correspond to a part (or whole) of the
     * {@link #getMountPoint mount point}.
     */
    public String getDaisyCocoonPath() {
        return daisyCocoonPath;
    }

    private void setDaisyCocoonPath(String daisyCocoonPath) {
        this.daisyCocoonPath = daisyCocoonPath;
        // for backwards compatibility, set it also as an attribute on the request
        request.setAttribute("daisyCocoonPath", daisyCocoonPath);
    }

    public boolean inSite() {
        return this.siteConf != null;
    }

    public SiteConf getSiteConf() {
        if (this.siteConf == null)
            throw new RuntimeException("Site is not set.");
        return siteConf;
    }

    public void setSiteConf(SiteConf siteConf) {
        this.siteConf = siteConf;
        
        // set request attribute for backwards compatibility
        request.setAttribute("siteConf", siteConf);
    }

    public boolean isSkinSet() {
        return skin != null;
    }

    public String getSkin() {
        if (skin == null)
            throw new RuntimeException("Skin is not set.");
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;

        // set request attribute for backwards compatibility
        request.setAttribute("skin", skin);
    }

    /**
     * Test is the locale is set. Usually this is always the case, except if an error would
     * occur before this initialization.
     */
    public boolean isLocaleSet() {
        return locale != null;
    }

    public Locale getLocale() {
        if (locale == null)
            throw new RuntimeException("Locale is not set.");
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
        this.localeAsString = locale.toString();

        // set request attributes for backwards compatibility
        request.setAttribute("locale", locale);
        request.setAttribute("localeAsString", localeAsString);
    }

    public String getLocaleAsString() {
        if (locale == null)
            throw new RuntimeException("Locale is not set.");
        return localeAsString;
    }

    public Repository getRepository() throws Exception {
        Session session = request.getSession(false);
        Repository repository = null;
        if (session != null)
            repository = (Repository)session.getAttribute("daisy-repository");
        if (repository == null) {
            return getGuestRepository();
        }
        return repository;
    }

    public Repository getGuestRepository() throws Exception {
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

    public Repository login(String login, String password) throws Exception {
        RepositoryManager repositoryManager = (RepositoryManager)serviceManager.lookup("daisy-repository-manager");
        try {
            Repository repository = repositoryManager.getRepository(new Credentials(login, password));
            Session session = request.getSession(true);
            session.setAttribute("daisy-repository", repository);
            return repository;
        } finally {
            serviceManager.release(repositoryManager);
        }
    }

    /**
     * Gets access to the repository object for a specific user, without changing
     * the current repository of the session.
     */
    public Repository getRepository(String login, String password) throws Exception {
        RepositoryManager repositoryManager = (RepositoryManager)serviceManager.lookup("daisy-repository-manager");
        try {
            Repository repository = repositoryManager.getRepository(new Credentials(login, password));
            return repository;
        } finally {
            serviceManager.release(repositoryManager);
        }
    }

    public VersionMode getVersionMode() {
        Session session = request.getSession(false);
        VersionMode versionMode = null;
        if (session != null)
            versionMode = (VersionMode)session.getAttribute("dsyVersionMode");

        if (versionMode != null)
            return versionMode;
        
        Cookie cookie = (Cookie)request.getCookieMap().get("dsyVersionMode");
        if (cookie != null)
            return VersionMode.get(cookie.getValue());

        return VersionMode.LIVE;
    }

    public void setVersionMode(VersionMode versionMode) {
        Session session = request.getSession(false);
        String mode = null;
        if (session != null) {
            session.setAttribute("dsyVersionMode", versionMode);
            return;
        }
        
        Cookie c = new HttpCookie("dsyVersionMode", String.valueOf(versionMode));
        response.addCookie(c);
    }

    public static void setLocaleCookie(String locale, Response response) {
        Cookie cookie = response.createCookie("locale", locale);
        cookie.setPath("/");
        cookie.setMaxAge(3 * 31 * 24 * 60 * 60); // about three months
        response.addCookie(cookie);
    }

    public SaxBuffer getGlobalSkinConf() throws Exception {
        SitesManager sitesManager = (SitesManager)serviceManager.lookup(SitesManager.ROLE);
        try {
            return sitesManager.getGlobalSkinConf();
        } finally {
            serviceManager.release(sitesManager);
        }
    }

    /**
     * Returns the layoutType specified on the request, or null if not specified.
     */
    public String getLayoutType() {
        return request.getParameter("layoutType");
    }

    public PageContext getPageContext() {
        return new PageContext(request);
    }

    public PageContext getPageContext(String layoutType) {
        return new PageContext(null, layoutType, request);
    }

    public PageContext getPageContext(String layoutType, Repository repository) {
        return new PageContext(repository, layoutType, request);
    }

    public PageContext getPageContext(Repository repository) {
        return new PageContext(repository, null, request);
    }

    public ConfigurationManager getConfigurationManager() {
        ConfigurationManager configurationManager = null;
        try {
            configurationManager = (ConfigurationManager)serviceManager.lookup(ConfigurationManager.ROLE);
        } catch (ServiceException e) {
            throw new RuntimeException("Error getting configuration manager.", e);
        } finally {
            if (configurationManager != null)
                serviceManager.release(configurationManager);
        }
        return configurationManager;
    }

    public Logger getLog() {
        return logger;
    }

}
