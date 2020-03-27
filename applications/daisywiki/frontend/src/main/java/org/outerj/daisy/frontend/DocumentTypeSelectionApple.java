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
package org.outerj.daisy.frontend;

import java.util.*;

import org.apache.cocoon.components.flow.apples.AppleRequest;
import org.apache.cocoon.components.flow.apples.AppleResponse;
import org.apache.cocoon.components.flow.apples.StatelessAppleController;
import org.outerj.daisy.frontend.components.siteconf.SiteConf;
import org.outerj.daisy.frontend.util.AbstractDaisyApple;
import org.outerj.daisy.frontend.util.GenericPipeConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.DocumentType;

public class DocumentTypeSelectionApple extends AbstractDaisyApple implements StatelessAppleController {

    protected void processRequest(AppleRequest appleRequest, AppleResponse appleResponse) throws Exception {
        Locale locale = frontEndContext.getLocale();
        Repository repository = frontEndContext.getRepository();
        SiteConf siteConf = frontEndContext.getSiteConf();
        String requestedNavigationPath = appleRequest.getSitemapParameter("requestedNavigationPath");

        long branchId = RequestUtil.getBranchId(request, siteConf.getBranchId(), repository);
        long languageId = RequestUtil.getLanguageId(request, siteConf.getLanguageId(), repository);

        DocumentType[] filteredDocumentTypes = getFilteredDocumentTypes(repository, siteConf.getCollectionId(), false,
                siteConf, branchId, languageId);

        String returnTo = request.getParameter("returnTo");
        if (returnTo == null)
            returnTo = getMountPoint() + "/" + siteConf.getName() + "/" + requestedNavigationPath + "/edit";

        Map<String, Object> viewData = new HashMap<String, Object>();
        viewData.put("pageContext", frontEndContext.getPageContext());
        viewData.put("locale", locale);
        viewData.put("documentTypes", filteredDocumentTypes);
        viewData.put("requestedNavigationPath", requestedNavigationPath);
        viewData.put("goto", returnTo);
        viewData.put("branches", repository.getVariantManager().getAllBranches(false).getArray());
        viewData.put("languages", repository.getVariantManager().getAllLanguages(false).getArray());
        viewData.put("selectedBranchId", branchId);
        viewData.put("selectedLanguageId", languageId);

        GenericPipeConfig pipeConf = new GenericPipeConfig();
        pipeConf.setTemplate("resources/xml/doctypes_list.xml");
        pipeConf.setStylesheet("daisyskin:xslt/select_doctype.xsl");
        viewData.put("pipeConf", pipeConf);

        appleResponse.sendPage("internal/genericPipe", viewData);
    }

    public static DocumentType[] getFilteredDocumentTypes(Repository repository, long collectionId,
            boolean omitDeprecated, SiteConf siteConf, long branchId, long languageId) throws Exception {
        DocumentType[] documentTypes = repository.getRepositorySchema().getAllDocumentTypes(false).getArray();        
        long[] documentTypeIds = new long[documentTypes.length];
        for (int i = 0; i < documentTypes.length; i++) {
            documentTypeIds[i] = documentTypes[i].getId();
        }
        
        long[] filteredIds = repository.getAccessManager().filterDocumentTypes(documentTypeIds, collectionId, branchId,
                languageId);

        List<DocumentType> filteredDocumentTypes = new ArrayList<DocumentType>();        
        for (long filteredId : filteredIds) {
            DocumentType foundDocumentType = getDocumentType(documentTypes, filteredId);
            if (foundDocumentType == null) {
                throw new Exception("Encountered a situation which should be impossible, documentType " + filteredId + " not found.");
            } else if (!(omitDeprecated && foundDocumentType.isDeprecated())) {
                filteredDocumentTypes.add(foundDocumentType);
            }
        }

        filteredDocumentTypes = siteConf.filterDocumentTypes(filteredDocumentTypes);

        return filteredDocumentTypes.toArray(new DocumentType[filteredDocumentTypes.size()]);
    }

    private static DocumentType getDocumentType(DocumentType[] documentTypes, long documentTypeId) {
        for (DocumentType documentType : documentTypes) {
            if (documentType.getId() == documentTypeId) {
                return documentType;
            }
        }
        return null;
    }
}
