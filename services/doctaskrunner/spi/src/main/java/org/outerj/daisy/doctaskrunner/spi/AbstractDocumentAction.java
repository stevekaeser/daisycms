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
package org.outerj.daisy.doctaskrunner.spi;

import org.outerj.daisy.doctaskrunner.DocumentAction;
import org.outerj.daisy.doctaskrunner.DocumentExecutionResult;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

/**
 * Support class for DocumentActions 
 */
public abstract class AbstractDocumentAction implements DocumentAction {
    
    protected VariantKey[] variantKeys;
    protected TaskSpecification taskSpecification;
    protected TaskContext taskContext;
    protected Repository repository;
    
    public void setup(VariantKey[] variantKeys, TaskSpecification taskSpecifiation, TaskContext taskContext, Repository repository) throws Exception {
        this.variantKeys = variantKeys;
        this.taskSpecification = taskSpecifiation;
        this.taskContext = taskContext;
        this.repository = repository;
    }

    abstract public void execute(VariantKey variantKey, DocumentExecutionResult documentExecutionResult) throws Exception;
    
    public void tearDown() throws Exception {
        //
    }
    
    public boolean requiresAdministratorRole() {
        return true;
    }
}
