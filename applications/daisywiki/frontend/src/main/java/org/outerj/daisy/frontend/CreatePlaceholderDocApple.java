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
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.JavascriptXslUtil;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.ValueComparator;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.MultiValueField;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.datatype.SelectionList;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.util.StringMessage;
import org.apache.cocoon.forms.FormContext;
import org.apache.tools.ant.filters.EscapeUnicode;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class CreatePlaceholderDocApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();
        Form form = createForm(repository, siteConf);

        if (request.getMethod().equals("POST")) {
            boolean success = form.process(new FormContext(request, locale));
            if (success) {
                String name = (String)form.getChild("name").getValue();
                
                long documentTypeId = ((Long)form.getChild("documentType").getValue()).longValue();
                long branchId = ((Long)form.getChild("branchId").getValue()).longValue();
                long languageId = ((Long)form.getChild("languageId").getValue()).longValue();
                Document document = repository.createDocument(name, documentTypeId, branchId, languageId);

                Object[] collectionIds = (Object[])form.getChild("collections").getValue();
                CollectionManager collectionManager = repository.getCollectionManager();
                for (int i = 0; i < collectionIds.length; i++) {
                    long collectionId = ((Long)collectionIds[i]).longValue();
                    document.addToCollection(collectionManager.getCollection(collectionId, false));
                }

                DocumentType documentType = repository.getRepositorySchema().getDocumentTypeById(documentTypeId, false);
                PartTypeUse[] partTypeUses = documentType.getPartTypeUses();
                for (PartTypeUse partTypeUse : partTypeUses) {
                    if (partTypeUse.isRequired() && partTypeUse.getPartType().isDaisyHtml()) {
                        document.setPart(partTypeUse.getPartType().getName(), "text/xml", "<html><body><p>TODO</p></body></html>".getBytes("UTF-8"));
                    }
                }

                // do not validate, required parts and fields might not have a value
                document.save(false);

                Map<String, Object> viewData = new HashMap<String, Object>();
                viewData.put("documentId", String.valueOf(document.getId()));
                viewData.put("mountPoint", frontEndContext.getMountPoint());
                viewData.put("siteName", siteConf.getName());
                // needs to be escaped since some characters '"' can cause javascript breakage
                viewData.put("name",  JavascriptXslUtil.escape(name));
                viewData.put("branchId", String.valueOf(branchId));
                viewData.put("branch", repository.getVariantManager().getBranch(branchId, false).getName());
                viewData.put("languageId", String.valueOf(languageId));
                viewData.put("language", repository.getVariantManager().getLanguage(languageId, false).getName());
                viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
                viewData.put("pipeConf", GenericPipeConfig.templatePipe("resources/xml/placeholder_finish.xml"));
                appleResponse.sendPage("internal/genericPipe", viewData);
                return;
            }
        } else if (request.getMethod().equals("GET")) {
            long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
            long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);
            form.getChild("branchId").setValue(new Long(branchId));
            form.getChild("languageId").setValue(new Long(languageId));

            long documentTypeId;
            if (request.getParameter("documentType") == null) {
                documentTypeId = siteConf.getDefaultDocumentTypeId();
                if (documentTypeId == -1)
                    documentTypeId = repository.getRepositorySchema().getDocumentTypeByName("SimpleDocument", false).getId();
            } else {
                documentTypeId = RequestUtil.getLongParameter(request, "documentType");
            }
            form.getChild("documentType").setValue(new Long(documentTypeId));
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }

        updateDocumentTypes(form, repository, siteConf, locale);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("CocoonFormsInstance", form);
        viewData.put("locale", locale);
        viewData.put("mountPoint", frontEndContext.getMountPoint());
        viewData.put("siteName", siteConf.getName());
        viewData.put("branchesArray", repository.getVariantManager().getAllBranches(false).getArray());
        viewData.put("languagesArray", repository.getVariantManager().getAllLanguages(false).getArray());
        viewData.put("pageContext", frontEndContext.getPageContext("dialog"));
        viewData.put("formTemplate", "resources/form/placeholder_doc_template.xml");
        appleResponse.sendPage("GenericFormPipe", viewData);
    }

    private Form createForm(Repository repository, SiteConf siteConf) throws Exception {
        final Form form = FormHelper.createForm(serviceManager, "resources/form/placeholder_doc_definition.xml");

        MultiValueField collectionsField = (MultiValueField)form.getChild("collections");
        collectionsField.setSelectionList(repository.getCollectionManager().getCollections(false).getArray(), "id", "name");
        collectionsField.setValues(new Long[] {new Long(siteConf.getCollectionId())});

        return form;
    }

    private void updateDocumentTypes(Form form, Repository repository, SiteConf siteConf, Locale locale) {
        // The list of document types is dependent on the branch and language selected in the form.
        try {
            Field documentTypeField = (Field)form.getChild("documentType");
            long branchId = (Long)form.getChild("branchId").getValue();
            long languageId = (Long)form.getChild("languageId").getValue();
            DocumentType[] documentTypes = DocumentTypeSelectionApple.getFilteredDocumentTypes(repository, -1, true, siteConf, branchId, languageId);
            Arrays.sort(documentTypes, new DocumentTypeLabelComparator(true, locale));
            documentTypeField.setSelectionList(createDocumentTypeSelectionList(documentTypes, locale, documentTypeField.getDatatype()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private SelectionList createDocumentTypeSelectionList(DocumentType[] types, Locale locale, Datatype datatype) {
        StaticSelectionList selectionList = new StaticSelectionList(datatype);
        for (DocumentType type : types) {
            selectionList.addItem(new Long(type.getId()), new StringMessage(type.getLabel(locale)));
        }
        return selectionList;
    }
    
    private static class DocumentTypeLabelComparator implements Comparator<DocumentType> {
        private final ValueComparator<String> delegate;
        private final Locale locale;
        
        public DocumentTypeLabelComparator (boolean ascending, Locale locale) {
            delegate = new ValueComparator<String>(ascending, locale);
            this.locale = locale;
        }

        public int compare(DocumentType o1, DocumentType o2) {
            return this.delegate.compare(o1.getLabel(locale), o2.getLabel(locale));
        }
        
    }
}
