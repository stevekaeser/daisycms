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
package org.jbpm.job.executor;

import org.jbpm.job.Job;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.db.JobSession;
import org.outerj.daisy.workflow.WorkflowException;
import org.outerj.daisy.workflow.serverimpl.DaisyConnectionProvider;
import org.outerj.daisy.workflow.serverimpl.WfObjectBuilder;
import org.outerj.daisy.workflow.jbpm_util.Mailer;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;

import javax.sql.DataSource;
import java.io.StringWriter;
import java.io.PrintWriter;

//
// Daisy note: this is a custom version of jBPM's JobExecutorThread
//  It adds:
//   - making the datasource available
//   - setting the actorId on the jbpmContext
//   - making some Daisy objects available to action through transient process variables
//
//  See the sections marked with *** daisy change ***
//

public class DaisyJobExecutorThread extends JobExecutorThread {
    private DataSource dataSource;
    private Repository repository;
    private RepositoryManager repositoryManager;
    private Mailer mailer;
    private WfObjectBuilder wfObjectBuilder;

    public DaisyJobExecutorThread(String name, JobExecutor jobExecutor, JbpmConfiguration jbpmConfiguration,
            int idleInterval, int maxIdleInterval, long maxLockTime, int maxHistory,
            DataSource dataSource, Repository repository, RepositoryManager repositoryManager, Mailer mailer, WfObjectBuilder wfObjectBuilder) {
        super(name, jobExecutor, jbpmConfiguration, idleInterval, maxIdleInterval, maxLockTime, maxHistory);

        this.dataSource = dataSource;
        this.repository = repository;
        this.repositoryManager = repositoryManager;
        this.mailer = mailer;
        this.wfObjectBuilder = wfObjectBuilder;
    }

    public void run() {
        // *** daisy change ***
        DaisyConnectionProvider.DATASOURCE.set(dataSource);
        super.run();
    }

    protected void executeJob(Job job) {
      JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
      try {
        // *** daisy change ***
        jbpmContext.setActorId(String.valueOf(repository.getUserId()));
        JobSession jobSession = jbpmContext.getJobSession();
        job = jobSession.loadJob(job.getId());

        // *** daisy change ***
        ProcessInstance processInstance = job.getProcessInstance();
        ContextInstance contextInstance = processInstance.getContextInstance();
        contextInstance.setTransientVariable("repository", repository);
        contextInstance.setTransientVariable("wfRepository", repository);
        contextInstance.setTransientVariable("repositoryManager", repositoryManager);
        contextInstance.setTransientVariable("mailer", mailer);
        contextInstance.setTransientVariable("_wfObjectBuilder", wfObjectBuilder);
        try {
            contextInstance.setTransientVariable("_resourceBundles", wfObjectBuilder.getI18nBundle(job.getProcessInstance().getProcessDefinition()));
        } catch (WorkflowException wfException) {
            // this should not happen since we (probably) already successfully obtained objects via the wfObjectBuilder
            throw new RuntimeException(wfException);
        }

        try {
//          log.debug("executing job "+job);
          if (job.execute(jbpmContext)) {
            jobSession.deleteJob(job);
          }

        } catch (Exception e) {
//          log.debug("exception while executing '"+job+"'", e);
          StringWriter sw = new StringWriter();
          e.printStackTrace(new PrintWriter(sw));
          job.setException(sw.toString());
          job.setRetries(job.getRetries()-1);
        }

        // if this job is locked too long
        long totalLockTimeInMillis = System.currentTimeMillis() - job.getLockTime().getTime();
        if (totalLockTimeInMillis>maxLockTime) {
          jbpmContext.setRollbackOnly();
        }

      } finally {
        try {
          jbpmContext.close();
        } catch (RuntimeException e) {
//          log.error("problem committing job execution transaction", e);
          throw e;
        }
      }
    }

}
