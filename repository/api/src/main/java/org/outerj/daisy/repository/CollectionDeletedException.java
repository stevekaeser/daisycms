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
package org.outerj.daisy.repository;

/**
 * Thrown if a collection has been removed at the same time someone was
 * editing a document that belonged to the removed collection. When
 * that document is saved, this Exception will be thrown.
 * 
 * <p>The id of the removed collection can be fetched
 * using the getMessage method.</p>
 */
public class CollectionDeletedException extends RepositoryException {

    public CollectionDeletedException() {
        super();
    }

    public CollectionDeletedException(String message) {
        super(message);
    }

    public CollectionDeletedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * gets the id of the deleted collection.
     */

    public String getMessage() {
        return super.getMessage();
    }

}
