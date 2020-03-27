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
package org.outerj.daisy.tools.importexport.export.cli;

import org.outerj.daisy.tools.importexport.export.ExportListener;
import org.outerj.daisy.tools.importexport.export.BaseExportListener;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.util.ProgressIndicator;

import java.io.PrintStream;

class CliExportListener extends BaseExportListener implements ExportListener {
    private PrintStream out;
    private ExportOptions exportOptions;
    private ProgressIndicator documentProgess;
    private boolean interrupted = false;

    public CliExportListener(PrintStream out, ExportOptions exportOptions) {
        this.out = out;
        this.exportOptions = exportOptions;
        this.documentProgess = new ProgressIndicator(out);
    }

    public void info(String message) {
        out.println(message);
    }

    public void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception {
        documentProgess.failureOccured();
        super.failed(variantKey, e);

        if (exportOptions.getFailOnError()) {
            throw new ImportExportException("Got an error exporting the document " + variantKey, e);
        }
    }

    protected boolean includeFullStackTracesOfFailures() {
        return exportOptions.getStackTracesOfFailures();
    }

    protected String getStackTraceDisabledMessage() {
        return "(stacktraces disabled, enable them via export options)";
    }

    public void startDocumentProgress(int total) {
        documentProgess.startProgress(total);
    }

    public void updateDocumentProgress(int current) {
        documentProgess.updateProgress(current);
    }

    public void endDocumentProgress() {
        if (interrupted) {
            out.println("interrupted!");
        } else {
            documentProgess.endProgress();
        }
    }

    public synchronized boolean isInterrupted() {
        return interrupted;
    }

    public synchronized void interrupt() {
        interrupted = true;
    }
}
