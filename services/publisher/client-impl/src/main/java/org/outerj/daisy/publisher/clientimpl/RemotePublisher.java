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
package org.outerj.daisy.publisher.clientimpl;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.outerj.daisy.publisher.BlobInfo;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class RemotePublisher implements Publisher {
    private RemoteRepositoryImpl repository;

    public RemotePublisher(RemoteRepositoryImpl repository) {
        this.repository = repository;
    }

    public BlobInfo getBlobInfo(VariantKey variantKey, String versionSpec, String partType) throws RepositoryException {
        Map<String, String> requestParams = new HashMap<String, String>();
        requestParams.put("documentId", String.valueOf(variantKey.getDocumentId()));
        requestParams.put("branch", String.valueOf(variantKey.getBranchId()));
        requestParams.put("language", String.valueOf(variantKey.getLanguageId()));
        requestParams.put("version", versionSpec);
        requestParams.put("partType", partType);

        GetMethod method = repository.getResource("/publisher/blob", requestParams);
        return new BlobInfoImpl(method);
    }

    public void processRequest(PublisherRequestDocument publisherRequestDocument, ContentHandler contentHandler) throws RepositoryException, SAXException {
        try {
            DaisyHttpClient httpClient = repository.getHttpClient();
            PostMethod method = new PostMethod("/publisher/request");
            method.setRequestEntity(new InputStreamRequestEntity(publisherRequestDocument.newInputStream()));

            try {
                httpClient.executeMethod(method, null, false);
                SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
                parser.getXMLReader().setContentHandler(contentHandler);
                InputSource is = new InputSource(method.getResponseBodyAsStream());
                parser.getXMLReader().parse(is);
            } finally {
                method.releaseConnection();
            }
        } catch (Exception e) {
            if (e instanceof RepositoryException)
                throw (RepositoryException)e;
            if (e instanceof SAXException)
                throw (SAXException)e;
            throw new RepositoryException("Error handling publisher request.", e);
        }
    }
}
