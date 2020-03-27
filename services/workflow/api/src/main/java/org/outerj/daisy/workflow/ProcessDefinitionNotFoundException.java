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

public class ProcessDefinitionNotFoundException extends WorkflowException {
    private String name;
    private String id;

    public ProcessDefinitionNotFoundException(String nameOrId, boolean isName) {
        if (nameOrId == null)
            throw new IllegalArgumentException("Null argument: nameOrId");

        if (isName)
            name = nameOrId;
        else
            id = nameOrId;
    }

    public ProcessDefinitionNotFoundException(Map state) {
        if (state.containsKey("name"))
            this.name = (String)state.get("name");
        else if (state.containsKey("id"))
            this.id = (String)state.get("id");
        else
            throw new RuntimeException("exception creation problem: state is missing name or id");
    }

    public String getMessage() {
        if (name != null)
            return "There is no workflow process definition named \"" + name + "\".";
        else
            return "There is no workflow process definition with ID " + id;
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        if (name != null)
            state.put("name", name);
        else if (id != null)
            state.put("id", id);
        return state;
    }
}
