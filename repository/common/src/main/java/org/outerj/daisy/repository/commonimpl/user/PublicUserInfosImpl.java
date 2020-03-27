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

import org.outerj.daisy.repository.user.PublicUserInfos;
import org.outerj.daisy.repository.user.PublicUserInfo;
import org.outerx.daisy.x10.PublicUserInfosDocument;
import org.outerx.daisy.x10.PublicUserInfoDocument;

public class PublicUserInfosImpl implements PublicUserInfos {
    private PublicUserInfo[] publicUserInfos;

    public PublicUserInfosImpl(PublicUserInfo[] publicUserInfo) {
        this.publicUserInfos = publicUserInfo;
    }

    public PublicUserInfo[] getArray() {
        return publicUserInfos;
    }

    public PublicUserInfosDocument getXml() {
        PublicUserInfoDocument.PublicUserInfo[] publicUserInfosXml = new PublicUserInfoDocument.PublicUserInfo[publicUserInfos.length];        
        for (int i = 0; i < publicUserInfos.length; i++) {
            publicUserInfosXml[i] = publicUserInfos[i].getXml().getPublicUserInfo();
        }

        PublicUserInfosDocument doc = PublicUserInfosDocument.Factory.newInstance();
        doc.addNewPublicUserInfos().setPublicUserInfoArray(publicUserInfosXml);

        return doc;
    }
}
