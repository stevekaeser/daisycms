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

import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;
import org.outerj.daisy.linkextraction.LinkType;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Provides some base functionality that is likely useful for
 * different ExportListener implementations.
 */
public abstract class BaseExportListener implements ExportListener {
    private List<FailureInfo> failed = new ArrayList<FailureInfo>();
    private List<ImpExpVariantKey> succeeded = new ArrayList<ImpExpVariantKey>();
    private List<ImpExpVariantKey> skippedBecauseRetired = new ArrayList<ImpExpVariantKey>();
    private List<ImpExpVariantKey> skippedBecauseNoLiveVersion = new ArrayList<ImpExpVariantKey>();
    private Set<ImpExpVariantKey> links = new HashSet<ImpExpVariantKey>();
    private List<ImpExpVariantKey> brokenLinks;
    private List<ItemFailureInfo> failedItems = new ArrayList<ItemFailureInfo>();

    public void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception {
        String errorDescription = getThrowableDescription(e);
        String stackTrace = getStackTrace(e);

        failed.add(new FailureInfo(variantKey, errorDescription, stackTrace));
    }

    private String getThrowableDescription(Throwable e) {
        StringBuilder errorDescription = new StringBuilder(200);
        Throwable current = e;
        do {
            if (errorDescription.length() > 0)
                errorDescription.append(" --> ");
            errorDescription.append(current.getMessage());
            current = current.getCause();
        } while (current != null);

        return errorDescription.toString();
    }

    private String getStackTrace(Throwable e) {
        String stackTrace;
        if (includeFullStackTracesOfFailures()) {
            StringWriter exceptionWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(exceptionWriter));
            stackTrace = exceptionWriter.toString();
        } else {
            stackTrace = getStackTraceDisabledMessage();
        }
        return stackTrace;
    }

    protected abstract boolean includeFullStackTracesOfFailures();

    protected abstract String getStackTraceDisabledMessage();

    public void success(ImpExpVariantKey variantKey) {
        succeeded.add(variantKey);
    }

    public void skippedBecauseRetired(ImpExpVariantKey variantKey) {
        skippedBecauseRetired.add(variantKey);
    }

    public void skippedBecauseNoLiveVersion(ImpExpVariantKey variantKey) {
        skippedBecauseNoLiveVersion.add(variantKey);
    }

    public void hasLink(ImpExpVariantKey sourceVariantKeyk, ImpExpVariantKey targetVariantKey, LinkType linkType) {
        links.add(targetVariantKey);
    }

    public Set<ImpExpVariantKey> getLinks() {
        return links;
    }

    public List<ImpExpVariantKey> getSucceeded() {
        return succeeded;
    }

    public List<FailureInfo> getFailed() {
        return failed;
    }

    public List<ImpExpVariantKey> getSkippedBecauseRetired() {
        return skippedBecauseRetired;
    }

    public List<ImpExpVariantKey> getSkippedBecauseNoLiveVersion() {
        return skippedBecauseNoLiveVersion;
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

    /**
     * Failure info for non-documents.
     */
    public static class ItemFailureInfo {
        private final String type;
        private final String name;
        private final String errorDescription;
        private final String stackTrace;

        public ItemFailureInfo(String type, String name, String errorDescription, String stackTrace) {
            this.type = type;
            this.name = name;
            this.errorDescription = errorDescription;
            this.stackTrace = stackTrace;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public String getStackTrace() {
            return stackTrace;
        }
    }

    /**
     * Gets list of broken links. The broken links are determined the first
     * time this method is called.
     */
    public List getBrokenLinks() {
        if (brokenLinks != null)
            return brokenLinks;

        Set<ImpExpVariantKey> succeededHash = new HashSet<ImpExpVariantKey>(getSucceeded());
        Set<ImpExpVariantKey> retiredHash = new HashSet<ImpExpVariantKey>(getSkippedBecauseRetired());
        List<ImpExpVariantKey> list = new ArrayList<ImpExpVariantKey>();

        for (ImpExpVariantKey variantKey : getLinks()) {
            if (!succeededHash.contains(variantKey) && !retiredHash.contains(variantKey)) {
                list.add(variantKey);
            }
        }

        this.brokenLinks = list;
        return list;
    }

    public void failedItem(String itemType, String itemName, Throwable e) {
        String errorDescription = getThrowableDescription(e);
        String stackTrace = getStackTrace(e);

        failedItems.add(new ItemFailureInfo(itemType, itemName, errorDescription, stackTrace));
    }

    public List<ItemFailureInfo> getFailedItems() {
        return Collections.unmodifiableList(failedItems);
    }

    public void generateSax(ContentHandler contentHandler) throws SAXException {
        contentHandler.startDocument();
        contentHandler.startElement("", EXPORT_RESULT_EL, EXPORT_RESULT_EL, new AttributesImpl());

        if (skippedBecauseRetired.size() > 0) {
            contentHandler.startElement("", SKIPPED_BECAUSE_RETIRED_EL, SKIPPED_BECAUSE_RETIRED_EL, new AttributesImpl());

            Iterator it = skippedBecauseRetired.iterator();
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            while (it.hasNext()) {
                ImpExpVariantKey variantKey = (ImpExpVariantKey)it.next();
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", SKIPPED_BECAUSE_RETIRED_EL, SKIPPED_BECAUSE_RETIRED_EL);
        }

        if (skippedBecauseNoLiveVersion.size() > 0) {
            contentHandler.startElement("", SKIPPED_BECAUSE_NO_LIVE_VERSION_EL, SKIPPED_BECAUSE_NO_LIVE_VERSION_EL, new AttributesImpl());

            Iterator it = skippedBecauseNoLiveVersion.iterator();
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            while (it.hasNext()) {
                ImpExpVariantKey variantKey = (ImpExpVariantKey)it.next();
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", SKIPPED_BECAUSE_NO_LIVE_VERSION_EL, SKIPPED_BECAUSE_NO_LIVE_VERSION_EL);
        }

        if (failed.size() > 0) {
            contentHandler.startElement("", FAILURES_EL, FAILURES_EL, new AttributesImpl());

            Iterator it = failed.iterator();
            AttributesImpl attrs = new AttributesImpl();
            while (it.hasNext()) {
                FailureInfo failureInfo = (FailureInfo)it.next();
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

        getBrokenLinks(); // to make sure they are determined
        if (brokenLinks.size() > 0) {
            contentHandler.startElement("", BROKENLINK_EL, BROKENLINK_EL, new AttributesImpl());

            Iterator it = brokenLinks.iterator();
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            while (it.hasNext()) {
                ImpExpVariantKey variantKey = (ImpExpVariantKey)it.next();
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", BROKENLINK_EL, BROKENLINK_EL);
        }


        if (succeeded.size() > 0) {
            contentHandler.startElement("", SUCCEEDED_EL, SUCCEEDED_EL, new AttributesImpl());

            Iterator it = succeeded.iterator();
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            while (it.hasNext()) {
                ImpExpVariantKey variantKey = (ImpExpVariantKey)it.next();
                attrs.clear();
                attrs.addAttribute("", ID_ATTR, ID_ATTR, "CDATA", variantKey.getDocumentId());
                attrs.addAttribute("", BRANCH_ATTR, BRANCH_ATTR, "CDATA", variantKey.getBranch());
                attrs.addAttribute("", LANG_ATTR, LANG_ATTR, "CDATA", variantKey.getLanguage());
                contentHandler.startElement("", DOCUMENT_EL, DOCUMENT_EL, attrs);
                contentHandler.endElement("", DOCUMENT_EL, DOCUMENT_EL);
            }
            contentHandler.endElement("", SUCCEEDED_EL, SUCCEEDED_EL);
        }

        if (failedItems.size() > 0) {
            contentHandler.startElement("", OTHER_FAILURES_EL, OTHER_FAILURES_EL, new AttributesImpl());
            org.xml.sax.helpers.AttributesImpl attrs = new org.xml.sax.helpers.AttributesImpl();
            for (ItemFailureInfo item : failedItems) {
                attrs.clear();
                attrs.addAttribute("", "type", "type", "CDATA", item.getType());
                attrs.addAttribute("", "name", "name", "CDATA", item.getName());

                contentHandler.startElement("", OTHER_FAILURE_EL, OTHER_FAILURE_EL, attrs);

                contentHandler.startElement("", "description", "description", new AttributesImpl());
                contentHandler.characters(item.getErrorDescription().toCharArray(), 0, item.getErrorDescription().length());
                contentHandler.endElement("", "description", "description");

                contentHandler.startElement("", "stackTrace", "stackTrace", new AttributesImpl());
                contentHandler.characters(item.getStackTrace().toCharArray(), 0, item.getStackTrace().length());
                contentHandler.endElement("", "stackTrace", "stackTrace");

                contentHandler.endElement("", OTHER_FAILURE_EL, OTHER_FAILURE_EL);
            }
            contentHandler.endElement("", OTHER_FAILURES_EL, OTHER_FAILURES_EL);
        }

        contentHandler.endElement("", EXPORT_RESULT_EL, EXPORT_RESULT_EL);
        contentHandler.endDocument();
    }

    private static final String EXPORT_RESULT_EL = "exportResult";
    private static final String SKIPPED_BECAUSE_RETIRED_EL = "skippedBecauseRetired";
    private static final String SKIPPED_BECAUSE_NO_LIVE_VERSION_EL = "skippedBecauseNoLiveVersion";
    private static final String FAILURES_EL = "failed";
    private static final String SUCCEEDED_EL = "succeeded";
    private static final String BROKENLINK_EL = "brokenLink";
    private static final String DOCUMENT_EL = "document";
    private static final String ID_ATTR = "id";
    private static final String BRANCH_ATTR = "branch";
    private static final String LANG_ATTR = "language";
    private static final String OTHER_FAILURES_EL = "otherFailures";
    private static final String OTHER_FAILURE_EL = "failure";
}
