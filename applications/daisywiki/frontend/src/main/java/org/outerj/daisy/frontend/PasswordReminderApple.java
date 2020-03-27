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
package org.outerj.daisy.frontend;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.components.userregistrar.UserRegistrar;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class PasswordReminderApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Form form = FormHelper.createForm(serviceManager, "resources/form/passwordreminder_definition.xml");
        Locale locale = frontEndContext.getLocale();

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);

        if (request.getParameter("confirmKey") != null) {
            String confirmKey = RequestUtil.getStringParameter(request, "confirmKey");
            long userId = RequestUtil.getLongParameter(request, "userId");
            UserRegistrar userRegistrar = (UserRegistrar)serviceManager.lookup(UserRegistrar.ROLE);
            String newPassword = userRegistrar.assignNewPassword(userId, confirmKey);

            viewData.put("newPassword", newPassword);
            viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/newpassword.xml"));
            appleResponse.sendPage("internal/genericPipe", viewData);
        } else if (request.getMethod().equals("GET")) {
            viewData.put("CocoonFormsInstance", form);
            viewData.put("submitPath", getMountPoint() + "/passwordReminder");
            appleResponse.sendPage("Form-passwordreminder-Pipe", viewData);
        } else if (request.getMethod().equals("POST")) {
            // handle a form submit
            boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

            if (!endProcessing) {
                viewData.put("CocoonFormsInstance", form);
                viewData.put("submitPath", getMountPoint() + "/passwordReminder");
                appleResponse.sendPage("Form-passwordreminder-Pipe", viewData);
            } else {
                String login = (String)form.getChild("login").getValue();
                UserRegistrar userRegistrar = (UserRegistrar)serviceManager.lookup(UserRegistrar.ROLE);
                String server = RequestUtil.getServer(request);
                userRegistrar.sendPasswordReminder(login, server, getMountPoint(), locale);
                viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/pwdremindersuccess.xml"));
                appleResponse.sendPage("internal/genericPipe", viewData);
            }
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

}
