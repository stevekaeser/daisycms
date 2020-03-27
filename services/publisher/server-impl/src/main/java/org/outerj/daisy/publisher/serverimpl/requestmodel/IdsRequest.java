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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;

import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.io.IOException;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.text.Collator;

public class IdsRequest extends AbstractRequest {
    public IdsRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        String[] ids = extractIds(publisherContext);
        Arrays.sort(ids, Collator.getInstance(publisherContext.getLocale()));

        AttributesImpl emptyAttrs = new AttributesImpl();

        contentHandler.startElement(PublisherImpl.NAMESPACE, "ids", "p:ids", emptyAttrs);

        for (String id: ids) {
            contentHandler.startElement(PublisherImpl.NAMESPACE, "id", "p:id", emptyAttrs);
            contentHandler.characters(id.toCharArray(), 0, id.length());
            contentHandler.endElement(PublisherImpl.NAMESPACE, "id", "p:id");
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "ids", "p:ids");
    }

    private String[] extractIds(PublisherContext publisherContext) throws Exception {
        Version version = publisherContext.getVersion();
        InputStream is = null;
        try {
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            Set<String> ids = new HashSet<String>();
            Part[] parts = version.getParts().getArray();
            RepositorySchema schema = publisherContext.getRepository().getRepositorySchema();
            for (Part part : parts) {
                if (part.getMimeType().equals("text/xml") && schema.getPartTypeById(part.getTypeId(), false).isDaisyHtml()) {
                    is = part.getDataStream();
                    try {
                        parser.parse(new InputSource(is), new IdCollector(ids));
                    } catch (SAXException e) {
                        // ignore
                    } catch (IOException e) {
                        // ignore
                    }
                    is.close();
                }
            }
            return ids.toArray(new String[0]);
        } finally {
            if (is != null)
                try { is.close(); } catch (Throwable e) { publisherContext.getLogger().error("Error closing part input stream.", e); }
        }
    }

    private static class IdCollector extends DefaultHandler {
        private final Set<String> ids;

        public IdCollector(Set<String> ids) {
            this.ids = ids;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            String id = attributes.getValue("id");
            if (id != null)
                ids.add(id);
        }
    }
}