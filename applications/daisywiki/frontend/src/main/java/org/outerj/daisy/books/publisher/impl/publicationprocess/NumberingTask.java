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

import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.cocoon.i18n.Bundle;

import javax.xml.parsers.SAXParser;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.InputStream;
import java.io.OutputStream;

public class NumberingTask implements PublicationProcessTask {
    private final String input;
    private final String output;

    public NumberingTask(String input, String output) {
        this.input = input;
        this.output = output;
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running numbering task.");
        BookInstance bookInstance = context.getBookInstance();
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String startXmlLocation = publicationOutputPath + input;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bookInstance.getResource(startXmlLocation);
            os = bookInstance.getResourceOutputStream(publicationOutputPath + output);
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            XmlSerializer serializer = new XmlSerializer(os);
            NumberingHandler numberingHandler = new NumberingHandler(serializer, context);
            parser.getXMLReader().setContentHandler(numberingHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) {}
            if (os != null)
                try { os.close(); } catch (Exception e) {}
        }
    }

    static class NumberingHandler extends ForwardingContentHandler {
        private final Stack<SectionInfo> headers;
        private PublicationContext context;
        private TypedCounter figureCounter = new TypedCounter();
        private TypedCounter tableCounter = new TypedCounter();

        private static final Pattern headerPattern = Pattern.compile("h([0-9]+)");

        public NumberingHandler(ContentHandler consumer, PublicationContext context) {
            super(consumer);
            headers = new Stack<SectionInfo>();
            this.context = context;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            Matcher matcher = headerPattern.matcher(localName);
            if (namespaceURI.equals("")) {
                if (matcher.matches()) {
                    int level = Integer.parseInt(matcher.group(1));
                    int nullLevel = level - 1; // = level number when counting from 0 instead of 1, useful when working with Java collections

                    if (level == 1) {
                        figureCounter.reset();
                        tableCounter.reset();
                    }

                    SectionInfo newSectionInfo = null;
                    String sectionType = null;
                    if (nullLevel == headers.size()) {
                        sectionType = getSectionType(atts);
                        boolean anonymousSection = !shouldIncreaseNumber(sectionType); 
                        int number = getStartNumber(headers.size() + 1, sectionType);
                        if (anonymousSection)
                            number -= 1;
                        newSectionInfo = new SectionInfo(number, sectionType, anonymousSection);
                    } else if (nullLevel < headers.size()) {
                        SectionInfo prevSection = null;
                        while (level <= headers.size())
                            prevSection = headers.pop();

                        sectionType = getSectionType(atts);
                        if (shouldIncreaseNumber(sectionType)) {
                            if (!prevSection.sectionType.equals(sectionType) && shouldResetNumber(sectionType)) {
                                newSectionInfo = new SectionInfo(1, sectionType, false);
                            } else {
                                newSectionInfo = new SectionInfo(prevSection.number + 1, sectionType, false);
                            }
                        } else {
                            newSectionInfo = new SectionInfo(prevSection.number, sectionType, true);
                        }
                    } else {
                        // jumped over a heading level -- stop numbering sections
                    }

                    if (newSectionInfo != null) {
                        headers.push(newSectionInfo);
                        if (!newSectionInfo.anonymous) {
                            NumberingPattern numberingPattern = getSectionNumberPattern(headers.size(), sectionType);
                            if (numberingPattern != null) {
                                numberingPattern.apply(headers.toArray(new SectionInfo[headers.size()]), context.getI18nBundle());
                                AttributesImpl newAttrs = new AttributesImpl(atts);
                                addNumberAttributes(newAttrs, newSectionInfo);
                                atts = newAttrs;
                            }
                        }
                    }
                } else if (localName.equals("img")) {
                    atts = processArtifact(atts, "daisy-image-type", figureCounter, "figure");
                } else if (localName.equals("table")) {
                    atts = processArtifact(atts, "daisy-table-type", tableCounter, "table");
                }
            }
            super.startElement(namespaceURI, localName, qName, atts);
        }

        private String getSectionType(Attributes atts) {
            String sectionType = atts.getValue("daisySectionType");
            if (sectionType == null) {
                if (headers.isEmpty()) {
                    sectionType = "default";
                } else {
                    // inherit section type from parent
                    sectionType = headers.peek().sectionType;
                }
            }
            return sectionType;
        }

        private void addNumberAttributes(AttributesImpl attrs, SectionInfo sectionInfo) {
            attrs.addCDATAAttribute("daisyNumber", sectionInfo.completeFormattedNumber);
            attrs.addCDATAAttribute("daisyPartialNumber", sectionInfo.formattedNumber);
            attrs.addCDATAAttribute("daisyRawNumber", String.valueOf(sectionInfo.number));
        }

        private NumberingPattern getSectionNumberPattern(int level, String sectionType) throws SAXException {
            String pattern = context.getProperties().get("numbering." + sectionType + ".h" + level);
            if (pattern == null || pattern.trim().equals(""))
                return null;
            try {
                return parseNumberingPattern(pattern);
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }

        private int getStartNumber(int level, String sectionType) throws SAXException {
            String propName = "numbering." + sectionType + ".h" + level + ".start-number";
            String startNumber = context.getProperties().get(propName);
            if (startNumber == null || startNumber.trim().equals("")) {
                return 1;
            } else {
                try {
                    return Integer.parseInt(startNumber);
                } catch (NumberFormatException e) {
                    throw new SAXException("Start number specified for property \"" + propName + "\" is not an integer number: " + startNumber);
                }
            }
        }

        private boolean shouldIncreaseNumber(String sectionType) {
            String propName = "numbering." + sectionType + ".increase-number";
            String increaseNumber = context.getProperties().get(propName);
            if (increaseNumber == null || increaseNumber.trim().equals(""))
                return true;
            else
                return increaseNumber.equalsIgnoreCase("true");
        }

        private boolean shouldResetNumber(String sectionType) {
            String propName = "numbering." + sectionType + ".reset-number";
            String resetNumber = context.getProperties().get(propName);
            if (resetNumber == null || resetNumber.trim().equals(""))
                return false;
            else
                return resetNumber.equalsIgnoreCase("true");
        }

        private Attributes processArtifact(Attributes attrs, String typeAttrName, TypedCounter counter, String artifactName) throws SAXException {
            String caption = attrs.getValue("daisy-caption");
            if (caption != null) {
                AttributesImpl newAttrs = new AttributesImpl(attrs);

                String type = attrs.getValue(typeAttrName);
                if (type == null || type.trim().equals("")) {
                    type = "default";
                    // update attributes with this type -- avoids that e.g. stylesheets also need this same logic
                    if (newAttrs.getIndex(typeAttrName) != -1)
                        newAttrs.setValue(newAttrs.getIndex(typeAttrName), type);
                    else
                        newAttrs.addCDATAAttribute(typeAttrName, type);
                }

                int number = counter.getNextNumber(type);
                NumberingPattern pattern = getArtifactNumberPattern(artifactName, type);
                if (pattern != null) {
                    // use a dummy section to represent the table or figure
                    SectionInfo dummySection = new SectionInfo(number, type, false);
                    SectionInfo[] sectionInfos = new SectionInfo[headers.size() + 1];
                    for (int i = 0; i < headers.size(); i++)
                        sectionInfos[i] = headers.get(i);
                    sectionInfos[sectionInfos.length - 1] = dummySection;
                    pattern.apply(sectionInfos, context.getI18nBundle());
                    addNumberAttributes(newAttrs, dummySection);
                }
                return newAttrs;
            }
            return attrs;
        }

        private NumberingPattern getArtifactNumberPattern(String artifactName, String type) throws SAXException {
            String pattern = context.getProperties().get(artifactName + "." + type + ".numberpattern");
            if (pattern == null || pattern.trim().equals(""))
                return null;
            try {
                return parseNumberingPattern(pattern);
            } catch (Exception e) {
                throw new SAXException(e);
            }
        }
    }

    static class TypedCounter {
        private Map<String, Integer> counters = new HashMap<String, Integer>();

        public void reset() {
            counters.clear();
        }

        public int getNextNumber(String type) {
            Integer value = counters.get(type);
            value = value == null ? new Integer(1) : new Integer(value.intValue() + 1);
            counters.put(type, value);
            return value.intValue();
        }
    }

    /**
     * Parses a numbering pattern.
     *
     * A numbering pattern is simply a string in which certain characters have a special
     * meaning. Characters that do not have a special meaning are output as is.
     *
     * A numbering pattern must contain exactly one of the following: 1, I, i, A, a.
     * This character will be replaced by the actual section number, in the style as
     * indicated by the character.
     *
     * A numbering pattern may in addition include 'hx' strings to reference the section
     * number of the parent heading of level x.
     *
     * For example 'h1.h2.1' would be a nice format for a level 3 heading.
     *
     * Additionally, numbering patterns can contain i18n keys to be looked up
     * in a resource bundle. The syntax for this is to put the i18n key between
     * dollar signs, i.e. $chapter$.
     */
    static NumberingPattern parseNumberingPattern(String pattern) throws Exception {
        SectionNumber sectionNumber = null;
        List<NumberPatternPart> parts = new ArrayList<NumberPatternPart>();

        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            switch (c) {
                case '1':
                case 'I':
                case 'i':
                case 'A':
                case 'a':
                    if (sectionNumber != null) {
                        throw new Exception("Numbering pattern contains double section number reference: \"" + pattern + "\".");
                    }
                    sectionNumber = new SectionNumber(c);
                    parts.add(sectionNumber);
                    break;
                case 'h':
                    i = i + 1;
                    if (i >= pattern.length() || !( (pattern.charAt(i) >= '0' && pattern.charAt(i) <= '9') || pattern.charAt(i) == 'r')) {
                        throw new Exception("Error in numbering pattern: character h should be followed by an integer number or 'r': \"" + pattern + "\".");
                    }
                    int level;
                    if (pattern.charAt(i) == 'r')
                        level = -1;
                    else
                        level = pattern.charAt(i) - '0';
                    ParentSectionNumber parentSectionNumber = new ParentSectionNumber(level);
                    parts.add(parentSectionNumber);
                    break;
                case '$':
                    // i18n key - search for next $
                    int endPos = pattern.indexOf('$', i + 1);
                    if (endPos == -1)
                        throw new Exception("Error in numbering pattern: unclosed i18n key reference: \"" + pattern + "\".");
                    String i18nKey = pattern.substring(i + 1, endPos);
                    parts.add(new I18nPart(i18nKey));
                    i = endPos;
                    break;
                default:
                    FreeChar freeChar = new FreeChar(c);
                    parts.add(freeChar);
                    break;
            }
        }

        if (sectionNumber == null) {
            throw new Exception("Numbering pattern is missing section number reference.");
        }

        return new NumberingPattern(parts.toArray(new NumberPatternPart[parts.size()]));
    }


    static class NumberingPattern {
        private final NumberPatternPart[] parts;

        public NumberingPattern(NumberPatternPart[] parts) {
            this.parts = parts;
        }

        /**
         * Applies the numbering pattern to the last SectionInfo in the supplied array,
         * assuming that a NumberingPattern has already been applied to the earlier sections
         * in the array.
         */
        public void apply(SectionInfo[] sectionInfos, Bundle bundle) {
            StringBuilder result = new StringBuilder();
            for (NumberPatternPart part : parts) {
                part.output(result, sectionInfos, bundle);
            }
            sectionInfos[sectionInfos.length - 1].completeFormattedNumber = result.toString();
        }
    }

    static interface NumberPatternPart {
        void output(StringBuilder result, SectionInfo[] sectionInfos, Bundle bundle);
    }

    static class FreeChar implements NumberPatternPart {
        private final char c;

        public FreeChar(char c) {
            this.c = c;
        }

        public void output(StringBuilder result, SectionInfo[] sectionInfos, Bundle bundle) {
            result.append(c);
        }
    }

    static class I18nPart implements NumberPatternPart {
        private final String i18nKey;

        public I18nPart(String i18nKey) {
            this.i18nKey = i18nKey;
        }

        public void output(StringBuilder result, SectionInfo[] sectionInfos, Bundle bundle) {
            result.append(bundle.getString(i18nKey));
        }
    }

    static class ParentSectionNumber implements NumberPatternPart {
        private final int level;

        public ParentSectionNumber(int level) {
            this.level = level;
        }

        public void output(StringBuilder result, SectionInfo[] sectionInfos, Bundle bundle) {
            if (level != -1) {
                int index = level - 1;
                if (index >= 0 && index < sectionInfos.length) {
                    result.append(sectionInfos[index].formattedNumber);
                } else {
                    result.append('h').append(level);
                }
            } else {
                String number = null;
                for (SectionInfo sectionInfo : sectionInfos) {
                    if (!sectionInfo.anonymous) {
                        number = sectionInfo.formattedNumber;
                        break;
                    }
                }
                if (number != null)
                    result.append(number);
                else
                    result.append("hr");
            }
        }
    }

    static class SectionNumber implements NumberPatternPart {
        private final char type;

        public SectionNumber(char type) {
            this.type = type;
        }

        public void output(StringBuilder result, SectionInfo[] sectionInfos, Bundle bundle) {
            int number = sectionInfos[sectionInfos.length - 1].number;
            String value;
            switch (type) {
                case '1':
                    value = String.valueOf(number);
                    break;
                case 'I':
                    value = NumeratorFormatter.long2roman(number, true);
                    break;
                case 'i':
                    value = NumeratorFormatter.long2roman(number, true).toLowerCase();
                    break;
                case 'A':
                    value = NumeratorFormatter.int2alphaCount(number);
                    break;
                case 'a':
                    value = NumeratorFormatter.int2alphaCount(number).toLowerCase();
                    break;
                default:
                    throw new RuntimeException("Unsupported numbering type: " + type);
            }
            sectionInfos[sectionInfos.length - 1].formattedNumber = value;
            result.append(value);
        }
    }

    static class SectionInfo {
        private final int number;
        private final String sectionType;
        private String formattedNumber = "0";
        private String completeFormattedNumber;
        private boolean anonymous;

        public SectionInfo(int number, String sectionType, boolean anonymous) {
            this.number = number;
            this.sectionType = sectionType;
            this.anonymous = anonymous;
        }
    }
}
