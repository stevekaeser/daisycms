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
package org.outerj.daisy.repository;

import java.util.Map;
import java.util.HashMap;

/**
 * Exception thrown in case of permission-related problems.
 */
public class AccessException extends RepositoryException {
    String message;

    protected AccessException() {
    }

    public AccessException(String message) {
        super(message);
        this.message = message;
    }

    public AccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccessException(Map params) {
        this.message = (String)params.get("message");
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>(1);
        if (message != null)
            map.put("message", message);
        return map;
    }

    public String getMessage() {
        return message != null ? message : super.getMessage();
    }
}
