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

import org.outerj.daisy.emailnotifier.Subscriber;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerx.daisy.x10.SubscriberDocument;

import java.util.Locale;

public class SubscriberImpl implements Subscriber {
    private long userId;
    private Locale locale;

    public SubscriberImpl(long userId, Locale locale) {
        this.userId = userId;
        this.locale = locale;
    }

    public long getUserId() {
        return userId;
    }

    public Locale getLocale() {
        return locale;
    }

    public SubscriberDocument getXml() {
        SubscriberDocument subscriberDocument = SubscriberDocument.Factory.newInstance();
        SubscriberDocument.Subscriber subscriberXml = subscriberDocument.addNewSubscriber();
        subscriberXml.setUserId(userId);
        if (locale != null)
            subscriberXml.setLocale(LocaleHelper.getString(locale));
        return subscriberDocument;
    }
}
