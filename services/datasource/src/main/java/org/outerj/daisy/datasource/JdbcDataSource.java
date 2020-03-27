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
package org.outerj.daisy.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.management.*;
import javax.annotation.PreDestroy;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.dbcp.BasicDataSource;

/**
 * A DataSource implementation based on Jakarta Commons DBCP.
 *
 * <p>Originally based on the JdbcDataSource from the Apache James projects.
 *
 */
public class JdbcDataSource extends AbstractLogEnabled implements javax.sql.DataSource, JdbcDataSourceMBean {

    private BasicDataSource source = null;
    private MBeanServer mbeanServer;
    private ObjectName mbeanName = new ObjectName("Daisy:name=JdbcDataSource");

    public JdbcDataSource(Configuration configuration, MBeanServer mbeanServer) throws Exception {
        this.mbeanServer = mbeanServer;
        this.configure(configuration);
        this.initialize();
    }

    @PreDestroy
    public void destroy() {
        this.dispose();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        //Configure the DBCP
        try {
            source = new BasicDataSource();

            String url = configuration.getChild("url").getValue(null);
            source.setUrl(url);
            String username = configuration.getChild("username").getValue(null);
            source.setUsername(username);
            String password = configuration.getChild("password").getValue(null);
            source.setPassword(password);

            int maxActive = configuration.getChild("maxActive").getValueAsInteger(8);
            int maxIdle = configuration.getChild("maxIdle").getValueAsInteger(8);
            int minIdle = configuration.getChild("minIdle").getValueAsInteger(0);
            int maxWait = configuration.getChild("maxWait").getValueAsInteger(5000);

            source.setMaxActive(maxActive);
            source.setMaxIdle(maxIdle);
            source.setMinIdle(minIdle);
            source.setMaxWait(maxWait);

            Configuration propertyConf[] = configuration.getChild("connectionProperties").getChildren("property");
            for (int i = 0; i < propertyConf.length; i++) {
                source.addConnectionProperty(propertyConf[i].getName(), propertyConf[i].getValue());
            }

            // DBCP uses a PrintWriter approach to logging.  This
            // Writer class will bridge between DBCP and Avalon
            // Logging. Unfortunately, DBCP 1.0 is clueless about the
            // concept of a log level.
            final java.io.Writer writer = new java.io.CharArrayWriter() {
                public void flush() {
                    // flush the stream to the log
                    if (JdbcDataSource.this.getLogger().isErrorEnabled()) {
                        JdbcDataSource.this.getLogger().error(toString());
                    }
                    reset();    // reset the contents for the next message
                }
            };

            source.setLogWriter(new PrintWriter(writer, true));

            // Get a connection and close it, just to test that it works
            source.getConnection().close();
        } catch (Exception e) {
            throw new ConfigurationException("Error configurable datasource", e);
        }
    }

    private void initialize() throws Exception {
        mbeanServer.registerMBean(this, mbeanName);
    }

    private void dispose() {
        try {
            source.close();
        } catch (Throwable e) {
            getLogger().error("Error disposing pool.", e);
        }
        try {
            mbeanServer.unregisterMBean(mbeanName);
        } catch (Exception e) {
            getLogger().error("Error unregistering MBean", e);
        }
    }

    public int getLoginTimeout() throws SQLException {
        return source.getLoginTimeout();
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        source.setLoginTimeout(seconds);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return source.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new RuntimeException("I won't let you overwrite my log writer.");
    }

    public Connection getConnection() throws SQLException {
        return source.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return source.getConnection(username, password);
    }

    public int getNumActive() {
        return source.getNumActive();
    }

    public int getNumIdle() {
        return source.getNumIdle();
    }

    public String getUrl() {
        return source.getUrl();
    }

    public String getUserName() {
        return source.getUsername();
    }

    public int getMinIdle() {
        return source.getMinIdle();
    }

    public int getMaxIdle() {
        return source.getMaxIdle();
    }

    public int getMaxActive() {
        return source.getMaxActive();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException ("Wrapping is not supported");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return false;  
    }
}
