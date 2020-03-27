/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.install;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * @author paul
 * 
 */
public class XmlConfUpdater {
    private File sourceFile;

    private File targetFile;

    private Document sourceDoc;

    private Document targetDoc;

    private List<UpdateAction> xPaths;

    private String LINE_FORMAT = "^((\\[(.*?):(.*?)\\])\\s)?(.*?)(\\s(<<[>!=]{2})\\s?(.*))?$";

    private boolean showDebug;

    /**
     * @param args
     *            Accepted command line arguments '-t' '--target' '-s'
     *            '--source' '-x' '--xpaths' -h' '--help'
     */
    public static void main(String[] args) {
        Options options = new Options();
        Option sourceFileOption = new Option("s", "source", true, "The source file that needs to be updated");
        Option targetFileOption = new Option("t", "target", true, "The target file that will serve as target");
        Option xPaths = new Option("x", "xpaths", true, "XPaths specifying which paths must be transfered from source to target");

        sourceFileOption.setRequired(true);
        sourceFileOption.setArgName("source-file");
        targetFileOption.setRequired(true);
        targetFileOption.setArgName("target-file");
        xPaths.setRequired(true);
        xPaths.setArgName("xpaths-file");

        options.addOption(sourceFileOption);
        options.addOption(targetFileOption);
        options.addOption(xPaths);
        options.addOption("d", "debug", false, "Print debugging messages");
        options.addOption("h", "help", false, "Show this message");

        CommandLineParser parser = new PosixParser();

        try {
            CommandLine cmd = parser.parse(options, args);
            try {
                XmlConfUpdater updater = new XmlConfUpdater(cmd.getOptionValue("s"), cmd.getOptionValue("t"), cmd.getOptionValue("x"), cmd.hasOption("d"));
                updater.update();
                System.out.println("Updated " + cmd.getOptionValue("t"));
            } catch (Exception e) {
                System.out.println("Could not update file " + cmd.getOptionValue("t"));
                e.printStackTrace();
                System.exit(1);
            }
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("XConfUpdater", options, true);
        }
    }

    /**
     * Constucts an Updater object
     * 
     * @param sourceFileName
     *            Source xml file from which items will be copied to the target
     *            xml file
     * @param targetFileName
     *            Target xml file receiving items from the source file
     * @param xpathFile
     *            File specifying XPaths that will be copied from source to
     *            target. Each entry is newline delimited.
     * @throws Exception
     */
    public XmlConfUpdater(String sourceFileName, String targetFileName, String xpathFile, boolean showDebug) throws Exception {
        this.showDebug = showDebug;
        sourceFile = new File(sourceFileName);
        targetFile = new File(targetFileName);
        sourceDoc = InstallHelper.parseFile(sourceFile);
        targetDoc = InstallHelper.parseFile(targetFile);
        xPaths = getXPaths(xpathFile);

    }

    /**
     * Updates the target xml file with the source xml file. The items specified
     * in the xPathFile will be copied from source to target. Existing tags in
     * the target xml file will be overwritten.
     * 
     * @throws Exception
     */
    public void update() throws Exception {        
        for (UpdateAction action : xPaths) {
            try {
                int status = action.execute();
                if (showDebug) {
                    if ( status == UpdateAction.ACTION_SUCCEEDED) {
                        System.out.println("Xpath OK : " + action.getXPath().getRootExpr().getText());
                    } else {
                        System.out.println("skipping xpath " + action.getXPath().getRootExpr().getText());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("Failed xpath" + action.getXPath().getRootExpr().getText());
            }
        }
        InstallHelper.saveDocument(targetFile, targetDoc);
    }

    private List<UpdateAction> getXPaths(String fileName) throws Exception {
        List<UpdateAction> pathsInserts = new ArrayList<UpdateAction>();
        BufferedReader br;
        if (fileName.equals("-"))
            br = new BufferedReader(new InputStreamReader(System.in));
        else
            br = new BufferedReader(new FileReader(fileName));

        try {
            String line;
            Pattern linePattern = Pattern.compile(LINE_FORMAT);
            while ((line = br.readLine()) != null) {
                Matcher lineMatcher = linePattern.matcher(line);
                lineMatcher.find();
                String nsPrefix = lineMatcher.group(3);
                String ns = lineMatcher.group(4);
                DOMXPath xpath = new DOMXPath(lineMatcher.group(5));
                String commandString = lineMatcher.group(7);
                String tag = lineMatcher.group(8);

                if (ns != null && nsPrefix != null)
                    xpath.addNamespace(nsPrefix, ns);

                UpdateAction xPathAction = UpdateAction.createAction(sourceDoc, targetDoc, xpath, tag, commandString);

                pathsInserts.add(xPathAction);
            }

        } finally {
            br.close();
        }
        return pathsInserts;
    }

    private static abstract class UpdateAction {

        private static final String INSERT_SEPARATOR = "<<==";

        private static final String REPLACE_SEPARATOR = "<<>>";

        private static final String REMOVE_SEPARATOR = "<<!!";

        private static final String XPATH_REFERENCE_REGEX = "(?<!\\$)\\{.*?\\}";

        public static final int ACTION_SUCCEEDED = 0;

        public static final int ACTION_FAILED = 1;

        protected Document tagDoc;

        protected Document targetDoc;

        protected Document sourceDoc;

        protected DOMXPath xPath;        
        
        private boolean showDebug = false;

        private UpdateAction(Document sourceDoc, Document targetDoc, DOMXPath xPath, String tag) throws Exception {
            this.sourceDoc = sourceDoc;
            this.targetDoc = targetDoc;
            this.xPath = xPath;
            // Change the tag into a dom node
            if (tag != null) {
                DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                InputStream is = new ByteArrayInputStream(tag.getBytes());
                tagDoc = documentBuilder.parse(is);
                // replace { } thingies
                translateXpathReferences(tagDoc.getDocumentElement(), targetDoc.getDocumentElement());
            }
        }

        public static UpdateAction createAction(Document sourceDoc, Document targetDoc, DOMXPath xpath, String tag, String command) throws Exception {
            UpdateAction action = null;
            if (command == null)
                action = new CopyAction(sourceDoc, targetDoc, xpath);
            else if (command.equals(INSERT_SEPARATOR))
                action = new InsertAction(sourceDoc, targetDoc, xpath, tag);
            else if (command.equals(REPLACE_SEPARATOR))
                action = new ReplaceAction(sourceDoc, targetDoc, xpath, tag);
            else if (command.equals(REMOVE_SEPARATOR))
                action = new RemoveAction(sourceDoc, targetDoc, xpath);

            return action;
        }

        public abstract int execute() throws Exception;

        private void translateXpathReferences(Node newNode, Node rootNode) throws Exception {
            if (newNode.getChildNodes().getLength() > 0) {
                for (int i = 0; i < newNode.getChildNodes().getLength(); i++)
                    translateXpathReferences(newNode.getChildNodes().item(i), rootNode);
            }
            Pattern pattern = Pattern.compile(XPATH_REFERENCE_REGEX);
            if ((newNode.getNodeType() == Node.TEXT_NODE || newNode.getNodeType() == Node.ATTRIBUTE_NODE) && newNode.getNodeValue() != null) {

                String nodeValue = newNode.getNodeValue();
                Matcher matcher = pattern.matcher(nodeValue);
                while (matcher.find()) {
                    String xpath = matcher.group();
                    xpath = xpath.substring(1, xpath.length() - 1);
                    Node matchedNode = (Node) new DOMXPath(xpath).selectSingleNode(rootNode);
                    if (matchedNode != null) {
                        if ( showDebug) 
                            System.out.println("Found node to be replaced at : " + xpath + " value : " + matchedNode.getNodeValue());
                        // substitute element or attribute text
                        if (matchedNode.getNodeType() == Node.TEXT_NODE || matchedNode.getNodeType() == Node.ATTRIBUTE_NODE) {
                            nodeValue = matcher.replaceFirst(matchedNode.getNodeValue());
                            // append element
                        } else if (matchedNode.getNodeType() == Node.ELEMENT_NODE) {
                            newNode.getParentNode().appendChild(newNode.getOwnerDocument().importNode(matchedNode, true));
                            nodeValue = matcher.replaceFirst("");

                        } else {
                            System.out.println("I don't know what to do with that type of node :" + matchedNode.getNodeType());
                            nodeValue = matcher.replaceFirst("");
                        }

                    } else {
                        throw new IOException("This XPath could not be found " + xpath);
                    }
                    matcher = pattern.matcher(nodeValue);
                }
                newNode.setNodeValue(nodeValue);

            } else if (newNode.getNodeType() == Node.ELEMENT_NODE) {
                NamedNodeMap atts = newNode.getAttributes();
                for (int i = 0; i < atts.getLength(); i++)
                    translateXpathReferences(atts.item(i), rootNode);
            }
        }

        public DOMXPath getXPath() {
            return xPath;
        }

        public void setXPath(DOMXPath path) {
            xPath = path;
        }
        
        public boolean isShowDebug() {
            return showDebug;
        }

        public void setShowDebug(boolean showDebug) {
            this.showDebug = showDebug;
        }
    }

    private static class InsertAction extends UpdateAction {
        public InsertAction(Document sourceDoc, Document targetDoc, DOMXPath xPath, String tag) throws Exception {
            super(sourceDoc, targetDoc, xPath, tag);
        }

        public int execute() throws Exception {
            int status = ACTION_SUCCEEDED;

            Node targetNode = (Node) xPath.selectSingleNode(targetDoc);
            if (targetNode != null)
                targetNode.appendChild(targetDoc.importNode(tagDoc.getDocumentElement(), true));
            else
                status = ACTION_FAILED;

            return status;
        }
    }

    private static class ReplaceAction extends UpdateAction {
        public ReplaceAction(Document sourceDoc, Document targetDoc, DOMXPath xPath, String tag) throws Exception {
            super(sourceDoc, targetDoc, xPath, tag);
        }

        public int execute() throws Exception {
            int status = ACTION_SUCCEEDED;

            Node targetNode = (Node) xPath.selectSingleNode(targetDoc);
            Node newNode = targetDoc.importNode(tagDoc.getDocumentElement(), true);
            if (targetNode != null)
                targetNode.getParentNode().replaceChild(newNode, targetNode);
            else
                status = ACTION_FAILED;

            return status;
        }
    }

    private static class RemoveAction extends UpdateAction {
        public RemoveAction(Document sourceDoc, Document targetDoc, DOMXPath xPath) throws Exception {
            super(sourceDoc, targetDoc, xPath, null);
        }

        public int execute() throws Exception {
            int status = ACTION_SUCCEEDED;
            Node targetNode = (Node) xPath.selectSingleNode(targetDoc);
            if (targetNode != null)
                targetNode.getParentNode().removeChild(targetNode);
            else
                status = ACTION_FAILED;

            return status;
        }
    }

    private static class CopyAction extends UpdateAction {
        public CopyAction(Document sourceDoc, Document targetDoc, DOMXPath xPath) throws Exception {
            super(sourceDoc, targetDoc, xPath, null);
        }

        public int execute() throws Exception {
            int status = ACTION_SUCCEEDED;
            Element sourceElement = (Element) xPath.selectSingleNode(sourceDoc);
            Element targetElement = (Element) xPath.selectSingleNode(targetDoc);
            if (sourceElement != null) {
                if (targetElement != null) {
                    // target element exists and will be replaced
                    targetElement.getParentNode().replaceChild(targetDoc.importNode(sourceElement, true), targetElement);
                } else {
                    // target element does not exist so we'll climb the parental
                    // ladder to find the first existing item
                    do {
                        sourceElement = (Element) xPath.selectSingleNode(sourceDoc);
                        xPath = getParentXpath(xPath);
                        targetElement = (Element) xPath.selectSingleNode(targetDoc);
                    } while (targetElement == null);

                    targetElement.appendChild(targetDoc.importNode(sourceElement, true));
                }
            }
            return status;
        }

        private DOMXPath getParentXpath(DOMXPath path) throws JaxenException {
            String rootText = path.getRootExpr().getText();
            return new DOMXPath(rootText.substring(0, rootText.lastIndexOf("/")));
        }
    }
}
