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
package org.outerj.daisy.publisher.serverimpl.requestmodel;

import org.xml.sax.ContentHandler;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.repository.Repository;

public class AvailableVariantsRequest extends AbstractRequest {
    public AvailableVariantsRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Repository repository = publisherContext.getRepository();
        repository.getAvailableVariants(publisherContext.getDocumentId()).getXml(true).save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
}
