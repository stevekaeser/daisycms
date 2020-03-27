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

import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.RepositoryException;

public interface CommentStrategy {
    public CommentImpl storeComment(DocId docId, long branchId, long languageId, CommentVisibility visibility, String text, AuthenticatedUser user) throws RepositoryException;

    public void deleteComment(DocId docId, long branchId, long languageId, long id, AuthenticatedUser user) throws RepositoryException;

    public Comment[] loadComments(DocId docId, long branchId, long languageId, AuthenticatedUser user) throws RepositoryException;

    public Comment[] loadComments(CommentVisibility visibility, AuthenticatedUser user) throws RepositoryException;

    public Comment[] loadComments(AuthenticatedUser user) throws RepositoryException;
}
