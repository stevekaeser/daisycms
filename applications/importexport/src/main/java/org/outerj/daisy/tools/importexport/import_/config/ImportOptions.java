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

import org.outerj.daisy.tools.importexport.config.SchemaCustomizer;
import org.outerj.daisy.tools.importexport.config.BasicSchemaCustomizer;
import org.outerj.daisy.repository.ChangeType;

/**
 * Options that influence the import.
 *
 * Descriptions of these options can be found in the Daisy documentation.
 */
public class ImportOptions {
    private boolean validateOnSave = true;
    private boolean documentTypeChecksEnabled = true;
    private boolean createDocuments = true;
    private boolean createVariants = true;
    private boolean updateDocuments = true;
    private int maxSizeForDataCompare = -1;
    private boolean storeOwner = false;
    private boolean failOnNonExistingOwner = true;
    private boolean createMissingCollections = true;
    private boolean failOnPermissionDenied = false;
    private boolean failOnLockedDocument = false;
    private boolean failOnError = false;
    private boolean fullStackTracesOfFailures = false;
    private boolean enableDetailOutput = false;
    private boolean checkVersion = true;
    private boolean schemaCreateOnly = false;
    private boolean schemaClearLocalizedData = false;
    private boolean saveAsDraft = false;
    private boolean importVersionState = true;
    private boolean unretire = true;
    private String excludeFilesPattern = "^(\\..*)|(CVS)$";
    private DocumentImportCustomizer docImpCustomizer = new BasicCustomizer();
    private SchemaCustomizer schemaCustomizer = new BasicSchemaCustomizer();
    private String changeComment = "Updated by import";
    private ChangeType changeType = ChangeType.MAJOR;
    private boolean storeReferenceLanguage = true;

    public boolean getValidateOnSave() {
        return validateOnSave;
    }

    public void setValidateOnSave(boolean validateOnSave) {
        this.validateOnSave = validateOnSave;
    }

    public boolean getDocumentTypeChecksEnabled() {
        return documentTypeChecksEnabled;
    }

    public void setDocumentTypeChecksEnabled(boolean documentTypeChecksEnabled) {
        this.documentTypeChecksEnabled = documentTypeChecksEnabled;
    }

    public boolean getCreateDocuments() {
        return createDocuments;
    }

    public void setCreateDocuments(boolean createDocuments) {
        this.createDocuments = createDocuments;
    }

    public boolean getCreateVariants() {
        return createVariants;
    }

    public void setCreateVariants(boolean createVariants) {
        this.createVariants = createVariants;
    }

    public boolean getUpdateDocuments() {
        return updateDocuments;
    }

    public void setUpdateDocuments(boolean updateDocuments) {
        this.updateDocuments = updateDocuments;
    }

    public int getMaxSizeForDataCompare() {
        return maxSizeForDataCompare;
    }

    public void setMaxSizeForDataCompare(int maxSizeForDataCompare) {
        this.maxSizeForDataCompare = maxSizeForDataCompare;
    }

    public DocumentImportCustomizer getDocumentImportCustomizer() {
        return docImpCustomizer;
    }

    public void setDocumentImportCustomizer(DocumentImportCustomizer docImportCustomizer) {
        if (docImpCustomizer == null)
            throw new IllegalArgumentException("Null argument: docImportCustomizer");
        this.docImpCustomizer = docImportCustomizer;
    }

    public boolean getFailOnNonExistingOwner() {
        return failOnNonExistingOwner;
    }

    public SchemaCustomizer getSchemaCustomizer() {
        return schemaCustomizer;
    }

    public void setSchemaCustomizer(SchemaCustomizer schemaCustomizer) {
        if (schemaCustomizer == null)
            throw new IllegalArgumentException("Null argument: schemaCustomizer");
        this.schemaCustomizer = schemaCustomizer;
    }

    public void setFailOnNonExistingOwner(boolean failOnNonExistingOwner) {
        this.failOnNonExistingOwner = failOnNonExistingOwner;
    }

    public boolean getStoreOwner() {
        return storeOwner;
    }

    public void setStoreOwner(boolean storeOwner) {
        this.storeOwner = storeOwner;
    }

    public boolean getCreateMissingCollections() {
        return createMissingCollections;
    }

    public void setCreateMissingCollections(boolean createMissingCollections) {
        this.createMissingCollections = createMissingCollections;
    }

    public boolean getFailOnPermissionDenied() {
        return failOnPermissionDenied;
    }

    public void setFailOnPermissionDenied(boolean failOnPermissionDenied) {
        this.failOnPermissionDenied = failOnPermissionDenied;
    }

    public boolean getFailOnLockedDocument() {
        return failOnLockedDocument;
    }

    public void setFailOnLockedDocument(boolean failOnLockedDocument) {
        this.failOnLockedDocument = failOnLockedDocument;
    }

    public boolean getFailOnError() {
        return failOnError;
    }

    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    public boolean getFullStackTracesOfFailures() {
        return fullStackTracesOfFailures;
    }

    public void setFullStackTracesOfFailures(boolean fullStackTracesOfFailures) {
        this.fullStackTracesOfFailures = fullStackTracesOfFailures;
    }

    public DocumentImportCustomizer getDocImpCustomizer() {
        return docImpCustomizer;
    }

    public void setDocImpCustomizer(DocumentImportCustomizer docImpCustomizer) {
        this.docImpCustomizer = docImpCustomizer;
    }

    public boolean getEnableDetailOutput() {
        return enableDetailOutput;
    }

    public void setEnableDetailOutput(boolean enableDetailOutput) {
        this.enableDetailOutput = enableDetailOutput;
    }

    public boolean getCheckVersion() {
        return checkVersion;
    }

    public void setCheckVersion(boolean checkVersion) {
        this.checkVersion = checkVersion;
    }

    public boolean getSchemaCreateOnly() {
        return schemaCreateOnly;
    }

    public void setSchemaCreateOnly(boolean schemaCreateOnly) {
        this.schemaCreateOnly = schemaCreateOnly;
    }

    public boolean getSchemaClearLocalizedData() {
        return schemaClearLocalizedData;
    }

    public void setSchemaClearLocalizedData(boolean schemaClearLocalizedData) {
        this.schemaClearLocalizedData = schemaClearLocalizedData;
    }

    public boolean getSaveAsDraft() {
        return saveAsDraft;
    }

    public void setSaveAsDraft(boolean saveAsDraft) {
        this.saveAsDraft = saveAsDraft;
    }

    public boolean getImportVersionState() {
        return importVersionState;
    }

    public void setImportVersionState(boolean importVersionState) {
        this.importVersionState = importVersionState;
    }

    public boolean getUnretire() {
        return unretire;
    }

    public void setUnretire(boolean unretire) {
        this.unretire = unretire;
    }

    public String getExcludeFilesPattern() {
        return excludeFilesPattern;
    }

    public void setExcludeFilesPattern(String excludeFilesPattern) {
        this.excludeFilesPattern = excludeFilesPattern;
    }

    public String getChangeComment() {
        return changeComment;
    }

    public void setChangeComment(String changeComment) {
        this.changeComment = changeComment;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(ChangeType changeType) {
        this.changeType = changeType;
    }

    public boolean getStoreReferenceLanguage() {
        return storeReferenceLanguage;
    }

    public void setStoreReferenceLanguage(boolean storeReferenceLanguage) {
        this.storeReferenceLanguage = storeReferenceLanguage;
    }
}
