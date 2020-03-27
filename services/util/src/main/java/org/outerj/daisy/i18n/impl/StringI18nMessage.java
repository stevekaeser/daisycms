/*
 * Copyright 2007 Outerthought bvba and Schaubroeck nv
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
package org.outerj.daisy.i18n.impl;

import org.outerj.daisy.i18n.I18nMessage;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class StringI18nMessage implements I18nMessage {
    private String value;

    public StringI18nMessage(String value) {
        this.value = value;
    }

    public String getText() {
        return value;
    }

    public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
        char[] ch = value.toCharArray();
        contentHandler.characters(ch, 0, ch.length);
    }

    public String toString() {
        return value;
    }
}
