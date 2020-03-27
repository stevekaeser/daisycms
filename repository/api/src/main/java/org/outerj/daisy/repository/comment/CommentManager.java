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
package org.outerj.daisy.repository.comment;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

/**
 * Everything related to document comments.
 *
 * <p>The CommentManager can be retrieved via {@link org.outerj.daisy.repository.Repository#getCommentManager()}.
 *
 */
public interface CommentManager {
    Comment addComment(String documentId, long branchId, long languageId, CommentVisibility visibility, String commentText) throws RepositoryException;

    /**
     * Adds a comment to the branch "main", language "default" of this document.
     */
    Comment addComment(String documentId, CommentVisibility visibility, String commentText) throws RepositoryException;

    /**
     * Deletes a comment. These are the rules for deleting comments:
     * <ul>
     *   <li>Administrators can delete all comments, also private comments from other users.
     *   <li>Users with write access to the document can delete their own private comments,
     *       public comments and editors-only comments.
     *   <li>Other users can only delete their own private comments.
     * </ul>
     */
    void deleteComment(String documentId, long branchId, long languageId, long commentId) throws RepositoryException;

    /**
     * Deletes a comment from the branch "main", language "default" of the document.
     */
    void deleteComment(String documentId, long commentId) throws RepositoryException;

    void deleteComment(Comment comment) throws RepositoryException;

    /**
     * Returns the comments for a specific document variant. Only private comment of the current
     * user will be included (also for administrators). Editors-only comments are only
     * included when the user has write access to the document.
     *
     * <p>The comments are ordered by creation date.
     */
    Comments getComments(String documentId, long branchId, long langugeId) throws RepositoryException;

    /**
     * @see #getComments(String, long, long)
     */
    Comments getComments(VariantKey variantKey) throws RepositoryException;

    /**
     * Gets the comments from the branch "main", languge "default" of the document.
     */
    Comments getComments(String documentId) throws RepositoryException;

    /**
     * Returns all comments with the specified visibility, created by the current user.
     */
    Comments getComments(CommentVisibility visibility) throws RepositoryException;

    /**
     * Returns all comments created by the current user.
     */
    Comments getComments() throws RepositoryException;
}
