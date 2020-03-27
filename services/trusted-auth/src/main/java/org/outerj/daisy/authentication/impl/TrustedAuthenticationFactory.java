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
package org.outerj.daisy.authentication.impl;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.outerj.daisy.authentication.spi.*;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Constructs and registers LdapAuthenticationSchemes with the UserAuthenticator.
 *
 */
public class TrustedAuthenticationFactory {
    private PluginRegistry pluginRegistry;
    private Map<String, AuthenticationScheme> schemes = new HashMap<String, AuthenticationScheme>();

    public TrustedAuthenticationFactory(Configuration configuration, PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.registerSchemes();
    }

    @PreDestroy
    public void destroy() {
        unregisterSchemes();
    }

    private void registerSchemes() throws Exception {
        for (Map.Entry<String, AuthenticationScheme> entry : schemes.entrySet()) {
            pluginRegistry.addPlugin(AuthenticationScheme.class, entry.getKey(), entry.getValue());
        }
    }

    private void unregisterSchemes() {
        for (Map.Entry<String, AuthenticationScheme> entry : schemes.entrySet()) {
            pluginRegistry.removePlugin(AuthenticationScheme.class, entry.getKey(), entry.getValue());
        }
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        Configuration[] schemeConfs = configuration.getChildren("scheme");
        for (Configuration schemeConf : schemeConfs) {
            String name = schemeConf.getAttribute("name");
            String description = schemeConf.getAttribute("description");

            List<String> keys = new ArrayList<String>();
            for (Configuration keyConfig: schemeConf.getChild("keys").getChildren("key")) {
                keys.add(keyConfig.getValue());                
            }
            
            UserCreator userCreator = UserCreatorFactory.createUser(schemeConf, name);
            
            AuthenticationScheme scheme = new TrustedAuthenticationScheme(name, description, keys, userCreator);

            Configuration cacheConf = schemeConf.getChild("cache");
            if (cacheConf.getAttributeAsBoolean("enabled")) {
                int maxCacheSize = cacheConf.getAttributeAsInteger("maxCacheSize", 3000);
                long maxCacheDuration = cacheConf.getAttributeAsLong("maxCacheDuration", 30 * 60 * 1000); // default: half an hour
                scheme = new CachingAuthenticationScheme(scheme, maxCacheDuration, maxCacheSize);
            }

            if (schemes.containsKey(name))
                throw new ConfigurationException("Duplicate authentication scheme name: " + name);

            schemes.put(name, scheme);
        }
    }
}
