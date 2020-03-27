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
 * Listener interface for repository related events.
 *
 * <p>The events are fired synchronously after their corresponding
 * operation has completed successfully. Therefore, it is very important
 * that event listeners operate very quickly, since otherwise they can
 * seriously impact basic document operations.
 *
 * <p>Event listeners are registered through {@link Repository#addListener(RepositoryListener)}.
 *
 */
public interface RepositoryListener {
    /**
     * @param id the id of the item to which the event applies. For most entities this is a Long,
     *           for document it is a String (the document ID), for document variant related
     *           events it is a {@link VariantKey} object.
     * @param updateCount the "update count" of the item (see the getUpdateCount method on various entities
     * such as documents, users, field/part/document types, etc), if applicable, otherwise -1
     */
    void repositoryEvent(RepositoryEventType eventType, Object id, long updateCount);

}
