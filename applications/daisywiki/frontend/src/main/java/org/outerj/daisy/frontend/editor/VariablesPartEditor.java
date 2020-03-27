/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend.editor;

import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.frontend.util.FormHelper;
import org.outerj.daisy.frontend.util.MultiMessage;
import org.outerj.daisy.frontend.util.TaggedMessage;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.cocoon.forms.formmodel.Form;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.avalon.framework.service.ServiceManager;
import org.xml.sax.*;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.io.StringReader;

public class VariablesPartEditor extends AbstractPartEditor {
    
    public VariablesPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
        // TODO Auto-generated constructor stub
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new VariablesPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }

    @Override
    protected String getDefinitionStylesheet() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_variables_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_variables_template.xml";
    }

    @Override
    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if (readonly) {
            widget.setState(WidgetState.DISABLED);
        }
        ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
        parentWidget.lookupWidget("variables").addValidator(new ConditionalValidator(validateOnSave, new PartRequiredValidator(partTypeUse.isRequired(), true)));
        parentWidget.lookupWidget("variables").addValidator(new VariablesValidator());

    }

    public void load(Document document) throws Exception {
        PartEditorHelper.load(widget.lookupWidget("variables"), partTypeUse.getPartType(), document);
    }

    public void save(Document document) throws Exception {
        PartEditorHelper.save(widget.lookupWidget("variables"), partTypeUse.getPartType(), document, "text/xml", null);
    }

    private static class VariablesValidator implements WidgetValidator {
        public boolean validate(Widget widget) {
            boolean success = true;
            String value = (String)widget.getValue();
            if (value != null) {
                List<Error> errors = new ArrayList<Error>();
                XMLReader reader;
                try {
                    reader = LocalSAXParserFactory.newXmlReader();
                } catch (Exception e) {
                    throw new RuntimeException("Unexpected error.", e);
                }
                InputSource is = new InputSource(new StringReader(value));
                reader.setContentHandler(new VariablesValidationHandler(errors));
                reader.setErrorHandler(new VariablesErrorHandler(errors));
                try {
                    reader.parse(is);
                } catch (SAXParseException e) {
                    // will be reported via error handler
                } catch (Exception e) {
                    errors.add(new Error(e.getMessage()));
                }

                if (errors.size() > 0) {
                    success = false;

                    MultiMessage message = new MultiMessage();
                    message.addMessage(new TaggedMessage("title", "Error parsing or validating XML:"));
                    for (int i = 0; i < errors.size(); i++) {
                        if (i > 6 && errors.size() > 9) {
                            message.addMessage(new TaggedMessage("error", "... " + (errors.size() - i) + " more errors"));
                            break;
                        }
                        Error error = errors.get(i);
                        message.addMessage(new TaggedMessage("error", error.toString()));
                    }
                    ((ValidationErrorAware)widget).setValidationError(new ValidationError(message));
                }
            }
            return success;
        }
    }

    private static class Error {
        private int line;
        private int column;
        private String message;

        public Error(int line, int column, String message) {
            this.line = line;
            this.column = column;
            this.message = message;
        }

        public Error(Locator locator, String message) {
            this.line = locator != null ? locator.getLineNumber() : -1;
            this.column = locator != null ? locator.getColumnNumber() : -1;
            this.message = message;
        }

        public Error(String message) {
            this.line = -1;
            this.column = -1;
            this.message = message;
        }

        public String toString() {
            return line + ":" + column + " - " + message;
        }
    }

    private static class VariablesValidationHandler implements ContentHandler {
        private List<Error> errors;
        private Locator locator;
        private int elementNesting = 0;
        private final String NS = "http://outerx.org/daisy/1.0#variables";

        public VariablesValidationHandler(List<Error> errors) {
            this.errors = errors;
        }
        
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            elementNesting++;

            if (elementNesting == 1) {
                if (!uri.equals(NS) || !localName.equals("variables")) {
                    errors.add(new Error(locator, "Root element should be named <variables> in namespace " + NS));
                }
            } else if (elementNesting == 2) {
                if (uri.equals(NS) && !localName.equals("variable")) {
                    errors.add(new Error(locator, "Expected a <variable> element in namespace " + NS));
                } else if (uri.equals(NS) && localName.equals("variable") && atts.getValue("name") == null) {
                    errors.add(new Error(locator, "Missing name attribute."));
                }
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            elementNesting--;
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
        }

        public void endPrefixMapping(String prefix) throws SAXException {
        }

        public void characters(char ch[], int start, int length) throws SAXException {
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        }

        public void processingInstruction(String target, String data) throws SAXException {
        }

        public void skippedEntity(String name) throws SAXException {
        }
    }

    private static class VariablesErrorHandler implements ErrorHandler {
        private List<Error> errors;

        public VariablesErrorHandler(List<Error> errors) {
            this.errors = errors;
        }

        public void warning(SAXParseException exception) throws SAXException {
        }

        public void error(SAXParseException exception) throws SAXException {
            errors.add(new Error(exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage()));
        }

        public void fatalError(SAXParseException exception) throws SAXException {
            errors.add(new Error(exception.getLineNumber(), exception.getColumnNumber(), exception.getMessage()));
        }
    }
}
