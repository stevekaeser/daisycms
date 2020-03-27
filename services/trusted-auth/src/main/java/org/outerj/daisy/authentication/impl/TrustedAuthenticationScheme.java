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
package org.outerj.daisy.authentication.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.AuthenticationScheme;
import org.outerj.daisy.authentication.spi.UserCreator;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

public class TrustedAuthenticationScheme implements AuthenticationScheme {
    private final String name;
    private final String description;
    private final List<String> trustedKeys;
    private final UserCreator userCreator;
    //private final Log log = LogFactory.getLog(getClass());

    public TrustedAuthenticationScheme(String name, String description, List<String> keys, UserCreator userCreator) {
        this.name = name;
        this.trustedKeys = new ArrayList<String>(keys);
        this.description = description;
        this.userCreator = userCreator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void clearCaches() {
        // do nothing
    }

    public boolean check(Credentials credentials) throws AuthenticationException {

        for (String key: trustedKeys) {
        	String joined = credentials.getLogin().concat(key);
        	if (DigestUtils.md5Hex(joined).equals(credentials.getPassword())) {
                return true;
            }
        }
        
        return false;
    }

    public User createUser(Credentials credentials, UserManager userManager) throws AuthenticationException {
        if (userCreator != null) {
            if (check(credentials)) {
                return userCreator.create(credentials.getLogin(), userManager);
            }
        }
        return null;
    }
}
