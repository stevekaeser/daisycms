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

import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10.AclDocument;

import java.util.Date;

/**
 * An ACL (Access Control List) ordered list of AclObjects. An
 * AclObject specifies to what (= which documents) its child AclEntries
 * applies.
 *
 * <p>Modifications to the Acl are only made permanent after a call to
 * {@link #save()}.
 */
public interface Acl {
    /**
     * Creates a new AclObject. This AclObject will not be added to this Acl,
     * use e.g. the {@link #add(AclObject)} method to do that.
     */
    AclObject createNewObject(String objectExpression);

    AclObject get(int index);

    void remove(int index);

    void add(AclObject aclObject);

    void add(int index, AclObject aclObject);

    void clear();

    int size();

    Date getLastModified();

    long getLastModifier();

    /**
     * Saves this Acl. This includes the saving of its child AclObjects
     * and their respective AclEntries.
     */
    void save() throws RepositoryException;

    AclDocument getXml();

    void setFromXml(AclDocument.Acl aclXml);

    long getUpdateCount();
}