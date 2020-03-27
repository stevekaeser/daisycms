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
package org.outerj.daisy.emailnotifier;

import org.outerx.daisy.x10.SubscriptionDocument;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

import java.util.Locale;

public interface Subscription {
    /**
     * Returns the user to which this subscription applies.
     */
    public long getUserId();

    public void setReceiveDocumentEvents(boolean value);

    public boolean getReceiveDocumentEvents();

    public void setReceiveSchemaEvents(boolean value);

    public boolean getReceiveSchemaEvents();

    public void setReceiveUserEvents(boolean value);

    public boolean getReceiveUserEvents();

    public void setReceiveCollectionEvents(boolean value);

    public boolean getReceiveCollectionEvents();

    public void setReceiveAclEvents(boolean value);

    public boolean getReceiveAclEvents();

    public void setReceiveCommentEvents(boolean value);

    public boolean getReceiveCommentEvents();

    /**
     * Returns the locale to use to format the notification mails, returns null if not set.
     */
    public Locale getLocale();

    /**
     * Sets the locale used to format the notification mails.
     * @param locale can be null
     */
    public void setLocale(Locale locale);

    public VariantKey[] getSubscribedVariantKeys();

    /**
     * Note: if any of the VariantKey components for an entry is '-1', it means 'any'
     * (for the document ID, use '*', see also {@link EmailSubscriptionManager#DOCUMENT_ID_WILDCARD}).
     */
    public void setSubscribedVariantKeys(VariantKey[] keys);

    public CollectionSubscriptionKey[] getSubscribedCollectionKeys();

    /**
     * Note: if any of the CollectionSubscriptionKey components for an entry is '-1', it means 'any'.
     */
    public void setSubscribedCollectionKeys(CollectionSubscriptionKey[] keys);

    public void save() throws RepositoryException;

    public SubscriptionDocument getXml();

    public void setFromXml(SubscriptionDocument.Subscription subscriptionXml);
}
