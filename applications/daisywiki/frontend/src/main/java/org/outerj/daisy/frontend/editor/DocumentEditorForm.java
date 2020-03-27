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
package org.outerj.daisy.frontend.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;

import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.FormContext;
import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.excalibur.xml.sax.XMLizable;
import org.outerj.daisy.frontend.RequestUtil;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.util.Constants;

/**
 * Object representing the document editor form. It consists of multiple CForms.
 * This object should be created by the {@link DocumentEditorFormBuilder}.
 */
public class DocumentEditorForm {
    static enum FormValidState { VALID, NOT_VALID, NOT_VALIDATED }

    /**
     * Contains an instance of {@link PartFormInfo} for each part form.
     */
    private List<PartFormInfo> partFormInfos = new ArrayList<PartFormInfo>();
    private Map<String, Form> partForms = new HashMap<String, Form>();
    private Map<String, String> partFormTemplates = new HashMap<String, String>();

    private Form linksForm;
    private Form fieldsForm;
    private Form miscForm;
    private Form additionalPartsAndFieldsForm;
    private String documentName;
    private String requestedDocumentSequence;
    private String requestedNamespace;
    private XMLizable documentNameValidationError;
    private XMLizable documentIdValidationError;
    private boolean publishImmediately = false;
    private long syncedWithLanguageId = -1;
    private long syncedWithVersionId = -1;
    private boolean isMajorChange = false;
    private String changeComment = null;
    private String activeFormName;
    /** Contains all forms hashed on name. */
    private Map<String, Form> forms = new HashMap<String, Form>();
    /**
     * hasBeenTriedToSave is set to true after the first time the user pressed
     * the 'save' button. It determines whether validation should be performed
     * and whether validation error should be shown.
     */
    private boolean hasBeenTriedToSave = false;
    /**
     * A cache of what forms have already been validated. This is needed to avoid
     * frequent re-validation, which can be expensive.
     */
    private Map<String, FormValidState> formValidState = new HashMap<String, FormValidState>();
    
    private DocumentEditorContext documentEditorContext;
    private AclResultInfo aclInfo;
    

    protected DocumentEditorForm(DocumentEditorContext documentEditorContext) {
        this.documentEditorContext = documentEditorContext;        
        
        // Note: if there are parts of fields, then their forms will be set as
        // initial form to display instead.
        this.activeFormName = documentEditorContext.getTabSequence().contains("links") ? "links" : "misc";
    }

    public DocumentType getDocumentType() {
        return documentEditorContext.getDocumentType();
    }
    
    public Repository getRepository() {
        return documentEditorContext.getRepository();
    }

    public String getDocumentId() {
        return documentEditorContext.getDocumentId();
    }

    public long getDocumentBranchId() {
        return documentEditorContext.getDocumentBranchId();
    }
    
    public String getDocumentBranch() {
        return documentEditorContext.getDocumentBranch();
    }

    public long getDocumentLanguageId() {
        return documentEditorContext.getDocumentLanguageId();
    }
    
    public String getDocumentLanguage() {
        return documentEditorContext.getDocumentLanguage();
    }

    public boolean process(Request request, Locale locale, String formName) throws Exception {
        // Note: the activeForm request parameter contains the name of the form that
        // should become active, while the formName argument contains the name
        // of the form currently submitted.
        String activeForm = request.getParameter("activeForm");
        if (activeForm == null) {
            throw new Exception("Missing request parameter: activeForm");
        }
        if (!forms.containsKey(activeForm)) {
            throw new Exception("Invalid value for activeForm request parameter: " + activeForm);
        }
        this.activeFormName = activeForm;

        if (!RequestUtil.getBooleanParameter(request, "skipCrossEditorFields", false)) { // this parameter is sent by the part-editor applet
            // Handle cross-editor fields (not managed by CForms)
            if (aclInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.DOCUMENT_NAME))
                this.documentName = request.getParameter("name") != null ? request.getParameter("name").trim() : null;
            
            Repository repository = this.documentEditorContext.getRepository();
            // administrators get to request a document id themselves
            if (Arrays.binarySearch(repository.getActiveRoleIds(), 1) >= 0) {
                String docIdString = request.getParameter("requestedDocumentId") != null ? request.getParameter("requestedDocumentId").trim() : null;
                String ns = request.getParameter("namespace") != null ? request.getParameter("namespace").trim() : null;
                
                if (docIdString != null && !"".equals(docIdString) && ns != null && !"".equals(ns)) {
                    this.requestedDocumentSequence = docIdString;
                    this.requestedNamespace = ns;
                } else {
                    this.requestedDocumentSequence = null;
                    this.requestedNamespace = null;
                }
            }
            
            boolean newValidateOnSave = request.getParameter("validateOnSave") != null;
            if (newValidateOnSave != this.documentEditorContext.isValidateOnSave()) {
                documentEditorContext.setValidateOnSave(newValidateOnSave);
                // if the 'validate on save' flag changed, all forms should be revalidated
                for (String someForm : formValidState.keySet()) {
                    formValidState.put(someForm, FormValidState.NOT_VALIDATED);
                }
            }
            
            this.publishImmediately = request.getParameter("publishImmediately") != null;
            if (aclInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.CHANGE_TYPE))
                this.isMajorChange = request.getParameter("majorChange") != null;
            if (aclInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.CHANGE_COMMENT))
                this.changeComment = request.getParameter("changeComment");

            if (aclInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.SYNCED_WITH)) {
                String syncedWithLanguageParam = request.getParameter("syncedWithLanguageId");
                this.syncedWithLanguageId = syncedWithLanguageParam == null ? -1 : Long.valueOf(syncedWithLanguageParam);
                String syncedWithVersionParam = request.getParameter("syncedWithVersionId");
                this.syncedWithVersionId = syncedWithVersionParam == null ? -1 : Long.valueOf(syncedWithVersionParam);
            }
        }

        Form form = getForm(formName);
        form.process(new FormContext(request, locale));
        formValidState.put(formName, FormValidState.NOT_VALIDATED);
        boolean isSave = form.getSubmitWidget() == null;

        if (!hasBeenTriedToSave && isSave) {
            hasBeenTriedToSave = true;
        }
        
        if (isSave) {
            // validate all forms
            boolean allFormsValid = true;
            for (String currentFormName : forms.keySet()) {
                if (!validateForm(currentFormName))
                    allFormsValid = false;
            }
            if (!(documentNameValid() && documentIdValid()))
                allFormsValid = false;
            return allFormsValid;
        } else {
            return false;
        }
    }


    public boolean documentNameValid() {
        if (!hasBeenTriedToSave) {
            return true;
        }

        if (documentName == null || documentName.length() == 0) {
            documentNameValidationError = new I18nMessage("general.field-required", FormsConstants.I18N_CATALOGUE);
        } else if (documentName.length() > 1023) {
            documentNameValidationError = new I18nMessage("validation.string.max-length", new String[] {"1023"}, FormsConstants.I18N_CATALOGUE);
        } else {
            documentNameValidationError = null;
        }

        return documentNameValidationError == null;
    }
    
    public boolean documentIdValid() throws Exception{
        if (!hasBeenTriedToSave) {
            return true;
        }
        String requestedId = this.getRequestedDocumentId();
        if (requestedId != null) {
            Matcher m = Constants.DAISY_DOCID_PATTERN.matcher(requestedId);
            if (m.matches()) {                
                Namespace namespace = this.documentEditorContext.getRepository().getNamespaceManager().getNamespace(this.requestedNamespace);
                if (namespace.isManaged()) {
                    documentIdValidationError = new I18nMessage("editdoc.validate.namespace.managed");
                    return false;
                } else {
                    documentIdValidationError = null;
                    return true;
                }
            } else {
                documentIdValidationError = new I18nMessage("editdoc.validate.document-id.format");
                return false;
            }
        } else {
            documentIdValidationError = null;
            return true;
        }
    }

    public XMLizable getDocumentNameValidationError() {
        return documentNameValidationError;
    }

    public XMLizable getDocumentIdValidationError() {
        return documentIdValidationError;
    }

    private Form getForm(String formName) throws Exception {
        Form form = forms.get(formName);
        if (form == null)
            throw new Exception("Invalid form name: \"" + formName + "\".");
        return form;
    }

    public Form getPartForm(String partName) {
        return partForms.get("part-" + partName);
    }

    public Form[] getPartForms() {
        return partForms.values().toArray(new Form[0]);
    }

    public Form getMiscForm() {
        return miscForm;
    }

    public Form getFieldsForm() {
        return fieldsForm;
    }

    public Form getLinksForm() {
        return linksForm;
    }

    public Form getAdditionalPartsAndFieldsForm() {
        return additionalPartsAndFieldsForm;
    }

    public void setActiveForm(String formName) throws Exception {
        if (!forms.containsKey(formName)) {
            throw new Exception("Invalid form name: \"" + formName + "\".");
        }
        this.activeFormName = formName;
    }

    public Form getActiveForm() {
        return forms.get(activeFormName);
    }

    public String getActiveFormName() {
        return activeFormName;
    }

    public String getActiveFormTemplate() {
        if (activeFormName.equals("fields")) {
            return "cocoon:/internal/documentEditor/fieldEditorFormTemplate";
        } else if (activeFormName.startsWith("part-")) {
            return partFormTemplates.get(activeFormName);
        } else {
            return "resources/form/doceditor_" + activeFormName + "_template.xml";
        }
    }

    public Map<String, Object> getActiveFormTemplateViewData() {
        Map<String, Object> viewData = (Map<String, Object>)getActiveForm().getAttribute("customViewData");
        if (viewData != null) {
            return viewData;
        } else {
            return Collections.emptyMap();
        }
    }

    protected void addPartForm(PartTypeUse partTypeUse, String partFormTemplate, Form form) {
        String formName = "part-" + partTypeUse.getPartType().getName();
        partForms.put(formName, form);
        partFormInfos.add(new PartFormInfo(partTypeUse, formName, form));
        partFormTemplates.put(formName, partFormTemplate);
        forms.put(formName, form);

        String firstForm = this.documentEditorContext.getTabSequence().get(0);
        if (partForms.size() == 1 && firstForm.equals("parts")) {
            // this was the first part form, set it as initial form to show
            activeFormName = formName;
        } else if (partForms.size() == 0) {
            activeFormName = firstForm;
        }
    }

    protected void setLinksForm(Form form) {
        this.linksForm = form;
        forms.put("links", form);
    }

    protected void setFieldsForm(Form form) {
        this.fieldsForm = form;
        forms.put("fields", form);
        
        String firstForm = this.documentEditorContext.getTabSequence().get(0);
        if (partForms.size() == 0 || firstForm.equals("fields"))
            activeFormName = "fields";
    }

    protected void setMiscForm(Form form) {
        this.miscForm = form;
        forms.put("misc", form);
    }

    protected void setAdditionalPartsAndFieldsForm(Form form) {
        this.additionalPartsAndFieldsForm = form;
        forms.put("additionalPartsAndFields", form);
    }

    public boolean hasPartForms() {
        return partForms.size() > 0;
    }

    public boolean hasFieldsForm() {
        return fieldsForm != null;
    }

    public boolean hasAdditionalPartsOrFieldsForm() {
        boolean hasAdditionalParts = ((Repeater)additionalPartsAndFieldsForm.getChild("additionalParts")).getSize() > 0;
        boolean hasAdditionalFields = ((Repeater)additionalPartsAndFieldsForm.getChild("additionalFields")).getSize() > 0;

        return hasAdditionalParts || hasAdditionalFields;
    }

    public List getPartFormInfos() {
        return partFormInfos;
    }

    public static class PartFormInfo {
        private PartTypeUse partTypeUse;
        private String partFormName;
        private String partLabel;
        private String partDescription;
        private boolean isRequired;

        public PartFormInfo(PartTypeUse partTypeUse, String partFormName, Form partForm) {
            this.partTypeUse = partTypeUse;
            this.partFormName = partFormName;
            this.partLabel = partTypeUse.getPartType().getLabel(partForm.getLocale());
            this.partDescription = partTypeUse.getPartType().getDescription(partForm.getLocale());
            this.isRequired = partTypeUse.isRequired();
        }

        public PartTypeUse getPartTypeUse() {
            return partTypeUse;
        }
        
        public String getFormName() {
            return partFormName;
        }

        public String getLabel() {
            return partLabel;
        }

        public String getPartDescription() {
            return partDescription;
        }

        public boolean isRequired() {
            return isRequired;
        }

    }

    public PartFormInfo getCurrentPartFormInfo() {
        for (PartFormInfo partFormInfo : partFormInfos) {
            if (partFormInfo.getFormName().equals(activeFormName))
                return partFormInfo;
        }
        return null;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String name) {
        this.documentName = name;
    }

    public String getRequestedDocumentId() {
        if (this.requestedDocumentSequence != null && !"".equals(this.requestedDocumentSequence) && 
                this.requestedNamespace != null && !"".equals(this.requestedNamespace))
            return this.requestedDocumentSequence + "-" + this.requestedNamespace;
        else 
            return null;
    }

    public void setRequestedDocumentId(String requestedDocumentId) throws Exception{
        Matcher m = Constants.DAISY_DOCID_PATTERN.matcher(requestedDocumentId);
        if (m.matches()) {
            this.requestedDocumentSequence = m.group(1);
            this.requestedNamespace = m.group(2);
        } else {
            throw new Exception (requestedDocumentId + " does not match the pattern " + Constants.DAISY_DOCID_PATTERN.pattern());
        }        
    }

    public String getRequestedDocumentSequence() {
        return requestedDocumentSequence;
    }

    public String getRequestedNamespace() {
        return requestedNamespace;
    }

    public boolean getValidateOnSave() {
        return documentEditorContext.isValidateOnSave();
    }

    public void setValidateOnSave(boolean validateOnSave) {
        documentEditorContext.setValidateOnSave(validateOnSave);
    }

    public boolean getPublishImmediately() {
        return publishImmediately;
    }

    public void setPublishImmediately(boolean publishImmediately) {
        this.publishImmediately = publishImmediately;
    }

    public long getSyncedWithLanguageId() {
        return syncedWithLanguageId;
    }

    public void setSyncedWithLanguageId(long syncedWithLanguageId) {
        this.syncedWithLanguageId = syncedWithLanguageId;
    }

    public long getSyncedWithVersionId() {
        return syncedWithVersionId;
    }

    public void setSyncedWithVersionId(long syncedWithVersionId) {
        this.syncedWithVersionId = syncedWithVersionId;
    }

    public boolean getMajorChange() {
        return isMajorChange;
    }

    public void setMajorChange(boolean isMajorChange) {
        this.isMajorChange = isMajorChange;
    }
    
    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }

    public AclResultInfo getAclInfo() {
        return aclInfo;
    }

    public void setAclInfo(AclResultInfo aclInfo) {
        this.aclInfo = aclInfo;
    }

    private boolean validateForm(String formName) throws Exception {
        FormValidState state = formValidState.get(formName);
        if (state == null)
            state = FormValidState.NOT_VALIDATED;

        if (state == FormValidState.NOT_VALIDATED) {
            Form form = getForm(formName);
            form.endProcessing(false);
            form.validate();
            formValidState.put(formName, form.isValid() ? FormValidState.VALID : FormValidState.NOT_VALID);
            return form.isValid();
        } else {
            return state == FormValidState.VALID;
        }
    }

    public boolean isValid(String formName) throws Exception {
        FormValidState state = formValidState.get(formName);
        if (state == null || state == FormValidState.NOT_VALIDATED) {
            if (!hasBeenTriedToSave) {
                return true;
            } else if (activeFormName.equals(formName) && getActiveForm().getSubmitWidget() != null) {
                return true;
            } else {
                return validateForm(formName);
            }
        } else if (state == FormValidState.VALID) {
            return true;
        } else if (state == FormValidState.NOT_VALID) {
            return false;
        } else {
            throw new RuntimeException("Unexpected situation, FormValidState == " + state);
        }
    }

    /**
     * Returns true if all part forms are valid. Useful to be called from the template.
     */
    public boolean arePartFormsValid() throws Exception {
        for (String formName : partForms.keySet()) {
            if (!isValid(formName))
                return false;
        }
        return true;
    }
}
