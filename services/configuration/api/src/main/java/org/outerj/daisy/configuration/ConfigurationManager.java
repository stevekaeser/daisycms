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
package org.outerj.daisy.configuration;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.w3c.dom.Element;

/**
 * ConfigurationManager manages configuration for components.
 *
 * <p>Currently this is an Avalon/Merlin compatibility oriented.
 *
 * <p>This is a temporary solution until there's time to develop new.</p>
 */
public interface ConfigurationManager {
    /**
     * Get configuration.
     *
     * @param group group to which the configuration belongs (for organisational & namespacing purposes)
     * @param name name of the configuration
     */
    Configuration getConfiguration(String group, String name) throws ConfigurationException;

    /**
     * Gets configuration, using the specified defaultConfig as default configuration
     * (todo mention something about the cascading config).
     */
    Configuration getConfiguration(String group, String name, Configuration defaultConfig) throws ConfigurationException;

    /**
     * Converts a DOM-tree to a configuration structure.
     */
    Configuration toConfiguration(Element element, String locationDescription);
}
