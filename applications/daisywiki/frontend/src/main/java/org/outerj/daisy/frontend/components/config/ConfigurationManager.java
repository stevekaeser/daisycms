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
package org.outerj.daisy.frontend.components.config;

import org.apache.avalon.framework.configuration.Configuration;

/**
 * A place to get configuration information from.
 *
 * <p>There are three levels of configuration:
 * <ul>
 *   <li>site-level config
 *   <li>datadir config
 *   <li>webapp config
 * </ul>
 *
 * <p>Configuration is addressed by slash-separated paths, paths do not start or end with slash.
 */
public interface ConfigurationManager {

    static final String ROLE = ConfigurationManager.class.getName();

    /**
     * @param fallback fallback to webapp conf
     * @return the configuration, or null if there is no configuration
     */
    Configuration getConfiguration(String path, boolean fallback);

    /**
     *
     * @param site name of the Daisy Wiki site
     * @param fallback fallback to global conf (datadir, webapp)
     * @return the configuration, or null if there is no configuration
     */
    Configuration getConfiguration(String site, String path, boolean fallback);

    /**
     * Does getConfiguration(path, true).
     */
    Configuration getConfiguration(String path);

    /**
     * Does getConfiguration(site, path, true).
     */
    Configuration getConfiguration(String site, String path);
}
