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

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.forms.FormManager;
import org.apache.cocoon.forms.datatype.StaticSelectionList;
import org.apache.cocoon.forms.event.ActionEvent;
import org.apache.cocoon.forms.event.ActionListener;
import org.apache.cocoon.forms.formmodel.Action;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.i18n.Bundle;
import org.apache.cocoon.i18n.BundleFactory;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.repository.AvailableVariant;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.variant.Language;

/**
 * Constructs {@link DocumentEditorContext}s.
 */
public class DocumentEditorFormBuilder {
    private DocumentType documentType;
    private String documentId;
    private long docRefLangId;
    private DocumentEditorContext documentEditorContext;
    private DocumentEditorForm documentEditorForm;

    private DocumentEditorFormBuilder(DocumentType documentType, String documentId, long docRefLangId, 
            DocumentEditorContext documentEditorContext) {
        this.documentType = documentType;
        this.documentId = documentId;
        this.docRefLangId = docRefLangId;
        this.documentEditorContext = documentEditorContext;
    }

    public static DocumentEditorForm build(DocumentType documentType, String documentId, long docRefLangId,
            DocumentEditorContext editorContext) throws Exception {
        return new DocumentEditorFormBuilder(documentType, documentId, docRefLangId, editorContext).build();
    }


    private DocumentEditorForm build() throws Exception {
        documentEditorForm = new DocumentEditorForm(documentEditorContext);
        WidgetResolver partWidgetResolver = new WidgetResolver() {
            public Widget resolveWidget(String key) {
                return documentEditorForm.getPartForm(key).lookupWidget("part_" + documentType.getPartTypeUse(key).getPartType().getId());
            }
        };
        WidgetResolver fieldWidgetResolver = new WidgetResolver() {
            public Widget resolveWidget(String key) {
                return documentEditorForm.getFieldsForm().lookupWidget("field_" + documentType.getFieldTypeUse(key).getFieldType().getId());
            }
        };
        documentEditorContext.setPartWidgetResolver(partWidgetResolver);
        documentEditorContext.setFieldWidgetResolver(fieldWidgetResolver);
        documentEditorForm.setMajorChange(true);
        
        documentEditorContext.createEditors(Arrays.asList(documentType.getPartTypeUses()), Arrays.asList(documentType.getFieldTypeUses()));

        Repository repository = documentEditorContext.getRepository();
        Locale locale = documentEditorContext.getLocale();
        
        Form linksForm = getLinksForm();
        Form miscForm = getMiscForm();
        Form additionalPartsAndFieldsForm = getAdditionalPartsAndFieldsForm();
        Form fieldsForm = getFieldsForm();
        
        documentEditorForm.setLinksForm(linksForm);
        documentEditorForm.setMiscForm(miscForm);
        documentEditorForm.setAdditionalPartsAndFieldsForm(additionalPartsAndFieldsForm);
        if (fieldsForm != null)
            documentEditorForm.setFieldsForm(fieldsForm);
        
        Map objectModel = (Map)ContextHelper.getObjectModel(documentEditorContext.getContext());
        Object oldViewData = FlowHelper.getContextObject(objectModel);
        FormManager formManager = null;
        try {
            formManager = (FormManager)documentEditorContext.getServiceManager().lookup(FormManager.ROLE);

            for (PartEditor partEditor: documentEditorContext.getPartEditors()) {
                Map<String,Object> viewData = new HashMap<String,Object>();
                viewData.put("documentType", documentType);
                viewData.put("partEditor", partEditor);
                viewData.put("locale", locale);
                viewData.put("serviceManager", documentEditorContext.getServiceManager());
                FlowHelper.setContextObject(objectModel, viewData);
                Form partEditForm = formManager.createForm("cocoon:/internal/documentEditor/partEditorFormDefinition");
                partEditForm.setAttribute("partEditor", partEditor); // is this still necessary?

                documentEditorForm.addPartForm(partEditor.getPartTypeUse(), partEditor.getFormTemplate(), partEditForm);
            }
        } finally {
            if (formManager != null) {
                documentEditorContext.getServiceManager().release(formManager);
            }
            FlowHelper.setContextObject(objectModel, oldViewData);
        }
        
        return documentEditorForm;
    }

    private Form getLinksForm() throws Exception {
        final Form form = FormHelper.createForm(documentEditorContext.getServiceManager(), "resources/form/doceditor_links_definition.xml");

        Action addLink = (Action)form.getChild("addLink");
        addLink.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Repeater links = (Repeater)form.getChild("links");
                links.addRow();
            }
        });

        return form;
    }

    private Form getMiscForm() throws Exception {
        final Form form = FormHelper.createForm(documentEditorContext.getServiceManager(), "resources/form/doceditor_misc_definition.xml");
        Repository repository = documentEditorContext.getRepository();
        long docLangId = documentEditorContext.getDocumentLanguageId();
        
        Action addCustomField = (Action)form.getChild("addCustomField");
        addCustomField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                Repeater customFields = (Repeater)form.getChild("customFields");
                customFields.addRow();
            }
        });
        
        Field referenceLanguage = (Field)form.getChild("referenceLanguageId");
        StaticSelectionList list = new StaticSelectionList(referenceLanguage.getDatatype());
        list.addItem(-1, getEditorI18nMessage("editdoc.reference-language-none"));
        
        Set<Language> candidateReferenceLanguages = new HashSet<Language>();
        
        // the document about to be created is a candidate
        candidateReferenceLanguages.add(repository.getVariantManager().getLanguage(docLangId, false));
        // the current reference language is also a candidate
        if (docRefLangId != -1) {
            candidateReferenceLanguages.add(repository.getVariantManager().getLanguage(docRefLangId, false));
        }
        if (documentId != null) {
            for (AvailableVariant variant: repository.getAvailableVariants(documentId).getArray()) {
                candidateReferenceLanguages.add(variant.getLanguage());
            }
        }
        
        // create a list of possible referenceLanguages sorted by name
        Language[] languages = repository.getVariantManager().getAllLanguages(false).getArray();
        Arrays.sort(languages, new Comparator() {
            public int compare(Object language0, Object language1) {
                return ((Language)language0).getName().compareTo(((Language)language1).getName());
            }
        });
        for (Language language: languages) {
            if (candidateReferenceLanguages.contains(language)) {
                list.addItem(language.getId(), language.getName());
            }
        }

        referenceLanguage.setSelectionList(list);
        
        return form;
    }

    private Form getAdditionalPartsAndFieldsForm() throws Exception {
        final Form form = FormHelper.createForm(documentEditorContext.getServiceManager(), "resources/form/doceditor_additionalPartsAndFields_definition.xml");
        form.setAttribute("documentEditorForm", documentEditorForm);
        return form;
    }

    private Form getFieldsForm() throws Exception {
        if (documentType.getFieldTypeUses().length == 0)
            return null;

        Form form = createFieldEditorForm(documentType);
        ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
        form.addValidator(new ConditionalValidator(validateOnSave, new CheckFieldsFormValidator())); 
        return form;
    }

    /**
     * Dynamically creates a form definition for the given document type.
     */
    private Form createFieldEditorForm(DocumentType documentType) throws Exception {
        FieldTypeUse[] fieldTypeUses = documentType.getFieldTypeUses();
        FieldEditor[] fieldEditors = new FieldEditor[fieldTypeUses.length];
        fieldEditors = documentEditorContext.getFieldEditors();

        Context context = documentEditorContext.getContext();
        Locale locale = documentEditorContext.getLocale();
        ServiceManager serviceManager = documentEditorContext.getServiceManager();
        Map objectModel = ContextHelper.getObjectModel(context);
        Object oldViewData = FlowHelper.getContextObject(objectModel);

        FormManager formManager = null;
        try {
            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("fieldsFormCacheKey", "daisy-fieldform-documenttype-" + documentType.getId() + "-" + documentType.getUpdateCount());
            viewData.put("fieldsFormValidity", FieldsFormSourceValidity.getValidity(documentType, fieldEditors, context));
            viewData.put("documentType", documentType);
            viewData.put("fieldEditors", fieldEditors);
            viewData.put("locale", locale);

            formManager = (FormManager)serviceManager.lookup(FormManager.ROLE);
            FlowHelper.setContextObject(objectModel, viewData);

            Form form = formManager.createForm("cocoon:/internal/documentEditor/fieldEditorFormDefinition");

            // fieldEditors are initialized via DocumentBinding.load() -> DocumentEditorContext.setupEditors();
            
            // Custom view data will be added to the normal view data of the form template (see DocumentEditorForm.getActiveFormTemplateViewData)
            // This is because the form template pipeline also needs to validity object, the fieldEditors array, etc.
            form.setAttribute("customViewData", viewData);

            return form;
        } finally {
            if (formManager != null)
                serviceManager.release(formManager);

            FlowHelper.setContextObject(objectModel, oldViewData);
        }
    }
    
    private String getEditorI18nMessage(String key) throws Exception {
        Bundle bundle = null;
        ServiceManager serviceManager = documentEditorContext.getServiceManager();
        Locale locale = documentEditorContext.getLocale();
        BundleFactory bundleFactory = (BundleFactory)serviceManager.lookup(BundleFactory.ROLE);
        try {
            bundle = bundleFactory.select("resources/i18n", "messages", locale);
            return bundle.getString(key);
        } finally {
            if (bundle != null)
                bundleFactory.release(bundle);
            serviceManager.release(bundleFactory);
        }
    }

}
