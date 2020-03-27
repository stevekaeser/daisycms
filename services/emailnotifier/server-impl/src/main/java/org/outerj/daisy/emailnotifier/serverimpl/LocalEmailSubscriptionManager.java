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
package org.outerj.daisy.emailnotifier.serverimpl;

import org.outerj.daisy.emailnotifier.*;
import org.outerj.daisy.emailnotifier.commonimpl.*;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.jdbcutil.JdbcHelper;

import java.sql.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

public class LocalEmailSubscriptionManager implements EmailSubscriptionManager {
    private Repository repository;
    private LocalEmailSubscriptionManagerProvider.Context context;
    private JdbcHelper jdbcHelper;
    private SubscriptionStrategyImpl subscriptionStrategy = new SubscriptionStrategyImpl();

    protected LocalEmailSubscriptionManager(Repository repository, LocalEmailSubscriptionManagerProvider.Context context,
            JdbcHelper jdbcHelper) {
        this.repository = repository;
        this.context = context;
        this.jdbcHelper = jdbcHelper;
    }

    public Subscription getSubscription() throws RepositoryException {
        return getSubscription(repository.getUserId());
    }

    private void storeSubscription(SubscriptionImpl subscription) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            deleteSubscriptionInt(subscription.getUserId(), conn);

            stmt = conn.prepareStatement("insert into emailntfy_subscriptions(user_id, document_events, schema_events, user_events, collection_events, acl_events, comment_events, locale) values (?,?,?,?,?,?,?,?)");
            stmt.setLong(1, subscription.getUserId());
            stmt.setBoolean(2, subscription.getReceiveDocumentEvents());
            stmt.setBoolean(3, subscription.getReceiveSchemaEvents());
            stmt.setBoolean(4, subscription.getReceiveUserEvents());
            stmt.setBoolean(5, subscription.getReceiveCollectionEvents());
            stmt.setBoolean(6, subscription.getReceiveAclEvents());
            stmt.setBoolean(7, subscription.getReceiveCommentEvents());
            stmt.setString(8, subscription.getLocale() != null ? LocaleHelper.getString(subscription.getLocale()) : null);
            stmt.execute();
            stmt.close();

            // store document subscriptions

            VariantKey[] variantKeys = subscription.getSubscribedVariantKeys();
            if (variantKeys.length > 0) {
                stmt = conn.prepareStatement("insert into document_subscriptions(user_id,doc_id,branch_id,lang_id) values(?,?,?,?)");
                stmt.setLong(1, subscription.getUserId());
                for (VariantKey variantKey : variantKeys) {
                    String documentId = variantKey.getDocumentId();
                    if (!documentId.equals(DOCUMENT_ID_WILDCARD))
                        documentId = repository.normalizeDocumentId(variantKey.getDocumentId());
                    stmt.setString(2, documentId);
                    stmt.setLong(3, variantKey.getBranchId());
                    stmt.setLong(4, variantKey.getLanguageId());
                    stmt.execute();
                }
                stmt.close();
            }

            // store collection subscriptions

            CollectionSubscriptionKey[] collectionKeys = subscription.getSubscribedCollectionKeys();
            if (collectionKeys.length > 0) {
                stmt = conn.prepareStatement("insert into collection_subscriptions(user_id,collection_id,branch_id,lang_id) values(?,?,?,?)");
                stmt.setLong(1, subscription.getUserId());
                for (CollectionSubscriptionKey collectionKey : collectionKeys) {
                    stmt.setLong(2, collectionKey.getCollectionId());
                    stmt.setLong(3, collectionKey.getBranchId());
                    stmt.setLong(4, collectionKey.getLanguageId());
                    stmt.execute();
                }
                stmt.close();
            }


            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("An error occured storing subscription information for user " + repository.getUserId(), e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private static final String SELECT_SUBSCRIPTION = "select user_id, document_events, schema_events, user_events, collection_events, acl_events, comment_events, locale from emailntfy_subscriptions";

    public Subscription getSubscription(long userId) throws RepositoryException {
        if (userId != repository.getUserId() && !repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can access the subscriptions of other users.");

        Connection conn = null;
        PreparedStatement stmt = null;
        SubscriptionLoadContext subscriptionLoadContext = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            stmt = conn.prepareStatement(SELECT_SUBSCRIPTION + " where user_id = ?");
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                subscriptionLoadContext = SubscriptionLoadContext.create(conn, jdbcHelper);
                return instatiateSubscriptionFromResultSet(rs, subscriptionLoadContext);
            } else {
                User user = repository.getUserManager().getUser(userId, false);
                return new SubscriptionImpl(subscriptionStrategy, user.getId());
            }
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while retrieving subscription information for user " + repository.getUserId(), e);
        } finally {
            if (subscriptionLoadContext != null)
                subscriptionLoadContext.cleanup();
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * Holds prepared statements for loading document and collection subscriptions.
     */
    static class SubscriptionLoadContext {
        private PreparedStatement documentSubStmt;
        private PreparedStatement collectionSubStmt;
        private JdbcHelper jdbcHelper;

        private SubscriptionLoadContext() {
        }

        static SubscriptionLoadContext create(Connection conn, JdbcHelper jdbcHelper) throws SQLException {
            SubscriptionLoadContext context = new SubscriptionLoadContext();
            context.documentSubStmt = conn.prepareStatement("select doc_id, branch_id, lang_id from document_subscriptions where user_id = ?");
            context.collectionSubStmt = conn.prepareStatement("select collection_id, branch_id, lang_id from collection_subscriptions where user_id = ?");
            context.jdbcHelper = jdbcHelper;
            return context;
        }

        public PreparedStatement getDocumentSubscriptionStmt() {
            return documentSubStmt;
        }

        public PreparedStatement getCollectionSubscriptionStmt() {
            return collectionSubStmt;
        }

        void cleanup() {
            jdbcHelper.closeStatement(documentSubStmt);
            jdbcHelper.closeStatement(collectionSubStmt);
        }
    }

    private SubscriptionImpl instatiateSubscriptionFromResultSet(ResultSet rs, SubscriptionLoadContext slContext) throws SQLException {
        long userId = rs.getLong("user_id");
        SubscriptionImpl subscription = new SubscriptionImpl(subscriptionStrategy, userId);
        subscription.setReceiveDocumentEvents(rs.getBoolean("document_events"));
        subscription.setReceiveSchemaEvents(rs.getBoolean("schema_events"));
        subscription.setReceiveUserEvents(rs.getBoolean("user_events"));
        subscription.setReceiveCollectionEvents(rs.getBoolean("collection_events"));
        subscription.setReceiveAclEvents(rs.getBoolean("acl_events"));
        subscription.setReceiveCommentEvents(rs.getBoolean("comment_events"));
        String locale = rs.getString("locale");
        if (locale != null)
            subscription.setLocale(LocaleHelper.parseLocale(locale));

        PreparedStatement stmt = slContext.getDocumentSubscriptionStmt();
        stmt.setLong(1, userId);
        rs = stmt.executeQuery();
        List<VariantKey> variantKeys = new ArrayList<VariantKey>();
        while (rs.next()) {
            variantKeys.add(new VariantKey(rs.getString(1), rs.getLong(2), rs.getLong(3)));
        }
        subscription.setSubscribedVariantKeys(variantKeys.toArray(new VariantKey[0]));

        stmt = slContext.getCollectionSubscriptionStmt();
        stmt.setLong(1, userId);
        rs = stmt.executeQuery();
        List<CollectionSubscriptionKey> collectionKeys = new ArrayList<CollectionSubscriptionKey>();
        while (rs.next()) {
            collectionKeys.add(new CollectionSubscriptionKey(rs.getLong(1), rs.getLong(2), rs.getLong(3)));
        }
        subscription.setSubscribedCollectionKeys(collectionKeys.toArray(new CollectionSubscriptionKey[0]));

        return subscription;
    }

    public void deleteSubscription() throws RepositoryException {
        deleteSubscription(repository.getUserId());
    }

    public void deleteSubscription(long userId) throws RepositoryException {
        if (userId != repository.getUserId() && !repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can delete the subscriptions of other users.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            deleteSubscriptionInt(userId, conn);
            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("An error occured while deleting subscription information for user " + repository.getUserId(), e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deleteSubscriptionInt(long userId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();

            stmt = conn.prepareStatement("delete from document_subscriptions where user_id = ?");
            stmt.setLong(1, userId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from collection_subscriptions where user_id = ?");
            stmt.setLong(1, userId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from emailntfy_subscriptions where user_id = ?");
            stmt.setLong(1, userId);
            stmt.execute();
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Subscriptions getSubscriptions() throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can access the list of all subscriptions.");

        Connection conn = null;
        Statement stmt = null;
        SubscriptionLoadContext subscriptionLoadContext = null;
        try {
            List<Subscription> subscriptions = new ArrayList<Subscription>();
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SELECT_SUBSCRIPTION);
            subscriptionLoadContext = SubscriptionLoadContext.create(conn, jdbcHelper);
            while (rs.next()) {
                subscriptions.add(instatiateSubscriptionFromResultSet(rs, subscriptionLoadContext));
            }
            return new SubscriptionsImpl(subscriptions.toArray(new Subscription[subscriptions.size()]));
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while retrieving the subscriptions.", e);
        } finally {
            if (subscriptionLoadContext != null)
                subscriptionLoadContext.cleanup();
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * @param query a query having the user_id as the first (only) item in the select clause
     */
    private Subscribers getSubscribers(String query) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can retrieve lists of subscribers.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            List<Subscriber> subscribers = new ArrayList<Subscriber>();
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setBoolean(1, true);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                long userId = rs.getLong(1);
                String localeString = rs.getString("locale");
                Locale locale = localeString != null ? LocaleHelper.parseLocale(localeString) : null;
                subscribers.add(new SubscriberImpl(userId, locale));
            }
            return new SubscribersImpl(subscribers.toArray(new Subscriber[subscribers.size()]));
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while retrieving subscribers.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Subscribers getAllDocumentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException {
        return getAllEventSubscribersForDocumentOrCollections(documentId, branchId, languageId, collections, "document_events");
    }

    private Subscribers getAllEventSubscribersForDocumentOrCollections(String documentId, long branchId, long languageId, long[] collections, String subscriptionColumn) throws RepositoryException {
        documentId = repository.normalizeDocumentId(documentId);
        StringBuilder query = new StringBuilder();
        query.append("select distinct(es.user_id), locale from emailntfy_subscriptions es");
        query.append(" left outer join document_subscriptions ds on (es.user_id = ds.user_id)");
        query.append(" left outer join collection_subscriptions cs on (es.user_id = cs.user_id)");
        query.append(" where es.").append(subscriptionColumn).append(" = ? and");
        query.append(" (");
        query.append("   (");
        query.append("         (ds.doc_id = '").append(documentId).append("' or ds.doc_id = '*')");
        query.append("     and (ds.branch_id = ").append(branchId).append(" or ds.branch_id = -1)");
        query.append("     and (ds.lang_id = ").append(languageId).append(" or ds.lang_id = -1)");
        query.append("   )");
        if (collections.length > 0) {
            for (long collection : collections) {
                query.append(" or");
                query.append(" (");
                query.append("       (cs.collection_id = ").append(collection).append(" or cs.collection_id = -1)");
                query.append("   and (cs.branch_id = ").append(branchId).append(" or cs.branch_id = -1)");
                query.append("   and (cs.lang_id = ").append(languageId).append(" or cs.lang_id = -1)");
                query.append(" )");
            }
        }
        query.append(" )");

        return getSubscribers(query.toString());
    }

    public Subscribers getAllUserEventSubscribers() throws RepositoryException {
        return getSubscribers("select user_id, locale from emailntfy_subscriptions where user_events = ?");
    }

    public Subscribers getAllCollectionEventSubscribers() throws RepositoryException {
        return getSubscribers("select user_id, locale from emailntfy_subscriptions where collection_events = ?");
    }

    public Subscribers getAllSchemaEventSubscribers() throws RepositoryException {
        return getSubscribers("select user_id, locale from emailntfy_subscriptions where schema_events = ?");
    }

    public Subscribers getAllAclEventSubscribers() throws RepositoryException {
        return getSubscribers("select user_id, locale from emailntfy_subscriptions where acl_events = ?");
    }

    public Subscribers getAllCommentEventSubscribers(String documentId, long branchId, long languageId, long[] collections) throws RepositoryException {
        return getAllEventSubscribersForDocumentOrCollections(documentId, branchId, languageId, collections, "comment_events");
    }

    public void addDocumentSubscription(VariantKey variantKey) throws RepositoryException {
        addDocumentSubscription(repository.getUserId(), variantKey);
    }

    public void addDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException {
        if (userId != repository.getUserId() && !repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can adjust the subscriptions of other users.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            // first check if this user has already a subscription, if not create one
            stmt = conn.prepareStatement("select 1 from emailntfy_subscriptions where user_id = ?");
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next()) {
                Subscription subscription = getSubscription(userId);
                subscription.setReceiveDocumentEvents(true);
                subscription.setSubscribedVariantKeys(new VariantKey[] {variantKey});
                subscription.save();
                return;
            }

            // if the user already had a subscription, directly work on the database...

            String documentId = variantKey.getDocumentId();
            if (!documentId.equals(DOCUMENT_ID_WILDCARD))
                documentId = repository.normalizeDocumentId(variantKey.getDocumentId());
            stmt = conn.prepareStatement("select 1 from document_subscriptions where user_id = ? and doc_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, userId);
            stmt.setString(2, documentId);
            stmt.setLong(3, variantKey.getBranchId());
            stmt.setLong(4, variantKey.getLanguageId());
            rs = stmt.executeQuery();
            boolean needsInsert = !rs.next();
            stmt.close();

            if (needsInsert) {
                stmt = conn.prepareStatement("insert into document_subscriptions(user_id, doc_id, branch_id, lang_id) values(?,?,?,?)");
                stmt.setLong(1, userId);
                stmt.setString(2, documentId);
                stmt.setLong(3, variantKey.getBranchId());
                stmt.setLong(4, variantKey.getLanguageId());
                stmt.execute();
                stmt.close();
            }

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("An error occured while adding the document subscription (user ID " + userId + ", " + variantKey.toString() + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public boolean isSubsribed(VariantKey variantKey) throws RepositoryException {
        return isSubsribed(repository.getUserId(), variantKey);
    }

    public boolean isSubsribed(long userId, VariantKey variantKey) throws RepositoryException {
        if (userId != repository.getUserId() && !repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can check the subscriptions of other users.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("select 1 from document_subscriptions where user_id = ? and doc_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, userId);
            stmt.setString(2, repository.normalizeDocumentId(variantKey.getDocumentId()));
            stmt.setLong(3, variantKey.getBranchId());
            stmt.setLong(4, variantKey.getLanguageId());
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while checking document subscription (user ID " + userId + ", " + variantKey.toString() + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deleteDocumentSubscription(VariantKey variantKey) throws RepositoryException {
        deleteDocumentSubscription(repository.getUserId(), variantKey);
    }

    public void deleteDocumentSubscription(long userId, VariantKey variantKey) throws RepositoryException {
        if (userId != repository.getUserId() && !repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can adjust the subscriptions of other users.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("delete from document_subscriptions where user_id = ? and doc_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, userId);
            stmt.setString(2, repository.normalizeDocumentId(variantKey.getDocumentId()));
            stmt.setLong(3, variantKey.getBranchId());
            stmt.setLong(4, variantKey.getLanguageId());
            stmt.execute();
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while removing document subscription (user ID " + userId + ", " + variantKey.toString() + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deleteAllSubscriptionsForDocument(String documentId) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can remove all subscriptions on a document.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("delete from document_subscriptions where doc_id = ?");
            stmt.setString(1, repository.normalizeDocumentId(documentId));
            stmt.execute();
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while removing document subscriptions (document ID " + documentId + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deleteAllSubscriptionsForDocumentVariant(VariantKey variantKey) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can remove all subscriptions on a document variant.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("delete from document_subscriptions where doc_id = ? and branch_id = ? and lang_id = ?");
            stmt.setString(1, repository.normalizeDocumentId(variantKey.getDocumentId()));
            stmt.setLong(2, variantKey.getBranchId());
            stmt.setLong(3, variantKey.getLanguageId());
            stmt.execute();
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while removing document variant subscriptions (" + variantKey.toString() + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public void deleteAllSubscriptionsForCollection(long collectionId) throws RepositoryException {
        if (!repository.isInRole(Role.ADMINISTRATOR))
            throw new RuntimeException("Only users with role administrator can remove all subscriptions on a collection.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try  {
            conn = context.getDataSource().getConnection();
            stmt = conn.prepareStatement("delete from collection_subscriptions where collection_id = ?");
            stmt.setLong(1, collectionId);
            stmt.execute();
        } catch (Throwable e) {
            throw new RepositoryException("An error occured while removing collection subscriptions (collection ID " + collectionId + ").", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public class Context {
        private Context() {
        }

        public Repository getRepository() {
            return repository;
        }
    }

    class SubscriptionStrategyImpl implements SubscriptionStrategy {
        public void storeSubscription(SubscriptionImpl subscription) throws RepositoryException {
            LocalEmailSubscriptionManager.this.storeSubscription(subscription);
        }
    }

}
