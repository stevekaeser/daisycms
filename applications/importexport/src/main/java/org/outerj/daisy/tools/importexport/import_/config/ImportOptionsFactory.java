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
package org.outerj.daisy.tools.importexport.import_.config;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.util.ImportExportUtil;
import org.outerj.daisy.tools.importexport.config.SchemaCustomizer;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.ChangeType;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.BufferedInputStream;

/**
 * Builds an {@link ImportOptions} instance from an XML description
 * of the import options.
 */
public class ImportOptionsFactory {
    /**
     * Returns sensible default options.
     */
    public static ImportOptions getDefaultImportOptions() {
        ImportOptions options = new ImportOptions();

        BasicCustomizer customizer = new BasicCustomizer();
        customizer.addPartNotToStore("Image", "ImageThumbnail");
        customizer.addPartNotToRemove("Image", "ImageThumbnail");
        customizer.addPartNotToStore("Image", "ImagePreview");
        customizer.addPartNotToRemove("Image", "ImagePreview");
        customizer.addFieldNotToStore("Image", "ImageWith");
        customizer.addFieldNotToRemove("Image", "ImageWidth");
        customizer.addFieldNotToStore("Image", "ImageHeight");
        customizer.addFieldNotToRemove("Image", "ImageHeight");
        options.setDocumentImportCustomizer(customizer);

        return options;
    }

    public static ImportOptions parseFromXml(InputStream is, Repository repository) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        if (!(is instanceof BufferedInputStream))
            is = new BufferedInputStream(is);

        Document document = factory.newDocumentBuilder().parse(is);
        Element root = document.getDocumentElement();

        ImportOptions options = new ImportOptions();

        Element docCustomizerEl = DocumentHelper.getElementChild(root, "documentCustomizer", false);
        if (docCustomizerEl != null) {
            try {
                DocumentImportCustomizer customizer = (DocumentImportCustomizer)ImportExportUtil.useFactory(docCustomizerEl, repository);
                options.setDocumentImportCustomizer(customizer);
            } catch (Exception e) {
                throw new ImportExportException("Error calling document import customizer factory.", e);
            }
        }

        Element schemaCustomizerEl = DocumentHelper.getElementChild(root, "schemaCustomizer", false);
        if (schemaCustomizerEl != null) {
            try {
                SchemaCustomizer customizer = (SchemaCustomizer) ImportExportUtil.useFactory(schemaCustomizerEl, repository);
                options.setSchemaCustomizer(customizer);
            } catch (Exception e) {
                throw new ImportExportException("Error calling schema customizer factory.", e);
            }
        }

        options.setValidateOnSave(DocumentHelper.getBooleanElement(root, VALIDATE_ON_SAVE, options.getValidateOnSave()));
        options.setDocumentTypeChecksEnabled(DocumentHelper.getBooleanElement(root, DOCTYPE_CHECKS_ENABLED, options.getDocumentTypeChecksEnabled()));
        options.setCreateDocuments(DocumentHelper.getBooleanElement(root, CREATE_DOCUMENTS, options.getCreateDocuments()));
        options.setCreateVariants(DocumentHelper.getBooleanElement(root, CREATE_VARIANTS, options.getCreateVariants()));
        options.setUpdateDocuments(DocumentHelper.getBooleanElement(root, UPDATE_DOCUMENTS, options.getUpdateDocuments()));
        options.setMaxSizeForDataCompare(DocumentHelper.getIntegerElement(root, MAX_SIZE_FORE_DATACOMPARE, options.getMaxSizeForDataCompare()));
        options.setStoreOwner(DocumentHelper.getBooleanElement(root, STORE_OWNER, options.getStoreOwner()));
        options.setFailOnNonExistingOwner(DocumentHelper.getBooleanElement(root, FAIL_ON_NONEXISTING_OWNER, options.getFailOnNonExistingOwner()));
        options.setCreateMissingCollections(DocumentHelper.getBooleanElement(root, CREATE_MISSING_COLLECTIONS, options.getCreateMissingCollections()));
        options.setFailOnPermissionDenied(DocumentHelper.getBooleanElement(root, FAIL_ON_PERMISSION_DENIED, options.getFailOnPermissionDenied()));
        options.setFailOnLockedDocument(DocumentHelper.getBooleanElement(root, FAIL_ON_LOCKED_DOCUMENT, options.getFailOnLockedDocument()));
        options.setFailOnError(DocumentHelper.getBooleanElement(root, FAIL_ON_ERROR, options.getFailOnError()));
        options.setFullStackTracesOfFailures(DocumentHelper.getBooleanElement(root, FULL_STACKTRACE_FAILURES, options.getFullStackTracesOfFailures()));
        options.setEnableDetailOutput(DocumentHelper.getBooleanElement(root, ENABLE_DETAIL, options.getEnableDetailOutput()));
        options.setCheckVersion(DocumentHelper.getBooleanElement(root, CHECK_VERSION, options.getCheckVersion()));
        options.setSchemaCreateOnly(DocumentHelper.getBooleanElement(root, SCHEMA_CREATE_ONLY, options.getSchemaCreateOnly()));
        options.setSchemaClearLocalizedData(DocumentHelper.getBooleanElement(root, SCHEMA_CLEAR_LOCALIZED_DATA, options.getSchemaClearLocalizedData()));
        options.setSaveAsDraft(DocumentHelper.getBooleanElement(root, SAVE_AS_DRAFT, options.getSaveAsDraft()));
        options.setImportVersionState(DocumentHelper.getBooleanElement(root, IMPORT_VERSION_STATE, options.getImportVersionState()));
        options.setUnretire(DocumentHelper.getBooleanElement(root, UNRETIRE, options.getUnretire()));
        options.setExcludeFilesPattern(DocumentHelper.getStringElement(root, EXCLUDE_FILES_PATTERN, options.getExcludeFilesPattern()));
        options.setChangeComment(DocumentHelper.getStringElement(root, CHANGE_COMMENT, options.getChangeComment()));
        options.setChangeType(ChangeType.fromString(DocumentHelper.getStringElement(root, CHANGE_TYPE, options.getChangeType().toString())));
        options.setStoreReferenceLanguage(DocumentHelper.getBooleanElement(root, STORE_REFERENCE_LANGUAGE, options.getStoreReferenceLanguage()));

        return options;
    }

    public static String toXml(ImportOptions options) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("<?xml version=\"1.0\"?>\n");
        buffer.append("<importOptions>\n");
        addOptionXml(buffer, VALIDATE_ON_SAVE, String.valueOf(options.getValidateOnSave()));
        addOptionXml(buffer, DOCTYPE_CHECKS_ENABLED, String.valueOf(options.getDocumentTypeChecksEnabled()));
        addOptionXml(buffer, CREATE_DOCUMENTS, String.valueOf(options.getCreateDocuments()));
        addOptionXml(buffer, CREATE_VARIANTS, String.valueOf(options.getCreateVariants()));
        addOptionXml(buffer, UPDATE_DOCUMENTS, String.valueOf(options.getUpdateDocuments()));
        addOptionXml(buffer, MAX_SIZE_FORE_DATACOMPARE, String.valueOf(options.getMaxSizeForDataCompare()));
        addOptionXml(buffer, STORE_OWNER, String.valueOf(options.getStoreOwner()));
        addOptionXml(buffer, FAIL_ON_NONEXISTING_OWNER, String.valueOf(options.getFailOnNonExistingOwner()));
        addOptionXml(buffer, CREATE_MISSING_COLLECTIONS, String.valueOf(options.getCreateMissingCollections()));
        addOptionXml(buffer, FAIL_ON_PERMISSION_DENIED, String.valueOf(options.getFailOnPermissionDenied()));
        addOptionXml(buffer, FAIL_ON_LOCKED_DOCUMENT, String.valueOf(options.getFailOnLockedDocument()));
        addOptionXml(buffer, FAIL_ON_ERROR, String.valueOf(options.getFailOnError()));
        addOptionXml(buffer, FULL_STACKTRACE_FAILURES, String.valueOf(options.getFullStackTracesOfFailures()));
        addOptionXml(buffer, ENABLE_DETAIL, String.valueOf(options.getEnableDetailOutput()));
        addOptionXml(buffer, CHECK_VERSION, String.valueOf(options.getCheckVersion()));
        addOptionXml(buffer, SCHEMA_CREATE_ONLY, String.valueOf(options.getSchemaCreateOnly()));
        addOptionXml(buffer, SCHEMA_CLEAR_LOCALIZED_DATA, String.valueOf(options.getSchemaClearLocalizedData()));
        addOptionXml(buffer, SAVE_AS_DRAFT, String.valueOf(options.getSaveAsDraft()));
        addOptionXml(buffer, IMPORT_VERSION_STATE, String.valueOf(options.getImportVersionState()));
        addOptionXml(buffer, UNRETIRE, String.valueOf(options.getUnretire()));
        addOptionXml(buffer, EXCLUDE_FILES_PATTERN, options.getExcludeFilesPattern());
        addOptionXml(buffer, CHANGE_COMMENT, options.getChangeComment());
        addOptionXml(buffer, CHANGE_TYPE, options.getChangeType().toString());
        addOptionXml(buffer, STORE_REFERENCE_LANGUAGE, String.valueOf(options.getStoreReferenceLanguage()));

        buffer.append("\n");
        buffer.append("  <!--+\n");
        buffer.append("      | A document customizer allows to manipulate the content of a document before import.\n");
        buffer.append("      +-->\n");
        buffer.append(options.getDocumentImportCustomizer().getXml());
        buffer.append("\n");
        buffer.append("  <!--+\n");
        buffer.append("      | A schema customizer allows to manipulate the schema before import.\n");
        buffer.append("      +-->\n");
        buffer.append(options.getSchemaCustomizer().getXml());

        buffer.append("</importOptions>\n");
        return buffer.toString();
    }

    private static void addOptionXml(StringBuilder buffer, String name, String value) {
        buffer.append("  <").append(name).append(">").append(value).append("</").append(name).append(">\n");
    }

    private static String VALIDATE_ON_SAVE = "validateOnSave";
    private static String DOCTYPE_CHECKS_ENABLED = "documentTypeChecksEnabled";
    private static String CREATE_DOCUMENTS = "createDocuments";
    private static String CREATE_VARIANTS = "createVariants";
    private static String UPDATE_DOCUMENTS = "updateDocuments";
    private static String MAX_SIZE_FORE_DATACOMPARE = "maxSizeForDataCompare";
    private static String STORE_OWNER = "storeOwner";
    private static String FAIL_ON_NONEXISTING_OWNER = "failOnNonExistingOwner";
    private static String CREATE_MISSING_COLLECTIONS = "createMissingCollections";
    private static String FAIL_ON_PERMISSION_DENIED = "failOnPermissionDenied";
    private static String FAIL_ON_LOCKED_DOCUMENT = "failOnLockedDocument";
    private static String FAIL_ON_ERROR = "failOnError";
    private static String FULL_STACKTRACE_FAILURES = "fullStackTracesOfFailures";
    private static String ENABLE_DETAIL = "enableDetailOutput";
    private static String CHECK_VERSION = "checkVersion";
    private static String SCHEMA_CREATE_ONLY = "schemaCreateOnly";
    private static String SCHEMA_CLEAR_LOCALIZED_DATA = "schemaClearLocalizedData";
    private static String SAVE_AS_DRAFT = "saveAsDraft";
    private static String IMPORT_VERSION_STATE = "importVersionState";
    private static String UNRETIRE = "unretire";
    private static String EXCLUDE_FILES_PATTERN = "excludeFilesPattern";
    private static String CHANGE_COMMENT = "changeComment";
    private static String CHANGE_TYPE = "changeType";
    private static String STORE_REFERENCE_LANGUAGE = "storeReferenceLanguage";

}
