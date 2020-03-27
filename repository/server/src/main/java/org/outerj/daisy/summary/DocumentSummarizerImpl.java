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
package org.outerj.daisy.summary;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.schema.RepositorySchema;

import java.io.InputStream;
import java.io.BufferedInputStream;


public class DocumentSummarizerImpl implements DocumentSummarizer {
    private static final int SUMMARY_LENGTH = 300;

    public String getSummary(Document document, long versionId, RepositorySchema repositorySchema) throws Exception {
        Part[] parts;

        if (versionId == -1) {
            parts = document.getPartsInOrder().getArray();
        } else {
            parts = document.getVersion(versionId).getPartsInOrder().getArray();
        }

        for (Part part : parts) {
            if (part.getMimeType().equals("text/xml")) {
                boolean daisyHtml = repositorySchema.getPartTypeById(part.getTypeId(), false).isDaisyHtml();
                if (daisyHtml) {
                    String summary;
                    InputStream is = new BufferedInputStream(part.getDataStream());
                    try {
                        summary = HtmlSummarizer.extractSummary(is, SUMMARY_LENGTH);
                    } finally {
                        is.close();
                    }
                    return summary;
                }
            }
        }

        return null;
    }
}
