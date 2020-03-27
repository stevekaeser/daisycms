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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Messages;
import org.apache.cocoon.forms.formmodel.Repeater;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.util.I18nMessage;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.frontend.PublicationTypesFormHelper;
import org.outerj.daisy.books.publisher.PublicationSpec;
import org.outerj.daisy.books.publisher.PublicationSpecBuilder;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerx.daisy.x10Bookpubspecs.PublicationSpecsDocument;

public class BookPublicationTypesPartEditor extends AbstractPartEditor {

    public BookPublicationTypesPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new BookPublicationTypesPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }

    public void init(final Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if  (readonly) {
            widget.lookupWidget("editors").setState(WidgetState.DISABLED);
        }
        // Code duplicated from selectpublicationtypes_definition.xml
        widget.addValidator(new WidgetValidator() {
            public boolean validate(Widget widget) {
                Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/publications");
                  if (repeater.getSize() == 0) {
                    if (widget.getAttribute("publications-required") != null) {
                      I18nMessage message = new I18nMessage("pubtypes.need-at-least-one");
                      ((Messages)widget.lookupWidget("editors/gui/messages")).addMessage(message);
                      return false;
                    }
                  } else {
                    // check no two publications have the same output name
                    Set<String> names = new HashSet<String>();
                    for (int i=0; i < repeater.getSize(); i++) {
                      Field outputNameWidget = (Field)repeater.getRow(i).getChild("outputName");
                      String outputName = (String)outputNameWidget.getValue();
                      if (names.contains(outputName)) {
                        I18nMessage message = new I18nMessage("pubtypes.duplicate-output-name");
                        outputNameWidget.setValidationError(new ValidationError(message));
                        return false;
                      }
                      names.add(outputName);
                    }
                  }
                  return true;
            }
        });

        Field editMode = (Field)widget.lookupWidget("editmode");
        editMode.setValue("gui");
        editMode.addValueChangedListener(new EditModeListener(new EditModeListener.EditModeListenerConfig() {
            public String getXmlFieldPath() {
                return "editors/xml/xmlText";
            }
            
            public String getGuiFieldPath() {
                return "editors/gui/publications";
            }

            public String getXmlFromGuiEditor() {
                return getPublicationTypesXml(widget);
            }

            public boolean loadGui(String xml) {
                PublicationSpec[] specs;
                try {
                    specs = PublicationSpecBuilder.build(new StringReader(xml));
                } catch (Throwable e) {
                    // TODO nicer error message?
                    ((ValidationErrorAware)widget.lookupWidget(getXmlFieldPath())).setValidationError(new ValidationError(e.toString(), false));
                    return false;
                }
                try {
                    PublicationTypesFormHelper.loadPublicationSpecs(widget, specs, documentEditorContext.getServiceManager());
                } catch (Throwable e) {
                    // TODO nicer error message?
                    ((ValidationErrorAware)widget.lookupWidget(getXmlFieldPath())).setValidationError(new ValidationError(e.toString(), false));
                    return false;
                }
                return true;
            }

            public void clearGui() {
                Repeater repeater = (Repeater)widget.lookupWidget("editors/gui/publications");
                repeater.clear();
            }
        }));

        try {
            PublicationTypesFormHelper.initPublicationsForm(widget, documentEditorContext.getServiceManager());
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize publication widget", e);
        }
    }

    public void load(Document document) throws Exception {
        Part part = document.getPart(partTypeUse.getPartType().getId());
        byte[] data = part.getData();

        PublicationSpec[] specs = null;
        try {
            specs = PublicationSpecBuilder.build(part.getDataStream());
        } catch (Throwable e) {
            // ignore
        }

        if (specs != null) {
            try {
                PublicationTypesFormHelper.loadPublicationSpecs(widget, specs, documentEditorContext.getServiceManager());
            } catch (Throwable e) {
                // TODO nicer error message?
                ((ValidationErrorAware)widget.lookupWidget("editors/xml/xmlText")).setValidationError(new ValidationError(e.toString(), false));
                specs = null;
            }
        }

        if (specs == null) {
            // parsing failed
            widget.setAttribute("ignore-editmode-change", Boolean.TRUE);
            widget.lookupWidget("editmode").setValue("xml");
            // TODO might want to do smarter encoding detection
            widget.lookupWidget("editors/xml/xmlText").setValue(new String(data, "UTF-8"));
        }
    }

    private String getPublicationTypesXml(Widget widget) {
        PublicationSpecsDocument publicationSpecsDocument = PublicationTypesFormHelper.getXml(widget);
        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        xmlOptions.setCharacterEncoding("UTF-8");
        return publicationSpecsDocument.xmlText(xmlOptions);
    }

    public void save(Document document) throws Exception {
        Field editMode = (Field)widget.lookupWidget("editmode");
        if (editMode.getValue().equals("xml")) {
            PartEditorHelper.save(widget.lookupWidget("editors/xml/xmlText"), partTypeUse.getPartType(), document, "text/xml", null);
        } else if (editMode.getValue().equals("gui")) {
            String xml = getPublicationTypesXml(widget);
            PartEditorHelper.save(null, partTypeUse.getPartType(), document, "text/xml", xml.getBytes("UTF-8"));
        }
    }

    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_bookpublicationtypes_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_bookpublicationtypes_template.xml";
    }

}
