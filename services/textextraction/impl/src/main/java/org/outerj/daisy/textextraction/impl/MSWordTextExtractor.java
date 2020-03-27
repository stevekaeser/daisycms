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

import java.io.InputStream;
import java.util.List;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

/**
 * Text extractor for Microsoft Word files.
 */
public class MSWordTextExtractor extends AbstractTextExtractor implements TextExtractor {

    public MSWordTextExtractor() {
        super();
    }

    public MSWordTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        HWPFDocument wordDoc = new HWPFDocument(is);
        Range range = wordDoc.getRange();
        return range.text();
    }
}
