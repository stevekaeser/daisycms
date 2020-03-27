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

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

/**
 * Management of email notification subscriptions.
 *
 * <p>This is an optional repository extension component.
 * 
 * <p>The EmailSubscriptionManager extension is obtained from the
 * {@link org.outerj.daisy.repository.Repository Repository} as follows:
 *
 * <pre>
 * subscriptionManager = (EmailSubscriptionManager)repository.getExtension("EmailSubscriptionManager");
 * </pre>
 *
 * <p>In the remote repository API, the EmailSubscriptionManager extension can be registered as follows:
 *
 * <pre>
 * RemoteRepositoryManager repositoryManager = ...;
 * repositoryManager.registerExtension("EmailSubscriptionManager",
 *     new Packages.org.outerj.daisy.emailnotifier.clientimpl.RemoteEmailSubscriptionManagerProvider());
 * </pre>
 *
 */
public interface EmailSubscriptionManager {

    static final String DOCUMENT_ID_WILDCARD = "*";

    /**
     * Gets subscription information for the current user. If the user doesn't have
     * a subscription yet, this also returns a Subscription object.
     */
    public Subscription getSubscription() throws RepositoryException;

    /**
     * Retrieves the subscription of another user. Only users acting as administrator
     * can do this.
     */
    public Subscription getSubscription(long userId) throws RepositoryException;

    /**
     * Removes the subscription for the current user. If the user does not have a
     * subscription, this method should silently return.
     */
    public void deleteSubscription() throws RepositoryException;

    /**
     * Deletes the subscription of another user. Only users acting as administrator can
     * do this. If the user does not have a subscription, this method should silently return.
     */
    public void deleteSubscription(long userId) throws RepositoryException;

    /**
     * Get all available subscriptions. Only users acting as administrator can do this.
     */
    public Subscriptions getSubscriptions() throws RepositoryException;

    /**
     * @param variantKey documentId can be {@link #DOCUMENT_ID_WILDCARD} (*), and
     * the branchId and languageId components can be -1 to indicate "any document/branch/language".
     */
    public void addDocumentSubscription(VariantKey variantKey) throws RepositoryException;

    public void addDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException;

    /**
     * Checks if the user is subscribed to the specified document variant.
     * This will only return true if an exact match for the subscription
     * is found, thus -1 for branchId and/or languageId doesn't work as
     * a wildcard.
     */
    public boolean isSubsribed(VariantKey variantKey) throws RepositoryException;

    public boolean isSubsribed(long userId, VariantKey variantKey) throws RepositoryException;

    public void deleteDocumentSubscription(VariantKey variantKey) throws RepositoryException;

    public void deleteDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException;

    /**
     * Deletes subscriptions for the specified document variant for all users (useful if eg the
     * document variant has been deleted). Can only be done by users acting in the Administrator role.
     */
    public void deleteAllSubscriptionsForDocumentVariant(VariantKey variantKey) throws RepositoryException;

    /**
     * Deletes subscriptions for the specified document for all users (useful if eg the
     * document variant has been deleted). Can only be done by users acting in the Administrator role.
     */
    public void deleteAllSubscriptionsForDocument(String documentId) throws RepositoryException;

    public void deleteAllSubscriptionsForCollection(long collectionId) throws RepositoryException;

    /**
     * Returns the users subscribed to changes for documents.
     *
     * @param documentId  the id of the document
     * @param branchId can be -1 to specify 'whatever branch the subscription applies to'
     * @param languageId can be -1 to specify 'whatever language the subscription applies to'
     * @param collections  the collections the document belongs to.
     */
    public Subscribers getAllDocumentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException;

    public Subscribers getAllUserEventSubscribers() throws RepositoryException;

    public Subscribers getAllCollectionEventSubscribers() throws RepositoryException;

    public Subscribers getAllSchemaEventSubscribers() throws RepositoryException;

    public Subscribers getAllAclEventSubscribers() throws RepositoryException;

    public Subscribers getAllCommentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException;
}
