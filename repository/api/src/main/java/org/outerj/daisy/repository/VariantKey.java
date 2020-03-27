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

import org.outerx.daisy.x10.VariantKeyDocument;

/**
 * An immutable object identifying a specific document variant.
 */
public final class VariantKey implements Comparable {
    private final String documentId;
    private final long branchId;
    private final long languageId;

    public VariantKey(String documentId, long branchId, long languageId) {
        if (documentId == null)
            throw new IllegalArgumentException("documentId can not be null");
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
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

    public VariantKeyDocument.VariantKey getXml() {
        VariantKeyDocument.VariantKey variantKeyXml = VariantKeyDocument.VariantKey.Factory.newInstance();
        variantKeyXml.setDocumentId(documentId);
        variantKeyXml.setBranchId(branchId);
        variantKeyXml.setLanguageId(languageId);
        return variantKeyXml;
    }

    public int compareTo(Object o) {
        VariantKey otherKey = (VariantKey)o;
        int docCompareResult = documentId.compareTo(otherKey.documentId);
        if (docCompareResult == 0) {
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
        } else {
            return docCompareResult;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof VariantKey) {
            VariantKey otherKey = (VariantKey)obj;
            return (this.documentId.equals(otherKey.documentId) && this.branchId == otherKey.branchId
                    && this.languageId == otherKey.languageId);
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

        return iTotal;
    }

    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }

    public String toString() {
        return " {document ID "  + documentId + ", branch ID " + branchId + ", language ID " + languageId + "}";
    }
}
