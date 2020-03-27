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
package org.outerj.daisy.event;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.jms.Sender;

import javax.jms.*;
import javax.sql.DataSource;
import javax.annotation.PreDestroy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class EventDispatcherImpl implements EventDispatcher {
    private EventDispatchThread eventDispatchThread;
    private DataSource dataSource;
    private String jmsTopicName;
    private JmsClient jmsClient;
    private Sender topicSender;
    private final Log log = LogFactory.getLog(getClass());

    public EventDispatcherImpl(Configuration configuration, DataSource dataSource, JmsClient jmsClient) throws Exception {
        this.dataSource = dataSource;
        this.jmsClient = jmsClient;
        this.configure(configuration);
        this.initialize();
        this.start();
    }

    @PreDestroy
    public void destroy() throws Exception {
        this.stop();
    }

    private void configure(Configuration configuration) throws ConfigurationException {
        this.jmsTopicName = configuration.getChild("jmsTopic").getValue();
    }

    public void notifyNewEvents() {
        eventDispatchThread.notify();
    }

    private void initialize() throws Exception {
        topicSender = jmsClient.getSender(jmsTopicName);
    }

    private void start() throws Exception {
        eventDispatchThread = new EventDispatchThread();
        eventDispatchThread.setDaemon(true);
        eventDispatchThread.start();
    }

    private void stop() throws Exception {
        log.info("Waiting for event dispatcher thread to end.");
        eventDispatchThread.interrupt();
        try {
            eventDispatchThread.join();
        } catch (InterruptedException e) {
            // ignore
        }
        jmsClient.unregisterSender(topicSender);
    }

    private class EventDispatchThread extends Thread {
        public EventDispatchThread() {
            super("Daisy event dispatcher");
        }

        public synchronized void run() {
            try {
                while (true) {
                    Connection conn = null;
                    PreparedStatement stmt = null;
                    PreparedStatement messageStmt = null;
                    PreparedStatement removeEventStmt = null;
                    try {
                        conn = dataSource.getConnection();

                        stmt = conn.prepareStatement("select seqnr from events order by seqnr");
                        ResultSet rs = stmt.executeQuery();
                        List<Long> seqnrsToProcess = new ArrayList<Long>();

                        while (rs.next()) {
                            seqnrsToProcess.add(new Long(rs.getLong(1)));
                        }
                        stmt.close();

                        messageStmt = conn.prepareStatement("select message_type, message from events where seqnr = ?");
                        removeEventStmt = conn.prepareStatement("delete from events where seqnr = ?");

                        for (Long seqnrsToProces : seqnrsToProcess) {
                            // Check if we don't want to stop
                            if (Thread.interrupted())
                                return;
                            long seqnr = seqnrsToProces.longValue();

                            messageStmt.setLong(1, seqnr);
                            rs = messageStmt.executeQuery();
                            rs.next();
                            String messageType = rs.getString(1);
                            String message = rs.getString(2);
                            rs.close();

                            if (log.isDebugEnabled())
                                log.debug("Will forward message " + seqnr + " to JMS.");

                            Message jmsMessage = topicSender.createTextMessage(message);
                            jmsMessage.setStringProperty("type", messageType);

                            // Again check if we don't want to stop, in an attempt to avoid a forever-wait
                            // condition in ActiveMQ when trying to send a message while the VM is shutting down.
                            if (Thread.interrupted())
                                return;
                            topicSender.send(jmsMessage);

                            removeEventStmt.setLong(1, seqnr);
                            removeEventStmt.execute();
                        }
                    } catch (Throwable e) {
                        if (e instanceof InterruptedException) {
                            return;
                        } else {
                            log.error("Exception in event dispatcher.", e);
                        }
                    } finally {
                        closeStatement(stmt);
                        closeStatement(messageStmt);
                        closeStatement(removeEventStmt);
                        try {
                            if (conn != null)
                                conn.close();
                        } catch (Throwable e) {
                            log.error("Failed to close database connection.", e);
                        }
                    }
                    if (Thread.interrupted())
                        return;
                    wait(5000);
                }
            } catch (InterruptedException e) {
                // ignore
            } finally {
                log.info("Event dispatcher thread ended.");
            }
        }

        private void closeStatement(PreparedStatement stmt) {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Throwable e) {
                log.error("Failed to close JDBC statement.", e);
            }
        }
    }
}
