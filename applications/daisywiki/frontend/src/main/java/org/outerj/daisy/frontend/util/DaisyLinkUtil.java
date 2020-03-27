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
package org.outerj.daisy.frontend.util;

import org.outerj.daisy.frontend.components.siteconf.SiteConf;

import java.util.regex.Matcher;

public class DaisyLinkUtil {
    /**
     * Returns a suitable query string with branch and language parameters
     * for a daisy: URL parsed with {@link org.outerj.daisy.util.Constants.DAISY_LINK_PATTERN}
     * in the context of a certain site and document.
     */
    public static String getBranchLangQueryString(Matcher matcher, SiteConf siteConf, long documentBranchId, long documentLanguageId) {
        String branch = matcher.group(2);
        String language = matcher.group(3);

        StringBuilder queryString = new StringBuilder();
        // only show branch/language parameter if it differs from the site branch. Since we don't know what format is in the matcher check against branch/language name and id 
        if (branch != null && branch.length() > 0 && !( branch.equals(String.valueOf(siteConf.getBranchId())) || branch.equals(siteConf.getBranch()))) {
            queryString.append("?branch=").append(branch);
        } else if (documentBranchId != siteConf.getBranchId()) {
            queryString.append("?branch=").append(documentBranchId);
        }
        if (language != null && language.length() > 0 && !( language.equals(String.valueOf(siteConf.getLanguageId())) || language.equals(siteConf.getLanguage())) ) {
            queryString.append(queryString.length() > 0 ? "&" : "?");
            queryString.append("language=").append(language);
        } else if (documentLanguageId != siteConf.getLanguageId()) {
            queryString.append(queryString.length() > 0 ? "&" : "?");
            queryString.append("language=").append(documentLanguageId);
        }
        return queryString.toString();
    }
}
