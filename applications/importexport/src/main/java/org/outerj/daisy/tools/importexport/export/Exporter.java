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
package org.outerj.daisy.tools.importexport.export;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.export.config.DocumentExportCustomizer;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.model.collection.ImpExpCollectionsXmlizer;
import org.outerj.daisy.tools.importexport.model.document.ImpExpDocument;
import org.outerj.daisy.tools.importexport.model.document.ImpExpDocumentFactory;
import org.outerj.daisy.tools.importexport.model.document.ImpExpDocumentXmlizer;
import org.outerj.daisy.tools.importexport.model.document.ImpExpField;
import org.outerj.daisy.tools.importexport.model.document.ImpExpPart;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMeta;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMetaXmlizer;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespaces;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespacesXmlizer;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaFactory;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaXmlizer;
import org.outerj.daisy.tools.importexport.model.variant.ImpExpVariants;
import org.outerj.daisy.tools.importexport.model.variant.ImpExpVariantsXmlizer;
import org.outerj.daisy.tools.importexport.util.XmlProducer;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.util.Constants;

/**
 * This is the core of the export tool, use this class to programatically
 * perform an export.
 *
 * <p>This performs a normal export, for the translation management export,
 * see {@link org.outerj.daisy.tools.importexport.export.tm.TMExporter}.
 */
public class Exporter {
    private DocumentSet documentSet;
    private ExportFile exportFile;
    private Repository repository;
    private ExportOptions options;
    private ExportListener listener;
    private ImpExpNamespaces impExpNamespaces;
    private NamespaceManager namespaceManager;
    private LinkHandler linkHandler;
    private AdminItems adminItems = new AdminItems();
    private ImpExpVariants variants;
    private Date exportTime;
    private List<ImpExpVariantKey> retiredDocuments = new ArrayList<ImpExpVariantKey>();

    /**
     * Perform an export. This is the main API entry point to perform an export.
     *
     * @param documentSet can either be a {@link DocumentSet} or its subclass {@link ExportSet}
     * @param exportTime the time to write in the meta file as the time of this export
     */
    public static void run(DocumentSet documentSet, ExportFile exportFile, Date exportTime,
            Repository repository, ExportOptions options, ExportListener listener) throws Exception {
        new Exporter(documentSet, exportFile, exportTime, repository, options, listener).run();
    }

    private Exporter(DocumentSet documentSet, ExportFile exportFile, Date exportTime,
            Repository repository, ExportOptions options, ExportListener listener) {
        this.documentSet = documentSet;
        this.exportFile = exportFile;
        this.exportTime = exportTime;
        this.repository = repository;
        this.options = options;
        this.listener = listener;
        this.impExpNamespaces = new ImpExpNamespaces();
        this.namespaceManager = repository.getNamespaceManager();
        this.linkHandler = new LinkHandler(this, listener, options, repository);
        this.variants = new ImpExpVariants();
    }

    private void run() throws Exception {
        listener.info("Determing set of documents to export.");
        VariantKey[] variantKeys = documentSet.getDocuments().toArray(new VariantKey[0]);
        checkInterrupted();

        // Write documents
        exportDocuments(variantKeys);

        // Write retired documents
        if (retiredDocuments.size() > 0 && options.getIncludeListOfRetiredDocuments()) {
            try {
                exportRetiredDocuments();
            } catch (Throwable e) {
                throw new ImportExportException("Error exporting list of retired documents.", e);
            }
        }
        checkInterrupted();

        // Add additional things (schema, collections, ...) which are not related to the documents
        addAdditionalItems();
        checkInterrupted();

        // Write namespaces
        try {
            exportNamespaces();
        } catch (Throwable e) {
            throw new ImportExportException("Error exporting namespaces.", e);
        }
        checkInterrupted();

        // Write schema
        try {
            exportSchema();
        } catch (Throwable e) {
            throw new ImportExportException("Error exporting schema.", e);
        }
        checkInterrupted();

        // Write variants
        try {
            exportVariants();
        } catch (Throwable e) {
            throw new ImportExportException("Error exporting variants.", e);
        }
        checkInterrupted();

        // Write collections
        try {
            exportCollections();
        } catch (Throwable e) {
            throw new ImportExportException("Error exporting collections.", e);
        }

        // Write meta
        try {
            exportMeta();
        } catch (Throwable e) {
            throw new ImportExportException("Error writing export metadata.", e);
        }
    }

    private void exportDocuments(VariantKey[] variantKeys) throws Exception {
        listener.info("Exporting " + variantKeys.length + " documents.");
        listener.startDocumentProgress(variantKeys.length);
        try {
            for (int i = 0; i < variantKeys.length; i++) {
                checkInterrupted();
                listener.updateDocumentProgress(i);
                exportDocument(variantKeys[i]);
            }
        } finally {
            listener.endDocumentProgress();
        }
    }

    private void exportDocument(VariantKey variantKey) throws Exception {
        //listener.info("Exporting " + formatVariantKey(variantKey) + " [" + (i + 1) + "/" + variantKeys.length + "]");

        String branch = repository.getVariantManager().getBranch(variantKey.getBranchId(), false).getName();
        String language = repository.getVariantManager().getLanguage(variantKey.getLanguageId(), false).getName();
        ImpExpVariantKey impExpVariantKey = new ImpExpVariantKey(variantKey.getDocumentId(), branch, language);

        try {
            // Get the document
            Document document = repository.getDocument(variantKey, false);

            // Make sure we have this namespace of this document
            needNamespaceOfDocId(document.getId(), true);

            // Make sure we have the branch and language of this document
            needsBranchLanguage(branch, language, true);

            // Check if document is retired
            if (document.isRetired()) {
                retiredDocuments.add(impExpVariantKey);
                listener.skippedBecauseRetired(impExpVariantKey);
                return;
            }

            // Get last or live version according to export options
            Version version;
            if (options.getExportLastVersion())
                version = document.getLastVersion();
            else
                version = document.getLiveVersion();

            if (version == null) {
                listener.skippedBecauseNoLiveVersion(impExpVariantKey);
                return;
            }

            ImpExpDocument impExpDoc = ImpExpDocumentFactory.fromDocument(document,  version, repository);

            // check export version state option
            if (!options.getExportVersionState())
                impExpDoc.setVersionState(null);

            // Filter document
            options.getDocumentExportCustomizer().preExportFilter(impExpDoc);

            // Remove owner if not wanted in export
            if (!options.getExportDocumentOwners())
                impExpDoc.setOwner(null);

            if (!options.getExportReferenceLanguage()) {
                impExpDoc.setReferenceLanguage(null);
            } else {
                if (impExpDoc.getReferenceLanguage() != null)
                needsBranchLanguage(null, impExpDoc.getReferenceLanguage(), true);
            }

            // Extract links
            linkHandler.handleLinks(impExpDoc);

            // Write document
            OutputStream os = null;
            try {
                String basePath = "documents/" + impExpDoc.getId() + "/" + impExpDoc.getBranch() + "~" + impExpDoc.getLanguage();
                os = exportFile.getOutputStream(basePath + "/document.xml");
                MyPartWriter myPartWriter = new MyPartWriter(basePath, exportFile, options.getDocumentExportCustomizer());
                ImpExpDocumentXmlizer.toXml(impExpDoc, os, myPartWriter, repository);
            } finally {
                if (os != null)
                    os.close();
            }

            adminItems.addDocumentType(impExpDoc.getType(), true);
            // A document might contain fields and parts that are not in the document type, therefore assure
            // these get exported too.
            for (ImpExpField field : impExpDoc.getFields()) adminItems.addFieldType(field.getType().getName(), true);
            for (ImpExpPart part : impExpDoc.getParts()) adminItems.addPartType(part.getType().getName(), true);

            listener.success(impExpVariantKey);
        } catch (Throwable e) {
            listener.failed(impExpVariantKey, e);
        }
    }

    private void addAdditionalItems() {
        if (documentSet instanceof ExportSet) {
            ExportSet exportSet = (ExportSet)documentSet;

            for (String documentType : exportSet.getDocumentTypes()) adminItems.addDocumentType(documentType, false);
            for (String fieldType : exportSet.getFieldTypes()) adminItems.addFieldType(fieldType, false);
            for (String partType : exportSet.getPartTypes()) adminItems.addPartType(partType, false);
            for (String collection : exportSet.getCollections()) adminItems.addCollection(collection, false);

            for (String namespace : exportSet.getNamespaces()) {
                try {
                    String fingerprint = namespaceManager.getNamespace(namespace).getFingerprint();
                    impExpNamespaces.addNamespace(namespace, fingerprint, false);
                } catch (NamespaceNotFoundException e) {
                    listener.failedItem("namespace", namespace, e);
                }
            }
        }
    }

    private void exportNamespaces() throws Exception {
        listener.info("Exporting namespaces.");
        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/namespaces.xml");
            ImpExpNamespacesXmlizer.toXml(impExpNamespaces, os);
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void exportSchema() throws Exception {
        listener.info("Exporting schema.");

        checkInterrupted();

        ImpExpSchemaFactory.SchemaFactoryListener schemaListener = new ImpExpSchemaFactory.SchemaFactoryListener() {

            public void failedDocumentType(String name, Throwable e) throws Exception {
                if (adminItems.isDocumentTypeRequired(name))
                    throw new ImportExportException("Failed to export required document type", e);
                else
                    listener.failedItem("documentType", name, e);
            }

            public void failedPartType(String name, Throwable e) throws Exception {
                if (adminItems.isPartTypeRequired(name))
                    throw new ImportExportException("Failed to export required part type", e);
                else
                    listener.failedItem("partType", name, e);
            }

            public void failedFieldType(String name, Throwable e) throws Exception {
                if (adminItems.isFieldTypeRequired(name))
                    throw new ImportExportException("Failed to export required field type", e);
                else
                    listener.failedItem("fieldType", name, e);
            }
        };

        ImpExpSchema schema = ImpExpSchemaFactory.build(adminItems.getDocumentTypes(), adminItems.getFieldTypes(), adminItems.getPartTypes(), repository, schemaListener);
        checkInterrupted();

        // customize schema
        options.getSchemaCustomizer().customize(schema);
        checkInterrupted();

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/schema.xml");
            ImpExpSchemaXmlizer.toXml(schema, os, repository);
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void exportVariants() throws Exception {
        listener.info("Exporting variants.");

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/variants.xml");
            ImpExpVariantsXmlizer.toXml(variants, os);
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void exportCollections() throws Exception {
        listener.info("Exporting collections.");

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/collections.xml");
            ImpExpCollectionsXmlizer.toXml(adminItems.getCollections(), os);
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void exportRetiredDocuments() throws Exception {
        listener.info("Exporting retired documents.");

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/retired.xml");

            XmlProducer xmlProducer = new XmlProducer(os);
            xmlProducer.startElement("retiredDocuments");

            Map<String, String> attrs = new LinkedHashMap<String, String>();
            for (Object retiredDocument : retiredDocuments) {
                ImpExpVariantKey variantKey = (ImpExpVariantKey)retiredDocument;
                attrs.clear();
                attrs.put("id", variantKey.getDocumentId());
                attrs.put("branch", variantKey.getBranch());
                attrs.put("language", variantKey.getLanguage());
                xmlProducer.emptyElement("document", attrs);
            }

            xmlProducer.endElement("retiredDocuments");
            xmlProducer.flush();
        } finally {
            if (os != null)
                os.close();
        }
    }

    private void exportMeta() throws Exception {
        listener.info("Writing export meta information.");

        ImpExpMeta meta = new ImpExpMeta();
        meta.setDaisyServerVersion(repository.getServerVersion());
        meta.setExportTime(XmlizerUtil.formatValue(exportTime, ValueType.DATETIME, null));

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/meta.xml");
            ImpExpMetaXmlizer.toXml(meta, os);
        } finally {
            if (os != null)
                os.close();
        }
    }

    /**
     * Registers that the namespace of the provided document ID should be
     * added to the export.
     *
     * @param required if true, the namespace is marked as required, i.e. if it is
     *                 not present in the target repository upon import,
     *                 the import will fail.
     */
    protected void needNamespaceOfDocId(String documentId, boolean required) throws NamespaceNotFoundException {
        Matcher matcher = Constants.DAISY_DOCID_PATTERN.matcher(documentId);
        if (matcher.matches()) {
            String namespaceName = matcher.group(2);
            if (namespaceName != null) {
                Namespace namespace;
                try {
                    namespace = namespaceManager.getNamespace(namespaceName);
                } catch (NamespaceNotFoundException e) {
                    if (required) {
                        throw e;
                    } else {
                        // ignore silently
                        return;
                    }
                }
                impExpNamespaces.addNamespace(namespace.getName(), namespace.getFingerprint(), required);
            }
        }
    }

    protected void needsBranchLanguage(String branch, String language, boolean required) throws RepositoryException {
        VariantManager variantManager = repository.getVariantManager();

        if (branch != null) {
            String branchDescription = variantManager.getBranchByName(branch, false).getDescription();
            variants.addBranch(branch, branchDescription, required);
        }

        if (language != null) {
            String langDescription = variantManager.getLanguageByName(language, false).getDescription();
            variants.addLanguage(language, langDescription, required);
        }
    }

    protected void needsBranchLanguage(long branchId, long languageId, boolean required) throws RepositoryException {
        VariantManager variantManager = repository.getVariantManager();

        // branchId or languageId could be -1 in they are not specified in links

        if (branchId != -1) {
            Branch branch = variantManager.getBranch(branchId, false);
            variants.addBranch(branch.getName(), branch.getDescription(), required);
        }

        if (languageId != -1) {
            Language lang = variantManager.getLanguage(languageId, false);
            variants.addLanguage(lang.getName(), lang.getDescription(), required);
        }
    }

    static class MyPartWriter implements ImpExpDocumentXmlizer.PartWriter {
        private String basePath;
        private int counter = 0;
        private ExportFile exportFile;
        private DocumentExportCustomizer exportCustomizer;

        public MyPartWriter(String basePath, ExportFile exportFile, DocumentExportCustomizer exportCustomizer) {
            this.basePath = basePath;
            this.exportFile = exportFile;
            this.exportCustomizer = exportCustomizer;
        }

        public String writePart(ImpExpPart part) throws Exception {
            counter++;
            String dataRef = exportCustomizer.exportFilename("data" + counter, part);
            String path = basePath + "/" + dataRef;

            ImpExpPart.PartDataAccess dataAccess = part.getDataAccess();
            exportFile.store(path, dataAccess.getInputStream());

            return dataRef;
        }
    }

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            throw new ImportExportException("Export was interrupted on user's request.");
        }
    }
}
