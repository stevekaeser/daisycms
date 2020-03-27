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

import org.outerj.daisy.authentication.UserAuthenticator;
import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.AuthenticationScheme;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUserImpl;
import org.outerj.daisy.repository.commonimpl.user.AuthenticationSchemeInfoImpl;
import org.outerj.daisy.repository.commonimpl.user.AuthenticationSchemeInfosImpl;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.AuthenticationFailedException;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.*;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.plugin.PluginHandle;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.collections.primitives.LongList;
import org.apache.commons.collections.primitives.ArrayLongList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.annotation.PreDestroy;

public class UserAuthenticatorImpl implements UserAuthenticator, UserAuthenticatorImplMBean {
    private UserManager userManager;
    private Map<String, AuthenticationScheme> schemes = new ConcurrentHashMap<String, AuthenticationScheme>(16, .75f, 2);
    private AuthenticationSchemeInfos authenticationSchemeInfos = new AuthenticationSchemeInfosImpl(new AuthenticationSchemeInfo[0]);
    private String schemeForUserCreation;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=UserAuthenticator");
    private PluginRegistry pluginRegistry;
    private AuthenticationSchemePluginUser pluginUser = new AuthenticationSchemePluginUser();
    private Log log = LogFactory.getLog(getClass());

    public UserAuthenticatorImpl(Configuration configuration, MBeanServer mbeanServer, PluginRegistry pluginRegistry) throws Exception {
        this.mbeanServer = mbeanServer;
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.initialize();
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.unsetPluginUser(AuthenticationScheme.class, pluginUser);
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            log.error("Error unregistering MBean", e);
        }
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        Configuration schemeForUserCreationConf = configuration.getChild("authenticationSchemeForUserCreation", false);
        if (schemeForUserCreationConf != null) {
            schemeForUserCreation = schemeForUserCreationConf.getValue();
        }
    }

    private void initialize() throws Exception {
        pluginRegistry.setPluginUser(AuthenticationScheme.class, pluginUser);
        mbeanServer.registerMBean(this, mbeanName);
    }

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    public AuthenticatedUser authenticate(Credentials credentials) throws RepositoryException {

        if(credentials.getLogin() == null || credentials.getLogin().trim().length() == 0) {
            throw new AuthenticationException("Refusing to authenticate user with empty login");
        }
        
        if(credentials.getPassword() == null || credentials.getPassword().trim().length() == 0) {
            throw new AuthenticationException("Refusing to authenticate user without password");
        }

        // Handle the special case of the user $system
        if (credentials.getLogin().equals("$system")) {
            throw new AuthenticationException("User $system is an internally used user that can not log in.");
        }

        if (userManager == null) {
            throw new AuthenticationException("Got a request for authentication before being supplied with a UserManager instance.");
        }

        // Get the user (will throw an exception if the user does not exist)
        User user = null;
        try {
            user = userManager.getUser(credentials.getLogin(), false);
        } catch (UserNotFoundException e) {
            if (schemeForUserCreation != null) {
                AuthenticationScheme scheme = schemes.get(schemeForUserCreation);
                if (scheme != null) {
                    user = scheme.createUser(credentials, userManager);
                    if (user == null && log.isDebugEnabled())
                        log.debug("Authentication scheme for user creation (" + schemeForUserCreation + ") did not create a user for login \"" + credentials.getLogin() + "\".");
                } else {
                    if (log.isDebugEnabled())
                        log.debug("Authentication scheme for user creation does not exist: \"" + schemeForUserCreation + "\".");
                }
            } else {
                log.debug("User does not exist: \"" + credentials.getLogin() + "\", and there is no authentication scheme for user creation configured.");
            }
            if (user == null)
                throw e;
        }

        // Check the user is marked as 'confirmed'
        if (!user.isConfirmed())
            throw new AuthenticationFailedException(credentials.getLogin());

        // Check the credentials
        AuthenticationScheme authenticationScheme = schemes.get(user.getAuthenticationScheme());
        if (authenticationScheme == null)
            throw new AuthenticationException("The authentication scheme of the user does not exist.");

        if (!authenticationScheme.check(credentials))
            throw new AuthenticationFailedException(credentials.getLogin());

        // Everything ok: user is allowed
        long[] availableRoleIds= user.getAllRoleIds();
        long[] activeRoleIds = determineActiveRoles(user.getDefaultRole(), availableRoleIds);
        return new AuthenticatedUserImpl(user.getId(), null, activeRoleIds, availableRoleIds, user.getLogin());
    }

    /**
     * The active roles are either: the default role if any, otherwise all roles except
     * the administrator role, except if Administrator would be the only role.
     */
    private long[] determineActiveRoles(Role defaultRole, long[] availableRoleIds) {
        if (defaultRole != null)
            return new long[] {defaultRole.getId()};
        if (availableRoleIds.length == 1)
            return availableRoleIds;

        LongList roleIds = new ArrayLongList(availableRoleIds.length);
        for (long availableRoleId : availableRoleIds) {
            if (availableRoleId != Role.ADMINISTRATOR)
                roleIds.add(availableRoleId);
        }
        return roleIds.toArray();
    }

    public AuthenticationSchemeInfos getAuthenticationSchemes() {
        return authenticationSchemeInfos;
    }

    private class AuthenticationSchemePluginUser implements PluginUser<AuthenticationScheme> {

        public void pluginAdded(PluginHandle<AuthenticationScheme> pluginHandle) {
            if (pluginHandle.getName().length() > 50)
                throw new RuntimeException("The name of an authentication scheme may not exceed 50 characters.");

            schemes.put(pluginHandle.getName(), pluginHandle.getPlugin());
            rebuildAuthenticationSchemeInfos();
        }

        public void pluginRemoved(PluginHandle<AuthenticationScheme> pluginHandle) {
            schemes.remove(pluginHandle.getName());
            rebuildAuthenticationSchemeInfos();
        }
    }

    private void rebuildAuthenticationSchemeInfos() {
        AuthenticationSchemeInfo[] infos = new AuthenticationSchemeInfo[schemes.size()];
        int i = 0;
        for (Map.Entry<String, AuthenticationScheme> entry : schemes.entrySet()) {
            infos[i] = new AuthenticationSchemeInfoImpl(entry.getKey(), entry.getValue().getDescription());
            i++;
        }
        this.authenticationSchemeInfos = new AuthenticationSchemeInfosImpl(infos);
    }

    public synchronized void clearPasswordCaches() {
        for (AuthenticationScheme scheme : schemes.values()) {
            scheme.clearCaches();
        }
    }
}
