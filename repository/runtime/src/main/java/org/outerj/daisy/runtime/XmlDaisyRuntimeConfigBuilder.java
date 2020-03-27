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
package org.outerj.daisy.runtime;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.runtime.repository.ArtifactRepository;
import org.outerj.daisy.configutil.PropertyResolver;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
import java.net.URL;
import java.net.URLDecoder;

public class XmlDaisyRuntimeConfigBuilder {
    public static DaisyRuntimeConfig build(File runtimeConfig, Set<String> disabledContainerIds, ArtifactRepository repository, Properties configProps) throws Exception {
        Document document = DocumentHelper.parse(runtimeConfig);
        return build(document.getDocumentElement(), disabledContainerIds, repository, configProps);
    }

    public static DaisyRuntimeConfig build(Element runtimeConfig, Set<String> disabledContainerIds, ArtifactRepository repository, Properties configProps) throws Exception {
        Element containersElement = DocumentHelper.getElementChild(runtimeConfig, "containers", true);

        List<ContainerEntry> imports = new ArrayList<ContainerEntry>();
        Element[] importEls = DocumentHelper.getElementChildren(containersElement);
        for (Element importEl : importEls) {
            if (importEl.getNamespaceURI() == null) {
                File fileToImport = null;
                String id = DocumentHelper.getAttribute(importEl, "id", true);
                if (importEl.getLocalName().equals("file")) {
                    String path = PropertyResolver.resolveProperties(DocumentHelper.getAttribute(importEl, "path", true));
                    fileToImport = new File(path);
                } else if (importEl.getLocalName().equals("artifact")) {
                    String groupId = DocumentHelper.getAttribute(importEl, "groupId", true);
                    String artifactId = DocumentHelper.getAttribute(importEl, "artifactId", true);
                    String version = DocumentHelper.getAttribute(importEl, "version", true);

                    // A bit of hackery to convert the artifact URL back to a file path,
                    // maybe we should rather change the ArtifactRepository to be file-based
                    URL artifactURL = repository.resolve(groupId, artifactId, version);
                    String artifactFilePath = URLDecoder.decode(artifactURL.getFile(), "UTF-8");
                    if (artifactFilePath == null || artifactFilePath.equals(""))
                        throw new DaisyRTException("Could not convert artifact URL to a file path: " + artifactURL);

                    fileToImport = new File(artifactFilePath);
                } else if (importEl.getLocalName().equals("directory")) {
                    String dirName = PropertyResolver.resolveProperties(DocumentHelper.getAttribute(importEl, "path", true));
                    File dir = new File(dirName);
                    if (dir.exists() && dir.isDirectory()) {
                        File[] jarFiles = dir.listFiles(new JarFilter());
                        // order of imports is important, to provide some deterministic behaviour, sort the entries by name
                        Arrays.sort(jarFiles, new FileNameComparator());
                        for (File file : jarFiles) {
                            String genId = id + "-" + stripJarExt(file.getName());
                            imports.add(new ContainerEntry(genId, file));
                        }
                    }
                } else {
                    throw new Exception("Unexpected element: " + importEl.getLocalName());
                }

                if (fileToImport != null) {
                    if (!fileToImport.exists()) {
                        throw new DaisyRTException("Import does not exist: " + fileToImport.getAbsolutePath());
                    }
                    imports.add(new ContainerEntry(id, fileToImport));
                }
            }
        }

        // Remove disabled container entries
        Iterator<ContainerEntry> it = imports.iterator();
        while (it.hasNext()) {
            ContainerEntry entry = it.next();
            if (disabledContainerIds.contains(entry.getId()))
                it.remove();
        }

        return new DaisyRuntimeConfig(imports, configProps, repository);
    }

    private static String stripJarExt(String name) {
        if (name.endsWith(".jar")) {
            return name.substring(0, name.length() - 4);
        } else {
            return name;
        }
    }

    private static class JarFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".jar");
        }
    }

    private static class FileNameComparator implements Comparator<File> {
        public int compare(File o1, File o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }
}
