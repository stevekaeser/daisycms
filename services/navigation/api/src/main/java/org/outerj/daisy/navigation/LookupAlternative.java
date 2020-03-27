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
package org.outerj.daisy.navigation;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;

public class LookupAlternative implements Comparable {
    private final String name;
    private final long collectionId;
    private final VariantKey navigationDoc;
    private final VersionMode versionMode;

    /**
     *
     * @param name a name that identifies this lookup alternative
     * @param collectionId the collection this lookup alternative is associated with
     * @param navigationDoc the navigation doc for this lookup alternative
     */
    public LookupAlternative(String name, long collectionId, VariantKey navigationDoc, VersionMode versionMode) {
        if (name == null)
            throw new IllegalArgumentException("name argument cannot be null");
        if (navigationDoc == null)
            throw new IllegalArgumentException("navigationDoc argument cannot be null");

        this.name = name;
        this.collectionId = collectionId;
        this.navigationDoc = navigationDoc;
        this.versionMode = versionMode;
    }

    public LookupAlternative(String name, long collectionId, VariantKey navigationDoc) {
        this(name, collectionId, navigationDoc, VersionMode.LIVE);
    }

    public String getName() {
        return name;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public VariantKey getNavigationDoc() {
        return navigationDoc;
    }

    public VersionMode getVersionMode() {
        return versionMode;
    }

    public int compareTo(Object o) {
        LookupAlternative other = (LookupAlternative)o;

        int nameCompare = name.compareTo(other.name);
        if (nameCompare != 0)
            return nameCompare;

        if (collectionId < other.collectionId)
            return -1;
        else if (collectionId > other.collectionId)
            return 1;

        int navDocCompare = navigationDoc.compareTo(other.navigationDoc);
        if (navDocCompare != 0)
            return navDocCompare;

        if (versionMode.equals(other.versionMode)) {
            return 0;
        } else if (versionMode.isLast()) {
            return 1;
        } else if (versionMode.isLive()) {
            return other.versionMode.isLast() ? -1 : 1;
        } else {
            return other.versionMode.isLive() || other.versionMode.isLast() ? -1 : versionMode.getDate().compareTo(other.versionMode.getDate()); 
        }
    }
}
