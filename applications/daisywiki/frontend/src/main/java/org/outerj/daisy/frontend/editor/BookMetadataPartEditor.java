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

import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Map;
import java.util.Iterator;

/**
 * Part editor for the book metadata.
 */
public class BookMetadataPartEditor extends AbstractPartEditor {
    
    
    public BookMetadataPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new BookMetadataPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }
    
    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_bookmeta_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_bookmeta_template.xml";
    }

    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if (readonly) {
            widget.lookupWidget("editors").setState(WidgetState.DISABLED);
        }
        Field editMode = (Field)widget.lookupWidget("editmode");
        editMode.setValue("gui");
        editMode.addValueChangedListener(new EditModeListener(new EditModeListener.EditModeListenerConfig() {
            public String getXmlFieldPath() {
                return "editors/xml/metadataXml";
            }
            
            public String getGuiFieldPath() {
                return "editors/gui";
            }

            public String getXmlFromGuiEditor() {
                return getMetadataXml((Repeater)widget.lookupWidget(getGuiFieldPath() + "/metadata"));
            }

            public boolean loadGui(String xml) {
                Map metadata;
                try {
                    metadata = XMLPropertiesHelper.load(new InputSource(new StringReader(xml)), null, "metadata");
                } catch (Exception e) {
                    // TODO nicer error message?
                    ((ValidationErrorAware)widget.lookupWidget(getXmlFieldPath())).setValidationError(new ValidationError(e.toString(), false));
                    return false;
                }
                Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/metadata");
                repeater.clear();
                loadRepeater(repeater, metadata);
                return true;
            }

            public void clearGui() {
                Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/metadata");
                repeater.clear();
            }
        }));

        // set some defaults
        Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/metadata");
        Repeater.RepeaterRow row = repeater.addRow();
        row.getChild("name").setValue("title");
        row.getChild("value").setValue("enter book title here");
        row = repeater.addRow();
        row.getChild("name").setValue("locale");
        row.getChild("value").setValue("en");
    }

    public void load(Document document) throws Exception {
        Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/metadata");
        repeater.clear();

        byte[] data = document.getPart(partTypeUse.getPartType().getId()).getData();

        Map metadata = null;
        try {
            metadata = XMLPropertiesHelper.load(new ByteArrayInputStream(data), "metadata");
        } catch (Throwable e) {
            // ignore
        }

        if (metadata != null) {
            loadRepeater(repeater, metadata);
        } else {
            // parsing failed
            widget.setAttribute("ignore-editmode-change", Boolean.TRUE);
            widget.lookupWidget("editmode").setValue("xml");
            // TODO might want to do smarter encoding detection
            widget.lookupWidget("editors/xml/metadataXml").setValue(new String(data, "UTF-8"));
        }
    }

    public void save(Document document) throws Exception {
        Field editMode = (Field)widget.lookupWidget("editmode");
        if (editMode.getValue().equals("xml")) {
            PartEditorHelper.save(widget.lookupWidget("editors/xml/metadataXml"), partTypeUse.getPartType(), document, "text/xml", null);
        } else if (editMode.getValue().equals("gui")) {
            Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/metadata");
            String xml = getMetadataXml(repeater);
            PartEditorHelper.save(null, partTypeUse.getPartType(), document, "text/xml", xml.getBytes("UTF-8"));
        }
    }

    private static String getMetadataXml(Repeater repeater) {
        XmlObject xml = XmlObject.Factory.newInstance();
        XmlCursor cursor = xml.newCursor();
        cursor.toNextToken();
        cursor.beginElement("metadata");

        for (int i = 0; i < repeater.getSize(); i++) {
            Repeater.RepeaterRow row = repeater.getRow(i);
            String name = (String)row.getChild("name").getValue();
            if (name == null)
                name = "";
            String value = (String)row.getChild("value").getValue();
            if (value == null)
                value = "";
            cursor.beginElement("entry");
            cursor.insertAttributeWithValue("key", name);
            cursor.insertChars(value);
            cursor.toEndToken();
            cursor.toNextToken();
        }
        cursor.dispose();

        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setCharacterEncoding("UTF-8");
        return xml.xmlText(xmlOptions);
    }

    private static void loadRepeater(Repeater repeater, Map metadata) {
        Iterator metadataIt = metadata.entrySet().iterator();
        while (metadataIt.hasNext()) {
            Map.Entry entry = (Map.Entry)metadataIt.next();
            Repeater.RepeaterRow row = repeater.addRow();
            row.getChild("name").setValue(entry.getKey());
            row.getChild("value").setValue(entry.getValue());
        }
    }
}
