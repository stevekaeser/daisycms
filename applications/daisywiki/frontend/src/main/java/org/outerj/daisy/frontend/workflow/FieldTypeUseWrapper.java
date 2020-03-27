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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerx.daisy.x10.FieldTypeUseDocument;

public class FieldTypeUseWrapper implements FieldTypeUse {
    
    private FieldTypeUse delegate;
    private boolean required;
    private boolean editable;
    
    public FieldTypeUseWrapper(FieldTypeUse fieldTypeUse, boolean required, boolean editable) {
        this.delegate = fieldTypeUse;
        this.required = required;
        this.editable = editable;
    }

    public FieldTypeUseDocument getExtendedXml() {
        FieldTypeUseDocument result = delegate.getExtendedXml();
        result.getFieldTypeUse().setRequired(required);
        result.getFieldTypeUse().setEditable(editable);
        return result;
    }

    public FieldType getFieldType() {
        return delegate.getFieldType();
    }

    public FieldTypeUseDocument getXml() {
        FieldTypeUseDocument result = delegate.getXml();
        result.getFieldTypeUse().setRequired(required);
        result.getFieldTypeUse().setEditable(editable);
        return result;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isRequired() {
        return required;
    }

    public void setEditable(boolean editable) {
        throw new UnsupportedOperationException("This is a read-only FieldTypeUse representation");
    }

    public void setRequired(boolean required) {
        throw new UnsupportedOperationException("This is a read-only FieldTypeUse representation");
    }

}
