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
import org.apache.xmlbeans.XmlObject;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An XMLizable wrapper around an XMLBeans XmlObject.
 */
public class XmlObjectXMLizable implements XMLizable {
    private XmlObject xmlObject;
    private boolean dropDocumentElement;

    /**
     * @param xmlObject null is allowed
     */
    public XmlObjectXMLizable(XmlObject xmlObject) {
        this.xmlObject = xmlObject;
    }

    public XmlObjectXMLizable(XmlObject xmlObject, boolean dropDocumentElement) {
        this.xmlObject = xmlObject;
        this.dropDocumentElement = dropDocumentElement;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        if (xmlObject != null) {
            if (dropDocumentElement) {
                IncludeXMLConsumer consumer = new IncludeXMLConsumer(contentHandler);
                consumer.setIgnoreRootElement(true);
                contentHandler = consumer;
            }
            xmlObject.save(contentHandler, null);
        }
    }
}
