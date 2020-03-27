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
package org.outerj.daisy.configuration.spring;

import org.springframework.beans.factory.FactoryBean;
import org.apache.avalon.framework.configuration.Configuration;
import org.outerj.daisy.configuration.ConfigurationManager;
import org.w3c.dom.Element;

public class ConfigurationFactoryBean implements FactoryBean {
    private Element defaultConfig;
    private ConfigurationManager configurationManager;
    private String group;
    private String name;
    private String resourceDescription;

    public Object getObject() throws Exception {
        Configuration defaultConfig = this.defaultConfig == null ? null : configurationManager.toConfiguration(this.defaultConfig, resourceDescription);
        return configurationManager.getConfiguration(group, name, defaultConfig);
    }

    public Class getObjectType() {
        return Configuration.class;
    }

    public boolean isSingleton() {
        return false;
    }

    public void setDefaultConfiguration(Element element) {
        this.defaultConfig = element;
    }

    public void setConfigurationManager(ConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResourceDescription(String resourceDescription) {
        this.resourceDescription = resourceDescription;
    }
}
