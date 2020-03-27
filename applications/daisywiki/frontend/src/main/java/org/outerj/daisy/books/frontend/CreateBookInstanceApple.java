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
package org.outerj.daisy.books.frontend;

import java.io.InputStream;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.FormManager;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Group;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Union;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.outerj.daisy.books.publisher.BookPublisher;
import org.outerj.daisy.books.publisher.PublicationSpec;
import org.outerj.daisy.books.publisher.PublicationSpecBuilder;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;
import org.outerj.daisy.books.store.BookAcl;
import org.outerj.daisy.books.store.BookAclActionType;
import org.outerj.daisy.books.store.BookAclBuilder;
import org.outerj.daisy.books.store.BookAclSubjectType;
import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.BookStoreUtil;
import org.outerj.daisy.books.store.impl.AclResult;
import org.outerj.daisy.books.store.impl.BookAclEvaluator;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;

public class CreateBookInstanceApple extends AbstractDaisyApple implements Serviceable, Disposable {
    private boolean init = false;
    private boolean paramsFormOk = false;
    private boolean publicationsFormOk = false;
    private Form paramsForm;
    private Form publicationsForm;
    private Form aclForm;
    private ServiceManager serviceManager;
    private Repository repository;
    private BookPublisher bookPublisher;
    private Locale locale;
    private VariantKey bookDefinition;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
        this.bookPublisher = (BookPublisher)serviceManager.lookup(BookPublisher.ROLE);
    }

    public void dispose() {
        serviceManager.release(bookPublisher);
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        if (!init) {
            repository = frontEndContext.getRepository();
            if (repository.isInRole("guest") && repository.getActiveRoleIds().length == 1) {
                throw new Exception("Users in the guest role are not allowed to publish books.");
            }

            String bookDefinitionDocumentId = repository.normalizeDocumentId(RequestUtil.getStringParameter(request, "bookDefinitionDocumentId"));
            long bookDefinitionBranchId = RequestUtil.getLongParameter(request, "bookDefinitionBranchId");
            long bookDefinitionLanguageId = RequestUtil.getLongParameter(request, "bookDefinitionLanguageId");
            bookDefinition = new VariantKey(bookDefinitionDocumentId, bookDefinitionBranchId, bookDefinitionLanguageId);

            //
            // Prepare book instance params form
            //

            Document bookDefDoc = repository.getDocument(bookDefinitionDocumentId, bookDefinitionBranchId, bookDefinitionLanguageId, false);
            Version bookDefDocVersion = bookDefDoc.getLiveVersion();
            if (bookDefDocVersion == null)
                throw new Exception("Book definition does not have a live version.");
            String bookDefName = bookDefDocVersion.getDocumentName();
            String bookDefBranchName = repository.getVariantManager().getBranch(bookDefinitionBranchId, false).getName();
            String bookDefLanguageName = repository.getVariantManager().getLanguage(bookDefinitionLanguageId, false).getName();

            paramsForm = FormHelper.createForm(serviceManager, "resources/form/bookinstanceparams_definition.xml");
            paramsForm.getChild("bookDefinitionName").setValue(bookDefName);
            paramsForm.getChild("bookDefinitionId").setValue(bookDefinitionDocumentId);
            paramsForm.getChild("bookDefinitionBranchName").setValue(bookDefBranchName);
            paramsForm.getChild("bookDefinitionLanguageName").setValue(bookDefLanguageName);

            locale = frontEndContext.getLocale();
            // set default book instance name and label
            SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
            dateFormat.applyPattern("yyyyMMdd-HHmmss");
            String date = dateFormat.format(new Date());
            String defaultBookInstanceName = bookDefName + "--" + date;
            defaultBookInstanceName = defaultBookInstanceName.toLowerCase();
            defaultBookInstanceName = BookStoreUtil.fixIllegalFileNameCharacters(defaultBookInstanceName);
            String defaultBookInstanceLabel = bookDefName + " -- " + date;
            paramsForm.getChild("bookInstanceName").setValue(defaultBookInstanceName);
            paramsForm.getChild("bookInstanceName").addValidator(new BookIntanceNameValidator());
            paramsForm.getChild("bookInstanceLabel").setValue(defaultBookInstanceLabel);

            Field dataBranchIdField = (Field)paramsForm.getChild("dataBranchId");
            dataBranchIdField.setValue(new Long(bookDefinitionBranchId));
            dataBranchIdField.setSelectionList(repository.getVariantManager().getAllBranches(false).getArray(), "id", "name");

            Field dataLanguageIdField = (Field)paramsForm.getChild("dataLanguageId");
            dataLanguageIdField.setValue(new Long(bookDefinitionLanguageId));
            dataLanguageIdField.setSelectionList(repository.getVariantManager().getAllLanguages(false).getArray(), "id", "name");

            paramsForm.getChild("dataVersion").setValue("live");
            Map bookMetadata = getBookMetaData(bookDefDocVersion);
            String locale = bookMetadata.containsKey("locale") ? (String)bookMetadata.get("locale") : "en-US";
            paramsForm.getChild("locale").setValue(locale);

            //
            // Prepare publication types form
            //
            publicationsForm = FormHelper.createForm(serviceManager, "resources/form/selectpublicationtypes_definition.xml"); 
            publicationsForm.setAttribute("publications-required", Boolean.TRUE); 
            publicationsForm.getChild("editmode").setValue("gui"); 
            publicationsForm.getChild("editmode").setState(WidgetState.INVISIBLE); 
            PublicationTypesFormHelper.initPublicationsForm(publicationsForm, serviceManager); 
            if (bookDefDoc.hasPart("BookPublicationsDefault")) { 
                PublicationSpec[] specs = PublicationSpecBuilder.build(bookDefDoc.getPart("BookPublicationsDefault").getDataStream()); 
                PublicationTypesFormHelper.loadPublicationSpecs(publicationsForm, specs, serviceManager); 
            } 

            //
            // Prepare the ACL form
            //
            aclForm = FormHelper.createForm(serviceManager, "resources/form/bookacl_definition.xml");
            aclForm.getChild("editmode").setValue("gui");
            aclForm.getChild("editmode").setState(WidgetState.INVISIBLE);
            initAclForm(bookDefDocVersion);
            BookAclEditorApple.annotateAclSubjectValues(aclForm, repository);

            //
            // Finish init
            //
            init = true;
            appleResponse.redirectTo(EncodingUtil.encodePath(getParamsPath()));
            return;
        }

        String resource = appleRequest.getSitemapParameter("resource");
        if (resource == null) {
            throw new Exception("Missing 'resource' sitemap parameter.");
        } else if (resource.equals("params")) {
            if (request.getMethod().equals("GET")) {
                showParamsForm(frontEndContext, appleResponse);
            } else if (request.getMethod().equals("POST")) {
                boolean finished = paramsForm.process(new FormContext(request, locale));
                if (finished) {
                    paramsFormOk = true;
                    appleResponse.redirectTo(EncodingUtil.encodePath(getPublicationTypesPath()));
                } else {
                    paramsFormOk = false;
                    showParamsForm(frontEndContext, appleResponse);
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (resource.equals("publicationtypes")) {
            if (!paramsFormOk) {
                appleResponse.redirectTo(EncodingUtil.encodePath(getParamsPath()));
            } else if (request.getMethod().equals("GET")) {
                showPublicationTypesForm(frontEndContext, appleResponse);
            } else if (request.getMethod().equals("POST")) {
                boolean finished = publicationsForm.process(new FormContext(request, locale));
                if (publicationsForm.getSubmitWidget() == publicationsForm.getChild("goBack")) {
                    publicationsFormOk = finished;
                    appleResponse.redirectTo(EncodingUtil.encodePath(getParamsPath()));
                } else if (finished) {
                    publicationsFormOk = true;
                    appleResponse.redirectTo(EncodingUtil.encodePath(getAclPath()));
                } else {
                    publicationsFormOk = false;
                    showPublicationTypesForm(frontEndContext, appleResponse);
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else if (resource.equals("acl")) {
            if (!paramsFormOk) {
                appleResponse.redirectTo(getParamsPath());
            } else if (!publicationsFormOk) {
                appleResponse.redirectTo(getPublicationTypesPath());
            } else if (request.getMethod().equals("GET")) {
                showAclForm(frontEndContext, appleResponse);
            } else if (request.getMethod().equals("POST")) {
                boolean finished = aclForm.process(new FormContext(request, locale));
                if (aclForm.getSubmitWidget() == aclForm.getChild("goBack")) {
                    appleResponse.redirectTo(getPublicationTypesPath());
                } else if (finished) {
                    String[] taskIdAndInstanceName = publishBook(frontEndContext);
                    Map<String, Object> viewData = new HashMap<String, Object>();
                    viewData.put("pageContext", frontEndContext.getPageContext());
                    viewData.put("taskId", taskIdAndInstanceName[0]);
                    viewData.put("bookInstanceName", URLEncoder.encode(taskIdAndInstanceName[1], "UTF-8"));
                    viewData.put("pipeConf", GenericPipeConfig.templatePipe("books/resources/xml/bookpublished.xml"));
                    appleResponse.sendPage(frontEndContext.getDaisyCocoonPath() + "/internal/genericPipe", viewData);
                } else {
                    showAclForm(frontEndContext, appleResponse);
                }
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else {
            throw new ResourceNotFoundException(resource);
        }
    }

    private String[] publishBook(FrontEndContext frontEndContext) throws Exception {
        long dataBranchId = ((Long)paramsForm.getChild("dataBranchId").getValue()).longValue();
        long dataLanguageId = ((Long)paramsForm.getChild("dataLanguageId").getValue()).longValue();
        VersionMode dataVersion = VersionMode.get((String)paramsForm.getChild("dataVersion").getValue());
        Locale locale = LocaleHelper.parseLocale((String)paramsForm.getChild("locale").getValue());
        String bookInstanceName = (String)paramsForm.getChild("bookInstanceName").getValue();
        String bookInstanceLabel = (String)paramsForm.getChild("bookInstanceLabel").getValue();
        String daisyCocoonPath = frontEndContext.getDaisyCocoonPath();
        String daisyContextPath = frontEndContext.getDaisyContextPath();

        Repeater publicationsRepeater = (Repeater)publicationsForm.lookupWidget("editors/gui/publications");
        PublicationSpec[] specs = new PublicationSpec[publicationsRepeater.getSize()];
        for (int i = 0; i < publicationsRepeater.getSize(); i++) {
            Repeater.RepeaterRow row = publicationsRepeater.getRow(i);
            String publicationTypeName = (String)row.getChild("typeName").getValue();
            String outputName = (String)row.getChild("outputName").getValue();
            String outputLabel = (String)row.getChild("outputLabel").getValue();
            Map<String, String> properties = getProperties((Repeater)row.getChild("properties"));
            specs[i] = new PublicationSpec(publicationTypeName, outputName, outputLabel, properties);
        }

        BookAcl acl = BookAclEditorApple.getBookAcl(aclForm);

        return bookPublisher.publishBook(repository, bookDefinition, dataBranchId, dataLanguageId, dataVersion, locale,
                bookInstanceName, bookInstanceLabel, daisyCocoonPath, daisyContextPath, specs, acl);
    }

    private Map<String, String> getProperties(Repeater propertiesRepeater) {
        Map<String, String> properties = new LinkedHashMap<String, String>();
        for (int i = 0; i < propertiesRepeater.getSize(); i++) {
            Repeater.RepeaterRow row = propertiesRepeater.getRow(i);
            
            String name = (String)row.getChild("name").getValue(); 
            if (name != null)
                name = name.trim();
            
            String value = (String)row.getChild("value").getValue(); 
            
            if (name != null && name.length() > 0) {
                properties.put(name, value == null ? "": value);
            }
        }
        return properties;
    }

    private void showParamsForm(FrontEndContext frontEndContext, AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("locale", locale);
        viewData.put("CocoonFormsInstance", paramsForm);
        viewData.put("pageContext", frontEndContext.getPageContext());
        appleResponse.sendPage("Form-bookinstanceparams-Pipe", viewData);
    }

    
    private void showPublicationTypesForm(FrontEndContext frontEndContext, AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("locale", locale);
        viewData.put("CocoonFormsInstance", publicationsForm);
        viewData.put("pageContext", frontEndContext.getPageContext());
        
        appleResponse.sendPage("Form-selectpublicationtypes-Pipe", viewData);
    }

    private void showAclForm(FrontEndContext frontEndContext, AppleResponse appleResponse) throws Exception {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("inBookPublishWizard", Boolean.TRUE);
        viewData.put("locale", locale);
        viewData.put("CocoonFormsInstance", aclForm);
        viewData.put("pageContext", frontEndContext.getPageContext());
        appleResponse.sendPage("Form-bookacl-Pipe", viewData);
    }

    private String getParamsPath() {
        return getMountPoint() + "/books/createBookInstance/" + getContinuationId() + "/params";
    }

    private String getPublicationTypesPath() {
        return getMountPoint() + "/books/createBookInstance/" + getContinuationId() + "/publicationtypes";
    }

    private String getAclPath() {
        return getMountPoint() + "/books/createBookInstance/" + getContinuationId() + "/acl";
    }


    private void initAclForm(Version bookDefDoc) throws Exception {
        BookAclEditorApple.initForm(aclForm, repository);

        Repeater aclEntriesRepeater = (Repeater)aclForm.lookupWidget("editors/gui/entries");

        if (bookDefDoc.hasPart("BookAclDefault")) {
            BookAcl bookAcl = BookAclBuilder.build(bookDefDoc.getPart("BookAclDefault").getDataStream());
            BookAclEditorApple.load(aclForm, bookAcl);

            // check the default book ACL allows manage permission for the current user, otherwise
            // add a new rule to give that permission
            AclResult result = BookAclEvaluator.evaluate(bookAcl, repository.getUserId(), repository.getActiveRoleIds());
            if (!result.canManage()) {
                Repeater.RepeaterRow row = aclEntriesRepeater.addRow();
                row.getChild("subjectType").setValue(BookAclSubjectType.USER);
                row.getChild("subjectValue").setValue(new Long(repository.getUserId()));
                row.getChild("readPerm").setValue(BookAclActionType.GRANT);
                row.getChild("managePerm").setValue(BookAclActionType.GRANT);
            }
        } else {
            // No default ACL specified in book definition, make one ourselves
            Repeater.RepeaterRow row = aclEntriesRepeater.addRow();
            row.getChild("subjectType").setValue(BookAclSubjectType.USER);
            row.getChild("subjectValue").setValue(new Long(repository.getUserId()));
            row.getChild("readPerm").setValue(BookAclActionType.GRANT);
            row.getChild("managePerm").setValue(BookAclActionType.GRANT);
        }
    }

    private Map getBookMetaData(Version bookDefDocVersion) throws Exception {
        if (bookDefDocVersion.hasPart("BookMetadata")) {
            InputStream is = null;
            try {
                is = bookDefDocVersion.getPart("BookMetadata").getDataStream();
                return XMLPropertiesHelper.load(is, "metadata");
            } finally {
                if (is != null)
                    is.close();
            }
        } else {
            return new HashMap();
        }
    }

    private class BookIntanceNameValidator implements WidgetValidator {
        public boolean validate(Widget widget) {
            String name = (String)widget.getValue();
            BookStore bookStore = (BookStore)repository.getExtension("BookStore");
            if (bookStore.existsBookInstance(name)) {
                ((ValidationErrorAware)widget).setValidationError(new ValidationError("bookip.duplicate-instance-name", true));
                return false;
            }
            return true;
        }
    }
}
