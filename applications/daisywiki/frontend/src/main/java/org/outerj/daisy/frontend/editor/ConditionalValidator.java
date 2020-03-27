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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.validation.WidgetValidator;

public class ConditionalValidator implements WidgetValidator {
    
    private ValidationCondition condition;
    private WidgetValidator delegate;
    
    public ConditionalValidator(ValidationCondition condition, WidgetValidator delegate) {
        if (condition == null)
            throw new NullPointerException("condition should not be null");
        if (delegate == null)
            throw new NullPointerException("delegate should not be null");
        
        this.condition = condition;
        this.delegate = delegate;
    }

    public boolean validate(Widget widget) {
        if  (!condition.checkCondition()) {
            return true;
        }
        return delegate.validate(widget);
    }
    
}
