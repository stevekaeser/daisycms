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
package org.outerj.daisy.frontend.workflow;

import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.workflow.QueryValueSource;
import org.outerj.daisy.frontend.RequestUtil;
import org.apache.cocoon.environment.Request;

import java.util.Map;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;

public class SearchHelper {

    public static OrderByParams getOrderByParams(Request request) throws Exception {
        String orderBy = request.getParameter("orderBy");
        if (orderBy != null && orderBy.length() > 0) {
            OrderByParams orderByParams = new OrderByParams();
            orderByParams.orderBy = orderBy;
            String orderBySourceName = RequestUtil.getStringParameter(request, "orderBySource");
            if (orderBySourceName.length() == 0)
                throw new Exception("Invalid request: parameter 'orderBySource' is missing.");
            orderByParams.orderBySource = QueryValueSource.fromString(orderBySourceName);
            String orderByDirectionName = request.getParameter("orderByDirection");
            if (orderByDirectionName.length() == 0)
                orderByDirectionName = null;
            orderByParams.orderByDirection = orderByDirectionName == null ? SortOrder.ASCENDING : SortOrder.fromString(orderByDirectionName);
            return orderByParams;
        } else {
            return null;
        }
    }

    public static OrderByParams getOrderByParams(String orderBy, QueryValueSource orderBySource, SortOrder orderByDirection) {
        OrderByParams params = new OrderByParams();
        params.orderBy = orderBy;
        params.orderBySource = orderBySource;
        params.orderByDirection = orderByDirection;
        return params;
    }

    public static OffsetParams getOffsetParams(Request request) {
        String offsetParam = request.getParameter("offset");
        OffsetParams params = new OffsetParams();
        if (offsetParam == null) {
            params.offset = 0;
        } else {
            params.offset = Integer.parseInt(offsetParam);
        }
        return params;
    }

    public static String buildUrl(String base, Map<String, String> params) throws UnsupportedEncodingException {
        StringBuilder dataUrl = new StringBuilder();
        dataUrl.append(base).append("?");
        for (Map.Entry<String, String> param : params.entrySet()) {
            dataUrl.append(param.getKey());
            dataUrl.append("=");
            dataUrl.append(URLEncoder.encode(param.getValue(), "UTF-8"));
            dataUrl.append("&");
        }
        return dataUrl.toString();
    }

    public static class OrderByParams {
        public String orderBy;
        public QueryValueSource orderBySource;
        public SortOrder orderByDirection;
    }

    public static class OffsetParams {
        public int offset;
        public int length = 10;
    }
}
