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

import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerx.daisy.x10.FieldTypeUseDocument;

public class FieldTypeUseImpl implements FieldTypeUse {
    private FieldType fieldType;
    private boolean required;
    private boolean editable;
    private DocumentTypeImpl owner;
    private static final String READ_ONLY_MESSAGE = "This field-type-use is read-only.";

    public FieldTypeUseImpl(DocumentTypeImpl owner, FieldType fieldType, boolean required, boolean editable) {
        this.owner = owner;
        this.fieldType = fieldType;
        this.required = required;
        this.editable = editable;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        if (owner.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.required = required;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        if (owner.isReadOnly())
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.editable = editable;
    }

    public FieldTypeUseDocument getXml() {
        return getXml(false);
    }

    public FieldTypeUseDocument getExtendedXml() {
        return getXml(true);
    }

    private FieldTypeUseDocument getXml(boolean extended) {
        FieldTypeUseDocument fieldTypeUseDoc = FieldTypeUseDocument.Factory.newInstance();
        FieldTypeUseDocument.FieldTypeUse fieldTypeUseXml = fieldTypeUseDoc.addNewFieldTypeUse();
        fieldTypeUseXml.setFieldTypeId(getFieldType().getId());
        fieldTypeUseXml.setRequired(isRequired());
        fieldTypeUseXml.setEditable(isEditable());
        if (extended)
            fieldTypeUseXml.setFieldType(getFieldType().getXml().getFieldType());
        return fieldTypeUseDoc;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof FieldTypeUseImpl))
            return false;

        FieldTypeUseImpl other = (FieldTypeUseImpl)obj;

        if (fieldType.getId() != other.fieldType.getId())
            return false;

        if (editable != other.editable)
            return false;

        return required == other.required;
    }
}
