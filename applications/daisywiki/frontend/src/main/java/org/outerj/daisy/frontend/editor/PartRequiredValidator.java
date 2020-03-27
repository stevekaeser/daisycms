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
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Field;
import org.outerj.daisy.frontend.HtmlHelper;

/**
 * Validates that the part has a value, if required & if validation enabled.
 */
class PartRequiredValidator implements WidgetValidator {
    private boolean isRequired;
    private boolean htmlMode;

    public PartRequiredValidator(boolean isRequired, boolean htmlMode) {
        this.isRequired = isRequired;
        this.htmlMode = htmlMode;
    }

    public boolean validate(Widget widget) {
        boolean success = true;
        if (isRequired) {
            if (htmlMode) {
                if (HtmlHelper.isEmpty((String)widget.getValue())) {
                    ((Field)widget).setValidationError(new ValidationError("editdoc.part-required", true));
                    success = false;
                }
            } else {
                if (widget.validate() && widget.getValue() == null) {
                    ((Field)widget).setValidationError(new ValidationError("editdoc.part-required", true));
                    success = false;
                }
            }
        }
        return success;
    }
}
