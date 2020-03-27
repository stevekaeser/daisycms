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

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10Publisher.*;
import org.xml.sax.ContentHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Upon special request:
 * This URL offers a built-in publisher request that can be performed using GET, and is
 * equivalent to the /publisher/documentPage request available in Daisy 1.2
 *
 */
public class PubDocumentHandler extends AbstractPublisherRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.GET)) {
            String documentId = HttpUtil.getStringParam(request, "documentId");
            String locale = HttpUtil.getStringParam(request, "locale");
            String version = HttpUtil.getStringParam(request, "version");
            boolean includeNavigation = HttpUtil.getBooleanParam(request, "includeNavigation");
            String navigationDocId = null;
            String activePath = null;
            boolean contextualized = true;
            if (includeNavigation) {
                navigationDocId = HttpUtil.getStringParam(request, "navigationDocId");
                activePath = request.getParameter("activePath"); // this is allowed to be null
                contextualized = HttpUtil.getBooleanParam(request, "contextualized");
            }

            String branch = String.valueOf(HttpUtil.getBranchId(request, repository));
            String language = String.valueOf(HttpUtil.getLanguageId(request, repository));

            PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.newInstance();
            PublisherRequestDocument.PublisherRequest publisherRequest = publisherRequestDocument.addNewPublisherRequest();
            publisherRequest.setLocale(locale);
            DocumentDocument.Document pubDocReq = publisherRequest.addNewDocument();
            pubDocReq.setId(documentId);
            pubDocReq.setBranch(branch);
            pubDocReq.setLanguage(language);
            pubDocReq.setVersion(version);
            pubDocReq.addNewAclInfo();
            pubDocReq.addNewSubscriptionInfo();
            pubDocReq.addNewShallowAnnotatedVersion();
            pubDocReq.addNewAnnotatedDocument();
            PreparedDocumentsDocument.PreparedDocuments prepDocReq = pubDocReq.addNewPreparedDocuments();
            PreparedDocumentsDocument.PreparedDocuments.Context prepDocReqContext = prepDocReq.addNewContext();
            prepDocReqContext.setBranch(branch);
            prepDocReqContext.setLanguage(language);
            if (includeNavigation) {
                VariantKeyType prepDocNav = prepDocReq.addNewNavigationDocument();
                prepDocNav.setId(navigationDocId);
                prepDocNav.setBranch(branch);
                prepDocNav.setLanguage(language);
            }
            pubDocReq.addNewComments();
            if (includeNavigation) {
                NavigationTreeDocument.NavigationTree navigationTreeReq = publisherRequest.addNewNavigationTree();
                VariantKeyType navDoc = navigationTreeReq.addNewNavigationDocument();
                navDoc.setId(navigationDocId);
                navDoc.setBranch(branch);
                navDoc.setLanguage(language);
                VariantKeyType activeDoc = navigationTreeReq.addNewActiveDocument();
                activeDoc.setId(documentId);
                activeDoc.setBranch(branch);
                activeDoc.setLanguage(language);
                if (activePath != null) {
                    navigationTreeReq.setActivePath(activePath);
                }
                navigationTreeReq.setContextualized(contextualized);
            }

            ContentHandler serializer = new XmlSerializer(response.getOutputStream());

            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            publisher.processRequest(publisherRequestDocument, serializer);

        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/document";
    }
}
