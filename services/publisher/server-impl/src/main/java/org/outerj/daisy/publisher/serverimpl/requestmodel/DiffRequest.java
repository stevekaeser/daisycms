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
import org.outerj.daisy.docdiff.DocDiffOutputHelper;
import org.outerj.daisy.docdiff.DiffGenerator;
import org.outerj.daisy.publisher.serverimpl.XmlDocDiffOutput;
import org.outerj.daisy.publisher.PublisherException;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.Repository;

public class DiffRequest extends AbstractRequest {
    private final PubReqExpr otherDocumentIdExpr;
    private final PubReqExpr otherBranchExpr;
    private final PubReqExpr otherLanguageExpr;
    private final PubReqExpr otherVersionExpr;
    private final ContentDiffType diffType;

    public DiffRequest(PubReqExpr otherDocumentIdExpr, PubReqExpr otherBranchExpr, PubReqExpr otherLanguageExpr,
            PubReqExpr otherVersionExpr, ContentDiffType diffType, LocationInfo locationInfo) {
        super(locationInfo);
        this.otherDocumentIdExpr = otherDocumentIdExpr;
        this.otherBranchExpr = otherBranchExpr;
        this.otherLanguageExpr = otherLanguageExpr;
        this.otherVersionExpr = otherVersionExpr;
        this.diffType = diffType;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();

        Document document1 = publisherContext.getDocument();
        Version version1 = publisherContext.getVersion();

        if (version1 == null) // diff with live version but live version doesn't exist
            return;

        String otherDocumentId = otherDocumentIdExpr == null ? publisherContext.getDocumentId() : otherDocumentIdExpr.evalAsString(publisherContext, publisherContext.getDocumentId(), this);
        long otherBranchId = otherBranchExpr == null ? publisherContext.getBranchId() : otherBranchExpr.evalAsBranchId(publisherContext, this);
        long otherLanguageId = otherLanguageExpr == null ? publisherContext.getLanguageId() : otherLanguageExpr.evalAsLanguageId(publisherContext, this);
        String otherVersionIdSpec = otherVersionExpr == null ? String.valueOf(version1.getId() - 1) : otherVersionExpr.evalAsString(publisherContext, String.valueOf(version1.getId() - 1), this);

        Document document2 = repository.getDocument(otherDocumentId, otherBranchId, otherLanguageId, false);

        long otherVersionId;
        if (otherVersionIdSpec.equalsIgnoreCase("live")) {
            otherVersionId = document2.getLiveVersionId();
        } else if (otherVersionIdSpec.equalsIgnoreCase("last")) {
            otherVersionId = document2.getLastVersionId();
        } else {
            try {
                otherVersionId = Long.parseLong(otherVersionIdSpec);
            } catch (NumberFormatException e) {
                throw new PublisherException("Invalid version specification: " + otherVersionIdSpec);
            }
        }

        if (otherVersionId < 1)
            return;

        Version version2 = document2.getVersion(otherVersionId);

        DocDiffOutputHelper diffOutputHelper = new DocDiffOutputHelper(document1, document2, version1, version2, repository, publisherContext.getLocale());
        DiffGenerator.generateDiff(version1, version2, new XmlDocDiffOutput(contentHandler, diffOutputHelper, diffType, repository));
    }
}
