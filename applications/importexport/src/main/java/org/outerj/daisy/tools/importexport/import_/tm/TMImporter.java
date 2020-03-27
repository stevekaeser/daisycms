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

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileEntry;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.namespaces.NamespaceImporter;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.DocumentSetHelper;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Matcher;

public class TMImporter {
    private ImportFile importFile;
    private DocumentSet documentSet;
    private Set importSubSet;
    private ImportListener listener;
    private Repository repository;
    private ImportOptions options;
    private TMConfig tmConfig;
    private String targetLanguage;
    private HtmlCleanerTemplate htmlCleanerTemplate;

    private static Pattern DOC_NAME_PATTERN = Pattern.compile("^([^~]+)~([^~]+).xml$");

    public static void run(ImportFile importFile, DocumentSet documentSet, Repository repository,
            ImportOptions options, ImportListener listener, TMConfig tmConfig, String targetLanguage,
            HtmlCleanerTemplate htmlCleanerTemplate) throws Exception {
        if (importFile == null)
            throw new IllegalArgumentException("Null argument: importFile");
        if (repository == null)
            throw new IllegalArgumentException("Null argument: repository");
        if (options == null)
            throw new IllegalArgumentException("Null argument: options");
        if (listener == null)
            throw new IllegalArgumentException("Null argument: listener");
        if (targetLanguage == null)
            throw new IllegalArgumentException("Null argument: targetLanguage");
        if (tmConfig == null)
            throw new IllegalArgumentException("Null argument: tmConfig");

        new TMImporter(importFile, documentSet, repository, options, listener, tmConfig, targetLanguage, htmlCleanerTemplate).run();
    }

    private TMImporter(ImportFile importFile, DocumentSet documentSet, Repository repository, ImportOptions options,
            ImportListener listener, TMConfig tmConfig, String targetLanguage, HtmlCleanerTemplate htmlCleanerTemplate) {
        this.targetLanguage = targetLanguage;
        this.importFile = importFile;
        this.documentSet = documentSet;
        this.listener = listener;
        this.repository = repository;
        this.options = options;
        this.tmConfig = tmConfig;
        this.htmlCleanerTemplate = htmlCleanerTemplate;
    }

    private void run() throws Exception {
        // Make sure target language exists
        try {
            repository.getVariantManager().getLanguage(targetLanguage, false);
        } catch (LanguageNotFoundException e) {
            throw new ImportExportException("The specified target language does not exist: " + targetLanguage, e);
        }

        NamespaceImporter.run(importFile, listener, repository);
        checkInterrupted();

        determineDocumentSubSet();
        checkInterrupted();

        importDocuments();
        checkInterrupted();
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

        // First search for all documents to import
        listener.info("Determing list of documents to import.");
        List<DocumentToImport> documentsToImport = new ArrayList<DocumentToImport>();
        ImportFileEntry[] docEntries = importFile.getRoot().getChildren();

        Pattern excludePattern;
        try {
            excludePattern = Pattern.compile(options.getExcludeFilesPattern());
        } catch (PatternSyntaxException e) {
            throw new ImportExportException("Error in exclude file pattern: " + options.getExcludeFilesPattern(), e);
        }

        for (ImportFileEntry entry : docEntries) {
            checkInterrupted();
            Matcher variantExcludeMatcher = excludePattern.matcher(entry.getName());
            Matcher matcher = DOC_NAME_PATTERN.matcher(entry.getName());
            if (entry.isDirectory()) {
                // a directory: silently skip it
            } else if (variantExcludeMatcher.matches()) {
                // matches file exclusion pattern, silently skip it
            } else if (matcher.matches()) {
                String documentId = matcher.group(1);
                String branch = matcher.group(2);
                ImpExpVariantKey key = new ImpExpVariantKey(documentId, branch, targetLanguage);
                if (importSubSet == null || importSubSet.contains(key))
                    documentsToImport.add(new DocumentToImport(documentId, branch, targetLanguage, entry));
            } else {
                listener.info("Encountered a directory that does not match the naming convention (will skip it): " + entry.getPath());
            }
        }

        // Start the actual import
        listener.info("There are " + documentsToImport.size() + " document/s to import.");
        listener.startDocumentProgress(documentsToImport.size());
        try {
            for (int i = 0; i < documentsToImport.size(); i++) {
                checkInterrupted();
                listener.updateDocumentProgress(i);
                DocumentToImport doc = documentsToImport.get(i);
                importDocument(doc);
            }
        } finally {
            listener.endDocumentProgress();
        }
    }

    private void importDocument(DocumentToImport doc) throws Exception {
        String documentId = doc.getDocumentId();
        String branch = doc.getBranch();
        String language = doc.getLanguage();
        ImpExpVariantKey variantKey = new ImpExpVariantKey(documentId, branch, language);

        DocumentImportResult result = null;
        boolean gotError = false;
        try {
            result = TMDocumentLoader.run(documentId, branch, language, doc.getEntry(), repository, options, listener, tmConfig, htmlCleanerTemplate);
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

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            throw new ImportExportException("Import was interrupted on user's request.");
        }
    }
}
