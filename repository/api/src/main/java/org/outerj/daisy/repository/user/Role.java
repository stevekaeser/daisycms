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
package org.outerj.daisy.repository.user;

import java.util.Date;

import org.outerx.daisy.x10.RoleDocument;
import org.outerj.daisy.repository.RepositoryException;

/**
 * A role that a user can have.
 */
public interface Role {
    /**
     * ID of the Administrator role. The Administrator is a built-in
     * role required for the correct operation of the repository.
     * Some operations can only be done by users having the Administrator
     * role.
     */
    long ADMINISTRATOR = 1;

    /**
     * Gets the name of this role.
     */
    String getName();

    /**
     * Sets the name of this role.
     */
    void setName(String roleName);

    /**
     * Gets the description of this role, which can be null.
     */
    String getDescription();

    /**
     * Sets the description of this role. Can be set to null.
     */
    void setDescription(String description);

    /**
     * Gets the id of this role, or -1 if this role object hasn't been saved yet.
     */
    long getId();

    /**
     * Persist this Role to the data store. If this is the first time
     * the role is saved, its id will be assigned.
     */
    void save() throws RepositoryException;

    /**
     * Gets the last modified date of this role object.
     * 
     * <p>Returns null if this object hasn't been saved to the data store yet.
     */
    Date getLastModified();

    /**
     * Gets the data store id of the last modifier of this role object.
     * 
     * <p>Returns null if this object hasn't been saved to the data store yet.
     */
    long getLastModifier();
    
    /**
     * Gets an XML representation of this object.
     */
    RoleDocument getXml();

    long getUpdateCount();
}
