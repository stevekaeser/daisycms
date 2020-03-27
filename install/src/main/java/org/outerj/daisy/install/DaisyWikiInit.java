/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.install;

import java.io.FileInputStream;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.RepositoryManager;
import org.outerj.daisy.repository.acl.AccessManager;
import org.outerj.daisy.repository.acl.Acl;
import org.outerj.daisy.repository.acl.AclEntry;
import org.outerj.daisy.repository.acl.AclObject;
import static org.outerj.daisy.repository.acl.AclSubjectType.*;
import static org.outerj.daisy.repository.acl.AclPermission.*;
import static org.outerj.daisy.repository.acl.AclActionType.*;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.user.Role;
import org.outerj.daisy.repository.user.RoleNotFoundException;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.repository.user.UserNotFoundException;
import org.outerj.daisy.util.VersionHelper;

public class DaisyWikiInit {
    private long guestRoleId;

    private Repository repository;

    public static void main(String[] args) throws Exception {
        new DaisyWikiInit().install(args);
    }

    private void install(String[] args) throws Exception {
        Options options = new Options();        
        options.addOption(new Option("c", "conf", true, "Configuration file for automated install"));
        options.addOption(new Option("v", "version", false, "Print version info"));

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("daisy-wiki-init", options, true);
            System.exit(0);
        }

        if (cmd.hasOption('v')) {
            System.out.println(VersionHelper.getVersionString(getClass().getClassLoader(), "org/outerj/daisy/install/versioninfo.properties"));
            System.exit(0);
        }
        
        if (cmd.hasOption("c")) {
            FileInputStream is = null;
            try {
                is = new FileInputStream(cmd.getOptionValue("c"));
                Properties props = new Properties();
                props.load(is);

                Credentials credentials = new Credentials(InstallHelper.getPropertyValue(props, InitialisationProperties.DAISY_LOGIN), InstallHelper
                        .getPropertyValue(props, InitialisationProperties.DAISY_PASSWORD));

                System.out.println("\nConnecting to the repository.");
                RepositoryManager repositoryManager = new RemoteRepositoryManager(InstallHelper.getPropertyValue(props, InitialisationProperties.DAISY_URL),
                        credentials);
                repository = repositoryManager.getRepository(credentials);
                repository.switchRole(Role.ADMINISTRATOR);
            } finally {
                if (is != null)
                    is.close();
                else
                    System.out.println("Could not open " + cmd.getOptionValue("c") + " exiting ...");
            }

        } else {
            InstallHelper.printTitle("Daisy Wiki Initialisation");
            System.out.println();
            init();
        }

        installGuestUser();
        installACL();
        installSchema();

        System.out.println();
        System.out.println("Finished.");
    }

    private void init() throws Exception {
        repository = InstallHelper.promptRepository().getRepository();
    }

    private void installGuestUser() throws Exception {
        InstallHelper.printSubTitle("Creating guest user and role.");

        UserManager userManager = repository.getUserManager();
        Role role;
        try {
            role = userManager.getRole("guest", false);
            System.out.println("Existing guest role found, id = " + role.getId());
        } catch (RoleNotFoundException e) {
            role = userManager.createRole("guest");
            role.save();
            System.out.println("Guest role created, id = " + role.getId());
        }
        this.guestRoleId = role.getId();

        User user;
        try {
            user = userManager.getUser("guest", false);
            System.out.println("Existing guest user found, id = " + user.getId());
        } catch (UserNotFoundException e) {
            user = userManager.createUser("guest");
            user.setPassword("guest");
            user.addToRole(role);
            user.save();
            System.out.println("Guest user created, id = " + user.getId());
        }
    }

    private void installSchema() throws Exception {
        System.out.println();
        InstallHelper.printSubTitle("Schema creation");
        System.out.println();
        SchemaUploader.load(getClass().getClassLoader().getResourceAsStream("org/outerj/daisy/install/daisywiki_schema.xml"), repository);
    }

    private void installACL() throws Exception {
        System.out.println();
        InstallHelper.printSubTitle("ACL initialisation");
        System.out.println();

        AccessManager accessManager = repository.getAccessManager();
        Acl acl = accessManager.getStagingAcl();
        if (acl.size() > 0) {
            System.out.println("ACL is not empty -- will not touch it.");
            System.out.println("");
            return;
        }

        System.out.println("A default ACL will be installed. This will limit the users with role 'guest'");
        System.out.println("to read operations. All other users will have both read and write privileges.");

        acl.clear();
        AclObject aclObject = acl.createNewObject("true");
        acl.add(aclObject);

        AclEntry aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);
        aclEntry.set(PUBLISH, GRANT);
        aclEntry.set(DELETE, GRANT);

        aclEntry = aclObject.createNewEntry(ROLE, guestRoleId);
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, DENY);
        aclEntry.set(PUBLISH, DENY);
        aclEntry.set(DELETE, DENY);

        aclObject = acl.createNewObject("conceptual = 'true'");
        acl.add(aclObject);

        aclEntry = aclObject.createNewEntry(EVERYONE, -1);
        aclObject.add(aclEntry);
        aclEntry.set(READ, GRANT);
        aclEntry.set(WRITE, GRANT);

        aclEntry = aclObject.createNewEntry(ROLE, guestRoleId);
        aclObject.add(aclEntry);
        aclEntry.set(READ, DENY);
        aclEntry.set(WRITE, DENY);

        acl.save();
        accessManager.copyStagingToLive();

        System.out.println("ACL configured.");
    }

    private class InitialisationProperties {
        public static final String DAISY_URL = "daisyUrl";

        public static final String DAISY_LOGIN = "daisyLogin";

        public static final String DAISY_PASSWORD = "daisyPassword";
    }
}
