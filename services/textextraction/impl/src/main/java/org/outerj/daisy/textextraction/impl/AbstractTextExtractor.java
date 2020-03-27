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

import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Collections;

public abstract class AbstractTextExtractor implements TextExtractor {
    private final List<String> mimeTypes;
    private PluginRegistry pluginRegistry;

    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    /**
     * The default constructor is useful if you want to use a textextractor without
     * registering it as a plugin (testing, outside repository server, ...).
     */
    public AbstractTextExtractor() {
        mimeTypes = Collections.emptyList();
    }

    protected abstract String getName();

    public AbstractTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        this.mimeTypes = mimeTypes;
        this.pluginRegistry = pluginRegistry;
        pluginRegistry.addPlugin(TextExtractor.class, getName(), this);
    }

    @PreDestroy
    public void destroy() {
        if (pluginRegistry != null)
            pluginRegistry.removePlugin(TextExtractor.class, getName(), this);
    }
}
