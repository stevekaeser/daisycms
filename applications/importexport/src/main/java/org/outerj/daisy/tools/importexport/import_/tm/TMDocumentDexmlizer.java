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
package org.outerj.daisy.tools.importexport.import_.tm;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;
import org.outerj.daisy.tools.importexport.model.document.ImpExpField;
import org.outerj.daisy.tools.importexport.model.document.ImpExpLink;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.xmlutil.*;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.sax.TransformerHandler;
import java.io.*;

public class TMDocumentDexmlizer {
    private String documentId;
    private String branch;
    private String language;
    private org.w3c.dom.Document xmlDoc;
    private Repository repository;
    private ImpExpDocument document;
    private HtmlCleanerTemplate htmlCleanerTemplate;

    private static XPathExpression NAME_XPATH;
    private static XPathExpression PARTS_XPATH;
    private static XPathExpression FIELDS_XPATH;
    private static XPathExpression FIELD_VALUE_XPATH;
    private static XPathExpression STRING_XPATH;
    private static XPathExpression LINKS_XPATH;
    private static XPathExpression LINK_TITLE_XPATH;
    private static XPathExpression LINK_TARGET_XPATH;
    private static XPathExpression REF_LANG_XPATH;
    private static XPathExpression REF_VERSION_XPATH;

    static {
        try {
            XPathFactory xpathFactory = LocalXPathFactory.get();
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("ie", "http://outerx.org/daisy/1.0#tm-impexp");
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(nsContext);

            NAME_XPATH = xpath.compile("string(/ie:document/ie:title[1])");
            PARTS_XPATH = xpath.compile("/ie:document/ie:part");
            FIELDS_XPATH = xpath.compile("/ie:document/ie:field");
            FIELD_VALUE_XPATH = xpath.compile("ie:value");
            STRING_XPATH = xpath.compile("string(.)");
            LINKS_XPATH = xpath.compile("/ie:document/ie:link");
            LINK_TITLE_XPATH = xpath.compile("ie:title");
            LINK_TARGET_XPATH = xpath.compile("ie:target");
            REF_LANG_XPATH = xpath.compile("/ie:document/@exportedLanguage");
            REF_VERSION_XPATH = xpath.compile("/ie:document/@exportedVersion");

        } catch (XPathExpressionException e) {
            throw new RuntimeException("Error initializing xpath expressions in document loader.", e);
        }
    }

    public static TMDoc fromXml(String documentId, String branch, String language, InputStream is,
            Repository repository, HtmlCleanerTemplate htmlCleanerTemplate) throws Exception {
        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        org.w3c.dom.Document xmlDoc = factory.newDocumentBuilder().parse(is);

        return new TMDocumentDexmlizer(documentId, branch, language, xmlDoc, repository, htmlCleanerTemplate).run();
    }

    private TMDocumentDexmlizer(String documentId, String branch, String language, org.w3c.dom.Document xmlDoc,
            Repository repository, HtmlCleanerTemplate htmlCleanerTemplate) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.xmlDoc = xmlDoc;
        this.repository = repository;
        this.htmlCleanerTemplate = htmlCleanerTemplate;
    }

    private TMDoc run() throws Exception {
        String name = getName();

        // we don't know the document type since it is not part of the export data, but it is not required
        // by the import code either, so we set it to a dummy value.
        document = new ImpExpDocument(documentId, branch, language, "dummy!", name);

        loadParts();
        loadFields();
        loadLinks();

        String expLang = (String)REF_LANG_XPATH.evaluate(xmlDoc, XPathConstants.STRING);
        if (expLang.length() == 0)
            throw new ImportExportException("Invalid export data: missing or empty exportedLanguage attribute.");
        String refVersionString = (String)REF_VERSION_XPATH.evaluate(xmlDoc, XPathConstants.STRING);
        if (refVersionString.length() == 0)
            throw new ImportExportException("Invalid export data: missing or empty exportedVersion attribute.");
        long expVersion;
        try {
            expVersion = Long.parseLong(refVersionString);
        } catch (NumberFormatException e) {
            throw new ImportExportException("Invalid version number in /ie:document/@exportedVersion : \"" + refVersionString + "\".");
        }

        TMDoc tmDoc = new TMDoc();
        tmDoc.exportedLanguage = expLang;
        tmDoc.exportedVersion = expVersion;
        tmDoc.impExpDoc = document;

        return tmDoc;
    }

    private String getName() throws ImportExportException {
        String name;
        try {
            name = (String)NAME_XPATH.evaluate(xmlDoc, XPathConstants.STRING);
        } catch (XPathExpressionException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the document name.", e);
        }

        if (name == null || name.trim().equals(""))
            throw new ImportExportException("Missing or empty name for " + getDescription());

        return name;
    }

    private void loadParts() throws Exception {
        NodeList partElements;
        try {
             partElements = (NodeList)PARTS_XPATH.evaluate(xmlDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the parts.", e);
        }

        for (int i = 0; i < partElements.getLength(); i++) {
            Element partEl = (Element)partElements.item(i);
            String type = DocumentHelper.getAttribute(partEl, "name", true);
            PartType partType = repository.getRepositorySchema().getPartTypeByName(type, false);
            String mimeType = DocumentHelper.getAttribute(partEl, "mimeType", true);
            final byte[] partData = getPartData(partEl, type, partType.isDaisyHtml());

            ImpExpPart part = new ImpExpPart(partType, mimeType, null, new ImpExpPart.PartDataAccess() {
                public InputStream getInputStream() throws Exception {
                    return new ByteArrayInputStream(partData);
                }

                public long getSize() throws Exception {
                    return partData.length;
                }
            });

            document.addPart(part);
        }
    }

    private byte[] getPartData(Element element, String partName, boolean daisyHtml) throws ImportExportException {
        // Search for first (and should be only) child element
        Element el = null;
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                el = (Element)node;
            }
        }

        if (el == null) {
            throw new ImportExportException("Missing XML data for part " + partName);
        }

        try {

            TransformerHandler transformer = LocalTransformerFactory.get().newTransformerHandler();
            transformer.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");

            if (daisyHtml && htmlCleanerTemplate != null) {
                StringWriter writer = new StringWriter();
                Result result = new StreamResult(writer);
                transformer.setResult(result);
                transformer.startDocument();
                NamespaceNormalizingDOMStreamer.stream(el, transformer, transformer);
                transformer.endDocument();
                return htmlCleanerTemplate.newHtmlCleaner().cleanToByteArray(writer.toString());
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                Result result = new StreamResult(bos);
                transformer.setResult(result);
                transformer.startDocument();
                NamespaceNormalizingDOMStreamer.stream(el, transformer, transformer);
                transformer.endDocument();
                return bos.toByteArray();
            }
        } catch (Exception e) {
            throw new ImportExportException("Error extracting XML data for part " + partName);
        }
    }

    private void loadFields() throws Exception {
        NodeList fieldElements;
        try {
             fieldElements = (NodeList)FIELDS_XPATH.evaluate(xmlDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the fields.", e);
        }

        for (int i = 0; i < fieldElements.getLength(); i++) {
            Element fieldEl = (Element)fieldElements.item(i);

            String type = DocumentHelper.getAttribute(fieldEl, "name", true);
            FieldType fieldType = repository.getRepositorySchema().getFieldTypeByName(type, false);

            if (!fieldType.isHierarchical()) { // hierarchical fields are normally not present in export, skip them if they would be
                Object value = getFieldValue(fieldEl, fieldType);
                document.addField(new ImpExpField(fieldType, value));
            }
        }
    }

    private Object getFieldValue(Element fieldEl, FieldType fieldType) throws Exception {
        if (fieldType.isMultiValue()) {
            NodeList valueEls = (NodeList)FIELD_VALUE_XPATH.evaluate(fieldEl, XPathConstants.NODESET);
            Object[] parsedValues = new Object[valueEls.getLength()];
            for (int i = 0; i < valueEls.getLength(); i++) {
                String value = DocumentHelper.getElementText((Element)valueEls.item(i), true);
                parsedValues[i] = XmlizerUtil.parseValue(fieldType.getValueType(), value, repository);
            }
            return parsedValues;
        } else {
            String value = (String)STRING_XPATH.evaluate(fieldEl, XPathConstants.STRING);
            Object parsedValue = XmlizerUtil.parseValue(fieldType.getValueType(), value, repository);
            return parsedValue;
        }
    }

    private void loadLinks() throws Exception {
        NodeList linkElements;
        try {
             linkElements = (NodeList)LINKS_XPATH.evaluate(xmlDoc, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new ImportExportException("Error evaluating the XPath expression for getting the links.", e);
        }

        for (int i = 0; i < linkElements.getLength(); i++) {
            Element linkEl = (Element)linkElements.item(i);
            String title = (String)LINK_TITLE_XPATH.evaluate(linkEl, XPathConstants.STRING);
            String target = (String)LINK_TARGET_XPATH.evaluate(linkEl, XPathConstants.STRING);
            ImpExpLink link = new ImpExpLink(title, target);
            document.addLink(link);
        }
    }


    protected String getDescription() {
        return documentId + "~" + branch + "~" + language;
    }

    public static class TMDoc {
        public String exportedLanguage;
        public long exportedVersion;
        public ImpExpDocument impExpDoc;
    }
}
