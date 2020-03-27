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
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.emailnotifier.Subscribers;
import org.outerj.daisy.util.HttpConstants;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.collections.primitives.ArrayLongList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.StringTokenizer;

public class EventSubscribersHandler extends AbstractSubscriptionRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.GET)) {
            String type = (String)matchMap.get("1");
            Subscribers subscribers;
            EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");

            if (type.equals("document") || type.equals("comment")) {
                String documentId = HttpUtil.getStringParam(request, "documentId");
                long branchId = HttpUtil.getLongParam(request, "branch");
                long languageId = HttpUtil.getLongParam(request, "language");
                String collectionIdsParam = request.getParameter("collectionIds");
                StringTokenizer tokenizer = new StringTokenizer(collectionIdsParam, ",");
                LongList collectionIdList = new ArrayLongList();
                while (tokenizer.hasMoreTokens()) {
                    collectionIdList.add(HttpUtil.parseId("collection", tokenizer.nextToken()));
                }
                if (type.equals("document"))
                    subscribers = subscriptionManager.getAllDocumentEventSubscribers(documentId, branchId, languageId, collectionIdList.toArray());
                else
                    subscribers = subscriptionManager.getAllCommentEventSubscribers(documentId, branchId, languageId, collectionIdList.toArray());
            } else if (type.equals("schema")) {
                subscribers = subscriptionManager.getAllSchemaEventSubscribers();
            } else if (type.equals("user")) {
                subscribers = subscriptionManager.getAllUserEventSubscribers();
            } else if (type.equals("collection")) {
                subscribers = subscriptionManager.getAllCollectionEventSubscribers();
            } else if (type.equals("acl")) {
                subscribers = subscriptionManager.getAllAclEventSubscribers();
            } else {
                HttpUtil.sendCustomError("Invalid event group type: " + type, HttpConstants._400_Bad_Request, response);
                return;
            }

            subscribers.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/*EventsSubscribers";
    }
}
