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
package org.outerj.daisy.frontend;

import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.Map;
import java.util.Stack;
import java.io.IOException;

/**
 * A transformer that counts the maximum number of cells on a row in a table
 * (taking into account colspan attributes) and inserts that information into
 * the SAX stream. This seemed a bit too annoying to do in XSL.
 */
public class TableHelperTransformer extends AbstractTransformer {
    private Stack<TableInfo> tableInfoStack;
    private TableInfo tableInfo;

    public void setup(SourceResolver sourceResolver, Map map, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        tableInfoStack = new Stack<TableInfo>();
        tableInfo = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attrs) throws SAXException {
        if (namespaceURI.equals("")) {
            if (localName.equals("table")) {
                if (tableInfo != null)
                    tableInfoStack.push(tableInfo);
                tableInfo = new TableInfo();
                tableInfo.columnWidths = attrs.getValue("column-widths");
                tableInfo.printColumnWidths = attrs.getValue("print-column-widths");
            } else if (tableInfo != null && localName.equals("tr")) {
                tableInfo.maxColumnsCurrentRow = 0;
                for (int i = 0; i < tableInfo.activeRowSpans.length; i++) {
                    int rowspan = tableInfo.activeRowSpans[i];
                    if (rowspan > 0) {
                        tableInfo.maxColumnsCurrentRow++;
                        tableInfo.activeRowSpans[i] = rowspan - 1;
                    }
                }
            } else if (tableInfo != null && localName.equals("td") || localName.equals("th")) {
                String rowspanAttr = attrs.getValue("rowspan");
                if (rowspanAttr != null && rowspanAttr.length() > 0) {
                    try {
                        int rowspan = Integer.parseInt(rowspanAttr);
                        if (rowspan > 1) {
                            tableInfo.activeRowSpansCount++;
                            assureActiveRowSpanArrayIsBigEnough();
                            tableInfo.activeRowSpans[tableInfo.activeRowSpansCount] = rowspan - 1;
                        }
                    } catch (NumberFormatException e) {
                        // invalid number in rowspan attribute: ignore
                    }
                }
                tableInfo.maxColumnsCurrentRow += 1;
            }
        }
        super.startElement(namespaceURI, localName, qName, attrs);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals("") && tableInfo != null) {
            if (localName.equals("table")) {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addCDATAAttribute("maxColumns", String.valueOf(tableInfo.maxColumns));

                super.startElement("", "computedInfo", "computedInfo", attrs);

                if (tableInfo.columnWidths != null) {
                    super.startElement("", "html", "html", new AttributesImpl());
                    generateColumnInfo(tableInfo.columnWidths, tableInfo.maxColumns);
                    super.endElement("", "html", "html");
                }

                if (tableInfo.printColumnWidths != null) {
                    super.startElement("", "print", "print", new AttributesImpl());
                    generateColumnInfo(tableInfo.printColumnWidths, tableInfo.maxColumns);
                    super.endElement("", "print", "print");
                }

                super.endElement("", "computedInfo", "computedInfo");
                
                tableInfo = null;
                if (tableInfoStack.size() > 0) {
                    tableInfo = tableInfoStack.pop();
                }
            } else if (localName.equals("tr")) {
                if (tableInfo.maxColumnsCurrentRow > tableInfo.maxColumns)
                    tableInfo.maxColumns = tableInfo.maxColumnsCurrentRow;
            }
        }
        super.endElement(namespaceURI, localName, qName);
    }

    private void generateColumnInfo(String columnInfo, int maxColumns) throws SAXException {
        String[] columnInfos = columnInfo.split(";", maxColumns);
        for (int i = 0; i < columnInfos.length; i++) {
            AttributesImpl printColAttrs = new AttributesImpl();
            if (columnInfos[i].trim().length() > 0)
            printColAttrs.addCDATAAttribute("width", columnInfos[i]);
            super.startElement("", "col", "col", printColAttrs);
            super.endElement("", "col", "col");
        }
        for (int i = columnInfos.length; i < maxColumns; i++) {
            super.startElement("", "col", "col", new AttributesImpl());
            super.endElement("", "col", "col");
        }
    }

    static class TableInfo {
        public int maxColumns = 0;
        public int maxColumnsCurrentRow = 0;
        // Note: the activeRowSpans array only grows, i.e. each new cell with a rowspan adds
        // an entry to this array. This is not really a problem unless one would have
        // a really huge table with lots of rowspans.
        public int[] activeRowSpans = new int[5];
        public int activeRowSpansCount = -1;
        public String columnWidths;
        public String printColumnWidths;
    }

    private void assureActiveRowSpanArrayIsBigEnough() {
        if (tableInfo.activeRowSpans.length <= tableInfo.activeRowSpansCount) {
            int[] newArray = new int[tableInfo.activeRowSpans.length + 10];
            System.arraycopy(tableInfo.activeRowSpans, 0, newArray, 0, tableInfo.activeRowSpans.length);
            tableInfo.activeRowSpans = newArray;
        }
    }
}
