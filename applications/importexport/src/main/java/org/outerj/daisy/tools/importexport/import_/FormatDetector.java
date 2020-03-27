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
package org.outerj.daisy.tools.importexport.import_;

import org.outerj.daisy.tools.importexport.model.meta.ImpExpMeta;
import org.outerj.daisy.tools.importexport.model.meta.ImpExpMetaDexmlizer;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.ImpExpFormat;

import java.io.InputStream;

/**
 * Tries to find out the {@link ImpExpFormat format} of an export. This is done by reading the
 * meta data.
 */
public class FormatDetector {

    public static ImpExpFormat detectFormat(ImportFile importFile) throws Exception {
        return detectFormat(importFile, null);
    }

    /**
     *
     * @param format user specified format, takes precendence over auto-detected format.
     */
    public static ImpExpFormat detectFormat(ImportFile importFile, String format) throws Exception {
        if (format == null) {
            ImpExpMeta meta = readMeta(importFile);
            format = meta.getExportFormat();
        }

        if (format == null || format.equals("default")) {
            return ImpExpFormat.DEFAULT;
        } else if (format.equals("tm")) {
            return ImpExpFormat.TRANSLATION_MANAGEMENT;
        } else {
            throw new ImportExportException("Unsupported format: \"" + format + "\".");
        }
    }

    private static ImpExpMeta readMeta(ImportFile importFile) throws Exception {
        String META_FILE_PATH  = "info/meta.xml";

        if (!importFile.exists(META_FILE_PATH)) {
            return new ImpExpMeta();
        }

        InputStream is = null;
        try {
            is = importFile.getPath(META_FILE_PATH).getInputStream();
            return ImpExpMetaDexmlizer.fromXml(is);
        } finally {
            if (is != null)
                is.close();
        }
    }
}
