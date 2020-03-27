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
package org.outerj.daisy.emailer.serverimpl;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.emailer.Emailer;
import org.outerj.daisy.repository.spi.ExtensionProvider;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.plugin.PluginRegistry;

import javax.mail.Session;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.Date;
import java.sql.*;

public class CommonEmailer implements Emailer {
    private PluginRegistry pluginRegistry;
    private ExtensionProvider extensionProvider = new MyExtensionProvider();
    private String protocol;
    private String smtpHost;
    private String smtpPort;
    private String smtpLocalhost;
    private boolean authenticate = false;
    private boolean startTls = false;
    private String userName;
    private String password;
    private String emailCharSet;
    private String defaultFrom;
    private int emailThreadInterval;
    private int maxTryCount;
    private int retryInterval;
    private long maxAge;
    private Session session;
    private DataSource dataSource;
    private Thread emailerThread;
    private boolean debugJavamail;
    private Log log = LogFactory.getLog(getClass());
    private static final String EXTENSION_NAME = "Emailer";

    public CommonEmailer(Configuration configuration, DataSource dataSource,
            PluginRegistry pluginRegistry) throws Exception {
        this.dataSource = dataSource;
        this.pluginRegistry = pluginRegistry;
        configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        this.stop();
        pluginRegistry.removePlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        smtpHost = configuration.getChild("smtpHost").getValue();
        smtpLocalhost = configuration.getChild("smtpLocalhost").getValue(null);
        smtpPort = configuration.getChild("smtpPort").getValue(null);

        protocol = configuration.getChild("useSSL").getValueAsBoolean(false) ? "smtps" : "smtp";
        if (configuration.getChild("authentication", false) != null) {
            authenticate = true;
            userName = configuration.getChild("authentication").getAttribute("username");
            password = configuration.getChild("authentication").getAttribute("password");
            startTls = configuration.getChild("startTLS").getValueAsBoolean(false);
        }

        emailCharSet = configuration.getChild("emailCharSet").getValue(null);
        defaultFrom = configuration.getChild("fromAddress").getValue();

        emailThreadInterval = configuration.getChild("emailThreadInterval").getValueAsInteger();
        maxTryCount = configuration.getChild("maxTryCount").getValueAsInteger();
        retryInterval = configuration.getChild("retryInterval").getValueAsInteger();
        debugJavamail = configuration.getChild("javaMailDebug").getValueAsBoolean(false);
        maxAge = configuration.getChild("maxAge").getValueAsLong(7) * 24 * 60 * 60 * 1000;
    }

    private void initialize() throws Exception {
        Properties props = new Properties();
        props.put("mail." + protocol + ".host", smtpHost);
        if (smtpLocalhost != null)
            props.put("mail." + protocol + ".localhost", smtpLocalhost);
        if (smtpPort != null)
            props.put("mail." + protocol + ".port", smtpPort);
        if (authenticate) {
            props.put("mail." + protocol + ".auth", "true");
            props.put("mail." + protocol + ".starttls.enable", String.valueOf(startTls));
        }

        session = Session.getInstance(props);
        if (debugJavamail)
            session.setDebug(true);

        pluginRegistry.addPlugin(ExtensionProvider.class, EXTENSION_NAME, extensionProvider);
    }

    private void start() throws Exception {
        emailerThread = new Thread(new EmailerThread(), "Daisy Emailer");
        emailerThread.start();
    }

    private void stop() throws Exception {
        log.info("Waiting for emailer thread to end.");
        emailerThread.interrupt();
        try {
            emailerThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private class MyExtensionProvider implements ExtensionProvider {
        public Object createExtension(Repository repository) {
            return new LocalEmailer(repository, CommonEmailer.this);
        }
    }

    private void sendEmail(String from, String to, String subject, String messageText) throws Exception {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, true));
        msg.setSubject(subject);
        if (emailCharSet != null)
            msg.setText(messageText, emailCharSet);
        else
            msg.setText(messageText);
        msg.setSentDate(new Date());

        Transport transport = session.getTransport(protocol);
        try {
            if (authenticate) {
                transport.connect(smtpHost, userName, password);
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, msg.getAllRecipients());
        } finally {
            transport.close();
        }
    }

    public void send(String to, String subject, String messageText) {
        if (to == null)
            throw new NullPointerException("To address should not be null");
        if (subject == null)
            throw new NullPointerException("Subject should not be null");
        if (messageText == null)
            throw new NullPointerException("Message text should not be null");

        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement("insert into email_queue(from_address,to_address,subject,message,retry_count,created) values(?,?,?,?,?,?)");
            stmt.setNull(1, Types.VARCHAR);
            stmt.setString(2, to);
            stmt.setString(3, subject);
            stmt.setString(4, messageText);
            stmt.setInt(5, 0);
            stmt.setTimestamp(6, new java.sql.Timestamp(System.currentTimeMillis()));
            stmt.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create email db record.", e);
        } finally {
            closeStatement(stmt);
            closeConnection(conn);
        }
    }

    private void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (Exception e) {
                log.error("Error closing connection.", e);
            }
        }
    }

    private void closeStatement(PreparedStatement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                log.error("Error closing prepared statement.", e);
            }
        }
    }

    class EmailerThread implements Runnable {
        public void run() {
            try {
                long lastInvocationTime = System.currentTimeMillis();
                while (true) {
                    try {
                        lastInvocationTime = System.currentTimeMillis();

                        Connection conn = null;
                        PreparedStatement stmt = null;
                        PreparedStatement stmtUpdate = null;
                        PreparedStatement stmtDelete = null;
                        try {
                            conn = dataSource.getConnection();
                            stmt = conn.prepareStatement("select id,from_address,to_address,subject,message,retry_count,created from email_queue where retry_count < ? and (last_try_time is null or last_try_time < ?) order by created");
                            stmt.setLong(1, maxTryCount);
                            stmt.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis() - (retryInterval * 60000)));
                            ResultSet rs = stmt.executeQuery();

                            while (rs.next()) {
                                String from = rs.getString("from_address");
                                if (from == null)
                                    from = defaultFrom;
                                String to = rs.getString("to_address");
                                String subject = rs.getString("subject");
                                String message = rs.getString("message");
                                int retryCount = rs.getInt("retry_count");
                                long id = rs.getLong("id");
                                boolean success = false;

                                try {
                                    sendEmail(from, to, subject, message);
                                    success = true;
                                } catch (Throwable e) {
                                    // update DB record
                                    if (stmtUpdate == null)
                                        stmtUpdate = conn.prepareStatement("update email_queue set retry_count = ?, last_try_time = ?, error = ? where id = ?");
                                    stmtUpdate.setInt(1, retryCount + 1);
                                    stmtUpdate.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                                    stmtUpdate.setString(3, e.toString());
                                    stmtUpdate.setLong(4, id);
                                    stmtUpdate.execute();
                                }

                                if (success) {
                                    if (stmtDelete == null)
                                        stmtDelete = conn.prepareStatement("delete from email_queue where id = ?");
                                    stmtDelete.setLong(1, id);
                                    stmtDelete.execute();
                                }
                            }

                            stmt.close();

                            // cleanup expired messages
                            stmt = conn.prepareStatement("delete from email_queue where retry_count >= ? and last_try_time < ?");
                            stmt.setLong(1, maxTryCount);
                            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis() - maxAge));
                            int messagesDeleted = stmt.executeUpdate();
                            if (messagesDeleted > 0)
                                log.warn("Removed " + messagesDeleted + " expired unsent messages from the email queue.");
                        } catch (SQLException e) {
                            throw new RuntimeException("Database-related problem in emailer-thread.", e);
                        } finally {
                            closeStatement(stmt);
                            closeStatement(stmtUpdate);
                            closeStatement(stmtDelete);
                            closeConnection(conn);
                        }
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException)
                            return;
                        else
                            log.error("Error in the emailer thread.", e);
                    }

                    if (Thread.interrupted())
                        return;
                    
                    // sleeping is performed after the try-catch block, so that in case of an exception
                    // we also sleep (the remaining exceptions will probably be problems connecting
                    // to the database, in which case we better wait a bit before trying again)
                    long sleepTime = emailThreadInterval - (System.currentTimeMillis() - lastInvocationTime);
                    if (sleepTime > 0) {
                        Thread.sleep(sleepTime);
                    }
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                log.info("Emailer thread ended.");
            }
        }
    }
}
