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
package org.outerj.daisy.publisher.serverimpl.variables.impl;

import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VersionedData;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.xml.sax.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

public class VariablesParser {
    private static final String VARIABLES_PART_NAME = "VariablesData";
    private static final String NS = "http://outerx.org/daisy/1.0#variables";

    public static Variables parseVariables(VersionedData document) throws RepositoryException, SAXException, ParserConfigurationException, IOException {
        if (document.hasPart(VARIABLES_PART_NAME)) {
            Part part = document.getPart(VARIABLES_PART_NAME);

            InputStream is = null;
            try {
                is = part.getDataStream();
                XMLReader xmlReader = LocalSAXParserFactory.newXmlReader();
                VariablesHandler variablesHandler = new VariablesHandler();
                xmlReader.setContentHandler(variablesHandler);
                xmlReader.parse(new InputSource(is));
                return new VariablesImpl(variablesHandler.getVariables());
            } finally {
                if (is != null)
                    try { is.close(); } catch (IOException e) { /* ignore */ }
            }
        } else {
            return null;
        }
    }

    private static class VariablesHandler implements ContentHandler {
        private int elementNesting = 0;
        private String varName;
        private SaxBuffer varBuffer;
        private Map<String, SaxBuffer> variables = new HashMap<String, SaxBuffer>();

        public Map<String, SaxBuffer> getVariables() {
            return variables;
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            elementNesting++;

            if (elementNesting == 2) {
                if (uri.equals(NS) && localName.equals("variable")) {
                    varName = atts.getValue("name");
                    if (varName != null)
                        varBuffer = new SaxBuffer();
                }
            } else if (varBuffer != null) {
                varBuffer.startElement(uri, localName, qName, atts);
            }
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (elementNesting == 2) {
                if (varName != null) {
                    variables.put(varName, varBuffer);
                    varName = null;
                    varBuffer = null;
                }
            } else if (varBuffer != null) {
                varBuffer.endElement(uri, localName, qName);
            }

            elementNesting--;
        }

        public void setDocumentLocator(Locator locator) {
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            if (varBuffer != null)
                varBuffer.startPrefixMapping(prefix, uri);
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            if (varBuffer != null)
                varBuffer.endPrefixMapping(prefix);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (varBuffer != null)
                varBuffer.characters(ch, start, length);
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            if (varBuffer != null)
                varBuffer.ignorableWhitespace(ch, start, length);
        }

        public void processingInstruction(String target, String data) throws SAXException {
            if (varBuffer != null)
                varBuffer.processingInstruction(target, data);
        }

        public void skippedEntity(String name) throws SAXException {
            if (varBuffer != null)
                varBuffer.skippedEntity(name);
        }
    }
}
