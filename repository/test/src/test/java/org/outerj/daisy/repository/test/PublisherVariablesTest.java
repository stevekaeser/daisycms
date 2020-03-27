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
package org.outerj.daisy.repository.test;

import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.testsupport.DOMBuilder;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.publisher.Publisher;
import org.outerj.daisy.util.Constants;
import org.outerx.daisy.x10Publisher.*;
import org.jaxen.dom.DOMXPath;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.io.UnsupportedEncodingException;

public class PublisherVariablesTest extends AbstractDaisyTestCase {

    protected boolean resetDataStores() {
        return true;
    }

    public void testVariables() throws Exception {
        RepositoryManager repoManager = getLocalRepositoryManager();

        Repository repository = repoManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        Publisher publisher = (Publisher)repository.getExtension("Publisher");

        // Create a second user
        Role userRole = repository.getUserManager().getRole("User", false);
        User user = repository.getUserManager().createUser("user1");
        user.addToRole(userRole);
        user.setPassword("user1");
        user.save();

        Repository userRepository = repoManager.getRepository(new Credentials("user1", "user1"));
        Publisher userPublisher = (Publisher)userRepository.getExtension("Publisher");


        // Create a 'restricted' collection and deny access to documents in that collection for users
        DocumentCollection restrictedColl = repository.getCollectionManager().createCollection("restricted");
        restrictedColl.save();

        Acl acl = repository.getAccessManager().getStagingAcl();

        AclObject aclObject = acl.createNewObject("true");
        AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.setAll(AclActionType.GRANT);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        aclObject = acl.createNewObject("InCollection('restricted')");
        aclEntry = aclObject.createNewEntry(AclSubjectType.ROLE, userRole.getId());
        aclEntry.set(AclPermission.READ, AclActionType.DENY);
        aclObject.add(aclEntry);
        acl.add(aclObject);

        acl.save();
        repository.getAccessManager().copyStagingToLive();

        // Create variables schema
        RepositorySchema schema = repository.getRepositorySchema();

        PartType variablesDataType = schema.createPartType("VariablesData", "text/xml");
        variablesDataType.save();
        
        DocumentType variablesType = schema.createDocumentType("Variables");
        variablesType.addPartType(variablesDataType, true);
        variablesType.save();

        // create variable documents
        Document variablesDocument1 = repository.createDocument("Variables 1", "Variables");
        Map<String, String> variables = new HashMap<String, String>();
        variables.put("var1", "%%%VAR1%%%");
        variables.put("var2", "%%%VAR2%%%");
        variablesDocument1.setPart("VariablesData", "text/xml", getVariablesData(variables));
        variablesDocument1.save();

        variables.put("var1", "%%%VAR1updated%%%");
        variablesDocument1.setPart("VariablesData", "text/xml", getVariablesData(variables));
        variablesDocument1.setNewVersionState(VersionState.DRAFT);
        variablesDocument1.save();

        Document variablesDocument2 = repository.createDocument("Variables 2", "Variables");
        variables.clear();
        variables.put("var2", "%%%VAR2alt%%%");
        variablesDocument2.setPart("VariablesData", "text/xml", getVariablesData(variables));
        variablesDocument2.save();

        Document variablesDocument3 = repository.createDocument("Variables 3", "Variables");
        variables.clear();
        variables.put("var5", "%%%VAR5%%%");
        variablesDocument3.setPart("VariablesData", "text/xml", getVariablesData(variables));
        variablesDocument3.addToCollection(restrictedColl);
        variablesDocument3.save();

        Document[] varDocs = new Document[] {variablesDocument1, variablesDocument2, variablesDocument3};

        // create a document type for daisy-html docs
        PartType textDataType = schema.createPartType("TextData", "text/xml");
        textDataType.setDaisyHtml(true);
        textDataType.save();

        DocumentType textType = schema.createDocumentType("Text");
        textType.addPartType(textDataType, true);
        textType.save();

        // create documents referencing the variables
        Document textDocument = repository.createDocument("Text1 ${var1}", "Text");
        textDocument.setPart("TextData", "text/xml", "<html><body><p><span class='variable'>var1</span> <span class='variable'>var2</span> <span class='variable'>var5</span> </p></body></html>".getBytes("UTF-8"));
        textDocument.save();

        DOMXPath xpath;
        org.w3c.dom.Document document;
        DOMBuilder domBuilder;

        // Basic var substitution test
        {
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            DocumentDocument.Document pubDoc = pubReqDoc.getPublisherRequest().addNewDocument();
            pubDoc.setId(textDocument.getId());
            pubDoc.addNewPreparedDocuments();

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//text()[contains(., '%%%VAR1%%%')]");
            List list = xpath.selectNodes(document);
            assertEquals(1, list.size());

            xpath = createXPath("//text()[contains(., '%%%VAR2%%%')]");
            list = xpath.selectNodes(document);
            assertEquals(1, list.size());

            xpath = createXPath("//text()[contains(., '%%%VAR5%%%')]");
            list = xpath.selectNodes(document);
            assertEquals(1, list.size());

            xpath = createXPath("//text()[contains(., 'something else')]");
            list = xpath.selectNodes(document);
            assertEquals(0, list.size());

            xpath = createXPath("//d:document[contains(@name, '%%%VAR1%%%')]");
            list = xpath.selectNodes(document);
            assertEquals(1, list.size());
        }

        // Test switching to last version mode
        {
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            pubReqDoc.getPublisherRequest().setVersionMode("last");
            DocumentDocument.Document pubDoc = pubReqDoc.getPublisherRequest().addNewDocument();
            pubDoc.setId(textDocument.getId());
            pubDoc.addNewPreparedDocuments();

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//text()[contains(., '%%%VAR1updated%%%')]");
            List list = xpath.selectNodes(document);
            assertEquals(1, list.size());
        }

        // Test user has no read access to var doc
        {
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            DocumentDocument.Document pubDoc = pubReqDoc.getPublisherRequest().addNewDocument();
            pubDoc.setId(textDocument.getId());
            pubDoc.addNewPreparedDocuments();

            domBuilder = new DOMBuilder();
            userPublisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//text()[contains(., '%%%VAR5%%%')]");
            List list = xpath.selectNodes(document);
            assertEquals(0, list.size());

            xpath = createXPath("//span[@class='daisy-unresolved-variable' and contains(., 'var5')]");
            list = xpath.selectNodes(document);
            assertEquals(1, list.size());
        }

        // Test substitution in query results
        {
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            PerformQueryDocument.PerformQuery performQuery = pubReqDoc.getPublisherRequest().addNewPerformQuery();
            performQuery.setQuery("select name where name like '%${var1}%'");

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//d:value[contains(., '%%%VAR1%%%')]");
            List list = xpath.selectNodes(document);
            assertEquals(1, list.size());

            // disable variables config
            pubReqDoc.getPublisherRequest().unsetVariablesConfig();

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//d:value[contains(., '%%%VAR1%%%')]");
            list = xpath.selectNodes(document);
            assertEquals(0, list.size());
        }

        // Test cache invalidation, resolveVariables
        {
            // define an extra variable in doc 3, if cache is properly invalidated it will be accessible
            variables.clear();
            variables.put("var5", "%%%VAR5%%%");
            variables.put("var6", "%%%VAR6%%%");
            variablesDocument3.setPart("VariablesData", "text/xml", getVariablesData(variables));
            variablesDocument3.save();

            // Test resolveVariables request
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            ResolveVariablesDocument.ResolveVariables resolveVariables = pubReqDoc.getPublisherRequest().addNewResolveVariables();
            resolveVariables.addText("${var6}");

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//p:text[contains(., '%%%VAR6%%%')]");
            List list = xpath.selectNodes(document);
            assertEquals(1, list.size());
        }

        // Test variablesList
        {
            PublisherRequestDocument pubReqDoc = getBasePubReq(varDocs);
            pubReqDoc.getPublisherRequest().addNewVariablesList();

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//p:variablesList/p:variable");
            List list = xpath.selectNodes(document);
            assertEquals(4, list.size());

            // user will see less variables
            domBuilder = new DOMBuilder();
            userPublisher.processRequest(pubReqDoc, domBuilder);
            document = domBuilder.getDocument();

            xpath = createXPath("//p:variablesList/p:variable");
            list = xpath.selectNodes(document);
            assertEquals(2, list.size());
        }

        // Add an invalid variables document, should be skipped
        {
            Document variablesDocument4 = repository.createDocument("Variables 4", "Variables");
            variablesDocument4.setPart("VariablesData", "text/xml", "foo".getBytes("UTF-8")); // invalid XML
            variablesDocument4.save();

            Document[] newVarDocs = new Document[] {variablesDocument1, variablesDocument2, variablesDocument3, variablesDocument4};

            PublisherRequestDocument pubReqDoc = getBasePubReq(newVarDocs);
            pubReqDoc.getPublisherRequest().addNewVariablesList();

            domBuilder = new DOMBuilder();
            publisher.processRequest(pubReqDoc, domBuilder);
        }
    }

    private byte[] getVariablesData(Map<String, String> variables) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        builder.append("<v:variables xmlns:v='").append(Constants.VARIABLES_NS).append("'>");
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            builder.append("<v:variable name='").append(entry.getKey()).append("'>").append(entry.getValue()).append("</v:variable>");
        }
        builder.append("</v:variables>");
        return builder.toString().getBytes("UTF-8");
    }

    private PublisherRequestDocument getBasePubReq(Document[] variableDocs) {
        PublisherRequestDocument pubReqDoc = PublisherRequestDocument.Factory.newInstance();
        PublisherRequestDocument.PublisherRequest pubReq = pubReqDoc.addNewPublisherRequest();
        VariablesConfigType variablesConfig = pubReq.addNewVariablesConfig();
        VariablesConfigType.VariableSources varSources = variablesConfig.addNewVariableSources();

        for (Document doc : variableDocs) {
            VariantKeyType varDoc = varSources.addNewVariableDocument();
            varDoc.setId(doc.getId());
            varDoc.setBranch(String.valueOf(doc.getBranchId()));
            varDoc.setLanguage(String.valueOf(doc.getLanguageId()));
        }

        return pubReqDoc;
    }

    private DOMXPath createXPath(String expr) throws Exception {
        DOMXPath xpath = new DOMXPath(expr);
        xpath.addNamespace("n", Constants.NAVIGATION_NS);
        xpath.addNamespace("d", Constants.DAISY_NAMESPACE);
        xpath.addNamespace("p", Constants.PUBLISHER_NS);
        return xpath;
    }
}
