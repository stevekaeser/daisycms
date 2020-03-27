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
package org.outerj.daisy.repository.serverimpl;

import org.outerj.daisy.repository.commonimpl.CollectionStrategy;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocumentCollectionImpl;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.CollectionCreatedDocument;
import org.outerx.daisy.x10.CollectionDocument;
import org.outerx.daisy.x10.CollectionUpdatedDocument;
import org.outerx.daisy.x10.CollectionDeletedDocument;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class LocalCollectionStrategy extends AbstractLocalStrategy implements CollectionStrategy {
    private static final String STMT_SELECT_ALL_FROM_COLLECTIONS = "select id, name, last_modified, last_modifier, updatecount from collections";

    public LocalCollectionStrategy(LocalRepositoryManager.Context context, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        super(context, systemUser, jdbcHelper);
    }

    public void store(DocumentCollectionImpl collection) throws RepositoryException {
        logger.debug("begin storage of collection");
        DocumentCollectionImpl.IntimateAccess collectionInt = collection.getIntimateAccess(this);

        if (!collectionInt.getCurrentUser().isInAdministratorRole())
            throw new RepositoryException("Only Administrators can create or update collections.");
        long id = collection.getId();
        boolean isNew = (id == -1);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            logger.debug("connection fetched");
            String collectionName = collection.getName();
            java.util.Date lastModified = new Date();
            logger.debug("currently in object: id "+id+" colname: "
                    +collectionName+" lastMod: "+lastModified);
            /* The user that requested the Collection is now persisting it,
             * therefore he will be the last modifier
             */
            long lastModifier = collectionInt.getCurrentUser().getId();
            XmlObject eventDescription;

            if (id == -1) {
                //a new collection must be stored in the data store
                stmt = conn.prepareStatement("insert into collections(id, name, last_modified, last_modifier, updatecount) values (?,?,?,?,?)");
                id = context.getNextCollectionId();
                stmt.setLong(1, id);
                stmt.setString(2, collectionName);
                stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
                stmt.setLong(4, lastModifier);
                stmt.setLong(5, 1L);
                stmt.execute();
                stmt.close();

                eventDescription = createCollectionCreatedEvent(collection, id, lastModified);
            } else {
                //we have to update an existing collection

                // check if nobody else changed it in the meantime
                stmt = conn.prepareStatement("select updatecount from collections where id = ? " + jdbcHelper.getSharedLockClause());
                stmt.setLong(1, id);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    throw new RepositoryException("Unexpected error: the Collection with id " + id + " does not exist in the database.");
                } else {
                    long dbUpdateCount = rs.getLong(1);
                    if (dbUpdateCount != collection.getUpdateCount())
                        throw new ConcurrentUpdateException(DocumentCollection.class.getName(), String.valueOf(collection.getId()));
                }
                stmt.close();

                DocumentCollectionImpl oldCollection = loadCollectionInt(collection.getId(), collectionInt.getCurrentUser(), conn);

                stmt = conn.prepareStatement("update collections set name=?, last_modified=?, last_modifier=?, updatecount=? where id=?");
                stmt.setString(1, collectionName);
                stmt.setTimestamp(2, new Timestamp(lastModified.getTime()));
                stmt.setLong(3, lastModifier);
                stmt.setLong(4, collection.getUpdateCount() + 1);
                stmt.setLong(5, id);
                stmt.execute();
                stmt.close();

                eventDescription = createCollectionUpdatedEvent(oldCollection, collection, lastModified, collection.getUpdateCount() + 1);
            }

            eventHelper.createEvent(eventDescription, isNew ? "CollectionCreated" : "CollectionUpdated", conn);

            //everything went ok, so we can actively update the collection OBJECT as well.
            collectionInt.saved(id, collectionName, lastModified, lastModifier, collection.getUpdateCount() + 1);
            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error storing collection.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            logger.debug("collection storage complete");
        }

        if (isNew)
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_CREATED, new Long(id), collection.getUpdateCount());
        else
            context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_UPDATED, new Long(id), collection.getUpdateCount());
    }

    public DocumentCollectionImpl loadCollection(long collectionId, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);
            return loadCollectionInt(collectionId, user, conn);
        } catch (Throwable e) {
            if (e instanceof RepositoryException)
                throw (RepositoryException)e;
            throw new RepositoryException("Error loading collection.", e);
        } finally {
            jdbcHelper.closeConnection(conn);
        }
    }

    private DocumentCollectionImpl loadCollectionInt(long collectionId, AuthenticatedUser user, Connection conn) throws SQLException, RepositoryException {
        PreparedStatement stmt = null;
        try {
            logger.debug("begin fetching collection with collectionId "+collectionId);
            stmt = conn.prepareStatement("select id, name, last_modified, last_modifier, updatecount from collections where id = ? ");
            stmt.setLong(1, collectionId);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new CollectionNotFoundException(collectionId);

            DocumentCollectionImpl collection;

            String name = rs.getString(2);
            Date lastModified = rs.getTimestamp(3);
            long lastModifier = rs.getLong(4);
            long updateCount = rs.getLong(5);

            collection = new DocumentCollectionImpl(this, name, user);
            DocumentCollectionImpl.IntimateAccess collectionInt = collection.getIntimateAccess(this);
            collectionInt.saved(collectionId, name, lastModified, lastModifier, updateCount);
            return collection;
        } finally {
            jdbcHelper.closeStatement(stmt);
            logger.debug("collection load completed");
        }
    }

    public DocumentCollectionImpl loadCollectionByName(String name, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        long id;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select id from collections where name = ?");
            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();
            if (!rs.next())
                throw new CollectionNotFoundException(name);

            id = rs.getLong(1);
            stmt.close();

            return loadCollectionInt(id, user, conn);
        } catch (Throwable e) {
            throw new RepositoryException("Error loading collection.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Collection<DocumentCollectionImpl> loadCollections(AuthenticatedUser user) throws RepositoryException {
        PreparedStatement stmt = null;
        Connection conn = null;

        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement(STMT_SELECT_ALL_FROM_COLLECTIONS);
            ResultSet rs = stmt.executeQuery();

            List<DocumentCollectionImpl> list = new ArrayList<DocumentCollectionImpl>();
            while (rs.next()) {
                DocumentCollectionImpl coll = getDocumentCollectionImpl(user, rs);
                list.add(coll);
            }

            return list;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading collections.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * Note that this method does NOT call next() on the ResultSet.
     *
     * @param user the user requesting the DocumentCollection
     * @param rs the resultset that has been created for the collections table
     * @return a DocumentCollectionImpl with the values obtained from the ResultSet
     * @throws java.sql.SQLException
     */
    private DocumentCollectionImpl getDocumentCollectionImpl(AuthenticatedUser user, ResultSet rs) throws SQLException {
        DocumentCollectionImpl collection;

        long id = rs.getLong(1);
        String name = rs.getString(2);
        Date lastModified = rs.getTimestamp(3);
        long lastModifier = rs.getLong(4);
        long updateCount = rs.getLong(5);

        collection = new DocumentCollectionImpl(this, name, user);
        DocumentCollectionImpl.IntimateAccess collectionInt = collection.getIntimateAccess(this);
        collectionInt.saved(id, name, lastModified, lastModifier, updateCount);
        return collection;
    }

    public void deleteCollection(long collectionId, AuthenticatedUser user) throws RepositoryException {
        logger.debug("begin deletion of collection");

        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can delete collections.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            // we'll need a transaction here...
            jdbcHelper.startTransaction(conn);

            // retrieve collection, needed for collection deleted event
            DocumentCollectionImpl collection = loadCollectionInt(collectionId, user, conn);

            logger.debug("start by removing all the child records");

            stmt = conn.prepareStatement("delete from document_collections where collection_id=?");
            stmt.setLong(1, collectionId);

            // remark that it IS possible that no records are affected, in case the collection
            // has been defined but a document never was added to the collection
            stmt.executeUpdate();
            stmt.close();

            // if the previous action went ok, we can delete the collection itself

            stmt = conn.prepareStatement("delete from collections where id=?");
            stmt.setLong(1, collectionId);
            int amountOfRecordsModified = stmt.executeUpdate();
            stmt.close();

            if (amountOfRecordsModified == 0)
                throw new RepositoryException("This collection was not found in the repository!");

            XmlObject eventDescription = createCollectionDeletedEvent(collection);
            eventHelper.createEvent(eventDescription, "CollectionDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error deleting collection.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
            logger.debug("collection deletion complete");
        }

        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.COLLECTION_DELETED, new Long(collectionId), -1);
    }

    private CollectionCreatedDocument createCollectionCreatedEvent(DocumentCollectionImpl collection, long collectionId, Date lastModified) {
        CollectionDocument.Collection collectionXml = collection.getXml().getCollection();
        collectionXml.setId(collectionId);
        collectionXml.setLastModified(getCalendar(lastModified));
        collectionXml.setLastModifier(collection.getIntimateAccess(this).getCurrentUser().getId());
        collectionXml.setUpdatecount(1);

        CollectionCreatedDocument collectionCreatedDocument = CollectionCreatedDocument.Factory.newInstance();
        collectionCreatedDocument.addNewCollectionCreated().addNewNewCollection().setCollection(collectionXml);

        return collectionCreatedDocument;
    }

    private CollectionUpdatedDocument createCollectionUpdatedEvent(DocumentCollectionImpl oldCollection, DocumentCollectionImpl newCollection, Date lastModified, long newUpdateCount) {
        CollectionUpdatedDocument collectionUpdatedDocument = CollectionUpdatedDocument.Factory.newInstance();
        CollectionUpdatedDocument.CollectionUpdated collectionUpdated = collectionUpdatedDocument.addNewCollectionUpdated();

        collectionUpdated.addNewOldCollection().setCollection(oldCollection.getXml().getCollection());

        CollectionDocument.Collection newCollectionXml = newCollection.getXml().getCollection();
        newCollectionXml.setLastModifier(newCollection.getIntimateAccess(this).getCurrentUser().getId());
        newCollectionXml.setLastModified(getCalendar(lastModified));
        newCollectionXml.setUpdatecount(newUpdateCount);

        collectionUpdated.addNewNewCollection().setCollection(newCollectionXml);

        return collectionUpdatedDocument;
    }

    private CollectionDeletedDocument createCollectionDeletedEvent(DocumentCollectionImpl collection) {
        CollectionDeletedDocument collectionDeletedDocument = CollectionDeletedDocument.Factory.newInstance();
        CollectionDeletedDocument.CollectionDeleted collectionDeleted = collectionDeletedDocument.addNewCollectionDeleted();
        collectionDeleted.addNewDeletedCollection().setCollection(collection.getXml().getCollection());
        collectionDeleted.setDeletedTime(new GregorianCalendar());
        collectionDeleted.setDeleterId(collection.getIntimateAccess(this).getCurrentUser().getId());
        return collectionDeletedDocument;
    }
}
