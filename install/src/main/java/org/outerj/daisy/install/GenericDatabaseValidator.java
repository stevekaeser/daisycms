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
package org.outerj.daisy.install;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class GenericDatabaseValidator implements DatabaseValidator {
    
    private static Log log = LogFactory.getLog(GenericDatabaseValidator.class);
    
    private String databaseProductName;
    private int majorVersion;
    private int minorVersion;
    private int patchVersion;

    public GenericDatabaseValidator(String databaseProductName, int majorVersion, int minorVersion, int patchVersion) {
        this.databaseProductName = databaseProductName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.patchVersion = patchVersion;
    }

    public void validate(Connection conn, String humanDbName, List<String> warnings, List<String> minorProblems, List<String> majorProblems) throws Exception {
        DatabaseMetaData metadata = conn.getMetaData();

        String dbProductName = metadata.getDatabaseProductName();
        if (!dbProductName.equalsIgnoreCase(databaseProductName)) {
            String problem = humanDbName + " reported it is \"" + dbProductName + "\" but \"" + databaseProductName + "\" was expected.";
            minorProblems.add(problem);
            return; // further checks don't make sense
        }

        log.info("Detected database version: " + metadata.getDatabaseProductVersion());

        int foundMajorVersion = metadata.getDatabaseMajorVersion();
        int foundMinorVersion = metadata.getDatabaseMinorVersion();
        if ((foundMajorVersion != majorVersion && majorVersion > -1) || (foundMinorVersion != minorVersion && minorVersion > -1)) {
            String problem = "Expected version " + majorVersion + "." + minorVersion + " for " + humanDbName + " but instead found " + foundMajorVersion + "." + foundMinorVersion;
            minorProblems.add(problem);
            return; // further checks don't make sense
        }

        // check patch version if desired, this code is probably quite mysql specific
        if (patchVersion != -1) {
            Pattern versionPattern = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+).*");
            Matcher versionMatcher = versionPattern.matcher(metadata.getDatabaseProductVersion());
            if (versionMatcher.matches()) {
                int majorVersion = Integer.parseInt(versionMatcher.group(1));
                int minorVersion = Integer.parseInt(versionMatcher.group(2));
                int patchVersion = Integer.parseInt(versionMatcher.group(3));

                if (patchVersion < this.patchVersion) {
                    String version = majorVersion + "." + minorVersion + "." + patchVersion;
                    String recommendedVersion = majorVersion + "." + minorVersion + "." + patchVersion;
                    minorProblems.add(humanDbName + " is using " + databaseProductName + " version " + version + " but for Daisy we recommend at least" + recommendedVersion);
                }
            } else {
                minorProblems.add(humanDbName + " has an unrecognized version string: " + metadata.getDatabaseProductName());
            }
        }
        
    }

}
