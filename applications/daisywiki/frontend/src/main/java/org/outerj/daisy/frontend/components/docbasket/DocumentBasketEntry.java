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
package org.outerj.daisy.frontend.components.docbasket;

import org.apache.excalibur.xml.sax.XMLizable;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An entry in a document basket.
 *
 * <p>The specific document variant version identified by document basket entry
 * is immutable, other properties (document name) can be modified.
 *
 * <p>Two entries are equal if they are about the same document variant version.
 */
public class DocumentBasketEntry implements XMLizable {
    private final String documentId;
    private final String branch;
    private final String language;
    private final long versionId;
    private String documentName;
    private int hashCode;

    public DocumentBasketEntry(String documentId, String branch, String language, long versionId, String documentName) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.versionId = versionId;
        this.documentName = documentName;
        initHashCode();
    }

    private void initHashCode() {
        HashCodeBuilder builder = new HashCodeBuilder();
        builder.append(this.documentId);
        builder.append(branch);
        builder.append(language);
        builder.append(versionId);
        this.hashCode = builder.toHashCode();
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

    public long getVersionId() {
        return versionId;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public void toSAX(ContentHandler contentHandler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addCDATAAttribute("id", String.valueOf(documentId));
        attrs.addCDATAAttribute("branch", branch);
        attrs.addCDATAAttribute("language", language);
        attrs.addCDATAAttribute("versionId", versionId == -1 ? "" : String.valueOf(versionId));
        attrs.addCDATAAttribute("name", documentName);
        contentHandler.startElement("", "entry", "entry", attrs);
        contentHandler.endElement("", "entry", "entry");
    }

    public boolean equals(Object obj) {
        DocumentBasketEntry other = (DocumentBasketEntry)obj;
        return other.documentId.equals(this.documentId)
                && other.branch.equals(this.branch)
                && other.language.equals(this.language)
                && other.versionId == this.versionId;
    }

    public int hashCode() {
        return hashCode;
    }
}
