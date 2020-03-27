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

import java.util.ArrayList;

import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.Roles;
import org.outerx.daisy.x10.RolesDocument;
import org.outerx.daisy.x10.RoleDocument;

public class RolesImpl implements Roles {
    private Role[] roles;
    
    public RolesImpl(Role[] roles) {
        this.roles = roles;
    }

    public Role[] getArray() {
        return roles;
    }

    public RolesDocument getXml() {
        RolesDocument rolesDocument = RolesDocument.Factory.newInstance();
        RolesDocument.Roles rolesXml = rolesDocument.addNewRoles();
        
        ArrayList rolesList = new ArrayList();
        
        for (int i = 0; i < roles.length; i++) {
            rolesList.add(roles[i].getXml().getRole());
        }
        
        rolesXml.setRoleArray((RoleDocument.Role[])rolesList.toArray(new RoleDocument.Role[roles.length]));
        return rolesDocument;
    }

}
