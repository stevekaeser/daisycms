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
package org.outerj.daisy.repository.commonimpl.acl;

import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.acl.AclPermission;
import static org.outerj.daisy.repository.acl.AclSubjectType.*;
import org.outerx.daisy.x10.*;

import java.util.EnumMap;

public final class AclEntryImpl implements AclEntry {
    private AclSubjectType subjectType;
    private long subjectValue;
    private EnumMap<AclPermission, Access> permissions = new EnumMap<AclPermission, Access>(AclPermission.class);
    private AclImpl ownerAcl;
    private boolean isAdded;

    public AclEntryImpl(AclImpl ownerAcl, AclSubjectType subjectType, long subjectValue) {
        if (subjectType == null)
            throw new NullPointerException("null AclSubjectType not allowed.");
        checkSubjectValue(subjectType, subjectValue);

        this.ownerAcl = ownerAcl;
        this.subjectType = subjectType;
        this.subjectValue = subjectValue;
    }

    private void checkSubjectValue(AclSubjectType subjectType, long subjectValue) {
        if (subjectType == EVERYONE && subjectValue != -1)
            throw new IllegalArgumentException("subjectValue should be -1 if subjectType is EVERYONE");
        if (subjectType == OWNER && subjectValue != -1)
            throw new IllegalArgumentException("subjectValue should be -1 if subjectType is OWNER");
        else if (subjectType != EVERYONE && subjectType != OWNER && subjectValue == -1)
            throw new IllegalArgumentException("subjectValue should only be -1 if subjectType is EVERYONE or OWNER");
    }

    protected AclImpl getOwner() {
        return ownerAcl;
    }

    public AclActionType get(AclPermission aclPermission) {
        if (!permissions.containsKey(aclPermission))
            return AclActionType.DO_NOTHING;
        else
            return permissions.get(aclPermission).action;
    }

    public AccessDetails getDetails(AclPermission aclPermission) {
        if (!permissions.containsKey(aclPermission))
            return null;
        else
            return permissions.get(aclPermission).details;
    }

    public void set(AclPermission permission, AclActionType action) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        permissions.put(permission, new Access(action));
    }

    public void set(AclPermission permission, AclActionType action, AccessDetails accessDetails) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        if (accessDetails != null && !action.supportsAccessDetails())
            throw new RuntimeException("Access details are not support for action type \"" + action + "\".");

        if (permissions.containsKey(permission)) {
            AccessDetailsImpl currentDetails = (AccessDetailsImpl)permissions.get(permission).details;
            if (currentDetails != null)
                currentDetails.setIsAdded(false);
        }

        if (accessDetails != null) {
            if (accessDetails.getPermission() != permission)
                throw new IllegalArgumentException("The supplied access details are for permission " + accessDetails.getPermission() + " instead of for permission " + permission);
            preAddChecks(accessDetails);
            ((AccessDetailsImpl)accessDetails).setIsAdded(true);
        }

        permissions.put(permission, new Access(action, accessDetails));
    }

    public void setAll(AclActionType action) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        for (AclPermission permission : AclPermission.values()) {
            set(permission, action);
        }
    }

    public long getSubjectValue() {
        return subjectValue;
    }

    public AclSubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectValue(long value) {
        checkSubjectValue(this.subjectType, value);

        this.subjectValue = value;
    }

    public void setSubjectType(AclSubjectType subjectType) {
        this.subjectType = subjectType;
    }

    private void preAddChecks(AccessDetails details) {
        if (!(details instanceof AccessDetailsImpl))
            throw new RuntimeException("Incorrect AccessDetails implementation provided, only the ones obtained using createNewDetail() on this AclEntry may be used.");

        AccessDetailsImpl detailsImpl = (AccessDetailsImpl)details;

        if (detailsImpl.getOwner() != ownerAcl)
            throw new RuntimeException("The specified AccessDetails belongs to a different Acl, it cannot be added to this AclEntry.");

        if (detailsImpl.isAdded())
            throw new RuntimeException("The specified AccessDetails is already added to the ACL.");
    }

    public AclEntryDocument getXml() {
        AclEntryDocument aclEntryDocument = AclEntryDocument.Factory.newInstance();
        AclEntryDocument.AclEntry aclEntryXml = aclEntryDocument.addNewAclEntry();

        aclEntryXml.setSubjectType(AclSubject.Enum.forString(subjectType.toString()));
        aclEntryXml.setSubjectValue(subjectValue);

        PermissionsDocument.Permissions permissionsXml = aclEntryXml.addNewPermissions();

        for (AclPermission permission : AclPermission.values()) {
            if (permissions.containsKey(permission)) {
                PermissionsDocument.Permissions.Permission permissionXml = permissionsXml.addNewPermission();
                permissionXml.setType(org.outerx.daisy.x10.AclPermission.Enum.forString(permission.toString()));
                permissionXml.setAction(AclAction.Enum.forString(permissions.get(permission).action.toString()));

                AccessDetails details = getDetails(permission);
                if (details != null) {
                    permissionXml.setAccessDetails(details.getXml().getAccessDetails());
                }
            }
        }

        return aclEntryDocument;
    }

    public AccessDetails createNewDetails() {
        return new AccessDetailsImpl(ownerAcl, AclPermission.READ);
    }

    public AccessDetails createNewDetails(AclPermission permission) {
        return new AccessDetailsImpl(ownerAcl, permission);
    }

    private static class Access {
        private AclActionType action;
        private AccessDetails details;

        public Access(AclActionType action, AccessDetails details) {
            this.action = action;
            this.details = details;
        }

        public Access(AclActionType action) {
            this.action = action;
            this.details = null;
        }
    }

    protected boolean isAdded() {
        return isAdded;
    }

    protected void setIsAdded(boolean isAdded) {
        this.isAdded = isAdded;
    }
}
