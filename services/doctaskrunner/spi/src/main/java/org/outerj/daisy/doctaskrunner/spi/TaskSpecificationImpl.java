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
package org.outerj.daisy.doctaskrunner.spi;

import org.outerj.daisy.doctaskrunner.TaskSpecification;

public class TaskSpecificationImpl implements TaskSpecification {
    private final String description;
    private final String actionType;
    private final String actionParameters;
    private final boolean stopOnFirstError;
    private final int retryCount;
    private final int retryInterval; // in seconds
    
    private static final int defaultRetryCount = 5;
    private static final int defaultRetryInterval = -1; // we should do something with this later on such as -1 == detect how long we wait
    
    public TaskSpecificationImpl(String description, String actionType, String actionParameters, boolean stopOnFirstError) {
        this(description, actionType, actionParameters, stopOnFirstError, defaultRetryCount, defaultRetryInterval);
    }

    /**
     * @param description
     * @param actionType
     * @param actionParameters
     * @param stopOnFirstError
     * @param retryCount
     * @param retryInterval the amount of seconds between two different attempts
     */
    public TaskSpecificationImpl(String description, String actionType, String actionParameters, boolean stopOnFirstError, int retryCount, int retryInterval) {
        if (description == null)
            throw new IllegalArgumentException("description parameter is null");
        if (actionType == null)
            throw new IllegalArgumentException("actionType parameter is null");

        this.description = description;
        this.actionType = actionType;
        this.actionParameters = actionParameters;
        this.stopOnFirstError = stopOnFirstError;
        this.retryCount = retryCount;
        this.retryInterval = retryInterval;
    }

    public String getDescription() {
        return description;
    }

    public String getActionType() {
        return actionType;
    }

    public String getParameters() {
        return actionParameters;
    }

    public boolean stopOnFirstError() {
        return stopOnFirstError;
    }
    
    public int getMaxTryCount() {
    	return retryCount;
    }
    
    public int getRetryInterval() {
    	return retryInterval;
    }
    
}