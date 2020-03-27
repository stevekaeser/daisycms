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

import static org.outerj.daisy.repository.acl.AclPermission.*;

import java.util.*;

/**
 * Enumeration of the permissions that can be set
 * in {@link AccessDetails}.
 */
public enum AclDetailPermission {
    /** Can data from versions other than the live one be read? */
    NON_LIVE("non_live", READ),
    ALL_FIELDS("all_fields", READ, WRITE),
    ALL_PARTS("all_parts", READ, WRITE),
    /** Can the fulltext index be read? */
    FULLTEXT_INDEX("fulltext", READ),
    /** If fulltext index can be read, may fulltext context fragments be retrieved? */
    FULLTEXT_FRAGMENTS("ft_fragments", READ),
    /** Can the document summary be read? */
    SUMMARY("summary", READ),
    DOCUMENT_NAME("doc_name", WRITE),
    LINKS("links", WRITE),
    CUSTOM_FIELDS("custom_fields", WRITE),
    COLLECTIONS("collections", WRITE),
    DOCUMENT_TYPE("doctype", WRITE),
    RETIRED("retired", WRITE),
    PRIVATE("private", WRITE),
    REFERENCE_LANGUAGE("reflang", WRITE),
    CHANGE_COMMENT("change_comment", WRITE),
    CHANGE_TYPE("change_type", WRITE),
    SYNCED_WITH("synced_with", WRITE),
    VERSION_META("version_meta", WRITE),
    LIVE_HISTORY("live_history", READ, PUBLISH);

    private String name;
    private Set<AclPermission> applicablePermissions = EnumSet.noneOf(AclPermission.class);

    private static Map<String, AclDetailPermission> PERMISSIONS_BY_NAME = new HashMap<String, AclDetailPermission>();
    static {
        register(NON_LIVE);
        register(ALL_FIELDS);
        register(ALL_PARTS);
        register(FULLTEXT_INDEX);
        register(FULLTEXT_FRAGMENTS);
        register(SUMMARY);
        register(DOCUMENT_NAME);
        register(LINKS);
        register(CUSTOM_FIELDS);
        register(COLLECTIONS);
        register(DOCUMENT_TYPE);
        register(RETIRED);
        register(PRIVATE);
        register(REFERENCE_LANGUAGE);
        register(CHANGE_COMMENT);
        register(CHANGE_TYPE);
        register(SYNCED_WITH);
        register(VERSION_META);
        register(LIVE_HISTORY);
    }

    private static void register(AclDetailPermission adp) {
        PERMISSIONS_BY_NAME.put(adp.name, adp);
    }

    private AclDetailPermission(String name, AclPermission... applicablePermissions) {
        this.name = name;
        this.applicablePermissions.addAll(Arrays.asList(applicablePermissions));
    }

    public String toString() {
        return name;
    }

    public static AclDetailPermission fromString(String name) {
        AclDetailPermission perm = PERMISSIONS_BY_NAME.get(name);
        if (perm == null)
            throw new RuntimeException("Unrecognized ACL read details permission: " + name);
        return perm;
    }

    public boolean appliesTo(AclPermission permission) {
        return this.applicablePermissions.contains(permission);
    }
}

