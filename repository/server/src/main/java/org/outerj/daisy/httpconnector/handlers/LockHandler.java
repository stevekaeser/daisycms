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
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.LockType;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.LockInfoDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class LockHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/document/*/lock";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String documentId = (String)matchMap.get("1");
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);

        if (request.getMethod().equals(HttpConstants.GET)) {
            Document document = repository.getDocument(documentId, branchId, languageId, false);
            document.getLockInfo(true).getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            LockInfoDocument lockInfoDocument = LockInfoDocument.Factory.parse(request.getInputStream(), xmlOptions);
            LockInfoDocument.LockInfo lockInfoXml = lockInfoDocument.getLockInfo();
            LockType lockType = LockType.fromString(lockInfoXml.getType().toString());
            long duration = lockInfoXml.getDuration();

            Document document = repository.getDocument(documentId, branchId, languageId, true);
            document.lock(duration, lockType);
            document.getLockInfo(false).getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            Document document = repository.getDocument(documentId, branchId, languageId, true);
            document.releaseLock();
            document.getLockInfo(false).getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
