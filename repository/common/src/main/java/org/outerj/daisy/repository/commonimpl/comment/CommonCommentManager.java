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
package org.outerj.daisy.repository.commonimpl.comment;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.Comments;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;

public class CommonCommentManager {
    private CommentStrategy commentStrategy;

    public CommonCommentManager(CommentStrategy commentStrategy) {
        this.commentStrategy = commentStrategy;
    }

    public Comment addComment(DocId docId, long branchId, long languageId, CommentVisibility visibility, String commentText, AuthenticatedUser user) throws RepositoryException {
        return commentStrategy.storeComment(docId, branchId, languageId, visibility, commentText, user);
    }

    public void deleteComment(DocId docId, long branchId, long languageId, long commentId, AuthenticatedUser user) throws RepositoryException {
        commentStrategy.deleteComment(docId, branchId, languageId, commentId, user);
    }

    public Comments getComments(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException {
        return new CommentsImpl(commentStrategy.loadComments(docId, branchId, languageId, user));
    }

    public Comments getComments(CommentVisibility visibility, AuthenticatedUser user) throws RepositoryException {
        return new CommentsImpl(commentStrategy.loadComments(visibility, user));
    }

    public Comments getComments(AuthenticatedUser user) throws RepositoryException {
        return new CommentsImpl(commentStrategy.loadComments(user));
    }
}
