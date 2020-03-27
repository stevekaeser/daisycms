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
package org.outerj.daisy.tools.importexport.docset;

import org.apache.commons.collections.set.ListOrderedSet;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.tools.importexport.ImportExportException;

import java.util.*;

/**
 * A {@link DocumentSet} implementation based on a Daisy query.
 */
public class QueryDocumentSet implements DocumentSet {
    private Repository repository;
    private String query;

    public QueryDocumentSet(String query, Repository repository) {
        this.query = query;
        this.repository = repository;
    }

    public Set<VariantKey> getDocuments() throws ImportExportException {
        try {
            return ListOrderedSet.decorate(Arrays.asList(repository.getQueryManager().performQueryReturnKeys(query, Locale.getDefault())));
        } catch (RepositoryException e) {
            throw new ImportExportException("Error executing query " + query, e);
        }
    }
}
