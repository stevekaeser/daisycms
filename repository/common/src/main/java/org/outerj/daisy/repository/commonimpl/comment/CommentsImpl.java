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

import org.outerj.daisy.repository.comment.Comments;
import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.Comment;
import org.outerx.daisy.x10.CommentsDocument;
import org.outerx.daisy.x10.CommentDocument;

public class CommentsImpl implements Comments {
    private final Comment[] comments;

    public CommentsImpl(Comment[] comments) {
        this.comments = comments;
    }

    public Comment[] getArray() {
        return comments;
    }

    public CommentsDocument getXml() {
        CommentDocument.Comment[] commentsXml = new CommentDocument.Comment[comments.length];
        for (int i = 0; i < comments.length; i++) {
            commentsXml[i] = comments[i].getXml().getComment();
        }

        CommentsDocument commentsDocument = CommentsDocument.Factory.newInstance();
        commentsDocument.addNewComments().setCommentArray(commentsXml);

        return commentsDocument;
    }
}
