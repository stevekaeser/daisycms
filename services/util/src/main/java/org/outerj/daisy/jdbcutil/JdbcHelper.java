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
package org.outerj.daisy.jdbcutil;

import java.lang.reflect.Constructor;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;

public abstract class JdbcHelper {
    private Log log;
    private static Map<String, Class> jdbcHelpers = new HashMap<String, Class>();

    static {
        jdbcHelpers.put("MySQL", MySqlJdbcHelper.class);
        jdbcHelpers.put("PostgreSQL", PostgresqlJdbcHelper.class);
        jdbcHelpers.put("Oracle", OracleJdbcHelper.class);
    }

    public static JdbcHelper getInstance(String databaseProductName, Log log) {
        Class clazz = jdbcHelpers.get(databaseProductName);
        if (clazz == null) {
            throw new RuntimeException("Unsupported database: " + databaseProductName);
        }

        try {
            Constructor constructor = clazz.getConstructor(Log.class);
            return (JdbcHelper) constructor.newInstance(log);
        } catch (Exception e) {
            throw new RuntimeException("Error creating JdbcHelper.", e);
        }
    }

    public static JdbcHelper getInstance(DataSource dataSource, Log log) {
        String databaseProductName = null;
        try {
            Connection conn = dataSource.getConnection();
            try {
                databaseProductName = conn.getMetaData().getDatabaseProductName();
            } finally {
                conn.close();
            }
        } catch (Exception e) {
            throw new RuntimeException("Problem determining database product name.", e);
        }
        return JdbcHelper.getInstance(databaseProductName, log);
    }


    private JdbcHelper(Log log) {
        this.log = log;
    }

    public void closeStatement(Statement stmt) {
        if (stmt == null)
            return;

        try {
            // note: calling close on an already closed statement has no effect, so we don't have to worry about that
            stmt.close();
        } catch (Exception e) {
            log.error("Error closing SQL statement.", e);
        }
    }

    public void closeConnection(Connection conn) {
        if (conn == null)
            return;

        try {
            if (!conn.getAutoCommit()) {
                conn.commit(); // in case a commit has somewhere been forgotten                
            }
        } catch (Throwable e) {
            log.error("Error while committing.", e);
        }
        
        try {
            if (!conn.getAutoCommit()) {                
                conn.setAutoCommit(true);
            }
        } catch (Throwable e) {
            log.error("Error setting connection to autocommit + committing.", e);
        }

        try {
            conn.close();
        } catch (Throwable e) {
            log.error("Error closing connection.", e);
        }
    }

    public void rollback(Connection conn) {
        try {
            if (conn != null)
                conn.rollback();
        } catch (Throwable e) {
            log.error("Error rolling back transaction.", e);
        }
    }

    public abstract void startTransaction(Connection conn) throws SQLException;

    public abstract String getSharedLockClause();

    /** Returns the name of a SQL function that gets the length of a string (in characters, not in bytes). */
    public String[] getStringLengthFunction() {
        return new String[] { "CHAR_LENGTH(", ")" };
    }

    public String[] getStringConcatFunction() {
        return new String[] { "CONCAT(", ")" };
    }

    public String getStringLeftFunction() {
        return "LEFT";
    }

    public String getStringRightFunction() {
        return "RIGHT";
    }

    public String getSubstringFunction() {
        return "SUBSTRING";
    }

    public String getLowerCaseFunction() {
        return "LOWER";
    }

    public String getUpperCaseFunction() {
        return "UPPER";
    }

    public String[] getExtractYearFunction() {
        return new String[] { "EXTRACT(YEAR FROM ", ")"};
    }

    /** 1 = January */
    public String[] getExtractMonthFunction() {
        return new String[] { "EXTRACT(MONTH FROM ", ")"};
    }

    public String[] getDayOfWeekFunction() {
        return new String[] {"DAYOFWEEK(", ")"};
    }

    public String[] getDayOfMonthFunction() {
        return new String[] {"DAYOFMONTH(", ")"};
    }

    public String[] getDayOfYearFunction() {
        return new String[] {"DAYOFYEAR(", ")"};
    }

    /** First week = week with a sunday. Mysql mode 6
     *  http://dev.mysql.com/doc/refman/5.0/en/date-and-time-functions.html#function_week
     */
    public String[] getWeekFunction() {
        return new String[] { "WEEK(", ", 6)" };
    }
    
    public void setNullableIdField(PreparedStatement stmt, int column, long idValue) throws SQLException {
        if (idValue == -1) {
            stmt.setNull(column, Types.BIGINT);
        } else {
            stmt.setLong(column, idValue);
        }
    }

    public long getNullableIdField(ResultSet rs, String column) throws SQLException {
        long id = rs.getLong(column);
        if (id == 0)
            return -1;
        else
            return id;
    }

    public long getNullableIdField(ResultSet rs, int column) throws SQLException {
        long id = rs.getLong(column);
        if (id == 0)
            return -1;
        else
            return id;
    }

    static class MySqlJdbcHelper extends JdbcHelper {
        public MySqlJdbcHelper(Log log) {
            super(log);
        }

        public void startTransaction(Connection conn) throws SQLException {
            // Note: the default transaction level of MySQL is repeatable read and
            // offers consistent reads
            conn.setAutoCommit(false);
        }

        public String getSharedLockClause() {
            return "lock in share mode";
        }

        public String[] getWeekFunction() {
            return new String[] { "WEEK(", ", 6)" };
        }
    }

    static class PostgresqlJdbcHelper extends JdbcHelper {
        public PostgresqlJdbcHelper(Log log) {
            super(log);
        }

        public void startTransaction(Connection conn) throws SQLException {
            conn.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            conn.setAutoCommit(false);
        }

        public String getSharedLockClause() {
            return "for update";
        }
    }

    static class OracleJdbcHelper extends JdbcHelper {
        public OracleJdbcHelper(Log log) {
            super(log);
        }

        public void startTransaction(Connection conn) throws SQLException {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
        }

        public String getSharedLockClause() {
            return "for update";
        }
    }
}
