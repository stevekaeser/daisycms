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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.util.Constants;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.namespace.NamespaceManager;
import org.outerj.daisy.repository.RepositoryRuntimeException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.DocumentNotFoundException;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.repository.commonimpl.namespace.CommonNamespaceManager;

import java.util.regex.Matcher;

/**
 * Represents a parsed document ID. Only used in the internal implementation, not
 * exposed to the outside world.
 */
public final class DocId {
    private final long docSeqId;
    private final String namespace;
    private final long nsId;
    private final String docId;

    public DocId(long docSeqId, String namespace, long nsId) {
        this.docSeqId = docSeqId;
        this.namespace = namespace;
        this.nsId = nsId;
        this.docId = docSeqId + "-" + namespace;
    }

    public long getSeqId() {
        return docSeqId;
    }

    public String getNamespace() {
        return namespace;
    }

    public long getNamespaceId() {
        return nsId;
    }

    /**
     * Shorter name for {@link #getNamespaceId()}.
     */
    public long getNsId() {
        return nsId;
    }

    public String toString() {
        return docId;
    }

    public static DocId getDocId(long docSeqId, String namespace, CommonRepository repository) {
        long nsId;
        try {
            nsId = repository.getNamespaceManager().getNamespace(namespace).getId();
        } catch (NamespaceNotFoundException e) {
            throw new RepositoryRuntimeException("Invalid namespace: " + namespace, e);
        }
        return new DocId(docSeqId, namespace, nsId);
    }

    public static DocId parseDocId(String documentId, CommonRepository repository) {
        try {
            return parseDocId(documentId, repository, 1);
        } catch (NamespaceNotFoundException e) {
            throw new InvalidDocumentIdException("Invalid document ID \"" + documentId + "\".", e);
        }
    }

    public static DocId parseDocId(String documentId, Repository repository) {
        try {
            return parseDocId(documentId, repository, 2);
        } catch (NamespaceNotFoundException e) {
            throw new InvalidDocumentIdException("Invalid document ID \"" + documentId + "\".", e);
        }
    }

    public static DocId parseDocIdThrowNotFound(String documentId, CommonRepository repository) throws DocumentNotFoundException {
        try {
            return parseDocId(documentId, repository, 1);
        } catch (NamespaceNotFoundException e) {
            throw new DocumentNotFoundException(documentId, e);
        }
    }

    public static DocId parseDocIdThrowNotFound(String documentId, Repository repository) throws DocumentNotFoundException {
        try {
            return parseDocId(documentId, repository, 2);
        } catch (NamespaceNotFoundException e) {
            throw new DocumentNotFoundException(documentId, e);
        }
    }

    private static DocId parseDocId(String documentId, Object repositoryObject, int repoType) throws NamespaceNotFoundException {
        if (documentId == null)
            throw new IllegalArgumentException("documentId argument is not allowed to be null");
        Matcher matcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(documentId);
        if (matcher.matches()) {
            long docSeqId = Long.parseLong(matcher.group(1));
            String namespace = matcher.group(2);
            long nsId;
            if (repoType == 1) {
                CommonNamespaceManager namespaceManager = ((CommonRepository)repositoryObject).getNamespaceManager();
                if (namespace == null)
                    namespace = namespaceManager.getRepositoryNamespace();
                nsId = namespaceManager.getNamespace(namespace).getId();
            } else if (repoType == 2) {
                NamespaceManager namespaceManager = ((Repository)repositoryObject).getNamespaceManager();
                if (namespace == null)
                    namespace = namespaceManager.getRepositoryNamespace();
                nsId = namespaceManager.getNamespace(namespace).getId();
            } else {
                // assert check, should never occur
                throw new RuntimeException("Unexpected repoType value: " + repoType);
            }
            return new DocId(docSeqId, namespace, nsId);
        } else {
            throw new InvalidDocumentIdException("Invalid document ID: \"" + documentId + "\".");
        }
    }

    public boolean equals(Object obj) {
        DocId otherDocId = (DocId)obj;
        return this.nsId == otherDocId.nsId && this.docSeqId == otherDocId.docSeqId;
    }
}
