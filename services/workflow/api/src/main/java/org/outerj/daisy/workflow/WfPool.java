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
package org.outerj.daisy.workflow;

import org.outerx.daisy.x10Workflow.PoolDocument;
import org.outerj.daisy.repository.RepositoryException;

import java.util.Date;

/**
 * A workflow pool -- something to which work can be assigned.
 */
public interface WfPool {
    /**
     * Returns -1 on unsaved pools.
     */
    long getId();

    String getName();

    void setName(String name);

    String getDescription();

    void setDescription(String description);

    void save() throws RepositoryException;

    /**
     * Returns null when not yet saved.
     */
    Date getLastModified();

    long getLastModifier();

    long getUpdateCount();

    PoolDocument getXml();

    void setAllFromXml(PoolDocument.Pool poolXml);
}
