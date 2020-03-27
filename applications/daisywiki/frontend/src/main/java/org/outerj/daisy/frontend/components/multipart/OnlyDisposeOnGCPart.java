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
package org.outerj.daisy.frontend.components.multipart;

import java.io.IOException;
import java.io.InputStream;

import org.apache.cocoon.servlet.multipart.Part;

public class OnlyDisposeOnGCPart extends Part {

    private Part part;
    
    public OnlyDisposeOnGCPart(Part part) {
        super(part.getHeaders());
        if (part == null) {
            throw new NullPointerException("part should not be null");
        }
        this.part=part;
    }

    @Override
    public void dispose() {
        // intentionally do not dispose part
    }

    @Override
    public String getFileName() {
        return part.getFileName();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return part.getInputStream();
    }

    @Override
    public int getSize() {
        return part.getSize();
    }

    /**
     * This is probably not even necessary because the part will probably be GC'd as well.
     */
    @Override
    protected void finalize() throws Throwable {
        part.dispose();
        this.part = null;
    }

    
        
}
