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
package org.outerj.daisy.repository.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.DocumentTaskManager;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.TaskState;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Document;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.schema.PartType;
import org.outerj.daisy.repository.schema.RepositorySchema;
import org.outerj.daisy.repository.testsupport.AbstractDaisyTestCase;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument;
import org.outerx.daisy.x10DocumentActions.CaseHandlingAttribute.CaseHandling;
import org.outerx.daisy.x10DocumentActions.ReplaceParametersDocument.ReplaceParameters;
import org.outerx.daisy.x10DocumentActions.SearchParametersDocument.SearchParameters;
import org.outerx.daisy.x10DocumentActions.SimpleActionsParametersDocument.SimpleActionsParameters;

public abstract class AbstractDocumentTaskTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testTaskManager() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        RepositorySchema schema = repository.getRepositorySchema();
        
        PartType partType1 = schema.createPartType("parttype1", "text/xml");
        partType1.setDaisyHtml(true);
        partType1.save();
        
        DocumentType documentType = schema.createDocumentType("doctype1");
        documentType.addPartType(partType1, false);
        documentType.save();

        Document document1 = repository.createDocument("document1", documentType.getId());
        document1.setPart("parttype1", "text/xml", "<html><body><p>The text '<b>find <i>this</i></b>' should be replaced</p></body></html>".getBytes());
        document1.save();

        final Document document2 = repository.createDocument("document2", documentType.getId());
        document2.save();

        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
        DocumentSelection selection = taskManager.createEnumerationDocumentSelection(new VariantKey[] { document1.getVariantKey(), document2.getVariantKey() });

        {
            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test task", "javascript", "throw 'oops';", true));

            waitTillTaskCompletion(taskId, taskManager);

            Task task = taskManager.getTask(taskId);
            assertTrue(TaskState.INTERRUPTED_BY_ERROR == task.getState());
            TaskDocDetail[] taskDocDetails = taskManager.getTaskDocDetails(taskId).getArray();
            assertEquals(2, taskDocDetails.length);
            assertEquals(document1.getVariantKey(), taskDocDetails[0].getVariantKey());
            assertEquals(document2.getVariantKey(), taskDocDetails[1].getVariantKey());
            assertTrue(taskDocDetails[0].getState() == DocumentExecutionState.ERROR);
            assertTrue(taskDocDetails[1].getState() == DocumentExecutionState.WAITING);
        }

        // The same but don't stop at the first error
        {
            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test task", "javascript", "throw 'oops';", false));
            waitTillTaskCompletion(taskId, taskManager);

            // same tests
            Task task = taskManager.getTask(taskId);
            assertTrue(TaskState.FINISHED_WITH_ERRORS == task.getState());
            TaskDocDetail[] taskDocDetails = taskManager.getTaskDocDetails(taskId).getArray();
            assertEquals(2, taskDocDetails.length);
            assertEquals(document1.getVariantKey(), taskDocDetails[0].getVariantKey());
            assertEquals(document2.getVariantKey(), taskDocDetails[1].getVariantKey());
            assertTrue(taskDocDetails[0].getState() == DocumentExecutionState.ERROR);
            assertTrue(taskDocDetails[1].getState() == DocumentExecutionState.ERROR);
            assertNotNull(taskDocDetails[0].getDetails());
            assertNotNull(taskDocDetails[1].getDetails());
        }

        // now run a meaningful task which does something with the documents
        {
            String script = "var doc = repository.getDocument(variantKey, true);\n" +
                    "doc.setCustomField('a', 'b' + doc.getId());\n" +
                    "doc.save()";
            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test task", "javascript", script, false));

            waitTillTaskCompletion(taskId, taskManager);
            Task task = taskManager.getTask(taskId);
            assertTrue(TaskState.FINISHED == task.getState());
            TaskDocDetail[] taskDocDetails = taskManager.getTaskDocDetails(taskId).getArray();
            assertEquals(2, taskDocDetails.length);
            assertTrue(taskDocDetails[0].getState() == DocumentExecutionState.DONE);
            assertTrue(taskDocDetails[1].getState() == DocumentExecutionState.DONE);
            assertNull(taskDocDetails[0].getDetails());
            assertNull(taskDocDetails[1].getDetails());

            Document document1Reloaded = repository.getDocument(document1.getVariantKey(), false);
            assertEquals("b" + document1Reloaded.getId(), document1Reloaded.getCustomField("a"));
            Document document2Reloaded = repository.getDocument(document2.getVariantKey(), false);
            assertEquals("b" + document2Reloaded.getId(), document2Reloaded.getCustomField("a"));
        }

        // check that another users cannot see these tasks
        {
            User newUser = repository.getUserManager().createUser("jan");
            Role userRole = repository.getUserManager().getRole("User", false);
            newUser.addToRole(userRole);
            newUser.setDefaultRole(userRole);
            newUser.setPassword("pwd");
            newUser.save();

            Repository janRepository = repositoryManager.getRepository(new Credentials("jan", "pwd"));
            DocumentTaskManager janTaskManager = (DocumentTaskManager)janRepository.getExtension("DocumentTaskManager");
            assertEquals(0, janTaskManager.getTasks().getArray().length);

            // check that testuser can also see the tasks in non-admin role
            repository.switchRole(userRole.getId());
            Task[] tasks = taskManager.getTasks().getArray();
            assertEquals(3, tasks.length);
            repository.switchRole(Role.ADMINISTRATOR);

            // check unability for jan to delete task
            try {
                janTaskManager.deleteTask(tasks[0].getId());
                fail("User jan should not be able to delete task that does not belong to him.");
            } catch (TaskException e) {}

            // check unability for jan to retrieve task details
            try {
                janTaskManager.getTaskDocDetails(tasks[0].getId());
                fail("User jan should not be able to get task details of a task that does not belong to him.");
            } catch (TaskException e) {}
        }

        // try getting all tasks and deleting tasks
        {
            Task[] tasks = taskManager.getTasks().getArray();
            assertEquals(3, tasks.length);
            for (Task task : tasks) {
                taskManager.deleteTask(task.getId());
            }

            try {
                taskManager.getTask(tasks[0].getId());
                fail("Getting a deleted task should fail.");
            } catch (TaskException e) {}
        }

        // try user interruption of task
        {
            String script = "java.lang.Thread.sleep(3000);";
            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test task", "javascript", script, false));
            taskManager.interruptTask(taskId);
            waitTillTaskCompletion(taskId, taskManager);
            Task task = taskManager.getTask(taskId);
            assertTrue(task.getState() == TaskState.INTERRUPTED_BY_USER);
        }

        // test simple actions
        {
            DocumentCollection collection = repository.getCollectionManager().createCollection("abc");
            collection.save();
            
            SimpleActionsParametersDocument params = SimpleActionsParametersDocument.Factory.newInstance();
            SimpleActionsParameters paramsXml = params.addNewSimpleActionsParameters();
            SimpleActionsParameters.AddToCollection addToCollection = paramsXml.addNewAddToCollection();
            addToCollection.setCollection("abc");
            SimpleActionsParameters.RemoveFromCollection removeFromCollection = paramsXml.addNewRemoveFromCollection();
            removeFromCollection.setCollection("abc");

            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test simple task", "simple", params.toString(), false));
            waitTillTaskCompletion(taskId, taskManager);
            Task task = taskManager.getTask(taskId);
            assertTrue(task.getState() == TaskState.FINISHED);
        }
        
        // test search action
        {
            SearchParametersDocument params = SearchParametersDocument.Factory.newInstance();
            SearchParameters paramsXml = params.addNewSearchParameters();
            paramsXml.setRegexp("find this");

            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test search task", "search", params.toString(), false));
            waitTillTaskCompletion(taskId, taskManager);
            Task task = taskManager.getTask(taskId);
            assertTrue(task.getState() == TaskState.FINISHED);
        }

        // test replace action
        {
            ReplaceParametersDocument params = ReplaceParametersDocument.Factory.newInstance();
            ReplaceParameters paramsXml = params.addNewReplaceParameters();
            paramsXml.setRegexp("find this");
            paramsXml.setReplacement("--replaced--");

            long taskId = taskManager.runTask(selection, new TaskSpecificationImpl("my test replace task", "replace", params.toString(), false));
            waitTillTaskCompletion(taskId, taskManager);
            Task task = taskManager.getTask(taskId);
            assertTrue(task.getState() == TaskState.FINISHED);

            document1 = repository.getDocument(document1.getVariantKey(), false);
            assertTrue(new String(document1.getPart("parttype1").getData()).contains("--replaced--"));
        }
        
        // test failed action
        {
            String script = "throw new Packages.org.outerj.daisy.doctaskrunner.DocTaskFailException();";
            int maxTries = 2;
            TaskSpecificationImpl taskSpec = new TaskSpecificationImpl("my test fail task", "javascript", script, false, maxTries, 1);
            long taskId = taskManager.runTask(selection, taskSpec);
            waitTillTaskCompletion(taskId, taskManager);

            // same tests
            Task task = taskManager.getTask(taskId);
            System.out.println(task.getState());
            assertTrue(TaskState.FINISHED_WITH_FAILURES == task.getState());
            TaskDocDetail[] taskDocDetails = taskManager.getTaskDocDetails(taskId).getArray();
            assertEquals(2, taskDocDetails.length);
            assertEquals(document1.getVariantKey(), taskDocDetails[0].getVariantKey());            
            assertEquals(document2.getVariantKey(), taskDocDetails[1].getVariantKey());            
            assertTrue(taskDocDetails[0].getState() == DocumentExecutionState.FAIL);
            assertTrue(taskDocDetails[1].getState() == DocumentExecutionState.FAIL);
            assertEquals(maxTries, taskDocDetails[0].getTryCount());
            assertEquals(maxTries, taskDocDetails[1].getTryCount());
        }
    }
    
    public void testSearchAndReplaceActions() throws Exception {
//      private Pattern find_this = Pattern.compile("find this");
  //    
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);

        RepositorySchema schema = repository.getRepositorySchema();
        
        PartType partType = schema.createPartType("parttype3", "text/xml");
        partType.setDaisyHtml(true);
        partType.save();
        
        DocumentType documentType = schema.createDocumentType("doctype3");
        documentType.addPartType(partType, false);
        documentType.save();

        Document doc = repository.createDocument("search and replace test", documentType.getId());
        doc.save(false);

        // test that special text in special pre classes is not replaced
        String html= "<html><body><p>This is an include:</p><pre class=\"include\">daisy:123-DSY</pre>" +
                   "<p>This is a simple preformatted section</p><pre>Daisy is a nice product</pre></body></html>";
        String expected = "<html><body><p>This is an include:</p><pre class=\"include\">daisy:123-DSY</pre>" +
                   "<p>This is a simple preformatted section</p><pre>Sunflower is a nice product</pre></body></html>";
        checkReplaceTask(repository, doc.getVariantKey(), "daisy", "sunflower", html, expected);
        
        // test regular expression and sensible case replacement
        html = "<html><body><img daisy-caption=\"alpha Alphaa ALPHAAA AlPhAaaA aLPHAaAaAa\"/></body></html>";
        expected = "<html><body><img daisy-caption=\"alpha Alpha ALPHA AlPha aLPHa\"/></body></html>";
        checkReplaceTask(repository, doc.getVariantKey(), "(alph)a*", "$1a", html, expected);
        
        // another sensible case replacement test
        html = "<html><body>alpha Alpha ALPHA AlPhA aLPHA</body></html>";
        expected = "<html><body>beta Beta BETA bETa bETa</body></html>";
        checkReplaceTask(repository, doc.getVariantKey(), "alpha", "bETa", html, expected);

    }
    
    public void checkReplaceTask(Repository repository, VariantKey key, String regexp, String replacement, String html, String expected) throws Exception {
        Document doc = repository.getDocument(key, true);
        doc.setPart("parttype3", "text/xml", html.getBytes());
        doc.save();
        
        DocumentTaskManager taskManager = (DocumentTaskManager)repository.getExtension("DocumentTaskManager");
        DocumentSelection selection = taskManager.createEnumerationDocumentSelection(new VariantKey[]{doc.getVariantKey()});
        ReplaceParametersDocument replaceParams = ReplaceParametersDocument.Factory.newInstance();
        ReplaceParameters params = replaceParams.addNewReplaceParameters();
        params.setCaseHandling(CaseHandling.SENSIBLE);
        params.setRegexp(regexp);
        params.setReplacement(replacement);
        TaskSpecification spec = new TaskSpecificationImpl("test replace task", "replace", replaceParams.xmlText(), true);
        
        long taskId = taskManager.runTask(selection, spec);
        waitTillTaskCompletion(taskId, taskManager);
        
        doc = repository.getDocument(key, false);
        assertEquals(expected, new String(doc.getPart("parttype3").getData()));
    }

    private void waitTillTaskCompletion(long taskId, DocumentTaskManager taskManager) throws Exception {
        Task task = taskManager.getTask(taskId);
        while (!task.getState().isStoppedState()) {
            System.out.println("Waiting for task to finish...");
            Thread.sleep(500);
            task = taskManager.getTask(taskId);
        }
    }
}
