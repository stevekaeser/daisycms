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

import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.DriverManager;

public class PostgresqlDatabaseSpecifics implements DatabaseSpecifics {
    private final char statementSeparator = ';';
    private final boolean dropStatementSeparator = false;

    public String getCurrentDateTimeFunction() {
        return "localtimestamp(0)";
    }

    public String[] getPreStatements() {
        return new String[0];
    }

    public String[] getPostStatements() {
        return new String[0];
    }

    public char getStatementSeparator() {
        return statementSeparator;
    }

    public boolean getDropStatementSeparator() {
        return dropStatementSeparator;
    }

    public Reader getSchemaScript() {
        InputStream schemaScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/postgresql-daisy-schema.sql");
        return new InputStreamReader(schemaScriptIS);
    }

    public Reader getDataScript() {
        InputStream dataScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/postgresql-daisy-data.sql");
        return new InputStreamReader(dataScriptIS);
    }

    public void clearDatabase(String dbUrl, String dbName, String dbUser, String dbPassword) throws SQLException {
        dbUrl = dbUrl.replaceAll(dbName, "template1");
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            stmt = conn.createStatement();

            stmt.execute("DROP DATABASE " + dbName);
            stmt.close();
            stmt.execute("CREATE DATABASE " + dbName + " ENCODING = 'UTF-8'");
        } finally {
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
    }

    public String getForeignKeyStatement(String table, String field, String otherTable, String otherField, String constraintName) {
        return "alter table " + table + " add constraint " + constraintName + " foreign key (" + field + ") references " + otherTable + " (" + otherField + ")";
    }

    public String getTestStatement() {
        return "select current_date";
    }

    public void checkForTransactions(Connection conn) {
        // PostgreSQl supports transactions, so no further check is needed
    }

}
