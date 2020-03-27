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
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.xml.*;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.excalibur.pool.Recyclable;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

import java.util.Map;
import java.io.IOException;

/**
 * This transformer inserts previously styled documents at the location of the
 * insertStyledDocument element, and meanwhile performs the actual nesting of
 * the included documents.
 */
public class IncludePreparedDocumentsTransformer extends AbstractTransformer implements Recyclable {
    private Request request;

    public void setup(SourceResolver sourceResolver, Map objectMap, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        request = ObjectModelHelper.getRequest(objectMap);
    }

    public void recycle() {
        super.recycle();
        this.request = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (namespaceURI.equals("") && localName.equals("insertStyledDocument")) {
            String styledResultsId = atts.getValue("styledResultsId");
            if (styledResultsId == null)
                throw new SAXException("Missing styledResultsId on insertStyledDocument element");
            PreparedDocuments preparedDocuments = (PreparedDocuments)request.getAttribute(styledResultsId);
            if (preparedDocuments == null)
                throw new SAXException("PreparedDocuments not found in request attribute \"" + styledResultsId + "\".");
            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getPreparedDocument(1);
            if (preparedDocument == null)
                throw new SAXException("Missing root (1) preparedDocument.");
            PreparedIncludeHandler preparedIncludeHandler = new PreparedIncludeHandler(this, preparedDocuments, false);
            preparedDocument.getSaxBuffer().toSAX(new IncludeXMLConsumer(preparedIncludeHandler));
        } else {
            super.startElement(namespaceURI, localName, qName, atts);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (namespaceURI.equals("") && localName.equals("insertStyledDocument")) {
            // ignore element
        } else {
            super.endElement(namespaceURI, localName, qName);
        }
    }
}
