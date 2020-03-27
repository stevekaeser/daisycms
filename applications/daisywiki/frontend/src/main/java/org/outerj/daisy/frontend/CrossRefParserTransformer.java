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
import org.xml.sax.Locator;
import org.outerj.daisy.util.Constants;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.IOException;

public class CrossRefParserTransformer extends AbstractTransformer {
    private static final Pattern CROSS_REF_PATTERN = Pattern.compile("^([^:]+):(.+)$");
    private StringBuilder crossRefBuffer;
    private int nesting;
    private int crossRefNesting;

    public void setup(SourceResolver sourceResolver, Map map, String string, Parameters parameters) throws ProcessingException, SAXException, IOException {
        this.crossRefBuffer = null;
        this.nesting = 0;
        this.crossRefNesting = -1;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        nesting++;
        if (uri.equals("") && localName.equals("span") && "crossreference".equals(attributes.getValue("class")) && crossRefNesting == -1) {
            crossRefBuffer = new StringBuilder();
            crossRefNesting = nesting;
        } else if (crossRefNesting == -1) {
            super.startElement(uri, localName, qName, attributes);
        }
    }

    public void characters(char[] chars, int start, int length) throws SAXException {
        if (crossRefNesting != -1) {
            crossRefBuffer.append(chars, start, length);
        } else {
            super.characters(chars, start, length);
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (nesting == crossRefNesting) {
            String crossref = crossRefBuffer.toString().trim();
            Matcher crossRefMatcher = CROSS_REF_PATTERN.matcher(crossref);

            String crossRefType;
            String crossRefTarget = null;
            String crossRefBookTarget = null;

            if (crossRefMatcher.matches()) {
                crossRefType = crossRefMatcher.group(1);
                crossRefTarget = crossRefMatcher.group(2);
                Matcher targetMatcher = Constants.DAISY_LINK_PATTERN.matcher(crossRefTarget);
                if (targetMatcher.matches()) {
                    String targetDocId = targetMatcher.group(1);
                    String fragId = targetMatcher.group(7);
                    crossRefBookTarget = "#dsy" + targetDocId;
                    if (fragId != null)
                        crossRefBookTarget = crossRefBookTarget + "_" + fragId.substring(1);
                } else if (crossRefTarget.startsWith("#")) {
                    crossRefBookTarget = crossRefTarget;
                } else {
                    crossRefType = "invalid";
                    crossRefTarget = null;
                    crossRefBookTarget = null;
                }
            } else {
                // invalid crossref
                crossRefType = "invalid";
            }

            AttributesImpl crossRefAttrs = new AttributesImpl();
            crossRefAttrs.addCDATAAttribute("class", "crossreference");
            if (crossRefType != null)
                crossRefAttrs.addCDATAAttribute("crossRefType", crossRefType);
            if (crossRefTarget != null)
                crossRefAttrs.addCDATAAttribute("crossRefTarget", crossRefTarget);
            if (crossRefBookTarget != null)
                crossRefAttrs.addCDATAAttribute("crossRefBookTarget", crossRefBookTarget);
            super.startElement("", "span", "span", crossRefAttrs);
            super.characters(crossref.toCharArray(), 0, crossref.length());
            super.endElement("", "span", "span");

            crossRefNesting = -1;
            crossRefBuffer = null;
        } else if (crossRefNesting == -1) {
            super.endElement(uri, localName, qName);
        }
        nesting--;
    }

    public void setDocumentLocator(Locator locator) {
        if (crossRefNesting == -1)
            super.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        if (crossRefNesting == -1)
            super.startDocument();
    }

    public void endDocument() throws SAXException {
        if (crossRefNesting == -1)
            super.endDocument();
    }

    public void startPrefixMapping(String string, String string1) throws SAXException {
        if (crossRefNesting == -1)
            super.startPrefixMapping(string, string1);
    }

    public void endPrefixMapping(String string) throws SAXException {
        if (crossRefNesting == -1)
            super.endPrefixMapping(string);
    }

    public void ignorableWhitespace(char[] chars, int i, int i1) throws SAXException {
        if (crossRefNesting == -1)
            super.ignorableWhitespace(chars, i, i1);
    }

    public void processingInstruction(String string, String string1) throws SAXException {
        if (crossRefNesting == -1)
            super.processingInstruction(string, string1);
    }

    public void skippedEntity(String string) throws SAXException {
        if (crossRefNesting == -1)
            super.skippedEntity(string);
    }

    public void startDTD(String string, String string1, String string2) throws SAXException {
        if (crossRefNesting == -1)
            super.startDTD(string, string1, string2);
    }

    public void endDTD() throws SAXException {
        if (crossRefNesting == -1)
            super.endDTD();
    }

    public void startEntity(String string) throws SAXException {
        if (crossRefNesting == -1)
            super.startEntity(string);
    }

    public void endEntity(String string) throws SAXException {
        if (crossRefNesting == -1)
            super.endEntity(string);
    }

    public void startCDATA() throws SAXException {
        if (crossRefNesting == -1)
            super.startCDATA();
    }

    public void endCDATA() throws SAXException {
        if (crossRefNesting == -1)
            super.endCDATA();
    }

    public void comment(char[] chars, int i, int i1) throws SAXException {
        if (crossRefNesting == -1)
            super.comment(chars, i, i1);
    }
}
