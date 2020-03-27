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
package org.outerj.daisy.books.store.impl;

import org.outerj.daisy.books.store.*;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookstoremeta.PublicationsInfoDocument;
import org.apache.xmlbeans.XmlOptions;

import java.io.InputStream;
import java.util.List;

/**
 * Builds a PublicationsInfo object from its XML description.
 */
public class PublicationsInfoBuilder {
    public static PublicationsInfo build(InputStream is) throws Exception {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            return build(PublicationsInfoDocument.Factory.parse(is, xmlOptions));
        } finally {
            is.close();
        }
    }

    public static PublicationsInfo build(PublicationsInfoDocument publicationsInfoDocument) throws Exception {
        String errors = XmlUtil.validate(publicationsInfoDocument);
        if (errors != null)
            throw new Exception("The publications info XML is not valid according to its XML Schema, encountered errors: " + errors);

        List<PublicationsInfoDocument.PublicationsInfo.PublicationInfo> infosXml = publicationsInfoDocument.getPublicationsInfo().getPublicationInfoList();
        PublicationInfo[] infos = new PublicationInfo[infosXml.size()];
        for (int i = 0; i < infos.length; i++) {
            PublicationsInfoDocument.PublicationsInfo.PublicationInfo infoXml = infosXml.get(i);
            infos[i] = new PublicationInfo(infoXml.getName(), infoXml.getLabel(), infoXml.getStartResource(),
                    infoXml.getPackage(), infoXml.getPublishedBy(), infoXml.getPublishedOn().getTime());
        }

        return new PublicationsInfo(infos);
    }
}
