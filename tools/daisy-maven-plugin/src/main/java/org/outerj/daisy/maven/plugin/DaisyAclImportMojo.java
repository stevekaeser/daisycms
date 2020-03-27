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
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfPoolNotFoundException;
import org.outerx.daisy.x10.AclDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Import an ACL and/or workflow pools in Daisy.
 *
 * @author Jan Hoskens
 * @goal import-acl
 * @description Export the daisy ACL.
 */
public class DaisyAclImportMojo extends AbstractDaisyMojo {

    /**
     * The ACL file to import.
     *
     * @parameter expression="${aclFile}" default-value="${project.basedir}/src/main/dsy-exp/acl.xml"
     * @required
     */
    private File aclFile;

    /**
     * The workflow pools file.
     *
     * @parameter expression="${wfPoolFile}"
     * @required
     */
    private File wfPoolFile;

    /**
     * If <code>true</code>(default) import the ACL.
     *
     * @parameter expression="${importAcl}" default-value="true"
     */
    private boolean importAcl;

    /**
     * If <code>true</code>(default) import the workflow pools.
     *
     * @parameter expression="${importWfPools}" default-value="true"
     */
    private boolean importWfPools;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        Document aclExport;
        try {
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            if (importAcl) {
                aclExport = documentBuilder.parse(aclFile);
                Acl targetAcl = getAccessManager().getStagingAcl();
                doImport(aclExport, targetAcl);
                getAccessManager().copyStagingToLive();
                getLog().info("Imported ACL found in: " + aclFile.getAbsolutePath());
            }

            if (importWfPools) {
                aclExport = documentBuilder.parse(wfPoolFile);
                importWfPools(aclExport);
                getLog().info("Imported workflow pools found in: " + wfPoolFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not import ACL.", e);
        }
    }

    private void doImport(Document aclExport, Acl targetAcl) throws Exception {
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        NamespaceContext namespaceContext = new NamespaceContext() {

            public String getNamespaceURI(String prefix) {
                if ("acl".equals(prefix))
                    return "http://outerx.org/daisy/1.0";
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

        XPathExpression expr = xpath.compile("/aclExport/roles/acl:role");
        NodeList roles = (NodeList) expr.evaluate(aclExport, XPathConstants.NODESET);
        Map<String, Role> daisyRoles = new HashMap<String, Role>();
        for (int i = 0; i < roles.getLength(); ++i) {
            Role role = createRole((Element) roles.item(i));
            daisyRoles.put(role.getName(), role);
        }

        expr = xpath.compile("/aclExport/users/acl:user");
        NodeList users = (NodeList) expr.evaluate(aclExport, XPathConstants.NODESET);
        Map<String, User> daisyUsers = new HashMap<String, User>();
        for (int i = 0; i < users.getLength(); ++i) {
            User user = createUser((Element) users.item(i));
            daisyUsers.put(user.getLogin(), user);
        }

        expr = xpath.compile("//acl:acl");
        Element acl = (Element) expr.evaluate(aclExport, XPathConstants.NODE);
        expr = xpath.compile("//acl:aclEntry");
        NodeList result = (NodeList) expr.evaluate(acl, XPathConstants.NODESET);
        for (int i = 0; i < result.getLength(); ++i) {
            Element aclEntry = (Element) result.item(i);
            String subjectType = aclEntry.getAttribute("subjectType");
            String subjectValue = aclEntry.getAttribute("subjectValue");
            if ("user".equals(subjectType)) {
                User daisyUser = daisyUsers.get(subjectValue);
                if (daisyUser == null)
                    throw new MojoExecutionException("Cannot find user: " + subjectValue);
                aclEntry.setAttribute("subjectValue", String.valueOf(daisyUser.getId()));
            } else if ("role".equals(subjectType)) {
                Role daisyRole = daisyRoles.get(subjectValue);
                if (daisyRole == null)
                    throw new MojoExecutionException("Cannot find role: " + subjectValue);
                aclEntry.setAttribute("subjectValue", String.valueOf(daisyRole.getId()));
            }
        }

        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Node importedAcl = document.importNode(acl, true);
        document.appendChild(importedAcl);

        targetAcl.setFromXml(AclDocument.Factory.parse(document).getAcl());
        targetAcl.save();
    }

    private Role createRole(Element roleXml) throws Exception {
        if (roleXml != null) {
            UserManager userManager = getUserManager();
            Role role = null;
            try {
                role = userManager.getRole(roleXml.getAttribute("name"), false);
            } catch (Exception e) {
            }

            if (role == null) {
                role = userManager.createRole(roleXml.getAttribute("name"));
                role.setDescription(roleXml.getAttribute("description"));
                role.save();
            }

            return role;
        }
        return null;
    }

    private User createUser(Element userXml) throws Exception {
        if (userXml != null) {
            UserManager userManager = getUserManager();
            User user = null;
            try {
                user = userManager.getUser(userXml.getAttribute("login"), false);
            } catch (Exception e) {
            }

            if (user == null) {
                user = userManager.createUser(userXml.getAttribute("login"));
                user.setAuthenticationScheme(userXml.getAttribute("authenticationScheme"));
                user.setConfirmed(userXml.getAttribute("confirmed") == "true");
                user.setConfirmKey(userXml.getAttribute("confirmKey"));
                user.setDefaultRole(createRole((Element) userXml.getElementsByTagName("role").item(0)));
                user.setEmail(userXml.getAttribute("email"));
                user.setFirstName(userXml.getAttribute("firstName"));
                user.setLastName(userXml.getAttribute("lastName"));
                user.setUpdateableByUser(userXml.getAttribute("updateableByUser") == "true");
                user.setPassword(userXml.getAttribute("login"));

                NodeList roles = userXml.getElementsByTagName("roles");
                for (int i = 0; i < roles.getLength(); ++i) {
                    user.addToRole(createRole((Element) roles.item(i)));
                }

                user.save();
            }

            return user;
        }
        return null;
    }

    private void importWfPools(Document wfPoolsDocument) throws Exception {
        waitForWorkflow();
        NodeList poolNodes = wfPoolsDocument.getElementsByTagNameNS("http://outerx.org/daisy/1.0#workflow",
                "pool");
        WfPoolManager wfPoolManager = getWfPoolManager();
        for (int i = 0; i < poolNodes.getLength(); ++i) {
            Element pool = (Element) poolNodes.item(i);
            String poolName = pool.getAttribute("name");
            try {
                wfPoolManager.getPoolByName(poolName);
            } catch (WfPoolNotFoundException e) {
                WfPool wfPool = wfPoolManager.createPool(poolName);
                wfPool.setDescription(pool.getAttribute("description"));
                wfPool.save();
            }
        }
    }
}