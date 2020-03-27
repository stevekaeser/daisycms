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

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.serverimpl.query.QueryMetadataRegistry;
import org.apache.commons.logging.Log;

import java.util.Locale;

/**
 * Internal workflow context object.
 */
public class IntWfContext {
    private WorkflowMetaManager wfMetaManager;
    private WfObjectBuilder wfObjectBuilder;
    private Repository repository;
    private WfPoolManager poolManager;
    private QueryMetadataRegistry registry;
    private Locale locale;
    private Log log;

    public IntWfContext(WorkflowMetaManager wfMetaManager, WfObjectBuilder wfObjectBuilder,
            QueryMetadataRegistry registry, Repository repository, WfPoolManager poolManager, Locale locale,
            Log log) {
        this.wfMetaManager = wfMetaManager;
        this.wfObjectBuilder = wfObjectBuilder;
        this.registry = registry;
        this.repository = repository;
        this.poolManager = poolManager;
        this.locale = locale;
        this.log = log;
    }

    public WorkflowMetaManager getWorkflowMetaManager() {
        return wfMetaManager;
    }

    public WfObjectBuilder getWfObjectBuilder() {
        return wfObjectBuilder;
    }

    public Repository getRepository() {
        return repository;
    }

    public WfPoolManager getPoolManager() {
        return poolManager;
    }

    public QueryMetadataRegistry getQueryMetadataRegistry() {
        return registry;
    }

    public Locale getLocale() {
        return locale;
    }

    public Log getLogger() {
        return log;
    }
}
