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
import org.outerj.daisy.frontend.components.userregistrar.UserRegistrar;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class UserRegistrationApple  extends AbstractDaisyApple implements Serviceable, Disposable {
    private ServiceManager serviceManager;
    private boolean init = false;
    private Locale locale;
    private Form form;
    private Map<String, Object> viewDataTemplate;
    private String returnTo;
    private UserRegistrar userRegistrar;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        userRegistrar = (UserRegistrar)serviceManager.lookup(UserRegistrar.ROLE);
    }

    public void dispose() {
        if (userRegistrar != null)
            serviceManager.release(userRegistrar);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            returnTo = request.getParameter("returnTo");
            if (returnTo == null || returnTo.equals(""))
                returnTo = getMountPoint() + "/";
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(serviceManager, "resources/form/userregistration_definition.xml");

            String path = getMountPoint() + "/registration/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", path);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("returnTo", returnTo);
            viewDataTemplate.put("CocoonFormsInstance", form);

            init = true;

            appleResponse.redirectTo(path);
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                viewData.put("pageContext", frontEndContext.getPageContext());
                appleResponse.sendPage("Form-userregistration-Pipe", viewData);
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    appleResponse.sendPage("Form-userregistration-Pipe", getViewData(frontEndContext));
                } else {
                    String login = (String)form.getChild("login").getValue();
                    String email = (String)form.getChild("email").getValue();
                    String firstName = (String)form.getChild("firstName").getValue();
                    String lastName = (String)form.getChild("lastName").getValue();
                    String password = (String)form.getChild("password").getValue();

                    String server = RequestUtil.getServer(request);

                    userRegistrar.registerNewUser(login, password, email, firstName, lastName, server, getMountPoint(), locale);

                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("pageContext", frontEndContext.getPageContext());
                    viewData.put("locale", locale);
                    viewData.put("returnTo", returnTo);
                    viewData.put("email", email);

                    viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/registrationsuccess.xml"));
                    appleResponse.sendPage("internal/genericPipe", viewData);
                }
            } else {
                throw new Exception("Unspported HTTP method: " + methodName);
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }
}
