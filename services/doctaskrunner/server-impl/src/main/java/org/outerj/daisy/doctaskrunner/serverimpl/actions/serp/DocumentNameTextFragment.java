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
package org.outerj.daisy.doctaskrunner.serverimpl.actions.serp;

import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.RepositoryException;

public class DocumentNameTextFragment implements TextFragment {
    
    private Map<String, String> attributes = Collections.singletonMap("type", "documentname");
    
    private final String originalName;
    private final Document document;
    
    public DocumentNameTextFragment(Document document) {
        try {
            this.originalName = document.getLastVersion().getDocumentName();
        } catch (RepositoryException e) {
            throw new RuntimeException("Could not get live version", e);
        }
        this.document = document;
    }

    public String getOriginalText() {
        return originalName;
    }

    public int replace(Pattern pattern, String replacement,
            boolean useSensibleCase) {

        document.setName(SearchAndReplaceUtil.performSimpleTextReplacement(originalName, replacement, pattern, useSensibleCase));
        return SearchAndReplaceUtil.countMatches(originalName, pattern);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

}
