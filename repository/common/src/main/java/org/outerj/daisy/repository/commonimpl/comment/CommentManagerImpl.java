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

import org.outerj.daisy.repository.comment.CommentManager;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.Comments;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocId;

public class CommentManagerImpl implements CommentManager {
    private final CommonRepository repository;
    private final CommonCommentManager commonCommentManager;
    private final AuthenticatedUser user;

    public CommentManagerImpl(CommonRepository repository, AuthenticatedUser user) {
        this.repository = repository;
        this.commonCommentManager = repository.getCommentManager();
        this.user = user;
    }

    public Comment addComment(String documentId, long branchId, long languageId, CommentVisibility visibility, String commentText) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, repository);
        return commonCommentManager.addComment(docId, branchId, languageId, visibility, commentText, user);
    }

    public Comment addComment(String documentId, CommentVisibility visibility, String commentText) throws RepositoryException {
        return addComment(documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, visibility, commentText);
    }

    public void deleteComment(String documentId, long branchId, long languageId, long commentId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, repository);
        commonCommentManager.deleteComment(docId, branchId, languageId, commentId, user);
    }

    public void deleteComment(String documentId, long commentId) throws RepositoryException {
        deleteComment(documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID, commentId);
    }

    public void deleteComment(Comment comment) throws RepositoryException {
        deleteComment(comment.getOwnerDocumentId(), comment.getOwnerBranchId(), comment.getOwnerLanguageId(), comment.getId());
    }

    public Comments getComments(String documentId, long branchId, long languageId) throws RepositoryException {
        DocId docId = DocId.parseDocIdThrowNotFound(documentId, repository);
        return commonCommentManager.getComments(docId, branchId, languageId, user);
    }

    public Comments getComments(VariantKey variantKey) throws RepositoryException {
        return getComments(variantKey.getDocumentId(), variantKey.getBranchId(), variantKey.getLanguageId());
    }

    public Comments getComments(String documentId) throws RepositoryException {
        return getComments(documentId, Branch.MAIN_BRANCH_ID, Language.DEFAULT_LANGUAGE_ID);
    }

    public Comments getComments(CommentVisibility visibility) throws RepositoryException {
        return commonCommentManager.getComments(visibility, user);
    }

    public Comments getComments() throws RepositoryException {
        return commonCommentManager.getComments(user);
    }
}
