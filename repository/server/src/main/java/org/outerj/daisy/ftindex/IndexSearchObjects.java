/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.commons.logging.Log;

/**
 * This object holds an IndexReader and IndexSearcher couple. When they need to be
 * closed, the closing will be delayed until there are no users of it anymore.
 * To be able to do this, users need to reliably call {@link #addRef()} and
 * {@link #removeRef()}.
 */
public class IndexSearchObjects {
    private IndexReader indexReader;
    private IndexSearcher indexSearcher;
    private Log logger;
    private long refCount = 0;
    private boolean wantsToClose = false;

    public IndexSearchObjects(IndexReader indexReader, IndexSearcher indexSearcher, Log logger) {
        this.indexReader = indexReader;
        this.indexSearcher = indexSearcher;
        this.logger = logger;
    }

    public synchronized void addRef() {
        if (wantsToClose)
            throw new RuntimeException("Can't add ref when closing.");

        refCount++;
    }

    public synchronized void removeRef() {
        if (refCount == 0)
            throw new RuntimeException("Trying to remove ref when refs are zero.");

        refCount--;

        if (refCount == 0 && wantsToClose) {
            doClose();
        }
    }

    private void doClose() {
        try {
            indexReader.close();
        } catch (Throwable e) {
            logger.error("Error closing Lucene index reader.", e);
        }
        try {
            indexSearcher.close();
        } catch (Throwable e) {
            logger.error("Error closing Lucene index searcher.", e);
        }
    }

    public synchronized void close() {
        if (refCount == 0) {
            doClose();
        } else {
            wantsToClose = true;
        }
    }

    public IndexReader getIndexReader() {
        return indexReader;
    }

    public IndexSearcher getIndexSearcher() {
        return indexSearcher;
    }
}
