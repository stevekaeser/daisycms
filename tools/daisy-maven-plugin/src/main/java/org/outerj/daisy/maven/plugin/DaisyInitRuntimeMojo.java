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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.jaxen.SimpleVariableContext;
import org.jaxen.dom.DOMXPath;
import org.outerj.daisy.install.DatabaseCreator;
import org.outerj.daisy.install.DatabaseInfo;
import org.outerj.daisy.install.DatabaseParams;
import org.outerj.daisy.install.InstallHelper;
import org.outerj.daisy.repository.CollectionManager;
import org.outerj.daisy.repository.CollectionNotFoundException;
import org.outerj.daisy.repository.DocumentCollection;
import org.outerj.daisy.repository.DocumentTypeInconsistencyException;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryException;
import org.outerj.daisy.repository.ValueType;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclActionType;
import org.outerj.daisy.repository.acl.AclEntry;
import org.outerj.daisy.repository.acl.AclObject;
import org.outerj.daisy.repository.acl.AclPermission;
import org.outerj.daisy.repository.acl.AclSubjectType;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.RoleNotFoundException;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.repository.variant.Branch;
import org.outerj.daisy.repository.variant.Language;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadListener;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoadResult;
import org.outerj.daisy.tools.importexport.import_.schema.SchemaLoader;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchema;
import org.outerj.daisy.tools.importexport.model.schema.ImpExpSchemaDexmlizer;
import org.outerj.daisy.util.Gpw;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * This whole class is actually a quick mash-up of all needed functionality
 * extracted from daisy's init classes. Because these classes aren't reusable, a
 * lot has been copied and fixed to suit our needs, but eventually it would be
 * better if daisy itself would adapt the install process in such a way that it
 * can be made available for auto-installment in other code.
 *
 * TODO see javadoc above, refactor if possible.
 *
 * @author Jan Hoskens
 * @goal init-runtime
 * @aggregator
 * @description Create the necessary runtime environment for daisy. A daisydata
 *              and daisywikidata dir should be created as well as all databases
 *              and configurations.
 */
public class DaisyInitRuntimeMojo extends AbstractDaisyMojo {

    /**
     * An admin user that has the privilege to create databases and users.
     *
     * @parameter expression="${dbAdminUser} default-value="root"
     */
    private String dbAdminUser;

    /**
     * Password for the dbAdminUser.
     *
     * @parameter expression="${dbAdminPassword}"
     */
    private String dbAdminPassword;

    /**
     * Connection url to create databases.
     *
     * @parameter expression="${dbAdminUrl}"
     */
    private String dbAdminUrl;

    /**
     * Hostname to use instead of localhost.
     *
     * This parameter is a workaround for the annoying URISyntaxException
     * problem you get on windows when localhost is resolved to the computername
     * and the latter contains an illegal character (such as '_'). If this
     * parameter is given, it will be used as a base hostname for all urls if
     * they are automatically created (defaults). However, all non-default paths
     * (mostly db related) will need to be configured with the correct host.
     *
     * @parameter expression="${hostname}" default-value="localhost"
     */
    private String hostname;

    /**
     * The namespaces to create for this instance.
     *
     * @parameter
     */
    private List<Namespace> namespaces;
    
    /**
     * Initial collections to create for this instance.
     *
     * @parameter
     */
    private String[] collections;
    
    /**
     * @parameter expression="${databaseName}" default-value="daisyrepository"
     */
    private String databaseName;

    /**
     * @parameter expression="${databaseUrl}"
     */
    private String databaseUrl;

    /**
     * @parameter expression="${databaseUser}" default-value="daisy"
     */
    private String databaseUser;

    /**
     * @parameter expression="${databasePassword}" default-value="daisy"
     */
    private String databasePassword;

    /**
     * @parameter expression="${jmsDatabaseName}" default-value="activemq"
     */
    private String jmsDatabaseName;

    /**
     * @parameter expression="${jmsDatabaseUrl}"
     */
    private String jmsDatabaseUrl;

    /**
     * @parameter expression="${jmsDatabaseUser}" default-value="activemq"
     */
    private String jmsDatabaseUser;

    /**
     * @parameter expression="${jmsDatabasePassword}" default-value="activemq"
     */
    private String jmsDatabasePassword;

    /**
     * @parameter expression="${jmsAdminPassword}"
     */
    private String jmsAdminPassword;

    /**
     * @parameter expression="${driverClass}"
     *            default-value="com.mysql.jdbc.Driver"
     */
    private String driverClass;

    /**
     * @parameter expression="${driverClassPath}"
     */
    private String driverClassPath;

    /**
     * @parameter expression="${hibernateDialect}"
     *            default-value="org.hibernate.dialect.MySQLInnoDBDialect"
     */
    private String hibernateDialect;

    /**
     * @parameter expression="${internalUserPassword}"
     */
    private String internalUserPassword;

    /**
     * @parameter expression="${workflowUserPassword}"
     */
    private String workflowUserPassword;

    /**
     * @parameter expression="${taskSite}"
     */
    private String taskSite;

    /**
     * If we have a production version, don't use the "daisy/" prefix in the
     * task url.
     *
     * @parameter expression="${taskUrlProduction}" default-value="true"
     */
    private boolean taskUrlProduction;

    /**
     * @parameter expression="${registrarPassword}"
     */
    private String registrarPassword;

    /**
     * @parameter expression="${isUpdateXconf}" default-value="true"
     */
    private boolean isUpdateXconf;

    /**
     * @parameter expression="${smtpServer}"
     */
    private String smtpServer;

    /**
     * @parameter expression="${smtpPort}" default-value="25"
     */
    private int smtpPort;

    /**
     * @parameter expression="${smtpUsername}"
     */
    private String smtpUsername;

    /**
     * @parameter expression="${smtpPassword}"
     */
    private String smtpPassword;

    /**
     * @parameter expression="${smtpSSL}"
     */
    private boolean smtpSSL = false;

    /**
     * @parameter expression="${smtpTLS}"
     */
    private boolean smtpTLS = false;

    /**
     * @parameter expression="${mailFrom}"
     */
    private String mailFrom;

    /**
     * @parameter expression="${siteName}"
     */
    private String siteName;

    /**
     * @parameter
     */
    private String[] languages;

    /**
     * @parameter
     */
    private String defaultLanguage;

    private DatabaseParams dbParams;

    private DatabaseParams jmsDbParams;

    private long guestRoleId;

    private File xconfFile;

    private org.outerj.daisy.repository.Document navigationDoc;
    private DocumentCollection collection;
    private org.outerj.daisy.repository.Document sampleDoc;

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        versionCheck();
        if (driverClassPath == null)
            driverClassPath = "${daisy.home}/lib/mysql/mysql-connector-java/3.1.12/mysql-connector-java-3.1.12.jar";
        try {
            if (repositoryServiceRunning()) {
                getLog().info("Repository already installed and server is running.");
            } else if (repositoryServiceInstalled()) {
                getLog().info("Repository already installed, starting server.");
                startRepositoryService();
            } else {
                installRepository();
            }

            if (wikiServiceRunning()) {
                getLog().info("Wiki already installed and running.");
            } else if (wikiServiceInstalled()) {
                getLog().info("Wiki already installed, starting wiki.");
                startWikiService();
            } else {
                installWiki();
            }

        } catch (Exception e) {
            throw new MojoExecutionException("Error while initializing the repository.", e);
        }
    }

    /**
     * Install sequence for the repository.
     *
     * @throws Exception
     */
    protected void installRepository() throws Exception {
        initDbParams();
        createDatabases();
        installDaisyData();
        installRepositoryService();
        startRepositoryService();
        waitForRepository();
        createNamespaces(namespaces);
        createCollections();
    }

    private void createCollections() throws Exception {
        if (collections != null) {
            for (String collection: collections) {
                createCollection(collection);
            }
        }
    }

    private void initDbParams() throws Exception {
        if (dbAdminUrl == null) {
            dbAdminUrl = "jdbc:mysql://" + hostname;
        }
        databaseUrl = databaseUrl == null ? dbAdminUrl + "/" + databaseName + "?characterEncoding=UTF-8"
                : databaseUrl;
        jmsDatabaseUrl = jmsDatabaseUrl == null ? dbAdminUrl + "/" + jmsDatabaseName
                + "?characterEncoding=UTF-8" : jmsDatabaseUrl;

        dbParams = new DatabaseParams(databaseName, databaseUrl, databaseUser,
                databasePassword, driverClass, driverClassPath, hibernateDialect);

        jmsDbParams = new DatabaseParams(jmsDatabaseName, jmsDatabaseUrl,
                jmsDatabaseUser, jmsDatabasePassword, driverClass, driverClassPath, hibernateDialect);

        getLog().info("Loading driver");
        dbParams.loadDriver();
    }

    /**
     * Install sequence for the wiki.
     *
     * @throws Exception
     */
    protected void installWiki() throws Exception {
        installGuestUser();
        installACL();
        installSchema();
        installWikiDataDirectory();
        if (siteName == null) {
            getLog().info("${siteName} is not defined, so no site will be created");
        } else {
            addWikiSite();
        }
        installWikiService();
        startWikiService();
    }

    protected void createDatabases() throws SQLException, MojoExecutionException {
        createDatabase(databaseName, databaseUser, databasePassword);
        createDatabase(jmsDatabaseName, jmsDatabaseUser, jmsDatabasePassword);
    }

    /*
     * The following stuff can be found in RepositoryServerInit
     */
    protected void createDatabase(String databaseName, String user, String password) throws SQLException,
            MojoExecutionException {
        Connection conn = null;
        try {
            
            conn = DriverManager.getConnection(dbAdminUrl + "/", dbAdminUser, dbAdminPassword);
            String[] statements = new String[] {
                    "DROP DATABASE IF EXISTS " + databaseName,
                    "CREATE DATABASE " + databaseName + " CHARACTER SET 'utf8'",
                    "GRANT ALL ON " + databaseName + ".* TO '" + user + "'@localhost IDENTIFIED BY '"
                            + password + "'",
                    "GRANT ALL ON " + databaseName + ".* TO '" + user + "'@'%' IDENTIFIED BY '" + password
                            + "'" };
            Statement statement = conn.createStatement();
            for (String st : statements) {
                statement.addBatch(st);
            }
            int[] results = statement.executeBatch();
            for (int i = 0; i < results.length; ++i) {
                if (results[i] == Statement.EXECUTE_FAILED)
                    throw new MojoExecutionException("SQL failure: " + statements[i]);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create database", e);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    private void installDaisyData() throws Exception {
        internalUserPassword = internalUserPassword == null ? InstallHelper.generatePassword()
                : internalUserPassword;
        workflowUserPassword = workflowUserPassword == null ? InstallHelper.generatePassword()
                : workflowUserPassword;

        DatabaseCreator dbCreator = new DatabaseCreator();
        List<DatabaseCreator.NewUserInfo> newUserInfos = DatabaseCreator.createDefaultUsers(bootstrapUser,
                bootstrapPassword, "internal", internalUserPassword, "workflow", workflowUserPassword);
        dbCreator.run(dbParams.getUrl(), dbParams.getUser(), dbParams.getPassword(), newUserInfos);
        createDaisyDataDirectory();

        jmsAdminPassword = jmsAdminPassword == null ? Gpw.generate(8) : jmsAdminPassword;

        jmsConfiguration(jmsDbParams, jmsAdminPassword);
    }

    private void createDaisyDataDirectory() throws Exception {
        getLog().info("Creating the repository data directory");
        repoDataDir.mkdirs();

        getLog().info("Will create Daisy data directory at:");
        getLog().info(repoDataDir.getAbsolutePath());

        File blobStoreDir = new File(repoDataDir, "blobstore");
        blobStoreDir.mkdir();
        File indexStoreDir = new File(repoDataDir, "indexstore");
        indexStoreDir.mkdir();
        File pubReqsDir = new File(repoDataDir, "pubreqs");
        pubReqsDir.mkdir();
        File logDir = new File(repoDataDir, "logs");
        logDir.mkdir();
        File confDir = new File(repoDataDir, "conf");
        confDir.mkdir();
        File pluginsDir = new File(repoDataDir, "plugins");
        pluginsDir.mkdir();
        File pluginsBeforeRepoDir = new File(pluginsDir, "load-before-repository");
        pluginsBeforeRepoDir.mkdir();
        File pluginsAfterRepoDir = new File(pluginsDir, "load-after-repository");
        pluginsAfterRepoDir.mkdir();

        // By using slash as path separator, the configuration will work on both
        // Windows and Unix systems,
        // allowing to easier move a Daisy installation between these systems.
        String sep = "/";

        // create config override file
        File repoServerConfig = new File(confDir, "myconfig.xml");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.REPO_CONF), repoServerConfig);
        Document repoServerConfigDoc = InstallHelper.parseFile(repoServerConfig);

        Namespace defaultNamespace = null;
        for (Namespace ns: namespaces) {
            if (ns.isManaged()) {
                defaultNamespace = ns; 
                break;
            }
        }
        if (defaultNamespace == null)
            throw new MojoExecutionException("There should be at least one managed namespace");

        // configure namespace
        DOMXPath namespaceXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/repository-manager']/configuration/namespace");
        Element namespaceEl = (Element) namespaceXPath.selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(namespaceEl, defaultNamespace.getNs());
        if (defaultNamespace.getUri() != null && defaultNamespace.getUri().trim().length() != 0) {
            namespaceEl.setAttribute("fingerprint", defaultNamespace.getUri());
        }

        // configure blobstore, indexstore and pubreqs directories
        DOMXPath blobstoreXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/blobstore']/configuration/directory");
        Element blobStoreEl = (Element) blobstoreXPath.selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(blobStoreEl, "${daisy.datadir}" + sep + "blobstore");

        DOMXPath indexstoreXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/fullTextIndex']/configuration/indexDirectory");
        Element indexStoreEl = (Element) indexstoreXPath.selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(indexStoreEl, "${daisy.datadir}" + sep + "indexstore");

        DOMXPath pubreqsXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/publisher/publisher']/configuration/publisherRequestDirectory");
        Element pubreqsEl = (Element) pubreqsXPath.selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(pubreqsEl, "${daisy.datadir}" + sep + "pubreqs");

        // update passwords for internal user
        DOMXPath credentialsXPath = new DOMXPath("//credentials[@key='internal']");
        List<Element> credentialsEls = credentialsXPath.selectNodes(repoServerConfigDoc);
        for (Element el: credentialsEls) {
            el.setAttribute("password", internalUserPassword);
        }

        // update password for workflow user
        DOMXPath workflowUserXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/workflow/workflow-manager']/configuration/workflowUser");
        Element workflowUserEl = (Element) workflowUserXPath.selectSingleNode(repoServerConfigDoc);
        workflowUserEl.setAttribute("password", workflowUserPassword);

        // update driver registrar params
        addJdbcDriverToConf(repoServerConfigDoc, dbParams);

        // update db params
        DOMXPath dataSourceConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/datasource/datasource']/configuration");
        DOMXPath dbUserNameXPath = new DOMXPath("username");
        DOMXPath dbPasswordXPath = new DOMXPath("password");
        DOMXPath dbUrlXPath = new DOMXPath("url");
        Element dataSourceConfEl = (Element) dataSourceConfXPath.selectSingleNode(repoServerConfigDoc);
        Element dbUserNameEl = (Element) dbUserNameXPath.selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbUserNameEl, dbParams.getUser());
        Element dbPasswordEl = (Element) dbPasswordXPath.selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbPasswordEl, dbParams.getPassword());
        Element dbUrlEl = (Element) dbUrlXPath.selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbUrlEl, dbParams.getUrl());

        // workflow params
        DOMXPath workflowConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/workflow/workflow-manager']/configuration");
        DOMXPath hibernateDialectXPath = new DOMXPath(
                "jbpm/hibernate/properties/entry[@key='hibernate.dialect']");
        Element workflowConfEl = (Element) workflowConfXPath.selectSingleNode(repoServerConfigDoc);
        Element hibernateDialectEl = (Element) hibernateDialectXPath.selectSingleNode(workflowConfEl);
        InstallHelper.setElementValue(hibernateDialectEl, dbParams.getHibernateDialect());
        DOMXPath taskUrlXPath = new DOMXPath("taskURL");
        Element taskUrlEl = (Element) taskUrlXPath.selectSingleNode(workflowConfEl);
        String taskUrl = taskUrlEl.getTextContent();
        if (!"localhost".equals(hostname)) {
            taskUrl = taskUrl.replace("localhost", hostname);
        }
        if (taskSite != null) {
            taskUrl = taskUrl.replace("${site}", taskSite);
        }
        if (taskUrlProduction) {
            taskUrl = taskUrl.replace("/daisy/", "/");
        }
        InstallHelper.setElementValue(taskUrlEl, taskUrl);

        // update mail params
        if (smtpServer == null) {
            smtpServer = hostname;
        }
        Element smtpEl = (Element) new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/smtpHost")
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpEl, smtpServer);
        if (smtpPort != 25) {
            smtpEl = (Element) new DOMXPath(
                    "/targets/target[@path='/daisy/emailer/emailer']/configuration")
                    .selectSingleNode(repoServerConfigDoc);
            Element smtpPortElement = repoServerConfigDoc.createElement("smtpPort");
            InstallHelper.setElementValue(smtpPortElement, String.valueOf(smtpPort));
            smtpEl.appendChild(smtpPortElement);
        }
        smtpEl = (Element) new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/useSSL")
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpEl, String.valueOf(smtpSSL));
        smtpEl = (Element) new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/startTLS")
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpEl, String.valueOf(smtpTLS));
        if (smtpUsername != null) {
            smtpEl = (Element) new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration")
                .selectSingleNode(repoServerConfigDoc);
            Element smtpAuthenticationElement = repoServerConfigDoc.createElement("authentication");
            smtpAuthenticationElement.setAttribute("username", smtpUsername);
            smtpAuthenticationElement.setAttribute("password", smtpPassword);
            smtpEl.appendChild(smtpAuthenticationElement);
        }

        if (mailFrom == null) {
            mailFrom = "daisy@" + smtpServer;
        }
        DOMXPath mailFromXPath = new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/fromAddress");
        Element mailFromEl = (Element) mailFromXPath.selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(mailFromEl, mailFrom);

        // set mbean server password to something random
        String jmxPwd = InstallHelper.isDevelopmentSetup() ? "daisyjmx" : Gpw.generate(8);
        DOMXPath mbeanHttpXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmx/mbeanserver']/configuration/httpAdaptor");
        Element mbeanHttpEl = (Element) mbeanHttpXPath.selectSingleNode(repoServerConfigDoc);
        mbeanHttpEl.setAttribute("password", jmxPwd);
        mbeanHttpEl.setAttribute("host", hostname);
        DOMXPath mbeanXmlHttpXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmx/mbeanserver']/configuration/xmlHttpAdaptor");
        Element mbeanXmlHttpEl = (Element) mbeanXmlHttpXPath.selectSingleNode(repoServerConfigDoc);
        mbeanXmlHttpEl.setAttribute("password", jmxPwd);
        mbeanXmlHttpEl.setAttribute("host", hostname);

        InstallHelper.saveDocument(repoServerConfig, repoServerConfigDoc);

        // copy log configuration
        File logConfig = new File(confDir, "repository-log4j.properties");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.LOG_CONF), logConfig);

        // create sample in pubreqs directory
        File samplePubReqs = new File(pubReqsDir, "sample");
        samplePubReqs.mkdirs();
        InstallHelper.copyStream(getClass().getClassLoader().getResourceAsStream(
                "org/outerj/daisy/install/pubreqs-sample/README.txt"), new File(pubReqsDir, "README.txt"));
        InstallHelper.copyStream(getClass().getClassLoader().getResourceAsStream(
                "org/outerj/daisy/install/pubreqs-sample/default.xml"),
                new File(samplePubReqs, "default.xml"));
        InstallHelper.copyStream(getClass().getClassLoader().getResourceAsStream(
                "org/outerj/daisy/install/pubreqs-sample/mapping.xml"),
                new File(samplePubReqs, "mapping.xml"));

        // put README in the plugins directory
        InstallHelper.copyStream(getClass().getClassLoader().getResourceAsStream(
                "org/outerj/daisy/install/plugins-README.txt"), new File(pluginsDir, "README.txt"));
    }

    private File getTemplateConf(TemplateConf conf) {
        String path;
        File base;
        if (InstallHelper.isDevelopmentSetup()) {
            path = conf.devLocation;
            base = InstallHelper.getDaisySourceHome();
        } else {
            path = conf.distLocation;
            base = InstallHelper.getDaisyHome();
        }
        return new File(base, path);
    }

    /**
     * Add the JDBC driver in the config to the list of drivers to be loaded.
     *
     * @return true if config changed, false if not (= when driver was already
     *         present)
     */
    private boolean addJdbcDriverToConf(Document repoServerConfigDoc, DatabaseParams dbParams)
            throws Exception {
        DOMXPath driversXPath = new DOMXPath(
                "/targets/target[@path='/daisy/driverregistrar/driverregistrar']/configuration/drivers");
        Element driversEl = (Element) driversXPath.selectSingleNode(repoServerConfigDoc);

        // Check if driver is already present
        // Note: classname and classpath are inserted in the xpath expression
        // via variables, since
        // XPath does not provide a way to escape single or double quotes in
        // strings
        SimpleVariableContext vars = new SimpleVariableContext();
        vars.setVariableValue("classname", dbParams.getDriverClassName());
        vars.setVariableValue("classpath", dbParams.getDriverClassPath());
        DOMXPath driverPresentXPath = new DOMXPath(
                "count(driver[classname = $classname and classpath = $classpath]) > 0");
        driverPresentXPath.setVariableContext(vars);
        boolean driverPresent = driverPresentXPath.booleanValueOf(driversEl);
        if (driverPresent)
            return false;

        Element driverEl = repoServerConfigDoc.createElement("driver");
        Element driverClassNameEl = repoServerConfigDoc.createElement("classname");
        InstallHelper.setElementValue(driverClassNameEl, dbParams.getDriverClassName());
        Element driverClasspathEl = repoServerConfigDoc.createElement("classpath");
        InstallHelper.setElementValue(driverClasspathEl, dbParams.getDriverClassPath());
        driverEl.appendChild(driverClassNameEl);
        driverEl.appendChild(driverClasspathEl);
        driversEl.appendChild(driverEl);

        addLineBreaks(driversEl);

        return true;
    }

    private void addLineBreaks(Element element) {
        boolean lastWasTextNode = false;
        Node node = element.getFirstChild();
        while (node != null) {
            if (node instanceof Element) {
                if (!lastWasTextNode) {
                    Node text = element.getOwnerDocument().createTextNode("\n");
                    element.insertBefore(text, node);
                }
                addLineBreaks((Element) node);
            }
            lastWasTextNode = node instanceof Text;
            node = node.getNextSibling();
        }

        if (!lastWasTextNode && element.getFirstChild() != null) {
            Node text = element.getOwnerDocument().createTextNode("\n");
            element.appendChild(text);
        }
    }

    private Element createPropertyElement(String name, String value, Document doc) {
        Element el = doc.createElementNS(null, "property");
        el.setAttribute("name", name);
        el.setAttribute("value", value);
        return el;
    }

    private void storeProperties(Properties properties, String comment, File file) throws Exception {
        try {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file);
                properties.store(os, comment);
            } finally {
                if (os != null)
                    os.close();
            }
        } catch (IOException e) {
            throw new Exception("Error storing properties to " + file.getAbsolutePath(), e);
        }
    }

    private enum TemplateConf {
        REPO_CONF("/repository/server/src/conf/myconfig.xml.template",
                "/repository-server/conf/myconfig.xml.template"), LOG_CONF(
                "/repository/server/src/conf/repository-log4j.properties",
                "/repository-server/conf/repository-log4j.properties"), JMS_CONF(
                "/repository/server/src/conf/activemq-conf.xml.template",
                "/repository-server/conf/activemq-conf.xml.template"), LOGIN_CONFIG(
                "/repository/server/src/conf/login.config", "/repository-server/conf/login.config"), GROUPS_PROPS(
                "/repository/server/src/conf/groups.properties", "/repository-server/conf/groups.properties"), USERS_PROPS(
                "/repository/server/src/conf/users.properties", "/repository-server/conf/users.properties");

        String devLocation;
        String distLocation;

        private TemplateConf(String devLocation, String distLocation) {
            this.devLocation = devLocation;
            this.distLocation = distLocation;
        }
    }

    private void jmsConfiguration(DatabaseParams jmsDbParams, String jmsAdminPassword) throws Exception {
        getLog().info("Configuration of embedded JMS service (ActiveMQ)");
        getLog().info("ActiveMQ database parameters");

        File jmsConfig = getTemplateConf(TemplateConf.JMS_CONF);
        File jmsConfigDest = new File(repoDataDir, "conf" + File.separator + "activemq-conf.xml");

        boolean interactiveMode = jmsDbParams == null;
        if (jmsDbParams == null) {
            DatabaseInfo dbInfo = DatabaseInfo.ALL_DATABASES.get("mysql5");
            String defaultDatabaseName = InstallHelper.isDevelopmentSetup() ? "daisydev_activemq"
                    : "activemq";
            jmsDbParams = InstallHelper.collectDatabaseParams(dbInfo, "activemq", "activemq",
                    defaultDatabaseName);
        }

        if (interactiveMode && !jmsDbParams.isDatabaseEmpty()) {
            getLog().error("ActiveMQ database not empty, can't continue.");
            throw new MojoFailureException("ActiveMQ database not empty, can't continue.");
        }

        getLog().info("Creating ActiveMQ configuration");

        // Copy configuration files
        getLog().info("Installing configuration files...");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.LOGIN_CONFIG), new File(jmsConfigDest
                .getParentFile(), "/login.config"));
        InstallHelper.copyFile(getTemplateConf(TemplateConf.GROUPS_PROPS), new File(jmsConfigDest
                .getParentFile(), "/groups.properties"));
        InstallHelper.copyFile(getTemplateConf(TemplateConf.USERS_PROPS), new File(jmsConfigDest
                .getParentFile(), "/users.properties"));

        // Update activemq configuration
        getLog().info("Configuring activemq.conf file...");
        Document configDoc = InstallHelper.parseFile(jmsConfig);

        Element dbElement = configDoc.createElementNS(null, "bean");
        dbElement.setAttribute("id", "dataSource");
        dbElement.setAttribute("class", "org.apache.commons.dbcp.BasicDataSource");
        dbElement.setAttribute("destroy-method", "close");

        String activeMQJdbcURL = jmsDbParams.getUrl();
        if (activeMQJdbcURL.startsWith("jdbc:mysql:")) {
            String extraParams = "relaxAutoCommit=true&sessionVariables=storage_engine=InnoDB";
            if (activeMQJdbcURL.indexOf('?') != -1)
                activeMQJdbcURL += "&" + extraParams;
            else
                activeMQJdbcURL += "?" + extraParams;
        }
        dbElement.appendChild(createPropertyElement("url", activeMQJdbcURL, configDoc));
        dbElement.appendChild(createPropertyElement("username", jmsDbParams.getUser(), configDoc));
        dbElement.appendChild(createPropertyElement("password", jmsDbParams.getPassword(), configDoc));
        dbElement.appendChild(createPropertyElement("defaultTransactionIsolation", "2", configDoc));
        addLineBreaks(dbElement);

        DOMXPath dbConfigXPath = new DOMXPath("/beans/bean[@id='dataSource']");
        Element oldDbConfig = (Element) dbConfigXPath.selectSingleNode(configDoc);
        oldDbConfig.getParentNode().replaceChild(dbElement, oldDbConfig);

        // Configure ActiveMQ dataDirectory. This is a fixed value but it is
        // configured here since
        // it contains the ${daisy.datadir} property which does not exist in
        // development setups.
        // Note that while we only use JDBC persistence, ActiveMQ still insists
        // on creating this
        // directory anyway (but it is not used as far as I see).
        DOMXPath brokerXPath = new DOMXPath("/beans/amq:broker");
        brokerXPath.addNamespace("amq", "http://activemq.org/config/1.0");
        Element brokerEl = (Element) brokerXPath.selectSingleNode(configDoc);
        brokerEl.setAttribute("dataDirectory", "file:${daisy.datadir}/activemq-data");

        // fix the tcp host
        DOMXPath transportConnectorXPath = new DOMXPath("amq:transportConnectors/amq:transportConnector");
        transportConnectorXPath.addNamespace("amq", "http://activemq.org/config/1.0");
        List<Element> transportConnectors = (List<Element>) transportConnectorXPath.selectNodes(brokerEl);
        for (Element transportConnector : transportConnectors) {
            String uri = transportConnector.getAttribute("uri");
            uri = uri.replace("localhost", hostname);
            transportConnector.setAttribute("uri", uri);
        }

        InstallHelper.saveDocument(jmsConfigDest, configDoc);

        // Update users.properties
        getLog().info("Configuring users.properties file...");
        File usersFile = new File(jmsConfigDest.getParentFile(), "users.properties");
        Properties activeMqUsers = new Properties();
        activeMqUsers.put("admin", jmsAdminPassword);
        storeProperties(activeMqUsers, "Users for ActiveMQ", usersFile);

        // Update myconfig.xml
        getLog().info("Configuring myconfig.xml with the JMS settings...");
        File repoServerConfig = new File(repoDataDir, "conf" + File.separator + "myconfig.xml");
        Document repoServerConfigDoc = InstallHelper.parseFile(repoServerConfig);

        DOMXPath credentialsXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/credentials");
        Element credentialsEl = (Element) credentialsXPath.selectSingleNode(repoServerConfigDoc);
        credentialsEl.setAttribute("password", jmsAdminPassword);

        DOMXPath amqConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/initialContext/property[@name='java.naming.provider.url']/@value");
        Node amqConfEl = (Node) amqConfXPath.selectSingleNode(repoServerConfigDoc);
        amqConfEl
                .setNodeValue("vm://DaisyJMS?brokerConfig=${double-url-encode:xbean:${file-to-uri:${daisy.datadir}/conf/activemq-conf.xml}}&jms.dispatchAsync=true");

        addJdbcDriverToConf(repoServerConfigDoc, jmsDbParams);

        InstallHelper.saveDocument(repoServerConfig, repoServerConfigDoc);

        getLog().info("ActiveMQ configuration done.");
    }

    /*
     * The following is stuff extracted from DaisyWikiInit
     */

    private void installGuestUser() throws Exception {
        getLog().info("Creating guest user and role.");
        UserManager userManager = getRepository().getUserManager();
        Role role;
        try {
            role = userManager.getRole("guest", false);
            getLog().info("Existing guest role found, id = " + role.getId());
        } catch (RoleNotFoundException e) {
            role = userManager.createRole("guest");
            role.save();
            getLog().info("Guest role created, id = " + role.getId());
        }
        guestRoleId = role.getId();

        User user;
        try {
            user = userManager.getUser("guest", false);
            getLog().info("Existing guest user found, id = " + user.getId());
        } catch (UserNotFoundException e) {
            user = userManager.createUser("guest");
            user.setPassword("guest");
            user.addToRole(role);
            user.save();
            getLog().info("Guest user created, id = " + user.getId());
        }
    }

    private void installACL() throws Exception {
        getLog().info("ACL initialisation");

        AccessManager accessManager = getRepository().getAccessManager();
        Acl acl = accessManager.getStagingAcl();
        if (acl.size() > 0) {
            getLog().info("ACL is not empty -- will not touch it.");
            return;
        }

        getLog().info("A default ACL will be installed. This will limit the users with role 'guest'");
        getLog().info("to read operations. All other users will have both read and write privileges.");

        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(AclSubjectType.EVERYONE, -1);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.GRANT);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.GRANT);
        aclEntry.set(AclPermission.DELETE, AclActionType.GRANT);
        aclObject.add(aclEntry);

        aclEntry = aclObject.createNewEntry(AclSubjectType.ROLE, guestRoleId);
        aclEntry.set(AclPermission.READ, AclActionType.GRANT);
        aclEntry.set(AclPermission.WRITE, AclActionType.DENY);
        aclEntry.set(AclPermission.PUBLISH, AclActionType.DENY);
        aclEntry.set(AclPermission.DELETE, AclActionType.DENY);
        aclObject.add(aclEntry);

        acl.save();
        accessManager.copyStagingToLive();

        getLog().info("ACL configured.");
    }

    private void installSchema() throws Exception {
        getLog().info("Schema creation");
        load(
                getClass().getClassLoader().getResourceAsStream(
                        "org/outerj/daisy/install/daisywiki_schema.xml"), getRepository());
    }

    void load(InputStream is, Repository repository) throws Exception {
        ImpExpSchema impExpSchema = ImpExpSchemaDexmlizer.fromXml(is, repository,
                new ImpExpSchemaDexmlizer.Listener() {
                    public void info(String message) {
                        getLog().info(message);
                    }
                });

        SchemaLoader.load(impExpSchema, repository, false, false, new MySchemaLoadListener());
    }

    class MySchemaLoadListener implements SchemaLoadListener {
        public void conflictingFieldType(String fieldTypeName, ValueType requiredType, ValueType foundType)
                throws Exception {
            getLog().warn(
                    "WARNING!!! Field type " + fieldTypeName
                            + " already exists, and has a different value type: expected " + requiredType
                            + " but is " + foundType);
        }

        public void conflictingMultiValue(String fieldTypeName, boolean needMultivalue,
                boolean foundMultivalue) throws Exception {
            getLog().warn(
                    "WARNING!!! Field type " + fieldTypeName
                            + " already exists, and has a different multi-value property: expected "
                            + needMultivalue + " but is " + foundMultivalue);
        }

        public void conflictingHierarchical(String fieldTypeName, boolean needHierarchical,
                boolean foundHierarchical) throws Exception {
            getLog().warn(
                    "WARNING!!! Field type " + fieldTypeName
                            + " already exists, and has a different hierarchical property: expected "
                            + needHierarchical + " but is " + foundHierarchical);
        }

        public void fieldTypeLoaded(String fieldTypeName, SchemaLoadResult result) {
            getLog().info("Field type " + fieldTypeName + " : " + result);
        }

        public void partTypeLoaded(String partTypeName, SchemaLoadResult result) {
            getLog().info("Part type " + partTypeName + " : " + result);
        }

        public void documentTypeLoaded(String documentTypeName, SchemaLoadResult result) {
            getLog().info("Document type " + documentTypeName + " : " + result);
        }

        public void done() {
            getLog().info("Done.");
        }

        public boolean isInterrupted() {
            return false;
        }
    }

    /*
     * The following is extracted from DaisyWikiInitDataDir
     */

    public void installWikiDataDirectory() throws Exception {
        createDirectory();
        xconfFile = new File(wikiDataDir, "daisy.xconf");
        createRegistrar();
        updateXconf();
    }

    private void createDirectory() throws Exception {
        getLog().info("Copying template data directory...");
        File dataDirTemplate;
        if (InstallHelper.isDevelopmentSetup()) {
            File daisySourceHome = InstallHelper.getDaisySourceHome();
            dataDirTemplate = new File(daisySourceHome, "applications/daisywiki/frontend/src/cocoon/wikidata");
        } else {
            File daisyHome = InstallHelper.getDaisyHome();
            dataDirTemplate = new File(daisyHome, "daisywiki/wikidata-template");
        }
        InstallHelper.copyFile(dataDirTemplate, wikiDataDir);
        getLog().info("Done.");
    }

    private void createRegistrar() throws Exception {
        // FIXME 2 wikis on same repository : 2nd wiki will not set up registrar
        // user since he already exists. Password must be grabbed from some
        // source ( original daisy.xconf ? )
        getLog().info("Creating registrar user.");

        String suggestPwd = InstallHelper.isDevelopmentSetup() ? "defaultpwd" : InstallHelper
                .generatePassword();
        UserManager userManager = getRepository().getUserManager();
        User user;
        boolean isNewUser = true;
        try {
            user = userManager.getUser("registrar", true);
            getLog().info("Existing registrar user found, id = " + user.getId());
            isNewUser = false;
        } catch (UserNotFoundException e) {
            registrarPassword = registrarPassword == null ? suggestPwd : registrarPassword;

            user = userManager.createUser("registrar");
            user.setPassword(registrarPassword);
            Role adminRole = userManager.getRole(Role.ADMINISTRATOR, false);
            user.addToRole(adminRole);
            user.setDefaultRole(adminRole);
            user.save();
            getLog().info("Registrar user created, id = " + user.getId());
        }

        if (!xconfFile.exists()) {
            System.out.println("daisy.xconf file does not exist at " + xconfFile.getAbsolutePath()
                    + ", skipping automatic update.");
        } else {
            if (isUpdateXconf) {
                // if (!isNewUser && InstallHelper.isDevelopmentSetup()) {
                // settings.setRegistrarPassword("defaultpwd");
                if (!isNewUser) {
                    getLog()
                            .warn(
                                    "The registrar already existed, please verify afterwards if the password in daisy.xconf matches.");
                }
                getLog().info("Will now update the daisy.xconf with the registrar user password.");
                Document xconfDocument = InstallHelper.parseFile(xconfFile);
                DOMXPath registrarXPath = new DOMXPath(
                        "/cocoon/component[@role='org.outerj.daisy.frontend.components.userregistrar.UserRegistrar']/registrarUser");
                Element registrarEl = (Element) registrarXPath.selectSingleNode(xconfDocument);
                registrarEl.setAttribute("password", registrarPassword);
                InstallHelper.saveDocument(xconfFile, xconfDocument);
            }
        }
    }

    private void updateXconf() throws Exception {
        // dev setup uses default passwords, no updates required
        if (InstallHelper.isDevelopmentSetup())
            return;

        getLog().info("Configuring daisy.xconf");

        // Either grab settings from myconfig or prompt user
        if (!xconfFile.exists()) {
            getLog()
                    .info(
                            "Did not find daisy.xconf at " + xconfFile.getAbsolutePath()
                                    + ", will skip updating it.");
        } else {
            String jmsAdminPassword = null;
            File myconfig = new File(repoDataDir, "conf" + File.separator + "myconfig.xml");
            if (repoDataDir != null && repoDataDir.exists() && myconfig.exists()) {
                Document myconfigDoc = InstallHelper.parseFile(myconfig);
                DOMXPath jmsAdminPwdXpath = new DOMXPath(
                        "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/credentials/@password");
                jmsAdminPassword = jmsAdminPwdXpath.stringValueOf(myconfigDoc);
            } else {
                getLog().warn(
                        "The myconfig.xml file could not be found at this location : "
                                + myconfig.getAbsolutePath() + ".");
            }

            if (jmsAdminPassword == null) {
                getLog().warn("Certain values necessary for the installation could not be obtained.");
            }

            Document xconfDoc = InstallHelper.parseFile(xconfFile);

            // Update jms setup
            DOMXPath jmsCredentialsXPath = new DOMXPath(
                    "/cocoon/component[@class='org.outerj.daisy.jms.impl.JmsClientImpl']/jmsConnection/credentials");
            Element jmsCredentialsEl = (Element) jmsCredentialsXPath.selectSingleNode(xconfDoc);
            jmsCredentialsEl.setAttribute("password", jmsAdminPassword);

            getLog().info("Will now save the updated daisy.xconf");
            InstallHelper.saveDocument(xconfFile, xconfDoc);
        }

    }

    /*
     * The following is extracted from DaisyWikiAddSite
     */

    public void addWikiSite() throws Exception {
        getLog().info("Adding initial site.");
        createCollection(siteName);
        addLanguages();
        createInitialDocuments();
        generateSiteConfs();
    }

    private void createCollection(String collectionName) throws Exception {
        getLog().info("Creating collection " + collectionName);
        CollectionManager collectionManager = getRepository().getCollectionManager();
        boolean collectionExists = false;
        try {
            collection = collectionManager.getCollectionByName(collectionName, false);
            collectionExists = true;
            getLog().info("*** Collection warning (harmless) ***");
            getLog().info("A collection with the name " + collectionName + " already exists, it will be re-used.");
        } catch (CollectionNotFoundException e) {
            /* ignore */
        }

        if (!collectionExists) {
            collection = getRepository().getCollectionManager().createCollection(collectionName);
            collection.save();
            getLog().info("Collection created.");
        }
    }

    private void addLanguages() throws Exception {
        if (defaultLanguage == null) {
            if (languages != null) {
                defaultLanguage = languages[0];
            } else {
                defaultLanguage = daisyConfig.getLocale().getLanguage();
            }
        }

        if (languages == null)
            languages = new String[] { defaultLanguage };

        for (String language : languages) {
            try {
                // fetch site language to check its existance
                getRepository().getVariantManager().getLanguageByName(language, false).getName();
            } catch (LanguageNotFoundException e) {
                Language daisyLanguage = getRepository().getVariantManager().createLanguage(language);
                daisyLanguage.save();
            }
        }
    }

    private void createInitialDocuments() throws Exception {

        createSampleDoc(defaultLanguage);
        createNavigationDoc(defaultLanguage);
        updateNavigationDocument(navigationDoc);

        for (String siteLanguage : languages) {
            if (siteLanguage.equals(defaultLanguage))
                continue;
            getLog().info("Creating variant for sample document. (" + siteLanguage + ")");
            getRepository().createVariant(sampleDoc.getId(), Branch.MAIN_BRANCH_NAME, defaultLanguage, -1,
                    Branch.MAIN_BRANCH_NAME, siteLanguage, true);
            getLog().info("Creating variant for navigation document. (" + siteLanguage + ")");
            getRepository().createVariant(navigationDoc.getId(), Branch.MAIN_BRANCH_NAME, defaultLanguage,
                    -1, Branch.MAIN_BRANCH_NAME, siteLanguage, true);
        }
    }

    private void updateNavigationDocument(org.outerj.daisy.repository.Document document)
            throws DocumentTypeInconsistencyException, UnsupportedEncodingException, RepositoryException {
        //
        // Update navigation to link to sample doc
        //
        getLog().info("Updating navigation document.");
        StringBuilder navigation = new StringBuilder();
        navigation.append("<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'>\n");
        navigation.append("  <d:collections>\n");
        navigation.append("    <d:collection name='").append(siteName).append("'/>\n");
        navigation.append("  </d:collections>\n");
        navigation.append("  <d:doc id='").append(sampleDoc.getId()).append("' label='").append(
                sampleDoc.getName()).append("'/>\n");
        navigation.append("  <d:group label='All documents A->Z'>\n");
        navigation.append("    <d:query q='select name where true order by name'/>\n");
        navigation.append("  </d:group>\n");
        navigation.append("</d:navigationTree>");
        document.setPart("NavigationDescription", "text/xml", navigation.toString().getBytes("UTF-8"));
        document.save();
        getLog().info("Navigation document updated.");
    }

    private void createNavigationDoc(String siteLanguage) throws Exception {
        getLog().info("Creating navigation document. (" + siteLanguage + ")");
        navigationDoc = getRepository().createDocument("Navigation for " + siteName, "Navigation",
                Branch.MAIN_BRANCH_NAME, siteLanguage);
        String emptyNavigation = "<d:navigationTree xmlns:d='http://outerx.org/daisy/1.0#navigationspec'/>";
        navigationDoc.setPart("NavigationDescription", "text/xml", emptyNavigation.getBytes("UTF-8"));
        navigationDoc.addToCollection(collection);
        if (defaultLanguage != null) {
            navigationDoc.setReferenceLanguageId(getRepository().getVariantManager().getLanguage(
                    defaultLanguage, false).getId());
        }
        navigationDoc.save();
        getLog().info("Navigation document created, id = " + navigationDoc.getId());
    }

    private void createSampleDoc(String siteLanguage) throws Exception {
        getLog().info("Creating sample document. (" + siteLanguage + ")");
        sampleDoc = getRepository().createDocument(siteName + " home", "SimpleDocument",
                Branch.MAIN_BRANCH_NAME, siteLanguage);
        String content = "<html><body><h1>Welcome!</h1><p>Hello, welcome to your new site.</p></body></html>";
        sampleDoc.setPart("SimpleDocumentContent", "text/xml", content.getBytes("UTF-8"));
        sampleDoc.addToCollection(collection);
        if (defaultLanguage != null) {
            sampleDoc.setReferenceLanguageId(getRepository().getVariantManager().getLanguage(defaultLanguage,
                    false).getId());
        }
        sampleDoc.save();
        getLog().info("Sample document created, id = " + sampleDoc.getId());
    }

    private void generateSiteConfs() throws Exception {
        for (String siteLanguage : languages) {
            StringBuilder siteConfBuffer = new StringBuilder();
            siteConfBuffer.append("<siteconf xmlns=\"http://outerx.org/daisy/1.0#siteconf\">");
            StringBuffer siteTitle = new StringBuffer(siteName);
            if (languages.length > 1) {
                siteTitle.append(" (").append(siteLanguage).append(")");
            }
            siteConfBuffer.append("\n  <title>").append(siteTitle).append("</title>");
            siteConfBuffer.append("\n  <description>The \"").append(siteTitle)
                    .append("\" site</description>");
            siteConfBuffer.append("\n  <skin>default</skin>");
            siteConfBuffer.append("\n  <navigationDocId>").append(navigationDoc.getId()).append(
                    "</navigationDocId>");
            siteConfBuffer.append("\n  <homepageDocId>").append(sampleDoc.getId()).append("</homepageDocId>");
            siteConfBuffer.append("\n  <collectionId>").append(collection.getId()).append("</collectionId>");
            siteConfBuffer.append("\n  <contextualizedTree>false</contextualizedTree>");
            siteConfBuffer.append("\n  <branch>").append(Branch.MAIN_BRANCH_NAME).append("</branch>");
            siteConfBuffer.append("\n  <language>").append(siteLanguage).append("</language>");
            if (defaultLanguage != null) {
                siteConfBuffer.append("\n  <defaultReferenceLanguage>").append(defaultLanguage).append(
                        "</defaultReferenceLanguage>");
            }
            siteConfBuffer.append("\n  <defaultDocumentType>SimpleDocument</defaultDocumentType>");
            siteConfBuffer.append("\n  <newVersionStateDefault>publish</newVersionStateDefault>");
            siteConfBuffer.append("\n  <locking>");
            siteConfBuffer
                    .append("\n    <automatic lockType='pessimistic' defaultTime='15' autoExtend='true'/>");
            siteConfBuffer.append("\n  </locking>");
            siteConfBuffer.append("\n</siteconf>");
            String siteConf = siteConfBuffer.toString();

            File newDir;
            if (languages.length > 1) {
                newDir = new File(new File(wikiDataDir, "sites"), siteName + "-" + siteLanguage);
            } else {
                newDir = new File(new File(wikiDataDir, "sites"), siteName);
            }
            newDir.mkdir();
            File siteConfFile = new File(newDir, "siteconf.xml");
            if (siteConfFile.exists()) {
                InstallHelper.backupFile(siteConfFile);
            }

            OutputStream os = new FileOutputStream(siteConfFile);
            try {
                OutputStreamWriter writer = new OutputStreamWriter(os, "UTF-8");
                writer.write(siteConf);
                writer.flush();
            } finally {
                os.close();
            }
            getLog().info("Written " + siteConfFile.getAbsolutePath());

            File cocoonExtDir = new File(newDir, "cocoon");
            cocoonExtDir.mkdir();
            File sitemap = new File(cocoonExtDir, "sitemap.xmap");
            os = new FileOutputStream(sitemap);
            OutputStreamWriter writer = null;
            try {
                writer = new OutputStreamWriter(os, "UTF-8");
                writer.write(readResource("org/outerj/daisy/install/sample-extension-sitemap.xml"));
                writer.flush();
            } finally {
                if (writer != null)
                    writer.close();
                os.close();
            }
        }
    }

    String readResource(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        Reader reader = new InputStreamReader(is, "UTF-8");
        BufferedReader bufferedReader = new BufferedReader(reader);

        StringBuilder buffer = new StringBuilder();
        int c = bufferedReader.read();
        while (c != -1) {
            buffer.append((char) c);
            c = bufferedReader.read();
        }

        return buffer.toString();
    }

}
