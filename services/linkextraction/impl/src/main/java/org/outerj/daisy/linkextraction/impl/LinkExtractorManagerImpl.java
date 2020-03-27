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
package org.outerj.daisy.linkextraction.impl;

import org.outerj.daisy.linkextraction.LinkExtractorManager;
import org.outerj.daisy.linkextraction.LinkExtractor;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.plugin.PluginHandle;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LinkExtractorManagerImpl implements LinkExtractorManager {
    private Map<String, LinkExtractor> linkExtractors = new ConcurrentHashMap<String, LinkExtractor>(16, .75f, 1);
    private PluginRegistry pluginRegistry;
    private PluginUser<LinkExtractor> pluginUser = new MyPluginUsr();

    public LinkExtractorManagerImpl(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        pluginRegistry.setPluginUser(LinkExtractor.class, pluginUser);
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.unsetPluginUser(LinkExtractor.class, pluginUser);
    }

    public LinkExtractor getLinkExtractor(String name) {
        return linkExtractors.get(name);
    }

    public LinkExtractor[] getLinkExtractors() {
        return linkExtractors.values().toArray(new LinkExtractor[0]);
    }

    private class MyPluginUsr implements PluginUser<LinkExtractor> {

        public void pluginAdded(PluginHandle<LinkExtractor> pluginHandle) {
            if (pluginHandle.getPlugin().getDescription() == null)
                throw new IllegalArgumentException("Description of the linkextractor cannot be null.");

            linkExtractors.put(pluginHandle.getName(), pluginHandle.getPlugin());
        }

        public void pluginRemoved(PluginHandle<LinkExtractor> pluginHandle) {
            linkExtractors.remove(pluginHandle.getName());
        }
    }
}
