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
package org.outerj.daisy.doctaskrunner.serverimpl;

import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.Tasks;
import org.outerj.daisy.doctaskrunner.commonimpl.EnumerationDocumentSelection;
import org.outerj.daisy.doctaskrunner.commonimpl.QueryDocumentSelection;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

public class DocumentTaskManagerImpl implements DocumentTaskManager {
    private final CommonDocumentTaskManager delegate;
    private final Repository repository;

    public DocumentTaskManagerImpl(CommonDocumentTaskManager delegate, Repository repository) {
        this.delegate = delegate;
        this.repository = repository;
    }

    public long runTask(DocumentSelection documentSelection, TaskSpecification taskSpecification) throws TaskException {
        return delegate.runTask(documentSelection, taskSpecification, repository);
    }

    public Task getTask(long id) throws TaskException {
        return delegate.getTask(id, repository);
    }

    public Tasks getTasks() throws TaskException {
        return delegate.getTasks(repository);
    }

    public void deleteTask(long id) throws TaskException {
        delegate.deleteTask(id, repository);
    }

    public void interruptTask(long id) throws TaskException {
        delegate.interruptTask(id, repository);
    }
    
    public TaskDocDetails getTaskDocDetails(long taskId) throws TaskException {
        return delegate.getTaskDocDetails(taskId, repository);
    }

    public TaskSpecification createTaskSpecification(String description, String script, String scriptLanguage, boolean stopOnFirstError) {
        return new TaskSpecificationImpl(description, scriptLanguage, script, stopOnFirstError);
    }

    public DocumentSelection createQueryDocumentSelection(String query) {
        return new QueryDocumentSelection(query);
    }

    public DocumentSelection createEnumerationDocumentSelection(VariantKey[] variantKeys) {
        return new EnumerationDocumentSelection(variantKeys);
    }
    
    public String[] getAllowedTasks() throws TaskException {
    	return delegate.getAllowedTasks(repository);
    }

}