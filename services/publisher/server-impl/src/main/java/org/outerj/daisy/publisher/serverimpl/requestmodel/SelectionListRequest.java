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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.outerj.daisy.repository.schema.FieldType;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.apache.xmlbeans.XmlObject;

public class SelectionListRequest extends AbstractRequest implements Request {
    private final String fieldTypeNameOrId;
    private final PubReqExpr branchExpr;
    private final PubReqExpr languageExpr;

    public SelectionListRequest(String fieldTypeNameOrId, PubReqExpr branchExpr, PubReqExpr languageExpr, LocationInfo locationInfo) {
        super(locationInfo);
        this.fieldTypeNameOrId = fieldTypeNameOrId;
        this.branchExpr = branchExpr;
        this.languageExpr = languageExpr;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        long branchId = branchExpr.evalAsBranchId(publisherContext, this);
        long languageId = languageExpr.evalAsLanguageId(publisherContext, this);

        FieldType fieldType = publisherContext.getRepository().getRepositorySchema().getFieldType(fieldTypeNameOrId, false);
        XmlObject expListXml = fieldType.getExpandedSelectionListXml(branchId, languageId, publisherContext.getLocale());

        if (expListXml != null)
            expListXml.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
}
