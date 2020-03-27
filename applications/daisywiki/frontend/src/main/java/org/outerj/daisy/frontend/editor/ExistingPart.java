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
package org.outerj.daisy.frontend.editor;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.servlet.multipart.Part;
import org.outerj.daisy.repository.RepositoryException;

public class ExistingPart extends Part {
    protected org.outerj.daisy.repository.Part part;
    
    protected ExistingPart(org.outerj.daisy.repository.Part part) {
        super(prepareHeaders(part));
        this.part = part; 
    }

    public String getFileName() {
        return null;
    }
    
    public int getSize() {
        return (int)part.getSize();
    }

    public InputStream getInputStream() throws IOException {
        try {
            return part.getDataStream();
        } catch (RepositoryException exception) {
            throw new RuntimeException("Exception while trying to obatin datastream for part " + part.getTypeName());
        }
    }

    public void dispose() {
    }
    
    protected static Map prepareHeaders(org.outerj.daisy.repository.Part part) {
        Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("filename", part == null ? null : part.getFileName()==null?"data":part.getFileName());
        headers.put("content-type", part == null ? null : part.getMimeType()); 
        return headers;
    }
}
