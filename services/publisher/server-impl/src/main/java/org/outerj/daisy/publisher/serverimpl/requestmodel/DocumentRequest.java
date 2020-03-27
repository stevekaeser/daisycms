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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

public class DocumentRequest extends AbstractParentPublisherRequest implements Request {
    private final boolean documentInformationSet;
    private final String documentId;
    private final String branch;
    private final String language;
    private final String version;

    public DocumentRequest(LocationInfo locationInfo) {
        super(locationInfo);
        this.documentId = null;
        this.branch = null;
        this.language = null;
        this.version = null;
        this.documentInformationSet = false;
        this.locationInfo = locationInfo;
    }

    public DocumentRequest(String documentId, String branch, String language, String version, LocationInfo locationInfo) {
        super(locationInfo);
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.version = version;
        this.documentInformationSet = true;
        this.locationInfo = locationInfo;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
        if (documentInformationSet) {
            Repository repository = publisherContext.getRepository();
            String documentId = repository.normalizeDocumentId(this.documentId);
            VariantManager variantManager= repository.getVariantManager();
            long branchId = branch != null ? variantManager.getBranch(branch, false).getId() : Branch.MAIN_BRANCH_ID;
            long languageId = language != null ? variantManager.getLanguage(language, false).getId() : Language.DEFAULT_LANGUAGE_ID;
            long versionId = -1; // default to live version (for backwards compatibility from before '-3' was introduced)
            if (version != null) {
                if (version.equals("default") || version.equalsIgnoreCase("")) {
                    versionId = -3;
                } else if (version.equalsIgnoreCase("live")) {
                    versionId = -1;
                } else if (version.equalsIgnoreCase("last")) {
                    versionId = -2;
                } else {
                    try {
                        versionId = Long.parseLong(version);
                    } catch (NumberFormatException e) {
                        versionId = repository.getDocument(documentId, branchId, languageId, false).getVersionId(VersionMode.get(version));
                    }
                }
            }

            childPublisherContext.setDocumentVariant(documentId, branchId, languageId);
            childPublisherContext.setVersionId(versionId);
            childPublisherContext.setVersionMode(publisherContext.getVersionMode());
        }
        emitDocument(contentHandler, childPublisherContext);
    }
    

    private void emitDocument(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();

        String documentId = publisherContext.getDocumentId();
        long branchId = publisherContext.getBranchId();
        long languageId = publisherContext.getLanguageId();

        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "documentId", "documentId", "CDATA", documentId);
        attrs.addAttribute("", "branchId", "branchId", "CDATA", String.valueOf(branchId));
        attrs.addAttribute("", "branch", "branch", "CDATA", repository.getVariantManager().getBranch(branchId, false).getName());
        attrs.addAttribute("", "languageId", "languageId", "CDATA", String.valueOf(languageId));
        attrs.addAttribute("", "language", "language", "CDATA", repository.getVariantManager().getLanguage(languageId, false).getName());
        contentHandler.startElement(PublisherImpl.NAMESPACE, "document", "p:document", attrs);

        super.processInt(contentHandler, publisherContext);

        contentHandler.endElement(PublisherImpl.NAMESPACE, "document", "p:document");
    }
}
