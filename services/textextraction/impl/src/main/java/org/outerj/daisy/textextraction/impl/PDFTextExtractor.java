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

import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.util.PDFTextStripper;
import org.outerj.daisy.textextraction.TextExtractor;
import org.outerj.daisy.plugin.PluginRegistry;

import java.io.CharArrayWriter;
import java.io.InputStream;
import java.util.List;

public class PDFTextExtractor extends AbstractTextExtractor implements TextExtractor {

    public PDFTextExtractor() {
        super();
    }

    public PDFTextExtractor(List<String> mimeTypes, PluginRegistry pluginRegistry) {
        super(mimeTypes, pluginRegistry);
    }

    protected String getName() {
        return getClass().getName();
    }

    public String getText(InputStream is) throws Exception {
        PDDocument pdfDocument = null;
        try {
            PDFParser parser = new PDFParser(is);
            parser.parse();

            pdfDocument = parser.getPDDocument();

            CharArrayWriter writer = new CharArrayWriter();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.writeText(pdfDocument, writer);

            return writer.toString();
        } finally {
            if( pdfDocument != null )
                pdfDocument.close();
        }
    }
}
