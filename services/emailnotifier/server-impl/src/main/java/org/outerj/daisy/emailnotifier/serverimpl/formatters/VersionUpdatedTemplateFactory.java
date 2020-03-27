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
import org.outerj.daisy.repository.user.UserManager;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.outerx.daisy.x10.VersionUpdatedDocument;
import org.outerx.daisy.x10.VersionDocument.Version;

import java.util.*;
import java.text.DateFormat;

public class VersionUpdatedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        VersionUpdatedDocument versionUpdatedDocument = (VersionUpdatedDocument)eventDescription;
        VersionUpdatedDocument.VersionUpdated versionUpdated = versionUpdatedDocument.getVersionUpdated();

        Document document = repository.getDocument(versionUpdated.getDocumentId(), versionUpdated.getBranchId(), versionUpdated.getLanguageId(), false);
        Version newVersionXml = versionUpdated.getNewVersion().getVersion();
        Version oldVersionXml = versionUpdated.getOldVersion().getVersion();
        
        UserManager userManager = repository.getUserManager();
        VariantManager variantManager = repository.getVariantManager();

        LiveVersionChangedMailTemplate template = new LiveVersionChangedMailTemplate();
        template.url = urlProvider.getURL(document);
        template.docId = document.getId();
        template.branch = TemplateUtil.getBranchName(versionUpdated.getBranchId(), variantManager);
        template.language = TemplateUtil.getLanguageName(versionUpdated.getLanguageId(), variantManager);
        template.versionId = newVersionXml.getId();
        template.docName = newVersionXml.getDocumentName();
        if (!oldVersionXml.getState().equals(newVersionXml.getState())) {
            template.oldState = oldVersionXml.getState();
        }
        template.state = newVersionXml.getState();
        if (newVersionXml.getSyncedWithLanguageId() != -1) {
            template.syncedWith = new StringBuffer(TemplateUtil.getLanguageName(newVersionXml.getSyncedWithLanguageId(),variantManager))
                .append(":").append(String.valueOf(newVersionXml.getSyncedWithVersionId())).toString(); 
        }
        
        template.changeType = newVersionXml.getChangeType();
        template.changeComment = newVersionXml.getChangeComment();
        template.modified = newVersionXml.getLastModified().getTime();
        template.modifier = userManager.getUserDisplayName(newVersionXml.getLastModifier());

        return template;
    }

    class LiveVersionChangedMailTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();
        public String url;
        public String docId;
        public String branch;
        public String language;
        public long versionId;
        public String docName;
        public String state;
        public String oldState;
        public String syncedWith;
        public String changeType;
        public String changeComment;
        public Date modified;
        public String modifier;
        public boolean liveVersionChanged;
        public long newLiveVersionId = -1;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            return bundle.getString("versionstate.subject") + " " + docName;
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                StringBuilder buffer = new StringBuilder();
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                buffer.append(bundle.getString("versionstate.intro")).append('\n');

                if (url != null) {
                    buffer.append('\n').append(url).append('\n');
                }

                buffer.append('\n').append(bundle.getString("versionstate.document-id")).append(": ").append(docId);
                buffer.append('\n').append(bundle.getString("common.branch")).append(": ").append(branch);
                buffer.append('\n').append(bundle.getString("common.language")).append(": ").append(language);
                buffer.append('\n').append(bundle.getString("versionstate.document-name")).append(": ").append(docName);
                buffer.append('\n').append(bundle.getString("versionstate.version")).append(": ").append(versionId);
                buffer.append('\n').append(bundle.getString("versionstate.new-state")).append(": ").append(bundle.getString("state." + state));
                if (oldState == null) {
                    buffer.append(" (").append(bundle.getString("updated.unchanged")).append(')');
                } else {
                    buffer.append(" (").append(bundle.getString("updated.previously")).append(' ').append(bundle.getString("state." + oldState)).append(')');
                }
                buffer.append('\n').append(bundle.getString("versionstate.new-synced-with")).append(": ")
                    .append(syncedWith == null ? bundle.getString("versionstate.not-synced") : syncedWith);
                buffer.append('\n').append(bundle.getString("versionstate.new-change-type")).append(": ").append(bundle.getString("changetype." + changeType));
                buffer.append('\n').append(bundle.getString("versionstate.new-change-comment")).append(": ").append(StringUtils.trimToEmpty(changeComment));
                buffer.append('\n').append(bundle.getString("versionstate.modified-on")).append(": ").append(dateFormat.format(modified))
                    .append(' ').append(bundle.getString("versionstate.modified-by")).append(": ").append(modifier);
                if (liveVersionChanged && newLiveVersionId == -1) {
                    buffer.append("\n\n").append(bundle.getString("versionstate.unpublished"));
                } else if (liveVersionChanged){
                    buffer.append("\n\n").append(bundle.getString("versionstate.new-live-version")).append(" ").append(newLiveVersionId);
                }

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }
    }
}
