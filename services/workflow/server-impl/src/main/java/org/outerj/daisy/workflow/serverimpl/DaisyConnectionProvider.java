/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.workflow.serverimpl;

import org.hibernate.connection.ConnectionProvider;
import org.hibernate.HibernateException;

import javax.sql.DataSource;
import java.util.Properties;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * An implementation of Hibernate's ConnectionProvider that allows to
 * use Daisy's DataSource to provide JDBC connections to hibernate,
 * access to the DataSource is provided by means of a thread local variable.
 *
 * <p>An alternative would be to emply a lightweight JNDI implementaiton
 * (such as JBosss'es JNP), however for now this perfectly solves the
 * problem in a simple way.
 */
public class DaisyConnectionProvider implements ConnectionProvider {
    public static ThreadLocal<DataSource> DATASOURCE = new ThreadLocal<DataSource>();

    public DaisyConnectionProvider() {
    }

    public void configure(Properties properties) throws HibernateException {
    }

    public Connection getConnection() throws SQLException {
        return DATASOURCE.get().getConnection();
    }

    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    public void close() throws HibernateException {
    }

    public boolean supportsAggressiveRelease() {
        return false;
    }
}
