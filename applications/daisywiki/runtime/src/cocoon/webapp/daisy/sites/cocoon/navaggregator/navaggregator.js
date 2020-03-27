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
cocoon.load("resource://org/outerj/daisy/frontend/util/daisy-util.js");
importClass(Packages.org.outerj.daisy.navigation.NavigationParams);
importClass(Packages.org.apache.cocoon.xml.SaxBuffer);

function navAggregator() {
    var daisy = getDaisy();
    var pageContext = daisy.getPageContext();
    var repository = daisy.getRepository();
    var siteConf = daisy.getSiteConf();

    // Retrieve a generated navigation tree
    var activePath = cocoon.request.getParameter("activePath");
    var navigationData = new SaxBuffer();
    var navigationManager = repository.getExtension("NavigationManager");
    var navigationParams = new NavigationParams(siteConf.getNavigationDoc(), activePath, false);
    navigationManager.generateNavigationTree(navigationData, navigationParams, null, true);

    // construct and perform a publisher request based on this navigation tree
    var params = new Object();
    params["navigationTree"] = navigationData;
    params["pageContext"] = pageContext;
    params["localeAsString"] = daisy.getLocaleAsString();
    // java.lang.System.out.println(daisy.buildPublisherRequest("navaggr-pubreq", params));
    var publisherResult = daisy.performPublisherRequest("navaggr-pubreq", params, "html");

    // show the result
    var viewData = new Object();
    viewData["pageContext"] = pageContext;
    viewData["publisherResult"] = publisherResult;
    cocoon.sendPage("navaggr-result", viewData);
}