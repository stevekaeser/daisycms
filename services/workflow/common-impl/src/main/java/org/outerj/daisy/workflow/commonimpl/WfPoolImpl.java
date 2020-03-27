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
package org.outerj.daisy.workflow.commonimpl;

import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerx.daisy.x10Workflow.PoolDocument;

import java.util.Date;

public class WfPoolImpl implements WfPool {
    private long id = -1;
    private String name;
    private String description;
    private long lastModifier;
    private Date lastModified;
    private long updateCount = 0;
    private WfPoolStrategy strategy;
    private Repository repository;
    private boolean readOnly = false;
    private IntimateAccess intimateAccess = new IntimateAccess();
    private static final String READONLY_MESSAGE = "This workflow pool object is readonly.";

    public WfPoolImpl(String name, WfPoolStrategy strategy, Repository repository)  {
        checkName(name);
        this.name = name;
        this.strategy = strategy;
        this.repository = repository;
    }

    public IntimateAccess getIntimateAccess(WfPoolStrategy strategy) {
        if (readOnly)
            throw new RuntimeException(READONLY_MESSAGE);

        if (this.strategy == strategy)
            return intimateAccess;
        else
            return null;
    }

    private void checkName(String name) {
        if (name == null)
            throw new IllegalArgumentException("Null argument: name");

        if (name.trim().length() == 0)
            throw new IllegalArgumentException("empty or all-whitespace pool name");
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (readOnly)
            throw new RuntimeException(READONLY_MESSAGE);

        checkName(name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (readOnly)
            throw new RuntimeException(READONLY_MESSAGE);

        this.description = description;
    }

    public void save() throws RepositoryException {
        if (readOnly)
            throw new RuntimeException(READONLY_MESSAGE);

        strategy.store(this, repository);
    }

    public Date getLastModified() {
        return lastModified != null ? (Date)lastModified.clone() : null;
    }

    public long getLastModifier() {
        return lastModifier;
    }

    public long getUpdateCount() {
        return updateCount;
    }

    public void makeReadOnly() {
        this.readOnly = true;
    }

    public PoolDocument getXml() {
        PoolDocument document = PoolDocument.Factory.newInstance();
        PoolDocument.Pool xml = document.addNewPool();

        xml.setName(name);
        if (description != null)
            xml.setDescription(description);

        if (id != -1) {
            xml.setId(id);
            xml.setLastModified(WfXmlHelper.getCalendar(lastModified));
            xml.setLastModifier(lastModifier);
            xml.setUpdateCount(updateCount);
        }

        return document;
    }

    public void setAllFromXml(PoolDocument.Pool poolXml) {
        if (readOnly)
            throw new RuntimeException(READONLY_MESSAGE);

        this.name = poolXml.getName();
        this.description = poolXml.getDescription();
    }

    public class IntimateAccess {
        private IntimateAccess() {
            // private on purpose
        }

        public Repository getRepository() {
            return repository;
        }

        public void setId(long id) {
            if (readOnly)
                throw new RuntimeException(READONLY_MESSAGE);
            WfPoolImpl.this.id = id;
        }

        public void setLastModified(Date lastModified) {
            if (readOnly)
                throw new RuntimeException(READONLY_MESSAGE);
            WfPoolImpl.this.lastModified = lastModified;
        }

        public void setLastModifier(long lastModifier) {
            if (readOnly)
                throw new RuntimeException(READONLY_MESSAGE);
            WfPoolImpl.this.lastModifier = lastModifier;
        }

        public void setUpdateCount(long updateCount) {
            if (readOnly)
                throw new RuntimeException(READONLY_MESSAGE);
            WfPoolImpl.this.updateCount = updateCount;
        }
    }
}
