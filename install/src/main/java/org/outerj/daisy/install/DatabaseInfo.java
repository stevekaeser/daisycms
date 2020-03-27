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

import java.util.HashMap;
import java.util.Map;

// TODO: convert to an enum?
public class DatabaseInfo {

    public static Map<String, DatabaseInfo> ALL_DATABASES;
    static {
        ALL_DATABASES = new HashMap<String, DatabaseInfo>();
        ALL_DATABASES.put("mysql5", new DatabaseInfo("MySQL-5.x",
                "com.mysql.jdbc.Driver",
                "mysql/mysql-connector-java/3.1.12/mysql-connector-java-3.1.12.jar",
                "jdbc:mysql://localhost/${xxdbname}?characterEncoding=UTF-8",
                "org.hibernate.dialect.MySQLInnoDBDialect",
                new GenericDatabaseValidator("mysql", 5, -1, -1)));
        ALL_DATABASES.put("mysql4", new DatabaseInfo("MySQL-4.1.x (requires version 4.1.7 or higher)",
                "com.mysql.jdbc.Driver",
                "mysql/mysql-connector-java/3.1.12/mysql-connector-java-3.1.12.jar",
                "jdbc:mysql://localhost/${xxdbname}?useServerPrepStmts=false&characterEncoding=UTF-8",
                "org.hibernate.dialect.MySQLInnoDBDialect",
                new GenericDatabaseValidator("mysql", 4, 1, 7)));
        ALL_DATABASES.put("postgresql74", new DatabaseInfo("PostgreSQL (7.4.x) -- do not use this, it doesn't work correctly",
                "org.postgresql.Driver",
                "postgresql/postgresql/7.4-216.jdbc3/postgresql-7.4-216.jdbc3.jar",
                "jdbc:postgresql://localhost/${xxdbname}",
                "org.hibernate.dialect.PostgreSQLDialect",
                new NoOpDatabaseValidator()));
        ALL_DATABASES.put("oracle10g", new DatabaseInfo("ORACLE (10g) -- do not use this, it doesn't work correctly",
                "oracle.jdbc.driver.OracleDriver",
                "oracle/ojdbc14/10.2.0.3.0/ojdbc14-10.2.0.3.0.jar",
                "jdbc:oracle:thin:@localhost:1521:ora10g",
                "org.hibernate.dialect.Oracle9Dialect",
                new NoOpDatabaseValidator()));
    }

    private String description;
    private String driverClass;
    private String driverPath;
    private String driverUrl;
    private String hibernateDialect;
    private DatabaseValidator validator;

    public DatabaseInfo(String description, String driverClass, String driverPath, String driverUrl,
            String hibernateDialect, DatabaseValidator validator) {
        this.description = description;
        this.driverClass = driverClass;
        this.driverPath = driverPath;
        this.driverUrl = driverUrl;
        this.hibernateDialect = hibernateDialect;
        this.validator = validator;
    }

    public String getDescription() {
        return description;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public String getDriverPath() {
        return driverPath;
    }

    public String getDriverUrl(String dbname) {
        return driverUrl.replaceAll("@dbname@", dbname);
    }

    public String getHibernateDialect() {
        return hibernateDialect;
    }

    public DatabaseValidator getValidator() {
        return validator;
    }
}

