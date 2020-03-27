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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.jdbcutil.DriverLoader;

public class DatabaseParams {
    private Log log = LogFactory.getLog(DatabaseParams.class);

    private final String dbName;
    private final String url;
    private final String user;
    private final String password;
    private final String driverClassName;
    private final String driverClassPath;
    private final String hibernateDialect;
    private final DatabaseValidator validator;
    private Boolean databaseEmpty = null;

    public DatabaseParams(String dbName, String url, String user, String password, String driverClassName, String driverClassPath,
            String hibernateDialect) {
        this(dbName, url, user, password, driverClassName, driverClassPath, hibernateDialect, null);
    }

    public DatabaseParams(String dbName, String url, String user, String password, String driverClassName, String driverClassPath,
            String hibernateDialect, DatabaseValidator validator) {
        this.dbName = dbName;
        this.url = url;
        this.user = user;
        this.password = password;
        this.driverClassName = driverClassName;
        this.driverClassPath = driverClassPath;
        this.hibernateDialect = hibernateDialect;
        this.validator = validator;
    }
    
    public String getDbName() {
        return dbName;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public String getDriverClassPath() {
        return driverClassPath;
    }

    public void loadDriver() throws Exception {
        DriverLoader.loadDatabaseDriver(PropertyResolver.resolveProperties(driverClassPath), driverClassName);
    }

    public String getHibernateDialect() {
        return hibernateDialect;
    }
    
    /**
     * Tries to find out if the current dbParams object can be used for runtime daisy operations.
     * @param warnings
     * @param minorProblems
     * @param majorProblems
     */
    public void checkDatabase(String humanDbName, List<String> warnings, List<String> minorProblems, List<String> majorProblems) throws Exception {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            if (validator != null) {
                validator.validate(conn, humanDbName, warnings, minorProblems, majorProblems);
            }
            checkDatabaseEmpty(humanDbName, minorProblems);
        } catch (SQLException e) {
            log.error(e);
            majorProblems.add("There is a problem with " + humanDbName);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

    public void checkDatabaseEmpty(String humanDbName, List<String> minorProblems) throws Exception {
        if (!isDatabaseEmpty()) {
            minorProblems.add(humanDbName + " is not empty. (Existing data will be erased if you continue)");
        }
    }
    
    /**
     * Returns true if the database is empty
     * @param dbParams
     * @return
     * @throws Exception
     */
    public boolean isDatabaseEmpty() throws Exception {
        if (databaseEmpty == null) {
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(getUrl(), getUser(), getPassword());

                ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
                return !rs.next();
            } finally {
                if (conn != null)
                    conn.close();
            }
        }
        return databaseEmpty;
    }

    /**
     * Clears the database.
     * @param dbParams
     * @throws Exception
     */
    public void clearDatabase() throws Exception {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(getUrl(), getUser(), getPassword());
            DatabaseSpecifics dbSpecifics = DatabaseSpecificsFactory.getDatabaseSpecifics(conn);
            dbSpecifics.clearDatabase(url, dbName, user, password);
        } finally {
            if (conn != null)
                conn.close();
        }
    }

}
