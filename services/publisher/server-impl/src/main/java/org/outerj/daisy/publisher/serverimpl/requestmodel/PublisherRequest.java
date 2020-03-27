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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import java.util.Locale;

import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.repository.VersionMode;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class PublisherRequest extends AbstractParentPublisherRequest implements Request {
    private final Locale locale;
    private final String styleHint;
    private final boolean inlineExceptions;
    private final VersionMode versionMode;
    private final VariablesConfig variablesConfig;

    /**
     *
     * @param locale allowed to be null
     * @param styleHint optional, allowed to be null
     * @param versionMode optional, allowed to be null
     */
    public PublisherRequest(Locale locale, String styleHint, boolean inlineExceptions, VersionMode versionMode,
            VariablesConfig variablesConfig, LocationInfo locationInfo) {
        super(locationInfo);
        this.locale = locale;
        this.styleHint = styleHint;
        this.inlineExceptions = inlineExceptions;
        this.versionMode = versionMode;
        this.variablesConfig = variablesConfig;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("p", PublisherImpl.NAMESPACE);
        AttributesImpl attrs = new AttributesImpl();
        if (styleHint != null)
            attrs.addAttribute("", "styleHint", "styleHint", "CDATA", styleHint);
        contentHandler.startElement(PublisherImpl.NAMESPACE, "publisherResponse", "p:publisherResponse", attrs);

        PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
        childPublisherContext.setVariablesConfig(variablesConfig);

        if (locale != null)
            childPublisherContext.setLocale(locale);
        if (versionMode != null)
            childPublisherContext.setVersionMode(versionMode);
        else
            childPublisherContext.setVersionMode(publisherContext.getVersionMode());
        super.processInt(contentHandler, childPublisherContext);

        contentHandler.endElement(PublisherImpl.NAMESPACE, "publisherResponse", "p:publisherResponse");
        contentHandler.endPrefixMapping("p");
        contentHandler.endDocument();
    }

    public boolean getInlineExceptions() {
        return inlineExceptions;
    }
}
