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
package org.outerj.daisy.doctaskrunner.serverimpl.httphandlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.util.HttpConstants;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class TaskDocDetailsHandler extends AbstractDocTaskRequestHandler {
    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
        long taskId = HttpUtil.parseId("task", (String)matchMap.get("1"));
        if (request.getMethod().equals(HttpConstants.GET)) {
            taskManager.getTaskDocDetails(taskId).getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }

    public String getPathPattern() {
        return "/task/*/docdetails";
    }
}
