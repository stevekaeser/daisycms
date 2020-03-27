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
package org.outerj.daisy.eventdumper;

import org.apache.xmlbeans.XmlObject;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.io.StringReader;

/**
 * Simple tool to dump all daisy JMS events to standard out. All parameters are hardcoded for now.
 */
public class EventDumper {
    public static void main(String[] args) throws Exception {
        new EventDumper().run();
    }

    public void run() throws Exception {
        String jmsUserName = "admin";
        String jmsPassword = "jmsadmin";

        Hashtable environment = new Hashtable();
        environment.put("java.naming.factory.initial", "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
        environment.put("java.naming.provider.url", "tcp://localhost:61616");
        Context context = new InitialContext(environment);

        TopicConnectionFactory jmsFactory = (TopicConnectionFactory) context.lookup("ConnectionFactory");
        Topic jmsTopic = (Topic) context.lookup("dynamicTopics/daisy");

        TopicConnection jmsConnection = jmsFactory.createTopicConnection(jmsUserName, jmsPassword);

        TopicSession jmsSession = jmsConnection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
        TopicSubscriber jmsSubscriber = jmsSession.createSubscriber(jmsTopic);
        jmsSubscriber.setMessageListener(new EventListener());

        jmsConnection.start();

        System.out.println("Started");

    }

    class EventListener implements MessageListener {
        public void onMessage(Message aMessage) {
            try {
                TextMessage message = (TextMessage)aMessage;
                String messageType = message.getStringProperty("type");
                System.out.println("================================= New Message ====================================");
                System.out.println("Received message of type: " + messageType);
                System.out.println("--------------------------------- Message Body -----------------------------------");
                // parse and print xml, this will ensure it is 'pretty printed'
                XmlObject xml = XmlObject.Factory.parse(new StringReader(message.getText()));
                System.out.println(xml);
            } catch (Exception e) {
                System.out.println("Exception processing message: " + e.toString());
                e.printStackTrace();
            }
        }
    }
}
