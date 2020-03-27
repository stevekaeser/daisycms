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
package org.outerj.daisy.runtime.cli;

import org.outerj.daisy.runtime.repository.ArtifactRepository;
import org.outerj.daisy.runtime.repository.ChainedMaven1StyleArtifactRepository;
import org.outerj.daisy.runtime.DaisyRuntimeConfig;
import org.outerj.daisy.runtime.XmlDaisyRuntimeConfigBuilder;
import org.outerj.daisy.runtime.DaisyRTException;
import org.outerj.daisy.runtime.DaisyRuntime;
import org.apache.commons.cli.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.text.DateFormat;
import java.util.*;

@SuppressWarnings({"AccessStaticViaInstance"})
public class DaisyRuntimeCli {
    protected final Log infolog = LogFactory.getLog(Logging.INFO_LOG_CATEGORY);

    public static void main(String[] args) throws Exception {
        new DaisyRuntimeCli().run(args);
    }

    private DaisyRuntimeCli() {

    }

    private void run(String[] args) throws Exception {
        Options cliOptions = new Options();

        Option configOption = OptionBuilder
                .withArgName("configfile")
                .hasArg()
                .withDescription("The Daisy runtime configuration file")
                .withLongOpt("config")
                .create('c');
        cliOptions.addOption(configOption);

        Option repositoryLocationOption = OptionBuilder
                .withArgName("maven-repo-path")
                .hasArg()
                .withDescription("Location of the (Maven-style) artifact repository. Use comma-separated entries to specify multiple locations which will be searched in the order as specified.")
                .withLongOpt("repository")
                .create('r');
        cliOptions.addOption(repositoryLocationOption);

        Option disabledContainersOption = OptionBuilder
                .withArgName("cont-id1,cont-id2,...")
                .hasArg()
                .withDescription("Comma-separated list of containers that should be disabled.")
                .withLongOpt("disable-containers")
                .create('i');
        cliOptions.addOption(disabledContainersOption);

        Option disableClassSharingOption = OptionBuilder
                .withDescription("Disable optional sharing of classes between containers")
                .withLongOpt("disable-class-sharing")
                .create('d');
        cliOptions.addOption(disableClassSharingOption);

        Option consoleLoggingOption = OptionBuilder
                .withArgName("loglevel")
                .hasArg()
                .withDescription("Enable logging to console for the root log category with specified loglevel (debug, info, warn, error)")
                .withLongOpt("console-logging")
                .create('l');
        cliOptions.addOption(consoleLoggingOption);

        Option consoleLogCatOption = OptionBuilder
                .withArgName("logcategory")
                .hasArg()
                .withDescription("Enable console logging only for this category")
                .withLongOpt("console-log-category")
                .create('m');
        cliOptions.addOption(consoleLogCatOption);

        Option logConfigurationOption = OptionBuilder
                .withArgName("config")
                .hasArg()
                .withDescription("Log4j configuration file (properties or .xml)")
                .withLongOpt("log-configuration")
                .create("o");
        cliOptions.addOption(logConfigurationOption);

        Option classLoadingLoggingOption = OptionBuilder
                .withDescription("Print information about the classloader setup (at startup).")
                .withLongOpt("classloader-log")
                .create("z");
        cliOptions.addOption(classLoadingLoggingOption);

        Option verboseOption = OptionBuilder
                .withDescription("Prints lots of information.")
                .withLongOpt("verbose")
                .create("v");
        cliOptions.addOption(verboseOption);

        Option quietOption = OptionBuilder
                .withDescription("Suppress normal output.")
                .withLongOpt("quiet")
                .create("q");
        cliOptions.addOption(quietOption);

        Option helpOption = new Option("h", "help", false, "Shows help");
        cliOptions.addOption(helpOption);

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

        Logging.setupLogging(cmd.hasOption(verboseOption.getOpt()), cmd.hasOption(quietOption.getOpt()),
                cmd.hasOption(classLoadingLoggingOption.getOpt()), cmd.getOptionValue(logConfigurationOption.getOpt()),
                cmd.getOptionValue(consoleLoggingOption.getOpt()), cmd.getOptionValue(consoleLogCatOption.getOpt()));

        infolog.info("Starting the Daisy repository server.");
        
        File configFile = null;
        File repositoryFile = null;

        if (!cmd.hasOption(configOption.getOpt())) {
            System.out.println("Missing -" + configOption.getOpt() + " argument");
            System.exit(1);
        } else {
            configFile = new File(cmd.getOptionValue(configOption.getOpt()));
            if (!configFile.exists()) {
                System.out.println("Specified config file does not exist: " + configFile.getAbsolutePath());
                System.exit(1);
            }
        }

        if (!cmd.hasOption(repositoryLocationOption.getOpt())) {
            System.out.println("Missing -" + repositoryLocationOption.getOpt() + " argument");
            System.exit(1);
        }

        ArtifactRepository artifactRepository = new ChainedMaven1StyleArtifactRepository(cmd.getOptionValue(repositoryLocationOption.getOpt()));

        Set<String> disabledContainerIds = getDisabledContainerIds(cmd.getOptionValue(disabledContainersOption.getOpt()));

        DaisyRuntimeConfig runtimeConfig;
        try {
            runtimeConfig = XmlDaisyRuntimeConfigBuilder.build(configFile, disabledContainerIds, artifactRepository, new Properties());
        } catch (Throwable e) {
            throw new DaisyRTException("Error reading runtime configuration file " + configFile.getAbsolutePath(), e);
        }

        if (cmd.hasOption(disableClassSharingOption.getOpt()))
            runtimeConfig.setEnableArtifactSharing(false);

        DaisyRuntime runtime = new DaisyRuntime(runtimeConfig);
        try {
            runtime.init();
            Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHandler(runtime)));
            printStartedMessage();
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Startup failed. Will try to shutdown and exit.");
            try {
                runtime.shutdown();
            } finally {
                System.exit(1);
            }
        }

    }

    private void printStartedMessage() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        String now = dateFormat.format(new Date());
        infolog.info("Daisy repository server started [" + now + "]");
    }

    private void printHelp(Options cliOptions) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("daisy-runtime", cliOptions, true);
    }

    private Set<String> getDisabledContainerIds(String spec) {
        if (spec == null)
            return Collections.emptySet();

        Set<String> ids = new HashSet<String>();

        String[] items = spec.split(",");
        for (String item : items) {
            item = item.trim();
            if (item.length() > 0)
                ids.add(item);
        }

        return ids;
    }

    public static class ShutdownHandler implements Runnable {
        private final DaisyRuntime runtime;

        public ShutdownHandler(DaisyRuntime runtime) {
            this.runtime = runtime;
        }

        public void run() {
            runtime.shutdown();
        }
    }
}
