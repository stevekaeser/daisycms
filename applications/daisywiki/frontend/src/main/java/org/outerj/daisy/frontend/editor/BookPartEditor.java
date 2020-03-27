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
package org.outerj.daisy.frontend.editor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.cocoon.forms.formmodel.Field;
import org.apache.cocoon.forms.formmodel.Widget;
import org.apache.cocoon.forms.formmodel.WidgetState;
import org.apache.xmlbeans.XmlOptions;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.schema.PartTypeUse;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerx.daisy.x10Bookdef.BookDocument;
import org.outerx.daisy.x10Bookdef.SectionDocument;
import org.outerx.daisy.x10Publisher.PublisherRequestDocument;
import org.outerx.daisy.x10Publisher.ResolveDocumentIdsDocument;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class BookPartEditor extends AbstractPartEditor {
    
    public BookPartEditor(PartTypeUse partTypeUse,
            Map<String, String> properties,
            DocumentEditorContext documentEditorContext) {
        super(partTypeUse, properties, documentEditorContext);
    }

    public static class Factory implements PartEditorFactory {
        public PartEditor getPartEditor(PartTypeUse partTypeUse, Map properties, DocumentEditorContext documentEditorContext) {
            return new BookPartEditor(partTypeUse, properties, documentEditorContext);
        }
    }

    @Override
    protected String getDefinitionStylesheet() {
        return null;
    }

    @Override
    protected String getDefinitionTemplate() {
        return "wikidata:/resources/form/parteditor_book_definition.xml";
    }

    public String getFormTemplate() {
        return "wikidata:/resources/form/parteditor_book_template.xml";
    }
    
    public void init(Widget parentWidget, boolean readonly) {
        super.init(parentWidget, readonly);
        if (readonly) {
            widget.lookupWidget("book").setState(WidgetState.DISABLED);
            widget.lookupWidget("validateEditors").setState(WidgetState.DISABLED);
        }
        ValidationCondition validateOnSave = new ValidateOnSaveCondition(documentEditorContext);
        widget.lookupWidget("book").addValidator(new ConditionalValidator(validateOnSave, new PartRequiredValidator(partTypeUse.isRequired(), true)));
    }

    public void load(Document document) throws Exception {
        // annotate book tree XML with document names
        byte[] data = document.getPart(partTypeUse.getPartType().getId()).getData();
        String result = annotateWithDocumentTitles(data, document, documentEditorContext.getRepository());
        Field field = (Field)widget.lookupWidget("book");
        if (result != null) {
            field.setValue(result);
        } else {
            field.setValue(new String(data, "UTF-8"));
        }
    }

    public void save(Document document) throws Exception {
        Field field = (Field)widget.lookupWidget("book");
        String value = (String)field.getValue();
        byte[] data = null;
        if (value != null) {
            // remove (auto-added) titles again from section nodes which have an ID
            try {
                XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
                BookDocument bookDocument = BookDocument.Factory.parse(new StringReader(value), xmlOptions);
                removeTitles(bookDocument.getBook().getContent().getSectionList());
                ByteArrayOutputStream os = new ByteArrayOutputStream(value.length() + 300);
                bookDocument.save(os);
                data = os.toByteArray();
            } catch (Throwable e) {
                // don't fail on this
            }
        }
        PartEditorHelper.save(widget.lookupWidget("book"), partTypeUse.getPartType(), document, "text/xml", data);
    }

    private String annotateWithDocumentTitles(byte[] data, Document document, Repository repository) {
        try {
            XmlOptions xmlOptions = new XmlOptions().setLoadUseXMLReader(LocalSAXParserFactory.newXmlReader());
            BookDocument bookDocument = BookDocument.Factory.parse(new ByteArrayInputStream(data), xmlOptions);
            PublisherRequestDocument publisherRequestDocument = PublisherRequestDocument.Factory.newInstance();
            PublisherRequestDocument.PublisherRequest publisherRequest = publisherRequestDocument.addNewPublisherRequest();
            ResolveDocumentIdsDocument.ResolveDocumentIds resolveDocIds = publisherRequest.addNewResolveDocumentIds();
            resolveDocIds.setBranch(String.valueOf(document.getBranchId()));
            resolveDocIds.setLanguage(String.valueOf(document.getLanguageId()));

            List<SectionDocument.Section> sections = bookDocument.getBook().getContent().getSectionList();
            buildRequest(sections, resolveDocIds);
            Publisher publisher = (Publisher)repository.getExtension("Publisher");
            ResolvedIdCollector collector = new ResolvedIdCollector();
            publisher.processRequest(publisherRequestDocument, collector);
            annotateSections(sections, collector.getNames(), -1);

            return bookDocument.xmlText();
        } catch (Throwable e) {
            // resolving IDs failed, don't care
            return null;
        }
    }

    private void buildRequest(List<SectionDocument.Section> sections, ResolveDocumentIdsDocument.ResolveDocumentIds resolveDocIds) {
        for (SectionDocument.Section section : sections) {
            if (section.isSetDocumentId()) {
                ResolveDocumentIdsDocument.ResolveDocumentIds.Document document = resolveDocIds.addNewDocument();
                document.setId(section.getDocumentId());
                if (section.isSetBranch())
                    document.setBranch(section.getBranch());
                if (section.isSetLanguage())
                    document.setLanguage(section.getLanguage());
                if (section.isSetVersion())
                    document.setVersion(section.getVersion());
            }
            buildRequest(section.getSectionList(), resolveDocIds);
        }
    }

    private int annotateSections(List<SectionDocument.Section> sections, List names, int pos) {
        for (SectionDocument.Section section : sections) {
            if (section.isSetDocumentId()) {
                pos++;
                section.setTitle((String)names.get(pos));
            }
            pos = annotateSections(section.getSectionList(), names, pos);
        }
        return pos;
    }

    static class ResolvedIdCollector extends DefaultHandler {
        private List<String> documentNames = new ArrayList<String>(50);

        public List<String> getNames() {
            return documentNames;
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (uri.equals("http://outerx.org/daisy/1.0#publisher") && localName.equals("document")) {
                documentNames.add(attributes.getValue("name"));
            }
        }
    }

    private void removeTitles(List<SectionDocument.Section> sections) {
        for (SectionDocument.Section section : sections) {
            if (section.isSetDocumentId() && section.isSetTitle()) {
                section.unsetTitle();
            }
            removeTitles(section.getSectionList());
        }
    }

}