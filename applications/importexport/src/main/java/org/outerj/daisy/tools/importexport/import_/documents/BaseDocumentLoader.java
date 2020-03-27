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
package org.outerj.daisy.tools.importexport.import_.documents;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.document.*;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.util.ObjectUtils;

import java.util.*;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class BaseDocumentLoader {
    private Repository repository;
    private ImportFileEntry dir;
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


    public static BaseDocumentLoader load(String documentId, String branch, String language,
            ImportFileEntry dir, Repository repository, ImportOptions options, ImportListener listener)
            throws Exception {
        BaseDocumentLoader loader = new BaseDocumentLoader(documentId, branch, language, dir, repository, options, listener);
        loader.run();
        return loader;
    }

    private BaseDocumentLoader(String documentId, String branch, String language, ImportFileEntry dir,
            Repository repository, ImportOptions options, ImportListener listener) {
        this.documentId = documentId;
        this.branch = branch;
        this.language = language;
        this.dir = dir;
        this.repository = repository;
        this.options = options;
        this.listener = listener;
    }

    private void run() throws Exception {
        parseDocumentXml();
        options.getDocumentImportCustomizer().preImportFilter(impExpDoc);

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
            createDocument();
        } catch (DocumentVariantNotFoundException e) {
            createVariant();
        }

        if (document != null) {
            document.setDocumentTypeChecksEnabled(options.getDocumentTypeChecksEnabled());
        }
    }

    private void parseDocumentXml() throws Exception {
        InputStream is = null;
        try {
            is = dir.getChild("document.xml").getInputStream();
            impExpDoc = ImpExpDocumentDexmlizer.fromXml(documentId, branch, language, is, repository, new ImpExpDocumentDexmlizer.DataRefResolver() {
                public InputStream getInputStream(String dataRef) throws Exception {
                    return dir.getChild(dataRef).getInputStream();
                }

                public long getSize(String dataRef) throws Exception {
                    return dir.getChild(dataRef).getSize();
                }
            });
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    private void createDocument() {
        if (options.getCreateDocuments()) {
            result = DocumentImportResult.CREATED;
            listener.debug("Document " + documentId + " does not yet exist in the repository, it will be created.");
        } else {
            result = DocumentImportResult.CREATE_SKIPPED;
            listener.debug("Document " + documentId + " does not yet exist in the repository, it will not be created.");
            dontSave = true;
            return;
        }
        document = repository.createDocument(impExpDoc.getName(), impExpDoc.getType(), branch, language);
        document.setRequestedId(documentId);
        anyChanges = true;
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


    protected String getDescription() {
        return documentId + "~" + branch + "~" + language;
    }

    public void save() throws RepositoryException {
        if (dontSave) {
            listener.info("save is called when it shouldn't be called, this is a bug.");
        } else {
            if (anyChanges) {
                options.getDocumentImportCustomizer().beforeSaveFilter(document);
                if (options.getImportVersionState() && impExpDoc.getVersionState() != null)
                    document.setNewVersionState(impExpDoc.getVersionState());
                else if (options.getSaveAsDraft())
                    document.setNewVersionState(VersionState.DRAFT);
                if (options.getChangeType() != null)
                    document.setNewChangeType(options.getChangeType());
                if (options.getChangeComment() != null)
                    document.setNewChangeComment(options.getChangeComment());
                document.save(options.getValidateOnSave());
                listener.debug("Document " + getDescription() + " saved.");
            } else {
                result = DocumentImportResult.NO_UPDATED_NEEDED;
                listener.debug("Document " + getDescription() + " did not change, hence is not saved.");
            }
        }
    }

    public void storeName() throws ImportExportException {
        String name = impExpDoc.getName();
        if (!document.getName().equals(name)) {
            document.setName(name);
            anyChanges = true;
        }
    }

    public void storeParts() throws Exception {
        Set<String> storedPartTypes = new HashSet<String>();
        ImpExpPart[] parts = impExpDoc.getParts();

        for (ImpExpPart part : parts) {
            storedPartTypes.add(part.getType().getName());

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
                if (isDataEqual(existingPart, part)) {
                    listener.debug("Data for part " + type + " was not changed, will not store new data.");
                    storeData = false;
                }
            }

            if (storeData) {
                document.setPart(type, part.getMimeType(), new ImportPartDataSource(part.getDataAccess()));
                anyChanges = true;
            } else {
                // it could be that the mime type was updated
                if (!existingPart.getMimeType().equals(part.getMimeType())) {
                    document.setPartMimeType(type, part.getMimeType());
                    anyChanges = true;
                }
            }

            // update filename if necessary
            if (!ObjectUtils.safeEquals(document.getPart(type).getFileName(), part.getFileName())) {
                document.setPartFileName(type, part.getFileName());
                anyChanges = true;
            }

        }

        // Remove parts that are in the document but not in the import
        Part[] currentParts = document.getParts().getArray();
        for (Part part : currentParts) {
            String typeName = part.getTypeName();
            if (!storedPartTypes.contains(typeName)) {
                if (options.getDocumentImportCustomizer().removePartWhenMissing(impExpDoc, document, typeName)) {
                    document.deletePart(part.getTypeId());
                    anyChanges = true;
                    listener.debug("Removed part " + typeName + " from the document.");
                }
            }
        }
    }

    public void storeFields() throws Exception {
        Set<String> storedFieldTypes = new HashSet<String>();
        ImpExpField[] fields = impExpDoc.getFields();

        for (ImpExpField field : fields) {
            storedFieldTypes.add(field.getType().getName());

            if (!options.getDocumentImportCustomizer().storeField(impExpDoc, document, field.getType().getName()))
                continue;

            long type = field.getType().getId();
            Object value = field.getValue();

            // compare for changes before setting field
            Field existingField = document.hasField(type) ? document.getField(type) : null;
            if (existingField == null || !fieldValuesEqual(value, existingField.getValue())) {
                document.setField(type, value);
                anyChanges = true;
            }
        }

        // Remove fields that are in the document but not in the import
        Field[] currentFields = document.getFields().getArray();
        for (Field field : currentFields) {
            String typeName = field.getTypeName();
            if (!storedFieldTypes.contains(typeName)) {
                if (options.getDocumentImportCustomizer().removeFieldWhenMissing(impExpDoc, document, typeName)) {
                    document.deleteField(field.getTypeId());
                    anyChanges = true;
                    listener.debug("Removed field " + typeName + " from the document.");
                }
            }
        }
    }

    public void storeLinks() {
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

    public void storeCustomFields() {
        if (!options.getDocumentImportCustomizer().storeCustomFields(impExpDoc, document))
            return;

        ImpExpCustomField[] customFields = impExpDoc.getCustomFields();
        Map existingFields = document.getCustomFields();

        boolean needsUpdating = true;
        if (customFields.length == existingFields.size()) {
            for (ImpExpCustomField customField : customFields) {
                if (!customField.getValue().equals(existingFields.get(customField.getName()))) {
                    break;
                }
            }
            needsUpdating = false;
        }

        if (!needsUpdating)
            return;

        document.clearCustomFields();
        for (ImpExpCustomField customField : customFields) {
            document.setCustomField(customField.getName(), customField.getValue());
        }
        anyChanges = true;
    }

    public void storeOwner() throws Exception {
        if (!options.getStoreOwner())
            return;

        if (impExpDoc.getOwner() == null)
            return;

        long ownerId;
        try {
            ownerId = repository.getUserManager().getUserId(impExpDoc.getOwner());
        } catch (UserNotFoundException e) {
            if (options.getFailOnNonExistingOwner())
                throw new ImportExportException("Nonexisting owner " + impExpDoc.getOwner(), e);
            else
                return;
        }

        if (document.getOwner() != ownerId) {
            document.setOwner(ownerId);
            anyChanges = true;
        }
    }

    public void storeReferenceLanguage() throws Exception {
        if (!options.getStoreReferenceLanguage())
            return;

        if (impExpDoc.getReferenceLanguage() == null)
            return;

        long refLangId;
        try {
            refLangId = repository.getVariantManager().getLanguage(impExpDoc.getReferenceLanguage(), false).getId();
        } catch (LanguageNotFoundException e) {
            throw new ImportExportException("Nonexisting language specified as reference language " + impExpDoc.getReferenceLanguage(), e);
        }

        if (document.getReferenceLanguageId() != refLangId) {
            document.setReferenceLanguageId(refLangId);
            anyChanges = true;
        }
    }

    public void storeCollections() throws Exception {
        String[] collectionNames = impExpDoc.getCollections();
        DocumentCollection[] collections = new DocumentCollection[collectionNames.length];

        CollectionManager collectionManager = repository.getCollectionManager();
        for (int i = 0; i < collectionNames.length; i++) {
            try {
                collections[i] = collectionManager.getCollectionByName(collectionNames[i], false);
            } catch (CollectionNotFoundException e) {
                if (!options.getCreateMissingCollections())
                    throw e;
                listener.debug("Document " + getDescription() + " belongs to non-existing collection " + collectionNames[i] + ", will try to create it.");
                // Will fail for non-admin users
                DocumentCollection newCollection;
                try {
                    newCollection = collectionManager.createCollection(collectionNames[i]);
                    newCollection.save();
                } catch (RepositoryException e1) {
                    throw new ImportExportException("Error creating collection " + collectionNames[i], e);
                }
                collections[i] = newCollection;
            }
        }
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

    public void unretire() {
        if (options.getUnretire() && document.isRetired()) {
            document.setRetired(false);
            anyChanges = true;
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

    public static boolean isDataEqual(final Part part1, final Part part2) throws Exception {
        SizeAndInputStreamEnabled p1 = new SizeAndInputStreamEnabled() {

            public long getSize() throws Exception {
                return part1.getSize();
            }

            public InputStream getInputStream() throws Exception {
                return part2.getDataStream();
            }
        };

        SizeAndInputStreamEnabled p2 = new SizeAndInputStreamEnabled() {

            public long getSize() throws Exception {
                return part2.getSize();
            }

            public InputStream getInputStream() throws Exception {
                return part2.getDataStream();
            }
        };

        return isDataEqual(p1, p2);
    }

    public static boolean isDataEqual(final Part part, final ImpExpPart impExpPart) throws Exception {
        SizeAndInputStreamEnabled p1 = new SizeAndInputStreamEnabled() {

            public long getSize() {
                return part.getSize();
            }

            public InputStream getInputStream() throws RepositoryException {
                return part.getDataStream();
            }
        };

        SizeAndInputStreamEnabled p2 = new SizeAndInputStreamEnabled() {

            public long getSize() throws Exception {
                return impExpPart.getDataAccess().getSize();
            }

            public InputStream getInputStream() throws Exception {
                return impExpPart.getDataAccess().getInputStream();
            }
        };

        return isDataEqual(p1, p2);
    }

    private static interface SizeAndInputStreamEnabled {
        long getSize() throws Exception;

        InputStream getInputStream() throws Exception;
    }

    public static boolean isDataEqual(SizeAndInputStreamEnabled part1, SizeAndInputStreamEnabled part2) throws Exception {
        if (part1.getSize() != part2.getSize())
            return false;

        InputStream input1 = null;
        InputStream input2 = null;


        try {
            input1 = part1.getInputStream();
            input2 = part2.getInputStream();

            // This bit of code is copied from Jakarta commons-IO (IOUtils.contentEquals)
            if (!(input1 instanceof BufferedInputStream)) {
                input1 = new BufferedInputStream(input1);
            }
            if (!(input2 instanceof BufferedInputStream)) {
                input2 = new BufferedInputStream(input2);
            }

            int ch = input1.read();
            while (-1 != ch) {
                int ch2 = input2.read();
                if (ch != ch2) {
                    return false;
                }
                ch = input1.read();
            }

            int ch2 = input2.read();
            return (ch2 == -1);
        } catch (Throwable e) {
            throw new ImportExportException("Error comparing old and new part content.", e);
        } finally {
            if (input1 != null)
                try { input1.close(); } catch (Exception e) { /* ignore */ }
            if (input2 != null)
                try { input2.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    public boolean getDontSave() {
        return dontSave;
    }

    public DocumentImportResult getResult() {
        return result;
    }

}
