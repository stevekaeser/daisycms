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
package org.outerj.daisy.tools.importexport.import_.cli;

import org.outerj.daisy.tools.importexport.import_.fs.ImportFile;
import org.outerj.daisy.tools.importexport.import_.fs.ImportFileFactory;
import org.outerj.daisy.tools.importexport.import_.FormatDetector;
import org.outerj.daisy.tools.importexport.import_.Importer;
import org.outerj.daisy.tools.importexport.ImpExpFormat;
import org.outerj.daisy.tools.importexport.import_.tm.TMImporter;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.tools.importexport.tm.TMConfigFactory;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptions;
import org.outerj.daisy.tools.importexport.import_.config.ImportOptionsFactory;
import org.outerj.daisy.tools.importexport.util.ImportExportUtil;
import org.outerj.daisy.tools.importexport.docset.DocumentSet;
import org.outerj.daisy.tools.importexport.docset.DocumentSetFactory;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.variant.LanguageNotFoundException;
import org.outerj.daisy.util.VersionHelper;
import org.outerj.daisy.util.CliUtil;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.htmlcleaner.HtmlCleanerTemplate;
import org.outerj.daisy.htmlcleaner.HtmlCleanerFactory;
import org.apache.commons.cli.*;
import org.xml.sax.InputSource;

import javax.xml.transform.OutputKeys;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.*;

/**
 * Command-lik interface of the import tool.
 */
public class ImportCli {
    private boolean importRunning = false;

    public static void main(String[] args) throws Exception {
        new ImportCli().run(args);
    }

    private ImportCli() {

    }

    private void run(String[] args) throws Exception {
        Options cliOptions = new Options();

        Option optionsOption = new Option("o", "options", true, "Specifies path to options XML file (use -d to get a template)");
        cliOptions.addOption(optionsOption);

        Option dumpOptionsOption = new Option("d", "dump-sample-options", false, "Shows sample options to provide via -o");
        cliOptions.addOption(dumpOptionsOption);

        Option importPathOption = new Option("i", "import", true, "Path to the import location (dir or zip)");
        cliOptions.addOption(importPathOption);

        Option importSetOption = new Option("f", "importset", true, "File specifying subset of docs to import");
        cliOptions.addOption(importSetOption);

        Option repoURLOption = new Option("l", "repo-url", true, "Repository server URL, e.g. http://localhost:9263");
        cliOptions.addOption(repoURLOption);

        Option repoUserOption = new Option("u", "repo-user", true, "Daisy repository login");
        cliOptions.addOption(repoUserOption);

        Option repoPwdOption = new Option("p", "repo-pwd", true, "Daisy repository password");
        cliOptions.addOption(repoPwdOption);

        Option repoRolesOption = new Option("r", "repo-roles", true, "Daisy repository role/s (comma separated)");
        cliOptions.addOption(repoRolesOption);

        Option skipVersionCheckOption = new Option("s", "skip-version-check", false, "Skip version check");
        cliOptions.addOption(skipVersionCheckOption);

        Option versionOption = new Option("v", "version", false, "Print version and exit");
        cliOptions.addOption(versionOption);

        Option helpOption = new Option("h", "help", false, "Shows help");
        cliOptions.addOption(helpOption);

        Option formatOption = new Option("t", "format", true, "Export format: 'default' or 'tm'");
        cliOptions.addOption(formatOption);

        Option targetLanguageOption = new Option("a", "target-language", true, "Only for tm (translation management) import: the target language variant");
        cliOptions.addOption(targetLanguageOption);

        Option tmConfigOption = new Option("g", "tm-config", true, "Only for tm (translation management) import: tm-specific config file");
        cliOptions.addOption(tmConfigOption);

        Option htmlCleanerConfigOption = new Option("e", "htmlcleaner-config", true, "Only for tm (translation management) import: location of htmlcleaner.xml config");
        cliOptions.addOption(htmlCleanerConfigOption);

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        boolean showHelp = false;
        try {
            cmd = parser.parse(cliOptions, args);
        } catch (ParseException e) {
            showHelp = true;
        }

        if (showHelp || cmd.hasOption(helpOption.getOpt())) {
            printHelp(cliOptions);
            System.exit(1);
        }

        Properties versionProperties = ImportExportUtil.getVersionProperties();
        if (cmd.hasOption(versionOption.getOpt())) {
            System.out.println(VersionHelper.formatVersionString(versionProperties));
            System.exit(0);
        }

        if (cmd.hasOption(dumpOptionsOption.getOpt())) {
            System.out.println(ImportOptionsFactory.toXml(ImportOptionsFactory.getDefaultImportOptions()));
            return;
        }

        if (!cmd.hasOption(importPathOption.getOpt())) {
            System.out.println("Missing -" + importPathOption.getOpt() + " argument");
            System.exit(1);
        }

        String repositoryURL = "http://localhost:9263";
        if (!cmd.hasOption(repoURLOption.getOpt())) {
            System.out.println("-" + repoURLOption.getOpt() + " not specified, assuming repository is listening at " + repositoryURL);
        } else {
            repositoryURL = cmd.getOptionValue(repoURLOption.getOpt());
        }

        String user;
        if (!cmd.hasOption(repoUserOption.getOpt())) {
            System.out.println("Missing -" + repoUserOption.getOpt() + " argument");
            System.exit(1);
        }
        user = cmd.getOptionValue(repoUserOption.getOpt());

        String pwd;
        if (!cmd.hasOption(repoPwdOption.getOpt())) {
            pwd = CliUtil.promptPassword("Daisy password for " + user + ": ", false);
            if (pwd == null) {
                System.out.println("No password specified, exiting.");
                System.exit(1);
            }
        } else {
            pwd = cmd.getOptionValue(repoPwdOption.getOpt());
        }

        CliImportListener listener = null;
        int exitCode = 0;
        try {
            System.out.println("\nConnecting to the repository.");
            RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryURL, new Credentials(user, pwd));
            Repository repository = repositoryManager.getRepository(new Credentials(user, pwd));

            if (cmd.hasOption(repoRolesOption.getOpt())) {
                long[] roles = ImportExportUtil.parseRoles(cmd.getOptionValue(repoRolesOption.getOpt()), repository);
                repository.setActiveRoleIds(roles);
            }

            ImportOptions options;
            if (cmd.hasOption(optionsOption.getOpt())) {
                String optionsPath = cmd.getOptionValue(optionsOption.getOpt());
                InputStream is = null;
                try {
                    is = new FileInputStream(optionsPath);
                    options = ImportOptionsFactory.parseFromXml(is, repository);
                } finally {
                    if (is != null)
                        try { is.close(); } catch (Exception e) { /* ignore */ }
                }
            } else {
                options = ImportOptionsFactory.getDefaultImportOptions();
            }

            listener = new CliImportListener(options, getOut());

            ImportFile importFile = ImportFileFactory.getImportFile(new File(cmd.getOptionValue(importPathOption.getOpt())));

            if (!cmd.hasOption(skipVersionCheckOption.getOpt())) {
                String serverVersion = repository.getServerVersion();
                String importToolVersion = versionProperties.getProperty("artifact.version");
                if (!serverVersion.equals(importToolVersion)) {
                    System.out.println("Version of import tool and repository server do not match.");
                    System.out.println("Version of import tool: " + importToolVersion);
                    System.out.println("Version of server     : " + serverVersion);
                    System.out.println();
                    System.out.println("It is recommended to use a corresponding version. To skip the version check, use -" + skipVersionCheckOption.getOpt());
                    System.exit(1);
                }
            }

            DocumentSet documentSet = null;
            if (cmd.hasOption(importSetOption.getOpt())) {
                InputStream is = null;
                try {
                    is = new FileInputStream(cmd.getOptionValue(importSetOption.getOpt()));
                    documentSet = DocumentSetFactory.parseFromXml(is, repository);
                } catch (Throwable e) {
                    throw new ImportExportException("Error reading import document subset.", e);
                } finally {
                    if (is != null)
                        is.close();
                }
            }

            ImpExpFormat format = FormatDetector.detectFormat(importFile, cmd.getOptionValue(formatOption.getOpt()));

            String targetLanguage = null;
            TMConfig tmConfig = null;
            HtmlCleanerTemplate htmlCleanerTemplate = null;
            if (format == ImpExpFormat.TRANSLATION_MANAGEMENT) {
                targetLanguage = cmd.getOptionValue(targetLanguageOption.getOpt());
                if (targetLanguage == null) {
                    System.out.println("Missing -" + targetLanguageOption.getOpt() + " argument");
                    System.exit(1);
                }

                // This is just an (optional) early validation
                try {
                    repository.getVariantManager().getLanguage(targetLanguage, false);
                } catch (LanguageNotFoundException e) {
                    System.out.println("Target language does not exist: \"" + targetLanguage + "\".");
                    System.exit(1);
                }

                if (cmd.hasOption(tmConfigOption.getOpt())) {
                    String tmConfigFile = cmd.getOptionValue(tmConfigOption.getOpt());
                    tmConfig = TMConfigFactory.parseFromXml(tmConfigFile, repository);
                } else {
                    tmConfig = new TMConfig();
                }

                if (cmd.hasOption(htmlCleanerConfigOption.getOpt())) {
                    String htmlCleanerConfig = cmd.getOptionValue(htmlCleanerConfigOption.getOpt());
                    FileInputStream fis = new FileInputStream(htmlCleanerConfig);
                    try {
                        htmlCleanerTemplate = new HtmlCleanerFactory().buildTemplate(new InputSource(fis));
                    } finally {
                        fis.close();
                    }
                }
            }

            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new ImportCliShutdownHook(listener, mainThread));

            try {
                setImportRunning(true);
                switch (format) {
                    case DEFAULT:
                        Importer.run(importFile, documentSet, repository, options, listener);
                        break;
                    case TRANSLATION_MANAGEMENT:
                        TMImporter.run(importFile, documentSet, repository, options, listener, tmConfig, targetLanguage, htmlCleanerTemplate);
                        break;
                    default:
                        throw new RuntimeException("Unexepected: unrecognized format: " + format);
                }
            } finally {
                setImportRunning(false);
                printImportSummary(listener);
            }
        } catch (Throwable e) {
            ImportExportUtil.logError(e, "importerror", getOut());
            exitCode = 1;
        }

        if (listener == null || !listener.isInterrupted()) {
            // Only call System.exit when the import was not interrupted, otherwise
            // the ImportCliShutdownHook will keep waiting
            System.exit(exitCode);
        }
    }

    private synchronized void setImportRunning(boolean running) {
        this.importRunning = running;
    }

    private synchronized boolean isImportRunning() {
        return importRunning;
    }

    private void printImportSummary(CliImportListener listener) {
        Collection failedBecauseLocked = listener.getFailedBecauseLockedDocument();
        Collection failedBecauseAccessDenied = listener.getFailedBecauseAccessDenied();
        Collection failed = listener.getFailedDocuments();
        Collection succeeded = listener.getSucceeded();

        getOut().println();
        getOut().println("Import summary:");
        getOut().println("---------------");
        getOut().println(" Succeeded: " + succeeded.size());
        getOut().println(" Failed because the document was locked: " + failedBecauseLocked.size());
        getOut().println(" Failed because access denied to document: " + failedBecauseAccessDenied.size());
        getOut().println(" Failed for other reason: " + failed.size());


        try {
            File summaryFile;
            SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
            dateFormat.applyPattern("yyyyMMddHHmmss");
            String baseName = "importsummary_" + dateFormat.format(new Date());
            int c = 1;
            while (true) {
                File file = new File(baseName + c + ".xml");
                if (file.createNewFile()) {
                    summaryFile = file;
                    break;
                }
                c++;
            }

            FileOutputStream fos = new FileOutputStream(summaryFile);

            Properties props = new Properties();
            props.put(OutputKeys.INDENT, "yes");
            XmlSerializer xmlSerializer = new XmlSerializer(fos, props);
            listener.generateSax(xmlSerializer);
            fos.close();

            getOut().println("Detailed document listing written to " + summaryFile.getAbsolutePath());
            getOut().println();
        } catch (Exception e) {
            getOut().println("Error writing summary file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public PrintStream getOut() {
        return System.out;
    }

    private void printHelp(Options cliOptions) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("daisy-import", cliOptions, true);
        System.out.println();
        System.out.println("To import, you need at least:");
        System.out.println();
        System.out.println("daisy-import -u <username> -i /path/to/importdata[.zip]");
        System.out.println();
        System.out.println("/path/to/importdata is the data to be imported. This could be created");
        System.out.println("with the daisy-export tool or in some other way.");
        System.out.println();
    }

    class ImportCliShutdownHook extends Thread {
        CliImportListener listener;
        Thread importThread;

        public ImportCliShutdownHook(CliImportListener listener, Thread importThread) {
            this.listener = listener;
            this.importThread = importThread;
        }

        public void run() {
            if (!isImportRunning())
                return;
            listener.interrupt();
            try {
                importThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
