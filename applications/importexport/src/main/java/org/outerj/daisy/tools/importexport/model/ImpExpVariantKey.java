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
package org.outerj.daisy.tools.importexport.model;

public class ImpExpVariantKey {
    private final String documentId;
    private final String branch;
    private final String language;

    public ImpExpVariantKey(String documentId, String branch, String language) {
        if (documentId == null)
            throw new IllegalArgumentException("documentId can not be null");
        if (branch == null)
            throw new IllegalArgumentException("Null argument: branch");
        if (language == null)
            throw new IllegalArgumentException("Null argument: language");
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getBranch() {
        return branch;
    }

    public String getLanguage() {
        return language;
    }

    public int compareTo(Object o) {
        ImpExpVariantKey otherKey = (ImpExpVariantKey)o;
        int docCompareResult = documentId.compareTo(otherKey.documentId);
        if (docCompareResult == 0) {
            int branchCompareResult = branch.compareTo(otherKey.branch);
            if (branchCompareResult == 0) {
                return language.compareTo(otherKey.language);
            } else {
                return branchCompareResult;
            }
        } else {
            return docCompareResult;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof ImpExpVariantKey) {
            ImpExpVariantKey otherKey = (ImpExpVariantKey)obj;
            return this.documentId.equals(otherKey.documentId)
                    && this.branch.equals(otherKey.branch)
                    && this.language.equals(otherKey.language);
        }

        return false;
    }

    public int hashCode() {
        return (documentId + branch + language).hashCode();
    }

    public String toString() {
        return documentId + "~" + branch + "~" + language;
    }
}
