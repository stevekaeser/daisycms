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
package org.outerj.daisy.configuration.impl;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.excalibur.configuration.CascadingConfiguration;
import org.outerj.daisy.configuration.ConfigurationManager;
import org.outerj.daisy.configutil.ConfigurationWrapper;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ConfigurationManagerImpl implements ConfigurationManager {
    private File configOverrideFile;
    private Map<String, String> merlinCompatMapping = new HashMap<String, String>();
    private Map<String, Configuration> configOverrides = new HashMap<String, Configuration>();

    public ConfigurationManagerImpl(String configOverridePath, Map<String, String> merlinCompatMapping) throws ConfigurationException {
        configOverrideFile = new File(configOverridePath);
        if (!configOverrideFile.exists())
            throw new ConfigurationException("Configuration override file does not exist: " + configOverrideFile.getAbsolutePath());

        this.merlinCompatMapping = merlinCompatMapping;

        initialize();
    }

    private void initialize() throws ConfigurationException {
        // Read the configuration overrides file
        Configuration overrideConf;
        try {
            overrideConf = new DefaultConfigurationBuilder().buildFromFile(configOverrideFile);
        } catch (Exception e) {
            throw new ConfigurationException("Error reading configuration file " + configOverrideFile.getAbsolutePath(), e);
        }

        try {
            Configuration[] targets = overrideConf.getChildren("target");
            for (Configuration target : targets) {
                String path = target.getAttribute("path");
                path = merlinCompatMapping.containsKey(path) ? merlinCompatMapping.get(path) : path;
                Configuration config = target.getChild("configuration", true);
                configOverrides.put(path, config);
            }
        } catch (Exception e) {
            throw new ConfigurationException("Error processing configuration overrides in " + configOverrideFile.getAbsolutePath(), e);
        }

    }

    public Configuration getConfiguration(String group, String name) throws ConfigurationException {
        return getConfiguration(group, name, null);
    }

    public Configuration getConfiguration(String group, String name, Configuration defaultConfig) throws ConfigurationException {
        Configuration customConfig = getConfigurationOverride(group, name);

        if (defaultConfig == null && customConfig == null)
            throw new ConfigurationException("No configuration found for " + group + "/" + name);

        if (defaultConfig != null && customConfig != null)
            return new ConfigurationWrapper(new CascadingConfiguration(customConfig, defaultConfig));
        else if (customConfig != null)
            return new ConfigurationWrapper(customConfig);
        else
            return new ConfigurationWrapper(defaultConfig);
    }

    private Configuration getConfigurationOverride(String group, String name) {
        return configOverrides.get(group + "/" + name);
    }

    public Configuration toConfiguration(Element element, String locationDescription) {
        final DefaultConfiguration configuration =
                new DefaultConfiguration(element.getNodeName(), locationDescription);
        final NamedNodeMap attributes = element.getAttributes();
        final int length = attributes.getLength();
        for (int i = 0; i < length; i++) {
            final Node node = attributes.item(i);
            final String name = node.getNodeName();
            final String value = node.getNodeValue();
            configuration.setAttribute(name, value);
        }

        boolean flag = false;
        String content = "";
        final NodeList nodes = element.getChildNodes();
        final int count = nodes.getLength();
        for (int i = 0; i < count; i++) {
            final Node node = nodes.item(i);
            if (node instanceof Element) {
                final Configuration child = toConfiguration((Element)node, locationDescription);
                configuration.addChild(child);
            } else if (node instanceof CharacterData) {
                final CharacterData data = (CharacterData)node;
                if (data.getData() != null)
                    content += data.getData();
                flag = true;
            }
        }

        if (flag) {
            configuration.setValue(content);
        }

        return new ConfigurationWrapper(configuration);
    }
    
}
