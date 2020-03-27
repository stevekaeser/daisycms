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
package org.outerj.daisy.repository.commonimpl;

import org.outerj.daisy.repository.LinkExtractorInfos;
import org.outerj.daisy.repository.LinkExtractorInfo;
import org.outerx.daisy.x10.LinkExtractorsDocument;
import org.outerx.daisy.x10.LinkExtractorDocument;

public class LinkExtractorInfosImpl implements LinkExtractorInfos {
    private final LinkExtractorInfo[] linkExtractorInfos;

    public LinkExtractorInfosImpl(LinkExtractorInfo[] linkExtractorInfos) {
        this.linkExtractorInfos = linkExtractorInfos;
    }

    public LinkExtractorInfo[] getArray() {
        return linkExtractorInfos;
    }

    public LinkExtractorsDocument getXml() {
        LinkExtractorDocument.LinkExtractor[] linkExtractorsXml = new LinkExtractorDocument.LinkExtractor[linkExtractorInfos.length];
        for (int i = 0; i < linkExtractorInfos.length; i++) {
            linkExtractorsXml[i] = linkExtractorInfos[i].getXml().getLinkExtractor();
        }

        LinkExtractorsDocument linkExtractorsDocument = LinkExtractorsDocument.Factory.newInstance();
        linkExtractorsDocument.addNewLinkExtractors().setLinkExtractorArray(linkExtractorsXml);
        return linkExtractorsDocument;
    }
}
