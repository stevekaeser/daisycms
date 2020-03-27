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
package org.outerj.daisy.frontend;

import org.apache.cocoon.generation.Generator;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.avalon.framework.parameters.ParameterException;
import org.apache.xmlbeans.XmlObject;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.outerj.daisy.repository.Repository;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyPropagatedException;
import org.outerj.daisy.repository.query.QueryManager;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;

public class QueryGenerator implements Generator {
    private XMLConsumer consumer;
    private String query;
    private Map objectModel;

    public void setup(SourceResolver sourceResolver, Map objectModel, String source, Parameters parameters) throws ProcessingException, SAXException, IOException {
        try {
            query = parameters.getParameter("query");
        } catch (ParameterException e) {
            throw new ProcessingException(e);
        }
        this.objectModel = objectModel;
    }

    public void setConsumer(XMLConsumer xmlConsumer) {
        this.consumer = xmlConsumer;
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        XmlObject result;
        try {
            FrontEndContext frontEndContext = FrontEndContext.get(ObjectModelHelper.getRequest(objectModel));
            Repository repository = frontEndContext.getRepository();
            QueryManager queryManager = repository.getQueryManager();
            Locale locale = frontEndContext.getLocale();
            result = queryManager.performQuery(query, locale);
        } catch (Exception e) {
            handleException(e);
            return;
        }

        result.save(consumer, consumer);
    }

    private void handleException(Exception e) throws SAXException {
        consumer.startDocument();
        AttributesImpl attrs = new AttributesImpl();
        attrs.addAttribute("", "class", "class", "CDATA", "daisy-error");
        consumer.startElement("", "p", "p", attrs);
        insertString("Could not perform the following query:");
        insertBr();
        insertString(query);
        insertBr();
        insertString("Encountered the following errors:");
        insertBr();
        insertString(getExceptionMessage(e));
        Throwable cause = e.getCause();
        while (cause != null) {
            insertBr();
            insertString(getExceptionMessage(cause));
            cause = cause.getCause();
        }
        consumer.endElement("", "p", "p");
        consumer.endDocument();
    }

    private String getExceptionMessage(Throwable e) {
        if (e instanceof DaisyPropagatedException)
            return ((DaisyPropagatedException)e).getUserMessage();
        else
            return e.getMessage();
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
}
