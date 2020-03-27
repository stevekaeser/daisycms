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
package org.outerj.daisy.credentialsprovider.impl;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.RepositoryException;

public class CredentialsProviderImpl implements CredentialsProvider {
    
    public static final String NAME = "CredentialsProvider";

    private PluginRegistry pluginRegistry;
    private Map<String, Credentials> credentials = new HashMap<String, Credentials>();

    public CredentialsProviderImpl(Configuration configuration, PluginRegistry pluginRegistry) throws ConfigurationException, RepositoryException {
        this.pluginRegistry = pluginRegistry;
        configure(configuration);
        initialize();
    }
    
    public void configure(Configuration configuration) throws ConfigurationException {
        for (Configuration repo: configuration.getChildren("credentials")) {
            String key = repo.getAttribute("key");
            credentials.put(key, new Credentials(repo.getAttribute("login"), repo.getAttribute("password")));
        }
    }
    
    public void initialize() {
        pluginRegistry.addPlugin(CredentialsProvider.class, NAME, this);
    }
    
    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(CredentialsProvider.class, NAME, this);
    }

    public Credentials getCredentials(String key) {
        return credentials.get(key);
    }

}
