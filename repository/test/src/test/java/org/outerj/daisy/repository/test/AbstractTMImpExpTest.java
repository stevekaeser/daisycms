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
import org.outerj.daisy.repository.schema.*;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.tools.importexport.export.tm.TMExporter;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.export.fs.LocalExportFile;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.config.ExportOptionsFactory;
import org.outerj.daisy.tools.importexport.export.BaseExportListener;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.QueryDocumentSet;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.tools.importexport.import_.tm.TMImporter;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.local.LocalImportFile;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptionsFactory;
import org.outerj.daisy.tools.importexport.import_.BaseImportListener;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;
import org.outerj.daisy.htmlcleaner.HtmlCleanerFactory;
import org.outerj.daisy.xmlutil.SimpleNamespaceContext;
import org.outerj.daisy.xmlutil.LocalXPathFactory;
import org.outerj.daisy.xmlutil.LocalTransformerFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.util.Date;

/**
 * Testcase for translation management export-import.
 */
public abstract class AbstractTMImpExpTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    public void testImpExp() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);


        //
        // Init repo state
        //

        // Create languages
        Language enLang = repository.getVariantManager().createLanguage("en");
        enLang.save();

        Language nlLang = repository.getVariantManager().createLanguage("nl");
        nlLang.save();

        // Create schema
        RepositorySchema schema = repository.getRepositorySchema();

        PartType htmlContent = schema.createPartType("HtmlContent", "text/xml");
        htmlContent.setDaisyHtml(true);
        htmlContent.save();

        PartType binaryContent = schema.createPartType("BinaryContent", "");
        binaryContent.save();

        FieldType stringField = schema.createFieldType("StringField", ValueType.STRING);
        stringField.save();

        FieldType mvStringField = schema.createFieldType("MVStringField", ValueType.STRING, true);
        mvStringField.save();

        FieldType stringWithSelListField = schema.createFieldType("StringWithSelList", ValueType.STRING);
        StaticSelectionList list = stringWithSelListField.createStaticSelectionList();
        list.createItem("foo");
        list.createItem("bar");
        stringWithSelListField.save();

        FieldType longField = schema.createFieldType("LongField", ValueType.LONG);
        longField.save();

        FieldType hierStringField = schema.createFieldType("HierStringField", ValueType.STRING, false, true);
        hierStringField.save();

        DocumentType docType = schema.createDocumentType("Basic");
        docType.addPartType(htmlContent, false);
        docType.addPartType(binaryContent, false);
        docType.addFieldType(stringField, false);
        docType.addFieldType(mvStringField, false);
        docType.addFieldType(stringWithSelListField, false);
        docType.addFieldType(longField, false);
        docType.addFieldType(hierStringField, false);
        docType.save();

        DocumentType docType2 = schema.createDocumentType("Basic2");
        docType2.addPartType(htmlContent, false);
        docType2.addPartType(binaryContent, false);
        docType2.addFieldType(stringField, false);
        docType2.addFieldType(mvStringField, false);
        docType2.addFieldType(stringWithSelListField, false);
        docType2.addFieldType(longField, false);
        docType2.addFieldType(hierStringField, false);
        docType2.save();

        // Create collections
        DocumentCollection collection1 = repository.getCollectionManager().createCollection("Collection 1");
        collection1.save();
        DocumentCollection collection2 = repository.getCollectionManager().createCollection("Collection 2");
        collection2.save();

        // Create documents

        // a document with a bit of everything
        Document doc1 = repository.createDocument("Document 1", "Basic", "main", "en");
        doc1.setField("StringField", "My string field");
        doc1.setField("MVStringField", new String[] {"val1", "val2"});
        doc1.setField("StringWithSelList", "foo");
        doc1.setField("LongField", new Long(55));
        doc1.setField("HierStringField", new HierarchyPath(new String[] {"a", "b", "c"}));
        doc1.setPart("HtmlContent", "text/xml", "<html><body><p>Hello</p></body></html>".getBytes("UTF-8"));
        doc1.setPart("BinaryContent", "application/octect-stream", "abc".getBytes());
        doc1.addLink("Link 1", "http://www.daisycms.org");
        doc1.addLink("Link 2", "http://www.outerthought.org");
        doc1.save();

        // a document with an existing translated version
        Document doc2 = repository.createDocument("Document 2", "Basic", "main", "en");
        doc2.setField("StringField", "hello");
        doc2.setField("LongField", new Long(60));
        doc2.addToCollection(collection1);
        doc2.save();

        Document doc2Nl = repository.createVariant(doc2.getId(), doc2.getBranchId(), enLang.getId(), -1, doc2.getBranchId(), nlLang.getId(), false);
        doc2Nl.setField("StringField", "old hello");
        doc2Nl.setField("LongField", new Long(59));
        doc2Nl.addToCollection(collection2);
        doc2Nl.save();

        doc2.changeDocumentType("Basic2");
        doc2.save();

        // a document with an existing translated version which should be left unchanged
        Document doc3 = repository.createDocument("Document 3", "Basic", "main", "en");
        doc3.setField("StringField", "value");
        doc3.save();

        Document doc3Nl = repository.createVariant(doc3.getId(), doc3.getBranchId(), enLang.getId(), -1, doc2.getBranchId(), nlLang.getId(), false);
        doc3Nl.setField("StringField", "value");
        doc3Nl.save();

        final int DOCUMENT_COUNT = 3;

        //
        // Common config
        //

        String tmpDir = System.getProperty("java.io.tmpdir");
        File expDir = new File(tmpDir, "daisy-tmimpexp-test");
        if (expDir.exists())
            deleteDirectory(expDir);

        TMConfig tmConfig = new TMConfig();
        tmConfig.addLangIndepField("HierStringField");
        tmConfig.addLangIndepField("Basic", "LongField");
        tmConfig.addLangIndepPart("BinaryContent");

        //
        // Export
        //
        DocumentSet documentSet = new QueryDocumentSet("select id where language = 'en'", repository);
        ExportFile exportFile = new LocalExportFile(expDir);
        Date exportTime = new Date();
        ExportOptions exportOptions = ExportOptionsFactory.getDefaultExportOptions();
        MyExportListener listener = new MyExportListener();

        TMExporter.run(documentSet, exportFile, exportTime, repository, exportOptions, listener, tmConfig);

        assertEquals(DOCUMENT_COUNT, listener.getSucceeded().size());

        //
        // Modify export data
        //
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setNamespaceAware(true);

        // Document 1
        {
            File doc1ExpFile = new File(expDir, doc1.getId() + "~main.xml");
            org.w3c.dom.Document doc1Doc = docBuilderFactory.newDocumentBuilder().parse(doc1ExpFile);

            XPath xpath = LocalXPathFactory.get().newXPath();
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("ie", "http://outerx.org/daisy/1.0#tm-impexp");
            xpath.setNamespaceContext(nsContext);

            Element stringFieldEl = (Element)xpath.evaluate("/ie:document/ie:field[@name='StringField']", doc1Doc, XPathConstants.NODE);
            setElementText(stringFieldEl, "translated value");

            Element helloEl = (Element)xpath.evaluate("/ie:document/ie:part[@name='HtmlContent']//p[. = 'Hello']", doc1Doc, XPathConstants.NODE);
            setElementText(helloEl, "Hallo");

            saveDocument(doc1Doc, doc1ExpFile);
        }

        // Document 2
        {
            File doc2ExpFile = new File(expDir, doc2.getId() + "~main.xml");
            org.w3c.dom.Document doc2Doc = docBuilderFactory.newDocumentBuilder().parse(doc2ExpFile);

            XPath xpath = LocalXPathFactory.get().newXPath();
            SimpleNamespaceContext nsContext = new SimpleNamespaceContext();
            nsContext.addPrefix("ie", "http://outerx.org/daisy/1.0#tm-impexp");
            xpath.setNamespaceContext(nsContext);

            Element stringFieldEl = (Element)xpath.evaluate("/ie:document/ie:title", doc2Doc, XPathConstants.NODE);
            setElementText(stringFieldEl, "Document 2 translated");


            saveDocument(doc2Doc, doc2ExpFile);
        }

        //
        // Import
        //
        ImportFile importFile = new LocalImportFile(expDir);
        ImportOptions importOptions = ImportOptionsFactory.getDefaultImportOptions();
        MyImportListener importListener = new MyImportListener();
        assertNotNull(getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/repository/test/resources/htmlcleaner.xml"));
        HtmlCleanerTemplate htmlCleanerTempate = new HtmlCleanerFactory().buildTemplate(new InputSource(getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/repository/test/resources/htmlcleaner.xml")));

        TMImporter.run(importFile, null, repository, importOptions, importListener, tmConfig, "nl", htmlCleanerTempate);

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
        assertEquals(DOCUMENT_COUNT, importListener.getSucceeded().size());

        //
        // Post-import tests
        //
        {
            // Test document 1
            {
                Document transDoc = repository.getDocument(doc1.getId(), "main", "nl", false);

                // language independent fields should be there...
                assertTrue(transDoc.hasField("HierStringField"));
                assertTrue(transDoc.hasField("LongField"));
                assertTrue(transDoc.hasPart("BinaryContent"));
                // but other not-exported non-language independent fields not...
                assertFalse(transDoc.hasField("StringWithSelList"));

                assertEquals("translated value", transDoc.getField("StringField").getValue());
                assertEquals("val2", ((Object[])transDoc.getField("MVStringField").getValue())[1]);

                assertTrue(new String(transDoc.getPart("HtmlContent").getData()).indexOf("Hallo") > -1);

                VersionKey syncedWith = transDoc.getLastVersion().getSyncedWith();
                assertNotNull(syncedWith);
                assertEquals(enLang.getId(), syncedWith.getLanguageId());
                assertEquals(1, syncedWith.getVersionId());

                Links links = transDoc.getLinks();
                assertEquals("Link 1", links.getArray()[0].getTitle());
                assertEquals("http://www.daisycms.org", links.getArray()[0].getTarget());
                assertEquals("Link 2", links.getArray()[1].getTitle());
            }

            // Test document 2
            {
                Document transDoc = repository.getDocument(doc2.getId(), "main", "nl", false);

                assertEquals("Document 2 translated", transDoc.getDocumentName());
                assertEquals(59l, transDoc.getField("LongField").getValue()); // should have stayed 59 since LongField is only language-independent for the "Basic" document type
                assertEquals("hello", transDoc.getField("StringField").getValue());

                assertTrue(transDoc.inCollection(collection1));
                assertFalse(transDoc.inCollection(collection2));

                // test document type change was synced
                assertEquals(docType2.getId(), transDoc.getDocumentTypeId());

                VersionKey syncedWith = transDoc.getLastVersion().getSyncedWith();
                assertNotNull(syncedWith);
                assertEquals(enLang.getId(), syncedWith.getLanguageId());
                assertEquals(1, syncedWith.getVersionId());
            }

            // Test document 3
            {
                Document transDoc = repository.getDocument(doc3.getId(), "main", "nl", false);
                assertEquals(1, transDoc.getLastVersionId());
            }
        }

        //
        // Perform import a second time, no changes should happen now
        //
        importListener = new MyImportListener();
        TMImporter.run(importFile, null, repository, importOptions, importListener, tmConfig, "nl", htmlCleanerTempate);

        assertEquals(DOCUMENT_COUNT, importListener.getSucceeded().size());
        for (BaseImportListener.SuccessInfo successInfo : importListener.getSucceeded()) {
            assertEquals(DocumentImportResult.NO_UPDATED_NEEDED, successInfo.getResult());
        }

        //
        // Unset the 'synced with' of a version, it should be updated on import, even if
        // no new version has been created
        //
        {
            Document transDoc = repository.getDocument(doc2.getId(), "main", "nl", true);
            Version version = transDoc.getLastVersion();
            version.setSyncedWith(null);
            version.save();

            importListener = new MyImportListener();
            TMImporter.run(importFile, null, repository, importOptions, importListener, tmConfig, "nl", htmlCleanerTempate);

            assertEquals(DOCUMENT_COUNT, importListener.getSucceeded().size());
            for (BaseImportListener.SuccessInfo successInfo : importListener.getSucceeded()) {
                if (successInfo.getVariantKey().getDocumentId().equals(doc2.getId()))
                    assertEquals(DocumentImportResult.UPDATED, successInfo.getResult());
                else
                    assertEquals(DocumentImportResult.NO_UPDATED_NEEDED, successInfo.getResult());
            }

            transDoc = repository.getDocument(doc2.getId(), "main", "nl", true);
            Version newVersion = transDoc.getLastVersion();
            // the import should not have created a new version
            assertEquals(version.getId(), newVersion.getId());
            // but should have set the synced with
            assertNotNull(newVersion.getSyncedWith());
        }

        //
        // Cleanup
        //
        deleteDirectory(expDir);
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

    private void saveDocument(org.w3c.dom.Document document, File file) throws TransformerException {
        Transformer transformer = LocalTransformerFactory.get().newTransformer();
        transformer.transform(new DOMSource(document), new StreamResult(file));
    }

    private void setElementText(Element element, String value) {
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            element.removeChild(nodeList.item(i));
        }

        element.appendChild(element.getOwnerDocument().createTextNode(value));
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
            return null;
        }

        public boolean isInterrupted() {
            return false;
        }
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;
}
