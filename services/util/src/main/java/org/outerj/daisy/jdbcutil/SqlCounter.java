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

import org.apache.commons.logging.Log;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SqlCounter {
    private DataSource dataSource;
    private Log log;
    private String tableName;

    public SqlCounter(String tableName, DataSource dataSource, Log log) {
        this.tableName = tableName;
        this.dataSource = dataSource;
        this.log = log;
    }

    public long getNextId() throws SQLException {
        synchronized (this) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs;
            long maxid = 0;
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(true);
                stmt = conn.prepareStatement("select maxid from " + tableName);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    log.info(tableName + " table did not contain a record, will start numbering from 1.");
                } else {
                    maxid = rs.getLong(1);
                }

                maxid++;

                stmt.close();
                stmt = conn.prepareStatement("update " + tableName + " set maxid = ?");
                stmt.setLong(1, maxid);
                int updatedRows = stmt.executeUpdate();

                if (updatedRows == 0) {
                    stmt.close();
                    stmt = conn.prepareStatement("insert into " + tableName + "(maxid) values(?)");
                    stmt.setLong(1, maxid);
                    stmt.execute();
                }
            } finally {
                if (stmt != null) {
                    try {
                        stmt.close();
                    } catch (Throwable e) {
                        log.error("Error closing prepared statement.", e);
                    }
                }
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (Throwable e) {
                        log.error("Error closing datasource connection.", e);
                    }
                }
            }
            return maxid;
        }
    }
}
