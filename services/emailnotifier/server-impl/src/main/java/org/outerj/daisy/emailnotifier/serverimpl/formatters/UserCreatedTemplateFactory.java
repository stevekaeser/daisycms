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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10.UserCreatedDocument;
import org.outerx.daisy.x10.UserDocument;
import org.outerx.daisy.x10.RoleDocument.Role;

public class UserCreatedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        UserCreatedDocument userCreatedDocument = (UserCreatedDocument)eventDescription;
        UserDocument.User userXml = userCreatedDocument.getUserCreated().getNewUser().getUser();

        UserCreatedTemplate template = new UserCreatedTemplate();
        template.userId = userXml.getId();
        template.login = userXml.getLogin();
        template.displayName = repository.getUserManager().getUser(userXml.getId(), false).getDisplayName();
        template.email = userXml.getEmail();
        template.updateableByUser = userXml.getUpdateableByUser();
        template.confirmed = userXml.getConfirmed();
        template.authenticationScheme = userXml.getAuthenticationScheme();
        template.createdOn = userXml.getLastModified().getTime();
        
        template.creator = repository.getUserManager().getUser(userXml.getLastModifier(), false);
        
        if (userXml.isSetRole()) {
            template.role = userXml.getRole().getName();
        }
        List<Role> roles = userXml.getRoles().getRoleList();
        template.roles = new String[roles.size()];
        for (int i=0; i < roles.size(); i++) {
            template.roles[i] = roles.get(i).getName();
        }
        return template;
    }

    static class UserCreatedTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();
        private long userId;
        private String login;
        private String email;
        private String displayName;
        private boolean updateableByUser;
        private boolean confirmed;
        private String authenticationScheme;
        private String role;
        private String[] roles;
        
        private Date createdOn;
        private User creator;

        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            StringBuffer subject = new StringBuffer(bundle.getString("usercreated.subject")).append(": ");
            if (displayName == null) {
                subject.append(login);
            } else {
                subject.append(displayName)
                .append(" (")
                .append(login)
                .append(")");
            }
            subject.append(" -"); // add a separator between the created user and the actor
            return subject.toString();
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuffer buffer = new StringBuffer();
                buffer.append(bundle.getString("usercreated.intro"));
                buffer.append(" ").append(bundle.getString("usercreated.on")).append(" ").append(dateFormat.format(createdOn));
                buffer.append(" ").append(bundle.getString("usercreated.by")).append(" ");
                appendFormattedUser(buffer, creator);
                buffer.append("\n");
                buffer.append('\n').append(bundle.getString("user.id")).append(": ").append(userId);
                buffer.append('\n').append(bundle.getString("user.login")).append(": ").append(login);
                if (email != null) {
                    buffer.append('\n').append(bundle.getString("user.email")).append(": ").append(email);
                }
                buffer.append('\n').append(bundle.getString("user.displayName")).append(": ").append(displayName);
                buffer.append('\n').append(bundle.getString("user.updateableByUser")).append(": ").append(bundle.getString("common."+updateableByUser));
                buffer.append('\n').append(bundle.getString("user.confirmed")).append(": ").append(bundle.getString("common."+confirmed));
                buffer.append('\n').append(bundle.getString("user.authenticationScheme")).append(": ").append(authenticationScheme);
                buffer.append('\n').append(bundle.getString("user.defaultrole")).append(": ").append(role==null?bundle.getString("user.roles.all"):role);
                buffer.append('\n').append(bundle.getString("user.roles.available")).append(": ").append(StringUtils.join(roles, ", "));

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