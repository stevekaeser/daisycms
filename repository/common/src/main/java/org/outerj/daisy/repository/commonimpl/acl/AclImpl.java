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
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.*;

import java.util.*;

public final class AclImpl implements Acl {
    private List<AclObjectImpl> objects = new ArrayList<AclObjectImpl>();
    private boolean readOnly = false;
    private AclStrategy aclStrategy;
    private Date lastModified;
    private long lastModifier=-1;
    private long id;
    private AuthenticatedUser currentModifier;
    private long updateCount = 0;
    private IntimateAccess intimateAccess = new IntimateAccess();
    protected static final String READ_ONLY_MESSAGE = "This ACL cannot be modified.";

    public AclImpl(AclStrategy aclStrategy, Date lastModified, long lastModifier, long id, AuthenticatedUser currentModifier, long updateCount) {
        this.aclStrategy = aclStrategy;
        this.lastModified = lastModified;
        this.lastModifier = lastModifier;
        this.id = id;
        this.currentModifier = currentModifier;
        this.updateCount = updateCount;
    }

    public IntimateAccess getIntimateAccess(AclStrategy aclStrategy) {
        if (aclStrategy == this.aclStrategy)
            return intimateAccess;
        else
            return null;
    }

    public AclObject createNewObject(String objectExpression) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        return new AclObjectImpl(this, aclStrategy, objectExpression);
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public AclObject get(int index) {
        return objects.get(index);
    }

    public void remove(int index) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        objects.remove(index).setIsAdded(false);
    }

    public void add(AclObject aclObject) {
        preAddChecks(aclObject);
        AclObjectImpl aclObjectImpl = (AclObjectImpl)aclObject;
        aclObjectImpl.setIsAdded(true);
        objects.add(aclObjectImpl);
    }

    public void add(int index, AclObject aclObject) {
        preAddChecks(aclObject);
        AclObjectImpl aclObjectImpl = (AclObjectImpl)aclObject;
        aclObjectImpl.setIsAdded(true);
        objects.add(index, aclObjectImpl);
    }

    private void preAddChecks(AclObject aclObject) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        if (aclObject == null)
            throw new NullPointerException("null AclObject not allowed.");

        if (!(aclObject instanceof AclObjectImpl))
            throw new RuntimeException("Incorrect AclObject implementation provided, only the ones obtained using createNewObject() on this AclObject may be used.");

        AclObjectImpl aclObjectImpl = (AclObjectImpl)aclObject;

        if (aclObjectImpl.getOwner() != this)
            throw new RuntimeException("The specified AclObject belongs to a different Acl, it cannot be added to this Acl.");

        if (aclObjectImpl.isAdded())
            throw new RuntimeException("The specified AclObject is already added to the ACL.");
    }

    public void clear() {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        for (AclObjectImpl object : objects) {
            object.setIsAdded(false);
        }

        objects.clear();
    }

    public int size() {
        return objects.size();
    }

    public Date getLastModified() {
        if (lastModified != null)
            return (Date)lastModified.clone();
        else
            return null;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public void save() throws RepositoryException {
        aclStrategy.storeAcl(this);
    }

    public AclDocument getXml() {
        AclDocument aclDocument = AclDocument.Factory.newInstance();
        AclDocument.Acl aclXml = aclDocument.addNewAcl();

        aclXml.setId(id);
        GregorianCalendar lastModifiedCalendar = new GregorianCalendar();
        lastModifiedCalendar.setTime(lastModified);
        aclXml.setLastModified(lastModifiedCalendar);
        aclXml.setLastModifier(lastModifier);
        aclXml.setUpdateCount(updateCount);

        AclObjectDocument.AclObject[] aclObjectsXml = new AclObjectDocument.AclObject[objects.size()];
        for (int i = 0; i < aclObjectsXml.length; i++) {
            AclObject aclObject = objects.get(i);
            aclObjectsXml[i] = aclObject.getXml().getAclObject();
        }

        aclXml.setAclObjectArray(aclObjectsXml);

        return aclDocument;
    }

    public void setFromXml(AclDocument.Acl aclXml) {
        if (readOnly)
            throw new RuntimeException(READ_ONLY_MESSAGE);

        clear();

        for (AclObjectDocument.AclObject objectXml : aclXml.getAclObjectList()) {
            AclObject object = createNewObject(objectXml.getExpression());

            for (AclEntryDocument.AclEntry entryXml : objectXml.getAclEntryList()) {
                AclSubjectType subjectType = AclSubjectType.fromString(entryXml.getSubjectType().toString());
                AclEntry entry = object.createNewEntry(subjectType, entryXml.getSubjectValue());

                for (PermissionsDocument.Permissions.Permission permissionXml : entryXml.getPermissions().getPermissionList()) {
                    AclPermission permission = AclPermission.fromString(permissionXml.getType().toString());
                    AclActionType actionType = AclActionType.fromString(permissionXml.getAction().toString());

                    AccessDetails details = null;
                    if (permissionXml.isSetAccessDetails()) {
                        details = new AccessDetailsImpl(this, permission);
                        details.setFromXml(permissionXml.getAccessDetails());
                    }
                    entry.set(permission, actionType, details);
                }

                object.add(entry);
            }

            add(object);
        }
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            AclImpl.this.id = id;
        }

        public AuthenticatedUser getCurrentModifier() {
            return currentModifier;
        }

        public void setLastModified(Date lastModified) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            AclImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            AclImpl.this.lastModifier = lastModifier;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READ_ONLY_MESSAGE);

            AclImpl.this.updateCount = updateCount;
        }

        public List<AclObjectImpl> getObjects() {
            return objects;
        }
    }
}
