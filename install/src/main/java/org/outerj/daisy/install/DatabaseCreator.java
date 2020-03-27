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

import java.sql.*;
import java.io.Reader;
import java.io.BufferedReader;
import java.security.MessageDigest;
import java.util.List;
import java.util.ArrayList;

public class DatabaseCreator {

    /**
     * Makes the current database empty. This is used by the testcases to start
     * from a fresh state for each testcase.
     *
     * <p>This method should make the database empty, as if were just created as new.
     */
    public void clearDatabase(String dburl, String dbuser, String dbpwd, String dbname) throws Exception {
        Connection conn = DriverManager.getConnection(dburl, dbuser, dbpwd);
        DatabaseSpecifics dbSpecifics = null;
        try {
            dbSpecifics = DatabaseSpecificsFactory.getDatabaseSpecifics(conn);
            dbSpecifics.clearDatabase(dburl, dbname, dbuser, dbpwd);
        } finally {
            conn.close();
        }
    }

    /**
     * Installs the database script, optionally first dropping the existing
     * database if it exists, and creating a bootstrap user (with Administrator role).
     *
     * <p>The script is supposed to create all the required tables, and create
     * the Administrator role with id 1. The user id 2 should be available.
     *
     * @param dburl
     * @param dbuser username for the database
     * @param dbpwd password for the database
     * @throws Exception
     */
    public void run(String dburl, String dbuser, String dbpwd, List<NewUserInfo> newUserInfos) throws Exception {

        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(dburl, dbuser, dbpwd);
            DatabaseSpecifics dbSpecifics = DatabaseSpecificsFactory.getDatabaseSpecifics(conn);

            // Some checks before creating the schema
            dbSpecifics.checkForTransactions(conn);
            checkDatabaseIsEmpty(conn);

            stmt = conn.createStatement();

            for (String preStmt : dbSpecifics.getPreStatements())
                stmt.execute(preStmt);

            execute(dbSpecifics.getSchemaScript(), conn, dbSpecifics);

            conn.setAutoCommit(false);
            execute(dbSpecifics.getDataScript(), conn, dbSpecifics);
            conn.commit();
            conn.setAutoCommit(true);

            stmt.execute(dbSpecifics.getForeignKeyStatement("roles", "last_modifier", "users", "id", "role_modifier_fk"));

            for (String postStmt : dbSpecifics.getPostStatements())
                stmt.execute(postStmt);

            for (NewUserInfo newUserInfo : newUserInfos) {
                createUser(conn, newUserInfo.id, newUserInfo.login,  newUserInfo.password, newUserInfo.defaultRoleId,
                        newUserInfo.roles, dbSpecifics);
            }
        } finally {
            if (stmt != null)
                try { stmt.close(); } catch (Exception e) { /* ignore */ }
            if (conn != null)
                conn.close();
        }
    }

    private void checkDatabaseIsEmpty(Connection conn) throws Exception {
        ResultSet rs = conn.getMetaData().getTables(null, null, null, null);
        if (rs.next()) {
            throw new Exception("The specified database is not empty.");
        }
    }

    private void createUser(Connection conn, long id, String login, String password, long defaultRoleId, long[] roles, DatabaseSpecifics dbSpecifics ) throws SQLException {
        PreparedStatement stmt = null;
        try {
            StringBuilder sqlInsert = new StringBuilder();
            sqlInsert.append("insert into users(id,login,password,default_role,first_name,last_name,email,updateable_by_user,confirmed,confirmkey,auth_scheme,last_modified,last_modifier,updatecount) ");
            String valueStmt = "values(?, ?, ?, ?, null, null, null, ?, ?, null, 'daisy', @datetime@, 1, 1)";
            valueStmt = valueStmt.replaceAll("@datetime@", dbSpecifics.getCurrentDateTimeFunction());
            sqlInsert.append(valueStmt);

            stmt = conn.prepareStatement( sqlInsert.toString() );
            stmt.setLong(1, id);
            stmt.setString(2, login);
            stmt.setString(3, hashPassword(password));
            stmt.setLong(4, defaultRoleId);
            stmt.setBoolean(5, true);
            stmt.setBoolean(6, true);
            stmt.execute();
            stmt.close();

            stmt = conn.prepareStatement("insert into user_roles(user_id,role_id) values(?, ?)");
            for (long role : roles) {
                stmt.setLong(1, id);
                stmt.setLong(2, role);
                stmt.execute();
            }
        } finally {
            if (stmt != null)
                try { stmt.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    private void execute(Reader script, Connection conn, DatabaseSpecifics dbSpecifics) throws Exception {
        BufferedReader reader = new BufferedReader(script);

        StringBuilder stmtBuffer = new StringBuilder();
        char statementSeparator = dbSpecifics.getStatementSeparator();
        boolean dropStatementSeparator = dbSpecifics.getDropStatementSeparator();

        String line;
        int lineno = 0;
        for (line = reader.readLine(); line != null; line = reader.readLine(), ++lineno) {
            line = line.trim();
            if (line.length() == 0)
                continue;
            if (line.charAt(0) == '#')
                continue;
            if (line.length() >= 2 && line.charAt(0) == '-' && line.charAt(1) == '-' )
                continue;
            if (!lastCharacterIsStatementSeparator(line, statementSeparator)) {
                stmtBuffer.append(" ").append(line).append(" ");
                continue;
            }
            if (!dropStatementSeparator) {
                stmtBuffer.append(" ").append(line).append(" ");
            }
            Statement stmt = conn.createStatement();
            String statement = "";
            try {
                statement = stmtBuffer.toString().trim();
                statement = statement.replaceAll("'current-date-and-time'", dbSpecifics.getCurrentDateTimeFunction());
                stmt.execute(statement);
                System.out.print( '.' );
            } catch (SQLException e) {
                System.out.println();
                System.out.println(e.getMessage());
                System.out.print( "Error in Line: " + lineno + ": " + statement );
                System.out.println();
            } finally {
                stmt.close();
            }
            stmtBuffer.setLength(0);
        }
        System.out.println();
    }

    private boolean lastCharacterIsStatementSeparator(String buffer, char statementSeparator) {
        for (int i = buffer.length() - 1; i >=0; i--) {
            char c = buffer.charAt(i);
            if (!Character.isWhitespace(c))
                return c == statementSeparator;
        }
        return false;
    }

    public static String hashPassword(String password) {
        if (password == null)
            return null;
        try {
            byte[] data = password.getBytes("UTF-8");
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(data);
            byte[] result = digest.digest();
            return toHexString(result);
        } catch (Exception e) {
            throw new RuntimeException("Problem calculating password hash.", e);
        }
    }

    public static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static List<NewUserInfo> createDefaultUsers(String bootstrapUserLogin, String bootstrapUserPassword,
            String internalUserLogin, String internalUserPassword,
            String workflowUserLogin, String workflowUserPassword) {

        List<NewUserInfo> newUserInfos = new ArrayList<NewUserInfo>();

        newUserInfos.add(new NewUserInfo(2, internalUserLogin, internalUserPassword, 1, new long[] {1}));
        newUserInfos.add(new NewUserInfo(3, bootstrapUserLogin, bootstrapUserPassword, 2, new long[] {1, 2}));
        newUserInfos.add(new NewUserInfo(4, workflowUserLogin, workflowUserPassword, 1, new long[] {1}));

        return newUserInfos;
    }

    public static class NewUserInfo {
        long id;
        String login;
        String password;
        long defaultRoleId;
        long[] roles;

        public NewUserInfo(long id, String login, String password, long defaultRoleId, long[] roles) {
            this.id = id;
            this.login = login;
            this.password = password;
            this.defaultRoleId = defaultRoleId;
            this.roles = roles;
        }
    }

}
