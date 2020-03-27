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
package org.outerj.daisy.books.publisher.impl.cocooncomponents;

import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.ProcessingException;
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.util.Constants;

import java.util.regex.Matcher;
import java.util.Map;
import java.io.IOException;

public class BookLinkTransformer extends AbstractTransformer {
    private String documentId;

    public void setup(SourceResolver sourceResolver, Map map, String string, Parameters parameters) throws ProcessingException, SAXException, IOException {
        this.documentId = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE)) {
            if (localName.equals("document")) {
                documentId = atts.getValue("id");
            } else if (documentId != null && localName.equals("link")) {
                atts = translateLink(atts, "target");
            }
        }
        if (documentId != null && namespaceURI.equals("")) {
            if (localName.equals("a")) {
                atts = translateLink(atts, "href");
            } else if (localName.equals("span") && "crossreference".equals(atts.getValue("class"))) {
                // This assumes cross references have been handled by the CrossRefParserTransformer.
                atts = translateLink(atts, "crossRefBookTarget");                    
            }
        }
        super.startElement(namespaceURI, localName, qName, atts);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE) && localName.equals("document"))
            documentId = null;
        super.endElement(namespaceURI, localName, qName);
    }

    Attributes translateLink(Attributes attributes, String linkAttrName) {
        String href = attributes.getValue(linkAttrName);
        if (href != null) {
            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(href);
            if (matcher.matches()) {
                // Note: branch, language and version information of the links is completely ignored.
                // It assumed that a book will only contain one variant/version of a document.
                String newHref;
                String documentId = matcher.group(1);

                String fragmentIdentifier = matcher.group(7);
                if (fragmentIdentifier != null && !fragmentIdentifier.startsWith("#dsy")) {
                    newHref = "#dsy" + documentId + "_" + fragmentIdentifier.substring(1);
                } else if (fragmentIdentifier != null) {
                    newHref = fragmentIdentifier;
                } else {
                    newHref = "#dsy" + documentId;
                }

                AttributesImpl newAttributes = new AttributesImpl(attributes);
                newAttributes.setAttribute(newAttributes.getIndex("", linkAttrName), "", linkAttrName, linkAttrName, "CDATA", newHref);
                attributes = newAttributes;
            } else if (href.startsWith("#") && !href.startsWith("#dsy")) {
                String newHref = "#dsy" + documentId + "_" + href.substring(1);
                AttributesImpl newAttributes = new AttributesImpl(attributes);
                newAttributes.setAttribute(newAttributes.getIndex("", linkAttrName), "", linkAttrName, linkAttrName, "CDATA", newHref);
                attributes = newAttributes;
            }
        }
        return attributes;
    }
}
