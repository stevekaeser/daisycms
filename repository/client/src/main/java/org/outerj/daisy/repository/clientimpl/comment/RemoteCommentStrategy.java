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
package org.outerj.daisy.repository.clientimpl.comment;

import org.outerj.daisy.repository.commonimpl.comment.CommentImpl;
import org.outerj.daisy.repository.commonimpl.comment.CommentStrategy;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyHttpClient;
import org.outerj.daisy.repository.clientimpl.infrastructure.AbstractRemoteStrategy;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.NameValuePair;
import org.outerx.daisy.x10.CommentDocument;
import org.outerx.daisy.x10.CommentsDocument;

import java.util.List;

public class RemoteCommentStrategy extends AbstractRemoteStrategy implements CommentStrategy {

    public RemoteCommentStrategy(RemoteRepositoryManager.Context context) {
        super(context);
    }

    public CommentImpl storeComment(DocId docId, long branchId, long languageId, CommentVisibility visibility, String text, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        PostMethod method = new PostMethod("/repository/document/" + docId.toString() + "/comment");

        CommentDocument commentDocument = CommentDocument.Factory.newInstance();
        CommentDocument.Comment commentXml = commentDocument.addNewComment();
        commentXml.setBranchId(branchId);
        commentXml.setLanguageId(languageId);
        commentXml.setContent(text);
        commentXml.setVisibility(CommentDocument.Comment.Visibility.Enum.forString(visibility.toString()));

        method.setRequestEntity(new InputStreamRequestEntity(commentDocument.newInputStream()));

        CommentDocument responseCommentDocument = (CommentDocument)httpClient.executeMethod(method, CommentDocument.class, true);
        CommentDocument.Comment responseCommentXml = responseCommentDocument.getComment();
        return instantiateCommentFromXml(responseCommentXml);
    }

    public Comment[] loadComments(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        GetMethod method = new GetMethod("/repository/document/" + docId.toString() + "/comment");
        method.setQueryString(getBranchLangParams(branchId, languageId));

        CommentsDocument commentsDocument = (CommentsDocument)httpClient.executeMethod(method, CommentsDocument.class, true);
        return instantiateCommentsFromXml(commentsDocument);
    }

    public Comment[] loadComments(CommentVisibility visibility, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        GetMethod method = new GetMethod("/repository/comments");

        if (visibility != null) {
            NameValuePair[] queryString = new NameValuePair[1];
            queryString[0] = new NameValuePair("visibility", visibility.toString());
            method.setQueryString(queryString);
        }

        CommentsDocument commentsDocument = (CommentsDocument)httpClient.executeMethod(method, CommentsDocument.class, true);
        return instantiateCommentsFromXml(commentsDocument);
    }

    public Comment[] loadComments(AuthenticatedUser user) throws RepositoryException {
        return loadComments(null, user);
    }

    private Comment[] instantiateCommentsFromXml(CommentsDocument commentsDocument) {
        List<CommentDocument.Comment> commentsXml = commentsDocument.getComments().getCommentList();
        Comment[] comments = new Comment[commentsXml.size()];
        for (int i = 0; i < comments.length; i++) {
            comments[i] = instantiateCommentFromXml(commentsXml.get(i));
        }
        return comments;
    }

    private CommentImpl instantiateCommentFromXml(CommentDocument.Comment commentXml) {
        DocId docId = DocId.parseDocId(commentXml.getDocumentId(), context.getCommonRepository());
        return new CommentImpl(docId, commentXml.getBranchId(), commentXml.getLanguageId(),
                commentXml.getId(), commentXml.getContent(),
                CommentVisibility.fromString(commentXml.getVisibility().toString()),
                commentXml.getCreatedOn().getTime(), commentXml.getCreatedBy());
    }

    public void deleteComment(DocId docId, long branchId, long languageId, long id, AuthenticatedUser user) throws RepositoryException {
        DaisyHttpClient httpClient = getClient(user);
        DeleteMethod method = new DeleteMethod("/repository/document/" + docId.toString() + "/comment/" + id);
        method.setQueryString(getBranchLangParams(branchId, languageId));
        httpClient.executeMethod(method, null, true);
    }
}
