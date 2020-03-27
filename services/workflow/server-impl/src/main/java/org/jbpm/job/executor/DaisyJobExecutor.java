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

import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.workflow.jbpm_util.Mailer;
import org.outerj.daisy.workflow.serverimpl.WfObjectBuilder;

import javax.sql.DataSource;

//
// Custom version of jBPM's JobExecutor in order to be able to start
// a custom version of the JobExecutorThread. See DaisyJobExecutorThread
// for more info.
//

public class DaisyJobExecutor extends JobExecutor {
    DataSource dataSource;
    Repository repository;
    RepositoryManager repositoryManager;
    Mailer mailer;
    WfObjectBuilder wfObjectBuilder;

    public void daisyInit(DataSource dataSource, Repository repository, RepositoryManager repositoryManager, Mailer mailer, WfObjectBuilder wfObjectBuilder) {
        this.dataSource = dataSource;
        this.repository = repository;
        this.repositoryManager = repositoryManager;
        this.mailer = mailer;
        this.wfObjectBuilder = wfObjectBuilder;
    }

    @Override
    protected synchronized void startThread() {
      String threadName = getNextThreadName();
      Thread thread = new DaisyJobExecutorThread(threadName, this, jbpmConfiguration, idleInterval, maxIdleInterval,
              maxLockTime, historyMaxSize, dataSource, repository, repositoryManager, mailer, wfObjectBuilder);
      threads.put(threadName, thread);
//      log.debug("starting new job executor thread '"+threadName+"'");
      thread.start();
    }

}
