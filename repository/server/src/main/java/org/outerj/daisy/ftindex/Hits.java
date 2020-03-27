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
package org.outerj.daisy.ftindex;

import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.repository.VariantKey;

public interface Hits {
    /**
     * Returns the VariantKey for the nth matching document.
     */
    VariantKey getVariantKey(int n);

    /**
     * Returns the list of all variant keys, sorted by score (descending).
     */
    List<VariantKey> getAllVariantKeys();

    /**
     * Returns the total number of hits available in this set.
     */
    int length();

    /**
     * Returns the score for the nth document in this set.
     */
    float score(int n);

    /**
     * Returns the score for a document in the result set with is the given VariantKey.
     */
    float score(VariantKey key);
    
    /**
     * Returns a contextualized text fragment based on the sought after terms
     */
    XmlObject contextFragments(int n, int fragmentAmount) throws Exception;

    /**
     * Returns a contextualized text fragment based on the sought after terms
     */
    XmlObject contextFragments(VariantKey key, int fragmentAmount) throws Exception;

    /**
     * Cleans up the Hits object
     */
    void dispose();

}
