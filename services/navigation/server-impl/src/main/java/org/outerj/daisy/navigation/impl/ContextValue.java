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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.repository.ValueType;

/**
 * A ContextValue is a value made available by a node in the navigation tree
 * for retrieval from child nodes in the navigation tree. ContextValues
 * are only provided by query nodes.
 */
public class ContextValue {
    private ValueType valueType;
    private Object value;

    public ContextValue(ValueType valueType, Object value) {
        if (valueType == null)
            throw new IllegalArgumentException("Null argument: valueType");
        if (value == null)
            throw new IllegalArgumentException("Null argument: value");

        this.valueType = valueType;
        this.value = value;
    }

    public ValueType getValueType() {
        return valueType;
    }

    public Object getValue() {
        return value;
    }
}
