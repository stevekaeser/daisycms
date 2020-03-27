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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.outerj.daisy.xmlutil.SaxBuffer;
import org.outerj.daisy.i18n.I18nMessage;

public class I18nMessageImpl implements I18nMessage {
    private final SaxBuffer buffer;
    private final String text;

    public I18nMessageImpl(SaxBuffer buffer) {
        this.buffer = buffer;

        StringBuilder tmp = new StringBuilder();
        for (SaxBuffer.SaxBit bit: buffer.getBits()) {
            if (bit instanceof SaxBuffer.Characters) {
                SaxBuffer.Characters charbit = (SaxBuffer.Characters)bit;
                tmp.append(charbit.ch, 0, charbit.ch.length);
            }
        }
        this.text = tmp.toString();
    }

    public String getText() {
        return text;
    }

    public void generateSaxFragment(ContentHandler contentHandler) throws SAXException {
        buffer.toSAX(contentHandler);
    }

    public String toString() {
        return text;
    }
}
