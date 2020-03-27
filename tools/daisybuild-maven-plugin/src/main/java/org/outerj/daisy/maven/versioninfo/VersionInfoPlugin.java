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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.Locale;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class VersionInfoPlugin {
    public static void generateVersionInfo(String propFilePath, String version) throws IOException {
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

        File file = new File(propFilePath);
        file.getParentFile().mkdirs();
        FileOutputStream fos = new FileOutputStream(file);
        try {
            versionInfo.store(fos, "Daisy build & version info");
        } finally {
            fos.close();
        }
    }
}
