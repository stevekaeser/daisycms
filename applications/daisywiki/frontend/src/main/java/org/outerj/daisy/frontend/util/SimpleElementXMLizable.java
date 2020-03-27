/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

public class SimpleElementXMLizable implements XMLizable {
    private final String element;
    private final String content;

    public SimpleElementXMLizable(String element, String content) {
        this.element = element;
        this.content = content;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", element, element, new AttributesImpl());
        contentHandler.characters(content.toCharArray(), 0, content.length());
        contentHandler.endElement("", element, element);
    }
}
