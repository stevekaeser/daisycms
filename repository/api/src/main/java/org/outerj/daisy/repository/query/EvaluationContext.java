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
package org.outerj.daisy.repository.query;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Version;

import java.util.List;
import java.util.ArrayList;

public final class EvaluationContext {
    private List<ContextDocumentEntry> contextDocs = new ArrayList<ContextDocumentEntry>();

    public Document getContextDocument() {
        return getContextDocument(1);
    }

    public Version getContextVersion() {
        return getContextVersion(1);
    }

    public Document getContextDocument(int position) {
        int actualPos = contextDocs.size() - position; // position specifies index from the end of the list
        if (actualPos >= 0 && actualPos < contextDocs.size())
            return contextDocs.get(actualPos).getDocument();
        else
            return null;
    }

    public Version getContextVersion(int position) {
        int actualPos = contextDocs.size() - position; // position specifies index from the end of the list
        if (actualPos >= 0 && actualPos < contextDocs.size())
            return contextDocs.get(actualPos).getVersion();
        else
            return null;
    }

    /**
     * Pushes a context document on the stack of context documents.
     * The version is allowed to be null.
     */
    public void pushContextDocument(Document document, Version version) {
        contextDocs.add(new ContextDocumentEntry(document, version));
    }

    /**
     * Removes a context document (the latest pushed one) from the stack of context documents.
     */
    public void popContextDocument() {
        if (contextDocs.size() > 0)
            contextDocs.remove(contextDocs.size() - 1);
        else
            throw new RuntimeException("EvaluationContext: stack of context documents is empty");
    }

    /**
     * Sets the context document. This removes any previously pushed context
     * documents from the stack, thus after calling this method the context
     * document stack contains just one entry. The version is allowed to be null.
     */
    public void setContextDocument(Document document, Version version) {
        contextDocs.clear();
        pushContextDocument(document, version);
    }

    private final class ContextDocumentEntry {
        private Document document;
        private Version version;

        public ContextDocumentEntry(Document document, Version version) {
            this.document = document;
            this.version = version;
        }

        public Document getDocument() {
            return document;
        }

        public Version getVersion() {
            return version;
        }
    }
}
