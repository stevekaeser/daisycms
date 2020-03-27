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
package org.outerj.daisy.htmlcleaner;

import org.xml.sax.SAXException;
import org.xml.sax.Locator;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;

import java.io.OutputStream;
import java.io.IOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.util.*;

/**
 * A special serializer for outputting XML-well-formed HTML.
 *
 * <p>This serializer is not meant as a general purpose serializer. Rather
 * it is used as part of a HTML cleaning pipeline whose goal is to normalize
 * html before storing it in a CMS.<p>
 *
 * <p>This serializer isn't designed or tested for performance, but that doesn't matter since it's
 * only used for update operations and for relatively small content blurbs.</p>
 *
 * <p>The serializer will limit the output width to a certain number of characters.
 * It can be configured to output a variable number of characters around the start
 * and end tags of certain elements. Sequences of multiple whitespace characters
 * are collapsed.</p>
 *
 * <p>The input must contain the html root tag.</p>
 *
 * <p>The output encoding is always UTF-8. Note that this can't simply be changed
 * because the serializer currently doesn't check whether characters can be
 * outputted in the given encoding (UTF-8 supports all of unicode, so such
 * checks are not required). The serializer does not check for characters
 * that are illegal in XML.</p>
 */
public class StylingHtmlSerializer implements ContentHandler {
    private Writer writer;
    private LineRenderer line;
    private StartElementInfo currentStartElement;
    private boolean inPreElement = false;
    private HtmlCleanerTemplate template;
    private OutputElementDescriptor dummy = new OutputElementDescriptor(0, 0, 0, 0, true);

    public StylingHtmlSerializer(HtmlCleanerTemplate template) {
        this.template = template;
    }

    public void setOutputStream(OutputStream outputStream) throws IOException {
        // Note: this serializer assumes hardcoded UTF-8. Otherwise it would need extra
        // functionality like character escaping etc.
        this.writer = new OutputStreamWriter(outputStream, "UTF-8");
        line = new LineRenderer();
        currentStartElement = null;
    }

    public void startDocument() throws SAXException {
    }

    public void endDocument() throws SAXException {
        try {
            line.flushLine(false);
            writer.flush();
        } catch (IOException e) {
            throw new SAXException(e);
        }
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        writePendingStartElement(false);
        line.writeText(escapeReservedCharacters(new String(ch, start, length)));
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        writePendingStartElement(false);
        currentStartElement = new StartElementInfo(localName, atts);
        if (localName.equals("pre")) {
            line.flushLine(false);
            OutputElementDescriptor descriptor = getElementDescriptor("pre");
            line.newLines(descriptor.getNewLinesBeforeOpenTag());
            inPreElement = true;
        }
    }

    public void writePendingStartElement(boolean empty) throws SAXException {
        if (currentStartElement != null) {
            String localName = currentStartElement.getLocalName();
            Attributes atts = currentStartElement.getAttrs();

            StringBuilder tag = new StringBuilder(localName.length() + 2 + (atts.getLength() * 50));
            tag.append('<').append(localName);

            if (atts.getLength() > 0) {
                for (int i = 0; i < atts.getLength(); i++) {
                    tag.append(' ');
                    tag.append(atts.getLocalName(i));
                    tag.append("=\"");
                    tag.append(escapeAttribute(atts.getValue(i)));
                    tag.append('"');
                }
            }

            if (empty) {
                tag.append("/>");
            } else {
                tag.append('>');
            }

            OutputElementDescriptor descriptor = getElementDescriptor(localName);
            if (!inPreElement)
                line.newLines(descriptor.getNewLinesBeforeOpenTag());
            line.writeStartTag(tag.toString(), descriptor);
            if (!inPreElement) {
                if (empty)
                    line.newLines(descriptor.getNewLinesAfterCloseTag());
                else
                    line.newLines(descriptor.getNewLinesAfterOpenTag());
            }

            if (localName.equals("pre"))
                line.flushLine(false);

            currentStartElement = null;
        }
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equals("pre"))
            inPreElement = false;

        if (currentStartElement != null) {
            writePendingStartElement(true);
        } else {
            String tag = "</" + localName + ">";
            OutputElementDescriptor descriptor = getElementDescriptor(localName);
            if (!inPreElement)
                line.newLines(descriptor.getNewLinesBeforeCloseTag());
            line.writeEndTag(tag, descriptor);
            if (!inPreElement)
                line.newLines(descriptor.getNewLinesAfterCloseTag());
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
    }

    public void endPrefixMapping(String prefix) throws SAXException {
    }

    public void skippedEntity(String name) throws SAXException {
    }

    public void setDocumentLocator(Locator locator) {
    }

    public void processingInstruction(String target, String data) throws SAXException {
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
    }

    public void endCDATA() throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void comment(char ch[], int start, int length) throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }

    private OutputElementDescriptor getElementDescriptor(String localName) {
        OutputElementDescriptor descriptor = template.outputElementDescriptors.get(localName);
        if (descriptor != null)
            return descriptor;
        return dummy;
    }

    /**
     * Escapes an attribute value assuming it is quoted in double quotes.
     */
    private String escapeAttribute(String value) {
        StringBuilder newValue = new StringBuilder(value.length() + 10);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    newValue.append("&quot;");
                    break;
                case '<': // strictly spoken only needed after ]]
                    newValue.append("&lt;");
                    break;
                case '>':
                    newValue.append("&gt;");
                    break;
                case '&':
                    newValue.append("&amp;");
                    break;
                default:
                    newValue.append(c);
            }
        }
        return newValue.toString();
    }

    private String escapeReservedCharacters(String text) {
        StringBuilder newText = new StringBuilder(text.length() + 10);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '<':
                    newText.append("&lt;");
                    break;
                case '>': // strictly spoken only needed after ]]
                    newText.append("&gt;");
                    break;
                case '&':
                    newText.append("&amp;");
                    break;
                default:
                    // if the character is not in the range allowed by XML, simply skip it
                    // (Note: neko html doesn't removed these characters when parsing, so we do it here)
                    if (c == 0x9 || c == 0xA || c == 0xD || (c >= 0x20 && c <= 0xD7FF) || (c >= 0xE000 && c <= 0xFFFD)
                            || (c >= 0x10000 && c <= 0x10FFFF))
                        newText.append(c);
            }
        }
        return newText.toString();
    }

    /**
     * Class that keeps track of the current line being written, and whether
     * a new line should be started and such. All output should happen via
     * this class.
     */
    private class LineRenderer {
        private Line line = new Line();

        /**
         * Outputs the given number of newlines.
         */
        public void newLines(int count) throws SAXException {
            try {
                if (count == 0)
                    return;

                if (line.getLength() > 0)
                    flushLine(false);

                if (count == 1)
                    writer.write('\n');
                else
                    for (int i = 0; i < count; i++)
                        writer.write('\n');
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        public void writeText(String text) throws SAXException {
            try {
                if (inPreElement) {
                    writer.write(text);
                } else {
                    List words = getWords(text);

                    if (startsWithWhitespace(text))
                        line.addSpace();

                    if (words.size() > 0) {

                        Iterator wordsIt = words.iterator();
                        boolean firstWord = true;

                        while (wordsIt.hasNext()) {
                            String word = (String)wordsIt.next();
                            if (line.getLength() > 0 && line.getLength() + word.length() + 1 > template.maxLineWidth) {
                                if (!line.endsOnWordOrSpace())
                                    writeText(line.emptyIfPossibleBeforeWord(), true);
                                else
                                    writeText(line.empty(), true);
                            }

                            if (!firstWord)
                                line.addSpace();
                            line.addWord(word);

                            firstWord = false;
                        }

                        if (endsWithWhitespace(text))
                            line.addSpace();
                    }
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        private void writeText(String text, boolean newLine) throws IOException {
            if (text != null) {
                writer.write(text);
                if (newLine)
                    writer.write('\n');
            }
        }

        private void flushLine(boolean newLine) throws SAXException {
            try {
                writeText(line.empty(), newLine);
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        private boolean startsWithWhitespace(String text) {
            if (text.length() == 0)
                return false;
            return Character.isWhitespace(text.charAt(0));
        }

        private boolean endsWithWhitespace(String text) {
            if (text.length() == 0)
                return false;
            return Character.isWhitespace(text.charAt(text.length() - 1));
        }

        public void writeStartTag(String text, OutputElementDescriptor descriptor) throws SAXException {
            try {
                if (inPreElement) {
                    writer.write(text);
                } else {
                    // if the line is full
                    if (line.getLength() > 0 && line.getLength() + 1 + text.length() > template.maxLineWidth) {
                        String toWrite;
                        if (descriptor.isInline())
                            toWrite = line.emptyIfPossibleBeforeInlineTag();
                        else
                            toWrite = line.empty();
                        writeText(toWrite, true);
                    }

                    line.addStartTag(text, descriptor);
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        public void writeEndTag(String text, OutputElementDescriptor descriptor) throws SAXException {
            try {
                if (inPreElement) {
                    writer.write(text);
                } else {
                    // if the line is full
                    if (line.getLength() > 0 && line.getLength() + text.length() > template.maxLineWidth) {
                        String toWrite;
                        if (descriptor.isInline())
                            toWrite = line.emptyIfPossibleBeforeInlineTag();
                        else
                            toWrite = line.empty();
                        writeText(toWrite, true);
                    }

                    line.addEndTag(text, descriptor);
                }
            } catch (IOException e) {
                throw new SAXException(e);
            }
        }

        private List getWords(String text) {
            List<String> words = new ArrayList<String>();
            int beginWord = -1;
            for (int i = 0; i < text.length(); i++) {
                if (Character.isWhitespace(text.charAt(i))) {
                    if (beginWord != -1) {
                        String newWord = text.substring(beginWord, i);
                        words.add(newWord);
                        beginWord = -1;
                    }
                } else if (beginWord == -1) {
                    beginWord = i;
                }
            }

            if (beginWord != -1) {
                String newWord = text.substring(beginWord);
                words.add(newWord);
            }

            return words;
        }
    }

    /**
     * The Line class keeps track of which items are on the current line,
     * and helps finding where the line can be split if it becomes to long.
     * Breaks are only allowed at whitespace locations or at 'block' tags,
     * NOT where two inline tags or an inline tag and a word are next to
     * each other whithout whitespace between them.
     */
    private class Line {
        private List<LineItem> lineItems = new ArrayList<LineItem>();
        private int length = 0;

        public void addStartTag(String text, OutputElementDescriptor descriptor) {
            lineItems.add(new StartTag(text, descriptor));
            length += text.length();
        }

        public void addEndTag(String text, OutputElementDescriptor descriptor) {
            if (!descriptor.isInline() && lineItems.size() > 0 && getLastLineItem() instanceof Space) {
                lineItems.remove(lineItems.size() - 1);
                length--; // one space removed
            }
            lineItems.add(new EndTag(text, descriptor));
            length += text.length();
        }

        public void addWord(String text) {
            lineItems.add(new Word(text));
            length += text.length();
        }

        public void addSpace() {
            boolean addSpace = true;

            if (lineItems.size() > 0) {
                LineItem lastItem = getLastLineItem();
                if (lastItem instanceof Space)
                    addSpace = false;
                else if (lastItem instanceof Tag && !((Tag)lastItem).descriptor.isInline())
                    addSpace = false;
            } else if (lineItems.size() == 0) {
                addSpace = false;
            }

            if (addSpace) {
                lineItems.add(new Space());
                length += 1;
            }
        }

        public boolean endsOnWordOrSpace() {
            if (lineItems.size() > 0) {
                LineItem lineItem = getLastLineItem();
                return lineItem instanceof Word || lineItem instanceof Space;
            } else {
                return false;
            }
        }

        public int getLength() {
            return length;
        }

        public String empty() {
            return empty(lineItems.size() - 1);
        }

        /**
         *
         * @param until index of last item to be included
         */
        public String empty(int until) {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i <= until; i++) {
                LineItem lineItem = lineItems.get(i);
                if (i == until && lineItem instanceof Space)
                    continue;
                text.append(lineItem.text);
            }
            lineItems = new ArrayList<LineItem>(lineItems.subList(until + 1, lineItems.size()));
            recalcLength();
            return text.toString();
        }

        private void recalcLength() {
            int newLength = 0;
            for (LineItem lineItem : lineItems) {
                newLength += lineItem.text.length();
            }
            this.length = newLength;
        }

        /**
         * If a word it to be added, but it doesn't fit anymore on the line, this method
         * is used to try to empty the line, either completely or as far as possible, depending
         * on whether there is an apropriate place to split the line.
         *
         * @return the text to be written out, or null if none
         */
        public String emptyIfPossibleBeforeWord() {
            LineItem lineItem = getLastLineItem();
            if (lineItem instanceof Tag && ((Tag)lineItem).descriptor.isInline()) {
                int splitPoint = searchSplitPoint();
                if (splitPoint == -1)
                    return null;
                else
                    return empty(splitPoint);
            } else {
                return empty();
            }
        }

        public String emptyIfPossibleBeforeInlineTag() {
            LineItem lineItem = getLastLineItem();
            if ((lineItem instanceof Tag && ((Tag)lineItem).descriptor.isInline()) || lineItem instanceof Word) {
                int splitPoint = searchSplitPoint();
                if (splitPoint == -1)
                    return null;
                else
                    return empty(splitPoint);
            } else {
                return empty();
            }
        }

        /**
         * This method should only be called if the last item on the line is a Word
         * or a inline Tag.
         *
         * Returns -1 if there's not suitable split point, otherwise returns the index
         * of the last item before the possible split point.
         */
        private int searchSplitPoint() {
            if (lineItems.size() < 2) {
                // there is only one item on the line, and it cannot be disconnected from the next item
                return -1;
            }

            LineItem previousLineItem = getLastLineItem();
            for (int i = lineItems.size() - 2; i >= 0; i--) {
                LineItem currentLineItem = lineItems.get(i);
                if (currentLineItem instanceof Word && previousLineItem instanceof Word) {
                    // between two words, we can split
                    return i;
                } else if (currentLineItem instanceof Space) {
                    return i;
                } else if (currentLineItem instanceof Tag && !((Tag)currentLineItem).descriptor.isInline()) {
                    return i;
                }
                previousLineItem = currentLineItem;
            }
            return -1;
        }

        private LineItem getLastLineItem() {
            return lineItems.get(lineItems.size() - 1);
        }

        abstract class LineItem {
            final String text;

            public LineItem(String text) {
                this.text = text;
            }
        }

        abstract class Tag extends LineItem {
            final OutputElementDescriptor descriptor;

            public Tag(String text, OutputElementDescriptor descriptor) {
                super(text);
                this.descriptor = descriptor;
            }
        }

        class StartTag extends Tag {
            public StartTag(String text, OutputElementDescriptor descriptor) {
                super(text, descriptor);
            }
        }

        class EndTag extends Tag {
            public EndTag(String text, OutputElementDescriptor descriptor) {
                super(text, descriptor);
            }
        }

        class Word extends LineItem {

            public Word(String text) {
                super(text);
            }
        }

        class Space extends LineItem {
            public Space() {
                super(" ");
            }
        }
    }

    private static class StartElementInfo {
        private final String localName;
        private final Attributes attrs;

        public StartElementInfo(String localName, Attributes attrs) {
            this.localName = localName;
            this.attrs = attrs;
        }

        public String getLocalName() {
            return localName;
        }

        public Attributes getAttrs() {
            return attrs;
        }
    }
}
