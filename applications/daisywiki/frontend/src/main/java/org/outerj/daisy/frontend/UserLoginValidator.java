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
package org.outerj.daisy.frontend;

import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.components.ContextHelper;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.repository.Repository;

public class UserLoginValidator implements WidgetValidator, Contextualizable {
    private Context context;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public boolean validate(Widget widget) {
        String login = (String)widget.getValue();
        if (login == null)
            return true;

        boolean valid = true;
        try {
            FrontEndContext frontEndContext = FrontEndContext.get(ContextHelper.getRequest(context));
            Repository repository = frontEndContext.getRepository();
            repository.getUserManager().getPublicUserInfo(login);
        } catch (UserNotFoundException e) {
            valid = false;
            ((ValidationErrorAware)widget).setValidationError(new ValidationError("Non-existing user login: " + login, false));
        } catch (Throwable e) {
            throw new RuntimeException("Error trying to check existence of user \"" + login + "\".", e);
        }

        return valid;
    }
}