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
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.BadRequestException;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.AclDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class AclHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/acl/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String aclName = (String)matchMap.get("1");

        if (!aclName.equals("live") && !aclName.equals("staging")) {
            response.sendError(HttpConstants._404_Not_Found);
            return;
        }

        AccessManager accessManager = repository.getAccessManager();

        String action = request.getParameter("action");

        if (action == null) {
            if (request.getMethod().equals(HttpConstants.GET)) {
                Acl acl = aclName.equals("live") ? accessManager.getLiveAcl() : accessManager.getStagingAcl();
                acl.getXml().save(response.getOutputStream());
            } else if (request.getMethod().equals(HttpConstants.POST)) {
                // Note that the live ACL is not updateable, but that will give an exception anyway so we don't
                // handle that case here explicitely
                Acl acl = aclName.equals("live") ? accessManager.getLiveAcl() : accessManager.getStagingAcl();
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                AclDocument aclDocument = AclDocument.Factory.parse(request.getInputStream(), xmlOptions);

                if (acl.getUpdateCount() != aclDocument.getAcl().getUpdateCount())
                    throw new ConcurrentUpdateException(Acl.class.getName(), String.valueOf(aclName));

                acl.setFromXml(aclDocument.getAcl());
                acl.save();

                // Possible optimalisation here: only send XML containing lastModified/lastModifier, since that's
                // the only new information.
                acl.getXml().save(response.getOutputStream());
            } else {
                response.sendError(HttpConstants._405_Method_Not_Allowed);
            }
        } else if (action.equals("putLive") && aclName.equals("staging")) {
            accessManager.copyStagingToLive();
        } else if (action.equals("revertChanges") && aclName.equals("staging")) {
            accessManager.copyLiveToStaging();
        } else if (action.equals("evaluate")) {
            String documentId = HttpUtil.getStringParam(request, "documentId");
            long userId = HttpUtil.getLongParam(request, "user");
            long[] roleIds = getLongParams(request, "role");
            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);

            AclResultInfo result;
            if (aclName.equals("live"))
                result = accessManager.getAclInfoOnLive(userId, roleIds, documentId, branchId, languageId);
            else
                result = accessManager.getAclInfoOnStaging(userId, roleIds, documentId, branchId, languageId);

            result.getXml().save(response.getOutputStream());
        } else if (action.equals("evaluateConceptual")) {
            long documentTypeId = HttpUtil.getDocumentTypeId(request, repository, "documentType");
            long userId = HttpUtil.getLongParam(request, "user");
            long[] roleIds = getLongParams(request, "role");
            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);

            AclResultInfo result;
            if (aclName.equals("live"))
                result = accessManager.getAclInfoOnLiveForConceptualDocument(userId, roleIds, documentTypeId, branchId, languageId);
            else
                result = accessManager.getAclInfoOnStagingForConceptualDocument(userId, roleIds, documentTypeId, branchId, languageId);

            result.getXml().save(response.getOutputStream());
        } else {
            throw new BadRequestException("Unsupported value for action parameter: " + action);
        }
    }

    private static long[] getLongParams(HttpServletRequest request, String name) throws Exception {
        String[] values = request.getParameterValues(name);
        if (values == null || values.length == 0)
            return new long[0];

        long[] longValues = new long[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                longValues[i] = Long.parseLong(values[i]);
            } catch (NumberFormatException e) {
                throw new BadRequestException("The value of the request parameter \"" + name + "\" should be an integer value, got: " + values[i]);
            }
        }

        return longValues;
    }

}
