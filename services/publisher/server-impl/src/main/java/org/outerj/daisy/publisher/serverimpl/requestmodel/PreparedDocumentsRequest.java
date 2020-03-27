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
import org.outerj.daisy.repository.*;
import org.outerj.daisy.publisher.serverimpl.docpreparation.PreparedDocuments;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.PublisherException;

import java.util.Map;
import java.util.HashMap;

public class PreparedDocumentsRequest extends AbstractRequest implements Request {
    private final VariantKey navigationDoc;
    private final boolean applyDocumentSpecificStyling;
    private final String pubReqSet;
    private final String displayContext;
    private final boolean doDiff;
    private final String diffList;
    
    
    /**
     *
     * @param navigationDoc allowed to be null
     */
    public PreparedDocumentsRequest(VariantKey navigationDoc, String pubReqSet, boolean applyDocumentSpecificStyling, boolean doDiff, String diffList,
            String displayContext, LocationInfo locationInfo) {
        super(locationInfo);
        this.navigationDoc = navigationDoc;
        this.pubReqSet = pubReqSet;
        this.applyDocumentSpecificStyling = applyDocumentSpecificStyling;
        this.displayContext = displayContext;
        this.doDiff = doDiff;
        this.diffList  = diffList;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        Document document = publisherContext.getDocument();
        Version version = publisherContext.getVersion();
        if (version != null) {
            if (publisherContext.searchRecursivePrepDocs(document.getId(), document.getBranchId(), document.getLanguageId(), pubReqSet)) {
                throw new PublisherException("Detected recursive p:preparedDocuments call on same document variant using the same publishe request set. Document variant: "
                        + document.getVariantKey() + ", publisher request set: " + pubReqSet);
            }

            PreparedDocuments preparedDocuments = new PreparedDocuments(navigationDoc, pubReqSet, doDiff, diffList);
            PublisherImpl publisher = publisherContext.getPublisher();
            PublisherContextImpl childPublisherContext = new PublisherContextImpl(publisherContext);
            childPublisherContext.setPreparedDocuments(preparedDocuments);
            childPublisherContext.setContentProcessor(null); // for the case where we have nested preparedDocuments calls
            childPublisherContext.setVersionMode(publisherContext.getVersionMode());

            PreparedDocuments.PreparedDocument preparedDocument = preparedDocuments.getNewPreparedDocument(document.getVariantKey());
            publisher.performRequest(pubReqSet, document, version, childPublisherContext, new StripDocumentHandler(preparedDocument.getSaxBuffer()));
            
            Map<String, String> attrs = new HashMap<String, String>();
            attrs.put("applyDocumentTypeStyling", String.valueOf(applyDocumentSpecificStyling));
            if (displayContext != null)
                attrs.put("displayContext", displayContext);
            preparedDocuments.generateSax(contentHandler, attrs);
        }
    }
}
