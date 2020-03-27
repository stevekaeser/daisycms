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

import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.publisher.serverimpl.variables.NavTreeVarResolverHandler;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.xml.sax.ContentHandler;

public class NavigationTreeRequest extends AbstractRequest implements Request {
    private final PubReqExpr navDocIdExpr;
    private final PubReqExpr navDocBranchExpr;
    private final PubReqExpr navDocLanguageExpr;
    private final PubReqExpr activeDocIdExpr;
    private final PubReqExpr activeDocBranchExpr;
    private final PubReqExpr activeDocLanguageExpr;
    private final PubReqExpr activePathExpr;
    private final VersionMode versionMode;
    private final boolean contextualized;
    private final int depth;
    private PubReqExpr addChildCountsExpr;
    private final DocumentRequest documentRequest;

    /**
     * @param versionMode optional, can be null (defaults to publisher context version mode)
     */
    public NavigationTreeRequest(PubReqExpr navDocIdExpr,
            PubReqExpr navDocBranchExpr,
            PubReqExpr navDocLanguageExpr,
            PubReqExpr activeDocIdExpr,
            PubReqExpr activeDocBranchExpr,
            PubReqExpr activeDocLanguageExpr,
            PubReqExpr activePathExpr,
            boolean contextualized,
            int depth,
            VersionMode versionMode,
            PubReqExpr addChildCountsExpr,
            DocumentRequest documentRequest,
            LocationInfo locationInfo) {
        super(locationInfo);
        this.navDocIdExpr = navDocIdExpr;
        this.navDocBranchExpr = navDocBranchExpr;
        this.navDocLanguageExpr = navDocLanguageExpr;
        this.activeDocIdExpr = activeDocIdExpr;
        this.activeDocBranchExpr = activeDocBranchExpr;
        this.activeDocLanguageExpr = activeDocLanguageExpr;
        this.activePathExpr = activePathExpr;
        this.contextualized = contextualized;
        this.depth = depth;
        this.versionMode = versionMode;
        this.addChildCountsExpr = addChildCountsExpr;
        this.documentRequest = documentRequest;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        VersionMode navVersionMode = this.versionMode;
        if (navVersionMode == null)
            navVersionMode = publisherContext.getVersionMode();

        String navDocId = navDocIdExpr.evalAsString(publisherContext, this);
        long navDocBranch = navDocBranchExpr.evalAsBranchId(publisherContext, this);
        long navDocLanguage = navDocLanguageExpr.evalAsLanguageId(publisherContext, this);
        VariantKey navigationDoc = new VariantKey(navDocId, navDocBranch, navDocLanguage);

        VariantKey activeDocument = null;
        if (activeDocIdExpr != null) {
            String activeDocId = activeDocIdExpr.evalAsString(publisherContext, this);
            long activeDocBranch = activeDocBranchExpr.evalAsBranchId(publisherContext, this);
            long activeDocLanguage = activeDocLanguageExpr.evalAsLanguageId(publisherContext, this);
            activeDocument = new VariantKey(activeDocId, activeDocBranch, activeDocLanguage);
        }

        String activePath = activePathExpr.evalAsString(publisherContext, null, this);
        boolean addChildCounts = addChildCountsExpr.evalAsBoolean(publisherContext, false, this);

        NavigationParams navigationParams = new NavigationParams(navigationDoc, navVersionMode, activePath, contextualized, depth, addChildCounts, publisherContext.getLocale());
        NavigationManager navigationManager = (NavigationManager)publisherContext.getRepository().getExtension("NavigationManager");

        Variables variables = publisherContext.getVariables();
        if (variables != null) {
            contentHandler = new NavTreeVarResolverHandler(contentHandler, variables);
        }

        if (documentRequest != null) {
            contentHandler = new AnnotateDocumentHandler(documentRequest, "doc", "http://outerx.org/daisy/1.0#navigation", contentHandler, publisherContext);
        }

        navigationManager.generateNavigationTree(new StripDocumentHandler(contentHandler), navigationParams,
                activeDocument, true, true);
    }
}
