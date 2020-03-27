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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.authentication.spi.UpdatingAuthenticationScheme;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

public class UserMaintainer {

    private String repositoryKey;
    
    private CredentialsProvider credentialsProvider;
    
    private Credentials repositoryCredentials;
    
    private Repository repository;

    private UserManager userManager;

    private long interval;

    private ScheduledExecutorService executor;

    private Map<String, UpdatingAuthenticationScheme> schemes = new ConcurrentHashMap<String, UpdatingAuthenticationScheme>(16, .75f, 2);

    private UpdatingAuthenticationSchemePluginUser pluginUser = new UpdatingAuthenticationSchemePluginUser();

    private PluginRegistry pluginRegistry;

    private Log log = LogFactory.getLog(getClass());

    public UserMaintainer(Configuration configuration, RepositoryManager repositoryManager, PluginRegistry pluginRegistry, CredentialsProvider credentialsProvider) throws ConfigurationException,
            RepositoryException {

        interval = configuration.getChild("interval").getValueAsLong();

        repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
        repository = repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));
        repository.switchRole(Role.ADMINISTRATOR);

        userManager = repository.getUserManager();

        this.pluginRegistry = pluginRegistry;
        
        pluginRegistry.setPluginUser(UpdatingAuthenticationScheme.class, pluginUser);
    }

    @PostConstruct
    public void start() {
        executor = Executors.newScheduledThreadPool(1);

        executor.scheduleWithFixedDelay(new Runnable() {
            public void run() {
                performUserUpdate();
            }
        }, interval * 60, interval * 60, TimeUnit.SECONDS);

    }

    @PreDestroy
    public void stop() {
        pluginRegistry.unsetPluginUser(UpdatingAuthenticationScheme.class, pluginUser);
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    private void performUserUpdate() {
        log.debug("Performing update");
        for (String schemeName : schemes.keySet()) {
            UpdatingAuthenticationScheme scheme = schemes.get(schemeName);
            try {
                // TODO add a method to the userManager for getting users by
                // scheme
                for (User user : userManager.getUsers().getArray()) {                            
                    try {
                        if (user.getAuthenticationScheme().equals(schemeName)) {
                            log.debug("Updating user " + user.getLogin());
                            // check to see if the updater saved the user
                            User oldUser = userManager.getUser(user.getId(), false);
                            
                            scheme.update(user, userManager);
                            // if the old user has a lower update count then we can asume that the scheme did a save. That way we don't have to.
                            // check if user has been changed before saving so that we don't needlessly save.
                            if (oldUser.getUpdateCount() >= user.getUpdateCount() && !areUsersEqual(oldUser, user)) { 
                                user.save();
                            }
                        }
                    } catch (Throwable e) {
                        log.error("Could not perform update on scheme : " + schemeName + " for user : " + user.getLogin(), e);
                    }
                }
            } catch (RepositoryException e) {
                log.error("Could not perform update on scheme : " + schemeName, e);
            }
        }

    }
    
    private boolean areUsersEqual (User oldUser, User user) {
        StringComparator sc = new StringComparator();
        
        if (oldUser.getId() != user.getId()) return false;
        if (sc.compare(oldUser.getLogin(), user.getLogin()) != 0) return false;
        if (sc.compare(oldUser.getFirstName(), user.getFirstName()) != 0) return false;
        if (sc.compare(oldUser.getLastName(), user.getLastName()) != 0) return false;
        if (sc.compare(oldUser.getEmail(), user.getEmail()) != 0) return false;
        if (sc.compare(oldUser.getConfirmKey(), user.getConfirmKey()) != 0) return false;
        if (sc.compare(oldUser.getAuthenticationScheme(), user.getAuthenticationScheme()) != 0) return false;
        
        long[] oldRoleIds = oldUser.getAllRoleIds();
        long[] newRoleIds = user.getAllRoleIds();
        Arrays.sort(oldRoleIds);
        Arrays.sort(newRoleIds);        
        if (!Arrays.equals(oldRoleIds, newRoleIds)) return false;
        
        if (oldUser.getDefaultRole() != null && user.getDefaultRole() != null) {
            if (oldUser.getDefaultRole().getId() != user.getDefaultRole().getId()) return false;
        } else if (oldUser.getDefaultRole() == null && user.getDefaultRole() == null) {
            // do nothing
        } else {
            return false;
        }        
                
        return true;
    }

    private class UpdatingAuthenticationSchemePluginUser implements PluginUser<UpdatingAuthenticationScheme> {

        public void pluginAdded(PluginHandle<UpdatingAuthenticationScheme> pluginHandle) {
            if (pluginHandle.getName().length() > 50)
                throw new RuntimeException("The name of an authentication scheme may not exceed 50 characters.");

            schemes.put(pluginHandle.getName(), pluginHandle.getPlugin());
        }

        public void pluginRemoved(PluginHandle<UpdatingAuthenticationScheme> pluginHandle) {
            schemes.remove(pluginHandle.getName());
        }
    }
    
    private class StringComparator implements Comparator<String> {

        public int compare(String s1, String s2) {
            if (s1 == null) {
                if (s2 == null) return 0;
                else return -1;
            }
            if (s2 == null) {
                return 1;
            }
            return s1.compareTo(s2);
        }
        
    }
}
