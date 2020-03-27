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
package org.outerj.daisy.httpconnector.handlers;

import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.util.HttpConstants;
import org.apache.xmlbeans.XmlObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class SelectionListDataHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/schema/fieldType/*/selectionListData";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        long id = HttpUtil.parseId("field type", (String)matchMap.get("1"));

        if (request.getMethod().equals(HttpConstants.GET)) {
            FieldType fieldType = repository.getRepositorySchema().getFieldTypeById(id, false);

            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);
            String locale = HttpUtil.getStringParam(request, "locale");

            XmlObject expListXml = fieldType.getExpandedSelectionListXml(branchId, languageId, LocaleHelper.parseLocale(locale));
            if (expListXml == null)
                throw new RepositoryException("Field type " + fieldType.getName() + " does not have a selection list.");
            expListXml.save(response.getOutputStream());
        }  else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
