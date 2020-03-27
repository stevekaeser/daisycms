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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import org.outerj.daisy.doctaskrunner.DocTaskFailException;
import org.outerj.daisy.doctaskrunner.DocumentAction;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.TaskState;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;

public class TaskRunner implements Runnable {
    private DocumentAction documentAction;
    private TaskSpecification taskSpecification;
    private TaskContext taskContext;
    private Repository repository;
    
    private LinkedList<VariantKey> variantKeys;
    
    private int initialDocumentSelectionSize;
    
    private boolean isInitialized = false;
    
    private boolean skipContextInitResults = false;
    
    private boolean isTaskStateFinal;
    
    private int tryCount = 0;
    
    private Lock executionLock;    

    private int currentPercentage = 0;
    
    private boolean hasErrors = false;
    
    public TaskRunner(DocumentAction documentAction,
            VariantKey[] remainingKeys,
            int initialDocumentSelectionSize,
            TaskSpecification taskSpecification, TaskContext taskContext, boolean skipContextInitResults,
            Repository repository, int initialTryCount, boolean initialHasErrors) {
        this.documentAction = documentAction;
        Arrays.sort(remainingKeys);
        this.variantKeys = new LinkedList<VariantKey>(Arrays.asList(remainingKeys));
        this.initialDocumentSelectionSize = initialDocumentSelectionSize;
        this.taskSpecification = taskSpecification;
        this.taskContext = taskContext;
        this.skipContextInitResults = skipContextInitResults;
        this.repository = repository;
        this.tryCount = initialTryCount;
        this.hasErrors = initialHasErrors;
        
    }

    public void run() {
        LinkedList<VariantKey> failedDocuments = new LinkedList<VariantKey>();
        tryCount++;
        
        try {
            if (!isInitialized) {
                VariantKey[] keys = variantKeys.toArray(new VariantKey[variantKeys.size()]);
                Arrays.sort(keys);
                
                normalizeDocumentIds(variantKeys);

                if (!skipContextInitResults) {
                    taskContext.setTaskState(TaskState.RUNNING, "Collecting documents to process", null, 0); 
                    taskContext.initDocumentResults(keys, repository);
                } else {
                    taskContext.setTaskState(TaskState.RESUMING_AFTER_SHUTDOWN, "Resuming after interruption by server shutdown", null, 0);
                }
                
                try {
                    documentAction.setup(keys, taskSpecification, taskContext, repository);
                } catch (Exception e) {
                    setFinalTaskState(TaskState.INTERRUPTED_BY_ERROR, "", createExceptionDescription(e));
                    throw e;
                }            
            
                executionLock = taskContext.getExecutionLock();
                if (!skipContextInitResults) {
                    taskContext.setProgress(formatProgress(0));
                }
                isInitialized = true;
            }
            
            while (!this.variantKeys.isEmpty()) {
                VariantKey variantKey = this.variantKeys.poll();
                if (taskContext.isInterrupted()) {
                    setFinalTaskState(taskContext.getInterruptedReason(), formatProgress(currentPercentage), null);
                    break; // 
                }

                // execute the script
                executionLock.lockInterruptibly();
                DocumentExecutionResultImpl result = new DocumentExecutionResultImpl();
                result.setTryCount(tryCount);
                try {
                    documentAction.execute(variantKey, result);
                } catch (DocumentLockedException dle) {
                    // We should throw a fail here
                    result.setState(DocumentExecutionState.FAIL);
                    failedDocuments.add(variantKey);
                } catch (DocTaskFailException fe) {
                    // We should throw a fail here
                    result.setState(DocumentExecutionState.FAIL);
                    failedDocuments.add(variantKey);                
                } catch (Exception e) {
                    result.setException(e);
                } finally {
                    executionLock.unlock();
                }
                
                // TODO check if there is a failure and add this to the retry queue 

                // update result state for this document variant
                if (result.getException() != null) {
                    hasErrors = true;
                    taskContext.setDocumentResult(variantKey, DocumentExecutionState.ERROR, 
                                createExceptionDescription(result.getException()), tryCount);
                } else {
                    taskContext.setDocumentResult(variantKey, result.getState(),
                                result.getMessage(), tryCount);
                }

                if (result.getException() != null && taskSpecification.stopOnFirstError()) {
                    setFinalTaskState(TaskState.INTERRUPTED_BY_ERROR, formatProgress(currentPercentage), null);
                    break;
                }

                // calculate percentage and update progress indication
                int percentage = (int)(((double)1 - ((double)variantKeys.size() / (double)initialDocumentSelectionSize)) * 100);
                if (percentage != currentPercentage) {
                    currentPercentage = percentage;
                    taskContext.setProgress(formatProgress(currentPercentage));
                }
            }
        } catch (Throwable e) {

            setFinalTaskState(TaskState.FINISHED_WITH_ERRORS, formatProgress(currentPercentage), createExceptionDescription(e));
            
        } finally {
            if(!failedDocuments.isEmpty() && this.taskSpecification.getMaxTryCount() > this.tryCount) {
                // just put the failed documents back in the queue
                this.variantKeys = failedDocuments;
                // and reschedule this task
                this.taskContext.getExecutor().schedule(this, this.taskSpecification.getRetryInterval(), TimeUnit.SECONDS);
            } 
            
            // if the max tries have been reached or all documents have been done. lets clean everything up
            if (this.variantKeys.isEmpty()  || (!this.variantKeys.isEmpty() && hasErrors && taskSpecification.stopOnFirstError()) ) {
                // stop this thing all together. So we won't be rescheduling anything
                
                // so if there are errors or that many fails that the amount of tries just didn't cut it then just give up and say 
                // that this whole thing ended with errors
                if (hasErrors) {
                    setFinalTaskState(TaskState.FINISHED_WITH_ERRORS, "", null);
                }
                if (!failedDocuments.isEmpty()) {
                    setFinalTaskState(TaskState.FINISHED_WITH_FAILURES, "", null);
                }
                // only tear down when we're really finished
                try {
                    documentAction.tearDown();
                } catch (Exception e) {
                    setFinalTaskState(TaskState.FINISHED_WITH_ERRORS, formatProgress(currentPercentage), createExceptionDescription(e));
                }
    
                setFinalTaskState(TaskState.FINISHED, "", null);
                taskContext.cleanup();                
            }
        }
    }

    private void setFinalTaskState(TaskState taskState, String progress,
            String details) {
        if (!isTaskStateFinal) {
            taskContext.setTaskState(taskState, progress, details, this.tryCount);
            isTaskStateFinal = true;
        }
    }

    private void normalizeDocumentIds(List<VariantKey> variantKeys) {
        for (int i = 0; i < variantKeys.size(); i++) {
            VariantKey variantKey = variantKeys.get(i);
            String documentId = variantKey.getDocumentId();
            String normalizedDocumentId = repository.normalizeDocumentId(documentId);
            if (!documentId.equals(normalizedDocumentId))
                variantKeys.set(i, new VariantKey(normalizedDocumentId, variantKey.getBranchId(), variantKey.getLanguageId()));
        }
    }

    private String formatProgress(int percentage) {
        return percentage + "%";
    }

    private String createExceptionDescription(Throwable throwable) {
        Writer writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

}
