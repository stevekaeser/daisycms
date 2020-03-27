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
package org.outerj.daisy.maven.versioninfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal versioninfo
 */
public class VersionInfoMojo extends AbstractMojo {
    
    /**
     * @parameter expression="${versioninfo.propfile}" default-value="${project.build.outputDirectory}/version.properties"
     */
    private File propFile;
    
    /**
     * @parameter expression="${versioninfo.version}" default-value="${project.version}"/>
     */
    private String version;
    
    public static void generateVersionInfo(File propFile, String version) throws IOException {
        Properties versionInfo = new Properties();

        versionInfo.put("artifact.version", version);

        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostName = "Unknown";
        }
        versionInfo.put("build.hostname", hostName);

        versionInfo.put("build.user.name", System.getProperty("user.name"));
        versionInfo.put("build.os.name", System.getProperty("os.name"));
        versionInfo.put("build.os.arch", System.getProperty("os.arch"));
        versionInfo.put("build.os.version", System.getProperty("os.version"));
        versionInfo.put("build.java.vm.version", System.getProperty("java.vm.version"));
        versionInfo.put("build.java.vm.vendor", System.getProperty("java.vm.vendor"));
        versionInfo.put("build.java.vm.name", System.getProperty("java.vm.name"));

        SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
        dateFormat.applyPattern("yyyyMMdd");
        SimpleDateFormat dateTimeFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, Locale.getDefault());
        dateTimeFormat.applyPattern("yyyyMMdd HH:mm:ssZ");

        Date now = new Date();
        versionInfo.put("build.date", dateFormat.format(now));
        versionInfo.put("build.datetime", dateTimeFormat.format(now));

        propFile.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(propFile);
        try {
            versionInfo.store(fos, "Daisy build & version info");
        } finally {
            fos.close();
        }
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            generateVersionInfo(propFile, version);
        } catch (IOException ioe) {
            throw new MojoFailureException("Failed to generate version property file " + propFile.getPath());
        }
    }
    
    public void setPropFile(File propFile) {
        this.propFile = propFile;
    }

    public void setVersion(String version) {
        this.version = version;
    }   
}