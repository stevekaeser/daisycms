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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.query.EvaluationContext;

/**
 * This ContentHandler executes queries and inserts include
 * instructions for the documents in the resultset (rather
 * then including the resultset itself).
 */
public class QueryIncludeProcessor extends QueriesProcessor {
    public QueryIncludeProcessor(ContentHandler consumer, ContentProcessor owner) {
        super(consumer, owner);
    }

    protected String getSensitiveClass() {
        return "query-and-include";
    }

    protected void executeQuery(String query, Attributes queryAttrs) throws SAXException {
        VariantKey[] result = null;
        try {
            EvaluationContext evaluationContext = new EvaluationContext();
            evaluationContext.setContextDocument(owner.getDocument(), owner.getVersion());
            result = queryManager.performQueryReturnKeys(query, null, getQueryOptions(), locale, evaluationContext);
        } catch (Exception e) {
            outputFailedQueryMessage(e, query);
        }

        String shiftHeadings = queryAttrs.getValue("daisy-shift-headings");

        if (result != null) {
            // output include instructions for each query result
            for (int i = 0; i < result.length; i++) {
                AttributesImpl attrs = new AttributesImpl();
                attrs.addAttribute("", "class", "class", "CDATA", "include");
                if (shiftHeadings != null)
                    attrs.addAttribute("", "daisy-shift-headings", "daisy-shift-headings", "CDATA", shiftHeadings);
                consumer.startElement("", "pre", "pre", attrs);
                VariantKey key = result[i];
                String url = "daisy:" + key.getDocumentId() + "@" + key.getBranchId() + ":" + key.getLanguageId();
                characters(url.toCharArray(), 0, url.length());
                consumer.endElement("", "pre", "pre");
            }
        }
    }
}
