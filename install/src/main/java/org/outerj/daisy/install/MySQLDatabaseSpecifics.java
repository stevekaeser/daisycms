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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.*;

public class MySQLDatabaseSpecifics implements DatabaseSpecifics {
    private final char statementSeparator = ';';
    private final boolean dropStatementSeparator = false;

    public String getCurrentDateTimeFunction() {
        return "NOW()";
    }

    public String[] getPreStatements() {
        return new String[] {"SET FOREIGN_KEY_CHECKS = 0;"};
    }

    public String[] getPostStatements() {
        return new String[] {"SET FOREIGN_KEY_CHECKS = 1;"};
    }

    public char getStatementSeparator() {
        return statementSeparator;
    }

    public boolean getDropStatementSeparator() {
        return dropStatementSeparator;
    }

    public Reader getSchemaScript() {
        InputStream schemaScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/mysql-daisy-schema.sql");
        return new InputStreamReader(schemaScriptIS);
    }

    public Reader getDataScript() {
        InputStream dataScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/mysql-daisy-data.sql");
        return new InputStreamReader(dataScriptIS);
    }

    public String getForeignKeyStatement(String table, String field, String otherTable, String otherField, String constraintName) {
        return "alter table " + table + " add constraint " + constraintName + " foreign key (" + field + ") references " + otherTable + " (" + otherField + ")";
    }

    public String getTestStatement() {
        return "select current_date";
    }

    public void checkForTransactions(Connection conn) throws SQLException, Exception {
        // check whether InnoDB tables are enabled
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            System.out.println("Checking whether InnoDB tables are enabled...");
            ResultSet rs = stmt.executeQuery("SHOW VARIABLES LIKE 'have_innodb'");
            rs.next();
            if (!(rs.getString("Value").equals("YES"))) {
                throw new Exception("Database server does not support transactions (InnoDB tables are disabled).");
            }
            System.out.println("InnoDB ok");
        } finally {
            if (stmt != null)
                stmt.close();
        }
    }

    public void clearDatabase(String dbUrl, String dbName, String dbUser, String dbPassword) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            stmt = conn.createStatement();
            System.out.print("Making database empty, dropping tables");
            stmt.execute("SET FOREIGN_KEY_CHECKS=0");
            ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                System.out.print(".");
                stmt.execute("drop table " + tableName);
            }
            System.out.println();
        } finally {
            if (stmt != null)
                try { stmt.execute("SET FOREIGN_KEY_CHECKS=1"); } catch (Exception e) { /* ignore */ }
            if (stmt != null)
                stmt.close();
            if (conn != null)
                conn.close();
        }
    }

}
