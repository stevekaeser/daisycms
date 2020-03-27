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
package org.outerj.daisy.doctaskrunner.commonimpl;

import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.Tasks;
import org.outerx.daisy.x10Doctaskrunner.TaskDocument;
import org.outerx.daisy.x10Doctaskrunner.TasksDocument;

public class TasksImpl implements Tasks {
    private final Task[] tasks;

    public TasksImpl(Task[] tasks) {
        this.tasks = tasks;
    }

    public Task[] getArray() {
        return tasks;
    }

    public TasksDocument getXml() {
        TasksDocument tasksDocument = TasksDocument.Factory.newInstance();

        TaskDocument.Task[] tasksXml = new TaskDocument.Task[tasks.length];
        for (int i = 0; i < tasks.length; i++) {
            tasksXml[i] = tasks[i].getXml().getTask();
        }

        tasksDocument.addNewTasks().setTaskArray(tasksXml);
        return tasksDocument;
    }
}
