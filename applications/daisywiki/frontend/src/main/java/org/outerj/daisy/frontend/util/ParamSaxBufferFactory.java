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

import org.apache.cocoon.xml.ParamSaxBuffer;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.StringReader;

public class ParamSaxBufferFactory {

    /**
     * Helper factory for create ParamSaxBuffers from XML blurbs. The resulting
     * buffer will not contain start and endDocument events. The xmlBlurb parameter
     * does not need to have a root element, it can simply be a string with some
     * parameters in it.
     */
    public static ParamSaxBuffer create(String xmlBlurb) {
        String xml = "<dummyRoot>" + xmlBlurb + "</dummyRoot>";

        ParamSaxBuffer buffer = new ParamSaxBuffer();
        IncludeXMLConsumer includeConsumer = new IncludeXMLConsumer(buffer);
        includeConsumer.setIgnoreRootElement(true);

        try {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);
            SAXParser parser = parserFactory.newSAXParser();
            XMLReader reader = parser.getXMLReader();
            reader.setContentHandler(includeConsumer);
            InputSource is = new InputSource(new StringReader(xml));
            reader.parse(is);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return buffer;
    }
}
