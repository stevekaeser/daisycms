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
package org.outerj.daisy.frontend.admin;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.WfProcessDefinition;
import org.outerj.daisy.repository.Repository;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Upload;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.servlet.multipart.Part;

import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;

public class WfProcessDefinitionUploadApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private Form form;
    private Repository repository;
    private boolean init = false;
    private Locale locale;
    private Map<String, Object> viewDataTemplate;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            locale = frontEndContext.getLocale();
            form = FormHelper.createForm(serviceManager, "resources/form/wfprocessdef_definition.xml");
            repository = frontEndContext.getRepository();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("mountPoint", getMountPoint());

            init = true;

            String path = getMountPoint() + "/admin/wfProcessDefinition/upload/" + getContinuationId();
            appleResponse.redirectTo(path);
        } else {
            String methodName = request.getMethod();
            if (methodName.equals("GET")) {
                // display the form
                appleResponse.sendPage("Form-wfprocessdef-Pipe", getViewData(frontEndContext));
            } else if (methodName.equals("POST")) {
                // handle a form submit
                boolean endProcessing = form.process(new FormContext(request, locale));

                if (!endProcessing) {
                    appleResponse.sendPage("Form-wfprocessdef-Pipe", getViewData(frontEndContext));
                } else {
                    WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");

                    Upload upload = (Upload)form.getChild("processDefinition");
                    Part part = (Part)upload.getValue();

                    String mimeType;
                    String type = (String)form.getChild("processType").getValue();
                    if (type == null || type.equals("auto")) {
                        mimeType = part.getMimeType();
                    } else if (type.equals("xml")) {
                        mimeType = "text/xml";
                    } else if (type.equals("zip")) {
                        mimeType = "application/zip";
                    } else {
                        throw new RuntimeException("Unexpected process type: " + type);
                    }

                    WfProcessDefinition processDefinition;
                    InputStream is = null;
                    try {
                        is = part.getInputStream();
                        processDefinition = workflowManager.deployProcessDefinition(is, mimeType, locale);
                    } finally {
                        if (is != null)
                            try { is.close(); } catch (Exception e) { /* ignore */ }
                    }

                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("pageXml", new XmlObjectXMLizable(processDefinition.getXml()));
                    viewData.put("mountPoint", getMountPoint());
                    viewData.put("pageContext", frontEndContext.getPageContext());

                    appleResponse.sendPage("ProcessDefinitionConfirmPage", viewData);
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
