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
package org.outerj.daisy.tools.artifacter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * A search and refactor tool for artifacts in Maven pom.xml and
 * runtime configuration (classloader.xml and runtime-config.xml) files.
 */
public class Artifacter {
    
    private static final XPathFactory XPATH_FACTORY = XPathFactory.newInstance();
    private static final MyNamespaceContext NAMESPACE_CONTEXT = new MyNamespaceContext();
    
    public static void main(String[] args) throws Exception {
        new Artifacter().run(args);
    }

    private Collection<XmlFile> mavenProjectXmls;
    private Collection<XmlFile> runtimeXmls;

    public void run(String[] args) throws Exception {

        Options options = new Options();
        options.addOption(new Option("f", "find-usages", true, "Find usages of an artifact <groupId:artifactId>"));
        options.addOption(new Option("r", "rename", false, "Renames artifact usages, use options o and n to specify original and new artifact name"));
        options.addOption(new Option("o", "original", true, "original artifact name in rename operation as <groupId:artifactId>"));
        options.addOption(new Option("n", "new", true, "new artifact name in rename operation as <groupId:artifactId>"));
        options.addOption(new Option("u", "update", true, "Update artifact version, specify <groupId:artifactId:versionId>"));
        options.addOption(new Option("g", "global-update", true, "Update version of all artifacts of a certain group, specify <groupId:versionId>"));
        options.addOption(new Option("c", "create-local-repo", false, "Creates a repository directory structure containing all jars contained in the runtime configuration files (classloader.xml etc.)."));
        options.addOption(new Option("l", "copy-deps-into-local-repo", true, "Copies the dependencies from the pom.xml given as argument into another maven repository. Needs -s and -d arguments."));
        options.addOption(new Option("a", "copy-artifact-into-local-repo", true, "Copies the artifact built by the pom.xml given as argument into another maven repository. Needs -s and -d arguments."));
        options.addOption(new Option("s", "source-repo", true, "Source repository for -c and -l."));
        options.addOption(new Option("d", "dest-repo", true, "Destination repository for -c and -l."));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption('f')) {
            String artifactSpec = cmd.getOptionValue('f');
            findUsages(artifactSpec);
        } else if (cmd.hasOption('r')) {
            if (!cmd.hasOption('o') || !cmd.hasOption('n')) {
                System.out.println("Missing arguments for rename operation.");
                return;
            }
            String originalArtifactSpec = cmd.getOptionValue('o');
            String newArtifactSpec = cmd.getOptionValue('n');
            renameArtifact(originalArtifactSpec, newArtifactSpec);
        } else if (cmd.hasOption('u')) {
            String artifactSpec = cmd.getOptionValue('u');
            updateArtifactVersion(artifactSpec);
        } else if (cmd.hasOption('g')) {
            String spec = cmd.getOptionValue('g');
            globalVersionUpdate(spec);
        } else if (cmd.hasOption('c') || cmd.hasOption('l') || cmd.hasOption('a')) {
            File sourceRepo = new File(cmd.getOptionValue('s'));
            if (!sourceRepo.exists()) {
                System.err.println("Source repository " + sourceRepo.getAbsolutePath() + " does not exist.");
                System.exit(1);
            }
            File destinationRepo = new File(cmd.getOptionValue('d'));
            if (!destinationRepo.exists()) {
                System.err.println("Destination repository " + destinationRepo.getAbsolutePath() + " does not exist.");
                System.exit(1);
            }
            if (cmd.hasOption('c')) {
                createRuntimeDepsRepository(sourceRepo, destinationRepo);
            } else if (cmd.hasOption('l')) {
                File projectXml = new File(cmd.getOptionValue('l'));
                if (!projectXml.exists()) {
                    System.err.println("Maven pom.xml " + destinationRepo.getAbsolutePath() + " does not exist.");
                    System.exit(1);
                }
                throw new UnsupportedOperationException("This operation is no longer supported.  Use mvn dependency:copy-dependencies.");
            } else if (cmd.hasOption('a')) {
                File projectXml = new File(cmd.getOptionValue('a'));
                if (!projectXml.exists()) {
                    System.err.println("Maven pom.xml " + destinationRepo.getAbsolutePath() + " does not exist.");
                    System.exit(1);
                }
                copyArtifactToRepository(sourceRepo, destinationRepo, projectXml);
            }
        } else {
            printHelp();
        }

    }

    private void printHelp() {
        System.out.println("[help instructions todo]");
    }

    private void updateArtifactVersion(String artifact) throws Exception {
        findFiles();

        Pattern artifactPattern = Pattern.compile("^(.*):(.*):(.*)$");
        Matcher matcher = artifactPattern.matcher(artifact);
        if (!matcher.matches()) {
            System.out.println("Invalid artifact spec: " + artifact);
            System.exit(1);
        }

        String groupId = matcher.group(1);
        String artifactId = matcher.group(2);
        String version = matcher.group(3);

        updateInProjectXmls(groupId, artifactId, version);
        updateInRuntimeXmls(groupId, artifactId, version);
        saveFiles(mavenProjectXmls);
        saveFiles(runtimeXmls);
    }

    private void updateInProjectXmls(String groupId, String artifactId, String version) throws Exception {
        System.out.println("Searching and updating in pom.xml files...");

        XPathExpression xfind = xpath(groupId, artifactId, version).compile("//*[mvn:groupId=$groupId and mvn:artifactId=$artifactId]");
        XPathExpression xversion = xpath().compile("mvn:version");

        updateInProjectXmls(version, xfind, xversion);
    }

    private void updateInProjectXmls(String version, XPathExpression xfind,
            XPathExpression xversion) throws XPathExpressionException {
        for (XmlFile xmlFile : mavenProjectXmls) {
            // dependencies
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i);
                Element versionEl = (Element)xversion.evaluate(element, XPathConstants.NODE);
                if (versionEl == null)
                    continue;
                if (versionEl.getTextContent().startsWith("$")) // don't update expressions
                    continue;
                
                setElementValue(versionEl, version);
                xmlFile.changed = true;
            }
        }
    }

    private void updateInRuntimeXmls(String groupId, String artifactId, String version) throws Exception {
        System.out.println("Searching and updating in runtime files...");

        XPathExpression xfind = xpath(groupId, artifactId, version).compile("//artifact[@groupId=$groupId and @artifactId=$artifactId]");

        for (XmlFile xmlFile : runtimeXmls) {
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i);
                element.setAttribute("version", version);
                xmlFile.changed = true;
            }
        }
    }

    private void globalVersionUpdate(String spec) throws Exception {
        findFiles();

        Pattern artifactPattern = Pattern.compile("^([^:]*):([^:]*)$");
        Matcher matcher = artifactPattern.matcher(spec);
        if (!matcher.matches()) {
            System.out.println("Invalid spec: " + spec);
            System.exit(1);
        }

        String groupId = matcher.group(1);
        String version = matcher.group(2);

        globalUpdateInProjectXmls(groupId, version);
        globalUpdateInRuntimeXmls(groupId, version);
        saveFiles(mavenProjectXmls);
        saveFiles(runtimeXmls);
    }

    private void globalUpdateInProjectXmls(String groupId, String version) throws Exception {
        System.out.println("Searching and updating in pom.xml files...");

        XPathExpression xfind = xpath(groupId).compile("//*[mvn:groupId=$groupId]");
        XPathExpression xversion = xpath().compile("mvn:version");

        updateInProjectXmls(version, xfind, xversion);
    }

    private void globalUpdateInRuntimeXmls(String groupId, String version) throws Exception {
        System.out.println("Searching and updating in runtime files...");

        XPathExpression xfind = xpath(groupId).compile("//artifact[@groupId=$groupId]");

        for (XmlFile xmlFile : runtimeXmls) {
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i);
                element.setAttribute("version", version);
                xmlFile.changed = true;
            }
        }
    }

    private void renameArtifact(String originalArtifactSpec, String newArtifactSpec) throws Exception{
        Artifact originalArtifact = parseArtifactSpec(originalArtifactSpec);
        Artifact newArtifact = parseArtifactSpec(newArtifactSpec);

        findFiles();
        checkIfNewAlreadyExists(newArtifact);
        renameInProjectXmls(originalArtifact, newArtifact);
        renameUsagesInRuntimeXmls(originalArtifact, newArtifact);
        saveFiles(mavenProjectXmls);
        saveFiles(runtimeXmls);
    }

    private void checkIfNewAlreadyExists(Artifact newArtifact) throws Exception {
        XPathExpression xfind = xpath(newArtifact).compile("//*[mvn:groupId=$groupId and mvn:artifactId=$artifactId]");

        for (XmlFile xmlFile : mavenProjectXmls) {
            if ((Boolean)xfind.evaluate(xmlFile.document, XPathConstants.BOOLEAN)) {
                System.out.println("Warning: the following file:");
                System.out.println(xmlFile.file.getAbsolutePath());
                System.out.println("already defines the artifact " + newArtifact.groupId + ":" + newArtifact.artifactId);
                if (!promptYesNo("Are you sure you want to continue? [yes/no, default = no]", false)) {
                    System.exit(1);
                }
            }
        }
    }

    private void renameInProjectXmls(Artifact originalArtifact, Artifact newArtifact) throws Exception {
        System.out.println("Searching and renaming in pom.xml files...");
        
        XPathExpression xfind = xpath(originalArtifact).compile("//*[mvn:groupId=$groupId and mvn:artifactId=$artifactId]");
        
        XPathExpression xgroup = xpath(newArtifact).compile("mvn:groupId");
        XPathExpression xartifact = xpath(newArtifact).compile("mvn:artifactId");

        for (XmlFile xmlFile : mavenProjectXmls) {
            // dependencies
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i);
                Element groupIdEl = (Element)xgroup.evaluate(element, XPathConstants.NODE);
                Element artifactIdEl = (Element)xartifact.evaluate(element, XPathConstants.NODE);
                setElementValue(groupIdEl, newArtifact.groupId);
                setElementValue(artifactIdEl, newArtifact.artifactId);
                xmlFile.changed = true;
            }
        }
    }

    private void setElementValue(Element element, String value) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++)
            element.removeChild(nodes.item(i));

        element.appendChild(element.getOwnerDocument().createTextNode(value));
    }

    private void renameUsagesInRuntimeXmls(Artifact originalArtifact, Artifact newArtifact) throws Exception {
        System.out.println("Searching and renaming in runtime files...");
        XPathExpression xfind = xpath(originalArtifact).compile("//artifact[@groupId=$groupId and @artifactId=$artifactId]");
        for (XmlFile xmlFile : runtimeXmls) {
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i);
                element.setAttribute("groupId", newArtifact.groupId);
                element.setAttribute("artifactId", newArtifact.artifactId);
                xmlFile.changed = true;
            }
        }
    }

    private void findUsages(String artifactSpec) throws Exception {
        Artifact artifact = parseArtifactSpec(artifactSpec);

        System.out.println("Will search for groupId '" + artifact.groupId + "' and artifactId '" + artifact.artifactId + "'.");
        System.out.println();

        findFiles();
        printUsagesInProjectXmls(artifact);
        printUsagesInRuntimeXmls(artifact);
    }

    private void printUsagesInProjectXmls(Artifact artifact) throws Exception {
        System.out.println("Searching in pom.xml files...");
        System.out.println("---------------------------------");

        XPathExpression xfind = xpath(artifact).compile("//mvn:dependency[mvn:groupId=$groupId and mvn:artifactId=$artifactId]");
        XPathExpression xversion = xpath().compile("string(mvn:version)");

        printUsages(xfind, xversion, mavenProjectXmls);
        System.out.println();
    }

    private void printUsagesInRuntimeXmls(Artifact artifact) throws Exception {
        System.out.println("Searching in runtime files...");
        System.out.println("-----------------------------");

        XPathExpression xfind = xpath(artifact).compile("//artifact[@artifactId=$artifactId and @groupId=$groupId]");
        XPathExpression xversion = xpath().compile("string(@version)");
        
        printUsages(xfind, xversion, runtimeXmls);
        System.out.println();
    }

    private void printUsages(XPathExpression xfind, XPathExpression xversion,
            Collection<XmlFile> xmlFiles) throws XPathExpressionException {
        for (XmlFile xmlFile : xmlFiles) {
            NodeList elements = (NodeList) xfind.evaluate(xmlFile.document,
                    XPathConstants.NODESET);
            for (int i = 0; i < elements.getLength(); i++) {
                Element element = (Element) elements.item(i);
                String version = (String) xversion.evaluate(element, XPathConstants.STRING);
                System.out.println(xmlFile.file.getAbsolutePath() + " uses version " + version);
            }
        }
    }

    private Document parseFile(File file) throws Exception {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

    private void saveFiles(Collection<XmlFile> xmlFiles) throws Exception {
        for (XmlFile xmlFile : xmlFiles) {
            if (xmlFile.changed) {
                save(xmlFile.file, xmlFile.document);
                System.out.println("Saved updated " + xmlFile.file.getAbsolutePath());
            }
        }
    }

    private void save(File file, Document document) throws Exception {
        DOMSource source = new DOMSource(document);
        StreamResult fileResult = new StreamResult(file);

        SAXTransformerFactory saxTransformerFactory = (SAXTransformerFactory)SAXTransformerFactory.newInstance();
        TransformerHandler handler = saxTransformerFactory.newTransformerHandler();
        handler.getTransformer().setOutputProperty("encoding", "UTF-8");
        handler.setResult(fileResult);

        AttributeReorderHandler attributeReorderHandler = new AttributeReorderHandler(handler);
        SAXResult attributeReorderResult = new SAXResult();
        attributeReorderResult.setHandler(attributeReorderHandler);
        attributeReorderResult.setLexicalHandler(attributeReorderHandler);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, attributeReorderResult);
    }

    private void findFiles() throws Exception {
        File workingDir = new File(System.getProperty("user.dir"));
        System.out.println("Working directory is: " + workingDir);
        System.out.println();

        findMavenProjectXmls(workingDir);
        findRuntimeXmls(workingDir);
    }

    private void findMavenProjectXmls(File workingDir) throws Exception {
        System.out.println("Searching for Maven pom.xml files...");
        mavenProjectXmls = findFiles(workingDir, new MavenProjectFileFilter());
        System.out.println("Found " + mavenProjectXmls.size() + " pom.xml files.");
        System.out.println();
    }

    private void findRuntimeXmls(File workingDir) throws Exception {
        System.out.println("Searching for runtime files...");
        runtimeXmls = findFiles(workingDir, new RuntimeFileFilter());
        System.out.println("Found " + runtimeXmls.size() + " runtime configuration files.");
        System.out.println();
    }

    private Collection<XmlFile> findFiles(File startDir, FileFilter filter) throws Exception {
        List<XmlFile> result = new ArrayList<XmlFile>();
        File[] files = startDir.listFiles();
        for (File file : files) {
            if (file.isDirectory() && !file.getName().equals("target")) {
                Collection<XmlFile> subDirResult = findFiles(file, filter);
                result.addAll(subDirResult);
            } else if (filter.accept(file.getName())) {
                XmlFile xmlFile = new XmlFile();
                xmlFile.file = file;
                xmlFile.document = parseFile(xmlFile.file);
                result.add(xmlFile);
            }
        }
        return result;
    }

    public Artifact parseArtifactSpec(String artifactSpec) throws Exception {
        int colonPos = artifactSpec.indexOf(':');
        if (colonPos == -1)
            throw new Exception("artifact specification does not contain a colon");

        Artifact artifact = new Artifact();
        artifact.groupId = artifactSpec.substring(0, colonPos);
        artifact.artifactId = artifactSpec.substring(colonPos + 1);
        return artifact;
    }

    private boolean promptYesNo(String message, boolean defaultInput) throws Exception {
        String input = "";
        while (!input.equals("yes") && !input.equals("no")) {
            input = prompt(message, defaultInput ? "yes" : "no");
            input = input.toLowerCase();
        }
        return input.equals("yes");
    }

    private String prompt(String message, String defaultInput) throws Exception {
        System.err.println(message);
        System.err.flush();
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String input;
        try {
            input = in.readLine();
        } catch (IOException e) {
            throw new Exception("Error reading input from console.", e);
        }
        if (input == null || input.trim().equals(""))
            input = defaultInput;
        return input;
    }

    static class Artifact {
        public String groupId;
        public String artifactId;
    }

    static class XmlFile {
        public File file;
        public Document document;
        public boolean changed = false;
    }

    /**
     * Creates a repository containing all dependencies from all runtime configuration files.
     */
    private void createRuntimeDepsRepository(File sourceRepo, File destinationRepo) throws Exception {
        findFiles();

        XPathExpression xfind = xpath().compile("//artifact");
        for (XmlFile xmlFile : runtimeXmls) {
            NodeList elements = (NodeList)xfind.evaluate(xmlFile.document, XPathConstants.NODESET);
            for (int i=0; i<elements.getLength(); i++) {
                Element element = (Element)elements.item(i); 
                String groupId = element.getAttribute("groupId");
                String artifactId = element.getAttribute("artifactId");
                String version = element.getAttribute("version");

                copyArtifact(sourceRepo, destinationRepo, groupId, artifactId, version);
            }
        }
    }

    private void copyArtifact(File sourceRepo, File destinationRepo, String groupId, String artifactId, String version) throws Exception {
        String jarName = artifactId + "-" + version + ".jar";

        File srcGroupDir = sourceRepo;
        File destGroupDir = destinationRepo;
        for (String groupPart: groupId.split("\\.")) {
            srcGroupDir = new File(srcGroupDir, groupPart);
            destGroupDir = new File(destGroupDir, groupPart);
        }
        
        File srcDir = new File(new File(srcGroupDir, artifactId), version);
        File srcFile = new File(srcDir, jarName);

        File destDir = new File(new File(destGroupDir, artifactId), version);
        File destFile = new File(destDir, jarName);

        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        System.out.println("Copying from: " + srcFile.getAbsolutePath());
        System.out.println("        to:   " + destFile.getAbsolutePath());
        copyFile(srcFile, destFile);
    }

    private void copyArtifactToRepository(File sourceRepo, File destinationRepo, File projectXml) throws Exception {
        Document project = parseFile(projectXml);

        String groupId = xpath().compile("string(/mvn:project/mvn:groupId)").evaluate(project);
        String artifactId = xpath().compile("string(/mvn:project/mvn:artifactId)").evaluate(project);
        String version = xpath().compile("string(/mvn:project/mvn:version)").evaluate(project);
        copyArtifact(sourceRepo, destinationRepo, groupId, artifactId, version);
    }

    private void copyFile(File source, File destination) throws Exception {
        FileChannel srcChannel = new FileInputStream(source).getChannel();
        FileChannel dstChannel = new FileOutputStream(destination).getChannel();
        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
        srcChannel.close();
        dstChannel.close();
    }

    static interface FileFilter {
        boolean accept(String name);
    }

    static class FixedFileFilter implements FileFilter {
        private final String name;

        public FixedFileFilter(String name) {
            this.name = name;
        }

        public boolean accept(String name) {
            return this.name.equals(name);
        }
    }

    static class MavenProjectFileFilter implements FileFilter {

        public MavenProjectFileFilter() {
        }

        public boolean accept(String name) {
            return "pom.xml".equals(name);
        }
    }

    static class RuntimeFileFilter implements FileFilter {
        public boolean accept(String name) {
            return name.endsWith("classloader.xml") || name.equals("runtime-config.xml");
        }
    }
    
    static class MyNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("prefix should not be null");
            if (prefix.equals("mvn")) return "http://maven.apache.org/POM/4.0.0";
            return XMLConstants.NULL_NS_URI;
        }

        public String getPrefix(String uri) {
            throw new UnsupportedOperationException("should not be needed for xpath");
        }

        public Iterator getPrefixes(String uri) {
            throw new UnsupportedOperationException("should not be needed for xpath");
        }
        
    }
    
    private XPath xpath() {
        return xpath(null, null, null);
    }
    
    private XPath xpath(String groupId) {
        return xpath(groupId, null, null);
    }

    private XPath xpath(String groupId, String artifactId) {
        return xpath(groupId, artifactId, null);
    }

    private XPath xpath(String groupId, String artifactId, String version) {
        XPath result = XPATH_FACTORY.newXPath();
        result.setNamespaceContext(NAMESPACE_CONTEXT);
        result.setXPathVariableResolver(new ArtifactVariableResolver(groupId, artifactId, version));
        return result;
    }

    private XPath xpath(Artifact artifact) {
        XPath result = xpath();
        result.setXPathVariableResolver(new ArtifactVariableResolver(artifact));
        return result;
    }
}
