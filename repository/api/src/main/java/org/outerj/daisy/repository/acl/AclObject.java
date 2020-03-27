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

import org.outerx.daisy.x10.AclObjectDocument;

/**
 * Specifies the object (i.e. the documents) to which a set of
 * AclEntries apply.
 *
 * <p>To save modification to this AclObject, call {@link Acl#save()}
 * on the containing Acl object.
 */
public interface AclObject {
    public String getObjectExpr();

    public void setObjectExpr(String expr);

    public AclEntry createNewEntry(AclSubjectType subjectType, long subjectValue);

    public AclEntry get(int index);

    public void remove(int index);

    public void add(AclEntry aclEntry);

    public void add(int index, AclEntry aclEntry);

    public void clear();

    public int size();
    
    public AclObjectDocument getXml();
}
