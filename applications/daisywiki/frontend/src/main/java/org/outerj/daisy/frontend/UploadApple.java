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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.event.ValueChangedEvent;
import org.apache.cocoon.forms.event.ValueChangedListener;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.MultiValueField;
import org.apache.cocoon.forms.formmodel.Upload;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.editor.UploadPartDataSource;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.PartDataSource;
import org.outerj.daisy.repository.Repository;

/**
 * Apple used when uploading a new attachment or image.
 */
public class UploadApple extends AbstractDaisyApple implements Serviceable {
    private ServiceManager serviceManager;
    private Form form;
    private boolean init = false;
    private Map<String, Object> viewDataTemplate;
    private Locale locale;
    private SiteConf siteConf;
    private Repository repository;
    private String documentTypeName;
    private String partTypeName;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            documentTypeName = RequestUtil.getStringParameter(request, "documentType");
            partTypeName = RequestUtil.getStringParameter(request, "partType");

            form = FormHelper.createForm(serviceManager, "resources/form/upload_definition.xml");

            locale = frontEndContext.getLocale();
            siteConf = frontEndContext.getSiteConf();
            repository = frontEndContext.getRepository();
            long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);

            MultiValueField collectionsField = (MultiValueField)form.getChild("collections");
            collectionsField.setSelectionList(repository.getCollectionManager().getCollections(false).getArray(), "id", "name");
            collectionsField.setValue(new Long[] { new Long(siteConf.getCollectionId()) });

            form.getChild("branchId").setValue(new Long(branchId));
            form.getChild("languageId").setValue(new Long(languageId));

            Upload upload = (Upload)form.getChild("file");
            upload.addValueChangedListener(new ValueChangedListener() {
                public void valueChanged(ValueChangedEvent valueChangedEvent) {
                    org.apache.cocoon.servlet.multipart.Part docUploadPart = (org.apache.cocoon.servlet.multipart.Part)valueChangedEvent.getNewValue();
                    if (docUploadPart != null) {
                        form.getChild("mimetype").setValue(docUploadPart.getMimeType());
                        String fileName = RequestUtil.removePathFromUploadFileName(docUploadPart.getUploadName());
                        form.getChild("filename").setValue(fileName);
                        Field docName = (Field)form.getChild("name");
                        if (docName.getValue() == null) {
                            int pos = fileName.lastIndexOf('.');
                            if (pos != -1)
                                fileName = fileName.substring(0, pos);
                            docName.setValue(fileName);
                        }
                    }
                }
            });

            viewDataTemplate = new HashMap<String, Object>();
            viewDataTemplate.put("CocoonFormsInstance", form);
            viewDataTemplate.put("locale", locale);
            viewDataTemplate.put("submitPath", getPath());
            viewDataTemplate.put("mountPoint", getMountPoint());
            viewDataTemplate.put("branchesArray", repository.getVariantManager().getAllBranches(false).getArray());
            viewDataTemplate.put("languagesArray", repository.getVariantManager().getAllLanguages(false).getArray());

            init = true;
            appleResponse.redirectTo(EncodingUtil.encodePath(getPath()));
        } else {
            String method = request.getMethod();
            if (method.equals("GET")) {
                Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
                viewData.put("formTemplate", "resources/form/upload_template.xml");
                appleResponse.sendPage("GenericFormPipe", viewData);
            } else if (method.equals("POST")) {
                boolean endProcessing = form.process(new FormContext(request, locale));
                if (endProcessing) {
                    String name = (String)form.getChild("name").getValue();
                    long branchId = ((Long)form.getChild("branchId").getValue()).longValue();
                    long languageId = ((Long)form.getChild("languageId").getValue()).longValue();
                    String mimeType = (String)form.getChild("mimetype").getValue();
                    String fileName = (String)form.getChild("filename").getValue();

                    long uploadDocTypeId = repository.getRepositorySchema().getDocumentTypeByName(documentTypeName, false).getId();
                    Document document = repository.createDocument(name, uploadDocTypeId, branchId, languageId);
                    document.setPart(partTypeName, mimeType, getUploadData());
                    document.setPartFileName(partTypeName, fileName);

                    // save the collections
                    MultiValueField collectionsField = (MultiValueField)form.getChild("collections");
                    Object[] collections = (Object[])collectionsField.getValue();
                    CollectionManager collectionManager = repository.getCollectionManager();
                    document.clearCollections();
                    for (Object collection : collections) {
                        document.addToCollection(collectionManager.getCollection(((Long)collection).longValue(), false));
                    }
                    document.save();

                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("mountPoint", getMountPoint());
                    viewData.put("uploadDocId", String.valueOf(document.getId()));
                    viewData.put("uploadName", document.getName());
                    viewData.put("branch", repository.getVariantManager().getBranch(branchId, false).getName());
                    viewData.put("language", repository.getVariantManager().getLanguage(languageId, false).getName());
                    viewData.put("branchId", String.valueOf(branchId));
                    viewData.put("languageId", String.valueOf(languageId));
                    viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
                    viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/upload_finish.xml"));
                    appleResponse.sendPage("internal/genericPipe", viewData);
                } else {
                    Map<String, Object> viewData = new HashMap<String, Object>(viewDataTemplate);
                    viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
                    viewData.put("formTemplate", "resources/form/upload_template.xml");
                    appleResponse.sendPage("GenericFormPipe", viewData);
                }
            } else {
                throw new Exception("Unexpected HTML method: " + method);
            }
        }
    }

    private PartDataSource getUploadData() throws Exception {
        org.apache.cocoon.servlet.multipart.Part docUploadPart = (org.apache.cocoon.servlet.multipart.Part)form.getChild("file").getValue();
        return new UploadPartDataSource(docUploadPart);
    }

    private String getPath() {
        return getMountPoint() + "/" + siteConf.getName() + "/editing/upload/" + getContinuationId();
    }
}

