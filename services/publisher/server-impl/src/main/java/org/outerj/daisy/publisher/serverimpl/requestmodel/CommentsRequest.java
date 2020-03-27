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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.outerj.daisy.repository.comment.Comments;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.InsertBreaksInCommentsHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerx.daisy.x10.CommentsDocument;
import org.outerx.daisy.x10.CommentDocument;
import org.apache.xmlbeans.XmlCursor;

import java.text.DateFormat;
import java.util.List;

public class CommentsRequest extends AbstractRequest {
    public CommentsRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Comments comments = publisherContext.getRepository().getCommentManager().getComments(publisherContext.getVariantKey());
        CommentsDocument commentsDocument = comments.getXml();

        UserManager userManager = publisherContext.getRepository().getUserManager();
        DateFormat dateFormat = publisherContext.getTimestampFormat();
        annotateComments(commentsDocument.getComments().getCommentList(), userManager, dateFormat);

        commentsDocument.save(new StripDocumentHandler(new InsertBreaksInCommentsHandler(contentHandler)), new DummyLexicalHandler());
    }

    private void annotateComments(List<CommentDocument.Comment> commentsXml, UserManager userManager, DateFormat dateFormat) throws RepositoryException {
        for (CommentDocument.Comment commentXml : commentsXml) {
            XmlCursor cursor = commentXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("createdOnFormatted", dateFormat.format(commentXml.getCreatedOn().getTime()));
            cursor.insertAttributeWithValue("createdByDisplayName", userManager.getUserDisplayName(commentXml.getCreatedBy()));
            cursor.dispose();
        }
    }
}
