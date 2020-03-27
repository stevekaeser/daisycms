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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.outerj.daisy.repository.schema.FieldTypeUse;

import java.util.Iterator;

/**
 * Checks that required fields have a value, if validation has not been disabled.
 */
public class CheckFieldsFormValidator implements WidgetValidator {

    public boolean validate(Widget widget) {
        boolean success = true;

        Iterator childWidgetsIt = ((Form)widget).getChildren();
        while (childWidgetsIt.hasNext()) {
            Widget childWidget = (Widget)childWidgetsIt.next();
            if (childWidget.getId().startsWith("field_")) {
                FieldEditor fieldEditor = (FieldEditor)childWidget.getAttribute("fieldEditor");
                FieldTypeUse fieldTypeUse = fieldEditor.getFieldTypeUse();
                if (fieldTypeUse.isRequired() && !fieldEditor.hasValue(childWidget)) {
                    fieldEditor.setValidationError(new ValidationError("editdoc.field-required", true));
                    success = false;
                }
            }
        }

        return success;
    }
}
