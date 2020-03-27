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
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;

import java.util.List;

/**
 * Resolves variables in one or more text strings.
 */
public class ResolveVariablesRequest extends AbstractRequest {
    private final List<String> texts;

    public ResolveVariablesRequest(List<String> texts, LocationInfo locationInfo) {
        super(locationInfo);
        this.texts = texts;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        AttributesImpl attrs = new AttributesImpl();
        contentHandler.startElement(PublisherImpl.NAMESPACE, "resolvedVariables", "p:resolvedVariables", attrs);

        for (String text : texts) {
            contentHandler.startElement(PublisherImpl.NAMESPACE, "text", "p:text", attrs);
            String newText = publisherContext.resolveVariables(text);
            text = newText != null ? newText : text;
            contentHandler.characters(text.toCharArray(), 0, text.length());
            contentHandler.endElement(PublisherImpl.NAMESPACE, "text", "p:text");
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "resolvedVariables", "p:resolvedVariables");
    }
}
