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

import org.outerj.daisy.tools.importexport.config.SchemaCustomizer;
import org.outerj.daisy.tools.importexport.config.BasicSchemaCustomizer;

/**
 * Options that influence the export.
 *
 * Descriptions of these options can be found in the Daisy documentation.
 */
public class ExportOptions {
    private SchemaCustomizer schemaCustomizer = new BasicSchemaCustomizer();
    private DocumentExportCustomizer documentExportCustomizer = new BasicCustomizer();
    private boolean exportLastVersion = false;
    private boolean failOnError = false;
    private boolean includeListOfRetiredDocuments = true;
    private boolean exportDocumentOwners = false;
    private boolean stackTracesOfFailures = false;
    private boolean linkExtractionEnabled = true;
    private boolean exportVersionState = true;
    private boolean exportReferenceLanguage = true;

    public SchemaCustomizer getSchemaCustomizer() {
        return schemaCustomizer;
    }

    public void setSchemaCustomizer(SchemaCustomizer schemaCustomizer) {
        if (schemaCustomizer == null)
            throw new IllegalArgumentException("Null argument: schemaCustomizer");
        this.schemaCustomizer = schemaCustomizer;
    }

    public DocumentExportCustomizer getDocumentExportCustomizer() {
        return documentExportCustomizer;
    }

    public void setDocumentExportCustomizer(DocumentExportCustomizer documentExportCustomizer) {
        if (documentExportCustomizer == null)
            throw new IllegalArgumentException("Null argument: documentExportCustomizer");
        this.documentExportCustomizer = documentExportCustomizer;
    }

    public boolean getExportLastVersion() {
        return exportLastVersion;
    }

    public void setExportLastVersion(boolean exportLastVersion) {
        this.exportLastVersion = exportLastVersion;
    }

    public boolean getFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean getIncludeListOfRetiredDocuments() {
        return includeListOfRetiredDocuments;
    }

    public void setIncludeListOfRetiredDocuments(boolean includeListOfRetiredDocuments) {
        this.includeListOfRetiredDocuments = includeListOfRetiredDocuments;
    }

    public boolean getExportDocumentOwners() {
        return exportDocumentOwners;
    }

    public void setExportDocumentOwners(boolean exportDocumentOwners) {
        this.exportDocumentOwners = exportDocumentOwners;
    }

    public boolean getStackTracesOfFailures() {
        return stackTracesOfFailures;
    }

    public void setStackTracesOfFailures(boolean stackTracesOfFailures) {
        this.stackTracesOfFailures = stackTracesOfFailures;
    }

    public boolean getLinkExtractionEnabled() {
        return linkExtractionEnabled;
    }

    public void setLinkExtractionEnabled(boolean linkExtractionEnabled) {
        this.linkExtractionEnabled = linkExtractionEnabled;
    }

    public boolean getExportVersionState() {
        return exportVersionState;
    }

    public void setExportVersionState(boolean exportVersionState) {
        this.exportVersionState = exportVersionState;
    }

    public boolean getExportReferenceLanguage() {
        return exportReferenceLanguage;
    }

    public void setExportReferenceLanguage(boolean exportReferenceLanguage) {
        this.exportReferenceLanguage = exportReferenceLanguage;
    }
}
