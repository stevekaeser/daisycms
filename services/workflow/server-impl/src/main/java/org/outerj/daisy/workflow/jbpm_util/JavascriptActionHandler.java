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
package org.outerj.daisy.workflow.jbpm_util;

import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.Token;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.def.Task;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Scriptable;
import org.dom4j.Element;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;

/**
 * An action handler for jBPM that executes some javascript
 * read from the script tag.
 */
public class JavascriptActionHandler implements ActionHandler {
    public Element script;

    public void execute(ExecutionContext executionContext) throws Exception {
        if (script == null)
            throw new Exception("XML element is not set.");

        if (!script.isTextOnly())
            throw new Exception("The <script> element in a Daisy JavascriptActionHandler should only contain plain text, and no nested elements.");
        String source = script.getText();

        Context cx = Context.enter();
        try {
            // Make sure rhino can see the jBPM and Daisy classes
            cx.setApplicationClassLoader(this.getClass().getClassLoader());

            cx.setOptimizationLevel(-1);
            Scriptable scope = cx.initStandardObjects();

            ContextInstance contextInstance = executionContext.getContextInstance();
            Repository repository = (Repository)contextInstance.getTransientVariable("repository");
            Repository wfRepository = (Repository)contextInstance.getTransientVariable("wfRepository");
            RepositoryManager repositoryManager = (RepositoryManager)contextInstance.getTransientVariable("repositoryManager");
            Mailer mailer = (Mailer)contextInstance.getTransientVariable("mailer");
            if (repository == null || wfRepository == null || repositoryManager == null || mailer == null)
                throw new Exception("Unexpected situation: repository, wfRepository, repositoryManager or mailer transient variables not available.");

            putInScope(scope, repository, "repository");
            putInScope(scope, wfRepository, "wfRepository");
            putInScope(scope, repositoryManager, "repositoryManager");
            putInScope(scope, mailer, "mailer");
            putInScope(scope, new Variables(contextInstance, executionContext.getTaskInstance(), executionContext.getProcessInstance().getRootToken()), "variables");

            // Make available the same stuff as jBPM does for beanshell
            putInScope(scope, executionContext, "executionContext");
            putInScope(scope, executionContext.getToken(), "token");
            putInScope(scope, executionContext.getNode(), "node");

            Task task = executionContext.getTask();
            if (task != null)
                putInScope(scope, task, "task");
            TaskInstance taskInstance = executionContext.getTaskInstance();
            if (taskInstance != null)
                putInScope(scope, taskInstance, "taskInstance");

            // TODO caching of compiled scripts?
            Script script = cx.compileString(source, "", 1, null);
            script.exec(cx, scope);
        } finally {
            cx.setApplicationClassLoader(null);
            Context.exit();
        }
    }

    private void putInScope(Scriptable scope, Object javaObject, String name) {
        Object wrappedJavaObject = Context.javaToJS(javaObject, scope);
        ScriptableObject.putProperty(scope, name, wrappedJavaObject);
    }

    private static class Variables {
        private final ContextInstance contextInstance;
        private final TaskInstance taskInstance;
        private final Token rootToken;

        public Variables(ContextInstance contextInstance, TaskInstance taskInstance, Token rootToken) {
            this.contextInstance = contextInstance;
            this.taskInstance = taskInstance;
            this.rootToken = rootToken;
        }

        /**
         * Returns a variable associated with the root token.
         */
        public Object getGlobalVariable(String name) {
            return contextInstance.getLocalVariable(name, rootToken);
        }

        public void setGlobalVariable(String name, Object value) {
            contextInstance.createVariable(name, value, contextInstance.getProcessInstance().getRootToken());
        }

        public void deleteGlobalVariable(String name) {
            contextInstance.deleteVariable(name, contextInstance.getProcessInstance().getRootToken());
        }

        /**
         * Returns a variable associated with a task, throws an exception if no task is available.
         */
        public Object getTaskVariable(String name) {
            return getTaskVariable(name, true);
        }

        public void setTaskVariable(String name, Object value) {
            if (taskInstance == null)
                throw new RuntimeException("Tried to set task-local variable \"" + name + "\" but no task is available.");

            taskInstance.setVariableLocally(name, value);
        }

        public void deleteTaskVariable(String name) {
            if (taskInstance == null)
                throw new RuntimeException("Tried to delete task-local variable \"" + name + "\" but no task is available.");

            taskInstance.deleteVariableLocally(name);
        }

        /**
         * Returns a variable associated with a task.
         */
        public Object getTaskVariable(String name, boolean failOnNoTask) {
            if (taskInstance == null) {
                if (failOnNoTask)
                    throw new RuntimeException("Tried to access task-local variable \"" + name + "\" but no task is available.");
                else
                    return null;
            }
            return taskInstance.getVariableLocally(name);
        }
    }
}
