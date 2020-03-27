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
package org.outerj.daisy.repository.commonimpl.user;

import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.Roles;

/**
 * A set of utility methods for working with Roles objects.
 */
public class RolesUtil {
    private static Role[] roles;
    /**
     * returns a specific role from a roles object.
     * 
     * @param rolesToSearch the roles object to search the role in
     * @param roleName the name of the role object to search in the roles object
     * @return the found Role, or null if the role with the specified name
     * was not found in the Roles object.
     */
    public static Role getRole(Roles rolesToSearch, String roleName) {
        if (rolesToSearch == null || roleName == null) 
            throw new IllegalArgumentException("One of the arguments was null!");

        roles = rolesToSearch.getArray();
        if (roles.length==0) return null;
        else {
            int i=0;
            Role r = null;
            boolean nameFound = false;
            
            while (i<roles.length && !nameFound) {
                r=roles[i];
                if (r.getName().equals(roleName))
                    nameFound=true;
                i++;
            }
            
            if(!nameFound) return null;
            else return r;
        }

    }

    /**
     * returns a specific role from a roles object.
     * 
     * @param rolesToSearch the roles object to search the role in
     * @param roleId the data store id of the role object to search in the roles object
     * @return the found Role, or null if the role with the specified id 
     * was not found in the Roles object.
     */
    public static Role getRole(Roles rolesToSearch, long roleId) {
        if (rolesToSearch == null) 
            throw new IllegalArgumentException("Roles argument was null!");

        roles = rolesToSearch.getArray();
        if (roles.length==0) return null;
        else {
            int i=0;
            Role r = null;
            boolean roleFound = false;
            
            while (i<roles.length && !roleFound) {
                r=roles[i];
                if (r.getId() == roleId)
                    roleFound=true;
                i++;
            }
            
            if(!roleFound) return null;
            else return r;
        }

    }
        
}
