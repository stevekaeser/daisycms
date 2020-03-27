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

import org.outerj.daisy.repository.AccessException;
import org.outerj.daisy.repository.DocumentLockedException;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * Provides some base functionality that is likely useful for
 * different ImportListener implementations.
 */
public abstract class BaseImportListener implements ImportListener {
    private List<ImpExpVariantKey> failedBecausePermissionDenied = new ArrayList<ImpExpVariantKey>();
    private List<ImpExpVariantKey> failedBecauseLockedDocument = new ArrayList<ImpExpVariantKey>();
    private List<FailureInfo> failedOtherReason = new ArrayList<FailureInfo>();
    private List<SuccessInfo> succeeded = new ArrayList<SuccessInfo>();

    public void permissionDenied(ImpExpVariantKey variantKey, AccessException e) throws Exception {
        failedBecausePermissionDenied.add(variantKey);
    }

    public void lockedDocument(ImpExpVariantKey variantKey, DocumentLockedException e) throws Exception {
        failedBecauseLockedDocument.add(variantKey);
    }

    public void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception {
        StringBuilder errorDescription = new StringBuilder(200);
        Throwable current = e;
        do {
            if (errorDescription.length() > 0)
                errorDescription.append(" --> ");
            errorDescription.append(current.getMessage());
            current = current.getCause();
        } while (current != null);

        String stackTrace;
        if (includeFullStackTracesOfFailures()) {
            StringWriter exceptionWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(exceptionWriter));
            stackTrace = exceptionWriter.toString();
        } else {
            stackTrace = getStackTraceDisabledMessage();
        }

        failedOtherReason.add(new FailureInfo(variantKey, errorDescription.toString(), stackTrace));
    }

    protected abstract boolean includeFullStackTracesOfFailures();

    protected abstract String getStackTraceDisabledMessage();

    public void success(ImpExpVariantKey variantKey, DocumentImportResult result) {
        succeeded.add(new SuccessInfo(variantKey, result));
    }

    public Collection<ImpExpVariantKey> getFailedBecauseAccessDenied() {
        return failedBecausePermissionDenied;
    }

    public Collection<ImpExpVariantKey> getFailedBecauseLockedDocument() {
        return failedBecauseLockedDocument;
    }

    public Collection<FailureInfo> getFailedDocuments() {
        return failedOtherReason;
    }

    public Collection<SuccessInfo> getSucceeded() {
        return succeeded;
    }

    public static class FailureInfo {
        private final ImpExpVariantKey variantKey;
        private final String errorDescription;
        private final String stackTrace;

        public FailureInfo(ImpExpVariantKey variantKey, String errorDescription, String stackTrace) {
            this.variantKey = variantKey;
            this.errorDescription = errorDescription;
            this.stackTrace = stackTrace;
        }

        public ImpExpVariantKey getVariantKey() {
            return variantKey;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getStackTrace() {
            return stackTrace;
        }
    }

    public static class SuccessInfo {
        private final ImpExpVariantKey variantKey;
        private final DocumentImportResult result;

        public SuccessInfo(ImpExpVariantKey variantKey, DocumentImportResult result) {
            this.variantKey = variantKey;
            this.result = result;
        }

        public ImpExpVariantKey getVariantKey() {
            return variantKey;
        }

        public DocumentImportResult getResult() {
            return result;
        }
    }

    /**
     * Allows to produce a piece of SAX with information about the imported schema.
     * This will be included in the result generated by {@link #generateSax(org.xml.sax.ContentHandler)}.
     * This is optional, the implementation can be empty.
     */
    public abstract void generateSchemaSaxFragment(ContentHandler contentHandler) throws SAXException;

    public void generateSax(ContentHandler contentHandler) throws SAXException {
        contentHandler.startDocument();
        contentHandler.startElement("", IMPORT_RESULT_EL, IMPORT_RESULT_EL, new AttributesImpl());

        generateSchemaSaxFragment(contentHandler);

        if (failedBecauseLockedDocument.size() > 0) {
            contentHandler.startElement("", FAILED_BECAUSE_LOCKED_EL, FAILED_BECAUSE_LOCKED_EL, new AttributesImpl());

            for (ImpExpVariantKey variantKey : failedBecauseLockedDocument) {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", FAILED_BECAUSE_LOCKED_EL, FAILED_BECAUSE_LOCKED_EL);
        }

        if (failedBecausePermissionDenied.size() > 0) {
            contentHandler.startElement("", FAILED_BECAUSE_ACCESS_DENIED_EL, FAILED_BECAUSE_ACCESS_DENIED_EL, new AttributesImpl());

            AttributesImpl attrs = new AttributesImpl();
            for (ImpExpVariantKey variantKey : failedBecausePermissionDenied) {
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", FAILED_BECAUSE_ACCESS_DENIED_EL, FAILED_BECAUSE_ACCESS_DENIED_EL);
        }

        if (failedOtherReason.size() > 0) {
            contentHandler.startElement("", FAILURES_EL, FAILURES_EL, new AttributesImpl());

            AttributesImpl attrs = new AttributesImpl();
            for (FailureInfo failureInfo : failedOtherReason) {
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", failureInfo.getVariantKey().getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", failureInfo.getVariantKey().getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", failureInfo.getVariantKey().getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);

                contentHandler.startElement("", "description", "description", new AttributesImpl());
                contentHandler.characters(failureInfo.getErrorDescription().toCharArray(), 0, failureInfo.getErrorDescription().length());
                contentHandler.endElement("", "description", "description");

                contentHandler.startElement("", "stackTrace", "stackTrace", new AttributesImpl());
                contentHandler.characters(failureInfo.getStackTrace().toCharArray(), 0, failureInfo.getStackTrace().length());
                contentHandler.endElement("", "stackTrace", "stackTrace");

                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", FAILURES_EL, FAILURES_EL);
        }

        if (succeeded.size() > 0) {
            contentHandler.startElement("", SUCCEEDED_EL, SUCCEEDED_EL, new AttributesImpl());

            Iterator it = succeeded.iterator();
            AttributesImpl attrs = new AttributesImpl();
            while (it.hasNext()) {
                SuccessInfo sucessInfo = (SuccessInfo)it.next();
                ImpExpVariantKey variantKey = sucessInfo.getVariantKey();
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                attrs.addAttribute("", "result", "result", "CDATA", sucessInfo.getResult().toString());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", SUCCEEDED_EL, SUCCEEDED_EL);
        }

        contentHandler.endElement("", IMPORT_RESULT_EL, IMPORT_RESULT_EL);
        contentHandler.endDocument();
    }

    private static final String IMPORT_RESULT_EL = "importResult";
    private static final String FAILED_BECAUSE_LOCKED_EL = "failedBecauseLocked";
    private static final String FAILED_BECAUSE_ACCESS_DENIED_EL = "failedBecauseAccessDenied";
    private static final String FAILURES_EL = "failed";
    private static final String SUCCEEDED_EL = "succeeded";
    private static final String DOCUMENT_EL = "document";
    private static final String ID_ATTR = "id";
    private static final String BRANCH_ATTR = "branch";
    private static final String LANG_ATTR = "language";
}