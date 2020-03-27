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
package org.outerj.daisy.books.store.impl;

import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VersionMode;

import java.util.Collection;

public class BookStoreImpl implements BookStore {
    private final CommonBookStore commonBookStore;
    private final Repository repository;

    BookStoreImpl(CommonBookStore commonBookStore, Repository repository) {
        this.commonBookStore = commonBookStore;
        this.repository = repository;
    }

    public BookInstance createBookInstance(String name, String label, long dataBranchId, long dataLanguageId, VersionMode dataVersion, String bookDefinition) {
        return commonBookStore.createBookInstance(name, label, dataBranchId, dataLanguageId, dataVersion, repository, bookDefinition);
    }

    public BookInstance getBookInstance(String name) {
        return commonBookStore.getBookInstance(name, repository);
    }

    public void deleteBookInstance(String name) {
        commonBookStore.deleteBookInstance(name, repository);
    }

    public Collection<BookInstance> getBookInstances() {
        return commonBookStore.getBookInstances(repository);
    }
    
    public Collection<BookInstance> getBookInstances(String bookDefinition) {
    	return commonBookStore.getBookInstances(bookDefinition, repository);
    }

    public void renameBookInstance(String oldName, String newName) {
        commonBookStore.renameBookInstance(oldName, newName, repository);
    }

    public boolean existsBookInstance(String name) {
        return commonBookStore.existsBookInstance(name, repository);
    }
}
