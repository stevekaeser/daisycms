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
package org.outerj.daisy.emailnotifier.serverimpl.formatters;

import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.variant.VariantManager;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.CommentDocument;
import org.outerx.daisy.x10.CommentDeletedDocument;

import java.util.*;
import java.text.DateFormat;

public class CommentDeletedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        CommentDeletedDocument commentDeletedDocument = (CommentDeletedDocument)eventDescription;
        CommentDocument.Comment commentXml = commentDeletedDocument.getCommentDeleted().getDeletedComment().getComment();

        Document document = repository.getDocument(commentXml.getDocumentId(), commentXml.getBranchId(), commentXml.getLanguageId(), false);
        VariantManager variantManager = repository.getVariantManager();

        CommentDeletedMailTemplate template = new CommentDeletedMailTemplate();
        template.docName = document.getName();
        template.docId = document.getId();
        template.branch = TemplateUtil.getBranchName(commentXml.getBranchId(), variantManager);
        template.language = TemplateUtil.getLanguageName(commentXml.getLanguageId(), variantManager);
        template.url = urlProvider.getURL(document);
        template.deleter = repository.getUserManager().getUserDisplayName(commentDeletedDocument.getCommentDeleted().getDeleterId());
        template.deletedOn = commentDeletedDocument.getCommentDeleted().getDeletedTime().getTime();
        template.commentText = commentXml.getContent();

        return template;
    }

    static class CommentDeletedMailTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();
        private String docName;
        private String docId;
        private String branch;
        private String language;
        private String commentText;
        private String deleter;
        private Date deletedOn;
        private String url;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("commentdeleted.subject") + " " + docName;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuilder buffer = new StringBuilder();
                buffer.append(bundle.getString("commentdeleted.intro"));
                buffer.append("\n");
                if (url != null) {
                    buffer.append('\n').append(url).append('\n');
                }
                buffer.append('\n').append(bundle.getString("common.id")).append(": ").append(docId);
                buffer.append('\n').append(bundle.getString("common.name")).append(": ").append(docName);
                buffer.append('\n').append(bundle.getString("common.branch")).append(": ").append(branch);
                buffer.append('\n').append(bundle.getString("common.language")).append(": ").append(language);
                buffer.append("\n\n").append(bundle.getString("commentdeleted.deleter")).append(": ").append(deleter);
                buffer.append("\n").append(bundle.getString("commentdeleted.deletedon")).append(": ").append(dateFormat.format(deletedOn));
                buffer.append("\n\n");
                buffer.append(commentText);

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }
    }
}