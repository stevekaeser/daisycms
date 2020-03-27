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

import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.xml.sax.ContentHandler;


public class AclInfoRequest extends AbstractRequest {
    public AclInfoRequest(LocationInfo locationInfo) {
        super(locationInfo);
    }

    public void processInt(ContentHandler contentHandler, PublisherContext publisherContext) throws Exception {
        VariantKey variantKey = publisherContext.getVariantKey();
        Repository repository = publisherContext.getRepository();
        AccessManager accessManager = repository.getAccessManager();
        AclResultInfo aclResultInfo = accessManager.getAclInfoOnLive(repository.getUserId(), repository.getActiveRoleIds(), variantKey);
        aclResultInfo.getXml().save(new StripDocumentHandler(contentHandler), new DummyLexicalHandler());
    }
}
