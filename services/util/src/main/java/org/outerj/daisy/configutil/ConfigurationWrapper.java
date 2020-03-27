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
package org.outerj.daisy.configutil;

import org.apache.avalon.framework.configuration.AbstractConfiguration;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

public class ConfigurationWrapper extends AbstractConfiguration {

    private Configuration delegate;

    public ConfigurationWrapper(Configuration delegate) {
        this.delegate = delegate;
    }
    public String getAttribute(String attr) throws ConfigurationException {
        return PropertyResolver.resolveProperties(delegate.getAttribute(attr));
    }
    public String[] getAttributeNames() {
        return delegate.getAttributeNames();
    }
    public Configuration[] getChildren() {
        return wrapConfArray(delegate.getChildren());
    }
    public Configuration[] getChildren(String name) {
        return wrapConfArray(delegate.getChildren(name));
    }
    public String getLocation() {
        return delegate.getLocation();
    }
    public String getName() {
        return delegate.getName();
    }
    public String getNamespace() throws ConfigurationException {
        return delegate.getNamespace();
    }
    public String getValue() throws ConfigurationException {
        return PropertyResolver.resolveProperties(delegate.getValue());
    }
    protected Configuration[] wrapConfArray(Configuration[] confArray) {
        for (int i = 0; i < confArray.length; i++) {
            if (confArray[i] != null)
                confArray[i] = new ConfigurationWrapper(confArray[i]);
        }
        return confArray;
    }
    public Configuration getChild(String name) {
        return getChild(name, true);
    }
    public Configuration getChild(String name, boolean create) {
        Configuration child = delegate.getChild(name, create);
        if (child == null) return null;
        return new ConfigurationWrapper(child);
    }
    @Override
    protected String getPrefix() throws ConfigurationException {
        return "";
    }
}
