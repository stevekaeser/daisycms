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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

public interface TaskContext {
    boolean isInterrupted();

    TaskState getInterruptedReason();

    void setProgress(String progress);

    void initDocumentResults(VariantKey[] keys, Repository repository);

    void setDocumentResult(VariantKey key, DocumentExecutionState state, String details, int tryCount);

    void setTaskState(TaskState state, String progress, String details, int tryCount);

    void cleanup();

    /**
     * Returns a lock that should be acquired during task execution. This lock should be taken
     * before, and released after, the execution of the task on each individual document variant.
     */
    Lock getExecutionLock();
    
    ScheduledExecutorService getExecutor();    
}
