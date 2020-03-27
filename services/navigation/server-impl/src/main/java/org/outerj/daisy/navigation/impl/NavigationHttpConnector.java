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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.httpconnector.spi.RequestHandler;
import org.outerj.daisy.navigation.impl.httphandlers.NavigationLookupHandler;
import org.outerj.daisy.navigation.impl.httphandlers.NavigationTreeHandler;
import org.outerj.daisy.navigation.impl.httphandlers.NavigationPreviewHandler;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.annotation.PreDestroy;
import java.util.*;

/**
 * Exposes the functionality of the NavigationManager via HTTP.
 *
 */
public class NavigationHttpConnector {
    private PluginRegistry pluginRegistry;

    private List<RequestHandler> handlers;

    private RequestHandler compatLookupHandler;
    private RequestHandler compatPreviewHandler;
    private RequestHandler compatNavTreeHandler;

    private NavigationLookupHandler lookupHandler = new NavigationLookupHandler();
    private NavigationTreeHandler treeHandler = new NavigationTreeHandler();
    private NavigationPreviewHandler previewHandler = new NavigationPreviewHandler();

    public NavigationHttpConnector(PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        initialize();
    }

    @PreDestroy
    public void destroy() {
        dispose();
    }

    private void initialize() throws Exception {
        handlers = new ArrayList<RequestHandler>();
        handlers.add(lookupHandler);
        handlers.add(treeHandler);
        handlers.add(previewHandler);

        for (RequestHandler handler : handlers) {
            pluginRegistry.addPlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }

        // These handler only serves for backwards compatibility to support old URLs
        // that don't follow the namespaced structure (thus not "/navigation/*").
        //
        // We could consider to remove this in Daisy 3.0
        compatLookupHandler = new NavigationLookupHandler() {
            public String getNamespace() {
                return "navigationLookup";
            }

            public String getPathPattern() {
                return "";
            }
        };
        compatPreviewHandler = new NavigationPreviewHandler() {
            public String getNamespace() {
                return "navigationPreview";
            }

            public String getPathPattern() {
                return "";
            }
        };
        compatNavTreeHandler = new NavigationTreeHandler() {
            public String getNamespace() {
                return "navigation";
            }

            public String getPathPattern() {
                return "";
            }
        };

        pluginRegistry.addPlugin(RequestHandler.class, compatLookupHandler.getNamespace(), compatLookupHandler);
        pluginRegistry.addPlugin(RequestHandler.class, compatNavTreeHandler.getNamespace(), compatNavTreeHandler);
        pluginRegistry.addPlugin(RequestHandler.class, compatPreviewHandler.getNamespace(), compatPreviewHandler);
    }

    private void dispose() {
        for (RequestHandler handler : handlers) {
            pluginRegistry.removePlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }

        pluginRegistry.removePlugin(RequestHandler.class, compatLookupHandler.getNamespace(), compatLookupHandler);
        pluginRegistry.removePlugin(RequestHandler.class, compatNavTreeHandler.getNamespace(), compatNavTreeHandler);
        pluginRegistry.removePlugin(RequestHandler.class, compatPreviewHandler.getNamespace(), compatPreviewHandler);
    }
}
