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
package org.outerj.daisy.publisher.serverimpl.docpreparation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.xmlbeans.XmlObject;
import org.outerj.daisy.publisher.serverimpl.DummyLexicalHandler;
import org.outerj.daisy.publisher.serverimpl.requestmodel.PublisherContext;
import org.outerj.daisy.publisher.serverimpl.variables.QueryVarResolverHandler;
import org.outerj.daisy.publisher.serverimpl.variables.Variables;
import org.outerj.daisy.repository.VersionMode;
import org.outerj.daisy.repository.query.EvaluationContext;
import org.outerj.daisy.repository.query.QueryManager;
import org.outerj.daisy.xmlutil.StripDocumentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This handler will replace all p tags with class "query" with the result
 * of executing the query contained in it. In case of an error a p tag
 * with class "daisy-error" will be generated, containing a description of
 * the error.
 */
public class QueriesProcessor implements ContentHandler {
    protected final QueryManager queryManager;
    protected final ContentHandler consumer;
    protected final ContentProcessor owner;
    protected final Locale locale;
    private final Variables variables;
    private boolean inQuery = false;
    private Attributes queryElementAttrs;
    private int queryElementNesting;
    private StringBuilder queryBuffer;
    private int nestedElementCounter = 0;

    public QueriesProcessor(ContentHandler consumer, ContentProcessor owner) {
        this.owner = owner;
        this.queryManager = owner.getPublisherContext().getRepository().getQueryManager();
        this.consumer = consumer;
        this.locale = owner.getPublisherContext().getLocale();
        PublisherContext publisherContext = owner.getPublisherContext();
        this.variables = publisherContext.getVariables();
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        nestedElementCounter++;
        if (localName.equals("pre") && namespaceURI.equals("")) {
            String clazz = atts.getValue("class");
            if (!inQuery && clazz != null && clazz.equals(getSensitiveClass())) {
                inQuery = true;
                queryElementNesting = nestedElementCounter;
                queryBuffer = new StringBuilder(400);
                queryElementAttrs = new AttributesImpl(atts);
            }
        }

        if (!inQuery) {
            consumer.startElement(namespaceURI, localName, qName, atts);
        }
    }

    protected String getSensitiveClass() {
        return "query";
    }

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (!inQuery) {
            consumer.endElement(namespaceURI, localName, qName);
        }

        if (inQuery && queryElementNesting == nestedElementCounter) {
            inQuery = false;
            String query = queryBuffer.toString();
            executeQuery(query, queryElementAttrs);
        }

        nestedElementCounter--;
    }

    protected void executeQuery(String query, Attributes attrs) throws SAXException {
        XmlObject result = null;
        try {
            EvaluationContext evaluationContext = new EvaluationContext();
            evaluationContext.setContextDocument(owner.getDocument(), owner.getVersion());
            result = queryManager.performQuery(query, null, getQueryOptions(), locale, evaluationContext);
        } catch (Exception e) {
            outputFailedQueryMessage(e, query);
        }

        // output query result
        if (result != null) {
            ContentHandler resultHandler = consumer;
            if (variables != null) {
                resultHandler = new QueryVarResolverHandler(consumer, variables);
            }
            result.save(new StripDocumentHandler(resultHandler), new DummyLexicalHandler());
        }
    }

    protected Map<String, String> getQueryOptions() {
        if (owner.getPublisherContext().getVersionMode() != null) {
            Map<String, String> queryOptions = new HashMap<String, String>(3);
            queryOptions.put("point_in_time", owner.getPublisherContext().getVersionMode().toString());
            return queryOptions;
        } else {
            return null;
        }
    }

    protected void outputFailedQueryMessage(Exception e, String query) throws SAXException {
        // Output a paragraph containing description of the error
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "class", "class", "CDATA", "daisy-error");
        consumer.startElement("", "p", "p", attrs);

        insertString("Could not perform the following embedded query:");
        insertBr();
        insertString(query);
        insertBr();
        insertString("Encountered the following errors:");
        insertBr();
        insertString(e.getMessage());
        Throwable cause = e.getCause();
        while (cause != null) {
            insertBr();
            insertString(cause.getMessage());
            cause = cause.getCause();
        }

        consumer.endElement("", "p", "p");
    }

    private void insertBr() throws SAXException {
        consumer.startElement("", "br", "br", new AttributesImpl());
        consumer.endElement("", "br", "br");
    }

    private void insertString(String message) throws SAXException {
        if (message == null)
            message = "null";
        consumer.characters(message.toCharArray(), 0, message.length());
    }

    public void endDocument() throws SAXException {
        if (!inQuery)
            consumer.endDocument();
    }

    public void startDocument() throws SAXException {
        if (!inQuery)
            consumer.startDocument();
    }

    public void characters(char ch[], int start, int length) throws SAXException {
        if (inQuery) {
            queryBuffer.append(ch, start, length);
        } else {
            consumer.characters(ch, start, length);
        }
    }

    public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        if (!inQuery)
            consumer.ignorableWhitespace(ch, start, length);
    }

    public void endPrefixMapping(String prefix) throws SAXException {
        if (!inQuery)
            consumer.endPrefixMapping(prefix);
    }

    public void skippedEntity(String name) throws SAXException {
        if (!inQuery)
            consumer.skippedEntity(name);
    }

    public void setDocumentLocator(Locator locator) {
        if (!inQuery)
            consumer.setDocumentLocator(locator);
    }

    public void processingInstruction(String target, String data) throws SAXException {
        if (!inQuery)
            consumer.processingInstruction(target, data);
    }

    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        if (!inQuery)
            consumer.startPrefixMapping(prefix, uri);
    }

}
