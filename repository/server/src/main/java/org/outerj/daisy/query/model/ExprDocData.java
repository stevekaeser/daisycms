/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.query.model;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.VersionedData;
import org.outerj.daisy.repository.acl.AclResultInfo;

public class ExprDocData {
    public final Document document;
    public final Version version;
    public final VersionedData versionedData;
    public final AclResultInfo aclResultInfo;
    public final boolean conceptual;

    /**
     * See {@link #ExprDocData(Document, Version, AclResultInfo)}.
     */
    public ExprDocData(Document document, Version version) {
        this(document, version, null);
    }

    /**
     * Constructor. Document is required, version is optional. In case the version
     * is missing, the data will be retrieved from the document instead. Expression
     * implementatins should not rely on document.getLast/LiveVersion(), since it might
     * be the supplied Document object is not yet saved and hence has no versions yet.
     *
     * @param aclResultInfo the ACL evaluation result for the document for the current user.
     *                      Optional, but if not present might disable certain functions.
     */
    public ExprDocData(Document document, Version version, AclResultInfo aclResultInfo) {
        this(document, version, aclResultInfo, false);
    }

    /**
     *
     * @param conceptual this only has meaning for ACL expression purposes. Indicates if
     *                   the document is 'conceptual', used for getting the initial
     *                   access permissions for new documents from the ACL.
     */
    public ExprDocData(Document document, Version version, AclResultInfo aclResultInfo, boolean conceptual) {
        if (document == null)
            throw new IllegalArgumentException("Null argument: document");
        this.document = document;
        this.version = version;
        this.versionedData = version != null ? version : document;
        this.aclResultInfo = aclResultInfo;
        this.conceptual = conceptual;
    }
}
