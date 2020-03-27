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
package org.outerj.daisy.doctaskrunner.serverimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.backuplock.spi.SuspendableProcess;
import org.outerj.daisy.credentialsprovider.CredentialsProvider;
import org.outerj.daisy.doctaskrunner.DocumentAction;
import org.outerj.daisy.doctaskrunner.DocumentActionFactory;
import org.outerj.daisy.doctaskrunner.DocumentExecutionState;
import org.outerj.daisy.doctaskrunner.DocumentSelection;
import org.outerj.daisy.doctaskrunner.Task;
import org.outerj.daisy.doctaskrunner.TaskContext;
import org.outerj.daisy.doctaskrunner.TaskDocDetail;
import org.outerj.daisy.doctaskrunner.TaskDocDetails;
import org.outerj.daisy.doctaskrunner.TaskException;
import org.outerj.daisy.doctaskrunner.TaskSpecification;
import org.outerj.daisy.doctaskrunner.TaskState;
import org.outerj.daisy.doctaskrunner.Tasks;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskDocDetailImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskDocDetailsImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TaskImpl;
import org.outerj.daisy.doctaskrunner.commonimpl.TasksImpl;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.JavascriptDocumentAction;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.ReplaceDocumentAction;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.SearchDocumentAction;
import org.outerj.daisy.doctaskrunner.serverimpl.actions.SimpleActions;
import org.outerj.daisy.doctaskrunner.spi.TaskSpecificationImpl;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.jdbcutil.SqlCounter;
import org.outerj.daisy.plugin.PluginHandle;
import org.outerj.daisy.plugin.PluginRegistry;
import org.outerj.daisy.plugin.PluginUser;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.VariantKey;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.user.Role;

public class CommonDocumentTaskManager implements SuspendableProcess {
    private DataSource dataSource;
    private PluginRegistry pluginRegistry;
    private PluginUser<DocumentActionFactory> pluginUser = new MyPluginUser();
    private JdbcHelper jdbcHelper;
    private Log log = LogFactory.getLog(getClass());
    private SqlCounter taskCounter;
    private final Map<Long, TaskHolder> tasksById = Collections.synchronizedMap(new HashMap<Long, TaskHolder>());
    private Map<String, DocumentActionFactory> documentActionFactories = new ConcurrentHashMap<String, DocumentActionFactory>(16, .75f, 1);
    private Map<String, String[]> allowedTaskRoles = new HashMap<String, String[]>();
    private ExtensionProvider extensionProvider = new MyExtensionProvider();
    private long taskJanitorTaskMaxAge;
    private long taskJanitorRunInterval;
    private Thread janitorThread;
    private ReadWriteLock suspendLock = new ReentrantReadWriteLock(true);
    private RepositoryManager repositoryManager;
    private Repository repository;
    private String repositoryKey;
    private CredentialsProvider credentialsProvider;
    private static final String EXTENSION_NAME = "DocumentTaskManager";
    private static final String SUSPEND_PROCESS_NAME = "Document Task Manager";
    
    private static final DocumentActionFactory JAVASCRIPT_ACTION_FACTORY = new DocumentActionFactory() {
		public DocumentAction create() throws TaskException {
			return new JavascriptDocumentAction();
		}
    };
    
    private static final DocumentActionFactory SIMPLE_ACTIONS_FACTORY = new DocumentActionFactory() {
		public DocumentAction create() throws TaskException {
			return new SimpleActions();
		}
    };
    
    private static final DocumentActionFactory SEARCH_ACTION_FACTORY = new DocumentActionFactory() {
		public DocumentAction create() throws TaskException {
			return new SearchDocumentAction();
		}
    };
    
    private static final DocumentActionFactory REPLACE_ACTION_FACTORY = new DocumentActionFactory() {
		public DocumentAction create() throws TaskException {
			return new ReplaceDocumentAction();
		}
    };
    
    public CommonDocumentTaskManager(Configuration configuration, DataSource dataSource,
            PluginRegistry pluginRegistry, RepositoryManager repositoryManager, CredentialsProvider credentialsProvider) throws Exception {
        this.dataSource = dataSource;
        this.pluginRegistry = pluginRegistry;
        this.repositoryManager = repositoryManager;
        this.credentialsProvider = credentialsProvider;

        configure(configuration);

        initialize();
        start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        stop();
        dispose();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        this.taskJanitorTaskMaxAge = configuration.getChild("taskJanitor").getAttributeAsLong("maxAge");
        this.taskJanitorRunInterval = configuration.getChild("taskJanitor").getAttributeAsLong("runInterval");
        Configuration allowedTasks = configuration.getChild("allowedTasks", false);
		if (allowedTasks != null) {
			List<String>roles = new ArrayList<String>();
			for (Configuration task: allowedTasks.getChildren("task")) {
				for (Configuration role: task.getChildren("role")) {
					roles.add(role.getValue());
				}
				allowedTaskRoles.put(task.getAttribute("name"), roles.toArray(new String[roles.size()]));
				roles.clear();
			}
        }
		this.repositoryKey = configuration.getChild("repositoryKey").getValue("internal");
    }

    private void initialize() throws Exception {
        pluginRegistry.setPluginUser(DocumentActionFactory.class, pluginUser);
        this.repository = this.repositoryManager.getRepository(credentialsProvider.getCredentials(repositoryKey));

        jdbcHelper = JdbcHelper.getInstance(dataSource, log);
        taskCounter = new SqlCounter("task_sequence", dataSource, log);

        markRunningTasksAsInterrupted();
        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);

        // The document task manager registers itself for suspending its active while
        // a backup is being taken. Only the actual execution of the actions will be
        // blocked while in suspended state, other operations will keep working
        // (including adding new document tasks).
        pluginRegistry.addPlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);
        
        pluginRegistry.addPlugin(DocumentActionFactory.class, "javascript", JAVASCRIPT_ACTION_FACTORY);
        pluginRegistry.addPlugin(DocumentActionFactory.class, "simple", SIMPLE_ACTIONS_FACTORY);
        pluginRegistry.addPlugin(DocumentActionFactory.class, "search", SEARCH_ACTION_FACTORY);
        pluginRegistry.addPlugin(DocumentActionFactory.class, "replace", REPLACE_ACTION_FACTORY);
    }

    private void dispose() {
        pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
        pluginRegistry.removePlugin(SuspendableProcess.class, SUSPEND_PROCESS_NAME, this);
        pluginRegistry.removePlugin(DocumentActionFactory.class, "javascript", JAVASCRIPT_ACTION_FACTORY);
        pluginRegistry.removePlugin(DocumentActionFactory.class, "simple", SIMPLE_ACTIONS_FACTORY);
        pluginRegistry.removePlugin(DocumentActionFactory.class, "search", SEARCH_ACTION_FACTORY);
        pluginRegistry.removePlugin(DocumentActionFactory.class, "replace", REPLACE_ACTION_FACTORY);
        pluginRegistry.unsetPluginUser(DocumentActionFactory.class, pluginUser);
    }

    private void start() throws Exception {
        resumeInterruptedTasks();
        janitorThread = new Thread(new ExpiredTasksJanitor(), "Daisy Expired Document Tasks Janitor");
        janitorThread.start();
    }

    private void stop() throws Exception {
        // Mark all running tasks as interrupted
        Collection<TaskHolder> currentTasks;
        synchronized (tasksById) {
            currentTasks = new ArrayList<TaskHolder>(tasksById.values());
        }
        if (currentTasks.size() > 0) {
            log.info("Interrupting " + currentTasks.size() + " running document tasks.");
            for (TaskHolder taskHolder : currentTasks) {
                log.info("Interrupting document task " + taskHolder.getTaskContext().taskId);
                taskHolder.getTaskContext().interrupt(TaskState.INTERRUPTED_BY_SHUTDOWN);
            }
        }

        // Stop the janitor thread
        log.info("Waiting for document task janitor thread to end.");
        janitorThread.interrupt();
        try {
            janitorThread.join();
            log.info("Document task janitor thread ended.");
        } catch (InterruptedException e) {
            // ignore
        }

        // Shut down current executors and wait for them to die
        // This may take a while.  We probably should do this concurrently for each task.
        for (TaskHolder taskHolder : currentTasks) {
        	ExecutorService es = taskHolder.getExecutorService();
        	es.shutdown();
    	   try {
    	     // Wait a while for existing tasks to terminate
             log.info("Waiting for document task " + taskHolder.getTaskContext().taskId + " to be terminated.");
    	     if (!es.awaitTermination(60, TimeUnit.SECONDS)) {
    	       es.shutdownNow(); // Cancel currently executing tasks
    	       // Wait a while for tasks to respond to being cancelled
    	       if (!es.awaitTermination(60, TimeUnit.SECONDS))
    	           System.err.println("Document task " + taskHolder.getTaskContext().taskId + " did not terminate.");
    	     }
    	   } catch (InterruptedException ie) {
    	     // (Re-)Cancel if current thread also interrupted
    	     es.shutdownNow();
    	     // Preserve interrupt status
    	     Thread.currentThread().interrupt();
    	   }
        }
    }

    class MyExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new DocumentTaskManagerImpl(CommonDocumentTaskManager.this, repository);
        }
    }

    public boolean suspendExecution(long msecs) throws InterruptedException {
        return suspendLock.writeLock().tryLock(msecs, TimeUnit.MILLISECONDS);
    }

    public void resumeExecution() {
        suspendLock.writeLock().unlock();
    }

    private void markRunningTasksAsInterrupted() throws Exception {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("update document_tasks set state = ? where state = ? or state = ?");
            stmt.setString(1, TaskState.INTERRUPTED_BY_SHUTDOWN.getCode());
            stmt.setString(2, TaskState.INITIALISING.getCode());
            stmt.setString(3, TaskState.RUNNING.getCode());
            int updatedRows = stmt.executeUpdate();
            if (log.isDebugEnabled())
                log.debug("Number of tasks marked as 'interrupted by shutdown': " + updatedRows);
        } catch (Throwable e) {
            throw new Exception("Error while marking tasks as 'interrupted by shutdown'", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public long runTask(DocumentSelection documentSelection, TaskSpecification taskSpecification, Repository repository) throws TaskException {
        long taskId;
        
        String actionType = taskSpecification.getActionType();
		if (!documentActionFactories.containsKey(actionType)) {
            throw new TaskException("Document action type '" + actionType + "' is not registered" );
        }
		if (!isTaskAllowed(actionType, repository)) {
			throw new TaskException("You don't have any of the required roles for the action type '" + actionType + "'");
		}
		VariantKey[] keys;
		try {
		    keys = documentSelection.getKeys(repository);
		} catch (RepositoryException re) {
		    throw new TaskException("Cannot obtain initial variantKeys", re);
		}
		
		DocumentAction documentAction = documentActionFactories.get(actionType).create();

        {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                taskId = taskCounter.getNextId();
                java.util.Date now = new java.util.Date();
                
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("insert into document_tasks(id, owner, state, started_at, progress, try_count, max_tries, retry_interval, description, action_type, action_parameters) values(?,?,?,?,?,?,?,?,?,?,?)");
                stmt.setLong(1, taskId);
                stmt.setLong(2, repository.getUserId());
                stmt.setString(3, TaskState.INITIALISING.getCode());
                stmt.setTimestamp(4, new Timestamp(now.getTime()));
                stmt.setString(5, "initializing");
                stmt.setInt(6, 0); // we haven't tried anything yet
                stmt.setInt(7, taskSpecification.getMaxTryCount());
                stmt.setInt(8, taskSpecification.getRetryInterval());
                stmt.setString(9, taskSpecification.getDescription());
                stmt.setString(10, actionType);
                stmt.setString(11, taskSpecification.getParameters());
                stmt.execute();

                TaskContextImpl taskContext = createTaskContext(taskId);
                try {
                    internalRunTask(keys, keys.length, taskSpecification, repository,
                            taskContext, documentAction, false, 0, false);
                } catch (Throwable t) {
                    cleanupFailedStart(taskContext, t);
                    throw new TaskException("Problem starting task.", t);
                }
                
            } catch (Throwable e) {
                throw new TaskException("Error inserting task record.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }
        

        return taskId;
    }

    private void cleanupFailedStart(TaskContextImpl taskContext, Throwable reason) throws TaskException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("delete from document_tasks where id = ?");
            stmt.setLong(1, taskContext.taskId);
            stmt.execute();
        } catch (Exception e2) {
            throw new TaskException("Problem starting task and problem cleaning up afterwards: " + reason.toString(), e2);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }
    
    private void cleanupFailedResume(TaskContextImpl taskContext, Throwable reason) throws TaskException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            taskContext.setTaskState(TaskState.INTERRUPTED_BY_ERROR, "Failed to resume task after iterruption: " + reason.getMessage());
        } catch (Exception e) {
            throw new TaskException("Problem starting task and problem cleaning up afterwards: " + reason.toString(), e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }
    
    private TaskContextImpl createTaskContext(long taskId) {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
        
        executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
        executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        return new TaskContextImpl(taskId, executor);
    }

    private void internalRunTask(VariantKey[] remainingVariantKeys, int initialDocumentSelectionSize,
            TaskSpecification taskSpecification, Repository repository,
            TaskContextImpl taskContext, DocumentAction documentAction, boolean skipContextInitResult, int initialTryCount, boolean initialHasErrors) throws TaskException {
        long taskId = taskContext.taskId;
        try {
            TaskRunner taskRunner = new TaskRunner(documentAction, remainingVariantKeys, initialDocumentSelectionSize, taskSpecification, taskContext, skipContextInitResult, repository, initialTryCount, initialHasErrors);            
            
            ScheduledExecutorService executor = taskContext.getExecutor();
            TaskHolder taskHolder = new TaskHolder(taskRunner, taskContext, repository.getUserId(), executor);
            tasksById.put(new Long(taskId), taskHolder);
            executor.execute(taskRunner); // the first time round no need to schedule anything, run it immediately 

        } catch (Throwable e) {
            tasksById.remove(new Long(taskId));
            throw new TaskException("Problem running task", e);
        }
    }

    static class TaskHolder {
        private final TaskRunner taskRunner;
        private final TaskContextImpl taskContext;
        private final long ownerId;
        private final ExecutorService executorService;

        public TaskHolder(TaskRunner taskRunner, TaskContextImpl taskContext, long ownerId, ExecutorService executorService) {
            this.taskRunner = taskRunner;
            this.taskContext = taskContext;
            this.ownerId = ownerId;
            this.executorService = executorService;
        }

        public TaskRunner getTaskRunner() {
            return taskRunner;
        }

        public TaskContextImpl getTaskContext() {
            return taskContext;
        }

        public long getOwnerId() {
            return ownerId;
        }

        public ExecutorService getExecutorService() {
            return executorService;
        }
    }

    class TaskContextImpl implements TaskContext {
        private TaskState interruptedReason = null;
        private long taskId;
        private ScheduledExecutorService executor;

        public TaskContextImpl(long taskId, ScheduledExecutorService executor) {
            this.taskId = taskId;
            this.executor = executor;
        }

        public synchronized void interrupt(TaskState reason) {
            if (this.interruptedReason == null) {
                this.interruptedReason = reason;
            } else {
                // if already interrupted, silently return
            }
        }

        public boolean isInterrupted() {
            return interruptedReason != null;
        }

        public TaskState getInterruptedReason() {
            return interruptedReason;
        }

        public void setProgress(String progress) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("update document_tasks set progress = ? where id = ?");
                stmt.setString(1, progress);
                stmt.setLong(2, taskId);
                stmt.execute();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected error trying to update task progress.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

        public void initDocumentResults(VariantKey[] keys, Repository repository) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                jdbcHelper.startTransaction(conn);

                stmt = conn.prepareStatement("insert into task_doc_details(task_id, doc_id, branch_id, lang_id, seqnr, state) values(?,?,?,?,?,?)");
                stmt.setLong(1, taskId);
                stmt.setString(6, DocumentExecutionState.WAITING.getCode());

                for (int i = 0; i < keys.length; i++) {
                    VariantKey variantKey = keys[i];
                    // 1 already set
                    stmt.setString(2, variantKey.getDocumentId());
                    stmt.setLong(3, variantKey.getBranchId());
                    stmt.setLong(4, variantKey.getLanguageId());
                    stmt.setLong(5, i);
                    // 6 already set
                    stmt.execute();
                }
                conn.commit();
            } catch (Throwable e) {
                jdbcHelper.rollback(conn);
                throw new RuntimeException("Unexpected error trying to initialise document states.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

        public void setDocumentResult(VariantKey key, DocumentExecutionState state, String details, int tryCount) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("update task_doc_details set state = ?, details = ?, try_count = ? where task_id = ? and doc_id = ? and branch_id = ? and lang_id = ?");
                stmt.setString(1, state.getCode());
                stmt.setString(2, details);
                stmt.setInt(3, tryCount);
                stmt.setLong(4, taskId);
                stmt.setString(5, key.getDocumentId());
                stmt.setLong(6, key.getBranchId());
                stmt.setLong(7, key.getLanguageId());
                stmt.execute();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected error trying to update document state.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }
        
        public void setTaskState(TaskState state, String details) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("update document_tasks set state = ?, details = ? where id = ?");
                stmt.setString(1, state.getCode());
                stmt.setString(2, details);
                stmt.setLong(3, taskId);
                stmt.execute();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected error trying to update task state.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

        public void setTaskState(TaskState state, String progress, String details, int tryCount) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("update document_tasks set state = ?, progress = ?, details = ?, finished_at = ?, try_count = ? where id = ?");
                stmt.setString(1, state.getCode());
                stmt.setString(2, progress);
                stmt.setString(3, details);
                stmt.setTimestamp(4, state.isStoppedState() ? new Timestamp(System.currentTimeMillis()) : null);
                stmt.setInt(5, tryCount);
                stmt.setLong(6, taskId);
                stmt.execute();
            } catch (Throwable e) {
                throw new RuntimeException("Unexpected error trying to update task state.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

        public void cleanup() {
            tasksById.remove(new Long(taskId));
        }

        public Lock getExecutionLock() {
            return suspendLock.readLock();
        }

        public ScheduledExecutorService getExecutor() {
            return executor;
        }

        public void setExecutor(ScheduledExecutorService executor) {
            this.executor = executor;            
        }
        
        
    }

    private static final String SELECT_TASK = "select id, action_type, owner, started_at, finished_at, state, progress, description, action_parameters, details, try_count, max_tries, retry_interval from document_tasks";

    public Task getTask(long taskId, Repository repository) throws TaskException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(SELECT_TASK + " where id = ?");
            stmt.setLong(1, taskId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new TaskException("No task found with ID " + taskId);

            if (!repository.isInRole(Role.ADMINISTRATOR) && rs.getLong("owner") != repository.getUserId())
                throw new TaskException("Access denied to task with ID " + taskId);

            return getTaskFromResultSet(rs);
        } catch (Throwable e) {
            if (e instanceof TaskException)
                throw (TaskException)e;

            throw new TaskException("Error loading task with ID " + taskId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public Tasks getTasks(Repository repository) throws TaskException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            StringBuilder query = new StringBuilder(SELECT_TASK);
            if (!repository.isInRole(Role.ADMINISTRATOR))
                query.append(" where owner = ?");
            stmt = conn.prepareStatement(query.toString());
            if (!repository.isInRole(Role.ADMINISTRATOR))
                stmt.setLong(1, repository.getUserId());
            ResultSet rs = stmt.executeQuery();

            List<Task> tasks = new ArrayList<Task>();

            while (rs.next()) {
                tasks.add(getTaskFromResultSet(rs));
            }

            return new TasksImpl(tasks.toArray(new Task[tasks.size()]));
        } catch (Throwable e) {
            throw new TaskException("Error loading tasks.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    private TaskImpl getTaskFromResultSet(ResultSet rs) throws SQLException {
        long taskId = rs.getLong("id");
        String description = rs.getString("description");
        TaskState state = TaskState.getByCode(rs.getString("state"));
        long ownerId = rs.getLong("owner");
        String progress = rs.getString("progress");
        String details = rs.getString("details");
        String actionType = rs.getString("action_type");
        String actionParameters = rs.getString("action_parameters");
        Date startedAt = rs.getTimestamp("started_at");
        Date finishedAt = rs.getTimestamp("finished_at");
        int tryCount = rs.getInt("try_count");
        int maxTries = rs.getInt("max_tries");
        int retryInterval = rs.getInt("retry_interval");

        TaskImpl task = new TaskImpl(taskId, description, state, ownerId, progress, details, actionType, actionParameters,
                startedAt, finishedAt, tryCount, maxTries, retryInterval);

        return task;
    }

    public void deleteTask(long taskId, Repository repository) throws TaskException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            jdbcHelper.startTransaction(conn);

            stmt = conn.prepareStatement("select state, owner from document_tasks where id = ? " + jdbcHelper.getSharedLockClause());
            stmt.setLong(1, taskId);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next())
                throw new TaskException("No task found with ID " + taskId);

            if (!repository.isInRole(Role.ADMINISTRATOR) && rs.getLong("owner") != repository.getUserId())
                throw new TaskException("Access denied to task with ID " + taskId);

            TaskState state = TaskState.getByCode(rs.getString("state"));
            if (state == TaskState.INITIALISING || state == TaskState.RUNNING)
                throw new TaskException("Cannot delete task with ID " + taskId + " because it has not yet ended.");

            stmt.close();

            deleteTask(taskId, conn);

            conn.commit();
        } catch (Throwable e) {
            jdbcHelper.rollback(conn);
            if (e instanceof TaskException)
                throw (TaskException)e;

            throw new TaskException("Problem deleting task with ID " + taskId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    /**
     * Pefroms actual deletion of task, assumes necessary locks are taken and transaction is started.
     */
    private void deleteTask(long taskId, Connection conn) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement("delete from task_doc_details where task_id = ?");
            stmt.setLong(1, taskId);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("delete from document_tasks where id = ?");
            stmt.setLong(1, taskId);
            stmt.execute();
            stmt.close();
        } finally {
            jdbcHelper.closeStatement(stmt);
        }

    }

    public void interruptTask(long taskId, Repository repository) throws TaskException {
        Long taskKey = new Long(taskId);
        TaskHolder taskHolder = tasksById.get(taskKey);

        if (taskHolder == null)
            throw new TaskException("There is no task running with ID " + taskId);

        if (!repository.isInRole(Role.ADMINISTRATOR) && taskHolder.getOwnerId() != repository.getUserId())
            throw new TaskException("You are not allowed to interrupt the task with ID " + taskId);

        taskHolder.getTaskContext().interrupt(TaskState.INTERRUPTED_BY_USER);
    }

    public void resumeInterruptedTasks() throws Exception {
        // NOTE: when resuming we reprocess all documents of the last started try (i.e. we don't resume after the last processed document.
        // hence, documents that had already failed during the interrupted try are reprocessed (resulting in documents that are tried more than maxTries)

        Connection conn = null;
        PreparedStatement selectTaskStmt = null;
        PreparedStatement selectTaskDocDetailsStmt = null;
        PreparedStatement countTaskDocDetailsStmt = null;
        PreparedStatement countErrorDocsStmt = null;
        try {
            conn = dataSource.getConnection();
            selectTaskStmt = conn.prepareStatement("select id, owner, description, action_type, action_parameters, try_count from document_tasks where state = ?");
            selectTaskStmt.setString(1, TaskState.INTERRUPTED_BY_SHUTDOWN.getCode());
            
            selectTaskDocDetailsStmt = conn.prepareStatement("select doc_id, branch_id, lang_id from task_doc_details where task_id = ? and state in (?, ?)");
            selectTaskDocDetailsStmt.setString(2, DocumentExecutionState.WAITING.getCode());
            selectTaskDocDetailsStmt.setString(3, DocumentExecutionState.FAIL.getCode());

            countTaskDocDetailsStmt = conn.prepareStatement("select count(*) from task_doc_details where task_id = ?");
            countErrorDocsStmt = conn.prepareStatement("select count(*) from task_doc_details where task_id = ? and state = ?");
            countErrorDocsStmt.setString(2, DocumentExecutionState.ERROR.getCode());

            ResultSet tasks = selectTaskStmt.executeQuery();
            while (tasks.next()) {
                Long taskId = tasks.getLong("id");
                Long userId = tasks.getLong("owner");
                int tryCount = tasks.getInt("try_count");
                String description = tasks.getString("description");
                String actionType = tasks.getString("action_type");
                String actionParameters = tasks.getString("action_parameters");
                boolean stopOnFirstError = false;
                
                TaskContextImpl taskContext = createTaskContext(taskId);
                try {
                    taskContext.setTaskState(TaskState.RESUMING_AFTER_SHUTDOWN, "resuming after shutdown");
    
                    DocumentAction documentAction = documentActionFactories.get(actionType).create();
    
                    countTaskDocDetailsStmt.setLong(1, taskId);
                    ResultSet countResult = countTaskDocDetailsStmt.executeQuery();
                    countResult.next();
                    int totalKeys = countResult.getInt(1);
                    
                    List<VariantKey> remainingVariantKeys = new ArrayList<VariantKey>();
                    selectTaskDocDetailsStmt.setLong(1, taskId);
                    
                    ResultSet variants = selectTaskDocDetailsStmt.executeQuery();
                    while (variants.next()) {
                        String docId = variants.getString(1);
                        long branchId = variants.getLong(2);
                        long langId = variants.getLong(3);
                        remainingVariantKeys.add(new VariantKey(docId, branchId, langId));
                    }
                    
                    TaskSpecification taskSpec = new TaskSpecificationImpl(description, actionType, actionParameters, stopOnFirstError);
                    
                    Repository userRepository = repository.getRepositoryManager().getRepositoryAsUser(repository.getUserManager().getUser(userId, false));
                    
                    countErrorDocsStmt.setLong(1, taskId);
                    ResultSet rs = countErrorDocsStmt.executeQuery();
                    rs.next();
                    boolean initialHasErrors = rs.getInt(1) > 0;
                    
                    internalRunTask(remainingVariantKeys.toArray(new VariantKey[remainingVariantKeys.size()]), totalKeys, taskSpec, userRepository,
                            taskContext, documentAction, true, tryCount - 1, initialHasErrors);
                } catch (Throwable t) {
                    cleanupFailedResume(taskContext, t);
                    log.warn("Failed to resume task " + taskContext.taskId, t);
                }
            }
            
        } catch (Throwable e) {
            throw new Exception("Error while resuming tasks in state 'interrupted by shutdown' (Some may have resumed, some may not)", e);
        } finally {
            jdbcHelper.closeStatement(selectTaskDocDetailsStmt);
            jdbcHelper.closeStatement(selectTaskStmt);
            jdbcHelper.closeStatement(countTaskDocDetailsStmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    public TaskDocDetails getTaskDocDetails(long taskId, Repository repository) throws TaskException {
        // Do a call to getTask so that existence and access permissions are verified
        getTask(taskId, repository);

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("select doc_id, branch_id, lang_id, state, details, try_count from task_doc_details where task_id = ? order by seqnr");
            stmt.setLong(1, taskId);
            ResultSet rs = stmt.executeQuery();

            List<TaskDocDetail> taskDocDetails = new ArrayList<TaskDocDetail>();
            while (rs.next()) {
                VariantKey variantKey = new VariantKey(rs.getString("doc_id"), rs.getLong("branch_id"), rs.getLong("lang_id"));
                DocumentExecutionState state = DocumentExecutionState.getByCode(rs.getString("state"));
                String details = rs.getString("details");
                int tryCount = rs.getInt("try_count");
                taskDocDetails.add(new TaskDocDetailImpl(variantKey, state, details, tryCount));
            }
            
            return new TaskDocDetailsImpl(taskDocDetails.toArray(new TaskDocDetail[taskDocDetails.size()]));
        } catch (Throwable e) {
            throw new TaskException("Error retrieving task document details for task " + taskId, e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            jdbcHelper.closeConnection(conn);
        }
    }

    class ExpiredTasksJanitor implements Runnable {
        public void run() {
            try {
                while (true) {
                    if (Thread.interrupted())
                        return;

                    Thread.sleep(taskJanitorRunInterval);

                    Connection conn = null;
                    PreparedStatement stmt = null;
                    try {
                        conn = dataSource.getConnection();
                        jdbcHelper.startTransaction(conn);

                        // Note: the search is performed on started_at and not on finished_at because finished_at may
                        // not always have a value (e.g. when the task was interrupted by shutdown)
                        stmt = conn.prepareStatement("select id from document_tasks where started_at < ? and state not in ('" + TaskState.INITIALISING.getCode() + "', '" + TaskState.RUNNING.getCode() + "') " + jdbcHelper.getSharedLockClause());
                        stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis() - taskJanitorTaskMaxAge));
                        ResultSet rs = stmt.executeQuery();
                        List<Long> taskIds = new ArrayList<Long>();
                        while (rs.next()) {
                            taskIds.add(new Long(rs.getLong(1)));
                        }
                        stmt.close();

                        for (Long taskId : taskIds) {
                            deleteTask(taskId, conn);
                        }

                        conn.commit();
                    } catch (Throwable e) {
                        jdbcHelper.rollback(conn);
                        log.error("Expired tasks janitor: error while performing my job.", e);
                    } finally {
                        jdbcHelper.closeStatement(stmt);
                        jdbcHelper.closeConnection(conn);
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                log.debug("Expired document task janitor thread ended.");
            }
        }
    }
    
    public boolean actionTypeExists(String actionType) {
        return documentActionFactories.containsKey(actionType);
    }
    
    public String[] getAllowedTasks(Repository repository) {
    	List<String> result = new ArrayList<String>();
    	for (String name: documentActionFactories.keySet()) {
    		if (isTaskAllowed(name, repository)) {
    			result.add(name);
    		}
    	}
    	
    	return result.toArray(new String[result.size()]);
    }

	private boolean isTaskAllowed(String name, Repository repository) {
		if (repository.isInRole(Role.ADMINISTRATOR))
			return true;
		if (!allowedTaskRoles.containsKey(name)) {
			return false;
		}
		for (String role: allowedTaskRoles.get(name)) {
			if (repository.isInRole(role)) {
				return true;
			}
		}
		return false;
	}

	private class MyPluginUser implements PluginUser<DocumentActionFactory> {
		public void pluginAdded(PluginHandle<DocumentActionFactory> pluginHandle) {
			documentActionFactories.put(pluginHandle.getName(), pluginHandle.getPlugin());
		}
	
		public void pluginRemoved(PluginHandle<DocumentActionFactory> pluginHandle) {
			documentActionFactories.remove(pluginHandle.getName());
		}
	}

}
