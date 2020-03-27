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

import org.outerj.daisy.repository.user.AuthenticationSchemeInfos;
import org.outerj.daisy.repository.user.AuthenticationSchemeInfo;
import org.outerx.daisy.x10.AuthenticationSchemesDocument;
import org.outerx.daisy.x10.AuthenticationSchemeDocument;

public class AuthenticationSchemeInfosImpl implements AuthenticationSchemeInfos {
    private final AuthenticationSchemeInfo[] schemeInfos;

    public AuthenticationSchemeInfosImpl(AuthenticationSchemeInfo[] infos) {
        this.schemeInfos = infos;
    }

    public AuthenticationSchemeInfo[] getArray() {
        return schemeInfos;
    }

    public AuthenticationSchemesDocument getXml() {
        AuthenticationSchemesDocument schemesDocument = AuthenticationSchemesDocument.Factory.newInstance();

        AuthenticationSchemeDocument.AuthenticationScheme[] schemesXml = new AuthenticationSchemeDocument.AuthenticationScheme[schemeInfos.length];
        for (int i = 0; i < schemeInfos.length; i++) {
            schemesXml[i] = schemeInfos[i].getXml().getAuthenticationScheme();
        }

        schemesDocument.addNewAuthenticationSchemes().setAuthenticationSchemeArray(schemesXml);
        return schemesDocument;
    }
}
