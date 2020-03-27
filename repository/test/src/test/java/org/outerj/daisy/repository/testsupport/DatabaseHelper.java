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
package org.outerj.daisy.repository.testsupport;

import org.outerj.daisy.install.DatabaseCreator;
import org.outerj.daisy.jdbcutil.DriverLoader;

import java.util.List;

public class DatabaseHelper {
    private String dbdriver;
    private String dburl;
    private String dbuser;
    private String dbpwd;
    private String dbname;

    public DatabaseHelper(TestSupportConfig config) throws Exception {
        dbdriver = config.getRequiredProperty("testsupport.driver");
        dburl = config.getRequiredProperty("testsupport.dburl");
        dbuser = config.getRequiredProperty("testsupport.dbuser");
        dbpwd = config.getRequiredProperty("testsupport.dbpwd");
        dbname = config.getRequiredProperty("testsupport.dbname");
        String driverClassPath = config.getRequiredProperty("testsupport.driverClasspath");
        DriverLoader.loadDatabaseDriver(driverClassPath, dbdriver);
    }

    public void resetDatabase(String bootstrapuser, String bootstrappwd) throws Exception {
        DatabaseCreator dbCreator = new DatabaseCreator();
        dbCreator.clearDatabase(dburl, dbuser, dbpwd, dbname);

        List<DatabaseCreator.NewUserInfo> newUserInfos = DatabaseCreator.createDefaultUsers(
                bootstrapuser, bootstrappwd,
                "internal", "defaultpwd",
                "workflow", "defaultpwd");

        dbCreator.run(dburl, dbuser, dbpwd, newUserInfos);
    }
}
