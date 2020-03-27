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
package org.outerj.daisy.repository.spi.local;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;

/**
 * A pre-save hook is executed after the repository user requested to
 * save the document, but before the document is actually saved.
 *
 * <p>This hook can modify the content of the document before saving.
 * For example, to extract information from images or other types
 * of documents and assign it to parts or fields.
 *
 * <p>The PreSaveHook should be registered as a plugin with the
 * {@link org.outerj.daisy.plugin.PluginRegistry PluginRegistry}
 */
public interface PreSaveHook {
    /**
     * Performs the pre-save work by modifying the supplied Document object.
     *
     * <p>If this method throws an exception, it will be logged, but the
     * document will still be saved. Hence, a pre-save hook cannot prevent
     * a document from being saved.
     */
    void process(Document document, Repository repository) throws Exception;
}
