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
package org.outerj.daisy.frontend.util;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.FormManager;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.binding.BindingManager;

public class FormHelper {
    public static Form createForm(ServiceManager serviceManager, String formDefinitionFileName) throws Exception {
        FormManager formManager = null;

        try {
            formManager = (FormManager)serviceManager.lookup(FormManager.ROLE);
            Form form = formManager.createForm(formDefinitionFileName);
            return form;
        } finally {
            if (formManager != null)
                serviceManager.release(formManager);
        }
    }

    public static Binding createBinding(ServiceManager serviceManager, String bindingDefinitionFileName) throws Exception {
        BindingManager bindingManager = null;

        try {
            bindingManager = (BindingManager)serviceManager.lookup(BindingManager.ROLE);
            Binding binding = bindingManager.createBinding(bindingDefinitionFileName);
            return binding;
        } finally {
            if (bindingManager != null)
                serviceManager.release(bindingManager);
        }
    }
}
