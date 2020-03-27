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
package org.outerj.daisy.publisher.serverimpl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.publisher.GlobalPublisherException;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.serverimpl.requestmodel.LocationInfo;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherRequest;
import org.outerj.daisy.publisher.serverimpl.requestmodel.RootPublisherContext;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerx.daisy.x10.ErrorDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class PublisherImpl implements Publisher {
    public static final String NAMESPACE = "http://outerx.org/daisy/1.0#publisher";
    private Repository repository;
    private CommonPublisher commonPublisher;
    private Log log;

    public PublisherImpl(Repository repository, CommonPublisher commonPublisher, Log log) {
        this.repository = repository;
        this.commonPublisher = commonPublisher;
        this.log = log;
    }

    public BlobInfo getBlobInfo(VariantKey variantKey, String versionSpec, String partType) throws RepositoryException {
        Document document = repository.getDocument(variantKey, false);

        Version version = null;
        try {
            long versionId = Long.parseLong(versionSpec);
            version = document.getVersion(versionId);
        } catch (NumberFormatException nfe) {
            version = document.getVersion(VersionMode.get(versionSpec));
        }
        if (version == null)
            throw new RepositoryException("Document " + document.getVariantKey() + " does not have a version matching the specification " + versionSpec);

        Part part;
        if (!(partType.charAt(0) >= '0' && partType.charAt(0) <= '9')) {
            part = version.getPart(partType);
        } else {
            part = version.getPart(Long.parseLong(partType));
        }

        return new BlobInfoImpl(part, version.getCreated());
    }

    public void processRequest(PublisherRequestDocument publisherRequestDocument, ContentHandler contentHandler)
            throws RepositoryException, SAXException {
        PublisherRequest publisherRequest = commonPublisher.buildPublisherRequest(publisherRequestDocument);
        RootPublisherContext publisherContext = new RootPublisherContext(repository, this, commonPublisher.getVariablesManager(), log);
        try {
            executePublisherRequest(publisherRequest, publisherContext, contentHandler);
        } catch (Throwable e) {
            List<LocationInfo> locationInfos = publisherContext.getLocationStack();
            List<String> locationStack = new ArrayList<String>(locationInfos.size());
            for (int i = locationInfos.size() - 1; i >= 0; i--) { // an execution stack should be in reverse order
                locationStack.add(locationInfos.get(i).getFormattedLocation());
            }
            throw new GlobalPublisherException("Error executing publisher request.", locationStack, e);
        }
    }

    public void performRequest(String pubReqSetName, Document document, Version version,
            PublisherContext publisherContext, ContentHandler contentHandler) throws Exception {
        PublisherRequest publisherRequest = commonPublisher.lookupPublisherRequest(pubReqSetName, document, version, publisherContext);
        executePublisherRequest(publisherRequest, publisherContext, contentHandler);
    }

    private void executePublisherRequest(PublisherRequest publisherRequest, PublisherContext publisherContext,
            ContentHandler contentHandler) throws Exception {
        boolean inlineExceptions = publisherRequest.getInlineExceptions();
        SaxBuffer buffer = new SaxBuffer();

        if (inlineExceptions) {
            try {
                publisherRequest.process(buffer, publisherContext);
            } catch (Throwable e) {
                streamThrowable(e, contentHandler);
                return;
            }
        } else {
            publisherRequest.process(buffer, publisherContext);
        }
        buffer.toSAX(contentHandler);
    }

    private void streamThrowable(Throwable e, ContentHandler contentHandler) throws SAXException {
        ErrorDocument errorDocument = HttpUtil.buildErrorXml(e);
        contentHandler.startDocument();
        contentHandler.startPrefixMapping("p", NAMESPACE);
        contentHandler.startElement(PublisherImpl.NAMESPACE, "publisherResponse", "p:publisherResponse", new AttributesImpl());
        errorDocument.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
        contentHandler.endElement(PublisherImpl.NAMESPACE, "publisherResponse", "p:publisherResponse");
        contentHandler.endPrefixMapping("p");
        contentHandler.endDocument();
    }
}
