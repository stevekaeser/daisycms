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

import org.outerj.daisy.emailnotifier.Subscription;
import org.outerj.daisy.emailnotifier.CollectionSubscriptionKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.VariantKey;
import org.outerx.daisy.x10.SubscriptionDocument;

import java.util.Locale;
import java.util.List;

public class SubscriptionImpl implements Subscription {
    private long userId;
    private boolean receiveDocumentEvents;
    private boolean receiveSchemaEvents;
    private boolean receiveUserEvents;
    private boolean receiveCollectionEvents;
    private boolean receiveAclEvents;
    private boolean receiveCommentEvents;
    private Locale locale;
    private VariantKey[] subscribedVariantKeys = new VariantKey[0];
    private CollectionSubscriptionKey[] subscribedCollectionKeys = new CollectionSubscriptionKey[0];
    private SubscriptionStrategy subscriptionStrategy;

    public SubscriptionImpl(SubscriptionStrategy subscriptionStrategy, long userId) {
        this.subscriptionStrategy = subscriptionStrategy;
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    public void setReceiveDocumentEvents(boolean value) {
        this.receiveDocumentEvents = value;
    }

    public boolean getReceiveDocumentEvents() {
        return receiveDocumentEvents;
    }

    public void setReceiveSchemaEvents(boolean value) {
        this.receiveSchemaEvents = value;
    }

    public boolean getReceiveSchemaEvents() {
        return receiveSchemaEvents;
    }

    public void setReceiveUserEvents(boolean value) {
        this.receiveUserEvents = value;
    }

    public boolean getReceiveUserEvents() {
        return receiveUserEvents;
    }

    public void setReceiveCollectionEvents(boolean value) {
        this.receiveCollectionEvents = value;
    }

    public boolean getReceiveCollectionEvents() {
        return receiveCollectionEvents;
    }

    public void setReceiveAclEvents(boolean value) {
        this.receiveAclEvents = value;
    }

    public boolean getReceiveAclEvents() {
        return receiveAclEvents;
    }

    public void setReceiveCommentEvents(boolean value) {
        this.receiveCommentEvents = value;
    }

    public boolean getReceiveCommentEvents() {
        return receiveCommentEvents;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public VariantKey[] getSubscribedVariantKeys() {
        return subscribedVariantKeys;
    }

    public void setSubscribedVariantKeys(VariantKey[] keys) {
        subscribedVariantKeys = keys;
    }

    public CollectionSubscriptionKey[] getSubscribedCollectionKeys() {
        return subscribedCollectionKeys;
    }

    public void setSubscribedCollectionKeys(CollectionSubscriptionKey[] keys) {
        subscribedCollectionKeys = keys;
    }

    public void save() throws RepositoryException {
        subscriptionStrategy.storeSubscription(this);
    }

    public SubscriptionDocument getXml() {
        SubscriptionDocument subscriptionDocument = SubscriptionDocument.Factory.newInstance();
        SubscriptionDocument.Subscription subscriptionXml = subscriptionDocument.addNewSubscription();
        subscriptionXml.setUserId(getUserId());
        subscriptionXml.setReceiveDocumentEvents(getReceiveDocumentEvents());
        subscriptionXml.setReceiveUserEvents(getReceiveUserEvents());
        subscriptionXml.setReceiveCollectionEvents(getReceiveCollectionEvents());
        subscriptionXml.setReceiveSchemaEvents(getReceiveSchemaEvents());
        subscriptionXml.setReceiveAclEvents(getReceiveAclEvents());
        subscriptionXml.setReceiveCommentEvents(getReceiveCommentEvents());
        subscriptionXml.addNewSubscribedDocuments().setVariantArray(getSubscribedDocumentsInXml());
        subscriptionXml.addNewSubscribedCollections().setCollectionArray(getSubscribedCollectionsInXml());
        if (locale != null)
            subscriptionXml.setLocale(LocaleHelper.getString(locale));
        return subscriptionDocument;
    }

    private SubscriptionDocument.Subscription.SubscribedDocuments.Variant[] getSubscribedDocumentsInXml() {
        SubscriptionDocument.Subscription.SubscribedDocuments.Variant[] subscribedVariantsXml = new SubscriptionDocument.Subscription.SubscribedDocuments.Variant[subscribedVariantKeys.length];
        for (int i = 0; i < subscribedVariantKeys.length; i++) {
            SubscriptionDocument.Subscription.SubscribedDocuments.Variant variantXml = SubscriptionDocument.Subscription.SubscribedDocuments.Variant.Factory.newInstance();
            variantXml.setDocumentId(subscribedVariantKeys[i].getDocumentId());
            variantXml.setBranchId(subscribedVariantKeys[i].getBranchId());
            variantXml.setLanguageId(subscribedVariantKeys[i].getLanguageId());
            subscribedVariantsXml[i] = variantXml;
        }
        return subscribedVariantsXml;
    }

    private SubscriptionDocument.Subscription.SubscribedCollections.Collection[] getSubscribedCollectionsInXml() {
        SubscriptionDocument.Subscription.SubscribedCollections.Collection[] collectionsXml = new SubscriptionDocument.Subscription.SubscribedCollections.Collection[subscribedCollectionKeys.length];
        for (int i = 0; i < subscribedCollectionKeys.length; i++) {
            SubscriptionDocument.Subscription.SubscribedCollections.Collection collectionXml = SubscriptionDocument.Subscription.SubscribedCollections.Collection.Factory.newInstance();
            collectionXml.setCollectionId(subscribedCollectionKeys[i].getCollectionId());
            collectionXml.setBranchId(subscribedCollectionKeys[i].getBranchId());
            collectionXml.setLanguageId(subscribedCollectionKeys[i].getLanguageId());
            collectionsXml[i] = collectionXml;
        }
        return collectionsXml;
    }

    public void setFromXml(SubscriptionDocument.Subscription subscriptionXml) {
        setReceiveDocumentEvents(subscriptionXml.getReceiveDocumentEvents());
        setReceiveSchemaEvents(subscriptionXml.getReceiveSchemaEvents());
        setReceiveUserEvents(subscriptionXml.getReceiveUserEvents());
        setReceiveCollectionEvents(subscriptionXml.getReceiveCollectionEvents());
        setReceiveAclEvents(subscriptionXml.getReceiveAclEvents());
        setReceiveCommentEvents(subscriptionXml.getReceiveCommentEvents());
        setSubscribedVariantKeys(instantiateVariantKeysFromXml(subscriptionXml.getSubscribedDocuments().getVariantList()));
        setSubscribedCollectionKeys(instantiateCollectionKeysFromXml(subscriptionXml.getSubscribedCollections().getCollectionList()));
        if (subscriptionXml.getLocale() != null)
            this.locale = LocaleHelper.parseLocale(subscriptionXml.getLocale());
        else
            this.locale = null;
    }

    private VariantKey[] instantiateVariantKeysFromXml(List<SubscriptionDocument.Subscription.SubscribedDocuments.Variant> variantsXml) {
        VariantKey[] variantKeys = new VariantKey[variantsXml.size()];
        for (int i = 0; i < variantsXml.size(); i++) {
            SubscriptionDocument.Subscription.SubscribedDocuments.Variant variantXml = variantsXml.get(i);
            variantKeys[i] = new VariantKey(variantXml.getDocumentId(), variantXml.getBranchId(), variantXml.getLanguageId());
        }
        return variantKeys;
    }

    private CollectionSubscriptionKey[] instantiateCollectionKeysFromXml(List<SubscriptionDocument.Subscription.SubscribedCollections.Collection> collectionsXml) {
        CollectionSubscriptionKey[] collectionKeys = new CollectionSubscriptionKey[collectionsXml.size()];
        for (int i = 0; i < collectionKeys.length; i++) {
            SubscriptionDocument.Subscription.SubscribedCollections.Collection collectionXml = collectionsXml.get(i);
            collectionKeys[i] = new CollectionSubscriptionKey(collectionXml.getCollectionId(), collectionXml.getBranchId(), collectionXml.getLanguageId());
        }
        return collectionKeys;
    }
}
