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
import org.outerx.daisy.x10.AclObjectDocument;
import org.outerx.daisy.x10.AclEntryDocument;

import java.util.ArrayList;
import java.util.List;

public final class AclObjectImpl implements AclObject {
    private String objectExpr;
    private Object compiledExpr;
    private List<AclEntryImpl> entries = new ArrayList<AclEntryImpl>();
    private AclImpl ownerAcl;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private AclStrategy aclStrategy;
    private boolean isAdded;

    public AclObjectImpl(AclImpl ownerAcl, AclStrategy aclStrategy, String objectExpr) {
        if (objectExpr == null)
            throw new NullPointerException("objectExpr cannot be null");

        this.objectExpr = objectExpr;
        this.ownerAcl = ownerAcl;
        this.aclStrategy = aclStrategy;
    }

    public IntimateAccess getIntimateAccess(AclStrategy aclStrategy) {
        if (aclStrategy == this.aclStrategy)
            return intimateAccess;
        else
            return null;
    }

    protected AclImpl getOwner() {
        return ownerAcl;
    }

    public String getObjectExpr() {
        return objectExpr;
    }

    public void setObjectExpr(String expr) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        if (expr == null)
            throw new NullPointerException("expr cannot be null");

        this.objectExpr = expr;
        this.compiledExpr = null;
    }

    public AclEntry createNewEntry(AclSubjectType subjectType, long subjectValue) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        return new AclEntryImpl(ownerAcl, subjectType, subjectValue);
    }

    public AclEntry get(int index) {
        return entries.get(index);
    }

    public void remove(int index) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        entries.remove(index).setIsAdded(false);
    }

    public void add(AclEntry aclEntry) {
        preAddChecks(aclEntry);
        AclEntryImpl aclEntryImpl = (AclEntryImpl)aclEntry;
        aclEntryImpl.setIsAdded(true);
        entries.add(aclEntryImpl);
    }

    private void preAddChecks(AclEntry aclEntry) {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        if (aclEntry == null)
            throw new NullPointerException("null AclEntry not allowed.");

        if (!(aclEntry instanceof AclEntryImpl))
            throw new RuntimeException("Incorrect AclEntry implementation provided, only the ones obtained using createNewEntry() on this AclObject may be used.");

        AclEntryImpl aclEntryImpl = (AclEntryImpl)aclEntry;

        if (aclEntryImpl.getOwner() != ownerAcl)
            throw new RuntimeException("The specified AclEntry belongs to a different Acl, it cannot be added to this AclObject.");

        if (aclEntryImpl.isAdded())
            throw new RuntimeException("The specified AclEntry is already added to the ACL.");
    }

    public void add(int index, AclEntry aclEntry) {
        preAddChecks(aclEntry);
        AclEntryImpl aclEntryImpl = (AclEntryImpl)aclEntry;
        aclEntryImpl.setIsAdded(true);
        entries.add(index, aclEntryImpl);
    }

    public void clear() {
        if (ownerAcl.isReadOnly())
            throw new RuntimeException(AclImpl.READ_ONLY_MESSAGE);

        for (AclEntryImpl entry : entries) {
            entry.setIsAdded(false);
        }

        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public AclObjectDocument getXml() {
        AclObjectDocument aclObjectDocument = AclObjectDocument.Factory.newInstance();
        AclObjectDocument.AclObject aclObject = aclObjectDocument.addNewAclObject();

        aclObject.setExpression(objectExpr);

        AclEntryDocument.AclEntry[] aclEntriesXml = new AclEntryDocument.AclEntry[entries.size()];
        for (int i = 0; i < aclEntriesXml.length; i++) {
            AclEntry aclEntry = entries.get(i);
            aclEntriesXml[i] = aclEntry.getXml().getAclEntry();
        }

        aclObject.setAclEntryArray(aclEntriesXml);

        return aclObjectDocument;
    }

    public class IntimateAccess {
        private IntimateAccess() {
        }

        public List<AclEntryImpl> getEntries() {
            return entries;
        }

        public void setCompiledExpression(Object compiledExpression) {
            compiledExpr = compiledExpression;
        }

        public Object getCompiledExpression() {
            return compiledExpr;
        }
    }

    protected boolean isAdded() {
        return isAdded;
    }

    protected void setIsAdded(boolean isAdded) {
        this.isAdded = isAdded;
    }
}
