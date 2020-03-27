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
package org.outerj.daisy.ftindex;

import java.util.Date;

import org.outerj.daisy.repository.query.QueryException;

public interface FullTextIndex {
    /**
     * Peforms a search on the fulltext search.
     *
     * <p><b>It is of high importance that the caller calls the dispose method on the returned Hits object.
     * This should be guaranteed to happen, i.o.w. use a try-finally block.</b></p>
     */
    Hits search(String analyzer, String query, long branchId, long languageId, Date date, boolean searchName, boolean searchContent, boolean searchFields) throws QueryException;

    /**
     * Index the given content for the given document variant between beginDate and endDate
     * This will first delete any previous indexed content for that document variant, and then index
     * the new content.
     *
     * <p>The parameters documentName, content and fields are all optional (can all be null),
     * if they are all null then the index for this document between startDate and endDate will just be deleted.</p>
     * <p>@see unindex(String documentId, long branchId, long languageId, Date beginDate, Date endDate)</p>
     */
    void index(String documentId, long branchId, long languageId, Date beginDate, Date endDate, String documentName, String content, String fields) throws Exception;

    /**
     * Remove all occurences of the given variant from the index
     */
    void unindex(String documentId, long branchId, long languageId) throws Exception;

    /**
     * Remove a specific entry from the fulltext index
     */
    void unindex(String documentId, long branchId, long languageId, Date beginDate, Date endDate) throws Exception;

}
