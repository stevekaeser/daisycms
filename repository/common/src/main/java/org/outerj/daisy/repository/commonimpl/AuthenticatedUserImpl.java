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
import org.outerj.daisy.repository.user.Role;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class AuthenticatedUserImpl implements AuthenticatedUser {
    private long userId;
    private String password;
    private long[] activeRoleIds;
    private long[] availableRoleIds;
    private String login;

    public AuthenticatedUserImpl(long userId, String password, long[] activeRoleIds, long[] availableRoleIds, String login) {
        this.userId = userId;
        this.password = password;
        this.activeRoleIds = activeRoleIds;
        this.availableRoleIds = availableRoleIds;
        this.login = login;
    }

    public long getId() {
        return userId;
    }

    public String getLogin() {
        return login;
    }

    public String getPassword() {
        return password;
    }

    public long[] getActiveRoleIds() {
        return activeRoleIds.clone();
    }

    public long[] getAvailableRoleIds() {
        return availableRoleIds.clone();
    }

    public boolean isInRole(long roleId) {
        for (long activeRoleId : activeRoleIds)
            if (activeRoleId == roleId)
                return true;
        return false;
    }

    public boolean isInAdministratorRole() {
        return isInRole(Role.ADMINISTRATOR);
    }

    public void setActiveRoleIds(long[] roleIds) {
        if (roleIds.length < 1)
            throw new IllegalArgumentException("Error setting active roles: at least one role must be specified.");

        // check that the user has all of the specified roles, and remove duplicates.
        Set<Long> roles = new HashSet<Long>();
        for (long roleId : roleIds) {
            if (!hasRole(roleId))
                throw new IllegalArgumentException("Error setting active roles: user \"" + login + "\" does not have this role: \"" + roleId + "\".");
            roles.add(new Long(roleId));
        }

        long[] newActiveRoles = new long[roles.size()];
        Iterator it = roles.iterator();
        int i = 0;
        while (it.hasNext()) {
            newActiveRoles[i] = ((Long)it.next()).longValue();
            i++;
        }
        this.activeRoleIds = newActiveRoles;
    }

    private boolean hasRole(long roleId) {
        for (long availableRoleId : availableRoleIds) {
            if (availableRoleId == roleId)
                return true;
        }
        return false;
    }

    public UserInfoDocument getXml() {
        UserInfoDocument userInfoDocument = UserInfoDocument.Factory.newInstance();
        UserInfoDocument.UserInfo userInfo = userInfoDocument.addNewUserInfo();

        userInfo.setUserId(userId);
        userInfo.addNewActiveRoleIds().setRoleIdArray(getActiveRoleIds());
        userInfo.addNewAvailableRoleIds().setRoleIdArray(getAvailableRoleIds());

        return userInfoDocument;
    }
}
