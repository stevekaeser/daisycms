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

import java.util.Map;
import java.util.HashMap;

public class UserNotFoundException extends UserManagementException {
    private long id;
    private String name;

    private static final String ID_KEY = "id";
    private static final String NAME_KEY = "name";

    public UserNotFoundException(long id) {
        this.id = id;
    }

    public UserNotFoundException(String login) {
        this.name = login;
    }

    public UserNotFoundException(Map params) {
        if (params.containsKey(ID_KEY)) {
            this.id = Long.parseLong((String)params.get(ID_KEY));
        } else {
            this.name = (String)params.get(NAME_KEY);
        }
    }

    public Map<String, String> getState() {
        Map<String, String> map = new HashMap<String, String>(1);
        if (name == null)
            map.put(ID_KEY, String.valueOf(id));
        else
            map.put(NAME_KEY, name);
        return map;
    }

    public String getMessage() {
        if (name != null)
            return "The user with login \"" + name + "\" does not exist";
        else
            return "The user with ID " + id + " does not exist.";
    }
}
