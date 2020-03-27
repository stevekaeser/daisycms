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
package org.outerj.daisy.frontend.components.siteconf;

import org.apache.cocoon.xml.SaxBuffer;

import java.util.List;

public interface SitesManager {
    public static final String ROLE = SitesManager.class.getName();

    /**
     * Returns the SiteConf object for the named site. This method
     * throws an exception if it does not exist.
     */
    SiteConf getSiteConf(String siteName) throws Exception;

    /**
     * Gets the SiteConf object for the named site, does not
     * throw an exception but returns null if the site does not exist.
     */
    SiteConf getSiteConfSoftly(String name);

    /**
     * Returns the global (non-site-specific) skinconf. Can be null.
     */
    SaxBuffer getGlobalSkinConf();

    /**
     * Returns the global (non-site-specific) skin name. If not explicitely
     * configured by the user, this returns null.
     */
    String getGlobalSkinName();

    /**
     * Let the SitesManager know there is a new site created. Normally, this
     * is detected by listening to file system changes, but that can take a
     * while. If you want the site to be synchronously available, you can
     * use this method.
     */
    void addNewSite(String name) throws Exception;

    /**
     * Returns a list of all available sites. The list
     * contains the SiteConf object for each site.
     */
    List<SiteConf> getSiteConfs() throws Exception;

    String getGlobalCocoonSitemapLocation();
}
