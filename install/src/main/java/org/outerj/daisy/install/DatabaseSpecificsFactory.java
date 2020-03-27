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

import java.util.Map;
import java.util.HashMap;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

public class DatabaseSpecificsFactory {
    private static Map<String, DatabaseSpecifics> DATABASE_SPECIFICS_MAP;
    static {
        DATABASE_SPECIFICS_MAP = new HashMap<String, DatabaseSpecifics>();
        DATABASE_SPECIFICS_MAP.put("MySQL", new MySQLDatabaseSpecifics());
        DATABASE_SPECIFICS_MAP.put("PostgreSQL", new PostgresqlDatabaseSpecifics());
        DATABASE_SPECIFICS_MAP.put("Oracle", new OracleDatabaseSpecifics());
    }

    public static DatabaseSpecifics getDatabaseSpecifics(Connection conn) throws Exception {
        DatabaseMetaData dbMeataData = conn.getMetaData();
        String dbProductName = dbMeataData.getDatabaseProductName();
        DatabaseSpecifics dbSpecifics = DATABASE_SPECIFICS_MAP.get(dbProductName);
        if (dbSpecifics == null) {
            throw new Exception("Unsupported database: \"" + dbProductName + "\".");
        }
        return dbSpecifics;
    }
}
