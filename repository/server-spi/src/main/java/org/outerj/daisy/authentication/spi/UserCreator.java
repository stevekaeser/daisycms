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

import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.authentication.spi.AuthenticationException;

/**
 * Helper class to support auto-user-creation.
 *
 * See also {@link UserCreatorFactory}.
 */
public class UserCreator {
    private final String defaultRole;
    private final String[] roles;
    private final boolean updateableByUser;
    private final String scheme;
    private final DefaultUserPropertyVisitor defaultVisitor;

    public UserCreator(String[] roles, String defaultRole, boolean updateableByUser, String scheme) {
        this.roles = roles;
        this.defaultRole = defaultRole;
        this.updateableByUser = updateableByUser;
        this.scheme = scheme;
        this.defaultVisitor = new DefaultUserPropertyVisitor();
    }

    public User create(String login, UserManager userManager) throws AuthenticationException {
        
        return this.create(login, defaultVisitor, userManager);
    }
    
    public User create (String login, UserPropertyVisitor visitor, UserManager userManager) throws AuthenticationException {
        User user = userManager.createUser(login);
        try {
            visitor.visit(user, userManager);            
            
            user.setAuthenticationScheme(scheme);
            user.save();
        } catch (RepositoryException e) {
            throw new AuthenticationException("Error creating new user during login.", e);
        }

        return user;
    }
    
    private class DefaultUserPropertyVisitor implements UserPropertyVisitor {

        public void visit(User user, UserManager userManager) throws RepositoryException{
            for (int i = 0; i < roles.length; i++) {
                Role role = userManager.getRole(roles[i], false);
                user.addToRole(role);
            }

            if (defaultRole != null)
                user.setDefaultRole(userManager.getRole(defaultRole, false));

            user.setUpdateableByUser(updateableByUser);
        }        
    }
}
