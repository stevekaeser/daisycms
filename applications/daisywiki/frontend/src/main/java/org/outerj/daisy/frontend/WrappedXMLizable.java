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

import org.apache.excalibur.xml.sax.XMLizable;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class WrappedXMLizable implements XMLizable {
    
    private String element;
    private Attributes atts = new AttributesImpl();
    private XMLizable wrappedXMLizable;
    
    public WrappedXMLizable(String element, XMLizable wrappedXMLizable) {
        this.element = element;
        this.wrappedXMLizable = wrappedXMLizable;
    }

    public WrappedXMLizable(String element, Attributes atts, XMLizable wrappedXMLizable) {
        this(element, wrappedXMLizable);
        this.atts = atts;
    }

    public void toSAX(ContentHandler handler) throws SAXException {
        handler.startElement("", element, element, atts);
        wrappedXMLizable.toSAX(handler);
        handler.endElement("", element, element);
    }

    
}
