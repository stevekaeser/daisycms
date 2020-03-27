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
package org.outerj.daisy.workflow;

import org.outerj.daisy.repository.RepositoryException;

import java.util.Map;
import java.util.HashMap;

public class WorkflowException extends RepositoryException {
    private String message;

    public WorkflowException() {
        super();
    }

    public WorkflowException(String message) {
        super(message);
    }

    public WorkflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public WorkflowException(Throwable cause) {
        super(cause);
    }

    public String getMessage() {
        return message != null ? message : super.getMessage();
    }

    public WorkflowException(Map state) {
        this.message = (String)state.get("message");
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        String message = getMessage();
        if (message != null)
            state.put("message", getMessage());
        return state;
    }
}
