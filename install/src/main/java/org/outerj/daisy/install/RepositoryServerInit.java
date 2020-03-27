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
package org.outerj.daisy.install;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.jaxen.SimpleVariableContext;
import org.jaxen.dom.DOMXPath;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.util.Gpw;
import org.outerj.daisy.util.VersionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

public class RepositoryServerInit {

    private Log log = LogFactory.getLog(RepositoryServerInit.class);

    private Properties properties = null;
    private boolean noQuestions = false;
    private boolean forceInstall = false;

    // database params
    private DatabaseParams dbParams;
    private DatabaseParams jmsDbParams;

    // repository data directories
    private String repoDataPath;
    private String blobStorePath;
    private String indexStorePath;
    private String pubReqsPath;
    private String logPath;
    private String confPath;
    private String pluginsPath;
    private String pluginsBeforeRepoPath;
    private String pluginsAfterRepoPath;

    // repository namespace & credentials
    private String namespace;
    private String fingerprint;
    private String adminUser;
    private String adminPass;
    private String internalUser;
    private String internalPass;
    private String workflowUser;
    private String workflowPass;

    // misc
    private String smtpHost;
    private String smtpPort;
    private String smtpSSL;
    private String smtpTLS;
    private String smtpUser;
    private String smtpPass;

    private String mailFrom;
    private String jmsAdminPass;

    /**
     * Problems which can be safely ignored (default result = continue)
     */
    private List<String> warnings = new ArrayList<String>();
    /**
     * Problems which are more severe (default result = cancel installation)
     */
    private List<String> minorProblems = new ArrayList<String>();
    /**
     * Problems which can not be ignored. (Always cancels installation)
     */
    private List<String> majorProblems = new ArrayList<String>();

    public static void main(String[] args) throws Exception {
        new RepositoryServerInit().install(args);
    }

    private void install(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(new Option("i", "install", true,
                "Installation properties file"));
        options.addOption(new Option("d", "dump", true,
                "Create an example installation properties file"));
        options.addOption(new Option("a", "autoinstall", false,
                "Automated installation. Assumes the default answer for additional questions"));
        options.addOption(new Option(
                "f",
                "force",
                false,
                "Force automated installation. Will continue installation after detectig minor problems"));
        options.addOption(new Option("v", "version", true, "Print version"));
        options.addOption(new Option("h", "help", false, "Print version info"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption('h')) {
            showUsage(options);
            System.exit(0);
        } else if (cmd.hasOption('v')) {
            log.info(VersionHelper.getVersionString(
                    getClass().getClassLoader(),
                    "org/outerj/daisy/install/versioninfo.properties"));
            System.exit(0);
        }

        boolean dump = cmd.hasOption('d');
        boolean install = cmd.hasOption('i');
        if (!dump && !install) {
            showUsage(options);
            System.exit(0);
        }
        if (dump && install) {
            log.info("You should use -d or -i <file>, but not both.");
            System.exit(0);
        }

        noQuestions = cmd.hasOption('a');
        forceInstall = cmd.hasOption('f');

        if (dump) {
            File dumpFile = new File(cmd.getOptionValue('d'));

            String input = IOUtils
                    .toString(getClass()
                            .getClassLoader()
                            .getResourceAsStream(
                                    "org/outerj/daisy/install/daisy-repository-init.properties"));
            if (InstallHelper.isDevelopmentSetup()) {
                Pattern p = Pattern.compile("^repo.datadir=.*$",
                        Pattern.MULTILINE);
                input = p
                        .matcher(input)
                        .replaceFirst(
                                Matcher.quoteReplacement("repo.datadir=${daisy.sourcehome}/../devrepodata"));
                p = Pattern.compile("^repo.dbName=.*$", Pattern.MULTILINE);
                input = p
                        .matcher(input)
                        .replaceFirst(
                                Matcher.quoteReplacement("repo.dbName=daisydev_repository"));
                p = Pattern.compile("^repo.jms.dbName=.*$", Pattern.MULTILINE);
                input = p
                        .matcher(input)
                        .replaceFirst(
                                Matcher.quoteReplacement("repo.jms.dbName=daisydev_activemq"));
                p = Pattern.compile("^#\\ repo.jms.adminpass=.*$",
                        Pattern.MULTILINE);
                input = p
                        .matcher(input)
                        .replaceFirst(
                                Matcher.quoteReplacement("repo.jms.adminpass=jmsadmin"));
            }
            IOUtils.write(input, new FileOutputStream(dumpFile));
        } else if (install) {
            Properties installProps = new Properties();
            installProps.load(new FileInputStream(cmd.getOptionValue('i')));

            Properties props = new Properties();
            props.putAll(System.getProperties());
            props.putAll(installProps);

            SimpleLog simpleLog = new SimpleLog("daisy-repository-init");
            simpleLog.setLevel(SimpleLog.LOG_LEVEL_INFO);
            install(props, simpleLog);
        }
    }

    private void showUsage(Options options) {
        new HelpFormatter().printHelp("daisy-repository-init", options);
    }

    public void install(Properties properties) throws Exception {
        install(properties, null); // use the defeault log
    }

    public void install(Properties properties, Log log) throws Exception {
        this.log = log;
        if (this.properties != null) {
            throw new RuntimeException(
                    "install(properties) should be invoked only once. You should call this method on a new instance");
        }
        this.properties = properties;
        install();
    }

    private void install() throws Exception {
        checkProperties();
        if (majorProblems.size() > 0) {
            log.error("There were some major problems:");
            for (String p : majorProblems)
                log.error("          * " + p);
            log.error("Installation cannot continue");
            return;
        }
        if (minorProblems.size() > 0) {
            log.warn("There were some minor problems:");
            for (String p : minorProblems)
                log.warn("          * " + p);
        }
        if (warnings.size() > 0) {
            log.info("There are some warnings");
            for (String p : warnings)
                log.info("          * " + p);
        }

        if (noQuestions) {
            if (minorProblems.size() > 0 && !forceInstall) {
                log.info("Not continuing with automated installation. (add --force)");
                return;
            }
        } else {
            if (minorProblems.size() > 0) {
                if (!InstallHelper.promptYesNo(
                        "Continue anyway? [yes/no, default: no]", false)) {
                    log.info("Installation cancelled.");
                    return;
                }
            } else if (warnings.size() > 0) {
                if (!InstallHelper.promptYesNo(
                        "Continue anyway? [yes/no, default: yes]", true)) {
                    log.info("Installation cancelled.");
                    return;
                }
            } else {
                if (!InstallHelper.promptYesNo(
                        "Continue with installation? [yes/no, default: yes]",
                        true)) {
                    log.info("Installation cancelled.");
                    return;
                }
            }
        }

        log.info("Starting installation");

        log.info("Repository database initialising");
        setupRepoDb();
        log.info("Repository database initialised");

        repoDataPath = p("repo.datadir");

        if (repoDataPath != null && repoDataPath.trim().length() > 0) {
            log.info("Creating repository data directory.");
            setupRepoData();
            log.info("Created repository data directory.");

            log.info("ActiveMQ database initialising");
            setupActiveMQ();
            log.info("ActiveMQ database initialised");
        } else {
            log.info("Not creating repository data directory.");
        }

        log.info("Daisy repository server configuration finished.");
    }

    private void checkProperties() throws Exception {
        dbParams = getDbParams("repo.");
        checkDatabase("the repository database", dbParams);

        repoDataPath = p("repo.datadir");
        if (repoDataPath != null) {
            boolean repoDataDirEmpty = InstallHelper.verifyEmptyDirectory(
                    repoDataPath, majorProblems);

            blobStorePath = p("repo.blobstore.dir",
                    "${repo.datadir}${file.separator}blobstore");
            indexStorePath = p("repo.indexstore.dir",
                    "${repo.datadir}${file.separator}indexstore");
            pubReqsPath = p("repo.pubreqs.dir",
                    "${repo.datadir}${file.separator}pubreqs");
            logPath = p("repo.logs.dir", "${repo.datadir}${file.separator}logs");
            confPath = p("repo.conf.dir",
                    "${repo.datadir}${file.separator}conf");
            pluginsPath = p("repo.plugins.dir",
                    "${repo.datadir}${file.separator}plugins");
            pluginsBeforeRepoPath = p("repo.loadbefore.dir", new File(
                    pluginsPath, "load-before-repository").getPath());
            pluginsAfterRepoPath = p("repo.loadafter.dir", new File(
                    pluginsPath, "load-after-repository").getPath());

            if (repoDataDirEmpty) {
                InstallHelper
                        .verifyEmptyDirectory(blobStorePath, majorProblems);
                InstallHelper.verifyEmptyDirectory(indexStorePath,
                        majorProblems);
                InstallHelper.verifyEmptyDirectory(pubReqsPath, majorProblems);
                InstallHelper.verifyEmptyDirectory(logPath, majorProblems);
                InstallHelper.verifyEmptyDirectory(confPath, majorProblems);
                InstallHelper.verifyEmptyDirectory(pluginsPath, majorProblems);
                InstallHelper.verifyEmptyDirectory(pluginsBeforeRepoPath,
                        majorProblems);
                InstallHelper.verifyEmptyDirectory(pluginsAfterRepoPath,
                        majorProblems);
            }

            adminUser = p("repo.adminUser");
            adminPass = p("repo.adminPass");
            internalUser = p("repo.internalUser", "internal");
            internalPass = p("repo.internalPass",
                    InstallHelper.isDevelopmentSetup() ? "defaultpwd"
                            : InstallHelper.generatePassword());
            workflowUser = p("repo.workflowUser", "workflow");
            workflowPass = p("repo.workflowPass",
                    InstallHelper.isDevelopmentSetup() ? "defaultpwd"
                            : InstallHelper.generatePassword());

            namespace = p("repo.namespace", "DSY");

            String namespaceNamePattern = "^[a-zA-Z0-9_]{1,200}$";
            Pattern namespaceNameRegexp = Pattern.compile(namespaceNamePattern);
            String namespaceLC = namespace.toLowerCase();
            if (!namespaceNameRegexp.matcher(namespace).matches()) {
                log.info("Invalid namespace name: it should confirm to regexp "
                        + namespaceNamePattern);
            } else if (namespaceLC.startsWith("daisy")
                    || (namespaceLC.startsWith("dsy") && namespaceLC.length() > "dsy"
                            .length())) {
                log.info("Invalid namespace name: all names starting with DSY or equal to or starting with DAISY are reserved (case insensitive restriction).");
            }

            fingerprint = p("repo.fingerprint", null);

            smtpHost = p("repo.mail.smtp.host", "undefined");
            smtpSSL = p("repo.mail.stmp.useSSL", "false");
            smtpTLS = p("repo.mail.stmp.useTLS", "false");
            smtpUser = p("repo.mail.stmp.user", null);
            smtpPass = p("repo.mail.stmp.pass", null);
            smtpPort = p("repo.mail.smtp.port", null);
            mailFrom = p("repo.mail.from", "undefined@undefined._com");

            jmsDbParams = getDbParams("repo.jms.");
            checkDatabase("the activemq database", jmsDbParams);

            jmsAdminPass = p("repo.jms.adminpass", Gpw.generate(8));
        }

    }

    private void checkDatabase(String humanDbName, DatabaseParams p)
            throws Exception {
        p.checkDatabase(humanDbName, warnings, minorProblems, majorProblems);
    }

    private DatabaseParams getDbParams(String prefix) throws Exception {
        String dbType = p(prefix, "mysql5");
        DatabaseInfo dbInfo = DatabaseInfo.ALL_DATABASES.get(dbType);
        if (dbInfo == null) {
            throw new RuntimeException("Unknown database type " + dbType);
        }

        String dbName = p(prefix + "dbName");
        String user = p(prefix + "dbUser");
        String password = p(prefix + "dbPass");

        String typePrefix = prefix + "db." + dbType + ".";
        String url = p(typePrefix + "url", dbInfo.getDriverUrl(dbName));
        String driverClass = p(typePrefix + "driverClass",
                dbInfo.getDriverClass());
        String driverClasspath = p(
                typePrefix + "driverClasspath",
                InstallHelper.getRepoLocation() + File.separator
                        + dbInfo.getDriverPath());
        String hibernateDialect = p(typePrefix + "hibernateDialect",
                dbInfo.getHibernateDialect());

        DatabaseParams result = new DatabaseParams(dbName, url, user, password,
                driverClass, driverClasspath, hibernateDialect,
                dbInfo.getValidator());
        log.debug("Registering driver...");
        result.loadDriver();
        log.debug("Successful.");

        return result;
    }

    private void setupRepoDb() throws Exception {
        if (!dbParams.isDatabaseEmpty()) {
            log.info("Clearing repository database");
            dbParams.clearDatabase();
        }
        DatabaseCreator dbCreator = new DatabaseCreator();
        List<DatabaseCreator.NewUserInfo> newUserInfos = DatabaseCreator
                .createDefaultUsers(adminUser, adminPass, internalUser,
                        internalPass, workflowUser, workflowPass);
        dbCreator.run(dbParams.getUrl(), dbParams.getUser(),
                dbParams.getPassword(), newUserInfos);
    }

    private void setupRepoData() throws Exception {
        log.info("Will create Daisy data directory at:" + repoDataPath);

        new File(repoDataPath).mkdirs();

        File blobStore = new File(blobStorePath);
        blobStore.mkdirs();
        File indexStoreDir = new File(indexStorePath);
        indexStoreDir.mkdirs();
        File pubReqsDir = new File(pubReqsPath);
        pubReqsDir.mkdirs();
        File logDir = new File(logPath);
        logDir.mkdirs();
        File confDir = new File(confPath);
        confDir.mkdirs();
        File pluginsDir = new File(pluginsPath);
        pluginsDir.mkdirs();
        File pluginsBeforeRepoDir = new File(pluginsBeforeRepoPath);
        pluginsBeforeRepoDir.mkdirs();
        File pluginsAfterRepoDir = new File(pluginsAfterRepoPath);
        pluginsAfterRepoDir.mkdirs();

        // By using slash as path separator, the configuration will work on both
        // Windows and Unix systems,
        // allowing to easier move a Daisy installation between these systems.
        String sep = "/";

        // create config override file
        File repoServerConfig = new File(confDir, "myconfig.xml");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.REPO_CONF),
                repoServerConfig);
        Document repoServerConfigDoc = InstallHelper
                .parseFile(repoServerConfig);

        // configure namespace
        DOMXPath namespaceXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/repository-manager']/configuration/namespace");
        Element namespaceEl = (Element) namespaceXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(namespaceEl, namespace);
        if (fingerprint != null) {
            namespaceEl.setAttribute("fingerprint", fingerprint);
        }

        // configure blobstore, indexstore and pubreqs directories
        DOMXPath blobstoreXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/blobstore']/configuration/directory");
        Element blobStoreEl = (Element) blobstoreXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(blobStoreEl, "${daisy.datadir}" + sep
                + "blobstore");

        DOMXPath indexstoreXPath = new DOMXPath(
                "/targets/target[@path='/daisy/repository/fullTextIndex']/configuration/indexDirectory");
        Element indexStoreEl = (Element) indexstoreXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(indexStoreEl, "${daisy.datadir}" + sep
                + "indexstore");

        DOMXPath pubreqsXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/publisher/publisher']/configuration/publisherRequestDirectory");
        Element pubreqsEl = (Element) pubreqsXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(pubreqsEl, "${daisy.datadir}" + sep
                + "pubreqs");

        // update passwords for internal user
        DOMXPath credentialsXPath = new DOMXPath(
                "//credentials[@key='internal']");
        List<Element> credentialsEls = credentialsXPath
                .selectNodes(repoServerConfigDoc);
        for (Element el : credentialsEls) {
            el.setAttribute("password", internalPass);
        }

        // update password for workflow user
        DOMXPath workflowUserXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/workflow/workflow-manager']/configuration/workflowUser");
        Element workflowUserEl = (Element) workflowUserXPath
                .selectSingleNode(repoServerConfigDoc);
        workflowUserEl.setAttribute("password", workflowPass);

        // update driver registrar params
        addJdbcDriverToConf(repoServerConfigDoc, dbParams);

        // update db params
        DOMXPath dataSourceConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/datasource/datasource']/configuration");
        DOMXPath dbUserNameXPath = new DOMXPath("username");
        DOMXPath dbPasswordXPath = new DOMXPath("password");
        DOMXPath dbUrlXPath = new DOMXPath("url");
        Element dataSourceConfEl = (Element) dataSourceConfXPath
                .selectSingleNode(repoServerConfigDoc);
        Element dbUserNameEl = (Element) dbUserNameXPath
                .selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbUserNameEl, dbParams.getUser());
        Element dbPasswordEl = (Element) dbPasswordXPath
                .selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbPasswordEl, dbParams.getPassword());
        Element dbUrlEl = (Element) dbUrlXPath
                .selectSingleNode(dataSourceConfEl);
        InstallHelper.setElementValue(dbUrlEl, dbParams.getUrl());

        // workflow params
        DOMXPath workflowConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/extensions/workflow/workflow-manager']/configuration");
        DOMXPath hibernateDialectXPath = new DOMXPath(
                "jbpm/hibernate/properties/entry[@key='hibernate.dialect']");
        Element workflowConfEl = (Element) workflowConfXPath
                .selectSingleNode(repoServerConfigDoc);
        Element hibernateDialectEl = (Element) hibernateDialectXPath
                .selectSingleNode(workflowConfEl);
        InstallHelper.setElementValue(hibernateDialectEl,
                dbParams.getHibernateDialect());

        // update mail params
        DOMXPath smtpHostXPath = new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/smtpHost");
        Element smtpHostEl = (Element) smtpHostXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpHostEl, smtpHost);

        DOMXPath smtpSSLXPath = new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/useSSL");
        Element smtpSSLEl = (Element) smtpSSLXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpSSLEl, smtpSSL);

        if (smtpPort != null) {
            Element smtpPortEl = repoServerConfigDoc.createElement("smtpPort");
            InstallHelper.setElementValue(smtpPortEl, smtpPort);
            smtpSSLEl.getParentNode().insertBefore(smtpPortEl, smtpSSLEl);
        }
        DOMXPath smtpTLSXPath = new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/startTLS");
        Element smtpTLSEl = (Element) smtpTLSXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(smtpTLSEl, String.valueOf(smtpTLS));

        if (smtpUser != null) {
            DOMXPath smtpConfigXPath = new DOMXPath(
                    "/targets/target[@path='/daisy/emailer/emailer']/configuration");
            Element smtpConfigEl = (Element) smtpConfigXPath
                    .selectSingleNode(repoServerConfigDoc);
            Element smtpAuthenticationElement = repoServerConfigDoc
                    .createElement("authentication");
            smtpAuthenticationElement.setAttribute("username", smtpUser);
            smtpAuthenticationElement.setAttribute("password", smtpPass);
            smtpConfigEl.appendChild(smtpAuthenticationElement);
        }

        DOMXPath mailFromXPath = new DOMXPath(
                "/targets/target[@path='/daisy/emailer/emailer']/configuration/fromAddress");
        Element mailFromEl = (Element) mailFromXPath
                .selectSingleNode(repoServerConfigDoc);
        InstallHelper.setElementValue(mailFromEl, mailFrom);

        // set mbean server password to something random
        String jmxPwd = InstallHelper.isDevelopmentSetup() ? "daisyjmx" : Gpw
                .generate(8);
        DOMXPath mbeanHttpXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmx/mbeanserver']/configuration/httpAdaptor");
        Element mbeanHttpEl = (Element) mbeanHttpXPath
                .selectSingleNode(repoServerConfigDoc);
        mbeanHttpEl.setAttribute("password", jmxPwd);
        DOMXPath mbeanXmlHttpXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmx/mbeanserver']/configuration/xmlHttpAdaptor");
        Element mbeanXmlHttpEl = (Element) mbeanXmlHttpXPath
                .selectSingleNode(repoServerConfigDoc);
        mbeanXmlHttpEl.setAttribute("password", jmxPwd);

        InstallHelper.saveDocument(repoServerConfig, repoServerConfigDoc);

        // copy log configuration
        File logConfig = new File(confDir, "repository-log4j.properties");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.LOG_CONF),
                logConfig);

        // create sample in pubreqs directory
        File samplePubReqs = new File(pubReqsPath, "sample");
        samplePubReqs.mkdirs();
        InstallHelper.copyStream(
                getClass().getClassLoader().getResourceAsStream(
                        "org/outerj/daisy/install/pubreqs-sample/README.txt"),
                new File(pubReqsPath, "README.txt"));
        InstallHelper.copyStream(
                getClass().getClassLoader().getResourceAsStream(
                        "org/outerj/daisy/install/pubreqs-sample/default.xml"),
                new File(samplePubReqs, "default.xml"));
        InstallHelper.copyStream(
                getClass().getClassLoader().getResourceAsStream(
                        "org/outerj/daisy/install/pubreqs-sample/mapping.xml"),
                new File(samplePubReqs, "mapping.xml"));

        // put README in the plugins directory
        InstallHelper.copyStream(
                getClass().getClassLoader().getResourceAsStream(
                        "org/outerj/daisy/install/plugins-README.txt"),
                new File(pluginsPath, "README.txt"));
    }

    /**
     * Add the JDBC driver in the config to the list of drivers to be loaded.
     * 
     * @return true if config changed, false if not (= when driver was already
     *         present)
     */
    private boolean addJdbcDriverToConf(Document repoServerConfigDoc,
            DatabaseParams dbParams) throws Exception {
        DOMXPath driversXPath = new DOMXPath(
                "/targets/target[@path='/daisy/driverregistrar/driverregistrar']/configuration/drivers");
        Element driversEl = (Element) driversXPath
                .selectSingleNode(repoServerConfigDoc);

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
        Element driverClassNameEl = repoServerConfigDoc
                .createElement("classname");
        InstallHelper.setElementValue(driverClassNameEl,
                dbParams.getDriverClassName());
        Element driverClasspathEl = repoServerConfigDoc
                .createElement("classpath");
        InstallHelper.setElementValue(driverClasspathEl,
                dbParams.getDriverClassPath());
        driverEl.appendChild(driverClassNameEl);
        driverEl.appendChild(driverClasspathEl);
        driversEl.appendChild(driverEl);

        addLineBreaks(driversEl);

        return true;
    }

    private void setupActiveMQ() throws Exception {
        if (!jmsDbParams.isDatabaseEmpty()) {
            log.info("Clearing activemq database");
            jmsDbParams.clearDatabase();
        }

        File jmsConfig = getTemplateConf(TemplateConf.JMS_CONF);
        File jmsConfigDest = new File(repoDataPath, "conf" + File.separator
                + "activemq-conf.xml");

        // Copy configuration files
        log.info("Installing configuration files...");
        InstallHelper.copyFile(getTemplateConf(TemplateConf.LOGIN_CONFIG),
                new File(jmsConfigDest.getParentFile(), "/login.config"));
        InstallHelper.copyFile(getTemplateConf(TemplateConf.GROUPS_PROPS),
                new File(jmsConfigDest.getParentFile(), "/groups.properties"));
        InstallHelper.copyFile(getTemplateConf(TemplateConf.USERS_PROPS),
                new File(jmsConfigDest.getParentFile(), "/users.properties"));

        // Update activemq configuration
        log.info("Configuring activemq.conf file...");
        Document configDoc = InstallHelper.parseFile(jmsConfig);

        Element dbElement = configDoc.createElementNS(null, "bean");
        dbElement.setAttribute("id", "dataSource");
        dbElement.setAttribute("class",
                "org.apache.commons.dbcp.BasicDataSource");
        dbElement.setAttribute("destroy-method", "close");

        String activeMQJdbcURL = jmsDbParams.getUrl();
        if (activeMQJdbcURL.startsWith("jdbc:mysql:")) {
            String extraParams = "relaxAutoCommit=true&sessionVariables=storage_engine=InnoDB";
            if (activeMQJdbcURL.indexOf('?') != -1)
                activeMQJdbcURL += "&" + extraParams;
            else
                activeMQJdbcURL += "?" + extraParams;
        }
        dbElement.appendChild(createPropertyElement("url", activeMQJdbcURL,
                configDoc));
        dbElement.appendChild(createPropertyElement("username",
                jmsDbParams.getUser(), configDoc));
        dbElement.appendChild(createPropertyElement("password",
                jmsDbParams.getPassword(), configDoc));
        dbElement.appendChild(createPropertyElement(
                "defaultTransactionIsolation", "2", configDoc));
        addLineBreaks(dbElement);

        DOMXPath dbConfigXPath = new DOMXPath("/beans/bean[@id='dataSource']");
        Element oldDbConfig = (Element) dbConfigXPath
                .selectSingleNode(configDoc);
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
        brokerEl.setAttribute("dataDirectory",
                "file:${daisy.datadir}/activemq-data");

        InstallHelper.saveDocument(jmsConfigDest, configDoc);

        // Update users.properties
        log.info("Configuring users.properties file...");
        File usersFile = new File(jmsConfigDest.getParentFile(),
                "users.properties");
        Properties activeMqUsers = new Properties();
        activeMqUsers.put("admin", jmsAdminPass);
        storeProperties(activeMqUsers, "Users for ActiveMQ", usersFile);

        // Update myconfig.xml
        log.info("Configuring myconfig.xml with the JMS settings...");
        File repoServerConfig = new File(repoDataPath, "conf" + File.separator
                + "myconfig.xml");
        Document repoServerConfigDoc = InstallHelper
                .parseFile(repoServerConfig);

        DOMXPath credentialsXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/credentials");
        Element credentialsEl = (Element) credentialsXPath
                .selectSingleNode(repoServerConfigDoc);
        credentialsEl.setAttribute("password", jmsAdminPass);

        DOMXPath amqConfXPath = new DOMXPath(
                "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/initialContext/property[@name='java.naming.provider.url']/@value");
        Node amqConfEl = (Node) amqConfXPath
                .selectSingleNode(repoServerConfigDoc);
        amqConfEl
                .setNodeValue("vm://DaisyJMS?brokerConfig=${double-url-encode:xbean:${file-to-uri:${daisy.datadir}/conf/activemq-conf.xml}}&jms.dispatchAsync=true");

        addJdbcDriverToConf(repoServerConfigDoc, jmsDbParams);

        InstallHelper.saveDocument(repoServerConfig, repoServerConfigDoc);

        log.info("ActiveMQ configuration done.");
    }

    private Element createPropertyElement(String name, String value,
            Document doc) {
        Element el = doc.createElementNS(null, "property");
        el.setAttribute("name", name);
        el.setAttribute("value", value);
        return el;
    }

    private void storeProperties(Properties properties, String comment,
            File file) throws Exception {
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
            throw new Exception("Error storing properties to "
                    + file.getAbsolutePath(), e);
        }
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

    private enum TemplateConf {
        REPO_CONF("/repository/server/src/conf/myconfig.xml.template",
                "/repository-server/conf/myconfig.xml.template"), LOG_CONF(
                "/repository/server/src/conf/repository-log4j.properties",
                "/repository-server/conf/repository-log4j.properties"), JMS_CONF(
                "/repository/server/src/conf/activemq-conf.xml.template",
                "/repository-server/conf/activemq-conf.xml.template"), LOGIN_CONFIG(
                "/repository/server/src/conf/login.config",
                "/repository-server/conf/login.config"), GROUPS_PROPS(
                "/repository/server/src/conf/groups.properties",
                "/repository-server/conf/groups.properties"), USERS_PROPS(
                "/repository/server/src/conf/users.properties",
                "/repository-server/conf/users.properties");

        String devLocation;
        String distLocation;

        private TemplateConf(String devLocation, String distLocation) {
            this.devLocation = devLocation;
            this.distLocation = distLocation;
        }
    }

    private String p(String key) {
        if (!properties.containsKey(key)) {
            throw new RuntimeException("Missing property: " + key);
        }
        return PropertyResolver.resolveProperties(properties.getProperty(key),
                properties);
    }

    private String p(String key, String defaultValue) {
        if (!properties.containsKey(key)) {
            if (defaultValue == null)
                return defaultValue;
            return PropertyResolver.resolveProperties(defaultValue, properties);
        }
        return PropertyResolver.resolveProperties(properties.getProperty(key),
                properties);
    }

}
