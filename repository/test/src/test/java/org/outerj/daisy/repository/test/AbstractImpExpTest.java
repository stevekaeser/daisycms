/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.tools.importexport.import_.BaseImportListener;
import org.outerj.daisy.tools.importexport.import_.Importer;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptionsFactory;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.local.LocalImportFile;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.BaseSchemaLoadListener;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.QueryDocumentSet;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.export.fs.LocalExportFile;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.config.ExportOptionsFactory;
import org.outerj.daisy.tools.importexport.export.BaseExportListener;
import org.outerj.daisy.tools.importexport.export.Exporter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.File;
import java.util.Date;

/**
 * Testcase for the import/export tools.
 */
public abstract class AbstractImpExpTest extends AbstractDaisyTestCase {
    private RepositoryManager repositoryManager;
    private Repository repository;

    protected boolean resetDataStores() {
        return true;
    }

    private void initRepositoryFields() throws Exception {
        repositoryManager = getRepositoryManager();
        repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
    }

    public void testImpExp() throws Exception {
        initRepositoryFields();


        //
        // Common config
        //

        String tmpDir = System.getProperty("java.io.tmpdir");
        File expDir = new File(tmpDir, "daisy-impexp-test");
        if (expDir.exists())
            deleteDirectory(expDir);

        //
        // Test export / import with empty repository
        //
        {
            // Export
            DocumentSet documentSet = new QueryDocumentSet("select id where true", repository);
            ExportFile exportFile = new LocalExportFile(expDir);
            Date exportTime = new Date();
            ExportOptions exportOptions = ExportOptionsFactory.getDefaultExportOptions();
            MyExportListener listener = new MyExportListener();

            Exporter.run(documentSet, exportFile, exportTime, repository, exportOptions, listener);
            assertEquals(0, listener.getSucceeded().size());

            // Import
            ImportFile importFile = new LocalImportFile(expDir);
            ImportOptions importOptions = ImportOptionsFactory.getDefaultImportOptions();
            MyImportListener importListener = new MyImportListener();

            Importer.run(importFile, null, repository, importOptions, importListener);

            // Normally there are no failures, but printing failure info is useful for debugging
            System.out.println("Failed because access denied: " + importListener.getFailedBecauseAccessDenied().size());
            System.out.println("Failed because locked: " + importListener.getFailedBecauseLockedDocument().size());
            for (BaseImportListener.FailureInfo failureInfo : importListener.getFailedDocuments()) {
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("Failure importing " + failureInfo.getVariantKey());
                System.out.println(failureInfo.getErrorDescription());
                System.out.println(failureInfo.getStackTrace());
                System.out.println("---------------------------------------------------------------------------");
            }
            assertEquals(0, importListener.getSucceeded().size());
        }

        //
        // Test export / import with non-empty repository
        // Before import, the repository is reset
        //
        {
            // Create some content
            RepositorySchema schema = repository.getRepositorySchema();

            FieldType stringField = schema.createFieldType("StringField", ValueType.STRING);
            stringField.save();

            PartType partType = schema.createPartType("BinaryContent", "");
            partType.save();

            DocumentType docType = schema.createDocumentType("Basic");
            docType.addFieldType(stringField, false);
            docType.addPartType(partType, false);
            docType.save();

            Document document = repository.createDocument("Document 1", "Basic", "main", "default");
            document.setField("StringField", "some value");
            document.setPart("BinaryContent", "application/octet-stream", "abc".getBytes("UTF-8"));
            document.save();

            // Export
            DocumentSet documentSet = new QueryDocumentSet("select id where true", repository);
            ExportFile exportFile = new LocalExportFile(expDir);
            Date exportTime = new Date();
            ExportOptions exportOptions = ExportOptionsFactory.getDefaultExportOptions();
            MyExportListener listener = new MyExportListener();

            Exporter.run(documentSet, exportFile, exportTime, repository, exportOptions, listener);
            assertEquals(1, listener.getSucceeded().size());


            // Clean repository, use another default namespace
            restartRepository(true, "FOOBAR");
            initRepositoryFields();

            // Import
            ImportFile importFile = new LocalImportFile(expDir);
            ImportOptions importOptions = ImportOptionsFactory.getDefaultImportOptions();
            MyImportListener importListener = new MyImportListener();

            Importer.run(importFile, null, repository, importOptions, importListener);

            // Normally there are no failures, but printing failure info is useful for debugging
            System.out.println("Failed because access denied: " + importListener.getFailedBecauseAccessDenied().size());
            System.out.println("Failed because locked: " + importListener.getFailedBecauseLockedDocument().size());
            for (BaseImportListener.FailureInfo failureInfo : importListener.getFailedDocuments()) {
                System.out.println("---------------------------------------------------------------------------");
                System.out.println("Failure importing " + failureInfo.getVariantKey());
                System.out.println(failureInfo.getErrorDescription());
                System.out.println(failureInfo.getStackTrace());
                System.out.println("---------------------------------------------------------------------------");
            }
            assertEquals(1, importListener.getSucceeded().size());
        }

        // TODO the testcase should test much more behavior:
        //         - test for conflicting namespaces
        //         - test fields with all datatypes and multivalue/hierarchical
        //         - test updates
        //         - test error handling
        //         - test various options
        //         - ...
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                deleteDirectory(file);
            } else {
                file.delete();
            }
        }
        dir.delete();
    }

    private static class MyExportListener extends BaseExportListener {

        protected boolean includeFullStackTracesOfFailures() {
            return true;
        }

        protected String getStackTraceDisabledMessage() {
            return null;
        }

        public void info(String message) {
        }

        public void startDocumentProgress(int total) {
        }

        public void updateDocumentProgress(int current) {
        }

        public void endDocumentProgress() {
        }

        public boolean isInterrupted() {
            return false;
        }
    }

    private static class MyImportListener extends BaseImportListener {
        private SchemaLoadListener schemaListener;

        public MyImportListener() {
            schemaListener = new BaseSchemaLoadListener() {
                public boolean isInterrupted() {
                    return false;
                }
            };
        }
        protected boolean includeFullStackTracesOfFailures() {
            return true;
        }

        protected String getStackTraceDisabledMessage() {
            return null;
        }

        public void generateSchemaSaxFragment(ContentHandler contentHandler) throws SAXException {
        }

        public void startActivity(String name) {
        }

        public void info(String message) {
        }

        public void debug(String message) {
        }

        public void startDocumentProgress(int total) {
        }

        public void updateDocumentProgress(int current) {
        }

        public void endDocumentProgress() {
        }

        public SchemaLoadListener getSchemaListener() {
            return schemaListener;
        }

        public boolean isInterrupted() {
            return false;
        }
    }


    protected abstract RepositoryManager getRepositoryManager() throws Exception;
}
