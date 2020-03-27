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
 * An immutable object identifying a specific document variant and version.
 */
public class VersionKey implements Comparable {
    private final String documentId;
    private final long branchId;
    private final long languageId;
    private final long versionId;

    public VersionKey(String documentId, long branchId, long languageId, long versionId) {
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
        this.versionId = versionId;
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

    public long getVersionId() {
        return versionId;
    }

    public String getVersion() {
        if (versionId == -1) {
            return "live";
        } else if (versionId == -2) {
            return "last";
        } else {
            return String.valueOf(versionId);
        }
    }

    public int compareTo(Object o) {
        VersionKey otherKey = (VersionKey)o;
        int docIdCompareResult = documentId.compareTo(otherKey.documentId);
        if (docIdCompareResult == 0) {
            if (branchId == otherKey.branchId) {
                if (languageId == otherKey.languageId) {
                    if (versionId == otherKey.versionId) {
                        return 0;
                    } else if (versionId < otherKey.versionId) {
                        return -1;
                    } else {
                        return 1;
                    }
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
        } else {
            return docIdCompareResult;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof VersionKey) {
            VersionKey otherKey = (VersionKey)obj;
            return (this.documentId.equals(otherKey.documentId) && this.branchId == otherKey.branchId
                    && this.languageId == otherKey.languageId && this.versionId == otherKey.versionId);
        }

        return false;
    }

    public int hashCode() {
        // The calculation technique for this hashcode is taken from the HashCodeBuilder
        // of Jakarta Commons Lang, which in itself is based on techniques from the
        // "Effective Java" book by Joshua Bloch.
        final int iConstant = 159;
        int iTotal = 615;

        iTotal = iTotal * iConstant + documentId.hashCode();
        iTotal = appendHash(branchId, iTotal, iConstant);
        iTotal = appendHash(languageId, iTotal, iConstant);
        iTotal = appendHash(versionId, iTotal, iConstant);

        return iTotal;
    }

    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }

    public String toString() {
        return "{document ID "  + documentId + ", branch ID " + branchId + ", language ID " + languageId + ", version ID " + versionId + "}";
    }
}
