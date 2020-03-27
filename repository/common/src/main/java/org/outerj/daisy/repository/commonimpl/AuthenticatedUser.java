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
package org.outerj.daisy.repository.commonimpl;

import org.outerx.daisy.x10.UserInfoDocument;

/**
 * A user object representing an authenticated user.
 *
 * <p>This user object is obtained after successful authentication
 * from the {@link org.outerj.daisy.authentication.UserAuthenticator UserAuthenticator}.
 * This user object is different from {@link org.outerj.daisy.repository.user.User User}
 * which is used for managing users, while this user object is rather
 * a proof of successful authentication used inside the implementation.
 * If you are a user of the repository API, you should never be in
 * contact with this object, this object is only relevant to the internal
 * implementation.
 * 
 */
public interface AuthenticatedUser {
    public long getId();

    /**
     * The login string for this user. See the explanation for
     * {@link #getPassword()} for why this can sometimes be needed.
     */
    public String getLogin();

    /**
     * The password can sometimes be needed if the the Repository implementation
     * itself needs to connect to other services using this user. This is the
     * case e.g. for remote implementations of the repository API. If it's known
     * to be unneeded, this method may return null.
     */
    public String getPassword();

    /**
     * The currently active role of the user. While a user can have multiple roles,
     * there can only be one role 'active' at a time.
     */
    public long[] getActiveRoleIds();

    public boolean isInRole(long roleId);

    public boolean isInAdministratorRole();

    /**
     * The roles this user has. These are the roles that can be supplied
     * to the {@link #setActiveRoleIds(long[])} method.
     */
    public long[] getAvailableRoleIds();

    /**
     * Changes the active roles.
     */
    public void setActiveRoleIds(long[] roleIds);

    public UserInfoDocument getXml();
}
