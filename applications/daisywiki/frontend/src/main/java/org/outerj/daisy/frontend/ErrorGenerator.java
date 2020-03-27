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
import org.apache.cocoon.util.location.LocationUtils;
import org.apache.cocoon.util.location.LocatableException;
import org.apache.cocoon.util.location.MultiLocatable;
import org.apache.cocoon.util.location.Location;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.xml.XMLConsumer;
import org.apache.cocoon.xml.AttributesImpl;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.xml.sax.SAXException;
import org.outerj.daisy.repository.clientimpl.infrastructure.DaisyPropagatedException;
import org.outerj.daisy.repository.clientimpl.infrastructure.MyStackTraceElement;
import org.outerj.daisy.repository.LocalizedException;
import org.outerj.daisy.publisher.GlobalPublisherException;

import java.io.IOException;
import java.util.Map;
import java.util.Locale;
import java.util.List;

public class ErrorGenerator implements Generator {
    private Throwable throwable;
    private XMLConsumer consumer;
    private String layoutType;
    private Locale locale;
    private FrontEndContext frontEndContext;

    public void setup(SourceResolver sourceResolver, Map objectModel, String s, Parameters parameters) throws ProcessingException, SAXException, IOException {
        throwable = ObjectModelHelper.getThrowable(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        frontEndContext = FrontEndContext.get(request);

        if ("true".equals(request.getAttribute("smallErrorPage")))
            layoutType = "plain";
        else
            layoutType = null;

        locale = frontEndContext.isLocaleSet() ? frontEndContext.getLocale() : Locale.getDefault();

        if (throwable == null)
            throw new ProcessingException("No throwable found in object model.");
    }

    public void generate() throws IOException, SAXException, ProcessingException {
        consumer.startDocument();
        consumer.startElement("", "page", "page", new AttributesImpl());

        frontEndContext.getPageContext(layoutType).toSAX(consumer);

        consumer.startElement("", "error", "error", new AttributesImpl());

        //
        // Publisher stacktrace
        //
        generatePublisherStacktrace();

        //
        // Cocoon stacktrace
        //
        generateCocoonStacktrace();

        //
        // Java stacktrace
        //
        consumer.startElement("", "exceptionChain", "exceptionChain", new AttributesImpl());

        while (throwable != null) {
            generateThrowable(throwable);
            throwable = ExceptionUtils.getCause(throwable);
        }

        consumer.endElement("", "exceptionChain", "exceptionChain");
        consumer.endElement("", "error", "error");
        consumer.endElement("", "page", "page");
        consumer.endDocument();
    }

    private void generateThrowable(Throwable throwable) throws SAXException {
        if (throwable instanceof DaisyPropagatedException) {
            DaisyPropagatedException dpe = (DaisyPropagatedException)throwable;
            AttributesImpl attrs = new AttributesImpl();
            attrs.addCDATAAttribute("message", String.valueOf(dpe.getUserMessage()));
            attrs.addCDATAAttribute("class", dpe.getRemoteClassName());
            consumer.startElement("", "throwable", "throwable", attrs);

            AttributesImpl stackTraceAttrs = new AttributesImpl();
            stackTraceAttrs.addCDATAAttribute("remote", "true");
            consumer.startElement("", "stackTrace", "stackTrace", stackTraceAttrs);
            MyStackTraceElement[] stacktrace = dpe.getRemoteStackTrace();
            for (MyStackTraceElement stacktraceElement : stacktrace) {
                AttributesImpl steAttrs = new AttributesImpl();
                if (stacktraceElement.isNativeMethod())
                    steAttrs.addCDATAAttribute("nativeMethod", String.valueOf(stacktraceElement.isNativeMethod()));
                steAttrs.addCDATAAttribute("className", stacktraceElement.getClassName());
                steAttrs.addCDATAAttribute("fileName", stacktraceElement.getFileName());
                steAttrs.addCDATAAttribute("lineNumber", String.valueOf(stacktraceElement.getLineNumber()));
                steAttrs.addCDATAAttribute("methodName", String.valueOf(stacktraceElement.getMethodName()));
                consumer.startElement("", "stackTraceElement", "stackTraceElement", steAttrs);
                consumer.endElement("", "stackTraceElement", "stackTraceElement");
            }
            consumer.endElement("", "stackTrace", "stackTrace");

            consumer.endElement("", "throwable", "throwable");
        } else {
            String message;
            if (throwable instanceof LocalizedException)
                message = ((LocalizedException)throwable).getMessage(locale);
            else if (throwable instanceof LocatableException)
                message = ((LocatableException)throwable).getRawMessage();
            else if (throwable instanceof GlobalPublisherException)
                message = ((GlobalPublisherException)throwable).getRawMessage();
            else
                message = throwable.getMessage();
            
            if (message == null)
                message = "";

            AttributesImpl attrs = new AttributesImpl();
            attrs.addCDATAAttribute("message", message);
            attrs.addCDATAAttribute("class", throwable.getClass().getName());
            consumer.startElement("", "throwable", "throwable", attrs);

            consumer.startElement("", "stackTrace", "stackTrace", new AttributesImpl());
            StackTraceElement[] stacktrace = throwable.getStackTrace();
            for (StackTraceElement stacktraceElement : stacktrace) {
                AttributesImpl steAttrs = new AttributesImpl();
                if (stacktraceElement.isNativeMethod())
                    steAttrs.addCDATAAttribute("nativeMethod", String.valueOf(stacktraceElement.isNativeMethod()));
                steAttrs.addCDATAAttribute("className", stacktraceElement.getClassName());
                steAttrs.addCDATAAttribute("fileName", stacktraceElement.getFileName());
                steAttrs.addCDATAAttribute("lineNumber", String.valueOf(stacktraceElement.getLineNumber()));
                steAttrs.addCDATAAttribute("methodName", String.valueOf(stacktraceElement.getMethodName()));
                consumer.startElement("", "stackTraceElement", "stackTraceElement", steAttrs);
                consumer.endElement("", "stackTraceElement", "stackTraceElement");
            }
            consumer.endElement("", "stackTrace", "stackTrace");

            consumer.endElement("", "throwable", "throwable");
        }

    }

    private void generateCocoonStacktrace() throws SAXException {
        // Cocoon stacktrace: dump all located exceptions in the exception stack
        AttributesImpl attr = new AttributesImpl();
        consumer.startElement("", "cocoonStackTrace", "cocoonStackTrace", attr);
        Throwable current = throwable;
        while (current != null) {
            Location loc = LocationUtils.getLocation(current);
            if (LocationUtils.isKnown(loc)) {
                // One or more locations: dump it
                consumer.startElement("", "exception", "exception", attr);

                String message = current instanceof LocatableException ? ((LocatableException)current).getRawMessage() : current.getMessage();
                consumer.startElement("", "message", "message", new AttributesImpl());
                consumer.characters(message.toCharArray(), 0, message.length());
                consumer.endElement("", "message", "message");

                attr.clear();
                consumer.startElement("", "locations", "locations", attr);
                dumpLocation(loc, attr);

                if (current instanceof MultiLocatable) {
                    List locations = ((MultiLocatable)current).getLocations();
                    for (int i = 1; i < locations.size(); i++) { // start at 1 because we already dumped the first one
                        attr.clear();
                        dumpLocation((Location)locations.get(i), attr);
                    }
                }
                consumer.endElement("", "locations", "locations");
                consumer.endElement("", "exception", "exception");
            }


            // Dump parent location
            current = ExceptionUtils.getCause(current);
        }

        consumer.endElement("", "cocoonStackTrace", "cocoonStackTrace");
    }

    // This method is based on the corresponding one from Cocoon's ExceptionGenerator
    private void dumpLocation(Location loc, AttributesImpl attr) throws SAXException {
        attr.addCDATAAttribute("uri", loc.getURI());
        attr.addCDATAAttribute("line", Integer.toString(loc.getLineNumber()));
        attr.addCDATAAttribute("column", Integer.toString(loc.getColumnNumber()));
        consumer.startElement("", "location", "location", attr);
        String description = loc.getDescription();
        if (description != null)
            consumer.characters(description.toCharArray(), 0, description.length());
        consumer.endElement("", "location", "location");
    }


    private void generatePublisherStacktrace() throws SAXException {
        AttributesImpl attr = new AttributesImpl();
        consumer.startElement("", "publisherStackTrace", "publisherStackTrace", attr);
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof GlobalPublisherException) {
                List<String> locationStack = ((GlobalPublisherException)current).getLocationStack();
                if (locationStack != null) {
                    consumer.startElement("", "locations", "locations", attr);

                    for (String location : locationStack) {
                        consumer.startElement("", "location", "location", attr);
                        consumer.characters(location.toCharArray(), 0, location.length());
                        consumer.endElement("", "location", "location");
                    }

                    consumer.endElement("", "locations", "locations");

                    break;
                }
            }
            current = ExceptionUtils.getCause(current);
        }

        consumer.endElement("", "publisherStackTrace", "publisherStackTrace");
    }

    public void setConsumer(XMLConsumer xmlConsumer) {
        this.consumer = xmlConsumer;
    }

}
