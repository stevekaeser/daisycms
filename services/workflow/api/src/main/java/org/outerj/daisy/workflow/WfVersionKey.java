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
package org.outerj.daisy.workflow;

import org.outerj.daisy.util.ObjectUtils;
import org.outerj.daisy.repository.*;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * A pointer to a specific version of a document variant.
 *
 * <p>This object is immutable.
 */
public class WfVersionKey implements Comparable {
    private final String documentId;
    private final long branchId;
    private final long languageId;
    private final String version;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^([0-9]*)|(last)|(LAST)|(live)|(LIVE)$");

    /**
     *
     * @param version a version number, or the strings live/LIVE or last/LAST
     */
    public WfVersionKey(String documentId, long branchId, long languageId, String version) {
        if (documentId == null)
            throw new IllegalArgumentException("documentId can not be null");
        this.documentId = documentId;
        this.branchId = branchId;
        this.languageId = languageId;
        if (version != null) {
            Matcher matcher = VERSION_PATTERN.matcher(version);
            if (!matcher.matches())
                throw new IllegalArgumentException("Invalid version: \"" + version + "\".");
            // normalize version representation
            if (version.equalsIgnoreCase("last")) {
                version = "last";
            } else if (version.equalsIgnoreCase("live")) {
                version = "live";
            } else {
                version = String.valueOf(Long.parseLong(version));
            }
        }
        this.version = version;
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

    /**
     * Version (number or live/last), can be null.
     */
    public String getVersion() {
        return version;
    }

    public int compareTo(Object o) {
        WfVersionKey otherKey = (WfVersionKey)o;
        int docCompareResult = documentId.compareTo(otherKey.documentId);
        if (docCompareResult == 0) {
            if (branchId == otherKey.branchId) {
                if (languageId == otherKey.languageId) {
                    if (ObjectUtils.safeEquals(version, otherKey.version)) {
                        return 0;
                    } else if (version == null) {
                        return -1;
                    } else if (otherKey.version == null) {
                        return 1;
                    } else {
                        return version.compareTo(otherKey.version);
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
            return docCompareResult;
        }
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj instanceof WfVersionKey) {
            WfVersionKey otherKey = (WfVersionKey)obj;
            return (this.documentId.equals(otherKey.documentId) && this.branchId == otherKey.branchId
                    && this.languageId == otherKey.languageId && ObjectUtils.safeEquals(this.version, otherKey.version));
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
        if (version != null)
            iTotal = iTotal * iConstant + version.hashCode();
        else
            iTotal = iTotal * iConstant;

        return iTotal;
    }

    private int appendHash(long value, int iTotal, int iConstant) {
        return iTotal * iConstant + ((int) (value ^ (value >> 32)));
    }

    public String toString() {
        return " {document ID "  + documentId + ", branch ID " + branchId + ", language ID " + languageId + ", version " + version + "}";
    }

    /**
     * Gets a VariantKey equivalent to this WfVersionKey (missing the version
     * information of course).
     */
    public VariantKey getVariantKey() {
        return new VariantKey(documentId, branchId, languageId);
    }

    /**
     * Retrieve the document version pointed to by this key from the given repository.
     * If the version component of this WfVersionKey is null, the last version will
     * be returned.
     */
    public Version getVersion(Repository repository) throws RepositoryException {
        Document document = repository.getDocument(documentId, branchId, languageId, true);
        if (version == null || version.equalsIgnoreCase("last")) {
            return document.getLastVersion();
        } else if (version.equalsIgnoreCase("live")) {
            Version version = document.getLiveVersion();
            if (version == null)
                throw new RepositoryException("Document does not have a live version.");
            return version;
        } else {
            return document.getVersion(Long.parseLong(version));
        }
    }

    /**
     * Convenience method to construct a WfVersionKey object from the given document and version string.
     */
    public static WfVersionKey get(Document document, String version) {
        return new WfVersionKey(document.getId(), document.getBranchId(), document.getLanguageId(), version);
    }
}
