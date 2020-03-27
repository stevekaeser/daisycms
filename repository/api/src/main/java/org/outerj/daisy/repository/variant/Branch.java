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
package org.outerj.daisy.repository.variant;

import org.outerx.daisy.x10.BranchDocument;
import org.outerj.daisy.repository.RepositoryException;

import java.util.Date;

/**
 * Definition of a branch.
 */
public interface Branch {
    static final long MAIN_BRANCH_ID = 1;
    static final String MAIN_BRANCH_NAME = "main";

    long getId();

    String getName();

    String getDescription();

    void setName(String name);

    void setDescription(String description);

    void save() throws RepositoryException;

    long getLastModifier();

    Date getLastModified();

    long getUpdateCount();

    BranchDocument getXml();

    void setAllFromXml(BranchDocument.Branch branchXml);
}
