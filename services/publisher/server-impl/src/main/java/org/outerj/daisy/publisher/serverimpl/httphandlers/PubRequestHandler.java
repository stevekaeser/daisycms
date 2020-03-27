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
package org.outerj.daisy.publisher.serverimpl.httphandlers;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.xml.sax.ContentHandler;
import org.apache.xmlbeans.XmlOptions;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class PubRequestHandler extends AbstractPublisherRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.POST)) {
            ContentHandler serializer = new XmlSerializer(response.getOutputStream());

            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            xmlOptions.setDocumentSourceName("submitted request");
            PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.parse(request.getInputStream(), xmlOptions);
            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            publisher.processRequest(publisherRequestDocument, serializer);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/request";
    }
}
