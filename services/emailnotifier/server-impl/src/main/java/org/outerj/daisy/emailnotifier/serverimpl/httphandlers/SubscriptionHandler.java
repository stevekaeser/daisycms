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
package org.outerj.daisy.emailnotifier.serverimpl.httphandlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.emailnotifier.Subscription;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.apache.xmlbeans.XmlOptions;
import org.outerx.daisy.x10.SubscriptionDocument;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class SubscriptionHandler extends AbstractSubscriptionRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long userId = HttpUtil.parseId("user", (String)matchMap.get("1"));

        EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");
        Subscription subscription = subscriptionManager.getSubscription(userId);

        if (request.getMethod().equals(HttpConstants.GET)) {
            response.setContentType("text/xml");
            subscription.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            SubscriptionDocument subscriptionDocument = SubscriptionDocument.Factory.parse(request.getInputStream(), xmlOptions);
            SubscriptionDocument.Subscription subscriptionXml = subscriptionDocument.getSubscription();
            subscription.setFromXml(subscriptionXml);
            subscription.save();
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            subscriptionManager.deleteSubscription(userId);
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/subscription/*";
    }
}
