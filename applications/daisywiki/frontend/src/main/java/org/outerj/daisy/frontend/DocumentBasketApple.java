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
import org.outerj.daisy.frontend.components.docbasket.DocumentBasket;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.forms.util.StringMessage;
import org.apache.excalibur.xml.sax.XMLizable;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class DocumentBasketApple extends AbstractDaisyApple implements StatelessAppleController {
    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {

        // Note: guest users are allowed to use the document basket too

        if (request.getMethod().equals("GET")) {
            // GET request: show basket content

            DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, false);
            if (documentBasket == null)
                documentBasket = new DocumentBasket(); // not stored in session on purpose: avoids session creation

            showDocumentBasket(appleResponse, documentBasket, null);
        } else if (request.getMethod().equals("POST")) {
            String action = RequestUtil.getStringParameter(request, "basketAction");

            DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, true);

            if (action.equals("clear")) {
                documentBasket.clear();
                appleResponse.redirectTo("documentBasket");
            } else if (action.equals("removeSelected")) {
                List<DocumentBasketEntry> entries = getEntriesAndCheckConcurrentUpdates(documentBasket, request, appleResponse);
                if (entries == null)
                    return;

                // Read out selected items
                boolean[] selected = getSelectStatus(request, entries.size());

                List<DocumentBasketEntry> newEntries = new ArrayList<DocumentBasketEntry>(entries);
                for (int i = selected.length - 1; i >=0 ; i--) {
                    if (selected[i])
                        newEntries.remove(i);
                }
                documentBasket.setEntries(newEntries);

                appleResponse.redirectTo("documentBasket");
            } else {
                throw new Exception("Invalid value for action parameter: \"" + action + "\".");
            }
            
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

    private void showDocumentBasket(AppleResponse appleResponse, DocumentBasket documentBasket, XMLizable message) {
        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("documentBasket", documentBasket);
        if (message != null)
            viewData.put("basketMessage", message);
        viewData.put("pageContext", frontEndContext.getPageContext());

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/docbasket.xml");
        pipeConf.setStylesheet("daisyskin:xslt/docbasket/documentbasket.xsl");
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    /**
     * Gets the list of entries from the basket, but also checks the basket hasn't changed
     * since this request. If this is the case, this method will set an appropriate apple
     * response and return null.
     */
    private List<DocumentBasketEntry> getEntriesAndCheckConcurrentUpdates(DocumentBasket documentBasket, Request request, AppleResponse appleResponse) throws Exception {
        List<DocumentBasketEntry> entries = documentBasket.getEntries();
        long currentUpdateCount = documentBasket.getUpdateCount();
        long requestUpdateCount = RequestUtil.getLongParameter(request, "basketUpdateCount");
        if (currentUpdateCount != requestUpdateCount) {
            showDocumentBasket(appleResponse, documentBasket, new StringMessage("Your basket was updated since you requested this operation, so the operation has not been performed."));
            return null;
        }
        return entries;
    }

    private boolean[] getSelectStatus(Request request, int size) {
        boolean[] selected = new boolean[size];
        for (int i = 0; i < size; i++) {
            String checked = request.getParameter("entry." + (i + 1));
            selected[i] = checked != null && checked.equals("true");
        }
        return selected;
    }

}
