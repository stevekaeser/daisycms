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
package org.outerj.daisy.workflow.jbpm_util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.dom4j.Element;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.def.ActionHandler;
import org.jbpm.graph.exe.ExecutionContext;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.taskmgmt.exe.PooledActor;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.outerj.daisy.emailnotifier.EmailSubscriptionManager;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.workflow.VariableScope;
import org.outerj.daisy.workflow.WfPool;
import org.outerj.daisy.workflow.WfPoolManager;
import org.outerj.daisy.workflow.WfProcessInstance;
import org.outerj.daisy.workflow.WfTask;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.WorkflowManager;
import org.outerj.daisy.workflow.serverimpl.WfObjectBuilder;

import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class TaskMailerAction implements ActionHandler {

    public Element mailTemplate;

    public void execute(ExecutionContext executionContext) throws Exception {
        if (mailTemplate == null)
            throw new Exception("The mail template has not been set");

        if (!mailTemplate.isTextOnly())
            throw new Exception("The mail template element should not contain any nested elements");
        String templateName = mailTemplate.getText();

        ContextInstance contextInstance = executionContext.getContextInstance();
        Mailer mailer = (Mailer) contextInstance.getTransientVariable("mailer");

        // Get the repository
        Repository wfRepository = (Repository) contextInstance.getTransientVariable("wfRepository");
        WorkflowManager workflowManager = (WorkflowManager) wfRepository.getExtension("WorkflowManager");
        WfPoolManager poolManager = workflowManager.getPoolManager();

        TaskInstance taskInstance = executionContext.getTaskInstance();
        if (taskInstance == null)
            throw new WorkflowException("The emailer action cannot be used when no task is available.");

        List<Long> userIds = new ArrayList<Long>();

        String taskActorId = taskInstance.getActorId();
        if (taskActorId != null) {
            userIds.add(this.parseActorId(taskActorId));
        }

        Set pooledActors = taskInstance.getPooledActors();
        if (pooledActors != null) {
            Set poolIds = PooledActor.extractActorIds(pooledActors);
            Iterator poolIterator = poolIds.iterator();

            while (poolIterator.hasNext()) {
                String poolIdString = (String) poolIterator.next();
                // the poolId might be the actual Id or a name. So first figure out which it is.
                WfPool pool = retrievePool(poolIdString, poolManager);
                
                userIds.addAll(poolManager.getUsersForPool(pool.getId()));
            }
        }

        if (userIds.isEmpty()) {
            // actorId can be null when the task is being unassigned, in which case no notification should be send
            return;
        }

        String currentActor = executionContext.getJbpmContext().getActorId();

        // Get user's locale preference for mails from the EmailSubscriptionManager (another extension)
        // TODO this is not ideal, eventually we should probably have a global locale user preference in Daisy
        EmailSubscriptionManager subscriptionManager = (EmailSubscriptionManager) wfRepository.getExtension("EmailSubscriptionManager");

        for (Long userId : userIds) {
            // don't send mails to self
            if (userId.toString().equals(currentActor)) {
                continue;
            }
            // Construct the data for the template
            WfObjectBuilder objectBuilder = (WfObjectBuilder) contextInstance.getTransientVariable("_wfObjectBuilder");

            ProcessInstance processInstance = taskInstance.getProcessInstance();

            // Get the user's email address
            String email = wfRepository.getUserManager().getUser(userId, false).getEmail();

            Locale locale = subscriptionManager.getSubscription(userId).getLocale(); // getSubscription never returns null
            if (locale == null)
                locale = Locale.getDefault();

            WfTask task = objectBuilder.buildTask(taskInstance, locale);
            WfProcessInstance wfProcessInstance = objectBuilder.buildProcessInstance(processInstance, locale);

            Map<String, Object> mailData = new HashMap<String, Object>();
            mailData.put("task", task);
            mailData.put("process", wfProcessInstance);
            mailData.put("getVariable", new TaskMailerAction.WfVariableTemplateModel());

            mailer.sendMail(templateName, locale, email, mailData);
        }
    }

    public static class WfVariableTemplateModel implements TemplateMethodModelEx {
        public Object exec(List arguments) throws TemplateModelException {
            // this could use some checking of arguments to throw nicer exceptions
            WfTask task = (WfTask) ((BeanModel) arguments.get(0)).getWrappedObject();
            String scopeName = arguments.get(2).toString();
            String name = arguments.get(1).toString();

            VariableScope variableScope = VariableScope.fromString(scopeName);
            return task.getVariable(name, variableScope);
        }
    }

    private long parseActorId(String actorId) throws WorkflowException {
        long userId;
        try {
            userId = Long.parseLong(actorId);
            return userId;
        } catch (NumberFormatException e) {
            throw new WorkflowException("Actor ID is not a valid Daisy user ID: " + actorId);
        }
    }
    
    private WfPool retrievePool(String poolIdString, WfPoolManager poolManager) throws WorkflowException  {        
        try {
            try {
                long poolId = Long.parseLong(poolIdString);
                return poolManager.getPool(poolId);
            } catch (NumberFormatException e) {
                // So the string doesn't hold an Id but a pool name. Lets see if it exists
                return poolManager.getPoolByName(poolIdString);
            }
        } catch (RepositoryException e) {
            throw new WorkflowException("Could not find Pool with identifier : " + poolIdString, e);
        }
    }
}