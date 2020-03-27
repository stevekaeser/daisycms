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
package org.outerj.daisy.tools.importexport.import_.cli;

import org.outerj.daisy.tools.importexport.import_.ImportListener;
import org.outerj.daisy.tools.importexport.import_.BaseImportListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.util.ProgressIndicator;
import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.DocumentLockedException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.PrintStream;

class CliImportListener extends BaseImportListener implements ImportListener {
    private ImportOptions importOptions;
    private PrintStream out;
    private ProgressIndicator documentProgress;
    private CliSchemaListener schemaListener;
    private boolean interrupted = false;

    public CliImportListener(ImportOptions importOptions, PrintStream out) {
        this.importOptions = importOptions;
        this.out = out;
        this.documentProgress = new ProgressIndicator(out);
        this.schemaListener = new CliSchemaListener(out);
    }

    public void startActivity(String name) {
        info("");
        info(name);
    }

    public void info(String message) {
        out.println(message);
    }

    public void debug(String message) {
        if (importOptions.getEnableDetailOutput()) {
            out.println(message);
        }
    }

    public void permissionDenied(ImpExpVariantKey variantKey, AccessException e) throws Exception {
        super.permissionDenied(variantKey, e);
        documentProgress.failureOccured();
        if (importOptions.getFailOnPermissionDenied()) {
            throw e;
        }
    }

    public void lockedDocument(ImpExpVariantKey variantKey, DocumentLockedException e) throws Exception {
        super.lockedDocument(variantKey, e);
        documentProgress.failureOccured();
        if (importOptions.getFailOnLockedDocument()) {
            throw e;
        }
    }

    public void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception {
        documentProgress.failureOccured();
        super.failed(variantKey, e);
        if (importOptions.getFailOnError()) {
            throw new ImportExportException("Got an error importing the document " + variantKey, e);
        }
    }

    protected boolean includeFullStackTracesOfFailures() {
        return importOptions.getFullStackTracesOfFailures();
    }

    protected String getStackTraceDisabledMessage() {
        return "(stacktraces disabled, enable them via import options)";
    }

    public void success(ImpExpVariantKey variantKey, DocumentImportResult result) {
        super.success(variantKey, result);
    }

    public void startDocumentProgress(int total) {
        if (!importOptions.getEnableDetailOutput())
            documentProgress.startProgress(total);
    }

    public void updateDocumentProgress(int current) {
        if (!importOptions.getEnableDetailOutput())
            documentProgress.updateProgress(current);
    }

    public void endDocumentProgress() {
        if (isInterrupted()) {
            out.println("interrupted!");
        } else if (!importOptions.getEnableDetailOutput()) {
            documentProgress.endProgress();
        }
    }

    public SchemaLoadListener getSchemaListener() {
        return schemaListener;
    }

    public void generateSchemaSaxFragment(ContentHandler contentHandler) throws SAXException {
        schemaListener.generateSaxFragment(contentHandler);
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public synchronized void interrupt() {
        interrupted = true;
        schemaListener.interrupt();
    }
}
