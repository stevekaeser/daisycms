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
package org.outerj.daisy.authentication.impl;

import org.outerj.daisy.authentication.spi.AuthenticationException;
import org.outerj.daisy.authentication.spi.*;
import org.outerj.daisy.repository.Credentials;
import org.outerj.daisy.repository.user.User;
import org.outerj.daisy.repository.user.UserManager;
import org.outerj.daisy.jdbcutil.JdbcHelper;
import org.outerj.daisy.plugin.PluginRegistry;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;

/**
 * Responsible for creating and registering the DaisyAuthenticationScheme with the UserAuthenticator.
 *
 */
public class DaisyAuthenticationFactory {
    private final Log log = LogFactory.getLog(getClass());
    private PluginRegistry pluginRegistry;
    private DataSource dataSource;
    private JdbcHelper jdbcHelper;
    private boolean enableCaching;
    private long maxCacheDuration;
    private int maxCacheSize;
    private AuthenticationScheme authScheme;
    private final static String AUTH_SCHEME_NAME = "daisy";

    public DaisyAuthenticationFactory(Configuration configuration, DataSource dataSource,
            PluginRegistry pluginRegistry) throws Exception {
        this.dataSource = dataSource;
        this.pluginRegistry = pluginRegistry;
        this.configure(configuration);
        this.initialize();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        Configuration cacheConf = configuration.getChild("cache");
        if (cacheConf.getAttributeAsBoolean("enabled")) {
            enableCaching = true;
            maxCacheSize = cacheConf.getAttributeAsInteger("maxCacheSize", 3000);
            maxCacheDuration = cacheConf.getAttributeAsLong("maxCacheDuration", 30 * 60 * 1000); // default: half an hour
        }
    }

    private void initialize() throws Exception {
        jdbcHelper = JdbcHelper.getInstance(dataSource, log);
        if (enableCaching) {
            authScheme = new CachingAuthenticationScheme(new DaisyAuthenticationScheme(), maxCacheDuration, maxCacheSize);
        } else {
            authScheme = new DaisyAuthenticationScheme();
        }
        pluginRegistry.addPlugin(AuthenticationScheme.class, AUTH_SCHEME_NAME, authScheme);
    }

    @PreDestroy
    public void destroy() {
        pluginRegistry.removePlugin(AuthenticationScheme.class, AUTH_SCHEME_NAME, authScheme);
    }

    class DaisyAuthenticationScheme implements AuthenticationScheme {

        public String getDescription() {
            return "Daisy built-in";
        }

        public void clearCaches() {
            // do nothing
        }

        public boolean check(Credentials credentials) throws AuthenticationException {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs;
            try {
                conn = dataSource.getConnection();
                stmt = conn.prepareStatement("select login, password, default_role, id from users where login = ?");
                stmt.setString(1, credentials.getLogin());
                rs = stmt.executeQuery();

                if (!rs.next())
                    return false;

                // To be sure the login string is exactly the same (e.g. SQL search might be case-insensitive).
                if (!credentials.getLogin().equals(rs.getString("login")))
                    return false;

                String password = rs.getString("password");

                if (password == null || !password.equals(hashPassword(credentials.getPassword())))
                    return false;

                return true;
            } catch (Exception e) {
                throw new AuthenticationException("Error trying to authenticate user with Daisy Authentication Scheme.", e);
            } finally {
                jdbcHelper.closeStatement(stmt);
                jdbcHelper.closeConnection(conn);
            }
        }

        public User createUser(Credentials crendentials, UserManager userManager) throws AuthenticationException {
            return null;
        }
    }

    private static String hashPassword(String password) {
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

    private static String toHexString(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    static char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
}
