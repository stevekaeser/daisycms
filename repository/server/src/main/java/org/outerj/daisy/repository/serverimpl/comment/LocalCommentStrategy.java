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
package org.outerj.daisy.repository.serverimpl.comment;

import org.outerj.daisy.repository.commonimpl.comment.CommentStrategy;
import org.outerj.daisy.repository.commonimpl.comment.CommentImpl;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.repository.serverimpl.AbstractLocalStrategy;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.CommentCreatedDocument;
import org.outerx.daisy.x10.CommentDeletedDocument;

import java.sql.*;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

public class LocalCommentStrategy extends AbstractLocalStrategy implements CommentStrategy {
    public LocalCommentStrategy(LocalRepositoryManager.Context context, AuthenticatedUser systemUser, JdbcHelper jdbcHelper) {
        super(context, systemUser, jdbcHelper);
    }

    public CommentImpl storeComment(DocId docId, long branchId, long languageId, CommentVisibility visibility, String text, AuthenticatedUser user) throws RepositoryException {

        // check ACL. Currently we assume that everyone with read access can add comments. Usually, this would
        // mean we don't even have to check this permission here since otherwise the user wouldn't have been
        // able to get a handle on the document in the first place, except if meanwhile the ACL was updated.
        AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                user.getId(), user.getActiveRoleIds(), docId, branchId, languageId);
        if (!aclInfo.isAllowed(AclPermission.READ))
            throw new AccessException("User " + user.getId() + " is not allowed to add comments to " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)));
        if (visibility == CommentVisibility.EDITORS && !aclInfo.isAllowed(AclPermission.WRITE))
            throw new RepositoryException("Comments with editors-only visibility can only be created by users with write rights to the document variant.");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            long commentId = context.getNextCommentId();
            Date createdOn = new Date();

            stmt = conn.prepareStatement("insert into comments (id, doc_id, ns_id, branch_id, lang_id, created_by, created_on, visibility, comment_text) values (?,?,?,?,?,?,?,?,?)");
            stmt.setLong(1, commentId);
            stmt.setLong(2, docId.getSeqId());
            stmt.setLong(3, docId.getNsId());
            stmt.setLong(4, branchId);
            stmt.setLong(5, languageId);
            stmt.setLong(6, user.getId());
            stmt.setTimestamp(7, new Timestamp(createdOn.getTime()));
            stmt.setString(8, visibility.getCode());
            stmt.setString(9, text);
            stmt.execute();
            stmt.close();

            CommentImpl newComment = new CommentImpl(docId, branchId, languageId, commentId, text, visibility, createdOn, user.getId());

            // make async event
            XmlObject eventDescription = createCommentCreatedEvent(newComment);
            eventHelper.createEvent(eventDescription, "CommentCreated", conn);

            conn.commit();

            return newComment;
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem storing comment.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public CommentCreatedDocument createCommentCreatedEvent(Comment comment) {
        CommentCreatedDocument commentCreatedDocument = CommentCreatedDocument.Factory.newInstance();
        CommentCreatedDocument.CommentCreated commentCreated = commentCreatedDocument.addNewCommentCreated();
        commentCreated.addNewNewComment().setComment(comment.getXml().getComment());
        return commentCreatedDocument;
    }

    public void deleteComment(DocId docId, long branchId, long languageId, long id, AuthenticatedUser user) throws RepositoryException {

        AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                user.getId(), user.getActiveRoleIds(), docId, branchId, languageId);
        boolean writeAccess = aclInfo.isAllowed(AclPermission.WRITE);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = context.getDataSource().getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select visibility, comment_text, created_by, created_on from comments where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? and id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setLong(5, id);
            ResultSet rs = stmt.executeQuery();

            CommentImpl comment;
            if (rs.next()) {
                comment = new CommentImpl(docId, branchId, languageId, id, rs.getString("comment_text"), CommentVisibility.getByCode(rs.getString("visibility")), rs.getDate("created_on"), rs.getLong("created_by"));
            } else {
                throw new RepositoryException("The document " + docId + ", branch " + getBranchLabel(branchId) + ", language " + getLanguageLabel(languageId) + " doesn't have a comment with ID " + id + ".");
            }
            stmt.close();

            boolean canDelete = true;
            if (!user.isInAdministratorRole()) {
                if (!writeAccess && comment.getVisibility() != CommentVisibility.PRIVATE)
                    canDelete = false;
                if (comment.getVisibility() == CommentVisibility.PRIVATE && comment.getCreatedBy() != user.getId())
                    canDelete = false;
            }

            if (!canDelete)
                throw new RepositoryException("You are not allowed to remove comment " + id + " of " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)));

            stmt = conn.prepareStatement("delete from comments where id = ? and doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ?");
            stmt.setLong(1, id);
            stmt.setLong(2, docId.getSeqId());
            stmt.setLong(3, docId.getNsId());
            stmt.setLong(4, branchId);
            stmt.setLong(5, languageId);
            stmt.execute();
            stmt.close();

            // make async event
            XmlObject eventDescription = createCommentDeletedEvent(comment, user.getId());
            eventHelper.createEvent(eventDescription, "CommentDeleted", conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            throw new RepositoryException("Problem deleting comment.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public CommentDeletedDocument createCommentDeletedEvent(Comment comment, long deleterId) {
        CommentDeletedDocument commentDeletedDocument = CommentDeletedDocument.Factory.newInstance();
        CommentDeletedDocument.CommentDeleted commentDeleted = commentDeletedDocument.addNewCommentDeleted();
        commentDeleted.addNewDeletedComment().setComment(comment.getXml().getComment());
        commentDeleted.setDeletedTime(getCalendar(new Date()));
        commentDeleted.setDeleterId(deleterId);
        return commentDeletedDocument;
    }

    public Comment[] loadComments(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        // check ACL, people need at least read access to a document to load its comments
        AclResultInfo aclInfo = context.getCommonRepository().getAccessManager().getAclInfoOnLive(systemUser,
                user.getId(), user.getActiveRoleIds(), docId, branchId, languageId);
        if (!aclInfo.isAllowed(AclPermission.READ))
            throw new AccessException("User " + user.getId() + " cannot read the comments of " + getFormattedVariant(new VariantKey(docId.toString(), branchId, languageId)));

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = context.getDataSource().getConnection();
            StringBuilder query = new StringBuilder(SELECT_COMMENTS);
            query.append(" where doc_id = ? and ns_id = ? and branch_id = ? and lang_id = ? ");
            query.append(" and (visibility = '").append(CommentVisibility.PUBLIC.getCode()).append("'");
            query.append(" or (visibility = '").append(CommentVisibility.PRIVATE.getCode()).append("' and created_by = ?)");
            if (aclInfo.isAllowed(AclPermission.WRITE))
                query.append(" or visibility = '").append(CommentVisibility.EDITORS.getCode()).append("'");
            query.append(") order by created_on");
            stmt = conn.prepareStatement(query.toString());
            stmt.setLong(1, docId.getSeqId());
            stmt.setLong(2, docId.getNsId());
            stmt.setLong(3, branchId);
            stmt.setLong(4, languageId);
            stmt.setLong(5, user.getId());
            rs = stmt.executeQuery();
            return getCommentsFromResultSet(rs);
        } catch (Throwable e) {
            throw new RepositoryException("Problem loading comments.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Comment[] loadComments(CommentVisibility visibility, AuthenticatedUser user) throws RepositoryException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs;
        try {
            conn = context.getDataSource().getConnection();
            StringBuilder query = new StringBuilder(SELECT_COMMENTS);
            query.append(" where created_by = ?");
            if (visibility != null)
                query.append(" and visibility = '").append(visibility.getCode()).append("'");
            query.append(" order by created_on desc");
            stmt = conn.prepareStatement(query.toString());
            stmt.setLong(1, user.getId());
            rs = stmt.executeQuery();
            return getCommentsFromResultSet(rs);
        } catch (Throwable e) {
            throw new RepositoryException("Problem loading comments.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Comment[] loadComments(AuthenticatedUser user) throws RepositoryException {
        return loadComments(null, user);
    }

    private static final String SELECT_COMMENTS = "select doc_id, ns_id, ns.name_ as ns_name, branch_id, lang_id, comments.id, comment_text, visibility, created_on, created_by from comments left join daisy_namespaces ns on (comments.ns_id = ns.id) ";

    private Comment[] getCommentsFromResultSet(ResultSet rs) throws SQLException {
        List<Comment> comments = new ArrayList<Comment>();
        while (rs.next()) {
            DocId docId = new DocId(rs.getLong(1), rs.getString(3), rs.getLong(2));
            CommentImpl comment = new CommentImpl(docId, rs.getLong(4), rs.getLong(5), rs.getLong(6), rs.getString(7), CommentVisibility.getByCode(rs.getString(8)), rs.getTimestamp(9), rs.getLong(10));
            comments.add(comment);
        }
        return comments.toArray(new Comment[comments.size()]);
    }
}
