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
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.publisher.serverimpl.PublisherImpl;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.httpconnector.spi.HttpUtil;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.apache.xmlbeans.XmlObject;

import java.util.List;
import java.util.ArrayList;

public class GroupRequest extends AbstractRequest implements ParentPublisherRequest {
    private final PubReqExpr idExpr;
    private final boolean catchErrors;
    private List<Request> requests = new ArrayList<Request>();

    public GroupRequest(PubReqExpr idExpr, boolean catchErrors, LocationInfo locationInfo) {
        super(locationInfo);
        this.idExpr = idExpr;
        this.catchErrors = catchErrors;
    }

    public void addRequest(Request request) {
        requests.add(request);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        String id = idExpr.evalAsString(publisherContext, this);
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "id", "id", "CDATA", id);

        if (catchErrors) {
            // When catching errors, we need to buffer the output of the processing of
            // the children, since due to the streaming nature of SAX it's otherwise
            // impossible to recover
            SaxBuffer buffer = new SaxBuffer();
            XmlObject errorXml = null;
            try {
                processChildren(buffer, publisherContext);
            } catch (Throwable e) {
                errorXml = HttpUtil.buildErrorXml(e);
            }

            if (errorXml != null)
                attrs.addAttribute("", "error", "error", "CDATA", "true");
            contentHandler.startElement(PublisherImpl.NAMESPACE, "group", "p:group", attrs);

            if (errorXml != null) {
                errorXml.save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
            } else {
                buffer.toSAX(contentHandler);
            }
        } else {
            contentHandler.startElement(PublisherImpl.NAMESPACE, "group", "p:group", attrs);
            processChildren(contentHandler, publisherContext);
        }

        contentHandler.endElement(PublisherImpl.NAMESPACE, "group", "p:group");
    }

    private void processChildren(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        for (Request request : requests) {
            request.process(contentHandler, publisherContext);
        }
    }
}
