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

import javax.jms.Message;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.MapMessage;

public interface Sender {
    void send(Message message) throws Exception;

    TextMessage createTextMessage(String message) throws JMSException, InterruptedException;

    MapMessage createMapMessage() throws JMSException, InterruptedException;

    void commit() throws JMSException;
}
