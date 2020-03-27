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
package org.outerj.daisy.doctaskrunner;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;

/**
 * Represents a set of documents on which a task willl be run.
 */
public interface DocumentSelection {
    /**
     *
     * @param repository useful if a e.g. a query needs to be executed to determine the result.
     * @return an array of SelectedDocument objects, for which getVariantKey() should return a unique variantKey (no duplicates).
     */
    VariantKey[] getKeys(Repository repository) throws RepositoryException;

    String getDescription();
}
