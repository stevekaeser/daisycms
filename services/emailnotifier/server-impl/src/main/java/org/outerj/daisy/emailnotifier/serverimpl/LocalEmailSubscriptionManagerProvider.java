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
package org.outerj.daisy.emailnotifier.serverimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.sql.DataSource;
import javax.annotation.PreDestroy;

public class LocalEmailSubscriptionManagerProvider {
    private PluginRegistry pluginRegistry;
    private ExtensionProvider extensionProvider;
    private DataSource dataSource;
    private Context context = new Context();
    private JdbcHelper jdbcHelper;
    private final Log log = LogFactory.getLog(getClass());
    private static final String EXTENSION_NAME = "EmailSubscriptionManager";

    public LocalEmailSubscriptionManagerProvider(DataSource dataSource, PluginRegistry pluginRegistry) throws Exception {
        this.dataSource = dataSource;
        this.pluginRegistry = pluginRegistry;
        this.initialize();
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private void initialize() throws Exception {
        extensionProvider = new MyExtensionProvider();
        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
        jdbcHelper = JdbcHelper.getInstance(context.getDataSource(), context.getLogger());
    }

    class MyExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new LocalEmailSubscriptionManager(repository, context, jdbcHelper);
        }
    }

    public class Context {
        private Context() {
        }

        public DataSource getDataSource() {
            return dataSource;
        }

        public Log getLogger() {
            return log;
        }
    }
}
