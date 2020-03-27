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

import org.outerj.daisy.frontend.util.*;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.ContainerWidget;
import org.apache.cocoon.forms.FormContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public class TestAclApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private AccessManager accessManager;
    private Form form;
    private boolean init = false;
    private Locale locale;
    private Map<String, Object> viewDataTemplate;
    private String aclName;
    private String path;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            aclName = appleRequest.getSitemapParameter("name");

            if (!"live".equals(aclName) && !"staging".equals(aclName))
                throw new ResourceNotFoundException("ACL does not exist: " + aclName);

            Repository repository = frontEndContext.getRepository();
            accessManager = repository.getAccessManager();

            form = FormHelper.createForm(serviceManager, "resources/form/testacl_definition.xml");
            locale = frontEndContext.getLocale();

            path = getMountPoint() + "/admin/testacl/" + aclName + "/" + getContinuationId();

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("submitPath", path);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("branchesArray", repository.getVariantManager().getAllBranches(false).getArray());
            viewDataTemplate.put("languagesArray", repository.getVariantManager().getAllLanguages(false).getArray());
            viewDataTemplate.put("documentTypesArray", repository.getRepositorySchema().getAllDocumentTypes(false).getArray());

            init = true;

            appleResponse.redirectTo(path);
        } else {
            String resource = appleRequest.getSitemapParameter("resource", null);

            if ("result".equals(resource)) {
                if (!form.isValid()) {
                    appleResponse.redirectTo(path);
                } else {
                    long userId = (Long)form.getChild("userId").getValue();
                    long roleId = (Long)form.getChild("roleId").getValue();

                    String testOn = (String)form.getChild("testOn").getValue();
                    AclResultInfo result;

                    if (testOn.equals("existing")) {
                        ContainerWidget docWidget = (ContainerWidget)form.lookupWidget("document/existing");
                        String documentId = (String)docWidget.getChild("documentId").getValue();
                        long branchId = (Long)docWidget.getChild("branchId").getValue();
                        long languageId = (Long)docWidget.getChild("languageId").getValue();

                        if (aclName.equals("live"))
                            result = accessManager.getAclInfoOnLive(userId, new long[] {roleId}, documentId, branchId, languageId);
                        else
                            result = accessManager.getAclInfoOnStaging(userId, new long[] {roleId}, documentId, branchId, languageId);
                    } else if (testOn.equals("conceptual")) {
                        ContainerWidget docWidget = (ContainerWidget)form.lookupWidget("document/conceptual");
                        long documentType = (Long)docWidget.getChild("documentType").getValue();
                        long branchId = (Long)docWidget.getChild("branchId").getValue();
                        long languageId = (Long)docWidget.getChild("languageId").getValue();

                        if (aclName.equals("live")) {
                            result = accessManager.getAclInfoOnLiveForConceptualDocument(userId, new long[] {roleId},
                                    documentType, branchId, languageId);
                        } else {
                            result = accessManager.getAclInfoOnStagingForConceptualDocument(userId, new long[] {roleId},
                                    documentType, branchId, languageId);
                        }
                    } else {
                        throw new RuntimeException("Unrecognized testOn case: " + testOn);
                    }

                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("pageXml", new XmlObjectXMLizable(result.getXml()));
                    viewData.put("pageContext", frontEndContext.getPageContext());

                    appleResponse.sendPage("TestAclResultPipe", viewData);
                }
            } else if (resource == null) {
                String methodName = appleRequest.getCocoonRequest().getMethod();
                if (methodName.equals("GET")) {
                    // display the form
                    appleResponse.sendPage("TestAclPipe", getViewData(frontEndContext));
                } else if (methodName.equals("POST")) {
                    // handle a form submit
                    boolean endProcessing = form.process(new FormContext(appleRequest.getCocoonRequest(), locale));

                    if (!endProcessing) {
                        appleResponse.sendPage("TestAclPipe", getViewData(frontEndContext));
                    } else {
                        appleResponse.redirectTo(path + "/result");
                    }
                }
            } else {
                throw new ResourceNotFoundException("Unexisting resource: " + resource);
            }
        }
    }

    private Map<String, Object> getViewData(FrontEndContext frontEndContext) {
        Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
        viewData.put("pageContext", frontEndContext.getPageContext());
        return viewData;
    }
}
