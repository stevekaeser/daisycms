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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10.UserDeletedDocument;
import org.outerx.daisy.x10.UserDocument;

public class UserDeletedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        UserDeletedDocument userDeletedDocument = (UserDeletedDocument)eventDescription;
        UserDocument.User oldUserXml = userDeletedDocument.getUserDeleted().getDeletedUser().getUser();

        UserDeletedTemplate template = new UserDeletedTemplate();
        template.userId = oldUserXml.getId();
        template.userLogin= oldUserXml.getLogin();

        template.deletedOn = userDeletedDocument.getUserDeleted().getDeletedTime().getTime();
        template.deleter = repository.getUserManager().getUser(userDeletedDocument.getUserDeleted().getDeleterId(), false); 
        
        return template;
    }

    static class UserDeletedTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();

        private Date deletedOn;
        private User deleter;

        private long userId;
        private String userLogin;
        
        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            StringBuffer subject = new StringBuffer(bundle.getString("userupdated.subject")).append(": ");
            subject.append(userLogin);
            subject.append(" -"); // add a separator between the deleted user and the actor
            return subject.toString();
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuffer buffer = new StringBuffer();
                buffer.append(bundle.getString("userdeleted.intro"));
                buffer.append(' ').append(bundle.getString("userdeleted.on")).append(" ").append(dateFormat.format(deletedOn));
                buffer.append(' ').append(bundle.getString("userdeleted.by")).append(" ");
                appendFormattedUser(buffer, deleter);
                buffer.append('\n').append(bundle.getString("user.id")).append(": ").append(userId);
                buffer.append('\n').append(bundle.getString("user.login")).append(": ").append(userLogin);

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }
        
        public void appendFormattedUser(StringBuffer buffer, String displayName, String login) {
            if (displayName == null) {
                buffer.append(displayName).append(" (").append(login).append(")");
            } else {
                buffer.append(login);
            }
        }

        public void appendFormattedUser(StringBuffer buffer, User user) {
            appendFormattedUser(buffer, user.getDisplayName(), user.getLogin());
        }
    }
}