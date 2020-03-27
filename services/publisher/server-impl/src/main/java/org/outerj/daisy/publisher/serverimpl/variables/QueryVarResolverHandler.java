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
package org.outerj.daisy.publisher.serverimpl.variables;

import org.outerj.daisy.publisher.serverimpl.AbstractHandler;
import org.outerj.daisy.util.Constants;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A SAX handler which will resolve variables in document name identifiers
 * in query results.
 */
public class QueryVarResolverHandler extends AbstractHandler {
    private static final String NS = Constants.DAISY_NAMESPACE;
    private Variables variables;
    private boolean[] nameColumns = new boolean[0];
    private int elementNesting = 0;
    private int rowElementNesting = -1;
    private int columnPosition;
    private StringBuilder nameBuffer = new StringBuilder();
    private boolean inNameColumn = false;

    public QueryVarResolverHandler(ContentHandler consumer, Variables variables) {
        super(consumer);
        this.variables = variables;
    }


    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        elementNesting++;
        if (uri.equals(NS)) {
            if (localName.equals("title")) {
                augmentTitleCount();
                String name = atts.getValue("name");
                // right now only simple name columns are recognized... might tokenize the
                // expression to check if it contains a 'name' identifier
                if (name.equals("name")) {
                    setNameColumn();
                }
            } else if (localName.equals("row")) {
                rowElementNesting = elementNesting;
                columnPosition = 0;
            } else {
                // columns == the immediate children of the row
                if (rowElementNesting != -1 && elementNesting == rowElementNesting + 1) {
                    if (isNameColumn(columnPosition) && localName.equals("value")) {
                        inNameColumn = true;
                        nameBuffer.setLength(0);
                    }
                    columnPosition++;
                }
            }
        }
        super.startElement(uri, localName, qName, atts);
    }

    private void augmentTitleCount() {
        boolean[] oldNameValues = nameColumns;
        nameColumns = new boolean[nameColumns.length + 1];
        System.arraycopy(oldNameValues, 0, nameColumns, 0, oldNameValues.length);
    }

    private void setNameColumn() {
        nameColumns[nameColumns.length - 1] = true;
    }

    private boolean isNameColumn(int index) {
        return nameColumns[index];
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals(NS)) {
            if (inNameColumn) {
                if (elementNesting != rowElementNesting + 1) // sanity check
                    throw new RuntimeException("Error: nested elements in d:value in search results. Should not occur.");
                inNameColumn = false;
                String name = nameBuffer.toString();
                String newName = VariablesHelper.substituteVariables(name, variables);
                if (newName != null) {
                    characters(newName.toCharArray(), 0, newName.length());
                } else {
                    characters(name.toCharArray(), 0, name.length());
                }
            } else if (localName.equals("row")) {
                rowElementNesting = -1;
            }
        }
        elementNesting--;
        super.endElement(namespaceURI, localName, qName);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inNameColumn) {
            nameBuffer.append(ch, start, length);
        } else {
            super.characters(ch, start, length);
        }
    }
}
