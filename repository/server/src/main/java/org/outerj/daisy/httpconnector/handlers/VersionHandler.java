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

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.ChangeType;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.VersionState;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.BadRequestException;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10.VersionDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;

public class VersionHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/document/*/version/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String documentId = (String)matchMap.get("1");
        long versionId = HttpUtil.parseId("version", (String)matchMap.get("2"));
        long branchId = HttpUtil.getBranchId(request, repository);
        long languageId = HttpUtil.getLanguageId(request, repository);

        if (request.getMethod().equals(HttpConstants.GET)) {
            Document document = repository.getDocument(documentId, branchId, languageId, false);
            document.getVersion(versionId).getXml().save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            String action = request.getParameter("action");
            if ("changeState".equals(action)) {
                String newState = request.getParameter("newState");
                if (newState == null)
                    throw new BadRequestException("Missing parameter \"newState\".");
                VersionState versionState = VersionState.fromString(newState);
                Document document = repository.getDocument(documentId, branchId, languageId, true);
                Version version = document.getVersion(versionId);
                version.setState(versionState);
                version.save();
                version.getShallowXml().save(response.getOutputStream());
            } else if (action == null) {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                VersionDocument versionDocument = VersionDocument.Factory.parse(request.getInputStream(), xmlOptions);
                VersionDocument.Version versionXml = versionDocument.getVersion();
                Document document = repository.getDocument(documentId, branchId, languageId, true);
                Version version = document.getVersion(versionId);
                version.setState(VersionState.fromString(versionXml.getState()));
                if (versionXml.isSetSyncedWithLanguageId() && versionXml.isSetSyncedWithVersionId())
                    version.setSyncedWith(versionXml.getSyncedWithLanguageId(), versionXml.getSyncedWithVersionId());
                else
                    version.setSyncedWith(null);
                version.setChangeType(ChangeType.fromString(versionXml.getChangeType()));
                version.setChangeComment(versionXml.getChangeComment());
                version.save();
                version.getShallowXml().save(response.getOutputStream());
            } else {
                throw new BadRequestException("Unsupported value for \"action\" parameter: " + action);
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
