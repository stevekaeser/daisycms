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
package org.outerj.daisy.publisher.serverimpl;

import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.SAXException;

public class DummyLexicalHandler implements LexicalHandler {
    public void endCDATA() throws SAXException {
    }

    public void endDTD() throws SAXException {
    }

    public void startCDATA() throws SAXException {
    }

    public void comment(char ch[], int start, int length) throws SAXException {
    }

    public void endEntity(String name) throws SAXException {
    }

    public void startEntity(String name) throws SAXException {
    }

    public void startDTD(String name, String publicId, String systemId) throws SAXException {
    }
}
