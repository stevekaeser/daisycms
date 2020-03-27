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

import java.io.IOException;
import java.util.Map;
import java.util.Stack;

import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.FormsConstants;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.i18n.Bundle;
import org.apache.cocoon.i18n.BundleFactory;
import org.apache.cocoon.template.JXTemplateGenerator;
import org.apache.cocoon.transformation.AbstractSAXTransformer;
import org.outerj.daisy.frontend.FrontEndContext;
import org.outerj.daisy.frontend.PageContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class InlineEditFormTransformer extends AbstractSAXTransformer {

    private FrontEndContext frontEndContext;
    private Form form;
    private Map flowObjectModel;
    private int formLevel;
    private Stack<String> actions = new Stack<String>();
    
    public InlineEditFormTransformer() {
        super();
        defaultNamespaceURI = InlineEditorApple.INLINEEDITOR_NAMESPACE;
    }
    
    @Override
    public void recycle() {
        super.recycle();
        form = null;
        frontEndContext = null;
        flowObjectModel = null;
    }



    @Override
    public void setupTransforming() throws IOException, ProcessingException,
            SAXException {
        super.setupTransforming();
        this.frontEndContext = FrontEndContext.get(ObjectModelHelper.getRequest(objectModel));
        this.form = (Form)((Request)ObjectModelHelper.getRequest(objectModel)).getAttribute("CocoonFormsInstance");
        this.flowObjectModel = (Map)FlowHelper.getContextObject(objectModel);
        this.actions.clear();
        formLevel = 0;
    }
    
    @Override
    public void startElement(String uri, String name, String raw,
            Attributes attr) throws SAXException {
        // TODO: suppress emitting ie: tags and elements, because it makes dojo try to load 'ie.js', 'ie/__manifest__.js' and what not :-(
        if (ignoreEventsCount > 0) {
            ignoreEventsCount++;
        }
        if (formLevel > 0) {
            formLevel++;
        }
        for (int i=0; i<attr.getLength(); i++) {
            if (attr.getURI(i).equals(InlineEditorApple.INLINEEDITOR_NAMESPACE)) {
                if (attr.getLocalName(i).equals("form") && form != null) {
                    AttributesImpl attr2 = new AttributesImpl();
                    attr2.addAttribute(FormsConstants.TEMPLATE_NS, "id", "ft:id", "CDATA", attr.getValue(i));
                    attr2.addAttribute(FormsConstants.TEMPLATE_NS, "action", "ft:action", "CDATA", (String)form.getAttribute("editPath"));
                    attr2.addAttribute(FormsConstants.TEMPLATE_NS, "method", "ft:method", "CDATA", "POST");
                    attr2.addAttribute(FormsConstants.TEMPLATE_NS, "enctype", "ft:enctype", "CDATA", "multipart/form-data");
                    startPrefixMapping("ft", FormsConstants.TEMPLATE_NS);
                    super.startElement(FormsConstants.TEMPLATE_NS, "form-template", "ft:form-template", attr2);
                    if (formLevel > 0) {
                        throw new SAXException("nested @ie:form elements are not supported");
                    }
                    formLevel++;
                } else if (attr.getLocalName(i).equals("name") && form != null) {
                    AttributesImpl attr2 = new AttributesImpl();
                    attr2.addAttribute(FormsConstants.TEMPLATE_NS, "id", "ft:id", "CDATA", "documentName");
                    super.startElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget", attr2);
                    super.endElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget");
                    ignoreEventsCount++;
                } else if (attr.getLocalName(i).equals("field") && form != null) {
                    String fieldName = attr.getValue(i);
                    FieldEditor fieldEditor = ((Map<String,FieldEditor>)form.getAttribute("fieldEditorsByName")).get(fieldName);
                    if (fieldEditor == null) {
                        throw new SAXException("No FieldEditor found for field named " + fieldName);
                    }
                    try {
                        fieldEditor.generateFormTemplateFragment(contentHandler, form.getLocale(), "inlineEditor", manager);
                    } catch (Exception e) {
                        throw new RuntimeException("Error injecting form template fragment", e);
                    }
                    ignoreEventsCount++;
                } else if (attr.getLocalName(i).equals("part") && form != null) {
                    String partName = attr.getValue(i);
                    PartEditor partEditor = ((Map<String,PartEditor>)form.getAttribute("partEditorsByName")).get(partName);
                    if (partEditor == null) {
                        throw new SAXException("No PartEditor found for part named " + partName);
                    }
                    
                    startPrefixMapping("jx", JXTemplateGenerator.NS);
                    AttributesImpl widgetAttr = new AttributesImpl();
                    widgetAttr.addAttribute("", "id", "id", "CDATA", "part_" + partEditor.getPartTypeUse().getPartType().getId());
                    super.startElement(FormsConstants.TEMPLATE_NS, "group", "ft:group", widgetAttr);
                    AttributesImpl importAttr = new AttributesImpl();
                    importAttr.addAttribute("", "uri", "uri", "CDATA", partEditor.getFormTemplate());
                    super.startElement(JXTemplateGenerator.NS, "import", "jx:import", importAttr);
                    super.endElement(JXTemplateGenerator.NS, "import", "jx:import");
                    super.endElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget");
                    endPrefixMapping("jx");
                    ignoreEventsCount++;
                } else if (attr.getLocalName(i).equals("mode")) {
                    if (form != null && attr.getValue(i).equals("view")) {
                        ignoreEventsCount++;
                    } else if (form == null && attr.getValue(i).equals("edit")) {
                        ignoreEventsCount++;
                    }
                    AttributesImpl attr2 = new AttributesImpl(attr);
                    attr2.removeAttribute(i);
                    attr = attr2;
                }
            }
        }
        
        if (uri.equals(InlineEditorApple.INLINEEDITOR_NAMESPACE)) {
            super.startElement(uri, name, raw, attr);
        } else {
            super.startElement(uri, name, raw, filterAttributes(attr));
        }
    }
    
    private Attributes filterAttributes(Attributes attr) {
        AttributesImpl result = new AttributesImpl();
        for (int i=0; i<attr.getLength(); i++) {
            if (attr.getURI(i).equals(InlineEditorApple.INLINEEDITOR_NAMESPACE))
                continue;
            result.addAttribute(attr.getURI(i), attr.getLocalName(i), attr.getQName(i), attr.getType(i), attr.getValue(i));
        }
        return result;
    }

    @Override
    public void startTransformingElement(String uri, String name, String raw,
            Attributes attr) throws ProcessingException, IOException,
            SAXException {
        if (name.equals("action")) {
            String type = attr.getValue("type");
            actions.push(type);
            PageContext pageContext = ((PageContext)flowObjectModel.get("pageContext")); 
            String mountPoint = pageContext.getMountPoint();
            String site = pageContext.getSiteConf().getName();
            String activePath = (String)flowObjectModel.get("activePath");
            String path = mountPoint + "/" + site + "/" + activePath;
            String variantParams = (String)flowObjectModel.get("variantParams");
            if (form == null && type.equals("edit")) {
                sendPostButton(path + "/inline-edit?" + variantParams, i18n("doclayout.edit"));
            } else if (form != null && type.equals("save")) {
                AttributesImpl attr2 = new AttributesImpl();
                attr2.addAttribute("", "id", "id", "CDATA", "save");
                super.startTransformingElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget", attr2);
            } else if (form != null && type.equals("cancel")) {
                AttributesImpl attr2 = new AttributesImpl();
                attr2.addAttribute("", "id", "id", "CDATA", "cancel");
                attr2.addAttribute("", "onclick", "onclick", "CDATA", "if (confirm(i18n('editdoc.confirm-cancel'))) { return true; } return false;");
                super.startTransformingElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget", attr2);
            }
        } else {
            throw new SAXException("Unknown start-element: " + raw);
        }
    }
    
    @Override
    public void endTransformingElement(String uri, String name, String raw)
            throws ProcessingException, IOException, SAXException {
        if (name.equals("action")) {
            String type = actions.pop();
            if (form == null && type.equals("edit")) {
                // nothing to do
            } else if (form != null && type.equals("save")){
                super.endTransformingElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget");
            } else if (form != null && type.equals("cancel")){
                super.endTransformingElement(FormsConstants.TEMPLATE_NS, "widget", "ft:widget");
            }
        } else {
            throw new SAXException("Unknown end-element: " + raw);
        }
    }

    private void sendPostButton(String href, String label) throws ProcessingException, IOException, SAXException {
        AttributesImpl formAttr = new AttributesImpl();
        formAttr.addAttribute("", "action", "action", "CDATA", href);
        formAttr.addAttribute("", "method", "method", "CDATA", "POST");
        AttributesImpl submitAttr = new AttributesImpl();
        submitAttr.addAttribute("", "type", "type", "CDATA", "submit");
        submitAttr.addAttribute("", "value", "value", "CDATA", label);

        super.startTransformingElement("", "form", "form", formAttr);
        super.startTransformingElement("", "input", "input", submitAttr);
        super.endTransformingElement("", "input", "input");
        super.endTransformingElement("", "form", "form");
    }
    
    private String i18n(String key) {
        Bundle bundle = null;
        BundleFactory bundleFactory = null;
        try {
            bundleFactory = (BundleFactory)manager.lookup(BundleFactory.ROLE);
            bundle = bundleFactory.select("resources/i18n", "messages", FrontEndContext.get(request).getLocaleAsString());
            return bundle.getString(key);
        } catch (Exception e) {
            getLogger().warn("Could not look up i18n key " + key, e);
            return "???" + key + "???";
        } finally {
            if (bundleFactory != null) {
                if (bundle != null)
                    bundleFactory.release(bundle);
                manager.release(bundleFactory);
            }
        }

    }

    @Override
    public void endElement(String uri, String name, String raw)
            throws SAXException {
        super.endElement(uri, name, raw);

        if (ignoreEventsCount > 0 ) {
            ignoreEventsCount--;
        }
        if (formLevel > 0) {
            formLevel--;
            if (formLevel == 0) {
                super.endElement(FormsConstants.TEMPLATE_NS, "form-template", "ft:form-template");
                super.endPrefixMapping("ft");
            }
        }
    }
    
}
