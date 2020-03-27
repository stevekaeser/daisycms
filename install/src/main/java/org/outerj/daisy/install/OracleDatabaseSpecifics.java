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
import java.sql.SQLException;
import java.sql.Connection;

public class OracleDatabaseSpecifics implements DatabaseSpecifics {
    private final char statementSeparator = '/';
    private final boolean dropStatementSeparator = true;

    public String getCurrentDateTimeFunction() {
        return "SysDate";
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
        InputStream schemaScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/oracle-daisy-schema.sql");
        return new InputStreamReader(schemaScriptIS);
    }

    public Reader getDataScript() {
        InputStream dataScriptIS = getClass().getResourceAsStream("/org/outerj/daisy/install/oracle-daisy-data.sql");
        return new InputStreamReader(dataScriptIS);
    }

    public void clearDatabase(String dbUrl, String dbName, String dbUser, String dbPassword) throws SQLException {
        // TODO
    }

    public String getForeignKeyStatement(String table, String field, String otherTable, String otherField, String constraintName) {
        return "alter table " + table + " add constraint " + constraintName + " foreign key (" + field + ") references " + otherTable + " (" + otherField + ")";
    }

    public String getTestStatement() {
        return "select 1 from dual";
    }

    public void checkForTransactions(Connection conn) {
        // Oracle supports transactions, so no further check is needed
    }

}