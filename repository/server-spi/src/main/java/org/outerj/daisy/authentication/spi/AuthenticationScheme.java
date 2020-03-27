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
package org.outerj.daisy.authentication.spi;

import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;

/**
 * Interface to be implemented by (password-based) authentication mechanisms.
 *
 * <p>The AuthenticationScheme should be registered as a plugin with the
 * {@link org.outerj.daisy.plugin.PluginRegistry PluginRegistry}
 */
public interface AuthenticationScheme {
    String getDescription();

    /**
     * @return true if authentication successful, false otherwise
     * @throws AuthenticationException if an error occured while authenticating
     */
    boolean check(Credentials credentials) throws AuthenticationException;

    /**
     * Clear caches maintained by this authentication scheme, if any.
     */
    void clearCaches();

    /**
     * If a user does not exist, the authentication scheme can be offered the
     * possibility to create the user (this is defined in the configuration of the
     * UserAuthenticator). It is up to the implementation of this method to
     * check the credentials are ok.
     */
    public User createUser(Credentials crendentials, UserManager userManager) throws AuthenticationException;
}
