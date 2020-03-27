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
package org.outerj.daisy.emailnotifier.serverimpl;

import org.apache.xmlbeans.XmlObject;

import java.util.Locale;

public class DummyMailTemplate implements MailTemplate {
    private String eventType;
    private String eventDescription;

    public DummyMailTemplate(String eventType, XmlObject eventDescription) {
        this.eventType = eventType;
        this.eventDescription = "The formatting of a nice message for this event hasn't been implemented yet, so I'll just put an XML dump of the event here.\n\n" + eventDescription.toString();
    }

    public String getSubject(Locale locale) {
        return eventType;
    }

    public String getMessage(Locale locale) {
        return eventDescription;
    }
}
