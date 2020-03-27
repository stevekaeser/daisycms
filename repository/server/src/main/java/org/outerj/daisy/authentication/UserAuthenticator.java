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
package org.outerj.daisy.authentication;

import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.AuthenticationSchemeInfos;

/**
 * The UserAuthenticator is responsible for the authentication of users, using registered
 * {@link org.outerj.daisy.authentication.spi.AuthenticationScheme} plugins.
 */
public interface UserAuthenticator {
    public void setUserManager(UserManager userManager);

    /**
     *
     * @throws org.outerj.daisy.repository.AuthenticationFailedException if the authentication failed
     * @throws org.outerj.daisy.authentication.spi.AuthenticationException if something failed while authenticating
     */
    public AuthenticatedUser authenticate(Credentials credentials) throws RepositoryException;

    /**
     * Returns the list of available (= registered) authentication schemes.
     */
    public AuthenticationSchemeInfos getAuthenticationSchemes();
}
