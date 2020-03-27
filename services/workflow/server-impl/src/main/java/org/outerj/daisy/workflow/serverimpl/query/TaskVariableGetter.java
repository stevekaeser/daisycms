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
package org.outerj.daisy.workflow.serverimpl.query;

import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfVariableDefinition;

class TaskVariableGetter extends AbstractVariableGetter {
    private final String varName;

    public TaskVariableGetter(String varName) {
        this.varName = varName;
    }

    protected Object retrieveValue(Provider provider) throws WorkflowException {
        return provider.getTaskInstance().getVariableLocally(varName);
    }

    protected WfVariableDefinition getVariableDefinition(Provider provider) throws WorkflowException {
        return provider.getTaskDefinition().getVariable(varName, VariableScope.TASK);
    }
}
