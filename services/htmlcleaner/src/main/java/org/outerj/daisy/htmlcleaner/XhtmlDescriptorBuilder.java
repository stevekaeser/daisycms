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

import org.cyberneko.dtd.parsers.DOMParser;
import org.xml.sax.InputSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

class XhtmlDescriptorBuilder {
    /**
     * Returns a Map containing ElementDescriptors for all elements in the XHTML strict DTD.
     */
    public Map<String, ElementDescriptor> build() throws Exception {
        DOMParser parser = new DOMParser();

        URL dtdURL = getClass().getClassLoader().getResource("org/outerj/daisy/htmlcleaner/xhtml1-strict.dtd");

        InputSource inputSource = new InputSource();
        inputSource.setByteStream(dtdURL.openStream());
        inputSource.setSystemId(dtdURL.toExternalForm());

        parser.parse(inputSource);
        Document document = parser.getDocument();

        Element dtdElement = document.getDocumentElement();
        Element externalSubset = findChildElement(dtdElement, "externalSubset");

        // build ElementDescriptors for each element
        Element[] elementDecls = findChildElements(externalSubset, "elementDecl");
        Map<String, ElementDescriptor> elementDescriptors = new HashMap<String, ElementDescriptor>();
        for (Element elementDecl : elementDecls) {
            String name = elementDecl.getAttribute("ename");
            elementDescriptors.put(name, new ElementDescriptor(name));
        }

        // add attribute information to them
        Element[] attlists = findChildElements(externalSubset, "attlist");
        for (Element attlist : attlists) {
            String ename = attlist.getAttribute("ename");
            Element[] attributeDecls = findChildElements(attlist, "attributeDecl");
            ElementDescriptor descriptor = elementDescriptors.get(ename);
            for (Element attributeDecl : attributeDecls) {
                String aname = attributeDecl.getAttribute("aname");
                if (!aname.equals("xmlns") && !aname.startsWith("xml:"))
                    descriptor.addAttribute(aname);
            }
        }

        // add child element information to them
        Element[] contentModels = findChildElements(externalSubset, "contentModel");
        for (Element contentModel : contentModels) {
            String ename = contentModel.getAttribute("ename");
            Element[] elements = findDescendants(contentModel, "element");
            ElementDescriptor descriptor = elementDescriptors.get(ename);
            for (Element element : elements) {
                String name = element.getAttribute("name");
                descriptor.addChild(name);
            }
        }

        return elementDescriptors;
    }

    private Element findChildElement(Element element, String name) {
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && node.getNodeName().equals(name))
                return (Element)node;
        }
        throw new RuntimeException("Did not find expected element: " + name);
    }

    private Element[] findChildElements(Element element, String name) {
        List<Element> foundElements = new ArrayList<Element>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && node.getNodeName().equals(name))
                foundElements.add((Element)node);
        }
        return foundElements.toArray(new Element[foundElements.size()]);
    }

    private Element[] findDescendants(Element element, String name) {
        List<Element> foundElements = new ArrayList<Element>();
        TreeWalker walker = ((DocumentTraversal)element.getOwnerDocument()).createTreeWalker(element, NodeFilter.SHOW_ELEMENT, null, false);
        while (walker.nextNode() != null) {
            Element currentEl = (Element)walker.getCurrentNode();
            if (currentEl.getNodeName().equals(name))
                foundElements.add(currentEl);
        }
        return foundElements.toArray(new Element[foundElements.size()]);
    }
}
