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
package org.outerj.daisy.tools.importexport.export;

import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpField;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;
import org.outerj.daisy.tools.importexport.model.document.ImpExpLink;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.linkextraction.*;
import org.outerj.daisy.linkextraction.impl.*;
import org.outerj.daisy.util.Constants;

import java.util.*;
import java.util.regex.Matcher;
import java.io.InputStream;

class LinkHandler {
    private Exporter exporter;
    private Repository repository;
    private ExportListener listener;
    private ExportOptions options;

    private static Map<String, LinkExtractor> LINK_EXTRACTORS = new HashMap<String, LinkExtractor>();
    static {
        LINK_EXTRACTORS.put("NavigationDescription", new NavigationLinkExtractor());
        LINK_EXTRACTORS.put("BookMetadata", new PropertiesLinkExtractor());
        LINK_EXTRACTORS.put("BookDefinitionDescription", new BookLinkExtractor());
        LINK_EXTRACTORS.put("BookPublicationsDefault", new BookPublicationsLinkExtractor());
    }
    private static DaisyHtmlLinkExtractor DAISY_HTML_LINKEXTRACTOR = new DaisyHtmlLinkExtractor();

    public LinkHandler(Exporter exporter, ExportListener listener, ExportOptions options, Repository repository) {
        this.exporter = exporter;
        this.repository = repository;
        this.listener = listener;
        this.options = options;
    }

    void handleLinks(ImpExpDocument impExpDoc) throws Exception {
        handleLinkFieldLinks(impExpDoc);
        if (options.getLinkExtractionEnabled()) {
            ExportLinkCollector linkCollector = new ExportLinkCollector(impExpDoc.getVariantKey());
            handleLinksInParts(impExpDoc, linkCollector);
            handleOutOfLineLinks(impExpDoc, linkCollector);
        }
    }

    private void handleLinkFieldLinks(ImpExpDocument impExpDoc) throws Exception {
        // Get links in the fields
        ImpExpField[] fields = impExpDoc.getFields();
        for (ImpExpField field : fields) {
            FieldType fieldType = field.getType();
            if (fieldType.getValueType() == ValueType.LINK) {
                Object value = field.getValue();
                if (fieldType.isMultiValue()) {
                    Object[] values = (Object[])value;
                    for (Object subValue : values) {
                        if (fieldType.isHierarchical()) {
                            for (Object element : ((HierarchyPath)subValue).getElements()) {
                                VariantKey variantKey = (VariantKey)element;
                                addLinkFieldLink(impExpDoc, variantKey);
                            }
                        } else {
                            VariantKey variantKey = (VariantKey)subValue;
                            addLinkFieldLink(impExpDoc, variantKey);
                        }
                    }
                } else {
                    if (fieldType.isHierarchical()) {
                        for (Object element : ((HierarchyPath)value).getElements()) {
                            VariantKey variantKey = (VariantKey)element;
                            addLinkFieldLink(impExpDoc, variantKey);
                        }
                    } else {
                        VariantKey variantKey = (VariantKey)value;
                        addLinkFieldLink(impExpDoc, variantKey);
                    }
                }
            }
        }
    }

    private void addLinkFieldLink(ImpExpDocument sourceDoc, VariantKey variantKey) throws RepositoryException {
        String documentId = repository.normalizeDocumentId(variantKey.getDocumentId());
        exporter.needNamespaceOfDocId(documentId, true);
        exporter.needsBranchLanguage(variantKey.getBranchId(), variantKey.getLanguageId(), true);

        String branch = variantKey.getBranchId() == -1 ? sourceDoc.getBranch() : repository.getVariantManager().getBranch(variantKey.getBranchId(), false).getName();
        String language = variantKey.getLanguageId() == -1 ? sourceDoc.getLanguage() : repository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName();

        listener.hasLink(sourceDoc.getVariantKey(), new ImpExpVariantKey(documentId, branch, language), LinkType.FIELD);
    }

    private void handleLinksInParts(ImpExpDocument impExpDoc, LinkCollector linkCollector) throws Exception {
        ImpExpPart[] parts = impExpDoc.getParts();
        for (ImpExpPart part : parts) {
            LinkExtractor linkExtractor;
            if (part.getType().isDaisyHtml()) {
                linkExtractor = DAISY_HTML_LINKEXTRACTOR;
            } else {
                linkExtractor = LINK_EXTRACTORS.get(part.getType().getName());
            }
            if (linkExtractor != null) {
                InputStream is = null;
                try {
                    is = part.getDataAccess().getInputStream();
                    linkExtractor.extractLinks(is, linkCollector, impExpDoc.getBranch(), impExpDoc.getLanguage());
                } finally {
                    if (is != null)
                        is.close();
                }
            }
        }
    }

    private void handleOutOfLineLinks(ImpExpDocument impExpDoc, LinkCollector linkCollector) {
        ImpExpLink[] links = impExpDoc.getLinks();
        for (ImpExpLink link : links) {
            linkCollector.addLink(LinkType.OUT_OF_LINE, link.getTarget());
        }
    }

    class ExportLinkCollector implements LinkCollector {
        private ImpExpVariantKey sourceVariantKey;

        public ExportLinkCollector(ImpExpVariantKey sourceVariantKey) {
            this.sourceVariantKey = sourceVariantKey;
        }

        public void addLink(LinkType linkType, String targetDocId, String targetBranch, String targetLanguage, long version) {
            try {
                String normalizedDocId = repository.normalizeDocumentId(targetDocId);
                String branch = repository.getVariantManager().getBranch(targetBranch, false).getName();
                String language = repository.getVariantManager().getLanguage(targetLanguage, false).getName();
                addLink(new ImpExpVariantKey(normalizedDocId, branch, language), linkType);
            } catch (RepositoryException e) {
                // invalid branch or language, ignore link
            } catch (InvalidDocumentIdException e) {
                // invalid document ID, ignore link
            }
        }

        public void addLink(LinkType linkType, String daisyLink) {
            if (daisyLink == null)
                return;
            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(daisyLink);
            if (matcher.matches()) {
                try {
                    String targetDocumentId = repository.normalizeDocumentId(matcher.group(1));

                    String branchString = matcher.group(2);
                    String languageString = matcher.group(3);

                    String targetBranch;
                    String targetLanguage;

                    if (branchString == null || branchString.length() == 0) {
                        targetBranch = sourceVariantKey.getBranch();
                    } else {
                        targetBranch = repository.getVariantManager().getBranch(branchString, false).getName();
                    }

                    if (languageString == null || languageString.length() == 0) {
                        targetLanguage = sourceVariantKey.getLanguage();
                    } else {
                        targetLanguage = repository.getVariantManager().getLanguage(languageString, false).getName();
                    }

                    addLink(new ImpExpVariantKey(targetDocumentId, targetBranch, targetLanguage), linkType);
                } catch (RepositoryException e) {
                    // invalid branch or language, ignore link
                } catch (InvalidDocumentIdException e) {
                    // invalid document ID, ignore link
                }
            }
        }

        private void addLink(ImpExpVariantKey variantKey, LinkType linkType) {
            try {
                exporter.needNamespaceOfDocId(variantKey.getDocumentId(), false);
                exporter.needsBranchLanguage(variantKey.getBranch(), variantKey.getLanguage(), false);
                listener.hasLink(sourceVariantKey, variantKey, linkType);
            } catch (NamespaceNotFoundException e) {
                throw new RuntimeException("Error with extracted link.", e);
            } catch (RepositoryException e) {
                throw new RuntimeException("Error with extracted link.", e);
            }
        }
    }
}
