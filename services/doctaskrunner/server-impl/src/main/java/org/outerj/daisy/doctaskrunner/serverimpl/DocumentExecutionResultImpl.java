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
package org.outerj.daisy.doctaskrunner.serverimpl;

import org.outerj.daisy.doctaskrunner.DocumentExecutionResult;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;

/**
 * An instance of this is passed to the DocumentAction for each execute call to allow
 * the action to provide information about how each document was processed.
 */
public class DocumentExecutionResultImpl implements DocumentExecutionResult {

    private DocumentExecutionState state = DocumentExecutionState.DONE;
    private String message = null;
    private Exception exception = null;
    private int tryCount = 0;
    private Object returnValue;
    
    
    public DocumentExecutionState getState() {
        return state;
    }
    public void setState(DocumentExecutionState state) {
        if (state == null)
            throw new NullPointerException("state should not be null");
        this.state = state;
    }

    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    public Exception getException() {
        return exception;
    }
    public void setException(Exception exception) {
        this.exception = exception;
    }
    
    public void setTryCount(int tryCount) {
        this.tryCount = tryCount;
    }
    
    public int getTryCount() {
        return this.tryCount;
    }
    
    public void setReturnValue(Object returnValue) {
        this.returnValue = returnValue;
    }
    
    public Object getReturnValue() {
        return this.returnValue;
    }
}
