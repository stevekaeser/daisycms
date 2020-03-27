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
package org.outerj.daisy.books.publisher.impl.util;

import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlCursor;

import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class CustomImplementationHelper {
    /**
     * Instantiates a class trying different constructors: first one taking an xmlObject,
     * then one taking a Map, and finally the default constructor
     */
    public static Object instantiateComponent(Class clazz, XmlObject xmlObject) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        // First try constructor taking an XmlObject as argument
        Constructor constructor = null;
        try {
            constructor = clazz.getConstructor(XmlObject.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        if (constructor != null) {
            return constructor.newInstance(xmlObject);
        }

        // Then try a constructor which takes a map as argument
        try {
            constructor = clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        if (constructor != null) {
            Map attributes = getAttributes(xmlObject);
            return constructor.newInstance(attributes);
        }

        // Finally use the default constructor
        return clazz.newInstance();

    }

    private static Map<String, String> getAttributes(XmlObject xmlObject) {
        Map<String, String> attributes = new HashMap<String, String>();
        XmlCursor cursor = xmlObject.newCursor();
        cursor.toFirstAttribute();
        do {
            attributes.put(cursor.getName().getLocalPart(), cursor.getTextValue());
        } while (cursor.toNextAttribute());
        cursor.dispose();
        return attributes;
    }
}
