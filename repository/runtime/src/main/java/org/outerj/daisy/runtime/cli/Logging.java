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

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

public class Logging {
    public static final String INFO_LOG_CATEGORY = "org.outerj.daisy.runtime.info";
    public static final String CLASSLOADING_LOG_CATEGORY = "org.outerj.daisy.runtime.classloading-info";
    public static final String CLASSLOADING_REPORT_CATEGORY = "org.outerj.daisy.runtime.classloading-report";

    public static void setupLogging(boolean verbose, boolean quiet, boolean classLoadingLog, String logConfLocation,
            String consoleLoggingLevel, String consoleLogCategory) {
        ConsoleAppender consoleAppender = new ConsoleAppender();
        consoleAppender.setName("console appender");
        consoleAppender.setLayout(new PatternLayout("[%t] %-5p %c - %m%n"));
        consoleAppender.activateOptions();

        boolean consoleAppenderAdded = false; // an appender should be added only once for a certain category (include inheritance) to avoid duplicate log messages

        if (logConfLocation != null) {
            if (logConfLocation.endsWith(".xml")) {
                DOMConfigurator.configure(logConfLocation);
            } else {
                PropertyConfigurator.configure(logConfLocation);
            }
        } else if (consoleLoggingLevel == null) {
            // If there's not log configuration specified, and console logging not enabled,
            // then default to printing error messages to the console
            System.out.println("Note: it is recommended to specify a log configuration. Will print error logs to the console.");
            Logger logger = Logger.getRootLogger();
            logger.setLevel(Level.ERROR);
            logger.addAppender(consoleAppender);
            consoleAppenderAdded = true;
        }

        if (consoleLoggingLevel != null) {
            Level level = null;
            if (consoleLoggingLevel.equalsIgnoreCase("trace"))
                level = Level.TRACE;
            else if (consoleLoggingLevel.equalsIgnoreCase("debug"))
                level = Level.DEBUG;
            else if (consoleLoggingLevel.equalsIgnoreCase("info"))
                level = Level.INFO;
            else if (consoleLoggingLevel.equalsIgnoreCase("error"))
                level = Level.ERROR;
            else if (consoleLoggingLevel.equalsIgnoreCase("fatal"))
                level = Level.FATAL;
            else
                System.err.println("Unrecognized log level: " + consoleLoggingLevel);

            if (level != null) {
                System.out.println("Setting console output for log level " + level.toString() + " on category " + consoleLogCategory);
                Logger logger = consoleLogCategory == null ? Logger.getRootLogger() : Logger.getLogger(consoleLogCategory);
                logger.setLevel(level);

                if (consoleLogCategory != null)
                    Logger.getRootLogger().setLevel(Level.ERROR);
                
                Logger rootLogger = Logger.getRootLogger();
                rootLogger.addAppender(consoleAppender);
                consoleAppenderAdded = true;
            }
        }

        if (quiet) {
            Logger logger = Logger.getLogger("org.outerj.daisy.runtime");
            logger.setLevel(Level.ERROR);
            return;
        }

        if (verbose) {
            Logger logger = Logger.getLogger("org.outerj.daisy.runtime");
            logger.setLevel(Level.DEBUG);
            logger.addAppender(consoleAppender);
            return;
        }

        Logger logger = Logger.getLogger(INFO_LOG_CATEGORY);
        logger.setLevel(Level.INFO);
        if (!consoleAppenderAdded)
            logger.addAppender(consoleAppender);

        if (classLoadingLog) {
            logger = Logger.getLogger(CLASSLOADING_LOG_CATEGORY);
            logger.setLevel(Level.INFO);
            if (!consoleAppenderAdded)
                logger.addAppender(consoleAppender);

            logger = Logger.getLogger(CLASSLOADING_REPORT_CATEGORY);
            logger.setLevel(Level.INFO);
            if (!consoleAppenderAdded)
                logger.addAppender(consoleAppender);
        } else {
            // Always print classloader warnings
            logger = Logger.getLogger(CLASSLOADING_LOG_CATEGORY);
            logger.setLevel(Level.WARN);
            if (!consoleAppenderAdded)
                logger.addAppender(consoleAppender);
        }
    }
}
