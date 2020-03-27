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
package org.outerj.daisy.frontend.components;

import org.apache.avalon.excalibur.component.ExcaliburComponentManager;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.frontend.util.WikiPropertiesHelper;

/**
 * This component manager gets configured as 'parent component manager' in Cocoon.
 * It is initialized with the daisy.xconf from the wikidata directory.
 */
public class DaisyComponentManager implements ComponentManager, Initializable, LogEnabled, Disposable, Contextualizable {

    private final String confFileName;
    private Logger logger;
    private Context context;

    private final ExcaliburComponentManager delegate;

    public DaisyComponentManager (String confFileName) {
        this.confFileName = confFileName;
        delegate = new ExcaliburComponentManager();
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public Component lookup(String role) throws ComponentException {
        return this.delegate.lookup(role);
    }

    public boolean hasComponent(String role) {
        return delegate.hasComponent(role);
    }

    public void release(Component component) {
        this.delegate.release(component);
    }

    public void initialize() throws Exception {
        DefaultConfigurationBuilder configBuilder = new DefaultConfigurationBuilder();
        String resolvedConfFileName = PropertyResolver.resolveProperties(confFileName, WikiPropertiesHelper.getResolveProperties(context));
        Configuration config = configBuilder.buildFromFile(resolvedConfFileName);

        this.delegate.enableLogging(logger);
        this.delegate.contextualize(context);
        this.delegate.configure(config);
        this.delegate.initialize();

        this.logger.debug("Daisy Component manager successfully initialized.");
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    public void dispose() {
        delegate.dispose();
    }
}
