/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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

import org.outerx.daisy.x10.AccessDetailsDocument;

import java.util.Set;

/**
 * AccessDetails can be part of the Acl model
 * ({@link Acl} -> {@link AclObject} -> {@link AclEntry} -> AccessDetails),
 * and also of {@link AclResultInfo}.
 *
 * <p>When used as part of {@link AclResultInfo}, the {@link AclDetailPermission permissions} will
 * all be set to either {@link AclActionType#GRANT} or {@link AclActionType#DENY}
 */
public interface AccessDetails {
    /**
     * Returns the kind of permission to which these details apply.
     */
    AclPermission getPermission();

    /**
     * Returns true if these access details don't limit any access, in
     * other words if they are equivalent to no details at all.
     */
    boolean isFullAccess();

    /**
     * Returns true if these access details don't limit any access,
     * except maybe for the live version restriction.
     */
    boolean isFullLiveAccess();

    void set(AclDetailPermission permission, AclActionType action);

    AclActionType get(AclDetailPermission permission);

    /**
     * Shortcut for "accessDetails.get(permission) == AclActionType.GRANT".
     */
    boolean isGranted(AclDetailPermission permission);

    /**
     * Returns true if neither the {@link AclDetailPermission#NON_LIVE} permission
     * nor the {@link AclDetailPermission#LIVE_HISTORY} permission are granted.
     */
    boolean liveOnly();
    
    /**
     * Returns true if the {@link AclDetailPermission#LIVE_HISTORY} permission
     * is granted.
     */
    boolean liveHistoryAccess();
    
    boolean canAccessField(String fieldTypeName);

    void addAccessibleField(String fieldTypeName);

    void addAccessibleFields(Set<String> fieldTypeNames);

    /**
     * Returns the names of the accessible fields.
     * This is only relevant when the {@link AclDetailPermission#ALL_FIELDS} permission
     * is denied.
     * This method never returns null, but rather an empty set.
     * The returned sets are unmodifiable.
     */
    Set<String> getAccessibleFields();

    void clearAccessibleFields();

    boolean canAccessPart(String partTypeName);

    Set<String> getAccessibleParts();

    void addAccessiblePart(String partTypeName);

    void addAccessibleParts(Set<String> partTypeName);

    void clearAccessibleParts();

    /**
     * Overwrites permissions in this access details object with permissions
     * from the specified access details object, if they are not {@link org.outerj.daisy.repository.acl.AclActionType#DO_NOTHING}.
     */
    void overwrite(AccessDetails accessDetails);
    
    /**
     * Make this AccessDetails more permissive by adding all the permissions allowed
     * by the given AccessDetails object.
     */
    void makeUnion(AccessDetails accessDetails);

    AccessDetailsDocument getXml();

    void setFromXml(AccessDetailsDocument.AccessDetails detailsXml);

}
