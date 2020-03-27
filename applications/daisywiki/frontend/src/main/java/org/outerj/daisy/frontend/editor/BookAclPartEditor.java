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

import java.io.StringReader;
import java.util.Map;

import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.frontend.BookAclEditorApple;
import org.outerj.daisy.books.store.BookAcl;
import org.outerj.daisy.books.store.BookAclBuilder;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.schema.PartTypeUse;

public class BookAclPartEditor extends AbstractPartEditor {

    public BookAclPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new BookAclPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }
    
    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_bookacl_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_bookacl_template.xml";
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
                return "editors/xml/xmlText";
            }
            
            public String getGuiFieldPath() {
                return "editors/gui/entries";
            }

            public String getXmlFromGuiEditor() {
                return getBookAclXml(widget);
            }

            public boolean loadGui(String xml) {
                BookAcl bookAcl;
                try {
                    bookAcl = BookAclBuilder.build(new StringReader(xml));
                } catch (Throwable e) {
                    // TODO nicer error message?
                    ((ValidationErrorAware)widget.getForm().lookupWidget(getXmlFieldPath())).setValidationError(new ValidationError(e.toString(), false));
                    return false;
                }
                try {
                    BookAclEditorApple.load(widget, bookAcl);
                    BookAclEditorApple.annotateAclSubjectValues(widget, documentEditorContext.getRepository());
                } catch (Throwable e) {
                    // TODO nicer error message?
                    ((ValidationErrorAware)widget.getForm().lookupWidget(getXmlFieldPath())).setValidationError(new ValidationError(e.toString(), false));
                    return false;
                }
                return true;
            }

            public void clearGui() {
                Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/entries");
                repeater.clear();
            }
        }));
    }

    private String getBookAclXml(Widget widget) {
        BookAcl bookAcl = BookAclEditorApple.getBookAcl(widget);
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setCharacterEncoding("UTF-8");
        return bookAcl.getXml().xmlText(xmlOptions);
    }

    public void save(Document document) throws Exception {
        Field editMode = (Field)widget.lookupWidget("editmode");
        if (editMode.getValue().equals("xml")) {
            PartEditorHelper.save(widget.lookupWidget("editors/xml/xmlText"), partTypeUse.getPartType(), document, "text/xml", null);
        } else if (editMode.getValue().equals("gui")) {
            String xml = getBookAclXml(widget);
            PartEditorHelper.save(null, partTypeUse.getPartType(), document, "text/xml", xml.getBytes("UTF-8"));
        }
    }

    public void load(Document document) throws Exception {
        Part part = document.getPart(partTypeUse.getPartType().getId());
        byte[] data = part.getData();

        BookAcl bookAcl = null;
        try {
            // Note: BookAclBuilder closes the input stream for us
            bookAcl = BookAclBuilder.build(part.getDataStream());
        } catch (Throwable e) {
            // ignore
        }

        if (bookAcl != null) {
            try {
                BookAclEditorApple.load(widget, bookAcl);
                BookAclEditorApple.annotateAclSubjectValues(widget, documentEditorContext.getRepository());
            } catch (Throwable e) {
                // TODO nicer error message?
                ((ValidationErrorAware)widget.lookupWidget("editors/xml/xmlText")).setValidationError(new ValidationError(e.toString(), false));
                bookAcl = null;
            }
        }

        if (bookAcl == null) {
            // parsing failed
            widget.setAttribute("ignore-editmode-change", Boolean.TRUE);
            widget.lookupWidget("editmode").setValue("xml");
            // TODO might want to do smarter encoding detection
            widget.lookupWidget("editors/xml/xmlText").setValue(new String(data, "UTF-8"));
        }
    }

}
