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
package org.outerj.daisy.workflow.serverimpl.httpconnector;

import org.outerj.daisy.httpconnector.spi.RequestHandler;
import org.outerj.daisy.workflow.serverimpl.httpconnector.handlers.*;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.ArrayList;

public class WorkflowHttpConnector {
    private PluginRegistry pluginRegistry;
    private List<RequestHandler> handlers;

    public WorkflowHttpConnector(PluginRegistry pluginRegistry) throws Exception {
        this.pluginRegistry = pluginRegistry;
        initialize();
    }

    @PreDestroy
    public void destroy() {
        dispose();
    }

    private void initialize() throws Exception {
        handlers = new ArrayList<RequestHandler>();
        handlers.add(new ProcessDefinitionsHandler());
        handlers.add(new ProcessDefinitionByNameHandler());
        handlers.add(new ProcessDefinitionHandler());
        handlers.add(new ProcessInstanceHandler());
        handlers.add(new ProcessInstancesHandler());
        handlers.add(new TaskHandler());
        handlers.add(new TasksHandler());
        handlers.add(new PoolHandler());
        handlers.add(new PoolMembershipHandler());
        handlers.add(new PoolsHandler());
        handlers.add(new PoolByNameHandler());
        handlers.add(new QueryTaskHandler());
        handlers.add(new QueryProcessHandler());
        handlers.add(new ProcessInstanceCountsHandler());
        handlers.add(new TimerHandler());
        handlers.add(new QueryTimerHandler());
        handlers.add(new InitialVariablesHandler());
        handlers.add(new AclInfoHandler());

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
