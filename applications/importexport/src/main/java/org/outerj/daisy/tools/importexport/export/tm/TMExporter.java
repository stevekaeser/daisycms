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
package org.outerj.daisy.tools.importexport.export.tm;

import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.ExportListener;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespaces;
import org.outerj.daisy.tools.importexport.model.namespace.ImpExpNamespacesXmlizer;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMeta;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMetaXmlizer;
import org.outerj.daisy.tools.importexport.model.document.*;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.tools.importexport.util.XmlizerUtil;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.namespace.Namespace;
import org.outerj.daisy.util.Constants;

import java.util.*;
import java.util.regex.Matcher;
import java.io.OutputStream;

/**
 * An alternative for {@link org.outerj.daisy.tools.importexport.export.Exporter} which
 * produces a format suitable for translation management purposes (i.e. for exchange
 * with translation agencies).
 */
public class TMExporter {
    private DocumentSet documentSet;
    private ExportFile exportFile;
    private Repository repository;
    private ExportOptions options;
    private ExportListener listener;
    private Date exportTime;
    private TMConfig tmConfig;
    private ImpExpNamespaces impExpNamespaces;

    public static void run(DocumentSet documentSet, ExportFile exportFile, Date exportTime,
            Repository repository, ExportOptions options, ExportListener listener, TMConfig tmConfig) throws Exception {
        new TMExporter(documentSet, exportFile, exportTime, repository, options, listener, tmConfig).run();
    }

    private TMExporter(DocumentSet documentSet, ExportFile exportFile, Date exportTime,
            Repository repository, ExportOptions options, ExportListener listener, TMConfig tmConfig) {
        this.documentSet = documentSet;
        this.exportFile = exportFile;
        this.exportTime = exportTime;
        this.repository = repository;
        this.options = options;
        this.listener = listener;
        this.tmConfig = tmConfig;
        this.impExpNamespaces = new ImpExpNamespaces();
    }

    private void run() throws Exception {
        listener.info("Determing set of documents to export.");
        VariantKey[] variantKeys = documentSet.getDocuments().toArray(new VariantKey[0]);
        checkInterrupted();

        // Check all documents belong to same language variant
        if (variantKeys.length > 0) {
            long firstLangId = variantKeys[0].getLanguageId();
            for (int i = 1; i < variantKeys.length; i++) {
                if (variantKeys[i].getLanguageId() != firstLangId)
                    throw new ImportExportException("Not all documents in the export set belong to the same language variant.");
            }
        }
        checkInterrupted();

        // Write documents
        exportDocuments(variantKeys);
        checkInterrupted();

        // Write namespaces
        try {
            exportNamespaces();
        } catch (Throwable e) {
            throw new ImportExportException("Error exporting namespaces.", e);
        }
        checkInterrupted();

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

            // Check if document is retired
            if (document.isRetired()) {
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

            // Filter document
            options.getDocumentExportCustomizer().preExportFilter(impExpDoc);

            // Write document
            OutputStream os = null;
            try {
                String path = impExpDoc.getId() + "~" + impExpDoc.getBranch() + ".xml";
                os = exportFile.getOutputStream(path);
                TMDocumentXmlizer.toXml(impExpDoc, version, os, repository, tmConfig);
            } finally {
                if (os != null)
                    os.close();
            }

            listener.success(impExpVariantKey);
        } catch (Throwable e) {
            listener.failed(impExpVariantKey, e);
        }
    }

    private void exportMeta() throws Exception {
        listener.info("Writing export meta information.");

        ImpExpMeta meta = new ImpExpMeta();
        meta.setDaisyServerVersion(repository.getServerVersion());
        meta.setExportTime(XmlizerUtil.formatValue(exportTime, ValueType.DATETIME, null));
        meta.setExportFormat("tm");

        OutputStream os = null;
        try {
            os = exportFile.getOutputStream("info/meta.xml");
            ImpExpMetaXmlizer.toXml(meta, os);
        } finally {
            if (os != null)
                os.close();
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
                    namespace = repository.getNamespaceManager().getNamespace(namespaceName);
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

    private void checkInterrupted() throws ImportExportException {
        if (listener.isInterrupted()) {
            throw new ImportExportException("Export was interrupted on user's request.");
        }
    }    
}
