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
package org.outerj.daisy.frontend.editor;

import org.apache.cocoon.forms.validation.ValidationError;
import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.datatype.convertor.ConversionResult;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlObject;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.outerj.daisy.frontend.util.TaggedMessage;
import org.outerj.daisy.frontend.util.MultiMessage;

import java.io.StringReader;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class XmlBeansConvertor implements Convertor {
    private Method parseMethod;

    public XmlBeansConvertor(Method parseMethod) {
        this.parseMethod = parseMethod;
    }

    public ConversionResult convertFromString(String value, Locale locale, FormatCache formatCache) {
        List errors = new ArrayList();
        boolean valid;

        XmlObject document;
        try {
            XmlOptions xmlOptions = new XmlOptions();
            xmlOptions.setDocumentSourceName("edited xml");
            xmlOptions.setErrorListener(errors);
            xmlOptions.setLoadLineNumbers();
            document = (XmlObject)parseMethod.invoke(null, new StringReader(value), xmlOptions);

            xmlOptions = new XmlOptions();
            xmlOptions.setErrorListener(errors);
            valid = document.validate(xmlOptions);
        } catch (InvocationTargetException e) {
            Throwable targetException = e.getTargetException();
            if (targetException instanceof XmlException) {
                return new ConversionResult(validationErrorForXmlErrors(((XmlException)targetException).getErrors()));
            } else {
                return new ConversionResult(new ValidationError("Problem parsing navigtion tree: " + e.getMessage(), false));
            }
        } catch (Exception e) {
            return new ConversionResult(new ValidationError("Problem parsing navigtion tree: " + e.getMessage(), false));
        }

        if (!valid) {
            return new ConversionResult(validationErrorForXmlErrors(errors));
        } else {
            return new ConversionResult(document.toString());
        }
    }

    private ValidationError validationErrorForXmlErrors(Collection xmlErrors) {
        MultiMessage message = new MultiMessage();
        message.addMessage(new TaggedMessage("title", "Error parsing or validating XML:"));
        Iterator errorsIt = xmlErrors.iterator();
        while (errorsIt.hasNext()) {
            XmlError error = (XmlError)errorsIt.next();
            message.addMessage(new TaggedMessage("error", error.getMessage()));
        }
        return new ValidationError(message);
    }

    public String convertToString(Object o, Locale locale, Convertor.FormatCache formatCache) {
        return (String)o;
    }

    public Class getTypeClass() {
        return java.lang.String.class;
    }

    public void generateSaxFragment(ContentHandler contentHandler, Locale locale) throws SAXException {
    }

}
