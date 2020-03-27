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
package org.outerj.daisy.emailnotifier.clientimpl;

import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.emailnotifier.Subscription;
import org.outerj.daisy.emailnotifier.Subscriptions;
import org.outerj.daisy.emailnotifier.Subscribers;
import org.outerj.daisy.emailnotifier.commonimpl.*;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryImpl;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.VariantKey;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.outerx.daisy.x10.*;

import java.util.Locale;
import java.util.List;

public class RemoteEmailSubscriptionManager implements EmailSubscriptionManager {
    private RemoteRepositoryImpl repository;
    private SubscriptionStrategyImpl subscriptionStrategy = new SubscriptionStrategyImpl();

    public RemoteEmailSubscriptionManager(RemoteRepositoryImpl repository) {
        this.repository = repository;
    }

    public Subscription getSubscription() throws RepositoryException {
        return getSubscription(repository.getUserId());
    }

    public Subscription getSubscription(long userId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        HttpMethod method = new GetMethod("/emailnotifier/subscription/" + userId);

        SubscriptionDocument subscriptionDocument = (SubscriptionDocument)httpClient.executeMethod(method, SubscriptionDocument.class, true);
        SubscriptionDocument.Subscription subscriptionXml = subscriptionDocument.getSubscription();
        SubscriptionImpl subscription = instantiateSubscriptionFromXml(subscriptionXml);
        return subscription;
    }

    private SubscriptionImpl instantiateSubscriptionFromXml(SubscriptionDocument.Subscription subscriptionXml) {
        SubscriptionImpl subscription = new SubscriptionImpl(subscriptionStrategy, subscriptionXml.getUserId());
        subscription.setFromXml(subscriptionXml);
        return subscription;
    }

    public void deleteSubscription() throws RepositoryException {
        deleteSubscription(repository.getUserId());
    }

    public void deleteSubscription(long userId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        HttpMethod method = new DeleteMethod("/emailnotifier/subscription/" + userId);

        httpClient.executeMethod(method, null, true);
    }

    public Subscriptions getSubscriptions() throws RepositoryException {
        return getSubscriptions("/emailnotifier/subscription");
    }

    private Subscriptions getSubscriptions(String path) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        HttpMethod method = new GetMethod(path);

        SubscriptionsDocument subscriptionsDocument = (SubscriptionsDocument)httpClient.executeMethod(method, SubscriptionsDocument.class, true);
        List<SubscriptionDocument.Subscription> subscriptionsXml = subscriptionsDocument.getSubscriptions().getSubscriptionList();
        SubscriptionImpl[] subscriptions = new SubscriptionImpl[subscriptionsXml.size()];
        for (int i = 0; i < subscriptionsXml.size(); i++) {
            subscriptions[i] = instantiateSubscriptionFromXml(subscriptionsXml.get(i));
        }
        return new SubscriptionsImpl(subscriptions);
    }

    private void storeSubscription(SubscriptionImpl subscription) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/emailnotifier/subscription/" + subscription.getUserId());
        method.setRequestEntity(new InputStreamRequestEntity(subscription.getXml().newInputStream()));

        httpClient.executeMethod(method, null, true);
    }

    public void addDocumentSubscription(VariantKey variantKey) throws RepositoryException {
        addDocumentSubscription(repository.getUserId(), variantKey);
    }

    public void addDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/emailnotifier/documentSubscription/" + variantKey.getDocumentId());
        NameValuePair[] queryString = {
            new NameValuePair("action", "add"),
            new NameValuePair("userId", String.valueOf(userId)),
            new NameValuePair("branch", String.valueOf(variantKey.getBranchId())),
            new NameValuePair("language", String.valueOf(variantKey.getLanguageId()))};
        method.setQueryString(queryString);

        httpClient.executeMethod(method, null, true);
    }

    public void deleteDocumentSubscription(VariantKey variantKey) throws RepositoryException {
        deleteDocumentSubscription(repository.getUserId(), variantKey);
    }

    public void deleteDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        PostMethod method = new PostMethod("/emailnotifier/documentSubscription/" + variantKey.getDocumentId());
        NameValuePair[] queryString = {
            new NameValuePair("action", "remove"),
            new NameValuePair("userId", String.valueOf(userId)),
            new NameValuePair("branch", String.valueOf(variantKey.getBranchId())),
            new NameValuePair("language", String.valueOf(variantKey.getLanguageId()))};
        method.setQueryString(queryString);

        httpClient.executeMethod(method, null, true);
    }

    public boolean isSubsribed(VariantKey variantKey) throws RepositoryException {
        return isSubsribed(repository.getUserId(), variantKey);
    }

    public boolean isSubsribed(long userId, VariantKey variantKey) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/emailnotifier/documentSubscription/" + variantKey.getDocumentId() + "/" + userId);
        NameValuePair[] queryString = {
            new NameValuePair("branch", String.valueOf(variantKey.getBranchId())),
            new NameValuePair("language", String.valueOf(variantKey.getLanguageId()))
        };
        method.setQueryString(queryString);
        SubscribedDocument subscribedDocument = (SubscribedDocument)httpClient.executeMethod(method, SubscribedDocument.class, true);
        return subscribedDocument.getSubscribed();
    }

    public void deleteAllSubscriptionsForDocument(String documentId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        DeleteMethod method = new DeleteMethod("/emailnotifier/documentSubscription/" + documentId);

        httpClient.executeMethod(method, null, true);
    }

    public void deleteAllSubscriptionsForDocumentVariant(VariantKey variantKey) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        DeleteMethod method = new DeleteMethod("/emailnotifier/documentSubscription/" + variantKey.getDocumentId());
        NameValuePair[] queryString = {
            new NameValuePair("branch", String.valueOf(variantKey.getBranchId())),
            new NameValuePair("language", String.valueOf(variantKey.getLanguageId()))
        };
        method.setQueryString(queryString);
        httpClient.executeMethod(method, null, true);
    }

    public void deleteAllSubscriptionsForCollection(long collectionId) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        DeleteMethod method = new DeleteMethod("/emailnotifier/collectionSubscription/" + collectionId);

        httpClient.executeMethod(method, null, true);
    }

    public Subscribers getAllDocumentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException {
        return getAllEventSubscribersForDocumentOrCollections(documentId, branchId, languageId, collections, "/emailnotifier/documentEventsSubscribers");
    }

    public Subscribers getAllCommentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException {
        return getAllEventSubscribersForDocumentOrCollections(documentId, branchId, languageId, collections, "/emailnotifier/commentEventsSubscribers");
    }

    private Subscribers getAllEventSubscribersForDocumentOrCollections(String documentId, long branchId, long languageId, long[] collections, String subscriptionResource) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod(subscriptionResource);

        StringBuilder collectionParam = new StringBuilder(collections.length * 4);
        for (int i = 0; i < collections.length; i++) {
            if (i > 0)
                collectionParam.append(',');
            collectionParam.append(collections[i]);
        }
        NameValuePair[] queryString = {
            new NameValuePair("documentId", documentId),
            new NameValuePair("branch", String.valueOf(branchId)),
            new NameValuePair("language", String.valueOf(languageId)),
            new NameValuePair("collectionIds", collectionParam.toString())};
        method.setQueryString(queryString);

        SubscribersDocument subscribersDocument = (SubscribersDocument)httpClient.executeMethod(method, SubscribersDocument.class, true);
        return instantiateSubscribers(subscribersDocument);
    }

    private Subscribers instantiateSubscribers(SubscribersDocument subscribersDocument) {
        List<SubscriberDocument.Subscriber> subscribersXml = subscribersDocument.getSubscribers().getSubscriberList();
        SubscriberImpl[] subscribers = new SubscriberImpl[subscribersXml.size()];
        for (int i = 0; i < subscribersXml.size(); i++) {
            String localeString = subscribersXml.get(i).getLocale();
            Locale locale = localeString != null ? LocaleHelper.parseLocale(localeString) : null;
            subscribers[i] = new SubscriberImpl(subscribersXml.get(i).getUserId(), locale);
        }
        return new SubscribersImpl(subscribers);
    }

    public Subscribers getAllUserEventSubscribers() throws RepositoryException {
        return getSubscribers("user");
    }

    public Subscribers getAllCollectionEventSubscribers() throws RepositoryException {
        return getSubscribers("collection");
    }

    public Subscribers getAllSchemaEventSubscribers() throws RepositoryException {
        return getSubscribers("schema");
    }

    public Subscribers getAllAclEventSubscribers() throws RepositoryException {
        return getSubscribers("acl");
    }

    private Subscribers getSubscribers(String eventName) throws RepositoryException {
        DaisyHttpClient httpClient = repository.getHttpClient();
        GetMethod method = new GetMethod("/emailnotifier/" + eventName + "EventsSubscribers");

        SubscribersDocument subscribersDocument = (SubscribersDocument)httpClient.executeMethod(method, SubscribersDocument.class, true);
        return instantiateSubscribers(subscribersDocument);
    }

    class SubscriptionStrategyImpl implements SubscriptionStrategy {
        public void storeSubscription(SubscriptionImpl subscription) throws RepositoryException {
            RemoteEmailSubscriptionManager.this.storeSubscription(subscription);
        }
    }
}
