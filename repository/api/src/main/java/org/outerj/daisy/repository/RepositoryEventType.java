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
package org.outerj.daisy.repository;

/**
 * Enumeration of the possible repository events. See also
 * {@link RepositoryListener} and {@link Repository#addListener(RepositoryListener)}.
 */
public final class RepositoryEventType {
    private final String name;

    private RepositoryEventType(String name) {
        this.name = name;
    }

    public String toString() {
        return name;
    }

    /**
     * Returns true if this event is related to branches.
     */
    public boolean isBranchEvent() {
        return this == BRANCH_CREATED || this == BRANCH_DELETED || this == BRANCH_UPDATED;
    }

    /**
     * Returns true if this event is related to languages.
     */
    public boolean isLanguageEvent() {
        return this == LANGUAGE_CREATED || this == LANGUAGE_DELETED || this == LANGUAGE_UPDATED;
    }

    /**
     * Returns true if this event is related to namespaces.
     */
    public boolean isNamespaceEvent() {
        return this == NAMESPACE_REGISTERED || this == NAMESPACE_UNREGISTERED || this == NAMESPACE_MANAGED || this == NAMESPACE_UNMANAGED;
    }

    /**
     * Returns true if this event is related to document variants (document variant
     * create-delete-update, lock change, version metadata change).
     */
    public boolean isVariantEvent() {
        return this == DOCUMENT_VARIANT_CREATED || this == DOCUMENT_VARIANT_DELETED
                || this == DOCUMENT_VARIANT_UPDATED || this == LOCK_CHANGE || this == VERSION_UPDATED;
    }

    /**
     * Returns true if this event is related to documents (not document variants).
     */
    public boolean isDocumentEvent() {
        return this == DOCUMENT_CREATED || this == DOCUMENT_UPDATED || this == DOCUMENT_DELETED;
    }
    
    /**
     * Returns true if this event is related to collections (create-delete-update)
     */
    public boolean isCollectionEvent() {
        return this == COLLECTION_CREATED || this == COLLECTION_UPDATED || this == COLLECTION_DELETED;
    }

    public static final RepositoryEventType COLLECTION_CREATED = new RepositoryEventType("CollectionCreated");
    public static final RepositoryEventType COLLECTION_DELETED = new RepositoryEventType("CollectionDeleted");
    public static final RepositoryEventType COLLECTION_UPDATED = new RepositoryEventType("CollectionUpdated");

    public static final RepositoryEventType DOCUMENT_CREATED = new RepositoryEventType("DocumentCreated");
    public static final RepositoryEventType DOCUMENT_UPDATED = new RepositoryEventType("DocumentUpdated");
    public static final RepositoryEventType DOCUMENT_DELETED = new RepositoryEventType("DocumentDeleted");

    public static final RepositoryEventType USER_CREATED = new RepositoryEventType("UserCreated");
    public static final RepositoryEventType USER_UPDATED = new RepositoryEventType("UserUpdated");
    public static final RepositoryEventType USER_DELETED = new RepositoryEventType("UserDeleted");

    public static final RepositoryEventType ROLE_CREATED = new RepositoryEventType("RoleCreated");
    public static final RepositoryEventType ROLE_UPDATED = new RepositoryEventType("RoleUpdated");
    public static final RepositoryEventType ROLE_DELETED = new RepositoryEventType("RoleDeleted");

    public static final RepositoryEventType BRANCH_CREATED = new RepositoryEventType("BranchCreated");
    public static final RepositoryEventType BRANCH_DELETED = new RepositoryEventType("BranchDeleted");
    public static final RepositoryEventType BRANCH_UPDATED = new RepositoryEventType("BranchUpdated");

    public static final RepositoryEventType LANGUAGE_CREATED = new RepositoryEventType("LanguageCreated");
    public static final RepositoryEventType LANGUAGE_DELETED = new RepositoryEventType("LanguageDeleted");
    public static final RepositoryEventType LANGUAGE_UPDATED = new RepositoryEventType("LanguageUpdated");

    public static final RepositoryEventType ACL_UPDATED = new RepositoryEventType("AclUpdated");    

    public static final RepositoryEventType NAMESPACE_REGISTERED = new RepositoryEventType("NamespaceRegistered");
    public static final RepositoryEventType NAMESPACE_UNREGISTERED = new RepositoryEventType("NamespaceUnregistered");
    public static final RepositoryEventType NAMESPACE_MANAGED = new RepositoryEventType("NamespaceManaged");
    public static final RepositoryEventType NAMESPACE_UNMANAGED = new RepositoryEventType("NamespaceUnmanaged");

    public static final RepositoryEventType DOCUMENT_VARIANT_CREATED = new RepositoryEventType("DocumentVariantCreated");
    public static final RepositoryEventType DOCUMENT_VARIANT_DELETED = new RepositoryEventType("DocumentVariantDeleted");
    public static final RepositoryEventType DOCUMENT_VARIANT_UPDATED = new RepositoryEventType("DocumentVariantUpdated");
    public static final RepositoryEventType DOCUMENT_VARIANT_TIMELINE_UPDATED = new RepositoryEventType("DocumentVariantTimelineUpdated");

    public static final RepositoryEventType LOCK_CHANGE = new RepositoryEventType("LockChange");

    public static final RepositoryEventType VERSION_UPDATED = new RepositoryEventType("VersionUpdated");

}
