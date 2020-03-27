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
package org.outerj.daisy.frontend.admin;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclSubjectType;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.binding.Binding;
import org.apache.cocoon.forms.FormContext;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

/**
 * Controller for the editing of an ACL.
 */
public class AclEditorApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private Form form;
    private Binding binding;
    private boolean init = false;
    private Map<String, Object> viewDataTemplate;
    private Locale locale;
    private Acl acl;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            Repository repository = frontEndContext.getRepository();
            AccessManager accessManager = repository.getAccessManager();

            form = FormHelper.createForm(serviceManager, "resources/form/acl_definition.xml");
            binding = FormHelper.createBinding(serviceManager, "resources/form/acl_binding.xml");
            locale = frontEndContext.getLocale();

            acl = accessManager.getStagingAcl();
            binding.loadFormFromModel(form, acl);
            annotateSubjectValues(form, repository);

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", getPath());
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("entityName", "acl");

            init = true;

            appleResponse.redirectTo(getPath());
        } else {
            String methodName = appleRequest.getCocoonRequest().getMethod();
            if (methodName.equals("GET")) {
                // display the form
                Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                viewData.put("pageContext", frontEndContext.getPageContext());
                appleResponse.sendPage("Form-acl-Pipe", viewData);
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                if (!endProcessing) {
                    Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                    viewData.put("pageContext", frontEndContext.getPageContext());
                    appleResponse.sendPage("Form-acl-Pipe", viewData);
                } else {
                    binding.saveFormToModel(form, acl);
                    acl.save();
                    appleResponse.redirectTo(getMountPoint() + "/admin");
                }
            } else {
                throw new HttpMethodNotAllowedException(methodName);
            }
        }
    }

    private String getPath() {
        return getMountPoint() + "/admin/acl/staging/edit/" + getContinuationId();
    }

    private void annotateSubjectValues(Form form, Repository repository) {
        UserManager userManager = repository.getUserManager();
        Repeater objectsRepeater = (Repeater)form.getChild("objects");
        for (int i = 0; i < objectsRepeater.getSize(); i++) {
            Repeater entriesRepeater = (Repeater)objectsRepeater.getRow(i).getChild("entries");
            for (int k = 0; k < entriesRepeater.getSize(); k++) {
                Repeater.RepeaterRow entry = entriesRepeater.getRow(k);
                AclSubjectType subjectType = (AclSubjectType)entry.getChild("subjectType").getValue();
                long subjectValue;
                subjectValue = ((Long)entry.getChild("subjectValue").getValue()).longValue();
                if (subjectType == AclSubjectType.ROLE) {
                    String roleName;
                    try {
                        roleName = userManager.getRole(subjectValue, false).getName();
                    } catch (Exception e) {
                        roleName = "(error)";
                    }
                    entry.getChild("subjectValueLabel").setValue(roleName);
                } else if (subjectType == AclSubjectType.USER) {
                    String userName;
                    try {
                        userName = userManager.getUser(subjectValue, false).getLogin();
                    } catch (Exception e) {
                        userName = "(error)";
                    }
                    entry.getChild("subjectValueLabel").setValue(userName);
                }
            }
        }
    }
}
