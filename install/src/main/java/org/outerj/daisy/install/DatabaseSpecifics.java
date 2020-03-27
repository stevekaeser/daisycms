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
import java.sql.SQLException;
import java.sql.Connection;

public interface DatabaseSpecifics {
    /**
     * Get the database function that returns the current date and time.
     */
    String getCurrentDateTimeFunction();

    /**
     * Checks whether the database server supports transactions
     * (this is needed in case of MySQL to assure that InnoDB tables are activated).
     * @param conn
     */
    public void checkForTransactions(Connection conn) throws SQLException, Exception;

    /**
     * Returns statements that have to be executed before the script starts running.
     */
    public String[] getPreStatements();

    /**
     * Returns statements that have to be executed before the script starts running.
     */
    public String[] getPostStatements();

    public char getStatementSeparator();

    /**
     * Indicates whether the statement separator has to be dropped or not
     * (which is e.g. the case for Oracle).
     */
    public boolean getDropStatementSeparator();

    public Reader getSchemaScript();

    public Reader getDataScript();

    /**
     * <p>Makes the current database empty. This is used by the testcases to start
     * from a fresh state for each testcase.</p>
     *
     * <p>This method should make the database empty, as if were just created as new.</p>
     */
    public void clearDatabase(String dbUrl, String dbName, String dbUser, String dbPassword) throws SQLException;

    public String getForeignKeyStatement(String table, String field, String otherTable, String otherField, String constraintName);

    public String getTestStatement();
}
