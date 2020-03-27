/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.forms.formmodel.ContainerWidget;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.HierarchyPath;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.FieldTypeUse;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerx.daisy.x10.FieldTypeUseDocument;
import org.xml.sax.ContentHandler;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * A base for simple implementations of the
 * {@link org.outerj.daisy.frontend.editor.FieldEditor FieldEditor} interface.
 * 
 */
public abstract class AbstractFieldEditor implements FieldEditor {

    protected FieldTypeUse fieldTypeUse;
    
    protected DocumentEditorContext documentEditorContext;

    protected Widget widget;
    
    public AbstractFieldEditor(FieldTypeUse fieldTypeUse, DocumentEditorContext documentEditorContext) {
        this.fieldTypeUse = fieldTypeUse;
        this.documentEditorContext = documentEditorContext;
    }

    public abstract void load(Document document) throws Exception;

    /**
     * This function is called by the
     * {@link #generateFormDefinitionFragment(ContentHandler, Locale, String, ServiceManager)} method.
     * This function should always turn the correct path of definition template.
     * 
     * @return String containing the location of the widget definition template.
     */
    public abstract String getDefinitionTemplate();

    /**
     * This function is called by the
     * {@link #generateFormDefinitionFragment(ContentHandler, Locale, String, ServiceManager)} method.
     * This function should return the correct path of a stylesheet or null.
     * 
     * @return String containing the location of the stylesheet that should
     *         transform the output of the widget definition template.
     */
    public abstract String getDefinitionStylesheet();

    /**
     * This function is called by the
     * {@link #generateFormTemplateFragment(ContentHandler, Locale, boolean, ServiceManager)} method. *
     * This function should always turn the correct path of a template.
     * 
     * @return String containing the location of the template that generates the
     *         widget template.
     */
    public abstract String getTemplateTemplate();

    /**
     * This function is called by the
     * {@link #generateFormTemplateFragment(ContentHandler, Locale, boolean, ServiceManager)} method.
     * This function should return the correct path of a stylesheet or null.
     * 
     * @return String containing the location of the
     */
    public abstract String getTemplateStylesheet();

    /**
     * This method generates sax events using the internal/GenericPipe found in
     * the sitemap. The template & stylesheet used are specified by implementing
     * the {@link #getDefinitionTemplate()} and
     * {@link #getDefinitionStylesheet()} methods.
     */
    public void generateFormDefinitionFragment(ContentHandler contentHandler, Locale locale, String displayMode, ServiceManager serviceManager) throws Exception {
        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setApplyI18n(true);
        pipeConfig.setApplyLayout(false);
        pipeConfig.setTransformLinks(false);
        pipeConfig.setXmlSerializer();

        pipeConfig.setTemplate(this.getDefinitionTemplate());
        String stylesheet = this.getDefinitionStylesheet();
        if (stylesheet != null && stylesheet.length() > 0)
            pipeConfig.setStylesheet(stylesheet);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("displayMode", displayMode);

        processGenericPipe(pipeConfig, viewData, contentHandler, locale);
    }

    /**
     * This method generates sax events using the internal/GenericPipe found in
     * the sitemap. The template & stylesheet used are specified by implementing
     * the {@link #getTemplateTemplate()} and {@link #getTemplateStylesheet()}
     * methods.
     */
    public void generateFormTemplateFragment(ContentHandler contentHandler, Locale locale, String displayMode, ServiceManager serviceManager) throws Exception {
        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setApplyI18n(true);
        pipeConfig.setApplyLayout(false);
        pipeConfig.setTransformLinks(false);
        pipeConfig.setXmlSerializer();

        pipeConfig.setTemplate(this.getTemplateTemplate());
        String stylesheet = this.getTemplateStylesheet();
        if (stylesheet != null && stylesheet.length() > 0)
            pipeConfig.setStylesheet(stylesheet);
        
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("displayMode", displayMode);

        processGenericPipe(pipeConfig, viewData, contentHandler, locale);
    }

    protected void processGenericPipe(GenericPipeConfig pipeConfig, Map<String, Object> viewData, ContentHandler contentHandler, Locale locale)
            throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, documentEditorContext.getLogger(), documentEditorContext.getContext(), documentEditorContext.getServiceManager(), null, false);
            if (viewData == null) 
                viewData = new HashMap<String, Object>();
            viewData.put("pipeConf", pipeConfig);
            viewData.put("fieldTypeUse", fieldTypeUse);
            viewData.put("fieldTypeUseXml", new XmlObjectXMLizable(getAnnotatedFieldTypeUseXml(locale)));
            pipelineUtil.processToSAX("internal/genericPipe", viewData, new IncludeXMLConsumer(contentHandler));
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }

    public FieldTypeUse getFieldTypeUse() {
        return fieldTypeUse;
    }

    public boolean hasValue(Widget parentWidget) {
        Widget widget = ((ContainerWidget)parentWidget).getChild("field");
        if (widget.getValue() == null && widget.validate()) {
            return false;
        } else if (fieldTypeUse.getFieldType().isMultiValue() && widget.validate()) {
            Object[] values = (Object[])widget.getValue();
            if (values.length == 0)
                return false;
        }
        return true;
    }

    public void init(Widget parentWidget, boolean readonly) {
        this.widget = ((ContainerWidget)parentWidget).getChild("field");
    }

    public void setValidationError(ValidationError error) {
        ValidationErrorAware valueWidget = (ValidationErrorAware)widget;
        valueWidget.setValidationError(error);
    }

    public void save(Document document) throws Exception {
        FieldType fieldType = fieldTypeUse.getFieldType();
        long fieldTypeId = fieldType.getId();
        Object value = widget.getValue();
        if (value == null) {
            document.deleteField(fieldTypeId);
        } else if (value instanceof Object[]) {
            Object[] values = (Object[])value;
            if (values.length > 0)
                document.setField(fieldTypeId, getValueToSave(values, fieldType, documentEditorContext.getRepository()));
            else
                document.deleteField(fieldTypeId);
        } else {
            document.setField(fieldTypeId, getValueToSave(value, fieldType, documentEditorContext.getRepository()));
        }
    }

    protected final FieldTypeUseDocument getAnnotatedFieldTypeUseXml(Locale locale) {
        FieldType fieldType = fieldTypeUse.getFieldType();
        FieldTypeUseDocument fieldTypeUseDoc = fieldTypeUse.getExtendedXml();
        String label = fieldType.getLabel(locale);
        String description = fieldType.getDescription(locale);
        XmlCursor cursor = fieldTypeUseDoc.getFieldTypeUse().getFieldType().newCursor();
        while (!cursor.isStart())
            cursor.toNextToken();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("label", label);
        if (description != null)
            cursor.insertAttributeWithValue("description", description);
        cursor.dispose();
        return fieldTypeUseDoc;
    }

    protected Object getValueToSave(Object value, FieldType fieldType, Repository repository) {
        if (fieldType.isHierarchical() && fieldType.isMultiValue()) {
            value = convertStringsToHierarchyPaths((Object[])value, fieldType.getValueType(), repository);
        } else if (fieldType.isHierarchical()) {
            value = convertStringToHierarchyPath((String)value, fieldType.getValueType(), repository);
        } else if (fieldType.getValueType() == ValueType.LINK) {
            VariantManager variantManager = repository.getVariantManager();
            if (value instanceof Object[]) {
                Object[] values = (Object[])value;
                Object[] newValues = new Object[values.length];
                for (int i = 0; i < values.length; i++)
                    newValues[i] = LinkFieldHelper.parseVariantKey((String)values[i], variantManager);
                value = newValues;
            } else {
                value = LinkFieldHelper.parseVariantKey((String)value, repository.getVariantManager());
            }
        }
        return value;
    }

    private HierarchyPath[] convertStringsToHierarchyPaths(Object[] values, ValueType valueType, Repository repository) {
        HierarchyPath[] paths = new HierarchyPath[values.length];
        for (int i = 0; i < paths.length; i++) {
            paths[i] = convertStringToHierarchyPath((String)values[i], valueType, repository);
        }
        return paths;
    }

    private HierarchyPath convertStringToHierarchyPath(String value, ValueType valueType, Repository repository) {
        String[] parts = HierarchicalFieldHelper.parseHierarchicalInput(value);
        List<Object> elements = new ArrayList<Object>();
        for (String part : parts) {
            if (valueType == ValueType.LINK) {
                elements.add(LinkFieldHelper.parseVariantKey(part, repository.getVariantManager()));
            } else if (valueType == ValueType.STRING) {
                elements.add(part);
            } else {
                throw new RuntimeException("Unexpected valuetype for hierarhical field: " + valueType);
            }
        }
        return new HierarchyPath(elements.toArray());
    }
}
