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
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.DocumentDocument;
import org.outerx.daisy.x10.DocumentVariantDeletedDocument;

import java.util.*;
import java.text.DateFormat;

public class DocumentVariantDeletedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        DocumentVariantDeletedDocument documentVariantDeletedDocument = (DocumentVariantDeletedDocument)eventDescription;
        DocumentVariantDeletedDocument.DocumentVariantDeleted documentVariantDeletedXml = documentVariantDeletedDocument.getDocumentVariantDeleted();
        DocumentDocument.Document documentXml = documentVariantDeletedXml.getDeletedDocumentVariant().getDocument();

        VariantManager variantManager = repository.getVariantManager();
        UserManager userManager = repository.getUserManager();

        DocumentVariantDeletedMailTemplate template = new DocumentVariantDeletedMailTemplate();
        template.documentId = documentXml.getId();
        template.branch = TemplateUtil.getBranchName(documentXml.getBranchId(), variantManager);
        template.language = TemplateUtil.getLanguageName(documentXml.getLanguageId(), variantManager);
        template.documentName = documentXml.getName();
        template.deleter = TemplateUtil.getUserName(documentVariantDeletedXml.getDeleterId(), userManager);
        template.deletedOn = documentVariantDeletedXml.getDeletedTime().getTime();

        return template;
    }

    static class DocumentVariantDeletedMailTemplate implements MailTemplate
    {
        private Map cachedByLocale = new HashMap();
        String documentId;
        String branch;
        String language;
        String documentName;
        String deleter;
        Date deletedOn;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("deleted.subject") + " " + documentName;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuilder buffer = new StringBuilder();
                ResourceBundle bundle = getBundle(locale);
                buffer.append(bundle.getString("deleted.intro")).append('\n');
                buffer.append(bundle.getString("common.id")).append(": ").append(documentId).append("\n");
                buffer.append(bundle.getString("common.branch")).append(": ").append(branch).append("\n");
                buffer.append(bundle.getString("common.language")).append(": ").append(language).append("\n");
                buffer.append(bundle.getString("common.name")).append(": ").append(documentName).append("\n");
                buffer.append("\n");
                buffer.append(bundle.getString("deleted.deleter")).append(": ").append(deleter).append("\n");
                buffer.append(bundle.getString("deleted.deleted-on")).append(": ").append(dateFormat.format(deletedOn)).append("\n");

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }
    }
}
