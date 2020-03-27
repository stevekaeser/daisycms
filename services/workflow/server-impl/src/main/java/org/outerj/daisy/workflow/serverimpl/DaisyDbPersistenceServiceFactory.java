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
package org.outerj.daisy.workflow.serverimpl;

import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.persistence.db.DbPersistenceService;
import org.jbpm.svc.Service;
import org.hibernate.cfg.Configuration;

import java.util.Properties;

/**
 * An extension of jBPM's DbPersistenceServiceFactory that allows to set
 * custom hibernate properties. The problem with jBPM's default configuration
 * is that it only allows to specify a path to a hibernate properties file,
 * making it difficult to set the properties programmatically.
 *
 * <p>There might be cleaner ways around this by integrating at a lower
 * level into jBPM, but this is a simple and effective solution.
 *
 * <p>The properties have to be provided through a thread-local variable.
 */
public class DaisyDbPersistenceServiceFactory extends DbPersistenceServiceFactory {
    public static ThreadLocal<Properties> HIBERNATE_PROPERTIES = new ThreadLocal<Properties>();

    public DaisyDbPersistenceServiceFactory() {
    }

    public Service openService() {
        return new DbPersistenceService(this);
    }

    public synchronized Configuration getConfiguration() {
        Configuration configuration = super.getConfiguration();
        Properties properties = HIBERNATE_PROPERTIES.get();
        configuration.setProperties(properties);
        return configuration;
    }
}
