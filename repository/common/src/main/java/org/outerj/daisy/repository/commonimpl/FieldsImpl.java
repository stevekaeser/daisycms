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

import org.outerj.daisy.repository.Field;
import org.outerj.daisy.repository.Fields;
import org.outerx.daisy.x10.FieldDocument;
import org.outerx.daisy.x10.FieldsDocument;

public class FieldsImpl implements Fields {
    private final Field[] fields;

    public FieldsImpl(Field[] fields) {
        this.fields = fields;
    }

    public Field[] getArray() {
        return fields;
    }

    public FieldsDocument getXml() {
        FieldsDocument fieldsDocument = FieldsDocument.Factory.newInstance();
        FieldDocument.Field[] fieldsXml = new FieldDocument.Field[fields.length];
        for (int i = 0; i < fields.length; i++) {
            fieldsXml[i] = fields[i].getXml().getField();
        }
        fieldsDocument.addNewFields().setFieldArray(fieldsXml);
        return fieldsDocument;
    }
}
