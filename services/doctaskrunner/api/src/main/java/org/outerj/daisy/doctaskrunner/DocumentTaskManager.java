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
package org.outerj.daisy.doctaskrunner;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

/**
 * The DocumentTaskManager is concerned with the execution of a certain task
 * on a set of documents. The task is executed in a background-thread.
 * The task is executed once for each document in the set,
 * and the state of execution for each document is tracked individually. This execution
 * progress is recorded persistently, so it is possible to see afterwards if
 * the task has run on all documents, and what the outcome was (succesful or error),
 * even after server restarts.
 *
 * <p>The run-information of a task can afterwards be explicitely deleted, or the implementation
 * of DocumentTaskManager may provide automatic cleanup based on an expiration interval.
 *
 * <p>This is an optional repository extension component.
 *
 * <p>The DocumentTaskManager is obtained from the {@link org.outerj.daisy.repository.Repository Repository} as
 * follows:
 *
 * <pre>
 * DocumentTaskManager docTaskManager = (WorkflowManager)repository.getExtension("DocumentTaskManager");
 * </pre>
 *
 * <p>In the remote repository API, the DocumentTaskManager extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("DocumentTaskManager",
 *     new Packages.org.outerj.daisy.doctaskrunner.clientimpl.RemoteDocumentTaskManagerProvider());
 * </pre>
 */
public interface DocumentTaskManager {
    /**
     * Runs a task.
     *
     * <p>The documentSelection can be created via {@link #createEnumerationDocumentSelection(org.outerj.daisy.repository.VariantKey[])}
     * or {@link #createQueryDocumentSelection(String)}.
     *
     * <p>The taskSpecification can be created by instantiating {@link org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl}.
     *
     * <p>Only certain tasks can be run by non-administrators {@link org.outerj.daisy.doctaskrunner.spi.spi.DocumentAction#requiredAdministratorRole()}, since
     * using generic scripting actions (javascript, groovy (possible future implementation), ...) nasty things can be done (infinite loops, calling System.exit, etc.).
     *
     * <p>After creation of the task, this method returns immediately. The state of the task
     * can then be queried using the {@link #getTask(long)} method.
     *
     * @return the ID of the task
     */
    long runTask(DocumentSelection documentSelection, TaskSpecification taskSpecification) throws TaskException, RepositoryException;

    Task getTask(long id) throws TaskException, RepositoryException;

    /**
     * For  non-administrator users, this returns all tasks belonging to the user.
     * For users acting in the Administrator role, this returns all tasks, of all users.
     */
    Tasks getTasks() throws TaskException, RepositoryException;

    /**
     * Deletes a task. A task can only be deleted if it isn't running.
     */
    void deleteTask(long id) throws TaskException, RepositoryException;

    /**
     * Interrupts a task (i.e. stop it after the document that's currently processed).
     */
    void interruptTask(long id) throws TaskException, RepositoryException;

    TaskDocDetails getTaskDocDetails(long taskId) throws TaskException, RepositoryException;

    /**
     * @deprecated To create a task specification, just create an instance like this:
     *   new TaskSepcificationImpl(description, type, parameters, stopOnFirstError);
     *   
     *   where type == scriptLanguage and parameters == script (note the order of the arguments is different)
     */
    TaskSpecification createTaskSpecification(String description, String script, String scriptLanguage, boolean stopOnFirstError);

    DocumentSelection createQueryDocumentSelection(String query);

    /**
     *
     * @param variantKeys should not contain any duplicates
     */
    DocumentSelection createEnumerationDocumentSelection(VariantKey[] variantKeys);
    
    String[] getAllowedTasks() throws TaskException, RepositoryException;
    
}