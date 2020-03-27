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
package org.outerj.daisy.frontend.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.FormManager;
import org.apache.cocoon.forms.formmodel.DataWidget;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.xml.EmbeddedXMLPipe;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.frontend.DocumentTypeSpecificStyler;
import org.outerj.daisy.frontend.PageContext;
import org.outerj.daisy.frontend.PreparedDocuments;
import org.outerj.daisy.frontend.PreparedDocumentsHandler;
import org.outerj.daisy.frontend.WikiPublisherHelper;
import org.outerj.daisy.frontend.WikiStylesheetProvider;
import org.outerj.daisy.frontend.util.EncodingUtil;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.ResponseUtil;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.publisher.docpreparation.FieldAnnotator;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentVariantNotFoundException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.commonimpl.acl.AclResultInfoImpl;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerx.daisy.x10.DocumentDocument;
import org.xml.sax.helpers.AttributesImpl;

public class InlineEditorApple extends DocumentEditorSupport implements LogEnabled {
    
    public static final String INLINEEDITOR_NAMESPACE = "http://outerx.org/daisy/1.0#inlineeditor";
    
    private Logger logger;
    private Form form;
    private boolean triedToSave = false;
    private DocumentEditorContext editorContext;
    private Map<String, Object> viewData;

    public void formCreated(Form form) {
        this.form = form;
    }

    protected void processRequest(AppleRequest appleRequest,
            AppleResponse appleResponse) throws Exception {
        super.processRequest(appleRequest, appleResponse);

        if (document == null) {
            if ("new".equals(lastPathPart)) {
                initialiseWithNewDocument(appleResponse);
                initEditorContext();
                viewData = getEditViewData();
            } else {
                initialiseWithExistingDocument(appleResponse);
                initEditorContext();
                viewData = getEditViewData();
                return;
            }
        }
        
        String resource = appleRequest.getSitemapParameter("resource");
        
        if (appleRequest.getCocoonRequest().getMethod().equals("GET") && resource == null) {
            appleResponse.redirectTo(EncodingUtil.encodePath(getPath() + "/"));
        } else if (editorContext != null && editorContext.handleCommonResources(appleRequest, appleResponse, document, resource, viewData)) {
            return;
        } else {
            if (form != null && appleRequest.getCocoonRequest().getMethod().equals("POST")) { // avoid processing the form for includePreview requests
                if  (processFormSubmit(null, appleResponse, resource)) {
                    return;
                }
            }

            PageContext pageContext = frontEndContext.getPageContext();
            
            WikiPublisherHelper helper = new WikiPublisherHelper(appleRequest.getCocoonRequest(), getContext(), serviceManager);
            
            SaxBuffer publisherResponse = null;
            if (document.isNew()) {
                // TODO: when starting from a template document we should use a publisher request for the styled document (and replace occurrences of its variantkey with "new@branch:language"
                SaxBuffer fakePublisherResponse = new SaxBuffer();
                publisherResponse = new SaxBuffer();

                String basePath = getMountPoint() + "/" + siteConf.getName() + "/";

                DocumentTypeSpecificStyler.StylesheetProvider stylesheetProvider = new WikiStylesheetProvider("html", serviceManager);
                DocumentTypeSpecificStyler documentTypeSpecificStyler = new DocumentTypeSpecificStyler("html", basePath,
                        frontEndContext.getDaisyCocoonPath(), stylesheetProvider, pageContext, repository, getContext(), serviceManager);
                PreparedDocumentsHandler pdh = new PreparedDocumentsHandler(publisherResponse, request, documentTypeSpecificStyler);
                
                fakePublisherResponse.startDocument();

                EmbeddedXMLPipe embedded = new EmbeddedXMLPipe(fakePublisherResponse);
                embedded.startPrefixMapping("p", "http://outerx.org/daisy/1.0#publisher");
                embedded.startElement("http://outerx.org/daisy/1.0#publisher", "publisherResponse", "p:publisherResponse", new AttributesImpl());


                AttributesImpl pDocumentAttr = new AttributesImpl();
                pDocumentAttr.addAttribute("", "documentId", "documentId", "CDATA", "new");
                pDocumentAttr.addAttribute("", "branchId", "branchId", "CDATA", Long.toString(document.getBranchId()));
                pDocumentAttr.addAttribute("", "branch", "branch", "CDATA", getBranch());
                pDocumentAttr.addAttribute("", "languageId", "languageId", "CDATA", Long.toString(document.getLanguageId()));
                pDocumentAttr.addAttribute("", "language", "language", "CDATA", getLanguage());
                embedded.startElement("http://outerx.org/daisy/1.0#publisher", "document", "p:document", pDocumentAttr);

                //p:subscriptionInfo
                //ns:comments
                //ns:availableVariants

                //p:preparedDocuments
                AttributesImpl preparedDocumentsAttr = new AttributesImpl();
                preparedDocumentsAttr.addAttribute("", "applyDocumentTypeStyling", "applyDocumentTypeStyling", "CDATA", "true");
                preparedDocumentsAttr.addAttribute("", "displayContext", "displayContext", "CDATA", "standalone");

                AttributesImpl preparedDocumentAttr = new AttributesImpl();
                preparedDocumentAttr.addAttribute("", "id", "id", "CDATA", "1");
                preparedDocumentAttr.addAttribute("", "documentId", "documentId", "CDATA", "new");
                preparedDocumentAttr.addAttribute("", "branchId", "branchId", "CDATA", Long.toString(document.getBranchId()));
                preparedDocumentAttr.addAttribute("", "languageId", "languageId", "CDATA", Long.toString(document.getLanguageId()));
                
                AttributesImpl pubRespAttr = new AttributesImpl(new AttributesImpl());
                pubRespAttr.addAttribute("", "styleHint", "styleHint", "CDATA", documentType.getName().concat(".xsl"));

                embedded.startElement("http://outerx.org/daisy/1.0#publisher", "preparedDocuments", "p:preparedDocuments", preparedDocumentsAttr);
                embedded.startElement("http://outerx.org/daisy/1.0#publisher", "preparedDocument", "p:preparedDocument", preparedDocumentAttr);
                embedded.startElement("http://outerx.org/daisy/1.0#publisher", "publisherResponse", "p:publisherResponse", pubRespAttr);
                
                final DocumentDocument documentXml = document.getXml();
                VersionMode versionMode = VersionMode.LAST;
                FieldAnnotator.annotateFields(documentXml.getDocument(), repository, locale, versionMode);
                documentXml.save(embedded, embedded);

                // ns:aclResult
                AclResultInfo aclResultInfo  = new AclResultInfoImpl(repository.getUserId(), repository.getActiveRoleIds(), "new", document.getBranchId(), document.getLanguageId());
                aclResultInfo.set(AclPermission.DELETE, AclActionType.GRANT, "owner", "owner");
                aclResultInfo.set(AclPermission.READ, AclActionType.GRANT, "owner", "owner");
                aclResultInfo.set(AclPermission.WRITE, AclActionType.GRANT, "owner", "owner");
                aclResultInfo.set(AclPermission.PUBLISH, AclActionType.DO_NOTHING, "FIXME: not implemented", "FIXME: not implemented");
                aclResultInfo.getXml().save(embedded, embedded);

                // ns:documentType
                documentType.getExtendedXml().save(embedded, embedded);

                embedded.endElement("http://outerx.org/daisy/1.0#publisher", "publisherResponse", "p:publisherResponse");
                embedded.endElement("http://outerx.org/daisy/1.0#publisher", "preparedDocument", "p:preparedDocument");
                embedded.endElement("http://outerx.org/daisy/1.0#publisher", "preparedDocuments", "p:preparedDocuments");
                
                //ns:version
                AttributesImpl versionAttr = new AttributesImpl();
                embedded.startElement("http://outerx.org/daisy/1.0", "version", "version", versionAttr);
                embedded.endElement("http://outerx.org/daisy/1.0", "version", "version");

                //ns:document
                documentXml.save(embedded, embedded);
                
                embedded.endElement("http://outerx.org/daisy/1.0#publisher", "document", "p:document");

                //navigation tree
                NavigationManager navManager = (NavigationManager)repository.getExtension("NavigationManager");
                String activePath = activeNavPath;

                // only consider the active document if the activeNavPath is not set
                VariantKey activeDocument = null;
                if ( activeNavPath == null || activeNavPath.length() == 0) {
                    activePath = navigationPath;
                    activeDocument = new VariantKey(siteConf.getHomePageDocId(), siteConf.getBranchId(), siteConf.getLanguageId());
                    Pattern parentDoc = Pattern.compile(".*\\/([^\\/]+)\\/new");
                    Matcher parent = parentDoc.matcher(navigationPath);
                    if (parent.matches()) {
                        String parentDocId = parent.group(1);
                        try {
                            activeDocument = repository.getDocument(parentDocId, document.getBranchId(), document.getLanguageId(), false).getVariantKey();
                        } catch (DocumentVariantNotFoundException dvnfe) {
                        }
                    }
                }
                
                NavigationParams navigationParams = new NavigationParams(siteConf.getNavigationDoc(), frontEndContext.getVersionMode(), activePath, siteConf.contextualizedTree(), siteConf.getNavigationDepth(), true, locale);                
                navManager.generateNavigationTree(embedded, navigationParams, activeDocument, true, false);
                
                //TODO: <p:group @id='navigationInfo'>?

                embedded.endElement("http://outerx.org/daisy/1.0#publisher", "publisherResponse", "p:publisherResponse");
                embedded.endPrefixMapping("p");
                fakePublisherResponse.endDocument();
                
                fakePublisherResponse.toSAX(pdh);
            } else {
                publisherResponse = helper.performPublisherRequest(frontEndContext.getDaisyCocoonPath() +"/internal/documentpage_pubreq.xml", viewData, "html");
            }

            if (form == null) {
                AclResultInfo aclInfo = editorContext.getAclInfo(document);
                buildForm(publisherResponse, pageContext, aclInfo);
                editorContext.setPartWidgetResolver(new WidgetResolver() {
                    public Widget resolveWidget(String key) {
                        return form.lookupWidget("part_" + editorContext.getPartEditorsByName().get(key).getPartTypeUse().getPartType().getId());
                    }
                });
                editorContext.setFieldWidgetResolver(new WidgetResolver() {
                    public Widget resolveWidget(String key) {
                        return form.lookupWidget("field_" + editorContext.getFieldEditorsByName().get(key).getFieldTypeUse().getFieldType().getId());
                    }
                });
                editorContext.setupEditors(document, aclInfo);
            }
    
            appleRequest.getCocoonRequest().setAttribute("CocoonFormsInstance", form);
            viewData.put("CocoonFormsInstance", form);
            viewData.put("publisherResponse", publisherResponse);
    
            appleResponse.sendPage("html-InlineEditPipe", viewData);
        }
    }

    private boolean processFormSubmit(AppleRequest appleRequest, AppleResponse appleResponse, String resource)
            throws HttpMethodNotAllowedException, Exception,
            RepositoryException {

        triedToSave |= "save".equals(request.getParameter("forms_submit_id"));

        Field nameWidget = (Field)form.getChild("documentName");
        if (nameWidget != null) {
            nameWidget.setRequired(triedToSave);
        }
        if (form.process(new FormContext(request, locale))) {
            if (form.getSubmitWidget() != null && form.getSubmitWidget().getId().equals("save")) {
                editorContext.saveEditors(document);
                
                if (nameWidget != null) {
                    document.setName((String)nameWidget.getValue());
                }
                
                document.save(false);
                document.releaseLock();
                appleResponse.redirectTo(frontEndContext.getMountPoint() + "/" + frontEndContext.getSiteConf().getName() + "/" + document.getId() + getVariantQueryString());
                return true;
            }
            return false;
        } else if (form.getSubmitWidget() != null && form.getSubmitWidget().getId().equals("cancel")) {
            document.releaseLock();
            if (returnTo != null)
                ResponseUtil.safeRedirect(appleRequest, appleResponse, returnTo);
            else if (document.getId() != null)
                ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePathQuery(currentPath + "/../" + document.getId() + ".html" + (!document.isVariantNew() ? getVariantQueryString() : "")));
            else
                ResponseUtil.safeRedirect(appleRequest, appleResponse, EncodingUtil.encodePath(getMountPoint() + "/" + siteConf.getName() + "/"));
            return true;
        }
        return false;
    }

    private void initEditorContext() throws RepositoryException {
        String branch = repository.getVariantManager().getBranch(document.getBranchId(), false).getName();
        String language = repository.getVariantManager().getLanguage(document.getLanguageId(), false).getName();

        editorContext = new DocumentEditorContext(documentType, document, repository, document.getId(), document.getBranchId(), document.getLanguageId(), branch, language, locale, serviceManager, frontEndContext.getPageContext(), logger, getContext(), frontEndContext.getDaisyCocoonPath(), lockExpires);
        editorContext.setSelectionListDataWidgetResolver(new DocumentEditorContext.SelectionListDataWidgetResolver() {
            public DataWidget lookupDataWidget(String widgetPath) {
                return (DataWidget)form.lookupWidget(widgetPath.replace('.', '/'));
            }
        });
    }
    
    private Map<String, Object> getEditViewData() {
        Map<String, Object> viewData = new HashMap<String, Object>();
        
        viewData.put("pageContext", editorContext.getPageContext());
        viewData.put("variantParams", getVariantParams());
        viewData.put("variantQueryString", getVariantQueryString());
        viewData.put("documentId", document.getId());
        viewData.put("branch", getBranch());
        viewData.put("language", getLanguage());
        viewData.put("version", "last");
        viewData.put("localeAsString", request.getAttribute("localeAsString"));
        viewData.put("activePath", activeNavPath);
        viewData.put("needsDocumentType", Boolean.TRUE);
        viewData.put("htmlareaLang", locale.getLanguage());
        viewData.put("documentEditorContext", editorContext);
        viewData.put("daisyVersion", repository.getClientVersion());
        viewData.put("heartbeatInterval", String.valueOf(DocumentEditorContext.HEARTBEAT_INTERVAL));
        viewData.put("displayMode", "inlineEditor");

        return viewData;
    }

    private void buildForm(SaxBuffer publisherResponse, PageContext pageContext, AclResultInfo aclInfo) throws Exception {
        SaxBuffer styledDocument = ((PreparedDocuments)request.getAttribute("styledResult-1")).getPreparedDocument(1).getSaxBuffer();
        InlineFormConfig inlineFormConfig = InlineFormConfig.createInlineFormConfig(styledDocument, documentType);
        
        DocumentType documentType = repository.getRepositorySchema().getDocumentTypeById(document.getDocumentTypeId(), false);
        form = createForm(document, documentType, inlineFormConfig, pageContext, aclInfo);
        form.addValidator(new ConditionalValidator(ValidationCondition.ALWAYS, new CheckFieldsFormValidator()));
    }

    /**
     * Dynamically creates a form definition for the given inlineFormConfig.
     */
    private Form createForm(Document document, DocumentType documentType, InlineFormConfig inlineFormConfig,
            PageContext pageContext, AclResultInfo aclInfo) throws Exception {
        List<String> parts = new ArrayList<String>();
        List<String> fields = new ArrayList<String>();
        editorContext.createEditors(inlineFormConfig.getParts(), inlineFormConfig.getFields());
        
        Map objectModel = ContextHelper.getObjectModel(getContext());
        Object oldViewData = FlowHelper.getContextObject(objectModel);
    
        FormManager formManager = null;
        try {
            Map<String, Object> viewData = new HashMap<String, Object>();
            //TODO: caching? (The cache key needs to reflect the fact that the selection of fields and parts influences the form definition)
            //viewData.put("fieldsFormCacheKey", "daisy-fieldform-documenttype-" + documentType.getId() + "-" + documentType.getUpdateCount());
            //viewData.put("fieldsFormValidity", new NOPValidity());
            //viewData.put("fieldsFormValidity", FieldsFormSourceValidity.getValidity(documentType, fieldEditors, getContext()));
            viewData.put("documentType", documentType);
            viewData.put("fieldEditors", editorContext.getFieldEditors());
            viewData.put("partEditors", editorContext.getPartEditors());
            viewData.put("locale", frontEndContext.getLocale());
            viewData.put("localeAsString", frontEndContext.getLocaleAsString());
            viewData.put("formConfig", inlineFormConfig);
            viewData.put("serviceManager", serviceManager);

            formManager = (FormManager)serviceManager.lookup(FormManager.ROLE);
            FlowHelper.setContextObject(objectModel, viewData);
            Form form = formManager.createForm("cocoon:/internal/documentEditor/inlineEditorFormDefinition");
            form.setAttribute("fieldEditorsByName", editorContext.getFieldEditorsByName());
            form.setAttribute("fieldEditors", editorContext.getFieldEditors());
            form.setAttribute("partEditorsByName", editorContext.getPartEditorsByName());
            form.setAttribute("partEditors", editorContext.getPartEditors());
            form.setAttribute("editPath", getPath());
            
            // initialize document name widget
            if (inlineFormConfig.isEditDocumentName()) {
                form.getChild("documentName").setValue(document.getName());
                form.getChild("documentName").setState(aclInfo.getAccessDetails(AclPermission.WRITE)
                        .isGranted(AclDetailPermission.DOCUMENT_NAME) ? WidgetState.ACTIVE : WidgetState.DISABLED);

            }
    
            // Custom view data will be added to the normal view data of the form template (see DocumentEditorForm.getActiveFormTemplateViewData)
            // This is because the form template pipeline also needs to validity object, the fieldEditors array, etc.
            form.setAttribute("customViewData", viewData);
            form.setAttribute("document", document);
    
            return form;
        } finally {
            if (formManager != null)
                serviceManager.release(formManager);
    
            FlowHelper.setContextObject(objectModel, oldViewData);
        }
    }

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected String getPath() {
        return currentPath + "/inline-edit/" + getContinuationId();
    }

}
