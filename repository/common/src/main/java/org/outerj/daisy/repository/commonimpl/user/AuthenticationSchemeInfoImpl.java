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

import org.outerj.daisy.repository.user.AuthenticationSchemeInfo;
import org.outerx.daisy.x10.AuthenticationSchemeDocument;

public class AuthenticationSchemeInfoImpl implements AuthenticationSchemeInfo {
    private final String name;
    private final String description;
    
    public AuthenticationSchemeInfoImpl(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AuthenticationSchemeDocument getXml() {
        AuthenticationSchemeDocument schemeDocument = AuthenticationSchemeDocument.Factory.newInstance();
        AuthenticationSchemeDocument.AuthenticationScheme schemeXml = schemeDocument.addNewAuthenticationScheme();
        schemeXml.setName(name);
        schemeXml.setDescription(description);
        return schemeDocument;
    }
}
