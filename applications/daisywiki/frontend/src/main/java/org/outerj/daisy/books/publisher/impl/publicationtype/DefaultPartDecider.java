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
package org.outerj.daisy.books.publisher.impl.publicationtype;

import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.books.publisher.impl.dataretrieval.PartDecider;

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * An implementation of PartDecider which makes its decision
 * based on the part type name.
 */
public class DefaultPartDecider implements PartDecider {
    private Set<String> partTypeNames = new HashSet<String>();

    public DefaultPartDecider(Map attributes) {
        String partTypeNamesAttr = (String)attributes.get("partTypeNames");
        if (partTypeNamesAttr != null) {
            StringTokenizer tokenizer = new StringTokenizer(partTypeNamesAttr, ",");
            while (tokenizer.hasMoreTokens()) {
                String token = tokenizer.nextToken().trim();
                partTypeNames.add(token);
            }
        }
    }

    public boolean needsPart(long documentTypeId, String documentTypeName, long partTypeId, String partTypeName,
            String mimeType, String fileName, long size, VariantKey document, long versionId, Repository repository) {
        return partTypeNames.contains(partTypeName);
    }
}
