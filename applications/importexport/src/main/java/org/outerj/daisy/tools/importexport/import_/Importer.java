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
package org.outerj.daisy.tools.importexport.import_;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.DocumentSetHelper;
import org.outerj.daisy.tools.importexport.import_.collections.CollectionImporter;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentLoader;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentLoaderFactory;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.namespaces.NamespaceImporter;
import org.outerj.daisy.tools.importexport.import_.retireddocs.RetiredDocumentsImporter;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaImporter;
import org.outerj.daisy.tools.importexport.import_.variants.VariantImporter;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMeta;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMetaDexmlizer;

/**
 * This is the core of the import tool, use this class to programatically
 * perform an export.
 */
public class Importer {
    private ImportFile importFile;
    private DocumentSet documentSet;
    private Set<ImpExpVariantKey> importSubSet;
    private ImportListener listener;
    private Repository repository;
    private ImportOptions options;
    private ImpExpMeta meta;

    private static Pattern VARIANT_DIR_NAME_PATTERN = Pattern.compile("^([^~]+)~([^~]+)$");

    /**
     * Entry point for doing an import.
     */
    public static void run(ImportFile importFile, Repository repository, ImportOptions options, ImportListener listener) throws Exception {
        run(importFile, null, repository, options, listener);
    }

    /**
     * Entry point for doing an import.
     *
     * @param documentSet specifies a subset of documents to be imported
     */
    public static void run(ImportFile importFile, DocumentSet documentSet, Repository repository, ImportOptions options, ImportListener listener) throws Exception {
        if (importFile == null)
            throw new IllegalArgumentException("Null argument: importFile");
        if (repository == null)
            throw new IllegalArgumentException("Null argument: repository");
        if (options == null)
            throw new IllegalArgumentException("Null argument: options");
        if (listener == null)
            throw new IllegalArgumentException("Null argument: listener");

        new Importer(importFile, documentSet, repository, options, listener).run();
    }

    private Importer(ImportFile importFile, DocumentSet documentSet, Repository repository, ImportOptions options,
            ImportListener listener) {
        this.importFile = importFile;
        this.documentSet = documentSet;
        this.listener = listener;
        this.repository = repository;
        this.options = options;
    }

    private void run() throws Exception {
        readMeta(importFile);
        checkInterrupted();

        NamespaceImporter.run(importFile, listener, repository);
        checkInterrupted();

        CollectionImporter.run(importFile, listener, repository);
        checkInterrupted();

        VariantImporter.run(importFile, listener, repository);
        checkInterrupted();

        SchemaImporter.run(importFile, options, listener, repository);
        checkInterrupted();

        determineDocumentSubSet();
        checkInterrupted();

        importDocuments();
        checkInterrupted();

        importRetiredDocuments();
    }

    private void determineDocumentSubSet() throws Exception {
        // Determine subset of docs to import, if any
        if (documentSet != null) {
            try {
                importSubSet = DocumentSetHelper.toImpExpVariantKeys(documentSet.getDocuments(), repository);
                documentSet = null;
            } catch (Exception e) {
                throw new ImportExportException("Error getting subset of documents to imports.", e);
            }
        }
    }

    private void importDocuments() throws Exception {
        listener.startActivity("Importing documents.");

        if (!importFile.exists("documents")) {
            listener.info("Import does not contain a document directory, will skip document import.");
            return;
        }

        DocumentLoader documentLoader = null;
        if (options.getCheckVersion()) {
            String exportDaisyServerVersion = meta.getDaisyServerVersion();
            if (exportDaisyServerVersion != null) {
                documentLoader = DocumentLoaderFactory.getDocumentLoader(exportDaisyServerVersion);
            }
        }
        if (documentLoader == null)
            documentLoader = DocumentLoaderFactory.getDocumentLoader();

        // First search for all documents to import
        listener.info("Determing list of documents to import.");
        Map<ImpExpVariantKey, DocumentToImport> documentsToImportMap = new HashMap<ImpExpVariantKey, DocumentToImport>();
        ImportFileEntry[] docEntries = importFile.getPath("documents").getChildren();

        Pattern excludePattern;
        try {
            excludePattern = Pattern.compile(options.getExcludeFilesPattern());
        } catch (PatternSyntaxException e) {
            throw new ImportExportException("Error in exclude file pattern: " + options.getExcludeFilesPattern(), e);
        }

        for (ImportFileEntry entry : docEntries) {
            String documentId = entry.getName();

            Matcher docExcludeMatcher = excludePattern.matcher(entry.getName());
            if (docExcludeMatcher.matches())
                continue;

            ImportFileEntry[] variantEntries = entry.getChildren();
            for (ImportFileEntry variantEntry : variantEntries) {
                checkInterrupted();
                Matcher variantExcludeMatcher = excludePattern.matcher(variantEntry.getName());
                Matcher matcher = VARIANT_DIR_NAME_PATTERN.matcher(variantEntry.getName());
                if (!variantEntry.isDirectory()) {
                    // a normal file: silently skip it
                } else if (variantExcludeMatcher.matches()) {
                    // matches file exclusion pattern, silently skip it
                } else if (matcher.matches()) {
                    String branch = matcher.group(1);
                    String language = matcher.group(2);
                    ImpExpVariantKey key = new ImpExpVariantKey(documentId, branch, language);
                    if (importSubSet == null || importSubSet.contains(key))
                        documentsToImportMap.put(key, new DocumentToImport(documentId, branch, language, variantEntry));
                } else {
                    listener.info("Encountered a directory that does not match the naming convention (will skip it): " + variantEntry.getPath());
                }
            }
        }
        
        List<DocumentToImport> documentsToImport = null;
        if (importSubSet != null) {
            documentsToImport = new ArrayList<DocumentToImport>(documentsToImportMap.size());
            for (ImpExpVariantKey key: importSubSet) {
                documentsToImport.add(documentsToImportMap.get(key));
            }
        } else {
            documentsToImport = new ArrayList<DocumentToImport>(documentsToImportMap.values());
        }
        
        // Start the actual import
        listener.info("There are " + documentsToImport.size() + " document/s to import.");
        listener.startDocumentProgress(documentsToImport.size());
        try {
            for (int i = 0; i < documentsToImport.size(); i++) {
                checkInterrupted();
                listener.updateDocumentProgress(i);
                DocumentToImport doc = documentsToImport.get(i);
                importDocument(doc, documentLoader);
            }
        } finally {
            listener.endDocumentProgress();
        }
    }

    private void importDocument(DocumentToImport doc, DocumentLoader documentLoader) throws Exception {
        String documentId = doc.getDocumentId();
        String branch = doc.getBranch();
        String language = doc.getLanguage();
        ImpExpVariantKey variantKey = new ImpExpVariantKey(documentId, branch, language);

        DocumentImportResult result = null;
        boolean gotError = false;
        try {
            result = documentLoader.run(documentId, branch, language, doc.getEntry(), repository, options, listener);
        } catch (Throwable throwable) {
            gotError = true;
            if (throwable instanceof AccessException) {
                listener.permissionDenied(variantKey, (AccessException)throwable);
            } else if (throwable instanceof DocumentLockedException) {
                listener.lockedDocument(variantKey, (DocumentLockedException)throwable);
            } else {
                listener.failed(variantKey, throwable);
            }
        }

        if (!gotError) {
            listener.success(variantKey, result);
        }
    }

    private void importRetiredDocuments() throws Exception {
        listener.startActivity("Importing retired documents");

        if (importFile.exists("info/retired.xml")) {
            RetiredDocumentsImporter.run(importFile, importSubSet, listener, repository);
        } else {
            listener.info("No info/retired.xml found.");
        }
    }

    static class DocumentToImport {
        String documentId;
        String branch;
        String language;
        ImportFileEntry entry;

        public DocumentToImport(String documentId, String branch, String language, ImportFileEntry entry) {
            this.documentId = documentId;
            this.branch = branch;
            this.language = language;
            this.entry = entry;
        }

        public String getDocumentId() {
            return documentId;
        }

        public String getBranch() {
            return branch;
        }

        public String getLanguage() {
            return language;
        }

        public ImportFileEntry getEntry() {
            return entry;
        }
    }

    private void readMeta(ImportFile importFile) throws Exception {
        String META_FILE_PATH  = "info/meta.xml";

        if (!importFile.exists(META_FILE_PATH)) {
            listener.info("No " + META_FILE_PATH + " found.");
            this.meta = new ImpExpMeta();
            return;
        }

        InputStream is = null;
        try {
            is = importFile.getPath(META_FILE_PATH).getInputStream();
            this.meta = ImpExpMetaDexmlizer.fromXml(is);
        } finally {
            if (is != null)
                is.close();
        }
    }

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            throw new ImportExportException("Import was interrupted on user's request.");
        }
    }
}
