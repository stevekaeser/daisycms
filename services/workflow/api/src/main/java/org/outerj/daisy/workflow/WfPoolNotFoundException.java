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

import java.util.Map;
import java.util.HashMap;

public class WfPoolNotFoundException extends WorkflowException {
    private long id;
    private String name;

    public WfPoolNotFoundException(long id) {
        this.id = id;
    }

    public WfPoolNotFoundException(String name) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        this.name = name;
    }

    public WfPoolNotFoundException(Map state) {
        String name = (String)state.get("name");
        if (name != null)
            this.name = name;
        else
            this.id = Long.parseLong((String)state.get("id"));
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        if (name != null)
            state.put("name", name);
        else
            state.put("id", String.valueOf(id));
        return state;
    }

    public String getMessage() {
        if (name != null)
            return "A workflow pool named \"" + name + "\" does not exist.";
        else
            return "A workflow pool with ID " + id + " does not exist.";
    }
}
