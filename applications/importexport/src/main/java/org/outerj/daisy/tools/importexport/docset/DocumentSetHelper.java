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
package org.outerj.daisy.tools.importexport.docset;

import org.apache.commons.collections.set.ListOrderedSet;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.variant.VariantManager;
import org.outerj.daisy.tools.importexport.model.ImpExpVariantKey;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

public class DocumentSetHelper {
    public static Set<ImpExpVariantKey> toImpExpVariantKeys(Set variantKeys, Repository repository) throws RepositoryException {
        VariantManager variantManager = repository.getVariantManager();

        Set result = new ListOrderedSet();
        Iterator it = variantKeys.iterator();
        while (it.hasNext()) {
            VariantKey variantKey = (VariantKey)it.next();
            String branch = variantManager.getBranch(variantKey.getBranchId(), false).getName();
            String language = variantManager.getLanguage(variantKey.getLanguageId(), false).getName();
            result.add(new ImpExpVariantKey(variantKey.getDocumentId(), branch, language));
        }

        return result;
    }
}
