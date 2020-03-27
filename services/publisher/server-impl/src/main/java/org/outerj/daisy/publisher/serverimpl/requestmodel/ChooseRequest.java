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

public class ChooseRequest extends AbstractRequest {
    private final List<ChooseWhen> whenBranches = new ArrayList<ChooseWhen>();
    private ChooseOtherwise chooseOtherwise;

    public ChooseRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }
    
    public void addWhen(ChooseWhen chooseWhen) {
        this.whenBranches.add(chooseWhen);
    }

    public void setOtherwise(ChooseOtherwise chooseOtherwise) {
        this.chooseOtherwise = chooseOtherwise;
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        for (ChooseWhen chooseWhen : whenBranches) {
            if (chooseWhen.matches(publisherContext)) {
                chooseWhen.process(contentHandler, publisherContext);
                return;
            }
        }
        if (chooseOtherwise != null) {
            chooseOtherwise.process(contentHandler, publisherContext);
        }
    }
}
