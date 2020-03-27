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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.publisher.serverimpl.variables.VariablesHelper;
import org.outerj.daisy.publisher.serverimpl.variables.EmptyVariables;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.util.Constants;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.helpers.AttributesImpl;

import java.util.*;
import java.text.MessageFormat;


public class VariablesProcessor implements ContentHandler {
    private ContentHandler consumer;
    private Variables variables;
    private Locale locale;

    private int elementNesting = 0;
    private int variableElementNesting = -1;
    private StringBuilder variableBuffer = new StringBuilder();

    private Map<String, Set<String>> variableElementAttr; // specifies which attributes of which elements need substitution applied

    public VariablesProcessor(ContentHandler consumer, PublisherContext publisherContext) {
        this.consumer = consumer;
        this.variables = publisherContext.getVariables();
        if (this.variables == null)
            this.variables = EmptyVariables.INSTANCE;
        this.variableElementAttr = publisherContext.getVariablesConfig().getVariableElementAttr();
        this.locale = publisherContext.getLocale();
    }


    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        elementNesting++;

        if (variableElementNesting != -1) {
            // ignore
        } else if (uri.equals("") && localName.equals("span") && "variable".equals(atts.getValue("class"))) {
            variableElementNesting = elementNesting;
        } else {
            if (uri.equals(Constants.DAISY_NAMESPACE) && localName.equals("document")) {
                // substitute in document name
                String name = atts.getValue("name");
                if (name != null) {
                    String result = VariablesHelper.substituteVariables(name, variables);
                    if (result != null) {
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.setAttribute(newAttrs.getIndex("name"), "", "name", "name", "CDATA", result);
                        atts = newAttrs;
                    }
                }
            } else if (uri.equals("") && isVariableResolvingElement(localName)) {
                // substitute in attributes
                Set<String> attrNames = variableElementAttr != null ? variableElementAttr.get(localName) : null;
                AttributesImpl newAttrs = null;
                for (int i = 0; i < atts.getLength(); i++) {
                    if (variableElementAttr == null || attrNames.contains(atts.getLocalName(i))) {
                        String value = atts.getValue(i);
                        if (value != null) {
                            String result = VariablesHelper.substituteVariables(value, variables);
                            if (result != null) {
                                if (newAttrs == null) // only clone attrs if necessary
                                    newAttrs = new AttributesImpl(atts);
                                newAttrs.setValue(i, result);
                            }
                        }
                    }
                }
                if (newAttrs != null)
                    atts = newAttrs;
            }
            consumer.startElement(uri, localName, qName, atts);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (variableElementNesting == elementNesting) {
            String varName = variableBuffer.toString().trim();
            if (varName.length() > 0) {
                SaxBuffer varValue = variables.resolve(varName);
                if (varValue != null)
                    varValue.toSAX(consumer);
                else
                    alertMissingVariable(varName);
            }
            variableBuffer.setLength(0);
            variableElementNesting = -1;
        } else if (variableElementNesting != -1) {
            // skip
        } else {
            consumer.endElement(uri, localName, qName);
        }

        elementNesting--;
    }

    private boolean isVariableResolvingElement(String name) {
        return variableElementAttr == null || variableElementAttr.containsKey(name);
    }

    private void alertMissingVariable(String varName) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "class", "class", "CDATA", "daisy-unresolved-variable");
        consumer.startElement("", "span", "span", attrs);

        ResourceBundle bundle = ResourceBundle.getBundle("org/outerj/daisy/publisher/serverimpl/messages", locale);
        String message = bundle.getString("unresolved-variable");
        message = MessageFormat.format(message, varName);

        consumer.characters(message.toCharArray(), 0, message.length());
        consumer.endElement("", "span", "span");
    }

    public void setDocumentLocator(Locator locator) {
        if (variableElementNesting == -1)
            consumer.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        if (variableElementNesting == -1)
            consumer.startDocument();
    }

    public void endDocument() throws SAXException {
        if (variableElementNesting == -1)
            consumer.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (variableElementNesting == -1)
            consumer.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (variableElementNesting == -1)
            consumer.endPrefixMapping(prefix);
    }


    public void characters(char ch[], int start, int length) throws SAXException {
        if (variableElementNesting == -1)
            consumer.characters(ch, start, length);
        else
            variableBuffer.append(ch, start, length);
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (variableElementNesting == -1)
            consumer.ignorableWhitespace(ch, start, length);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (variableElementNesting == -1)
            consumer.processingInstruction(target, data);
    }

    public void skippedEntity(String name) throws SAXException {
        if (variableElementNesting == -1)
            consumer.skippedEntity(name);
    }

}
