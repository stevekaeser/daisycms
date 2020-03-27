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
package org.outerj.daisy.backupTool;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.outerj.daisy.util.VersionHelper;

public class BackupTool {

    private File backupLocation;

    private static String XPATH_JMX_CONF = "/targets/target[@path = '/daisy/jmx/mbeanserver']/configuration/xmlHttpAdaptor";

    public static void main(String[] args) throws Exception {
        Options options = createOptions();
        CommandLineParser parser = new PosixParser();
        Map<String, File> confMap = new HashMap<String, File>();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                printHelp(options);
            } else if (cmd.hasOption('v')) {
                String versionString = VersionHelper.getVersionString(BackupTool.class.getClassLoader(),
                        "org/outerj/daisy/backupTool/versioninfo.properties");
                System.out.println(versionString);
            } else if ( (cmd.hasOption('b') || cmd.hasOption('r')) && !cmd.hasOption('l') ) {
                System.out.println("The -l option is required when using -b or -r.");
            } else if ( (cmd.hasOption('b') || cmd.hasOption('r'))  && !cmd.hasOption('d') ) {
                System.out.println("The -d option is required when using -b or -r.");
            } else {
                try {
                    BackupTool tool;
                    File backupLocation = null;
                    File datadir = null;

                    if (cmd.hasOption("a"))
                        confMap.put("additional-entries", new File(cmd.getOptionValue("a")));

                    if (cmd.hasOption("d")) {
                        String datadirPath = cmd.getOptionValue("d");
                        datadir = new File(datadirPath);
                        confMap.put("datadir", datadir);                    
                    }
                    
                    if (cmd.hasOption("l")) {
                        backupLocation = new File(cmd.getOptionValue("l"));
                    }

                    if (cmd.hasOption("b")) {
                        if (!datadir.exists()) {
                            System.out.println("Specified data dir does not exist: " + datadir.getAbsolutePath());
                            System.exit(1);
                        }

                        File myConfig = new File(datadir, "conf" + File.separator + "myconfig.xml");
                        confMap.put("myconfig", myConfig);

                        tool = new BackupTool(confMap, backupLocation);
                        tool.createBackup();

                    } else if (cmd.hasOption("r")) {
                        String backupName = cmd.getOptionValue("r");
                        tool = new BackupTool(confMap, backupLocation, backupName);
                        tool.restoreBackup(backupName, cmd.hasOption('q'));
                    } else if (cmd.hasOption("R")) {
                        String backupName = cmd.getOptionValue("R");
                        tool = new BackupTool(confMap, backupLocation, backupName);
                        tool.rehashBackup(backupName);
                    }

                } catch (Exception e) {
                    if (cmd.hasOption("e") && cmd.hasOption("s"))
                        sendExceptionEmail(cmd.getOptionValue("e"), cmd.getOptionValue("f"), cmd.getOptionValue("s"), e); 

                    e.printStackTrace();
                    System.exit(1);
                }
            }
        } catch (ParseException e) {
            if (e instanceof MissingOptionException)
                System.err.println(e.getMessage());
            else
                e.printStackTrace();

            printHelp(options);
            System.exit(1);
        }
    }

    public BackupTool(Map<String, File> confMap, File backupLocation, String backupname) throws Exception {
        if (!backupLocation.exists())
            throw new FileNotFoundException ("The backup directory " + backupLocation.getPath() + " does not exist");

        this.backupLocation = backupLocation;
        File backupInstance = new File(backupLocation, backupname);
        if (!backupInstance.exists())
            throw new FileNotFoundException ("The backup at " + backupInstance.getPath() + " could not be found");

        File confDir = new File(backupInstance, "conftemp");
        BackupHelper.unzipToDirectory(new File(backupInstance, "daisy-conf.zip"), confDir);
        confMap.put("myconfig", new File(confDir, "myconfig.xml"));
        confMap.put("confdir", confDir);

        init(confMap);
        BackupHelper.deleteFile(confDir);
    }

    public BackupTool(Map confMap,  File backupLocation) throws Exception {
        if (!backupLocation.exists()) {
            if (!backupLocation.mkdirs())
                throw new Exception("Could not create the backup directory " + backupLocation.getAbsolutePath());
        }
        this.backupLocation = backupLocation;
        init(confMap);
    }

    private void init(Map confMap) throws Exception {
        if (confMap.containsKey("myconfig")) {
            File myConfig = (File)confMap.get("myconfig");

            Document myConfigDocument = BackupHelper.parseFile(myConfig);
            Element jmxConf = BackupHelper.getElementFromDom(myConfigDocument, XPATH_JMX_CONF);

            JMXRepositoryLocker locker = new JMXRepositoryLocker(jmxConf.getAttribute("host"), Integer.parseInt(jmxConf.getAttribute("port")), jmxConf
                    .getAttribute("username"), jmxConf.getAttribute("password"));

            BackupManager.setLocker(locker);
            BackupManager.setBackupLocation(backupLocation);

            if (confMap.containsKey("datadir")) {
                File datadir = (File)confMap.get("datadir");
                BackupManager.getInstance().registerEntryLoader(new DaisyEntryLoader(myConfig, datadir));

                // Extracting the location of the activemq-conf.xml file out of the myconfig.xml
                // is not so easy, not only because it is embedded inside an URL but also because
                // it probably contains a ${daisy.datadir} reference, and we need to read the
                // activemq-conf.xml which is inside the backup. So we will just assume users
                // have not adjusted the default location.
                File confdir = (File)confMap.get("confdir");
                if (confdir == null) // no confdir means we're backuping, not restoring
                    confdir = new File(datadir, "conf");
                File amqConfFile = new File(confdir, "activemq-conf.xml");
                BackupManager.getInstance().registerEntryLoader(new ActiveMQEntryLoader(amqConfFile));
            }
        }

        if (confMap.containsKey("openjms"))
            BackupManager.getInstance().registerEntryLoader(new OpenJMSEntryLoader((File)confMap.get("openjms")));

        if (confMap.containsKey("additional-entries"))
            BackupManager.getInstance().registerEntryLoader(new FileListEntryLoader((File)confMap.get("additional-entries")));
    }

    public void createBackup() throws Exception {
        BackupInstance buInstance = BackupManager.getInstance().createBackupInstance();
        BackupManager.getInstance().backup(buInstance);
    }

    public void restoreBackup(String backupName, boolean quietRestore) throws Exception {
        boolean doRestore = true;
        BackupInstance buInstance = BackupManager.getInstance().loadBackupInstance(backupName, true);

        System.out.println();
        System.out.println("Before restoring a backup, make sure that the repository server");
        System.out.println("and the Daisy Wiki are not running.");
        System.out.println();

        if(!(quietRestore)) {
            doRestore = BackupHelper.promptYesNo("Restoring this backup will undo all changes made since " + buInstance.getCreateDate().toString()
                + "\nDo you wish to restore the backup? [yes|no, default: no]", false);
        }
        if (doRestore)               
            BackupManager.getInstance().restore(buInstance);    
    }
    
    public void rehashBackup(String backupName) throws Exception {
        BackupInstance buInstance = BackupManager.getInstance().loadBackupInstance(backupName, false);
        System.out.println("Rehashing backup entries at " + buInstance.getDirectory().getAbsolutePath());        
        BackupManager.getInstance().rehash(buInstance);
        System.out.println("Done");
    }

    private static void printHelp(Options o) {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("daisy-backup-tool", o, true);
    }

    private static Options createOptions() {
        Options options = new Options();

        Option versionOption = new Option("v", "version", false, "Print version info");

        Option configOption = new Option("d", "daisy-data-dir", true, "Daisy data directory");
        configOption.setArgName("daisydata-path");

        Option backupLocationOption = new Option("l", "backuplocation", true, "Location where backups are stored");
        backupLocationOption.setArgName("backup-path");

        Option emailTo = new Option("e", "emailaddress", true, "Where emails will be sent to in case of an exception");
        emailTo.setArgName("email-address");
        
        Option emailFrom = new Option("f", "fromaddress", true, "Sender address to use for exception emails"); 
        emailFrom.setArgName("from-address"); 

        Option smtpServer = new Option("s", "smtp-server", true, "Smtp server");
        smtpServer.setArgName("smtp-server");

        Option additionalEntries = new Option("a", "additional-entries", true, "Path to configuration of additional backup entries");
        additionalEntries.setArgName("entry-configuration-file");

        Option quietRestore = new Option("q", "quiet-restore", false, "Suppress confirmation before restoring an existing backup");

        OptionGroup commandGroup = new OptionGroup();
        Option restoreOption = new Option("r", "restore", true, "Restore an existing backup");
        restoreOption.setArgName("backup-name");
        Option backupOption = new Option("b", "backup", false, "Create a new backup");
        Option rehashOption = new Option("R", "rehash", true, "Rehash files from an existing backup");
        rehashOption.setArgName("backup-name");
        commandGroup.addOption(backupOption);
        commandGroup.addOption(restoreOption);
        commandGroup.addOption(rehashOption);
        commandGroup.addOption(versionOption);
        commandGroup.addOption( new Option("h", "help", false, "Show this message"));
        commandGroup.setRequired(true);

        
        options.addOption(configOption);
        options.addOption(backupLocationOption);        
        options.addOptionGroup(commandGroup);
//        options.addOption(openjmsConfigOption);
        options.addOption(emailTo);
        options.addOption(emailFrom); 
        options.addOption(smtpServer);
        options.addOption(additionalEntries);
        options.addOption(quietRestore);

        return options;
    }

    private static void sendExceptionEmail(String to, String from, String smtpHost, Exception e) throws Exception { 
        Properties props = new Properties();
        props.put("mail.smtp.host", smtpHost);

        MimeMessage msg = new MimeMessage(Session.getInstance(props));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        msg.setSubject("Daisy Backup Failed");
        
        if (from != null) {
            msg.setFrom(new InternetAddress(from));
        } 

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String text = "Stacktrace :\n" + sw.toString();

        msg.setText(text);
        msg.setSentDate(new Date());

        Transport.send(msg);
    }

}
