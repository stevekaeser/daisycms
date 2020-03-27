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
package org.outerj.daisy.tools.importexport.model.document;

import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.HierarchyPath;

public class ImpExpField {
    private FieldType type;
    private Object value;

    public ImpExpField(FieldType fieldType, Object value) {
        if (fieldType == null)
            throw new IllegalArgumentException("Null argument: fieldType");

        this.type = fieldType;
        setValue(value);
    }

    public FieldType getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (value == null)
            throw new IllegalArgumentException("Null argument: value");
        checkFieldValue(value);
        this.value = value;
    }

    private void checkFieldValue(Object value) {
        if (type.isMultiValue()) {
            if (!value.getClass().isArray())
                throw new IllegalArgumentException("The value for the multivalue-field \"" + type.getName() + "\" should be an array.");
            Object[] values = (Object[])value;
            if (values.length == 0)
                throw new IllegalArgumentException("The value supplied for the multivalue field \"" + type.getName() + "\" is a zero-length array, it should be an array of at least one element.");
            for (Object subValue : values) {
                checkValueHierarchical(subValue);
            }
        } else {
            checkValueHierarchical(value);
        }
    }

    private void checkValueHierarchical(Object value) {
        if (type.isHierarchical()) {
            if (!(value instanceof HierarchyPath)) {
                if (type.isMultiValue())
                    throw new IllegalArgumentException("The values in the array supplied for multivalue hierarchical field \"" + type.getName() + "\" should be HierarchyPath objects.");
                else
                    throw new IllegalArgumentException("The value supplied for hierarchical field \"" + type.getName() + "\" should be a HierarchyPath object.");
            }

            Object[] elements = ((HierarchyPath)value).getElements();
            for (Object element : elements) {
                checkPrimitiveValue(element);
            }
        } else {
            checkPrimitiveValue(value);
        }
    }

    private void checkPrimitiveValue(Object value) {
        ValueType valueType = type.getValueType();
        if (!valueType.getTypeClass().isAssignableFrom(value.getClass()))
            throw new IllegalArgumentException("The supplied value for the field \"" + type.getName() + "\" is not of the correct type. Expected was a " + valueType.toString() + " (" + valueType.getTypeClass().getName() + ") but got a " + value.getClass().getName());
    }
}
