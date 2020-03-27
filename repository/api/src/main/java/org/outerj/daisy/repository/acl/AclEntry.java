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
package org.outerj.daisy.repository.acl;

import org.outerx.daisy.x10.AclEntryDocument;

/**
 * An AclEntry specifies the permissions for a certain subject.
 *
 * <p>To save modification to this AclEntry, call {@link Acl#save()}
 * on the containing Acl object.
 */
public interface AclEntry {
    AclActionType get(AclPermission aclPermission);

    AccessDetails getDetails(AclPermission aclPermission);

    void set(AclPermission permission, AclActionType action);

    void set(AclPermission permission, AclActionType action, AccessDetails accessDetails);

    /**
     * Sets all permissions to the specified action.
     */
    void setAll(AclActionType action);

    /**
     * What's returned here depends on (or should be intrepreted according to)
     * what {@link #getSubjectType()} returns.
     *
     * <ul>
     *  <li>{@link AclSubjectType#USER}: a user id
     *  <li>{@link AclSubjectType#ROLE}: a role id
     *  <li>{@link AclSubjectType#EVERYONE} or {@link AclSubjectType#OWNER}: -1
     * </ul>
     */
    long getSubjectValue();

    AclSubjectType getSubjectType();

    void setSubjectValue(long value);

    void setSubjectType(AclSubjectType subjectType);

    /**
     * Creates AccessDetails for the {@link AclPermission#READ} permission.
     *
     * <p>This method is for backwards compatibility, it is recommended to
     * use {@link #createNewDetails(AclPermission)} instead.
     */
    AccessDetails createNewDetails();
    
    AccessDetails createNewDetails(AclPermission permission);

    AclEntryDocument getXml();
}
