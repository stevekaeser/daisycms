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

import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.documents.ImportPartDataSource;
import org.outerj.daisy.tools.importexport.import_.documents.BaseDocumentLoader;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;
import org.outerj.daisy.tools.importexport.model.document.ImpExpLink;
import org.outerj.daisy.tools.importexport.model.document.ImpExpField;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.util.ObjectUtils;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;

import java.io.InputStream;
import java.util.Set;
import java.util.Arrays;

public class TMDocumentLoader {
    private Repository repository;
    private ImportFileEntry file;
    private TMDocumentDexmlizer.TMDoc tmDoc;
    private ImpExpDocument impExpDoc;
    private String documentId;
    private String branch;
    private String language;
    private Document document;
    private ImportOptions options;
    private ImportListener listener;
    private boolean dontSave = false;
    private boolean anyChanges = false;
    private DocumentImportResult result;
    private TMConfig tmConfig;
    private Document referenceDocument;
    private Version referenceVersion;
    private HtmlCleanerTemplate htmlCleanerTemplate;

    public static DocumentImportResult run(String documentId, String branch, String language, ImportFileEntry fileEntry,
            Repository repository, ImportOptions options, ImportListener listener, TMConfig tmConfig,
            HtmlCleanerTemplate htmlCleanerTemplate) throws Exception {
        return new TMDocumentLoader(documentId, branch, language, fileEntry, repository, options, listener, tmConfig, htmlCleanerTemplate).run();
    }

    private TMDocumentLoader(String documentId, String branch, String language, ImportFileEntry fileEntry,
            Repository repository, ImportOptions options, ImportListener listener, TMConfig tmConfig,
            HtmlCleanerTemplate htmlCleanerTemplate) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.file = fileEntry;
        this.repository = repository;
        this.options = options;
        this.listener = listener;
        this.tmConfig = tmConfig;
        this.htmlCleanerTemplate = htmlCleanerTemplate;
    }

    private DocumentImportResult run() throws Exception {
        try {
            document = repository.getDocument(documentId, branch, language, true);
            if (options.getUpdateDocuments()) {
                result = DocumentImportResult.UPDATED;
                listener.debug("Document " + getDescription() + " already exists in the repository, it will be updated.");
            } else {
                result = DocumentImportResult.UPDATE_SKIPPED;
                listener.debug("Document " + getDescription() + " already exists in the repository, it will be left untouched.");
                dontSave = true;
            }
        } catch (DocumentNotFoundException e) {
            result = DocumentImportResult.CREATE_SKIPPED;
            listener.debug("Document " + documentId + " does not yet exist in the repository, it will not be created.");
            dontSave = true;
        } catch (DocumentVariantNotFoundException e) {
            createVariant();
        }

        if (document != null) {
            document.setDocumentTypeChecksEnabled(options.getDocumentTypeChecksEnabled());
        }

        if (dontSave)
            return getResult();

        parseDocumentXml();
        options.getDocumentImportCustomizer().preImportFilter(impExpDoc);

        // The reference variant is read from the export, rather than the current repository situation.
        // This is because the language-independent things need to be brought up to date with the
        // exported document (for the synced-with pointer to make sense).
        referenceDocument = repository.getDocument(documentId, branch, tmDoc.exportedLanguage, false);
        referenceVersion = referenceDocument.getVersion(tmDoc.exportedVersion);

        syncDocumentType();
        storeName();
        storeAndSyncParts();
        storeAndSyncFields();
        storeLinks();
        syncCollections();

        // Update synced-with pointer
        document.setNewSyncedWith(tmDoc.exportedLanguage, tmDoc.exportedVersion);

        save();

        return getResult();
    }

    protected String getDescription() {
        return documentId + "~" + branch + "~" + language;
    }

    public DocumentImportResult getResult() {
        return result;
    }

    private void parseDocumentXml() throws Exception {
        InputStream is = null;
        try {
            is = file.getInputStream();
            tmDoc = TMDocumentDexmlizer.fromXml(documentId, branch, language, is, repository, htmlCleanerTemplate);
            impExpDoc = tmDoc.impExpDoc;
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    private void createVariant() throws ImportExportException {
        if (options.getCreateVariants()) {
            result = DocumentImportResult.CREATED;
            listener.debug("Document " + documentId + " exists in the repository, but not yet in variant " + branch + "~" + language + ", it will be created.");
        } else {
            result = DocumentImportResult.CREATE_SKIPPED;
            listener.debug("Document " + documentId + " exists in the repository, but not yet in variant " + branch + "~" + language + ", it will not be created.");
            dontSave = true;
            return;
        }
        // We need some variant to start from (is needed for checking access rights), just pick whatever one.
        try {
            AvailableVariant existingVariant = repository.getAvailableVariants(documentId).getArray()[0];
            document = repository.createVariant(documentId, existingVariant.getBranch().getName(),
                    existingVariant.getLanguage().getName(), -1, branch, language, false);
        } catch (RepositoryException e) {
            throw new ImportExportException("Error creating variant " + branch + "~" + language + " on document " + documentId, e);
        }
        anyChanges = true;
    }

    public void save() throws RepositoryException {
        if (dontSave) {
            listener.info("save is called when it shouldn't be called, this is a bug.");
        } else {
            boolean needLastVersionCheck = false;
            if (anyChanges) {
                options.getDocumentImportCustomizer().beforeSaveFilter(document);
                if (options.getImportVersionState() && impExpDoc.getVersionState() != null)
                    document.setNewVersionState(impExpDoc.getVersionState());
                else if (options.getSaveAsDraft())
                    document.setNewVersionState(VersionState.DRAFT);

                if (options.getChangeComment() != null)
                    document.setNewChangeComment(options.getChangeComment());
                if (options.getChangeType() != null)
                    document.setNewChangeType(options.getChangeType());

                long oldLastVersion = document.getLastVersionId();
                document.save(options.getValidateOnSave());

                // check if a new version has been created.
                if (oldLastVersion == document.getLastVersionId())
                    needLastVersionCheck = true;

                listener.debug("Document " + getDescription() + " saved.");
            }

            boolean versionUpdated = false;
            if (!anyChanges || needLastVersionCheck) {
                // No new version has been created, update last version
                Version version = document.getLastVersion();
                if (version.getSyncedWith() == null
                        || version.getSyncedWith().getLanguageId() != referenceDocument.getLanguageId()
                        || version.getSyncedWith().getVersionId() < referenceVersion.getId()) {
                    version.setSyncedWith(referenceDocument.getLanguageId(), referenceVersion.getId());
                    version.save();
                    versionUpdated = true;
                }
            }

            if (!anyChanges && !versionUpdated) {
                result = DocumentImportResult.NO_UPDATED_NEEDED;
                listener.debug("Document " + getDescription() + " did not change, hence is not saved.");
            }
        }
    }

    private void storeName() throws ImportExportException {
        String name = impExpDoc.getName();
        if (!document.getName().equals(name)) {
            document.setName(name);
            anyChanges = true;
        }
    }

    private void storeAndSyncParts() throws Exception {
        ImpExpPart[] parts = impExpDoc.getParts();

        //
        // Update translated parts (this is update/create only, no deletes)
        //
        for (ImpExpPart part : parts) {
            if (!options.getDocumentImportCustomizer().storePart(impExpDoc, document, part.getType().getName()))
                continue;

            long type = part.getType().getId();

            // optionally compare for changes before setting part
            boolean storeData = true;
            Part existingPart = document.hasPart(type) ? document.getPart(type) : null;
            if (existingPart != null &&
                    (options.getMaxSizeForDataCompare() == -1 ||
                            (part.getDataAccess().getSize() < options.getMaxSizeForDataCompare()
                                    && existingPart.getSize() < options.getMaxSizeForDataCompare()))) {
                if (BaseDocumentLoader.isDataEqual(existingPart, part)) {
                    listener.debug("Data for part " + type + " was not changed, will not store new data.");
                    storeData = false;
                }
            }

            if (storeData) {
                document.setPart(type, part.getMimeType(), new ImportPartDataSource(part.getDataAccess()));
                anyChanges = true;
            }
        }

        //
        // Sync cross-variant parts (create, update, delete)
        //
        Set<String> crossLangParts = tmConfig.getLanguageIndependentParts(repository.getRepositorySchema().getDocumentTypeById(referenceDocument.getDocumentTypeId(), false).getName());
        for (String crossLangPart : crossLangParts) {
            PartType partType = repository.getRepositorySchema().getPartTypeByName(crossLangPart, false);

            if (referenceVersion.hasPart(partType.getId())) {
                Part part = referenceVersion.getPart(partType.getId());

                // optionally compare for changes before setting part
                boolean storeData = true;
                Part existingPart = document.hasPart(partType.getId()) ? document.getPart(partType.getId()) : null;
                if (existingPart != null &&
                        (options.getMaxSizeForDataCompare() == -1 ||
                                (part.getSize() < options.getMaxSizeForDataCompare()
                                        && existingPart.getSize() < options.getMaxSizeForDataCompare()))) {
                    if (BaseDocumentLoader.isDataEqual(existingPart, part)) {
                        listener.debug("Data for part " + crossLangPart + " was not changed, will not store new data.");
                        storeData = false;
                    }
                }

                if (storeData) {
                    document.setPart(partType.getId(), part.getMimeType(), new PartPartDataSource(part));
                    anyChanges = true;
                }

                // update filename if necessary
                if (!ObjectUtils.safeEquals(document.getPart(partType.getId()).getFileName(), part.getFileName())) {
                    document.setPartFileName(partType.getId(), part.getFileName());
                    anyChanges = true;
                }

            } else {
                if (document.hasPart(partType.getId())) {
                    document.deletePart(partType.getId());
                    anyChanges = true;
                }
            }
        }
    }

    private void storeAndSyncFields() throws Exception {
        ImpExpField[] fields = impExpDoc.getFields();

        //
        // Update translated fields (create or update, no delete)
        //
        for (ImpExpField field : fields) {
            if (!options.getDocumentImportCustomizer().storeField(impExpDoc, document, field.getType().getName()))
                continue;

            long type = field.getType().getId();
            Object value = field.getValue();

            Field existingField = document.hasField(type) ? document.getField(type) : null;
            if (existingField == null || !fieldValuesEqual(value, existingField.getValue())) {
                document.setField(type, value);
                anyChanges = true;
            }
        }

        //
        // Sync cross-variant fields (create, update, delete)
        //
        Set<String> crossLangFields = tmConfig.getLanguageIndependentFields(repository.getRepositorySchema().getDocumentTypeById(referenceDocument.getDocumentTypeId(), false).getName());
        for (String crossLangField : crossLangFields) {
            FieldType fieldType = repository.getRepositorySchema().getFieldType(crossLangField, false);

            if (referenceVersion.hasField(fieldType.getId())) {
                Field field = referenceVersion.getField(fieldType.getId());

                long type = field.getTypeId();
                Object value = field.getValue();

                // compare for changes before setting field
                Field existingField = document.hasField(type) ? document.getField(type) : null;
                if (existingField == null || !fieldValuesEqual(value, existingField.getValue())) {
                    document.setField(type, value);
                    anyChanges = true;
                }
            } else {
                if (document.hasField(fieldType.getId())) {
                    document.deleteField(fieldType.getId());
                    anyChanges = true;
                }
            }
        }
    }

    private boolean fieldValuesEqual(Object value1, Object value2) {
        if (value1 instanceof Object[]) {
            // if one is an array, they are alwyas both arrays
            Object[] value1Arr = (Object[])value1;
            Object[] value2Arr = (Object[])value2;
            return Arrays.equals(value1Arr, value2Arr);
        } else {
            return value1.equals(value2);
        }
    }

    private void storeLinks() {
        if (!options.getDocumentImportCustomizer().storeLinks(impExpDoc, document))
            return;

        ImpExpLink[] links = impExpDoc.getLinks();
        Link[] existingLinks = document.getLinks().getArray();

        boolean needsUpdating = true;
        if (links.length == existingLinks.length) {
            for (int i = 0; i < links.length; i++) {
                if (!links[i].getTarget().equals(existingLinks[i].getTarget())
                        || !links[i].getTitle().equals(existingLinks[i].getTitle())) {
                    break;
                }
            }
            needsUpdating = false;
        }

        if (!needsUpdating)
            return;

        document.clearLinks();
        for (ImpExpLink link : links) {
            document.addLink(link.getTitle(), link.getTarget());
        }
        anyChanges = true;
    }

    private void syncCollections() throws Exception {
        DocumentCollection[] collections = referenceDocument.getCollections().getArray();
        DocumentCollection[] existingCollections = document.getCollections().getArray();

        boolean needsUpdating = true;
        if (collections.length == existingCollections.length) {
            needsUpdating = false;
            for (DocumentCollection collection : collections) {
                boolean found = false;
                for (DocumentCollection existingCollection : existingCollections) {
                    if (collection.getId() == existingCollection.getId()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    needsUpdating = true;
                    break;
                }
            }
        }

        if (!needsUpdating)
            return;

        document.clearCollections();
        for (DocumentCollection collection : collections) {
            document.addToCollection(collection);
        }
        anyChanges = true;
    }

    private void syncDocumentType() throws Exception {
        long refDocTypeId = referenceDocument.getDocumentTypeId();

        if (document.getDocumentTypeId() != refDocTypeId) {
            document.changeDocumentType(refDocTypeId);
            anyChanges = true;
        }
    }
}
