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

import java.util.List;
import java.util.ArrayList;

public abstract class AbstractParentPublisherRequest extends AbstractRequest implements ParentPublisherRequest {
    private List<Request> requests = new ArrayList<Request>();

    public AbstractParentPublisherRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void addRequest(Request request) {
        this.requests.add(request);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        for (Request request : requests) {
            request.process(contentHandler, publisherContext);
        }
    }
}
