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
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.commons.jxpath.JXPathContext;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.outerj.daisy.books.publisher.impl.util.XMLPropertiesHelper;

import java.util.Map;
import java.io.IOException;

/**
 * Merges publication properties and metadata into the SAX stream (right after the opening root element).
 */
public class MergePropsAndMetadataTransformer extends AbstractTransformer {
    private Map<String, String> publicationProperties;
    private Map<String, String> bookMetadata;
    private boolean rootElement;

    public void setup(SourceResolver sourceResolver, Map objectModel, String string, Parameters parameters) throws ProcessingException, SAXException, IOException {
        Object flowContext = FlowHelper.getContextObject(objectModel);
        JXPathContext jxpc = JXPathContext.newContext(flowContext);

        publicationProperties = (Map<String, String>)jxpc.getValue("/pubProps");
        if (publicationProperties == null)
            throw new ProcessingException("Missing 'pubProps' in flow context.");

        bookMetadata = (Map<String, String>)jxpc.getValue("/bookMetadata");
        if (bookMetadata == null)
            throw new ProcessingException("Missing 'bookMetadata' in flow context.");

        rootElement = true;
    }

    public void recycle() {
        super.recycle();
        this.publicationProperties = null;
        this.bookMetadata = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(namespaceURI, localName, qName, attributes);

        if (rootElement) {
            XMLPropertiesHelper.generateSaxFragment(publicationProperties, contentHandler);
            XMLPropertiesHelper.generateSaxFragment(bookMetadata, "metadata", contentHandler);
            rootElement = false;
        }
    }
}
