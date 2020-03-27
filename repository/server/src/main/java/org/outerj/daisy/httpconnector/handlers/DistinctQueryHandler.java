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
package org.outerj.daisy.httpconnector.handlers;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.LocaleHelper;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.DistinctSearchResultDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DistinctQueryHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/distinctquery";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.GET)) {
            String query = HttpUtil.getStringParam(request, "q");
            String locale = HttpUtil.getStringParam(request, "locale");
            String sortOrderParam = HttpUtil.getStringParam(request, "sortOrder");
            SortOrder sortOrder = SortOrder.fromString(sortOrderParam);
            String extraCond = request.getParameter("extraCondition");

            QueryManager queryManager = repository.getQueryManager();
            DistinctSearchResultDocument resultDoc = queryManager.performDistinctQuery(query, extraCond, sortOrder, LocaleHelper.parseLocale(locale));
            resultDoc.save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
