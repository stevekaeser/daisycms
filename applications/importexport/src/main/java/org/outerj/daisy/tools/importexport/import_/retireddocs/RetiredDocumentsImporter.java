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
package org.outerj.daisy.tools.importexport.import_.retireddocs;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.xml.parsers.SAXParser;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class RetiredDocumentsImporter {
    private ImportFile importFile;
    private ImportListener listener;
    private Repository repository;
    private Set importSubSet;
    private static final String RETIRED_FILE_PATH = "info/retired.xml";

    public static void run(ImportFile importFile, Set importSubSet, ImportListener listener, Repository repository) throws Exception {
        new RetiredDocumentsImporter(importFile, importSubSet, listener, repository).run();
    }

    private RetiredDocumentsImporter(ImportFile importFile, Set importSubSet, ImportListener listener, Repository repository) {
        this.importFile = importFile;
        this.listener = listener;
        this.repository = repository;
        this.importSubSet = importSubSet;
    }


    private void run() throws Exception {
        listener.info("Reading " + RETIRED_FILE_PATH);

        List<ImpExpVariantKey> retiredDocs;
        InputStream is = null;
        try {
            is = importFile.getPath(RETIRED_FILE_PATH).getInputStream();
            RetiredDocsHandler retiredDocsHandler = new RetiredDocsHandler();
            SAXParser saxParser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            saxParser.getXMLReader().setContentHandler(retiredDocsHandler);
            saxParser.getXMLReader().parse(new InputSource(is));
            retiredDocs = retiredDocsHandler.getList();
        } catch (Throwable e) {
            throw new ImportExportException("Failed reading " + RETIRED_FILE_PATH, e);
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
        }

        listener.info("Will now retire " + retiredDocs.size() + " documents (if needed).");

        for (ImpExpVariantKey variantKey : retiredDocs) {
            boolean gotError = false;
            DocumentImportResult result = null;
            try {
                Document document = repository.getDocument(variantKey.getDocumentId(), variantKey.getBranch(), variantKey.getLanguage(), true);
                if (!document.isRetired()) {
                    document.setRetired(true);
                    document.save(false);
                    result = DocumentImportResult.RETIRED;
                } else {
                    result = DocumentImportResult.NO_RETIRE_NEEDED;
                }
            } catch (Throwable throwable) {
                gotError = true;
                if (throwable instanceof DocumentNotFoundException || throwable instanceof DocumentVariantNotFoundException)
                {
                    // the document isn't there -- don't consider this an error
                    listener.success(variantKey, DocumentImportResult.NOT_RETIRED_NONEXISTENT);
                } else if (throwable instanceof AccessException) {
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
    }

    class RetiredDocsHandler extends DefaultHandler {
        List<ImpExpVariantKey> retiredDocs = new ArrayList<ImpExpVariantKey>();

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals("") && localName.equals("document")) {
                String id = attributes.getValue("id");
                if (id == null)
                    return;

                String branch = attributes.getValue("branch");
                String language = attributes.getValue("language");

                if (branch == null)
                    branch = Branch.MAIN_BRANCH_NAME;
                if (language == null)
                    language = Language.DEFAULT_LANGUAGE_NAME;

                ImpExpVariantKey key = new ImpExpVariantKey(id, branch, language);
                if (importSubSet == null || importSubSet.contains(key))
                    retiredDocs.add(key);
            }
        }

        public List<ImpExpVariantKey> getList() {
            return retiredDocs;
        }
    }
}
