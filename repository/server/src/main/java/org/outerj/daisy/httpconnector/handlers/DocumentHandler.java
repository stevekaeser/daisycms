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

import org.apache.commons.logging.Log;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.ConcurrentUpdateException;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.BadRequestException;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.httpconnector.spi.UploadItem;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.DocumentDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;

public class DocumentHandler extends AbstractDocumentHandler {

    public DocumentHandler(Log requestErrorLogger) {
        super(requestErrorLogger);
    }

    public String getPathPattern() {
        return "/document/*";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        String id = (String)matchMap.get("1");

        if (request.getMethod().equals(HttpConstants.GET)) {
            long branchId = HttpUtil.getBranchId(request, repository);
            long languageId = HttpUtil.getLanguageId(request, repository);
            // retrieve the document (note: don't retrieve from cache so that the document contains fresh lock info)
            Document document = repository.getDocument(id, branchId, languageId, true);
            if (document.canReadLiveOnly()) {
                document.getXmlWithoutVersionedData().save(response.getOutputStream());
            } else {
                document.getXml().save(response.getOutputStream());
            }
        } else if (request.getMethod().equals(HttpConstants.POST)) {
            String actionParam = request.getParameter("action");
            if (actionParam != null && actionParam.equals("createVariant")) {
                long startBranchId = HttpUtil.getBranchId(request, repository, "startBranch");
                long startLanguageId = HttpUtil.getLanguageId(request, repository, "startLanguage");
                long startVersionId = HttpUtil.getLongParam(request, "startVersion");
                long newBranchId = HttpUtil.getBranchId(request, repository, "newBranch");
                long newLanguageId = HttpUtil.getLanguageId(request, repository, "newLanguage");
                Document document = repository.createVariant(id, startBranchId, startLanguageId, startVersionId, newBranchId, newLanguageId, true);
                document.getXml().save(response.getOutputStream());
            } else if (actionParam == null) {
                List<UploadItem> uploadedItems = support.parseMultipartRequest(request, response);
                UploadItem xmlItem = getItemByName(uploadedItems, "xml");
                if (xmlItem == null) {
                    HttpUtil.sendCustomError("The required field named \"xml\" is missing.", HttpConstants._400_Bad_Request, response);
                    return;
                }
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                DocumentDocument documentDocument = DocumentDocument.Factory.parse(xmlItem.getInputStream(), xmlOptions);
                DocumentDocument.Document documentXml = documentDocument.getDocument();

                // retrieve the document
                Document document;
                String createVariant = request.getParameter("createVariant");
                if (createVariant != null && createVariant.equals("yes")) {
                    long startBranchId = HttpUtil.getBranchId(request, repository, "startBranch");
                    long startLanguageId = HttpUtil.getLanguageId(request, repository, "startLanguage");
                    document = repository.createVariant(id, startBranchId, startLanguageId, -1, documentXml.getBranchId(), documentXml.getLanguageId(), false);
                } else {
                    document = repository.getDocument(id, documentXml.getBranchId(), documentXml.getLanguageId(), true);
                    if (document.getVariantUpdateCount() != documentXml.getVariantUpdateCount())
                        throw new ConcurrentUpdateException(Document.class.getName() + "-variant", document.getId() + "~" + document.getBranchId() + "~" + document.getLanguageId());
                }
                if (document.getUpdateCount() != documentXml.getUpdateCount())
                    throw new ConcurrentUpdateException(Document.class.getName(), document.getId());

                boolean documentTypeChecksEnabled = documentXml.isSetDocumentTypeChecksEnabled() ? documentXml.getDocumentTypeChecksEnabled() : true;
                document.setDocumentTypeChecksEnabled(documentTypeChecksEnabled);
                
                updateDocument(document, documentXml, uploadedItems, response, repository);

                boolean validate = documentXml.isSetValidateOnSave() ? documentXml.getValidateOnSave() : true;
                document.save(validate);

                // Send document XML back as response (to consider: maybe this is a bit heavy and/or unneeded?)
                document.getXml().save(response.getOutputStream());
            } else {
                throw new BadRequestException("Invalid value for action parameter: " + actionParam);
            }
        } else if (request.getMethod().equals(HttpConstants.DELETE)) {
            // The presence of the branch and language parameters will determine if we delete only a variant
            // or the complete document.
            String branchParam = request.getParameter("branch");
            String languageParam = request.getParameter("language");

            if (branchParam != null && languageParam != null) {
                long branchId = HttpUtil.getBranchId(request, repository);
                long languageId = HttpUtil.getLanguageId(request, repository);
                repository.deleteVariant(id, branchId, languageId);
            } else if (branchParam == null && languageParam == null) {
                repository.deleteDocument(id);
            } else {
                throw new BadRequestException("branch and language request parameters must both be specified, or not at all.");
            }
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
