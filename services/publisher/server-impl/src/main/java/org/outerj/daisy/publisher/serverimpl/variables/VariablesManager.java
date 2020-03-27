/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.publisher.serverimpl.variables;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.VersionMode;

public interface VariablesManager {
    /**
     * 
     * @param variableDocs the variant keys of the variable documents. Duplicates will be filtered out.
     * @param repository the repository to be used for checking the user has access rights on the variable documents.
     */
    Variables getVariables(VariantKey[] variableDocs, Repository repository, VersionMode versionMode);
}
