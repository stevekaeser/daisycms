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
package org.outerj.daisy.workflow.serverimpl.httpconnector.handlers;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.workflow.*;
import org.outerj.daisy.repository.RepositoryException;

import java.util.List;
import java.util.Locale;

public class QueryTimerHandler extends AbstractQueryHandler {
    XmlObject performQuery(WorkflowManager workflowManager, List<QuerySelectItem> selectItems, QueryConditions queryConditions, List<QueryOrderByItem> orderByItems, int chunkOffset, int chunkLength, Locale locale) throws RepositoryException {
        XmlObject result;
        if (selectItems == null) {
            List<WfTimer> timers = workflowManager.getTimers(queryConditions, orderByItems, chunkOffset, chunkLength, locale);
            result = WfListHelper.getTimersAsXml(timers);
        } else {
            result = workflowManager.searchTimers(selectItems, queryConditions, orderByItems, chunkOffset, chunkLength, locale);
        }
        return result;
    }

    public String getPathPattern() {
        return "/query/timer";
    }
}
