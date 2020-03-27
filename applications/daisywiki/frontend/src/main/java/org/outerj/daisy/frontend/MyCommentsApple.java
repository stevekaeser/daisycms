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

import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.publisher.Publisher;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;

import java.util.Map;
import java.util.HashMap;
import java.util.Locale;

public class MyCommentsApple  extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        RemoteRepositoryImpl repository = (RemoteRepositoryImpl)frontEndContext.getRepository();
        Locale locale = frontEndContext.getLocale();
        SiteConf siteConf = frontEndContext.getSiteConf();

        //
        // Handle actions, if any
        //

        String action = request.getParameter("action");
        if (action == null) {
            // ignore
        } else if (action.equals("deleteComment")) {
            if (request.getMethod().equals("POST")) {
                long commentId = RequestUtil.getLongParameter(request, "commentId");
                String documentId = RequestUtil.getStringParameter(request, "documentId");
                long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
                long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);
                repository.getCommentManager().deleteComment(documentId, branchId, languageId, commentId);
            } else {
                throw new HttpMethodNotAllowedException(request.getMethod());
            }
        } else {
            throw new Exception("Unsupported action parameter value: \"" + action + "\".");
        }

        //
        // Show comments
        //

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("localeAsString", LocaleHelper.getString(locale));
        viewData.put("pageContext", frontEndContext.getPageContext());

        PublisherRequestDocument publisherRequestDocument = PublisherXmlRequestBuilder.loadPublisherRequest("internal/mycommentspage_pubreq.xml", viewData, serviceManager, getContext());
        SaxBuffer publisherResponse = new SaxBuffer();
        Publisher publisher = (Publisher)repository.getExtension("Publisher");
        publisher.processRequest(publisherRequestDocument, publisherResponse);

        viewData.put("pageXml", publisherResponse);

        viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/mycomments.xsl"));

        appleResponse.sendPage("internal/genericPipe", viewData);
    }
}