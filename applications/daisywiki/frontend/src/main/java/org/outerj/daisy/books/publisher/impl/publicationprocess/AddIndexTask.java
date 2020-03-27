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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.transformation.I18nTransformer;

import javax.xml.parsers.SAXParser;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.Collator;
import java.io.InputStream;
import java.io.OutputStream;

public class AddIndexTask implements PublicationProcessTask {
    private final String input;
    private final String output;

    public AddIndexTask(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running add index task.");
        BookInstance bookInstance = context.getBookInstance();
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bookInstance.getResource(publicationOutputPath + input);
            os = bookInstance.getResourceOutputStream(publicationOutputPath + output);
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            XmlSerializer serializer = new XmlSerializer(os);
            AddIndexHandler addIndexHandler = new AddIndexHandler(serializer, context.getLocale());
            parser.getXMLReader().setContentHandler(addIndexHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) {
                    context.getPublicationLog().error("Error closing input stream.", e);
                }
            if (os != null)
                try { os.close(); } catch (Exception e) {
                    context.getPublicationLog().error("Error closing output stream.", e);
                }
        }
    }

    static class AddIndexHandler extends ForwardingContentHandler {
        private Index index = new Index();
        private StringBuilder indexEntryBuffer = new StringBuilder();
        private int indexEntryNesting = -1;
        private String indexEntryId = null;
        private int nesting = 0;
        private int indexNameCounter = 1;
        private Locale locale;

        public AddIndexHandler(ContentHandler consumer, Locale locale) {
            super(consumer);
            this.locale = locale;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            nesting++;
            // note: the indexEntryNesting == -1 test is to ignore nested indexentry-spans.
            if (indexEntryNesting == -1 && namespaceURI.equals("") && localName.equals("span")) {
                String className = atts.getValue("class");
                if ("indexentry".equals(className)) {
                    indexEntryBuffer.setLength(0);
                    indexEntryNesting = nesting;

                    String id = atts.getValue("id");
                    if (id == null) {
                        id = "dsy_idx_" + indexNameCounter++;
                        AttributesImpl newAttrs = new AttributesImpl(atts);
                        newAttrs.addCDATAAttribute("id", id);
                        atts = newAttrs;
                    }
                    indexEntryId = id;
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (indexEntryNesting == nesting) {
                // end of index entry
                String entry = indexEntryBuffer.toString();
                IndexEntry indexEntry = index.getEntry(entry);
                if (indexEntry != null)
                    indexEntry.addId(indexEntryId);

                // cleanup
                indexEntryNesting = -1;
                indexEntryId = null;
            } else if (nesting == 2 && namespaceURI.equals("") && localName.equals("body")) {
                // closing body tag, insert index if not empty
                index.generateSaxFragment(consumer, locale);
            }
            nesting--;
            super.endElement(namespaceURI, localName, qName);
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            if (indexEntryNesting != -1)
                indexEntryBuffer.append(ch, start, length);
            super.characters(ch, start, length);
        }
    }

    static class Index {
        private Map<String, IndexEntry> entries = new HashMap<String, IndexEntry>();

        public IndexEntry getEntry(String name) {
            Pattern subentryPattern = Pattern.compile("([^:]+)");
            Matcher subentryMatcher = subentryPattern.matcher(name);
            IndexEntry indexEntry = null;
            while (subentryMatcher.find()) {
                String subentry = subentryMatcher.group(1).trim();
                if (subentry.length() == 0)
                    continue;
                if (indexEntry == null) {
                    indexEntry = entries.get(subentry);
                    if (indexEntry == null) {
                        indexEntry = new IndexEntry(subentry);
                        entries.put(subentry, indexEntry);
                    }
                } else {
                    indexEntry = indexEntry.getSubEntry(subentry);
                }
            }
            return indexEntry;
        }

        public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
            if (entries.size() == 0)
                return;

            contentHandler.characters(new char[] {'\n'}, 0, 1);
            AttributesImpl indexHeaderAttrs = new AttributesImpl();
            indexHeaderAttrs.addCDATAAttribute("id", "index");
            contentHandler.startElement("", "h1", "h1", indexHeaderAttrs);

            AttributesImpl indexTitleAttrs = new AttributesImpl();
            indexTitleAttrs.addCDATAAttribute("key", "index");
            contentHandler.startPrefixMapping("i18n", I18nTransformer.I18N_NAMESPACE_URI);
            contentHandler.startElement(I18nTransformer.I18N_NAMESPACE_URI, "text", "i18n:text", indexTitleAttrs);
            contentHandler.endElement(I18nTransformer.I18N_NAMESPACE_URI, "text", "i18n:text");
            contentHandler.endPrefixMapping("i18n");

            contentHandler.endElement("", "h1", "h1");

            contentHandler.characters(new char[] {'\n'}, 0, 1);
            contentHandler.startElement("", "index", "index", new AttributesImpl());

            IndexEntry[] entries = this.entries.values().toArray(new IndexEntry[0]);
            IndexEntryComparator comparator = new IndexEntryComparator(locale);
            Arrays.sort(entries, comparator);

            IndexEntry prevEntry = null;
            for (int i = 0; i < entries.length; i++) {
                if (prevEntry == null || isDifferentGroup(prevEntry, entries[i])) {
                    if (prevEntry != null) {
                        contentHandler.characters(new char[] {'\n'}, 0, 1);
                        contentHandler.endElement("", "indexGroup", "indexGroup");
                    }
                    AttributesImpl indexGroupAtrrs = new AttributesImpl();
                    if (Character.isLetter(entries[i].getName().charAt(0)))
                        indexGroupAtrrs.addCDATAAttribute("name", String.valueOf(Character.toUpperCase(entries[i].getName().charAt(0))));
                    contentHandler.characters(new char[] {'\n'}, 0, 1);
                    contentHandler.startElement("", "indexGroup", "indexGroup", indexGroupAtrrs);
                }
                entries[i].generateSaxFragment(contentHandler, comparator);
                prevEntry = entries[i];
            }

            if (prevEntry != null) {
                contentHandler.characters(new char[] {'\n'}, 0, 1);
                contentHandler.endElement("", "indexGroup", "indexGroup");
            }

            contentHandler.characters(new char[] {'\n'}, 0, 1);
            contentHandler.endElement("", "index", "index");
        }

        public boolean isDifferentGroup(IndexEntry entry1, IndexEntry entry2) {
            char c1 = Character.toUpperCase(entry1.getName().charAt(0));
            char c2 = Character.toUpperCase(entry2.getName().charAt(0));
            return c1 != c2 && Character.isLetter(c2);
        }
    }

    static class IndexEntry {
        private String name;
        private List<String> ids = new ArrayList<String>();
        private Map<String, IndexEntry> children;

        public IndexEntry(String name) {
            this.name = name;
        }

        public void addId(String id) {
            this.ids.add(id);
        }

        public String getName() {
            return name;
        }

        public void addChild(IndexEntry entry) {
            if (children == null)
                children = new HashMap<String, IndexEntry>();
            children.put(entry.getName(), entry);
        }

        public IndexEntry getSubEntry(String name) {
            IndexEntry indexEntry = children == null ? null : children.get(name);
            if (indexEntry == null) {
                indexEntry = new IndexEntry(name);
                addChild(indexEntry);
            }
            return indexEntry;
        }

        public void generateSaxFragment(ContentHandler contentHandler, IndexEntryComparator comparator) throws SAXException {
            AttributesImpl entryAttrs = new AttributesImpl();
            entryAttrs.addAttribute("", "name", "name", "CDATA", getName());
            contentHandler.characters(new char[] {'\n'}, 0, 1);
            contentHandler.startElement("", "indexEntry", "indexEntry", entryAttrs);
            for (String id : ids) {
                contentHandler.characters(new char[]{'\n'}, 0, 1);
                contentHandler.startElement("", "id", "id", new AttributesImpl());
                contentHandler.characters(id.toCharArray(), 0, id.length());
                contentHandler.endElement("", "id", "id");
            }
            if (children != null) {
                IndexEntry[] entries = children.values().toArray(new IndexEntry[0]);
                Arrays.sort(entries, comparator);
                for (IndexEntry entry : entries) {
                    entry.generateSaxFragment(contentHandler, comparator);
                }
            }
            contentHandler.characters(new char[] {'\n'}, 0, 1);
            contentHandler.endElement("", "indexEntry", "indexEntry");
        }
    }

    static class IndexEntryComparator implements Comparator {
        private final Collator collator;

        public IndexEntryComparator(Locale locale) {
            this.collator = Collator.getInstance(locale);
        }

        public int compare(Object o1, Object o2) {
            String entry1 = ((IndexEntry)o1).getName();
            String entry2 = ((IndexEntry)o2).getName();
            return collator.compare(entry1, entry2);
        }
    }
}
