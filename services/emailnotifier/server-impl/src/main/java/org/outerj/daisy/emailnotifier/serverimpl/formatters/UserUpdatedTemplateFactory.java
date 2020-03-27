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

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.emailnotifier.serverimpl.DocumentURLProvider;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplate;
import org.outerj.daisy.emailnotifier.serverimpl.MailTemplateFactory;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10.UserDocument;
import org.outerx.daisy.x10.UserUpdatedDocument;
import org.outerx.daisy.x10.RoleDocument.Role;

public class UserUpdatedTemplateFactory implements MailTemplateFactory {
    public MailTemplate createMailTemplate(XmlObject eventDescription, Repository repository, DocumentURLProvider urlProvider) throws Exception {
        UserUpdatedDocument userUpdatedDocument = (UserUpdatedDocument)eventDescription;
        UserDocument.User oldUserXml = userUpdatedDocument.getUserUpdated().getOldUser().getUser();
        UserDocument.User newUserXml = userUpdatedDocument.getUserUpdated().getNewUser().getUser();

        UserUpdatedTemplate template = new UserUpdatedTemplate();
        template.userId = oldUserXml.getId();

        template.updatedOn = newUserXml.getLastModified().getTime();
        template.updater = repository.getUserManager().getUser(newUserXml.getLastModifier(), false);
        template.updateCount = newUserXml.getUpdateCount();
        
        template.loginBefore = oldUserXml.getLogin();
        template.loginAfter= newUserXml.getLogin();
        template.displayNameBefore = repository.getUserManager().getUser(oldUserXml.getId(), false).getDisplayName();
        template.displayNameAfter = repository.getUserManager().getUser(newUserXml.getId(), false).getDisplayName();
        template.emailBefore = oldUserXml.getEmail();
        template.emailAfter = newUserXml.getEmail();
        template.updateableByUserBefore = oldUserXml.getUpdateableByUser();
        template.updateableByUserAfter = newUserXml.getUpdateableByUser();
        template.confirmedBefore = oldUserXml.getConfirmed();
        template.confirmedAfter = newUserXml.getConfirmed();
        template.authenticationSchemeBefore = oldUserXml.getAuthenticationScheme();
        template.authenticationSchemeAfter = newUserXml.getAuthenticationScheme();

        if (oldUserXml.isSetRole()) {
            template.roleBefore = oldUserXml.getRole().getName();
        }
        if (newUserXml.isSetRole()) {
            template.roleAfter = newUserXml.getRole().getName();
        }
        
        List<Role> rolesBefore = oldUserXml.getRoles().getRoleList();
        template.rolesBefore = new String[rolesBefore.size()];
        for (int i=0; i < rolesBefore.size(); i++) {
            template.rolesBefore[i] = rolesBefore.get(i).getName();
        }
        List<Role> rolesAfter = newUserXml.getRoles().getRoleList();
        template.rolesAfter = new String[rolesAfter.size()];
        for (int i=0; i < rolesAfter.size(); i++) {
            template.rolesAfter[i] = rolesAfter.get(i).getName();
        }
        return template;
    }

    static class UserUpdatedTemplate implements MailTemplate {
        private Map cachedByLocale = new HashMap();

        private Date updatedOn;
        private User updater;
        private long updateCount;

        private long userId;

        private String loginBefore;
        private String loginAfter;
        private String emailBefore;
        private String emailAfter;
        private String displayNameBefore;
        private String displayNameAfter;
        private boolean updateableByUserBefore;
        private boolean updateableByUserAfter;
        private boolean confirmedBefore;
        private boolean confirmedAfter;
        private String authenticationSchemeBefore;
        private String authenticationSchemeAfter;
        private String roleBefore;
        private String roleAfter;
        private String[] rolesBefore;
        private String[] rolesAfter;
        
        private ResourceBundle getBundle(Locale locale) {
            return ResourceBundle.getBundle("org/outerj/daisy/emailnotifier/serverimpl/formatters/messages", locale);
        }

        public String getSubject(Locale locale) {
            ResourceBundle bundle = getBundle(locale);
            StringBuffer subject = new StringBuffer(bundle.getString("userupdated.subject")).append(": ");
            appendFormattedUser(subject, displayNameAfter, loginAfter);
            subject.append(" -"); // add a separator between the updated user and the actor
            return subject.toString();
        }

        public String getMessage(Locale locale) {
            String message = (String)cachedByLocale.get(locale);
            if (message == null) {
                ResourceBundle bundle = getBundle(locale);
                DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);

                StringBuffer buffer = new StringBuffer();
                buffer.append(bundle.getString("userupdated.intro"));
                buffer.append(" ").append(bundle.getString("userupdated.on")).append(" ").append(dateFormat.format(updatedOn));
                buffer.append(" ").append(bundle.getString("userupdated.by")).append(" ");
                appendFormattedUser(buffer, updater);
                buffer.append('\n');
                buffer.append('\n').append(bundle.getString("user.id")).append(": ").append(userId);
                buffer.append('\n').append(bundle.getString("userupdated.count")).append(": ").append(updateCount);
                appendProperty(buffer, bundle, "user.login", loginBefore, loginAfter);
                appendProperty(buffer, bundle, "user.email", emailBefore, emailAfter);
                appendProperty(buffer, bundle, "user.displayName", displayNameBefore, displayNameAfter);
                appendProperty(buffer, bundle, "user.updateableByUser", bundle.getString("common."+updateableByUserBefore), bundle.getString("common."+updateableByUserAfter));
                appendProperty(buffer, bundle, "user.confirmed", bundle.getString("common."+confirmedBefore), bundle.getString("common."+confirmedAfter));
                appendProperty(buffer, bundle, "user.authenticationScheme", authenticationSchemeBefore, authenticationSchemeAfter);
                appendProperty(buffer, bundle, "user.defaultrole", roleBefore, roleAfter, "user.roles.all");
                appendProperty(buffer, bundle, "user.roles.available", StringUtils.join(rolesBefore, ", "), StringUtils.join(rolesAfter, ", "));

                message = buffer.toString();
                cachedByLocale.put(locale, message);
            }
            return message;
        }
        
        private void appendProperty(StringBuffer buffer, ResourceBundle bundle, String labelKey, Object oldValue, Object newValue, String nullKey) {
            buffer.append('\n').append(bundle.getString(labelKey)).append(": ");
            if (newValue != null) {
                buffer.append(newValue);
            } else {
                buffer.append(bundle.getString(nullKey));
            }
            if (!ObjectUtils.equals(oldValue, newValue)) {
                buffer.append(" (");
                buffer.append(bundle.getString("userpropery.was")).append(": ");
                if (oldValue != null) {
                    buffer.append(oldValue);
                } else {
                    buffer.append(bundle.getString(nullKey));
                }
                buffer.append(")");
            }
        }
        private void appendProperty(StringBuffer buffer, ResourceBundle bundle, String labelKey, Object oldValue, Object newValue) {
            appendProperty(buffer, bundle, labelKey, oldValue, newValue, "userproperty.null"); 
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