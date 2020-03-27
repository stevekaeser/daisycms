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
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.DocumentTypes;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.util.HttpConstants;
import org.outerj.daisy.httpconnector.spi.RequestHandlerSupport;
import org.outerx.daisy.x10.DocumentTypeDocument;
import org.apache.xmlbeans.XmlOptions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

public class DocumentTypesHandler extends AbstractRepositoryRequestHandler {
    public String getPathPattern() {
        return "/schema/documentType";
    }

    public void handleRequest(Map matchMap, HttpServletRequest request, HttpServletResponse response, Repository repository, RequestHandlerSupport support) throws Exception {
        RepositorySchema repositorySchema = repository.getRepositorySchema();

        if (request.getMethod().equals(HttpConstants.POST)) {
            // A POST creates a new DocumentType and returns the XML representation of the newly created DocumentType
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            DocumentTypeDocument documentTypeDocument = DocumentTypeDocument.Factory.parse(request.getInputStream(), xmlOptions);
            DocumentTypeDocument.DocumentType documentTypeXml = documentTypeDocument.getDocumentType();
            DocumentType documentType = repositorySchema.createDocumentType(documentTypeXml.getName());
            documentType.setAllFromXml(documentTypeXml);
            documentType.save();

            documentTypeDocument = documentType.getXml();
            documentTypeDocument.save(response.getOutputStream());
        } else if (request.getMethod().equals(HttpConstants.GET)) {
            // A GET returns the list of all PartTypes as XML
            DocumentTypes documentTypes = repositorySchema.getAllDocumentTypes(true);
            documentTypes.getXml().save(response.getOutputStream());
        } else {
            response.sendError(HttpConstants._405_Method_Not_Allowed);
        }
    }
}
