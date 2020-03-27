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

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Map;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.components.flow.FlowHelper;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.transformation.AbstractTransformer;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.excalibur.source.Source;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.frontend.components.wikidatasource.WikiDataSource;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class PdfImageLinkTransformer extends AbstractTransformer {
    private BookInstance bookInstance;
    private String publicationTypesUri;
    private String publicationOutputName;
    private static final String XSL_FO_NS = "http://www.w3.org/1999/XSL/Format";

    public void setup(SourceResolver sourceResolver, Map objectModel, String string, Parameters parameters) throws ProcessingException, SAXException, IOException {
        Object flowContext = FlowHelper.getContextObject(objectModel);
        JXPathContext jxpc = JXPathContext.newContext(flowContext);
        bookInstance = (BookInstance)jxpc.getValue("/bookInstance");
        if (bookInstance == null)
            throw new ProcessingException("Missing 'bookInstance' in flow context.");

        publicationOutputName = (String)jxpc.getValue("/publicationOutputName");

        Source source = null;
        try {
            // Since this transformer should only be used in publication type sitemaps, resolving just "" should give
            // the location of the publication type directory.
            source = sourceResolver.resolveURI("");
            if (source instanceof WikiDataSource) {
                WikiDataSource wikiDataSource = (WikiDataSource) source;
                publicationTypesUri = "file:" + URLEncoder.encode(wikiDataSource.getFile().getAbsolutePath(), "UTF-8");
            } else {
                publicationTypesUri = source.getURI();
            }
        } finally {
            sourceResolver.release(source);
        }
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes) throws SAXException {
        if (namespaceURI.equals(XSL_FO_NS) && localName.equals("external-graphic")) {
            attributes = handleLinkAttribute(attributes, "src");
        }
        attributes = handleLinkAttribute(attributes, "background-image");
        super.startElement(namespaceURI, localName, qName, attributes);
    }

    private Attributes handleLinkAttribute(Attributes attributes, String linkAttrName) {
        String linkAttr = attributes.getValue(linkAttrName);
        if (linkAttr != null) {
            String uri = linkAttr;
            if (linkAttr.startsWith("bookinstance:")) {
                uri = bookInstance.getResourceURI(linkAttr.substring("bookinstance:".length())).toString();
                // ibex expects file:///my/path/to/bookinstance if path contains spaces (see DSY-389)
            } else if (linkAttr.startsWith("publication:")) {
                uri = bookInstance.getResourceURI(BookInstanceLayout.getPublicationOutputPath(publicationOutputName) + linkAttr.substring("publication:".length())).toString();                
            } else if (linkAttr.startsWith("publicationtype:")) {
                uri = publicationTypesUri + "/" + linkAttr.substring("publicationtype:".length());
            }
            if (uri != null) {
                uri = uri.replaceFirst("^file:/", "file:///");
            }

            AttributesImpl newAttrs  = new AttributesImpl(attributes);
            newAttrs.setValue(attributes.getIndex(linkAttrName), "url(" + uri + ")");
            attributes = newAttrs;
        }
        return attributes;
    }
}
