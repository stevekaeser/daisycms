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

import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclActionType;
import static org.outerj.daisy.repository.acl.AclActionType.*;
import org.outerj.daisy.repository.acl.AccessDetails;
import static org.outerj.daisy.repository.acl.AclDetailPermission.*;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerx.daisy.x10.AclResultDocument;
import org.outerx.daisy.x10.AclAction;
import org.outerx.daisy.x10.PermissionsDocument;

import java.util.EnumMap;
import java.util.Map;

public class AclResultInfoImpl implements AclResultInfo {
    private EnumMap<AclPermission, Info> infos = new EnumMap<AclPermission, Info>(AclPermission.class);
    private long userId;
    private long[] roleIds;
    private String documentId;
    private long branchId;
    private long languageId;

    public AclResultInfoImpl(long userId, long[] roleIds, String documentId, long branchId, long languageId) {
        for (AclPermission permission : AclPermission.values()) {
            infos.put(permission, new Info());
        }
        this.userId = userId;
        this.roleIds = roleIds;
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
    }

    private AclResultInfoImpl() {
        // used by clone method
    }

    public void set(AclPermission permission, AclActionType action, String objectExpr, String subjectReason) {
        Info info = infos.get(permission);
        info.action = action;
        info.objectExpr = objectExpr;
        info.details = null;
        info.subjectReason = subjectReason;
    }

    public void set(AclPermission permission, AclActionType action, AccessDetails details, String objectExpr, String subjectReason) {
        if (details != null && action != AclActionType.GRANT)
            throw new IllegalStateException("AccessDetails can only be specified if the action is GRANT.");
        if (details != null && details.getPermission() != permission)
            throw new IllegalArgumentException("The supplied AccessDetails is not for permission " + permission + " but for " + details.getPermission());

        Info info = infos.get(permission);
        info.action = action;
        info.details = details;
        info.objectExpr = objectExpr;
        info.subjectReason = subjectReason;
    }

    public void setDetails(AclPermission permission, AccessDetails details) {
        Info info = infos.get(permission);
        set(permission, info.action, details, info.objectExpr, info.subjectReason);
    }

    public AclActionType getActionType(AclPermission permission) {
        return infos.get(permission).action;
    }

    public AccessDetails getAccessDetails(AclPermission permission) {
        return infos.get(permission).details;
    }

    public boolean isAllowed(AclPermission permission) {
        return infos.get(permission).action == GRANT;
    }

    public boolean isFullyAllowed(AclPermission permission) {
        Info info = infos.get(permission);
        return (info.details == null || info.details.isFullAccess()) && info.action == GRANT;
    }

    public boolean isNonLiveAllowed(AclPermission permission) {
        Info info = infos.get(permission);
        return (info.details == null || info.details.isGranted(NON_LIVE)) && info.action == GRANT;
    }

    public boolean isLiveHistoryAllowed(AclPermission permission) {
        Info info = infos.get(permission);
        return (isNonLiveAllowed(permission) || info.details == null || info.details.isGranted(LIVE_HISTORY)) && info.action == GRANT;
    }

    public String getObjectExpr(AclPermission permission) {
        return infos.get(permission).objectExpr;
    }

    public String getSubjectReason(AclPermission permission) {
        return infos.get(permission).subjectReason;
    }

    public long getUserId() {
        return userId;
    }

    public long[] getRoleIds() {
        return roleIds;
    }

    public String getDocumentId() {
        return documentId;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    private class Info {
        public AclActionType action;
        public AccessDetails details;
        public String objectExpr;
        public String subjectReason;

        public Object clone() throws CloneNotSupportedException {
            Info clone = new Info();
            clone.action = action;
            clone.details = details; // assumes details are not mutable
            clone.objectExpr = objectExpr;
            clone.subjectReason = subjectReason;
            return clone;
        }
    }

    public void dump() {
        for (AclPermission permission : AclPermission.values()) {
            System.out.println("Permission: " + permission);
            System.out.println("Action: " + infos.get(permission).action);
            System.out.println("Matching object expression: " + infos.get(permission).objectExpr);
            System.out.println("Matching subject: " + infos.get(permission).subjectReason);
            System.out.println("---------------------------------------------------------------------");
        }
    }

    public AclResultDocument getXml() {
        AclResultDocument aclResultDocument = AclResultDocument.Factory.newInstance();
        AclResultDocument.AclResult aclResultXml = aclResultDocument.addNewAclResult();

        AclResultDocument.AclResult.User userXml = aclResultXml.addNewUser();
        userXml.setId(userId);
        userXml.addNewRoles().setRoleIdArray(roleIds);
        aclResultXml.setDocumentId(documentId);
        aclResultXml.setBranchId(branchId);
        aclResultXml.setLanguageId(languageId);

        PermissionsDocument.Permissions permissionsXml = aclResultXml.addNewPermissions();
        for (AclPermission permission : AclPermission.values()) {
            if (infos.get(permission).action != null) {
                Info info = infos.get(permission);
                PermissionsDocument.Permissions.Permission permissionXml = permissionsXml.addNewPermission();
                permissionXml.setType(org.outerx.daisy.x10.AclPermission.Enum.forString(permission.toString()));
                permissionXml.setAction(AclAction.Enum.forString(info.action.toString()));
                permissionXml.setObjectReason(info.objectExpr);
                permissionXml.setSubjectReason(info.subjectReason);
                if (info.details != null) {
                    permissionXml.setAccessDetails(info.details.getXml().getAccessDetails());
                }
            }
        }

        return aclResultDocument;
    }

    public void setFromXml(AclResultDocument.AclResult aclResultXml) {
        // first reset everything
        for (AclPermission permission : AclPermission.values()) {
            Info info = infos.get(permission);
            info.objectExpr = null;
            info.subjectReason = null;
            info.action = null;
        }

        for (PermissionsDocument.Permissions.Permission permissionXml : aclResultXml.getPermissions().getPermissionList()) {
            AclPermission permission = AclPermission.fromString(permissionXml.getType().toString());
            Info info = infos.get(permission);
            info.action = AclActionType.fromString(permissionXml.getAction().toString());
            info.objectExpr = permissionXml.getObjectReason();
            info.subjectReason = permissionXml.getSubjectReason();

            if (permissionXml.isSetAccessDetails()) {
                AccessDetails details = new AccessDetailsImpl(null, permission);
                details.setFromXml(permissionXml.getAccessDetails());
                info.details = details;
            }
        }
    }

    public AclResultInfo clone() throws CloneNotSupportedException {
        AclResultInfoImpl clone = new AclResultInfoImpl();
        clone.userId = userId;
        clone.roleIds = roleIds.clone();
        clone.documentId = documentId;
        clone.branchId = branchId;
        clone.languageId = languageId;

        for (AclPermission permission : AclPermission.values()) {
            clone.infos.put(permission, (Info)infos.get(permission).clone());
        }

        return clone;
    }

    public String getCompactString() {
        StringBuilder result = new StringBuilder();

        for (Map.Entry<AclPermission, Info> info : infos.entrySet()) {
            if (info.getValue().action == GRANT) {
                if (result.length() > 0)
                    result.append(",");
                switch (info.getKey()) {
                    case READ:
                        result.append("read");
                        AccessDetails details = info.getValue().details;
                        if (details != null && !details.isFullAccess()) {
                            if (details.liveOnly())
                                result.append(",liveOnly");
                            if (!details.isFullLiveAccess())
                                result.append(",restrictedRead"); // means: restrictions besides live-only
                        } else {
                            result.append(",fullRead");
                        }
                        break;
                    case WRITE:
                        result.append("write");
                        break;
                    case DELETE:
                        result.append("delete");
                        break;
                    case PUBLISH:
                        result.append("publish");
                        break;
                }
            }
        }

        return result.toString();
    }
}