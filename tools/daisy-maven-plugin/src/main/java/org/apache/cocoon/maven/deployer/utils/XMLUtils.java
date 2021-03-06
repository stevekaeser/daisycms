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
package org.apache.cocoon.maven.deployer.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @version $Id: XMLUtils.java 588009 2007-10-24 20:39:12Z vgritsenko $
 */
public class XMLUtils {

    public static Document parseXml(File file) throws IOException, SAXException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(new FileInputStream(file));
            return parseXml(is);
        } finally {
            if ( is != null ) {
                try {
                    is.close();
                } catch (IOException ignore) {
                    // ignore
                }
            }
        }

    }

    public static Document parseXml(InputStream source) throws IOException, SAXException {
        try {
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            documentFactory.setNamespaceAware(false);
            documentFactory.setValidating(false);
            DocumentBuilder docBuilder = documentFactory.newDocumentBuilder();

            return docBuilder.parse(source);
        } catch (ParserConfigurationException pce) {
            throw new IOException("Creating document failed:" + pce.getMessage());
        }
    }

    public static void write(Document node, OutputStream out) throws TransformerFactoryConfigurationError, TransformerException {
        final Properties format = new Properties();
        format.put(OutputKeys.METHOD, "xml");
        format.put(OutputKeys.OMIT_XML_DECLARATION, "no");
        format.put(OutputKeys.INDENT, "yes");
        if (node.getDoctype() != null) {
            if (node.getDoctype().getPublicId() != null) {
                format.put(OutputKeys.DOCTYPE_PUBLIC, node.getDoctype().getPublicId());
            }
            if (node.getDoctype().getSystemId() != null) {
                format.put(OutputKeys.DOCTYPE_SYSTEM, node.getDoctype().getSystemId());
            }
        }
        Transformer transformer;
        transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperties(format);
        transformer.transform(new DOMSource(node), new StreamResult(out));
    }

    public static List getChildNodes(Element parent, String nodeName) {
        final List nodes = new ArrayList();
        if (parent != null && nodeName != null) {
            final NodeList children = parent.getChildNodes();
            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    if (nodeName.equals(children.item(i).getLocalName())) {
                        nodes.add(children.item(i));
                    }
                }
            }
        }
        return nodes;
    }

    public static Element getChildNode(Element parent, String nodeName) {
        final List children = getChildNodes(parent, nodeName);
        if (children.size() > 0) {
            return (Element) children.get(0);
        }

        return null;
    }

    public static String getValue(Element node) {
        if (node != null) {
            if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
                return node.getNodeValue();
            } else {
                node.normalize();
                NodeList childs = node.getChildNodes();
                int i = 0;
                int length = childs.getLength();
                while (i < length) {
                    if (childs.item(i).getNodeType() == Node.TEXT_NODE) {
                        return childs.item(i).getNodeValue().trim();
                    } else {
                        i++;
                    }
                }
            }
        }
        return null;
    }

    public static void setValue(Element node, String value) {
        if (node != null) {
            // remove all children
            while (node.hasChildNodes()) {
                node.removeChild(node.getFirstChild());
            }
            node.appendChild(node.getOwnerDocument().createTextNode(value));
        }
    }
}