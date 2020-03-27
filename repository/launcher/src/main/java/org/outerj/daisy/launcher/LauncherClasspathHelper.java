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
package org.outerj.daisy.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class LauncherClasspathHelper {

    public static ClassLoader getClassLoader(String configResource, File repositoryLocation) {
        URL[] classPath = getClassPath(configResource, repositoryLocation);
        return new URLClassLoader(classPath, LauncherClasspathHelper.class.getClassLoader());
    }

    public static URL[] getClassPath(String configResource, File repositoryLocation) {
        Document document;
        InputStream is = LauncherClasspathHelper.class.getClassLoader().getResourceAsStream(configResource);
        if (is == null) {
            throw new RuntimeException("Resource not found: " + configResource);
        } else {
            try {
                document = parse(is);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing classloader configuration at " + configResource, e);
            }
        }

        Element classPathEl = null;
        NodeList children = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element && node.getLocalName().equals("classpath")) {
                classPathEl = (Element)node;
                break;
            }
        }

        if (classPathEl == null) {
            throw new RuntimeException("Classloader configuration does not contain a classpath element.");
        } else {
            List<URL> artifactURLs = new ArrayList<URL>();
            children = classPathEl.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node instanceof Element && node.getLocalName().equals("artifact")) {
                    Element artifactEl = (Element)node;
                    artifactURLs.add(getArtifactURL(artifactEl, repositoryLocation));
                }
            }
            return artifactURLs.toArray(new URL[0]);
        }
    }

    public static URL getArtifactURL(Element artifactEl, File repositoryLocation) {
        String groupId = artifactEl.getAttribute("groupId");
        String artifactId = artifactEl.getAttribute("artifactId");
        String version = artifactEl.getAttribute("version");

        File artifactFile = new File(repositoryLocation, groupId.replaceAll(Pattern.quote("."), Matcher.quoteReplacement(File.separator)) + File.separator + artifactId + File.separator + version + File.separator + artifactId + "-" + version + ".jar");
        if (!artifactFile.exists()) {
            throw new RuntimeException("Classpath entry not found at " + artifactFile.getAbsolutePath());
        } else {
            try {
                return artifactFile.toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static Document parse(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(is);
        return document;
    }
}
