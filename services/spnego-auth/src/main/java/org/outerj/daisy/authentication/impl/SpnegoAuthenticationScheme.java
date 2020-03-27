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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.Oid;
import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.AuthenticationScheme;
import org.outerj.daisy.authentication.spi.UserCreator;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

public class SpnegoAuthenticationScheme implements AuthenticationScheme {
    private final String name;
    private final String description;
    private final UserCreator userCreator;
    private final Log log = LogFactory.getLog(getClass());

    public SpnegoAuthenticationScheme(String name, String description, UserCreator userCreator) {
        this.name = name;
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
        System.out.println("[SPNEGO] Checking credentials for " + credentials.getLogin());
        String base64token = credentials.getPassword();
        boolean credentialsOk = false;
        
        try {
            //TODO: add commons-codec, don't use sun.....Base64
            byte[]token = Base64.decodeBase64(base64token.getBytes());
            GSSManager manager = GSSManager.getInstance();
            Oid spnegoOid = new Oid("1.3.6.1.5.5.2");
            GSSCredential myCred = manager.createCredential(null, GSSCredential.DEFAULT_LIFETIME, spnegoOid, GSSCredential.ACCEPT_ONLY);
            GSSContext context = manager.createContext(myCred);
            
            byte[] tokenForPeer = context.acceptSecContext(token, 0, token.length);

            if (!context.isEstablished()) {
                System.out.println("Context is not extablished");
                return false;
            }

            if (tokenForPeer != null) {
              System.out.println("There is a token for peer, ignoring this");
            }
            
            System.out.println("Context Established! ");  
            System.out.println("Client principal is " + context.getSrcName());  
            System.out.println("Server principal is " + context.getTargName());

            System.out.println("Canonical client principal: " + context.getSrcName().canonicalize(spnegoOid));
            return true;
        } catch (Exception e) {
            log.error("Spnego auth failed", e);
        }

        return credentialsOk;
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
