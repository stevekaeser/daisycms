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

import org.apache.cocoon.forms.datatype.convertor.ConvertorBuilder;
import org.apache.cocoon.forms.datatype.convertor.Convertor;
import org.apache.cocoon.forms.util.DomHelper;
import org.apache.xmlbeans.XmlOptions;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.io.Reader;

public class XmlBeansConvertorBuilder implements ConvertorBuilder {
    public Convertor build(Element element) throws Exception {
        String xmlbeansClassName = DomHelper.getAttribute(element, "class");
        Class xmlbeansClass = this.getClass().getClassLoader().loadClass(xmlbeansClassName);
        Class[] factoryClasses = xmlbeansClass.getDeclaredClasses();
        Class factoryClass = null;
        for (int i = 0; i < factoryClasses.length; i++) {
            if (factoryClasses[i].getName().endsWith("$Factory")) {
                factoryClass = factoryClasses[i];
                break;
            }
        }
        if (factoryClass == null) {
            throw new Exception("Could not find Factory inner class in XmlBeans class " + xmlbeansClassName);
        }

        Method parseMethod = factoryClass.getMethod("parse", Reader.class, XmlOptions.class);
        return new XmlBeansConvertor(parseMethod);
    }
}
