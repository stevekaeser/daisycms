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
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.query.SortOrder;
import org.outerj.daisy.repository.acl.*;
import org.outerj.daisy.repository.schema.DocumentType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.workflow.*;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.apache.xmlbeans.XmlString;

import java.io.*;
import java.util.*;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

public abstract class AbstractWorkflowTest extends AbstractDaisyTestCase {
    protected boolean resetDataStores() {
        return true;
    }

    protected Set<String> getDisabledContainerIds() {
        return Collections.emptySet();
    }

    protected abstract RepositoryManager getRepositoryManager() throws Exception;

    public void testWorkflow() throws Exception {
        RepositoryManager repositoryManager = getRepositoryManager();
        Repository repository = repositoryManager.getRepository(new Credentials("testuser", "testuser"));
        repository.switchRole(Role.ADMINISTRATOR);
        Role userRole = repository.getUserManager().getRole("User", false);

        WorkflowManager workflowManager = (WorkflowManager)repository.getExtension("WorkflowManager");
        Locale locale = Locale.US;

        // Create a dummy user (a normal user)
        User dummyUser = repository.getUserManager().createUser("dummy");
        dummyUser.setPassword("dummy");
        dummyUser.addToRole(repository.getUserManager().getRole("User", false));
        dummyUser.save();
        Repository dummyRepo = repositoryManager.getRepository(new Credentials("dummy", "dummy"));
        WorkflowManager dummyWfManager = (WorkflowManager)dummyRepo.getExtension("WorkflowManager");

        // Some basic deployment tests
        {
            String workflowWithoutStartState = "<process-definition name='boe'>" +
                    "  <state name='s'>" +
                    "    <transition to='end' />" +
                    "  </state>" +
                    "  <end-state name='end' />" +
                    "</process-definition>";

            try {
                workflowManager.deployProcessDefinition(new ByteArrayInputStream(workflowWithoutStartState.getBytes()), "text/xml", locale);
                fail("Deploying a process definition without a start state should fail.");
            } catch (RepositoryException e) {
                // expected
                assertTrue(e.getMessage().indexOf("start state") != -1);
            }

            String validWorkflow = "<process-definition name='boe'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='to-s' to='s' />" +
                    "    <transition name='to-x' to='x' />" +
                    "  </start-state>" +
                    "  <state name='s'>" +
                    "    <transition name='to-end' to='end' />" +
                    "  </state>" +
                    "  <end-state name='end' />" +
                    "</process-definition>";

            WfProcessDefinition workflowDefinition = workflowManager.deployProcessDefinition(new ByteArrayInputStream(validWorkflow.getBytes()), "text/xml", locale);
            assertTrue("Invalid transition should give a warning (among other things)", workflowDefinition.getProblems().size() > 1);

            // Non-admin user cannot deploy workflow definitions
            repository.switchRole(userRole.getId());
            try {
                workflowManager.deployProcessDefinition(new ByteArrayInputStream(validWorkflow.getBytes()), "text/xml", locale);
                fail("Non-admin user should not be able to deploy workflow definitions.");
            } catch (WfAuthorizationException e) {
                // expected
            }
            repository.switchRole(Role.ADMINISTRATOR);

            // Save it again to have a second version
            workflowManager.deployProcessDefinition(new ByteArrayInputStream(validWorkflow.getBytes()), "text/xml", locale);

            // Save another workflow (with a different name)
            String anotherWorkflow = "<process-definition name='boe2'>" +
                    "  <start-state name='start'>" +
                    "    <transition name='to-s' to='s' />" +
                    "  </start-state>" +
                    "  <state name='s'>" +
                    "    <transition name='to-end' to='end' />" +
                    "  </state>" +
                    "  <end-state name='end' />" +
                    "</process-definition>";
            workflowManager.deployProcessDefinition(new ByteArrayInputStream(anotherWorkflow.getBytes()), "text/xml", locale);

            // Note: when new workflow samples are added to the default repo setup, the
            //       below numbers need to be augmented for the test to succeed
            List<WfProcessDefinition> workflowDefinitions = workflowManager.getAllLatestProcessDefinitions(locale);
            assertEquals(5, workflowDefinitions.size());

            workflowDefinitions = workflowManager.getAllProcessDefinitions(locale);
            assertEquals(6, workflowDefinitions.size());
        }

        {
            // Test exceptions in case of trying to load workflow definitions which don't exist
            try {
                workflowManager.getProcessDefinition("555555", locale);
                fail("Expected a ProcessDefinitionNotFoundException");
            } catch (ProcessDefinitionNotFoundException e) {}

            try {
                workflowManager.getLatestProcessDefinition("a name which doesn't exist", locale);
                fail("Expected a ProcessDefinitionNotFoundException");
            } catch (ProcessDefinitionNotFoundException e) {}

            try {
                workflowManager.startProcess("555555", null, null, locale);
                fail("Expected a ProcessDefinitionNotFoundException");
            } catch (ProcessDefinitionNotFoundException e) {}

            try {
                workflowManager.deleteProcessDefinition("555555");
                fail("Expected a ProcessDefinitionNotFoundException");
            } catch (ProcessDefinitionNotFoundException e) {}

            try {
                workflowManager.getProcess("555555", locale);
                fail("Expected a ProcessInstanceNotFoundException");
            } catch (ProcessInstanceNotFoundException e) {}

            try {
                workflowManager.signal("555555", "boe", "boe", locale);
                fail("Expected a ProcessInstanceNotFoundException");
            } catch (ProcessInstanceNotFoundException e) {}
        }

        {
            String validWorkflow = "<process-definition name='test1'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <state name='s'>" +
                    "    <transition name='to-end' to='end'/>" +
                    "  </state>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            WfProcessDefinition workflowDefinition = workflowManager.deployProcessDefinition(new ByteArrayInputStream(validWorkflow.getBytes()), "text/xml", locale);
            WfProcessInstance workflow = workflowManager.startProcess(workflowDefinition.getId(), null, "trans1", locale);

            assertNotNull(workflow.getId());
            assertNotNull(workflow.getStart());
            assertNull(workflow.getEnd());

            assertEquals("/", workflow.getRootExecutionPath().getPath());
            assertEquals("s", workflow.getRootExecutionPath().getNode().getName());

            workflowManager.signal(workflow.getId(), workflow.getRootExecutionPath().getPath(), null, locale);
            workflow = workflowManager.getProcess(workflow.getId(), locale);

            assertNotNull(workflow.getEnd());
            assertEquals("end", workflow.getRootExecutionPath().getNode().getName());

            // Delete the workflow definition, which will also delete the associated workflow instances
            workflowManager.deleteProcessDefinition(workflowDefinition.getId());
        }

        {
            // A workflow with a task

            String validWorkflow = "<process-definition name='test2'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <task-node name='s'>" +
                    "    <task name='task1'>" +
                    "      <assignment actor-id='" + repository.getUserId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans2' to='end'/>" +
                    "  </task-node>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            WfProcessDefinition workflowDefinition = workflowManager.deployProcessDefinition(new ByteArrayInputStream(validWorkflow.getBytes()), "text/xml", locale);
            WfProcessInstance workflow = workflowManager.startProcess(workflowDefinition.getId(), null, "trans1", locale);
            assertEquals(1, workflowManager.getMyTasks(locale).size());

            WfProcessInstance workflow2 = workflowManager.startProcess(workflowDefinition.getId(), null, "trans1", locale);
            assertEquals(2, workflowManager.getMyTasks(locale).size());

            List<WfTask> tasks = workflowManager.getMyTasks(locale);
            WfTask task1 = tasks.get(0);
            WfTask task2 = tasks.get(1);

            workflowManager.updateTask(task1.getId(), null, locale);
            assertEquals(2, workflowManager.getMyTasks(locale).size());

            workflowManager.endTask(task1.getId(), null, null, locale);
            assertEquals(1, workflowManager.getMyTasks(locale).size());

            workflowManager.endTask(task2.getId(), null, "trans2", locale);
            assertEquals(0, workflowManager.getMyTasks(locale).size());
        }

        Map<String, byte[]> resourceBundles = new HashMap<String, byte[]>();
        resourceBundles.put("i18n/messages.xml", readResourceBytes("messages.xml"));
        resourceBundles.put("i18n/messages_nl.xml", readResourceBytes("messages_nl.xml"));

        {
            String workflow = "<process-definition name='test3'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <task-node name='s'>" +
                    "    <task name='test-task'>" +
                    "      <assignment actor-id='" + repository.getUserId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans2' to='end'/>" +
                    "  </task-node>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            // invalid metadata according to schema
            String invalidWorkflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflow'><an-invalid-tag/></workflowMeta>";

            try {
                workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, invalidWorkflowMeta, resourceBundles), "application/zip", locale);
                fail("Expected a WorkflowException because the workflow metadata is invalid.");
            } catch (WorkflowException e) {}

            // invalid meta: XML well-formedness error
            String anotherInvalidWorkflowMeta = "<workflow-met";
            try {
                workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, anotherInvalidWorkflowMeta, resourceBundles), "application/zip", locale);
                fail("Expected a WorkflowException because the workflow metadata is not well formed XML.");
            } catch (WorkflowException e) {}

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "</workflowMeta>";
            WfProcessDefinition wfDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);

            assertEquals("My workflow", wfDef.getLabel().getText());
            assertEquals("A workflow for test purposes.", wfDef.getDescription().getText());

            String workflowMeta2 = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "  <nodes>" +
                    "    <node path='s'>" +
                    "      <transition name='trans2'>" +
                    "        <label>Finish!</label>" +
                    "      </transition>" +
                    "    </node>" +
                    "  </nodes>" +
                    "</workflowMeta>";
            WfProcessDefinition wfDef2 = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta2, resourceBundles), "application/zip", locale);
            assertEquals("Finish!", wfDef2.getTask("test-task").getNode().getLeavingTransitions().get(0).getLabel().getText());

            String workflowMeta3 = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "  <tasks>" +
                    "    <task name='test-task'>" +
                    "      <label>My test task</label>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";
            WfProcessDefinition wfDef3 = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta3, resourceBundles), "application/zip", locale);
            assertEquals("My test task", wfDef3.getTask("test-task").getLabel().getText());
        }

        {
            String workflow = "<process-definition name='test3'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <task-node name='s'>" +
                    "    <event type='node-enter'>" +
                    "      <script>" +
                    "        executionContext.getContextInstance().createVariable(\"var-readonly\", \"hello\", token);" +
                    "      </script>" +
                    "    </event>" +
                    "    <task name='test-task'>" +
                    "      <assignment actor-id='" + repository.getUserId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans2' to='s2'/>" +
                    "  </task-node>" +
                    "  <task-node name='s2'>" +
                    "    <task name='another-test-task'>" +
                    "      <assignment actor-id='" + repository.getUserId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans3' to='end'/>" +
                    "  </task-node>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "  <variables>" +
                    "    <variable name='var2'         type='string'     scope='global'/>" +
                    "    <variable name='var-link'     type='daisy-link' scope='global'/>" +
                    "    <variable name='var-long'     type='long'       scope='global'/>" +
                    "    <variable name='var-date'     type='date'       scope='global'/>" +
                    "    <variable name='var-datetime' type='datetime'   scope='global'/>" +
                    "    <variable name='var-actor'    type='actor'      scope='global'/>" +
                    "    <variable name='var-actor2'   type='actor'      scope='global'/>" +
                    "    <variable name='var-boolean'  type='boolean'    scope='global'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='test-task'>" +
                    "      <label>My test task</label>" +
                    "      <variables>" +
                    "        <variable name='var1' type='string' scope='task'>" +
                    "          <label>Variable 1</label>" +
                    "        </variable>" +
                    "        <variable base='var2'/>" +
                    "        <variable base='var-link'/>" +
                    "        <variable base='var-long'/>" +
                    "        <variable base='var-date'/>" +
                    "        <variable base='var-datetime'/>" +
                    "        <variable base='var-actor'/>" +
                    "        <variable base='var-actor2'/>" +
                    "        <variable base='var-boolean'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            WfProcessDefinition wfDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);
            WfProcessInstance wfInst = workflowManager.startProcess(wfDef.getId(), null, null, locale);

            // two instances should exist at this point
            assertEquals(2, wfInst.getTasks().size());

            WfTask task = wfInst.getTask("test-task");
            assertNotNull(task);

            assertEquals(TaskPriority.NORMAL, task.getPriority());
            assertNull(task.getDueDate());

            TaskUpdateData taskUpdateData = new TaskUpdateData();
            taskUpdateData.setPriority(TaskPriority.HIGHEST);
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);

            assertEquals(TaskPriority.HIGHEST, task.getPriority());
            assertNull(task.getDueDate());

            Date dueDate = getDate(new Date(System.currentTimeMillis() + (1000 * 60 * 60)), true);
            taskUpdateData.setDueDate(dueDate);
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);

            assertEquals(dueDate, task.getDueDate());

            // Basic variables test
            taskUpdateData.setVariable("var1", VariableScope.TASK, WfValueType.STRING, "boe!");
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);
            assertEquals("boe!", task.getVariable("var1", VariableScope.TASK).getValue());
            assertNull(task.getVariable("var1", VariableScope.GLOBAL));

            // Setting variable in wrong scope should be ignored
            taskUpdateData.setVariable("var2", VariableScope.TASK, WfValueType.STRING, "boe2!");
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);
            assertNull(task.getVariable("var2", VariableScope.TASK));
            assertNull(task.getVariable("var2", VariableScope.GLOBAL));

            taskUpdateData.setVariable("var2", VariableScope.GLOBAL, WfValueType.STRING, "boe2!");
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);
            assertEquals("boe2!", task.getVariable("var2", VariableScope.GLOBAL).getValue());
            assertEquals(2, task.getVariables().size());

            taskUpdateData.deleteVariable("var2", VariableScope.GLOBAL);
            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);
            assertNull(task.getVariable("var2", VariableScope.GLOBAL));
            assertEquals(1, task.getVariables().size());

            assertEquals("Variable 1", wfDef.getTask("test-task").getVariable("var1", VariableScope.TASK).getLabel().getText());

            task = workflowManager.updateTask(task.getId(), new TaskUpdateData(), locale);
            // check variables etc. are not removed/reset when nothing is mentioned about them in the TaskUpdateData
            assertEquals(1, task.getVariables().size());
            assertEquals(TaskPriority.HIGHEST, task.getPriority());
            assertEquals(dueDate, task.getDueDate());

            List<Long> poolIds = new ArrayList<Long>();
            poolIds.add(1L);
            poolIds.add(2L);
            WfActorKey poolKey = new WfActorKey(poolIds);

            // try all variable data types
            taskUpdateData = new TaskUpdateData();
            Date date = getDate(new Date(), false);
            Date dateTime = getDate(new Date(), true);
            taskUpdateData.setVariable("var-link", VariableScope.GLOBAL, WfValueType.DAISY_LINK, new WfVersionKey("1-DSY", 1, 1, null));
            taskUpdateData.setVariable("var-long", VariableScope.GLOBAL, WfValueType.LONG, new Long(55));
            taskUpdateData.setVariable("var-date", VariableScope.GLOBAL, WfValueType.DATE, date);
            taskUpdateData.setVariable("var-datetime", VariableScope.GLOBAL, WfValueType.DATETIME, dateTime);
            taskUpdateData.setVariable("var-actor", VariableScope.GLOBAL, WfValueType.ACTOR, new WfActorKey(3));
            taskUpdateData.setVariable("var-actor2", VariableScope.GLOBAL, WfValueType.ACTOR, poolKey);
            taskUpdateData.setVariable("var-boolean", VariableScope.GLOBAL, WfValueType.BOOLEAN, Boolean.TRUE);

            task = workflowManager.updateTask(task.getId(), taskUpdateData, locale);
            assertEquals(new WfVersionKey("1-DSY", 1, 1, null), task.getVariable("var-link", VariableScope.GLOBAL).getValue());
            assertEquals(new Long(55), task.getVariable("var-long", VariableScope.GLOBAL).getValue());
            assertEquals(date, task.getVariable("var-date", VariableScope.GLOBAL).getValue());
            assertEquals(dateTime, task.getVariable("var-datetime", VariableScope.GLOBAL).getValue());
            assertEquals(new WfActorKey(3), task.getVariable("var-actor", VariableScope.GLOBAL).getValue());
            assertEquals(poolKey, task.getVariable("var-actor2", VariableScope.GLOBAL).getValue());
            assertEquals(Boolean.TRUE, task.getVariable("var-boolean", VariableScope.GLOBAL).getValue());


            String workflowMeta2 = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <variables>"  +
                    "    <variable name='var-required' type='string' scope='global' required='true'/>" +
                    "    <variable name='var-readonly' type='string' scope='global' readOnly='true'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='test-task'>" +
                    "      <variables>" +
                    "        <variable base='var-required'/>" +
                    "        <variable base='var-readonly'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            WfProcessDefinition wfDef2 = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta2, resourceBundles), "application/zip", locale);
            WfProcessInstance wfInst2 = workflowManager.startProcess(wfDef2.getId(), null, null, locale);
            WfTask task2 = wfInst2.getTask("test-task");

            taskUpdateData = new TaskUpdateData();
            taskUpdateData.setVariable("var-readonly", VariableScope.GLOBAL, WfValueType.STRING, "hi");
            // updating task should ignore the read only variable and not complain about the missing required variable
            task2 = workflowManager.updateTask(task2.getId(), taskUpdateData, locale);
            assertEquals("hello", task2.getVariable("var-readonly", VariableScope.GLOBAL).getValue());

            try {
                task2 = workflowManager.endTask(task2.getId(), taskUpdateData, null, locale);
                fail("Missing required variable should throw an exception.");
            } catch (WorkflowException e) {}

            taskUpdateData.setVariable("var-required", VariableScope.GLOBAL, WfValueType.STRING, "something");
            workflowManager.endTask(task2.getId(), taskUpdateData, null, locale);

            // Test process suspension, resumption and deletion
            WfProcessInstance suspendedProcess = workflowManager.suspendProcess(wfInst2.getId(), locale);
            assertTrue(suspendedProcess.isSuspended());
            suspendedProcess = workflowManager.getProcess(wfInst2.getId(), locale);
            assertTrue(suspendedProcess.isSuspended());
            suspendedProcess = workflowManager.resumeProcess(wfInst2.getId(), locale);
            assertFalse(suspendedProcess.isSuspended());
            workflowManager.deleteProcess(wfInst2.getId());
            try {
                workflowManager.getProcess(wfInst2.getId(), locale);
                fail("Expected a ProcessInstanceNotFoundException");
            } catch (ProcessInstanceNotFoundException e) { /* ignore */ };
        }

        //
        // Some more variable functionality: selection lists, initial values, styling
        //
        {
            String workflow = "<process-definition name='test4'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans' to='end'/>" +
                    "  </start-state>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wfmeta='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wf='http://outerx.org/daisy/1.0#workflow'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "  <variables>" +
                    "    <variable name='var1' type='string' scope='global'>" +
                    "      <selectionList>" +
                    "        <listItem>" +
                    "          <wf:string>value1</wf:string>" +
                    "          <label>Label 1</label>" +
                    "        </listItem>" +
                    "        <listItem>" +
                    "          <wf:string>value2</wf:string>" +
                    "        </listItem>" +
                    "      </selectionList>" +
                    "      <initialValueScript>" +
                    "        return 'value1';" +
                    "      </initialValueScript>" +
                    "      <wfmeta:styling foo='bar' xmlns=''>foo <b>bar</b> foo!</wfmeta:styling>" +
                    "    </variable>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='start-task'>" +
                    "      <variables>" +
                    "        <variable base='var1'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            WfProcessDefinition wfDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);
            WfTaskDefinition taskDef = wfDef.getTask("start-task");
            WfVariableDefinition variableDef = taskDef.getVariable("var1", VariableScope.GLOBAL);

            assertNotNull(variableDef.getSelectionList());
            assertEquals(2, variableDef.getSelectionList().size());
            assertEquals("value1", variableDef.getSelectionList().get(0).getValue());
            assertEquals("Label 1", variableDef.getSelectionList().get(0).getLabel().getText());
            assertEquals("value2", variableDef.getSelectionList().get(1).getValue());

            List<WfVariable> initialVariableValues = workflowManager.getInitialVariables(wfDef.getId(), null);
            assertEquals(1, initialVariableValues.size());
            assertEquals("value1", initialVariableValues.get(0).getValue());

            System.out.println("Styling info is: ");
            System.out.println(variableDef.getStyling().toString());
        }

        //
        // A review workflow testcase
        //
        {
            // Create a reviewer user
            User reviewerUser = repository.getUserManager().createUser("reviewer");
            reviewerUser.setPassword("reviewer");
            reviewerUser.addToRole(repository.getUserManager().getRole("User", false));
            reviewerUser.save();
            Repository reviewerRepo = repositoryManager.getRepository(new Credentials("reviewer", "reviewer"));
            WorkflowManager reviewerWfManager = (WorkflowManager)reviewerRepo.getExtension("WorkflowManager");

            // Set ACL to something meaningful
            AccessManager accessManager = repository.getAccessManager();
            Acl acl = accessManager.getStagingAcl();

            AclObject aclObject = acl.createNewObject("true");
            AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
            for (AclPermission permission : AclPermission.values()) {
                aclEntry.set(permission, AclActionType.GRANT);
            }
            aclObject.add(aclEntry);
            acl.add(aclObject);
            acl.save();
            accessManager.copyStagingToLive();

            // Create the document to review
            DocumentType docType = repository.getRepositorySchema().createDocumentType("TestDocType");
            docType.save();
            Document document = repository.createDocument("A document to review", docType.getName());
            document.setNewVersionState(VersionState.DRAFT);
            document.save();

            // Create the process definition
            String reviewWorkflow = readResource("review-process-definition.xml");
            String reviewMeta = readResource("review-process-meta.xml");

            WfProcessDefinition reviewDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(reviewWorkflow, reviewMeta, resourceBundles), "application/zip", locale);

            // check resource bundles work
            assertEquals("Review workflow", reviewDef.getLabel().getText());

            // check translated labels + resource bundle fallback
            WfProcessDefinition reviewDefNl = workflowManager.getProcessDefinition(reviewDef.getId(), new Locale("nl", "BE"));
            assertEquals("Verifieer-workflow", reviewDefNl.getLabel().getText());
            assertEquals("Start document review", reviewDefNl.getTask("startReviewTask").getLabel().getText());

            // Start the workflow
            TaskUpdateData startTaskData = new TaskUpdateData();
            WfVersionKey documentKey = WfVersionKey.get(document, "1");
            startTaskData.setVariable("document", VariableScope.GLOBAL, WfValueType.DAISY_LINK, documentKey);
            startTaskData.setVariable("reviewer", VariableScope.GLOBAL, WfValueType.ACTOR, new WfActorKey(reviewerUser.getId()));
            WfProcessInstance reviewInst = workflowManager.startProcess(reviewDef.getId(), startTaskData, "review", locale);

            // Reviewer does the reviewing
            List<WfTask> reviewerTasks = reviewerWfManager.getMyTasks(locale);
            System.out.println("reviewer tasks: " + reviewerTasks.size());
            assertEquals(1, reviewerTasks.size());
            WfTask reviewTask = reviewerTasks.get(0);
            assertEquals("reviewTask", reviewTask.getDefinition().getName());
            assertEquals(documentKey, reviewTask.getVariable("document").getValue());

            TaskUpdateData reviewerData = new TaskUpdateData();
            reviewerData.setVariable("reviewComment", VariableScope.GLOBAL, WfValueType.STRING, "This needs some more work.");
            reviewerWfManager.endTask(reviewTask.getId(), reviewerData, "requestChanges", locale);

            // Submitter gets requestChanges task and asks new review
            List<WfTask> tasks = workflowManager.getMyTasks(locale);
            WfTask requestChangesTask = null;
            for (WfTask task : tasks) {
                System.out.println("found task " + task.getDefinition().getName());
                if (task.getDefinition().getName().equals("requestChangesTask")) {
                    requestChangesTask = task;
                    break;
                }
            }
            assertNotNull("Did not find the request changes task", requestChangesTask);
            assertEquals("This needs some more work.", requestChangesTask.getVariable("reviewComment").getValue());

            // Do some update on the document
            document.setName("A document to review -- updated");
            document.setNewVersionState(VersionState.DRAFT);
            document.save();

            WfVersionKey updatedVersionKey = WfVersionKey.get(document, String.valueOf(document.getLastVersionId()));
            assertEquals(VersionState.DRAFT, updatedVersionKey.getVersion(repository).getState());

            TaskUpdateData requestChangesData = new TaskUpdateData();
            requestChangesData.setVariable("fixComment", VariableScope.GLOBAL, WfValueType.STRING, "Did some improvements.");
            requestChangesData.setVariable("document", VariableScope.GLOBAL, WfValueType.DAISY_LINK, updatedVersionKey);
            workflowManager.endTask(requestChangesTask.getId(), requestChangesData, "newReview", locale);

            // Reviewer accepts
            reviewerWfManager.endTask(reviewerWfManager.getMyTasks(locale).get(0).getId(), null, "approve", locale);
            assertEquals(0, reviewerWfManager.getMyTasks(locale).size());

            // check document version was put to state 'publish'
            assertEquals(VersionState.PUBLISH, updatedVersionKey.getVersion(repository).getState());

            // Check process is ended
            reviewInst = reviewerWfManager.getProcess(reviewInst.getId(), locale);
            assertNotNull(reviewInst.getEnd());

            //
            // Do another review, now with a pool assigned as reviewer
            //

            // First create a pool
            WfPoolManager poolManager = workflowManager.getPoolManager();
            WfPool pool = poolManager.createPool("My pool");
            pool.save();
            poolManager.addUsersToPool(pool.getId(), longList(reviewerUser.getId(), 500, 501, 502));

            startTaskData = new TaskUpdateData();
            documentKey = WfVersionKey.get(document, "1");
            startTaskData.setVariable("document", VariableScope.GLOBAL, WfValueType.DAISY_LINK, documentKey);
            startTaskData.setVariable("reviewer", VariableScope.GLOBAL, WfValueType.ACTOR, new WfActorKey(longList(pool.getId())));
            reviewInst = workflowManager.startProcess(reviewDef.getId(), startTaskData, "review", locale);

            assertEquals(0, reviewerWfManager.getMyTasks(locale).size());
            assertEquals(1, reviewerWfManager.getPooledTasks(locale).size());
            WfTask pooledTask = reviewerWfManager.getPooledTasks(locale).get(0);
            assertEquals(-1, pooledTask.getActorId());

            // Try same query ("my pooled tasks") using the generic query API
            QueryConditions queryConditions = new QueryConditions();
            List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
            queryConditions.addSpecialCondition("tasksInMyPool", new WfValueType[0], new Object[0]);
            queryConditions.addCondition("task.actor", WfValueType.USER, "is_null");
            queryConditions.addCondition("task.isOpen", WfValueType.BOOLEAN, "eq", Boolean.TRUE);
            assertEquals(1, reviewerWfManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US).size());

            try {
                dummyWfManager.requestPooledTask(pooledTask.getId(), locale);
                fail("Requesting a pooled task which does not belong to any of the user's pools should fail.");
            } catch (WfAuthorizationException e) {}

            WfTask myTask = reviewerWfManager.requestPooledTask(pooledTask.getId(), locale);
            assertEquals(reviewerUser.getId(), myTask.getActorId());
            assertEquals(1, reviewerWfManager.getMyTasks(locale).size());
            assertEquals(0, reviewerWfManager.getPooledTasks(locale).size());

            try {
                reviewerWfManager.requestPooledTask(pooledTask.getId(), locale);
                fail("Requesting assignment of a non-pooled task should fail.");
            } catch (WorkflowException e) {}
        }

        //
        // Test the generic process and task query API
        //
        {
            String workflow = "<process-definition name='searchtest'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans' to='end'/>" +
                    "  </start-state>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wfmeta='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wf='http://outerx.org/daisy/1.0#workflow'>" +
                    "  <label>My workflow</label>" +
                    "  <description>A workflow for test purposes.</description>" +
                    "  <variables>" +
                    "    <variable name='var1' type='string' scope='global'>" +
                    "      <selectionList>" +
                    "        <listItem>" +
                    "          <wf:string>value1</wf:string>" +
                    "          <label>Label 1</label>" +
                    "        </listItem>" +
                    "        <listItem>" +
                    "          <wf:string>value2</wf:string>" +
                    "        </listItem>" +
                    "      </selectionList>" +
                    "      <initialValueScript>" +
                    "        return 'value1';" +
                    "      </initialValueScript>" +
                    "      <wfmeta:styling foo='bar' xmlns=''>foo <b>bar</b> foo!</wfmeta:styling>" +
                    "    </variable>" +
                    "    <variable name='var2' type='long' scope='global'/>" +
                    "    <variable name='var3' type='string' scope='global'/>" +
                    "    <variable name='var4' type='boolean' scope='global'/>" +
                    "    <variable name='var5' type='daisy-link' scope='global'/>" +
                    "    <variable name='var6' type='daisy-link' scope='global'/>" +
                    "    <variable name='var7' type='date' scope='global'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='start-task'>" +
                    "      <variables>" +
                    "        <variable base='var1'/>" +
                    "        <variable base='var2'/>" +
                    "        <variable base='var3' type='string' scope='task'/>" +
                    "        <variable base='var4'/>" +
                    "        <variable base='var5'/>" +
                    "        <variable base='var6'/>" +
                    "        <variable base='var7'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            WfProcessDefinition wfDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);

            Date testDate = getDate(new Date(), false);
            TaskUpdateData startTaskData = new TaskUpdateData();
            startTaskData.setVariable("var1", VariableScope.GLOBAL, WfValueType.STRING, "value1");
            startTaskData.setVariable("var2", VariableScope.GLOBAL, WfValueType.LONG, new Long(33));
            startTaskData.setVariable("var3", VariableScope.TASK, WfValueType.STRING, "testval");
            startTaskData.setVariable("var4", VariableScope.GLOBAL, WfValueType.BOOLEAN, Boolean.TRUE);
            startTaskData.setVariable("var5", VariableScope.GLOBAL, WfValueType.DAISY_LINK, new WfVersionKey("99-DSY", 1, 1, null));
            startTaskData.setVariable("var6", VariableScope.GLOBAL, WfValueType.DAISY_LINK, new WfVersionKey("99-DSY", 1, 1, "live"));
            startTaskData.setVariable("var7", VariableScope.GLOBAL, WfValueType.DATE, testDate);
            workflowManager.startProcess(wfDef.getId(), startTaskData, null, Locale.US);

            List<WfTask> tasks;
            List<WfProcessInstance> processes;
            QueryConditions queryConditions;
            List<QueryOrderByItem> orderByItems;

            // QueryConditions without any conditions should work
            queryConditions = new QueryConditions();
            orderByItems = Collections.emptyList();
            workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);

            // Test a like-condition
            queryConditions = new QueryConditions();
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%val%");
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // Test any-combination
            queryConditions = new QueryConditions();
            queryConditions.setMeetAllCriteria(false);
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%val%");
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%somethingitdoesntcontain%");
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // Test all-combination
            queryConditions = new QueryConditions();
            queryConditions.setMeetAllCriteria(true);
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%val%");
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%somethingitdoesntcontain%");
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(0, tasks.size());

            // Test is not null condition
            queryConditions = new QueryConditions();
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "is_not_null");
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // test a between condition
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var2", WfValueType.LONG, "between", 32l, 34l);
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // test a between condition, searching for processes
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var2", WfValueType.LONG, "between", 32l, 34l);
            processes = workflowManager.getProcesses(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, processes.size());

            // less than
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var2", WfValueType.LONG, "lt", 34l);
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // greater than
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var2", WfValueType.LONG, "gt", 32l);
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // boolean search
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var4", WfValueType.BOOLEAN, "eq", Boolean.TRUE);
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // version key search
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var5", WfValueType.DAISY_LINK, "like", new WfVersionKey("99-DSY", 1, 1, null));
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var6", WfValueType.DAISY_LINK, "like", new WfVersionKey("99-DSY", 1, 1, "live"));
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var6", WfValueType.DAISY_LINK, "like", new WfVersionKey("99-DSY", 1, 1, "last"));
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(0, tasks.size());

            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var6", WfValueType.DAISY_LINK, "like", new WfVersionKey("99-DSY", 1, 1, null));
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // date search
            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var7", WfValueType.DATE, "eq", testDate);
            tasks = workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, tasks.size());

            // relatedToDocument special condition
            queryConditions = new QueryConditions();
            queryConditions.addSpecialCondition("relatedToDocument", new WfValueType[] { WfValueType.DAISY_LINK }, new Object[] {new WfVersionKey("99-DSY", 1, 1, null)});
            processes = workflowManager.getProcesses(queryConditions, orderByItems, -1, -1, Locale.US);
            assertEquals(1, processes.size());

            // Test some basic error checking
            queryConditions = new QueryConditions();
            try {
                queryConditions.addProcessVariableCondition("var7", WfValueType.DATE, "eq", "foo");
                fail("Giving an incorrect type of data should fail.");
            } catch (IllegalArgumentException e) {} catch (Exception e) {}

            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var7", WfValueType.DATE, "eq", new Date(), new Date());
            try {
                workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
                fail("Giving an incorrect number of values should fail.");
            } catch (IllegalArgumentException e) {} catch (Exception e) {}

            queryConditions = new QueryConditions();
            queryConditions.addProcessVariableCondition("var7", WfValueType.DATE, "like", testDate);
            try {
                workflowManager.getTasks(queryConditions, orderByItems, -1, -1, Locale.US);
                fail("A like-search for a date should give an error");
            } catch (WorkflowException e) {}

            queryConditions = new QueryConditions();
            queryConditions.addTaskVariableCondition("var3", WfValueType.STRING, "like", "%val%");
            try {
                workflowManager.getProcesses(queryConditions, orderByItems, -1, -1, Locale.US);
                fail("Searching for processes using taks variables should fail.");
            } catch (WorkflowException e) {}

            // test selecting some stuff -- tasks
            queryConditions = new QueryConditions();
            queryConditions.addCondition("process.definitionName", WfValueType.STRING, "eq", "searchtest");
            List<QuerySelectItem> selectItems = new ArrayList<QuerySelectItem>();
            selectItems.add(new QuerySelectItem("task.id", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("var1", QueryValueSource.PROCESS_VARIABLE));
            selectItems.add(new QuerySelectItem("task.definitionLabel", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("task.definitionDescription", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("task.create", QueryValueSource.PROPERTY));
            orderByItems = new ArrayList<QueryOrderByItem>();
            orderByItems.add(new QueryOrderByItem("task.id", QueryValueSource.PROPERTY, SortOrder.ASCENDING));
            SearchResultDocument.SearchResult result = workflowManager.searchTasks(selectItems, queryConditions, orderByItems, -1, -1, Locale.US).getSearchResult();
            assertEquals(1, result.getRows().getRowArray().length);
            assertTrue(result.getRows().getRowArray(0).getValueArray(1).isSetLabel());

            // test selecting some stuff -- processes
            queryConditions = new QueryConditions();
            queryConditions.addCondition("process.definitionName", WfValueType.STRING, "eq", "searchtest");
            selectItems = new ArrayList<QuerySelectItem>();
            selectItems.add(new QuerySelectItem("daisy_creator", QueryValueSource.PROCESS_VARIABLE));
            selectItems.add(new QuerySelectItem("daisy_owner", QueryValueSource.PROCESS_VARIABLE));
            selectItems.add(new QuerySelectItem("process.id", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("process.start", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("process.end", QueryValueSource.PROPERTY));
            selectItems.add(new QuerySelectItem("var1", QueryValueSource.PROCESS_VARIABLE));
            orderByItems = new ArrayList<QueryOrderByItem>();
            orderByItems.add(new QueryOrderByItem("process.start", QueryValueSource.PROPERTY, SortOrder.ASCENDING));
            result = workflowManager.searchProcesses(selectItems, queryConditions, orderByItems, -1, -1, Locale.US).getSearchResult();
            assertEquals(1, result.getRows().getRowArray().length);
            assertEquals("3", ((XmlString)result.getRows().getRowArray(0).getValueArray(0).getRaw()).getStringValue());
            assertEquals("testuser", ((XmlString)result.getRows().getRowArray(0).getValueArray(0).getLabel()).getStringValue());
            assertEquals("3", ((XmlString)result.getRows().getRowArray(0).getValueArray(1).getRaw()).getStringValue());
            assertEquals("testuser", ((XmlString)result.getRows().getRowArray(0).getValueArray(1).getLabel()).getStringValue());
        }

        // Some task-vs-global variable tests
        {
            String workflow = "<process-definition name='searchtest'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans' to='end'/>" +
                    "  </start-state>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wfmeta='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wf='http://outerx.org/daisy/1.0#workflow'>" +
                    "  <variables>" +
                    "    <variable name='var' type='date' scope='global'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='start-task'>" +
                    "      <variables>" +
                    "        <variable base='var' type='long'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            try {
                workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);
                fail("Overriding a global variable with a different type should fail.");

            } catch (WorkflowException e) {}

            workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wfmeta='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wf='http://outerx.org/daisy/1.0#workflow'>" +
                    "  <tasks>" +
                    "    <task name='start-task'>" +
                    "      <variables>" +
                    "        <variable name='var' type='long' scope='global'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            try {
                workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);
                fail("Defining a global variable not-globally should fail.");
            } catch (WorkflowException e) {}

            workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wfmeta='http://outerx.org/daisy/1.0#workflowmeta'" +
                    "      xmlns:wf='http://outerx.org/daisy/1.0#workflow'>" +
                    "  <variables>" +
                    "    <variable name='var' type='long' scope='global'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='start-task'>" +
                    "      <variables>" +
                    "        <variable base='var'>" +
                    "          <selectionList>" +
                    "            <listItem>" +
                    "              <wf:string>value1</wf:string>" +
                    "            </listItem>" +
                    "          </selectionList>" +
                    "        </variable>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            try {
                workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);
                fail("Changing the selection list of a global variable locally should fail.");
            } catch (WorkflowException e) {}
        }

        //
        // Some authorization tests
        //
        {
            // Create a user
            User authTestUser = repository.getUserManager().createUser("auth-test");
            authTestUser.setPassword("auth-test");
            authTestUser.addToRole(repository.getUserManager().getRole("User", false));
            authTestUser.save();
            Repository authTestRepo = repositoryManager.getRepository(new Credentials("auth-test", "auth-test"));
            WorkflowManager authTestWfManager = (WorkflowManager)authTestRepo.getExtension("WorkflowManager");

            WfPool pool = workflowManager.getPoolManager().createPool("AuthTestPool");
            pool.save();

            String workflow = "<process-definition name='testaccess'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <task-node name='s'>" +
                    "    <task name='test-task'>" +
                    "      <assignment pooled-actors='" + pool.getId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans2' to='end'/>" +
                    "  </task-node>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            String workflowMeta = "<workflowMeta xmlns='http://outerx.org/daisy/1.0#workflowmeta'>" +
                    "  <variables>" +
                    "    <variable name='daisy_document' type='daisy-link' scope='global'/>" +
                    "  </variables>" +
                    "  <tasks>" +
                    "    <task name='test-task'>" +
                    "      <variables>" +
                    "        <variable base='daisy_document'/>" +
                    "      </variables>" +
                    "    </task>" +
                    "  </tasks>" +
                    "</workflowMeta>";

            WfProcessDefinition processDef = workflowManager.deployProcessDefinition(buildWorkflowArchive(workflow, workflowMeta, resourceBundles), "application/zip", locale);

            WfProcessInstance processInstance = workflowManager.startProcess(processDef.getId(), new TaskUpdateData(), "trans1", Locale.US);

            // normal user should not be able to access a process with which has nothing to do
            try {
                authTestWfManager.getProcess(processInstance.getId(), Locale.US);
                fail("User should not be able to access process instance.");
            } catch (WfAuthorizationException e) {}

            workflowManager.getPoolManager().addUsersToPool(pool.getId(), longList(authTestUser.getId()));
            // now this should succeed
            authTestWfManager.getProcess(processInstance.getId(), Locale.US);
            // take away pool membership again
            workflowManager.getPoolManager().removeUsersFromPool(pool.getId(), longList(authTestUser.getId()));

            // Associate process with a document
            Document document = repository.createDocument("A test document", "TestDocType");
            document.save();

            TaskUpdateData taskUpdateData = new TaskUpdateData();
            taskUpdateData.setVariable("daisy_document", VariableScope.GLOBAL, WfValueType.DAISY_LINK, WfVersionKey.get(document, null));
            String taskId = processInstance.getTask("test-task").getId();
            workflowManager.updateTask(taskId, taskUpdateData, Locale.US);

            // now should succeed again because process instance is associated with a document the user can read
            authTestWfManager.getProcess(processInstance.getId(), Locale.US);

            // update document so user can't read it anymore
            document.setPrivate(true);
            document.save();

            try {
                authTestWfManager.getProcess(processInstance.getId(), Locale.US);
                fail("User should not be able to access process instance.");
            } catch (WfAuthorizationException e) {}

            // add user back to the pool and test filtering of task lists
            workflowManager.getPoolManager().addUsersToPool(pool.getId(), longList(authTestUser.getId()));
            assertEquals(0, authTestWfManager.getPooledTasks(Locale.US).size());

            // test direct task access
            try {
                authTestWfManager.getTask(taskId, Locale.US);
                fail("User should not be able to access task instance.");
            } catch (WfAuthorizationException e) {}

            // test direct task access
            try {
                authTestWfManager.requestPooledTask(taskId, Locale.US);
                fail("User should not be able to request pooled task.");
            } catch (WfAuthorizationException e) {}

            // make doc non-private, should make task accessible to the user
            document.setPrivate(false);
            document.save();

            // test direct task access
            authTestWfManager.getTask(taskId, Locale.US);

            // test pool-list access
            assertEquals(1, authTestWfManager.getPooledTasks(Locale.US).size());

            // test requesting pooled task
            authTestWfManager.requestPooledTask(taskId, Locale.US);
            authTestWfManager.getTask(taskId, Locale.US);

            // Once the user is assigned to the task, he should be able to access task and process even if he
            // can't read the associated document
            document.setPrivate(true);
            document.save();
            authTestWfManager.getTask(taskId, Locale.US);
            authTestWfManager.getProcess(processInstance.getId(), Locale.US);
        }

        //
        // Some timer tests
        //
        {
            String workflow = "<process-definition name='test-timers'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <state name='s'>" +
                    "    <event type='node-enter'>" +
                    "      <create-timer name='my-timer' duedate='1000 days'>" +
                    "      </create-timer>" +
                    "    </event>" +
                    "    <transition name='trans2' to='end'/>" +
                    "  </state>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            WfProcessDefinition processDef = workflowManager.deployProcessDefinition(new ByteArrayInputStream(workflow.getBytes()), "text/xml", locale);

            // start two processes, hence two timers
            workflowManager.startProcess(processDef.getId(), new TaskUpdateData(), "trans1", Locale.US);
            workflowManager.startProcess(processDef.getId(), new TaskUpdateData(), "trans1", Locale.US);

            // test getTimer and getXml
            workflowManager.getTimer("1", Locale.US).getXml();

            // test querying
            QueryConditions conditions = new QueryConditions();
            conditions.addCondition("timer.suspended", WfValueType.BOOLEAN, "eq", false);
            conditions.addCondition("timer.name", WfValueType.STRING, "eq", "my-timer");

            List<QueryOrderByItem> orderByItems = new ArrayList<QueryOrderByItem>();
            orderByItems.add(new QueryOrderByItem("timer.id", QueryValueSource.PROPERTY, SortOrder.DESCENDING));
            List<WfTimer> timers = workflowManager.getTimers(conditions, orderByItems, -1, -1, Locale.US);
            assertEquals(2, timers.size());

            List<QuerySelectItem> querySelectItems = new ArrayList<QuerySelectItem>();
            querySelectItems.add(new QuerySelectItem("timer.id", QueryValueSource.PROPERTY));
            querySelectItems.add(new QuerySelectItem("timer.name", QueryValueSource.PROPERTY));
            querySelectItems.add(new QuerySelectItem("timer.dueDate", QueryValueSource.PROPERTY));
            querySelectItems.add(new QuerySelectItem("process.id", QueryValueSource.PROPERTY));
            querySelectItems.add(new QuerySelectItem("timer.suspended", QueryValueSource.PROPERTY));
            querySelectItems.add(new QuerySelectItem("timer.failed", QueryValueSource.PROPERTY));
            SearchResultDocument result = workflowManager.searchTimers(querySelectItems, conditions, orderByItems, -1, -1, Locale.US);
            assertEquals(2, result.getSearchResult().getRows().getRowArray().length);
        }

        //
        // Test un-assign and re-assign
        //
        {
            WfPoolManager poolManager = workflowManager.getPoolManager();
            WfPool pool = poolManager.createPool("Assing reassign test pool");
            pool.save();
            poolManager.addUsersToPool(pool.getId(), longList(dummyUser.getId()));

            String workflow = "<process-definition name='testTaskAssignment'>" +
                    "  <swimlane name='initiator'/>" +
                    "  <swimlane name='lane2'>" +
                    "    <assignment pooled-actors='" + pool.getId() + "'/>" +
                    "  </swimlane>" +
                    "  <start-state name='start'>" +
                    "    <task name='start-task' swimlane='initiator'/>" +
                    "    <transition name='trans1' to='s'/>" +
                    "  </start-state>" +
                    "  <task-node name='s'>" +
                    "    <task name='test-task'>" +
                    "      <assignment pooled-actors='" + pool.getId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans2' to='s2'/>" +
                    "  </task-node>" +
                    "  <task-node name='s2'>" +
                    "    <task name='test-task2'>" +
                    "      <assignment actor-id='" + dummyUser.getId() + "'/>" +
                    "    </task>" +
                    "    <transition name='trans3' to='s3'/>" +
                    "  </task-node>" +
                    "  <task-node name='s3'>" +
                    "    <task name='test-task3' swimlane='lane2'>" +
                    "    </task>" +
                    "    <transition name='trans4' to='s4'/>" +
                    "  </task-node>" +
                    "  <task-node name='s4'>" +
                    "    <task name='test-task4' swimlane='lane2'>" +
                    "    </task>" +
                    "    <transition name='trans4' to='end'/>" +
                    "  </task-node>" +
                    "  <end-state name='end'/>" +
                    "</process-definition>";

            WfProcessDefinition processDef = workflowManager.deployProcessDefinition(new ByteArrayInputStream(workflow.getBytes()), "text/xml", locale);

            TaskUpdateData taskUpdateData = new TaskUpdateData();
            WfProcessInstance process = workflowManager.startProcess(processDef.getId(), taskUpdateData, null, locale);

            // dummy user requests task
            WfTask task = process.getTask("test-task");
            dummyWfManager.requestPooledTask(task.getId(), locale);

            // dummy user unassigns himself from the task
            task = dummyWfManager.unassignTask(task.getId(), locale);
            assertEquals(-1, task.getActorId());

            // testuser requests tasks, dummy user tries to unassign it but should not be allowed to do that
            workflowManager.requestPooledTask(task.getId(), locale);
            try {
                dummyWfManager.unassignTask(task.getId(), locale);
                fail("An ordinary user shouldn't be able to unassign the task of someone else.");
            } catch (WorkflowException e) {}

            // re-assign task to dummy user
            task = workflowManager.assignTask(task.getId(), new WfActorKey(dummyUser.getId()), false, locale);
            assertEquals(dummyUser.getId(), task.getActorId());

            // once more unassign it
            dummyWfManager.unassignTask(task.getId(), locale);

            // transition to next task
            task = workflowManager.assignTask(task.getId(), new WfActorKey(repository.getUserId()), false, locale);
            workflowManager.endTask(task.getId(), new TaskUpdateData(), null, locale);

            // test un-assigning when there are no pools to fall back to
            process = workflowManager.getProcess(process.getId(), locale);
            WfTask task2 = process.getTask("test-task2");
            try {
                dummyWfManager.unassignTask(task2.getId(), locale);
                fail("Unassignment should fail since there are no pools to fall back too.");
            } catch (WorkflowException e) {}

            // re-assign task2 to a pool, let the dummy request it and unassing himself from it, now this should work
            // since there is a pool to fall back too.
            task2 = workflowManager.assignTask(task2.getId(), new WfActorKey(longList(pool.getId())), false, locale);
            dummyWfManager.requestPooledTask(task2.getId(), locale);
            dummyWfManager.unassignTask(task2.getId(), locale);

            // move to next task
            task2 = workflowManager.endTask(task2.getId(), new TaskUpdateData(), null, locale);

            // Re-assigning task3 with overwrite of swimlane should make that task4, assigned to the same swimlane,
            // also becomes assigned to the same user.
            process = workflowManager.getProcess(process.getId(), locale);
            WfTask task3 = process.getTask("test-task3");
            workflowManager.assignTask(task3.getId(), new WfActorKey(dummyUser.getId()), true, locale);
            dummyWfManager.endTask(task3.getId(), new TaskUpdateData(), null, locale);

            process = workflowManager.getProcess(process.getId(), locale);
            WfTask task4 = process.getTask("test-task4");
            assertEquals(dummyUser.getId(), task4.getActorId());
        }
    }

    private List<Long> longList(long... ids) {
        List<Long> list = new ArrayList<Long>(ids.length);
        for (long id : ids) {
            list.add(id);
        }
        return list;
    }

    private Date getDate(Date date, boolean keepTime) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        if (!keepTime) {
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private InputStream buildWorkflowArchive(String processDefinition, String processMeta, Map<String, byte[]> bundles) throws IOException {
        Map<String, byte[]> entries = new HashMap<String, byte[]>();
        entries.put("processdefinition.xml", processDefinition.getBytes("UTF-8"));
        entries.put("daisy-process-meta.xml", processMeta.getBytes("UTF-8"));

        if (bundles != null) {
            for (Map.Entry<String, byte[]> bundle : bundles.entrySet()) {
                entries.put(bundle.getKey(), bundle.getValue());
            }
        }

        byte[] zipData = buildZip(entries);
        return new ByteArrayInputStream(zipData);
    }

    private byte[] buildZip(Map<String, byte[]> byteArrays) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(bos);

        for (Map.Entry<String, byte[]> entry : byteArrays.entrySet()) {
            zos.putNextEntry(new ZipEntry(entry.getKey()));
            zos.write(entry.getValue());
            zos.closeEntry();
        }

        zos.close();

        return bos.toByteArray();
    }

    String readResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/repository/test/resources/" + name);
        Reader reader = new InputStreamReader(is, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);

        StringBuilder buffer = new StringBuilder();
        int c = bufferedReader.read();
        while (c != -1) {
            buffer.append((char)c);
            c = bufferedReader.read();
        }

        return buffer.toString();
    }

    byte[] readResourceBytes(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/repository/test/resources/" + name);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) != -1) {
            bos.write(buffer, 0, read);
        }

        return bos.toByteArray();
    }
}
