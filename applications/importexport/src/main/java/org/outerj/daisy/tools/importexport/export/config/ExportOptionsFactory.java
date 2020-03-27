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
package org.outerj.daisy.tools.importexport.export.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.tools.importexport.config.SchemaCustomizer;
import org.outerj.daisy.tools.importexport.util.ImportExportUtil;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.repository.Repository;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;

/**
 * Builds an {@link ExportOptions} instance from an XML description of
 * the export options.
 */
public class ExportOptionsFactory {
    /**
     * Returns sensible default options.
     */
    public static ExportOptions getDefaultExportOptions() {
        ExportOptions options = new ExportOptions();

        BasicCustomizer customizer = new BasicCustomizer();
        customizer.addFieldToDrop("Image", "ImageWidth");
        customizer.addFieldToDrop("Image", "ImageHeight");
        customizer.addPartToDrop("Image", "ImagePreview");
        customizer.addPartToDrop("Image", "ImageThumbnail");
        options.setDocumentExportCustomizer(customizer);

        return options;
    }

    public static ExportOptions parseFromXml(InputStream is, Repository repository) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        Document document = factory.newDocumentBuilder().parse(is);
        Element root = document.getDocumentElement();

        ExportOptions options = new ExportOptions();

        Element customizerEl = DocumentHelper.getElementChild(root, "documentCustomizer", false);
        if (customizerEl != null) {
            try {
                DocumentExportCustomizer customizer = (DocumentExportCustomizer)ImportExportUtil.useFactory(customizerEl, repository);
                options.setDocumentExportCustomizer(customizer);
            } catch (Exception e) {
                throw new ImportExportException("Error calling document customizer factory.", e);
            }
        }

        Element schemaCustomizerEl = DocumentHelper.getElementChild(root, "schemaCustomizer", false);
        if (schemaCustomizerEl != null) {
            try {
                SchemaCustomizer customizer = (SchemaCustomizer)ImportExportUtil.useFactory(schemaCustomizerEl, repository);
                options.setSchemaCustomizer(customizer);
            } catch (Exception e) {
                throw new ImportExportException("Error calling schema customizer factory.", e);
            }
        }

        options.setExportLastVersion(DocumentHelper.getBooleanElement(root, EXPORT_LAST_VERSION, options.getExportLastVersion()));
        options.setFailOnError(DocumentHelper.getBooleanElement(root, FAIL_ON_ERROR, options.getFailOnError()));
        options.setIncludeListOfRetiredDocuments(DocumentHelper.getBooleanElement(root, INCL_RETIRED_DOCS, options.getIncludeListOfRetiredDocuments()));
        options.setExportDocumentOwners(DocumentHelper.getBooleanElement(root, EXPORT_OWNER, options.getExportDocumentOwners()));
        options.setStackTracesOfFailures(DocumentHelper.getBooleanElement(root, FAILURE_STACKTRACES, options.getStackTracesOfFailures()));
        options.setLinkExtractionEnabled(DocumentHelper.getBooleanElement(root, LINK_EXTRACTION_ENABLED, options.getLinkExtractionEnabled()));
        options.setExportVersionState(DocumentHelper.getBooleanElement(root, EXPORT_VERSION_STATE, options.getExportVersionState()));
        options.setExportReferenceLanguage(DocumentHelper.getBooleanElement(root, EXPORT_REF_LANG, options.getExportReferenceLanguage()));

        return options;
    }

    public static String toXml(ExportOptions options) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?>\n");
        buffer.append("<exportOptions>\n");

        addOptionXml(buffer, EXPORT_LAST_VERSION, String.valueOf(options.getExportLastVersion()));
        addOptionXml(buffer, FAIL_ON_ERROR, String.valueOf(options.getFailOnError()));
        addOptionXml(buffer, INCL_RETIRED_DOCS, String.valueOf(options.getIncludeListOfRetiredDocuments()));
        addOptionXml(buffer, EXPORT_OWNER, String.valueOf(options.getExportDocumentOwners()));
        addOptionXml(buffer, FAILURE_STACKTRACES, String.valueOf(options.getStackTracesOfFailures()));
        addOptionXml(buffer, LINK_EXTRACTION_ENABLED, String.valueOf(options.getLinkExtractionEnabled()));
        addOptionXml(buffer, EXPORT_VERSION_STATE, String.valueOf(options.getExportVersionState()));
        addOptionXml(buffer, EXPORT_REF_LANG, String.valueOf(options.getExportReferenceLanguage()));

        buffer.append("  <!--+\n");
        buffer.append("      | A document customizer allows to manipulate the content of a document before export.\n");
        buffer.append("      +-->\n");
        buffer.append(options.getDocumentExportCustomizer().getXml());
        buffer.append("\n");
        buffer.append("  <!--+\n");
        buffer.append("      | A schema customizer allows to manipulate the schema before export.\n");
        buffer.append("      +-->\n");
        buffer.append(options.getSchemaCustomizer().getXml());

        buffer.append("</exportOptions>\n");
        return buffer.toString();
    }

    private static void addOptionXml(StringBuilder buffer, String name, String value) {
        buffer.append("  <").append(name).append(">").append(value).append("</").append(name).append(">\n");
    }

    private static String EXPORT_LAST_VERSION = "exportLastVersion";
    private static String FAIL_ON_ERROR = "failOnError";
    private static String INCL_RETIRED_DOCS = "includeListOfRetiredDocuments";
    private static String EXPORT_OWNER = "exportDocumentOwners";
    private static String FAILURE_STACKTRACES = "stackTracesOfFailures";
    private static String LINK_EXTRACTION_ENABLED = "enableLinkExtraction";
    private static String EXPORT_VERSION_STATE = "exportVersionState";
    private static String EXPORT_REF_LANG = "exportReferenceLanguage";
}
