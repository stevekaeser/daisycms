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
import org.outerj.daisy.frontend.components.docbasket.DocumentBasket;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.xml.SaxBuffer;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

/**
 * Apple that lets the user get an aggregated rendering of all documents in their document basket.
 */
public class AggregateDocumentBasketApple extends AbstractDaisyApple implements StatelessAppleController, Serviceable {
    private ServiceManager serviceManager;
    private static final int LIMIT_FOR_HTML = 150;
    private static final int LIMIT_FOR_XSLFO = 50;

    public void service(ServiceManager serviceManager) throws ServiceException {
        this.serviceManager = serviceManager;
    }

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        String publishType = appleRequest.getSitemapParameter("publishType");

        DocumentBasket documentBasket = DocumentBasketHelper.getDocumentBasket(request, true);

        // List of entries is retrieved before checking the size, and we then keep working with
        // this copy of the list, since the basket may be updated (and thus grow) concurrently
        List<DocumentBasketEntry> entries = documentBasket.getEntries();

        if (publishType.equals("html") && documentBasket.size() > LIMIT_FOR_HTML) {
            throw new Exception("Aggregation of multiple documents into one HTML page is limited to " + LIMIT_FOR_HTML + " documents.");
        } else if (publishType.equals("xslfo") && documentBasket.size() > LIMIT_FOR_XSLFO) {
            throw new Exception("Aggregation of multiple documents into one PDF is limited to " + LIMIT_FOR_XSLFO + " documents.");
        }

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("documentBasketEntries", entries);
        viewData.put("localeAsString", frontEndContext.getLocaleAsString());
        viewData.put("pageContext", frontEndContext.getPageContext());

        WikiPublisherHelper publisherHelper = new WikiPublisherHelper(request, getContext(), serviceManager);
        SaxBuffer pubReqResult = publisherHelper.performPublisherRequest("internal/docbasket_aggr_pubreq.xml", viewData, publishType);

        viewData.put("pageXml", pubReqResult);

        appleResponse.sendPage("internal/documentBasket/" + publishType + "-publishAggregate", viewData);
    }

}
