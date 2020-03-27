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
package org.outerj.daisy.emailnotifier.commonimpl;

import org.outerj.daisy.emailnotifier.Subscriptions;
import org.outerj.daisy.emailnotifier.Subscription;
import org.outerx.daisy.x10.SubscriptionsDocument;
import org.outerx.daisy.x10.SubscriptionDocument;

public class SubscriptionsImpl implements Subscriptions {
    private Subscription[] subscriptions;

    public SubscriptionsImpl(Subscription[] subscriptions) {
        this.subscriptions = subscriptions;
    }

    public Subscription[] getArray() {
        return subscriptions;
    }

    public SubscriptionsDocument getXml() {
        SubscriptionsDocument subscriptionsDocument = SubscriptionsDocument.Factory.newInstance();

        SubscriptionDocument.Subscription[] subscriptionsAsXml = new SubscriptionDocument.Subscription[subscriptions.length];
        for (int i = 0; i < subscriptions.length; i++) {
            subscriptionsAsXml[i] = subscriptions[i].getXml().getSubscription();
        }

        subscriptionsDocument.addNewSubscriptions().setSubscriptionArray(subscriptionsAsXml);

        return subscriptionsDocument;
    }
}
