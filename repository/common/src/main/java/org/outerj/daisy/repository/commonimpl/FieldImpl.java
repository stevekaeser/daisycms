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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerx.daisy.x10.FieldDocument;
import org.outerx.daisy.x10.FieldValuesType;

public class FieldImpl implements Field {
    private long typeId;
    private Object value;
    private ValueType cachedValueType;
    private boolean cachedMultiValue;
    private boolean cachedHierarchical;
    private DocumentVariantImpl.IntimateAccess ownerVariantInt;

    public FieldImpl(DocumentVariantImpl.IntimateAccess ownerVariantInt, long typeId, Object value) {
        if (value == null)
            throw new NullPointerException("Field value cannot be null!");
        this.ownerVariantInt = ownerVariantInt;
        this.typeId = typeId;
        this.value = value;
    }

    public long getTypeId() {
        return typeId;
    }

    public Object getValue() {
        // value of this field should be immutable
        if (value instanceof Object[])
            return ((Object[])value).clone();
        else
            return value;
    }

    public ValueType getValueType() {
        assureTypeInfoLoaded();
        return cachedValueType;
    }

    public boolean isMultiValue() {
        assureTypeInfoLoaded();
        return cachedMultiValue;
    }

    public boolean isHierarchical() {
        assureTypeInfoLoaded();
        return cachedHierarchical;
    }

    private void assureTypeInfoLoaded() {
        if (cachedValueType == null) {
            FieldType fieldType;
            try {
                fieldType = ownerVariantInt.getRepositorySchema().getFieldTypeById(typeId, false, ownerVariantInt.getCurrentUser());
            } catch (RepositoryException e) {
                throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
            }
            cachedValueType = fieldType.getValueType();
            cachedMultiValue = fieldType.isMultiValue();
            cachedHierarchical = fieldType.isHierarchical();
        }
    }

    public String getTypeName() {
        FieldType fieldType;
        try {
            fieldType = ownerVariantInt.getRepositorySchema().getFieldTypeById(typeId, false, ownerVariantInt.getCurrentUser());
        } catch (RepositoryException e) {
            throw new RuntimeException(DocumentImpl.ERROR_ACCESSING_REPOSITORY_SCHEMA, e);
        }
        return fieldType.getName();
    }

    public FieldDocument getXml() {
        FieldDocument fieldDocument = FieldDocument.Factory.newInstance();
        FieldDocument.Field fieldXml = fieldDocument.addNewField();
        fieldXml.setTypeId(typeId);

        if (isMultiValue()) {
            Object[] values = (Object[])value;
            for (Object value : values)
                addHierarchyPathToXml(fieldXml, value);
        } else {
            addHierarchyPathToXml(fieldXml, value);
        }

        return fieldDocument;
    }


    private void addHierarchyPathToXml(FieldDocument.Field fieldXml, Object value) {
        if (isHierarchical()) {
            FieldValuesType.HierarchyPath hierarchyPathXml = fieldXml.addNewHierarchyPath();
            HierarchyPath path = (HierarchyPath)value;
            for (Object pathElement : path.getElements()) {
                addValueToXml(hierarchyPathXml, pathElement);
            }
        } else {
            addValueToXml(fieldXml, value);
        }
    }

    private void addValueToXml(FieldValuesType valuesXml, Object value) {
        FieldHelper.getXmlFieldValueSetter(getValueType()).addValue(value, valuesXml);
    }
}