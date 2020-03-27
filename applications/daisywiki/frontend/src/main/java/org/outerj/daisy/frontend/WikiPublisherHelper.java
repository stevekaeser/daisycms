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

import org.apache.cocoon.xml.SaxBuffer;
import org.apache.cocoon.environment.Request;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.context.Context;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.xml.sax.ContentHandler;

/**
 * Helper class for making publisher requests in the context of the Daisy Wiki.
 * The result of the publisher request is pulled through a PreparedDocumentsHandler
 * to apply document type specific styling, if needed.
 */
public class WikiPublisherHelper {
    private final Request request;
    private final FrontEndContext frontEndContext;
    private final Context context;
    private final ServiceManager serviceManager;
    private final Repository repository;
    private final String mountPoint;
    private final SiteConf siteConf;
    private final String daisyCocoonPath;

    public WikiPublisherHelper(Request request, Context context, ServiceManager serviceManager) throws Exception {
        this(FrontEndContext.get(request).getRepository(), request, context, serviceManager);
    }
    
    public WikiPublisherHelper(Repository repository, Request request, Context context, ServiceManager serviceManager) throws Exception {
        this.repository = repository;
        this.request = request;
        this.frontEndContext = FrontEndContext.get(request);
        this.context = context;
        this.serviceManager = serviceManager;
        this.mountPoint = frontEndContext.getMountPoint();
        this.daisyCocoonPath = frontEndContext.getDaisyCocoonPath();
        this.siteConf = frontEndContext.getSiteConf();
    }

    public PublisherRequestDocument buildPublisherRequest(String pipe, Object params) throws Exception {
        return PublisherXmlRequestBuilder.loadPublisherRequest(pipe, params, serviceManager, context);
    }

    public SaxBuffer performPublisherRequest(String pipe, Object params, String publishType) throws Exception {
        PublisherRequestDocument publisherRequestDocument = buildPublisherRequest(pipe, params);
        SaxBuffer result = new SaxBuffer();
        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        publisher.processRequest(publisherRequestDocument, createPreparedDocumentsHandler(result, publishType));
        return result;
    }

    private PreparedDocumentsHandler createPreparedDocumentsHandler(ContentHandler consumer, String publishType) {
        String basePath = mountPoint + "/" + siteConf.getName() + "/";

        PageContext pageContext = frontEndContext.getPageContext(repository);
        DocumentTypeSpecificStyler.StylesheetProvider stylesheetProvider = new WikiStylesheetProvider(publishType, serviceManager);
        DocumentTypeSpecificStyler documentTypeSpecificStyler = new DocumentTypeSpecificStyler(publishType, basePath,
                daisyCocoonPath, stylesheetProvider, pageContext, repository, context, serviceManager);
        return new PreparedDocumentsHandler(consumer, request, documentTypeSpecificStyler);
    }
}
