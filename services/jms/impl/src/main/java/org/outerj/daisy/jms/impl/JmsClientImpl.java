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
package org.outerj.daisy.jms.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.PreDestroy;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.activemq.broker.BrokerRegistry;
import org.apache.activemq.broker.BrokerService;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.thread.ThreadSafe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.outerj.daisy.configutil.ConfigurationWrapper;
import org.outerj.daisy.configutil.PropertyResolver;
import org.outerj.daisy.jms.JmsClient;
import org.outerj.daisy.jms.Sender;

// Note: this component is used by both the repository server and client
// In case of the wiki this is deployed in the avalon container, therefore the
// avalon interfaces are still supported here.

public class JmsClientImpl implements JmsClient, Configurable, Initializable, Disposable, ThreadSafe {
    private Properties contextProperties;
    private String jmsUserName;
    private String jmsPassword;
    private String connectionFactoryName;
    private String clientId;
    private Connection jmsConnection;
    private static final int CONN_RETRY_INTERVAL = 10000;
    private boolean stopping = false;
    private List<MyJmsMessageListener> consumers = new ArrayList<MyJmsMessageListener>();
    private final Object consumersLock = new Object();
    private List<SenderImpl> senders = new ArrayList<SenderImpl>();
    private final Object sendersLock = new Object();
    private Thread connectionEstablishThread;
    private final Object connectionEstablishThreadLock = new Object();
    private ReadWriteLock suspendLock = new ReentrantReadWriteLock();
    private String activeMqBrokerToShutDown;

    private final Log log = LogFactory.getLog(getClass());

    public JmsClientImpl() {
    }

    public JmsClientImpl(Configuration configuration) throws Exception {
        this.configure(configuration);
        this.initialize();
    }

    /**
     *
     * @param clientId the JMS client ID, should be unique for each client
     */
    public JmsClientImpl(Properties contextProperties, String userName, String password, String clientId,
                         String connectionFactoryName) throws Exception {
        this.contextProperties = contextProperties;
        this.jmsUserName = userName;
        this.jmsPassword = password;
        this.clientId = clientId;
        this.connectionFactoryName = connectionFactoryName;
        initialize();
    }

    public boolean suspend(long msecs) throws InterruptedException {
        return this.suspendLock.writeLock().tryLock(msecs, TimeUnit.MILLISECONDS);
    }

    public void resume() {
        this.suspendLock.writeLock().unlock();
    }

    public void configure(Configuration configuration) throws ConfigurationException {
        configuration = new ConfigurationWrapper(configuration);
        Configuration jmsConf = configuration.getChild("jmsConnection");
        Configuration[] propertiesConf = jmsConf.getChild("initialContext").getChildren("property");
        contextProperties = new Properties();
        for (int i = 0; i < propertiesConf.length; i++) {
            if (!contextProperties.containsKey(propertiesConf[i].getAttribute("name"))) {
                String value = PropertyResolver.resolveProperties(propertiesConf[i].getAttribute("value"));
                // special hack for ActiveMQ on Windows: broker config should be an URL,
                // but substitution of ${daisy.datadir} might contain backslashes, so
                // convert backslashes to slashes
                if (value.indexOf("brokerConfig=xbean:file:") != -1) {
                    value = value.replaceAll("\\\\", "/");
                }
                contextProperties.put(propertiesConf[i].getAttribute("name"), value);
            }
        }

        Configuration jmsCredentials = jmsConf.getChild("credentials", false);
        if (jmsCredentials != null) {
            jmsUserName = jmsCredentials.getAttribute("username");
            jmsPassword = jmsCredentials.getAttribute("password");
        }

        clientId = jmsConf.getChild("clientId").getValue();

        connectionFactoryName = jmsConf.getChild("connectionFactoryName").getValue();

        activeMqBrokerToShutDown = configuration.getChild("shutdownEmbeddedActiveMqBroker").getValue(null);
    }

    public void initialize() throws Exception {
        // Note: we don't let this component initialize (and thus don't let daisy start) if establishing
        // the JMS connections fails. Reason is to remind people that they need to start their JMS server,
        // or configure the connection properly.
        initializeJmsConnection(true);
    }

    protected void initializeJmsConnection(boolean failOnError) throws Exception {
        while (jmsConnection == null && !stopping) {
            if (Thread.interrupted())
                throw new InterruptedException();

            try {
                log.debug("Trying to establish JMS connection...");
                Context context = getContext();

                ConnectionFactory jmsFactory = (ConnectionFactory) context.lookup(connectionFactoryName);
                if (jmsUserName != null)
                    jmsConnection = jmsFactory.createConnection(jmsUserName, jmsPassword);
                else
                    jmsConnection = jmsFactory.createConnection();
                jmsConnection.setClientID(clientId);
                connectionUp();

                jmsConnection.setExceptionListener(new MyJmsExceptionListener());
                jmsConnection.start();
            } catch (Exception e) {
                if (failOnError)
                    throw e;
                Thread.sleep(CONN_RETRY_INTERVAL);
            }
        }
        log.info("JMS connection established.");
    }

    private class MyJmsExceptionListener implements ExceptionListener {
        private boolean gotError = false;

        public void onException(JMSException e) {
            if (stopping)
                return;

            synchronized (connectionEstablishThreadLock) {
                try {
                    jmsConnection.close();
                } catch (Throwable t) {
                    // ignore
                }

                if (gotError) {
                    log.error("Got another error on a JMS connection on which we got an error before.", e);
                } else if (connectionEstablishThread == null) {
                    gotError = true;
                    log.error("Error with the JMS connection. Will automatically try to re-establish connection every " + CONN_RETRY_INTERVAL + " ms.", e);
                    connectionDown();
                    jmsConnection = null;
                    connectionEstablishThread = new ConnectionEstablisherThread();
                    connectionEstablishThread.start();
                } else {
                    gotError = true;
                    log.error("Strange situation: got first error but there is already a connection establisher thread?", e);
                }
            }
        }
    }

    private class ConnectionEstablisherThread extends Thread {
        public ConnectionEstablisherThread() {
            super("DaisyJmsConnectionEstablisher");
            setDaemon(true);
        }

        public void run() {
            try {
                initializeJmsConnection(false);
            } catch (Exception e2) {
                log.error("Error trying to establish JMS topic connection, giving up.", e2);
            }

            // When stopping, don't take a lock since it would cause a deadlock
            if (!stopping) {
                synchronized (connectionEstablishThreadLock) {
                    end();
                }
            } else {
                end();
            }
        }

        private void end() {
            connectionEstablishThread = null;
            log.info("JMS connection re-establish thread ended.");
        }
    }

    private Context getContext() throws NamingException {
        ClassLoader current = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        try {
            return new InitialContext(contextProperties);
        } finally {
            Thread.currentThread().setContextClassLoader(current);
        }
    }

    @PreDestroy
    public void dispose() {
        stopping = true;

        synchronized (connectionEstablishThreadLock) {
            if (connectionEstablishThread != null) {
                log.info("Waiting for JMS connection re-establish thread to end");
                connectionEstablishThread.interrupt();
                try {
                    connectionEstablishThread.join();
                } catch (NullPointerException e) {
                    // can be ignored:
                    // connectionEstablishThread might have become null between the interrupt and join call
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        try {
            if (jmsConnection != null) {
                jmsConnection.stop();
                jmsConnection.close();
            }
        } catch (JMSException e) {
            log.error("Error closing JMS connection.", e);
        }

        if (activeMqBrokerToShutDown != null) {
            BrokerService service = BrokerRegistry.getInstance().lookup(activeMqBrokerToShutDown);
            if (service != null) {
                try {
                    service.stop();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                log.error("No ActiveMQ broker found to shut down with the name " + activeMqBrokerToShutDown);
            }
        }
    }

    private void connectionUp() {
        bringUp(consumers);
        bringUp(senders);
    }

    private void connectionDown() {
        bringDown(consumers);
        bringDown(senders);
    }

    private void bringUp(List<? extends Reconnectable> list) {
        for (Reconnectable reconnectable : list) {
            try {
                reconnectable.connectionUp();
            } catch (Throwable e) {

                log.error("Error 'upping' a JMS session. When you see this, it is recommended to restart the application.", e);
            }
        }
    }

    private void bringDown(List<? extends Reconnectable> list) {
        for (Reconnectable reconnectable : list) {
            try {
                reconnectable.connectionDown();
            } catch (Throwable e) {
                log.error("Error 'downing' a JMS session.", e);
            }
        }
    }

    public void registerDurableTopicListener(String topicName, String subscriptionName, MessageListener listener) throws Exception {
        MyJmsMessageListener theListener = new MyJmsMessageListener(topicName, subscriptionName, listener);
        theListener.connectionUp();
        synchronized (consumersLock) {
            consumers.add(theListener);
        }
    }

    public void registerListener(String destinationName, MessageListener listener) throws Exception {
        MyJmsMessageListener theListener = new MyJmsMessageListener(destinationName, null, listener);
        theListener.connectionUp();
        synchronized (consumersLock) {
            consumers.add(theListener);
        }
    }

    public void unregisterListener(MessageListener listener) {
        synchronized (consumersLock) {
            for (MyJmsMessageListener myListener : consumers) {
                if (myListener.getDelegate() == listener) {
                    consumers.remove(myListener);
                    myListener.dispose();
                    return;
                }
            }
        }
        throw new RuntimeException("The specified listener is currently not registered.");
    }

    public Sender getSender(String destinationName) {
        return getSender(destinationName, false);
    }

    public Sender getSender(String destinationName, boolean transacted) {
        SenderImpl sender = new SenderImpl(destinationName, transacted);
        try {
            sender.connectionUp();
        } catch (Exception e) {
            log.warn("Sender could not be initialized after initial retrieval, meaning the JMS connection is probably down.", e);
        }
        synchronized (sendersLock) {
            senders.add(sender);
        }
        return sender;
    }

    public void unregisterSender(Sender sender) {
        if (sender instanceof SenderImpl) {
            SenderImpl senderImpl = (SenderImpl)sender;
            synchronized (sendersLock) {
                senders.remove(senderImpl);
            }
            senderImpl.dispose();
        } else {
            throw new RuntimeException("Unexpected object: " + sender);
        }
    }

    interface Reconnectable {
        void connectionDown();

        void connectionUp() throws Exception;
    }

    class MyJmsMessageListener implements MessageListener, Reconnectable {
        private String destinationName;
        private String subscriptionName;
        private MessageListener delegate;
        private Session session;

        public MyJmsMessageListener(String destinationName, String subscriptionName, MessageListener delegate) {
            this.destinationName = destinationName;
            this.subscriptionName = subscriptionName;
            this.delegate = delegate;
        }

        public void onMessage(Message message) {
            try {
                suspendLock.readLock().lockInterruptibly();
            } catch (InterruptedException e) {
                // Note: when using AUTO_ACKNOWLEDGE, throwing a runtime exception will result
                // in immediate retry of delivery.
                throw new RuntimeException("Got InterruptedException while waiting for suspendLock.");
            }
            try {
                delegate.onMessage(message);
            } finally {
                suspendLock.readLock().unlock();
            }
        }

        public void connectionDown() {
        }

        public void connectionUp() throws Exception {
            Destination jmsDestination = (Destination) getContext().lookup(destinationName);
            session = jmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer;
            if (subscriptionName != null)
                consumer = session.createDurableSubscriber((Topic)jmsDestination, subscriptionName);
            else
                consumer = session.createConsumer(jmsDestination);
            consumer.setMessageListener(this);
        }

        public void dispose() {
            try {
                if (session != null)
                    session.close();
            } catch (Exception e) {
                log.error("Error closing JMS session.", e);
            }
        }

        public MessageListener getDelegate() {
            return delegate;
        }
    }

    class SenderImpl implements Sender, Reconnectable {
        private volatile boolean connectionUp = false;
        private Session session;
        private String destinationName;
        private MessageProducer messageProducer;
        private boolean transacted;

        public SenderImpl(String destinationName, boolean transacted) {
            this.destinationName = destinationName;
            this.transacted = transacted;
        }

        public void send(final Message message) throws Exception {
            executeWhenConnectionIsUp(new JMSAction() {
                public void run() throws JMSException {
                    messageProducer.send(message);
                }
            });
        }

        public TextMessage createTextMessage(final String text) throws JMSException, InterruptedException {
            final TextMessage[] message = new TextMessage[1];
            executeWhenConnectionIsUp(new JMSAction() {
                public void run() throws Exception {
                    message[0] = session.createTextMessage(text);
                }
            });
            return message[0];
        }

        public MapMessage createMapMessage() throws JMSException, InterruptedException {
            final MapMessage[] message = new MapMessage[1];
            executeWhenConnectionIsUp(new JMSAction() {
                public void run() throws Exception {
                    message[0] = session.createMapMessage();
                }
            });
            return message[0];
        }

        public synchronized void commit() throws JMSException {
            if (!connectionUp) {
                throw new RuntimeException("Cannot commit, JMS connection is down.");
            } else {
                session.commit();
            }
        }

        public synchronized void connectionDown() {
            connectionUp = false;
            session = null;
            messageProducer = null;
        }

        public synchronized void connectionUp() throws Exception {
            Destination jmsDestination = (Destination) getContext().lookup(destinationName);
            session = jmsConnection.createSession(transacted, Session.AUTO_ACKNOWLEDGE);
            messageProducer = session.createProducer(jmsDestination);
            connectionUp = true;
        }

        public synchronized void dispose() {
            try {
                if (session != null)
                    session.close();
            } catch (Exception e) {
                log.error("Error closing JMS session.", e);
            }
        }

        protected void executeWhenConnectionIsUp(JMSAction action) throws InterruptedException {
            stoppingLoop: while (!stopping) {
                // wait till connection is back up
                while (!connectionUp && !stopping) {
                    log.debug("JMS connection is down...");
                    Thread.sleep(CONN_RETRY_INTERVAL);
                }

                // connection is back, try to send message
                int i = 0;
                while (!stopping) {
                    try {
                        suspendLock.readLock().lockInterruptibly();
                    } catch (InterruptedException e) {
                        log.debug("Got interruptedexception while trying to get suspend lock.", e);
                        break stoppingLoop;
                    }
                    Exception failure = null;
                    try {
                        action.run();
                        return;
                    } catch (Exception e) {
                        if (e instanceof InterruptedException)
                            throw (InterruptedException)e;
                        failure = e;
                    } finally {
                        suspendLock.readLock().unlock();
                    }
                    if (failure != null) {
                        if (!connectionUp) {
                            // connection was just lost (again), go waiting till it is back up
                            break;
                        } else {
                            // action failed for another reason (maybe because connection is down but
                            // connectionUp flag is not yet changed), wait a little bit and try again, unless retry
                            // counter reach maximum
                            i++;
                            if (i >= 3) {
                                throw new RuntimeException("Failed to execute JMS action, giving up.", failure);
                            } else if (stopping) {
                                throw new InterruptedException("JMS client is shutting down.");
                            } else {
                                Thread.sleep(CONN_RETRY_INTERVAL);
                            }
                        }
                    }
                }
            }
            throw new InterruptedException("JMS client is shutting down.");
        }
    }

    interface JMSAction {
        public void run() throws Exception;
    }
}
