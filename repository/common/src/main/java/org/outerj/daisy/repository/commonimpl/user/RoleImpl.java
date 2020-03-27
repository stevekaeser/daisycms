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

import java.util.Date;
import java.util.GregorianCalendar;

import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.RoleDocument;

/**
 * An administrative role object.
 * 
 * <p>It is possible to change the role name before the object is persisted. 
 * After the save() method is called (i.e. persistence has happened), the 
 * rolename can no longer be changed!
 */
public class RoleImpl implements Role {
    private long id=-1;
    private String name;
    private String description;
    private long lastModifier;
    private Date lastModified;
    private long updateCount = 0;
    private AuthenticatedUser requestingUser;
    private UserManagementStrategy userManagementStrategy;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private boolean readOnly = false;
    private static final String READ_ONLY_MESSAGE = "This Role object is read-only.";

    /**
     * @param userManagementStrategy
     * @param roleName
     */
    public RoleImpl(UserManagementStrategy userManagementStrategy, String roleName, AuthenticatedUser requestingUser) {
        this.userManagementStrategy = userManagementStrategy;
        name = roleName;
        this.requestingUser = requestingUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String roleName) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        name = roleName;
    }

    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        this.description = description;
    }

    public long getId() {
        return id;
    }

    /**
     * persists the state of this object to the data store 
     */

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        userManagementStrategy.store(this);
    }

    /**
     * return the xml representation of this Role
     */

    public RoleDocument getXml() {
        RoleDocument roleDocument = RoleDocument.Factory.newInstance();
        RoleDocument.Role roleXml = roleDocument.addNewRole();
 
        roleXml.setDescription(description);
        roleXml.setName(name);
        roleXml.setUpdateCount(updateCount);

        if (id!=-1) {
            roleXml.setId(id);
            GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
            lastModifiedCalendar.setTime(lastModified);
            
            roleXml.setLastModified(lastModifiedCalendar);
            roleXml.setLastModifier(lastModifier);
        }
        return roleDocument;
    }

    /**
     * request intimate access to this object, only the strategy that created this object
     * is allowed to actually <b>get</b> this intimate access. 
     * @param strategy
     * @return
     */
    public RoleImpl.IntimateAccess getIntimateAccess(UserManagementStrategy strategy) {
        if (this.userManagementStrategy == strategy)
            return intimateAccess;
        else
            return null;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    /**
     * Disables all operations that can modify the state of this object.
     */
    public void makeReadOnly() {
        this.readOnly = true;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public void setLastModified(Date lastModDate) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            lastModified = lastModDate;
        }
        public void setLastModifier(long lastMod) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            lastModifier = lastMod;
        }
        public AuthenticatedUser getCurrentUser() {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            return requestingUser;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            RoleImpl.this.id = id;
        }

        public void saved(long id, String roleName, String roleDescription, long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            RoleImpl.this.id = id;
            name = roleName;
            description = roleDescription;
            RoleImpl.this.updateCount = updateCount;
        }
        
    }
}
