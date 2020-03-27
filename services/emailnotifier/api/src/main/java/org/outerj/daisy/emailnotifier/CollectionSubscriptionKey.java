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
package org.outerj.daisy.emailnotifier;

/**
 * Identifies a subscription for document in a certain collection
 * belonging to a certain branch/language.
 */
public class CollectionSubscriptionKey implements Comparable {
    private final long collectionId;
    private final long branchId;
    private final long languageId;

    public CollectionSubscriptionKey(long collectionId, long branchId, long languageId) {
        this.collectionId = collectionId;
        this.branchId = branchId;
        this.languageId = languageId;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }

    public int hashCode() {
        // The calculation technique for this hashcode is taken from the HashCodeBuilder
        // of Jakarta Commons Lang, which in itself is based on techniques from the
        // "Effective Java" book by Joshua Bloch.
        final int iConstant = 159;
        int iTotal = 615;

        iTotal = appendHash(collectionId, iTotal, iConstant);
        iTotal = appendHash(branchId, iTotal, iConstant);
        iTotal = appendHash(languageId, iTotal, iConstant);

        return iTotal;
    }

    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof CollectionSubscriptionKey) {
            CollectionSubscriptionKey otherKey = (CollectionSubscriptionKey)obj;
            return (this.collectionId == otherKey.collectionId && this.branchId == otherKey.branchId
                    && this.languageId == otherKey.languageId);
        }

        return false;
    }

    public int compareTo(Object o) {
        CollectionSubscriptionKey otherKey = (CollectionSubscriptionKey)o;
        if (collectionId == otherKey.collectionId) {
            if (branchId == otherKey.branchId) {
                if (languageId == otherKey.languageId) {
                    return 0;
                } else if (languageId < otherKey.languageId) {
                    return -1;
                } else {
                    return 1;
                }
            } else if (branchId < otherKey.branchId) {
                return -1;
            } else {
                return 1;
            }
        } else if (collectionId < otherKey.collectionId) {
            return -1;
        } else {
            return 1;
        }
    }
}
