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
package org.outerj.daisy.repository.serverimpl.acl;

import org.outerj.daisy.repository.commonimpl.acl.AclStrategy;
import org.outerj.daisy.repository.commonimpl.acl.AclImpl;
import org.outerj.daisy.repository.commonimpl.acl.AclEvaluationContext;
import org.outerj.daisy.repository.commonimpl.acl.AccessDetailsImpl;
import org.outerj.daisy.repository.commonimpl.*;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.repository.serverimpl.EventHelper;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.query.model.PredicateExpr;
import org.outerj.daisy.query.model.AclConditionViolation;
import org.outerj.daisy.query.model.ExprDocData;
import org.outerj.daisy.query.ExtQueryContext;
import org.outerj.daisy.query.EvaluationInfo;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.outerx.daisy.x10.AclUpdatedDocument;
import org.outerx.daisy.x10.AclDocument;

import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Server-side implementation of the AclStrategy interface.
 *
 */
public class LocalAclStrategy implements AclStrategy, AclEvaluationContext {
    /**
     * A lock controlling who (which thread) can read or write ACLs from/to the database.
     * This is used to serialize all database access (related to ACLs).
     */
    private Lock lock = new ReentrantLock();
    /**
     * Map containing cached copies of the ACLs which are used for ACL-evaluation.
     */
    private final Map<Long, Acl> acls = new HashMap<Long, Acl>();
    private AuthenticatedUser systemUser;
    private LocalRepositoryManager.Context context;
    private JdbcHelper jdbcHelper;
    private EventHelper eventHelper;

    public LocalAclStrategy(LocalRepositoryManager.Context context, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        this.context = context;
        this.systemUser = systemUser;
        this.jdbcHelper = jdbcHelper;
        this.eventHelper = new EventHelper(context, jdbcHelper);
    }

    public AclImpl loadAcl(long id, AuthenticatedUser user) throws RepositoryException {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RepositoryException("Could not get an ACL lock.", e);
        }
        try {
            return loadAclInternal(id, user);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Loads an ACL, assuming the lock has already been acquired by the caller.
     */
    private AclImpl loadAclInternal(long id, AuthenticatedUser user) throws RepositoryException {
        if (id != 1 && id != 2)
            throw new RepositoryException("Invalid ACL id: " + id);

        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtEntries = null;
        PreparedStatement stmtDetails = null;
        try {
            conn = context.getDataSource().getConnection();
            Date lastModified;
            long lastModifier;
            long updateCount;

            stmt = conn.prepareStatement("select last_modified, last_modifier, updatecount from acls where id = ?");
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                // record for this ACL did not yet exist, make it now
                stmt.close();
                lastModified = new Date();
                lastModifier = systemUser.getId();
                updateCount = 1;
                createInitialAclRecord(conn, id, lastModified, lastModifier);
            } else {
                lastModified = rs.getTimestamp("last_modified");
                lastModifier = rs.getLong("last_modifier");
                updateCount = rs.getLong("updatecount");
                stmt.close();
            }

            AclImpl acl = new AclImpl(this, lastModified, lastModifier, id, user, updateCount);

            stmtEntries = conn.prepareStatement("select id, subject_user_id, subject_role_id, subject_type, perm_read, perm_write, perm_publish, perm_delete, read_detail, write_detail, publish_detail from acl_entries where acl_object_id = ? and acl_id = ? order by id");
            stmtEntries.setLong(2, id);
            stmtDetails = conn.prepareStatement("select ad_type, ad_data from acl_accessdetail where acl_id = ? and acl_object_id = ? and acl_entry_id = ? and acl_permission = ?");
            stmtDetails.setLong(1, id);
            stmt = conn.prepareStatement("select id, objectspec from acl_objects where acl_id = ? order by id");
            stmt.setLong(1, id);
            rs = stmt.executeQuery();

            while (rs.next()) {
                long objectId = rs.getLong(1);
                String objectExpr = rs.getString(2);
                AclObject aclObject = acl.createNewObject(objectExpr);

                stmtEntries.setLong(1, objectId);
                ResultSet rsEntries = stmtEntries.executeQuery();
                while (rsEntries.next()) {
                    AclSubjectType subjectType = subjectTypeFromString(rsEntries.getString("subject_type"));
                    long subjectValue = -1;
                    if (subjectType == AclSubjectType.USER) {
                        subjectValue = rsEntries.getLong("subject_user_id");
                    } else if (subjectType == AclSubjectType.ROLE) {
                        subjectValue = rsEntries.getLong("subject_role_id");
                    }
                    AclEntry aclEntry = aclObject.createNewEntry(subjectType, subjectValue);
                    aclEntry.set(AclPermission.READ, actionTypeFromString(rsEntries.getString("perm_read")));
                    aclEntry.set(AclPermission.WRITE, actionTypeFromString(rsEntries.getString("perm_write")));
                    aclEntry.set(AclPermission.PUBLISH, actionTypeFromString(rsEntries.getString("perm_publish")));
                    aclEntry.set(AclPermission.DELETE, actionTypeFromString(rsEntries.getString("perm_delete")));

                    long entryId = rsEntries.getLong("id");

                    if (rsEntries.getBoolean("read_detail")) {
                        if (aclEntry.get(AclPermission.READ) != AclActionType.GRANT)
                            throw new RuntimeException("Unexpected situation: read access details available but read permission not granted.");

                        AccessDetails details = new AccessDetailsImpl(acl, AclPermission.READ);
                        readDetails(stmtDetails, objectId, entryId, details);
                        aclEntry.set(AclPermission.READ, AclActionType.GRANT, details);
                    }

                    if (rsEntries.getBoolean("write_detail")) {
                        if (aclEntry.get(AclPermission.WRITE) != AclActionType.GRANT)
                            throw new RuntimeException("Unexpected situation: write access details available but write permission not granted.");

                        AccessDetails details = new AccessDetailsImpl(acl, AclPermission.WRITE);
                        readDetails(stmtDetails, objectId, entryId, details);
                        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT, details);
                    }

                    if (rsEntries.getBoolean("publish_detail")) {
                        if (aclEntry.get(AclPermission.PUBLISH) != AclActionType.GRANT)
                            throw new RuntimeException("Unexpected situation: publish access details available but publish permission not granted.");

                        AccessDetails details = new AccessDetailsImpl(acl, AclPermission.PUBLISH);
                        readDetails(stmtDetails, objectId, entryId, details);
                        aclEntry.set(AclPermission.PUBLISH, AclActionType.GRANT, details);
                    }

                    aclObject.add(aclEntry);
                }
                rsEntries.close();

                acl.add(aclObject);
            }
            return acl;
        } catch (Throwable e) {
            throw new RepositoryException("Error loading ACL.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeStatement(stmtEntries);
            jdbcHelper.closeStatement(stmtDetails);
            jdbcHelper.closeConnection(conn);
        }
    }

    private void readDetails(PreparedStatement stmt, long objectId, long entryId, AccessDetails details) throws SQLException {
        stmt.setLong(2, objectId);
        stmt.setLong(3, entryId);
        stmt.setString(4, stringFromPermission(details.getPermission()));
        ResultSet detailsRs = stmt.executeQuery();

        while (detailsRs.next()) {
            String type = detailsRs.getString("ad_type");
            String data = detailsRs.getString("ad_data");
            if (type.equals("allowfield")) {
                details.addAccessibleField(data);
            } else if (type.equals("allowpart")) {
                details.addAccessiblePart(data);
            } else {
                // assume it is a detail permission
                details.set(AclDetailPermission.fromString(type), AclActionType.fromString(data));
            }
        }
    }

    private void createInitialAclRecord(Connection conn, long id, Date lastModified, long lastModifier) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("insert into acls(id, name, last_modified, last_modifier, updatecount) values (?,?,?,?,?)");
            stmt.setLong(1, id);
            stmt.setString(2, getAclName(id));
            stmt.setTimestamp(3, new Timestamp(lastModified.getTime()));
            stmt.setLong(4, lastModifier);
            stmt.setLong(5, 1L);
            stmt.execute();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }
    }

    private String getAclName(long id) {
        if (id == 1)
            return "live";
        else
            return "staging";
    }

    private AclSubjectType subjectTypeFromString(String subjectType) {
        if (subjectType.equals("U"))
            return AclSubjectType.USER;
        else if (subjectType.equals("R"))
            return AclSubjectType.ROLE;
        else if (subjectType.equals("E"))
            return AclSubjectType.EVERYONE;
        else if (subjectType.equals("O"))
            return AclSubjectType.OWNER;
        else
            throw new RuntimeException("Got unrecognized subject type string from database: " + subjectType);
    }

    private AclActionType actionTypeFromString(String actionType) {
        if (actionType.equals("G"))
            return AclActionType.GRANT;
        else if (actionType.equals("D"))
            return AclActionType.DENY;
        else if (actionType.equals("N"))
            return AclActionType.DO_NOTHING;
        else
            throw new RuntimeException("Got unrecognized action type string from database: " + actionType);
    }

    private String stringFromSubjectType(AclSubjectType subjectType) {
        switch (subjectType) {
            case USER:
                return "U";
            case ROLE:
                return "R";
            case EVERYONE:
                return "E";
            case OWNER:
                return "O";
            default:
                throw new RuntimeException("Unrecognized ACL subject type: " + subjectType);
        }
    }

    private String stringFromActionType(AclActionType actionType) {
        switch (actionType) {
            case DENY:
                return "D";
            case GRANT:
                return "G";
            case DO_NOTHING:
                return "N";
            default:
                throw new RuntimeException("Unrecognized ACL action type: " + actionType);
        }
    }

    private String stringFromPermission(AclPermission permission) {
        switch (permission) {
            case READ:
                return "R";
            case WRITE:
                return "W";
            case DELETE:
                return "D";
            case PUBLISH:
                return "P";
            default:
                throw new RuntimeException("Unrecognized ACL permission: " + permission);
        }
    }

    public void storeAcl(AclImpl acl) throws RepositoryException {
        if (!acl.getIntimateAccess(this).getCurrentModifier().isInAdministratorRole())
            throw new RepositoryException("Only the Administrator can store ACLs.");

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RepositoryException("Could not get an ACL lock.", e);
        }
        try {
            storeAclInternal(acl, false);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Stores an ACL, assuming the lock has already been acquired by the caller.
     */
    private void storeAclInternal(AclImpl acl, boolean forceOverwrite) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        PreparedStatement stmtInsertObject = null;
        PreparedStatement stmtInsertEntry = null;
        PreparedStatement stmtInsertDetail = null;
        ResultSet rs;
        AclImpl.IntimateAccess aclInt = acl.getIntimateAccess(this);
        long newUpdateCount;
        try {

            Date lastModified = new Date();
            long lastModifier = aclInt.getCurrentModifier().getId();

            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            AclImpl oldAcl = loadAclInternal(aclInt.getId(), aclInt.getCurrentModifier());

            stmt = conn.prepareStatement("select updatecount from acls where id = ?");
            stmt.setLong(1, aclInt.getId());
            rs = stmt.executeQuery();

            if (!rs.next()) {
                // Normally you will only be able to save an ACL if you first retrieved it, so at first
                // it would appear that this situation can't occur, but it can occur via copyStagingToLive
                createInitialAclRecord(conn, aclInt.getId(), new Date(), systemUser.getId());
            } else if (!forceOverwrite && acl.getUpdateCount() != rs.getLong(1)) {
                throw new ConcurrentUpdateException(Acl.class.getName(), String.valueOf(aclInt.getId()));
            }

            stmt.close();

            newUpdateCount = acl.getUpdateCount() + 1;

            // update lastmodifier/lastmodified information
            stmt = conn.prepareStatement("update acls set last_modified=?, last_modifier=?, updatecount=? where id=?");
            stmt.setTimestamp(1, new Timestamp(lastModified.getTime()));
            stmt.setLong(2, lastModifier);
            stmt.setLong(3, newUpdateCount);
            stmt.setLong(4, aclInt.getId());
            stmt.execute();
            stmt.close();

            // drop existing records for this ACL
            stmt = conn.prepareStatement("delete from acl_objects where acl_id = ?");
            stmt.setLong(1, aclInt.getId());
            stmt.execute();
            stmt.close();
            stmt = conn.prepareStatement("delete from acl_entries where acl_id = ?");
            stmt.setLong(1, aclInt.getId());
            stmt.execute();
            stmt.close();
            stmt = conn.prepareStatement("delete from acl_accessdetail where acl_id = ?");
            stmt.setLong(1, aclInt.getId());
            stmt.execute();
            stmt.close();

            stmtInsertObject = conn.prepareStatement("insert into acl_objects(acl_id, id, objectspec) values(?,?,?)");
            stmtInsertEntry = conn.prepareStatement("insert into acl_entries(acl_id, acl_object_id, id, subject_user_id, subject_role_id, subject_type, perm_read, perm_write, perm_publish, perm_delete, read_detail, write_detail, publish_detail) values(?,?,?,?,?,?,?,?,?,?,?,?,?)");
            stmtInsertDetail = conn.prepareStatement("insert into acl_accessdetail(acl_id, acl_object_id, acl_entry_id, acl_permission, ad_type, ad_data) values(?,?,?,?,?,?)");
            // insert the new records
            for (int i = 0; i < acl.size(); i++) {
                AclObject aclObject = acl.get(i);

                stmtInsertObject.setLong(1, aclInt.getId());
                stmtInsertObject.setLong(2, i);
                stmtInsertObject.setString(3, aclObject.getObjectExpr());
                stmtInsertObject.execute();

                for (int j = 0; j < aclObject.size(); j++) {
                    AclEntry aclEntry = aclObject.get(j);

                    stmtInsertEntry.setLong(1, aclInt.getId());
                    stmtInsertEntry.setLong(2, i);
                    stmtInsertEntry.setLong(3, j);

                    switch (aclEntry.getSubjectType()) {
                        case USER:
                            stmtInsertEntry.setLong(4, aclEntry.getSubjectValue());
                            stmtInsertEntry.setNull(5, Types.BIGINT);
                            break;
                        case ROLE:
                            stmtInsertEntry.setNull(4, Types.BIGINT);
                            stmtInsertEntry.setLong(5, aclEntry.getSubjectValue());
                            break;
                        case EVERYONE:
                        case OWNER:
                            stmtInsertEntry.setNull(4, Types.BIGINT);
                            stmtInsertEntry.setNull(5, Types.BIGINT);
                            break;
                        default:
                            // This situation should never occur
                            throw new RuntimeException("Encountered unexpected value for AclSubjectType: " + aclEntry.getSubjectType());
                    }

                    stmtInsertEntry.setString(6, stringFromSubjectType(aclEntry.getSubjectType()));
                    stmtInsertEntry.setString(7, stringFromActionType(aclEntry.get(AclPermission.READ)));
                    stmtInsertEntry.setString(8, stringFromActionType(aclEntry.get(AclPermission.WRITE)));
                    stmtInsertEntry.setString(9, stringFromActionType(aclEntry.get(AclPermission.PUBLISH)));
                    stmtInsertEntry.setString(10, stringFromActionType(aclEntry.get(AclPermission.DELETE)));

                    AccessDetails readDetails = aclEntry.getDetails(AclPermission.READ);
                    stmtInsertEntry.setBoolean(11, readDetails != null);
                    AccessDetails writeDetails = aclEntry.getDetails(AclPermission.WRITE);
                    stmtInsertEntry.setBoolean(12, writeDetails != null);
                    AccessDetails publishDetails = aclEntry.getDetails(AclPermission.PUBLISH);
                    stmtInsertEntry.setBoolean(13, publishDetails != null);
                    stmtInsertEntry.execute();

                    if (readDetails != null)
                        insertAccessDetails(readDetails, stmtInsertDetail, aclInt.getId(), i, j);

                    if (writeDetails != null)
                        insertAccessDetails(writeDetails, stmtInsertDetail, aclInt.getId(), i, j);

                    if (publishDetails != null)
                        insertAccessDetails(publishDetails, stmtInsertDetail, aclInt.getId(), i, j);
                }
            }

            eventHelper.createEvent(createAclUpdatedEvent(oldAcl, acl, lastModified, newUpdateCount), "AclUpdated", conn);

            conn.commit();

            // update object state
            aclInt.setLastModified(lastModified);
            aclInt.setLastModifier(lastModifier);
            aclInt.setUpdateCount(acl.getUpdateCount() + 1);

            // invalidate our cached copy of this ACL.
            invalidateCachedAcl(aclInt.getId());

        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Error storing ACL.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeStatement(stmtInsertObject);
            jdbcHelper.closeStatement(stmtInsertEntry);
            jdbcHelper.closeStatement(stmtInsertDetail);
            jdbcHelper.closeConnection(conn);
        }

        context.getCommonRepository().fireRepositoryEvent(RepositoryEventType.ACL_UPDATED, new Long(aclInt.getId()), newUpdateCount);
    }

    private void insertAccessDetails(AccessDetails details, PreparedStatement stmt, long aclId, long objectId, long entryId) throws SQLException {
        stmt.setLong(1, aclId);
        stmt.setLong(2, objectId);
        stmt.setLong(3, entryId);
        stmt.setString(4, stringFromPermission(details.getPermission()));

        for (AclDetailPermission permission : AclDetailPermission.values()) {
            if (permission.appliesTo(details.getPermission())) {
                if (details.get(permission) != AclActionType.DO_NOTHING) {
                    stmt.setString(5, permission.toString());
                    stmt.setString(6, details.get(permission).toString());
                    stmt.execute();
                }
            }
        }

        for (String field : details.getAccessibleFields()) {
            stmt.setString(5, "allowfield");
            stmt.setString(6, field);
            stmt.execute();
        }

        for (String part : details.getAccessibleParts()) {
            stmt.setString(5, "allowpart");
            stmt.setString(6, part);
            stmt.execute();
        }

    }

    private AclUpdatedDocument createAclUpdatedEvent(AclImpl oldAcl, AclImpl newAcl, Date lastModified, long newUpdateCount) {
        AclUpdatedDocument aclUpdatedDocument = AclUpdatedDocument.Factory.newInstance();
        AclUpdatedDocument.AclUpdated aclUpdated = aclUpdatedDocument.addNewAclUpdated();

        aclUpdated.addNewOldAcl().setAcl(oldAcl.getXml().getAcl());

        AclDocument.Acl newAclXml = newAcl.getXml().getAcl();
        newAclXml.setLastModified(getCalendar(lastModified));
        newAclXml.setLastModifier(newAcl.getIntimateAccess(this).getCurrentModifier().getId());
        newAclXml.setUpdateCount(newUpdateCount);

        aclUpdated.addNewNewAcl().setAcl(newAclXml);

        return aclUpdatedDocument;
    }

    private Calendar getCalendar(Date date) {
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        return calendar;
    }

    private void invalidateCachedAcl(long id) {
        acls.remove(new Long(id));
    }

    private AclImpl getCachedAcl(long id) throws RepositoryException {
        Long key = new Long(id);
        AclImpl acl = (AclImpl)acls.get(key);
        if (acl == null) {
            synchronized(acls) {
                // yes, I know double-checking isn't foolproof but that can't cause harm here
                if (!acls.containsKey(key)) {
                    acl = loadAcl(id, systemUser);
                    acl.makeReadOnly();
                    acls.put(key, acl);
                } else {
                    acl = (AclImpl)acls.get(key);
                }
            }
        }
        return acl;
    }

    public void copyStagingToLive(AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can copy the staging ACL to the live ACL.");

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RepositoryException("Could not get an ACL lock.", e);
        }
        try {
            AclImpl stagingAcl = loadAclInternal(STAGING_ACL_ID, user);
            AclImpl liveAcl = loadAclInternal(LIVE_ACL_ID, user);
            AclImpl.IntimateAccess stagingAclInt = stagingAcl.getIntimateAccess(this);
            stagingAclInt.setId(LIVE_ACL_ID);
            stagingAclInt.setUpdateCount(liveAcl.getUpdateCount());
            storeAclInternal(stagingAcl, true);
        } finally {
            lock.unlock();
        }
    }

    public void copyLiveToStaging(AuthenticatedUser user) throws RepositoryException {
        if (!user.isInAdministratorRole())
            throw new RepositoryException("Only Administrators can copy the live ACL to the staging ACL.");

        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RepositoryException("Could not get an ACL lock.", e);
        }
        try {
            AclImpl liveAcl = loadAclInternal(LIVE_ACL_ID, user);
            AclImpl stagingAcl = loadAclInternal(STAGING_ACL_ID, user);
            AclImpl.IntimateAccess liveAclInt = liveAcl.getIntimateAccess(this);
            liveAclInt.setId(STAGING_ACL_ID);
            liveAclInt.setUpdateCount(stagingAcl.getUpdateCount());
            storeAclInternal(liveAcl, true);
        } finally {
            lock.unlock();
        }
    }

    public Object compileObjectExpression(String expression, Repository repository) throws RepositoryException {
        try {
            PredicateExpr expr = context.getQueryFactory().parsePredicateExpression(expression);
            expr.prepare(new ExtQueryContext(repository));
            AclConditionViolation violation = expr.isAclAllowed();
            if (violation != null)
                throw new RepositoryException(violation.getMessage());
            return expr;
        } catch (QueryException e) {
            throw new RepositoryException("Error compiling object selection expression.", e);
        }
    }

    public boolean checkObjectExpression(Object compiledExpression, Document document, boolean conceptual,
            Repository repository) throws RepositoryException {
        try {
            EvaluationInfo evaluationInfo = new EvaluationInfo(new ExtQueryContext(repository));
            evaluationInfo.setVersionMode(VersionMode.LAST);
            return ((PredicateExpr)compiledExpression).evaluate(new ExprDocData(document, null, null, conceptual), evaluationInfo);
        } catch (QueryException e) {
            throw new RepositoryException("Error checking object selection expression.", e);
        }
    }

    private AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, Document document, boolean conceptual) throws RepositoryException {
        if (user.getId() != userId && !user.isInAdministratorRole())
            throw new RepositoryException("User " + user.getId() + " is not an Administrator and hence cannot access ACL info of user " + userId);

        AclImpl acl = getCachedAcl(id);
        AclEvaluator aclEvaluator = new AclEvaluator(acl, this, this, new RepositoryImpl(context.getCommonRepository(), systemUser));
        return aclEvaluator.getAclInfo(userId, roleIds, document, conceptual);

    }

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, Document document) throws RepositoryException {
        return getAclInfo(user, id, userId, roleIds, document, false);
    }

    public AclResultInfo getAclInfo(AuthenticatedUser user, long id, long userId, long[] roleIds, DocId docId, long branchId, long languageId) throws RepositoryException {
        if (user.getId() != userId && !user.isInAdministratorRole())
            throw new RepositoryException("User " + user.getId() + " is not an Administrator and hence cannot access ACL info of user " + userId);

        Document document = context.getCommonRepository().getDocument(docId, branchId, languageId, false, systemUser);
        return getAclInfo(user, id, userId, roleIds, document);
    }

    public AclResultInfo getAclInfoForConceptualDocument(AuthenticatedUser user, long id, long userId, long[] roleIds,
            long documentTypeId, long branchId, long languageId) throws RepositoryException {
        Document blankDoc = createConceptualDocument(user, documentTypeId, branchId, languageId);

        return getAclInfo(user, id, userId, roleIds, blankDoc, true);
    }

    public long[] filterDocumentTypes(AuthenticatedUser user, long[] documentTypeIds, long collectionId, long branchId,
            long languageId) throws RepositoryException {
        AclImpl acl = getCachedAcl(AclStrategy.LIVE_ACL_ID);
        ArrayLongList result = new ArrayLongList(documentTypeIds.length);
        AclEvaluator aclEvaluator = new AclEvaluator(acl, this, this, new RepositoryImpl(context.getCommonRepository(), systemUser));

        for (long documentTypeId : documentTypeIds) {
            // To be able to write a new document, you should have:
            //   - write access for the conceptual document, which can evaluate exactly at this point
            //   - write access for the the document content being saved, which we can only guess at this stage
            Document conceptualDoc = createConceptualDocument(user, documentTypeId, branchId, languageId);
            AclResultInfo aclInfo = aclEvaluator.getAclInfo(user.getId(), user.getActiveRoleIds(), conceptualDoc, true);
            if (aclInfo.isAllowed(AclPermission.WRITE)) {
                boolean success = aclEvaluator.hasPotentialWriteAccess(user.getId(), user.getActiveRoleIds(),
                        documentTypeId, collectionId, branchId, languageId);
                if (success) {
                    result.add(documentTypeId);
                }
            }
        }

        return result.toArray();
    }

    private Document createConceptualDocument(AuthenticatedUser user, long documentTypeId, long branchId, long languageId) {
        Document blankDoc = new DocumentImpl(null, this.context.getCommonRepository(), user, documentTypeId, branchId,
                languageId);
        return blankDoc;
    }

    public VariantKey[] filterDocuments(AuthenticatedUser user, VariantKey[] variantKeys, AclPermission permission, boolean nonLive) throws RepositoryException {
        AclImpl acl = getCachedAcl(AclStrategy.LIVE_ACL_ID);
        List<VariantKey> result = new ArrayList<VariantKey>(variantKeys.length);
        AclEvaluator aclEvaluator = new AclEvaluator(acl, this, this, new RepositoryImpl(context.getCommonRepository(), systemUser));

        CommonRepository repository = context.getCommonRepository();
        for (VariantKey variantKey : variantKeys) {
            try {
                Document document = repository.getDocument(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId(), false, systemUser);
                AclResultInfo aclResultInfo = aclEvaluator.getAclInfo(user.getId(), user.getActiveRoleIds(), document);
                if ((nonLive && aclResultInfo.isNonLiveAllowed(permission)) || (!nonLive && aclResultInfo.isAllowed(permission)))
                    result.add(variantKey);
            } catch (DocumentNotFoundException e) {
                // skip non-existing documents
            } catch (DocumentVariantNotFoundException e) {
                // skip non-existing document variants
            }
        }

        return result.toArray(new VariantKey[result.size()]);
    }
}
