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
package org.outerj.daisy.frontend.util;

import org.apache.excalibur.xml.sax.XMLizable;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class TaggedMessage implements XMLizable {
    private String elementLocalName;
    private String message;

    public TaggedMessage(String elementLocalName, String message) {
        this.elementLocalName = elementLocalName;
        this.message = message;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", elementLocalName, elementLocalName, new AttributesImpl());
        contentHandler.characters(message.toCharArray(), 0, message.length());
        contentHandler.endElement("", elementLocalName, elementLocalName);
    }
}
