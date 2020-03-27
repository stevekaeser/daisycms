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
package org.outerj.daisy.frontend.components.misc;

import org.apache.cocoon.forms.validation.WidgetValidator;
import org.apache.cocoon.forms.validation.ValidationErrorAware;
import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.formmodel.Widget;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.outerj.daisy.xmlutil.LocalSAXParserFactory;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import java.io.StringReader;
import java.io.IOException;

/**
 * An XML well-formedness validator for CForms.
 */
public class XmlValidator implements WidgetValidator {
    public boolean validate(Widget widget) {
        Object value = widget.getValue();
        if (value != null && value instanceof String) {
            SAXParserFactory parserFactory = LocalSAXParserFactory.getSAXParserFactory();
            SAXParser parser;
            try {
                parser = parserFactory.newSAXParser();
            } catch (Exception e) {
                throw new RuntimeException("Unexpected error constructing SAX parser in XML validator.", e);
            }
            try {
                parser.parse(new InputSource(new StringReader((String)value)), new DefaultHandler());
            } catch (SAXException e) {
                ((ValidationErrorAware)widget).setValidationError(new ValidationError("Well-formed XML required.", false));
                return false;
            } catch (IOException e) {
                throw new RuntimeException("Unexpected IO exception in XML validator.", e);
            }
        }
        return true;
    }
}
