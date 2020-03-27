/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.xmlutil.SaxBuffer;

import java.util.*;
import java.text.Collator;

public class VariablesListRequest extends AbstractRequest {

    public VariablesListRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        contentHandler.startElement(PublisherImpl.NAMESPACE, "variablesList", "p:variablesList", attrs);

        Variables variables = publisherContext.getVariables();
        if (variables != null) {
            Map<String, SaxBuffer> entries = variables.getEntries();

            List<Map.Entry<String, SaxBuffer>> entryList = new ArrayList<Map.Entry<String, SaxBuffer>>(entries.entrySet());

            final Collator collator = Collator.getInstance(publisherContext.getLocale());
            Collections.sort(entryList, new Comparator<Map.Entry<String, SaxBuffer>>() {
                public int compare(Map.Entry<String, SaxBuffer> o1, Map.Entry<String, SaxBuffer> o2) {
                    return collator.compare(o1.getKey(), o2.getKey());
                }
            });

            for (Map.Entry<String, SaxBuffer> entry : entryList) {
                attrs.clear();
                attrs.addAttribute("", "name", "name", "CDATA", entry.getKey());
                contentHandler.startElement(PublisherImpl.NAMESPACE, "variable", "p:variable", attrs);
                entry.getValue().toSAX(contentHandler);
                contentHandler.endElement(PublisherImpl.NAMESPACE, "variable", "p:variable");
            }
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "variablesList", "p:variablesList");
    }
}
