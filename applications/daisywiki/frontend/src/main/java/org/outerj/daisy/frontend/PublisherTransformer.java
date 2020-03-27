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
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.xml.IncludeXMLConsumer;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlSaxHandler;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.ContentHandler;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.publisher.Publisher;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;

import java.util.Map;
import java.io.IOException;

/**
 * A transformer which intercepts publisher requests (p:publisherRequest elements),
 * sends them to the publisher component, and inserts the publishers' response into
 * the SAX stream (in place of the request).
 */
public class PublisherTransformer extends AbstractTransformer implements Serviceable, Contextualizable, Disposable {
    private static final String NAMESPACE = "http://outerx.org/daisy/1.0#publisher";
    private ServiceManager serviceManager;
    private Repository repository;
    private boolean inPublisherRequest;
    private XmlSaxHandler xmlSaxHandler;
    private int publisherRequestElementNestingCount;
    private Request request;
    private FrontEndContext frontEndContext;
    private Context context;
    private PageContext pageContext;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public void setup(SourceResolver sourceResolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        request = ObjectModelHelper.getRequest(objectModel);
        frontEndContext = FrontEndContext.get(request);
        try {
            this.repository = frontEndContext.getRepository();
        } catch (Exception e) {
            throw new ProcessingException(e);
        }

        this.inPublisherRequest = false;
        this.pageContext = frontEndContext.getPageContext();
    }

    public void dispose() {
        this.repository = null;
        this.xmlSaxHandler = null;
        this.request = null;
        this.pageContext = null;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        if (inPublisherRequest) {
            xmlSaxHandler.getContentHandler().startElement(namespaceURI, localName, qName, atts);
            publisherRequestElementNestingCount++;
        } else if (localName.equals("publisherRequest") && namespaceURI.equals(NAMESPACE)) {
            inPublisherRequest = true;
            publisherRequestElementNestingCount = 0;
            xmlSaxHandler = XmlObject.Factory.newXmlSaxHandler();
            xmlSaxHandler.getContentHandler().startElement(namespaceURI, localName, qName, atts);
        } else {
            super.startElement(namespaceURI, localName, qName, atts);
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (inPublisherRequest && publisherRequestElementNestingCount == 0) {
            xmlSaxHandler.getContentHandler().endElement(namespaceURI, localName, qName);
            inPublisherRequest = false;

            // serialize the request and send it to the publisher
            XmlObject requestAsXmlObject;
            try {
                requestAsXmlObject = xmlSaxHandler.getObject();
            } catch (XmlException e) {
                throw new SAXException("Error getting recorded publisher request as XmlObject.", e);
            }

            PublisherRequestDocument publisherRequestDocument = (PublisherRequestDocument)requestAsXmlObject.changeType(PublisherRequestDocument.type);
            if (publisherRequestDocument == null)
                throw new SAXException("Could not change the type of the XmlObject to PublisherRequestDocument.");


            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            try {
                publisher.processRequest(publisherRequestDocument, new IncludeXMLConsumer(createPreparedDocumentsHandler(contentHandler)));
            } catch (RepositoryException e) {
                throw new SAXException("Error calling publisher.", e);
            }

            xmlSaxHandler = null;

        } else if (inPublisherRequest) {
            publisherRequestElementNestingCount--;
            xmlSaxHandler.getContentHandler().endElement(namespaceURI, localName, qName);
        } else {
            super.endElement(namespaceURI, localName, qName);
        }
    }

    private PreparedDocumentsHandler createPreparedDocumentsHandler(ContentHandler consumer) throws SAXException {
        String mountPoint = frontEndContext.getMountPoint();
        SiteConf siteConf = frontEndContext.getSiteConf();
        String daisyCocoonPath = frontEndContext.getDaisyCocoonPath();
        String basePath = mountPoint + "/" + siteConf + "/";

        Repository repository;
        try {
            repository = frontEndContext.getRepository();
        } catch (Exception e) {
            throw new SAXException(e);
        }
        String publishType = (String)request.getAttribute("documentStylingPublishType");

        DocumentTypeSpecificStyler.StylesheetProvider stylesheetProvider = new WikiStylesheetProvider(publishType, serviceManager);

        DocumentTypeSpecificStyler documentTypeSpecificStyler = new DocumentTypeSpecificStyler(publishType, basePath, daisyCocoonPath, stylesheetProvider, pageContext, repository, context, serviceManager);
        return new PreparedDocumentsHandler(consumer, request, documentTypeSpecificStyler);
    }

    public void setDocumentLocator(Locator locator) {
        if (!inPublisherRequest)
            super.setDocumentLocator(locator);
    }

    public void startDocument() throws SAXException {
        super.startDocument();
    }

    public void endDocument() throws SAXException {
        super.endDocument();
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (inPublisherRequest)
            xmlSaxHandler.getContentHandler().startPrefixMapping(prefix, uri);
        else
            super.startPrefixMapping(prefix, uri);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (inPublisherRequest)
            xmlSaxHandler.getContentHandler().endPrefixMapping(prefix);
        else
            super.endPrefixMapping(prefix);
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inPublisherRequest)
            xmlSaxHandler.getContentHandler().characters(ch, start, length);
        else
            super.characters(ch, start, length);
    }

    public void ignorableWhitespace(char[] chars, int start, int length) throws SAXException {
        if (!inPublisherRequest)
            super.ignorableWhitespace(chars, start, length);
    }

    public void processingInstruction(String s, String s1) throws SAXException {
        if (!inPublisherRequest)
            super.processingInstruction(s, s1);
    }

    public void skippedEntity(String s) throws SAXException {
        if (!inPublisherRequest)
            super.skippedEntity(s);
    }

    public void startDTD(String s, String s1, String s2) throws SAXException {
        if (!inPublisherRequest)
            super.startDTD(s, s1, s2);
    }

    public void endDTD() throws SAXException {
        if (!inPublisherRequest)
            super.endDTD();
    }

    public void startEntity(String s) throws SAXException {
        if (!inPublisherRequest)
            super.startEntity(s);
    }

    public void endEntity(String s) throws SAXException {
        if (!inPublisherRequest)
            super.endEntity(s);
    }

    public void startCDATA() throws SAXException {
        if (!inPublisherRequest)
            super.startCDATA();
    }

    public void endCDATA() throws SAXException {
        if (!inPublisherRequest)
            super.endCDATA();
    }

    public void comment(char[] chars, int i, int i1) throws SAXException {
        if (!inPublisherRequest)
            super.comment(chars, i, i1);
    }

}
