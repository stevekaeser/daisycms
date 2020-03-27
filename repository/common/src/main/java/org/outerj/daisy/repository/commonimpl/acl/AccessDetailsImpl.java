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
package org.outerj.daisy.repository.commonimpl.acl;

import org.outerj.daisy.repository.acl.AccessDetails;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import static org.outerj.daisy.repository.acl.AclActionType.*;
import static org.outerj.daisy.repository.acl.AclDetailPermission.*;
import org.outerx.daisy.x10.AccessDetailsDocument;

import java.util.Set;
import java.util.Collections;
import java.util.HashSet;
import java.util.EnumMap;

// To add a new property:
//   - update the "copy constructor"
//   - update the makeUnion method
//   - update the XML Schema and the getXml and setFromXml methods
//   - update the persistence code in LocalAclStrategy

public class AccessDetailsImpl implements AccessDetails {
    /** The kind of permission for which these details are being used. */
    private AclPermission permission;
    private Set<String> partNames;
    private Set<String> fieldNames;
    private EnumMap<AclDetailPermission, AclActionType> permissions = new EnumMap<AclDetailPermission, AclActionType>(AclDetailPermission.class);
    private AclImpl ownerAcl;
    private boolean isAdded;

    public AccessDetailsImpl(AclImpl ownerAcl, AclPermission permission) {
        this.ownerAcl = ownerAcl;
        this.permission = permission;
    }

    public AccessDetailsImpl(AclImpl ownerAcl, AclPermission permission, AclActionType initialAction) {
        this.ownerAcl = ownerAcl;
        this.permission = permission;
        for (AclDetailPermission detailPerm : AclDetailPermission.values()) {
            if (detailPerm.appliesTo(permission))
                permissions.put(detailPerm, initialAction);
        }
    }

    /**
     * Copy constructor.
     */
    public AccessDetailsImpl(AclImpl ownerAcl, AccessDetails accessDetails) {
        this.ownerAcl = ownerAcl;
        this.permission = accessDetails.getPermission();

        for (AclDetailPermission permission : AclDetailPermission.values()) {
            permissions.put(permission, accessDetails.get(permission));
        }

        Set<String> accessibleFields = accessDetails.getAccessibleFields();
        if (!accessibleFields.isEmpty()) {
            fieldNames = new HashSet<String>();
            fieldNames.addAll(accessibleFields);
        }

        Set<String> accessibleParts = accessDetails.getAccessibleParts();
        if (!accessibleParts.isEmpty()) {
            partNames = new HashSet<String>();
            partNames.addAll(accessibleParts);
        }
    }

    public AclPermission getPermission() {
        return permission;
    }

    public AclImpl getOwner() {
        return ownerAcl;
    }

    public boolean isFullAccess() {
        return isFullLiveAccess() && !(permission == AclPermission.READ
                && (!permissions.containsKey(AclDetailPermission.NON_LIVE) || permissions.get(AclDetailPermission.NON_LIVE) != AclActionType.GRANT));
    }

    public boolean isFullLiveAccess() {
        for (AclDetailPermission permission : AclDetailPermission.values()) {
            if (permission.appliesTo(this.permission) && permission != NON_LIVE && permission != LIVE_HISTORY && get(permission) != GRANT)
                return false;
        }
        return true;
    }


    public void set(AclDetailPermission permission, AclActionType action) {
        checkReadOnly();
        if (!permission.appliesTo(this.permission))
            throw new IllegalArgumentException("Detail permission " + permission + " cannot be used for permission " + this.permission);
        
        permissions.put(permission, action);

        if (permission == ALL_FIELDS && action != AclActionType.DENY)
            fieldNames = null;

        if (permission == ALL_PARTS && action != AclActionType.DENY)
            partNames = null;
    }

    public AclActionType get(AclDetailPermission permission) {
        if (!permissions.containsKey(permission))
            return DO_NOTHING;
        else
            return permissions.get(permission);
    }

    public boolean isGranted(AclDetailPermission permission) {
        return get(permission) == GRANT;
    }

    public boolean canAccessField(String fieldTypeName) {
        return get(ALL_FIELDS) == GRANT || (fieldNames != null && fieldNames.contains(fieldTypeName));
    }

    public boolean canAccessPart(String partTypeName) {
        return get(ALL_PARTS) == GRANT || (partNames != null && partNames.contains(partTypeName));
    }

    public Set<String> getAccessibleFields() {
        return fieldNames == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(fieldNames);
    }

    public void clearAccessibleFields() {
        checkReadOnly();

        if (fieldNames != null)
            fieldNames.clear();
    }

    public Set<String> getAccessibleParts() {
        return partNames == null ? Collections.<String>emptySet() : Collections.unmodifiableSet(partNames);
    }

    public void addAccessibleField(String fieldTypeName) {
        checkReadOnly();

        if (get(ALL_FIELDS) != AclActionType.DENY)
            throw new IllegalStateException("Accessible fields can only be specified if access to all fields is denied.");

        if (fieldNames == null)
            fieldNames = new HashSet<String>();

        fieldNames.add(fieldTypeName);
    }

    public void addAccessibleFields(Set<String> fieldTypeNames) {
        checkReadOnly();

        if (get(ALL_FIELDS) != AclActionType.DENY)
            throw new IllegalStateException("Accessible fields can only be specified if access to all fields is denied.");

        if (fieldNames == null)
            fieldNames = new HashSet<String>();

        fieldNames.addAll(fieldTypeNames);
    }

    public void addAccessiblePart(String partTypeName) {
        checkReadOnly();

        if (get(ALL_PARTS) != AclActionType.DENY)
            throw new IllegalStateException("Accessible parts can only be specified if access to all parts is denied.");

        if (partNames == null)
            partNames = new HashSet<String>();

        partNames.add(partTypeName);
    }

    public void addAccessibleParts(Set<String> partTypeNames) {
        checkReadOnly();

        if (get(ALL_PARTS) != AclActionType.DENY)
            throw new IllegalStateException("Accessible parts can only be specified if access to all parts is denied.");

        if (partNames == null)
            partNames = new HashSet<String>();

        partNames.addAll(partTypeNames);
    }

    public void clearAccessibleParts() {
        checkReadOnly();

        if (partNames != null)
            partNames.clear();
    }

    public boolean liveOnly() {
        return permission == AclPermission.READ
                && (!isGranted(AclDetailPermission.NON_LIVE))
                && (!isGranted(AclDetailPermission.LIVE_HISTORY));
    }
    
    public boolean liveHistoryAccess() {
        return permission == AclPermission.READ && ((permissions.get(AclDetailPermission.NON_LIVE)) == AclActionType.GRANT
                || (permissions.get(AclDetailPermission.LIVE_HISTORY) == AclActionType.GRANT)); 
    }
    
    public void overwrite(AccessDetails accessDetails) {
        checkReadOnly();

        if (accessDetails.getPermission() != this.permission)
            throw new IllegalArgumentException("The supplied access details are for permission " + accessDetails + " while these access details are for " + this.permission);

        for (AclDetailPermission permission : AclDetailPermission.values()) {
            AclActionType action = accessDetails.get(permission);
            if (action != DO_NOTHING) {
                set(permission, action);
            }
        }

        Set<String> accessibleFields = accessDetails.getAccessibleFields();
        if (!accessibleFields.isEmpty()) {
            addAccessibleFields(accessibleFields);
        }

        Set<String> accessibleParts = accessDetails.getAccessibleParts();
        if (!accessibleParts.isEmpty()) {
            addAccessibleParts(accessibleParts);
        }
    }

    public void makeUnion(AccessDetails accessDetails) {
        checkReadOnly();

        if (accessDetails.getPermission() != this.permission)
            throw new IllegalArgumentException("The supplied access details are for permission " + accessDetails + " while these access details are for " + this.permission);

        for (AclDetailPermission permission : AclDetailPermission.values()) {
            if (get(permission) == DENY && accessDetails.get(permission) == GRANT) {
                set(permission, GRANT);
            }
        }

        if (get(ALL_FIELDS) == AclActionType.DENY) {
            Set<String> accessibleFields = accessDetails.getAccessibleFields();
            if (!accessibleFields.isEmpty())
                addAccessibleFields(accessibleFields);
        }

        if (get(ALL_PARTS) == AclActionType.DENY) {
            Set<String> accessibleParts = accessDetails.getAccessibleParts();
            if (!accessibleParts.isEmpty())
                addAccessibleParts(accessibleParts);
        }
    }

    public AccessDetailsDocument getXml() {
        AccessDetailsDocument doc = AccessDetailsDocument.Factory.newInstance();
        AccessDetailsDocument.AccessDetails xml = doc.addNewAccessDetails();

        for (AclDetailPermission permission : AclDetailPermission.values()) {
            if (permission.appliesTo(this.permission)) {
                AccessDetailsDocument.AccessDetails.Permission permissionXml = xml.addNewPermission();
                permissionXml.setType(permission.toString());
                permissionXml.setAction(get(permission).toString());
            }
        }

        for (String field : getAccessibleFields()) {
            xml.addNewAllowFieldAccess().setName(field);
        }

        for (String part : getAccessibleParts()) {
            xml.addNewAllowPartAccess().setName(part);
        }

        return doc;
    }

    public void setFromXml(AccessDetailsDocument.AccessDetails detailsXml) {
        checkReadOnly();

        for (AccessDetailsDocument.AccessDetails.Permission permissionXml : detailsXml.getPermissionList()) {
            AclDetailPermission permission = AclDetailPermission.fromString(permissionXml.getType());
            AclActionType action = AclActionType.fromString(permissionXml.getAction());
            set(permission, action);
        }

        clearAccessibleFields();
        for (AccessDetailsDocument.AccessDetails.AllowFieldAccess fieldXml : detailsXml.getAllowFieldAccessList()) {
            addAccessibleField(fieldXml.getName());
        }

        clearAccessibleParts();
        for (AccessDetailsDocument.AccessDetails.AllowPartAccess partXml : detailsXml.getAllowPartAccessList()) {
            addAccessiblePart(partXml.getName());
        }
    }

    private void checkReadOnly() {
        if (ownerAcl != null && ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);
    }

    protected boolean isAdded() {
        return isAdded;
    }

    protected void setIsAdded(boolean isAdded) {
        this.isAdded = isAdded;
    }

}
