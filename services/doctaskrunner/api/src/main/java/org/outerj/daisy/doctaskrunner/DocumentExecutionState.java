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
package org.outerj.daisy.doctaskrunner;

public class DocumentExecutionState {
    private String name;
    private String code;

    private DocumentExecutionState(String name, String code) {
        this.name = name;
        this.code = code;
    }

    public String toString() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static DocumentExecutionState getByCode(String code) {
        if (code.equals("W"))
            return WAITING;
        else if (code.equals("D"))
            return DONE;
        else if (code.equals("E"))
            return ERROR;
        else if (code.equals("F"))
            return FAIL;
        else
            throw new RuntimeException("DocumentExecutionState: unrecognized code: \"" + code + "\"");
    }

    public static DocumentExecutionState fromString(String name) {
        if (WAITING.name.equals(name))
            return WAITING;
        else if (DONE.name.equals(name))
            return DONE;
        else if (FAIL.name.equals(name))
            return FAIL;
        else if (ERROR.name.equals(name))
            return ERROR;
        else
            throw new RuntimeException("DocumentExecutionState: unrecognized name: \"" + name + "\"");
    }

    public static final DocumentExecutionState WAITING = new DocumentExecutionState("waiting", "W");
    public static final DocumentExecutionState DONE = new DocumentExecutionState("done", "D");
    public static final DocumentExecutionState FAIL = new DocumentExecutionState("fail", "F");
    public static final DocumentExecutionState ERROR = new DocumentExecutionState("error", "E");
}
