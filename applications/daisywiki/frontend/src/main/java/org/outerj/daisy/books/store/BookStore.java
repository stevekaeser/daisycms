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
package org.outerj.daisy.books.store;

import java.util.Collection;

import org.outerj.daisy.repository.VersionMode;

public interface BookStore {
    /**
     * Throws an exception if a book instance with this name already exists.
     * The returned book instance will be locked.
     */
    BookInstance createBookInstance(String name, String label, long dataBranchId, long dataLanguageId, VersionMode dataVersion, String bookDefinition);

    BookInstance getBookInstance(String name);

    void deleteBookInstance(String name);

    Collection<BookInstance> getBookInstances();
    
    Collection<BookInstance> getBookInstances(String bookDefinition);

    void renameBookInstance(String oldName, String newName);

    /**
     * Checks whether a book instance with the given name exists.
     */
    boolean existsBookInstance(String name);
}
