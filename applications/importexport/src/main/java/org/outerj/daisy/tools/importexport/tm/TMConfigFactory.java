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
package org.outerj.daisy.tools.importexport.tm;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.schema.FieldTypeNotFoundException;
import org.outerj.daisy.repository.schema.PartTypeNotFoundException;
import org.outerj.daisy.repository.schema.DocumentTypeNotFoundException;
import org.outerj.daisy.xmlutil.LocalXPathFactory;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

public class TMConfigFactory {
    private static XPathExpression CROSS_LANG_PARTS_XPATH;
    private static XPathExpression CROSS_LANG_FIELDS_XPATH;

    static {
        try {
            XPathFactory xpathFactory = LocalXPathFactory.get();
            XPath xpath = xpathFactory.newXPath();

            CROSS_LANG_PARTS_XPATH = xpath.compile("/tm-config/language-independent/part");
            CROSS_LANG_FIELDS_XPATH = xpath.compile("/tm-config/language-independent/field");

        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error initializing xpath expressions.", e);
        }
    }

    public static TMConfig parseFromXml(String fileName, Repository repository) throws Exception {
        File file = new File(fileName);
        FileInputStream is = new FileInputStream(file);
        try {
            return parseFromXml(is, repository);
        } finally {
            is.close();
        }
    }

    public static TMConfig parseFromXml(InputStream is, Repository repository) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        Document document = factory.newDocumentBuilder().parse(is);

        TMConfig tmConfig = new TMConfig();

        // read cross-lang fields
        {
            NodeList nodeList = (NodeList)CROSS_LANG_FIELDS_XPATH.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                String type = DocumentHelper.getAttribute((Element)nodeList.item(i), "type", true);
                String documentType = DocumentHelper.getAttribute((Element)nodeList.item(i), "documentType", false);

                type = validateFieldTypeExists(type, repository);
                if (documentType != null)
                    documentType = validateDocumentTypeExists(documentType, repository);

                System.out.println("type = " + type);
                tmConfig.addLangIndepField(documentType, type);
            }
        }

        // read cross-lang parts
        {
            NodeList nodeList = (NodeList)CROSS_LANG_PARTS_XPATH.evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < nodeList.getLength(); i++) {
                String type = DocumentHelper.getAttribute((Element)nodeList.item(i), "type", true);
                String documentType = DocumentHelper.getAttribute((Element)nodeList.item(i), "documentType", false);

                type = validatePartTypeExists(type, repository);
                if (documentType != null)
                    documentType = validateDocumentTypeExists(documentType, repository);

                System.out.println("type = " + type);
                tmConfig.addLangIndepPart(documentType, type);
            }
        }

        return tmConfig;
    }

    private static String validateFieldTypeExists(String fieldType, Repository repository) throws ImportExportException, RepositoryException {
        try {
            return repository.getRepositorySchema().getFieldType(fieldType, false).getName();
        } catch (FieldTypeNotFoundException e) {
            throw new ImportExportException("Field type specified in translation management config does not exist: " + fieldType);
        }
    }

    private static String validatePartTypeExists(String partType, Repository repository) throws ImportExportException, RepositoryException {
        try {
            return repository.getRepositorySchema().getPartType(partType, false).getName();
        } catch (PartTypeNotFoundException e) {
            throw new ImportExportException("Part type specified in translation management config does not exist: " + partType);
        }
    }

    private static String validateDocumentTypeExists(String documentType, Repository repository) throws ImportExportException, RepositoryException {
        try {
            return repository.getRepositorySchema().getDocumentType(documentType, false).getName();
        } catch (DocumentTypeNotFoundException e) {
            throw new ImportExportException("Document type specified in translation management config does not exist: " + documentType);
        }
    }
}
