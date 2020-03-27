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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;

public class SubscriptionInfoRequest extends AbstractRequest implements Request {
    public SubscriptionInfoRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();

        if (!repository.hasExtension("EmailSubscriptionManager"))
            return;
        
        EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");

        String subscribed = String.valueOf(subscriptionManager.isSubsribed(publisherContext.getVariantKey()));
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "subscribed", "subscribed", "CDATA", String.valueOf(subscribed));
        contentHandler.startElement(PublisherImpl.NAMESPACE, "subscriptionInfo", "p:subscriptionInfo", attrs);
        contentHandler.endElement(PublisherImpl.NAMESPACE, "subscriptionInfo", "p:subscriptionInfo");
    }
}
