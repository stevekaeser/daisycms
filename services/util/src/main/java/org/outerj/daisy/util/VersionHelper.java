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
package org.outerj.daisy.util;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;

public class VersionHelper {
    public static String getVersion(Properties versionProps) {
        return versionProps.getProperty("artifact.version");
    }

    public static String formatVersionString(Properties versionProps) {
        // stored version info
        String version = versionProps.getProperty("artifact.version");
        String hostName = versionProps.getProperty("build.hostname");
        String date = versionProps.getProperty("build.datetime");

        // runtime
        String os = System.getProperty("os.name");
        String osArch = System.getProperty("os.arch");
        String osVersion = System.getProperty("os.version");
        String vmVersion = System.getProperty("java.vm.version");

        return version + " (build: " + hostName + "/" + date + "; run: " + os + "/" + osArch + "/" + osVersion + " java/" + vmVersion + ")";
    }

    public static Properties getVersionProperties(ClassLoader classLoader, String versionPropsLocation) throws IOException {
        Properties versionProps = new Properties();
        InputStream versionPropIs = null;
        try {
            versionPropIs = classLoader.getResourceAsStream(versionPropsLocation);
            if (versionPropIs == null)
                throw new IOException("Version properties files could not be found: " + versionPropsLocation);
            versionProps.load(versionPropIs);
        } finally {
            if (versionPropIs != null)
                versionPropIs.close();
        }
        return versionProps;
    }

    public static String getVersionString(ClassLoader classLoader, String versionPropsLocation) throws IOException {
        Properties properties = getVersionProperties(classLoader, versionPropsLocation);
        return formatVersionString(properties);
    }
}
