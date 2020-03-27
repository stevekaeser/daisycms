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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.datatype.Datatype;
import org.apache.cocoon.forms.formmodel.DataWidget;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.frontend.DaisyException;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.PageContext;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LockInfo;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.schema.SelectionList;
import org.outerx.daisy.x10Publisher.AnnotatedDocumentDocument1;
import org.outerx.daisy.x10Publisher.DocumentDocument;
import org.outerx.daisy.x10Publisher.GroupDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;

public class DocumentEditorContext {

    public static final int HEARTBEAT_INTERVAL = 10 * 60 * 1000; // 10 minutes

    private Repository repository;
    private DocumentType documentType;
    private Document document;
    private String documentId;
    private long branchId;
    private String branch;
    private long languageId;
    private String language;
    private Locale locale;
    private ServiceManager serviceManager;
    private PageContext pageContext;
    private Logger logger;
    private Context context;
    private String daisyCocoonPath;
    
    private boolean autoExtendLock;
    private long lockExpires;

    private Map<String, PartEditor> partEditorsByName;
    private PartEditor[] partEditors;
    private Map<String, Boolean> partAccessByName = new HashMap<String, Boolean>();
    private WidgetResolver partWidgetResolver;
    
    private Map<String, FieldEditor> fieldEditorsByName;
    private FieldEditor[] fieldEditors;
    private Map<String, Boolean> fieldAccessByName = new HashMap<String, Boolean>();
    private WidgetResolver fieldWidgetResolver;

    private SelectionListDataWidgetResolver selectionListDataWidgetResolver;
    
    private List<String> tabSequence = Arrays.asList("parts","fields","links","misc");
    
    private boolean validateOnSave = true;

    public DocumentEditorContext(DocumentType documentType, Document document, Repository repository, String documentId,
            long branchId, long languageId, String branch, String language, Locale locale, ServiceManager serviceManager, PageContext pageContext, Logger logger, Context context, String daisyCocoonPath, long lockExpires) {
        this.repository = repository;
        this.documentType = documentType;
        this.documentId = documentId;
        this.branchId = branchId;
        this.branch = branch;
        this.languageId = languageId;
        this.language = language;
        this.locale = locale;
        this.serviceManager = serviceManager;
        this.pageContext = pageContext;
        this.logger = logger;
        this.context = context;
        this.daisyCocoonPath = daisyCocoonPath;
        this.autoExtendLock = pageContext.getSiteConf().getAutoExtendLock();
        this.lockExpires = lockExpires;
    }
    
    public Document getDocument() {
        return document;
    }
    
    public String getDocumentId() {
        return documentId;
    }

    public String getDocumentBranch() {
        return branch;
    }

    public String getDocumentLanguage() {
        return language;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public Repository getRepository() {
        return repository;
    }
    public long getDocumentBranchId() {
        return branchId;
    }
    public long getDocumentLanguageId() {
        return languageId;
    }
    public Locale getLocale() {
        return locale;
    }
    public ServiceManager getServiceManager() {
        return serviceManager;
    }
    public PageContext getPageContext() {
        return pageContext;
    }
    public Logger getLogger() {
        return logger;
    }
    public Context getContext() {
        return context;
    }

    public String getDaisyCocoonPath() {
        return daisyCocoonPath;
    }

    public boolean isValidateOnSave() {
        return validateOnSave;
    }

    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;
    }
    
    public void createEditors(Collection<PartTypeUse> parts, Collection<FieldTypeUse> fields) throws Exception {
        partEditorsByName = new HashMap<String, PartEditor>();
        partEditors = new PartEditor[parts.size()];

        fieldEditorsByName = new HashMap<String, FieldEditor>();
        fieldEditors = new FieldEditor[fields.size()];
        
        int partCount = 0;
        for (PartTypeUse partTypeUse: parts) {
            if (partTypeUse == null) {
                continue;
            }
            PartEditor partEditor = PartEditorManager.getPartEditor(partTypeUse, this);
            partEditorsByName.put(partTypeUse.getPartType().getName(), partEditor);
            partEditors[partCount++] = partEditor;
            if (partEditor == null) {
                throw new RuntimeException("Could not instantiate part editor for " + partTypeUse.getPartType().getName() + ".  The custom part editor configuration may be invalid");
            }
        }
        
        int fieldCount = 0;
        for (FieldTypeUse fieldTypeUse: fields) {
            if (fieldTypeUse == null) {
                continue;
            }
            FieldEditor fieldEditor = FieldEditorManager.getFieldEditor(fieldTypeUse, this);
            fieldEditorsByName.put(fieldTypeUse.getFieldType().getName(), fieldEditor);
            fieldEditors[fieldCount++] = fieldEditor;
            if (fieldEditor == null) {
                throw new RuntimeException("Could not instantiate field editor for " + fieldTypeUse.getFieldType().getName() + ".  The custom field editor configuration may be invalid");
            }
        }
    }
    
    public void setPartWidgetResolver(WidgetResolver partWidgetResolver) {
        this.partWidgetResolver = partWidgetResolver;
    }

    public void setFieldWidgetResolver(WidgetResolver fieldWidgetResolver) {
        this.fieldWidgetResolver = fieldWidgetResolver;
    }
    
    public void setSelectionListDataWidgetResolver(SelectionListDataWidgetResolver selectionListDataWidgetResolver) {
        this.selectionListDataWidgetResolver = selectionListDataWidgetResolver;
    }
    
    public void setupEditors(Document document, AclResultInfo aclInfo) throws Exception {                
        calculateEditorAccess(aclInfo);
        
        for (PartEditor partEditor: partEditors) {
            String partTypeName = partEditor.getPartTypeUse().getPartType().getName();
            Widget widget = partWidgetResolver.resolveWidget(partTypeName);
            widget.setAttribute("partEditor", partEditor);
            partEditor.init(widget, !partAccessByName.get(partTypeName));
        }
        for (FieldEditor fieldEditor: fieldEditors) {
            String fieldTypeName = fieldEditor.getFieldTypeUse().getFieldType().getName();
            Widget widget = fieldWidgetResolver.resolveWidget(fieldTypeName);
            widget.setAttribute("fieldEditor", fieldEditor);
            fieldEditor.init(widget, !fieldAccessByName.get(fieldTypeName));
        }
        
        for (PartEditor partEditor: partEditors) {
            if (document.hasPart(partEditor.getPartTypeUse().getPartType().getId())) {
                partEditor.load(document);
            }
        }
        for (FieldEditor fieldEditor: fieldEditors) {
            if (document.hasField(fieldEditor.getFieldTypeUse().getFieldType().getId())) {
                fieldEditor.load(document);
            }
        }
        
        setValidateOnSave(true);

    }
    
    private void calculateEditorAccess(AclResultInfo info) {
        if (info == null) {
            throw new NullPointerException("(AclResultInfo)info should not be null");
        }
        
        Set<String> accessibleParts = null;
        Set<String> accessibleFields = null;
        boolean allFieldsGranted = true;
        boolean allPartsGranted = true;
        
        AccessDetails writeAccessDetails = info.getAccessDetails(AclPermission.WRITE);
        if (writeAccessDetails != null) {
            allFieldsGranted = writeAccessDetails.isGranted(AclDetailPermission.ALL_FIELDS);
            allPartsGranted = writeAccessDetails.isGranted(AclDetailPermission.ALL_PARTS);
            accessibleParts = writeAccessDetails.getAccessibleParts();
            accessibleFields = writeAccessDetails.getAccessibleFields();
        }
        
        for (PartEditor partEditor: partEditors) {
            String partTypeName = partEditor.getPartTypeUse().getPartType().getName();
            boolean combinedAccess = partEditor.getPartTypeUse().isEditable() && (allPartsGranted || accessibleParts.contains(partTypeName));
            partAccessByName.put(partTypeName, combinedAccess);
        }
        for (FieldEditor fieldEditor: fieldEditors) {
            String fieldTypeName = fieldEditor.getFieldTypeUse().getFieldType().getName();
            boolean combinedAccess = fieldEditor.getFieldTypeUse().isEditable() && (allFieldsGranted || accessibleFields.contains(fieldTypeName));
            fieldAccessByName.put(fieldTypeName, combinedAccess);
        }
    }

    public void saveEditors(Document document) throws Exception {
        for (PartEditor partEditor: partEditors) {
            if (partEditor.getPartTypeUse().isEditable()) {
                partEditor.save(document);
            }
        }
        for (FieldEditor fieldEditor: fieldEditors) {
            fieldEditor.save(document);
        }
    }

    public AclResultInfo getAclInfo(Document document) throws RepositoryException {
        AclResultInfo info;
        if (!document.isNew()) {
            info = repository.getAccessManager().getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), document.getVariantKey());
        } else {
            info = repository.getAccessManager().getAclInfoOnLiveForConceptualDocument(repository.getUserId(),
                    repository.getActiveRoleIds(), document.getDocumentTypeId(), document.getBranchId(), document.getLanguageId());

            if (!info.isAllowed(AclPermission.WRITE)) {
                // Unlikely to occur, since user will normally only be able to select
                // document type, branch, language combinations which he can create
                throw new RuntimeException("You are not allowed to create this document.");
            }
        }
        return info;
    }

    public FieldEditor[] getFieldEditors() {
        return fieldEditors;
    }

    public PartEditor[] getPartEditors() {
        return partEditors;
    }

    public Map<String, PartEditor> getPartEditorsByName() {
        return partEditorsByName;
    }

    public Map<String, FieldEditor> getFieldEditorsByName() {
        return fieldEditorsByName;
    }
    
    public List<String> getTabSequence() {
        return tabSequence;
    }

    public void setTabSequence(List<String> tabSequence) {
        this.tabSequence = tabSequence;
    }

    public boolean handleCommonResources(AppleRequest appleRequest, AppleResponse appleResponse, Document document, String resource, Map<String, Object> viewData) throws Exception {
        if (resource == null) {
            return false;
        } else if (resource.equals("heartbeat")) {
            sendHeartBeatResponse(appleResponse, document);
        } else if (resource.equals("selectionList")){
            handleSelectionListRequest(false, appleRequest.getCocoonRequest(), appleResponse, document.getBranchId(), document.getLanguageId());
        } else if (resource.equals("selectionList.xml")){
            handleSelectionListRequest(true, appleRequest.getCocoonRequest(), appleResponse, document.getBranchId(), document.getLanguageId());
        } else if (resource.equals("includePreviews")){
            handleIncludePreviewsRequest(appleRequest.getCocoonRequest(), appleResponse);
        } else if ("part".equals(resource)) {
            handlePartRequest(appleRequest, appleResponse);
        } else {
            return false;
        }
        
        return true;
    }

    private void handlePartRequest(AppleRequest appleRequest,
            AppleResponse appleResponse) throws DaisyException,
            RepositoryException {
        String partName = appleRequest.getSitemapParameter("part");
        Widget partWidget = partWidgetResolver.resolveWidget(partName);
        if (partWidget == null) {
            throw new DaisyException("There is no widget for a part named \""+partName+"\".");
        }
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("uploadWidget", partWidget.lookupWidget("last-upload-part"));
        appleResponse.sendPage("internal/readUploadWidget", viewData);
    }
    
    protected void sendHeartBeatResponse(AppleResponse appleResponse, Document document)
    throws RepositoryException {
        LockInfo lockInfo = null;
        if (autoExtendLock && !document.isNew()) {
            int margin = 3 * 60 * 1000; // 3 minutes
            if (lockExpires - System.currentTimeMillis() - HEARTBEAT_INTERVAL - margin < 0) {
                // time to extend the lock
                boolean success = document.lock(pageContext.getSiteConf().getDefaultLockTime(), pageContext.getSiteConf().getLockType());
                if (!success) {
                    lockInfo = document.getLockInfo(false);
                }
            }
        }
        Map<String, Object> viewData = new HashMap<String, Object>();
        if (lockInfo != null) {
            viewData.put("lockInfo", lockInfo);
            String userName = repository.getUserManager().getUserDisplayName(lockInfo.getUserId());
            viewData.put("lockUserName", userName);
        }
        GenericPipeConfig pipeConfig = GenericPipeConfig.templateOnlyPipe("resources/xml/heartbeat.xml");
        pipeConfig.setXmlSerializer();
        viewData.put("pipeConf", pipeConfig);
        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    protected void handleSelectionListRequest(boolean asXml, Request request, AppleResponse response, long branchId, long languageId) throws Exception {
        String widgetPath = RequestUtil.getStringParameter(request, "widgetPath");
        DataWidget widget = selectionListDataWidgetResolver.lookupDataWidget(widgetPath);
        Datatype datatype = widget.getDatatype();

        long fieldTypeId = RequestUtil.getLongParameter(request, "fieldTypeId");
        FieldTypeUse fieldTypeUse = documentType.getFieldTypeUse(fieldTypeId);
        if (fieldTypeUse == null)
            throw new Exception("Document type does not have a field type with ID " + fieldTypeId);
        FieldType fieldType = fieldTypeUse.getFieldType();

        SelectionList selectionList = fieldType.getSelectionList();
        if (selectionList == null)
            throw new Exception("Field type with id " + fieldTypeId + " does not have a selection list.");

        SelectionListAdapter selectionListAdapter = new SelectionListAdapter(datatype, selectionList, false,
                fieldType.getValueType(), fieldType.isHierarchical(), repository.getVariantManager(),
                branchId, languageId);
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("selectionListAdapter", selectionListAdapter);
        viewData.put("widgetPath", widgetPath);
        viewData.put("fieldType", fieldType);
        viewData.put("locale", locale);
        viewData.put("hierarchicalList", !asXml);
        viewData.put("pageContext", FrontEndContext.get(request).getPageContext());

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/selectionlist.xml");
        pipeConf.setApplyLayout(false);
        if (asXml) {
            pipeConf.setStylesheet("resources/xslt/selectionlist_as_options.xsl");
            pipeConf.setApplyI18n(false);
            pipeConf.setXmlSerializer();
        } else {
            pipeConf.setStylesheet("daisyskin:xslt/selectionlist.xsl");
        }
        viewData.put("pipeConf", pipeConf);

        response.sendPage("internal/genericPipe", viewData);
    }
    
    protected void handleIncludePreviewsRequest(Request request, AppleResponse response) throws Exception {
        PublisherRequestDocument publisherRequestDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest publisherRequest = publisherRequestDoc.addNewPublisherRequest();

        int c = 1;
        String documentId;
        while ((documentId = request.getParameter("preview." + c + ".documentId")) != null) {
            String branch = request.getParameter("preview." + c + ".branch");
            String language = request.getParameter("preview." + c + ".language");
            String version = request.getParameter("preview." + c + ".version");

            if (branch != null && language != null) {
                GroupDocument.Group group = publisherRequest.addNewGroup();
                group.setId("preview" + c);
                group.setCatchErrors(true);

                DocumentDocument.Document docReq = group.addNewDocument();
                docReq.setId(documentId);
                docReq.setBranch(branch);
                docReq.setLanguage(language);
                docReq.setVersion(version);

                AnnotatedDocumentDocument1.AnnotatedDocument annotatedDocReq = docReq.addNewAnnotatedDocument();
                annotatedDocReq.setInlineParts("#daisyHtml");
            }
            c++;
        }

        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        SaxBuffer publisherResponse = new SaxBuffer();
        publisher.processRequest(publisherRequestDoc, publisherResponse);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", publisherResponse);

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setStylesheet("daisyskin:xslt/includepreviews.xsl");
        pipeConf.setApplyLayout(false);
        pipeConf.setXmlSerializer();
        viewData.put("pipeConf", pipeConf);

        response.sendPage("internal/genericPipe", viewData);
    }

    public interface SelectionListDataWidgetResolver {
        public DataWidget lookupDataWidget(String widgetPath);
    }

}
