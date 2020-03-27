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
import org.outerj.daisy.tools.importexport.import_.documents.DocumentImportResult;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;

public interface ImportListener {
    void startActivity(String name);

    /**
     * Called to output an informational message.
     */
    void info(String message);

    void debug(String message);

    /**
     * Called when saving a document (new or existing) fails because of access
     * permissions.
     *
     * <p>The implementation can decide to simply log this or throw an exception.
     */
    void permissionDenied(ImpExpVariantKey variantKey, AccessException e) throws Exception;

    /**
     * Called when updating a document fails because it is locked.
     *
     * <p>The implementation can decide to simply log this or throw an exception.
     */
    void lockedDocument(ImpExpVariantKey variantKey, DocumentLockedException e) throws Exception;

    /**
     * Called when importing a document fails.
     *
     * <p>This method is not called if the more specialized failures {@link #permissionDenied}
     * or {@link #lockedDocument} are reported.
     *
     * <p>The implementation can decide to simply log this or throw an exception.
     */
    void failed(ImpExpVariantKey variantKey, Throwable e) throws Exception;

    /**
     * Called when the import of a document succeeded.
     */
    void success(ImpExpVariantKey variantKey, DocumentImportResult result);

    void startDocumentProgress(int total);

    void updateDocumentProgress(int current);

    void endDocumentProgress();

    SchemaLoadListener getSchemaListener();

    /**
     * If this method returns true, the import will be interrupted ASAP by throwing an exception.
     */
    boolean isInterrupted();
}
