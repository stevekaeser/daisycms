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

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.outerx.daisy.x10Publisher.ResolveDocumentIdsDocument;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.repository.variant.BranchNotFoundException;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;

import java.util.List;

public class ResolveDocumentIdsRequest extends AbstractRequest implements Request {
    private List<ResolveDocumentIdsDocument.ResolveDocumentIds.Document> documents;
    private long defaultBranchId;
    private long defaultLanguageId;

    public ResolveDocumentIdsRequest(List<ResolveDocumentIdsDocument.ResolveDocumentIds.Document> documents,
            long defaultBranchId, long defaultLanguageId, LocationInfo locationInfo) {
        super(locationInfo);
        this.documents = documents;
        this.defaultBranchId = defaultBranchId;
        this.defaultLanguageId = defaultLanguageId;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        contentHandler.startElement(PublisherImpl.NAMESPACE, "resolvedDocumentIds", "p:resolvedDocumentIds", new AttributesImpl());

        Repository repository = publisherContext.getRepository();
        VariantManager variantManager = publisherContext.getRepository().getVariantManager();
        for (ResolveDocumentIdsDocument.ResolveDocumentIds.Document document : documents) {
            String name = null;
            String id = document.getId();

            long branchId = defaultBranchId;
            long languageId = defaultLanguageId;

            if (document.isSetBranch()) {
                try {
                    branchId = variantManager.getBranch(document.getBranch(), false).getId();
                } catch (BranchNotFoundException e) {
                    name = "(branch does not exist: " + document.getBranch() + ")";
                }
            }

            if (name == null && document.isSetLanguage()) {
                try {
                    languageId = variantManager.getLanguage(document.getLanguage(), false).getId();
                } catch (BranchNotFoundException e) {
                    name = "(language does not exist: " + document.getLanguage() + ")";
                }
            }

            Document daisyDoc = null;
            if (name == null) {
                try {
                    daisyDoc = repository.getDocument(id, branchId, languageId, false);
                } catch (DocumentNotFoundException e) {
                    name = "(document does not exist)";
                } catch (DocumentVariantNotFoundException e) {
                    name = "(document variant does not exist)";
                }
            }

            if (name == null) {
                Version version = null;
                if (document.isSetVersion()) {
                    String versionParam = document.getVersion();
                    if (versionParam.equalsIgnoreCase("last")) {
                        version = daisyDoc.getLastVersion();
                    } else if (versionParam.equalsIgnoreCase("live")) {
                        version = daisyDoc.getLiveVersion();
                    } else {
                        try {
                            long versionId = Long.parseLong(versionParam);
                            if (versionId >= 1 && versionId <= daisyDoc.getLastVersionId()) {
                                version = daisyDoc.getVersion(versionId);
                            }
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
                if (version == null) {
                    version = daisyDoc.getLiveVersion();
                    if (version == null)
                        version = daisyDoc.getLastVersion();
                }
                name = version.getDocumentName();
            }

            AttributesImpl attrs = new AttributesImpl();
            attrs.addAttribute("", "id", "id", "CDATA", document.getId());
            if (document.isSetBranch())
                attrs.addAttribute("", "branch", "branch", "CDATA", document.getBranch());
            if (document.isSetLanguage())
                attrs.addAttribute("", "language", "language", "CDATA", document.getLanguage());
            if (document.isSetVersion())
                attrs.addAttribute("", "version", "version", "CDATA", document.getVersion());
            attrs.addAttribute("", "name", "name", "CDATA", name);
            contentHandler.startElement(PublisherImpl.NAMESPACE, "document", "document", attrs);
            contentHandler.endElement(PublisherImpl.NAMESPACE, "document", "document");
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "resolvedDocumentIds", "p:resolvedDocumentIds");
    }
}
