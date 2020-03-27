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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;

public class SqlDocumentIdCounter {
    private DataSource dataSource;
    private Log log;

    public SqlDocumentIdCounter(DataSource dataSource, Log log) {
        this.dataSource = dataSource;
        this.log = log;
    }

    public long getNextId(String namespace) throws Exception {
        synchronized (this) {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs;
            long maxid = 0;
            long namespaceId = 0;
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(true);

                stmt = conn.prepareStatement("select id from daisy_namespaces where name_ = ?");
                stmt.setString(1, namespace);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    log.error("Could not find a namespace with the name " + namespace);
                } else {
                    namespaceId = rs.getLong(1);
                }
                stmt.close();

                stmt = conn.prepareStatement("select maxid from document_sequence where ns_id = ?");
                stmt.setLong(1, namespaceId);
                rs = stmt.executeQuery();
                if (!rs.next()) {
                    String msg = "Namespace '" + namespace + "' is not managed by the repository. Could not generate an id. To solve this" + 
                    " try making the namespace managed";
                    log.error(msg);
                    throw new Exception(msg);
                } else {
                    maxid = rs.getLong(1);
                }

                maxid++;

                stmt.close();
                stmt = conn.prepareStatement("update document_sequence set maxid = ? where ns_id = ?");
                stmt.setLong(1, maxid);
                stmt.setLong(2, namespaceId);
                stmt.executeUpdate();
                
                // don't create a sequence if the sequence for the namespace doesn't exist                
            } finally {
                if (stmt != null) {                    
                    stmt.close();                   
                }
                if (conn != null) {
                    conn.close();                    
                }
            }
            return maxid;
        }
    }
}
