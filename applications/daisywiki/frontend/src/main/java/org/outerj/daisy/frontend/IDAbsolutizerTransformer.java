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
import org.apache.avalon.framework.parameters.Parameters;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.util.Constants;

import java.util.Map;
import java.io.IOException;

/**
 * This transform prefixes all element IDs with "dsy" + documentId + "_", in order
 * to have unique names when combining several documents on one page, and to avoid
 * conflicts with any other IDs that might appear on a HTML page (outside of the
 * document content).
 *
 * <p>This goes together with the {@link DaisyLinkTransformer} which adjust
 * fragment identifiers in daisy links.
 */
public class IDAbsolutizerTransformer extends AbstractTransformer {
    private boolean inPart;
    private String documentId;

    public void setup(SourceResolver sourceResolver, Map map, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        this.inPart = false;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE)) {
            if (localName.equals("part")) {
                inPart = true;
            } else if (localName.equals("document")) {
                documentId = attributes.getValue("id");
            }
        }

        int index;
        if (inPart && namespaceURI.equals("") && (index = attributes.getIndex("id")) != -1) {
            AttributesImpl newAttrs = new AttributesImpl(attributes);
            String id = newAttrs.getValue(index);
            newAttrs.setValue(index, "dsy" + documentId + "_" + id);
            attributes = newAttrs;
        }

        super.startElement(namespaceURI, localName, qName, attributes);
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals(Constants.DAISY_NAMESPACE) && localName.equals("part")) {
            inPart = false;
        }
        super.endElement(namespaceURI, localName, qName);
    }
}
