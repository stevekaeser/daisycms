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
package org.outerj.daisy.books.store;

import org.outerj.daisy.books.store.impl.XmlUtil;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookstoremeta.BookAclDocument;
import org.apache.xmlbeans.XmlOptions;

import java.io.InputStream;
import java.io.Reader;
import java.util.List;

/**
 * Builds a BookAcl object from its XML description.
 */
public class BookAclBuilder {
    public static BookAcl build(InputStream is) throws Exception {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            BookAclDocument aclDocument = BookAclDocument.Factory.parse(is, xmlOptions);
            return build(aclDocument);
        } finally {
            if (is != null)
                is.close();
        }
    }

    public static BookAcl build(Reader reader) throws Exception {
        try {
            BookAclDocument aclDocument = BookAclDocument.Factory.parse(reader);
            return build(aclDocument);
        } finally {
            if (reader != null)
                reader.close();
        }
    }

    public static BookAcl build(BookAclDocument aclDocument) throws Exception {
        String errors = XmlUtil.validate(aclDocument);
        if (errors != null)
            throw new Exception("The ACL XML is not valid according to its XML Schema, encountered errors: " + errors);

        List<BookAclDocument.BookAcl.BookAclEntry> entriesXml = aclDocument.getBookAcl().getBookAclEntryList();
        BookAclEntry[] entries = new BookAclEntry[entriesXml.size()];
        for (int i = 0; i < entriesXml.size(); i++) {
            BookAclDocument.BookAcl.BookAclEntry entryXml = entriesXml.get(i);
            BookAclSubjectType subjectType = BookAclSubjectType.fromString(entryXml.getSubjectType().toString());
            BookAclActionType readPermission = BookAclActionType.fromString(entryXml.getPermRead().toString());
            BookAclActionType managePermission = BookAclActionType.fromString(entryXml.getPermManage().toString());
            entries[i] = new BookAclEntry(subjectType, entryXml.getSubjectValue(), readPermission, managePermission);
        }

        return new BookAcl(entries);
    }
}
