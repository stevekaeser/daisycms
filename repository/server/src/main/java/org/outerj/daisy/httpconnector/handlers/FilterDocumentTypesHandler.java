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
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.ListUtil;
import org.outerx.daisy.x10.IdsDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class FilterDocumentTypesHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/filterDocumentTypes";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String collectionIdString = request.getParameter("collectionId");
        long collectionId = -1;
        if (collectionIdString != null) {
            collectionId = HttpUtil.parseId("collection", collectionIdString);
        }
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);

        XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
        long[] documentTypeIds = ListUtil.toArray(IdsDocument.Factory.parse(request.getInputStream(), xmlOptions).getIds().getIdList());
        AccessManager accessManager = repository.getAccessManager();
        long[] filterDocumentTypeIds = accessManager.filterDocumentTypes(documentTypeIds, collectionId, branchId, languageId);

        IdsDocument idsDocument = IdsDocument.Factory.newInstance();
        idsDocument.addNewIds().setIdArray(filterDocumentTypeIds);
        idsDocument.save(response.getOutputStream());
    }
}
