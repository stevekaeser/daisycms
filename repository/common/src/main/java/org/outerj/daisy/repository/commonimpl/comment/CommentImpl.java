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

import org.outerj.daisy.repository.comment.Comment;
import org.outerj.daisy.repository.comment.CommentVisibility;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerx.daisy.x10.CommentDocument;

import java.util.Date;
import java.util.GregorianCalendar;

public class CommentImpl implements Comment {
    private DocId ownerDocId;
    private long ownerBranchId;
    private long ownerLanguageId;
    private long id;
    private String text;
    private CommentVisibility visibility;
    private Date createdOn;
    private long createdBy;

    public CommentImpl(DocId ownerDocId, long ownerBranchId, long ownerLanguageId, long id, String text, CommentVisibility visibility, Date createdOn, long createdBy) {
        this.ownerDocId = ownerDocId;
        this.ownerBranchId = ownerBranchId;
        this.ownerLanguageId = ownerLanguageId;
        this.id = id;
        this.text = text;
        this.visibility = visibility;
        this.createdOn = createdOn;
        this.createdBy = createdBy;
    }

    public String getText() {
        return text;
    }

    public long getId() {
        return id;
    }

    public String getOwnerDocumentId() {
        return ownerDocId.toString();
    }

    public long getOwnerBranchId() {
        return ownerBranchId;
    }

    public long getOwnerLanguageId() {
        return ownerLanguageId;
    }

    public CommentVisibility getVisibility() {
        return visibility;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public Date getCreatedOn() {
        return (Date)createdOn.clone();
    }

    public CommentDocument getXml() {
        CommentDocument commentDocument = CommentDocument.Factory.newInstance();
        CommentDocument.Comment commentXml = commentDocument.addNewComment();
        commentXml.setContent(text);
        commentXml.setId(id);
        commentXml.setVisibility(CommentDocument.Comment.Visibility.Enum.forString(visibility.toString()));
        commentXml.setCreatedBy(createdBy);
        commentXml.setDocumentId(ownerDocId.toString());
        commentXml.setBranchId(ownerBranchId);
        commentXml.setLanguageId(ownerLanguageId);
        GregorianCalendar createdOnCalendar = new GregorianCalendar();
        createdOnCalendar.setTime(createdOn);
        commentXml.setCreatedOn(createdOnCalendar);
        return commentDocument;
    }
}
