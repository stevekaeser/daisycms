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
package org.outerj.daisy.navigation;

import java.util.HashMap;
import java.util.Map;

import org.outerj.daisy.repository.RepositoryException;

public class NavigationException extends RepositoryException {
    private String message;

    public NavigationException(String message) {
        super(message);
    }

    public NavigationException(String message, Throwable cause) {
        super(message, cause);
    }

    public NavigationException(Map state) {
        this.message = (String)state.get("message");
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
