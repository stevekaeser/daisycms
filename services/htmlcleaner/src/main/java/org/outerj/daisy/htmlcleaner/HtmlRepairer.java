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
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;

import java.util.*;

/**
 * Works on HTML input to clean it out to a
 * limited subset of HTML, mostly focussing on structural/semantic
 * elements (actually, what should be kept is configurable).
 *
 * <p>The input events should be in no namespace and contain
 * the html and body tags. All elements and attributes should already
 * be lowercased.</p>
 *
 * <p>All elements and attributes that are not explicitely allowed
 * in the configuration will be dropped (but
 * their character content will remain).</p>
 *
 * <p>Span and div elements are treated specially. The will only be
 * kept if their class attribute has one of the allowed values, specified
 * in the configuration of this component. Span elements that contain
 * a style attribute specifying bold and/or italic styling will
 * be converted to the equivalent strong/em tags.</p>
 *
 */
class HtmlRepairer {
    private HtmlCleanerTemplate template;
    /**
     * Hardcoded set of elements that can be removed if they contain
     * no character data or only other wipeable elements. Usually used
     * for inline elements.
     */
    private static Set<String> wipeableEmptyElements;
    static {
        wipeableEmptyElements = new HashSet<String>();
        wipeableEmptyElements.add("strong");
        wipeableEmptyElements.add("em");
        wipeableEmptyElements.add("sub");
        wipeableEmptyElements.add("sup");
        wipeableEmptyElements.add("a");
        wipeableEmptyElements.add("tt");
        wipeableEmptyElements.add("ul");
        wipeableEmptyElements.add("ol");
        wipeableEmptyElements.add("del");
        wipeableEmptyElements.add("span");
    }
    private static final char[] NEWLINE = new char[] { '\n' };
    private static Set<String> contentBlockElements;
    static {
        contentBlockElements = new HashSet<String>();
        contentBlockElements.add("p");
        contentBlockElements.add("h1");
        contentBlockElements.add("h2");
        contentBlockElements.add("h3");
        contentBlockElements.add("h4");
        contentBlockElements.add("h5");
        contentBlockElements.add("blockquote");
        contentBlockElements.add("li");
    }
    private static Set<String> needsCleanupOfEndBrs;
    static {
        needsCleanupOfEndBrs = new HashSet<String>();
        needsCleanupOfEndBrs.add("th");
        needsCleanupOfEndBrs.add("td");
        needsCleanupOfEndBrs.add("li");
    }

    public HtmlRepairer(HtmlCleanerTemplate template) {
        this.template = template;
    }

    /**
     * Cleans the HTML stored in the SaxBuffer.
     *
     * @param buffer should only contain following types of events: start/endElement, start/endDocument, characters
     * @param contentHandler where the outcome will be send to
     */
    public void clean(SaxBuffer buffer, ContentHandler contentHandler) throws SAXException {
        Cleaner cleaner = new Cleaner(buffer, contentHandler);
        cleaner.clean();
    }

    private class Cleaner {
        private ContentHandler finalContentHandler;
        private SaxBuffer input;

        public Cleaner(SaxBuffer input, ContentHandler contentHandler) {
            this.input = input;
            this.finalContentHandler = contentHandler;
        }

        private void clean() throws SAXException {
            // cleaning happens in multiple stages to make the logic simpler.
            // The different stages are implemented in pull-style, by reading
            // events from a SaxBuffer instance.

            CleaningPipe pipe = new CleaningPipe();
            pipe.addCleaningStep(new ElementCleanup());
            pipe.addCleaningStep(new IntroducedParas());
            pipe.addCleaningStep(new StructuralCleanup());
            // do structural cleanup a second time, since it might have introduced new elements which need to be checked again
            pipe.addCleaningStep(new StructuralCleanup());
            pipe.addCleaningStep(new CleanupBrsAndEmptyContentBlocks());
            pipe.addCleaningStep(new CleanupWipeableEmptyElements());
            // do content block cleanup a second time, since cleanup of empty inline elements might have left empty content blocks
            pipe.addCleaningStep(new CleanupBrsAndEmptyContentBlocks());
            pipe.addCleaningStep(new TranslateBeeaarsInPees());
            pipe.addCleaningStep(new CleanupNewlineAtEndOfPre());

            pipe.execute(input.getBits(), finalContentHandler);
        }
    }

    /**
     * <ul>
     *  <li>Makes sure all content is contained inside html/body
     *  <li>Drops unallowed elements
     *  <li>Does element translations (ie b into strong)
     *  <li>Only outputs non-namespaced elements
     * </ul>
     */
    private class ElementCleanup implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            Stack<XMLizable> endElements = new Stack<XMLizable>();
            boolean preSupported = template.descriptors.containsKey("pre");

            int i = 0;
            while (i < bits.size()) {
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;
                    if (!startElement.namespaceURI.equals("")) {
                        // namespaced elements are dropped
                        endElements.add(new EndElementInfo());
                    } else {
                        if (startElement.localName.equals("span")) {
                            // two possibilities:
                            //  * has only class attribute with recognized value
                            //  * has style with certain recognized effects (bold/italic) -> translate to semantic correct tag.
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null) {
                                if (template.allowedSpanClasses.contains(classAttr)) {
                                    // make new attributes element to make sure there are no other attributes on the element
                                    AttributesImpl attrs = getAllowedAttributes(startElement);
                                    output.startElement("span", attrs);
                                    endElements.push(new EndElementInfo("span"));
                                } else {
                                    // span element is dropped
                                    endElements.push(new EndElementInfo());
                                }
                            } else {
                                String styleAttr = startElement.attrs.getValue("style");
                                if (styleAttr != null) {
                                    StringTokenizer styleAttrTokenizer = new StringTokenizer(styleAttr, ";");
                                    boolean hasBold = false;
                                    boolean hasItalic = false;
                                    while (styleAttrTokenizer.hasMoreTokens()) {
                                        String styleToken = styleAttrTokenizer.nextToken();
                                        int colonPos = styleToken.indexOf(':');
                                        if (colonPos != -1) {
                                            String name = styleToken.substring(0, colonPos).trim().toLowerCase();
                                            String value = styleToken.substring(colonPos + 1).trim().toLowerCase();
                                            if (name.equals("font-weight") && value.equals("bold")) {
                                                hasBold = true;
                                            } else if (name.equals("font-style") && value.equals("italic")) {
                                                hasItalic = true;
                                            }
                                        }
                                    }

                                    MultiEndElementInfo endElement = new MultiEndElementInfo();
                                    if (hasBold) {
                                        output.startElement("strong", new AttributesImpl());
                                        endElement.add(new EndElementInfo("strong"));
                                    }
                                    if (hasItalic) {
                                        output.startElement("em", new AttributesImpl());
                                        endElement.add(new EndElementInfo("em"));
                                    }
                                    endElements.push(endElement);
                                } else {
                                    endElements.push(new EndElementInfo());
                                }
                            }
                        } else if (startElement.localName.equals("div")) {
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null && template.allowedDivClasses.contains(classAttr)) {
                                AttributesImpl attrs = getAllowedAttributes(startElement);
                                output.startElement("div", attrs);
                                endElements.push(new EndElementInfo("div"));
                            } else if (classAttr != null && template.dropDivClasses.contains(classAttr)) {
                                /** Skip over the content of the div element. */
                                int openElementCounter = 0;
                                while (true) {
                                    i++;
                                    if (i >= bits.size())
                                        throw new SAXException("Reached end of input without encountering matching close div tag.");

                                    Object nextBit = bits.get(i);
                                    if (nextBit instanceof SaxBuffer.StartElement) {
                                        openElementCounter++;
                                    } else if (nextBit instanceof SaxBuffer.EndElement) {
                                        if (openElementCounter == 0) {
                                            break;
                                        }
                                        openElementCounter--;
                                    }
                                }
                            } else {
                                // unallowed class, drop div element
                                endElements.push(new EndElementInfo());
                            }
                        }else if (startElement.localName.equals("table")) {
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null && template.dropTableClasses.contains(classAttr)) {
                                AttributesImpl attrs = getAllowedAttributes(startElement);
                                int classPos = attrs.getIndex("class");
                                if (classPos != -1)
                                    attrs.removeAttribute(classPos);
                                output.startElement("table", attrs);
                                endElements.push(new EndElementInfo("table"));
                             }else{
                                 output.startElement("table", getAllowedAttributes(startElement));
                                 endElements.push(new EndElementInfo("table"));
                             }
                        } else if (startElement.localName.equals("p")) {
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null && template.allowedParaClasses.contains(classAttr)) {
                                output.startElement("p", getAllowedAttributes(startElement));
                                endElements.push(new EndElementInfo("p"));
                            } else {
                                AttributesImpl attrs = getAllowedAttributes(startElement);
                                int classPos = attrs.getIndex("class");
                                if (classPos != -1)
                                    attrs.removeAttribute(classPos);
                                output.startElement("p", attrs);
                                endElements.push(new EndElementInfo("p"));
                            }
                        } else if (startElement.localName.equals("pre") && preSupported) {
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null && template.allowedPreClasses.contains(classAttr)) {
                                output.startElement("pre", getAllowedAttributes(startElement));
                                endElements.push(new EndElementInfo("pre"));
                            } else {
                                AttributesImpl attrs = getAllowedAttributes(startElement);
                                int classPos = attrs.getIndex("class");
                                if (classPos != -1)
                                    attrs.removeAttribute(classPos);
                                output.startElement("pre", attrs);
                                endElements.push(new EndElementInfo("pre"));
                            }
                        } else if (startElement.localName.equals("ol")) {
                            String classAttr = startElement.attrs.getValue("class");
                            if (classAttr != null && template.allowedOlClasses.contains(classAttr)) {
                                output.startElement("ol", getAllowedAttributes(startElement));
                                endElements.push(new EndElementInfo("ol"));
                            } else {
                                AttributesImpl attrs = getAllowedAttributes(startElement);
                                int classPos = attrs.getIndex("class");
                                if (classPos != -1)
                                    attrs.removeAttribute(classPos);
                                output.startElement("ol", attrs);
                                endElements.push(new EndElementInfo("ol"));
                            }
                        } else if (startElement.localName.equals("b")) {
                            // translate to <strong>
                            output.startElement("strong", getAllowedAttributes(startElement));
                            endElements.push(new EndElementInfo("strong"));
                        } else if (startElement.localName.equals("i")) {
                            // translate to <em>
                            output.startElement("em", getAllowedAttributes(startElement));
                            endElements.push(new EndElementInfo("em"));
                        } else if (startElement.localName.equals("strike")) {
                            // translate to <del>
                            output.startElement("del", getAllowedAttributes(startElement));
                            endElements.push(new EndElementInfo("del"));
                        } else if (startElement.localName.equals("html")) {
                            if (output.openElements.size() != 0)
                                throw new SAXException("html element can only appear as root element.");

                            output.startElement(startElement.localName, getAllowedAttributes(startElement));
                            endElements.push(new EndElementInfo(startElement.localName));

                            // fast forward to body element
                            while (true) {
                                i++;
                                if (i >= bits.size())
                                    throw new SAXException("Reached end of input without encountering opening body tag.");

                                Object nextBit = bits.get(i);
                                if (nextBit instanceof SaxBuffer.StartElement && ((SaxBuffer.StartElement)nextBit).localName.equals("body")
                                        && ((SaxBuffer.StartElement)nextBit).namespaceURI.equals("")) {
                                    i--;
                                    break;
                                }
                            }

                        } else if (startElement.localName.equals("body")) {
                            if (output.openElements.size() != 1)
                                throw new SAXException("body element can only appear as child of html element");

                            if (!output.openElements.get(0).getName().equals("html"))
                                throw new SAXException("body element can only appear as child of html element");

                            output.startElement("body", new AttributesImpl());
                            endElements.push(new EndElementInfo("body"));
                        } else if (startElement.localName.equals("img") && template.descriptors.containsKey("img")) {
                            AttributesImpl attrs = getAllowedAttributes(startElement);
                            if (template.imgAlternateSrcAttr != null) {
                                String altSrc = startElement.attrs.getValue(template.imgAlternateSrcAttr);
                                if (altSrc != null && !altSrc.equals("")) {
                                    int hrefIndex = attrs.getIndex("src");
                                    if (hrefIndex != -1)
                                        attrs.setValue(hrefIndex, altSrc);
                                    else
                                        attrs.addAttribute("", "src", "src", "CDATA", altSrc);
                                }
                            }
                            output.startElement(startElement.localName, attrs);
                            endElements.push(new EndElementInfo(startElement.localName));
                        } else if (startElement.localName.equals("a") && template.descriptors.containsKey("a")) {
                            AttributesImpl attrs = getAllowedAttributes(startElement);
                            if (template.linkAlternateHrefAttr != null) {
                                String altHref = startElement.attrs.getValue(template.linkAlternateHrefAttr);
                                if (altHref != null && !altHref.equals("")) {
                                    int hrefIndex = attrs.getIndex("href");
                                    if (hrefIndex != -1)
                                        attrs.setValue(hrefIndex, altHref);
                                    else
                                        attrs.addAttribute("", "href", "href", "CDATA", altHref);
                                }
                            }
                            output.startElement(startElement.localName, attrs);
                            endElements.push(new EndElementInfo(startElement.localName));
                        } else if (startElement.localName.equals("td") || startElement.localName.equals("th")) {
                            AttributesImpl attrs = getAllowedAttributes(startElement);

                            // remove dummy rowspan and colspan attributes
                            String rowspan = attrs.getValue("rowspan");
                            if (rowspan != null && rowspan.equals("1")) {
                                attrs.removeAttribute(attrs.getIndex("rowspan"));
                            }
                            String colspan = attrs.getValue("colspan");
                            if (colspan != null && colspan.equals("1")) {
                                attrs.removeAttribute(attrs.getIndex("colspan"));
                            }

                            output.startElement(startElement.localName, attrs);
                            endElements.push(new EndElementInfo(startElement.localName));
                        } else if ((startElement.localName.equals("style") || startElement.localName.equals("script")) && !template.descriptors.containsKey(startElement.localName)) {
                            // skip over the content
                            int endPos = searchEndElement(bits, i);
                            if (endPos == -1)
                                throw new SAXException("Abnormal situation which should never occur: didn't find closing tag for " + startElement.localName);
                            i = endPos;
                        } else if (template.descriptors.containsKey(startElement.localName)) {
                            output.startElement(startElement.localName, getAllowedAttributes(startElement));
                            endElements.push(new EndElementInfo(startElement.localName));
                        } else {
                            // skip element
                            endElements.push(new EndElementInfo());
                        }
                    }
                } else if (bit instanceof SaxBuffer.EndElement) {
                    XMLizable endElement = endElements.pop();
                    endElement.toSAX(output);
                } else if (bit instanceof SaxBuffer.Characters) {
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
                i++;
            }
        }
    }

    private AttributesImpl getAllowedAttributes(SaxBuffer.StartElement startElement) {
        // limit attributes to the allowed attributes
        ElementDescriptor elementDescriptor = template.descriptors.get(startElement.localName);
        AttributesImpl attrs = new AttributesImpl();
        if (elementDescriptor == null) {
        	return attrs;
        }
		String[] allowedAttributes = elementDescriptor.getAttributeNames();
        for (String allowedAttribute : allowedAttributes) {
            String value = startElement.attrs.getValue(allowedAttribute);
            if (value != null) {
                attrs.addAttribute("", allowedAttribute, allowedAttribute, "CDATA", value);
            }
        }
        return attrs;
    }

    /**
     * Puts p tags around all characters or elements that are child of html/body
     * but are not allowed there.
     */
    private class IntroducedParas implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            Stack<XMLizable> endElements = new Stack<XMLizable>();
            Stack<Integer> introducedParas = new Stack<Integer>();
            ElementDescriptor bodyDescriptor = template.descriptors.get("body");
            ElementDescriptor tdDescriptor = template.descriptors.get("td");
            ElementDescriptor thDescriptor = template.descriptors.get("th");
            ElementDescriptor paraDescriptor = template.descriptors.get("p");
            ElementDescriptor blockQuoteDescriptor = template.descriptors.get("blockquote");

            int i = -1;
            while (i < bits.size()) {
                i++;
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;

                    if (!introducedParas.empty() && introducedParas.peek() == 0 && !paraDescriptor.childAllowed(startElement.localName)) {
                        output.endElement("p");
                        introducedParas.pop();
                    } else if (output.openElements.size() > 1) {
                        StartElementInfo parentInfo = output.openElements.get(output.openElements.size() - 1);
                        String parentName = parentInfo.getName();
                        boolean startPara =
                                (parentName.equals("body") && !bodyDescriptor.childAllowed(startElement.localName)
                                && paraDescriptor.childAllowed(startElement.localName))
                                ||
                                (parentName.equals("td") && !tdDescriptor.childAllowed(startElement.localName)
                                && paraDescriptor.childAllowed(startElement.localName))
                                ||
                                (parentName.equals("th") && !thDescriptor.childAllowed(startElement.localName)
                                && paraDescriptor.childAllowed(startElement.localName))
                                ||
                                (parentName.equals("blockquote") && !blockQuoteDescriptor.childAllowed(startElement.localName)
                                && paraDescriptor.childAllowed(startElement.localName));

                        if (startPara) {
                            output.startElement("p", new AttributesImpl());
                            introducedParas.push(0);
                        }
                    }

                    if (!introducedParas.empty()) {
                        introducedParas.push(introducedParas.pop() + 1);
                    }

                    output.startElement(startElement.localName, startElement.attrs);
                    endElements.push(new EndElementInfo(startElement.localName));


                } else if (bit instanceof SaxBuffer.EndElement) {
                    if (!introducedParas.empty() && introducedParas.peek() == 0) {
                        output.endElement("p");
                        introducedParas.pop();
                    }

                    XMLizable endElement = endElements.pop();
                    endElement.toSAX(output);

                    if (!introducedParas.empty()) {
                        introducedParas.push(introducedParas.pop() - 1);
                    }
                } else if (bit instanceof SaxBuffer.Characters) {
                    if (output.openElements.size() > 1) {
                        StartElementInfo parentInfo = output.openElements.get(output.openElements.size() - 1);
                        String parentName = parentInfo.getName();
                        boolean startPara = parentName.equals("body") || parentName.equals("td") || parentName.equals("th") || parentName.equals("blockquote");
                        if (startPara) {
                            output.startElement("p", new AttributesImpl());
                            introducedParas.push(0);
                        }
                    }
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
            }
        }
    }

    /**
     * Performs structural corrections, so that the end result is
     * limited to what XHTML1 allows (or at least close to it).
     */
    private class StructuralCleanup implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            Stack<XMLizable> endElements = new Stack<XMLizable>();
            Map additionalEnds = new IdentityHashMap();

            int i = -1;
            while (i < bits.size()) {
                i++;
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;

                    ElementDescriptor descriptor = template.descriptors.get(startElement.localName);
                    if (descriptor == null)
                        throw new SAXException("Missing ElementDescriptor for tagname " + startElement.localName);

                    // check if this element can occur inside its parent
                    if (output.openElements.size() > 0) {
                        String parentElementName = (output.openElements.get(output.openElements.size() - 1)).getName();
                        ElementDescriptor parentDescriptor = template.descriptors.get(parentElementName);

                        boolean allowed = parentDescriptor.childAllowed(startElement.localName);

                        // if it's allowed, let's get it done and over with
                        if (allowed) {
                            output.startElement(startElement.localName, startElement.attrs);

                            XMLizable endElementInfo = new EndElementInfo(startElement.localName);
                            EndElementInfo extraEndElementInfo = (EndElementInfo)additionalEnds.remove(startElement);
                            if (extraEndElementInfo != null) {
                                MultiEndElementInfo multiEndElementInfo = new MultiEndElementInfo();
                                multiEndElementInfo.add(extraEndElementInfo);
                                multiEndElementInfo.add((EndElementInfo)endElementInfo);
                                endElementInfo = multiEndElementInfo;
                            }
                            endElements.push(endElementInfo);
                            continue;
                        }

                        // not allowed -> search for first parent where it is allowed
                        int firstGoodAncestor = -1;
                        for (int k = output.openElements.size() - 2; k >= 0; k--) {
                            String ancestorElementName = (output.openElements.get(k)).getName();
                            ElementDescriptor ancestorDescriptor = template.descriptors.get(ancestorElementName);
                            if (ancestorDescriptor.childAllowed(startElement.localName)) {
                                firstGoodAncestor = k;
                                break;
                            }
                        }

                        if (firstGoodAncestor != -1) {
                            // upon end of the problem element, we'll need to re-open the tags,
                            // collect this info now while we still have it
                            MultiEndElementInfo endElementInfo = new MultiEndElementInfo();
                            for (int k = output.openElements.size() - 1; k > firstGoodAncestor; k--) {
                                endElementInfo.add(output.openElements.get(k));
                            }
                            endElementInfo.add(new EndElementInfo(startElement.localName));

                            // close open elements to get to the allowed ancestor
                            for (int k = output.openElements.size() - 1; k > firstGoodAncestor; k--) {
                                output.endElement(output.openElements.get(k).getName());
                            }

                            // start the problem element
                            output.startElement(startElement.localName, startElement.attrs);

                            endElements.push(endElementInfo);
                        } else {
                            if (startElement.localName.equals("li")) {
                                // automatically introduce an ul
                                output.startElement("ul", new AttributesImpl());
                                output.startElement(startElement.localName, startElement.attrs);

                                // wrap all sibling li's into one big ul
                                SaxBuffer.StartElement sibling = null;
                                int nextSiblingPos = searchSibling(bits, i);
                                while (nextSiblingPos != -1 && ((SaxBuffer.StartElement)bits.get(nextSiblingPos)).localName.equals("li")) {
                                    sibling = (SaxBuffer.StartElement)bits.get(nextSiblingPos);
                                    nextSiblingPos = searchSibling(bits, nextSiblingPos);
                                }

                                if (sibling != null) {
                                    endElements.push(new EndElementInfo(startElement.localName));
                                    // the ul should be closed after the last li
                                    additionalEnds.put(sibling, new EndElementInfo("ul"));
                                } else {
                                    MultiEndElementInfo endElementInfo = new MultiEndElementInfo();
                                    endElementInfo.add(new EndElementInfo("ul"));
                                    endElementInfo.add(new EndElementInfo(startElement.localName));
                                    endElements.push(endElementInfo);
                                }
                            } else if (startElement.localName.equals("br")) {
                                // throw away the br
                                int endPos = searchEndElement(bits, i);
                                if (endPos == -1)
                                    throw new SAXException("Abnormal situation which should never occur: didn't find end of br.");
                                i = endPos;
                            } else {
                                throw new SAXException("Element \"" + startElement.localName + "\" is disallowed at its current location, and could not automatically fix this.");
                            }
                        }

                    } else {
                        output.startElement(startElement.localName, startElement.attrs);
                        endElements.push(new EndElementInfo(startElement.localName));
                    }

                } else if (bit instanceof SaxBuffer.EndElement) {
                    XMLizable endElement = endElements.pop();
                    endElement.toSAX(output);
                } else if (bit instanceof SaxBuffer.Characters) {
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
            }
        }
    }

    /**
     * Removes p's, headers containing only whitespace or br's, changes sequences
     * of more then two br's into a new paragraph, drops br's at start or
     * end of p, headers.
     */
    private class CleanupBrsAndEmptyContentBlocks implements CleaningStep {


        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            Stack<XMLizable> endElements = new Stack<XMLizable>();

            int i = -1;
            while (i < bits.size()) {
                i++;
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;

                    boolean contentBlockElement = contentBlockElements.contains(startElement.localName);
                    if (contentBlockElement || startElement.localName.equals("td") || startElement.localName.equals("th")) {
                        // starting a new p, td, ...: search if this element contains anything non-whitespace non-br
                        int elementNesting = 0;
                        int z = i;
                        boolean reachedEndElement = false;
                        while (true) {
                            z++;
                            Object bit2 = bits.get(z);
                            if (bit2 instanceof SaxBuffer.Characters && isWhitespace((SaxBuffer.Characters)bit2)) {
                                // continue loop
                            } else if (bit2 instanceof SaxBuffer.StartElement
                                    && ((SaxBuffer.StartElement)bit2).localName.equals("br")) {
                                elementNesting++;
                            } else if (bit2 instanceof SaxBuffer.EndElement
                                    && ((SaxBuffer.EndElement)bit2).localName.equals("br")) {
                                elementNesting--;
                            } else if (bit2 instanceof SaxBuffer.EndElement && elementNesting == 0) {
                                reachedEndElement = true;
                                break;
                            } else {
                                break;
                            }
                        }

                        if (reachedEndElement) {
                            if (contentBlockElement) {
                                // skip over this element
                                i = z;
                                continue;
                            } else {
                                output.startElement(startElement.localName, startElement.attrs);
                                endElements.push(new EndElementInfo(startElement.localName));
                                // skip content of this element
                                i = z - 1;
                                continue;
                            }
                        } else {
                            if (contentBlockElement) {
                                // skip over initial br's or whitespace at start of content block
                                i = z - 1;
                            } else {
                                // nothing to do
                            }
                        }

                    } else if (startElement.localName.equals("br")) {
                        // search for a parent content block element
                        int firstContentBlockAncestor = -1;
                        for (int k = output.openElements.size() - 1; k >= 0; k--) {
                            StartElementInfo startElementInfo = output.openElements.get(k);
                            if (contentBlockElements.contains(startElementInfo.getName())) {
                                firstContentBlockAncestor = k;
                                break;
                            }
                        }

                        // if we are inside a content block ...
                        if (firstContentBlockAncestor != -1) {
                            // count number of br's following this
                            int z = i;
                            int brCount = 1;
                            boolean continueSearch = true;
                            while (continueSearch) {
                                z++;
                                Object bit2 = bits.get(z);
                                if (bit2 instanceof SaxBuffer.EndElement) {
                                    String name = ((SaxBuffer.EndElement)bit2).localName;
                                    if (!name.equals("br")) {
                                        continueSearch = false;
                                    }
                                } else if (bit2 instanceof SaxBuffer.StartElement
                                    && ((SaxBuffer.StartElement)bit2).localName.equals("br")) {
                                    brCount++;
                                    continueSearch = true;
                                } else if (bit2 instanceof SaxBuffer.Characters && isWhitespace((SaxBuffer.Characters)bit2)) {
                                    continueSearch = true;
                                } else {
                                    continueSearch = false;
                                }
                            }

                            // if all the next bits till the first closing content block tag are either end elements or whitespace,
                            // then drop the br's.
                            boolean beforeEndContentBlock = false;
                            for (int t = z; t < bits.size(); t++) {
                                if (bits.get(t) instanceof SaxBuffer.EndElement) {
                                    SaxBuffer.EndElement endEl = (SaxBuffer.EndElement)bits.get(t);
                                    if (contentBlockElements.contains(endEl.localName)) {
                                        beforeEndContentBlock = true;
                                        break;
                                    }
                                    // other end element events: continue searching
                                } else if (bits.get(t) instanceof SaxBuffer.Characters && isWhitespace((SaxBuffer.Characters)bits.get(t))) {
                                    // whitespace: continue searching
                                } else {
                                    // everything else: stop
                                    break;
                                }
                            }
                            if (beforeEndContentBlock) {
                                i = z - 1;
                                continue;
                            }

                            if (brCount >= 2) {
                                // drop the br's, close content block element, open content block element
                                i = z - 1; // z is positioned on the first non-br, non-whitespace element following the br's

                                List<StartElementInfo> elementsToRestart = new ArrayList<StartElementInfo>();
                                for (int k = output.openElements.size() - 1; k >= firstContentBlockAncestor; k--) {
                                    elementsToRestart.add(output.openElements.get(k));
                                }

                                for (int k = output.openElements.size() - 1; k >= firstContentBlockAncestor; k--) {
                                    output.endElement(output.openElements.get(k).getName());
                                }

                                for (int k = elementsToRestart.size() - 1; k >= 0; k--) {
                                    StartElementInfo startElementInfo = elementsToRestart.get(k);
                                    output.startElement(startElementInfo.getName(), startElementInfo.getAttrs());
                                }
                                continue;
                            }
                        } else  if (startElement.localName.equals("br") && output.openElements.size() > 1
                            && needsCleanupOfEndBrs.contains(output.openElements.get(output.openElements.size() - 1).getName())) {
                            // this is useful to remove <br>s inside <td>s or <br>s at the end of <li>s like mozilla does
                            String elementName = output.openElements.get(output.openElements.size() - 1).getName();

                            boolean nextIsEndOfElement = false;
                            int r = i + 1;
                            for (; r < bits.size(); r++) {
                                Object nextBit = bits.get(r);
                                if (nextBit instanceof SaxBuffer.EndElement) {
                                    SaxBuffer.EndElement endEl = (SaxBuffer.EndElement)nextBit;
                                    if (endEl.localName.equals("br")) {
                                        continue;
                                    } else if (endEl.localName.equals(elementName)) {
                                        nextIsEndOfElement = true;
                                        break;
                                    } else {
                                        break;
                                    }
                                } else if (nextBit instanceof SaxBuffer.Characters && isWhitespace((SaxBuffer.Characters)nextBit)) {
                                    // do nothing
                                } else {
                                    break;
                                }
                            }

                            if (nextIsEndOfElement) {
                                i = r - 1;
                                continue;
                            }
                        }
                    }

                    output.startElement(startElement.localName, startElement.attrs);
                    endElements.push(new EndElementInfo(startElement.localName));


                } else if (bit instanceof SaxBuffer.EndElement) {
                    XMLizable endElement = endElements.pop();
                    endElement.toSAX(output);
                } else if (bit instanceof SaxBuffer.Characters) {
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
            }
        }
    }

    private class CleanupWipeableEmptyElements implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            Stack<XMLizable> endElements = new Stack<XMLizable>();

            int i = -1;
            while (i < bits.size()) {
                i++;
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;
                    if (wipeableEmptyElements.contains(startElement.localName)) {
                        boolean hasWhitespace = false;
                        boolean reachedEndElement = false;
                        int elementNesting = 0;
                        int k = i;
                        while (true) {
                            k++;
                            Object nextBit = bits.get(k);
                            if (nextBit instanceof SaxBuffer.StartElement && wipeableEmptyElements.contains(((SaxBuffer.StartElement)nextBit).localName)) {
                                elementNesting++;
                            } else if (nextBit instanceof SaxBuffer.Characters && isWhitespace((SaxBuffer.Characters)nextBit)) {
                                hasWhitespace = true;
                            } else if (nextBit instanceof SaxBuffer.EndElement && elementNesting > 0) {
                                elementNesting--;
                            } else if (nextBit instanceof SaxBuffer.EndElement && elementNesting == 0) {
                                reachedEndElement = true;
                                break;
                            } else {
                                break;
                            }
                        }

                        if (reachedEndElement) {
                            // skip the elements
                            i = k;
                            // if the wipeable elements contained whitespace, generate a whitespace character
                            if (hasWhitespace)
                                output.characters(new char[] { ' ' }, 0, 1);
                            continue;
                        }
                    }

                    output.startElement(startElement.localName, startElement.attrs);
                    endElements.push(new EndElementInfo(startElement.localName));

                } else if (bit instanceof SaxBuffer.EndElement) {
                    XMLizable endElement = endElements.pop();
                    endElement.toSAX(output);
                } else if (bit instanceof SaxBuffer.Characters) {
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
            }
        }
    }

    /**
     * Changes br elements inside pre elements into newline character events.
     */
    private class TranslateBeeaarsInPees implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            int preLevel = 0;
            int i = -1;
            while (i < bits.size()) {
                i++;
                Object bit = bits.get(i);
                if (bit instanceof SaxBuffer.StartElement) {
                    SaxBuffer.StartElement startElement = (SaxBuffer.StartElement)bit;
                    if (startElement.localName.equals("pre")) {
                        preLevel++;
                    } else if (preLevel > 0 && startElement.localName.equals("br")) {
                        // normally an opening br should be immediatelly followed by the closing br,
                        // so let us restrict us to that case
                        Object nextBit = bits.get(i + 1);
                        if (nextBit instanceof SaxBuffer.EndElement && ((SaxBuffer.EndElement)nextBit).localName.equals("br")) {
                            // replace this br by a newline
                            output.characters(NEWLINE, 0, 1);
                            i++;
                            continue;
                        }
                    }

                    output.startElement(startElement.localName, startElement.attrs);

                } else if (bit instanceof SaxBuffer.EndElement) {
                    SaxBuffer.EndElement endElement = (SaxBuffer.EndElement)bit;
                    if (endElement.localName.equals("pre")) {
                        preLevel--;
                    }
                    output.endElement(endElement.localName);
                } else if (bit instanceof SaxBuffer.Characters) {
                    ((SaxBuffer.Characters)bit).send(output);
                } else if (bit instanceof SaxBuffer.StartDocument) {
                    output.startDocument();
                } else if (bit instanceof SaxBuffer.EndDocument) {
                    output.endDocument();
                    // don't do any events after endDocument
                    return;
                }
            }
        }
    }

    /**
     * Removes a "\n" if it occurs right before a closing pre tag. Such a newline has
     * no meaning. This is often inserted by Firefox, and causes layout troubles in
     * Internet Explorer (subsequent block elements are extra indented, though they
     * shift left again once you start typing in them).
     */
    private class CleanupNewlineAtEndOfPre implements CleaningStep {

        public void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException {
            int i = 0;
            while (i < bits.size()) {
                SaxBuffer.SaxBit bit = bits.get(i);
                if (bit instanceof SaxBuffer.Characters
                        && i < bits.size() - 1
                        && bits.get(i + 1) instanceof SaxBuffer.EndElement
                        && ((SaxBuffer.EndElement)bits.get(i + 1)).localName.equals("pre")) {
                    SaxBuffer.Characters characters = (SaxBuffer.Characters)bit;
                    char[] ch = characters.ch;
                    if (ch.length > 0 && ch[ch.length - 1] == '\n' && (ch.length <= 1 || ch[ch.length - 2] != '\n')) {
                        output.characters(characters.ch, 0, characters.ch.length - 1);
                    } else {
                        characters.send(output);
                    }
                } else {
                    bit.send(output);
                }
                i++;
            }
        }
    }

    private boolean isWhitespace(SaxBuffer.Characters characters) {
        for (char ch : characters.ch) {
            if (!(Character.isWhitespace(ch) || ch == (char)160)) // 160 is &nbsp;
                return false;
        }
        return true;
    }

    /**
     *
     * @param bits SaxBuffer bits
     * @param index the index of the current start element, of which we want to find the sibling
     */
    private int searchSibling(List bits, int index) {
        int nesting = 0;
        boolean passedEndElement = false;
        for (int i = index + 1; i < bits.size(); i++) {
            Object bit = bits.get(i);
            if (bit instanceof SaxBuffer.StartElement) {
                if (passedEndElement)
                    return i;
                else
                    nesting ++;
            } else if (bit instanceof SaxBuffer.EndElement) {
                if (nesting == 0)
                    passedEndElement = true;
                else
                    nesting--;
            }
        }
        return -1;
    }

    /**
     *
     * @param bits SaxBuffer bits
     * @param index the index of the current start element, of which we want to find the end element
     */
    private int searchEndElement(List bits, int index) {
        int nesting = 0;
        for (int i = index + 1; i < bits.size(); i++) {
            Object bit = bits.get(i);
            if (bit instanceof SaxBuffer.StartElement) {
                nesting ++;
            } else if (bit instanceof SaxBuffer.EndElement) {
                if (nesting == 0)
                    return i;
            }
        }
        return -1;
    }

    private static class CleaningPipe {
        private List<CleaningStep> steps = new ArrayList<CleaningStep>();

        void addCleaningStep(CleaningStep step) {
            steps.add(step);
        }

        /**
         *
         * @param result where the result of the last cleanup step should be sent to
         */
        void execute(List<SaxBuffer.SaxBit> startBits, ContentHandler result) throws SAXException {
            List<SaxBuffer.SaxBit> bits = startBits;
            SaxBuffer buffer;
            for (int i = 0; i < steps.size(); i++) {
                buffer = new SaxBuffer();
                OutputHandler handler = new OutputHandler(i == steps.size() -1 ? result : buffer);
                steps.get(i).perform(bits, handler);
                bits = buffer.getBits();
            }
        }
    }

    private interface CleaningStep {
        void perform(List<SaxBuffer.SaxBit> bits, OutputHandler output) throws SAXException;
    }

    private static class OutputHandler extends ForwardingContentHandler {
        private List<StartElementInfo> openElements = new ArrayList<StartElementInfo>();

        public OutputHandler(ContentHandler consumer) {
            super(consumer);
        }

        public void startElement(String name, Attributes attrs) throws SAXException {
            super.startElement("", name, name, attrs);
            openElements.add(new StartElementInfo(name, attrs));
        }

        public void endElement(String name) throws SAXException {
            super.endElement("", name, name);
            String removed = openElements.remove(openElements.size() - 1).getName();
            if (!removed.equals(name)) {
                throw new SAXException("The close tag \"" + name + "\" did not match the open tag \"" + removed + "\".");
            }
        }

    }

    private static class StartElementInfo implements XMLizable {
        private final String name;
        private final Attributes attrs;

        public StartElementInfo(String name, Attributes attrs) {
            this.name = name;
            this.attrs = attrs;
        }

        public String getName() {
            return name;
        }

        public Attributes getAttrs() {
            return attrs;
        }

        public void toSAX(OutputHandler contentHandler) throws SAXException {
            contentHandler.startElement(name, attrs);
        }
    }

    private static class EndElementInfo implements XMLizable {
        private final boolean skip;
        private final String localName;

        public EndElementInfo() {
            this.skip = true;
            this.localName = null;
        }

        public EndElementInfo(String localName) {
            this.skip = false;
            this.localName = localName;
        }

        public void toSAX(OutputHandler contentHandler) throws SAXException {
            if (!skip) {
                contentHandler.endElement(localName);
            }
        }
    }

    private final class MultiEndElementInfo implements XMLizable {
        private List<XMLizable> tags = new ArrayList<XMLizable>(2);

        public void add(EndElementInfo endElement) {
            this.tags.add(endElement);
        }

        public void add(StartElementInfo endElement) {
            this.tags.add(endElement);
        }

        public void toSAX(OutputHandler contentHandler) throws SAXException {
            for (int i = tags.size() - 1; i >= 0; i--) {
                XMLizable tag = tags.get(i);
                tag.toSAX(contentHandler);
            }
        }
    }

    interface XMLizable {
        public void toSAX(OutputHandler contentHandler) throws SAXException;
    }
}
