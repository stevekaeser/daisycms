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
package org.outerj.daisy.navigation.impl;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.RepositoryException;

import java.util.Map;
import java.util.HashMap;

public class NavigationTree {
    private RootNode node;
    private Map<VariantKey, String> nodeLookupMap;

    public NavigationTree(RootNode node) {
        this.node = node;
    }

    public RootNode getRootNode() {
        return node;
    }
    
    public String lookupNode(VariantKey variantKey) throws RepositoryException {
        if (nodeLookupMap == null) {
            synchronized (this) {
                if (nodeLookupMap == null) {
                    Map<VariantKey, String> map = new HashMap<VariantKey, String>();
                    node.populateNodeLookupMap(map, "");
                    this.nodeLookupMap = map;
                }
            }
        }
        return nodeLookupMap.get(variantKey);
    }
    
}
