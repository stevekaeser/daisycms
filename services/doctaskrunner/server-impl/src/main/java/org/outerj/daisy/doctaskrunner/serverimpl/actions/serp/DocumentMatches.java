/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.doctaskrunner.serverimpl.actions.serp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;

import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.Part;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.Version;
import org.outerj.daisy.repository.acl.AclDetailPermission;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclResultInfo;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.xmlutil.MergeCharacterEventsHandler;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.xmlutil.SaxBuffer.SaxBit;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

public class DocumentMatches {
    private Pattern pattern;
    private Document document;
    private AclResultInfo aclResultInfo;
    
    // for each part, store the saxbits. 
    private Map<Part, List<SaxBit>> saxbits = new HashMap<Part, List<SaxBit>>();
    
    private List<TextFragment> partContentFragments = new ArrayList<TextFragment>();
    private Map<Part, Integer> partMatchCount = new HashMap<Part, Integer>();
    private TextFragment documentNameFragment = null;
    private boolean replaced = false;
    private boolean skippedDocumentName = false;
    private boolean skippedParts = false;
    
    public DocumentMatches(Repository repository, Document document, Pattern pattern) {
        this.pattern = pattern;
        this.document = document;
        
        try {
            aclResultInfo = repository.getAccessManager().getAclInfo(document);
        } catch (RepositoryException re) {
            throw new RuntimeException("Could not obtain acl info");
        }

        // If we don't have read non-live access, skip searching 
        if (aclResultInfo.getAccessDetails(AclPermission.READ).liveOnly() && document.getLastVersionId() != document.getLiveVersionId()) {
            return;
        }

        Version lastVersion;
        try {
            lastVersion = document.getLastVersion();
        } catch (RepositoryException e) {
            throw new RuntimeException("Could not get the last version of " + document.getVariantKey(), e);
        }
        if (pattern.matcher(lastVersion.getDocumentName()).find()) {
            documentNameFragment = new DocumentNameTextFragment(document);
        }
        
        RepositorySchema schema = repository.getRepositorySchema();
        
        // parse the html contained in daisy-html parts.  textFragments are added to the list
        SAXParser parser;
        XMLReader reader;
        try {
            parser = SAXParserFactory.newInstance().newSAXParser();
            reader = parser.getXMLReader();
        
            for (Part part: lastVersion.getParts().getArray()) {
                if  (schema.getPartTypeById(part.getTypeId(), false).isDaisyHtml()) {
                    Map<String, String> textFragmentInfo = new HashMap<String, String>();
                    textFragmentInfo.put("partTypeId", Long.toString(part.getTypeId()));
                    textFragmentInfo.put("partTypeName", part.getTypeName());
    
                    HtmlRegexpFragmentBuilder fragBuilder = new HtmlRegexpFragmentBuilder(pattern, textFragmentInfo);
                    reader.setContentHandler(new MergeCharacterEventsHandler(fragBuilder));
                    
                    reader.parse(new InputSource(part.getDataStream()));
    
                    this.saxbits.put(part, fragBuilder.getSaxBits());
                    
                    List<TextFragment> currentPartFragments = fragBuilder.getTextFragments();
                    partMatchCount.put(part, currentPartFragments.size());
                    partContentFragments.addAll(currentPartFragments);
                }
            }
        
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception while creating parser", e);
        }
    }

    public int replaceMatches(String replacement, boolean useSensibleCase) throws Exception {
        if (replaced) {
            throw new IllegalStateException("replaceMatches should be called only once");
        }
        
        int replacements = 0;

        if (documentNameFragment != null) {
            if (aclResultInfo.getAccessDetails(AclPermission.WRITE).isGranted(AclDetailPermission.DOCUMENT_NAME)) {
                replacements += documentNameFragment.replace(pattern, replacement, useSensibleCase); 
            } else {
                skippedDocumentName = true;
            }
        }
        for (TextFragment frag: partContentFragments) {
            replacements += frag.replace(pattern, replacement, useSensibleCase);
        }
        
        for (Part part: saxbits.keySet()) {
            if (!aclResultInfo.getAccessDetails(AclPermission.WRITE).canAccessPart(part.getTypeName())) {
                skippedParts = true;
                replacements -= partMatchCount.get(part);
                continue;
            }
            
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Properties properties = new Properties();
            properties.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
            XmlSerializer serializer = new XmlSerializer(os, properties);
            for (SaxBit bit: saxbits.get(part)) {
                bit.send(serializer);
            }
            
            document.setPart(part.getTypeId(), part.getMimeType(), os.toByteArray());
            
        }
        
        document.save(false);
        
        return replacements;
    }
    
    
    public List<TextFragment> getPartContentFragments() {
        return partContentFragments;
    }

    public TextFragment getDocumentNameFragment() {
        return documentNameFragment;
    }

    /**
     * Listens to SAX events and builds a list of matches.
     * To make sure that two consequent two runs result in identical matches a MergeCharactersEventHandler should be prepended.
     * 
     * TODO: perhaps a more flexible design for determining which text is a candidate for SERP could be useful?
     */
    static class HtmlRegexpFragmentBuilder extends SaxBuffer {

        // these tags delimit text fragments 
        private static Set<String> blockElements = new HashSet<String>(Arrays.asList(
                    "html", "head", "title", "body", "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "dl", "pre", "hr", "br", "blockquote", "address", "div", "fieldset", "table", "td", "th", "form", "p",
                    // object, embed and iframe are not really "block elements" but they visually break up words 
                    "object", "embed", "iframe" 
        ));
        
        // these elements are skipped
        private static Set<String> skipElements = new HashSet<String>(Arrays.asList(
                "script"
        ));
        
        // pre elements with these classes are skipped
        private static Set<String> skipPreClasses = new HashSet<String>(Arrays.asList(
                "query", "include", "query-and-include"
        ));
        
        
        private static Pattern whitespace = Pattern.compile("\\s+");
        private static Pattern leadingWhitespace = Pattern.compile("^\\s+", Pattern.MULTILINE);
            
        // Only xml between title (for head/title) or body tags are considered 
        boolean inBody = false;
        boolean inTitle = false;
        boolean inSkipElement = false;
        boolean inSkipPre = false;
        String skippedPreClass;
        
        // Text in these attributes is also search/and-replaced
        private static Set<String> textAttributes = new HashSet<String>(Arrays.asList("title", "alt", "daisy-caption"));
 
        /**
         * Stores list of {@link SaxBit} objects.
         */
        protected List<SaxBit> saxbits = new ArrayList<SaxBit>();

        private List<TextFragment> textFragments = new ArrayList<TextFragment>();
        
        private StringBuffer currentTextBuffer = new StringBuffer();
        private List<SaxBit> currentCharactersBits = new ArrayList<SaxBit>();
        
        private Pattern pattern;
        private Map<String, String> partInfo;
        
        public HtmlRegexpFragmentBuilder(Pattern pattern, Map<String, String> partInfo) {
            this.pattern = pattern;
            this.partInfo = partInfo;
        }

        public void skippedEntity(String name) throws SAXException {
            saxbits.add(new SkippedEntity(name));
        }

        public void setDocumentLocator(Locator locator) {
            // don't record this event
        }

        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
            saxbits.add(new IgnorableWhitespace(ch, start, length));
        }

        public void processingInstruction(String target, String data) throws SAXException {
            saxbits.add(new PI(target, data));
        }

        public void startDocument() throws SAXException {
            saxbits.add(StartDocument.SINGLETON);
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            String qNameLower = qName.toLowerCase();
            AttributesImpl newAtts = new AttributesImpl(atts);

            if (qNameLower.equals("body")) {
                inBody = true;
            } else if (qNameLower.equals("title")) {
                inTitle = true;
            } else if (skipElements.contains(qNameLower)) {
                inSkipElement = true;
            } else if (checkElementAndClassAttribute(qNameLower, "pre", newAtts.getValue("class"), skipPreClasses)) {
                inSkipPre = true;
                skippedPreClass = newAtts.getValue("class");
            }
            
            SharedAttributesStartElement startElement = new SharedAttributesStartElement(namespaceURI, localName, qName, newAtts);
            if (inBody) {
                for (String attribute: textAttributes) {
                    String attributeValue = newAtts.getValue(attribute);
                    if (attributeValue != null && pattern.matcher(attributeValue).find()) {
                        addAttributeTextFragment(qName, newAtts, attribute);
                    }
                }
            }
            saxbits.add(startElement);
        }

        /**
         * Returns true if an element has the expected name and the specified the specified class attribute contains at least one of the expected classes
         * @param name
         * @param expectedName
         * @param classAttr
         * @param classes
         * @return
         */
        private boolean checkElementAndClassAttribute(String name,
                String expectedName, String classAttr, Set<String> expectedClasses) {
            if ( !name.equals(expectedName) )
                return false;
            
            String paddedClassAttr = padString(classAttr);
            for (String clazz: expectedClasses) {
                if (paddedClassAttr.contains(padString(clazz))) {
                    return true;
                }
            }
            return false;
        }

        private String padString(String value) {
            return new StringBuffer(" ").append(value).append(" ").toString();
        }

        private void addAttributeTextFragment(String elementName, AttributesImpl newAtts, String attributeName) {
            textFragments.add(new AttributeTextFragment(elementName, newAtts, attributeName, partInfo));
        }

        /**
         * listing block elements is easier than listing non-block elements, hence the inverse logic.
         * @return
         */
        private boolean isElementInline(String namespaceURI, String localName,
                String name) {
            return (inBody || inTitle) && ! blockElements.contains(name.toLowerCase()); 
        }

        public void endPrefixMapping(String prefix) throws SAXException {
            saxbits.add(new EndPrefixMapping(prefix));
        }

        public void characters(char ch[], int start, int length) throws SAXException {
            boolean append = !inSkipElement && !inSkipPre && (inTitle || inBody);
            Characters bit;
            if (append) {
                String clean = whitespace.matcher(new String(ch, start, length)).replaceAll(" ");
                // if current text ends with space, remove leading space from cleaned text 
                if (currentTextBuffer.length() > 0 && currentTextBuffer.charAt(currentTextBuffer.length() - 1) == ' ') {
                    clean = leadingWhitespace.matcher(clean).replaceAll("");
                }
                currentTextBuffer.append(clean);
                bit = new Characters(clean.toCharArray(), 0, clean.length());
                currentCharactersBits.add(bit);
            } else {
                bit = new Characters(ch, start, length);
            }
            saxbits.add(bit);
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            String qNameLower = qName.toLowerCase();
            EndElement endElement = new EndElement(namespaceURI, localName, qName);
            if (!isElementInline(namespaceURI, localName, qName)) {
                addInlineTextFragment();
            }
            if (qNameLower.equals("body")) {
                inBody = false;
            } else if  (qNameLower.equals("title")) {
                inTitle = false;
            } else if  (inSkipElement && skipElements.contains(qNameLower)) {
                inSkipElement = false;
            } else if (inSkipPre && checkElementAndClassAttribute(qNameLower, "pre", skippedPreClass, skipPreClasses)) {
                inSkipPre = false;
            }

            saxbits.add(endElement);
        }

        public void endDocument() throws SAXException {
            saxbits.add(EndDocument.SINGLETON);
        }

        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            saxbits.add(new StartPrefixMapping(prefix, uri));
        }

        public void endCDATA() throws SAXException {
            saxbits.add(EndCDATA.SINGLETON);
        }

        public void comment(char ch[], int start, int length) throws SAXException {
            saxbits.add(new Comment(ch, start, length));
        }

        public void startEntity(String name) throws SAXException {
            saxbits.add(new StartEntity(name));
        }

        public void endDTD() throws SAXException {
            saxbits.add(EndDTD.SINGLETON);
        }

        public void startDTD(String name, String publicId, String systemId) throws SAXException {
            saxbits.add(new StartDTD(name, publicId, systemId));
        }

        public void startCDATA() throws SAXException {
            saxbits.add(StartCDATA.SINGLETON);
        }

        public void endEntity(String name) throws SAXException {
            saxbits.add(new EndEntity(name));
        }
        
        /**
         * If the current textbuffer matches the given pattern, an InlineTextFragment is added to the list
         */
        public void addInlineTextFragment() {
            if (currentTextBuffer.toString().trim().length() > 0) {
                String currentText = currentTextBuffer.toString();
                Matcher matcher = pattern.matcher(currentText);
                if (matcher.find()) {
                    textFragments.add(new InlineTextFragment(currentTextBuffer.toString(), currentCharactersBits, saxbits, partInfo));
                }
                currentTextBuffer.setLength(0);
                currentCharactersBits.clear();
            }
        }

        public void toSAX(ContentHandler contentHandler) throws SAXException {
            for (SaxBit saxbit : saxbits) {
                saxbit.send(contentHandler);
            }
        }

        /*
         * NOTE: Used in i18n XML bundle implementation
         */
        public String toString() {
            StringBuilder value = new StringBuilder();
            for (SaxBit saxbit : saxbits) {
                if (saxbit instanceof Characters) {
                    ((Characters)saxbit).toString(value);
                }
            }

            return value.toString();
        }

        public void recycle() {
            saxbits.clear();
        }

        public void dump(Writer writer) throws IOException {
            for (SaxBit saxbit : saxbits) {
                saxbit.dump(writer);
            }
            writer.flush();
        }
        
        public List<TextFragment> getTextFragments() {
            return textFragments;
        }
        
        public List<SaxBit> getSaxBits() {
            return saxbits;
        }

    }

    /**
     * This class has is used because SaxBit.StartElement makes a copy of attrs (and as such,
     * later changes to attrs are not reflected in SaxBit.StartElement)
     */
    public static final class SharedAttributesStartElement implements SaxBit, Serializable {
        public final String namespaceURI;
        public final String localName;
        public final String qName;
        public final AttributesImpl attrs;

        public SharedAttributesStartElement(String namespaceURI, String localName, String qName, AttributesImpl attrs) {
            this.namespaceURI = namespaceURI;
            this.localName = localName;
            this.qName = qName;
            this.attrs = attrs;
        }

        public void send(ContentHandler contentHandler) throws SAXException {
            contentHandler.startElement(namespaceURI, localName, qName, attrs);
        }

        public void dump(Writer writer) throws IOException {
            writer.write("[StartElement] namespaceURI=" + namespaceURI + ",localName=" + localName + ",qName=" + qName + "\n");
            for (int i = 0; i < attrs.getLength(); i++) {
                writer.write("      [Attribute] namespaceURI=" + attrs.getURI(i) + ",localName=" + attrs.getLocalName(i) + ",qName=" + attrs.getQName(i) + ",type=" + attrs.getType(i) + ",value=" + attrs.getValue(i) + "\n");
            }
        }
    }

    public boolean isSkippedDocumentName() {
        return skippedDocumentName;
    }

    public boolean isSkippedParts() {
        return skippedParts;
    }
    
}