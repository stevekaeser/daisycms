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
package org.outerj.daisy.frontend;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.User;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.excalibur.xml.sax.XMLizable;

/**
 * Utility class to stream information about the current repository user as SAX events.
 *
 * At the time of this writing, this was both used in the class {@link PageContext},
 * and in the document.xml template (which is the start of the document type specific
 * styling pipelines).
 */
public class UserInfoStreamer implements XMLizable {
    private static final Attributes EMPTY_ATTRIBUTES = new AttributesImpl();
    private final Repository repository;

    public UserInfoStreamer(Repository repository) {
        this.repository = repository;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        try {
            streamUserInfo(repository, contentHandler);
        } catch (RepositoryException e) {
            throw new SAXException(e);
        }
    }

    public static void streamUserInfo(Repository repository, ContentHandler contentHandler) throws SAXException, RepositoryException {
        contentHandler.startElement("", "user", "user", new org.xml.sax.helpers.AttributesImpl());
        generateStringElement("name", repository.getUserDisplayName(), contentHandler);
        generateStringElement("login", repository.getUserLogin(), contentHandler);
        generateStringElement("id", String.valueOf(repository.getUserId()), contentHandler);
        contentHandler.startElement("", "activeRoles", "activeRoles", new org.xml.sax.helpers.AttributesImpl());
        long[] activeRoleIds = repository.getActiveRoleIds();
        UserManager userManager = repository.getUserManager();
        for (long activeRoleId : activeRoleIds) {
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(activeRoleId));
            attrs.addAttribute("", "name", "name", "CDATA", userManager.getRoleDisplayName(activeRoleId));
            contentHandler.startElement("", "role", "role", attrs);
            contentHandler.endElement("", "role", "role");
        }
        contentHandler.endElement("", "activeRoles", "activeRoles");

        User user = userManager.getUser(repository.getUserId(), false);
        generateStringElement("updateableByUser", String.valueOf(user.isUpdateableByUser()), contentHandler);

        org.xml.sax.helpers.AttributesImpl availableRolesAttrs = new org.xml.sax.helpers.AttributesImpl();
        if (user.getDefaultRole() != null)
            availableRolesAttrs.addAttribute("", "default", "default", "CDATA", user.getDefaultRole().getName());
        contentHandler.startElement("", "availableRoles", "availableRoles", availableRolesAttrs);
        long roles[] = repository.getAvailableRoles();
        for (long role : roles) {
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", String.valueOf(role));
            attrs.addAttribute("", "name", "name", "CDATA", userManager.getRoleDisplayName(role));
            contentHandler.startElement("", "role", "role", attrs);
            contentHandler.endElement("", "role", "role");
        }
        contentHandler.endElement("", "availableRoles", "availableRoles");

        contentHandler.endElement("", "user", "user");
    }

    private static void generateStringElement(String name, String value, ContentHandler contentHandler) throws SAXException {
        contentHandler.startElement("", name, name, EMPTY_ATTRIBUTES);
        contentHandler.characters(value.toCharArray(), 0, value.length());
        contentHandler.endElement("", name, name);
    }
}
