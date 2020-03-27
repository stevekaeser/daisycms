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

function editorsRss() {
    rss("editors");
}

function normalRss() {
    rss("normal");
}

function minimalRss() {
    rss("minimal");
}

function rss(rsstype) {
    var daisy = getDaisy();

    // For the RSS feeds, we use "en-US" as locale by default, unless the request contains an explicit
    // locale parameter. This is because the RSS feeds may be cached for multiple users and we don't
    // want to use the locale of whichever user first requests the RSS feed to determine the locale
    // of the cached RSS feed.
    if (cocoon.request.getParameter("locale") == null) {
        Packages.org.outerj.daisy.frontend.WikiHelper.changeLocale(java.util.Locale.US, cocoon.request);
    }

    var login = cocoon.request.getParameter("login");
    var password = cocoon.request.getParameter("password");
    var repository = null;
    if (login != null && password != null) {
        repository = daisy.getRepository(login, password);
    } else {
        repository = daisy.getGuestRepository();
    }

    var since = new java.util.Date(java.lang.System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 14);  // millis * seconds * minutes * hours * days
    var sinceFormatted = Packages.org.outerj.daisy.repository.query.QueryHelper.formatDateTime(since);
    var maxResults = "20";

    var pageContext = daisy.getPageContext(repository);
    var params = new Object();
    params["pageContext"] = pageContext;
    params["localeAsString"] = daisy.getLocaleAsString();
    params["since"] = sinceFormatted;
    params["maxResults"] = maxResults;
    var publisherResult = daisy.performPublisherRequest(rsstype + "-rss-pubreq", params, "html", repository);

    var viewData = new Object();
    viewData["pageContext"] = pageContext;
    viewData["publisherResult"] = publisherResult;
    cocoon.sendPage(rsstype + "-rss-result", viewData);
}