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
import org.outerx.daisy.x10.CommentsDocument;
import org.outerx.daisy.x10.CommentDocument;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.InsertBreaksInCommentsHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.apache.xmlbeans.XmlCursor;

import java.text.DateFormat;
import java.util.List;

public class MyCommentsRequest extends AbstractRequest implements Request {
    public MyCommentsRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();
        CommentsDocument commentsDocument = repository.getCommentManager().getComments(CommentVisibility.PRIVATE).getXml();
        annotateMyComments(commentsDocument.getComments().getCommentList(), publisherContext);
        commentsDocument.save(new StripDocumentHandler(new InsertBreaksInCommentsHandler(contentHandler)), new DummyLexicalHandler());
    }

    public void annotateMyComments(List<CommentDocument.Comment> commentsXml, PublisherContext publisherContext) throws RepositoryException {
        Repository repository = publisherContext.getRepository();
        DateFormat dateFormat = publisherContext.getTimestampFormat();
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();


        for (CommentDocument.Comment commentXml : commentsXml) {
            Document document = repository.getDocument(commentXml.getDocumentId(), commentXml.getBranchId(), commentXml.getLanguageId(), false);

            String documentName;
            Version liveVersion = document.getLiveVersion();
            if (liveVersion != null)
                documentName = liveVersion.getDocumentName();
            else
                documentName = document.getName();
            String branch = variantManager.getBranch(commentXml.getBranchId(), false).getName();
            String language = variantManager.getLanguage(commentXml.getLanguageId(), false).getName();
            String createdByDisplayName = userManager.getUserDisplayName(commentXml.getCreatedBy());
            String createdOnFormatted = dateFormat.format(commentXml.getCreatedOn().getTime());

            XmlCursor cursor = commentXml.newCursor();
            cursor.toNextToken();
            cursor.insertAttributeWithValue("createdOnFormatted", createdOnFormatted);
            cursor.insertAttributeWithValue("createdByDisplayName", createdByDisplayName);
            cursor.insertAttributeWithValue("documentName", documentName);
            cursor.insertAttributeWithValue("branch", branch);
            cursor.insertAttributeWithValue("language", language);
            cursor.dispose();

        }
    }
}
