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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Properties;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.io.File;
import java.sql.*;

public class DriverLoader {
    /**
     * Registeres a dynamically-loaded JDBC driver.
     *
     * @param classpath A comma-separated path of jar files.
     * @param driverClass the JDBC driver class name
     */
    public static void loadDatabaseDriver(String classpath, String driverClass) throws Exception {
        ArrayList urls = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(classpath, ",");
        while (tokenizer.hasMoreTokens()) {
            String filename = tokenizer.nextToken().trim();
            try
            {
                URL url = new File(filename).toURL();
                urls.add(url);
            }
            catch (MalformedURLException e)
            {
                throw new Exception("Invalid filename in driver path.", e);
            }
        }
        ClassLoader classLoader = new URLClassLoader((URL[])urls.toArray(new URL[]{}), Thread.currentThread().getContextClassLoader());
        Driver driver = (Driver) Class.forName(driverClass, true, classLoader).newInstance();
        DriverManager.registerDriver(new DriverShim(driver));
    }

    /**
     * Work-around for the fact that DriverManager will only load Driver's loaded by
     * the system classloader.
     *
     * More information can be found on:
     * http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
     */
    private static final class DriverShim implements Driver
    {
        private Driver driver;

        DriverShim(Driver d)
        {
            this.driver = d;
        }

        public final boolean acceptsURL(final String u) throws SQLException
        {
            return this.driver.acceptsURL(u);
        }

        public final Connection connect(final String u, final Properties p) throws SQLException
        {
            return this.driver.connect(u, p);
        }

        public final int getMajorVersion()
        {
            return this.driver.getMajorVersion();
        }

        public final int getMinorVersion()
        {
            return this.driver.getMinorVersion();
        }

        public final DriverPropertyInfo[] getPropertyInfo(final String u, final Properties p) throws SQLException
        {
            return this.driver.getPropertyInfo(u, p);
        }

        public final boolean jdbcCompliant()
        {
            return this.driver.jdbcCompliant();
        }
    }
}
