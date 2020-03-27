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

import org.outerj.daisy.query.QueryContext;
import org.outerj.daisy.repository.query.QueryException;

public class FullTextQuery {
    private String query;
    private boolean searchName;
    private boolean searchContent;
    private boolean searchFields;
    private long branchId = -1;
    private long languageId = -1;
    private String branch;
    private String language;

    public FullTextQuery(String query) {
        this.query = query;
    }

    public void prepare(QueryContext context) throws QueryException {
        if (branch != null) {
            branchId = SqlUtils.parseBranch(branch, context);
        }

        if (language != null) {
            languageId = SqlUtils.parseLanguage(language, context);
        }
    }

    public void setSearchName(boolean searchName) {
        this.searchName = searchName;
    }

    public void setSearchContent(boolean searchContent) {
        this.searchContent = searchContent;
    }

    public void setSearchFields(boolean searchFields) {
        this.searchFields = searchFields;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public void setLanguageId(long languageId) {
        this.languageId = languageId;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getQuery() {
        return query;
    }

    public boolean getSearchName() {
        return searchName;
    }

    public boolean getSearchContent() {
        return searchContent;
    }

    public boolean getSearchFields() {
        return searchFields;
    }

    public long getBranchId() {
        return branchId;
    }

    public long getLanguageId() {
        return languageId;
    }
}
