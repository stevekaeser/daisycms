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
package org.outerj.daisy.tools.importexport.export.cli;

import org.outerj.daisy.repository.clientimpl.RemoteRepositoryManager;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.tools.importexport.export.config.ExportOptions;
import org.outerj.daisy.tools.importexport.export.config.ExportOptionsFactory;
import org.outerj.daisy.tools.importexport.export.ExportSetFactory;
import org.outerj.daisy.tools.importexport.export.ExportSet;
import org.outerj.daisy.tools.importexport.export.Exporter;
import org.outerj.daisy.tools.importexport.export.tm.TMExporter;
import org.outerj.daisy.tools.importexport.export.fs.ExportFile;
import org.outerj.daisy.tools.importexport.export.fs.ExportFileFactory;
import org.outerj.daisy.tools.importexport.util.ImportExportUtil;
import org.outerj.daisy.tools.importexport.ImportExportException;
import org.outerj.daisy.tools.importexport.ImpExpFormat;
import org.outerj.daisy.tools.importexport.tm.TMConfig;
import org.outerj.daisy.tools.importexport.tm.TMConfigFactory;
import org.outerj.daisy.util.VersionHelper;
import org.outerj.daisy.util.CliUtil;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.apache.commons.cli.*;

import javax.xml.transform.OutputKeys;
import java.io.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
 * Command-line interface of the export tool.
 */
public class ExportCli {
    private boolean exportRunning = false;

    public static void main(String[] args) throws Exception {
        new ExportCli().run(args);
    }

    private ExportCli() {

    }

    private void run(String[] args) throws Exception {
        Options cliOptions = new Options();

        Option optionsOption = new Option("o", "options", true, "Specifies path to options XML file (use -d to get a template)");
        cliOptions.addOption(optionsOption);

        Option dumpOptionsOption = new Option("d", "dump-sample-options", false, "Shows sample options to provide via -o");
        cliOptions.addOption(dumpOptionsOption);

        Option exportPathOption = new Option("e", "export", true, "Path to the export location (dir or zip)");
        cliOptions.addOption(exportPathOption);

        Option exportSetOption = new Option("f", "exportset", true, "File specifying docs to export");
        cliOptions.addOption(exportSetOption);

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

        Option tmConfigOption = new Option("g", "tm-config", true, "Only for tm (translation management) import: tm-specific config file");
        cliOptions.addOption(tmConfigOption);

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
            System.out.println(ExportOptionsFactory.toXml(ExportOptionsFactory.getDefaultExportOptions()));
            return;
        }

        if (!cmd.hasOption(exportPathOption.getOpt())) {
            System.out.println("Missing -" + exportPathOption.getOpt() + " argument");
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

        if (!cmd.hasOption(exportSetOption.getOpt())) {
            System.out.println("Missing -" + exportSetOption.getOpt() + " argument");
            System.exit(1);
        }

        CliExportListener listener = null;
        int exitCode = 0;
        try {
            System.out.println("\nConnecting to the repository.");
            RemoteRepositoryManager repositoryManager = new RemoteRepositoryManager(repositoryURL, new Credentials(user, pwd));
            Repository repository = repositoryManager.getRepository(new Credentials(user, pwd));

            if (cmd.hasOption(repoRolesOption.getOpt())) {
                long[] roles = ImportExportUtil.parseRoles(cmd.getOptionValue(repoRolesOption.getOpt()), repository);
                repository.setActiveRoleIds(roles);
            }

            ExportOptions options;
            if (cmd.hasOption(optionsOption.getOpt())) {
                String optionsPath = cmd.getOptionValue(optionsOption.getOpt());
                InputStream is = null;
                try {
                    is = new FileInputStream(optionsPath);
                    options = ExportOptionsFactory.parseFromXml(is, repository);
                } finally {
                    if (is != null)
                        try { is.close(); } catch (Exception e) { /* ignore */ }
                }
            } else {
                options = ExportOptionsFactory.getDefaultExportOptions();
            }

            listener = new CliExportListener(getOut(), options);

            ExportFile exportFile = ExportFileFactory.getExportFile(new File(cmd.getOptionValue(exportPathOption.getOpt())), listener);

            if (!cmd.hasOption(skipVersionCheckOption.getOpt())) {
                String serverVersion = repository.getServerVersion();
                String exportToolVersion = versionProperties.getProperty("artifact.version");
                if (!serverVersion.equals(exportToolVersion)) {
                    System.out.println("Version of export tool and repository server do not match.");
                    System.out.println("Version of export tool: " + exportToolVersion);
                    System.out.println("Version of server     : " + serverVersion);
                    System.out.println();
                    System.out.println("It is recommended to use a corresponding version. To skip the version check, use -" + skipVersionCheckOption.getOpt());
                    System.exit(1);
                }
            }

            String formatName = cmd.getOptionValue(formatOption.getOpt());
            ImpExpFormat format = formatName == null ? ImpExpFormat.DEFAULT : ImpExpFormat.fromString(formatName);

            TMConfig tmConfig = null;
            if (format == ImpExpFormat.TRANSLATION_MANAGEMENT) {
                if (cmd.hasOption(tmConfigOption.getOpt())) {
                    String tmConfigFile = cmd.getOptionValue(tmConfigOption.getOpt());
                    tmConfig = TMConfigFactory.parseFromXml(tmConfigFile, repository);
                } else {
                    tmConfig = new TMConfig();
                }
            }

            Date exportTime = new Date();
            ExportSet exportSet;
            InputStream is = null;
            try {
                is = new FileInputStream(cmd.getOptionValue(exportSetOption.getOpt()));
                exportSet = ExportSetFactory.parseFromXml(is, repository);
            } catch (Throwable e) {
                throw new ImportExportException("Error reading export document set.", e);
            } finally {
                if (is != null)
                    is.close();
            }

            final Thread mainThread = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new ExportCliShutdownHook(listener, mainThread));

            try {
                setExportRunning(true);
                switch (format) {
                    case DEFAULT:
                        Exporter.run(exportSet, exportFile, exportTime, repository, options, listener);
                        break;
                    case TRANSLATION_MANAGEMENT:
                        TMExporter.run(exportSet, exportFile, exportTime, repository, options, listener, tmConfig);
                        break;
                    default:
                        throw new RuntimeException("Unrecognized format: " + format);
                }
                exportFile.finish();
            } finally {
                setExportRunning(false);
                printExportSummary(listener);
            }
        } catch (Throwable e) {
            ImportExportUtil.logError(e, "exporterror", getOut());
            exitCode = 1;
        }

        if (listener == null || !listener.isInterrupted()) {
            // Only call System.exit when the export was not interrupted, otherwise
            // the ExportCliShutdownHook will keep waiting
            System.exit(exitCode);
        }
    }

    private PrintStream getOut() {
        return System.out;
    }

    private synchronized void setExportRunning(boolean running) {
        this.exportRunning = running;
    }

    private synchronized boolean isExportRunning() {
        return exportRunning;
    }

    private void printExportSummary(CliExportListener listener) {
        List failed = listener.getFailed();
        List succeeded = listener.getSucceeded();
        List skippedBecauseRetired = listener.getSkippedBecauseRetired();
        List skippedBecauseNoLiveVersion = listener.getSkippedBecauseNoLiveVersion();
        List brokenLinks = listener.getBrokenLinks();
        List failedItems = listener.getFailedItems();

        getOut().println();
        getOut().println("Export summary");
        getOut().println("--------------");
        getOut().println("Succeeded: " + succeeded.size());
        getOut().println("Failed: " + failed.size());
        getOut().println("Skipped because retired: " + skippedBecauseRetired.size());
        getOut().println("Skipped because no live version: " + skippedBecauseNoLiveVersion.size());
        getOut().println("Number of docs that are not included and cause broken links: " + brokenLinks.size());
        getOut().println("Non-document failures (optional schema types etc.): " + failedItems.size());

        try {
            File summaryFile;
            SimpleDateFormat dateFormat = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            dateFormat.applyPattern("yyyyMMddHHmmss");
            String baseName = "exportsummary_" + dateFormat.format(new Date());
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
            PrintWriter writer = new PrintWriter(fos);

            Properties props = new Properties();
            props.put(OutputKeys.INDENT, "yes");
            XmlSerializer xmlSerializer = new XmlSerializer(fos, props);
            listener.generateSax(xmlSerializer);
            fos.close();

            writer.close();
            fos.close();
            getOut().println("Detailed document listing written to " + summaryFile.getAbsolutePath());
            getOut().println();
        } catch (Exception e) {
            getOut().println("Error writing summary file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void printHelp(Options cliOptions) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("daisy-export", cliOptions, true);
        System.out.println();
        System.out.println("To export, you need at least:");
        System.out.println();
        System.out.println("daisy-export -u <username> -e /path/to/export[.zip] -f exportset.xml");
        System.out.println();
        System.out.println("exportset.xml is an XML file specifying the files to export, using a query or manually listed.");
        System.out.println("");
        System.out.println("Example:");
        System.out.println("");
        System.out.println("<documents>");
        System.out.println("  <!--");
        System.out.println("       List <query> and/or <document> elements.");
        System.out.println("       On <document>, branch and language are optional (defaults to main resp. default)");
        System.out.println("  -->");
        System.out.println("  <query>select id where true</query>");
        System.out.println("  <document id=\"123-DSY\" branch=\"somebranch\" language=\"somelanguage\"/>");
        System.out.println("</documents>");
        System.out.println();
        System.out.println("Use -o you can control various options, again specified in an XML file.");
        System.out.println("Use -d to see an example of such a file, showing the default configuration.");
        System.out.println();
    }

    class ExportCliShutdownHook extends Thread {
        CliExportListener listener;
        Thread exportThread;

        public ExportCliShutdownHook(CliExportListener listener, Thread exportThread) {
            this.listener = listener;
            this.exportThread = exportThread;
        }

        public void run() {
            if (!isExportRunning())
                return;
            listener.interrupt();
            try {
                exportThread.join();
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }
}
