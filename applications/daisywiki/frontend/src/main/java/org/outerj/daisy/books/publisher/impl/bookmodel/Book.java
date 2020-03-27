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
package org.outerj.daisy.books.publisher.impl.bookmodel;

import org.outerj.daisy.repository.VersionKey;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookdef.BookDocument;
import org.outerx.daisy.x10Bookdef.SectionContainerXml;
import org.outerx.daisy.x10Bookdef.impl.BookDocumentImpl;
import org.outerx.daisy.x10Bookdeps.BookDependenciesDocument;
import org.outerx.daisy.x10Bookdeps.BookDependenciesDocument.BookDependencies.Dependency;
import org.xml.sax.SAXException;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class Book implements SectionContainer {
    private SectionContainerHelper sectionContainerHelper = new SectionContainerHelper();

    public void addSection(Section section) {
        sectionContainerHelper.addSection(section);
    }

    public Section[] getSections() {
        return sectionContainerHelper.getSections();
    }

    public void store(OutputStream os) throws IOException {
        BookDocument bookDocument = BookDocument.Factory.newInstance();
        BookDocument.Book bookXml = bookDocument.addNewBook();

        SectionContainerXml content = bookXml.addNewContent();
        sectionContainerHelper.addXml(content);

        XmlOptions xmlOptions = new XmlOptions();
        xmlOptions.setSavePrettyPrint();
        bookDocument.save(os, xmlOptions);
    }

    public void load(InputStream processBookDefinitionStream) throws ParserConfigurationException, SAXException, XmlException, IOException {
        if (processBookDefinitionStream != null) {
            BookDocument bookDocument = BookDocument.Factory.parse(processBookDefinitionStream);
            List<org.outerx.daisy.x10Bookdef.SectionDocument.Section> sectionList = bookDocument.getBook().getContent().getSectionList();
            for (org.outerx.daisy.x10Bookdef.SectionDocument.Section section : sectionList) {
                Section s = new Section();
                s.setXml(section);
                this.addSection(s);
            }

        }
    }

}
