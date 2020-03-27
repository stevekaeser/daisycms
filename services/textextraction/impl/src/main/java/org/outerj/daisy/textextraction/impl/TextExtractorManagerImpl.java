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
package org.outerj.daisy.textextraction.impl;

import org.outerj.daisy.textextraction.TextExtractorManager;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.plugin.PluginHandle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class TextExtractorManagerImpl implements TextExtractorManager {
    private List<PluginHandle<TextExtractor>> textExtractorPlugins = new ArrayList<PluginHandle<TextExtractor>>();
    private PluginUser<TextExtractor> pluginUser = new MyPluginUser();
    private PluginRegistry pluginRegistry;
    private Map<String, TextExtractor> extractorsByMimeType = new HashMap<String, TextExtractor>();
    private final Log log = LogFactory.getLog(getClass());

    public TextExtractorManagerImpl(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
        pluginRegistry.setPluginUser(TextExtractor.class, pluginUser);
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.unsetPluginUser(TextExtractor.class, pluginUser);
    }

    public String getText(String mimeType, InputStream is) throws Exception {
        try {
            TextExtractor textExtractor = extractorsByMimeType.get(mimeType);

            if (textExtractor != null) {
                BufferedInputStream bis = new BufferedInputStream(is);
                return textExtractor.getText(bis);
            } else {
                if (log.isDebugEnabled())
                    log.debug("No textextractor registered for mimetype " + mimeType);
            }
            return null;
        } finally {
            is.close();
        }
    }

    public boolean supportsMimeType(String mimeType) {
        return extractorsByMimeType.containsKey(mimeType);
    }

    private void rebuildIndex() {
        Map<String, TextExtractor> extractorsByMimeType = new HashMap<String, TextExtractor>();

        for (PluginHandle<TextExtractor> pluginHandle : textExtractorPlugins) {
            for (String mimeType: pluginHandle.getPlugin().getMimeTypes()) {
                extractorsByMimeType.put(mimeType, pluginHandle.getPlugin());
            }
        }

        this.extractorsByMimeType = extractorsByMimeType;
    }

    private class MyPluginUser implements PluginUser<TextExtractor> {

        public void pluginAdded(PluginHandle<TextExtractor> pluginHandle) {
            textExtractorPlugins.add(pluginHandle);
            rebuildIndex();
        }

        public void pluginRemoved(PluginHandle<TextExtractor> pluginHandle) {
            textExtractorPlugins.remove(pluginHandle);
            rebuildIndex();
        }
    }
}
