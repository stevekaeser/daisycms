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

import java.util.Collections;
import java.util.List;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.repository.VariantKey;

public class EmptyHits implements Hits {
    public VariantKey getVariantKey(int n) {
        throw new IndexOutOfBoundsException("Invalid hit number: " + n);
    }

    public int length() {
        return 0;
    }

    public float score(int n) {
        throw new IndexOutOfBoundsException("Invalid hit number: " + n);
    }

    public XmlObject contextFragments(int n, int fragmentAmount) throws Exception {
        throw new IndexOutOfBoundsException("Invalid hit number: " + n);
    }

    public XmlObject contextFragments(VariantKey key, int fragmentAmount) throws Exception {
        throw new IndexOutOfBoundsException("Invalid document variant: " + key);
    }

    public float score(VariantKey key) {
        throw new IndexOutOfBoundsException("Invalid document variant: " + key);
    }

    public void dispose() {
    }

    public List<VariantKey> getAllVariantKeys() {
        return Collections.emptyList();
    }
}
