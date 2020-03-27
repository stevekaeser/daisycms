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

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.httpconnector.spi.BadRequestException;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.navigation.LookupAlternative;
import org.outerj.daisy.navigation.NavigationLookupResult;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Navigationspec.NavigationLookupDocument;

public class NavigationLookupHandler extends AbstractNavigationRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals("POST")) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            NavigationLookupDocument navigationLookupDocument = NavigationLookupDocument.Factory.parse(request.getInputStream(), xmlOptions);
            if (!navigationLookupDocument.validate()) {
                throw new BadRequestException("Invalid XML posted to navigationLookup.");
            }
            NavigationLookupDocument.NavigationLookup navigationLookupXml = navigationLookupDocument.getNavigationLookup();
            List<NavigationLookupDocument.NavigationLookup.LookupAlternative> lookupAlternativesXml = navigationLookupXml.getLookupAlternativeList();
            LookupAlternative[] lookupAlternatives = new LookupAlternative[lookupAlternativesXml.size()];
            for (int i = 0; i < lookupAlternativesXml.size(); i++) {
                NavigationLookupDocument.NavigationLookup.LookupAlternative lookupAlternativeXml = lookupAlternativesXml.get(i);
                lookupAlternatives[i] = new LookupAlternative(lookupAlternativeXml.getName(), lookupAlternativeXml.getCollectionId(),
                        new VariantKey(lookupAlternativeXml.getNavDocId(), lookupAlternativeXml.getNavBranchId(), lookupAlternativeXml.getNavLangId()),
                        VersionMode.get(lookupAlternativeXml.getNavVersionMode()));
            }

            String navigationPath = navigationLookupXml.getNavigationPath();
            long requestedBranchId = navigationLookupXml.isSetRequestedBranchId() ? navigationLookupXml.getRequestedBranchId() : -1;
            long requestedLangId = navigationLookupXml.isSetRequestedLanguageId() ? navigationLookupXml.getRequestedLanguageId() : -1;

            NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
            NavigationLookupResult lookupResult = navigationManager.lookup(navigationPath, requestedBranchId, requestedLangId, lookupAlternatives, navigationLookupXml.getAllowOld());
            lookupResult.getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals("GET")) {
            throw new BadRequestException("Using GET on /navigation/lookup is no longer supported.");
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/lookup";
    }
}
