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

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.AuthenticationScheme;
import org.outerj.daisy.authentication.spi.UserCreator;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

public class LdapAuthenticationScheme implements AuthenticationScheme {
    private final String name;
    private final String description;
    private final Map<String,String> templateEnvironment;
    private final String searchBase;
    private final String filter;
    private final UserCreator userCreator;
    private final Log log = LogFactory.getLog(getClass());

    public LdapAuthenticationScheme(String name, String description, Map<String,String> templateEnvironment, String searchBase, String filter, UserCreator userCreator) {
        this.name = name;
        this.description = description;
        this.templateEnvironment = templateEnvironment;
        this.searchBase = searchBase;
        this.filter = filter;
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
        Hashtable<String,String> env = new Hashtable<String, String>(this.templateEnvironment);
        boolean credentialsOk = false;
        // Reject empty or whitespace passwords
        String password = credentials.getPassword();
        if (password == null || password.trim().length() == 0) {
            return false;
        }

        String searchFilter = this.filter.replaceAll("\\$daisyLogin", credentials.getLogin());

        try {
            // Create a connection with the LDAP server
            InitialDirContext ctx = new InitialDirContext(env);
            try { 
                // Look for the user 
                SearchControls searchControls = new SearchControls();
                searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
                NamingEnumeration<SearchResult> results = ctx.search(this.searchBase, searchFilter, searchControls);
                
                if (results.hasMore()) {
                    // We have found a DN of a user now we'll try to bind with the ldap server to see if the passwords match
                    SearchResult result = results.next();
                    String userDN;
                    if (result.isRelative()) {
                        userDN = result.getName() + "," + this.searchBase ;                    
                    } else {
                        userDN = result.getName();
                    }
                    Hashtable<String,String> userEnv = new Hashtable<String, String>(this.templateEnvironment);
                    ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, userDN);
                    ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
                    // try again
                    ctx.lookup(this.searchBase);
                    credentialsOk = true;                    
                } else {
                    // user not found
                    log.debug("Failed to authenticate user with the following environment: " + env);
                    credentialsOk = false;
                } 
            } catch (NamingException e) {
                if (log.isDebugEnabled()) {
                    env.put(Context.SECURITY_CREDENTIALS, "***REMOVED ON PURPOSE***");
                    log.debug("Failed to authenticate user with following environment: " + env, e);
                }
                credentialsOk = false;
            } finally {
                ctx.close();
            }
            
        } catch (NamingException e) {
            if (log.isDebugEnabled()) {
                env.put(Context.SECURITY_CREDENTIALS, "***REMOVED ON PURPOSE***");
                log.debug("Could not connect to the LDAP server" + env, e);
            }
            credentialsOk = false;
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
