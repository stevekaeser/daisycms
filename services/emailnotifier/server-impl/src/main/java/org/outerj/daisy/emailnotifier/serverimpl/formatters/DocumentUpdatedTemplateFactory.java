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

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerx.daisy.x10.DocumentUpdatedDocument;
import org.outerx.daisy.x10.DocumentDocument;
import org.apache.xmlbeans.XmlObject;

import java.util.*;
import java.text.DateFormat;

public class DocumentUpdatedTemplateFactory implements MailTemplateFactory {

    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        DocumentUpdatedDocument documentUpdatedDocument = (DocumentUpdatedDocument)eventDescription;
        DocumentDocument.Document oldDocumentXml = documentUpdatedDocument.getDocumentUpdated().getOldDocument().getDocument();
        DocumentDocument.Document newDocumentXml = documentUpdatedDocument.getDocumentUpdated().getNewDocument().getDocument();
        Document document = repository.getDocument(newDocumentXml.getId(), false);

        DocumentUpdatedMailTemplate template = new DocumentUpdatedMailTemplate();
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();

        template.url = urlProvider.getURL(document);
        template.docId = newDocumentXml.getId();

        template.modified = newDocumentXml.getLastModified().getTime();
        template.modifier = TemplateUtil.getUserName(newDocumentXml.getLastModifier(), userManager);
        template._private = newDocumentXml.getPrivate();
        if (newDocumentXml.getPrivate() != oldDocumentXml.getPrivate()) {
            template.oldPrivate = oldDocumentXml.getPrivate() ? Boolean.TRUE : Boolean.FALSE;
        }
        template.retired = newDocumentXml.getRetired();
        if (newDocumentXml.getRetired() != oldDocumentXml.getRetired()) {
            template.oldRetired = oldDocumentXml.getRetired() ? Boolean.TRUE : Boolean.FALSE;
        }
        template.owner = TemplateUtil.getUserName(newDocumentXml.getOwner(), userManager);
        if (newDocumentXml.getOwner() != oldDocumentXml.getOwner()) {
            template.oldOwner = TemplateUtil.getUserName(oldDocumentXml.getOwner(), userManager);
        }
        template.referenceLanguage = TemplateUtil.getLanguageName(newDocumentXml.getReferenceLanguageId(), variantManager);
        if (newDocumentXml.getReferenceLanguageId() != oldDocumentXml.getReferenceLanguageId()) {
            template.oldReferenceLanguage = TemplateUtil.getLanguageName(oldDocumentXml.getReferenceLanguageId(), variantManager);
        }

        return template;
    }

    static class DocumentUpdatedMailTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();
        public String docId;
        public Date modified;
        public String modifier;
        public boolean _private;
        public Boolean oldPrivate;
        public boolean retired;
        public Boolean oldRetired;
        public String owner;
        public String oldOwner;
        public String referenceLanguage;
        public String oldReferenceLanguage;
        public String url;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("updated.subject") + " " + docId;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                StringBuilder buffer = new StringBuilder();
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);


                buffer.append(bundle.getString("updated.intro")).append('\n');

                if (url != null) {
                    buffer.append('\n').append(url).append('\n');
                }

                buffer.append('\n').append(bundle.getString("common.id")).append(": ").append(docId);
                buffer.append('\n').append(bundle.getString("updated.updated-on")).append(": ").append(dateFormat.format(modified))
                    .append(bundle.getString("updated.updated-by")).append(": ").append(modifier);
                buffer.append('\n').append(bundle.getString("updated.owner")).append(": ").append(owner);
                if (oldOwner == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(": ").append(oldOwner).append(")");
                }
                buffer.append('\n').append(bundle.getString("common.private")).append(": ").append(bundle.getString("common." + _private));
                if (oldPrivate == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(": ").append(bundle.getString("common." + oldPrivate)).append(")");
                }
                buffer.append('\n').append(bundle.getString("common.retired")).append(": ").append(bundle.getString("common." + retired));
                if (oldRetired == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(": ").append(bundle.getString("common." + oldRetired)).append(")");
                }
                buffer.append('\n').append(bundle.getString("common.reference-language")).append(": ").append(referenceLanguage);
                if (oldReferenceLanguage == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(": ").append(oldReferenceLanguage).append(")");
                }

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }

    }

}
