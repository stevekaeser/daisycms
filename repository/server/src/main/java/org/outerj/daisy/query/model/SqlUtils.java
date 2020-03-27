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
package org.outerj.daisy.query.model;

import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.InvalidDocumentIdException;
import org.outerj.daisy.repository.namespace.NamespaceNotFoundException;
import org.outerj.daisy.repository.commonimpl.DocId;
import org.outerj.daisy.repository.query.QueryException;
import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.util.Constants;

import java.util.regex.Matcher;

public class SqlUtils {
    public static String escapeString(String value) {
        StringBuilder result = new StringBuilder(value.length() + 8);

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'') {
                result.append("''");
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    public static long parseBranch(String branch, QueryContext context) throws QueryException {
        if (branch.length() > 0 && (Character.isDigit(branch.charAt(0)) || branch.charAt(0) == '-')) {
            try {
                return Long.parseLong(branch);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid branch ID: \"" + branch + "\".");
            }
        } else {
            try {
                return context.getBranchByName(branch).getId();
            } catch (RepositoryException e) {
                throw new QueryException("Problem with branch name \"" + branch + "\".");
            }
        }
    }

    public static long parseLanguage(String language, QueryContext context) throws QueryException {
        if (language.length() > 0 && (Character.isDigit(language.charAt(0)) || language.charAt(0) == '-')) {
            try {
                return Long.parseLong(language);
            } catch (NumberFormatException e) {
                throw new QueryException("Invalid language ID: \"" + language + "\".");
            }
        } else {
            try {
                return context.getLanguageByName(language).getId();
            } catch (RepositoryException e) {
                throw new QueryException("Problem with language name \"" + language + "\".");
            }
        }
    }

    /**
     * A custom method for parsing a document ID into a DocId object. This one
     * doesn't fail when the namespace in the document ID does not exist.
     * Rather, it uses -1 then as the internal namespace ID. This is OK
     * for searching or comparing in expressions, since no
     * document has a namespace with ID -1. This approach makes that a query
     * doesn't fail because a namespace name happens to be not registered
     * in the repository.
     */
    public static DocId parseDocId(String documentId, QueryContext queryContext) {
        if (documentId == null)
            throw new IllegalArgumentException("documentId argument is not allowed to be null");
        Matcher matcher = Constants.DAISY_COMPAT_DOCID_PATTERN.matcher(documentId);
        if (matcher.matches()) {
            long docSeqId = Long.parseLong(matcher.group(1));
            String namespace = matcher.group(2);
            long nsId;
            if (namespace == null)
                namespace = queryContext.getRepositoryNamespace();
            try {
                nsId = queryContext.getNamespace(namespace).getId();
            } catch (NamespaceNotFoundException e) {
                nsId = -1;
            }
            return new DocId(docSeqId, namespace, nsId);
        } else {
            throw new InvalidDocumentIdException("Invalid document ID: \"" + documentId + "\".");
        }
    }
}
