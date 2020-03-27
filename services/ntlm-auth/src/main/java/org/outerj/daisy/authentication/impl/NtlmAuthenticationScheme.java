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

import org.outerj.daisy.authentication.spi.AuthenticationScheme;
import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.UserCreator;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import jcifs.smb.SmbException;
import jcifs.smb.SmbAuthException;
import jcifs.smb.SmbSession;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.UniAddress;

import java.net.UnknownHostException;

public class NtlmAuthenticationScheme implements AuthenticationScheme {
    private final String name;
    private final String description;
    private final String domainControllerAddress;
    private final String domain;
    private final UserCreator userCreator;

    public NtlmAuthenticationScheme(String name, String description, String domainControllerAddress, String domain, UserCreator userCreator) {
        this.name = name;
        this.description = description;
        this.domainControllerAddress = domainControllerAddress;
        this.domain = domain;
        this.userCreator = userCreator;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean check(Credentials credentials) throws AuthenticationException {
        UniAddress mydomaincontroller;
        try {
            mydomaincontroller = UniAddress.getByName(domainControllerAddress);
        } catch (UnknownHostException e) {
            throw new AuthenticationException("Error authenticating using NTLM.", e);
        }
        NtlmPasswordAuthentication mycreds = new NtlmPasswordAuthentication(domain, credentials.getLogin(), credentials.getPassword());
        try {
            SmbSession.logon( mydomaincontroller, mycreds );
            // SUCCESS
            return true;
        } catch( SmbAuthException sae ) {
            // AUTHENTICATION FAILURE
            return false;
        } catch( SmbException se ) {
            // NETWORK PROBLEMS?
            throw new AuthenticationException("Error authenticating using NTLM.", se);
        }
    }

    public void clearCaches() {
        // do nothing
    }

    public User createUser(Credentials crendentials, UserManager userManager) throws AuthenticationException {
        if (userCreator != null) {
            if (check(crendentials)) {
                return userCreator.create(crendentials.getLogin(), userManager);
            }
        }
        return null;
    }
}
