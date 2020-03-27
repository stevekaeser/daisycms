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
package org.outerj.daisy.runtime.classloading;

import org.w3c.dom.Element;
import org.outerj.daisy.xmlutil.DocumentHelper;
import org.outerj.daisy.runtime.repository.ArtifactRepository;
import org.outerj.daisy.runtime.repository.ArtifactRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.List;
import java.util.ArrayList;

/**
 * Builds a ClassLoadingConfig from XML.
 */
public class XmlClassLoaderBuilder {
    private final Log log = LogFactory.getLog(getClass());
    private Element element;
    private List<URL> initialEntries;
    private ArtifactRepository repository;

    /**
     *
     * @param initialEntries initial entries, not specified in the XML config, to insert at the beginning of the classpath
     */
    public static ClassLoadingConfig build(Element element, List<URL> initialEntries, ArtifactRepository repository) throws Exception {
        return new XmlClassLoaderBuilder(element, initialEntries, repository).build();
    }

    private XmlClassLoaderBuilder(Element element, List<URL> initialEntries, ArtifactRepository repository) {
        this.element = element;
        this.initialEntries = initialEntries;
        this.repository = repository;
    }

    private ClassLoadingConfig build() throws Exception {

        List<ClassPathEntry> classpath = new ArrayList<ClassPathEntry>();

        Element classPathElement = DocumentHelper.getElementChild(element, "classpath", false);
        if (classPathElement != null) {
            Element[] classPathEls = DocumentHelper.getElementChildren(classPathElement);
            classpath: for (Element classPathEl : classPathEls) {
                if (classPathEl.getLocalName().equals("artifact") && classPathEl.getNamespaceURI() == null) {
                    // Create ArtifactRef
                    String groupId = DocumentHelper.getAttribute(classPathEl, "groupId", true);
                    String artifactId = DocumentHelper.getAttribute(classPathEl, "artifactId", true);
                    String version = DocumentHelper.getAttribute(classPathEl, "version", true);
                    ArtifactRef artifactRef = new ArtifactRef(groupId, artifactId, version);

                    // Check for double artifacts
                    for (ClassPathEntry entry : classpath) {
                        if (entry.getArtifactRef().equals(artifactRef)) {
                            log.error("Classloader specification contains second reference to same artifact, will skip second reference. Artifact = " + artifactRef);
                            continue classpath;
                        } else if (entry.getArtifactRef().equalsIgnoreVersion(artifactRef)) {
                            log.warn("Classloader specification contains second reference to same artifact but different version. Artifact = " + artifactRef);
                        }
                    }

                    // Creating SharingMode
                    String sharingModeParam = classPathEl.getAttribute("share");
                    ArtifactSharingMode sharingMode;
                    if (sharingModeParam == null || sharingModeParam.equals(""))
                        sharingMode = ArtifactSharingMode.PROHIBITED;
                    else
                        sharingMode = ArtifactSharingMode.fromString(sharingModeParam);

                    classpath.add(new ClassPathEntry(artifactRef, sharingMode));
                }
            }
        }

        return new ClassLoadingConfigImpl(classpath, initialEntries, repository);
    }
}
