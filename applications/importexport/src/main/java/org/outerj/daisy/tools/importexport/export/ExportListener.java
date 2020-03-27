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

/**
 * A call back interface through which the {@link Exporter export tool}
 * provides feedback about the export.
 */
public interface ExportListener {
    /**
     * Called to output an informational message.
     */
    void info(String message);

    void failedItem(String itemType, String itemName, Throwable e);

    void skippedBecauseRetired(ImpExpVariantKey variantKey);

    void skippedBecauseNoLiveVersion(ImpExpVariantKey variantKey);

    /**
     * Called when exporting a document fails.
     *
     * <p>The implementation can decide to simply log this or throw an exception
     * in order to stop the whole export.
     */
    void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception;

    /**
     * Called when the export of a document succeeded.
     */
    void success(ImpExpVariantKey variantKey);

    /**
     * Reports that a document contains a link to another document. This
     * may be called multiple times with the same arguments, if a link
     * to the same document occurs more then once.
     */
    void hasLink(ImpExpVariantKey sourceVariantKey, ImpExpVariantKey targetVariantKey, LinkType linkType);

    void startDocumentProgress(int total);

    void updateDocumentProgress(int current);

    void endDocumentProgress();

    /**
     * If this method returns true, the import will be interrupted ASAP by throwing an exception.
     */
    boolean isInterrupted();
}
