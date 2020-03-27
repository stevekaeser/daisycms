/*
 * Copyright 2004 Outerthought bvba and Schaubroeck nv
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.datatype.convertor.ConversionResult;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.cyberneko.html.parsers.SAXParser;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.util.Locale;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Cleans HTML to well-formed XML using NekoHTML.
 */
public class LiteralHtmlCleaningConvertor implements Convertor {

    public ConversionResult convertFromString(String text, Locale locale, FormatCache formatCache) {
        try {
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(text));

            SAXParser parser = new SAXParser();
            parser.setFeature("http://xml.org/sax/features/namespaces", true);
            parser.setFeature("http://cyberneko.org/html/features/override-namespaces", false);
            parser.setFeature("http://cyberneko.org/html/features/insert-namespaces", false);
            parser.setFeature("http://cyberneko.org/html/features/scanner/ignore-specified-charset", true);
            parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "UTF-8");
            parser.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
            parser.setProperty("http://cyberneko.org/html/properties/names/attrs", "lower");


            // TODO creating a sax transformer factory is probably expensive?
            SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
            TransformerHandler serializer = transformerFactory.newTransformerHandler();

            serializer.getTransformer().setOutputProperty(OutputKeys.METHOD, "xml");
            serializer.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            StringWriter writer = new StringWriter();
            serializer.setResult(new StreamResult(writer));

            parser.setContentHandler(serializer);
            parser.setProperty("http://xml.org/sax/properties/lexical-handler", serializer);
            parser.parse(is);

            return new ConversionResult(writer.toString());
        } catch (Throwable e) {
            Throwable t = ExceptionUtils.getRootCause(e);
            if (t == null)
                t = e;
            String message = t.getMessage();
            if (message == null)
                message = t.toString();
            ValidationError validationError = new ValidationError(message, false);
            return new ConversionResult(validationError);

        }
    }

    public String convertToString(Object object, Locale locale, FormatCache formatCache) {
        return (String)object;
    }

    public Class getTypeClass() {
        return java.lang.String.class;
    }

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
        // nothing to say about me
    }
}
