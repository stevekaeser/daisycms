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
package org.outerj.daisy.textextraction.impl;

import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

import java.io.*;
import java.util.List;

public class PlainTextExtractor extends AbstractTextExtractor implements TextExtractor {
    private static final int BUFFER_SIZE = 32768;

    public PlainTextExtractor() {
        super();
    }

    public PlainTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        StringBuilder result = new StringBuilder();
        Reader reader = new BufferedReader(new InputStreamReader(is));
        char[] buffer = new char[BUFFER_SIZE];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            result.append(buffer, 0, read);
        }
        return result.toString();
    }
}
