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
package org.outerj.daisy.repository.query;
import java.util.ArrayList;
import java.util.List;

import org.outerj.daisy.repository.VersionKey;


public class RemoteEvaluationContext {
    private List<VersionKey> contextDocs = new ArrayList<VersionKey>();
    
    public VersionKey getContextDocument() {
        return contextDocs.get(0);
    }
    
    public void setContextDocument(VersionKey key) {
        contextDocs.clear();
        contextDocs.add(key);
    }
    
    public VersionKey getContextDocument(int pos) {
        return contextDocs.get(pos);
    }
    
    /**
     * Pushes a context document on the stack of context documents.
     * The version is allowed to be null.
     */
    public void pushContextDocument(VersionKey versionKey) {
        contextDocs.add(versionKey);
    }

    /**
     * Removes a context document (the latest pushed one) from the stack of context documents.
     */
    public VersionKey popContextDocument() {
        if (contextDocs.size() > 0)
            return contextDocs.remove(contextDocs.size() - 1);
        else
            throw new RuntimeException("EvaluationContext: stack of context documents is empty");
    }
    
    public int size() {
        return this.contextDocs.size();
    }
    

}
