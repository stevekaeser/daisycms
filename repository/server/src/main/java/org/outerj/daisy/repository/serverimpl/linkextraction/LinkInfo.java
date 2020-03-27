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

import org.outerj.daisy.linkextraction.LinkType;

public class LinkInfo {
    public final long sourceDocSeqId;
    public final long sourceDocNsId;
    public final long sourceBranchId;
    public final long sourceLanguageId;
    public final long sourcePartTypeId;
    public boolean occursInLastVersion;
    public boolean occursInLiveVersion;
    public final long targetDocSeqId;
    public final long targetDocNsId;
    public final long targetBranchId;
    public final long targetLanguageId;
    public final LinkType linkType;
    public final long targetVersionId;

    public LinkInfo(long sourceDocSeqId, long sourceDocNsId, long sourceBranchId, long sourceLanguageId,
            long sourcePartTypeId, boolean occursInLastVersion, boolean occursInLiveVersion, long targetDocSeqId,
            long targetDocNsId, long targetBranchId, long targetLanguageId, long versionId, LinkType linkType) {
        this.sourceDocSeqId = sourceDocSeqId;
        this.sourceDocNsId = sourceDocNsId;
        this.sourceBranchId = sourceBranchId;
        this.sourceLanguageId = sourceLanguageId;
        this.sourcePartTypeId = sourcePartTypeId;
        this.occursInLastVersion = occursInLastVersion;
        this.occursInLiveVersion = occursInLiveVersion;
        this.targetDocSeqId = targetDocSeqId;
        this.targetDocNsId = targetDocNsId;
        this.targetBranchId = targetBranchId;
        this.targetLanguageId = targetLanguageId;
        this.targetVersionId = versionId;
        this.linkType = linkType;
    }

    public String getKey() {
        return sourceDocSeqId + "," + sourceDocNsId + "," + sourceBranchId + "," + sourceLanguageId + "," + sourcePartTypeId + "," + targetDocSeqId + "," + targetDocNsId + "," + targetBranchId + "," + targetLanguageId + "," + targetVersionId + "," + linkType;
    }

    public void merge(LinkInfo linkInfo) {
        if (sourceDocSeqId != linkInfo.sourceDocSeqId
                || sourceDocNsId != linkInfo.sourceDocNsId
                || sourceBranchId != linkInfo.sourceBranchId
                || sourceLanguageId != linkInfo.sourceLanguageId
                || sourcePartTypeId != linkInfo.sourcePartTypeId
                || targetDocSeqId != linkInfo.targetDocSeqId
                || targetDocNsId != linkInfo.targetDocNsId
                || linkType != linkInfo.linkType
                || targetVersionId != linkInfo.targetVersionId
                || targetBranchId != linkInfo.targetBranchId
                || targetLanguageId != linkInfo.targetLanguageId) {
            throw new RuntimeException("Cannot merge");
        }

        occursInLastVersion = occursInLastVersion | linkInfo.occursInLastVersion;
        occursInLiveVersion = occursInLiveVersion | linkInfo.occursInLiveVersion;
    }
}
