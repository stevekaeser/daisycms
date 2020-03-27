/*
 * Copyright 2008 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Writes a listing of dependencies in a specific format to a predefined file.
 *
 * @goal gen-classloader
 * @requiresDependencyResolution runtime
 * @description Genenerate a Daisy classloader file for a module.
 */
public class DaisyClassloaderMojo extends AbstractMojo {

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Helper class to assist in attaching artifacts to the project instance. project-helper instance, used to
     * make addition of resources simpler.
     *
     * @component
     */
    private MavenProjectHelper projectHelper;

    /**
     * @parameter expression="src/main/resources/DAISY-INF"
     */
    private String templateDirectory;

    /**
     * @parameter expression="classloader-template.xml"
     */
    private String templateFileName;

    /**
     * Where should the file end up?
     *
     * @parameter expression="${project.build.directory}/classes/DAISY-INF"
     */
    private String targetDirectory;

    /**
     * Filename to use.
     *
     * @parameter expression="classloader.xml"
     */
    private String targetFileName;

    /**
     * Indicates whether the project artifact itself should also be included
     *
     * @parameter default-value="false"
     * @optional
     */
    protected boolean includeSelf;

    /**
     * If we should exclude transitive dependencies
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeTransitive}" default-value="false"
     */
    protected boolean excludeTransitive;

    /**
     * Comma Separated list of Types to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeTypes}" default-value=""
     * @optional
     */
    protected String includeTypes;

    /**
     * Comma Separated list of Types to exclude. Empty String indicates don't exclude anything (default).
     * Ignored if includeTypes is used.
     *
     * @since 2.0
     * @parameter expression="${excludeTypes}" default-value=""
     * @optional
     */
    protected String excludeTypes;

    /**
     * Scope to include. An Empty string indicates all scopes (default).
     *
     * @since 2.0
     * @parameter expression="${includeScope}" default-value="runtime"
     * @optional
     */
    protected String includeScope;

    /**
     * Scope to exclude. An Empty string indicates no scopes (default). Ignored if includeScope is used.
     *
     * @since 2.0
     * @parameter expression="${excludeScope}" default-value=""
     * @optional
     */
    protected String excludeScope;

    /**
     * Comma Separated list of Classifiers to include. Empty String indicates include everything (default).
     *
     * @since 2.0
     * @parameter expression="${includeClassifiers}" default-value=""
     * @optional
     */
    protected String includeClassifiers;

    /**
     * Comma Separated list of Classifiers to exclude. Empty String indicates don't exclude anything
     * (default). Ignored if includeClassifiers is used.
     *
     * @since 2.0
     * @parameter expression="${excludeClassifiers}" default-value=""
     * @optional
     */
    protected String excludeClassifiers;

    /**
     * Comma Seperated list of Artifact names too exclude. Ignored if includeArtifacts is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeArtifactIds}" default-value=""
     */
    protected String excludeArtifactIds;

    /**
     * Comma Seperated list of Artifact names to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeArtifactIds}" default-value=""
     */
    protected String includeArtifactIds;

    /**
     * Comma Seperated list of GroupId Names to exclude. Ignored if includeGroupsIds is used.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${excludeGroupIds}" default-value=""
     */
    protected String excludeGroupIds;

    /**
     * Comma Seperated list of GroupIds to include.
     *
     * @since 2.0
     * @optional
     * @parameter expression="${includeGroupIds}" default-value=""
     */
    protected String includeGroupIds;

    /**
     * The dependencies to list in file.
     */
    private Collection<Artifact> dependenciesToList;

    /**
     * Destination file.
     */
    private File projectDescriptorFile;

    /**
     * Mapping of artifacts with their corresponding dom-element.
     */
    private Map<Entry, Element> entryMap = new HashMap<Entry, Element>();

    /**
     * If specified, a share attribute is added on each entry with the given value.
     * @parameter expression="${defaultShare}"
     * @optional
     */
    private String defaultShare;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Creating Dependency List...");
            doDependencyResolution();
            if (dependenciesToList.size() > 0) {
                createDestinationFile();
                // lookup template file
                File file = new File(project.getBasedir(), templateDirectory + "/" + templateFileName);
                getLog().debug("Looking for classloader template in location '" + file + "'.");
                if (file.exists()) {
                    getLog().info("Found classloader template, trying to parse it...");
                    parseClassloaderTemplate(file);
                } else {
                    createDependencyListing();
                }
                getLog().info(
                        "Wrote " + dependenciesToList.size() + " dependencies to file "
                                + projectDescriptorFile.getPath());
                projectHelper.attachArtifact(project, "xml", "dependencylist", projectDescriptorFile);
            }
        } catch (Exception ex) {
            throw new MojoExecutionException("Error while creating dependency list.", ex);
        }
    }

    private void createDestinationFile() throws IOException {
        File targetDirectoryFile = new File(targetDirectory);
        if (!targetDirectoryFile.exists())
            targetDirectoryFile.mkdirs();

        projectDescriptorFile = new File(targetDirectoryFile, targetFileName);
        if (!projectDescriptorFile.exists())
            projectDescriptorFile.createNewFile();
    }

    /**
     * This method uses a Filtering technique as showed by the maven-dependency-plugin. It allows for
     * including/excluding artifacts in a number of ways.
     *
     * @throws MojoExecutionException
     */
    @SuppressWarnings("unchecked")
    private void doDependencyResolution() throws ArtifactFilterException {
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(), this.excludeTransitive));
        filter.addFilter(new ScopeFilter(this.includeScope, this.excludeScope));
        filter.addFilter(new TypeFilter(this.includeTypes, this.excludeTypes));
        filter.addFilter(new ClassifierFilter(this.includeClassifiers, this.excludeClassifiers));
        filter.addFilter(new GroupIdFilter(this.includeGroupIds, this.excludeGroupIds));
        filter.addFilter(new ArtifactIdFilter(this.includeArtifactIds, this.excludeArtifactIds));

        // start with all artifacts.
        Set<Artifact> artifacts = project.getArtifacts();

        if (includeSelf)
            artifacts.add(project.getArtifact());

        // perform filtering
        dependenciesToList = filter.filter(artifacts);
    }

    /**
     * Create a project file containing the dependencies.
     *
     * @throws IOException
     * @throws SAXException
     */
    private void createDependencyListing() throws IOException, SAXException {
        // fill in file with all dependencies
        FileOutputStream fos = new FileOutputStream(projectDescriptorFile);
        TransformerHandler ch = null;
        TransformerFactory factory = TransformerFactory.newInstance();
        if (factory.getFeature(SAXTransformerFactory.FEATURE)) {
            try {
                ch = ((SAXTransformerFactory) factory).newTransformerHandler();
                // set properties
                Transformer serializer = ch.getTransformer();
                serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                // set result
                Result result = new StreamResult(fos);
                ch.setResult(result);
                // start file generation
                ch.startDocument();
                AttributesImpl atts = new AttributesImpl();
                ch.startElement("", "classloader", "classloader", atts);
                ch.startElement("", "classpath", "classpath", atts);
                SortedSet<Artifact> sortedArtifacts = new TreeSet<Artifact>(dependenciesToList);
                for (Artifact artifact : sortedArtifacts) {
                    atts.addAttribute("", "groupId", "groupId", "CDATA", artifact.getGroupId());
                    atts.addAttribute("", "artifactId", "artifactId", "CDATA", artifact.getArtifactId());
                    if (artifact.getClassifier() != null)
                        atts.addAttribute("", "classifier", "classifier", "CDATA", artifact.getClassifier());
                    if (!artifact.getGroupId().startsWith("org.kauriproject"))
                        atts.addAttribute("", "version", "version", "CDATA", artifact.getVersion());
                    if (defaultShare != null)
                        atts.addAttribute("", "share", "share", "CDATA", defaultShare);
                    ch.startElement("", "artifact", "artifact", atts);
                    ch.endElement("", "artifact", "artifact");
                    atts.clear();
                }
                ch.endElement("", "classpath", "classpath");
                ch.endElement("", "classloader", "classloader");
                ch.endDocument();
                fos.close();
            } catch (TransformerConfigurationException ex) {
                ex.printStackTrace();
                throw new SAXException("Unable to get a TransformerHandler.");
            }
        } else
            throw new RuntimeException("Could not load SAXTransformerFactory.");
    }

    private void parseClassloaderTemplate(File file) throws Exception {
        DOMResult domResult = null;
        Transformer transformer = null;
        try {
            Source xmlSource = new StreamSource(file);
            TransformerFactory tfFactory = TransformerFactory.newInstance();
            if (tfFactory.getFeature(DOMResult.FEATURE)) {
                transformer = tfFactory.newTransformer();
                domResult = new DOMResult();
                transformer.transform(xmlSource, domResult);
            }
        } catch (TransformerException ex) {
            throw new Exception("Error parsing file '" + file + "'.");
        }
        if (domResult != null && transformer != null) {
            Document docu = (Document) domResult.getNode();
            Element classpath;
            NodeList list;
            try {
                classpath = (Element) docu.getElementsByTagName("classpath").item(0);
                list = classpath.getElementsByTagName("artifact");
            } catch (NullPointerException npex) {
                throw new Exception("classloader template is invalid");
            }
            NamedNodeMap map;
            Entry entry;
            String groupId;
            String classifier;
            String artifactId;
            String version;
            String share;
            Element domArtifact;
            for (int i = 0; i < list.getLength(); i++) {
                domArtifact = (Element) list.item(i);
                map = domArtifact.getAttributes();
                groupId = extractAttVal(map, "groupId");
                artifactId = extractAttVal(map, "artifactId");
                classifier = extractAttVal(map, "classifier");
                version = extractAttVal(map, "version");
                share = extractAttVal(map, "share");
                entry = new Entry(groupId, artifactId, classifier, version, share);
                entryMap.put(entry, domArtifact);
            }

            // now add missing dependencies
            for (Artifact artifact : dependenciesToList) {
                entry = new Entry(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getVersion(), defaultShare);
                if (entryMap.containsKey(entry)) {
                    // complete existing entry
                    completeArtifactNode(entryMap.get(entry), entry);
                    entryMap.remove(entry);
                } else {
                    // missing entry
                    classpath.appendChild(entry.toDOM(docu));
                }
            }

            // print errors or warnings if there are unresolved entries in the template
            if (entryMap.size() > 0) {
                String msg = "";
                boolean incomplete = false;
                for (Entry key : entryMap.keySet()) {
                    if (!key.isComplete()) {
                        incomplete = true;
                    }
                    msg += key + ";";
                }
                getLog().warn("Template contains entries that could not be resolved: " + msg);
                if (incomplete) {
                    throw new Exception("Some of the unresolved entries are incomplete.");
                }
            }

            // serialize to file
            Result result = null;
            try {
                Source domSource = new DOMSource(docu);
                result = new StreamResult(projectDescriptorFile);
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.transform(domSource, result);
            } catch (TransformerException ex) {
                throw new Exception("Error serializing dom to file '" + projectDescriptorFile + "': " + ex);
            }
        }
    }

    private String extractAttVal(NamedNodeMap map, String name) {
        Node node = map.getNamedItem(name);
        return (node != null) ? node.getNodeValue() : null;
    }

    private void completeArtifactNode(Element domArtifact, Entry entry) throws Exception {
        NamedNodeMap map = domArtifact.getAttributes();
        if (map.getNamedItem("groupId") == null || map.getNamedItem("artifactId") == null) {
            getLog().error("groupId and artifactId must be provided for each entry in the template");
            throw new Exception("Template contains invalid entries.");
        }
        if (map.getNamedItem("version") == null) {
            domArtifact.setAttribute("version", entry.version);
        } else if (!map.getNamedItem("version").getNodeValue().equals(entry.version)) {
            getLog()
                    .warn("version conflict between entry in template and artifact on classpath for " + entry);
        }
    }

    private class Entry {
        protected String groupId;
        protected String artifactId;
        protected String classifier;
        protected String version;
        protected String share;

        public Entry(String groupId, String artifactId, String classifier, String version, String share) {
            this.groupId = groupId;
            this.artifactId = artifactId;
            this.classifier = classifier;
            this.version = version;
            this.share = share;
        }

        public Node toDOM(Document docu) {
            Element art = docu.createElement("artifact");
            art.setAttribute("groupId", groupId);
            art.setAttribute("artifactId", artifactId);
            if (classifier != null)
                art.setAttribute("classifier", classifier);
            art.setAttribute("version", version);
            if (share != null)
                art.setAttribute("share", share);
            return art;
        }

        public boolean isComplete() {
            return groupId != null && artifactId != null && version != null;
        }

        /**
         * @see java.lang.Object#hashCode()
         */
        public int hashCode() {
            return new HashCodeBuilder(715542779, 122039963).append(this.groupId).append(this.artifactId)
                    .append(this.classifier).toHashCode();
        }

        /**
         * @see java.lang.Object#equals(Object)
         */
        public boolean equals(Object object) {
            if (!(object instanceof Entry)) {
                return false;
            }
            Entry rhs = (Entry) object;
            boolean equal = true;
            if (!this.groupId.equals(rhs.groupId)) {
                equal = false;
            } else if (!this.artifactId.equals(rhs.artifactId)) {
                equal = false;
            } else if (!ObjectUtils.equals(this.classifier, rhs.classifier)) {
                equal = false;
            }

            return equal;
        }

        
        public String toString() {
            return "artifact(" + groupId + "," + artifactId + "," + classifier + "," + version + ")";
        }

    }

}