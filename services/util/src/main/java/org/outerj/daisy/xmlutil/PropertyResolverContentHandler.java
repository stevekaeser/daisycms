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
package org.outerj.daisy.xmlutil;

import org.outerj.daisy.configutil.PropertyResolver;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;

/**
 * Class which replaces ${expressions} in attributes and text nodes 
 * using PropertyResolver.resolverProperties(...).
 * 
 * Note: this class expects character events to be merged - so you may want to wrap it in a MergeCharacterEventsHandler
 */
public class PropertyResolverContentHandler extends ForwardingContentHandler {

    public PropertyResolverContentHandler(ContentHandler consumer) {
        super(consumer);
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        Attributes2Impl attr = new Attributes2Impl(attributes);
        for (int i = 0; i < attr.getLength(); i++) {
            attr.setValue(i, PropertyResolver.resolveProperties(attr.getValue(i)));
        }
        super.startElement(uri, localName, qName, attr);
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        ch = PropertyResolver.resolveProperties(new String(ch, start, length)).toCharArray();
        super.characters(ch, 0, ch.length);
    }
    
    
}
