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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.workflow.WfVariable;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfValueType;
import org.outerx.daisy.x10Workflow.VariableDocument;
import org.outerx.daisy.x10Workflow.ScopeType;

public class WfVariableImpl implements WfVariable {
    private final String name;
    private final VariableScope scope;
    private final WfValueType type;
    private final Object value;

    public WfVariableImpl(String name, VariableScope scope, WfValueType type, Object value) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");
        if (scope == null)
            throw new IllegalArgumentException("Null argument: scope");
        if (type == null)
            throw new IllegalArgumentException("Null argument: type");
        if (value  == null)
            throw new IllegalArgumentException("Null argument: value");

        if (!type.getTypeClass().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("Illegal value for given variable type, expected a " + type.getTypeClass().getName() + " but got a " + value.getClass().getName());

        this.name = name;
        this.scope = scope;
        this.type = type;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public VariableScope getScope() {
        return scope;
    }

    public WfValueType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public VariableDocument getXml() {
        VariableDocument document = VariableDocument.Factory.newInstance();
        VariableDocument.Variable xml = document.addNewVariable();
        xml.setName(name);
        xml.setScope(ScopeType.Enum.forString(scope.toString()));
        WfXmlHelper.setValue(xml, type, value);

        return document;
    }

}
