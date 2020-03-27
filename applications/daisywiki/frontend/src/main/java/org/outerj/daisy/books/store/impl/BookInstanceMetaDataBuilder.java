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

import java.io.InputStream;

import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.books.store.BookInstanceMetaData;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookstoremeta.BookInstanceMetaDataDocument;

/**
 * Builds a BookInstanceMetaData object based on its XML description.
 */
public class BookInstanceMetaDataBuilder {
    public static BookInstanceMetaData build(InputStream is) throws Exception {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            return build(BookInstanceMetaDataDocument.Factory.parse(is, xmlOptions));
        } finally {
            is.close();
        }
    }

    public static BookInstanceMetaData build(BookInstanceMetaDataDocument document) throws Exception {
        String errors = XmlUtil.validate(document);
        if (errors != null)
            throw new Exception("The meta data XML is not valid according to its XML Schema, encountered errors: " + errors);

        BookInstanceMetaDataDocument.BookInstanceMetaData metaDataXml = document.getBookInstanceMetaData();
        // default to live version for backwards compatibility
        String dataVersion = metaDataXml.getDataVersion() == null ? "live" : metaDataXml.getDataVersion();
        BookInstanceMetaData metaData = new BookInstanceMetaData(metaDataXml.getLabel(), metaDataXml.getCreatedOn().getTime(), metaDataXml.getCreatedBy(),  metaDataXml.getDataBranchId(), metaDataXml.getDataLanguageId(), VersionMode.get(dataVersion), metaDataXml.getBookDefinition(), metaDataXml.getUpdateFrom());
        if (metaDataXml.isSetBookPath())
            metaData.setBookPath(metaDataXml.getBookPath());
        
        return metaData;
    }
}
