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
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.jaxen.dom.DOMXPath;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.AuthenticationFailedException;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.util.VersionHelper;
import org.outerj.daisy.util.CliUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DaisyWikiInitDataDirectory {

    private File dataDirectory;
    private File xconfFile;
    private boolean advanced;

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option dataDirOption = new Option("d", "wikidata", true, "Where to create the data directory");
        dataDirOption.setArgName("path");
        // dataDirOption.setRequired(true);
        options.addOption(dataDirOption);
        Option confOption = new Option("c", "conf", true, "Configuration file for automated install");
        confOption.setArgName("conf-file");
        options.addOption(new Option("a", "advanced", false, "Advanced mode (asks more questions)"));
        options.addOption(confOption);
        options.addOption(new Option("v", "version", false, "Print version info"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);

            if (cmd.hasOption('v')) {
                System.out.println(VersionHelper.getVersionString(DaisyWikiInitDataDirectory.class.getClassLoader(),
                        "org/outerj/daisy/install/versioninfo.properties"));
                System.exit(0);
            }

            InstallHelper.printTitle("Wikidata directory creation");
            System.out.println();

            File datadir;
            if (cmd.hasOption("d")) {
                datadir = new File(cmd.getOptionValue("d"));
            } else {
                System.out.println("The wikidata directory is a directory containing all configuration");
                System.out.println("and data for the wiki.");
                System.out.println();
                System.out.println("The wikidata directory path you enter should be a non-existing or empty");
                System.out.println("directory. For example, enter c:\\daisywikidata or /home/<someuser>/daisywikidata");
                System.out.println();
                System.out.println("PLEASE NOTE: this directory is unrelated to the repository's daisydata directory.");
                System.out.println();

                String defaultPath = null;
                if (InstallHelper.isDevelopmentSetup()) {
                    defaultPath = new File(InstallHelper.getDaisySourceHome().getParent(), "devwikidata").getAbsolutePath();
                }

                String message = "Please enter a path where the wikidata directory should be created.";
                datadir = InstallHelper.promptForEmptyDir(message, defaultPath);
            }

            DaisyWikiInitDataDirectory install = new DaisyWikiInitDataDirectory(cmd.hasOption("a"), datadir);
            InitSettings initSettings;

            if (cmd.hasOption("c"))
                initSettings = new InitSettings(new File(cmd.getOptionValue("c")));
            else
                initSettings = new InitSettings();

            install.install(initSettings);

            System.out.println();
            System.out.println("Finished.");
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("daisy-wikidata-init", options, true);
            System.exit(1);
        }
    }

    public DaisyWikiInitDataDirectory(boolean advanced, File dataDirectory) throws Exception {
        this.advanced = advanced;
        this.dataDirectory = dataDirectory;
        if (dataDirectory.exists() && dataDirectory.list().length > 0 ) {
            throw new Exception("The existing data directory " + dataDirectory.getAbsolutePath() + " is not empty. Please choose another location.");
        }
        xconfFile = new File(dataDirectory, "daisy.xconf");
    }

    public void install(InitSettings settings) throws Exception {
        createDirectory();
        createRegistrar(settings);
        updateXconf(settings);
    }

    private void createDirectory() throws Exception {
        System.out.println();
        System.out.println("Copying template data directory...");
        File dataDirTemplate;
        if (InstallHelper.isDevelopmentSetup()) {
            File daisySourceHome = InstallHelper.getDaisySourceHome();
            dataDirTemplate = new File(daisySourceHome, "applications/daisywiki/runtime/src/cocoon/wikidata");
        } else {
            File daisyHome = InstallHelper.getDaisyHome();
            dataDirTemplate = new File(daisyHome, "daisywiki/wikidata-template");
        }
        InstallHelper.copyFile(dataDirTemplate, dataDirectory);
        System.out.println("Done.");
    }

    private void createRegistrar(InitSettings settings) throws Exception {
        // FIXME 2 wikis on same repository : 2nd wiki will not set up registrar
        // user since he already exists. Password must be grabbed from some
        // source ( original daisy.xconf ? )
        InstallHelper.verticalSpacing(3);
        InstallHelper.printSubTitle("Creating registrar user.");
        InstallHelper.verticalSpacing(1);

        String suggestPwd = InstallHelper.isDevelopmentSetup() ? "defaultpwd" : InstallHelper.generatePassword();

        if (settings.getRepository() == null) {
            InstallHelper.RepositoryAccess repoAccess = InstallHelper.promptRepository();
            settings.setRepositoryManager(repoAccess.getRepositoryManager());
            settings.setRepository(repoAccess.getRepository());
        }

        UserManager userManager = settings.getRepository().getUserManager();
        User user;
        boolean isNewUser = true;
        try {
            user = userManager.getUser("registrar", true);
            System.out.println("Existing registrar user found, id = " + user.getId());
            isNewUser = false;
        } catch (UserNotFoundException e) {

            if (advanced) {
                System.out.println();
                System.out.println("A user called \"registrar\" will now be created. This user");
                System.out.println("will be used to create new user accounts for the self-registering");
                System.out.println("users. A password is needed for this user.");
                System.out.println("The default password presented below is secure-random generated.");
                System.out.println("This password will be written in a configuration file, you do not");
                System.out.println("need to remember it, so it does not matter it is complex.");
                System.out.println();

                settings.setRegistrarPassword(InstallHelper.prompt("Enter password for user 'registrar' [ default = " + suggestPwd + " ] : ", suggestPwd));
            }

            if (settings.getRegistrarPassword() == null)
                settings.setRegistrarPassword(suggestPwd);

            user = userManager.createUser("registrar");
            user.setPassword(settings.getRegistrarPassword());
            Role adminRole = userManager.getRole(Role.ADMINISTRATOR, false);
            user.addToRole(adminRole);
            user.setDefaultRole(adminRole);
            user.save();
            System.out.println("Registrar user created, id = " + user.getId());
            System.out.println();            
        }
        
        if (!xconfFile.exists()) {
            System.out.println("daisy.xconf file does not exist at " + xconfFile.getAbsolutePath() + ", skipping automatic update.");
        } else {
            if (advanced) {
                System.out.println("The password of the registrar user needs to be specified in");
                System.out.println("the daisy.xconf file. I can do this for you.");
                settings.setUpdateXconf(InstallHelper.promptYesNo("OK to update daisy.xconf now? [yes/no, default = yes]", true));
                System.out.println();
            }
            if (settings.isUpdateXconf()) {
//                if (!isNewUser && InstallHelper.isDevelopmentSetup()) {
//                    settings.setRegistrarPassword("defaultpwd");
                if (!isNewUser) {
                    System.out.println();
                    System.out.println("Please enter the password for the existing registrar user.");
                    System.out.println("This might be found in another daisy wiki data directory.");
                    System.out.println("Look in the 'daisy.xconf' file for the word 'registrar'");
                    System.out.println("Leave blank to auto-generate a new password (only do this if you");
                    System.out.println("have no other Wiki instances anymore connecting to this repository).");

                    String registrarUserPassword = null;
                    boolean registrarPasswordOk = false;
                    while (!registrarPasswordOk) {
                        registrarUserPassword = CliUtil.promptPassword("Registrar user password : ", null);
                        if (registrarUserPassword == null) {
                            registrarPasswordOk = true;
                        } else {
                            try {
                                settings.getRepositoryManager().getRepository(new Credentials("registrar", registrarUserPassword));
                                registrarPasswordOk = true;
                            } catch (AuthenticationFailedException e) {
                                System.out.println();
                                System.out.println("Incorrect registrar password, try again.");
                                System.out.println();
                                registrarPasswordOk = false;
                            }
                        }
                    }
                    if (registrarUserPassword == null) {
                        // assign a new password
                        System.out.println();
                        System.out.println("Assigning a new password to the registrar user.");
                        registrarUserPassword = InstallHelper.generatePassword();
                        user.setPassword(registrarUserPassword);
                        user.save();
                        System.out.println("Done.");
                    }
                    settings.setRegistrarPassword(registrarUserPassword);
                }
                System.out.println("Will now update the daisy.xconf with the registrar user password.");
                Document xconfDocument = InstallHelper.parseFile(xconfFile);
                DOMXPath registrarXPath = new DOMXPath(
                        "/cocoon/component[@role='org.outerj.daisy.frontend.components.userregistrar.UserRegistrar']/registrarUser");
                Element registrarEl = (Element) registrarXPath.selectSingleNode(xconfDocument);
                registrarEl.setAttribute("password", settings.getRegistrarPassword());
                InstallHelper.saveDocument(xconfFile, xconfDocument);
            }
        }
    }

    private void updateXconf(InitSettings settings) throws Exception {
        // dev setup uses default passwords, no updates required
        if (InstallHelper.isDevelopmentSetup())
            return;

        InstallHelper.verticalSpacing(3);
        InstallHelper.printSubTitle("Configuring daisy.xconf");
        InstallHelper.verticalSpacing(1);

        // Either grab settings from myconfig or prompt user
        if (!xconfFile.exists()) {
            System.out.println("Did not find daisy.xconf at " + xconfFile.getAbsolutePath() + ", will skip updating it.");
        } else {
            String jmsAdminPassword = null;
            if (settings.getRepoDataDir() == null) {
                String repoDataDirPath = InstallHelper.prompt(
                        "Please enter the repository data directory. If there is no repository installed on the current machine leave blank", null);
                if (repoDataDirPath != null) {
                    settings.setRepoDataDir(new File(repoDataDirPath));
                } else {
                    System.out.println("The repository resides on another machine.");
                }
            }

            File myconfig = new File(settings.getRepoDataDir(), "conf" + File.separator + "myconfig.xml");
            if (settings.getRepoDataDir() != null && settings.getRepoDataDir().exists() && myconfig.exists()) {
                Document myconfigDoc = InstallHelper.parseFile(myconfig);
                DOMXPath jmsAdminPwdXpath = new DOMXPath(
                        "/targets/target[@path='/daisy/jmsclient/jmsclient']/configuration/jmsConnection/credentials/@password");
                jmsAdminPassword = jmsAdminPwdXpath.stringValueOf(myconfigDoc);
            } else {
                System.out.println("The myconfig.xml file could not be found at this location : " + myconfig.getAbsolutePath() + ".");
            }

            if (jmsAdminPassword == null) {
                System.out.println("Certain values necessary for the installation could not be obtained.  You will be prompted for these values : ");
                jmsAdminPassword = InstallHelper.prompt("JmsAdmin password ( search for 'jms' )");
            }

            Document xconfDoc = InstallHelper.parseFile(xconfFile);

            // Update jms setup
            DOMXPath jmsCredentialsXPath = new DOMXPath("/cocoon/component[@class='org.outerj.daisy.jms.impl.JmsClientImpl']/jmsConnection/credentials");
            Element jmsCredentialsEl = (Element) jmsCredentialsXPath.selectSingleNode(xconfDoc);
            jmsCredentialsEl.setAttribute("password", jmsAdminPassword);

            System.out.println("Will now save the updated daisy.xconf");
            InstallHelper.saveDocument(xconfFile, xconfDoc);
        }

    }
    
    private static class InitSettings {
        public static final String DAISY_URL = "daisyUrl";
        public static final String DAISY_LOGIN = "daisyLogin";
        public static final String DAISY_PASSWORD = "daisyPassword";
        public static final String REGISTRAR_USER_PASSWORD = "registrarUserPassword";
        public static final String UPDATE_XCONF = "updateXConf";
        public static final String REPO_DATA_DIR = "repoDataDir";

        private String daisyUrl;
        private RepositoryManager repositoryManager;
        private Repository repository;
        private Credentials credentials;
        private String registrarPassword;
        private boolean updateXconf = true;
        private File repoDataDir;

        public InitSettings() {

        }

        public InitSettings(File configProperties) throws Exception {
            FileInputStream is = null;
            try {
                is = new FileInputStream(configProperties);
                Properties props = new Properties();
                props.load(is);

                credentials = new Credentials(InstallHelper.getPropertyValue(props, DAISY_LOGIN), InstallHelper.getPropertyValue(props, DAISY_PASSWORD));

                repositoryManager = new RemoteRepositoryManager(InstallHelper.getPropertyValue(props, DAISY_URL), credentials);
                repository = repositoryManager.getRepository(credentials);
                repository.switchRole(Role.ADMINISTRATOR);

                registrarPassword = InstallHelper.getPropertyValue(props, REGISTRAR_USER_PASSWORD, null);
                updateXconf = Boolean.valueOf(InstallHelper.getPropertyValue(props, UPDATE_XCONF, Boolean.toString(updateXconf))).booleanValue();

                repoDataDir = new File(InstallHelper.getPropertyValue(props, REPO_DATA_DIR));

            } finally {
                if (is != null)
                    is.close();
                else
                    System.out.println("Could not open " + configProperties.getAbsolutePath() + " exiting ...");
            }
        }

        public Credentials getCredentials() {
            return credentials;
        }

        public void setCredentials(Credentials credentials) {
            this.credentials = credentials;
        }

        public String getDaisyUrl() {
            return daisyUrl;
        }

        public void setDaisyUrl(String daisyUrl) {
            this.daisyUrl = daisyUrl;
        }

        public String getRegistrarPassword() {
            return registrarPassword;
        }

        public void setRegistrarPassword(String registrarPassword) {
            this.registrarPassword = registrarPassword;
        }

        public boolean isUpdateXconf() {
            return updateXconf;
        }

        public void setUpdateXconf(boolean updateXconf) {
            this.updateXconf = updateXconf;
        }

        public RepositoryManager getRepositoryManager() {
            return repositoryManager;
        }

        public void setRepositoryManager(RepositoryManager repositoryManager) {
            this.repositoryManager = repositoryManager;
        }

        public Repository getRepository() {
            return repository;
        }

        public void setRepository(Repository repository) {
            this.repository = repository;
        }

        public File getRepoDataDir() {
            return repoDataDir;
        }

        public void setRepoDataDir(File repoDataDir) {
            this.repoDataDir = repoDataDir;
        }

    }

}
