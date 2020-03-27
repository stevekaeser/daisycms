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
package org.outerj.daisy.books.store.impl;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlOptions;
import org.apache.xmlbeans.XmlError;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class XmlUtil {
    public static String validate(XmlObject xmlObject) {
        XmlOptions xmlOptions = new XmlOptions();
        List errors = new ArrayList();
        xmlOptions.setErrorListener(errors);
        boolean valid = xmlObject.validate(xmlOptions);
        if (!valid) {
            StringBuilder message = new StringBuilder();
            Iterator errorsIt = errors.iterator();
            while (errorsIt.hasNext()) {
                XmlError error = (XmlError)errorsIt.next();
                message.append(error.getMessage());
                if (errorsIt.hasNext())
                    message.append(", ");
            }
            return message.toString();
        }
        return null;
    }
}
