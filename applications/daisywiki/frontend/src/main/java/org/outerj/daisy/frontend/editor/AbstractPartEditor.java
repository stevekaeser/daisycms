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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.avalon.framework.service.ServiceManager;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.components.LifecycleHelper;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.components.flow.util.PipelineUtil;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.xmlbeans.XmlCursor;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.XmlObjectXMLizable;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerx.daisy.x10.PartTypeUseDocument;
import org.xml.sax.ContentHandler;

public abstract class AbstractPartEditor implements PartEditor {
    
    protected PartTypeUse partTypeUse;

    protected Map<String, String> properties;

    protected DocumentEditorContext documentEditorContext;

    protected Widget widget;

    public AbstractPartEditor(PartTypeUse partTypeUse, Map<String, String> properties, DocumentEditorContext documentEditorContext) {
        this.partTypeUse = partTypeUse;
        this.properties = properties;
        this.documentEditorContext = documentEditorContext;
    }

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

        processGenericPipe(pipeConfig, null, contentHandler, locale, displayMode, serviceManager);
    }

    protected void processGenericPipe(GenericPipeConfig pipeConfig, Map<String, Object> viewData, ContentHandler contentHandler, Locale locale, String displayMode, ServiceManager serviceManager)
    throws Exception {
        PipelineUtil pipelineUtil = new PipelineUtil();
        try {
            LifecycleHelper.setupComponent(pipelineUtil, documentEditorContext.getLogger(), documentEditorContext.getContext(), serviceManager, null, false);

            Map objectModel = ContextHelper.getObjectModel(documentEditorContext.getContext());
            Map currentViewData = (Map)FlowHelper.getContextObject(objectModel);
            Map inheritViewData = new HashMap<String, Object>(currentViewData);
            if (viewData != null) {
                inheritViewData.putAll(viewData);
            }
            
            viewData = inheritViewData;
            viewData.put("pipeConf", pipeConfig);
            viewData.put("displayMode", displayMode);
            viewData.put("documentEditorContext", documentEditorContext);
            viewData.put("pageContext", documentEditorContext.getPageContext());
            viewData.put("partTypeUse", partTypeUse);
            viewData.put("partTypeUseXml", new XmlObjectXMLizable(getAnnotatedPartTypeUseXml(locale)));
            viewData.put("serviceManager", serviceManager);
            viewData.put("partEditor", this);

            pipelineUtil.processToSAX("internal/genericPipe", viewData, new IncludeXMLConsumer(contentHandler));
        } finally {
            LifecycleHelper.dispose(pipelineUtil);
        }
    }
    
    public void init(Widget parentWidget, boolean readonly) {
        this.widget = parentWidget;
        if  (widget != null) {
            widget.setAttribute("partEditor", this);
        }
    }
    
    protected abstract String getDefinitionTemplate();
    protected abstract String getDefinitionStylesheet();

    protected final PartTypeUseDocument getAnnotatedPartTypeUseXml(Locale locale) {
        PartType partType = partTypeUse.getPartType();
        PartTypeUseDocument partTypeUseDoc = partTypeUse.getExtendedXml();
        String label = partType.getLabel(locale);
        String description = partType.getDescription(locale);
        XmlCursor cursor = partTypeUseDoc.getPartTypeUse().getPartType().newCursor();
        while (!cursor.isStart())
            cursor.toNextToken();
        cursor.toNextToken();
        cursor.insertAttributeWithValue("label", label);
        if (description != null)
            cursor.insertAttributeWithValue("description", description);
        cursor.dispose();
        return partTypeUseDoc;
    }
    
    public void setValidationError(ValidationError error) {
        ValidationErrorAware valueWidget = (ValidationErrorAware)widget;
        valueWidget.setValidationError(error);
    }
    
    public PartTypeUse getPartTypeUse() {
        return partTypeUse;
    }

    public boolean hasValue(Widget parentWidget) {
        Widget widget = parentWidget.lookupWidget("part");
        if (widget.getValue() == null && widget.validate()) {
            return false;
        }
        return true;
    }

}
