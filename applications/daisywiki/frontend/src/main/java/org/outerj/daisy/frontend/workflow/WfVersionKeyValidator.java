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

import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.components.ContextHelper;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.Serviceable;
import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.FrontEndContext;

import java.util.regex.Matcher;

public class WfVersionKeyValidator  implements WidgetValidator, Contextualizable, Serviceable {
    private Context context;
    private ServiceManager serviceManager;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public boolean validate(Widget widget) {
        Field field = (Field)widget;
        String link = (String)field.getValue();
        if (link == null)
            return true;

        Repository repository;
        try {
            repository = FrontEndContext.get(ContextHelper.getRequest(context)).getRepository();
        } catch (Exception e) {
            throw new RuntimeException("Error getting access to the repository in " + this.getClass().getName(), e);
        }

        Matcher matcher = WfVersionKeyUtil.VERSIONKEY_PATTERN.matcher(link);
        if (matcher.matches()) {
            String documentId = matcher.group(1);
            String branch = matcher.group(2);
            String language = matcher.group(3);
            String version = matcher.group(4);

            Matcher docIdMatcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(documentId);
            if (!docIdMatcher.matches()) {
                field.setValidationError(new ValidationError("Invalid document ID in link: " + documentId));
                return false;
            }

            VariantManager variantManager = repository.getVariantManager();
            if (branch != null) {
                try {
                    variantManager.getBranch(branch, false).getId();
                } catch (RepositoryException e) {
                    field.setValidationError(new ValidationError("Invalid branch in link: " + branch));
                    return false;
                }
            }

            if (language != null) {
                try {
                    variantManager.getLanguage(language, false).getId();
                } catch (RepositoryException e) {
                    field.setValidationError(new ValidationError("Invalid language in link: " + language));
                    return false;
                }
            }

            if (version != null) {
                if (!version.equalsIgnoreCase("last") && !version.equalsIgnoreCase("live")) {
                    try {
                        Long.parseLong(version);
                    } catch (NumberFormatException e) {
                        field.setValidationError(new ValidationError("Invalid version in link: " + version));
                        return false;
                    }
                }
            }
        } else {
            field.setValidationError(new ValidationError("Link is not in correct form, it should follow this form: daisy:<documentId>@<branch>:<language>:<version>, the parts after documentId are optional (though recommended)."));
            return false;
        }

        return true;
    }
}
