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

import org.outerj.daisy.repository.commonimpl.variant.CommonVariantManager;
import org.outerj.daisy.repository.commonimpl.AuthenticatedUser;
import org.outerj.daisy.repository.commonimpl.CommonRepository;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.linkextraction.LinkCollector;
import org.outerj.daisy.linkextraction.LinkType;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.util.Constants;

import java.util.HashMap;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;

public class LinkCollectorImpl implements LinkCollector {
    private Map<String, LinkInfo> linksByKey = new HashMap<String, LinkInfo>();
    private final long sourceDocSeqId;
    private final long sourceDocNsId;
    private final long sourceBranchId;
    private final long sourceLanguageId;
    private long sourcePartId;
    private boolean inLastVersion;
    private boolean inLiveVersion;
    private final CommonRepository repository;
    private final CommonVariantManager variantManager;
    private final AuthenticatedUser systemUser;

    protected LinkCollectorImpl(long sourceDocSeqId, long sourceDocNsId, long sourceBranchId, long sourceLanguageId,
            CommonRepository repository, AuthenticatedUser systemUser) {
        this.sourceDocSeqId = sourceDocSeqId;
        this.sourceDocNsId = sourceDocNsId;
        this.sourceBranchId = sourceBranchId;
        this.sourceLanguageId = sourceLanguageId;
        this.repository = repository;
        this.variantManager = repository.getVariantManager();
        this.systemUser = systemUser;
    }

    protected void changeTo(long sourcePartId) {
        this.sourcePartId = sourcePartId;
        this.inLastVersion = inLastVersion;
        this.inLiveVersion = inLiveVersion;
    }

    public void addLink(LinkType linkType, String targetDocId, String targetBranch, String targetLanguage, long version) {
        try {
            long[] docId = parseDocumentId(targetDocId);
            if (docId == null)
                return;

            long targetDocSeqId = docId[0];
            long targetDocNsId = docId[1];

            long targetBranchId = getBranchId(targetBranch);
            long targetLanguageId = getLanguageId(targetLanguage);

            LinkInfo linkInfo = new LinkInfo(sourceDocSeqId, sourceDocNsId, sourceBranchId, sourceLanguageId, sourcePartId, inLastVersion, inLiveVersion, targetDocSeqId, targetDocNsId, targetBranchId, targetLanguageId, version, linkType);
            addLink(linkInfo);
        } catch (RepositoryException e) {
            // an invalid branch or language, skip link
        }
    }

    private void addLink(LinkInfo linkInfo) {
        String key = linkInfo.getKey();
        LinkInfo existingLinkInfo = linksByKey.get(key);
        if (existingLinkInfo != null)
            existingLinkInfo.merge(linkInfo);
        else
            linksByKey.put(key, linkInfo);
    }

    public void addLink(LinkType linkType, String daisyLink) {
        long[] target = getDaisyLink(daisyLink);
        if (target != null)
            addLink(new LinkInfo(sourceDocSeqId, sourceDocNsId, sourceBranchId, sourceLanguageId, sourcePartId,
                    inLastVersion, inLiveVersion,
                    target[0], target[1], target[2], target[3], target[4], linkType));
    }

    private long getBranchId(String branchNameOrId) throws RepositoryException {
        return variantManager.getBranch(branchNameOrId, false, systemUser).getId();
    }

    private long getLanguageId(String languageNameOrId) throws RepositoryException {
        return variantManager.getLanguage(languageNameOrId, false, systemUser).getId();
    }

    private long[] parseDocumentId(String documentId) {
        try {
            DocId docId = DocId.parseDocId(documentId, repository);
            return new long[] {docId.getSeqId(), docId.getNsId()};
        } catch (InvalidDocumentIdException e) {
            return null;
        }
    }

    public Collection<LinkInfo> getLinks() {
        return linksByKey.values();
    }

    protected long[] getDaisyLink(String link) {
        if (link != null) {
            Matcher matcher = Constants.DAISY_LINK_PATTERN.matcher(link);
            if (matcher.matches()) {
                long[] targetDocumentId = parseDocumentId(matcher.group(1));
                if (targetDocumentId == null)
                    return null;

                String branchString = matcher.group(2);
                String languageString = matcher.group(3);
                String versionString = matcher.group(4);

                long targetBranchId;
                long targetLanguageId;
                long targetVersionId = -1; // -1 == live version

                if (branchString == null || branchString.length() == 0) {
                    targetBranchId = sourceBranchId;
                } else {
                    try {
                        targetBranchId = variantManager.getBranch(branchString, false, systemUser).getId();
                    } catch (RepositoryException e) {
                        // skip this link
                        return null;
                    }
                }

                if (languageString == null || languageString.length() == 0) {
                    targetLanguageId = sourceLanguageId;
                } else {
                    try {
                        targetLanguageId = variantManager.getLanguage(languageString, false, systemUser).getId();
                    } catch (RepositoryException e) {
                        // skip this link
                        return null;
                    }
                }

                /*
                 *  NOTE: we currently don't use targetVersionId. The stored data (extracted_links.target_version_id)
                 *  is never consulted as we don't need it for the current (daisy 2.4) query language features ([Variant]Links{To,From}).
                 */
                if (versionString != null) {
                    if (versionString.equalsIgnoreCase("LAST")) {
                        targetVersionId = -2; // -2 == last version
                    } else if (versionString.equalsIgnoreCase("LIVE")) {
                        targetVersionId = -1; // -1 == live version (by default, doesn't really a purpose to specify that in the link)
                    } else {
                        try {
                            targetVersionId = Long.parseLong(versionString);
                        } catch (NumberFormatException nfe) {
                            // versionString could be in date format.
                        }
                    }
                }
                return new long[] {targetDocumentId[0], targetDocumentId[1], targetBranchId, targetLanguageId, targetVersionId};
            }
        }
        return null;
    }
}

