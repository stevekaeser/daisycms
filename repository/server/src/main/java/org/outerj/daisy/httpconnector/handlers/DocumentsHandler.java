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
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerj.daisy.httpconnector.spi.UploadItem;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerx.daisy.x10.DocumentDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.List;

public class DocumentsHandler extends AbstractDocumentHandler {

    public DocumentsHandler(Log requestErrorLogger) {
        super(requestErrorLogger);
    }

    public String getPathPattern() {
        return "/document";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        if (request.getMethod().equals(HttpConstants.POST)) {
            // POST here is for creating a new document

            List<UploadItem> uploadedItems = support.parseMultipartRequest(request, response);
            UploadItem xmlItem = getItemByName(uploadedItems, "xml");
            if (xmlItem == null) {
                HttpUtil.sendCustomError("The required field named \"xml\" is missing.", HttpConstants._400_Bad_Request, response);
                return;
            }
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            DocumentDocument documentDocument = DocumentDocument.Factory.parse(xmlItem.getInputStream(), xmlOptions);
            DocumentDocument.Document documentXml = documentDocument.getDocument();

            // create the document
            Document document = repository.createDocument(documentXml.getName(), documentXml.getTypeId(), documentXml.getBranchId(), documentXml.getLanguageId());
            if (documentXml.isSetRequestedId())
                document.setRequestedId(documentXml.getRequestedId());
            updateDocument(document, documentXml, uploadedItems, response, repository);

            boolean validate = documentXml.isSetValidateOnSave() ? documentXml.getValidateOnSave() : true;
            document.save(validate);

            // Send document XML back as response
            document.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
