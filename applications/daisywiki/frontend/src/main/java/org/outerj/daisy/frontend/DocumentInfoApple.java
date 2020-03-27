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
package org.outerj.daisy.frontend;

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.MultiXMLizable;
import org.outerj.daisy.frontend.util.SimpleElementXMLizable;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.publisher.GlobalPublisherException;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerx.daisy.x10Publisher.DocumentDocument;

import java.util.Map;
import java.util.HashMap;

/**
 * Returns some summary document information such as its name.
 */
public class DocumentInfoApple extends AbstractDaisyApple implements StatelessAppleController, LogEnabled {
    private Logger logger;

    public void enableLogging(Logger logger) {
        this.logger = logger;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        SiteConf siteConf = frontEndContext.getSiteConf();
        Repository repository = frontEndContext.getRepository();

        String documentId = RequestUtil.getStringParameter(request, "documentId");
        String branch = request.getParameter("branch");
        if (branch == null || branch.length() == 0)
            branch = String.valueOf(siteConf.getBranchId());
        String lang = request.getParameter("language");
        if (lang == null || lang.length() == 0)
            lang = String.valueOf(siteConf.getLanguageId());
        String version = request.getParameter("version");

        boolean includeVersionsList = "true".equals(request.getParameter("includeVersionList"));
        boolean includeIdList = "true".equals(request.getParameter("includeIdList"));

        // Build a publisher request to retrieve the info
        PublisherRequestDocument pubReqDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.addNewPublisherRequest();
        pubReq.setLocale(frontEndContext.getLocaleAsString());
        DocumentDocument.Document reqDoc = pubReq.addNewDocument();
        reqDoc.setId(documentId);
        reqDoc.setBranch(branch);
        reqDoc.setLanguage(lang);
        if (version != null && version.length() > 0)
            reqDoc.setVersion(version);
        reqDoc.addNewAnnotatedDocument();
        if (includeVersionsList)
            reqDoc.addNewAnnotatedVersionList();
        if (includeIdList)
            reqDoc.addNewIds();

        // execute the publisher request
        String result = "ok";
        Throwable exc = null;
        SaxBuffer pubResponse = new SaxBuffer();
        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        try {
            publisher.processRequest(pubReqDoc, pubResponse);
        } catch (GlobalPublisherException e) {
            exc = e.getCause(); // will never be null
        }

        if (exc != null) {
            if (exc instanceof DocumentNotFoundException) {
                result = "document-not-found";
            } else if (exc instanceof DocumentVariantNotFoundException) {
                result = "document-variant-not-found";
            } else if (exc instanceof AccessException) {
                result = "access-denied";
            } else if (exc instanceof VersionNotFoundException) {
                result = "version-not-found";
            } else if (exc instanceof BranchNotFoundException) {
                result = "branch-not-found";
            } else if (exc instanceof LanguageNotFoundException) {
                result = "language-not-found";
            } else {
                result = "error";
                logger.error("Error getting document info in DocumentInfoApple", exc);
            }
        }

        // send response to client
        MultiXMLizable pageXml = new MultiXMLizable();
        pageXml.add(new SimpleElementXMLizable("result", result));
        pageXml.add(pubResponse);

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageXml", pageXml);

        GenericPipeConfig pipeConfig = new GenericPipeConfig();
        pipeConfig.setStylesheet("resources/xslt/documentinfo.xsl");
        pipeConfig.setApplyLayout(false);
        pipeConfig.setXmlSerializer();
        viewData.put("pipeConf", pipeConfig);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}
