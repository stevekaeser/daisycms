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
package org.outerj.daisy.repository.user;

import org.outerj.daisy.repository.RepositoryException;

import java.util.Map;
import java.util.HashMap;

/**
 * An exception that is thrown when something went wrong
 * during User Management
 */
public class UserManagementException extends RepositoryException {
    private String message;

    public UserManagementException(String string) {
        super(string);
    }

    public UserManagementException(String string, Exception e) {
        super(string, e);
    }

    public UserManagementException(String string, Throwable e) {
        super(string, e);
    }

    public UserManagementException() {
        super();
    }

    public UserManagementException(Throwable arg0) {
        super(arg0);
    }

    public UserManagementException(Map state) {
        String message = (String)state.get("message");
        this.message = message;
    }

    public String getMessage() {
        if (this.message != null)
            return this.message;
        else
            return super.getMessage();
    }

    public Map<String, String> getState() {
        if (getMessage() != null) {
            Map<String, String> state = new HashMap<String, String>(1);
            state.put("message", getMessage());
            return state;
        } else {
            return null;
        }
    }

}
