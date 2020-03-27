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
import org.outerj.daisy.workflow.WfVariableDefinition;
import org.outerj.daisy.workflow.WfListItem;

import java.util.List;
import java.util.Locale;

public abstract class AbstractVariableGetter implements ValueGetter {
    protected abstract Object retrieveValue(Provider provider) throws WorkflowException;

    protected abstract WfVariableDefinition getVariableDefinition(Provider provider) throws WorkflowException;

    public Object getValue(Provider provider) throws WorkflowException {
        Object value = retrieveValue(provider);
        return value;
    }

    public Object getLabel(Provider provider, Object value, Locale locale) throws WorkflowException {
        if (value == null)
            return null;

        WfVariableDefinition def = getVariableDefinition(provider);
        if (def != null && def.getSelectionList() != null && def.getType().getTypeClass().isAssignableFrom(value.getClass())) {
            List<WfListItem> listItems = def.getSelectionList();
            for (WfListItem item : listItems) {
                if (item.getValue().equals(value)) {
                    return item.getLabel();
                }
            }
        }
        return null;
    }
}