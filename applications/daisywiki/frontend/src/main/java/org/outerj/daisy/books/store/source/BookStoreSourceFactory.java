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
package org.outerj.daisy.books.store.source;

import org.apache.excalibur.source.SourceFactory;
import org.apache.excalibur.source.Source;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.cocoon.components.ContextHelper;
import org.apache.cocoon.environment.Request;
import org.outerj.daisy.books.store.BookStore;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.frontend.FrontEndContext;

import java.util.Map;
import java.io.IOException;
import java.net.MalformedURLException;

public class BookStoreSourceFactory implements SourceFactory, ThreadSafe, Contextualizable {
    private Context context;

    public void contextualize(Context context) throws ContextException {
        this.context = context;
    }

    public Source getSource(String location, Map parameters) throws IOException {
        if (!location.startsWith("bookstore:"))
            throw new MalformedURLException("The URL does not use the bookstore sheme, it cannot be handled by this source implementation.");

        String spec = location.substring("bookstore:".length());
        int firstSlashPos = spec.indexOf('/');
        if (firstSlashPos == -1)
            throw new MalformedURLException("Invalid bookstore URL: " + location);

        String bookInstanceName = spec.substring(0, firstSlashPos);
        String resource = spec.substring(firstSlashPos + 1);

        Request request = ContextHelper.getRequest(context);
        Repository repository;
        try {
            repository = FrontEndContext.get(request).getRepository();
        } catch (Exception e) {
            throw new RuntimeException("Error getting repository from bookstore source.", e);
        }
        BookStore bookStore = (BookStore)repository.getExtension("BookStore");
        BookInstance bookInstance = bookStore.getBookInstance(bookInstanceName);

        return new BookStoreSource(bookInstance, resource);
    }

    public void release(Source source) {
    }
}
