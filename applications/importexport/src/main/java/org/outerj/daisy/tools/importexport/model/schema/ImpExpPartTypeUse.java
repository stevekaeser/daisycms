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
package org.outerj.daisy.tools.importexport.model.schema;

public class ImpExpPartTypeUse {
    private String partTypeName;
    private boolean required;
    private boolean editable = true;

    public ImpExpPartTypeUse(String partTypeName, boolean required) {
        if (partTypeName == null || partTypeName.trim().length() == 0)
            throw new IllegalArgumentException("Null or empty argument: partTypeName");
        this.partTypeName = partTypeName;
        this.required = required;
    }

    public String getPartTypeName() {
        return partTypeName;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }
}
