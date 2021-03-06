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
package org.outerj.daisy.publisher.serverimpl;

import org.outerj.daisy.publisher.serverimpl.httphandlers.BlobHandler;
import org.outerj.daisy.publisher.serverimpl.httphandlers.PubDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.httphandlers.PubRequestHandler;
import org.outerj.daisy.httpconnector.spi.RequestHandler;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

public class PublisherHttpConnector {
    private PluginRegistry pluginRegistry;
    private List<RequestHandler> handlers;

    public PublisherHttpConnector(PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        initialize();
    }

    @PreDestroy
    public void destroy() {
        dispose();
    }

    private void initialize() throws Exception {
        handlers = new ArrayList<RequestHandler>();
        handlers.add(new BlobHandler());
        handlers.add(new PubRequestHandler());
        handlers.add(new PubDocumentHandler());

        for (RequestHandler handler : handlers) {
            pluginRegistry.addPlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }
    }

    private void dispose() {
        for (RequestHandler handler : handlers) {
            pluginRegistry.removePlugin(RequestHandler.class, handler.getNamespace() + handler.getPathPattern(), handler);
        }
    }
}
