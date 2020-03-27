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
package org.outerj.daisy.repository.serverimpl.linkextraction;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.serverimpl.LocalRepositoryManager;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.commonimpl.schema.CommonRepositorySchema;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.linkextraction.LinkExtractor;

import java.util.Collection;
import java.io.InputStream;

public class LinkExtractorHelper {
    private final Document document;
    private final Version version;
    private final String documentBranch;
    private final String documentLanguage;
    private final DocId docId;
    private LinkCollectorImpl linkCollector;
    private final CommonRepositorySchema repositorySchema;
    private final CommonRepository repository;
    private final AuthenticatedUser systemUser;
    private final LocalRepositoryManager.Context context;

    /**
     * Constructor.
     *
     * <p>The boolean documentContainsLastVersion indicates whether the data currently in the document object
     * is the latest version. This is needed because the linkextractor is used during the storage of the
     * document, when the new version does not yet really exist (likewise, that's why we need documentId
     * and liveVersion explicitely).
     */
    public LinkExtractorHelper(Document document, DocId docId, Version version,
                               AuthenticatedUser systemUser, LocalRepositoryManager.Context context) {
        this.document = document;
        this.documentBranch = String.valueOf(document.getBranchId());
        this.documentLanguage = String.valueOf(document.getLanguageId());
        this.docId = docId;
        this.version = version;
        this.repository = context.getCommonRepository();
        this.repositorySchema = repository.getRepositorySchema();
        this.systemUser = systemUser;
        this.context = context;
    }

    public Collection<LinkInfo> extract() throws Exception {
        this.linkCollector = new LinkCollectorImpl(docId.getSeqId(), docId.getNsId(), document.getBranchId(),
                document.getLanguageId(), repository, systemUser);

        Field[] fields;
        Part[] parts;
        Link[] links;
        if (version == null) {
            fields = document.getFields().getArray();
            parts = document.getParts().getArray();
            links = document.getLinks().getArray();
        } else {
            fields = version.getFields().getArray();
            parts = version.getParts().getArray();
            links = version.getLinks().getArray();
        }

        extractLinks(fields);
        extractLinks(parts);
        extractLinks(links);

        return linkCollector.getLinks();
    }

    private void extractLinks(Part[] parts) throws Exception {
        for (Part part : parts) {
            this.linkCollector.changeTo(part.getTypeId());
            PartType partType = repositorySchema.getPartTypeById(part.getTypeId(), false, systemUser);

            if (partType.getLinkExtractor() != null) {
                LinkExtractor linkExtractor = context.getLinkExtractor(partType.getLinkExtractor());
                if (linkExtractor != null) {
                    InputStream is = null;
                    try {
                        is = part.getDataStream();
                        linkExtractor.extractLinks(is, linkCollector, documentBranch, documentLanguage);
                    } catch (Throwable e) {
                        String whichDoc = document.getId() != null ? document.getVariantKey().toString() : " (new document) ";
                        context.getLogger().error("Error calling link extractor for " + whichDoc + " part " + partType.getName(), e);
                    } finally {
                        if (is != null)
                            is.close();
                    }
                }
            }
        }
    }

    private void extractLinks(Link[] links) {
        this.linkCollector.changeTo(-1);
        for (Link link : links) {
            linkCollector.addLink(LinkType.OUT_OF_LINE, link.getTarget());
        }
    }

    private void extractLinks(Field[] fields) {
        this.linkCollector.changeTo(-1);
        for (Field field : fields) {
            if (field.getValueType() == ValueType.LINK) {
                Object value = field.getValue();

                Object[] values;
                if (!(value instanceof Object[]))
                    values = new Object[]{value};
                else
                    values = (Object[]) value;

                if (field.isHierarchical()) {
                    for (Object hierarchyValue : values) {
                        extractLinkFieldLinks(((HierarchyPath) hierarchyValue).getElements());
                    }
                } else {
                    extractLinkFieldLinks(values);
                }
            }
        }
    }

    private void extractLinkFieldLinks(Object[] values) {
        for (Object value : values) {
            VariantKey variantKey = (VariantKey)value;
            String targetBranch = variantKey.getBranchId() != -1 ? String.valueOf(variantKey.getBranchId()) : documentBranch;
            String targetLanguage = variantKey.getLanguageId() != -1 ? String.valueOf(variantKey.getLanguageId()) : documentLanguage;
            linkCollector.addLink(LinkType.FIELD, variantKey.getDocumentId(), targetBranch, targetLanguage, -1);
        }
    }
}
