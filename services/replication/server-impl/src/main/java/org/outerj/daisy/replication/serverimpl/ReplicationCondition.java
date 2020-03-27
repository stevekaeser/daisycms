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
package org.outerj.daisy.replication.serverimpl;

import java.util.regex.Matcher;

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.util.Constants;

public class ReplicationCondition {
    
    private String namespace;
    private String branch;
    private String language;
    
    public ReplicationCondition(String namespace, String branch, String language) {
        this.namespace = namespace;
        this.branch = branch;
        this.language = language;
    }
    
    public String getNamespace() {
        return namespace;
    }
    public String getBranch() {
        return branch;
    }
    public String getLanguage() {
        return language;
    }
    
    public boolean matches(Repository repository, VariantKey key) throws RepositoryException {
        Matcher matcher = Constants.DAISY_DOCID_PATTERN.matcher(key.getDocumentId());
        if (namespace != null && matcher.matches() && !matcher.group(2).equals(namespace))
            return false;
        if (branch != null && repository.getVariantManager().getBranch(branch, false).getId() != key.getBranchId())
            return false;
        if (language != null && repository.getVariantManager().getLanguage(language, false).getId() != key.getLanguageId())
            return false;
        return true;
    }

}
