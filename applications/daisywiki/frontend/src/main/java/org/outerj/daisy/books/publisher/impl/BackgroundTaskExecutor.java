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
package org.outerj.daisy.books.publisher.impl;

import org.apache.cocoon.Constants;
import org.apache.cocoon.Processor;
import org.apache.cocoon.components.CocoonComponentManager;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.component.WrapperComponentManager;

import java.util.Date;

public class BackgroundTaskExecutor implements Runnable {
    private final ServiceManager serviceManager;
    private final Context context;
    private final Logger logger;
    private final BookPublishTask bookPublishTask;
    private String taskId;
    private BookPublisherImpl owner;
    private Date started = new Date();

    public BackgroundTaskExecutor(BookPublishTask bookPublishTask, BookPublisherImpl owner, Logger logger, Context context, ServiceManager serviceManager) {
        this.bookPublishTask = bookPublishTask;
        this.logger = logger;
        this.context = context;
        this.serviceManager = serviceManager;
        this.owner = owner;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public BookPublishTask getBookPublishTask() {
        return bookPublishTask;
    }

    public Date getStarted() {
        return started;
    }

    /**
     * Note: setTaskId must be called before run!
     */
    public void run() {
        try {
            started = new Date();
            org.apache.cocoon.environment.Context envContext =
                    (org.apache.cocoon.environment.Context) context.get(Constants.CONTEXT_ENVIRONMENT_CONTEXT);

            BackgroundEnvironment env = new BackgroundEnvironment(logger, envContext);

            Processor processor = (Processor) serviceManager.lookup(Processor.ROLE);

            Object key = CocoonComponentManager.startProcessing(env);
            CocoonComponentManager.enterEnvironment(env, new WrapperComponentManager(serviceManager), processor);

            try {
                bookPublishTask.run();
            } finally {
                CocoonComponentManager.leaveEnvironment();
                CocoonComponentManager.endProcessing(env, key);
                serviceManager.release(processor);
            }
        } catch (Throwable e) {
            logger.error("Unexpected error in book publication background thread.", e);
        } finally {
            try {
                bookPublishTask.getBookInstance().unlock();
            } catch (Throwable e) {
                logger.error("Error unlocking book instance " + bookPublishTask.getBookInstance().getName(), e);
            }
            owner.taskEnded(taskId);
        }
    }
}
