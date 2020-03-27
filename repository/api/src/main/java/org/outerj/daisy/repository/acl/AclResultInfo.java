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

import org.outerx.daisy.x10.AclResultDocument;

/**
 * An object holding information about the evaluation of the ACL rules for a certain
 * user and/or role combination, for a certain document.
 *
 * <p>Provides not only information about the resulting permissions, but also about
 * why they were granted or denied.
 */
public interface AclResultInfo {

    void set(AclPermission permission, AclActionType action, String objectExpr, String subjectReason);

    void set(AclPermission permission, AclActionType action, AccessDetails details, String objectExpr, String subjectReason);

    void setDetails(AclPermission permission, AccessDetails details);

    AclActionType getActionType(AclPermission permission);

    AccessDetails getAccessDetails(AclPermission permission);

    boolean isAllowed(AclPermission permission);

    boolean isFullyAllowed(AclPermission permission);

    boolean isNonLiveAllowed(AclPermission permission);

    boolean isLiveHistoryAllowed(AclPermission permission);

    String getObjectExpr(AclPermission permission);

    String getSubjectReason(AclPermission permission);

    long getUserId();

    long[] getRoleIds();

    String getDocumentId();

    long getBranchId();

    long getLanguageId();

    AclResultDocument getXml();

    void setFromXml(AclResultDocument.AclResult aclResultXml);

    AclResultInfo clone() throws CloneNotSupportedException;

    /**
     * Returns a compact string representation of the most useful access information.
     */
    String getCompactString();
}
