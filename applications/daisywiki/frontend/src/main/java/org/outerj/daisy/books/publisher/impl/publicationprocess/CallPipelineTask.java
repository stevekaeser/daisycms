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
package org.outerj.daisy.books.publisher.impl.publicationprocess;

import org.outerj.daisy.books.store.BookInstance;
import org.outerj.daisy.books.publisher.impl.BookInstanceLayout;
import org.outerj.daisy.xmlutil.ForwardingContentHandler;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;
import org.outerj.daisy.xmlutil.XmlSerializer;
import org.outerj.daisy.repository.LocaleHelper;
import org.xml.sax.InputSource;
import org.xml.sax.ContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.apache.cocoon.xml.AttributesImpl;

import javax.xml.parsers.SAXParser;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A book publication process task which reacts on callPipeline tags which
 * reference an XML document in a bookStorePath attribute, and apply a
 * Cocoon pipeline to that XML document.
 * See the documentation for more usage information.
 */
public class CallPipelineTask implements PublicationProcessTask {
    private final String input;
    private final String output;
    private final String outputPrefix;
    private static final String NS = "http://outerx.org/daisy/1.0#bookCallPipelineTask";

    public CallPipelineTask(Map attributes) throws Exception {
        this.input = PublicationProcessTaskUtil.getRequiredAttribute(attributes, "input", "callPipeline");
        this.output = PublicationProcessTaskUtil.getRequiredAttribute(attributes, "output", "callPipeline");

        if (attributes.containsKey("outputPrefix")) {
            outputPrefix = (String)attributes.get("outputPrefix");
        } else {
            outputPrefix = "after-call-pipeline/";
        }
    }

    public void run(PublicationContext context) throws Exception {
        context.getPublicationLog().info("Running Call Pipeline task.");
        BookInstance bookInstance = context.getBookInstance();
        String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
        String startXmlLocation = publicationOutputPath + input;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = bookInstance.getResource(startXmlLocation);
            os = bookInstance.getResourceOutputStream(publicationOutputPath + output);
            SAXParser parser = LocalSAXParserFactory.getSAXParserFactory().newSAXParser();
            XmlSerializer serializer = new XmlSerializer(os);
            CallPipelineHandler callPipelineHandler = new CallPipelineHandler(serializer, context);
            parser.getXMLReader().setContentHandler(callPipelineHandler);
            parser.getXMLReader().parse(new InputSource(is));
        } finally {
            if (is != null)
                try { is.close(); } catch (Exception e) { /* ignore */ }
            if (os != null)
                try { os.close(); } catch (Exception e) { /* ignore */ }
        }
    }

    class CallPipelineHandler extends ForwardingContentHandler {
        private PublicationContext context;
        private boolean insideCallPipeline = false;
        private String callPipelineOutput;

        public CallPipelineHandler(ContentHandler consumer, PublicationContext context) {
            super(consumer);
            this.context = context;
        }

        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (namespaceURI.equals(NS) && localName.equals("callPipeline")) {
                if (insideCallPipeline)
                    throw new SAXException("callPipelineTask: Nested callPipeline elements are not supported.");
                callPipelineOutput = null;

                // get params
                String bookStorePath = getRequiredAttribute(atts, "bookStorePath");
                String outputPrefix = atts.getValue("outputPrefix");
                if (outputPrefix == null)
                    outputPrefix = CallPipelineTask.this.outputPrefix;
                String outputExtension = atts.getValue("outputExtension");
                if (outputExtension == null)
                    outputExtension = "";
                String pipe = getRequiredAttribute(atts, "pipe");

                // do it
                InputStream inputStream = null;
                OutputStream outputStream = null;
                try {
                    BookInstance bookInstance = context.getBookInstance();

                    String publicationOutputPath = BookInstanceLayout.getPublicationOutputPath(context.getPublicationOutputName());
                    callPipelineOutput = publicationOutputPath + outputPrefix + PublicationProcessTaskUtil.getFileName(bookStorePath) + outputExtension;
                    outputStream = bookInstance.getResourceOutputStream(callPipelineOutput);

                    inputStream = bookInstance.getResource(bookStorePath);

                    Map<String, Object> params = new HashMap<String, Object>();
                    params.put("inputStream", inputStream);
                    params.put("bookInstanceName", bookInstance.getName());
                    params.put("bookInstance", bookInstance);
                    params.put("locale", context.getLocale());
                    params.put("pubProps", context.getProperties());
                    params.put("bookMetadata", context.getBookMetadata());
                    params.put("localeAsString", LocaleHelper.getString(context.getLocale()));
                    params.put("publicationTypeName", context.getPublicationTypeName());
                    params.put("publicationOutputName", context.getPublicationOutputName());

                    pipe = context.getDaisyCocoonPath() + "/books/publicationTypes/" + context.getPublicationTypeName() + "/" + pipe;
                    PublicationProcessTaskUtil.executePipeline(pipe, params, outputStream, context);
                } catch (Exception e) {
                    throw new SAXException("Error (preparing for) executing pipeline in callPipeline task.", e);
                } finally {
                    if (inputStream != null) try { inputStream.close(); } catch (Exception e) { /* ingore */ }
                    if (outputStream != null) try { outputStream.close(); } catch (Exception e) { /* ingore */ }
                }
                insideCallPipeline = true;
            } else {
                if (insideCallPipeline && callPipelineOutput != null)
                    atts = processAttributes(atts);
                super.startElement(namespaceURI, localName, qName, atts);
            }
        }

        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (namespaceURI.equals(NS) && localName.equals("callPipeline")) {
                insideCallPipeline = false;
                // drop
            } else {
                super.endElement(namespaceURI, localName, qName);
            }
        }

        private String getRequiredAttribute(Attributes atts, String attrName) throws SAXException {
            String value = atts.getValue(attrName);
            if (value == null || value.trim().length() == 0)
                throw new SAXException("Missing attribute on callPipeline: " + attrName);
            return value;
        }

        private Attributes processAttributes(Attributes atts) {
            AttributesImpl newAttrs = new AttributesImpl(atts);
            for (int i = 0; i < newAttrs.getLength(); i++) {
                String value = newAttrs.getValue(i);
                value = value.replaceAll("\\{callPipelineOutput}", callPipelineOutput);
                newAttrs.setValue(i, value);
            }
            return newAttrs;
        }
    }
}
