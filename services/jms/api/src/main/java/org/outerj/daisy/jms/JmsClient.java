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
package org.outerj.daisy.jms;

import javax.jms.MessageListener;

/**
 * A JMS Client.
 *
 * <p>The purpose of the JMS client is:
 * <ul>
 *  <li>To have one location where the JMS connections are configured and established
 *      (instead of having each component do that of its own).
 *  <li>To automatically resume the JMS connection on failures, and hide these failures from
 *      the clients. Ideally, the users of this JmsClient should not be aware whether the
 *      connection with the JMS service is there or not. Message send calls should block
 *      until the connection is back, and message delivery should automatically restart.
 *  <li>To allow suspending the sending and delivery of JMS messages, useful while doing
 *      backups.
 * </ul>
 *
 */
public interface JmsClient {
    void registerDurableTopicListener(String topicName, String subscriptionName, MessageListener listener) throws Exception;

    void registerListener(String destinationName, MessageListener listener) throws Exception;

    void unregisterListener(MessageListener listener);

    Sender getSender(String destinationName, boolean transacted);

    Sender getSender(String destinationName);

    void unregisterSender(Sender sender);

    /**
     * Suspends all sending and delivering of JMS messages. This method should only return when
     * no more send or receives are in progress.
     *
     * @param msecs maximum time to wait for active sends/receives to end
     */
    boolean suspend(long msecs) throws InterruptedException;

    /**
     * Continues sending and delivering of JMS messages after it has previously been resumed.
     */
    void resume();
}
