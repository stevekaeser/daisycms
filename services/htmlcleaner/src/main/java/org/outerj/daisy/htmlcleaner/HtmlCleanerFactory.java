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

import org.xml.sax.InputSource;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link HtmlCleanerTemplate} based on a XML configuration.
 * The configuration describes such things as which elements and
 * attributes should be kept, or how wide the output should be.
 * See the example config files.
 *
 * <p>Instances of this class are not thread safe and not reusable,
 * in other words construct a new HtmlCleanerFactory each time you
 * need it.
 */
public class HtmlCleanerFactory {
    private boolean handledCleanup = false;
    private boolean handledSerialization = false;
    HtmlCleanerTemplate template = new HtmlCleanerTemplate();

    public HtmlCleanerTemplate buildTemplate(InputSource is) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document document = db.parse(is);
        document.normalize();

        Element docEl = document.getDocumentElement();
        if (!(docEl.getLocalName().equals("htmlcleaner") && docEl.getNamespaceURI() == null)) {
            throw new Exception("Htmlcleaner config file should have root elemnet 'htmlcleaner'.");
        }

        NodeList nodeList = docEl.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);

            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("cleanup")) {
                    handleCleanupNode((Element)node);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("serialization")) {
                    handleSerializationNode((Element)node);
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element: " + node.getNodeName());
                }
            }
        }
        template.initialize();
        return template;
    }

    private void handleCleanupNode(Element cleanupEl) throws Exception {
        if (handledCleanup)
            throw new Exception("Error in htmlcleaner config: cleanup element is only allowed once");
        handledCleanup = true;

        NodeList cleanupNodes = cleanupEl.getChildNodes();
        for (int k = 0; k < cleanupNodes.getLength(); k++) {
            Node node = cleanupNodes.item(k);
            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-span-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addAllowedSpanClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-div-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addAllowedDivClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-para-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addAllowedParaClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-pre-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addAllowedPreClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-ol-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addAllowedOlClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("drop-div-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addDropDivClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("drop-table-classes")) {
                    String[] classes = getClassChildren((Element)node);
                    for (String clazz : classes)
                        template.addDropTableClass(clazz);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("allowed-elements")) {
                    handleAllowedElementsNode((Element)node);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("img-alternate-src-attr")) {
                    String name = ((Element)node).getAttribute("name");
                    if (name.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing name attribute on img-alternate-src-attr");
                    template.setImgAlternateSrcAttr(name);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("link-alternate-href-attr")) {
                    String name = ((Element)node).getAttribute("name");
                    if (name.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing name attribute on link-alternate-href-attr");
                    template.setLinkAlternateHrefAttr(name);
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element " + node.getNodeName() + " inside " + cleanupEl.getNodeName());
                }
            }
        }

    }

    private String[] getClassChildren(Element element) throws Exception {
        List<String> classes = new ArrayList<String>();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("class")) {
                    Node text = node.getFirstChild();
                    if (text instanceof Text) {
                        classes.add(((Text)text).getData());
                    } else {
                        throw new Exception("Error in htmlcleaner: element class does not have a text node child");
                    }
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element: " + node.getNodeName() + " as child of " + element.getNodeName());
                }
            }
        }
        return classes.toArray(new String[classes.size()]);
    }

    private void handleAllowedElementsNode(Element element) throws Exception {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);

            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("element")) {
                    String name = ((Element)node).getAttribute("name");
                    if (name.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing name attribute on 'element' element");
                    String[] attributes = getAttributeChildren((Element)node);
                    template.addAllowedElement(name, attributes);
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element: '" + node.getNodeName() + "' as child of " + element.getNodeName());
                }
            }
        }
    }

    private String[] getAttributeChildren(Element element) throws Exception {
        List<String> names = new ArrayList<String>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("attribute")) {
                    String name = ((Element)node).getAttribute("name");
                    if (name.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing name attribute on attribute element");
                    names.add(name);
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element: '" + node.getNodeName() + "' as child of " + element.getNodeName());
                }
            }
        }
        return names.toArray(new String[names.size()]);
    }

    private void handleSerializationNode(Element element) throws Exception {
        if (handledSerialization)
            throw new Exception("Error in htmlcleaner config: serialization element is only allowed once");
        handledSerialization = true;

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("linewidth")) {
                    String value = ((Element)node).getAttribute("value");
                    if (value.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing value attribute on linewidth element.");
                    int intValue = Integer.parseInt(value);
                    template.setMaxLineWidth(intValue);
                } else if (node.getNamespaceURI() == null && node.getLocalName().equals("elements")) {
                    handleElementsNode((Element)node);
                } else {
                    throw new Exception("Error in htmlcleaner config: unexpected element '" + node.getNodeName() + "' as child of " + element.getNodeName());
                }
            }
        }
    }

    private void handleElementsNode(Element element) throws Exception {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element) {
                if (node.getNamespaceURI() == null && node.getLocalName().equals("element")) {
                    Element childEl = (Element)node;
                    String name = childEl.getAttribute("name");
                    if (name.equals(""))
                        throw new Exception("Error in htmlcleaner config: missing name attribute on 'element' element.");
                    String beforeOpenAttr = childEl.getAttribute("beforeOpen");
                    String afterOpenAttr = childEl.getAttribute("afterOpen");
                    String beforeCloseAttr = childEl.getAttribute("beforeClose");
                    String afterCloseAttr = childEl.getAttribute("afterClose");
                    int beforeOpen = 0, afterOpen = 0, beforeClose = 0, afterClose = 0;
                    if (!beforeOpenAttr.equals(""))
                        beforeOpen = Integer.parseInt(beforeOpenAttr);
                    if (!afterOpenAttr.equals(""))
                        afterOpen = Integer.parseInt(afterOpenAttr);
                    if (!beforeCloseAttr.equals(""))
                        beforeClose = Integer.parseInt(beforeCloseAttr);
                    if (!afterCloseAttr.equals(""))
                        afterClose = Integer.parseInt(afterCloseAttr);
                    boolean inline = "true".equals(childEl.getAttribute("inline"));
                    template.addOutputElement(name, beforeOpen, afterOpen, beforeClose, afterClose, inline);
                }
            }
        }
    }
}
