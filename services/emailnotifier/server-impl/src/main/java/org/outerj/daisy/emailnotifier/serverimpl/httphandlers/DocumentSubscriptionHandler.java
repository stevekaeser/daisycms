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
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.util.HttpConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DocumentSubscriptionHandler extends AbstractSubscriptionRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");
        String documentId = (String)matchMap.get("1");
        if (request.getMethod().equals("POST")) {
            String action = request.getParameter("action");
            if (action == null) {
                throw new Exception("Missing action parameter");
            } else if (action.equals("add")) {
                long userId = HttpUtil.getLongParam(request, "userId");
                long branchId = HttpUtil.getLongParam(request, "branch");
                long languageId = HttpUtil.getLongParam(request, "language");
                subscriptionManager.addDocumentSubscription(userId, new VariantKey(documentId, branchId, languageId));
            } else if (action.equals("remove")) {
                long userId = HttpUtil.getLongParam(request, "userId");
                long branchId = HttpUtil.getLongParam(request, "branch");
                long languageId = HttpUtil.getLongParam(request, "language");
                subscriptionManager.deleteDocumentSubscription(userId, new VariantKey(documentId, branchId, languageId));
            } else {
                throw new Exception("Invalid value for action parameter: " + action);
            }
        } else if (request.getMethod().equals("DELETE")) {
            if (request.getParameter("branch") != null) {
                long branchId = HttpUtil.getLongParam(request, "branch");
                long languageId = HttpUtil.getLongParam(request, "language");
                subscriptionManager.deleteAllSubscriptionsForDocumentVariant(new VariantKey(documentId, branchId, languageId));
            } else {
                subscriptionManager.deleteAllSubscriptionsForDocument(documentId);
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/documentSubscription/*";
    }
}
