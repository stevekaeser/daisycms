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
package org.outerj.daisy.navigation.impl.httphandlers;

import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.xml.sax.ContentHandler;

public class NavigationTreeHandler extends AbstractNavigationRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String navigationDocId = HttpUtil.getStringParam(request, "navigationDocId");
        long navigationDocBranchId = HttpUtil.getBranchId(request, repository, "navigationDocBranch");
        long navigationDocLanguageId = HttpUtil.getLanguageId(request, repository, "navigationDocLanguage");
        boolean allowOld = HttpUtil.getBooleanParam(request, "allowOld");
        VariantKey navigationDoc = new VariantKey(navigationDocId, navigationDocBranchId, navigationDocLanguageId);

        String activePath = request.getParameter("activePath"); // this is allowed to be null

        VariantKey activeDocument = null;
        String activeDocumentId = request.getParameter("activeDocumentId");
        if (activeDocumentId != null) {
            long activeDocumentBranchId = HttpUtil.getBranchId(request, repository, "activeDocumentBranch");
            long activeDocumentLanguageId = HttpUtil.getLanguageId(request, repository, "activeDocumentLanguage");
            activeDocument = new VariantKey(activeDocumentId, activeDocumentBranchId, activeDocumentLanguageId);
        }

        boolean contextualized = HttpUtil.getBooleanParam(request, "contextualized");
        int depth = HttpUtil.getIntParam(request, "depth", contextualized ? NavigationParams.DEFAULT_CONTEXTUALIZED_DEPTH : NavigationParams.DEFAULT_NONCONTEXTUALIZED_DEPTH);
        boolean handleErrors = HttpUtil.getBooleanParam(request, "handleErrors");
        VersionMode versionMode = VersionMode.get(HttpUtil.getStringParam(request, "navigationVersionMode", "live"));
        Locale locale = Locale.getDefault();
        if (request.getParameter("locale") != null)
            locale = LocaleHelper.parseLocale(request.getParameter("locale"));
        boolean addChildCounts = HttpUtil.getBooleanParam(request, "addChildCounts", false);

        NavigationParams navigationParams = new NavigationParams(navigationDoc, versionMode, activePath, contextualized, depth, addChildCounts, locale);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(5000);
        ContentHandler serializer = new XmlSerializer(bos);
        NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
        navigationManager.generateNavigationTree(serializer, navigationParams, activeDocument, handleErrors, allowOld);

        bos.writeTo(response.getOutputStream());
    }

    public String getPathPattern() {
        return "/tree";
    }
}
