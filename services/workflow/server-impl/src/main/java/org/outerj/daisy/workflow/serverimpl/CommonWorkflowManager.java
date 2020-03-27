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
package org.outerj.daisy.workflow.serverimpl;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.jbpm.context.exe.ContextInstance;
import org.jbpm.taskmgmt.exe.TaskInstance;
import org.jbpm.taskmgmt.exe.SwimlaneInstance;
import org.jbpm.taskmgmt.def.Task;
import org.jbpm.db.GraphSession;
import org.jbpm.db.TaskMgmtSession;
import org.jbpm.jpdl.par.ProcessArchive;
import org.jbpm.jpdl.xml.JpdlXmlReader;
import org.jbpm.jpdl.xml.Problem;
import org.jbpm.jpdl.xml.ProblemListener;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.graph.exe.Token;
import org.jbpm.persistence.db.DbPersistenceServiceFactory;
import org.jbpm.svc.Services;
import org.jbpm.job.Timer;
import org.jbpm.job.executor.DaisyJobExecutor;
import org.jbpm.job.executor.JobExecutor;
import org.outerj.daisy.repository.*;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.workflow.*;
import org.outerj.daisy.workflow.serverimpl.query.*;
import org.outerj.daisy.workflow.commonimpl.WfPoolManagerImpl;
import org.outerj.daisy.workflow.commonimpl.WfPoolStrategy;
import org.outerj.daisy.workflow.commonimpl.WfVariableDefinitionImpl;
import org.outerj.daisy.workflow.commonimpl.WfVariableImpl;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.plugin.PluginRegistry;
import org.xml.sax.InputSource;
import org.hibernate.Session;
import org.hibernate.Query;
import org.outerx.daisy.x10Workflow.SearchResultDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument;
import org.outerx.daisy.x10Workflowmeta.WorkflowAclInfoDocument.WorkflowAclInfo;
import org.mozilla.javascript.*;

import javax.sql.DataSource;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.zip.ZipInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

public class CommonWorkflowManager {
    private Log log = LogFactory.getLog(getClass());
    private Properties jbpmHibernateProperties;
    private DataSource dataSource;
    private RepositoryManager repositoryManager;
    private JbpmConfiguration jbpmConfiguration;
    private JdbcHelper jdbcHelper;
    private String jbpmConfigPath;
    private PluginRegistry pluginRegistry;
    private WorkflowExtensionProvider extensionProvider;
    private WorkflowMetaManager wfMetaManager = new WorkflowMetaManager();
    private WfObjectBuilder wfBuilder;
    private WfPoolStrategy wfPoolStrategy;
    private QueryMetadataRegistry queryMetadataRegistry;
    private WorkflowAuthorizer wfAuthorizer;
    private String[] mailTemplateLocations;
    private String taskURL;
    private WfMailer wfMailer;
    private String workflowUserLogin;
    private String workflowUserPassword;
    /**
     * Indicates if the workflow is enabled, i.o.w. initialized and registered as extension.
     * Usually this is always the case, except if the workflow user is not configured.
     * (Ideally, the component container should be able to handle skipping over a failed
     *  initialised extension component)
     */
    private boolean enabled = false;
    private static final Set<String> ZIP_MIME_TYPES;
    static {
        ZIP_MIME_TYPES = new HashSet<String>();
        ZIP_MIME_TYPES.add("application/zip");
        ZIP_MIME_TYPES.add("application/x-zip");
        ZIP_MIME_TYPES.add("application/x-zip-compressed");
    }
    private static final String CREATE_SCHEMA_MESSAGE = "Creating jBPM (workflow engine) database schema. Please be patient...";
    private static final String CREATE_SCHEMA_DONE_MESSAGE = "jBPM schema creation finished.";
    private static final String JBPM_SCHEMA_VERSION_KEY = "jbpm_schema_version";
    private static final String JBPM_CURRENT_SCHEMA_VERSION = "3.2";
    private static final String EXTENSION_NAME = "WorkflowManager";

    public CommonWorkflowManager(Configuration configuration, DataSource dataSource,
            PluginRegistry pluginRegistry, RepositoryManager repositoryManager,
            WorkflowAuthorizer workflowAuthorizer) throws Exception {
        this.dataSource = dataSource;
        this.pluginRegistry = pluginRegistry;
        this.repositoryManager = repositoryManager;
        this.wfAuthorizer = workflowAuthorizer;
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
        Configuration[] properties = configuration.getChild("jbpm").getChild("hibernate").getChild("properties").getChildren("entry");
        jbpmHibernateProperties = new Properties();
        for (Configuration property : properties) {
            jbpmHibernateProperties.put(property.getAttribute("key"), property.getValue());
        }

        jbpmConfigPath = configuration.getChild("jbpm").getChild("jbpm-config").getValue(null);

        // workflow user
        Configuration workflowUserConf = configuration.getChild("workflowUser");
        workflowUserLogin = workflowUserConf.getAttribute("login", null);
        if (workflowUserLogin != null)
            workflowUserPassword = workflowUserConf.getAttribute("password");

        // mail template locations
        Configuration[] mailTemplateLocationConfigs = configuration.getChild("mailTemplates").getChildren("location");
        mailTemplateLocations = new String[mailTemplateLocationConfigs.length];
        for (int i = 0; i < mailTemplateLocationConfigs.length; i++) {
            mailTemplateLocations[i] = PropertyResolver.resolveProperties(mailTemplateLocationConfigs[i].getValue());
        }

        // task URL
        taskURL = configuration.getChild("taskURL").getValue(null);
    }

    private void initialize() throws Exception {
        // Create JdbcHelper
        jdbcHelper = JdbcHelper.getInstance(dataSource, log);

        //
        // Check if the workflow user exists. If not, we don't start the workflow or register the workflow extension.
        // This special behaviour is to be forgiving towards users who upgrade from pre-workflow Daisy versions.
        // Note that wrong credentials will still cause a fatal error.
        //
        boolean loginSucceeded = false;
        boolean hasAdminRole = false;
        if (workflowUserLogin != null) {
            // try to log in with workflow user
            try {
                Repository repository = repositoryManager.getRepository(new Credentials(workflowUserLogin, workflowUserPassword));
                loginSucceeded = true;

                for (long role : repository.getActiveRoleIds()) {
                    if (role == Role.ADMINISTRATOR) {
                        hasAdminRole = true;
                        break;
                    }
                }
            } catch (UserNotFoundException e) {
                // ok
            }
        }

        if (!loginSucceeded) {
            if (workflowUserLogin == null) {
                log.error("No workflow user is configured. The WorkflowManager extension will not be available.");
                System.err.println();
                System.err.println("======================= ERROR =====================");
                System.err.println("No workflow user is configured.");
                System.err.println("The WorkflowManager extension will not be available.");
                System.err.println("====================================================");
                System.err.println();
            } else {
                log.error("The configured workflow user does not exist in the system (" + workflowUserLogin + "). The WorkflowManager extension will not be available.");
                System.err.println();
                System.err.println("========================== ERROR =========================");
                System.err.println("The configured workflow user does not exist in the system.");
                System.err.println("Workflow user login: " + workflowUserLogin);
                System.err.println("The WorkflowManager extension will not be available.");
                System.err.println("==========================================================");
                System.err.println();
            }
            return;
        }

        if (!hasAdminRole)
            throw new WorkflowException("The workflow user (" + workflowUserLogin + ") does not have the Administrator role.");

        // workflow user exists, enable ourselves
        enabled = true;

        // Create some other objects
        wfBuilder = new WfObjectBuilder(wfMetaManager);
        wfPoolStrategy = new LocalWfPoolStrategy(dataSource);
        queryMetadataRegistry = new QueryMetadataRegistry();
        queryMetadataRegistry.init();

        // Create JbpmConfiguration
        DaisyDbPersistenceServiceFactory.HIBERNATE_PROPERTIES.set(jbpmHibernateProperties);
        InputStream is = null;
        try {
            if (jbpmConfigPath != null) {
                is = new FileInputStream(jbpmConfigPath);
            } else {
                is = getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/workflow/serverimpl/jbpm.cfg.xml");
            }
            jbpmConfiguration = JbpmConfiguration.parseInputStream(is);
        } finally {
            if (is != null)
                is.close();
        }
        JbpmContext jbpmContext = jbpmConfiguration.createJbpmContext();
        jbpmContext.close();

        assureJbpmSchemaExists();

        // unset the properties
        DaisyDbPersistenceServiceFactory.HIBERNATE_PROPERTIES.set(null);

        // Create mailer component
        try {
            this.wfMailer = new WfMailer(mailTemplateLocations, taskURL, getWfRepository());
        } catch (Exception e) {
            throw new WorkflowException("Error with workflow mail template locations.", e);
        }

        // Make the workflow extension available
        extensionProvider = new WorkflowExtensionProvider();
        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private void dispose() {
        if (extensionProvider != null)
            pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private void start() throws Exception {
        if (enabled) {
            // Init JobExecutor
            Repository repository = getWfRepository();

            JobExecutor jobExecutor = jbpmConfiguration.getJobExecutor();
            if (jobExecutor instanceof DaisyJobExecutor) {
                ((DaisyJobExecutor)jobExecutor).daisyInit(dataSource, repository, repositoryManager, wfMailer, wfBuilder);
            } else {
                log.error("Important warning: the configured JobExecutor is not the DaisyJobExecutor. If this is not intended, this should be fixed.");
            }

            jbpmConfiguration.startJobExecutor();
        }
    }

    private void stop() throws Exception {
        if (enabled) {
            jbpmConfiguration.getJobExecutor().stopAndJoin();
        }
    }

    private Repository getWfRepository() throws WorkflowException {
        try {
            Repository repository = repositoryManager.getRepository(new Credentials(workflowUserLogin, workflowUserPassword));
            repository.switchRole(Role.ADMINISTRATOR);
            return repository;
        } catch (RepositoryException e) {
            throw new WorkflowException("Error getting workflow user repository.", e);
        }
    }

    private void assureJbpmSchemaExists() throws WorkflowException {
        PreparedStatement stmt = null;
        JbpmContext jbpmContext = getJbpmContext(null);
        try {
            Connection conn = jbpmContext.getConnection();
            stmt = conn.prepareStatement("select propvalue from daisy_system where propname = '" + JBPM_SCHEMA_VERSION_KEY + "'");
            ResultSet rs = stmt.executeQuery();
            boolean createSchema = false;
            if (rs.next()) {
                String installedJbpmSchemaVersion = rs.getString("propvalue");
                if (!installedJbpmSchemaVersion.equals(JBPM_CURRENT_SCHEMA_VERSION)) {
                    throw new WorkflowException("Installed jBPM schema version does not match expected version. Found: " + installedJbpmSchemaVersion + ", expected: " + JBPM_CURRENT_SCHEMA_VERSION);
                }
            } else {
                createSchema = true;
            }
            stmt.close();

            if (createSchema) {
                // Creating the schema takes some time, warn user about this
                log.warn(CREATE_SCHEMA_MESSAGE);
                System.out.println(CREATE_SCHEMA_MESSAGE);
                System.out.flush();

                Services services = jbpmContext.getServices();
                DbPersistenceServiceFactory persistenceServiceFactory = (DbPersistenceServiceFactory) services.getServiceFactory(Services.SERVICENAME_PERSISTENCE);
                persistenceServiceFactory.createSchema();

                // Schema successfully created, record this fact in the daisy_system table
                stmt = conn.prepareStatement("insert into daisy_system(propname, propvalue) values(?,?)");
                stmt.setString(1, JBPM_SCHEMA_VERSION_KEY);
                stmt.setString(2, JBPM_CURRENT_SCHEMA_VERSION);
                stmt.execute();
                stmt.close();

                //
                // Deploy some sample workflows
                //
                jbpmContext.setActorId("1"); // system user
                internalLoadSampleWorkflows(jbpmContext);

                log.warn(CREATE_SCHEMA_DONE_MESSAGE);
                System.out.println(CREATE_SCHEMA_DONE_MESSAGE);
                System.out.flush();
            }
        } catch (Throwable e) {
            jbpmContext.setRollbackOnly();
            throw new WorkflowException("Error checking or installing workflow database schema or workflow samples.", e);
        } finally {
            jdbcHelper.closeStatement(stmt);
            closeJbpmContext(jbpmContext);
        }
    }

    public WfPoolManager getPoolManager(Repository repository) {
        return new WfPoolManagerImpl(repository, wfPoolStrategy);
    }

    public WfProcessDefinition deployProcessDefinition(final InputStream is, final String mimeType, final Locale locale,
            final Repository repository) throws WorkflowException {
        if (is == null)
            throw new IllegalArgumentException("Null argument: is");
        if (mimeType == null)
            throw new IllegalArgumentException("Null argument: mimeType");

        return (WfProcessDefinition)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                return internalDeploy(is, mimeType, jbpmContext, locale, repository);
            }

            public String getErrorMessage() {
                return "Error deploying process definition.";
            }
        });
    }

    @SuppressWarnings("unchecked")
    private WfProcessDefinition internalDeploy(InputStream is, String mimeType, JbpmContext jbpmContext,
            Locale locale, Repository repository) throws WorkflowException {
        // Load/parse the process definition
        ProcessDefinition processDefinition;
        final List<Problem> problems;
        if (ZIP_MIME_TYPES.contains(mimeType)) {
            ProcessArchive reader;
            try {
                reader = new ProcessArchive(new ZipInputStream(is));
            } catch (IOException e) {
                throw new WorkflowException("Error reading process archive.", e);
            }
            processDefinition = reader.parseProcessDefinition();
            problems = reader.getProblems();
        } else if (mimeType.equals("text/xml")) {
            problems = new ArrayList<Problem>();
            JpdlXmlReader jpdlReader = new JpdlXmlReader(new InputSource(is), new ProblemListener() {
                public void addProblem(Problem problem) {
                    problems.add(problem);
                }
            });
            processDefinition = jpdlReader.readProcessDefinition();
        } else {
            throw new WorkflowException("Error deploying workflow: unsupported workflow definition mime type: " + mimeType);
        }

        // Check if we find this process definition acceptable for use in Daisy
        // (throws an exception if not)
        ProcessDefinitionVerifier.verify(processDefinition, wfMetaManager, wfBuilder);

        // Check authorization -- repository can be null in case of 'internal deploys', i.e. for the
        //                        installation of the sample workflows
        if (repository != null && !wfAuthorizer.canDeployProcessDefinition(processDefinition, repository)) {
            throw new WfAuthorizationException("You are not allowed to deploy this workflow definition.");
        }

        // Actually deploy the process definition
        jbpmContext.deployProcessDefinition(processDefinition);

        List<String> problemsAsStrings = new ArrayList<String>(problems.size());
        for (Problem problem : problems) {
            problemsAsStrings.add(problem.toString());
        }

        return wfBuilder.buildProcessDefinition(processDefinition, problemsAsStrings, locale);
    }

    public void loadSampleWorkflows(final Repository repository) throws WorkflowException {
        executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                internalLoadSampleWorkflows(jbpmContext);
                return null;
            }

            public String getErrorMessage() {
                return "Error loading sample workflows.";
            }
        });
    }

    private void internalLoadSampleWorkflows(JbpmContext jbpmContext) throws WorkflowException {
        String[] samples = new String[] {"review-process.zip", "generictask-process.zip", "timedpublish-process.zip"};
        for (String sample : samples) {
            String resourcePath = "org/outerj/daisy/workflow/serverimpl/samples/" + sample;
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (is == null)
                throw new WorkflowException("Workflow sample not found: " + resourcePath);
            try {
                internalDeploy(is, "application/zip", jbpmContext, Locale.getDefault(), null);
            } finally {
                try {is.close();} catch (Exception e) { /* ignore */ }
            }
        }
    }

    public void deleteProcessDefinition(final String processDefinitionId, final Repository repository) throws WorkflowException {
        final long processDefinitionIdLong = parseJbpmId(processDefinitionId);

        executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = getProcessDefinition(processDefinitionIdLong, graphSession);
                if (!wfAuthorizer.canDeleteProcessDefinition(processDefinition, repository)) {
                    throw new WfAuthorizationException("You are not allowed to delete this workflow definition.");
                }
                graphSession.deleteProcessDefinition(processDefinition);
                return null;
            }

            public String getErrorMessage() {
                return "Error deleting workflow definition " + processDefinitionId;
            }
        });

        // To make sure the cache shrinks when definitions are deleted
        wfBuilder.clearCache();
    }

    private ProcessDefinition getProcessDefinition(long processDefinitionId, GraphSession graphSession) throws ProcessDefinitionNotFoundException {
        ProcessDefinition processDefinition = graphSession.getProcessDefinition(processDefinitionId);
        if (processDefinition == null)
            throw new ProcessDefinitionNotFoundException(String.valueOf(processDefinitionId), false);
        return processDefinition;
    }

    public WfProcessDefinition getProcessDefinition(final String processDefinitionId, final Locale locale,
            final Repository repository) throws WorkflowException {
        final long processDefinitionIdLong = parseJbpmId(processDefinitionId);

        return (WfProcessDefinition)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = getProcessDefinition(processDefinitionIdLong, graphSession);
                if (!wfAuthorizer.canReadProcessDefinition(processDefinition, repository)) {
                    throw new WfAuthorizationException("You are not allowed access to process definition " + processDefinitionId);
                }
                return wfBuilder.getProcessDefinition(processDefinition, locale);
            }

            public String getErrorMessage() {
                return "Error loading workflow definition " + processDefinitionId;
            }
        });
    }

    public WfProcessDefinition getLatestProcessDefinition(final String workflowName, final Locale locale, final Repository repository) throws WorkflowException {
        if (workflowName == null)
            throw new IllegalArgumentException("Null argument: workflowName");

        return (WfProcessDefinition)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = graphSession.findLatestProcessDefinition(workflowName);
                if (processDefinition == null)
                    throw new ProcessDefinitionNotFoundException(workflowName, true);
                if (!wfAuthorizer.canReadProcessDefinition(processDefinition, repository)) {
                    throw new WfAuthorizationException("You are not allowed access to process definition " + workflowName);
                }
                return wfBuilder.getProcessDefinition(processDefinition, locale);
            }

            public String getErrorMessage() {
                return "Error loading latest workflow definition named " + workflowName;
            }
        });
    }

    public List<WfProcessDefinition> getAllLatestProcessDefinitions(final Locale locale, final Repository repository) throws WorkflowException {
        return (List<WfProcessDefinition>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                List<ProcessDefinition> processDefinitions = graphSession.findLatestProcessDefinitions();
                List<WfProcessDefinition> workflowDefinitions = new ArrayList<WfProcessDefinition>();
                for (ProcessDefinition processDefinition : processDefinitions) {
                    if (wfAuthorizer.canReadProcessDefinition(processDefinition, repository))
                        workflowDefinitions.add(wfBuilder.getProcessDefinition(processDefinition, locale));
                }
                return workflowDefinitions;
            }

            public String getErrorMessage() {
                return "Error loading list of latest workflow definitions.";
            }
        });
    }

    public List<WfProcessDefinition> getAllProcessDefinitions(final Locale locale, final Repository repository) throws WorkflowException {
        return (List<WfProcessDefinition>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                List<ProcessDefinition> processDefinitions = graphSession.findAllProcessDefinitions();
                List<WfProcessDefinition> workflowDefinitions = new ArrayList<WfProcessDefinition>();
                for (ProcessDefinition processDefinition : processDefinitions) {
                    if (wfAuthorizer.canReadProcessDefinition(processDefinition, repository))
                        workflowDefinitions.add(wfBuilder.getProcessDefinition(processDefinition, locale));
                }
                return workflowDefinitions;
            }

            public String getErrorMessage() {
                return "Error loading list of workflow definitions.";
            }
        });
    }

    public Map<String, Integer> getProcessInstanceCounts(final Repository repository) throws WorkflowException {
        if (!wfAuthorizer.canGetProcessInstanceCounts(repository))
            throw new WfAuthorizationException("You are not allowed to retrieve process instance count statistics.");

        return (Map<String, Integer>)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                Map<String, Integer> instanceCounts = new HashMap<String, Integer>();
                List results = session.createQuery("select pi.processDefinition.id, count(distinct pi.id) from ProcessInstance pi group by pi.processDefinition.id ").list();
                for (Object result : results) {
                    Object[] row = (Object[])result;
                    // Note: processDefinition.id is returned as a long, while the count is returned as a Long
                    instanceCounts.put(String.valueOf(row[0]), ((Number)row[1]).intValue());
                }
                return instanceCounts;
            }

            public String getErrorMessage() {
                return "Error loading list of workflow definitions.";
            }
        });
    }

    public List<WfVariable> getInitialVariables(final String processDefinitionId, final WfVersionKey contextDocument,
            final Repository repository) throws WorkflowException {
        final long processDefinitionIdLong = parseJbpmId(processDefinitionId);
        return (List<WfVariable>)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = getProcessDefinition(processDefinitionIdLong, graphSession);

                if (!wfAuthorizer.canStartProcess(processDefinition, repository))
                    throw new WfAuthorizationException("You are not allowed to start this process.");

                WfProcessDefinition process = wfBuilder.getProcessDefinition(processDefinition, Locale.getDefault());

                WfTaskDefinition startStateTask = null;
                for (WfTaskDefinition task : process.getTasks()) {
                    if (task.getNode().getNodeType().equals("StartState")) {
                        startStateTask = task;
                        break;
                    }
                }

                if (startStateTask == null)
                    return Collections.emptyList();

                List<WfVariable> initialVariables = new ArrayList<WfVariable>();

                Context rhinoContext = Context.enter();
                try {
                    // Make sure rhino can see the jBPM and Daisy classes
                    rhinoContext.setApplicationClassLoader(this.getClass().getClassLoader());

                    rhinoContext.setOptimizationLevel(-1);
                    Scriptable scope = rhinoContext.initStandardObjects();

                    List<WfVariableDefinition> variables = startStateTask.getVariables();
                    for (WfVariableDefinition variable : variables) {
                        WfVariableDefinitionImpl variableImpl = (WfVariableDefinitionImpl)variable;
                        if (variableImpl.getInitialValueScript() != null) {
                            Function script;
                            synchronized (variableImpl) {
                                script = (Function)variableImpl.getCompiledInitialValueScript();
                                if (script == null) {
                                    String scriptSource = variableImpl.getInitialValueScript();
                                    StringBuilder functionSource = new StringBuilder(scriptSource.length() + 200);
                                    functionSource.append("function(contextDocKey, repository, wfRepository, repositoryManager) {\n");
                                    functionSource.append(scriptSource);
                                    functionSource.append("\n}");
                                    String source = "Initial value script of variable " + variable.getName() + " in process definition " + processDefinitionId;
                                    script = rhinoContext.compileFunction(scope, functionSource.toString(), source, 1, null);
                                    variableImpl.setCompiledInitialValueScript(script);
                                }
                            }

                            Object result = script.call(rhinoContext, scope, null, new Object[] {contextDocument, repository, getWfRepository(), repositoryManager});
                            if (result instanceof Wrapper) {
                                result = ((Wrapper)result).unwrap();
                            } else if (result == Undefined.instance) {
                                result = null;
                            }

                            if (result != null && !variable.getType().getTypeClass().isAssignableFrom(result.getClass())) {
                                throw new RuntimeException("Initial value script for variable " + variable.getName() + " of process definition " + processDefinitionId + " returned an object of an incorrect type, expected a " + variable.getType().getTypeClass().getName() + " but got a " + result.getClass().getName());
                            }

                            if (result != null)
                                initialVariables.add(new WfVariableImpl(variable.getName(), variable.getScope(), variable.getType(), result));
                        }
                    }
                } finally {
                    rhinoContext.setApplicationClassLoader(null);
                    Context.exit();
                }

                return initialVariables;
            }

            public String getErrorMessage() {
                return "Error calculating initial variables for workflow definition " + processDefinitionId;
            }
        });
    }

    public WfProcessInstance startProcess(final String processDefinitionId, final TaskUpdateData startTaskData,
            final String initialTransition, final Locale locale, final Repository repository) throws WorkflowException {

        final long processDefinitionIdLong = parseJbpmId(processDefinitionId);
        return (WfProcessInstance)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = getProcessDefinition(processDefinitionIdLong, graphSession);

                if (!wfAuthorizer.canStartProcess(processDefinition, repository)) {
                    throw new WfAuthorizationException("You are not allowed to start this process.");
                }

                ProcessInstance processInstance = new ProcessInstance(processDefinition);

                // Set the daisy_creator and daisy_owner properties
                ContextInstance contextInstance = processInstance.getContextInstance();
                WfUserKey currentUser = new WfUserKey(repository.getUserId());
                contextInstance.setVariable("daisy_creator", currentUser, processInstance.getRootToken());
                contextInstance.setVariable("daisy_owner", currentUser, processInstance.getRootToken());

                if (processDefinition.getTaskMgmtDefinition().getStartTask() != null) {
                    setupTempDaisyVariables(processInstance, repository);
                    TaskInstance taskInstance = processInstance.getTaskMgmtInstance().createStartTaskInstance();
                    updateTask(startTaskData, taskInstance, processInstance, true);
                    if (initialTransition != null)
                        taskInstance.end(initialTransition);
                    else
                        taskInstance.end();
                } else if (startTaskData != null) {
                    throw new WorkflowException("Initial parameters where specified for starting the workflow, but the workflow has no start task.");
                } else {
                    if (initialTransition != null)
                        processInstance.getRootToken().signal(initialTransition);
                    else
                        processInstance.getRootToken().signal();
                }

                jbpmContext.save(processInstance);

                return wfBuilder.buildProcessInstance(processInstance, locale);
            }

            public String getErrorMessage() {
                return "Error starting workflow instance for definition " + processDefinitionId;
            }
        });
    }

    public WfExecutionPath signal(final String processInstanceId, final String executionPathFullName,
            final String transitionName, final Locale locale, final Repository repository) throws WorkflowException {
        final long processInstanceIdLong = parseJbpmId(processInstanceId);
        return (WfExecutionPath)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);

                if (!wfAuthorizer.canSignalProcess(processInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to signal this process instance (" + processInstanceId + ").");
                }

                setupTempDaisyVariables(processInstance, repository);

                Token token = processInstance.findToken(executionPathFullName);
                if (transitionName == null)
                    token.signal();
                else
                    token.signal(transitionName);

                jbpmContext.save(processInstance);

                return wfBuilder.buildExecutionPaths(token, locale);
            }


            public String getErrorMessage() {
                return "Error signalling the execution path " + executionPathFullName + " of workflow instance " + processInstanceId;
            }
        });
    }

    public WfProcessInstance getProcess(final String processInstanceId, final Locale locale, final Repository repository) throws WorkflowException {
        final long processInstanceIdLong = parseJbpmId(processInstanceId);
        return (WfProcessInstance)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);

                if (!wfAuthorizer.canReadProcess(processInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed access to process instance " + processInstanceId);
                }

                return wfBuilder.buildProcessInstance(processInstance, locale);
            }

            public String getErrorMessage() {
                return "Error retrieving workflow instance " + processInstanceId;
            }
        });
    }

    private ProcessInstance getProcessInstance(long processInstanceId, JbpmContext jbpmContext) throws ProcessInstanceNotFoundException {
        ProcessInstance processInstance = jbpmContext.getGraphSession().getProcessInstance(processInstanceId);
        if (processInstance == null)
            throw new ProcessInstanceNotFoundException(String.valueOf(processInstanceId));
        return processInstance;
    }

    public WfTask updateTask(final String taskId, final TaskUpdateData taskUpdateData, final Locale locale, final Repository repository) throws WorkflowException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);

                if (!wfAuthorizer.canUpdateTask(taskInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to update task " + taskId);
                }

                ProcessInstance processInstance = taskInstance.getToken().getProcessInstance();
                updateTask(taskUpdateData, taskInstance, processInstance, false);

                jbpmContext.save(processInstance);
                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error updating task " + taskId;
            }
        });
    }

    private void updateTask(TaskUpdateData taskUpdateData, TaskInstance taskInstance, ProcessInstance processInstance,
            boolean checkRequired) throws WorkflowException {
        // TODO think about making this smart enough to only save if there are really updates (useful? added value?)
        //      (maybe hibernate keeps track of changes?)

        if (!taskInstance.isOpen()) {
            throw new WorkflowException("Cannot update task instance " + taskInstance.getId() + " since it is closed.");
        }
        
        if (taskUpdateData == null) {
            taskUpdateData = new TaskUpdateData();
        }

        if (taskUpdateData.getPriority() != null) {
            int jbpmPriority = taskPriorityToJbpm(taskUpdateData.getPriority());
            taskInstance.setPriority(jbpmPriority);
        }

        if (taskUpdateData.getDueDate() != null) {
            taskInstance.setDueDate(taskUpdateData.getDueDate());
        } else if (taskUpdateData.getClearDueDate()) {
            taskInstance.setDueDate(null);
        }

        WfTaskDefinition taskDefinition = wfBuilder.getTaskDefinition(taskInstance.getTask(), Locale.getDefault());
        List<WfVariableDefinition> variablesDefinition = taskDefinition.getVariables();
        for (WfVariableDefinition variableDef : variablesDefinition) {
            String variableName = variableDef.getName();
            VariableScope variableScope = variableDef.getScope();
            Object value = taskUpdateData.getVariable(variableName, variableScope);
            ContextInstance contextInstance = taskInstance.getContextInstance();

            // skip updating of read-only variables
            if (variableDef.isReadOnly())
                continue;

            // Retrieve existing variable value
            Object existingValue;
            switch (variableDef.getScope()) {
                case TASK:
                    existingValue = taskInstance.getVariableLocally(variableName);
                    break;
                case GLOBAL:
                    existingValue = contextInstance.getLocalVariable(variableName, processInstance.getRootToken());
                    break;
                default:
                    throw new RuntimeException("Unrecognized variable scope: " + variableDef.getScope());
            }

            // Check if a value is present for required variables
            if (checkRequired &&
                    (variableDef.isRequired() && value == null && existingValue == null
                    || taskUpdateData.isVariableDeleted(variableName, variableScope)))
                throw new WorkflowException("Missing required workflow task variable: " + variableName);

            if (taskUpdateData.isVariableDeleted(variableName, variableScope)) {
                switch (variableScope) {
                    case TASK:
                        taskInstance.deleteVariableLocally(variableName);
                        break;
                    case GLOBAL:
                        contextInstance.deleteVariable(variableName, processInstance.getRootToken());
                        break;
                    default:
                        throw new RuntimeException("Unsupported variable scope: " + variableDef.getScope());
                }
            } else if (value != null) {
                if (!variableDef.getType().getTypeClass().isAssignableFrom(value.getClass())) {
                    throw new WorkflowException("Incorrect value class for workflow task variable: " + variableName);
                }
                switch (variableScope) {
                    case TASK:
                        taskInstance.setVariableLocally(variableName, value);
                        break;
                    case GLOBAL:
                        contextInstance.createVariable(variableName, value, processInstance.getRootToken());
                        break;
                    default:
                        throw new RuntimeException("Unsupported variable scope: " + variableDef.getScope());
                }
            }
        }
    }

    private int taskPriorityToJbpm(TaskPriority priority) {
        switch (priority) {
            case HIGHEST:
                return Task.PRIORITY_HIGHEST;
            case HIGH:
                return Task.PRIORITY_HIGH;
            case NORMAL:
                return Task.PRIORITY_NORMAL;
            case LOW:
                return Task.PRIORITY_LOW;
            case LOWEST:
                return Task.PRIORITY_LOWEST;
            default:
                throw new RuntimeException("Unexpected task priority: " + priority);
        }
    }

    public WfTask endTask(final String taskId, final TaskUpdateData taskUpdateData, final String transitionName,
            final Locale locale, final Repository repository) throws WorkflowException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);

                if (!wfAuthorizer.canUpdateTask(taskInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to update or end task " + taskId);
                }

                ProcessInstance processInstance = taskInstance.getToken().getProcessInstance();
                setupTempDaisyVariables(processInstance, repository);
                updateTask(taskUpdateData, taskInstance, processInstance, true);

                if (transitionName != null)
                    taskInstance.end(transitionName);
                else
                    taskInstance.end();

                jbpmContext.save(processInstance);
                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error updating or ending task " + taskId;
            }
        });
    }

    public WfTask getTask(final String taskId, final Locale locale, final Repository repository) throws WorkflowException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);

                // One is allowed read access to a task when one has read access to a process instance, since
                // task information is part of process read information too.
                if (!wfAuthorizer.canReadProcess(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository)) {
                    throw new WfAuthorizationException("You are not allowed read access to task " + taskId);
                }

                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error getting task with ID " + taskId;
            }
        });
    }

    public List<WfTask> getMyTasks(final Locale locale, final Repository repository) throws WorkflowException {
        return (List<WfTask>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskMgmtSession taskMgmtSession = jbpmContext.getTaskMgmtSession();
                List<TaskInstance> taskInstances = taskMgmtSession.findTaskInstances(jbpmContext.getActorId());
                taskInstances = accessFilterTasks(taskInstances, repository);
                return buildTasks(taskInstances, locale);
            }

            public String getErrorMessage() {
                return "Error getting tasks.";
            }
        });
    }


    public List<WfTask> getPooledTasks(final Locale locale, final Repository repository) throws WorkflowException {
        return (List<WfTask>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                // Get list of pools the user belongs to
                WfPoolManager poolManager = getPoolManager(repository);
                List<WfPool> pools = poolManager.getPoolsForUser(repository.getUserId());
                List<String> actorIds = new ArrayList<String>(pools.size());
                for (WfPool pool : pools) {
                    actorIds.add(String.valueOf(pool.getId()));
                    // this is in the case someone chooses to use the poolname instead of the poolid.
                    // eg. in the processdefinition the name of a pool could be used instead of an ID
                    // for a task/swimlane assignment
                    actorIds.add(pool.getName());
                }

                List<TaskInstance> taskInstances;
                if (actorIds.size() == 0) {
                    taskInstances = Collections.emptyList();
                } else {
                    taskInstances = jbpmContext.getGroupTaskList(actorIds);                    
                }
                taskInstances = accessFilterTasks(taskInstances, repository);
                return buildTasks(taskInstances, locale);
            }

            public String getErrorMessage() {
                return "Error getting pooled tasks list.";
            }
        });
    }

    public WfTask requestPooledTask(final String taskId, final Locale locale, final Repository repository) throws WorkflowException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);
                setupTempDaisyVariables(taskInstance.getProcessInstance(), repository);
                
                if (taskInstance.getActorId() != null)
                    throw new WorkflowException("Task " + taskId + " is not pooled, it is already assigned to a user.");

                if (!wfAuthorizer.canRequestPooledTask(taskInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to request this pooled task.");
                }

                taskInstance.setActorId(jbpmContext.getActorId());
                jbpmContext.save(taskInstance);
                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error assigning pooled task to user.";
            }
        });
    }

    public WfTask assignTask(final String taskId, final WfActorKey actor, final boolean overwriteSwimlane, final Locale locale, final Repository repository) throws RepositoryException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);
                setupTempDaisyVariables(taskInstance.getProcessInstance(), repository);
                
                if (!wfAuthorizer.canAssignTask(taskInstance, actor, repository)) {
                    throw new WfAuthorizationException("You are not allowed to change the actor for this task.");
                }

                if (actor.isUser()) {
                    taskInstance.setActorId(String.valueOf(actor.getUserId()), overwriteSwimlane);
                } else {
                    taskInstance.setActorId(null, overwriteSwimlane);
                    List<Long> poolIds = actor.getPoolIds();
                    String[] stringPoolIds = new String[poolIds.size()];
                    for (int i = 0; i < stringPoolIds.length; i++) {
                        stringPoolIds[i] = String.valueOf(poolIds.get(i));
                    }
                    taskInstance.setPooledActors(stringPoolIds);
                    if (overwriteSwimlane) {
                        SwimlaneInstance swimlane = taskInstance.getSwimlaneInstance();
                        swimlane.setPooledActors(stringPoolIds);
                    }
                }

                jbpmContext.save(taskInstance);
                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error assigning task to actor.";
            }
        });
    }

    WfTask unassignTask(final String taskId, final Locale locale, final Repository repository) throws RepositoryException {
        final long taskIdLong = parseJbpmId(taskId);
        return (WfTask)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);
                setupTempDaisyVariables(taskInstance.getProcessInstance(), repository);

                if (taskInstance.getActorId() == null)
                    throw new WorkflowException("Task " + taskId + " is not yet assigned to anyone, so can't be un-assigned.");

                Set pooledActors = taskInstance.getPooledActors();
                if (pooledActors == null || pooledActors.isEmpty())
                    throw new WorkflowException("Task " + taskId + " does not have pooled actors, unassignment not allowed.");

                // If there is a swimlane, and the actor of the swimlane is the same as the one of the task,
                // and the swimlane has pools, then also unassign from the swimlane
                boolean overwriteSwimlane = false;
                SwimlaneInstance swimlane = taskInstance.getSwimlaneInstance();
                if (swimlane != null && taskInstance.getActorId().equals(swimlane.getActorId())) {
                    pooledActors = swimlane.getPooledActors();
                    if (pooledActors != null && !pooledActors.isEmpty())
                        overwriteSwimlane = true;
                }

                if (!wfAuthorizer.canUnassignTask(taskInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to unassign this task.");
                }

                taskInstance.setActorId(null, overwriteSwimlane);
                jbpmContext.save(taskInstance);
                return wfBuilder.buildTask(taskInstance, locale);
            }

            public String getErrorMessage() {
                return "Error unassigning task.";
            }
        });
    }

    public List<WfTask> getTasks(final QueryConditions queryConditions, final List<QueryOrderByItem> orderByItems,
            final int chunkOffset, final int chunkLength, final Locale locale, final Repository repository) throws WorkflowException {

        return (List<WfTask>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);
                Query query = QueryGenerator.generateTaskQuery(queryConditions, session, context);
                List<TaskInstance> taskInstances = query.list();
                taskInstances = accessFilterTasks(taskInstances, repository);
                taskInstances = ResultSorter.sortTasks(taskInstances, orderByItems, context);
                taskInstances = ResultChunker.getChunk(taskInstances, chunkOffset, chunkLength).chunk;
                return buildTasks(taskInstances, locale);
            }

            public String getErrorMessage() {
                return "Error querying tasks.";
            }
        });
    }

    public SearchResultDocument searchTasks(final List<QuerySelectItem> selectItems, final QueryConditions queryConditions,
            final List<QueryOrderByItem> orderByItems, final int chunkOffset, final int chunkLength,
            final Locale locale, final Repository repository) throws WorkflowException {

        return (SearchResultDocument)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);
                Query query = QueryGenerator.generateTaskQuery(queryConditions, session, context);
                List<TaskInstance> taskInstances = query.list();
                taskInstances = accessFilterTasks(taskInstances, repository);
                taskInstances = ResultSorter.sortTasks(taskInstances, orderByItems, context);
                ResultChunker.ChunkResult chunkResult = ResultChunker.getChunk(taskInstances, chunkOffset, chunkLength);
                taskInstances = (List<TaskInstance>)chunkResult.chunk;
                return ResultBuilder.buildTaskResult(taskInstances, selectItems, chunkResult.chunkInfo, context);
            }

            public String getErrorMessage() {
                return "Error querying tasks.";
            }
        });
    }

    public List<WfProcessInstance> getProcesses(final QueryConditions queryConditions,
            final List<QueryOrderByItem> orderByItems, final int chunkOffset,
            final int chunkLength, final Locale locale, final Repository repository) throws WorkflowException {

        return (List<WfProcessInstance>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);
                Query query = QueryGenerator.generateProcessQuery(queryConditions, session, context);
                List<ProcessInstance> instances = query.list();
                instances = accessFilterProcesses(instances, repository);
                instances = ResultSorter.sortProcesses(instances, orderByItems, context);
                instances = ResultChunker.getChunk(instances, chunkOffset, chunkLength).chunk;
                return buildProcesses(instances, locale);
            }

            public String getErrorMessage() {
                return "Error querying process instances.";
            }
        });
    }

    public SearchResultDocument searchProcesses(final List<QuerySelectItem> selectItems,
            final QueryConditions queryConditions, final List<QueryOrderByItem> orderByItems,
            final int chunkOffset, final int chunkLength, final Locale locale, final Repository repository) throws WorkflowException {

        return (SearchResultDocument)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);
                Query query = QueryGenerator.generateProcessQuery(queryConditions, session, context);
                List<ProcessInstance> instances = query.list();
                instances = accessFilterProcesses(instances, repository);
                instances = ResultSorter.sortProcesses(instances, orderByItems, context);
                ResultChunker.ChunkResult chunkResult = ResultChunker.getChunk(instances, chunkOffset, chunkLength);
                instances = (List<ProcessInstance>)chunkResult.chunk;
                return ResultBuilder.buildProcessResult(instances, selectItems, chunkResult.chunkInfo, context);
            }

            public String getErrorMessage() {
                return "Error querying process instances.";
            }
        });
    }

    public void deleteProcess(final String processInstanceId, final Repository repository) throws WorkflowException {
        final long processInstanceIdLong = parseJbpmId(processInstanceId);
        executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                // try to retrieve the process instance so that an exception is thrown if it doesn't exist, and to check authorization
                ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);
                if (!wfAuthorizer.canDeleteProcess(processInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to delete process instance " + processInstanceId);
                }
                jbpmContext.getGraphSession().deleteProcessInstance(processInstanceIdLong);
                return null;
            }

            public String getErrorMessage() {
                return "Error deleting process instance " + processInstanceId;
            }
        });
    }

    public WfProcessInstance suspendProcess(final String processInstanceId, final Locale locale, final Repository repository) throws WorkflowException {
        final long processInstanceIdLong = parseJbpmId(processInstanceId);
        return (WfProcessInstance)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);
                if (!wfAuthorizer.canSuspendProcess(processInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to suspend process " + processInstanceId);
                }
                processInstance.suspend();
                jbpmContext.save(processInstance);
                return wfBuilder.buildProcessInstance(processInstance, locale);
            }

            public String getErrorMessage() {
                return "Error suspending process instance " + processInstanceId;
            }
        });
    }

    public WfProcessInstance resumeProcess(final String processInstanceId, final Locale locale, final Repository repository) throws WorkflowException {
        final long processInstanceIdLong = parseJbpmId(processInstanceId);
        return (WfProcessInstance)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);
                if (!wfAuthorizer.canResumeProcess(processInstance, repository)) {
                    throw new WfAuthorizationException("You are not allowed to resume process " + processInstanceId);
                }
                processInstance.resume();
                jbpmContext.save(processInstance);
                return wfBuilder.buildProcessInstance(processInstance, locale);
            }

            public String getErrorMessage() {
                return "Error resuming process instance " + processInstanceId;
            }
        });
    }

    public WfTimer getTimer(final String timerId, final Locale locale, final Repository repository) throws WorkflowException {
        final long timerIdLong = parseJbpmId(timerId);
        return (WfTimer)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                Timer timer = (Timer)jbpmContext.getSession().get(Timer.class, timerIdLong);
                if (timer == null)
                    throw new TimerNotFoundException(timerId);

                if (!wfAuthorizer.canReadProcess(timer.getProcessInstance(), repository)) {
                    throw new WfAuthorizationException("You are not allowed to access timer " + timerId);
                }

                return wfBuilder.buildTimer(timer, locale);
            }

            public String getErrorMessage() {
                return "Error gettting timer " + timerId;
            }
        });
    }

    public List<WfTimer> getTimers(final QueryConditions queryConditions, final List<QueryOrderByItem> orderByItems,
            final int chunkOffset, final int chunkLength, final Locale locale, final Repository repository) throws RepositoryException {

        return (List<WfTimer>)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);
                Query query = QueryGenerator.generateTimerQuery(queryConditions, session, context);
                List<Timer> timers = query.list();
                timers = accessFilterTimers(timers, repository);
                timers = ResultSorter.sortTimers(timers, orderByItems, context);
                timers = ResultChunker.getChunk(timers, chunkOffset, chunkLength).chunk;
                return buildTimers(timers, locale);
            }

            public String getErrorMessage() {
                return "Error querying timers.";
            }
        });
    }

    public SearchResultDocument searchTimers(final List<QuerySelectItem> selectItems, final QueryConditions queryConditions,
            final List<QueryOrderByItem> orderByItems, final int chunkOffset, final int chunkLength,
            final Locale locale, final Repository repository) throws RepositoryException {


        return (SearchResultDocument)executeJbpmRunnable(repository, new JbpmRunnable() {
            @SuppressWarnings("unchecked")
            public Object run(JbpmContext jbpmContext) throws Exception {
                Session session = jbpmContext.getSession();
                IntWfContext context = getContext(repository, locale);

                Query query = QueryGenerator.generateTimerQuery(queryConditions, session, context);
                List<Timer> timers = query.list();
                timers = accessFilterTimers(timers, repository);
                timers = ResultSorter.sortTimers(timers, orderByItems, context);
                ResultChunker.ChunkResult chunkResult = ResultChunker.getChunk(timers, chunkOffset, chunkLength);
                timers = chunkResult.chunk;
                return ResultBuilder.buildTimerResult(timers, selectItems, chunkResult.chunkInfo, context);
            }

            public String getErrorMessage() {
                return "Error querying timers.";
            }
        });
    }
    
    public boolean canUpdateTask(final Repository repository, String taskId) throws RepositoryException {
        final long taskIdLong = parseJbpmId(taskId);
        return (Boolean)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);

                return wfAuthorizer.canUpdateTask(taskInstance, repository);
            }
            public String getErrorMessage() {
                return "Error checking permission.";
            }
        });
        
    }

    public boolean canStartProcess(final Repository repository, final long processDefinitionId) throws RepositoryException {
        return (Boolean)executeJbpmRunnable(repository, new JbpmRunnable() {
            public Object run(JbpmContext jbpmContext) throws Exception {
                GraphSession graphSession = jbpmContext.getGraphSession();
                ProcessDefinition processDefinition = getProcessDefinition(processDefinitionId, graphSession); 

                return wfAuthorizer.canStartProcess(processDefinition, repository);
            }
            public String getErrorMessage() {
                return "Error checking permission.";
            }
        });
        
    }

    private TaskInstance getTaskInstance(long taskId, JbpmContext jbpmContext) throws TaskNotFoundException {
        TaskInstance taskInstance = jbpmContext.getTaskInstance(taskId);
        if (taskInstance == null)
            throw new TaskNotFoundException(String.valueOf(taskId));
        return taskInstance;
    }

    private List<TaskInstance> accessFilterTasks(List<TaskInstance> taskInstances, Repository repository) {
        List<TaskInstance> filteredTasks = new ArrayList<TaskInstance>();
        for (TaskInstance taskInstance : taskInstances) {
            if (wfAuthorizer.canReadProcess(taskInstance.getTaskMgmtInstance().getProcessInstance(), repository))
                filteredTasks.add(taskInstance);
        }
        return filteredTasks;
    }

    private List<WfTask> buildTasks(List<TaskInstance> taskInstances, Locale locale) throws WorkflowException {
        List<WfTask> tasks = new ArrayList<WfTask>(taskInstances.size());
        for (TaskInstance taskInstance : taskInstances) {
            tasks.add(wfBuilder.buildTask(taskInstance, locale));
        }
        return tasks;
    }

    private List<ProcessInstance> accessFilterProcesses(List<ProcessInstance> processInstances, Repository repository) {
        List<ProcessInstance> filteredProcesses = new ArrayList<ProcessInstance>();
        for (ProcessInstance processInstance : processInstances) {
            if (wfAuthorizer.canReadProcess(processInstance, repository))
                filteredProcesses.add(processInstance);
        }
        return filteredProcesses;
    }

    private List<WfProcessInstance> buildProcesses(List<ProcessInstance> processInstances, Locale locale) throws WorkflowException {
        List<WfProcessInstance> processes = new ArrayList<WfProcessInstance>(processInstances.size());
        for (ProcessInstance processInstance : processInstances)
            processes.add(wfBuilder.buildProcessInstance(processInstance, locale));
        return processes;
    }

    private List<WfTimer> buildTimers(List<Timer> timers, Locale locale) throws WorkflowException {
        List<WfTimer> wfTimers = new ArrayList<WfTimer>(timers.size());
        for (Timer timer : timers) {
            wfTimers.add(wfBuilder.buildTimer(timer, locale));
        }
        return wfTimers;
    }

    private List<Timer> accessFilterTimers(List<Timer> timers, Repository repository) {
        // Access control on timers is based on the process they belong to
        List<Timer> filteredTimers = new ArrayList<Timer>();
        for (Timer timer : timers) {
            if (wfAuthorizer.canReadProcess(timer.getProcessInstance(), repository))
                filteredTimers.add(timer);
        }
        return filteredTimers;
    }

    private IntWfContext getContext(Repository repository, Locale locale) {
        return new IntWfContext(wfMetaManager, wfBuilder, queryMetadataRegistry, repository, getPoolManager(repository), locale, log);
    }

    private long parseJbpmId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid ID: " + id);
        }
    }

    private void setupTempDaisyVariables(ProcessInstance processInstance, Repository repository) throws RepositoryException {
        ContextInstance contextInstance = processInstance.getContextInstance();
        contextInstance.setTransientVariable("repository", repository);
        contextInstance.setTransientVariable("wfRepository", getWfRepository());
        contextInstance.setTransientVariable("repositoryManager", repositoryManager);
        contextInstance.setTransientVariable("mailer", wfMailer);

        // Object meant only for private/internal use (should consider communicating these in some other way)
        contextInstance.setTransientVariable("_wfObjectBuilder", this.wfBuilder);
        try {
            contextInstance.setTransientVariable("_resourceBundles", this.wfBuilder.getI18nBundle(processInstance.getProcessDefinition()));
        } catch (WorkflowException wfException) {
            // this should not happen since we (probably) already successfully obtained objects via the wfObjectBuilder
            throw new RuntimeException(wfException);
        }
    }

    /**
     * Retrieves/creates a JbpmContext. The retrieved JbpmContext
     * should always be released using {@link #closeJbpmContext}
     * (using a try-finally block).
     */
    private JbpmContext getJbpmContext(Repository repository) throws WorkflowException {
        DaisyConnectionProvider.DATASOURCE.set(dataSource);
        try {
            JbpmContext context = jbpmConfiguration.createJbpmContext();
            if (repository != null)
                context.setActorId(String.valueOf(repository.getUserId()));
            return context;
        } catch (Throwable e) {
            // context creation failed, dissasociate data source from thread
            DaisyConnectionProvider.DATASOURCE.set(null);
            throw new WorkflowException("Error creating JbpmContext.", e);
        }
    }

    private static interface JbpmRunnable {
        public Object run(JbpmContext jbpmContext) throws Exception;

        public String getErrorMessage();
    }

    private Object executeJbpmRunnable(Repository repository, JbpmRunnable jbpmRunnable) throws WorkflowException {
        // Make available a repository to the DaisyAddressResolver
        Repository wfRepository = getWfRepository();

        DaisyAddressResolver.repository.set(wfRepository);
        JbpmContext jbpmContext = getJbpmContext(repository);
        try {
            return jbpmRunnable.run(jbpmContext);
        } catch (Throwable e) {
            jbpmContext.setRollbackOnly();
            if (e instanceof WorkflowException)
                throw (WorkflowException)e;
            else
                throw new WorkflowException(jbpmRunnable.getErrorMessage(), e);
        } finally {
            DaisyAddressResolver.repository.set(null);
            closeJbpmContext(jbpmContext);
        }
    }

    /**
     * Closes a JbpmContext retrieved using {@link #getJbpmContext}.
     * This also commits the database transaction (or rolls it back,
     * in case JbpmContext.setRollbackOnly was called).
     */
    private void closeJbpmContext(JbpmContext jbpmContext) {
        DaisyConnectionProvider.DATASOURCE.set(null);
        jbpmContext.close();
    }
    
    class WorkflowExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new WorkflowManagerImpl(CommonWorkflowManager.this, repository);
        }
    }

    public WorkflowAclInfoDocument getAclInfo(final String taskId,
            final String processId, final String processDefinitionId,
            final boolean includeGlobalInfo, final Repository repository) throws RepositoryException {
        WorkflowAclInfoDocument infoDoc = WorkflowAclInfoDocument.Factory.newInstance();
        final WorkflowAclInfo info = infoDoc.addNewWorkflowAclInfo();
        executeJbpmRunnable(repository, new JbpmRunnable() {

            public Object run(JbpmContext jbpmContext) throws Exception {
                if  (taskId != null) {
                    long taskIdLong = parseJbpmId(taskId);
                    TaskInstance taskInstance = getTaskInstance(taskIdLong, jbpmContext);
                    info.setCanUpdateTask(wfAuthorizer.canUpdateTask(taskInstance, repository));
                    info.setCanRequestPooledTask(wfAuthorizer.canRequestPooledTask(taskInstance, repository));
                    // not implemented because of the additional newActor argument
                    //wfAuthorizer.canAssignTask(taskInstance, newActor, repository);
                    info.setCanUnassignTask(wfAuthorizer.canUnassignTask(taskInstance, repository));
                }
                if  (processId != null) {
                    long processInstanceIdLong = parseJbpmId(processId);
                    ProcessInstance processInstance = getProcessInstance(processInstanceIdLong, jbpmContext);
                    wfAuthorizer.canSignalProcess(processInstance, repository);
                    wfAuthorizer.canReadProcess(processInstance, repository);
                    wfAuthorizer.canDeleteProcess(processInstance, repository);
                    wfAuthorizer.canSuspendProcess(processInstance, repository);
                    wfAuthorizer.canResumeProcess(processInstance, repository);
                }
                if  (processDefinitionId != null) {
                    long processDefinitionIdLong = parseJbpmId(processDefinitionId);
                    ProcessDefinition processDefinition = getProcessDefinition(processDefinitionIdLong, jbpmContext.getGraphSession());
                    info.setCanDeployProcessDefinition(wfAuthorizer.canDeployProcessDefinition(processDefinition, repository));
                    info.setCanDeleteProcessDefinition(wfAuthorizer.canDeleteProcessDefinition(processDefinition, repository));
                    info.setCanReadProcessDefinition(wfAuthorizer.canReadProcessDefinition(processDefinition, repository));
                    info.setCanStartProcess(wfAuthorizer.canStartProcess(processDefinition, repository));
                }
                if  (includeGlobalInfo) {
                    info.setCanGetProcessInstanceCounts(wfAuthorizer.canGetProcessInstanceCounts(repository));
                }
                
                return null;
            }

            public String getErrorMessage() {
                return "Failed to obtain acl info.";
            }

        });
        return infoDoc;
    }
}
