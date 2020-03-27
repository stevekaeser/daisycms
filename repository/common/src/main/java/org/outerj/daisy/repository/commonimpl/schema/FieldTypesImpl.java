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
package org.outerj.daisy.repository.commonimpl.schema;

import org.outerj.daisy.repository.schema.FieldTypes;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerx.daisy.x10.FieldTypesDocument;
import org.outerx.daisy.x10.FieldTypeDocument;

public class FieldTypesImpl implements FieldTypes {
    private FieldType[] fieldTypes;

    public FieldTypesImpl(FieldType[] fieldTypes) {
        this.fieldTypes = fieldTypes;
    }

    public FieldType[] getArray() {
        return fieldTypes;
    }

    public FieldTypesDocument getXml() {
        FieldTypeDocument.FieldType[] fieldTypeXml = new FieldTypeDocument.FieldType[fieldTypes.length];
        for (int i = 0; i < fieldTypes.length; i++) {
            fieldTypeXml[i] = fieldTypes[i].getXml().getFieldType();
        }

        FieldTypesDocument partTypesDocument = FieldTypesDocument.Factory.newInstance();
        FieldTypesDocument.FieldTypes partTypesXml = partTypesDocument.addNewFieldTypes();
        partTypesXml.setFieldTypeArray(fieldTypeXml);
        return partTypesDocument;
    }

    public int size() {
        return fieldTypes.length;
    }
}
