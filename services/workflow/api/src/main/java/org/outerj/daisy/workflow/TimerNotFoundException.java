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

public class TimerNotFoundException  extends WorkflowException {
    private String id;

    public TimerNotFoundException(String id) {
        if (id == null)
            throw new IllegalArgumentException("Null argument: id");

        this.id = id;
    }

    public TimerNotFoundException(Map state) {
        if (!state.containsKey("id"))
            throw new RuntimeException("exception creation problem: state is missing id");

        this.id = (String)state.get("id");
    }

    public String getMessage() {
        return "There is no timer with ID " + id;
    }

    public Map<String, String> getState() {
        Map<String, String> state = new HashMap<String, String>();
        state.put("id", id);
        return state;
    }
}

