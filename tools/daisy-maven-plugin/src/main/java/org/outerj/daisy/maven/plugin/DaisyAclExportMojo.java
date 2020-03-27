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
package org.outerj.daisy.maven.plugin;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WfPoolManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 *
 * @author Jan Hoskens
 * @goal export-acl
 * @description Export the daisy ACL.
 */
public class DaisyAclExportMojo extends AbstractDaisyMojo {

    /**
     * Destination file for ACL.
     *
     * @parameter expression="${aclFile}"
     * @required
     */
    private File aclFile;

    /**
     * Destination file for workflow pools.
     *
     * @parameter expression="${wfPoolFile}"
     * @required
     */
    private File wfPoolFile;

    /**
     * Overwrite any existing files.
     *
     * @parameter expression="${force}"
     */
    private boolean force;

    /**
     * Export ACL to file.
     *
     * @parameter expression="${exportAcl}" default-value="true"
     */
    private boolean exportAcl;

    /**
     * Export workflowpools to file.
     *
     * @parameter expression="${exportWfPools}" default-value="true"
     */
    private boolean exportWfPools;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            if (exportAcl) {
                if (aclFile.exists() && force)
                    aclFile.delete();
                if (aclFile.exists())
                    throw new MojoExecutionException("Cannot export ACL, file already exists: "
                            + aclFile.getAbsolutePath());
                exportAcl();
            }

            if (exportWfPools) {
                if (wfPoolFile.exists() && force)
                    wfPoolFile.delete();
                if (wfPoolFile.exists())
                    throw new MojoExecutionException("Cannot export workflow pools, file already exists: "
                            + wfPoolFile.getAbsolutePath());

                exportWfPools();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Could not export ACL.", e);
        }
    }

    private void exportAcl() throws Exception {
        Document exportDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = exportDocument.createElement("aclExport");
        exportDocument.appendChild(root);
        Element users = exportDocument.createElement("users");
        root.appendChild(users);
        Element roles = exportDocument.createElement("roles");
        root.appendChild(roles);
        Element acl = exportDocument.createElement("acl");
        root.appendChild(acl);
        final Element aclDOMDocument = (Element) exportDocument.importNode(((Document) getAccessManager()
                .getLiveAcl().getXml().newDomNode()).getDocumentElement(), true);
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        NamespaceContext namespaceContext = new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if ("acl".equals(prefix))
                    return aclDOMDocument.getNamespaceURI();
                return null;
            }

            public String getPrefix(String namespaceURI) {
                return null;
            }

            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }
        };
        xpath.setNamespaceContext(namespaceContext);
        XPathExpression expr = xpath.compile("//acl:aclEntry[@subjectType = 'role']");
        Object result = expr.evaluate(aclDOMDocument, XPathConstants.NODESET);
        NodeList roleNodes = (NodeList) result;
        Map<Long, Role> daisyRoles = new HashMap<Long, Role>();
        UserManager userManager = getUserManager();
        for (int i = 0; i < roleNodes.getLength(); i++) {
            Element roleNode = (Element) roleNodes.item(i);
            Long roleId = Long.parseLong(roleNode.getAttribute("subjectValue"));
            Role daisyRole = daisyRoles.get(roleId);
            if (daisyRole == null) {
                daisyRole = userManager.getRole(roleId, false);
                daisyRoles.put(roleId, daisyRole);
                roles.appendChild(exportDocument.importNode(daisyRole.getXml().newDomNode().getFirstChild(),
                        true));
            }
            roleNode.setAttribute("subjectValue", daisyRole.getName());
        }

        expr = xpath.compile("//acl:aclEntry[@subjectType = 'user']");
        result = expr.evaluate(aclDOMDocument, XPathConstants.NODESET);
        roleNodes = (NodeList) result;
        Map<Long, User> daisyUsers = new HashMap<Long, User>();
        for (int i = 0; i < roleNodes.getLength(); i++) {
            Element userNode = (Element) roleNodes.item(i);
            Long roleId = Long.parseLong(userNode.getAttribute("subjectValue"));
            User daisyUser = daisyUsers.get(roleId);
            if (daisyUser == null) {
                daisyUser = userManager.getUser(roleId, false);
                daisyUsers.put(roleId, daisyUser);
                roles.appendChild(exportDocument.importNode(daisyUser.getXml().newDomNode().getFirstChild(),
                        true));
            }
            userNode.setAttribute("subjectValue", daisyUser.getLogin());
        }

        acl.appendChild(aclDOMDocument);
        saveDOMDocument(exportDocument, aclFile);
    }

    private void exportWfPools() throws Exception {
        WfPoolManager poolManager = getWfPoolManager();
        List<WfPool> pools = poolManager.getPools();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        Document poolDocument = documentBuilderFactory.newDocumentBuilder().newDocument();
        Element poolsElement = poolDocument.createElement("pools");
        poolDocument.appendChild(poolsElement);
        for (WfPool wfPool : pools) {
            poolsElement.appendChild(poolDocument.importNode(wfPool.getXml().getDomNode().getFirstChild(),
                    true));
        }
        saveDOMDocument(poolDocument, wfPoolFile);
    }

}