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
package org.outerj.daisy.repository.commonimpl.user;

import org.outerj.daisy.repository.user.PublicUserInfo;
import org.outerx.daisy.x10.PublicUserInfoDocument;

public class PublicUserInfoImpl implements PublicUserInfo {
    private final long id;
    private final String login;
    private final String displayName;

    public PublicUserInfoImpl(long id, String login, String displayName) {
        this.id = id;
        this.login = login;
        this.displayName = displayName;
    }

    public long getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PublicUserInfoDocument getXml() {
        PublicUserInfoDocument doc = PublicUserInfoDocument.Factory.newInstance();
        PublicUserInfoDocument.PublicUserInfo publicUserInfoXml = doc.addNewPublicUserInfo();

        publicUserInfoXml.setId(id);
        publicUserInfoXml.setLogin(login);
        publicUserInfoXml.setDisplayName(displayName);

        return doc;
    }
}
