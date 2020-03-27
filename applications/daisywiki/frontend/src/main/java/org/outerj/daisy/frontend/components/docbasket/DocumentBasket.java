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
package org.outerj.daisy.frontend.components.docbasket;

import org.apache.excalibur.xml.sax.XMLizable;
import org.apache.cocoon.xml.AttributesImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.util.*;

public class DocumentBasket implements XMLizable {
    private LinkedHashSet<DocumentBasketEntry> entries = new LinkedHashSet<DocumentBasketEntry>();
    private long updateCount = 0;
    private static final int MAX_SIZE = 500;

    public synchronized List<DocumentBasketEntry> getEntries() {
        return new ArrayList<DocumentBasketEntry>(entries);
    }

    public synchronized void appendEntry(DocumentBasketEntry entry) {
        if (entries.size() >= MAX_SIZE)
            throw new RuntimeException("Your document basket has reached the maximal allowed size, " + MAX_SIZE + ". Please remove some documents before adding more.");

        entries.add(entry);

        updateCount++;
    }

    public synchronized void appendEntries(DocumentBasketEntry[] newEntries) {
        for (DocumentBasketEntry newEntry : newEntries) {
            appendEntry(newEntry);
        }
    }

    public synchronized void clear() {
        entries.clear();
        updateCount++;
    }

    public synchronized int size() {
        return entries.size();
    }

    public synchronized long getUpdateCount() {
        return updateCount;
    }

    public synchronized void setEntries(List<DocumentBasketEntry> entries) {
        if (entries.size() > MAX_SIZE)
            throw new RuntimeException("The document basket cannot contain more then " + MAX_SIZE + " documents.");
        this.entries.clear();
        this.entries.addAll(entries);
        updateCount++;
    }

    public synchronized void toSAX(ContentHandler contentHandler) throws SAXException {
        AttributesImpl attrs = new AttributesImpl();
        attrs.addCDATAAttribute("size", String.valueOf(entries.size()));
        attrs.addCDATAAttribute("updateCount", String.valueOf(updateCount));

        contentHandler.startElement("", "documentBasket", "documentBasket", attrs);

        for (DocumentBasketEntry entry : entries) {
            entry.toSAX(contentHandler);
        }

        contentHandler.endElement("", "documentBasket", "documentBasket");
    }
}
