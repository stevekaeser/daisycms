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
package org.outerj.daisy.frontend;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.apache.cocoon.xml.SaxBuffer;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketEntry;
import org.outerj.daisy.frontend.components.docbasket.DocumentBasketHelper;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.frontend.util.HttpMethodNotAllowedException;
import org.outerj.daisy.navigation.NavigationManager;
import org.outerj.daisy.navigation.NavigationParams;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.variant.VariantManager;

/**
 * Apple that lets the user select documents from the current site's navigation
 * tree to add them to the document basket.
 */
public class NavigationToDocumentBasketApple extends AbstractDaisyApple implements StatelessAppleController {
    private static Pattern DOC_PATTERN = Pattern.compile("([0-9]{1,19}(?:-[a-zA-Z0-9_]{1,200})?)\\.([0-9]+)\\.([0-9]+)");

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Repository repository = frontEndContext.getRepository();
        SiteConf siteConf = frontEndContext.getSiteConf();
        VersionMode versionMode = frontEndContext.getVersionMode();

        if (request.getMethod().equals("GET")) {
            // GET = show the navigation tree

            NavigationManager navigationManager = (NavigationManager)repository.getExtension("NavigationManager");
            SaxBuffer buffer = new SaxBuffer();
            NavigationParams navParams = new NavigationParams(siteConf.getNavigationDoc(), versionMode, null, false, frontEndContext.getLocale());
            navigationManager.generateNavigationTree(buffer, navParams, null, false, true);

            Map<String, Object> viewData = new HashMap<String, Object>();
            viewData.put("pageXml", buffer);
            viewData.put("pageContext", frontEndContext.getPageContext());
            viewData.put("pipeConf", GenericPipeConfig.stylesheetPipe("daisyskin:xslt/docbasket/docbasket_select_from_nav.xsl"));

            appleResponse.sendPage("internal/genericPipe", viewData);

        } else if (request.getMethod().equals("POST")) {
            // POST = selected nodes have been submitted

            VariantManager variantManager = repository.getVariantManager();
            String[] values = request.getParameterValues("document");
            if (values == null)
                values = new String[0];
            DocumentBasketEntry[] entries = new DocumentBasketEntry[values.length];

            for (int i = 0; i < values.length; i++) {
                Matcher docMatcher = DOC_PATTERN.matcher(values[i]);
                if (docMatcher.matches()) {
                    String documentId = docMatcher.group(1);
                    long branchId = Long.parseLong(docMatcher.group(2));
                    long languageId = Long.parseLong(docMatcher.group(3));

                    String branch = variantManager.getBranch(branchId, false).getName();
                    String language = variantManager.getLanguage(languageId, false).getName();

                    entries[i] = new DocumentBasketEntry(documentId, branch, language, -3, "");
                } else {
                    throw new Exception("Unexpected error in adding documents from navigation to document basket: input value did not match: \"" + values[i] + "\".");
                }
            }

            DocumentBasketHelper.updateDocumentNames(entries, request, repository);
            DocumentBasketHelper.getDocumentBasket(request, true).appendEntries(entries);

            appleResponse.redirectTo(getMountPoint() + "/" + siteConf.getName() + "/documentBasket");
        } else {
            throw new HttpMethodNotAllowedException(request.getMethod());
        }
    }

}
