/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class ServiceScriptInstaller {
    public static void main(String[] args) {
        if (InstallHelper.isDevelopmentSetup()) {
            System.out.println("Service script installation is not supported in development setup.");
            System.exit(1);
        }

        Options options = new Options();

        Option repositoryOption = new Option("r", "repository", true, "Install repository service scripts");
        repositoryOption.setArgName("repodata dir");
        options.addOption(repositoryOption);

        Option wikiOption = new Option("w", "wiki", true, "Install wiki service scripts");
        wikiOption.setArgName("wikidata dir");
        options.addOption(wikiOption);

        Option forceOption = new Option("f", "force", false, "Bypass failing checks etc., if any.");
        options.addOption(forceOption);

        Option helpOption = new Option("h", "help", false, "Help");
        options.addOption(helpOption);

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            List<InstallAction> actions = new ArrayList<InstallAction>();
            if (cmd.hasOption(helpOption.getOpt())) {
                printHelp(options);
            } else {
                boolean force = cmd.hasOption(forceOption.getOpt());

                if (cmd.hasOption(repositoryOption.getOpt())) {
                    File datadir = new File(cmd.getOptionValue(repositoryOption.getOpt()));
                    checkDataDir(datadir, "repository data directory", "conf/myconfig.xml", force);
                    actions.add(new InstallRepoServiceAction(datadir));
                }

                if (cmd.hasOption(wikiOption.getOpt())) {
                    File datadir = new File(cmd.getOptionValue(wikiOption.getOpt()));
                    checkDataDir(datadir, "wiki data directory", "daisy.xconf", force);
                    actions.add(new InstallWikiServiceAction(datadir));
                }

                if (actions.isEmpty()) {
                    printHelp(options);
                    System.exit(1);
                }

                ServiceScriptInstaller installer = new ServiceScriptInstaller();

                if (installer.install(actions)) 
                    System.exit(0);
                else
                    System.exit(1);
            }            
        } catch (ParseException e) {
            printHelp(options);
            System.exit(1);
        }
    }

    private static void checkDataDir(File datadir, String name, String testFilePath, boolean force) {
        if (!datadir.exists()) {
            System.out.printf("Specified path for %s does not exist:\n", name);
            System.out.println(datadir.getAbsolutePath());
            System.exit(1);
        }
        File testFile = new File(datadir, testFilePath);
        if (!force && !testFile.exists()) {
            System.out.printf("Specified path for %s does not seem to point\n", name);
            System.out.printf("to a valid %s.\n", name);
            System.out.println("Use -f (--force) to (re)install the service scripts anyway.");
            System.exit(1);
        }
        File serviceDir = new File(datadir, "service");
        if (!force && serviceDir.exists() && serviceDir.listFiles().length > 0) {
            System.out.printf("The target %s contains a non-empty service directory.\n", name);
            System.out.println("Use -f (--force) to (re)install the service scripts anyway.");
            System.exit(1);
        }
    }

    private static void printHelp(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("daisy-service-install", options, true);
    }

    public boolean install(List<InstallAction> actions) {
        InstallHelper.verticalSpacing(1);
        InstallHelper.printTitle("Service wrapper scripts installation");
        InstallHelper.verticalSpacing(1);

        boolean success = false;
        for (InstallAction action : actions) {
            success = action.perform();
            if (!success)
                break;
        }
        
        return success;
    }
    
    private static boolean isOSWindowsBased() {
        String os = System.getProperties().getProperty("os.name");
        return os.startsWith("Windows");
    }

    private static void copyFileToServiceDir(String subDir, String filename, boolean setExecutableBit, File destDir) throws Exception {
        File wrapperSourceDir = new File(InstallHelper.getDaisyHome(), "wrapper");
        File wrapperSourceSubDir = new File(wrapperSourceDir, subDir);
        File source = new File(wrapperSourceSubDir, filename);
        File destination = new File(destDir, filename);
        InstallHelper.copyFile(source, destination);
        if (setExecutableBit) {
            InstallHelper.setExecutable(destination);
        }
    }

    private interface InstallAction {
        boolean perform();
    }

    private static class InstallWikiServiceAction implements InstallAction {
        private final File datadir;

        public InstallWikiServiceAction(File datadir) {
            this.datadir = datadir;
        }

        public boolean perform() {
            boolean success = false;
            try {
                File serviceDir = new File(datadir, "service");
                serviceDir.mkdir();

                System.out.println("Service wrapper scripts for easy launching of the Daisy Wiki are now created.");

                // copy tanuki wrapper scripts to datadir
                
                if (isOSWindowsBased()) {
                    copyFileToServiceDir("bin", "daisy-wiki-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "install-daisy-wiki-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "uninstall-daisy-wiki-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "start-daisy-wiki-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "stop-daisy-wiki-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "restart-daisy-wiki-service.bat", false, serviceDir);
                } else {
                    copyFileToServiceDir("bin", "daisy-wiki-service", true, serviceDir);
                }
                
                copyFileToServiceDir("conf", "daisy-wiki-service.conf", false, serviceDir);

                File wrapperConfig = new File(serviceDir, "daisy-wiki-service.conf");
                InstallHelper.appendToMatchingLinesInFile(wrapperConfig, "set.default.DAISY_HOME=", System.getProperty("daisy.home"));
                InstallHelper.appendToMatchingLinesInFile(wrapperConfig, "set.default.DAISYWIKI_DATADIR=", datadir.getAbsolutePath());

                success = true;
                System.out.println("Done.");
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }

            return success;
        }
    }

    private static class InstallRepoServiceAction implements InstallAction {
        private final File datadir;

        public InstallRepoServiceAction(File datadir) {
            this.datadir = datadir;
        }

        public boolean perform() {
            boolean success = false;
            try {
                File serviceDir = new File(datadir, "service");
                serviceDir.mkdir();
                System.out.println("Service wrapper scripts for easy launching of the Daisy Repository");
                System.out.println("are now created.");

                // copy tanuki wrapper scripts to datadir
                if (isOSWindowsBased()) {
                    copyFileToServiceDir("bin", "daisy-repository-server-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "install-daisy-repository-server-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "uninstall-daisy-repository-server-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "start-daisy-repository-server-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "stop-daisy-repository-server-service.bat", false, serviceDir);
                    copyFileToServiceDir("service", "restart-daisy-repository-server-service.bat", false, serviceDir);
                } else {                    
                    copyFileToServiceDir("bin", "daisy-repository-server-service", true, serviceDir);
                }                
                
                copyFileToServiceDir("conf", "daisy-repository-server-service.conf", false, serviceDir);

                File wrapperConfig = new File(serviceDir, "daisy-repository-server-service.conf");
                InstallHelper.appendToMatchingLinesInFile(wrapperConfig, "set.default.DAISY_HOME=", System.getProperty("daisy.home"));
                InstallHelper.appendToMatchingLinesInFile(wrapperConfig, "set.default.DAISY_DATADIR=", datadir.getAbsolutePath());

                success = true;
                System.out.println("Done.");
            } catch (Exception e) {
                success = false;
                e.printStackTrace();
            }

            return success;
        }
    }
}
